package com.checkmate.android.util;

import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.checkmate.android.service.SharedEGL.SharedEglManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * EGL Initializer for early context creation in MainActivity
 * This helps reduce transition times and blank screens
 */
public class EglInitializer {
    private static final String TAG = "EglInitializer";
    private static EglInitializer sInstance;
    
    private HandlerThread mEglThread;
    private Handler mEglHandler;
    private EGLDisplay mEglDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext mEglContext = EGL14.EGL_NO_CONTEXT;
    private EGLSurface mPBufferSurface = EGL14.EGL_NO_SURFACE;
    private volatile boolean mInitialized = false;
    private final Object mLock = new Object();
    
    private EglInitializer() {}
    
    public static synchronized EglInitializer getInstance() {
        if (sInstance == null) {
            sInstance = new EglInitializer();
        }
        return sInstance;
    }
    
    /**
     * Initialize EGL context early in MainActivity onCreate
     * This runs on a separate thread to avoid blocking UI
     */
    public void initializeAsync(Context context) {
        if (mInitialized) {
            Log.d(TAG, "EGL already initialized");
            return;
        }
        
        // Create EGL initialization thread
        mEglThread = new HandlerThread("EglInitThread");
        mEglThread.start();
        mEglHandler = new Handler(mEglThread.getLooper());
        
        final CountDownLatch initLatch = new CountDownLatch(1);
        
        mEglHandler.post(() -> {
            try {
                initializeEglContext();
                mInitialized = true;
                Log.d(TAG, "EGL context initialized successfully");
                
                // Notify SharedEglManager that early init is complete
                SharedEglManager.getInstance().onEarlyEglInitComplete(mEglContext);
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize EGL context", e);
            } finally {
                initLatch.countDown();
            }
        });
        
        // Wait for initialization (with timeout)
        new Thread(() -> {
            try {
                if (!initLatch.await(3, TimeUnit.SECONDS)) {
                    Log.w(TAG, "EGL initialization timeout");
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "EGL initialization interrupted", e);
            }
        }).start();
    }
    
    private void initializeEglContext() throws RuntimeException {
        // Get EGL display
        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("Unable to get EGL display");
        }
        
        // Initialize EGL
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
            throw new RuntimeException("Unable to initialize EGL");
        }
        
        // Configure EGL
        int[] attribList = {
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE
        };
        
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(mEglDisplay, attribList, 0, configs, 0, configs.length,
                numConfigs, 0)) {
            throw new RuntimeException("Unable to choose EGL config");
        }
        
        // Create EGL context
        int[] contextAttribs = {
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        };
        
        mEglContext = EGL14.eglCreateContext(mEglDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
                contextAttribs, 0);
        if (mEglContext == EGL14.EGL_NO_CONTEXT) {
            throw new RuntimeException("Failed to create EGL context");
        }
        
        // Create a small PBuffer surface for initialization
        int[] surfaceAttribs = {
            EGL14.EGL_WIDTH, 1,
            EGL14.EGL_HEIGHT, 1,
            EGL14.EGL_NONE
        };
        
        mPBufferSurface = EGL14.eglCreatePbufferSurface(mEglDisplay, configs[0],
                surfaceAttribs, 0);
        if (mPBufferSurface == EGL14.EGL_NO_SURFACE) {
            throw new RuntimeException("Failed to create PBuffer surface");
        }
        
        // Make context current briefly to ensure it's properly initialized
        if (!EGL14.eglMakeCurrent(mEglDisplay, mPBufferSurface, mPBufferSurface, mEglContext)) {
            throw new RuntimeException("Failed to make EGL context current");
        }
        
        // Initialize basic GL state
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        
        // Release context (SharedEglManager will take over)
        EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT);
    }
    
    /**
     * Get the initialized EGL context
     */
    public EGLContext getEglContext() {
        synchronized (mLock) {
            return mInitialized ? mEglContext : EGL14.EGL_NO_CONTEXT;
        }
    }
    
    /**
     * Check if EGL is initialized
     */
    public boolean isInitialized() {
        synchronized (mLock) {
            return mInitialized;
        }
    }
    
    /**
     * Cleanup EGL resources (call in MainActivity onDestroy)
     */
    public void cleanup() {
        if (mEglHandler != null) {
            mEglHandler.post(this::cleanupEgl);
            mEglThread.quitSafely();
            try {
                mEglThread.join(1000);
            } catch (InterruptedException e) {
                Log.e(TAG, "Failed to join EGL thread", e);
            }
        }
    }
    
    private void cleanupEgl() {
        if (mPBufferSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(mEglDisplay, mPBufferSurface);
            mPBufferSurface = EGL14.EGL_NO_SURFACE;
        }
        
        if (mEglContext != EGL14.EGL_NO_CONTEXT) {
            EGL14.eglDestroyContext(mEglDisplay, mEglContext);
            mEglContext = EGL14.EGL_NO_CONTEXT;
        }
        
        if (mEglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglTerminate(mEglDisplay);
            mEglDisplay = EGL14.EGL_NO_DISPLAY;
        }
        
        mInitialized = false;
    }
}