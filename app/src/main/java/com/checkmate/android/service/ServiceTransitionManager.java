package com.checkmate.android.service;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.checkmate.android.service.SharedEGL.ServiceType;
import com.checkmate.android.service.SharedEGL.SharedEglManager;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Singleton;

/**
 * Service Transition Manager for seamless service switching.
 * This class ensures minimal loading time and no blank screens during service transitions.
 */
@Singleton
public class ServiceTransitionManager {
    private static final String TAG = "ServiceTransitionManager";
    private static final long TRANSITION_TIMEOUT_MS = 5000; // 5 seconds
    private static final long BLANK_FRAME_OVERLAY_DURATION_MS = 100; // 100ms
    
    private static volatile ServiceTransitionManager sInstance;
    private static final Object sLock = new Object();
    
    // Service state management
    private final ConcurrentHashMap<ServiceType, ServiceState> mServiceStates = new ConcurrentHashMap<>();
    private final AtomicReference<ServiceType> mCurrentActiveService = new AtomicReference<>();
    private final AtomicReference<ServiceType> mTransitioningToService = new AtomicReference<>();
    private final AtomicBoolean mIsTransitioning = new AtomicBoolean(false);
    
    // Context and dependencies
    private Context mContext;
    private SharedEglManager mEglManager;
    private Handler mMainHandler;
    
    // Transition callback interface
    public interface TransitionCallback {
        void onTransitionStarted(ServiceType fromService, ServiceType toService);
        void onTransitionCompleted(ServiceType toService);
        void onTransitionFailed(ServiceType fromService, ServiceType toService, String error);
        void onBlankFrameRequired(boolean showTimeOverlay);
    }
    
    private TransitionCallback mTransitionCallback;
    
    /**
     * Service state tracking
     */
    private static class ServiceState {
        final ServiceType type;
        final WeakReference<BaseBackgroundService> serviceRef;
        boolean isReady;
        boolean isActive;
        SurfaceTexture lastSurfaceTexture;
        int lastWidth, lastHeight;
        long lastActivityTime;
        
        ServiceState(ServiceType type, BaseBackgroundService service) {
            this.type = type;
            this.serviceRef = new WeakReference<>(service);
            this.isReady = false;
            this.isActive = false;
            this.lastActivityTime = System.currentTimeMillis();
        }
        
        BaseBackgroundService getService() {
            return serviceRef.get();
        }
        
        boolean isServiceAlive() {
            return serviceRef.get() != null;
        }
        
        void updateActivity() {
            this.lastActivityTime = System.currentTimeMillis();
        }
    }
    
