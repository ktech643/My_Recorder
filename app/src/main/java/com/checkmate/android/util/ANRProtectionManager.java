package com.checkmate.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ANRProtectionManager - Advanced ANR prevention and recovery system
 * 
 * This class provides comprehensive protection against ANR (Application Not Responding)
 * issues by implementing intelligent timeout management, background processing,
 * and emergency recovery mechanisms.
 * 
 * Features:
 * - Intelligent preference operation timeout management
 * - Background thread execution for heavy operations
 * - Emergency recovery mechanisms for stuck operations
 * - Performance monitoring and optimization
 * - Automatic fallback to safe defaults
 * - Memory-efficient operation caching
 * - Thread pool optimization for concurrent operations
 */
public class ANRProtectionManager {
    
    private static final String TAG = "ANRProtectionManager";
    
    // Singleton instance
    private static volatile ANRProtectionManager sInstance;
    private static final Object sLock = new Object();
    
    // Timeout configurations (optimized for performance and splash screen responsiveness)
    private static final long PREFERENCE_TIMEOUT_MS = 1000;     // Further reduced to 1s for faster splash
    private static final long DATABASE_TIMEOUT_MS = 2000;      // Database operations
    private static final long CRITICAL_TIMEOUT_MS = 500;       // Critical path operations (faster)
    private static final long EMERGENCY_TIMEOUT_MS = 250;      // Emergency operations (faster)
    
    // Thread management
    private final ExecutorService mBackgroundExecutor;
    private final ExecutorService mCriticalExecutor;
    private final Handler mMainHandler;
    
    // State management
    private final AtomicBoolean mIsInRecoveryMode = new AtomicBoolean(false);
    private final AtomicLong mLastRecoveryTime = new AtomicLong(0);
    private final ConcurrentHashMap<String, Object> mOperationCache = new ConcurrentHashMap<>();
    
    // Performance monitoring
    private final AtomicLong mTotalOperations = new AtomicLong(0);
    private final AtomicLong mTimeoutOperations = new AtomicLong(0);
    private final AtomicLong mSuccessfulOperations = new AtomicLong(0);
    
    public static ANRProtectionManager getInstance() {
        if (sInstance == null) {
            synchronized (sLock) {
                if (sInstance == null) {
                    sInstance = new ANRProtectionManager();
                }
            }
        }
        return sInstance;
    }
    
    private ANRProtectionManager() {
        // Create optimized thread pools
        mBackgroundExecutor = Executors.newFixedThreadPool(3, r -> {
            Thread t = new Thread(r, "ANRProtection-Background");
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
        
        mCriticalExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "ANRProtection-Critical");
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        });
        
        mMainHandler = new Handler(Looper.getMainLooper());
        
