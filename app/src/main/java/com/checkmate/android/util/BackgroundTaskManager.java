package com.checkmate.android.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Background Task Manager to prevent ANRs by handling operations off the main thread
 * Provides fallback mechanisms and timeout handling for long-running operations
 */
public class BackgroundTaskManager {
    private static final String TAG = "BackgroundTaskManager";
    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 4;
    private static final long TASK_TIMEOUT_MS = 30000; // 30 seconds
    
    private static BackgroundTaskManager instance;
    private final ExecutorService backgroundExecutor;
    private final ExecutorService ioExecutor;
    private final Handler mainHandler;
    private final AtomicInteger taskCounter = new AtomicInteger(0);
    
    public interface BackgroundTask<T> {
        T execute() throws Exception;
    }
    
    public interface TaskCallback<T> {
        void onSuccess(T result);
        void onError(Exception error);
        void onTimeout();
    }
    
    private BackgroundTaskManager() {
        // Background executor for general background tasks
        backgroundExecutor = Executors.newFixedThreadPool(CORE_POOL_SIZE, r -> {
            Thread t = new Thread(r, "Background-Task-" + taskCounter.incrementAndGet());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
        
        // IO executor for file/database operations
        ioExecutor = Executors.newFixedThreadPool(MAX_POOL_SIZE, r -> {
            Thread t = new Thread(r, "IO-Task-" + taskCounter.incrementAndGet());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 2);
            return t;
        });
        
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Get the singleton instance
     */
    public static synchronized BackgroundTaskManager getInstance() {
        if (instance == null) {
            instance = new BackgroundTaskManager();
        }
        return instance;
    }
    
    /**
     * Execute a background task with callback
     */
    public <T> void executeBackground(BackgroundTask<T> task, TaskCallback<T> callback) {
        executeBackground(task, callback, TASK_TIMEOUT_MS);
    }
    
    /**
     * Execute a background task with custom timeout
     */
    public <T> void executeBackground(BackgroundTask<T> task, TaskCallback<T> callback, long timeoutMs) {
        Future<?> future = backgroundExecutor.submit(() -> {
            try {
                T result = task.execute();
                runOnMainThread(() -> {
                    if (callback != null) {
                        callback.onSuccess(result);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Background task failed", e);
                CrashLogger.logCrash("BackgroundTask", e);
                runOnMainThread(() -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
            }
        });
        
        // Handle timeout
        if (timeoutMs > 0) {
            backgroundExecutor.submit(() -> {
                try {
                    Thread.sleep(timeoutMs);
                    if (!future.isDone()) {
                        future.cancel(true);
                        Log.w(TAG, "Background task timed out after " + timeoutMs + "ms");
                        CrashLogger.logEvent("Task Timeout", "Background task exceeded " + timeoutMs + "ms");
                        runOnMainThread(() -> {
                            if (callback != null) {
                                callback.onTimeout();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }
    
    /**
     * Execute an IO task (file/database operations)
     */
    public <T> void executeIO(BackgroundTask<T> task, TaskCallback<T> callback) {
        executeIO(task, callback, TASK_TIMEOUT_MS);
    }
    
    /**
     * Execute an IO task with custom timeout
     */
    public <T> void executeIO(BackgroundTask<T> task, TaskCallback<T> callback, long timeoutMs) {
        Future<?> future = ioExecutor.submit(() -> {
            try {
                T result = task.execute();
                runOnMainThread(() -> {
                    if (callback != null) {
                        callback.onSuccess(result);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "IO task failed", e);
                CrashLogger.logCrash("IOTask", e);
                runOnMainThread(() -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
            }
        });
        
        // Handle timeout
        if (timeoutMs > 0) {
            ioExecutor.submit(() -> {
                try {
                    Thread.sleep(timeoutMs);
                    if (!future.isDone()) {
                        future.cancel(true);
                        Log.w(TAG, "IO task timed out after " + timeoutMs + "ms");
                        CrashLogger.logEvent("IO Timeout", "IO task exceeded " + timeoutMs + "ms");
                        runOnMainThread(() -> {
                            if (callback != null) {
                                callback.onTimeout();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }
    
    /**
     * Execute a task with automatic fallback to synchronous execution if needed
     */
    public <T> T executeWithFallback(BackgroundTask<T> task, T fallbackValue) {
        return executeWithFallback(task, fallbackValue, 5000); // 5 second timeout
    }
    
    /**
     * Execute a task with automatic fallback and custom timeout
     */
    public <T> T executeWithFallback(BackgroundTask<T> task, T fallbackValue, long timeoutMs) {
        try {
            Future<T> future = backgroundExecutor.submit(() -> {
                try {
                    return task.execute();
                } catch (Exception e) {
                    Log.e(TAG, "Task execution failed", e);
                    CrashLogger.logCrash("FallbackTask", e);
                    return fallbackValue;
                }
            });
            
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
            
        } catch (Exception e) {
            Log.w(TAG, "Task failed or timed out, using fallback value", e);
            CrashLogger.logEvent("Fallback Used", "Task failed, using fallback: " + e.getMessage());
            return fallbackValue;
        }
    }
    
    /**
     * Run a runnable on the main thread
     */
    public void runOnMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }
    
    /**
     * Run a runnable on the main thread with delay
     */
    public void runOnMainThreadDelayed(Runnable runnable, long delayMs) {
        mainHandler.postDelayed(runnable, delayMs);
    }
    
    /**
     * Safe preference operations to prevent ANRs
     */
    public static class SafePreferences {
        
        /**
         * Set boolean preference safely in background
         */
        public static void setBooleanSafe(String key, boolean value, Runnable onComplete) {
            getInstance().executeBackground(
                () -> {
                    com.checkmate.android.AppPreference.setBool(key, value);
                    return null;
                },
                new TaskCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (onComplete != null) onComplete.run();
                    }
                    
                    @Override
                    public void onError(Exception error) {
                        Log.e(TAG, "Failed to set boolean preference: " + key, error);
                        if (onComplete != null) onComplete.run();
                    }
                    
                    @Override
                    public void onTimeout() {
                        Log.w(TAG, "Timeout setting boolean preference: " + key);
                        if (onComplete != null) onComplete.run();
                    }
                }
            );
        }
        
        /**
         * Set string preference safely in background
         */
        public static void setStringSafe(String key, String value, Runnable onComplete) {
            getInstance().executeBackground(
                () -> {
                    com.checkmate.android.AppPreference.setStr(key, value);
                    return null;
                },
                new TaskCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (onComplete != null) onComplete.run();
                    }
                    
                    @Override
                    public void onError(Exception error) {
                        Log.e(TAG, "Failed to set string preference: " + key, error);
                        if (onComplete != null) onComplete.run();
                    }
                    
                    @Override
                    public void onTimeout() {
                        Log.w(TAG, "Timeout setting string preference: " + key);
                        if (onComplete != null) onComplete.run();
                    }
                }
            );
        }
        
        /**
         * Set integer preference safely in background
         */
        public static void setIntegerSafe(String key, int value, Runnable onComplete) {
            getInstance().executeBackground(
                () -> {
                    com.checkmate.android.AppPreference.setInt(key, value);
                    return null;
                },
                new TaskCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (onComplete != null) onComplete.run();
                    }
                    
                    @Override
                    public void onError(Exception error) {
                        Log.e(TAG, "Failed to set integer preference: " + key, error);
                        if (onComplete != null) onComplete.run();
                    }
                    
                    @Override
                    public void onTimeout() {
                        Log.w(TAG, "Timeout setting integer preference: " + key);
                        if (onComplete != null) onComplete.run();
                    }
                }
            );
        }
    }
    
    /**
     * Get task execution statistics
     */
    public String getStats() {
        return String.format(
            "Background Task Manager Stats:\n" +
            "Background Executor Active: %d\n" +
            "IO Executor Active: %d\n" +
            "Total Tasks Created: %d",
            ((java.util.concurrent.ThreadPoolExecutor) backgroundExecutor).getActiveCount(),
            ((java.util.concurrent.ThreadPoolExecutor) ioExecutor).getActiveCount(),
            taskCounter.get()
        );
    }
    
    /**
     * Shutdown the task manager
     */
    public void shutdown() {
        try {
            backgroundExecutor.shutdown();
            ioExecutor.shutdown();
            
            if (!backgroundExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                backgroundExecutor.shutdownNow();
            }
            if (!ioExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                ioExecutor.shutdownNow();
            }
            
            CrashLogger.logEvent("BackgroundTaskManager", "Shutdown completed");
        } catch (InterruptedException e) {
            backgroundExecutor.shutdownNow();
            ioExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}