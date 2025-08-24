package com.checkmate.android.util;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility class for safe execution of operations with null checks and ANR prevention
 */
public class SafeExecutor {
    private static final String TAG = "SafeExecutor";
    private static final long DEFAULT_TIMEOUT_MS = 5000; // 5 seconds
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    /**
     * Execute operation safely with null checks
     */
    public static <T> T execute(@NonNull String operationName, 
                               @Nullable SafeOperation<T> operation, 
                               @Nullable T defaultValue) {
        if (operation == null) {
            CrashLogger.w(TAG, "Null operation provided for: " + operationName);
            return defaultValue;
        }
        
        return AnrDetector.executeWithAnrDetection(operationName, () -> {
            try {
                return operation.execute();
            } catch (Exception e) {
                CrashLogger.e(TAG, "Operation failed: " + operationName, e);
                return defaultValue;
            }
        }, defaultValue);
    }
    
    /**
     * Execute operation on main thread safely
     */
    public static <T> T executeOnMainThread(@NonNull String operationName,
                                          @Nullable SafeOperation<T> operation,
                                          @Nullable T defaultValue) {
        return executeOnMainThread(operationName, operation, defaultValue, DEFAULT_TIMEOUT_MS);
    }
    
    /**
     * Execute operation on main thread safely with timeout
     */
    public static <T> T executeOnMainThread(@NonNull String operationName,
                                          @Nullable SafeOperation<T> operation,
                                          @Nullable T defaultValue,
                                          long timeoutMs) {
        if (operation == null) {
            CrashLogger.w(TAG, "Null operation provided for main thread: " + operationName);
            return defaultValue;
        }
        
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // Already on main thread
            return execute(operationName, operation, defaultValue);
        }
        
        // Post to main thread with timeout protection
        AtomicReference<T> result = new AtomicReference<>(defaultValue);
        AtomicReference<Exception> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        mainHandler.post(() -> {
            try {
                T value = operation.execute();
                result.set(value);
            } catch (Exception e) {
                error.set(e);
                CrashLogger.e(TAG, "Main thread operation failed: " + operationName, e);
            } finally {
                latch.countDown();
            }
        });
        
        try {
            if (latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                Exception e = error.get();
                if (e != null) {
                    CrashLogger.e(TAG, "Operation failed on main thread: " + operationName, e);
                    return defaultValue;
                }
                return result.get();
            } else {
                CrashLogger.w(TAG, "Main thread operation timed out: " + operationName);
                return defaultValue;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            CrashLogger.e(TAG, "Main thread operation interrupted: " + operationName, e);
            return defaultValue;
        }
    }
    
    /**
     * Execute runnable safely with null checks
     */
    public static void executeRunnable(@NonNull String operationName, @Nullable Runnable runnable) {
        if (runnable == null) {
            CrashLogger.w(TAG, "Null runnable provided for: " + operationName);
            return;
        }
        
        AnrDetector.executeWithAnrDetection(operationName, () -> {
            try {
                runnable.run();
                return true;
            } catch (Exception e) {
                CrashLogger.e(TAG, "Runnable failed: " + operationName, e);
                return false;
            }
        }, false);
    }
    
    /**
     * Execute runnable on main thread safely
     */
    public static void executeRunnableOnMainThread(@NonNull String operationName, @Nullable Runnable runnable) {
        executeRunnableOnMainThread(operationName, runnable, 0);
    }
    
    /**
     * Execute runnable on main thread safely with delay
     */
    public static void executeRunnableOnMainThread(@NonNull String operationName, 
                                                  @Nullable Runnable runnable, 
                                                  long delayMs) {
        if (runnable == null) {
            CrashLogger.w(TAG, "Null runnable provided for main thread: " + operationName);
            return;
        }
        
        Runnable wrappedRunnable = () -> executeRunnable(operationName, runnable);
        
        if (delayMs > 0) {
            mainHandler.postDelayed(wrappedRunnable, delayMs);
        } else {
            mainHandler.post(wrappedRunnable);
        }
    }
    
    /**
     * Check if object is null and log warning
     */
    public static boolean checkNotNull(@Nullable Object obj, @NonNull String objectName) {
        if (obj == null) {
            CrashLogger.w(TAG, "Null object detected: " + objectName);
            return false;
        }
        return true;
    }
    
    /**
     * Check if object is null and log warning with context
     */
    public static boolean checkNotNull(@Nullable Object obj, @NonNull String objectName, @NonNull String context) {
        if (obj == null) {
            CrashLogger.w(TAG, "Null object detected: " + objectName + " in context: " + context);
            return false;
        }
        return true;
    }
    
    /**
     * Get value with null safety
     */
    @NonNull
    public static <T> T getValueOrDefault(@Nullable T value, @NonNull T defaultValue, @NonNull String valueName) {
        if (value == null) {
            CrashLogger.w(TAG, "Null value for: " + valueName + ", using default");
            return defaultValue;
        }
        return value;
    }
    
    /**
     * Safe interface for operations that may throw exceptions
     */
    public interface SafeOperation<T> {
        T execute() throws Exception;
    }
    
    /**
     * Safe interface for operations that return void and may throw exceptions
     */
    public interface SafeVoidOperation {
        void execute() throws Exception;
    }
    
    /**
     * Execute void operation safely
     */
    public static void executeVoid(@NonNull String operationName, @Nullable SafeVoidOperation operation) {
        if (operation == null) {
            CrashLogger.w(TAG, "Null void operation provided for: " + operationName);
            return;
        }
        
        AnrDetector.executeWithAnrDetection(operationName, () -> {
            try {
                operation.execute();
                return true;
            } catch (Exception e) {
                CrashLogger.e(TAG, "Void operation failed: " + operationName, e);
                return false;
            }
        }, false);
    }
}