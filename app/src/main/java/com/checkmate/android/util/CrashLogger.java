package com.checkmate.android.util;

import android.content.Context;
import android.util.Log;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Internal crash logging and reporting system
 * Stores logs in app's internal directory with session-based rotation
 */
public class CrashLogger {
    private static final String TAG = "CrashLogger";
    private static final String LOG_FILE_PREFIX = "checkmate_log_";
    private static final String CRASH_FILE_PREFIX = "checkmate_crash_";
    private static final String LOG_EXTENSION = ".txt";
    private static final long MAX_LOG_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int MAX_LOG_FILES = 5;
    
    private static Context appContext;
    private static File logDirectory;
    private static File currentLogFile;
    private static File currentCrashFile;
    private static ExecutorService logExecutor;
    private static SimpleDateFormat dateFormat;
    private static AtomicBoolean isInitialized = new AtomicBoolean(false);
    private static ReentrantLock fileLock = new ReentrantLock();
    private static String sessionId;
    
    /**
     * Initialize the crash logger
     */
    public static void initialize(Context context) {
        if (isInitialized.get()) {
            return;
        }
        
        try {
            appContext = context.getApplicationContext();
            logExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "CrashLogger-Thread");
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            });
            
            dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
            sessionId = dateFormat.format(new Date());
            
            // Create internal log directory
            logDirectory = new File(appContext.getFilesDir(), "crash_logs");
            if (!logDirectory.exists()) {
                logDirectory.mkdirs();
            }
            
            // Create session-specific log files
            currentLogFile = new File(logDirectory, LOG_FILE_PREFIX + sessionId + LOG_EXTENSION);
            currentCrashFile = new File(logDirectory, CRASH_FILE_PREFIX + sessionId + LOG_EXTENSION);
            
            // Clean up old log files
            cleanupOldLogs();
            
            isInitialized.set(true);
            logEvent("System", "CrashLogger initialized for session: " + sessionId);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize CrashLogger", e);
        }
    }
    
    /**
     * Log a crash with stack trace
     */
    public static void logCrash(String context, Throwable throwable) {
        if (!isInitialized.get()) {
            Log.w(TAG, "CrashLogger not initialized, using Android Log");
            Log.e(TAG, "Crash in " + context, throwable);
            return;
        }
        
        logExecutor.execute(() -> {
            try {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
                String crashLog = buildCrashLog(timestamp, context, throwable);
                
                writeToFile(currentCrashFile, crashLog);
                
                // Also log to regular log
                logEvent("CRASH", context + ": " + throwable.getMessage());
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to log crash", e);
            }
        });
    }
    
    /**
     * Log a general event or message
     */
    public static void logEvent(String tag, String message) {
        if (!isInitialized.get()) {
            Log.d(TAG, tag + ": " + message);
            return;
        }
        
        logExecutor.execute(() -> {
            try {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
                String logEntry = String.format("[%s] [%s] %s\n", timestamp, tag, message);
                
                writeToFile(currentLogFile, logEntry);
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to log event", e);
            }
        });
    }
    
    /**
     * Log ANR detection and recovery attempts
     */
    public static void logANR(String context, String details) {
        logEvent("ANR", context + " - " + details);
        
        // Also create a specific ANR crash log
        Exception anrException = new Exception("ANR detected in " + context + ": " + details);
        logCrash("ANR_DETECTION", anrException);
    }
    
    /**
     * Get the current session's log file path
     */
    public static String getCurrentLogPath() {
        return currentLogFile != null ? currentLogFile.getAbsolutePath() : "Not initialized";
    }
    
    /**
     * Get the current session's crash file path
     */
    public static String getCurrentCrashPath() {
        return currentCrashFile != null ? currentCrashFile.getAbsolutePath() : "Not initialized";
    }
    
    /**
     * Write content to file with proper locking
     */
    private static void writeToFile(File file, String content) {
        fileLock.lock();
        try {
            // Check if file size is getting too large
            if (file.exists() && file.length() > MAX_LOG_SIZE) {
                rotateLogFile(file);
            }
            
            try (FileWriter writer = new FileWriter(file, true)) {
                writer.write(content);
                writer.flush();
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to log file: " + file.getName(), e);
        } finally {
            fileLock.unlock();
        }
    }
    
    /**
     * Build detailed crash log entry
     */
    private static String buildCrashLog(String timestamp, String context, Throwable throwable) {
        StringBuilder crashLog = new StringBuilder();
        crashLog.append("==================== CRASH REPORT ====================\n");
        crashLog.append("Timestamp: ").append(timestamp).append("\n");
        crashLog.append("Session ID: ").append(sessionId).append("\n");
        crashLog.append("Context: ").append(context).append("\n");
        crashLog.append("Exception: ").append(throwable.getClass().getSimpleName()).append("\n");
        crashLog.append("Message: ").append(throwable.getMessage()).append("\n");
        crashLog.append("Stack Trace:\n");
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        crashLog.append(sw.toString());
        
        crashLog.append("======================================================\n\n");
        
        return crashLog.toString();
    }
    
    /**
     * Rotate log file when it gets too large
     */
    private static void rotateLogFile(File file) {
        try {
            String fileName = file.getName();
            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
            String extension = fileName.substring(fileName.lastIndexOf('.'));
            
            File rotatedFile = new File(file.getParent(), baseName + "_rotated_" + System.currentTimeMillis() + extension);
            file.renameTo(rotatedFile);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to rotate log file", e);
        }
    }
    
    /**
     * Clean up old log files to prevent storage bloat
     */
    private static void cleanupOldLogs() {
        try {
            File[] logFiles = logDirectory.listFiles((dir, name) -> 
                name.startsWith(LOG_FILE_PREFIX) || name.startsWith(CRASH_FILE_PREFIX));
            
            if (logFiles != null && logFiles.length > MAX_LOG_FILES) {
                // Sort by last modified and delete oldest files
                java.util.Arrays.sort(logFiles, (f1, f2) -> 
                    Long.compare(f1.lastModified(), f2.lastModified()));
                
                for (int i = 0; i < logFiles.length - MAX_LOG_FILES; i++) {
                    if (logFiles[i].delete()) {
                        Log.d(TAG, "Deleted old log file: " + logFiles[i].getName());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to cleanup old logs", e);
        }
    }
    
    /**
     * Get crash statistics for the current session
     */
    public static String getCrashStats() {
        if (!isInitialized.get()) {
            return "CrashLogger not initialized";
        }
        
        try {
            long logSize = currentLogFile.exists() ? currentLogFile.length() : 0;
            long crashSize = currentCrashFile.exists() ? currentCrashFile.length() : 0;
            int logFileCount = logDirectory.listFiles().length;
            
            return String.format(Locale.US, 
                "Session: %s\nLog Size: %d bytes\nCrash Size: %d bytes\nTotal Files: %d", 
                sessionId, logSize, crashSize, logFileCount);
                
        } catch (Exception e) {
            return "Error getting crash stats: " + e.getMessage();
        }
    }
    
    /**
     * Shutdown the crash logger
     */
    public static void shutdown() {
        if (logExecutor != null && !logExecutor.isShutdown()) {
            logEvent("System", "CrashLogger shutting down");
            logExecutor.shutdown();
        }
        isInitialized.set(false);
    }
}