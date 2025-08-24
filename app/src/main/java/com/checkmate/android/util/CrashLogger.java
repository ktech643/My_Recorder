package com.checkmate.android.util;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.checkmate.android.AppConstant;
import com.checkmate.android.AppPreference;
import com.checkmate.android.MyApp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Thread-safe crash logger that captures all debug logs and crashes
 * Stores logs in app's internal directory with session override
 */
public class CrashLogger implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "CrashLogger";
    private static volatile CrashLogger instance;
    private static final Object lock = new Object();
    
    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;
    private final ExecutorService logExecutor;
    private final SimpleDateFormat dateFormat;
    private final Handler mainHandler;
    
    private File logFile;
    private File crashFile;
    private BufferedWriter logWriter;
    private final Object writerLock = new Object();
    
    private static final int MAX_LOG_SIZE = 10 * 1024 * 1024; // 10MB max log size
    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE = "app_log.txt";
    private static final String CRASH_FILE = "crash_log.txt";
    
    private CrashLogger(Context context) {
        this.context = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        this.logExecutor = Executors.newSingleThreadExecutor();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        initializeLogFiles();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }
    
    public static CrashLogger getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null && MyApp.getInstance() != null) {
                    instance = new CrashLogger(MyApp.getInstance());
                }
            }
        }
        return instance;
    }
    
    private void initializeLogFiles() {
        logExecutor.execute(() -> {
            try {
                File logDir = new File(context.getFilesDir(), LOG_DIR);
                if (!logDir.exists() && !logDir.mkdirs()) {
                    Log.e(TAG, "Failed to create log directory");
                    return;
                }
                
                // Create/overwrite log files for new session
                logFile = new File(logDir, LOG_FILE);
                crashFile = new File(logDir, CRASH_FILE);
                
                // Delete old logs to start fresh
                if (logFile.exists()) {
                    logFile.delete();
                }
                if (crashFile.exists()) {
                    crashFile.delete();
                }
                
                // Create new log file
                logFile.createNewFile();
                
                synchronized (writerLock) {
                    logWriter = new BufferedWriter(new FileWriter(logFile, false));
                    writeHeader();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize log files", e);
            }
        });
    }
    
    private void writeHeader() throws IOException {
        logWriter.write("=== CheckMate App Log ===\n");
        logWriter.write("Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n");
        logWriter.write("Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")\n");
        logWriter.write("App Version: " + AppConstant.VERSION_NAME + "\n");
        logWriter.write("Session Start: " + dateFormat.format(new Date()) + "\n");
        logWriter.write("================================\n\n");
        logWriter.flush();
    }
    
    /**
     * Log a debug message
     */
    public void logDebug(String tag, String message) {
        log("DEBUG", tag, message, null);
    }
    
    /**
     * Log an info message
     */
    public void logInfo(String tag, String message) {
        log("INFO", tag, message, null);
    }
    
    /**
     * Log a warning message
     */
    public void logWarning(String tag, String message) {
        log("WARN", tag, message, null);
    }
    
    /**
     * Log an error message
     */
    public void logError(String tag, String message) {
        log("ERROR", tag, message, null);
    }
    
    /**
     * Log an error with exception
     */
    public void logError(String tag, Throwable throwable) {
        log("ERROR", tag, throwable.getMessage(), throwable);
    }
    
    /**
     * Log an error with message and exception
     */
    public void logError(String tag, String message, Throwable throwable) {
        log("ERROR", tag, message, throwable);
    }
    
    /**
     * Core logging method - thread safe
     */
    private void log(String level, String tag, String message, Throwable throwable) {
        if (logWriter == null) return;
        
        logExecutor.execute(() -> {
            try {
                synchronized (writerLock) {
                    if (logWriter == null) return;
                    
                    // Check file size and rotate if needed
                    if (logFile.length() > MAX_LOG_SIZE) {
                        rotateLogFile();
                    }
                    
                    // Write timestamp and log level
                    logWriter.write(dateFormat.format(new Date()));
                    logWriter.write(" [");
                    logWriter.write(level);
                    logWriter.write("] ");
                    logWriter.write(tag);
                    logWriter.write(": ");
                    
                    // Write message
                    if (message != null) {
                        logWriter.write(message);
                    }
                    
                    // Write stack trace if available
                    if (throwable != null) {
                        logWriter.write("\n");
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        throwable.printStackTrace(pw);
                        logWriter.write(sw.toString());
                    }
                    
                    logWriter.write("\n");
                    logWriter.flush();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to write log", e);
            }
        });
    }
    
    /**
     * Rotate log file when it gets too large
     */
    private void rotateLogFile() {
        try {
            synchronized (writerLock) {
                if (logWriter != null) {
                    logWriter.close();
                }
                
                // Archive old log
                File archiveFile = new File(logFile.getParent(), "app_log_old.txt");
                if (archiveFile.exists()) {
                    archiveFile.delete();
                }
                logFile.renameTo(archiveFile);
                
                // Create new log file
                logFile = new File(logFile.getParent(), LOG_FILE);
                logFile.createNewFile();
                logWriter = new BufferedWriter(new FileWriter(logFile, false));
                writeHeader();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to rotate log file", e);
        }
    }
    
    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        try {
            // Log crash to file
            logCrash(thread, throwable);
            
            // Save crash info to preferences for recovery
            saveCrashInfo(throwable);
            
            // Try to handle ANR recovery
            if (isANRException(throwable)) {
                handleANRRecovery();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling uncaught exception", e);
        }
        
        // Call default handler
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, throwable);
        }
    }
    
    /**
     * Log crash details to crash file
     */
    private void logCrash(Thread thread, Throwable throwable) {
        try {
            File logDir = new File(context.getFilesDir(), LOG_DIR);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            crashFile = new File(logDir, CRASH_FILE);
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(crashFile, true))) {
                writer.write("\n=== CRASH REPORT ===\n");
                writer.write("Time: " + dateFormat.format(new Date()) + "\n");
                writer.write("Thread: " + thread.getName() + " (ID: " + thread.getId() + ")\n");
                writer.write("Exception: " + throwable.getClass().getName() + "\n");
                writer.write("Message: " + throwable.getMessage() + "\n");
                writer.write("\nStack Trace:\n");
                
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                writer.write(sw.toString());
                
                writer.write("\n=== END CRASH REPORT ===\n");
                writer.flush();
            }
            
            // Also log to regular log
            logError("CRASH", "Uncaught exception on thread: " + thread.getName(), throwable);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to log crash", e);
        }
    }
    
    /**
     * Save crash info to preferences for recovery
     */
    private void saveCrashInfo(Throwable throwable) {
        try {
            String crashTime = String.valueOf(System.currentTimeMillis());
            String crashMessage = throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
            
            AppPreference.setStr("last_crash_time", crashTime);
            AppPreference.setStr("last_crash_message", crashMessage);
            AppPreference.setInt("crash_count", AppPreference.getInt("crash_count", 0) + 1);
        } catch (Exception e) {
            Log.e(TAG, "Failed to save crash info", e);
        }
    }
    
    /**
     * Check if exception is ANR related
     */
    private boolean isANRException(Throwable throwable) {
        String message = throwable.getMessage();
        return throwable instanceof RuntimeException && 
               message != null && 
               (message.contains("ANR") || 
                message.contains("Application Not Responding") ||
                message.contains("Blocked") ||
                message.contains("Watchdog"));
    }
    
    /**
     * Handle ANR recovery
     */
    private void handleANRRecovery() {
        try {
            // Set ANR flag for app restart
            AppPreference.setBool("anr_recovery_needed", true);
            
            // Log ANR
            logError("ANR", "Application Not Responding detected, attempting recovery");
            
            // Try to clear any blocking operations
            mainHandler.post(() -> {
                try {
                    // Clear any pending UI operations
                    mainHandler.removeCallbacksAndMessages(null);
                } catch (Exception e) {
                    Log.e(TAG, "Error clearing main handler", e);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle ANR recovery", e);
        }
    }
    
    /**
     * Get log file path
     */
    public String getLogFilePath() {
        return logFile != null ? logFile.getAbsolutePath() : null;
    }
    
    /**
     * Get crash file path
     */
    public String getCrashFilePath() {
        return crashFile != null ? crashFile.getAbsolutePath() : null;
    }
    
    /**
     * Clear all logs
     */
    public void clearLogs() {
        logExecutor.execute(() -> {
            try {
                synchronized (writerLock) {
                    if (logWriter != null) {
                        logWriter.close();
                    }
                }
                
                if (logFile != null && logFile.exists()) {
                    logFile.delete();
                }
                if (crashFile != null && crashFile.exists()) {
                    crashFile.delete();
                }
                
                initializeLogFiles();
            } catch (Exception e) {
                Log.e(TAG, "Failed to clear logs", e);
            }
        });
    }
    
    /**
     * Shutdown logger
     */
    public void shutdown() {
        logExecutor.execute(() -> {
            try {
                synchronized (writerLock) {
                    if (logWriter != null) {
                        logWriter.flush();
                        logWriter.close();
                        logWriter = null;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to shutdown logger", e);
            }
        });
        
        logExecutor.shutdown();
    }
}