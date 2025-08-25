package com.checkmate.android.logging;

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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Internal logger for capturing all debug logs and crashes
 * Thread-safe implementation with background logging
 */
public class InternalLogger {
    private static final String TAG = "InternalLogger";
    private static final String LOG_FILE_NAME = "app_log.txt";
    private static final String CRASH_FILE_NAME = "crash_log.txt";
    private static final long MAX_LOG_SIZE = 10 * 1024 * 1024; // 10MB
    
    private static volatile InternalLogger instance;
    private final Context context;
    private final Handler logHandler;
    private final HandlerThread logThread;
    private final SimpleDateFormat dateFormat;
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private File logFile;
    private File crashFile;
    
    private InternalLogger(Context context) {
        this.context = context.getApplicationContext();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        
        // Create background thread for logging
        logThread = new HandlerThread("InternalLoggerThread");
        logThread.start();
        logHandler = new Handler(logThread.getLooper());
        
        initializeLogFiles();
        setupUncaughtExceptionHandler();
        isInitialized.set(true);
    }
    
    public static void initialize(Context context) {
        if (instance == null) {
            synchronized (InternalLogger.class) {
                if (instance == null) {
                    instance = new InternalLogger(context);
                }
            }
        }
    }
    
    public static InternalLogger getInstance() {
        if (instance == null) {
            throw new IllegalStateException("InternalLogger must be initialized first");
        }
        return instance;
    }
    
    private void initializeLogFiles() {
        try {
            File logDir = new File(context.getFilesDir(), "logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            logFile = new File(logDir, LOG_FILE_NAME);
            crashFile = new File(logDir, CRASH_FILE_NAME);
            
            // Check and rotate log files if needed
            rotateLogFileIfNeeded(logFile);
            rotateLogFileIfNeeded(crashFile);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize log files", e);
        }
    }
    
    private void rotateLogFileIfNeeded(File file) {
        if (file.exists() && file.length() > MAX_LOG_SIZE) {
            File backupFile = new File(file.getAbsolutePath() + ".old");
            if (backupFile.exists()) {
                backupFile.delete();
            }
            file.renameTo(backupFile);
        }
    }
    
    private void setupUncaughtExceptionHandler() {
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                logCrash(throwable);
                
                // Call the default handler
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, throwable);
                }
            }
        });
    }
    
    // Logging methods with different levels
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
        log(LogLevel.WARNING, tag, message, null);
    }
    
    public static void w(String tag, String message, Throwable throwable) {
        log(LogLevel.WARNING, tag, message, throwable);
    }
    
    public static void e(String tag, String message) {
        log(LogLevel.ERROR, tag, message, null);
    }
    
    public static void e(String tag, String message, Throwable throwable) {
        log(LogLevel.ERROR, tag, message, throwable);
    }
    
    private static void log(LogLevel level, String tag, String message, Throwable throwable) {
        // Log to Android logcat
        switch (level) {
            case VERBOSE:
                Log.v(tag, message);
                break;
            case DEBUG:
                Log.d(tag, message);
                break;
            case INFO:
                Log.i(tag, message);
                break;
            case WARNING:
                if (throwable != null) {
                    Log.w(tag, message, throwable);
                } else {
                    Log.w(tag, message);
                }
                break;
            case ERROR:
                if (throwable != null) {
                    Log.e(tag, message, throwable);
                } else {
                    Log.e(tag, message);
                }
                break;
        }
        
        // Write to internal log file
        if (instance != null && instance.isInitialized.get()) {
            instance.writeToFile(level, tag, message, throwable);
        }
    }
    
    private void writeToFile(LogLevel level, String tag, String message, Throwable throwable) {
        logHandler.post(() -> {
            try {
                String timestamp = dateFormat.format(new Date());
                StringBuilder logEntry = new StringBuilder();
                logEntry.append(timestamp)
                        .append(" [")
                        .append(level.name())
                        .append("] ")
                        .append(tag)
                        .append(": ")
                        .append(message);
                
                if (throwable != null) {
                    logEntry.append("\n").append(getStackTraceString(throwable));
                }
                
                logEntry.append("\n");
                
                appendToFile(logFile, logEntry.toString());
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to write log to file", e);
            }
        });
    }
    
    public void logCrash(Throwable throwable) {
        try {
            String timestamp = dateFormat.format(new Date());
            StringBuilder crashEntry = new StringBuilder();
            crashEntry.append("=== CRASH REPORT ===\n")
                      .append("Time: ")
                      .append(timestamp)
                      .append("\n")
                      .append("Thread: ")
                      .append(Thread.currentThread().getName())
                      .append("\n")
                      .append("Exception: ")
                      .append(throwable.getClass().getName())
                      .append("\n")
                      .append("Message: ")
                      .append(throwable.getMessage())
                      .append("\n")
                      .append("Stack Trace:\n")
                      .append(getStackTraceString(throwable))
                      .append("\n=== END OF CRASH REPORT ===\n\n");
            
            // Write crash synchronously
            appendToFile(crashFile, crashEntry.toString());
            
            // Also write to regular log
            writeToFile(LogLevel.ERROR, TAG, "CRASH DETECTED", throwable);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to log crash", e);
        }
    }
    
    private void appendToFile(File file, String content) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to append to file: " + file.getName(), e);
        }
    }
    
    private String getStackTraceString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    public File getLogFile() {
        return logFile;
    }
    
    public File getCrashFile() {
        return crashFile;
    }
    
    public void clearLogs() {
        logHandler.post(() -> {
            try {
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
    
    public void shutdown() {
        if (logThread != null) {
            logThread.quitSafely();
        }
    }
    
    private enum LogLevel {
        VERBOSE, DEBUG, INFO, WARNING, ERROR
    }
}