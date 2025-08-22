package com.checkmate.android.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.checkmate.android.BuildConfig;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Comprehensive Logger utility for the CheckMate application
 * Logs to both Logcat and local files for debugging and crash tracking
 */
public class Logger {
    private static final String TAG = "CheckMate";
    private static final String LOG_FILE_NAME = "checkmate_log.txt";
    private static final String CRASH_FILE_NAME = "checkmate_crashes.txt";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    private static File logFile;
    private static File crashFile;
    private static ExecutorService executorService;
    private static boolean initialized = false;
    private static boolean enableFileLogging = true;
    private static boolean loggingEnabled = true; // Master flag - default ON for file logging
    private static boolean consoleLoggingEnabled = false; // Separate flag for console logging

    // Log levels
    public static final int VERBOSE = Log.VERBOSE;
    public static final int DEBUG = Log.DEBUG;
    public static final int INFO = Log.INFO;
    public static final int WARN = Log.WARN;
    public static final int ERROR = Log.ERROR;
    public static final int ASSERT = Log.ASSERT;

    /**
     * Enable or disable all logging
     * @param enabled true to enable logging, false to disable
     */
    public static void setLoggingEnabled(boolean enabled) {
        loggingEnabled = enabled;
        if (enabled && consoleLoggingEnabled) {
            Log.i(TAG, "Logging ENABLED");
        }
    }

    /**
     * Enable or disable console logging
     * @param enabled true to enable console logging, false to disable
     */
    public static void setConsoleLoggingEnabled(boolean enabled) {
        consoleLoggingEnabled = enabled;
    }

    /**
     * Check if logging is enabled
     */
    public static boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    /**
     * Initialize the logger with context
     */
    public static void init(Context context) {
        if (initialized) return;

        executorService = Executors.newSingleThreadExecutor();

        // Create log directory
        File logDir = new File(context.getExternalFilesDir(null), "logs");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        // Initialize log files
        logFile = new File(logDir, LOG_FILE_NAME);
        crashFile = new File(logDir, CRASH_FILE_NAME);

        // Always create files if they don't exist
        createLogFilesIfNeeded();

        initialized = true;

        // Log initialization to file only
        i("Logger", "Logger initialized successfully");
        i("Logger", "Log file path: " + logFile.getAbsolutePath());
        i("Logger", "Crash file path: " + crashFile.getAbsolutePath());
    }

    /**
     * Create log files if they don't exist
     */
    private static void createLogFilesIfNeeded() {
        try {
            if (logFile != null && !logFile.exists()) {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
            }
            if (crashFile != null && !crashFile.exists()) {
                crashFile.getParentFile().mkdirs();
                crashFile.createNewFile();
            }
        } catch (IOException e) {
            // Only log to console if console logging is enabled
            if (consoleLoggingEnabled) {
                Log.e(TAG, "Failed to create log files", e);
            }
        }
    }

    /**
     * Enable or disable file logging
     */
    public static void setFileLoggingEnabled(boolean enabled) {
        enableFileLogging = enabled;
    }

    /**
     * Verbose log
     */
    public static void v(String tag, String message) {
        log(VERBOSE, tag, message, null);
    }

    /**
     * Debug log
     */
    public static void d(String tag, String message) {
        log(DEBUG, tag, message, null);
    }

    /**
     * Info log
     */
    public static void i(String tag, String message) {
        log(INFO, tag, message, null);
    }

    /**
     * Warning log
     */
    public static void w(String tag, String message) {
        log(WARN, tag, message, null);
    }

    /**
     * Warning log with throwable
     */
    public static void w(String tag, String message, Throwable throwable) {
        log(WARN, tag, message, throwable);
    }

    /**
     * Error log
     */
    public static void e(String tag, String message) {
        log(ERROR, tag, message, null);
    }

    /**
     * Error log with throwable
     */
    public static void e(String tag, String message, Throwable throwable) {
        log(ERROR, tag, message, throwable);
    }

    /**
     * Log activity lifecycle events
     */
    public static void logActivityLifecycle(String activityName, String event) {
        i("ActivityLifecycle", activityName + " - " + event);
    }

    /**
     * Log method entry
     */
    public static void methodEntry(String tag, String methodName) {
        d(tag, ">>> " + methodName);
    }

    /**
     * Log method exit
     */
    public static void methodExit(String tag, String methodName) {
        d(tag, "<<< " + methodName);
    }

    /**
     * Log method exit with result
     */
    public static void methodExit(String tag, String methodName, Object result) {
        d(tag, "<<< " + methodName + " - Result: " + result);
    }

    /**
     * Log network requests
     */
    public static void logNetworkRequest(String url, String method, int responseCode) {
        i("Network", String.format("Request: %s %s - Response: %d", method, url, responseCode));
    }

