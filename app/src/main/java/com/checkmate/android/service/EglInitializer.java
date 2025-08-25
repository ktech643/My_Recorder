package com.checkmate.android.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.checkmate.android.service.SharedEGL.ServiceType;
import com.checkmate.android.service.SharedEGL.SharedEglManager;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles early EGL initialization with proper lifecycle management
 * Ensures EGL is ready before any service needs it
 */
public class EglInitializer {
    private static final String TAG = "EglInitializer";
    private static final long INIT_TIMEOUT_MS = 5000; // 5 seconds timeout
    
    private final Context context;
    private final Handler mainHandler;
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private final AtomicBoolean isInitializing = new AtomicBoolean(false);
    private CountDownLatch initLatch;
    
    public interface EglInitCallback {
        void onEglReady();
        void onEglError(String error);
    }
    
    public EglInitializer(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Initialize EGL context with callback
     * @param callback Callback for initialization result
     */
    public void initialize(EglInitCallback callback) {
        if (isInitialized.get()) {
            Log.d(TAG, "EGL already initialized");
            callback.onEglReady();
            return;
        }
        
        if (isInitializing.compareAndSet(false, true)) {
            initLatch = new CountDownLatch(1);
            
            // Initialize on a background thread
            new Thread(() -> {
                try {
                    Log.d(TAG, "Starting EGL initialization...");
                    
                    SharedEglManager eglManager = SharedEglManager.getInstance();
                    
                    // Set up listener for EGL ready callback
                    eglManager.setListener(new SharedEglManager.Listener() {
                        @Override
                        public void onEglReady() {
                            Log.d(TAG, "EGL initialization complete");
                            isInitialized.set(true);
                            isInitializing.set(false);
                            initLatch.countDown();
                            
                            mainHandler.post(() -> callback.onEglReady());
                        }
                    });
                    
                    // Initialize with a default service type
                    eglManager.initialize(context, ServiceType.BgCamera);
                    
                    // Wait for initialization to complete
                    boolean success = initLatch.await(INIT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    
                    if (!success) {
                        throw new RuntimeException("EGL initialization timed out");
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "EGL initialization failed", e);
                    isInitializing.set(false);
                    
                    mainHandler.post(() -> callback.onEglError(e.getMessage()));
                }
            }).start();
        } else {
            Log.d(TAG, "EGL initialization already in progress");
            
            // Wait for ongoing initialization
            new Thread(() -> {
                try {
                    if (initLatch != null && initLatch.await(INIT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                        mainHandler.post(() -> callback.onEglReady());
                    } else {
                        mainHandler.post(() -> callback.onEglError("Initialization in progress timed out"));
                    }
                } catch (InterruptedException e) {
                    mainHandler.post(() -> callback.onEglError("Interrupted while waiting for initialization"));
                }
            }).start();
        }
    }
    
    /**
     * Check if EGL is initialized and ready
     * @return true if EGL is ready
     */
    public boolean isEglReady() {
        return isInitialized.get() && SharedEglManager.getInstance().isEglReady();
    }
    
    /**
     * Wait synchronously for EGL to be ready
     * @param timeoutMs Maximum time to wait in milliseconds
     * @return true if EGL became ready within timeout
     */
    public boolean waitForEgl(long timeoutMs) {
        if (isInitialized.get()) {
            return true;
        }
        
        if (initLatch != null) {
            try {
                return initLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while waiting for EGL", e);
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Force reinitialize EGL (use with caution)
     * @param callback Callback for reinitialization result
     */
    public void reinitialize(EglInitCallback callback) {
        Log.w(TAG, "Force reinitializing EGL");
        
        // Reset state
        isInitialized.set(false);
        isInitializing.set(false);
        
        // Shutdown existing EGL
        SharedEglManager.getInstance().shutdown();
        
        // Wait a bit for cleanup
        mainHandler.postDelayed(() -> initialize(callback), 500);
    }
}