    private ServiceTransitionManager() {
        mMainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Get the singleton instance
     */
    public static ServiceTransitionManager getInstance() {
        if (sInstance == null) {
            synchronized (sLock) {
                if (sInstance == null) {
                    sInstance = new ServiceTransitionManager();
                }
            }
        }
        return sInstance;
    }
    
    /**
     * Initialize the transition manager
     */
    public void initialize(Context context, SharedEglManager eglManager) {
        mContext = context.getApplicationContext();
        mEglManager = eglManager;
        Log.d(TAG, "ServiceTransitionManager initialized");
    }
    
    /**
     * Set the transition callback
     */
    public void setTransitionCallback(TransitionCallback callback) {
        mTransitionCallback = callback;
    }
    
    /**
     * Register a service with the transition manager
     */
    public void registerService(ServiceType serviceType, BaseBackgroundService service) {
        ServiceState state = new ServiceState(serviceType, service);
        mServiceStates.put(serviceType, state);
        Log.d(TAG, "Registered service: " + serviceType);
    }
    
    /**
     * Unregister a service from the transition manager
     */
    public void unregisterService(ServiceType serviceType) {
        ServiceState state = mServiceStates.remove(serviceType);
        if (state != null) {
            Log.d(TAG, "Unregistered service: " + serviceType);
            
            // If this was the active service, clear it
            if (serviceType.equals(mCurrentActiveService.get())) {
                mCurrentActiveService.set(null);
            }
        }
    }
    
    /**
     * Transition to a new service with minimal loading time and no blank screens
     */
    public void transitionToService(ServiceType newServiceType) {
        if (mIsTransitioning.get()) {
            Log.w(TAG, "Transition already in progress, ignoring request for: " + newServiceType);
            return;
        }
        
        ServiceType currentService = mCurrentActiveService.get();
        if (newServiceType.equals(currentService)) {
            Log.d(TAG, "Already using service: " + newServiceType);
            return;
        }
        
        if (!mIsTransitioning.compareAndSet(false, true)) {
            Log.w(TAG, "Failed to acquire transition lock");
            return;
        }
        
        mTransitioningToService.set(newServiceType);
        
        Log.d(TAG, "Starting transition from " + currentService + " to " + newServiceType);
        
        // Notify callback
        if (mTransitionCallback != null) {
            mTransitionCallback.onTransitionStarted(currentService, newServiceType);
        }
        
        // Perform the transition
        performServiceTransition(currentService, newServiceType);
    }
    
    /**
     * Perform the actual service transition
     */
    private void performServiceTransition(ServiceType fromService, ServiceType toService) {
        try {
            // Step 1: Prepare the new service
            ServiceState newServiceState = mServiceStates.get(toService);
            if (newServiceState == null || !newServiceState.isServiceAlive()) {
                throw new RuntimeException("Target service not available: " + toService);
            }
            
            // Step 2: Show blank frame with time overlay if needed
            boolean needsBlankFrame = shouldShowBlankFrame(fromService, toService);
            if (needsBlankFrame && mTransitionCallback != null) {
                mTransitionCallback.onBlankFrameRequired(true);
            }
            
            // Step 3: Update EGL manager service registration
            if (mEglManager != null) {
                mEglManager.switchActiveService(toService);
            }
            
            // Step 4: Update surface configuration for new service
            updateSurfaceForService(toService);
            
            // Step 5: Deactivate old service (if any)
            if (fromService != null) {
                deactivateService(fromService);
            }
            
            // Step 6: Activate new service
            activateService(toService);
            
            // Step 7: Update active service reference
            mCurrentActiveService.set(toService);
            newServiceState.isActive = true;
            newServiceState.updateActivity();
            
            // Step 8: Hide blank frame overlay
            if (needsBlankFrame && mTransitionCallback != null) {
                mMainHandler.postDelayed(() -> {
                    if (mTransitionCallback != null) {
                        mTransitionCallback.onBlankFrameRequired(false);
                    }
                }, BLANK_FRAME_OVERLAY_DURATION_MS);
            }
            
            Log.d(TAG, "Service transition completed successfully: " + toService);
            
            // Notify callback
            if (mTransitionCallback != null) {
                mTransitionCallback.onTransitionCompleted(toService);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Service transition failed", e);
            
            // Notify callback of failure
            if (mTransitionCallback != null) {
                mTransitionCallback.onTransitionFailed(fromService, toService, e.getMessage());
            }
        } finally {
            // Always reset transition state
            mIsTransitioning.set(false);
            mTransitioningToService.set(null);
        }
    }
    
    /**
     * Determine if a blank frame should be shown during transition
     */
    private boolean shouldShowBlankFrame(ServiceType fromService, ServiceType toService) {
        // Show blank frame for transitions that might take time
        if (fromService == null) {
            return false; // First service activation
        }
        
        // Show blank frame for camera switches or cast transitions
        return (fromService == ServiceType.BgCameraFront && toService == ServiceType.BgCameraRear) ||
               (fromService == ServiceType.BgCameraRear && toService == ServiceType.BgCameraFront) ||
               (fromService == ServiceType.BgScreenCast) ||
               (toService == ServiceType.BgScreenCast);
    }
    
    /**
     * Update surface configuration for the specified service
     */
    private void updateSurfaceForService(ServiceType serviceType) {
        ServiceState state = mServiceStates.get(serviceType);
        if (state == null || !state.isServiceAlive()) {
            Log.w(TAG, "Cannot update surface for unavailable service: " + serviceType);
            return;
        }
        
        BaseBackgroundService service = state.getService();
        if (service != null && mEglManager != null) {
            // Use the last known surface configuration or get current one
            if (state.lastSurfaceTexture != null) {
                mEglManager.updateActiveServiceSurface(
                    state.lastSurfaceTexture, 
                    state.lastWidth, 
                    state.lastHeight
                );
            }
        }
    }
    
    /**
     * Activate a service
     */
    private void activateService(ServiceType serviceType) {
        ServiceState state = mServiceStates.get(serviceType);
        if (state != null && state.isServiceAlive()) {
            BaseBackgroundService service = state.getService();
            if (service != null) {
                // Notify service that it's becoming active
                // This could involve resuming camera, updating UI, etc.
                state.isActive = true;
                state.updateActivity();
                Log.d(TAG, "Activated service: " + serviceType);
            }
        }
    }
    
    /**
     * Deactivate a service
     */
    private void deactivateService(ServiceType serviceType) {
        ServiceState state = mServiceStates.get(serviceType);
        if (state != null && state.isServiceAlive()) {
            BaseBackgroundService service = state.getService();
            if (service != null) {
                // Notify service that it's being deactivated
                // This could involve pausing camera, hiding UI, etc.
                state.isActive = false;
                Log.d(TAG, "Deactivated service: " + serviceType);
            }
        }
    }
    
    /**
     * Update surface texture for a service
     */
    public void updateServiceSurface(ServiceType serviceType, SurfaceTexture surfaceTexture, int width, int height) {
        ServiceState state = mServiceStates.get(serviceType);
        if (state != null) {
            state.lastSurfaceTexture = surfaceTexture;
            state.lastWidth = width;
            state.lastHeight = height;
            state.updateActivity();
            
            // If this is the active service, update the EGL manager immediately
            if (serviceType.equals(mCurrentActiveService.get()) && mEglManager != null) {
                mEglManager.updateActiveServiceSurface(surfaceTexture, width, height);
            }
        }
    }
    
    /**
     * Mark a service as ready
     */
    public void markServiceReady(ServiceType serviceType) {
        ServiceState state = mServiceStates.get(serviceType);
        if (state != null) {
            state.isReady = true;
            state.updateActivity();
            Log.d(TAG, "Service marked as ready: " + serviceType);
        }
    }
    
    /**
     * Check if a service is ready
     */
    public boolean isServiceReady(ServiceType serviceType) {
        ServiceState state = mServiceStates.get(serviceType);
        return state != null && state.isReady && state.isServiceAlive();
    }
    
    /**
     * Get the currently active service
     */
    public ServiceType getCurrentActiveService() {
        return mCurrentActiveService.get();
    }
    
    /**
     * Check if a transition is in progress
     */
    public boolean isTransitioning() {
        return mIsTransitioning.get();
    }
    
    /**
     * Clean up dead service references
     */
    public void cleanupDeadServices() {
        mServiceStates.entrySet().removeIf(entry -> !entry.getValue().isServiceAlive());
    }
    
    /**
     * Destroy the transition manager and clean up resources
     */
    public void destroy() {
        Log.d(TAG, "Destroying ServiceTransitionManager");
        
        mServiceStates.clear();
        mCurrentActiveService.set(null);
        mTransitioningToService.set(null);
        mIsTransitioning.set(false);
        mTransitionCallback = null;
        mContext = null;
        mEglManager = null;
    }
}