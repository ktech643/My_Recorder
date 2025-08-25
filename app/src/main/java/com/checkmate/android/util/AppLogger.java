package com.checkmate.android.util;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread-safe application logger that writes logs to both Logcat and internal storage
 */
public class AppLogger {
    private static final String TAG = "AppLogger";
    private static final String LOG_FILE_NAME = "app_debug.log";
    private static final long MAX_LOG_SIZE = 10 * 1024 * 1024; // 10MB
    
    private static AppLogger instance;
    private static Context appContext;
    private static Handler logHandler;
    private static HandlerThread logThread;
    private static File logFile;
    private static final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    
    private AppLogger() {}
    
    public static synchronized void init(Context context) {
        if (isInitialized.getAndSet(true)) {
            return;
        }
        
        appContext = context.getApplicationContext();
        
        // Create background thread for logging
        logThread = new HandlerThread("AppLoggerThread");
        logThread.start();
        logHandler = new Handler(logThread.getLooper());
        
        // Initialize log file
        File logDir = new File(appContext.getFilesDir(), "logs");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        logFile = new File(logDir, LOG_FILE_NAME);
        
        // Clear log file for new session
        clearLogFile();
        
        i(TAG, "AppLogger initialized");
    }
    
    private static void clearLogFile() {
        if (logFile != null && logFile.exists()) {
            logFile.delete();
        }
    }
    
    public static void v(String tag, String message) {
        log(Log.VERBOSE, tag, message, null);
    }
    
    public static void v(String tag, String message, Throwable throwable) {
        log(Log.VERBOSE, tag, message, throwable);
    }
    
    public static void d(String tag, String message) {
        log(Log.DEBUG, tag, message, null);
    }
    
    public static void d(String tag, String message, Throwable throwable) {
        log(Log.DEBUG, tag, message, throwable);
    }
    
    public static void i(String tag, String message) {
        log(Log.INFO, tag, message, null);
    }
    
    public static void i(String tag, String message, Throwable throwable) {
        log(Log.INFO, tag, message, throwable);
    }
    
    public static void w(String tag, String message) {
        log(Log.WARN, tag, message, null);
    }
    
    public static void w(String tag, String message, Throwable throwable) {
        log(Log.WARN, tag, message, throwable);
    }
    
    public static void e(String tag, String message) {
        log(Log.ERROR, tag, message, null);
    }
    
    public static void e(String tag, String message, Throwable throwable) {
        log(Log.ERROR, tag, message, throwable);
    }
    
    public static void wtf(String tag, String message) {
        log(Log.ASSERT, tag, message, null);
    }
    
    public static void wtf(String tag, String message, Throwable throwable) {
        log(Log.ASSERT, tag, message, throwable);
    }
    
    private static void log(int priority, String tag, String message, Throwable throwable) {
        // Always log to Logcat
        if (throwable != null) {
            switch (priority) {
                case Log.VERBOSE:
                    Log.v(tag, message, throwable);
                    break;
                case Log.DEBUG:
                    Log.d(tag, message, throwable);
                    break;
                case Log.INFO:
                    Log.i(tag, message, throwable);
                    break;
                case Log.WARN:
                    Log.w(tag, message, throwable);
                    break;
                case Log.ERROR:
                    Log.e(tag, message, throwable);
                    break;
                case Log.ASSERT:
                    Log.wtf(tag, message, throwable);
                    break;
            }
        } else {
            switch (priority) {
                case Log.VERBOSE:
                    Log.v(tag, message);
                    break;
                case Log.DEBUG:
                    Log.d(tag, message);
                    break;
                case Log.INFO:
                    Log.i(tag, message);
                    break;
                case Log.WARN:
                    Log.w(tag, message);
                    break;
                case Log.ERROR:
                    Log.e(tag, message);
                    break;
                case Log.ASSERT:
                    Log.wtf(tag, message);
                    break;
            }
        }
        
        // Write to file if initialized
        if (isInitialized.get() && logHandler != null) {
            final String logEntry = formatLogEntry(priority, tag, message, throwable);
            logHandler.post(() -> writeToFile(logEntry));
        }
    }
    
    private static String formatLogEntry(int priority, String tag, String message, Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        
        // Timestamp
        sb.append(dateFormat.format(new Date()));
        sb.append(" ");
        
        // Priority
        switch (priority) {
            case Log.VERBOSE:
                sb.append("V");
                break;
            case Log.DEBUG:
                sb.append("D");
                break;
            case Log.INFO:
                sb.append("I");
                break;
            case Log.WARN:
                sb.append("W");
                break;
            case Log.ERROR:
                sb.append("E");
                break;
            case Log.ASSERT:
                sb.append("F");
                break;
        }
        sb.append("/");
        
        // Tag and message
        sb.append(tag);
        sb.append(": ");
        sb.append(message);
        
        // Throwable stack trace
        if (throwable != null) {
            sb.append("\n");
            sb.append(Log.getStackTraceString(throwable));
        }
        
        sb.append("\n");
        
        return sb.toString();
    }
    
    private static void writeToFile(String logEntry) {
        if (logFile == null) {
            return;
        }
        
        try {
            // Check file size and rotate if needed
            if (logFile.length() > MAX_LOG_SIZE) {
                rotateLogFile();
            }
            
            // Write log entry
            FileWriter writer = new FileWriter(logFile, true);
            writer.write(logEntry);
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write log to file", e);
        }
    }
    
    private static void rotateLogFile() {
        try {
            // Rename current log file
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
            String timestamp = sdf.format(new Date());
            File rotatedFile = new File(logFile.getParent(), "app_debug_" + timestamp + ".log");
            logFile.renameTo(rotatedFile);
            
            // Create new log file
            logFile = new File(logFile.getParent(), LOG_FILE_NAME);
            
            // Delete old rotated logs (keep only last 5)
            File logDir = logFile.getParentFile();
            File[] files = logDir.listFiles((dir, name) -> 
                name.startsWith("app_debug_") && name.endsWith(".log"));
            
            if (files != null && files.length > 5) {
                java.util.Arrays.sort(files, (f1, f2) -> 
                    Long.compare(f1.lastModified(), f2.lastModified()));
                
                for (int i = 0; i < files.length - 5; i++) {
                    files[i].delete();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to rotate log file", e);
        }
    }
    
    public static void shutdown() {
        if (logThread != null) {
            logThread.quit();
        }
    }
    
    /**
     * Get the current log file for sharing/viewing
     */
    public static File getLogFile() {
        return logFile;
    }
    
    /**
     * Get all log files including rotated ones
     */
    public static File[] getAllLogFiles() {
        if (logFile == null) {
            return new File[0];
        }
        
        File logDir = logFile.getParentFile();
        return logDir.listFiles((dir, name) -> 
            name.endsWith(".log"));
    }
}