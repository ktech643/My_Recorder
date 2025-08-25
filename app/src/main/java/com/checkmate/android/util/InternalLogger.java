package com.checkmate.android.util;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Internal logger and crash tracking mechanism for the CheckMate Android app.
 * Stores logs in app's internal directory and handles session-based log management.
 * Thread-safe implementation to prevent ANR issues.
 */
public class InternalLogger {
    private static final String TAG = "InternalLogger";
    private static final String LOG_DIR = "logs";
    private static final String CRASH_DIR = "crashes";
    private static final String LOG_FILE_PREFIX = "app_log_";
    private static final String CRASH_FILE_PREFIX = "crash_";
    private static final String FILE_EXTENSION = ".txt";
    private static final int MAX_LOG_FILES = 5;
    private static final int MAX_CRASH_FILES = 10;
    private static final int MAX_LOG_SIZE_MB = 5;
    private static final long MAX_LOG_SIZE_BYTES = MAX_LOG_SIZE_MB * 1024 * 1024;

    private static volatile InternalLogger instance;
    private final Context context;
    private final HandlerThread loggerThread;
    private final Handler loggerHandler;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat fileNameFormat;
    private final ConcurrentLinkedQueue<LogEntry> logQueue;
    private final AtomicBoolean isInitialized;
    private final Object writeLock = new Object();
    
    private File logDirectory;
    private File crashDirectory;
    private File currentLogFile;

    private enum LogLevel {
        VERBOSE, DEBUG, INFO, WARN, ERROR, CRASH
    }

    private static class LogEntry {
        final long timestamp;
        final LogLevel level;
        final String tag;
        final String message;
        final Throwable throwable;

        LogEntry(LogLevel level, String tag, String message, Throwable throwable) {
            this.timestamp = System.currentTimeMillis();
            this.level = level;
            this.tag = tag;
            this.message = message;
            this.throwable = throwable;
        }
    }

    private InternalLogger(Context context) {
        this.context = context.getApplicationContext();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        this.fileNameFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault());
        this.logQueue = new ConcurrentLinkedQueue<>();
        this.isInitialized = new AtomicBoolean(false);
        
        // Create background thread for logging operations
        this.loggerThread = new HandlerThread("InternalLogger", Thread.MIN_PRIORITY);
        this.loggerThread.start();
        this.loggerHandler = new Handler(this.loggerThread.getLooper());
        
