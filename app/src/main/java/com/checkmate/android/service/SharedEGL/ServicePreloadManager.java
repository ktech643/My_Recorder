package com.checkmate.android.service.SharedEGL;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.checkmate.android.AppPreference;
import com.checkmate.android.service.BaseBackgroundService;
import com.checkmate.android.service.BgAudioService;
import com.checkmate.android.service.BgCameraService;
import com.checkmate.android.service.BgCastService;
import com.checkmate.android.service.BgUSBService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages service preloading and warm-up to reduce transition times.
 * Services are started in the background and kept ready for quick switching.
 */
public class ServicePreloadManager {
    private static final String TAG = "ServicePreloadManager";
    private static final int WARMUP_DELAY_MS = 500;
    private static final int MAX_PRELOAD_SERVICES = 2; // Limit to conserve resources
    
    private static volatile ServicePreloadManager sInstance;
    private final Context mContext;
    private final Handler mPreloadHandler;
    private final HandlerThread mPreloadThread;
    
    // Preloaded services
    private final Map<ServiceType, PreloadState> mPreloadedServices = new ConcurrentHashMap<>();
    
    // Preload configuration
    private boolean mAutoPreloadEnabled = true;
    private ServiceType mLastUsedService = null;
    
    private enum PreloadState {
        NOT_LOADED,
        LOADING,
        READY,
        ERROR
    }
    
    public interface PreloadCallback {
        void onServicePreloaded(ServiceType serviceType);
        void onPreloadError(ServiceType serviceType, String error);
    }
    
    private final Map<ServiceType, PreloadCallback> mPreloadCallbacks = new HashMap<>();
    
    private ServicePreloadManager(Context context) {
        mContext = context.getApplicationContext();
        
        // Create preload thread
        mPreloadThread = new HandlerThread("ServicePreloadThread");
        mPreloadThread.start();
        mPreloadHandler = new Handler(mPreloadThread.getLooper());
        
        // Initialize preload states
        for (ServiceType type : ServiceType.values()) {
            mPreloadedServices.put(type, PreloadState.NOT_LOADED);
        }
        
        // Load configuration
        loadConfiguration();
    }
    
