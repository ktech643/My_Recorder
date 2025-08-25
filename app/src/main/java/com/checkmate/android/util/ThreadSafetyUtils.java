package com.checkmate.android.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.checkmate.android.AppPreference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Comprehensive thread safety utilities for the CheckMate Android app.
 * Provides safe operations for UI updates, preference access, and background tasks.
 */
public class ThreadSafetyUtils {
    private static final String TAG = "ThreadSafetyUtils";
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final ExecutorService backgroundExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "ThreadSafetyUtils-Background");
        t.setDaemon(true);
        return t;
    });
    private static final ReentrantLock operationLock = new ReentrantLock();
    private static final AtomicBoolean isShutdown = new AtomicBoolean(false);
    
    /**
     * Safely execute a runnable, catching and logging any exceptions
     */
    public static void safeExecute(@Nullable Runnable task) {
        if (task != null && !isShutdown.get()) {
            try {
                task.run();
            } catch (Exception e) {
                Log.e(TAG, "Error in safe execution", e);
                if (InternalLogger.getInstance() != null) {
                    InternalLogger.logError(TAG, "Error in safe execution", e);
                }
            }
        }
    }
    
    /**
     * Run task on main thread safely
     */
    public static void runOnMainThread(@NonNull Runnable task) {
        if (isShutdown.get()) {
            Log.w(TAG, "Attempting to run task on shutdown thread safety utils");
            return;
        }
        
        try {
            if (isMainThread()) {
                ANRDetector.reportMainThreadActivity();
                safeExecute(task);
            } else {
                mainHandler.post(() -> {
                    ANRDetector.reportMainThreadActivity();
                    safeExecute(task);
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error posting to main thread", e);
        }
    }
    
    /**
     * Run task on main thread with delay
     */
    public static void runOnMainThreadDelayed(@NonNull Runnable task, long delayMs) {
        if (isShutdown.get()) return;
        
        try {
            mainHandler.postDelayed(() -> {
                ANRDetector.reportMainThreadActivity();
                safeExecute(task);
            }, delayMs);
        } catch (Exception e) {
            Log.e(TAG, "Error posting delayed task to main thread", e);
        }
    }
    
    /**
     * Run task on background thread safely
     */
    public static Future<?> runOnBackgroundThread(@NonNull Runnable task) {
        if (isShutdown.get()) return null;
        
        try {
            return backgroundExecutor.submit(() -> safeExecute(task));
        } catch (Exception e) {
            Log.e(TAG, "Error submitting background task", e);
            return null;
        }
    }
    
    /**
     * Run task with lock protection
     */
    public static void runWithLock(@NonNull Runnable task) {
        if (isShutdown.get()) return;
        
        operationLock.lock();
        try {
            safeExecute(task);
        } finally {
            operationLock.unlock();
        }
    }
    
    /**
     * Check if current thread is main thread
     */
    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }
    
    /**
     * Safe preference operations with null checks
     */
    public static boolean safePutBoolean(@NonNull String key, boolean value) {
        try {
            AppPreference.setBool(key, value);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error setting boolean preference: " + key, e);
            return false;
        }
    }
    
    public static boolean safeGetBoolean(@NonNull String key, boolean defaultValue) {
        try {
            return AppPreference.getBool(key, defaultValue);
        } catch (Exception e) {
            Log.e(TAG, "Error getting boolean preference: " + key, e);
            return defaultValue;
        }
    }
    
    public static boolean safePutString(@NonNull String key, @Nullable String value) {
        try {
            AppPreference.setStr(key, value);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error setting string preference: " + key, e);
            return false;
        }
    }
    
    @Nullable
    public static String safeGetString(@NonNull String key, @Nullable String defaultValue) {
        try {
            return AppPreference.getStr(key, defaultValue);
        } catch (Exception e) {
            Log.e(TAG, "Error getting string preference: " + key, e);
            return defaultValue;
        }
    }
    
    public static boolean safePutInt(@NonNull String key, int value) {
        try {
            AppPreference.setInt(key, value);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error setting int preference: " + key, e);
            return false;
        }
    }
    
    public static int safeGetInt(@NonNull String key, int defaultValue) {
        try {
            return AppPreference.getInt(key, defaultValue);
        } catch (Exception e) {
            Log.e(TAG, "Error getting int preference: " + key, e);
            return defaultValue;
        }
    }
    
    /**
     * Safe null string checks
     */
    public static boolean isNullOrEmpty(@Nullable String str) {
        return str == null || str.trim().isEmpty();
    }
    
    public static String safeString(@Nullable String str, @NonNull String defaultValue) {
        return isNullOrEmpty(str) ? defaultValue : str;
    }
    
    /**
     * Safe view operations
     */
    public static void safeViewOperation(@NonNull Runnable viewOperation) {
        runOnMainThread(() -> {
            try {
                viewOperation.run();
            } catch (Exception e) {
                Log.e(TAG, "Error in safe view operation", e);
            }
        });
    }
    
    /**
     * Safe service operations with timeout
     */
    public static boolean runWithTimeout(@NonNull Runnable task, long timeoutMs) {
        if (isShutdown.get()) return false;
        
        try {
            Future<?> future = backgroundExecutor.submit(task);
            future.get(timeoutMs, TimeUnit.MILLISECONDS);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Task timed out or failed", e);
            return false;
        }
    }
    
    /**
     * Clear all pending main thread tasks
     */
    public static void clearMainThreadTasks() {
        try {
            mainHandler.removeCallbacksAndMessages(null);
        } catch (Exception e) {
            Log.e(TAG, "Error clearing main thread tasks", e);
        }
    }
    
    /**
     * Shutdown thread safety utils
     */
    public static void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            try {
                clearMainThreadTasks();
                backgroundExecutor.shutdown();
                if (!backgroundExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    backgroundExecutor.shutdownNow();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during shutdown", e);
                backgroundExecutor.shutdownNow();
            }
        }
    }
}