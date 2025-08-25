package com.checkmate.android.service.SharedEGL;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import com.checkmate.android.service.BaseBackgroundService;
import com.checkmate.android.service.BgAudioService;
import com.checkmate.android.service.BgCameraService;
import com.checkmate.android.service.BgCastService;
import com.checkmate.android.service.BgUSBService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages seamless transitions between services without stopping streams or showing blank screens.
 */
public class SeamlessTransitionManager {
    private static final String TAG = "SeamlessTransitionManager";
    private static final int TRANSITION_TIMEOUT_MS = 3000;
    private static final int PRELOAD_DELAY_MS = 100;
    
    private static volatile SeamlessTransitionManager sInstance;
    private final Context mContext;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private HandlerThread mTransitionThread;
    private Handler mTransitionHandler;
    
    private ServiceType mCurrentService = null;
    private ServiceType mPendingService = null;
    private BaseBackgroundService mCurrentServiceInstance = null;
    private BaseBackgroundService mPendingServiceInstance = null;
    
    private final AtomicBoolean mIsTransitioning = new AtomicBoolean(false);
    private final Object mTransitionLock = new Object();
    
    // Blank frame manager
    private final BlankFrameManager mBlankFrameManager;
    
    public interface TransitionListener {
        void onTransitionStart(ServiceType from, ServiceType to);
        void onTransitionProgress(float progress);
        void onTransitionComplete(ServiceType newService);
        void onTransitionError(String error);
    }
    
    private TransitionListener mTransitionListener;
    
    private SeamlessTransitionManager(Context context) {
        mContext = context.getApplicationContext();
        mBlankFrameManager = new BlankFrameManager(context);
        initTransitionThread();
    }
    