    public static ServicePreloadManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (ServicePreloadManager.class) {
                if (sInstance == null) {
                    sInstance = new ServicePreloadManager(context);
                }
            }
        }
        return sInstance;
    }
    
    private void loadConfiguration() {
        mAutoPreloadEnabled = AppPreference.getBool(AppPreference.KEY.AUTO_PRELOAD, true);
        
        // Determine last used service
        String lastService = AppPreference.getStr(AppPreference.KEY.LAST_USED_SERVICE, "");
        if (!lastService.isEmpty()) {
            try {
                mLastUsedService = ServiceType.valueOf(lastService);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid last used service: " + lastService);
            }
        }
    }
    
    /**
     * Enable or disable automatic service preloading.
     */
    public void setAutoPreloadEnabled(boolean enabled) {
        mAutoPreloadEnabled = enabled;
        AppPreference.setBool(AppPreference.KEY.AUTO_PRELOAD, enabled);
        
        if (!enabled) {
            // Stop all preloaded services
            stopAllPreloadedServices();
        }
    }
    
    /**
     * Preload a specific service for quick access.
     */
    public void preloadService(ServiceType serviceType, PreloadCallback callback) {
        if (!mAutoPreloadEnabled) {
            Log.d(TAG, "Auto preload disabled, skipping: " + serviceType);
            return;
        }
        
        PreloadState currentState = mPreloadedServices.get(serviceType);
        if (currentState == PreloadState.READY) {
            Log.d(TAG, "Service already preloaded: " + serviceType);
            if (callback != null) {
                callback.onServicePreloaded(serviceType);
            }
            return;
        }
        
        if (currentState == PreloadState.LOADING) {
            Log.d(TAG, "Service already loading: " + serviceType);
            // Add callback to be notified when ready
            if (callback != null) {
                mPreloadCallbacks.put(serviceType, callback);
            }
            return;
        }
        
        // Check if we need to unload a service first
        if (getPreloadedServiceCount() >= MAX_PRELOAD_SERVICES) {
            unloadLeastRecentlyUsedService();
        }
        
        // Start preloading
        mPreloadedServices.put(serviceType, PreloadState.LOADING);
        if (callback != null) {
            mPreloadCallbacks.put(serviceType, callback);
        }
        
        mPreloadHandler.post(() -> doPreloadService(serviceType));
    }
    
    /**
     * Preload commonly used services based on usage patterns.
     */
    public void preloadCommonServices() {
        if (!mAutoPreloadEnabled) {
            return;
        }
        
        Log.d(TAG, "Preloading common services");
        
        // Always preload the last used service
        if (mLastUsedService != null) {
            preloadService(mLastUsedService, null);
        }
        
        // Preload camera service as it's commonly used
        if (mLastUsedService != ServiceType.BgCamera) {
            mPreloadHandler.postDelayed(() -> 
                preloadService(ServiceType.BgCamera, null), WARMUP_DELAY_MS);
        }
    }
    
    /**
     * Warm up a service that's about to be used.
     * This ensures it's fully ready when needed.
     */
    public void warmUpService(ServiceType serviceType, PreloadCallback callback) {
        Log.d(TAG, "Warming up service: " + serviceType);
        
        // Update last used service
        mLastUsedService = serviceType;
        AppPreference.setStr(AppPreference.KEY.LAST_USED_SERVICE, serviceType.name());
        
        // Preload if not already loaded
        preloadService(serviceType, callback);
        
        // Predict next likely service based on patterns
        predictAndPreloadNextService(serviceType);
    }
    
    private void doPreloadService(ServiceType serviceType) {
        Log.d(TAG, "Starting preload of service: " + serviceType);
        
        try {
            // Start the service
            Intent intent = new Intent(mContext, getServiceClass(serviceType));
            mContext.startService(intent);
            
            // Wait for service to initialize
            boolean initialized = waitForServiceInitialization(serviceType);
            
            if (initialized) {
                mPreloadedServices.put(serviceType, PreloadState.READY);
                
                // Perform warm-up operations
                performWarmUp(serviceType);
                
                // Notify callback
                PreloadCallback callback = mPreloadCallbacks.remove(serviceType);
                if (callback != null) {
                    new Handler(mContext.getMainLooper()).post(() -> 
                        callback.onServicePreloaded(serviceType));
                }
                
                Log.d(TAG, "Service preloaded successfully: " + serviceType);
                
            } else {
                throw new Exception("Service initialization timeout");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to preload service: " + serviceType, e);
            
            mPreloadedServices.put(serviceType, PreloadState.ERROR);
            
            // Notify error
            PreloadCallback callback = mPreloadCallbacks.remove(serviceType);
            if (callback != null) {
                new Handler(mContext.getMainLooper()).post(() -> 
                    callback.onPreloadError(serviceType, e.getMessage()));
            }
        }
    }
    
    private boolean waitForServiceInitialization(ServiceType serviceType) {
        // Wait for service to register with SharedEglManager
        SharedEglManager eglManager = SharedEglManager.getInstance();
        
        for (int i = 0; i < 20; i++) { // 2 seconds timeout
            BaseBackgroundService service = eglManager.getServiceInstance(serviceType);
            if (service != null) {
                return true;
            }
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return false;
            }
        }
        
        return false;
    }
    
    private void performWarmUp(ServiceType serviceType) {
        // Perform service-specific warm-up operations
        SharedEglManager eglManager = SharedEglManager.getInstance();
        BaseBackgroundService service = eglManager.getServiceInstance(serviceType);
        
        if (service == null) {
            return;
        }
        
        switch (serviceType) {
            case BgCamera:
                // Initialize camera but don't start preview
                Log.d(TAG, "Warming up camera service");
                break;
                
            case BgUSBCamera:
                // Initialize USB detection
                Log.d(TAG, "Warming up USB camera service");
                break;
                
            case BgScreenCast:
                // Prepare screen capture resources
                Log.d(TAG, "Warming up screen cast service");
                break;
                
            case BgAudio:
                // Initialize audio recording
                Log.d(TAG, "Warming up audio service");
                break;
        }
    }
    
    private void predictAndPreloadNextService(ServiceType currentService) {
        // Simple prediction based on common usage patterns
        ServiceType predictedNext = null;
        
        switch (currentService) {
            case BgCamera:
                // Users often switch between front/rear and USB
                predictedNext = ServiceType.BgUSBCamera;
                break;
                
            case BgUSBCamera:
                // May switch back to regular camera
                predictedNext = ServiceType.BgCamera;
                break;
                
            case BgScreenCast:
                // Often switch to camera after screen cast
                predictedNext = ServiceType.BgCamera;
                break;
                
            case BgAudio:
                // May switch to video
                predictedNext = ServiceType.BgCamera;
                break;
        }
        
        if (predictedNext != null && predictedNext != currentService) {
            // Preload predicted service with delay
            mPreloadHandler.postDelayed(() -> 
                preloadService(predictedNext, null), WARMUP_DELAY_MS * 2);
        }
    }
    
    private void unloadLeastRecentlyUsedService() {
        // Find service to unload (not the current or last used)
        for (Map.Entry<ServiceType, PreloadState> entry : mPreloadedServices.entrySet()) {
            ServiceType type = entry.getKey();
            PreloadState state = entry.getValue();
            
            if (state == PreloadState.READY && 
                type != mLastUsedService && 
                type != getCurrentActiveService()) {
                
                Log.d(TAG, "Unloading service to make room: " + type);
                unloadService(type);
                break;
            }
        }
    }
    
    private void unloadService(ServiceType serviceType) {
        // Stop the service if it's not actively being used
        SharedEglManager eglManager = SharedEglManager.getInstance();
        BaseBackgroundService service = eglManager.getServiceInstance(serviceType);
        
        if (service != null && !service.isStreaming() && !service.isRecording()) {
            Intent intent = new Intent(mContext, getServiceClass(serviceType));
            mContext.stopService(intent);
            
            mPreloadedServices.put(serviceType, PreloadState.NOT_LOADED);
        }
    }
    
    private void stopAllPreloadedServices() {
        Log.d(TAG, "Stopping all preloaded services");
        
        for (ServiceType type : ServiceType.values()) {
            if (mPreloadedServices.get(type) == PreloadState.READY) {
                unloadService(type);
            }
        }
    }
    
    private ServiceType getCurrentActiveService() {
        // Get from SeamlessTransitionManager
        SeamlessTransitionManager transitionManager = 
            SeamlessTransitionManager.getInstance(mContext);
        return transitionManager.getCurrentService();
    }
    
    private int getPreloadedServiceCount() {
        int count = 0;
        for (PreloadState state : mPreloadedServices.values()) {
            if (state == PreloadState.READY || state == PreloadState.LOADING) {
                count++;
            }
        }
        return count;
    }
    
    private Class<?> getServiceClass(ServiceType serviceType) {
        switch (serviceType) {
            case BgCamera:
                return BgCameraService.class;
            case BgUSBCamera:
                return BgUSBService.class;
            case BgScreenCast:
                return BgCastService.class;
            case BgAudio:
                return BgAudioService.class;
            default:
                return null;
        }
    }
    
    /**
     * Check if a service is preloaded and ready.
     */
    public boolean isServiceReady(ServiceType serviceType) {
        return mPreloadedServices.get(serviceType) == PreloadState.READY;
    }
    
    /**
     * Get the preload state of a service.
     */
    public PreloadState getServiceState(ServiceType serviceType) {
        return mPreloadedServices.get(serviceType);
    }
    
    /**
     * Release resources.
     */
    public void release() {
        stopAllPreloadedServices();
        
        if (mPreloadThread != null) {
            mPreloadThread.quitSafely();
            mPreloadThread = null;
        }
        
        mPreloadCallbacks.clear();
        mPreloadedServices.clear();
    }
}