        initialize();
    }

    /**
     * Get singleton instance of InternalLogger
     */
    public static InternalLogger getInstance(Context context) {
        if (instance == null) {
            synchronized (InternalLogger.class) {
                if (instance == null) {
                    instance = new InternalLogger(context);
                }
            }
        }
        return instance;
    }

    /**
     * Initialize logger with proper error handling and recovery
     */
    private void initialize() {
        loggerHandler.post(() -> {
            try {
                setupDirectories();
                createNewLogFile();
                setupCrashHandler();
                isInitialized.set(true);
                Log.i(TAG, "InternalLogger initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize InternalLogger", e);
                // Fallback: still mark as initialized to prevent blocking
                isInitialized.set(true);
            }
        });
    }

    /**
     * Setup log and crash directories with proper error handling
     */
    private void setupDirectories() throws IOException {
        File internalDir = context.getFilesDir();
        
        logDirectory = new File(internalDir, LOG_DIR);
        crashDirectory = new File(internalDir, CRASH_DIR);
        
        if (!logDirectory.exists() && !logDirectory.mkdirs()) {
            throw new IOException("Failed to create log directory");
        }
        
        if (!crashDirectory.exists() && !crashDirectory.mkdirs()) {
            throw new IOException("Failed to create crash directory");
        }
        
        // Clean up old files
        cleanupOldFiles(logDirectory, MAX_LOG_FILES);
        cleanupOldFiles(crashDirectory, MAX_CRASH_FILES);
    }

    /**
     * Create new log file for current session
     */
    private void createNewLogFile() throws IOException {
        String fileName = LOG_FILE_PREFIX + fileNameFormat.format(new Date()) + FILE_EXTENSION;
        currentLogFile = new File(logDirectory, fileName);
        
        if (!currentLogFile.createNewFile()) {
            Log.w(TAG, "Log file already exists: " + fileName);
        }
        
        // Write session header
        writeToFile(currentLogFile, "=== NEW SESSION STARTED ===\n" +
                "Timestamp: " + dateFormat.format(new Date()) + "\n" +
                "App Version: " + getAppVersion() + "\n" +
                "Device: " + android.os.Build.MODEL + " (" + android.os.Build.VERSION.RELEASE + ")\n" +
                "================================\n\n");
    }

    /**
     * Setup uncaught exception handler for crash tracking
     */
    private void setupCrashHandler() {
        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            try {
                logCrash(exception, thread.getName());
            } catch (Exception e) {
                Log.e(TAG, "Failed to log crash", e);
            }
            
            // Call original handler to maintain default behavior
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, exception);
            }
        });
    }

    /**
     * Log crash to separate crash file
     */
    private void logCrash(Throwable exception, String threadName) {
        try {
            String fileName = CRASH_FILE_PREFIX + fileNameFormat.format(new Date()) + FILE_EXTENSION;
            File crashFile = new File(crashDirectory, fileName);
            
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            
            printWriter.println("=== CRASH REPORT ===");
            printWriter.println("Timestamp: " + dateFormat.format(new Date()));
            printWriter.println("Thread: " + threadName);
            printWriter.println("App Version: " + getAppVersion());
            printWriter.println("Device: " + android.os.Build.MODEL + " (" + android.os.Build.VERSION.RELEASE + ")");
            printWriter.println("SDK Version: " + android.os.Build.VERSION.SDK_INT);
            printWriter.println("\nException Details:");
            exception.printStackTrace(printWriter);
            printWriter.println("\n=== END CRASH REPORT ===");
            
            writeToFile(crashFile, stringWriter.toString());
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to write crash report", e);
        }
    }

    /**
     * Public logging methods - thread-safe
     */
    public static void v(String tag, String message) {
        log(LogLevel.VERBOSE, tag, message, null);
    }

    public static void d(String tag, String message) {
        log(LogLevel.DEBUG, tag, message, null);
    }

    public static void i(String tag, String message) {
        log(LogLevel.INFO, tag, message, null);
    }

    public static void w(String tag, String message) {
        log(LogLevel.WARN, tag, message, null);
    }

    public static void w(String tag, String message, Throwable throwable) {
        log(LogLevel.WARN, tag, message, throwable);
    }

    public static void e(String tag, String message) {
        log(LogLevel.ERROR, tag, message, null);
    }

    public static void e(String tag, String message, Throwable throwable) {
        log(LogLevel.ERROR, tag, message, throwable);
    }

    /**
     * Internal logging method with ANR prevention
     */
    private static void log(LogLevel level, String tag, String message, Throwable throwable) {
        // Also log to Android system log
        switch (level) {
            case VERBOSE:
                Log.v(tag, message, throwable);
                break;
            case DEBUG:
                Log.d(tag, message, throwable);
                break;
            case INFO:
                Log.i(tag, message, throwable);
                break;
            case WARN:
                Log.w(tag, message, throwable);
                break;
            case ERROR:
                Log.e(tag, message, throwable);
                break;
        }

        if (instance != null) {
            try {
                LogEntry entry = new LogEntry(level, tag, message, throwable);
                instance.logQueue.offer(entry);
                
                // Process logs on background thread to prevent ANR
                instance.loggerHandler.post(instance::processLogQueue);
            } catch (Exception e) {
                Log.e(TAG, "Failed to queue log entry", e);
            }
        }
    }

    /**
     * Process log queue on background thread
     */
    private void processLogQueue() {
        if (!isInitialized.get()) {
            return;
        }

        try {
            LogEntry entry;
            StringBuilder logBuffer = new StringBuilder();
            
            // Process up to 50 entries at once to improve performance
            int count = 0;
            while ((entry = logQueue.poll()) != null && count < 50) {
                String logLine = formatLogEntry(entry);
                logBuffer.append(logLine).append("\n");
                count++;
            }
            
            if (logBuffer.length() > 0) {
                synchronized (writeLock) {
                    // Check file size and rotate if necessary
                    if (currentLogFile != null && currentLogFile.length() > MAX_LOG_SIZE_BYTES) {
                        try {
                            createNewLogFile();
                        } catch (IOException e) {
                            Log.e(TAG, "Failed to rotate log file", e);
                        }
                    }
                    
                    if (currentLogFile != null) {
                        writeToFile(currentLogFile, logBuffer.toString());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing log queue", e);
        }
    }

    /**
     * Format log entry to string
     */
    private String formatLogEntry(LogEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append(dateFormat.format(new Date(entry.timestamp)));
        sb.append(" ").append(entry.level.name().charAt(0));
        sb.append("/").append(entry.tag);
        sb.append(": ").append(entry.message);
        
        if (entry.throwable != null) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            entry.throwable.printStackTrace(printWriter);
            sb.append("\n").append(stringWriter.toString());
        }
        
        return sb.toString();
    }

    /**
     * Thread-safe file writing with error recovery
     */
    private void writeToFile(File file, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.write(content);
            writer.flush();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to log file: " + file.getName(), e);
            // Try to recover by creating a new file
            try {
                if (file.equals(currentLogFile)) {
                    createNewLogFile();
                }
            } catch (IOException recoveryException) {
                Log.e(TAG, "Failed to recover from write error", recoveryException);
            }
        }
    }

    /**
     * Clean up old files to maintain storage limits
     */
    private void cleanupOldFiles(File directory, int maxFiles) {
        try {
            File[] files = directory.listFiles();
            if (files == null || files.length <= maxFiles) {
                return;
            }

            // Sort by last modified time
            java.util.Arrays.sort(files, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));

            // Delete oldest files
            for (int i = 0; i < files.length - maxFiles; i++) {
                if (files[i].delete()) {
                    Log.d(TAG, "Deleted old log file: " + files[i].getName());
                } else {
                    Log.w(TAG, "Failed to delete old log file: " + files[i].getName());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up old files", e);
        }
    }

    /**
     * Get app version for logging
     */
    private String getAppVersion() {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * Flush all pending logs immediately (for critical situations)
     */
    public void flush() {
        if (loggerHandler != null) {
            loggerHandler.post(this::processLogQueue);
        }
    }

    /**
     * Cleanup resources when app is shutting down
     */
    public void shutdown() {
        if (loggerHandler != null) {
            loggerHandler.post(() -> {
                processLogQueue(); // Flush remaining logs
                if (loggerThread != null) {
                    loggerThread.quitSafely();
                }
            });
        }
    }

    /**
     * Get log directory for external access
     */
    public File getLogDirectory() {
        return logDirectory;
    }

    /**
     * Get crash directory for external access
     */
    public File getCrashDirectory() {
        return crashDirectory;
    }
}