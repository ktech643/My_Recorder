package com.checkmate.android.util;

import android.content.Context;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Internal logging and crash tracking system for the CheckMate Android app.
 * Provides thread-safe logging, crash detection, and ANR monitoring.
 */
public class InternalLogger {
    private static final String TAG = "InternalLogger";
    private static final String LOG_FILE_NAME = "checkmate_internal.log";
    private static final String CRASH_FILE_NAME = "checkmate_crashes.log";
    private static final long MAX_LOG_SIZE = 5 * 1024 * 1024; // 5MB
    private static final long ANR_THRESHOLD_MS = 5000; // 5 seconds
    
    private static volatile InternalLogger instance;
    private static final Object lock = new Object();
    
    private final Context context;
    private final File logFile;
    private final File crashFile;
    private final ExecutorService executor;
    private final ConcurrentLinkedQueue<LogEntry> logQueue;
    private final AtomicBoolean isLoggingEnabled;
    private final AtomicLong lastMainThreadActivity;
    private final Handler mainHandler;
    private final Runnable anrDetector;
    
    // Log levels
    public enum LogLevel {
        VERBOSE(2), DEBUG(3), INFO(4), WARN(5), ERROR(6), ASSERT(7);
        
        private final int priority;
        LogLevel(int priority) { this.priority = priority; }
        public int getPriority() { return priority; }
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
    
