package com.checkmate.android.util;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;

import com.checkmate.android.MyApp;
import com.checkmate.android.ThreadSafeAppPreference;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ANR Handler with recovery mechanisms to prevent Application Not Responding issues
 */
public class ANRHandler {
    private static final String TAG = "ANRHandler";
    private static final long DEFAULT_TIMEOUT = 4000; // 4 seconds (below ANR threshold)
    private static final long RECOVERY_DELAY = 500; // 500ms delay before recovery
    
    private static volatile ANRHandler instance;
    private final Context context;
    private final Handler mainHandler;
    private final ExecutorService timeoutExecutor;
    private final ConcurrentHashMap<String, Future<?>> runningTasks;
    private final CrashLogger crashLogger;
    
    // Recovery strategies
    private final RecoveryStrategy[] recoveryStrategies;
    
    /**
     * Recovery strategy interface
     */
    public interface RecoveryStrategy {
        boolean canRecover(Context context);
        void recover(Context context);
        String getName();
    }
    
    /**
     * Task callback interface
     */
    public interface TaskCallback<T> {
        T execute() throws Exception;
    }
    
    /**
     * Error callback interface
     */
    public interface ErrorCallback {
        void onError(Exception e);
    }
    
    /**
     * Private constructor
     */
    private ANRHandler(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.runningTasks = new ConcurrentHashMap<>();
        this.crashLogger = CrashLogger.getInstance();
        
        // Create timeout executor with custom thread factory
        this.timeoutExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
            private final AtomicInteger count = new AtomicInteger(0);
            
            @Override
            public Thread newThread(@NonNull Runnable r) {
                Thread thread = new Thread(r, "ANRHandler-" + count.incrementAndGet());
                thread.setPriority(Thread.MAX_PRIORITY);
                return thread;
            }
        });
        
        // Initialize recovery strategies
        this.recoveryStrategies = new RecoveryStrategy[]{
            new ClearCacheRecovery(),
            new GarbageCollectionRecovery(),
            new ThreadPriorityRecovery(),
            new ReduceMemoryRecovery()
        };
    }
    
    /**
     * Get singleton instance
     */
    public static ANRHandler getInstance() {
        if (instance == null) {
            synchronized (ANRHandler.class) {
                if (instance == null) {
                    Context context = MyApp.getContext();
                    if (context != null) {
                        instance = new ANRHandler(context);
                    }
                }
            }
        }
        return instance;
    }
    
    /**
     * Execute task on main thread with timeout protection
     */
    public <T> void executeOnMainThreadSafe(@NonNull String taskName, 
                                           @NonNull TaskCallback<T> task,
                                           @NonNull SuccessCallback<T> onSuccess,
                                           @NonNull ErrorCallback onError) {
        executeOnMainThreadSafe(taskName, task, onSuccess, onError, DEFAULT_TIMEOUT);
    }
    
    /**
     * Execute task on main thread with custom timeout
     */
    public <T> void executeOnMainThreadSafe(@NonNull String taskName,
                                           @NonNull TaskCallback<T> task,
                                           @NonNull SuccessCallback<T> onSuccess,
                                           @NonNull ErrorCallback onError,
                                           long timeoutMs) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // Already on main thread, execute with protection
            executeWithProtection(taskName, task, onSuccess, onError, timeoutMs);
        } else {
            // Post to main thread with protection
            mainHandler.post(() -> 
                executeWithProtection(taskName, task, onSuccess, onError, timeoutMs)
            );
        }
    }
    
    /**
     * Execute task with timeout protection
     */
    private <T> void executeWithProtection(@NonNull String taskName,
                                          @NonNull TaskCallback<T> task,
                                          @NonNull SuccessCallback<T> onSuccess,
                                          @NonNull ErrorCallback onError,
                                          long timeoutMs) {
        Future<T> future = timeoutExecutor.submit(() -> {
            try {
                return task.execute();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        runningTasks.put(taskName, future);
        
        // Monitor task completion
        timeoutExecutor.execute(() -> {
            try {
                T result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
                runningTasks.remove(taskName);
                
                // Call success on main thread
                mainHandler.post(() -> onSuccess.onSuccess(result));
            } catch (TimeoutException e) {
                // Task timed out - potential ANR
                handleTimeout(taskName, future, onError);
            } catch (Exception e) {
                runningTasks.remove(taskName);
                
                // Call error on main thread
                mainHandler.post(() -> onError.onError(e));
            }
        });
    }
    
    /**
     * Handle timeout situation
     */
    private void handleTimeout(String taskName, Future<?> future, ErrorCallback onError) {
        crashLogger.logANR(TAG, "Task timeout detected: " + taskName, Thread.currentThread());
        
        // Cancel the task
        future.cancel(true);
        runningTasks.remove(taskName);
        
        // Apply recovery strategies
        applyRecoveryStrategies();
        
        // Notify error callback
        Exception timeoutException = new TimeoutException("Task '" + taskName + "' timed out");
        mainHandler.post(() -> onError.onError(timeoutException));
    }
    
    /**
     * Apply recovery strategies
     */
    private void applyRecoveryStrategies() {
        for (RecoveryStrategy strategy : recoveryStrategies) {
            try {
                if (strategy.canRecover(context)) {
                    crashLogger.i(TAG, "Applying recovery strategy: " + strategy.getName());
                    strategy.recover(context);
                }
            } catch (Exception e) {
                crashLogger.e(TAG, "Failed to apply recovery strategy: " + strategy.getName(), e);
            }
        }
    }
    
    /**
     * Execute background task with automatic main thread callback
     */
    public <T> void executeBackgroundTask(@NonNull String taskName,
                                         @NonNull TaskCallback<T> backgroundTask,
                                         @NonNull SuccessCallback<T> onSuccess,
                                         @NonNull ErrorCallback onError) {
        timeoutExecutor.execute(() -> {
            try {
                T result = backgroundTask.execute();
                mainHandler.post(() -> onSuccess.onSuccess(result));
            } catch (Exception e) {
                crashLogger.e(TAG, "Background task failed: " + taskName, e);
                mainHandler.post(() -> onError.onError(e));
            }
        });
    }
    
    /**
     * Check if main thread is responsive
     */
    public void checkMainThreadResponsive(@NonNull Runnable onResponsive, 
                                         @NonNull Runnable onBlocked) {
        final AtomicInteger flag = new AtomicInteger(0);
        
        mainHandler.post(() -> flag.set(1));
        
        timeoutExecutor.execute(() -> {
            try {
                Thread.sleep(100); // Wait 100ms
                
                if (flag.get() == 1) {
                    onResponsive.run();
                } else {
                    onBlocked.run();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    /**
     * Cancel all running tasks
     */
    public void cancelAllTasks() {
        for (Future<?> future : runningTasks.values()) {
            future.cancel(true);
        }
        runningTasks.clear();
    }
    
    /**
     * Shutdown handler
     */
    public void shutdown() {
        cancelAllTasks();
        timeoutExecutor.shutdown();
        try {
            if (!timeoutExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                timeoutExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            timeoutExecutor.shutdownNow();
        }
    }
    
    /**
     * Success callback interface
     */
    public interface SuccessCallback<T> {
        void onSuccess(T result);
    }
    
    // Recovery Strategy Implementations
    
    /**
     * Clear cache recovery strategy
     */
    private static class ClearCacheRecovery implements RecoveryStrategy {
        @Override
        public boolean canRecover(Context context) {
            return true;
        }
        
        @Override
        public void recover(Context context) {
            try {
                // Clear app cache
                File cacheDir = context.getCacheDir();
                if (cacheDir != null && cacheDir.isDirectory()) {
                    deleteDir(cacheDir);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to clear cache", e);
            }
        }
        
        private boolean deleteDir(File dir) {
            if (dir != null && dir.isDirectory()) {
                String[] children = dir.list();
                if (children != null) {
                    for (String child : children) {
                        boolean success = deleteDir(new File(dir, child));
                        if (!success) return false;
                    }
                }
                return dir.delete();
            } else if (dir != null && dir.isFile()) {
                return dir.delete();
            }
            return false;
        }
        
        @Override
        public String getName() {
            return "ClearCache";
        }
    }
    
    /**
     * Garbage collection recovery strategy
     */
    private static class GarbageCollectionRecovery implements RecoveryStrategy {
        @Override
        public boolean canRecover(Context context) {
            return true;
        }
        
        @Override
        public void recover(Context context) {
            System.gc();
            System.runFinalization();
        }
        
        @Override
        public String getName() {
            return "GarbageCollection";
        }
    }
    
    /**
     * Thread priority recovery strategy
     */
    private static class ThreadPriorityRecovery implements RecoveryStrategy {
        @Override
        public boolean canRecover(Context context) {
            return true;
        }
        
        @Override
        public void recover(Context context) {
            try {
                // Boost main thread priority temporarily
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);
                
                // Reset after delay
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
                }, 2000);
            } catch (Exception e) {
                Log.e(TAG, "Failed to adjust thread priority", e);
            }
        }
        
        @Override
        public String getName() {
            return "ThreadPriority";
        }
    }
    
    /**
     * Reduce memory usage recovery strategy
     */
    private static class ReduceMemoryRecovery implements RecoveryStrategy {
        @Override
        public boolean canRecover(Context context) {
            // Only apply if memory is low
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memoryInfo);
                return memoryInfo.lowMemory;
            }
            return false;
        }
        
        @Override
        public void recover(Context context) {
            try {
                // Clear preference cache
                ThreadSafeAppPreference.getInstance().clearCache();
                
                // Trim memory
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    if (context instanceof MyApp) {
                        ((MyApp) context).onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL);
                    }
                }
                
                // Force garbage collection
                System.gc();
            } catch (Exception e) {
                Log.e(TAG, "Failed to reduce memory usage", e);
            }
        }
        
        @Override
        public String getName() {
            return "ReduceMemory";
        }
    }
    
    // Import required for memory recovery
    private static class ComponentCallbacks2 {
        static final int TRIM_MEMORY_RUNNING_CRITICAL = 15;
    }
    
    // Import File class
    private static class File {
        private final java.io.File file;
        
        File(java.io.File dir, String child) {
            this.file = new java.io.File(dir, child);
        }
        
        boolean isDirectory() {
            return file.isDirectory();
        }
        
        boolean isFile() {
            return file.isFile();
        }
        
        String[] list() {
            return file.list();
        }
        
        boolean delete() {
            return file.delete();
        }
    }
}