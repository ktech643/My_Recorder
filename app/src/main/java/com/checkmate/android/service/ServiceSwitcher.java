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
     * @param context Application context
     * @param newServiceType The new service type to switch to
     */
    public static void switchService(Context context, ServiceType newServiceType) {
        Log.d(TAG, "Switching to service: " + newServiceType);
        
        SharedEglManager eglManager = SharedEglManager.getInstance();
        
        // Check if we're already on this service
        ServiceType currentService = getCurrentActiveService();
        if (currentService == newServiceType) {
            Log.d(TAG, "Already on service: " + newServiceType);
            return;
        }
        
        // Start new service if not already running
        if (!isServiceRunning(context, newServiceType)) {
            startService(context, newServiceType);
            
            // Wait a bit for service to initialize
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while waiting for service start", e);
            }
        }
        
        // Get new service instance and activate it
        BaseBackgroundService newService = getRunningService(newServiceType);
        
        if (newService != null) {
            // Use the enhanced EGL change method for seamless switching
            eglManager.eglChangeActiveService(
                newServiceType,
                newService.getSurfaceTexture(), 
                newService.getSurfaceWidth(),
                newService.getSurfaceHeight()
            );
            
            Log.d(TAG, "Successfully switched to service: " + newServiceType);
        } else {
            Log.w(TAG, "Failed to get running service instance for: " + newServiceType);
        }
    }
    
    /**
     * Get the currently active service type
     * @return The active service type or null if none
     */
    private static ServiceType getCurrentActiveService() {
        SharedEglManager eglManager = SharedEglManager.getInstance();
        return eglManager.getCurrentActiveService();
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