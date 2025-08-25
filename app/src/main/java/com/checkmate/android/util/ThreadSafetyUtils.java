package com.checkmate.android.util;

import android.os.Handler;
import android.os.Looper;

import com.checkmate.android.logging.InternalLogger;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class for thread-safe operations and preventing ANR
 */
public class ThreadSafetyUtils {
    private static final String TAG = "ThreadSafetyUtils";
    private static final int DEFAULT_TIMEOUT = 5000; // 5 seconds
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final ExecutorService executorService = Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger count = new AtomicInteger(1);
        
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("ThreadSafety-" + count.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    });
    
    /**
     * Check if current thread is the main UI thread
     */
    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }
    
    /**
     * Assert that current thread is the main UI thread
     */
    public static void assertMainThread() {
        if (!isMainThread()) {
            throw new IllegalStateException("This method must be called from the main thread");
        }
    }
    
    /**
     * Assert that current thread is NOT the main UI thread
     */
    public static void assertBackgroundThread() {
        if (isMainThread()) {
            throw new IllegalStateException("This method must not be called from the main thread");
        }
    }
    
    /**
     * Run a task on the main thread
     */
    public static void runOnMainThread(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        
        if (isMainThread()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }
    
    /**
     * Run a task on the main thread with delay
     */
    public static void runOnMainThreadDelayed(Runnable runnable, long delayMillis) {
        if (runnable == null) {
            return;
        }
        
        mainHandler.postDelayed(runnable, delayMillis);
    }
    
    /**
     * Run a task on background thread
     */
    public static void runOnBackgroundThread(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        
        executorService.execute(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error executing background task", e);
            }
        });
    }
    
    /**
     * Run a task on background thread with result
     */
    public static <T> Future<T> runOnBackgroundThread(Callable<T> callable) {
        if (callable == null) {
            return null;
        }
        
        return executorService.submit(() -> {
            try {
                return callable.call();
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error executing background task", e);
                throw e;
            }
        });
    }
    
    /**
     * Execute a task with timeout to prevent ANR
     */
    public static <T> T executeWithTimeout(Callable<T> task, T defaultValue, long timeoutMillis) {
        if (task == null) {
            return defaultValue;
        }
        
        if (!isMainThread()) {
            // Not on main thread, execute directly
            try {
                return task.call();
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error executing task", e);
                return defaultValue;
            }
        }
        
        // On main thread, execute asynchronously
        Future<T> future = executorService.submit(task);
        
        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            InternalLogger.e(TAG, "Task execution failed or timed out", e);
            future.cancel(true);
            return defaultValue;
        }
    }
    
    /**
     * Execute a task with default timeout
     */
    public static <T> T executeWithTimeout(Callable<T> task, T defaultValue) {
        return executeWithTimeout(task, defaultValue, DEFAULT_TIMEOUT);
    }
    
    /**
     * Safe null check helper
     */
    public static <T> T getOrDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
    
    /**
     * Safe string check helper
     */
    public static String getOrEmpty(String value) {
        return value != null ? value : "";
    }
    
    /**
     * Safe boolean check helper
     */
    public static boolean getOrFalse(Boolean value) {
        return value != null ? value : false;
    }
    
    /**
     * Safe integer check helper
     */
    public static int getOrZero(Integer value) {
        return value != null ? value : 0;
    }
    
    /**
     * Safe long check helper
     */
    public static long getOrZeroLong(Long value) {
        return value != null ? value : 0L;
    }
    
    /**
     * Safe float check helper
     */
    public static float getOrZeroFloat(Float value) {
        return value != null ? value : 0.0f;
    }
    
    /**
     * Safe double check helper
     */
    public static double getOrZeroDouble(Double value) {
        return value != null ? value : 0.0;
    }
    
    /**
     * Remove callbacks from main handler
     */
    public static void removeCallbacks(Runnable runnable) {
        if (runnable != null) {
            mainHandler.removeCallbacks(runnable);
        }
    }
    
    /**
     * Shutdown the executor service
     */
    public static void shutdown() {
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}