package com.checkmate.android.service;

import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.checkmate.android.service.SharedEGL.ServiceType;
import com.checkmate.android.service.SharedEGL.SharedEglManager;
import com.checkmate.android.util.TransitionOverlay;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Optimized service switcher that ensures seamless transitions
 * without blank screens or stream interruptions
 */
public class OptimizedServiceSwitcher {
    private static final String TAG = "OptimizedServiceSwitcher";
    private static final int TRANSITION_TIMEOUT_MS = 3000;
    private static final int PRELOAD_DELAY_MS = 100;
    
    private static volatile ServiceType mPendingService = null;
    private static volatile boolean mIsTransitioning = false;
    private static final Object mTransitionLock = new Object();
    
    private static Handler mMainHandler = new Handler(Looper.getMainLooper());
    
    /**
     * Optimized service switching with preloading and seamless transition
     */
    public static void switchServiceOptimized(Context context, ServiceType newServiceType, 
                                             ServiceSwitchCallback callback) {
        synchronized (mTransitionLock) {
            if (mIsTransitioning) {
                Log.w(TAG, "Transition already in progress, queueing request");
                mPendingService = newServiceType;
                return;
            }
            mIsTransitioning = true;
        }
        
        Log.d(TAG, "Starting optimized switch to service: " + newServiceType);
        
        // Run transition on background thread
        new Thread(() -> {
            try {
                performOptimizedTransition(context, newServiceType, callback);
            } finally {
                synchronized (mTransitionLock) {
                    mIsTransitioning = false;
                    
                    // Check for pending service switch
                    if (mPendingService != null) {
                        ServiceType pending = mPendingService;
                        mPendingService = null;
                        switchServiceOptimized(context, pending, callback);
                    }
                }
            }
        }).start();
    }
    
    private static void performOptimizedTransition(Context context, ServiceType newServiceType,
                                                   ServiceSwitchCallback callback) {
        SharedEglManager eglManager = SharedEglManager.getInstance();
        ServiceType currentService = eglManager.getCurrentActiveService();
        
        // If switching to the same service, just update configuration
        if (currentService == newServiceType) {
            Log.d(TAG, "Already on service " + newServiceType + ", updating configuration");
            updateServiceConfiguration(context, newServiceType);
            notifyCallback(callback, true, "Configuration updated");
            return;
        }
        
        // Step 1: Preload new service if not running
        BaseBackgroundService newService = preloadService(context, newServiceType);
        if (newService == null) {
            notifyCallback(callback, false, "Failed to preload service");
            return;
        }
        
        // Step 2: Prepare transition overlay if streaming/recording
        boolean needsOverlay = eglManager.isStreaming() || eglManager.isRecording();
        if (needsOverlay) {
            Log.d(TAG, "Showing transition overlay");
            mMainHandler.post(() -> TransitionOverlay.show(context));
        }
        
        try {
            // Step 3: Prepare new service surface
            CountDownLatch prepareLatch = new CountDownLatch(1);
            final boolean[] prepareSuccess = {false};
            
            mMainHandler.post(() -> {
                try {
                    // Get surface from new service
                    SurfaceTexture newSurface = newService.getSurfaceTexture();
                    if (newSurface != null) {
                        // Pre-warm the new service
                        newService.onSurfaceTextureAvailable(
                            newSurface,
                            newService.getSurfaceWidth(),
                            newService.getSurfaceHeight()
                        );
                        prepareSuccess[0] = true;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to prepare new service", e);
                } finally {
                    prepareLatch.countDown();
                }
            });
            
            if (!prepareLatch.await(TRANSITION_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Timeout preparing new service");
            }
            
            if (!prepareSuccess[0]) {
                throw new RuntimeException("Failed to prepare new service surface");
            }
            
            // Step 4: Perform atomic switch
            boolean switchSuccess = eglManager.switchToService(newServiceType);
            
            if (switchSuccess) {
                // Step 5: Activate new service
                mMainHandler.post(() -> {
                    newService.activateService(
                        newService.getSurfaceTexture(),
                        newService.getSurfaceWidth(),
                        newService.getSurfaceHeight()
                    );
                });
                
                // Give service time to stabilize
                Thread.sleep(PRELOAD_DELAY_MS);
                
                // Step 6: Stop old service if not streaming/recording
                if (!eglManager.isStreaming() && !eglManager.isRecording()) {
                    stopServiceDelayed(context, currentService, 500);
                }
                
                notifyCallback(callback, true, "Service switched successfully");
            } else {
                throw new RuntimeException("EGL manager failed to switch service");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Service transition failed", e);
            notifyCallback(callback, false, e.getMessage());
        } finally {
            // Remove overlay
            if (needsOverlay) {
                mMainHandler.postDelayed(() -> TransitionOverlay.hide(), 200);
            }
        }
    }
    
    /**
     * Preload a service without activating it
     */
    private static BaseBackgroundService preloadService(Context context, ServiceType serviceType) {
        SharedEglManager eglManager = SharedEglManager.getInstance();
        BaseBackgroundService service = eglManager.getServiceInstance(serviceType);
        
        if (service == null) {
            Log.d(TAG, "Service not running, starting: " + serviceType);
            Intent intent = new Intent(context, getServiceClass(serviceType));
            context.startService(intent);
            
            // Wait for service to register
            for (int i = 0; i < 30; i++) { // 3 seconds timeout
                service = eglManager.getServiceInstance(serviceType);
                if (service != null) {
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        
        return service;
    }
    
    /**
     * Update configuration for an existing service
     */
    private static void updateServiceConfiguration(Context context, ServiceType serviceType) {
        SharedEglManager eglManager = SharedEglManager.getInstance();
        BaseBackgroundService service = eglManager.getServiceInstance(serviceType);
        
        if (service != null) {
            // Update configuration without stopping streams
            eglManager.updateConfiguration();
        }
    }
    
    /**
     * Stop a service with delay
     */
    private static void stopServiceDelayed(Context context, ServiceType serviceType, int delayMs) {
        mMainHandler.postDelayed(() -> {
            Intent intent = new Intent(context, getServiceClass(serviceType));
            context.stopService(intent);
            Log.d(TAG, "Stopped service: " + serviceType);
        }, delayMs);
    }
    
    /**
     * Get service class from type
     */
    private static Class<?> getServiceClass(ServiceType serviceType) {
        switch (serviceType) {
            case BgCamera:
                return BgCameraService.class;
            case BgAudio:
                return BgAudioService.class;
            case BgScreenCast:
                return BgCastService.class;
            case BgUSBCamera:
                return BgUSBService.class;
            default:
                throw new IllegalArgumentException("Unknown service type: " + serviceType);
        }
    }
    
    /**
     * Notify callback on main thread
     */
    private static void notifyCallback(ServiceSwitchCallback callback, boolean success, String message) {
        if (callback != null) {
            mMainHandler.post(() -> callback.onServiceSwitched(success, message));
        }
    }
    
    /**
     * Callback interface for service switching
     */
    public interface ServiceSwitchCallback {
        void onServiceSwitched(boolean success, String message);
    }
}