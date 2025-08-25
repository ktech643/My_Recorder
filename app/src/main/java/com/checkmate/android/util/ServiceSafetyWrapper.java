package com.checkmate.android.util;

import android.content.Intent;
import android.os.IBinder;

/**
 * Safety wrapper for services that haven't been fully enhanced yet
 * Provides basic error handling and recovery for critical services
 */
public class ServiceSafetyWrapper {
    private static final String TAG = "ServiceSafetyWrapper";
    
    /**
     * Safely execute service onCreate
     */
    public static void safeOnCreate(String serviceName, Runnable createAction) {
        CriticalComponentsMonitor.executeComponentSafely(serviceName, () -> {
            try {
                InternalLogger.d(TAG, serviceName + " onCreate starting");
                createAction.run();
                InternalLogger.d(TAG, serviceName + " onCreate completed");
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error in " + serviceName + " onCreate", e);
                throw e;
            }
        });
    }
    
    /**
     * Safely execute service onStartCommand
     */
    public static int safeOnStartCommand(String serviceName, StartCommandOperation operation, Intent intent, int flags, int startId) {
        return CriticalComponentsMonitor.executeComponentSafely(serviceName, () -> {
            try {
                InternalLogger.d(TAG, serviceName + " onStartCommand starting");
                int result = operation.execute(intent, flags, startId);
                InternalLogger.d(TAG, serviceName + " onStartCommand completed with result: " + result);
                return result;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error in " + serviceName + " onStartCommand", e);
                throw e;
            }
        }, android.app.Service.START_NOT_STICKY);
    }
    
    /**
     * Safely execute service onDestroy
     */
    public static void safeOnDestroy(String serviceName, Runnable destroyAction) {
        CriticalComponentsMonitor.executeComponentSafely(serviceName, () -> {
            try {
                InternalLogger.i(TAG, serviceName + " onDestroy starting");
                destroyAction.run();
                InternalLogger.i(TAG, serviceName + " onDestroy completed");
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error in " + serviceName + " onDestroy", e);
                // Don't rethrow on destroy to prevent cascading failures
            }
        });
    }
    
    /**
     * Safely execute service onBind
     */
    public static IBinder safeOnBind(String serviceName, BindOperation operation, Intent intent) {
        return CriticalComponentsMonitor.executeComponentSafely(serviceName, () -> {
            try {
                InternalLogger.d(TAG, serviceName + " onBind starting");
                IBinder result = operation.execute(intent);
                InternalLogger.d(TAG, serviceName + " onBind completed");
                return result;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error in " + serviceName + " onBind", e);
                throw e;
            }
        }, null);
    }
    
    public interface StartCommandOperation {
        int execute(Intent intent, int flags, int startId) throws Exception;
    }
    
    public interface BindOperation {
        IBinder execute(Intent intent) throws Exception;
    }
}