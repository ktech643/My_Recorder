package com.checkmate.android.service;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import com.checkmate.android.service.SharedEGL.ServiceType;
import com.checkmate.android.service.SharedEGL.SharedEglManager;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages seamless transitions between services without interrupting streams
 */
public class ServiceTransitionManager {
    private static final String TAG = "ServiceTransitionManager";
    private static final int TRANSITION_FRAME_COUNT = 5;
    private static final int FRAME_INTERVAL_MS = 33; // ~30fps
    private static final int STABILIZATION_DELAY_MS = 100;
    
    private final Context context;
    private final Handler transitionHandler;
    private final HandlerThread transitionThread;
    private final AtomicBoolean isTransitioning = new AtomicBoolean(false);
    private final AtomicReference<ServiceType> currentServiceType = new AtomicReference<>();
    private final AtomicReference<ServiceType> targetServiceType = new AtomicReference<>();
    
    public interface TransitionCallback {
        void onTransitionStart(ServiceType from, ServiceType to);
        void onTransitionProgress(int frame, int totalFrames);
        void onTransitionComplete(ServiceType newService);
        void onTransitionError(String error);
    }
    
    public ServiceTransitionManager(Context context) {
        this.context = context.getApplicationContext();
        this.transitionThread = new HandlerThread("ServiceTransition");
        this.transitionThread.start();
        this.transitionHandler = new Handler(transitionThread.getLooper());
    }
    
    /**
     * Perform seamless transition to a new service
     * @param toServiceType Target service type
     * @param callback Transition callback
     */
    public void transitionTo(ServiceType toServiceType, TransitionCallback callback) {
        if (isTransitioning.get()) {
            Log.w(TAG, "Transition already in progress");
            callback.onTransitionError("Transition already in progress");
            return;
        }
        
        ServiceType fromServiceType = currentServiceType.get();
        if (fromServiceType == toServiceType) {
            Log.d(TAG, "Already on service: " + toServiceType);
            callback.onTransitionComplete(toServiceType);
            return;
        }
        
        isTransitioning.set(true);
        targetServiceType.set(toServiceType);
        
        transitionHandler.post(() -> {
            try {
                performTransition(fromServiceType, toServiceType, callback);
            } catch (Exception e) {
                Log.e(TAG, "Transition failed", e);
                callback.onTransitionError(e.getMessage());
            } finally {
                isTransitioning.set(false);
            }
        });
    }
    
    private void performTransition(ServiceType from, ServiceType to, TransitionCallback callback) {
        Log.d(TAG, "Starting transition from " + from + " to " + to);
        callback.onTransitionStart(from, to);
        
        SharedEglManager eglManager = SharedEglManager.getInstance();
        boolean isStreaming = eglManager.isStreaming();
        boolean isRecording = eglManager.isRecording();
        
        if (!isStreaming && !isRecording) {
            // No active stream, perform direct switch
            performDirectSwitch(to, callback);
        } else {
            // Active stream, perform seamless transition
            performSeamlessTransition(from, to, callback);
        }
    }
    
    private void performDirectSwitch(ServiceType to, TransitionCallback callback) {
        Log.d(TAG, "Performing direct switch to " + to);
        
        // Stop current service if any
        ServiceType current = currentServiceType.get();
        if (current != null) {
            stopService(current);
        }
        
        // Start new service
        startService(to);
        currentServiceType.set(to);
        
        callback.onTransitionComplete(to);
    }
    
    private void performSeamlessTransition(ServiceType from, ServiceType to, TransitionCallback callback) {
        Log.d(TAG, "Performing seamless transition from " + from + " to " + to);
        
        SharedEglManager eglManager = SharedEglManager.getInstance();
        
        // Phase 1: Prepare transition
        BaseBackgroundService newService = getOrCreateService(to);
        if (newService == null) {
            callback.onTransitionError("Failed to create service: " + to);
            return;
        }
        
        // Phase 2: Draw transition frames
        for (int i = 0; i < TRANSITION_FRAME_COUNT; i++) {
            drawTransitionFrame(i, TRANSITION_FRAME_COUNT);
            callback.onTransitionProgress(i + 1, TRANSITION_FRAME_COUNT);
            
            try {
                Thread.sleep(FRAME_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // Phase 3: Switch service
        eglManager.registerService(to, newService);
        
        // Phase 4: Update surface
        SurfaceTexture surfaceTexture = newService.getSurfaceTexture();
        if (surfaceTexture != null) {
            eglManager.updateActiveServiceSurface(
                surfaceTexture,
                newService.getSurfaceWidth(),
                newService.getSurfaceHeight()
            );
        }
        
        // Phase 5: Stabilization period
        try {
            Thread.sleep(STABILIZATION_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Phase 6: Cleanup old service if not streaming
        if (from != null && from != to) {
            transitionHandler.postDelayed(() -> {
                if (!eglManager.isServiceActive(from)) {
                    stopService(from);
                }
            }, 1000);
        }
        
        currentServiceType.set(to);
        callback.onTransitionComplete(to);
    }
    
    private void drawTransitionFrame(int frameIndex, int totalFrames) {
        SharedEglManager eglManager = SharedEglManager.getInstance();
        
        // Calculate transition progress
        float progress = (float) frameIndex / totalFrames;
        
        // Draw frame with transition overlay
        String message = String.format("Switching source... %d%%", (int)(progress * 100));
        eglManager.drawBlankFrameWithOverlay(message);
    }
    
    private BaseBackgroundService getOrCreateService(ServiceType type) {
        // This would get or create the appropriate service instance
        // Implementation depends on your service management architecture
        return null; // Placeholder
    }
    
    private void startService(ServiceType type) {
        // Start the appropriate service
        Log.d(TAG, "Starting service: " + type);
    }
    
    private void stopService(ServiceType type) {
        // Stop the appropriate service
        Log.d(TAG, "Stopping service: " + type);
    }
    
    /**
     * Get current active service type
     * @return Current service type or null
     */
    public ServiceType getCurrentServiceType() {
        return currentServiceType.get();
    }
    
    /**
     * Check if a transition is in progress
     * @return true if transitioning
     */
    public boolean isTransitioning() {
        return isTransitioning.get();
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        transitionThread.quitSafely();
    }
}