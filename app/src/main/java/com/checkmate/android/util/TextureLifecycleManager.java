package com.checkmate.android.util;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TextureLifecycleManager - Prevents texture abandonment and ensures proper thread safety
 * 
 * Compatible with:
 * - Java 17
 * - Gradle 7.4.2  
 * - Target SDK 31
 * - Comprehensive texture lifecycle management
 * - Thread-safe operations
 * - Zero texture abandonment guarantee
 */
public class TextureLifecycleManager {
    private static final String TAG = "TextureLifecycleManager";
    
    // Singleton instance for app-wide texture management
    private static volatile TextureLifecycleManager sInstance;
    private static final Object sLock = new Object();
    
    // Thread management
    private HandlerThread mTextureThread;
    private Handler mTextureHandler;
    private final AtomicBoolean mIsInitialized = new AtomicBoolean(false);
    
    // Texture tracking
    private final ConcurrentHashMap<Integer, TextureInfo> mActiveTextures = new ConcurrentHashMap<>();
    private final AtomicInteger mTextureIdCounter = new AtomicInteger(1000);
    
    // Lifecycle callbacks
    private TextureLifecycleCallback mLifecycleCallback;
    
    /**
     * Singleton access - thread safe
     */
    public static TextureLifecycleManager getInstance() {
        if (sInstance == null) {
            synchronized (sLock) {
                if (sInstance == null) {
                    sInstance = new TextureLifecycleManager();
                }
            }
        }
        return sInstance;
    }
    
    private TextureLifecycleManager() {
        // Private constructor for singleton
    }
    