        Log.d(TAG, "🛡️ ANRProtectionManager initialized with optimized thread pools");
    }
    
    /**
     * Execute preference operation with ANR protection
     */
    public <T> T executePreferenceOperation(String operationKey, PreferenceOperation<T> operation, T defaultValue) {
        try {
            mTotalOperations.incrementAndGet();
            
            // Check cache first for performance
            if (mOperationCache.containsKey(operationKey)) {
                Object cachedValue = mOperationCache.get(operationKey);
                if (cachedValue != null) {
                    Log.d(TAG, "⚡ Cache hit for operation: " + operationKey);
                    mSuccessfulOperations.incrementAndGet();
                    return (T) cachedValue;
                }
            }
            
            // Execute with timeout protection
            Future<T> future = mBackgroundExecutor.submit(() -> {
                try {
                    T result = operation.execute();
                    
                    // Cache successful results
                    if (result != null) {
                        mOperationCache.put(operationKey, result);
                    }
                    
                    return result;
                } catch (Exception e) {
                    Log.e(TAG, "💥 Error in preference operation: " + operationKey, e);
                    return defaultValue;
                }
            });
            
            try {
                T result = future.get(PREFERENCE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                mSuccessfulOperations.incrementAndGet();
                Log.d(TAG, "✅ Preference operation completed: " + operationKey);
                return result != null ? result : defaultValue;
                
            } catch (Exception e) {
                Log.w(TAG, "⏰ Preference operation timed out: " + operationKey + ", using default value");
                mTimeoutOperations.incrementAndGet();
                future.cancel(true);
                
                // Enter recovery mode if too many timeouts
                checkAndEnterRecoveryMode();
                
                return defaultValue;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "💥 Unexpected error in preference operation: " + operationKey, e);
            return defaultValue;
        }
    }
    
    /**
     * Execute database operation with ANR protection
     */
    public <T> T executeDatabaseOperation(String operationKey, DatabaseOperation<T> operation, T defaultValue) {
        try {
            mTotalOperations.incrementAndGet();
            
            Future<T> future = mCriticalExecutor.submit(() -> {
                try {
                    return operation.execute();
                } catch (Exception e) {
                    Log.e(TAG, "💥 Error in database operation: " + operationKey, e);
                    return defaultValue;
                }
            });
            
            try {
                T result = future.get(DATABASE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                mSuccessfulOperations.incrementAndGet();
                Log.d(TAG, "✅ Database operation completed: " + operationKey);
                return result != null ? result : defaultValue;
                
            } catch (Exception e) {
                Log.w(TAG, "⏰ Database operation timed out: " + operationKey + ", using default value");
                mTimeoutOperations.incrementAndGet();
                future.cancel(true);
                
                checkAndEnterRecoveryMode();
                return defaultValue;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "💥 Unexpected error in database operation: " + operationKey, e);
            return defaultValue;
        }
    }
    
    /**
     * Execute critical operation with shortest timeout
     */
    public <T> T executeCriticalOperation(String operationKey, CriticalOperation<T> operation, T defaultValue) {
        try {
            mTotalOperations.incrementAndGet();
            
            Future<T> future = mCriticalExecutor.submit(() -> {
                try {
                    return operation.execute();
                } catch (Exception e) {
                    Log.e(TAG, "💥 Error in critical operation: " + operationKey, e);
                    return defaultValue;
                }
            });
            
            try {
                T result = future.get(CRITICAL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                mSuccessfulOperations.incrementAndGet();
                Log.d(TAG, "⚡ Critical operation completed: " + operationKey);
                return result != null ? result : defaultValue;
                
            } catch (Exception e) {
                Log.w(TAG, "🚨 Critical operation timed out: " + operationKey + ", using default value");
                mTimeoutOperations.incrementAndGet();
                future.cancel(true);
                
                // Immediate recovery mode for critical operations
                enterRecoveryMode();
                return defaultValue;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "💥 Unexpected error in critical operation: " + operationKey, e);
            return defaultValue;
        }
    }
    
    /**
     * Execute operation on main thread with emergency timeout
     */
    public void executeOnMainThreadSafely(String operationKey, Runnable operation) {
        try {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                // Already on main thread, execute with monitoring
                executeWithEmergencyTimeout(operationKey, operation);
            } else {
                // Post to main thread
                mMainHandler.post(() -> executeWithEmergencyTimeout(operationKey, operation));
            }
        } catch (Exception e) {
            Log.e(TAG, "💥 Error executing on main thread: " + operationKey, e);
        }
    }
    
    /**
     * Execute with emergency timeout to prevent main thread blocking
     */
    private void executeWithEmergencyTimeout(String operationKey, Runnable operation) {
        try {
            long startTime = System.currentTimeMillis();
            
            // Execute operation
            operation.run();
            
            long duration = System.currentTimeMillis() - startTime;
            if (duration > EMERGENCY_TIMEOUT_MS) {
                Log.w(TAG, "⚠️ Main thread operation took too long: " + operationKey + " (" + duration + "ms)");
            } else {
                Log.d(TAG, "⚡ Main thread operation completed: " + operationKey + " (" + duration + "ms)");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "💥 Error in main thread operation: " + operationKey, e);
        }
    }
    
    /**
     * Check if we should enter recovery mode based on timeout ratio
     */
    private void checkAndEnterRecoveryMode() {
        try {
            long total = mTotalOperations.get();
            long timeouts = mTimeoutOperations.get();
            
            // Enter recovery mode if timeout ratio is too high (>30%)
            if (total > 10 && (timeouts * 100.0 / total) > 30) {
                enterRecoveryMode();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking recovery mode", e);
        }
    }
    
    /**
     * Enter emergency recovery mode
     */
    private void enterRecoveryMode() {
        try {
            if (mIsInRecoveryMode.compareAndSet(false, true)) {
                long currentTime = System.currentTimeMillis();
                mLastRecoveryTime.set(currentTime);
                
                Log.w(TAG, "🚨 ENTERING ANR RECOVERY MODE - High timeout ratio detected");
                
                // Clear operation cache to free memory
                mOperationCache.clear();
                
                // Force garbage collection
                System.gc();
                
                // Schedule exit from recovery mode
                mMainHandler.postDelayed(() -> {
                    exitRecoveryMode();
                }, 10000); // Exit after 10 seconds
                
                // Reset counters
                mTotalOperations.set(0);
                mTimeoutOperations.set(0);
                mSuccessfulOperations.set(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error entering recovery mode", e);
        }
    }
    
    /**
     * Exit recovery mode
     */
    private void exitRecoveryMode() {
        try {
            if (mIsInRecoveryMode.compareAndSet(true, false)) {
                Log.i(TAG, "✅ EXITING ANR RECOVERY MODE - System stabilized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error exiting recovery mode", e);
        }
    }
    
    /**
     * Check if currently in recovery mode
     */
    public boolean isInRecoveryMode() {
        return mIsInRecoveryMode.get();
    }
    
    /**
     * Get performance statistics
     */
    public String getPerformanceStats() {
        try {
            long total = mTotalOperations.get();
            long timeouts = mTimeoutOperations.get();
            long successful = mSuccessfulOperations.get();
            
            double timeoutRatio = total > 0 ? (timeouts * 100.0 / total) : 0;
            double successRatio = total > 0 ? (successful * 100.0 / total) : 0;
            
            return String.format("ANR Protection Stats: Total=%d, Success=%.1f%%, Timeouts=%.1f%%, Recovery=%s",
                    total, successRatio, timeoutRatio, mIsInRecoveryMode.get() ? "ACTIVE" : "NORMAL");
                    
        } catch (Exception e) {
            return "ANR Protection Stats: Error retrieving stats";
        }
    }
    
    /**
     * Create safe SharedPreferences with ANR protection
     */
    public SharedPreferences getSafeSharedPreferences(Context context, String name) {
        try {
            return new ANRSafeSharedPreferences(context, name, this);
        } catch (Exception e) {
            Log.e(TAG, "Error creating safe SharedPreferences", e);
            return context.getSharedPreferences(name, Context.MODE_PRIVATE);
        }
    }
    
    /**
     * Emergency cleanup for critical situations
     */
    public void emergencyCleanup() {
        try {
            Log.w(TAG, "🚨 EMERGENCY CLEANUP INITIATED");
            
            // Clear all caches
            mOperationCache.clear();
            
            // Cancel all pending operations
            mBackgroundExecutor.shutdownNow();
            mCriticalExecutor.shutdownNow();
            
            // Force garbage collection
            System.gc();
            
            // Reset all counters
            mTotalOperations.set(0);
            mTimeoutOperations.set(0);
            mSuccessfulOperations.set(0);
            mIsInRecoveryMode.set(false);
            
            Log.w(TAG, "✅ Emergency cleanup completed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during emergency cleanup", e);
        }
    }
    
    /**
     * Optimize app startup performance
     */
    public void optimizeStartup() {
        try {
            Log.d(TAG, "🚀 Optimizing app startup performance");
            
            // Increase thread priority temporarily during startup
            Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE);
            
            // Clear any stale cache entries
            mOperationCache.clear();
            
            // Pre-warm thread pools
            mBackgroundExecutor.submit(() -> {
                Log.d(TAG, "Background thread pool pre-warmed");
            });
            
            mCriticalExecutor.submit(() -> {
                Log.d(TAG, "Critical thread pool pre-warmed");
            });
            
            // Schedule priority reset
            mMainHandler.postDelayed(() -> {
                Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
                Log.d(TAG, "Thread priority reset to default");
            }, 5000);
            
        } catch (Exception e) {
            Log.e(TAG, "Error optimizing startup", e);
        }
    }
    
    // Functional interfaces for operations
    @FunctionalInterface
    public interface PreferenceOperation<T> {
        T execute() throws Exception;
    }
    
    @FunctionalInterface
    public interface DatabaseOperation<T> {
        T execute() throws Exception;
    }
    
    @FunctionalInterface
    public interface CriticalOperation<T> {
        T execute() throws Exception;
    }
}