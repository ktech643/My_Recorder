package com.checkmate.android.util;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Internal crash logging and tracking system
 * Stores logs in app's internal directory with session-based file rotation
 */
public class CrashLogger implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "CrashLogger";
    private static final String LOG_DIR = "crash_logs";
    private static final String LOG_FILE_PREFIX = "crash_log_";
    private static final String LOG_FILE_EXTENSION = ".txt";
    private static final int MAX_LOG_FILES = 10;
    private static final long MAX_LOG_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    
    private static volatile CrashLogger instance;
    private final Thread.UncaughtExceptionHandler defaultHandler;
    private final Context context;
    private final ExecutorService logExecutor;
    private final BlockingQueue<LogEntry> logQueue;
    private final AtomicBoolean isLogging;
    private final SimpleDateFormat dateFormat;
    private File currentLogFile;
    private File logDirectory;
    private final Handler mainHandler;
    
    // ANR detection
    private final ANRWatchdog anrWatchdog;
    
    /**
     * Log entry class
     */
    private static class LogEntry {
        final String tag;
        final String message;
        final Throwable throwable;
        final LogLevel level;
        final long timestamp;
        
        LogEntry(String tag, String message, Throwable throwable, LogLevel level) {
            this.tag = tag;
            this.message = message;
            this.throwable = throwable;
            this.level = level;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * Log levels
     */
    public enum LogLevel {
        VERBOSE("V"),
        DEBUG("D"),
        INFO("I"),
        WARNING("W"),
        ERROR("E"),
        CRASH("C"),
        ANR("A");
        
        private final String shortName;
        
        LogLevel(String shortName) {
            this.shortName = shortName;
        }
        
        public String getShortName() {
            return shortName;
        }
    }
    
    /**
     * Private constructor
     */
    private CrashLogger(Context context) {
        this.context = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        this.logExecutor = Executors.newSingleThreadExecutor();
        this.logQueue = new LinkedBlockingQueue<>();
        this.isLogging = new AtomicBoolean(true);
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        // Initialize ANR watchdog
        this.anrWatchdog = new ANRWatchdog();
        
        initializeLogDirectory();
        startLogProcessor();
        
        // Set as default uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(this);
    }
    
    /**
     * Get singleton instance
     */
    public static CrashLogger getInstance() {
        if (instance == null) {
            synchronized (CrashLogger.class) {
                if (instance == null) {
                    Context context = MyApp.getContext();
                    if (context != null) {
                        instance = new CrashLogger(context);
                    }
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize logging system
     */
    public static void initialize(Context context) {
        if (instance == null) {
            synchronized (CrashLogger.class) {
                if (instance == null) {
                    instance = new CrashLogger(context);
                }
            }
        }
    }
    
    /**
     * Initialize log directory
     */
    private void initializeLogDirectory() {
        logDirectory = new File(context.getFilesDir(), LOG_DIR);
        if (!logDirectory.exists()) {
            logDirectory.mkdirs();
        }
        
        // Create new session log file
        createNewLogFile();
        
        // Clean up old log files
        cleanupOldLogs();
    }
    
    /**
     * Create new log file for current session
     */
    private void createNewLogFile() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = LOG_FILE_PREFIX + timestamp + LOG_FILE_EXTENSION;
        currentLogFile = new File(logDirectory, fileName);
        
        // Write session header
        writeSessionHeader();
    }
    
    /**
     * Write session header information
     */
    private void writeSessionHeader() {
        StringBuilder header = new StringBuilder();
        header.append("=== CheckMate Crash Log ===\n");
        header.append("Session Start: ").append(dateFormat.format(new Date())).append("\n");
        header.append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        header.append("Android Version: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        header.append("App Version: ").append(getAppVersion()).append("\n");
        header.append("===========================\n\n");
        
        writeToFile(header.toString());
    }
    
    /**
     * Get app version
     */
    private String getAppVersion() {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    /**
     * Start log processor thread
     */
    private void startLogProcessor() {
        logExecutor.execute(() -> {
            while (isLogging.get()) {
                try {
                    LogEntry entry = logQueue.poll(1, TimeUnit.SECONDS);
                    if (entry != null) {
                        processLogEntry(entry);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
    
    /**
     * Process log entry
     */
    private void processLogEntry(LogEntry entry) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append(dateFormat.format(new Date(entry.timestamp)));
        logMessage.append(" [").append(entry.level.getShortName()).append("] ");
        logMessage.append(entry.tag).append(": ");
        logMessage.append(entry.message);
        
        if (entry.throwable != null) {
            logMessage.append("\n").append(getStackTraceString(entry.throwable));
        }
        
        logMessage.append("\n");
        
        writeToFile(logMessage.toString());
        
        // Also log to standard Android log
        switch (entry.level) {
            case VERBOSE:
                Log.v(entry.tag, entry.message, entry.throwable);
                break;
            case DEBUG:
                Log.d(entry.tag, entry.message, entry.throwable);
                break;
            case INFO:
                Log.i(entry.tag, entry.message, entry.throwable);
                break;
            case WARNING:
                Log.w(entry.tag, entry.message, entry.throwable);
                break;
            case ERROR:
            case CRASH:
            case ANR:
                Log.e(entry.tag, entry.message, entry.throwable);
                break;
        }
    }
    
    /**
     * Write to log file
     */
    private synchronized void writeToFile(String message) {
        if (currentLogFile == null) return;
        
        try {
            // Check file size and rotate if needed
            if (currentLogFile.length() > MAX_LOG_FILE_SIZE) {
                createNewLogFile();
            }
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentLogFile, true))) {
                writer.write(message);
                writer.flush();
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to log file", e);
        }
    }
    
    /**
     * Clean up old log files
     */
    private void cleanupOldLogs() {
        File[] logFiles = logDirectory.listFiles((dir, name) -> name.startsWith(LOG_FILE_PREFIX));
        if (logFiles != null && logFiles.length > MAX_LOG_FILES) {
            // Sort by last modified date
            java.util.Arrays.sort(logFiles, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
            
            // Delete oldest files
            for (int i = 0; i < logFiles.length - MAX_LOG_FILES; i++) {
                logFiles[i].delete();
            }
        }
    }
    
    /**
     * Get stack trace as string
     */
    private String getStackTraceString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    // Public logging methods
    
    public void v(String tag, String message) {
        log(tag, message, null, LogLevel.VERBOSE);
    }
    
    public void d(String tag, String message) {
        log(tag, message, null, LogLevel.DEBUG);
    }
    
    public void i(String tag, String message) {
        log(tag, message, null, LogLevel.INFO);
    }
    
    public void w(String tag, String message) {
        log(tag, message, null, LogLevel.WARNING);
    }
    
    public void w(String tag, String message, Throwable throwable) {
        log(tag, message, throwable, LogLevel.WARNING);
    }
    
    public void e(String tag, String message) {
        log(tag, message, null, LogLevel.ERROR);
    }
    
    public void e(String tag, String message, Throwable throwable) {
        log(tag, message, throwable, LogLevel.ERROR);
    }
    
    /**
     * Log crash
     */
    public void logCrash(String tag, String message, Throwable throwable) {
        log(tag, message, throwable, LogLevel.CRASH);
    }
    
    /**
     * Log ANR
     */
    public void logANR(String tag, String message, @Nullable Thread thread) {
        StringBuilder anrMessage = new StringBuilder(message);
        if (thread != null) {
            anrMessage.append("\nThread: ").append(thread.getName());
            anrMessage.append("\nStack trace:\n");
            for (StackTraceElement element : thread.getStackTrace()) {
                anrMessage.append("\tat ").append(element.toString()).append("\n");
            }
        }
        log(tag, anrMessage.toString(), null, LogLevel.ANR);
    }
    
    /**
     * General log method
     */
    private void log(String tag, String message, @Nullable Throwable throwable, LogLevel level) {
        if (!isLogging.get()) return;
        
        LogEntry entry = new LogEntry(tag, message, throwable, level);
        try {
            logQueue.offer(entry, 100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // If queue is full, log directly
            processLogEntry(entry);
        }
    }
    
    /**
     * Uncaught exception handler
     */
    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
        logCrash(TAG, "Uncaught exception in thread: " + thread.getName(), throwable);
        
        // Wait for log to be written
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        // Call default handler
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, throwable);
        }
    }
    
    /**
     * Get log directory
     */
    public File getLogDirectory() {
        return logDirectory;
    }
    
    /**
     * Get current log file
     */
    public File getCurrentLogFile() {
        return currentLogFile;
    }
    
    /**
     * Shutdown logger
     */
    public void shutdown() {
        isLogging.set(false);
        anrWatchdog.stopWatching();
        logExecutor.shutdown();
        try {
            logExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logExecutor.shutdownNow();
        }
    }
    
    /**
     * ANR Watchdog class
     */
    private class ANRWatchdog {
        private static final long ANR_TIMEOUT = 5000; // 5 seconds
        private final Handler watchdogHandler;
        private final Runnable watchdogRunnable;
        private volatile boolean isWatching = true;
        
        ANRWatchdog() {
            this.watchdogHandler = new Handler(Looper.getMainLooper());
            this.watchdogRunnable = new Runnable() {
                private final AtomicBoolean isBlocked = new AtomicBoolean(false);
                
                @Override
                public void run() {
                    if (!isWatching) return;
                    
                    // Reset flag
                    isBlocked.set(false);
                    
                    // Post a task to main thread
                    mainHandler.post(() -> isBlocked.set(true));
                    
                    // Check after timeout
                    watchdogHandler.postDelayed(() -> {
                        if (!isBlocked.get() && isWatching) {
                            // Main thread is blocked - ANR detected
                            handleANR();
                        }
                        
                        // Schedule next check
                        if (isWatching) {
                            watchdogHandler.postDelayed(this, ANR_TIMEOUT);
                        }
                    }, ANR_TIMEOUT);
                }
            };
            
            // Start watching
            watchdogHandler.post(watchdogRunnable);
        }
        
        void handleANR() {
            Thread mainThread = Looper.getMainLooper().getThread();
            logANR(TAG, "ANR detected - Main thread blocked for more than " + ANR_TIMEOUT + "ms", mainThread);
            
            // Try to recover
            recoverFromANR();
        }
        
        void recoverFromANR() {
            // Post recovery action to a background thread
            new Thread(() -> {
                try {
                    // Log all thread states
                    logAllThreadStates();
                    
                    // Try to interrupt blocking operations
                    // This is a last resort and may not always work
                    Thread mainThread = Looper.getMainLooper().getThread();
                    if (mainThread != null && mainThread.getState() == Thread.State.BLOCKED) {
                        i(TAG, "Attempting to interrupt blocked main thread");
                        mainThread.interrupt();
                    }
                } catch (Exception e) {
                    e(TAG, "Failed to recover from ANR", e);
                }
            }).start();
        }
        
        void logAllThreadStates() {
            StringBuilder threadInfo = new StringBuilder("Current thread states:\n");
            for (Thread thread : Thread.getAllStackTraces().keySet()) {
                threadInfo.append("Thread: ").append(thread.getName())
                        .append(" State: ").append(thread.getState())
                        .append(" Priority: ").append(thread.getPriority())
                        .append("\n");
            }
            i(TAG, threadInfo.toString());
        }
        
        void stopWatching() {
            isWatching = false;
            watchdogHandler.removeCallbacks(watchdogRunnable);
        }
    }
}