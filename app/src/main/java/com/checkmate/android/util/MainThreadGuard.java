package com.checkmate.android.util;

import android.os.Looper;
import android.util.Log;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Main Thread Guard to prevent blocking operations on the main thread
 * and provide safeguards against ANR-causing code
 */
public class MainThreadGuard {
    private static final String TAG = "MainThreadGuard";
    private static final long MAIN_THREAD_WARNING_THRESHOLD = 1000; // 1 second
    private static final AtomicBoolean isMonitoring = new AtomicBoolean(false);
    private static final AtomicLong lastMainThreadWarning = new AtomicLong(0);
    
    /**
     * Check if currently running on main thread
     */
    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }
    
    /**
     * Assert that current operation is NOT on main thread
     * Logs warning if it is on main thread
     */
    public static void assertNotMainThread(String operationName) {
        if (isMainThread()) {
            String warning = "WARNING: " + operationName + " is running on main thread - potential ANR risk!";
            Log.w(TAG, warning);
            CrashLogger.logEvent("Main Thread Violation", warning);
            
            // Log stack trace for debugging
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            StringBuilder stackString = new StringBuilder();
            for (int i = 0; i < Math.min(stackTrace.length, 10); i++) {
                stackString.append(stackTrace[i].toString()).append("\n");
            }
            CrashLogger.logEvent("Main Thread Stack", stackString.toString());
        }
    }
    
    /**
     * Assert that current operation IS on main thread
     * Logs warning if it's NOT on main thread
     */
    public static void assertMainThread(String operationName) {
        if (!isMainThread()) {
            String warning = "WARNING: " + operationName + " should run on main thread but is running on: " 
                + Thread.currentThread().getName();
            Log.w(TAG, warning);
            CrashLogger.logEvent("Wrong Thread", warning);
        }
    }
    
    /**
     * Execute a potentially blocking operation with safety checks
     */
    public static <T> T executeWithGuard(String operationName, java.util.concurrent.Callable<T> operation, T fallbackValue) {
        if (isMainThread()) {
            // Warn about main thread usage
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastMainThreadWarning.get() > MAIN_THREAD_WARNING_THRESHOLD) {
                assertNotMainThread(operationName);
                lastMainThreadWarning.set(currentTime);
            }
            
            // Try to execute quickly, but with timeout protection
            try {
                long startTime = System.currentTimeMillis();
                T result = operation.call();
                long duration = System.currentTimeMillis() - startTime;
                
                if (duration > MAIN_THREAD_WARNING_THRESHOLD) {
                    String warning = operationName + " took " + duration + "ms on main thread - potential ANR!";
                    Log.w(TAG, warning);
                    CrashLogger.logEvent("Slow Main Thread Operation", warning);
                }
                
                return result;
                
            } catch (Exception e) {
                Log.e(TAG, "Operation failed on main thread: " + operationName, e);
                CrashLogger.logCrash("MainThreadGuard." + operationName, e);
                return fallbackValue;
            }
        } else {
            // Safe to execute on background thread
            try {
                return operation.call();
            } catch (Exception e) {
                Log.e(TAG, "Operation failed on background thread: " + operationName, e);
                CrashLogger.logCrash("MainThreadGuard." + operationName, e);
                return fallbackValue;
            }
        }
    }
    
    /**
     * Safe wrapper for SharedPreferences operations
     */
    public static class SafeOperations {
        
        /**
         * Safe boolean preference read
         */
        public static boolean getBooleanSafe(String key, boolean defaultValue) {
            return executeWithGuard(
                "getBooleanSafe(" + key + ")",
                () -> com.checkmate.android.AppPreference.getBool(key, defaultValue),
                defaultValue
            );
        }
        
        /**
         * Safe string preference read
         */
        public static String getStringSafe(String key, String defaultValue) {
            return executeWithGuard(
                "getStringSafe(" + key + ")",
                () -> com.checkmate.android.AppPreference.getStr(key, defaultValue),
                defaultValue
            );
        }
        
        /**
         * Safe integer preference read
         */
        public static int getIntegerSafe(String key, int defaultValue) {
            return executeWithGuard(
                "getIntegerSafe(" + key + ")",
                () -> com.checkmate.android.AppPreference.getInt(key, defaultValue),
                defaultValue
            );
        }
        
        /**
         * Safe preference write (non-blocking)
         */
        public static void setPreferenceSafe(String key, Object value) {
            if (isMainThread()) {
                // Use background task manager for main thread writes
                BackgroundTaskManager.getInstance().executeBackground(
                    () -> {
                        if (value instanceof Boolean) {
                            com.checkmate.android.AppPreference.setBool(key, (Boolean) value);
                        } else if (value instanceof String) {
                            com.checkmate.android.AppPreference.setStr(key, (String) value);
                        } else if (value instanceof Integer) {
                            com.checkmate.android.AppPreference.setInt(key, (Integer) value);
                        }
                        return null;
                    },
                    new BackgroundTaskManager.TaskCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            // Preference set successfully
                        }
                        
                        @Override
                        public void onError(Exception error) {
                            Log.e(TAG, "Failed to set preference: " + key, error);
                        }
                        
                        @Override
                        public void onTimeout() {
                            Log.w(TAG, "Timeout setting preference: " + key);
                        }
                    }
                );
            } else {
                // Safe to execute directly on background thread
                executeWithGuard(
                    "setPreferenceSafe(" + key + ")",
                    () -> {
                        if (value instanceof Boolean) {
                            com.checkmate.android.AppPreference.setBool(key, (Boolean) value);
                        } else if (value instanceof String) {
                            com.checkmate.android.AppPreference.setStr(key, (String) value);
                        } else if (value instanceof Integer) {
                            com.checkmate.android.AppPreference.setInt(key, (Integer) value);
                        }
                        return null;
                    },
                    null
                );
            }
        }
    }
    
    /**
     * Monitor main thread blocking and report issues
     */
    public static void startMainThreadMonitoring() {
        if (isMonitoring.compareAndSet(false, true)) {
            Thread monitoringThread = new Thread(() -> {
                Log.d(TAG, "Main thread monitoring started");
                
                while (isMonitoring.get()) {
                    try {
                        Thread.sleep(1000); // Check every second
                        
                        // Check if ANR watchdog is reporting issues
                        ANRWatchdog watchdog = ANRWatchdog.getInstance();
                        if (!watchdog.isMainThreadResponsive()) {
                            Log.w(TAG, "Main thread unresponsive detected by monitoring");
                            CrashLogger.logEvent("Main Thread Monitor", "Unresponsive main thread detected");
                        }
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
                Log.d(TAG, "Main thread monitoring stopped");
            }, "MainThreadMonitor");
            
            monitoringThread.setDaemon(true);
            monitoringThread.setPriority(Thread.MIN_PRIORITY);
            monitoringThread.start();
        }
    }
    
    /**
     * Stop main thread monitoring
     */
    public static void stopMainThreadMonitoring() {
        isMonitoring.set(false);
    }
    
    /**
     * Get monitoring status
     */
    public static String getMonitoringStatus() {
        return String.format(
            "Main Thread Guard Status:\n" +
            "Monitoring Active: %s\n" +
            "Is Main Thread: %s\n" +
            "Last Warning Time: %d",
            isMonitoring.get(),
            isMainThread(),
            lastMainThreadWarning.get()
        );
    }
}