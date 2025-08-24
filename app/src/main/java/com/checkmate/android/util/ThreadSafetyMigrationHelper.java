package com.checkmate.android.util;

import android.util.Log;

/**
 * Helper class to migrate existing code to use thread-safe patterns
 * Provides compatibility methods that automatically use background execution
 */
public class ThreadSafetyMigrationHelper {
    private static final String TAG = "ThreadSafetyMigration";
    
    /**
     * Thread-safe replacement for AppPreference.setBool()
     * Automatically executes in background if called from main thread
     */
    public static void setBoolThreadSafe(String key, boolean value) {
        setBoolThreadSafe(key, value, null);
    }
    
    public static void setBoolThreadSafe(String key, boolean value, Runnable onComplete) {
        if (MainThreadGuard.isMainThread()) {
            // Use background execution for main thread calls
            BackgroundTaskManager.SafePreferences.setBooleanSafe(key, value, onComplete);
        } else {
            // Safe to execute directly
            try {
                com.checkmate.android.AppPreference.setBool(key, value);
                if (onComplete != null) {
                    onComplete.run();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to set boolean preference: " + key, e);
                CrashLogger.logCrash("ThreadSafetyMigration.setBoolThreadSafe", e);
            }
        }
    }
    
    /**
     * Thread-safe replacement for AppPreference.setStr()
     */
    public static void setStrThreadSafe(String key, String value) {
        setStrThreadSafe(key, value, null);
    }
    
    public static void setStrThreadSafe(String key, String value, Runnable onComplete) {
        if (MainThreadGuard.isMainThread()) {
            BackgroundTaskManager.SafePreferences.setStringSafe(key, value, onComplete);
        } else {
            try {
                com.checkmate.android.AppPreference.setStr(key, value);
                if (onComplete != null) {
                    onComplete.run();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to set string preference: " + key, e);
                CrashLogger.logCrash("ThreadSafetyMigration.setStrThreadSafe", e);
            }
        }
    }
    
    /**
     * Thread-safe replacement for AppPreference.setInt()
     */
    public static void setIntThreadSafe(String key, int value) {
        setIntThreadSafe(key, value, null);
    }
    
    public static void setIntThreadSafe(String key, int value, Runnable onComplete) {
        if (MainThreadGuard.isMainThread()) {
            BackgroundTaskManager.SafePreferences.setIntegerSafe(key, value, onComplete);
        } else {
            try {
                com.checkmate.android.AppPreference.setInt(key, value);
                if (onComplete != null) {
                    onComplete.run();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to set integer preference: " + key, e);
                CrashLogger.logCrash("ThreadSafetyMigration.setIntThreadSafe", e);
            }
        }
    }
    
    /**
     * Thread-safe preference getters (these are generally safe but add extra protection)
     */
    public static boolean getBoolThreadSafe(String key, boolean defaultValue) {
        return MainThreadGuard.SafeOperations.getBooleanSafe(key, defaultValue);
    }
    
    public static String getStrThreadSafe(String key, String defaultValue) {
        return MainThreadGuard.SafeOperations.getStringSafe(key, defaultValue);
    }
    
    public static int getIntThreadSafe(String key, int defaultValue) {
        return MainThreadGuard.SafeOperations.getIntegerSafe(key, defaultValue);
    }
    
    /**
     * Safe database operation wrapper
     */
    public static void executeDatabaseOperation(Runnable operation, Runnable onComplete) {
        MainThreadGuard.assertNotMainThread("Database Operation");
        
        BackgroundTaskManager.getInstance().executeIO(
            () -> {
                operation.run();
                return null;
            },
            new BackgroundTaskManager.TaskCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    if (onComplete != null) {
                        BackgroundTaskManager.getInstance().runOnMainThread(onComplete);
                    }
                }
                
                @Override
                public void onError(Exception error) {
                    Log.e(TAG, "Database operation failed", error);
                    CrashLogger.logCrash("DatabaseOperation", error);
                    if (onComplete != null) {
                        BackgroundTaskManager.getInstance().runOnMainThread(onComplete);
                    }
                }
                
                @Override
                public void onTimeout() {
                    Log.w(TAG, "Database operation timed out");
                    CrashLogger.logEvent("Database Timeout", "Database operation exceeded timeout");
                    if (onComplete != null) {
                        BackgroundTaskManager.getInstance().runOnMainThread(onComplete);
                    }
                }
            }
        );
    }
    
    /**
     * Safe file I/O operation wrapper
     */
    public static void executeFileOperation(Runnable operation, Runnable onComplete) {
        MainThreadGuard.assertNotMainThread("File Operation");
        
        BackgroundTaskManager.getInstance().executeIO(
            () -> {
                operation.run();
                return null;
            },
            new BackgroundTaskManager.TaskCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    if (onComplete != null) {
                        BackgroundTaskManager.getInstance().runOnMainThread(onComplete);
                    }
                }
                
                @Override
                public void onError(Exception error) {
                    Log.e(TAG, "File operation failed", error);
                    CrashLogger.logCrash("FileOperation", error);
                    if (onComplete != null) {
                        BackgroundTaskManager.getInstance().runOnMainThread(onComplete);
                    }
                }
                
                @Override
                public void onTimeout() {
                    Log.w(TAG, "File operation timed out");
                    CrashLogger.logEvent("File Timeout", "File operation exceeded timeout");
                    if (onComplete != null) {
                        BackgroundTaskManager.getInstance().runOnMainThread(onComplete);
                    }
                }
            }
        );
    }
    
    /**
     * Migration report - identifies patterns that should be migrated
     */
    public static void reportThreadSafetyIssues() {
        CrashLogger.logEvent("Migration Report", "Thread safety migration helper loaded");
        
        // This could be expanded to scan for specific patterns and report them
        Log.i(TAG, "Thread safety migration helper is active");
        Log.i(TAG, "Use setBoolThreadSafe(), setStrThreadSafe(), setIntThreadSafe() for safe preference writes");
        Log.i(TAG, "Use executeDatabaseOperation() for safe database access");
        Log.i(TAG, "Use executeFileOperation() for safe file I/O");
    }
}