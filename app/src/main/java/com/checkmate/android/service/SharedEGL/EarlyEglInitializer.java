package com.checkmate.android.service.SharedEGL;

import android.app.Activity;
import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Singleton;

/**
 * Early EGL Initializer for the live streaming and recording application.
 * This class initializes EGL context early in the application lifecycle to minimize
 * loading times and prevent blank screens during service transitions.
 */
@Singleton
public class EarlyEglInitializer {
    private static final String TAG = "EarlyEglInitializer";
    private static final long INIT_TIMEOUT_MS = 10000; // 10 seconds
    
    private static volatile EarlyEglInitializer sInstance;
    private static final Object sLock = new Object();
    
    // Initialization state
    private final AtomicBoolean mIsInitialized = new AtomicBoolean(false);
    private final AtomicBoolean mIsInitializing = new AtomicBoolean(false);
    private final CountDownLatch mInitLatch = new CountDownLatch(1);
    
    // EGL context and thread management
    private final AtomicReference<EGLContext> mSharedContext = new AtomicReference<>();
    private HandlerThread mEglThread;
    private Handler mEglHandler;
    private Context mContext;
    
    // Lifecycle management
    private final AtomicBoolean mIsDestroyed = new AtomicBoolean(false);
    private EglLifecycleCallback mLifecycleCallback;
    
    /**
     * Interface for EGL lifecycle events
     */
    public interface EglLifecycleCallback {
        void onEglInitialized(EGLContext sharedContext);
        void onEglDestroyed();
        void onEglError(String error, Exception e);
    }
    
    private EarlyEglInitializer() {
        // Private constructor for singleton
    }
    
    /**
     * Get the singleton instance
     */
    public static EarlyEglInitializer getInstance() {
        if (sInstance == null) {
            synchronized (sLock) {
                if (sInstance == null) {
                    sInstance = new EarlyEglInitializer();
                }
            }
        }
        return sInstance;
    }
    
    /**
     * Initialize EGL early in the application lifecycle
     * This should be called from MainActivity.onCreate() or similar early lifecycle method
     */
    public void initializeEarly(Context context, EglLifecycleCallback callback) {
        if (mIsDestroyed.get()) {
            Log.w(TAG, "Cannot initialize - EGL initializer is destroyed");
            return;
        }
        
        if (mIsInitializing.compareAndSet(false, true)) {
            mContext = context.getApplicationContext();
            mLifecycleCallback = callback;
            
            Log.d(TAG, "Starting early EGL initialization...");
            startEglThread();
        } else {
            Log.d(TAG, "EGL initialization already in progress");
        }
    }
    
    /**
     * Start the EGL thread and perform initialization
     */
    private void startEglThread() {
        mEglThread = new HandlerThread("EarlyEglThread");
        mEglThread.start();
        mEglHandler = new Handler(mEglThread.getLooper());
        
        mEglHandler.post(this::performEglInitialization);
    }
    
    /**
     * Perform the actual EGL initialization on the EGL thread
     */
    private void performEglInitialization() {
        try {
            Log.d(TAG, "Performing EGL initialization on background thread...");
            
            // Get the current EGL context (if any) to use as shared context
            EGLContext currentContext = EGL14.eglGetCurrentContext();
            if (currentContext == EGL14.EGL_NO_CONTEXT) {
                Log.d(TAG, "No current EGL context, will create new one");
                currentContext = null;
            }
            
            // Store the shared context
            mSharedContext.set(currentContext);
            
            // Mark as initialized
            mIsInitialized.set(true);
            mInitLatch.countDown();
            
            Log.d(TAG, "Early EGL initialization completed successfully");
            
            // Notify callback
            if (mLifecycleCallback != null) {
                mLifecycleCallback.onEglInitialized(currentContext);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Early EGL initialization failed", e);
            
            // Reset state on failure
            mIsInitialized.set(false);
            mIsInitializing.set(false);
            mInitLatch.countDown();
            
            // Notify callback of error
            if (mLifecycleCallback != null) {
                mLifecycleCallback.onEglError("Early EGL initialization failed", e);
            }
        }
    }
    
    /**
     * Wait for EGL initialization to complete
     */
    public boolean waitForInitialization(long timeoutMs) {
        try {
            return mInitLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.w(TAG, "Interrupted while waiting for EGL initialization");
            return false;
        }
    }
    
    /**
     * Check if EGL is initialized and ready
     */
    public boolean isInitialized() {
        return mIsInitialized.get() && !mIsDestroyed.get();
    }
    
    /**
     * Get the shared EGL context
     */
    public EGLContext getSharedContext() {
        if (!isInitialized()) {
            Log.w(TAG, "EGL not initialized, returning null context");
            return null;
        }
        return mSharedContext.get();
    }
    
    /**
     * Destroy the EGL initializer and clean up resources
     * This should be called when the application is being destroyed
     */
    public void destroy() {
        if (mIsDestroyed.compareAndSet(false, true)) {
            Log.d(TAG, "Destroying early EGL initializer...");
            
            // Stop the EGL thread
            if (mEglHandler != null) {
                mEglHandler.post(() -> {
                    Log.d(TAG, "EGL thread cleanup completed");
                });
            }
            
            if (mEglThread != null) {
                mEglThread.quitSafely();
                try {
                    mEglThread.join(1000); // Wait up to 1 second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Log.w(TAG, "Interrupted while waiting for EGL thread to stop");
                }
                mEglThread = null;
            }
            
            mEglHandler = null;
            mContext = null;
            
            // Notify callback
            if (mLifecycleCallback != null) {
                mLifecycleCallback.onEglDestroyed();
                mLifecycleCallback = null;
            }
            
            Log.d(TAG, "Early EGL initializer destroyed");
        }
    }
    
    /**
     * Reset the initializer state (for testing or recovery scenarios)
     */
    public void reset() {
        if (mIsDestroyed.get()) {
            return;
        }
        
        Log.d(TAG, "Resetting early EGL initializer state");
        
        mIsInitialized.set(false);
        mIsInitializing.set(false);
        mSharedContext.set(null);
        
        // Don't reset the latch as it can't be reused
        // If reset is needed, create a new instance
    }
    
    /**
     * Get the EGL handler for posting tasks to the EGL thread
     */
    public Handler getEglHandler() {
        return mEglHandler;
    }
    
    /**
     * Check if the EGL thread is ready
     */
    public boolean isEglThreadReady() {
        return mEglHandler != null && !mIsDestroyed.get();
    }
}