package com.checkmate.android.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.Surface;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import com.checkmate.android.R;
import com.checkmate.android.service.SharedEGL.ServiceType;
import com.checkmate.android.service.SharedEGL.SharedEglManager;
import com.checkmate.android.viewmodels.SharedViewModel;
import java.lang.ref.WeakReference;
import toothpick.Scope;
import toothpick.Toothpick;
import toothpick.config.Module;
import javax.inject.Inject;
import android.annotation.SuppressLint;
public abstract class BaseBackgroundService extends Service {
    // Common fields
    @Inject
    public SharedEglManager mEglManager;
    protected SurfaceTexture mPreviewTexture;
    protected Surface mPreviewSurface;
    protected Handler mHandler;
    protected boolean isRunning = false;
    protected PowerManager.WakeLock wakeLock;
    protected SharedViewModel sharedViewModel;

    // Service state fields
    protected int mRotation = 0;
    protected boolean mMirror = false;
    protected boolean mFlip = false;
    protected int mSurfaceWidth = 1280; // Default dimensions
    protected int mSurfaceHeight = 720;

    protected NotificationManager nm;
    protected SurfaceTexture.OnFrameAvailableListener mFrameAvailableListener;
    protected BackgroundNotification.NOTIFICATION_STATUS mCurrentStatus;
    // Common notification constants
    protected static final String CHANNEL_ID = "BackgroundService";
    protected static final int NOTIFICATION_ID = 1000;
    // Add this to the fields section of BaseBackgroundService
    protected Intent mRunningIntent;
    protected BackgroundNotification mNotifyCallback;

    // Common interface for status notifications
    public interface BackgroundNotification {
        enum NOTIFICATION_STATUS {
            CREATED, SERVICE_STARTED, OPENED, CONNECTED, DISCONNECTED, SERVICE_CLOSED
        }
        void onStatusChange(BaseBackgroundService.BackgroundNotification.NOTIFICATION_STATUS status, String data , ServiceType serviceType);
        void stopService(ServiceType serviceType);
    }

    // Abstract method to get the service type
    protected abstract ServiceType getServiceType();

    // Common binder class
    public abstract static class ServiceBinder<T extends BaseBackgroundService> extends Binder {
        private final WeakReference<T> serviceReference;

        public ServiceBinder(T service) {
            serviceReference = new WeakReference<>(service);
        }

        public T getService() {
            return serviceReference.get();
        }
    }

    public void setNotifyCallback(BackgroundNotification callback) {
        mNotifyCallback = callback;
    }

    // Common initialization
    @Override
    public void onCreate() {
        super.onCreate();
        nm = getSystemService(NotificationManager.class);
        startForeground(NOTIFICATION_ID, buildNotification());
        super.onCreate();
        mHandler = new Handler(Looper.getMainLooper());
        Scope serviceScope = Toothpick.openScope(this)
                .installModules(new Module() {{
                    bind(SharedEglManager.class)
                            .to(SharedEglManager.class)
                            .singleton();
                }});
        Toothpick.inject(this, serviceScope);
        
        // Get the singleton instance and register this service
        mEglManager = SharedEglManager.getInstance();
        
        // Register this service with the shared EGL manager
        mEglManager.registerService(getServiceType(), this);
    }

    // Add this method to provide common streaming status check
    public boolean isStreaming() {
        if (mEglManager != null) {
            return mEglManager.isStreaming();
        }
        return false;
    }

    public boolean isRecording() {
        if (mEglManager != null) {
            return mEglManager.isRecording();
        }
        return false;
    }

    // Add this getter method
    public Intent getRunningIntent() {
        return mRunningIntent;
    }
    // Common notification setup
    @SuppressLint("WrongConstant")
    protected Notification buildNotification() {
        NotificationChannel chan = new NotificationChannel(
                CHANNEL_ID,
                "Background Service",
                NotificationManager.IMPORTANCE_MIN
        );
        chan.setShowBadge(false);
        chan.setSound(null, null);
        chan.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        nm.createNotificationChannel(chan);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("")
                .setContentText("")
                .setSmallIcon(R.mipmap.ic_notif)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setTimeoutAfter(1)
                .build();
    }

