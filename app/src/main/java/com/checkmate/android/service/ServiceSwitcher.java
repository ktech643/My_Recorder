package com.checkmate.android.service;

import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.util.Log;
import com.checkmate.android.service.SharedEGL.ServiceType;
import com.checkmate.android.service.SharedEGL.SharedEglManager;

/**
 * Utility class for handling service switching without stopping streams/recordings
 */
public class ServiceSwitcher {
    private static final String TAG = "ServiceSwitcher";

    /**
     * Switch to a new service without stopping current streams/recordings
     * Uses optimized seamless switching for better user experience
     * @param context Application context
     * @param newServiceType The new service type to switch to
     */
    public static void switchService(Context context, ServiceType newServiceType) {
        Log.d(TAG, "Switching to service seamlessly: " + newServiceType);
        
        SharedEglManager eglManager = SharedEglManager.getInstance();
        
        // Check if EGL is ready for seamless switching
        if (!eglManager.isEglReady()) {
            Log.w(TAG, "EGL not ready, using traditional switching");
            switchServiceTraditional(context, newServiceType);
            return;
        }
        
        // Start new service if not already running
        if (!isServiceRunning(context, newServiceType)) {
            startService(context, newServiceType);
            
            // Small delay to allow service to initialize
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Get new service instance and activate it seamlessly
        BaseBackgroundService newService = getRunningService(newServiceType);
        
        if (newService != null) {
            // Use seamless activation to prevent interruptions
            eglManager.activateServiceSeamlessly(
                newServiceType,
                newService.getSurfaceTexture(), 
                newService.getSurfaceWidth(),
                newService.getSurfaceHeight()
            );
            
            // Update UI controls for the new service
            updateControlsForService(newServiceType);
            
            Log.d(TAG, "Successfully switched to service seamlessly: " + newServiceType);
        } else {
            Log.w(TAG, "Failed to get running service instance, using traditional switching");
            switchServiceTraditional(context, newServiceType);
        }
    }

    /**
     * Traditional service switching fallback method
     * @param context Application context
     * @param newServiceType The new service type to switch to
     */
    private static void switchServiceTraditional(Context context, ServiceType newServiceType) {
        Log.d(TAG, "Using traditional service switching for: " + newServiceType);
        
        // Start new service if not already running
        if (!isServiceRunning(context, newServiceType)) {
            startService(context, newServiceType);
        }
        
        // Get new service instance and activate it
        BaseBackgroundService newService = getRunningService(newServiceType);
        
        if (newService != null) {
            // Use traditional activation method
            newService.activateService(
                newService.getSurfaceTexture(), 
                newService.getSurfaceWidth(),
                newService.getSurfaceHeight()
            );
            
            Log.d(TAG, "Successfully switched to service traditionally: " + newServiceType);
        } else {
            Log.w(TAG, "Failed to get running service instance for: " + newServiceType);
        }
    }

    /**
     * Preload a service for faster switching
     * @param context Application context
     * @param serviceType The service type to preload
     */
    public static void preloadService(Context context, ServiceType serviceType) {
        Log.d(TAG, "Preloading service for faster switching: " + serviceType);
        
        SharedEglManager eglManager = SharedEglManager.getInstance();
        if (eglManager.isEglReady()) {
            eglManager.preloadServiceForSwitching(serviceType);
        }
        
        // Start the service if not running
        if (!isServiceRunning(context, serviceType)) {
            startService(context, serviceType);
        }
    }

    /**
     * Check if a service is currently running
     * @param context Application context
     * @param serviceType The service type to check
     * @return true if the service is running
     */
    private static boolean isServiceRunning(Context context, ServiceType serviceType) {
        SharedEglManager eglManager = SharedEglManager.getInstance();
        BaseBackgroundService service = eglManager.getServiceInstance(serviceType);
        return service != null;
    }

    /**
     * Start a service
     * @param context Application context
     * @param serviceType The service type to start
     */
    private static void startService(Context context, ServiceType serviceType) {
        Intent intent = new Intent(context, getServiceClass(serviceType));
        context.startService(intent);
    }

    /**
     * Get the service class for a given service type
     * @param serviceType The service type
     * @return The service class
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
     * Get a running service instance
     * @param serviceType The service type
     * @return The service instance or null if not found
     */
    private static BaseBackgroundService getRunningService(ServiceType serviceType) {
        SharedEglManager eglManager = SharedEglManager.getInstance();
        
        // Get the service instance directly from SharedEglManager
        BaseBackgroundService service = eglManager.getServiceInstance(serviceType);
        
        if (service != null) {
            Log.d(TAG, "Found running service instance for: " + serviceType);
            return service;
        } else {
            Log.w(TAG, "No running service instance found for: " + serviceType);
            return null;
        }
    }

    /**
     * Update UI controls for the new service
     * @param serviceType The new service type
     */
    public static void updateControlsForService(ServiceType serviceType) {
        Log.d(TAG, "Updating UI controls for service: " + serviceType);
        // This would typically update the UI to reflect the new active service
        // Implementation depends on your UI architecture
    }
} 