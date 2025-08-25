package com.checkmate.android.service.SharedEGL;

import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.checkmate.android.util.libgraph.EglCoreNew;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages EGL lifecycle throughout the application.
 * Ensures EGL is initialized early and maintained across service transitions.
 */
public class EGLLifecycleManager {
    private static final String TAG = "EGLLifecycleManager";
    private static final int INIT_TIMEOUT_SECONDS = 5;
    
    private static volatile EGLLifecycleManager sInstance;
    private final Object mLock = new Object();
    
    private EglCoreNew mEglCore;
    private EGLContext mSharedContext;
    private HandlerThread mEglThread;
    private Handler mEglHandler;
    private final AtomicBoolean mIsInitialized = new AtomicBoolean(false);
    private final AtomicBoolean mIsShuttingDown = new AtomicBoolean(false);
    private CountDownLatch mInitLatch;
    
    // Callbacks
    public interface EGLCallback {
        void onEGLInitialized();
        void onEGLError(String error);
    }
    
    private EGLCallback mCallback;
    
    private EGLLifecycleManager() {
        // Private constructor for singleton
    }
    
    public static EGLLifecycleManager getInstance() {
        if (sInstance == null) {
            synchronized (EGLLifecycleManager.class) {
                if (sInstance == null) {
                    sInstance = new EGLLifecycleManager();
                }
            }
        }
        return sInstance;
    }
    
    /**
     * Initialize EGL context early in the application lifecycle.
     * Should be called from MainActivity.onCreate()
     */
    public void initializeEGL(Context context, EGLCallback callback) {
        synchronized (mLock) {
            if (mIsInitialized.get()) {
                Log.d(TAG, "EGL already initialized");
                if (callback != null) {
                    callback.onEGLInitialized();
                }
                return;
            }
            
            if (mIsShuttingDown.get()) {
                Log.w(TAG, "EGL is shutting down, cannot initialize");
                if (callback != null) {
                    callback.onEGLError("EGL is shutting down");
                }
                return;
            }
            
            mCallback = callback;
            mInitLatch = new CountDownLatch(1);
            
            // Create EGL thread
            mEglThread = new HandlerThread("EGLLifecycleThread");
            mEglThread.start();
            mEglHandler = new Handler(mEglThread.getLooper());
            
            // Initialize EGL on the dedicated thread
            mEglHandler.post(() -> {
                try {
                    Log.d(TAG, "Initializing EGL context");
                    
                    // Create EGL core
                    mEglCore = new EglCoreNew(null, EglCoreNew.FLAG_RECORDABLE);
                    
                    // Get shared context for use by services
                    mSharedContext = mEglCore.getEGLContext();
                    
                    // Create a dummy surface to make context current
                    EGLSurface dummySurface = mEglCore.createOffscreenSurface(1, 1);
                    mEglCore.makeCurrent(dummySurface);
                    
                    mIsInitialized.set(true);
                    mInitLatch.countDown();
                    
                    Log.d(TAG, "EGL context initialized successfully");
                    
                    // Notify callback on main thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (mCallback != null) {
                            mCallback.onEGLInitialized();
                        }
                    });
                    
                } catch (Exception e) {
                    Log.e(TAG, "Failed to initialize EGL", e);
                    mInitLatch.countDown();
                    
                    // Notify error on main thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (mCallback != null) {
                            mCallback.onEGLError("Failed to initialize EGL: " + e.getMessage());
                        }
                    });
                }
            });
        }
    }
    
    /**
     * Wait for EGL initialization to complete.
     * @return true if initialized successfully, false otherwise
     */
    public boolean waitForInitialization() {
        if (mIsInitialized.get()) {
            return true;
        }
        
        if (mInitLatch == null) {
            return false;
        }
        
        try {
            return mInitLatch.await(INIT_TIMEOUT_SECONDS, TimeUnit.SECONDS) && mIsInitialized.get();
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting for EGL initialization", e);
            return false;
        }
    }
    
    /**
     * Get the shared EGL context for use by services.
     * @return The shared EGL context or null if not initialized
     */
    public EGLContext getSharedContext() {
        return mIsInitialized.get() ? mSharedContext : null;
    }
    
    /**
     * Get the EGL core instance.
     * @return The EGL core or null if not initialized
     */
    public EglCoreNew getEglCore() {
        return mIsInitialized.get() ? mEglCore : null;
    }
    
    /**
     * Check if EGL is initialized.
     * @return true if EGL is initialized
     */
    public boolean isInitialized() {
        return mIsInitialized.get();
    }
    
    /**
     * Release EGL resources.
     * Should only be called when the application is terminating.
     */
    public void release() {
        synchronized (mLock) {
            if (!mIsInitialized.get()) {
                return;
            }
            
            mIsShuttingDown.set(true);
            
            if (mEglHandler != null) {
                mEglHandler.post(() -> {
                    try {
                        if (mEglCore != null) {
                            mEglCore.release();
                            mEglCore = null;
                        }
                        mSharedContext = null;
                        mIsInitialized.set(false);
                        
                        Log.d(TAG, "EGL context released");
                    } catch (Exception e) {
                        Log.e(TAG, "Error releasing EGL context", e);
                    }
                });
            }
            
            // Quit the EGL thread
            if (mEglThread != null) {
                mEglThread.quitSafely();
                try {
                    mEglThread.join(1000);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interrupted while waiting for EGL thread to quit", e);
                }
                mEglThread = null;
            }
            
            mEglHandler = null;
            mCallback = null;
            mIsShuttingDown.set(false);
        }
    }
    
    /**
     * Make the EGL context current on the calling thread.
     * @param surface The surface to make current
     * @return true if successful
     */
    public boolean makeContextCurrent(EGLSurface surface) {
        if (!mIsInitialized.get() || mEglCore == null) {
            Log.e(TAG, "Cannot make context current - EGL not initialized");
            return false;
        }
        
        try {
            mEglCore.makeCurrent(surface);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to make context current", e);
            return false;
        }
    }
    
    /**
     * Release the current EGL context from the calling thread.
     */
    public void releaseContext() {
        if (mEglCore != null) {
            try {
                mEglCore.makeNothingCurrent();
            } catch (Exception e) {
                Log.e(TAG, "Failed to release context", e);
            }
        }
    }
}