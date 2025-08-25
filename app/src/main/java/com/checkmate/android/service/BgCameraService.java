package com.checkmate.android.service;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Range;
import android.view.Surface;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import com.checkmate.android.AppConstant;
import com.checkmate.android.AppPreference;
import com.checkmate.android.model.SurfaceModel;
import com.checkmate.android.service.SharedEGL.ServiceType;
import com.checkmate.android.service.SharedEGL.SharedEglManager;
import com.checkmate.android.util.SettingsUtils;
import com.checkmate.android.util.InternalLogger;
import com.checkmate.android.util.ANRSafeHelper;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import androidx.annotation.NonNull;

public class BgCameraService extends BaseBackgroundService {
    private static final String TAG = "BgCameraService";
    private static final int REOPEN_DELAY_MS = 500;
    private static final int MAX_RETRIES = 5;

    // Camera-specific fields
    private CameraDevice mCamera2;
    private CameraCaptureSession mCaptureSession;
    private CameraCaptureSession.StateCallback mSessionStateCallback;
    private CameraDevice.StateCallback mCameraStateCallback;
    private final HandlerThread mCameraThread = new HandlerThread("BgCamera");
    private final Handler mCameraHandler = new Handler(Looper.getMainLooper());
    private boolean mClosing;
    private int reopenAttempts = 0;
    private final Runnable reopenRunnable = this::initCamera;
    private BroadcastReceiver configChangeReceiver;
    private boolean isConfigChangeReceiverRegistered = false;
    // Status tracking
    public static final MutableLiveData<String> liveData = new MutableLiveData<>();
    // Binder
    // Binder implementation
    public static class CameraBinder extends ServiceBinder<BgCameraService> {
        public CameraBinder(BgCameraService service) {
            super(service);
        }
    }
    
    private WeakReference<CameraBinder> mBinderRef = new WeakReference<>(new CameraBinder(this));
    
    @Override
    public IBinder onBind(Intent intent) {
        try {
            InternalLogger.d(TAG, "BgCameraService onBind called");
            
            CameraBinder binder = mBinderRef.get();
            if (binder == null) {
                InternalLogger.w(TAG, "Binder reference was null, creating new one");
                binder = new CameraBinder(this);
                mBinderRef = new WeakReference<>(binder);
            }
            
            return binder;
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error in onBind", e);
            // Return a new binder as fallback
            return new CameraBinder(this);
        }
    }

    @Inject
    public BgCameraService() {
        // Constructor logic
    }

    @Override
    protected ServiceType getServiceType() {
        return ServiceType.BgCamera;
    }

    @Override
    public void onCreate() {
        try {
            InternalLogger.i(TAG, "BgCameraService onCreate starting");
            
            super.onCreate();
            mCurrentStatus = BackgroundNotification.NOTIFICATION_STATUS.CREATED;
            
            // Initialize wake lock with error handling
            initializeWakeLockSafely();
            
            // Start camera thread safely
            initializeCameraThreadSafely();
            
            // Initialize EGL manager with error recovery
            initializeEglManagerSafely();
            
            InternalLogger.i(TAG, "BgCameraService onCreate completed successfully");
            
        } catch (Exception e) {
            InternalLogger.e(TAG, "Critical error in BgCameraService onCreate", e);
            handleServiceStartupError(e);
        }
    }
    
