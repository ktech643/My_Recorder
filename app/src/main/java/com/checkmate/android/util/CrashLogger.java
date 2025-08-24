package com.checkmate.android.util;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe crash logging and debug log capture system
 * Automatically captures all crashes and debug logs, storing them in app's internal directory
 * Logs are overridden for each session to prevent excessive storage usage
 */
public class CrashLogger {
    private static final String TAG = "CrashLogger";
    private static final String LOG_FILE_NAME = "app_debug.log";
    private static final String CRASH_FILE_NAME = "app_crashes.log";
    private static final int MAX_LOG_SIZE_MB = 10; // 10MB max log file size
    private static final int MAX_LOG_LINES = 10000; // Maximum lines before rotation
    
    private static volatile CrashLogger instance;
    private static final Object lock = new Object();
    
    private final Context context;
    private final File logFile;
    private final File crashFile;
    private final ThreadPoolExecutor logExecutor;
    private final Handler mainHandler;
    private volatile boolean isInitialized = false;
    private int currentLogLines = 0;
    
    // Original uncaught exception handler
    private Thread.UncaughtExceptionHandler originalHandler;
    
    private CrashLogger(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        // Create log directory
        File logDir = new File(this.context.getFilesDir(), "logs");
        if (!logDir.exists()) {
            boolean created = logDir.mkdirs();
            if (!created) {
                Log.e(TAG, "Failed to create log directory");
            }
        }
        
        this.logFile = new File(logDir, LOG_FILE_NAME);
        this.crashFile = new File(logDir, CRASH_FILE_NAME);
        
        // Create thread pool for background logging
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        this.logExecutor = new ThreadPoolExecutor(
            1, 1, 30L, TimeUnit.SECONDS, queue,
            r -> {
                Thread t = new Thread(r, "CrashLogger-Thread");
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            }
        );
        
        setupUncaughtExceptionHandler();
        clearSessionLogs();
        writeSystemInfo();
        this.isInitialized = true;
        
        Log.d(TAG, "CrashLogger initialized successfully");
    }
    
