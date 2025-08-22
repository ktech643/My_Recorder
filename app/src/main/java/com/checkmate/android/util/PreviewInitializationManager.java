package com.checkmate.android.util;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.TextureView;

import com.checkmate.android.AppPreference;
import com.checkmate.android.service.SharedEGL.SharedEglManager;
import com.checkmate.android.viewmodels.SharedViewModel;

/**
 * Utility class to manage preview initialization and fix first-time preview issues
 */
public class PreviewInitializationManager {
    private static final String TAG = "PreviewInitManager";
    
    // Constants for preview initialization
    private static final int PREVIEW_INIT_DELAY_MS = 500;
    private static final int MAX_INIT_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000;
    
    private static PreviewInitializationManager instance;
    private final Context context;
    private final Handler mainHandler;
    
    private PreviewInitializationManager(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public static synchronized PreviewInitializationManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreviewInitializationManager(context);
        }
        return instance;
    }
    
    /**
     * Initialize preview with comprehensive error handling
     */
    public void initializePreview(TextureView textureView, SharedEglManager eglManager, 
                                SharedViewModel viewModel, PreviewInitCallback callback) {
        if (textureView == null || eglManager == null || viewModel == null) {
            Log.e(TAG, "Invalid parameters for preview initialization");
            if (callback != null) {
                callback.onPreviewInitFailed("Invalid parameters");
            }
            return;
        }
        
        Log.d(TAG, "Starting preview initialization");
        
        // Check if this is first launch
        boolean isFirstLaunch = AppPreference.getBool(AppPreference.KEY.APP_FIRST_LAUNCH, true);
        if (isFirstLaunch) {
            Log.d(TAG, "First launch detected, performing complete initialization");
            performFirstLaunchInitialization(textureView, eglManager, viewModel, callback);
        } else {
            Log.d(TAG, "Normal launch, performing standard initialization");
            performStandardInitialization(textureView, eglManager, viewModel, callback);
        }
    }
    
    /**
     * Perform first launch initialization with complete setup
     */
    private void performFirstLaunchInitialization(TextureView textureView, SharedEglManager eglManager,
                                                SharedViewModel viewModel, PreviewInitCallback callback) {
        // Mark first launch as complete
        AppPreference.setBool(AppPreference.KEY.APP_FIRST_LAUNCH, false);
        
        // Force EGL reinitialization
        if (!eglManager.eglIsReady) {
            Log.d(TAG, "EGL not ready on first launch, initializing...");
            eglManager.setupFrameRectSurface();
            
            // Wait for EGL to be ready
            mainHandler.postDelayed(() -> {
                if (eglManager.eglIsReady) {
                    Log.d(TAG, "EGL ready, continuing first launch initialization");
                    completePreviewSetup(textureView, eglManager, viewModel, callback);
                } else {
                    Log.w(TAG, "EGL still not ready after delay, retrying...");
                    retryPreviewInitialization(textureView, eglManager, viewModel, callback, 1);
                }
            }, PREVIEW_INIT_DELAY_MS);
        } else {
            completePreviewSetup(textureView, eglManager, viewModel, callback);
        }
    }
    
    /**
     * Perform standard initialization
     */
    private void performStandardInitialization(TextureView textureView, SharedEglManager eglManager,
                                             SharedViewModel viewModel, PreviewInitCallback callback) {
        if (eglManager.eglIsReady) {
            Log.d(TAG, "EGL ready, proceeding with standard initialization");
            completePreviewSetup(textureView, eglManager, viewModel, callback);
        } else {
            Log.w(TAG, "EGL not ready, attempting to initialize...");
            eglManager.setupFrameRectSurface();
            
            mainHandler.postDelayed(() -> {
                if (eglManager.eglIsReady) {
                    completePreviewSetup(textureView, eglManager, viewModel, callback);
                } else {
                    retryPreviewInitialization(textureView, eglManager, viewModel, callback, 1);
                }
            }, PREVIEW_INIT_DELAY_MS);
        }
    }
    
    /**
     * Complete preview setup after EGL is ready
     */
    private void completePreviewSetup(TextureView textureView, SharedEglManager eglManager,
                                    SharedViewModel viewModel, PreviewInitCallback callback) {
        try {
            // Ensure texture view is properly configured
            if (!textureView.isAvailable()) {
                Log.d(TAG, "TextureView not available, waiting for surface...");
                waitForTextureViewSurface(textureView, eglManager, viewModel, callback);
                return;
            }
            
            // Get camera texture from EGL manager
            SurfaceTexture cameraTexture = eglManager.getCameraTexture();
            if (cameraTexture == null) {
                Log.w(TAG, "Camera texture is null, requesting new surface");
                eglManager.setupFrameRectSurface();
                
                mainHandler.postDelayed(() -> {
                    completePreviewSetup(textureView, eglManager, viewModel, callback);
                }, PREVIEW_INIT_DELAY_MS);
                return;
            }
            
            // Update shared view model
            viewModel.setTextureView(textureView);
            
            // Set up surface texture listener if not already set
            if (textureView.getSurfaceTextureListener() == null) {
                setupSurfaceTextureListener(textureView, eglManager, viewModel);
            }
            
            Log.d(TAG, "Preview setup completed successfully");
            if (callback != null) {
                callback.onPreviewInitSuccess();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error during preview setup", e);
            if (callback != null) {
                callback.onPreviewInitFailed("Preview setup error: " + e.getMessage());
            }
        }
    }
    
    /**
     * Wait for TextureView surface to be available
     */
    private void waitForTextureViewSurface(TextureView textureView, SharedEglManager eglManager,
                                         SharedViewModel viewModel, PreviewInitCallback callback) {
        mainHandler.postDelayed(() -> {
            if (textureView.isAvailable()) {
                Log.d(TAG, "TextureView surface now available, completing setup");
                completePreviewSetup(textureView, eglManager, viewModel, callback);
            } else {
                Log.w(TAG, "TextureView surface still not available, retrying...");
                retryPreviewInitialization(textureView, eglManager, viewModel, callback, 1);
            }
        }, PREVIEW_INIT_DELAY_MS);
    }
    
    /**
     * Set up surface texture listener
     */
    private void setupSurfaceTextureListener(TextureView textureView, SharedEglManager eglManager,
                                           SharedViewModel viewModel) {
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "Surface texture available: " + width + "x" + height);
                
                // Update view model with new dimensions
                viewModel.setSurfaceModel(surface,width, height);
                
                // Ensure EGL manager has the correct surface
                if (eglManager != null) {
                    eglManager.setPreviewSurface(surface, width, height);
                }
            }
            
            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "Surface texture size changed: " + width + "x" + height);
                viewModel.setSurfaceModel(surface,width, height);
            }
            
            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                Log.d(TAG, "Surface texture destroyed");
                return true;
            }
            
            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                // Handle frame updates if needed
            }
        });
    }
    
    /**
     * Retry preview initialization
     */
    private void retryPreviewInitialization(TextureView textureView, SharedEglManager eglManager,
                                          SharedViewModel viewModel, PreviewInitCallback callback, int retryCount) {
        if (retryCount >= MAX_INIT_RETRIES) {
            Log.e(TAG, "Max retry attempts reached for preview initialization");
            if (callback != null) {
                callback.onPreviewInitFailed("Max retry attempts reached");
            }
            return;
        }
        
        Log.d(TAG, "Retrying preview initialization, attempt " + (retryCount + 1));
        
        mainHandler.postDelayed(() -> {
            initializePreview(textureView, eglManager, viewModel, callback);
        }, RETRY_DELAY_MS * retryCount);
    }
    
    /**
     * Force refresh preview when issues are detected
     */
    public void forcePreviewRefresh(TextureView textureView, SharedEglManager eglManager,
                                  SharedViewModel viewModel, PreviewInitCallback callback) {
        Log.d(TAG, "Forcing preview refresh");
        
        try {
            // Clear existing surface texture listener
            textureView.setSurfaceTextureListener(null);
            
            // Force layout refresh
            textureView.requestLayout();
            textureView.invalidate();
            
            // Wait a bit and reinitialize
            mainHandler.postDelayed(() -> {
                initializePreview(textureView, eglManager, viewModel, callback);
            }, PREVIEW_INIT_DELAY_MS);
            
        } catch (Exception e) {
            Log.e(TAG, "Error during forced preview refresh", e);
            if (callback != null) {
                callback.onPreviewInitFailed("Forced refresh error: " + e.getMessage());
            }
        }
    }
    
    /**
     * Callback interface for preview initialization
     */
    public interface PreviewInitCallback {
        void onPreviewInitSuccess();
        void onPreviewInitFailed(String error);
    }
}