    private InternalLogger(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.logFile = new File(context.getFilesDir(), LOG_FILE_NAME);
        this.crashFile = new File(context.getFilesDir(), CRASH_FILE_NAME);
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "InternalLogger");
            t.setDaemon(true);
            return t;
        });
        this.logQueue = new ConcurrentLinkedQueue<>();
        this.isLoggingEnabled = new AtomicBoolean(true);
        this.lastMainThreadActivity = new AtomicLong(System.currentTimeMillis());
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.anrDetector = this::checkForANR;
        
        setupUncaughtExceptionHandler();
        startANRMonitoring();
        startLogProcessor();
        
        logInfo(TAG, "InternalLogger initialized");
    }
    
    public static void initialize(@NonNull Context context) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new InternalLogger(context);
                }
            }
        }
    }
    
    public static InternalLogger getInstance() {
        if (instance == null) {
            throw new IllegalStateException("InternalLogger not initialized. Call initialize() first.");
        }
        return instance;
    }
    
    // Public logging methods
    public static void logVerbose(@NonNull String tag, @NonNull String message) {
        logVerbose(tag, message, null);
    }
    
    public static void logVerbose(@NonNull String tag, @NonNull String message, @Nullable Throwable throwable) {
        if (instance != null) {
            instance.log(LogLevel.VERBOSE, tag, message, throwable);
        }
    }
    
    public static void logDebug(@NonNull String tag, @NonNull String message) {
        logDebug(tag, message, null);
    }
    
    public static void logDebug(@NonNull String tag, @NonNull String message, @Nullable Throwable throwable) {
        if (instance != null) {
            instance.log(LogLevel.DEBUG, tag, message, throwable);
        }
    }
    
    public static void logInfo(@NonNull String tag, @NonNull String message) {
        logInfo(tag, message, null);
    }
    
    public static void logInfo(@NonNull String tag, @NonNull String message, @Nullable Throwable throwable) {
        if (instance != null) {
            instance.log(LogLevel.INFO, tag, message, throwable);
        }
    }
    
    public static void logWarn(@NonNull String tag, @NonNull String message) {
        logWarn(tag, message, null);
    }
    
    public static void logWarn(@NonNull String tag, @NonNull String message, @Nullable Throwable throwable) {
        if (instance != null) {
            instance.log(LogLevel.WARN, tag, message, throwable);
        }
    }
    
    public static void logError(@NonNull String tag, @NonNull String message) {
        logError(tag, message, null);
    }
    
    public static void logError(@NonNull String tag, @NonNull String message, @Nullable Throwable throwable) {
        if (instance != null) {
            instance.log(LogLevel.ERROR, tag, message, throwable);
        }
    }
    
    public static void logCrash(@NonNull String tag, @NonNull String message, @NonNull Throwable throwable) {
        if (instance != null) {
            instance.logCrash(tag, message, throwable);
        }
    }
    
    public static void updateMainThreadActivity() {
        if (instance != null) {
            instance.lastMainThreadActivity.set(System.currentTimeMillis());
        }
    }
    
    private void log(LogLevel level, String tag, String message, Throwable throwable) {
        if (!isLoggingEnabled.get()) return;
        
        try {
            // Also log to Android's Log system
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
                case ASSERT:
                    Log.e(tag, message, throwable);
                    break;
            }
            
            // Queue for file logging
            logQueue.offer(new LogEntry(level, tag, message, throwable));
            
        } catch (Exception e) {
            // Fallback to simple Android logging if our system fails
            Log.e(TAG, "Failed to log message", e);
        }
    }
    
    private void logCrash(String tag, String message, Throwable throwable) {
        try {
            Log.e(tag, "CRASH: " + message, throwable);
            
            // Write crash to separate file immediately
            executor.execute(() -> writeCrashToFile(tag, message, throwable));
            
            // Also add to regular log queue
            logQueue.offer(new LogEntry(LogLevel.ERROR, tag, "CRASH: " + message, throwable));
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to log crash", e);
        }
    }
    
    private void setupUncaughtExceptionHandler() {
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            try {
                logCrash(TAG, "Uncaught exception in thread: " + thread.getName(), exception);
                
                // Force write any pending logs
                flushLogs();
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to log uncaught exception", e);
            }
            
            // Call the original handler
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, exception);
            }
        });
    }
    
    private void startANRMonitoring() {
        executor.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(ANR_THRESHOLD_MS);
                    
                    long timeSinceLastActivity = System.currentTimeMillis() - lastMainThreadActivity.get();
                    if (timeSinceLastActivity > ANR_THRESHOLD_MS) {
                        // Potential ANR detected
                        logWarn(TAG, "Potential ANR detected. Main thread inactive for " + timeSinceLastActivity + "ms");
                        
                        // Try to detect if main thread is actually blocked
                        detectMainThreadBlock();
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logError(TAG, "Error in ANR monitoring", e);
                }
            }
        });
    }
    
    private void detectMainThreadBlock() {
        final AtomicBoolean responseReceived = new AtomicBoolean(false);
        
        mainHandler.post(() -> {
            responseReceived.set(true);
            updateMainThreadActivity();
        });
        
        // Wait a bit to see if main thread responds
        executor.execute(() -> {
            try {
                Thread.sleep(1000);
                if (!responseReceived.get()) {
                    logError(TAG, "ANR confirmed - Main thread not responding");
                    dumpThreadStacks();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    private void dumpThreadStacks() {
        try {
            StringBuilder stackDump = new StringBuilder();
            stackDump.append("Thread stack dump at ").append(new Date()).append("\n");
            
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            
            for (Thread thread : threads) {
                if (thread != null) {
                    stackDump.append("\nThread: ").append(thread.getName())
                            .append(" (").append(thread.getState()).append(")\n");
                    
                    StackTraceElement[] stackTrace = thread.getStackTrace();
                    for (StackTraceElement element : stackTrace) {
                        stackDump.append("  at ").append(element.toString()).append("\n");
                    }
                }
            }
            
            logError(TAG, "Thread stack dump:\n" + stackDump.toString());
            
        } catch (Exception e) {
            logError(TAG, "Failed to dump thread stacks", e);
        }
    }
    
    private void checkForANR() {
        updateMainThreadActivity();
    }
    
    private void startLogProcessor() {
        executor.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (!logQueue.isEmpty()) {
                        processLogQueue();
                    }
                    Thread.sleep(1000); // Process logs every second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error processing log queue", e);
                }
            }
        });
    }
    
    private void processLogQueue() {
        try {
            if (logFile.length() > MAX_LOG_SIZE) {
                rotateLogFile();
            }
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                LogEntry entry;
                while ((entry = logQueue.poll()) != null) {
                    writeLogEntry(writer, entry);
                }
                writer.flush();
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to log file", e);
        }
    }
    
    private void writeLogEntry(BufferedWriter writer, LogEntry entry) throws IOException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        
        writer.write(dateFormat.format(new Date(entry.timestamp)));
        writer.write(" ");
        writer.write(entry.level.name());
        writer.write("/");
        writer.write(entry.tag);
        writer.write(": ");
        writer.write(entry.message);
        
        if (entry.throwable != null) {
            writer.write("\n");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            entry.throwable.printStackTrace(pw);
            writer.write(sw.toString());
        }
        
        writer.write("\n");
    }
    
    private void writeCrashToFile(String tag, String message, Throwable throwable) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(crashFile, true))) {
                writer.write("\n========== CRASH REPORT ==========\n");
                writer.write("Timestamp: " + dateFormat.format(new Date()) + "\n");
                writer.write("Tag: " + tag + "\n");
                writer.write("Message: " + message + "\n");
                writer.write("Memory (Heap): " + (Debug.getNativeHeapAllocatedSize() / 1024 / 1024) + "MB\n");
                writer.write("Thread: " + Thread.currentThread().getName() + "\n");
                
                if (throwable != null) {
                    writer.write("Exception:\n");
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    throwable.printStackTrace(pw);
                    writer.write(sw.toString());
                }
                
                writer.write("=====================================\n\n");
                writer.flush();
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to write crash to file", e);
        }
    }
    
    private void rotateLogFile() {
        try {
            File backup = new File(context.getFilesDir(), LOG_FILE_NAME + ".old");
            if (backup.exists()) {
                backup.delete();
            }
            logFile.renameTo(backup);
            logFile.createNewFile();
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to rotate log file", e);
        }
    }
    
    private void flushLogs() {
        try {
            processLogQueue();
        } catch (Exception e) {
            Log.e(TAG, "Failed to flush logs", e);
        }
    }
    
    public void setLoggingEnabled(boolean enabled) {
        isLoggingEnabled.set(enabled);
    }
    
    public boolean isLoggingEnabled() {
        return isLoggingEnabled.get();
    }
    
    public File getLogFile() {
        return logFile;
    }
    
    public File getCrashFile() {
        return crashFile;
    }
    
    public void clearLogs() {
        executor.execute(() -> {
            try {
                if (logFile.exists()) {
                    logFile.delete();
                    logFile.createNewFile();
                }
                if (crashFile.exists()) {
                    crashFile.delete();
                    crashFile.createNewFile();
                }
                logQueue.clear();
                logInfo(TAG, "Logs cleared");
            } catch (IOException e) {
                logError(TAG, "Failed to clear logs", e);
            }
        });
    }
    
    public void shutdown() {
        try {
            isLoggingEnabled.set(false);
            flushLogs();
            executor.shutdown();
        } catch (Exception e) {
            Log.e(TAG, "Error during shutdown", e);
        }
    }
}