    /**
     * Initialize the crash logger system
     */
    public static void initialize(@NonNull Context context) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new CrashLogger(context);
                }
            }
        }
    }
    
    /**
     * Get the singleton instance
     */
    @Nullable
    public static CrashLogger getInstance() {
        return instance;
    }
    
    /**
     * Log debug message with thread safety
     */
    public static void d(@NonNull String tag, @NonNull String message) {
        logInternal(Log.DEBUG, tag, message, null);
    }
    
    /**
     * Log info message with thread safety
     */
    public static void i(@NonNull String tag, @NonNull String message) {
        logInternal(Log.INFO, tag, message, null);
    }
    
    /**
     * Log warning message with thread safety
     */
    public static void w(@NonNull String tag, @NonNull String message) {
        logInternal(Log.WARN, tag, message, null);
    }
    
    /**
     * Log warning message with throwable
     */
    public static void w(@NonNull String tag, @NonNull String message, @Nullable Throwable throwable) {
        logInternal(Log.WARN, tag, message, throwable);
    }
    
    /**
     * Log error message with thread safety
     */
    public static void e(@NonNull String tag, @NonNull String message) {
        logInternal(Log.ERROR, tag, message, null);
    }
    
    /**
     * Log error message with throwable
     */
    public static void e(@NonNull String tag, @NonNull String message, @Nullable Throwable throwable) {
        logInternal(Log.ERROR, tag, message, throwable);
    }
    
    /**
     * Log ANR detection
     */
    public static void logAnr(@NonNull String context, long duration) {
        String message = "ANR detected in " + context + " - Duration: " + duration + "ms";
        logInternal(Log.ERROR, "ANR_DETECTOR", message, null);
    }
    
    /**
     * Internal logging method with thread safety
     */
    private static void logInternal(int priority, @NonNull String tag, @NonNull String message, @Nullable Throwable throwable) {
        // Always log to Android Log
        if (throwable != null) {
            Log.println(priority, tag, message + "\n" + Log.getStackTraceString(throwable));
        } else {
            Log.println(priority, tag, message);
        }
        
        // Log to file if available
        CrashLogger logger = getInstance();
        if (logger != null && logger.isInitialized) {
            logger.writeToFile(priority, tag, message, throwable, false);
        }
    }
    
    /**
     * Setup uncaught exception handler
     */
    private void setupUncaughtExceptionHandler() {
        originalHandler = Thread.getDefaultUncaughtExceptionHandler();
        
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                logCrash(thread, throwable);
            } catch (Exception e) {
                Log.e(TAG, "Failed to log crash", e);
            }
            
            // Call original handler
            if (originalHandler != null) {
                originalHandler.uncaughtException(thread, throwable);
            }
        });
    }
    
    /**
     * Log crash information
     */
    private void logCrash(@NonNull Thread thread, @NonNull Throwable throwable) {
        String crashInfo = buildCrashInfo(thread, throwable);
        
        // Write to crash file
        writeToFile(Log.ERROR, "CRASH", crashInfo, throwable, true);
        
        Log.e(TAG, "Crash logged: " + throwable.getMessage());
    }
    
    /**
     * Build comprehensive crash information
     */
    @NonNull
    private String buildCrashInfo(@NonNull Thread thread, @NonNull Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== CRASH REPORT ===\n");
        sb.append("Time: ").append(getCurrentTimestamp()).append("\n");
        sb.append("Thread: ").append(thread.getName()).append(" (").append(thread.getId()).append(")\n");
        sb.append("Exception: ").append(throwable.getClass().getSimpleName()).append("\n");
        sb.append("Message: ").append(throwable.getMessage()).append("\n");
        sb.append("Device Info:\n");
        sb.append("  Brand: ").append(Build.BRAND).append("\n");
        sb.append("  Model: ").append(Build.MODEL).append("\n");
        sb.append("  Android: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("  Architecture: ").append(Build.SUPPORTED_ABIS[0]).append("\n");
        sb.append("\nStack Trace:\n");
        sb.append(getStackTraceString(throwable));
        sb.append("\n=== END CRASH REPORT ===\n\n");
        
        return sb.toString();
    }
    
    /**
     * Write system information to log file
     */
    private void writeSystemInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== SESSION START ===\n");
        sb.append("Time: ").append(getCurrentTimestamp()).append("\n");
        sb.append("App Version: ").append(CommonUtil.getVersionCode(context)).append("\n");
        sb.append("Device: ").append(Build.BRAND).append(" ").append(Build.MODEL).append("\n");
        sb.append("Android: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("Architecture: ").append(Build.SUPPORTED_ABIS[0]).append("\n");
        sb.append("Available Memory: ").append(getAvailableMemory()).append(" MB\n");
        sb.append("===================\n\n");
        
        writeToFileSync(sb.toString(), false);
    }
    
    /**
     * Write log entry to file with thread safety
     */
    private void writeToFile(int priority, @NonNull String tag, @NonNull String message, 
                           @Nullable Throwable throwable, boolean isCrash) {
        if (logExecutor.isShutdown()) {
            return;
        }
        
        logExecutor.execute(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append(getCurrentTimestamp()).append(" ");
                sb.append(getPriorityChar(priority)).append("/");
                sb.append(tag).append(": ");
                sb.append(message);
                
                if (throwable != null) {
                    sb.append("\n").append(getStackTraceString(throwable));
                }
                sb.append("\n");
                
                writeToFileSync(sb.toString(), isCrash);
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to write log to file", e);
            }
        });
    }
    
    /**
     * Synchronous file write operation
     */
    private synchronized void writeToFileSync(@NonNull String content, boolean isCrash) {
        File targetFile = isCrash ? crashFile : logFile;
        
        try {
            // Check file size and rotate if needed
            if (!isCrash && targetFile.exists() && 
                (targetFile.length() > MAX_LOG_SIZE_MB * 1024 * 1024 || currentLogLines > MAX_LOG_LINES)) {
                rotateLogFile();
            }
            
            try (FileWriter writer = new FileWriter(targetFile, true)) {
                writer.write(content);
                writer.flush();
            }
            
            if (!isCrash) {
                currentLogLines++;
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to log file: " + targetFile.getName(), e);
        }
    }
    
    /**
     * Clear session logs (called on initialization)
     */
    private void clearSessionLogs() {
        try {
            if (logFile.exists()) {
                boolean deleted = logFile.delete();
                if (!deleted) {
                    Log.w(TAG, "Failed to delete existing log file");
                }
            }
            
            // Keep crash file but add session separator
            if (crashFile.exists()) {
                writeToFileSync("\n=== NEW SESSION ===\n", true);
            }
            
            currentLogLines = 0;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear session logs", e);
        }
    }
    
    /**
     * Rotate log file when it gets too large
     */
    private void rotateLogFile() {
        try {
            File backupFile = new File(logFile.getParent(), LOG_FILE_NAME + ".old");
            if (backupFile.exists()) {
                boolean deleted = backupFile.delete();
                if (!deleted) {
                    Log.w(TAG, "Failed to delete old backup file");
                }
            }
            
            boolean renamed = logFile.renameTo(backupFile);
            if (!renamed) {
                Log.w(TAG, "Failed to rotate log file");
            }
            
            currentLogLines = 0;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to rotate log file", e);
        }
    }
    
    /**
     * Get current timestamp
     */
    @NonNull
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        return sdf.format(new Date());
    }
    
    /**
     * Get priority character for log level
     */
    private char getPriorityChar(int priority) {
        switch (priority) {
            case Log.VERBOSE: return 'V';
            case Log.DEBUG: return 'D';
            case Log.INFO: return 'I';
            case Log.WARN: return 'W';
            case Log.ERROR: return 'E';
            case Log.ASSERT: return 'A';
            default: return '?';
        }
    }
    
    /**
     * Get stack trace as string
     */
    @NonNull
    private String getStackTraceString(@NonNull Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }
    
    /**
     * Get available memory in MB
     */
    private long getAvailableMemory() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory()) / (1024 * 1024);
    }
    
    /**
     * Get crash logs content for debugging
     */
    @Nullable
    public String getCrashLogs() {
        if (!crashFile.exists()) {
            return null;
        }
        
        try {
            StringBuilder sb = new StringBuilder();
            java.util.Scanner scanner = new java.util.Scanner(crashFile);
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine()).append("\n");
            }
            scanner.close();
            return sb.toString();
        } catch (IOException e) {
            Log.e(TAG, "Failed to read crash logs", e);
            return null;
        }
    }
    
    /**
     * Get debug logs content
     */
    @Nullable
    public String getDebugLogs() {
        if (!logFile.exists()) {
            return null;
        }
        
        try {
            StringBuilder sb = new StringBuilder();
            java.util.Scanner scanner = new java.util.Scanner(logFile);
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine()).append("\n");
            }
            scanner.close();
            return sb.toString();
        } catch (IOException e) {
            Log.e(TAG, "Failed to read debug logs", e);
            return null;
        }
    }
    
    /**
     * Get log files directory
     */
    @NonNull
    public File getLogDirectory() {
        return logFile.getParentFile();
    }
    
    /**
     * Cleanup resources
     */
    public void shutdown() {
        if (logExecutor != null && !logExecutor.isShutdown()) {
            logExecutor.shutdown();
            try {
                if (!logExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logExecutor.shutdownNow();
            }
        }
        
        // Restore original exception handler
        if (originalHandler != null) {
            Thread.setDefaultUncaughtExceptionHandler(originalHandler);
        }
        
        isInitialized = false;
        Log.d(TAG, "CrashLogger shutdown completed");
    }
}