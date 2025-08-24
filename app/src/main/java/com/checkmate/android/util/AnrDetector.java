package com.checkmate.android.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ANR (Application Not Responding) Detection Utility
 * Monitors main thread responsiveness and logs potential ANR situations
 */
public class AnrDetector {
    private static final String TAG = "AnrDetector";
    private static final long ANR_THRESHOLD_MS = 5000; // 5 seconds
    private static final long CHECK_INTERVAL_MS = 1000; // Check every 1 second
    
    private static volatile AnrDetector instance;
    private static final Object lock = new Object();
    
    private final Handler mainHandler;
    private final Thread watcherThread;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicLong lastResponseTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicBoolean mainThreadResponded = new AtomicBoolean(true);
    
    private AnrDetector() {
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.watcherThread = new Thread(this::watchMainThread, "ANR-Detector");
        this.watcherThread.setDaemon(true);
        this.watcherThread.setPriority(Thread.MAX_PRIORITY);
    }
    
    /**
     * Get singleton instance
     */
    public static AnrDetector getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new AnrDetector();
                }
            }
        }
        return instance;
    }
    
    /**
     * Start ANR monitoring
     */
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            lastResponseTime.set(System.currentTimeMillis());
            mainThreadResponded.set(true);
            watcherThread.start();
            Log.d(TAG, "ANR detector started");
        }
    }
    
    /**
     * Stop ANR monitoring
     */
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            watcherThread.interrupt();
            Log.d(TAG, "ANR detector stopped");
        }
    }
    
    /**
     * Check if ANR monitoring is running
     */
    public boolean isRunning() {
        return isRunning.get();
    }
    
    /**
     * Main thread monitoring loop
     */
    private void watchMainThread() {
        while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
            try {
                long currentTime = System.currentTimeMillis();
                
                // Post a message to main thread to check responsiveness
                mainThreadResponded.set(false);
                mainHandler.post(() -> {
                    mainThreadResponded.set(true);
                    lastResponseTime.set(System.currentTimeMillis());
                });
                
                // Wait a bit for the main thread to respond
                Thread.sleep(CHECK_INTERVAL_MS);
                
                // Check if main thread responded
                if (!mainThreadResponded.get()) {
                    long anrDuration = currentTime - lastResponseTime.get();
                    if (anrDuration > ANR_THRESHOLD_MS) {
                        onAnrDetected(anrDuration);
                    }
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Log.e(TAG, "Error in ANR detector", e);
            }
        }
    }
    
    /**
     * Called when ANR is detected
     */
    private void onAnrDetected(long duration) {
        String mainThreadStack = getMainThreadStackTrace();
        String message = "Main thread unresponsive for " + duration + "ms\n" + mainThreadStack;
        
        // Log to crash logger
        CrashLogger.logAnr("Main Thread", duration);
        CrashLogger.e(TAG, message);
        
        Log.e(TAG, "ANR detected - Main thread blocked for " + duration + "ms");
    }
    
    /**
     * Get main thread stack trace
     */
    @NonNull
    private String getMainThreadStackTrace() {
        StackTraceElement[] stackTrace = Looper.getMainLooper().getThread().getStackTrace();
        StringBuilder sb = new StringBuilder();
        sb.append("Main thread stack trace:\n");
        
        for (StackTraceElement element : stackTrace) {
            sb.append("  at ").append(element.toString()).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Execute operation with ANR detection
     */
    public static <T> T executeWithAnrDetection(@NonNull String operationName, 
                                               @NonNull AnrOperation<T> operation, 
                                               T defaultValue) {
        long startTime = System.currentTimeMillis();
        
        try {
            T result = operation.execute();
            long duration = System.currentTimeMillis() - startTime;
            
            if (duration > ANR_THRESHOLD_MS) {
                CrashLogger.logAnr(operationName, duration);
                CrashLogger.w(TAG, "Slow operation detected: " + operationName + " took " + duration + "ms");
            }
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            CrashLogger.e(TAG, "Operation failed: " + operationName + " after " + duration + "ms", e);
            return defaultValue;
        }
    }
    
    /**
     * Interface for operations that can be monitored for ANR
     */
    public interface AnrOperation<T> {
        T execute() throws Exception;
    }
    
    /**
     * Utility method to check if current thread is main thread
     */
    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }
    
    /**
     * Post operation to main thread with timeout protection
     */
    public static void postToMainThread(@NonNull Runnable operation) {
        postToMainThread(operation, 0);
    }
    
    /**
     * Post operation to main thread with delay and timeout protection
     */
    public static void postToMainThread(@NonNull Runnable operation, long delayMs) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        
        Runnable wrappedOperation = () -> {
            long startTime = System.currentTimeMillis();
            try {
                operation.run();
                long duration = System.currentTimeMillis() - startTime;
                
                if (duration > ANR_THRESHOLD_MS) {
                    CrashLogger.logAnr("MainThread Operation", duration);
                }
            } catch (Exception e) {
                CrashLogger.e(TAG, "Main thread operation failed", e);
            }
        };
        
        if (delayMs > 0) {
            mainHandler.postDelayed(wrappedOperation, delayMs);
        } else {
            mainHandler.post(wrappedOperation);
        }
    }
}