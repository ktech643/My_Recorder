package com.checkmate.android.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.checkmate.android.AppPreference;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ANR-Safe Helper utility class for preventing Application Not Responding issues
 * Provides thread-safe operations with timeout mechanisms and recovery strategies
 */
public class ANRSafeHelper {
    private static final String TAG = "ANRSafeHelper";
    private static final int DEFAULT_TIMEOUT_SECONDS = 5;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long ANR_DETECTION_THRESHOLD = 5000; // 5 seconds
    
    private static volatile ANRSafeHelper instance;
    private final ExecutorService backgroundExecutor;
    private final Handler mainHandler;
    private final AtomicBoolean isInRecoveryMode;
    
    private ANRSafeHelper() {
        this.backgroundExecutor = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.isInRecoveryMode = new AtomicBoolean(false);
    }
    
    public static ANRSafeHelper getInstance() {
        if (instance == null) {
            synchronized (ANRSafeHelper.class) {
                if (instance == null) {
                    instance = new ANRSafeHelper();
                }
            }
        }
        return instance;
    }
    
    /**
     * Interface for operations that might cause ANR
     */
    public interface ANRSafeOperation<T> {
        T execute() throws Exception;
    }
    
    /**
     * Interface for operations that need fallback behavior
     */
    public interface FallbackOperation<T> {
        T executeEssential() throws Exception;
        T executeFallback() throws Exception;
    }
    
    /**
     * Execute operation with ANR protection and automatic retry
     */
    public <T> T executeWithANRProtection(ANRSafeOperation<T> operation, T defaultValue) {
        return executeWithANRProtection(operation, defaultValue, DEFAULT_TIMEOUT_SECONDS);
    }
    
