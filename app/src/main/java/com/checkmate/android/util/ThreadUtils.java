package com.checkmate.android.util;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread utilities to prevent ANR and ensure thread-safe operations
 */
public class ThreadUtils {
    private static final String TAG = "ThreadUtils";
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final ExecutorService ioExecutor = Executors.newCachedThreadPool();
    private static final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2);
    private static HandlerThread backgroundHandlerThread;
    private static Handler backgroundHandler;
    
    static {
        backgroundHandlerThread = new HandlerThread("BackgroundHandlerThread");
        backgroundHandlerThread.start();
        backgroundHandler = new Handler(backgroundHandlerThread.getLooper());
    }
    
    /**
     * Check if current thread is main thread
     */
    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }
    
    /**
     * Run on main thread
     */
    public static void runOnMainThread(@NonNull Runnable runnable) {
        if (isMainThread()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }
    
    /**
     * Run on main thread with delay
     */
    public static void runOnMainThreadDelayed(@NonNull Runnable runnable, long delayMillis) {
        mainHandler.postDelayed(runnable, delayMillis);
    }
    
    /**
     * Run on background thread
     */
    public static void runOnBackgroundThread(@NonNull Runnable runnable) {
        if (!isMainThread()) {
            runnable.run();
        } else {
            ioExecutor.execute(() -> {
                try {
                    runnable.run();
                } catch (Exception e) {
                    AppLogger.e(TAG, "Error in background thread", e);
                }
            });
        }
    }
    
    /**
     * Run on background thread with callback on main thread
     */
    public static <T> void runAsync(@NonNull Callable<T> task, @NonNull AsyncCallback<T> callback) {
        ioExecutor.execute(() -> {
            try {
                T result = task.call();
                runOnMainThread(() -> callback.onSuccess(result));
            } catch (Exception e) {
                AppLogger.e(TAG, "Error in async task", e);
                runOnMainThread(() -> callback.onError(e));
            }
        });
    }
    
    /**
     * Run with timeout to prevent ANR
     */
    public static void runWithTimeout(@NonNull Runnable runnable, long timeoutMillis, @NonNull Runnable onTimeout) {
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Future<?> future = ioExecutor.submit(() -> {
            try {
                runnable.run();
                completed.set(true);
            } catch (Exception e) {
                AppLogger.e(TAG, "Error in timeout task", e);
            }
        });
        
        scheduledExecutor.schedule(() -> {
            if (!completed.get()) {
                future.cancel(true);
                runOnMainThread(onTimeout);
            }
        }, timeoutMillis, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Schedule periodic task with ANR protection
     */
    public static void schedulePeriodic(@NonNull Runnable runnable, long initialDelay, long period, TimeUnit unit) {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                AppLogger.e(TAG, "Error in periodic task", e);
            }
        }, initialDelay, period, unit);
    }
    
    /**
     * Post to background handler
     */
    public static void postToBackground(@NonNull Runnable runnable) {
        backgroundHandler.post(runnable);
    }
    
    /**
     * Post to background handler with delay
     */
    public static void postToBackgroundDelayed(@NonNull Runnable runnable, long delayMillis) {
        backgroundHandler.postDelayed(runnable, delayMillis);
    }
    
    /**
     * Remove callbacks from main handler
     */
    public static void removeMainCallbacks(@NonNull Runnable runnable) {
        mainHandler.removeCallbacks(runnable);
    }
    
    /**
     * Remove callbacks from background handler
     */
    public static void removeBackgroundCallbacks(@NonNull Runnable runnable) {
        backgroundHandler.removeCallbacks(runnable);
    }
    
    /**
     * Ensure operation runs on main thread or throws
     */
    public static void assertMainThread() {
        if (!isMainThread()) {
            throw new IllegalStateException("This operation must be run on main thread");
        }
    }
    
    /**
     * Ensure operation runs on background thread or throws
     */
    public static void assertBackgroundThread() {
        if (isMainThread()) {
            throw new IllegalStateException("This operation must not be run on main thread");
        }
    }
    
    /**
     * Callback interface for async operations
     */
    public interface AsyncCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }
    
    /**
     * Shutdown all executors (call in Application.onTerminate)
     */
    public static void shutdown() {
        try {
            ioExecutor.shutdown();
            scheduledExecutor.shutdown();
            backgroundHandlerThread.quit();
        } catch (Exception e) {
            AppLogger.e(TAG, "Error shutting down executors", e);
        }
    }
}