    public static SeamlessTransitionManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (SeamlessTransitionManager.class) {
                if (sInstance == null) {
                    sInstance = new SeamlessTransitionManager(context);
                }
            }
        }
        return sInstance;
    }
    
    private void initTransitionThread() {
        mTransitionThread = new HandlerThread("TransitionThread");
        mTransitionThread.start();
        mTransitionHandler = new Handler(mTransitionThread.getLooper());
    }
    
    /**
     * Set the transition listener.
     */
    public void setTransitionListener(TransitionListener listener) {
        mTransitionListener = listener;
    }
    
    /**
     * Perform a seamless transition to a new service.
     */
    public void transitionToService(ServiceType newService, SurfaceTexture surfaceTexture, 
                                    int width, int height) {
        synchronized (mTransitionLock) {
            if (mIsTransitioning.get()) {
                Log.w(TAG, "Transition already in progress");
                return;
            }
            
            if (newService == mCurrentService) {
                Log.d(TAG, "Already on service: " + newService);
                return;
            }
            
            mIsTransitioning.set(true);
            mPendingService = newService;
            
            mTransitionHandler.post(() -> {
                performTransition(newService, surfaceTexture, width, height);
            });
        }
    }
    
    private void performTransition(ServiceType newService, SurfaceTexture surfaceTexture,
                                   int width, int height) {
        Log.d(TAG, "Starting transition from " + mCurrentService + " to " + newService);
        
        // Notify listener
        if (mTransitionListener != null) {
            mMainHandler.post(() -> 
                mTransitionListener.onTransitionStart(mCurrentService, newService));
        }
        
        try {
            // Step 1: Preload the new service
            updateProgress(0.1f);
            BaseBackgroundService newServiceInstance = preloadService(newService);
            if (newServiceInstance == null) {
                throw new Exception("Failed to preload service: " + newService);
            }
            
            // Step 2: Prepare blank frame if needed
            updateProgress(0.2f);
            boolean needsBlankFrame = shouldUseBlankFrame(mCurrentService, newService);
            if (needsBlankFrame) {
                mBlankFrameManager.startBlankFrames(surfaceTexture, width, height);
            }
            
            // Step 3: Initialize new service with shared EGL context
            updateProgress(0.3f);
            initializeNewService(newServiceInstance, surfaceTexture, width, height);
            
            // Step 4: Transfer active streams
            updateProgress(0.5f);
            transferActiveStreams(mCurrentServiceInstance, newServiceInstance);
            
            // Step 5: Switch surface rendering
            updateProgress(0.7f);
            switchSurfaceRendering(newServiceInstance, surfaceTexture);
            
            // Step 6: Stop old service (if not needed)
            updateProgress(0.9f);
            if (mCurrentServiceInstance != null && !StreamStateManager.getInstance().hasActiveStreams()) {
                stopService(mCurrentService);
            }
            
            // Step 7: Complete transition
            mCurrentService = newService;
            mCurrentServiceInstance = newServiceInstance;
            mPendingService = null;
            mPendingServiceInstance = null;
            
            // Stop blank frames
            if (needsBlankFrame) {
                mBlankFrameManager.stopBlankFrames();
            }
            
            updateProgress(1.0f);
            
            // Notify completion
            if (mTransitionListener != null) {
                mMainHandler.post(() -> 
                    mTransitionListener.onTransitionComplete(newService));
            }
            
            Log.d(TAG, "Transition completed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Transition failed", e);
            
            // Stop blank frames on error
            mBlankFrameManager.stopBlankFrames();
            
            // Notify error
            if (mTransitionListener != null) {
                mMainHandler.post(() -> 
                    mTransitionListener.onTransitionError(e.getMessage()));
            }
            
        } finally {
            mIsTransitioning.set(false);
        }
    }
    
    private BaseBackgroundService preloadService(ServiceType serviceType) {
        Log.d(TAG, "Preloading service: " + serviceType);
        
        // Get existing instance from SharedEglManager
        SharedEglManager eglManager = SharedEglManager.getInstance();
        BaseBackgroundService existingInstance = eglManager.getServiceInstance(serviceType);
        
        if (existingInstance != null) {
            Log.d(TAG, "Using existing service instance");
            return existingInstance;
        }
        
        // Start the service if not running
        Class<?> serviceClass = getServiceClass(serviceType);
        if (serviceClass == null) {
            Log.e(TAG, "Unknown service type: " + serviceType);
            return null;
        }
        
        // Use ServiceSwitcher to start the service
        ServiceSwitcher.switchService(mContext, serviceType);
        
        // Wait for service to be registered
        for (int i = 0; i < 10; i++) {
            existingInstance = eglManager.getServiceInstance(serviceType);
            if (existingInstance != null) {
                Log.d(TAG, "Service preloaded successfully");
                return existingInstance;
            }
            
            try {
                Thread.sleep(PRELOAD_DELAY_MS);
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while waiting for service", e);
                return null;
            }
        }
        
        Log.e(TAG, "Timeout waiting for service to start");
        return null;
    }
    
    private void initializeNewService(BaseBackgroundService service, 
                                      SurfaceTexture surfaceTexture,
                                      int width, int height) {
        Log.d(TAG, "Initializing new service");
        
        if (service == null) {
            Log.e(TAG, "Service is null");
            return;
        }
        
        // Activate the service with the surface
        service.activateService(surfaceTexture, width, height);
    }
    
    private void transferActiveStreams(BaseBackgroundService oldService,
                                       BaseBackgroundService newService) {
        Log.d(TAG, "Transferring active streams");
        
        StreamStateManager stateManager = StreamStateManager.getInstance();
        
        if (!stateManager.hasActiveStreams()) {
            Log.d(TAG, "No active streams to transfer");
            return;
        }
        
        // The SharedEglManager maintains the streams, so we just need to
        // ensure the new service is aware of the active state
        if (stateManager.isStreaming()) {
            Log.d(TAG, "Maintaining active stream");
        }
        
        if (stateManager.isRecording()) {
            Log.d(TAG, "Maintaining active recording");
        }
    }
    
    private void switchSurfaceRendering(BaseBackgroundService newService,
                                        SurfaceTexture surfaceTexture) {
        Log.d(TAG, "Switching surface rendering to new service");
        
        // The new service should already be rendering to the surface
        // through the activateService call
    }
    
    private void stopService(ServiceType serviceType) {
        Log.d(TAG, "Stopping service: " + serviceType);
        
        // Only stop if no streams are active
        if (StreamStateManager.getInstance().hasActiveStreams()) {
            Log.d(TAG, "Not stopping service - streams are active");
            return;
        }
        
        // Stop through MainActivity's service management
        if (mCurrentServiceInstance != null) {
            mCurrentServiceInstance.stopService(serviceType);
        }
    }
    
    private boolean shouldUseBlankFrame(ServiceType from, ServiceType to) {
        // Use blank frames for transitions that involve different capture sources
        if (from == null || to == null) {
            return false;
        }
        
        // Camera to USB or vice versa needs blank frame
        if ((from == ServiceType.BgCamera && to == ServiceType.BgUSBCamera) ||
            (from == ServiceType.BgUSBCamera && to == ServiceType.BgCamera)) {
            return true;
        }
        
        // Any transition to/from screen cast needs blank frame
        if (from == ServiceType.BgScreenCast || to == ServiceType.BgScreenCast) {
            return true;
        }
        
        // Audio transitions don't need blank frames
        if (from == ServiceType.BgAudio || to == ServiceType.BgAudio) {
            return false;
        }
        
        return false;
    }
    
    private void updateProgress(float progress) {
        if (mTransitionListener != null) {
            mMainHandler.post(() -> 
                mTransitionListener.onTransitionProgress(progress));
        }
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
     * Get the current active service.
     */
    public ServiceType getCurrentService() {
        return mCurrentService;
    }
    
    /**
     * Check if a transition is in progress.
     */
    public boolean isTransitioning() {
        return mIsTransitioning.get();
    }
    
    /**
     * Release resources.
     */
    public void release() {
        if (mTransitionThread != null) {
            mTransitionThread.quitSafely();
            mTransitionThread = null;
        }
        mTransitionHandler = null;
        mBlankFrameManager.release();
    }
}