    /**
     * Initialize wake lock with error handling
     */
    private void initializeWakeLockSafely() {
        ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
            try {
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                if (ANRSafeHelper.isNullWithLog(pm, "PowerManager")) {
                    InternalLogger.e(TAG, "PowerManager is null, cannot create wake lock");
                    return false;
                }
                
                String wakeLockTag = "CheckMate:CameraLock";
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag);
                
                if (wakeLock != null) {
                    wakeLock.acquire(60 * 60 * 2000); // 2 hours max
                    InternalLogger.d(TAG, "Wake lock acquired successfully");
                } else {
                    InternalLogger.e(TAG, "Failed to create wake lock");
                }
                
                return true;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error initializing wake lock", e);
                return false;
            }
        }, false);
    }
    
    /**
     * Initialize camera thread safely
     */
    private void initializeCameraThreadSafely() {
        ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
            try {
                if (mCameraThread != null && !mCameraThread.isAlive()) {
                    mCameraThread.start();
                    InternalLogger.d(TAG, "Camera thread started successfully");
                } else {
                    InternalLogger.w(TAG, "Camera thread already running or null");
                }
                return true;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error starting camera thread", e);
                return false;
            }
        }, false);
    }
    
    /**
     * Initialize EGL manager with comprehensive error handling
     */
    private void initializeEglManagerSafely() {
        ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
            try {
                // Clean and reset EGL manager asynchronously
                SharedEglManager.cleanAndResetAsync(() -> {
                    ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
                        try {
                            mEglManager = SharedEglManager.getInstance();
                            if (mEglManager != null) {
                                Context appContext = getApplicationContext();
                                if (ANRSafeHelper.isContextValid(appContext)) {
                                    mEglManager.initialize(appContext, ServiceType.BgCamera);
                                    InternalLogger.d(TAG, "EGL manager initialized successfully");
                                } else {
                                    InternalLogger.e(TAG, "Invalid application context for EGL initialization");
                                }
                            } else {
                                InternalLogger.e(TAG, "Failed to get EGL manager instance");
                            }
                            return true;
                        } catch (Exception e) {
                            InternalLogger.e(TAG, "Error in EGL manager initialization callback", e);
                            return false;
                        }
                    }, false);
                });
                return true;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error setting up EGL manager initialization", e);
                return false;
            }
        }, false);
        
        // Set up EGL listener safely
        setupEglListenerSafely();
    }
    
    /**
     * Set up EGL listener with comprehensive error handling
     */
    private void setupEglListenerSafely() {
        ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
            try {
                if (mEglManager != null) {
                    mEglManager.setListener(new SharedEglManager.Listener() {
                        @Override
                        public void onEglReady() {
                            handleEglReadySafely();
                        }
                        
                        private void drawFrame() {
                            drawFrameSafely();
                        }
                    });
                    InternalLogger.d(TAG, "EGL listener set successfully");
                } else {
                    InternalLogger.w(TAG, "EGL manager is null, cannot set listener");
                }
                return true;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error setting up EGL listener", e);
                return false;
            }
        }, false);
    }
    
    /**
     * Handle EGL ready event with comprehensive error handling
     */
    private void handleEglReadySafely() {
        ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
            try {
                if (ANRSafeHelper.isNullWithLog(mEglManager, "mEglManager in onEglReady")) {
                    return false;
                }
                
                // Check if preview texture already exists
                if (mPreviewTexture != null) {
                    InternalLogger.d(TAG, "Preview texture already exists, skipping creation");
                    return true;
                }
                
                // Get camera texture safely
                mPreviewTexture = mEglManager.getCameraTexture();
                if (ANRSafeHelper.isNullWithLog(mPreviewTexture, "Camera texture from EglManager")) {
                    InternalLogger.e(TAG, "Failed to get camera texture from EglManager");
                    return false;
                }
                
                // Create preview surface
                mPreviewSurface = new Surface(mPreviewTexture);
                mPreviewTexture.setDefaultBufferSize(1280, 720);
                InternalLogger.d(TAG, "Preview surface created and buffer size set");
                
                // Set up shared view model safely
                setupSharedViewModelSafely();
                
                // Set up frame available listener
                setupFrameAvailableListenerSafely();
                
                // Initialize camera
                ANRSafeHelper.getInstance().postToMainThreadSafely(() -> {
                    initCameraSafely();
                });
                
                return true;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error in handleEglReadySafely", e);
                return false;
            }
        }, false);
    }
    
    /**
     * Set up shared view model safely
     */
    private void setupSharedViewModelSafely() {
        try {
            if (sharedViewModel != null) {
                SurfaceModel surfaceModel = sharedViewModel.getSurfaceModel();
                if (surfaceModel != null) {
                    SurfaceTexture dsurfaceTexture = surfaceModel.getSurfaceTexture();
                    if (dsurfaceTexture != null) {
                        int dwidth = surfaceModel.getWidth();
                        int dheight = surfaceModel.getHeight();
                        mEglManager.setPreviewSurface(dsurfaceTexture, dwidth, dheight);
                        InternalLogger.d(TAG, "Preview surface set in EGL manager");
                    } else {
                        InternalLogger.w(TAG, "SurfaceTexture not created yet, skipping preview surface setup");
                    }
                } else {
                    InternalLogger.w(TAG, "SurfaceModel is null");
                }
            } else {
                InternalLogger.w(TAG, "SharedViewModel is null");
            }
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error setting up shared view model", e);
        }
    }
    
    /**
     * Set up frame available listener safely
     */
    private void setupFrameAvailableListenerSafely() {
        try {
            mFrameAvailableListener = surfaceTexture -> {
                ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
                    try {
                        if (mEglManager != null && mEglManager.getHandler() != null) {
                            mEglManager.getHandler().post(this::drawFrameSafely);
                        }
                        return true;
                    } catch (Exception e) {
                        InternalLogger.e(TAG, "Error in frame available listener", e);
                        return false;
                    }
                }, false);
            };
            
            if (mPreviewTexture != null) {
                mPreviewTexture.setOnFrameAvailableListener(mFrameAvailableListener);
                InternalLogger.d(TAG, "Frame available listener set successfully");
            }
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error setting up frame available listener", e);
        }
    }
    
    /**
     * Draw frame safely with error handling
     */
    private void drawFrameSafely() {
        ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
            try {
                if (mPreviewTexture != null) {
                    try {
                        mPreviewTexture.updateTexImage();
                        float[] tx = new float[16];
                        mPreviewTexture.getTransformMatrix(tx);
                    } catch (Exception e) {
                        InternalLogger.e(TAG, "Failed to updateTexImage()", e);
                        return false;
                    }
                }
                
                if (mEglManager != null) {
                    mEglManager.drawFrame();
                }
                
                return true;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error in drawFrameSafely", e);
                return false;
            }
        }, false);
    }
    
    /**
     * Initialize camera safely
     */
    private void initCameraSafely() {
        ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
            try {
                initCamera();
                return true;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error in initCameraSafely", e);
                // Attempt recovery
                handleCameraInitError(e);
                return false;
            }
        }, false);
    }
    
    /**
     * Handle camera initialization errors with recovery strategies
     */
    private void handleCameraInitError(Exception error) {
        InternalLogger.w(TAG, "Attempting camera initialization recovery");
        
        // Increment retry count and check if we should retry
        reopenAttempts++;
        if (reopenAttempts < MAX_RETRIES) {
            InternalLogger.i(TAG, "Scheduling camera init retry, attempt: " + reopenAttempts);
            mCameraHandler.postDelayed(() -> {
                initCameraSafely();
            }, REOPEN_DELAY_MS * reopenAttempts); // Progressive delay
        } else {
            InternalLogger.e(TAG, "Max camera init retries reached, giving up");
            reopenAttempts = 0;
        }
    }
    
    /**
     * Handle service startup errors
     */
    private void handleServiceStartupError(Exception error) {
        InternalLogger.e(TAG, "Service startup failed, attempting graceful degradation");
        
        try {
            // Update status to indicate error
            setStatus(BackgroundNotification.NOTIFICATION_STATUS.ERROR);
            
            // Try to clean up any partially initialized components
            cleanupPartialInitialization();
            
        } catch (Exception cleanupError) {
            InternalLogger.e(TAG, "Error during startup error cleanup", cleanupError);
        }
    }
    
    /**
     * Clean up partially initialized components
     */
    private void cleanupPartialInitialization() {
        ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
            try {
                if (wakeLock != null && wakeLock.isHeld()) {
                    wakeLock.release();
                    wakeLock = null;
                }
                
                if (mPreviewSurface != null) {
                    mPreviewSurface.release();
                    mPreviewSurface = null;
                }
                
                if (mPreviewTexture != null) {
                    mPreviewTexture.release();
                    mPreviewTexture = null;
                }
                
                return true;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error in cleanup", e);
                return false;
            }
        }, false);

        mEglManager.setListener(new SharedEglManager.Listener() {
            @Override
            public void onEglReady() {
                if (mEglManager != null) {
                    if (mPreviewTexture != null) return;

                    mPreviewTexture = mEglManager.getCameraTexture();
                    if (mPreviewTexture == null) {
                        Log.e(TAG, "Failed to get camera texture from EglManager");
                        return;
                    }

                    mPreviewSurface = new Surface(mPreviewTexture);
                    mPreviewTexture.setDefaultBufferSize(1280, 720);
                    Log.d(TAG, "Preview surface created and buffer size set");

                    if (sharedViewModel != null) {
                        SurfaceModel surfaceModel = sharedViewModel.getSurfaceModel();
                        SurfaceTexture dsurfaceTexture = surfaceModel.getSurfaceTexture();
                        if (dsurfaceTexture == null) {
                            Log.w(TAG, "onEglReady: SurfaceTexture not created yet, skipping for now");
                            return;
                        }
                        int dwidth = surfaceModel.getWidth();
                        int dheight = surfaceModel.getHeight();
                        mEglManager.setPreviewSurface(dsurfaceTexture, dwidth, dheight);
                    }

                    mFrameAvailableListener = surfaceTexture -> {
                        if (mEglManager != null && mEglManager.getHandler() != null) {
                            mEglManager.getHandler().post(this::drawFrame);
                        }
                    };
                    mPreviewTexture.setOnFrameAvailableListener(mFrameAvailableListener);
                    initCamera();
                }
            }

            private void drawFrame() {
                if (mPreviewTexture != null) {
                    try {
                        mPreviewTexture.updateTexImage();
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to updateTexImage()", e);
                        return;
                    }
                    float[] tx = new float[16];
                    mPreviewTexture.getTransformMatrix(tx);
                }
                if (mEglManager != null) {
                    mEglManager.drawFrame();
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            InternalLogger.d(TAG, "BgCameraService onStartCommand, flags: " + flags + ", startId: " + startId);
            
            super.onStartCommand(intent, flags, startId);
            mRunningIntent = intent;
            
            ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
                try {
                    setStatus(BackgroundNotification.NOTIFICATION_STATUS.SERVICE_STARTED);
                    return true;
                } catch (Exception e) {
                    InternalLogger.e(TAG, "Error setting service status", e);
                    return false;
                }
            }, false);
            
            InternalLogger.d(TAG, "BgCameraService onStartCommand completed");
            return START_STICKY;
            
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error in BgCameraService onStartCommand", e);
            return START_NOT_STICKY; // Don't restart if there's a critical error
        }
    }

    @Override
    public void onDestroy() {
        try {
            InternalLogger.i(TAG, "BgCameraService onDestroy starting");
            
            // Stop any pending retry attempts
            mCameraHandler.removeCallbacks(reopenRunnable);
            
            // Mark as closing to prevent new operations
            mClosing = true;
            
            // Cleanup camera resources safely
            cleanupCameraResourcesSafely();
            
            // Cleanup other resources
            cleanupServiceResourcesSafely();
            
            // Call super last
            super.onDestroy();
            
            InternalLogger.i(TAG, "BgCameraService onDestroy completed successfully");
            
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error in BgCameraService onDestroy", e);
        }
    }
    
    /**
     * Cleanup camera resources safely
     */
    private void cleanupCameraResourcesSafely() {
        ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
            try {
                // Close camera session and device
                safeCloseSessionAndDevice(mCamera2);
                
                // Release preview surface
                if (mPreviewSurface != null) {
                    try {
                        mPreviewSurface.release();
                        InternalLogger.d(TAG, "Preview surface released");
                    } catch (Exception e) {
                        InternalLogger.w(TAG, "Error releasing preview surface", e);
                    }
                    mPreviewSurface = null;
                }
                
                // Release preview texture
                if (mPreviewTexture != null) {
                    try {
                        mPreviewTexture.release();
                        InternalLogger.d(TAG, "Preview texture released");
                    } catch (Exception e) {
                        InternalLogger.w(TAG, "Error releasing preview texture", e);
                    }
                    mPreviewTexture = null;
                }
                
                InternalLogger.d(TAG, "Camera resources cleaned up successfully");
                return true;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error cleaning up camera resources", e);
                return false;
            }
        }, false, 3); // 3 second timeout for cleanup
    }
    
    /**
     * Cleanup service resources safely
     */
    private void cleanupServiceResourcesSafely() {
        ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
            try {
                // Quit camera thread
                if (mCameraThread != null && mCameraThread.isAlive()) {
                    try {
                        mCameraThread.quitSafely();
                        mCameraThread.join(2000); // Wait up to 2 seconds
                        InternalLogger.d(TAG, "Camera thread stopped");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        InternalLogger.w(TAG, "Interrupted while stopping camera thread", e);
                    } catch (Exception e) {
                        InternalLogger.w(TAG, "Error stopping camera thread", e);
                    }
                }
                
                // Unregister receiver if registered
                if (isConfigChangeReceiverRegistered && configChangeReceiver != null) {
                    try {
                        unregisterReceiver(configChangeReceiver);
                        isConfigChangeReceiverRegistered = false;
                        InternalLogger.d(TAG, "Config change receiver unregistered");
                    } catch (Exception e) {
                        InternalLogger.w(TAG, "Error unregistering config change receiver", e);
                    }
                }
                
                // Release wake lock
                if (wakeLock != null && wakeLock.isHeld()) {
                    try {
                        wakeLock.release();
                        InternalLogger.d(TAG, "Wake lock released");
                    } catch (Exception e) {
                        InternalLogger.w(TAG, "Error releasing wake lock", e);
                    }
                    wakeLock = null;
                }
                
                // Clear frame available listener
                mFrameAvailableListener = null;
                
                // Clear callbacks
                mSessionStateCallback = null;
                mCameraStateCallback = null;
                
                // Clear binder reference
                if (mBinderRef != null) {
                    mBinderRef.clear();
                    mBinderRef = null;
                }
                
                InternalLogger.d(TAG, "Service resources cleaned up successfully");
                return true;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error cleaning up service resources", e);
                return false;
            }
        }, false, 3); // 3 second timeout for cleanup
    }

    void clearSharedInstance() {
        if (mEglManager != null) {
            mEglManager.shutdown();           // frees GL/streams but leaves sInstance
            SharedEglManager.cleanAndReset();   // synchronous
            // SharedEglManager.cleanAndResetAsync(null); // non-blocking
            mEglManager = null;
        }
    }

    // Camera-specific methods
    private void initCamera() {
        mSessionStateCallback = new CameraCaptureSession.StateCallback() {
            @Override
            public void onReady(@NonNull CameraCaptureSession session) {
                Log.v(TAG, "onReady");
            }

            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                Log.v(TAG, "onConfigured");
                mCaptureSession = session;
                if (!mClosing) startPreview();
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                Log.v(TAG, "onConfigureFailed");
            }
        };

        mCameraStateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                Log.v(TAG, "onOpened");
                if (mClosing) {
                    Log.w(TAG, "Camera opened after service is closing, closing camera");
                    camera.close();
                    return;
                }
                mCamera2 = camera;
                setStatus(BackgroundNotification.NOTIFICATION_STATUS.OPENED);
                initCapture();
                createCaptureSession();
            }

            @Override
            public void onClosed(@NonNull CameraDevice camera) {
                Log.v(TAG, "onClosed");
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                Log.w(TAG, "Camera disconnected – will try to reopen");
                safeCloseSessionAndDevice(camera);
                scheduleReopen();
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                Log.e(TAG, "Camera error: " + error);
                switch (error) {
                    case ERROR_CAMERA_IN_USE:
                    case ERROR_MAX_CAMERAS_IN_USE:
                    case ERROR_CAMERA_DEVICE:
                        safeCloseSessionAndDevice(camera);
                        scheduleReopen();
                        break;
                    case ERROR_CAMERA_DISABLED:
                        stopSafe();
                        break;
                    case CameraDevice.StateCallback.ERROR_CAMERA_SERVICE:
                        stopSafe();
                        break;
                }
            }
        };

        final CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        mCameraHandler.post(() -> {
            try {
                String cameraId = AppPreference.getStr(AppPreference.KEY.SELECTED_POSITION, AppConstant.REAR_CAMERA);
                if (TextUtils.isEmpty(cameraId) ||
                        (!TextUtils.equals(cameraId, AppConstant.REAR_CAMERA) && !TextUtils.equals(cameraId, AppConstant.FRONT_CAMERA))) {
                    cameraId = AppConstant.REAR_CAMERA;
                }
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Camera permission not granted");
                    return;
                }
                String[] cameraIds = cameraManager.getCameraIdList();
                boolean isCameraAvailable = Arrays.asList(cameraIds).contains(cameraId);
                if (!isCameraAvailable) {
                    Log.e(TAG, "Camera ID not found: " + cameraId);
                    return;
                }
                cameraManager.openCamera(cameraId, mCameraStateCallback, mCameraHandler);
            } catch (CameraAccessException e) {
                Log.e(TAG, "CameraAccessException: " + Log.getStackTraceString(e));
                showToast("Camera access is disabled by policy.");
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException: " + Log.getStackTraceString(e));
                showToast("Camera security exception occurred.");
            } catch (Exception e) {
                Log.e(TAG, "Exception: " + Log.getStackTraceString(e));
                showToast("An error occurred while accessing the camera.");
            }
        });
    }

    private void initCapture() {
        configChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateDisplayOrientation();
            }
        };
        if (!isConfigChangeReceiverRegistered) {
            registerReceiver(configChangeReceiver, new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED));
            isConfigChangeReceiverRegistered = true;
        }
        updateDisplayOrientation();
    }

    private void updateDisplayOrientation() {
        if (mEglManager != null) {
            mEglManager.updateDisplayOrientation();
        }
    }

    private void safeCloseSessionAndDevice(CameraDevice camera) {
        if (mCaptureSession != null) {
            try { mCaptureSession.stopRepeating(); } catch (Exception ignored) {}
            try { mCaptureSession.close(); } catch (Exception ignored) {}
            mCaptureSession = null;
        }
        try {
            if (camera != null) {
                camera.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createCaptureSession() {
        if (mClosing) {
            Log.w(TAG, "Service is closing – skipping createCaptureSession()");
            return;
        }
        if (mPreviewSurface == null || !mPreviewSurface.isValid()) {
            Log.w(TAG, "Preview surface invalid or null – skipping createCaptureSession()");
            return;
        }
        try {
            List<Surface> outputSurfaces = Collections.singletonList(mPreviewSurface);
            mCamera2.createCaptureSession(outputSurfaces, mSessionStateCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to create capture session", e);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Surface abandoned – cannot create capture session", e);
        }
    }

    private void startPreview() {
        if (mCamera2 == null || mCaptureSession == null) return;
        try {
            CaptureRequest.Builder builder =
                    mCamera2.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            float fpsValue = SettingsUtils.findStreamFps(getApplicationContext(), Objects.requireNonNull(mEglManager.findCameraInfo().fpsRanges));
            Range<Integer> fpsRange = new Range<>((int) fpsValue, (int) fpsValue); // Assuming same min and max
            builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);
            builder.addTarget(mPreviewSurface);
            mCaptureSession.setRepeatingRequest(builder.build(), null, mCameraHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to start preview", e);
        }
    }

    private void scheduleReopen() {
        if (reopenAttempts++ < MAX_RETRIES && !mClosing) {
            mCameraHandler.postDelayed(reopenRunnable, REOPEN_DELAY_MS);
        } else {
            Log.e(TAG, "Max camera reopen attempts reached");
            stopSafe();
        }
    }

    public void stopSafe() {
        if (mNotifyCallback != null) {
            mNotifyCallback.stopService(ServiceType.BgCamera);
        } else {
            stopSelf();
        }
    }
}