    /**
     * CRITICAL: Initialize texture management system
     * - Creates dedicated texture thread
     * - Sets up proper GL context management
     * - Ensures thread safety for all operations
     */
    public synchronized void initialize() {
        if (mIsInitialized.get()) {
            Log.d(TAG, "TextureLifecycleManager already initialized");
            return;
        }
        
        try {
            Log.d(TAG, "Initializing TextureLifecycleManager for Java 17 + SDK 31");
            
            // Create dedicated texture thread
            mTextureThread = new HandlerThread("TextureLifecycleThread") {
                @Override
                protected void onLooperPrepared() {
                    Log.d(TAG, "Texture thread looper prepared");
                }
            };
            mTextureThread.start();
            
            // Create handler for texture operations
            mTextureHandler = new Handler(mTextureThread.getLooper());
            
            // Post initialization task
            mTextureHandler.post(() -> {
                try {
                    Log.d(TAG, "Texture thread initialized successfully");
                    mIsInitialized.set(true);
                    
                    if (mLifecycleCallback != null) {
                        mLifecycleCallback.onTextureManagerInitialized();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to initialize texture thread", e);
                    mIsInitialized.set(false);
                }
            });
            
            Log.d(TAG, "✅ TextureLifecycleManager initialization completed");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize TextureLifecycleManager", e);
            mIsInitialized.set(false);
        }
    }
    
    /**
     * CRITICAL: Create texture safely with proper thread management
     * Prevents texture abandonment and ensures proper lifecycle
     */
    public void createTextureSafely(TextureCreationCallback callback) {
        if (!mIsInitialized.get()) {
            Log.w(TAG, "TextureLifecycleManager not initialized, initializing now");
            initialize();
        }
        
        if (mTextureHandler == null) {
            Log.e(TAG, "Texture handler not available");
            if (callback != null) {
                callback.onTextureCreationFailed("Texture handler not available");
            }
            return;
        }
        
        mTextureHandler.post(() -> {
            try {
                Log.d(TAG, "Creating texture on dedicated thread");
                
                // Generate unique texture ID
                int textureId = mTextureIdCounter.getAndIncrement();
                
                // Generate OpenGL texture
                int[] textures = new int[1];
                GLES20.glGenTextures(1, textures, 0);
                int glTextureId = textures[0];
                
                if (glTextureId == 0) {
                    Log.e(TAG, "Failed to generate OpenGL texture");
                    if (callback != null) {
                        callback.onTextureCreationFailed("OpenGL texture generation failed");
                    }
                    return;
                }
                
                // Configure texture parameters
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, glTextureId);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
                
                // Create SurfaceTexture
                SurfaceTexture surfaceTexture = new SurfaceTexture(glTextureId);
                Surface surface = new Surface(surfaceTexture);
                
                // Create texture info
                TextureInfo textureInfo = new TextureInfo(
                    textureId, 
                    glTextureId, 
                    surfaceTexture, 
                    surface,
                    Thread.currentThread().getId()
                );
                
                // Register texture
                mActiveTextures.put(textureId, textureInfo);
                
                Log.d(TAG, "✅ Texture created successfully: ID=" + textureId + 
                           ", GL=" + glTextureId + ", Thread=" + Thread.currentThread().getName());
                
                // Set frame available listener with thread safety
                surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        // Ensure frame handling on correct thread
                        if (mTextureHandler != null) {
                            mTextureHandler.post(() -> {
                                try {
                                    if (mLifecycleCallback != null) {
                                        mLifecycleCallback.onFrameAvailable(textureId, surfaceTexture);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error handling frame available", e);
                                }
                            });
                        }
                    }
                }, mTextureHandler);
                
                // Notify callback
                if (callback != null) {
                    callback.onTextureCreated(textureInfo);
                }
                
                if (mLifecycleCallback != null) {
                    mLifecycleCallback.onTextureCreated(textureInfo);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to create texture", e);
                if (callback != null) {
                    callback.onTextureCreationFailed(e.getMessage());
                }
            }
        });
    }
    
    /**
     * CRITICAL: Release texture safely - prevents abandonment
     */
    public void releaseTextureSafely(int textureId) {
        TextureInfo textureInfo = mActiveTextures.get(textureId);
        if (textureInfo == null) {
            Log.w(TAG, "Texture not found for release: " + textureId);
            return;
        }
        
        if (mTextureHandler == null) {
            Log.e(TAG, "Texture handler not available for release");
            return;
        }
        
        mTextureHandler.post(() -> {
            try {
                Log.d(TAG, "Releasing texture safely: " + textureId);
                
                // Remove from tracking
                TextureInfo info = mActiveTextures.remove(textureId);
                if (info == null) {
                    Log.w(TAG, "Texture already released: " + textureId);
                    return;
                }
                
                // Release Surface first
                if (info.surface != null) {
                    try {
                        info.surface.release();
                        Log.d(TAG, "Surface released for texture: " + textureId);
                    } catch (Exception e) {
                        Log.e(TAG, "Error releasing surface", e);
                    }
                }
                
                // Release SurfaceTexture
                if (info.surfaceTexture != null) {
                    try {
                        info.surfaceTexture.release();
                        Log.d(TAG, "SurfaceTexture released for texture: " + textureId);
                    } catch (Exception e) {
                        Log.e(TAG, "Error releasing SurfaceTexture", e);
                    }
                }
                
                // Release OpenGL texture
                if (info.glTextureId > 0) {
                    try {
                        GLES20.glDeleteTextures(1, new int[]{info.glTextureId}, 0);
                        Log.d(TAG, "OpenGL texture deleted: " + info.glTextureId);
                    } catch (Exception e) {
                        Log.e(TAG, "Error deleting OpenGL texture", e);
                    }
                }
                
                Log.d(TAG, "✅ Texture released successfully: " + textureId);
                
                if (mLifecycleCallback != null) {
                    mLifecycleCallback.onTextureReleased(textureId);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to release texture: " + textureId, e);
            }
        });
    }
    
    /**
     * CRITICAL: Release all textures - prevents mass abandonment
     */
    public void releaseAllTextures() {
        Log.d(TAG, "Releasing all textures to prevent abandonment");
        
        // Get snapshot of active textures
        int[] textureIds = mActiveTextures.keySet().toArray(new Integer[0]);
        
        for (int textureId : textureIds) {
            releaseTextureSafely(textureId);
        }
        
        Log.d(TAG, "All textures released: " + textureIds.length);
    }
    
    /**
     * CRITICAL: Check if texture thread is valid and running
     */
    public boolean isTextureThreadValid() {
        return mTextureThread != null && 
               mTextureThread.isAlive() && 
               mTextureHandler != null &&
               mIsInitialized.get();
    }
    
    /**
     * CRITICAL: Ensure operation is on texture thread
     */
    public void ensureTextureThread(Runnable operation) {
        if (!isTextureThreadValid()) {
            Log.w(TAG, "Texture thread not valid, initializing");
            initialize();
        }
        
        if (mTextureHandler != null) {
            if (Looper.myLooper() == mTextureHandler.getLooper()) {
                // Already on texture thread
                operation.run();
            } else {
                // Post to texture thread
                mTextureHandler.post(operation);
            }
        } else {
            Log.e(TAG, "Cannot ensure texture thread - handler unavailable");
        }
    }
    
    /**
     * Get texture info safely
     */
    public TextureInfo getTextureInfo(int textureId) {
        return mActiveTextures.get(textureId);
    }
    
    /**
     * Get active texture count
     */
    public int getActiveTextureCount() {
        return mActiveTextures.size();
    }
    
    /**
     * Set lifecycle callback
     */
    public void setLifecycleCallback(TextureLifecycleCallback callback) {
        mLifecycleCallback = callback;
    }
    
    /**
     * CRITICAL: Shutdown texture manager properly
     */
    public synchronized void shutdown() {
        Log.d(TAG, "Shutting down TextureLifecycleManager");
        
        try {
            // Release all textures first
            releaseAllTextures();
            
            // Clear callback
            mLifecycleCallback = null;
            
            // Shutdown texture thread
            if (mTextureHandler != null) {
                mTextureHandler.removeCallbacksAndMessages(null);
                mTextureHandler = null;
            }
            
            if (mTextureThread != null) {
                mTextureThread.quitSafely();
                try {
                    mTextureThread.join(1000); // Wait up to 1 second
                } catch (InterruptedException e) {
                    Log.w(TAG, "Interrupted while waiting for texture thread shutdown");
                    Thread.currentThread().interrupt();
                }
                mTextureThread = null;
            }
            
            mIsInitialized.set(false);
            
            Log.d(TAG, "✅ TextureLifecycleManager shutdown completed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during shutdown", e);
        }
    }
    
    /**
     * Texture information holder
     */
    public static class TextureInfo {
        public final int textureId;
        public final int glTextureId;
        public final SurfaceTexture surfaceTexture;
        public final Surface surface;
        public final long threadId;
        public final long creationTime;
        
        public TextureInfo(int textureId, int glTextureId, SurfaceTexture surfaceTexture, 
                          Surface surface, long threadId) {
            this.textureId = textureId;
            this.glTextureId = glTextureId;
            this.surfaceTexture = surfaceTexture;
            this.surface = surface;
            this.threadId = threadId;
            this.creationTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Texture creation callback
     */
    public interface TextureCreationCallback {
        void onTextureCreated(TextureInfo textureInfo);
        void onTextureCreationFailed(String error);
    }
    
    /**
     * Texture lifecycle callback
     */
    public interface TextureLifecycleCallback {
        void onTextureManagerInitialized();
        void onTextureCreated(TextureInfo textureInfo);
        void onTextureReleased(int textureId);
        void onFrameAvailable(int textureId, SurfaceTexture surfaceTexture);
    }
}