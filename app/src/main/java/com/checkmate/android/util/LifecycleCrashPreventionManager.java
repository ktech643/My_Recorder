package com.checkmate.android.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import com.checkmate.android.AppPreference;
import com.checkmate.android.service.SharedEGL.SharedEglManager;

/**
 * Utility class to prevent crashes during activity lifecycle transitions
 */
public class LifecycleCrashPreventionManager {
    private static final String TAG = "LifecycleCrashPrevention";
    
    // Constants for crash prevention
    private static final int SURFACE_CLEANUP_DELAY_MS = 100;
    private static final int MAX_CLEANUP_RETRIES = 3;
    private static final int CLEANUP_RETRY_DELAY_MS = 200;
    
    private static LifecycleCrashPreventionManager instance;
    private final Context context;
    private final Handler mainHandler;
    
    private LifecycleCrashPreventionManager(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public static synchronized LifecycleCrashPreventionManager getInstance(Context context) {
        if (instance == null) {
            instance = new LifecycleCrashPreventionManager(context);
        }
        return instance;
    }
    
    /**
     * Safely pause EGL operations to prevent surface conflicts
     */
    public void safelyPauseEglOperations(SharedEglManager eglManager) {
        if (eglManager == null) {
            Log.d(TAG, "EGL manager is null, skipping pause operations");
            return;
        }
        
        try {
            Log.d(TAG, "Safely pausing EGL operations");
            
            // Stop any ongoing operations that might cause surface conflicts
            if (eglManager.isStreaming()) {
                Log.d(TAG, "Stopping streaming before pause");
                eglManager.stopStreaming();
            }
            
            if (eglManager.isRecording()) {
                Log.d(TAG, "Stopping recording before pause");
                eglManager.stopRecording(false);
            }
            
            // Release display surface to prevent window manager conflicts
            releaseDisplaySurfaceSafely(eglManager);
            
        } catch (Exception e) {
            Log.w(TAG, "Error during EGL pause operations", e);
        }
    }
    
    /**
     * Safely release display surface to prevent window manager conflicts
     */
    private void releaseDisplaySurfaceSafely(SharedEglManager eglManager) {
        try {
            // Use reflection to safely call the method if it exists
            java.lang.reflect.Method releaseMethod = eglManager.getClass()
                .getMethod("releaseDisplaySurface");
            
            if (releaseMethod != null) {
                Log.d(TAG, "Releasing display surface via reflection");
                releaseMethod.invoke(eglManager);
            } else {
                Log.d(TAG, "releaseDisplaySurface method not found, using alternative cleanup");
                performAlternativeSurfaceCleanup(eglManager);
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Error releasing display surface, using alternative cleanup", e);
            performAlternativeSurfaceCleanup(eglManager);
        }
    }
    
    /**
     * Perform alternative surface cleanup when the specific method is not available
     */
    private void performAlternativeSurfaceCleanup(SharedEglManager eglManager) {
        try {
            Log.d(TAG, "Performing alternative surface cleanup");
            
            // Force EGL context release to prevent surface conflicts
            if (eglManager.eglIsReady) {
                // Use the existing cleanup method
                SharedEglManager.cleanAndResetAsync(() -> {
                    Log.d(TAG, "Alternative surface cleanup completed");
                });
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Error during alternative surface cleanup", e);
        }
    }
    
    /**
     * Safely resume EGL operations after pause
     */
    public void safelyResumeEglOperations(SharedEglManager eglManager) {
        if (eglManager == null) {
            Log.d(TAG, "EGL manager is null, skipping resume operations");
            return;
        }
        
        try {
            Log.d(TAG, "Safely resuming EGL operations");
            
            // Wait a bit to ensure surface cleanup is complete
            mainHandler.postDelayed(() -> {
                try {
                    if (eglManager.eglIsReady) {
                        Log.d(TAG, "EGL operations resumed successfully");
                    } else {
                        Log.w(TAG, "EGL not ready after resume, reinitializing");
                        eglManager.setupFrameRectSurface();
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error during EGL resume operations", e);
                }
            }, SURFACE_CLEANUP_DELAY_MS);
            
        } catch (Exception e) {
            Log.w(TAG, "Error during EGL resume operations", e);
        }
    }
    
    /**
     * Perform comprehensive cleanup before activity destruction
     */
    public void performPreDestroyCleanup(SharedEglManager eglManager) {
        if (eglManager == null) {
            Log.d(TAG, "EGL manager is null, skipping pre-destroy cleanup");
            return;
        }
        
        try {
            Log.d(TAG, "Performing pre-destroy cleanup");
            
            // Stop all operations immediately
            if (eglManager.isStreaming()) {
                eglManager.stopStreaming();
            }
            
            if (eglManager.isRecording()) {
                eglManager.stopRecording(false);
            }
            
            // Force complete cleanup
            SharedEglManager.cleanAndReset();
            
        } catch (Exception e) {
            Log.w(TAG, "Error during pre-destroy cleanup", e);
        }
    }
    
    /**
     * Handle activity state changes to prevent crashes
     */
    public void onActivityStateChanged(String state, SharedEglManager eglManager) {
        Log.d(TAG, "Activity state changed to: " + state);
        
        switch (state) {
            case "PAUSE":
                safelyPauseEglOperations(eglManager);
                break;
                
            case "RESUME":
                safelyResumeEglOperations(eglManager);
                break;
                
            case "STOP":
                safelyPauseEglOperations(eglManager);
                break;
                
            case "DESTROY":
                performPreDestroyCleanup(eglManager);
                break;
                
            default:
                Log.d(TAG, "Unknown activity state: " + state);
                break;
        }
    }
    
    /**
     * Check if EGL operations are safe to perform
     */
    public boolean areEglOperationsSafe(SharedEglManager eglManager) {
        if (eglManager == null) {
            return false;
        }
        
        try {
            // Check if the EGL context is still valid
            return eglManager.eglIsReady;
        } catch (Exception e) {
            Log.w(TAG, "Error checking EGL operation safety", e);
            return false;
        }
    }
    
    /**
     * Force safe cleanup when crashes are detected
     */
    public void forceSafeCleanup(SharedEglManager eglManager) {
        Log.w(TAG, "Forcing safe cleanup due to crash detection");
        
        try {
            // Perform cleanup on main thread to prevent conflicts
            mainHandler.post(() -> {
                try {
                    performPreDestroyCleanup(eglManager);
                } catch (Exception e) {
                    Log.e(TAG, "Error during forced safe cleanup", e);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error during forced safe cleanup", e);
        }
    }
}