    /**
     * Execute operation with ANR protection and custom timeout
     */
    public <T> T executeWithANRProtection(ANRSafeOperation<T> operation, T defaultValue, int timeoutSeconds) {
        if (operation == null) {
            InternalLogger.w(TAG, "Operation is null, returning default value");
            return defaultValue;
        }
        
        // If already on main thread and in recovery mode, use direct fallback
        if (Looper.myLooper() == Looper.getMainLooper() && isInRecoveryMode.get()) {
            InternalLogger.w(TAG, "In recovery mode on main thread, using fallback");
            return defaultValue;
        }
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    // On main thread - use timeout mechanism
                    return executeOnMainThreadWithTimeout(operation, defaultValue, timeoutSeconds);
                } else {
                    // On background thread - execute directly with monitoring
                    long startTime = System.currentTimeMillis();
                    T result = operation.execute();
                    long executionTime = System.currentTimeMillis() - startTime;
                    
                    if (executionTime > ANR_DETECTION_THRESHOLD) {
                        InternalLogger.w(TAG, "Slow operation detected ({}ms), attempt {}", executionTime, attempt);
                    }
                    
                    return result;
                }
            } catch (Exception e) {
                InternalLogger.e(TAG, "Operation failed on attempt " + attempt, e);
                
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    InternalLogger.e(TAG, "All retry attempts failed, using default value");
                    break;
                }
                
                // Brief delay before retry
                try {
                    Thread.sleep(100 * attempt); // Progressive delay
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        return defaultValue;
    }
    
    /**
     * Execute operation on main thread with timeout protection
     */
    private <T> T executeOnMainThreadWithTimeout(ANRSafeOperation<T> operation, T defaultValue, int timeoutSeconds) {
        final AtomicReference<T> result = new AtomicReference<>(defaultValue);
        final AtomicReference<Exception> exception = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        
        Future<?> future = backgroundExecutor.submit(() -> {
            try {
                T value = operation.execute();
                result.set(value);
            } catch (Exception e) {
                exception.set(e);
            } finally {
                latch.countDown();
            }
        });
        
        try {
            if (latch.await(timeoutSeconds, TimeUnit.SECONDS)) {
                Exception ex = exception.get();
                if (ex != null) {
                    InternalLogger.w(TAG, "Operation completed with error", ex);
                    return defaultValue;
                }
                return result.get();
            } else {
                InternalLogger.e(TAG, "Operation timed out after " + timeoutSeconds + " seconds");
                future.cancel(true);
                enterRecoveryMode();
                return defaultValue;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            InternalLogger.e(TAG, "Operation interrupted", e);
            future.cancel(true);
            return defaultValue;
        }
    }
    
    /**
     * Execute with fallback strategy for critical operations
     */
    public <T> T executeWithFallback(FallbackOperation<T> operation, T defaultValue) {
        if (operation == null) {
            InternalLogger.w(TAG, "FallbackOperation is null, returning default value");
            return defaultValue;
        }
        
        try {
            // Try essential operation first
            return executeWithANRProtection(operation::executeEssential, null, 3);
        } catch (Exception e) {
            InternalLogger.w(TAG, "Essential operation failed, trying fallback", e);
            
            try {
                // Try fallback operation
                return executeWithANRProtection(operation::executeFallback, defaultValue, 2);
            } catch (Exception fallbackEx) {
                InternalLogger.e(TAG, "Fallback operation also failed", fallbackEx);
                return defaultValue;
            }
        }
    }
    
    /**
     * Post operation to main thread safely with timeout
     */
    public void postToMainThreadSafely(Runnable operation) {
        postToMainThreadSafely(operation, DEFAULT_TIMEOUT_SECONDS);
    }
    
    /**
     * Post operation to main thread safely with custom timeout
     */
    public void postToMainThreadSafely(Runnable operation, int timeoutSeconds) {
        if (operation == null) {
            InternalLogger.w(TAG, "Runnable is null, ignoring post operation");
            return;
        }
        
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // Already on main thread
            try {
                operation.run();
            } catch (Exception e) {
                InternalLogger.e(TAG, "Operation failed on main thread", e);
            }
            return;
        }
        
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Exception> exception = new AtomicReference<>();
        
        mainHandler.post(() -> {
            try {
                operation.run();
            } catch (Exception e) {
                exception.set(e);
                InternalLogger.e(TAG, "Posted operation failed", e);
            } finally {
                latch.countDown();
            }
        });
        
        try {
            if (!latch.await(timeoutSeconds, TimeUnit.SECONDS)) {
                InternalLogger.e(TAG, "Main thread operation timed out");
                enterRecoveryMode();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            InternalLogger.e(TAG, "Main thread operation interrupted", e);
        }
    }
    
    /**
     * Check if null and log warning with stack trace info
     */
    public static <T> boolean isNullWithLog(T object, String objectName) {
        if (object == null) {
            StackTraceElement caller = Thread.currentThread().getStackTrace()[3];
            InternalLogger.w(TAG, objectName + " is null in " + caller.getClassName() + "." + caller.getMethodName() + ":" + caller.getLineNumber());
            return true;
        }
        return false;
    }
    
    /**
     * Safe null check with default value
     */
    public static <T> T nullSafe(T object, T defaultValue, String objectName) {
        if (object == null) {
            if (objectName != null) {
                StackTraceElement caller = Thread.currentThread().getStackTrace()[3];
                InternalLogger.w(TAG, objectName + " is null, using default in " + caller.getClassName() + "." + caller.getMethodName());
            }
            return defaultValue;
        }
        return object;
    }
    
    /**
     * Enter ANR recovery mode
     */
    private void enterRecoveryMode() {
        if (isInRecoveryMode.compareAndSet(false, true)) {
            InternalLogger.e(TAG, "Entering ANR recovery mode");
            AppPreference.incrementRestartCount();
            
            // Schedule recovery mode exit after 30 seconds
            backgroundExecutor.submit(() -> {
                try {
                    Thread.sleep(30000); // 30 seconds
                    exitRecoveryMode();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }
    
    /**
     * Exit ANR recovery mode
     */
    public void exitRecoveryMode() {
        if (isInRecoveryMode.compareAndSet(true, false)) {
            InternalLogger.i(TAG, "Exiting ANR recovery mode");
        }
    }
    
    /**
     * Check if currently in recovery mode
     */
    public boolean isInRecoveryMode() {
        return isInRecoveryMode.get();
    }
    
    /**
     * Safe context operations with null checking
     */
    public static boolean isContextValid(Context context) {
        if (context == null) {
            InternalLogger.w(TAG, "Context is null");
            return false;
        }
        
        try {
            // Try to access context resources to verify it's valid
            context.getResources();
            return true;
        } catch (Exception e) {
            InternalLogger.w(TAG, "Context is invalid", e);
            return false;
        }
    }
    
    /**
     * Execute operation only if context is valid
     */
    public static void executeIfContextValid(Context context, Runnable operation) {
        if (isContextValid(context) && operation != null) {
            try {
                operation.run();
            } catch (Exception e) {
                InternalLogger.e(TAG, "Context operation failed", e);
            }
        }
    }
    
    /**
     * Cleanup resources when app is shutting down
     */
    public void shutdown() {
        InternalLogger.i(TAG, "Shutting down ANRSafeHelper");
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdown();
            try {
                if (!backgroundExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    backgroundExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                backgroundExecutor.shutdownNow();
            }
        }
    }
}