    // Common foreground service setup
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent,flags,startId);
        isRunning = true;
        return START_STICKY;
    }

    // Common cleanup
    @Override
    public void onDestroy() {
        // DO NOT stop streaming/recording here!
        // Only release service-specific resources
        
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        
        // Unregister without affecting stream
        if (mEglManager != null) {
            mEglManager.unregisterService(getServiceType());
        }
        
        Toothpick.closeScope(this);
        isRunning = false;
        super.onDestroy();
    }

    // Common methods for streaming/recording
    public void startStreaming() {
        if (mEglManager != null) {
            mEglManager.startStreaming();
        }
    }

    public void stopStreaming() {
        if (mEglManager != null) {
            mEglManager.stopStreaming();
        }
    }

    public void startRecording() {
        if (mEglManager != null) {
            mEglManager.startRecording();
        }
    }

    public void stopRecording() {
        if (mEglManager != null) {
            mEglManager.stopRecording(false);
        }
    }

    public void takeSnapshot() {
        if (mEglManager != null) {
            mEglManager.takeSnapshot();
        }
    }

    // Common utility methods
    protected void showToast(final String message) {
        mHandler.post(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }

    // Public API methods
    public void setStatus(BackgroundNotification.NOTIFICATION_STATUS status) {
        if (mNotifyCallback != null) {
            mNotifyCallback.onStatusChange(status, "",ServiceType.BgCamera);
        }
        mCurrentStatus = status;
    }

    public BackgroundNotification.NOTIFICATION_STATUS getCurrentStatus() {
        return mCurrentStatus;
    }

    public void setSharedViewModel(SharedViewModel vm) {
        this.sharedViewModel = vm;
    }

    public void updateOverlay(boolean is_landscape) {
        if (mEglManager != null) {
            mEglManager.updateOverlayLayout(is_landscape);
        }
    }

    public void setPreviewSurface(SurfaceTexture surface, int width, int height) {
        mPreviewTexture = surface;
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        if (mEglManager != null) {
            mEglManager.setPreviewSurface(surface, width, height);
        }
    }

    /**
     * Update the preview surface for this service when it becomes active
     * This method should be called when the service needs to update its display surface
     * @param surface The surface texture to use for preview
     * @param width The width of the preview surface
     * @param height The height of the preview surface
     */
    public void updateServiceSurface(SurfaceTexture surface, int width, int height) {
        if (mEglManager != null && mEglManager.isServiceActive(getServiceType())) {
            mEglManager.updateActiveServiceSurface(surface, width, height);
        }
    }

    // Orientation helpers
    public void setRotation(int rotation) {
        mRotation = rotation;
        if (mEglManager != null) {
            mEglManager.setRotation(rotation);
        }
    }

    public void setMirror(boolean mirror) {
        mMirror = mirror;
        if (mEglManager != null) {
            mEglManager.setMirror(mirror);
        }
    }

    public void setFlip(boolean flip) {
        mFlip = flip;
        if (mEglManager != null) {
            mEglManager.setFlip(flip);
        }
    }

    public void setNormal() {
        mRotation = 0;
        mMirror = false;
        mFlip = false;
        if (mEglManager != null) {
            mEglManager.setNormal();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mEglManager != null) {
            mEglManager.onConfigurationChanged(newConfig);
        }
    }

    // Status getters
    public boolean isStreamerReady() {
        if (mEglManager != null) {
            return mEglManager.isStreamerReady();
        }
        return false;
    }


    public boolean isRunningForeground() {
        return isRunning;
    }

    private void cancelNotification() {
        nm.cancel(R.string.camera_running);
    }

    // Add these getters for service state
    public int getRotation() {
        return mRotation; // Maintain rotation state in each service
    }
    
    public boolean getMirrorState() {
        return mMirror; // Maintain mirror state in each service
    }
    
    public boolean getFlipState() {
        return mFlip; // Maintain flip state in each service
    }

    public SurfaceTexture getSurfaceTexture() {
        return mPreviewTexture;
    }

    public int getSurfaceWidth() {
        return mSurfaceWidth; // Track in your service
    }

    public int getSurfaceHeight() {
        return mSurfaceHeight; // Track in your service
    }

    // Add method to switch to this service
    public void activateService(SurfaceTexture surface, int width, int height) {
        if (mEglManager != null) {
            mEglManager.switchActiveService(
                getServiceType(),
                surface,
                width,
                height
            );
        }
    }

    /**
     * Seamlessly switch to this service using StreamTransitionManager
     * This provides optimal performance with blank frame handling during transitions
     */
    public void activateServiceSeamlessly(SurfaceTexture surface, int width, int height) {
        ServiceType currentActive = null;
        if (mEglManager != null) {
            // Try to get current active service for smooth transition
            try {
                currentActive = mEglManager.getCurrentActiveService();
            } catch (Exception e) {
                // If no current service, proceed with direct activation
            }
        }
        
        // Use StreamTransitionManager for seamless transition
        StreamTransitionManager transitionManager = StreamTransitionManager.getInstance();
        transitionManager.switchService(
            currentActive,
            getServiceType(),
            surface,
            width,
            height
        );
    }

    /**
     * Update surface configuration dynamically without stopping streams
     */
    public void updateSurfaceConfiguration(int width, int height) {
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        
        if (mPreviewTexture != null) {
            mPreviewTexture.setDefaultBufferSize(width, height);
        }
        
        // Update SharedEglManager with new surface dimensions
        if (mEglManager != null) {
            mEglManager.updateActiveSurface(mPreviewTexture, width, height);
        }
    }

    /**
     * Update configuration dynamically using StreamTransitionManager
     */
    public void updateDynamicConfiguration(String configKey, Object configValue) {
        StreamTransitionManager transitionManager = StreamTransitionManager.getInstance();
        transitionManager.updateConfiguration(configKey, configValue);
    }

    /**
     * Prepare surface for optimal performance
     */
    protected void prepareSurfaceForOptimalPerformance() {
        if (mPreviewTexture == null) {
            Log.w("BaseBackgroundService", "Cannot prepare surface - preview texture is null");
            return;
        }
        
        try {
            // Set optimal buffer size
            mPreviewTexture.setDefaultBufferSize(mSurfaceWidth, mSurfaceHeight);
            
            // Create surface if needed
            if (mPreviewSurface == null) {
                mPreviewSurface = new Surface(mPreviewTexture);
            }
            
            // Set frame available listener for performance monitoring
            if (mFrameAvailableListener == null) {
                mFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        // Notify SharedEglManager that a new frame is available
                        if (mEglManager != null) {
                            mEglManager.requestRender();
                        }
                    }
                };
                mPreviewTexture.setOnFrameAvailableListener(mFrameAvailableListener);
            }
            
            Log.d("BaseBackgroundService", "Surface prepared for optimal performance: " + 
                  mSurfaceWidth + "x" + mSurfaceHeight);
            
        } catch (Exception e) {
            Log.e("BaseBackgroundService", "Failed to prepare surface for optimal performance", e);
        }
    }

    /**
     * Check if this service can handle seamless transitions
     */
    public boolean supportsSeamlessTransitions() {
        return mEglManager != null && mEglManager.isInitialized();
    }

    /**
     * Get optimal surface dimensions based on service type
     */
    protected void calculateOptimalSurfaceDimensions() {
        // Default implementation - services can override for specific needs
        ServiceType serviceType = getServiceType();
        
        switch (serviceType) {
            case BgCamera:
            case BgUSBCamera:
                // For cameras, use high resolution by default
                mSurfaceWidth = 1920;
                mSurfaceHeight = 1080;
                break;
                
            case BgScreenCast:
                // For screen casting, match device resolution
                // This would typically get actual screen dimensions
                mSurfaceWidth = 1280;
                mSurfaceHeight = 720;
                break;
                
            case BgAudio:
                // Audio service doesn't need video surface
                mSurfaceWidth = 640;
                mSurfaceHeight = 480;
                break;
                
            default:
                // Default HD resolution
                mSurfaceWidth = 1280;
                mSurfaceHeight = 720;
                break;
        }
        
        Log.d("BaseBackgroundService", "Calculated optimal surface dimensions for " + 
              serviceType + ": " + mSurfaceWidth + "x" + mSurfaceHeight);
    }

    /**
     * Initialize surface with optimal settings
     */
    protected void initializeOptimalSurface() {
        calculateOptimalSurfaceDimensions();
        prepareSurfaceForOptimalPerformance();
        
        // Ensure SharedEglManager has streamers ready
        if (mEglManager != null) {
            mEglManager.ensureStreamersCreated();
        }
    }

    /**
     * Clean up surface resources properly
     */
    protected void cleanupSurfaceResources() {
        try {
            if (mPreviewTexture != null) {
                mPreviewTexture.setOnFrameAvailableListener(null);
                mPreviewTexture.release();
                mPreviewTexture = null;
            }
            
            if (mPreviewSurface != null) {
                mPreviewSurface.release();
                mPreviewSurface = null;
            }
            
            mFrameAvailableListener = null;
            
            Log.d("BaseBackgroundService", "Surface resources cleaned up for service: " + getServiceType());
            
        } catch (Exception e) {
            Log.e("BaseBackgroundService", "Error cleaning up surface resources", e);
        }
    }

    @Override
    public void onDestroy() {
        cleanupSurfaceResources();
        super.onDestroy();
    }
}