    /**
     * Log network error
     */
    public static void logNetworkError(String url, String method, Throwable error) {
        e("Network", String.format("Request failed: %s %s", method, url), error);
    }

    /**
     * Log crash to dedicated crash file
     */
    public static void logCrash(Thread thread, Throwable throwable) {
        String crashInfo = buildCrashInfo(thread, throwable);

        // Log to regular log
        e("CRASH", crashInfo, throwable);

        // Write to crash file
        if (initialized && enableFileLogging) {
            writeToFile(crashFile, crashInfo, true);
        }
    }

    /**
     * Main logging method
     */
    private static void log(int level, String tag, String message, Throwable throwable) {
        // Check if logging is enabled at all
        if (!loggingEnabled) {
            return;
        }

        // Only log to Logcat if console logging is enabled
        if (consoleLoggingEnabled) {
            String fullTag = TAG + "/" + tag;
            switch (level) {
                case VERBOSE:
                    if (throwable != null) {
                        Log.v(fullTag, message, throwable);
                    } else {
                        Log.v(fullTag, message);
                    }
                    break;
                case DEBUG:
                    if (throwable != null) {
                        Log.d(fullTag, message, throwable);
                    } else {
                        Log.d(fullTag, message);
                    }
                    break;
                case INFO:
                    if (throwable != null) {
                        Log.i(fullTag, message, throwable);
                    } else {
                        Log.i(fullTag, message);
                    }
                    break;
                case WARN:
                    if (throwable != null) {
                        Log.w(fullTag, message, throwable);
                    } else {
                        Log.w(fullTag, message);
                    }
                    break;
                case ERROR:
                    if (throwable != null) {
                        Log.e(fullTag, message, throwable);
                    } else {
                        Log.e(fullTag, message);
                    }
                    break;
            }
        }

        // Write to file if enabled (only to file, not console)
        if (initialized && enableFileLogging) {
            // Ensure files exist before writing
            createLogFilesIfNeeded();
            String logEntry = buildLogEntry(level, tag, message, throwable);
            writeToFile(logFile, logEntry, false);
        }
    }

    /**
     * Build formatted log entry
     */
    private static String buildLogEntry(int level, String tag, String message, Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(dateFormat.format(new Date()));
        sb.append(" ");
        sb.append(getLevelString(level));
        sb.append("/");
        sb.append(tag);
        sb.append(": ");
        sb.append(message);

        if (throwable != null) {
            sb.append("\n");
            sb.append(Log.getStackTraceString(throwable));
        }

        return sb.toString();
    }

    /**
     * Build crash info string
     */
    private static String buildCrashInfo(Thread thread, Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n==================== CRASH REPORT ====================\n");
        sb.append("Time: ").append(dateFormat.format(new Date())).append("\n");
        sb.append("Thread: ").append(thread.getName()).append("\n");
        sb.append("Version: ").append(BuildConfig.VERSION_NAME)
                .append(" (").append(BuildConfig.VERSION_CODE).append(")\n");
        sb.append("Build Type: ").append(BuildConfig.BUILD_TYPE).append("\n");
        sb.append("\nStack Trace:\n");
        sb.append(Log.getStackTraceString(throwable));
        sb.append("\n======================================================\n");
        return sb.toString();
    }

    /**
     * Write to file asynchronously
     */
    private static void writeToFile(final File file, final String content, final boolean isCrash) {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.execute(() -> {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                    writer.write(content);
                    writer.newLine();
                    writer.flush();
                } catch (IOException e) {
                    // Don't log to console, just silently fail
                    // Could implement a circular buffer for failed writes if needed
                }
            });
        }
    }

    /**
     * Get string representation of log level
     */
    private static String getLevelString(int level) {
        switch (level) {
            case VERBOSE: return "V";
            case DEBUG: return "D";
            case INFO: return "I";
            case WARN: return "W";
            case ERROR: return "E";
            default: return "?";
        }
    }

    /**
     * Clear log files
     */
    public static void clearLogs() {
        if (initialized) {
            executorService.execute(() -> {
                try {
                    new FileWriter(logFile, false).close();
                    new FileWriter(crashFile, false).close();
                    i("Logger", "Log files cleared");
                } catch (IOException e) {
                    // Don't log to console
                }
            });
        }
    }

    /**
     * Get log file path
     */
    public static String getLogFilePath() {
        return logFile != null ? logFile.getAbsolutePath() : null;
    }

    /**
     * Get crash file path
     */
    public static String getCrashFilePath() {
        return crashFile != null ? crashFile.getAbsolutePath() : null;
    }

    /**
     * Shutdown the logger
     */
    public static void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}