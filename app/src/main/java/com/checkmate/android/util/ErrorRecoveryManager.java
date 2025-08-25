package com.checkmate.android.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages error recovery and resilience mechanisms
 */
public class ErrorRecoveryManager {
    private static final String TAG = "ErrorRecoveryManager";
    private static ErrorRecoveryManager instance;
    
    private final Context context;
    private final ConcurrentHashMap<String, ErrorContext> errorContexts = new ConcurrentHashMap<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    private static final int MAX_RETRY_COUNT = 3;
    private static final long RETRY_DELAY_BASE = 1000; // 1 second
    
    public interface RecoveryCallback {
        void onRecoverySuccess();
        void onRecoveryFailed(Exception lastError);
    }
    
    public interface RetryableOperation {
        void execute() throws Exception;
    }
    
    private static class ErrorContext {
        final AtomicInteger retryCount = new AtomicInteger(0);
        long lastErrorTime = 0;
        Exception lastException;
        
        boolean shouldRetry() {
            return retryCount.get() < MAX_RETRY_COUNT;
        }
        
        long getRetryDelay() {
            // Exponential backoff
            return RETRY_DELAY_BASE * (long) Math.pow(2, retryCount.get());
        }
        
        void recordError(Exception e) {
            lastErrorTime = System.currentTimeMillis();
            lastException = e;
            retryCount.incrementAndGet();
        }
        
        void reset() {
            retryCount.set(0);
            lastErrorTime = 0;
            lastException = null;
        }
    }
    
    private ErrorRecoveryManager(Context context) {
        this.context = context.getApplicationContext();
    }
    
    public static synchronized ErrorRecoveryManager getInstance(Context context) {
        if (instance == null) {
            instance = new ErrorRecoveryManager(context);
        }
        return instance;
    }
    
    /**
     * Execute an operation with automatic retry on failure
     */
    public void executeWithRetry(String operationId, RetryableOperation operation, RecoveryCallback callback) {
        ErrorContext errorContext = errorContexts.computeIfAbsent(operationId, k -> new ErrorContext());
        
        try {
            operation.execute();
            errorContext.reset();
            if (callback != null) {
                mainHandler.post(callback::onRecoverySuccess);
            }
        } catch (Exception e) {
            handleError(operationId, operation, callback, errorContext, e);
        }
    }
    
    /**
     * Handle error with retry logic
     */
    private void handleError(String operationId, RetryableOperation operation, 
                           RecoveryCallback callback, ErrorContext errorContext, Exception e) {
        Log.e(TAG, "Error in operation: " + operationId, e);
        errorContext.recordError(e);
        
        if (errorContext.shouldRetry()) {
            long delay = errorContext.getRetryDelay();
            Log.w(TAG, "Retrying operation " + operationId + " after " + delay + "ms (attempt " + 
                    errorContext.retryCount.get() + "/" + MAX_RETRY_COUNT + ")");
            
            mainHandler.postDelayed(() -> executeWithRetry(operationId, operation, callback), delay);
        } else {
            Log.e(TAG, "Operation " + operationId + " failed after " + MAX_RETRY_COUNT + " attempts");
            if (callback != null) {
                mainHandler.post(() -> callback.onRecoveryFailed(errorContext.lastException));
            }
        }
    }
    
    /**
     * Try to recover from a specific error
     */
    public void recoverFromError(String component, Exception error, Runnable recoveryAction) {
        Log.w(TAG, "Attempting recovery for component: " + component, error);
        
        mainHandler.post(() -> {
            try {
                // Log error to crash logger
                CrashLogger.getInstance().logError(TAG, "Recovery attempt for " + component, error);
                
                // Execute recovery action
                if (recoveryAction != null) {
                    recoveryAction.run();
                }
                
                Log.i(TAG, "Recovery successful for component: " + component);
            } catch (Exception recoveryError) {
                Log.e(TAG, "Recovery failed for component: " + component, recoveryError);
                CrashLogger.getInstance().logError(TAG, "Recovery failed for " + component, recoveryError);
            }
        });
    }
    
    /**
     * Reset error context for a specific operation
     */
    public void resetErrorContext(String operationId) {
        ErrorContext context = errorContexts.get(operationId);
        if (context != null) {
            context.reset();
        }
    }
    
    /**
     * Get current retry count for an operation
     */
    public int getRetryCount(String operationId) {
        ErrorContext context = errorContexts.get(operationId);
        return context != null ? context.retryCount.get() : 0;
    }
    
    /**
     * Check if operation should be retried
     */
    public boolean shouldRetry(String operationId) {
        ErrorContext context = errorContexts.get(operationId);
        return context != null && context.shouldRetry();
    }
    
    /**
     * Clear all error contexts
     */
    public void clearAll() {
        errorContexts.clear();
    }
}