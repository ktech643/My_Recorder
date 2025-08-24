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
import com.checkmate.android.util.CrashLogger;
import com.checkmate.android.util.SafeExecutor;

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
        return mBinderRef.get();
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
        super.onCreate();
        CrashLogger.d(TAG, "BgCameraService onCreate");
        
        SafeExecutor.executeVoid("bgCameraServiceOnCreate", () -> {
            mCurrentStatus = BackgroundNotification.NOTIFICATION_STATUS.CREATED;

            // Initialize wake lock with null safety
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (SafeExecutor.checkNotNull(pm, "PowerManager", "BgCameraService onCreate")) {
                String wakeLockTag = "CheckMate:CameraLock";
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag);
                if (SafeExecutor.checkNotNull(wakeLock, "wakeLock", "BgCameraService onCreate")) {
                    wakeLock.acquire(60 * 60 * 2000); // 2 hours
                    CrashLogger.d(TAG, "Wake lock acquired");
                } else {
                    CrashLogger.e(TAG, "Failed to create wake lock");
                }
            } else {
                CrashLogger.e(TAG, "PowerManager is null");
            }

            // Start camera thread with error handling
            try {
                if (!mCameraThread.isAlive()) {
                    mCameraThread.start();
                    CrashLogger.d(TAG, "Camera thread started");
                } else {
                    CrashLogger.w(TAG, "Camera thread already running");
                }
            } catch (Exception e) {
                CrashLogger.e(TAG, "Failed to start camera thread", e);
            }

            // Get the singleton instance with error handling
            SafeExecutor.executeVoid("initializeEglManager", () -> {
                SharedEglManager.cleanAndResetAsync(() -> SafeExecutor.executeVoid("eglManagerSetup", () -> {
                    try {
                        mEglManager = SharedEglManager.getInstance();
                        if (SafeExecutor.checkNotNull(mEglManager, "mEglManager", "BgCameraService onCreate")) {
                            mEglManager.initialize(getApplicationContext(), ServiceType.BgCamera);
                            CrashLogger.d(TAG, "EGL Manager initialized");
                        } else {
                            CrashLogger.e(TAG, "Failed to get EGL Manager instance");
                        }
                    } catch (Exception e) {
                        CrashLogger.e(TAG, "EGL Manager initialization failed", e);
                    }
                }));
            });
        });

        // Initialize EGL listener with null safety and error handling
        SafeExecutor.executeVoid("setEglListener", () -> {
            if (SafeExecutor.checkNotNull(mEglManager, "mEglManager", "setListener")) {
                mEglManager.setListener(new SharedEglManager.Listener() {
                    @Override
                    public void onEglReady() {
                        SafeExecutor.executeVoid("onEglReady", () -> {
                            if (!SafeExecutor.checkNotNull(mEglManager, "mEglManager", "onEglReady")) {
                                return;
                            }
                            
                            if (mPreviewTexture != null) {
                                CrashLogger.d(TAG, "Preview texture already exists, skipping creation");
                                return;
                            }

                            mPreviewTexture = mEglManager.getCameraTexture();
                            if (!SafeExecutor.checkNotNull(mPreviewTexture, "mPreviewTexture", "onEglReady")) {
                                CrashLogger.e(TAG, "Failed to get camera texture from EglManager");
                                return;
                            }

                            try {
                                mPreviewSurface = new Surface(mPreviewTexture);
                                mPreviewTexture.setDefaultBufferSize(1280, 720);
                                CrashLogger.d(TAG, "Preview surface created and buffer size set");
                            } catch (Exception e) {
                                CrashLogger.e(TAG, "Failed to create preview surface", e);
                                return;
                            }

                            if (SafeExecutor.checkNotNull(sharedViewModel, "sharedViewModel", "onEglReady")) {
                                try {
                                    SurfaceModel surfaceModel = sharedViewModel.getSurfaceModel();
                                    if (SafeExecutor.checkNotNull(surfaceModel, "surfaceModel", "onEglReady")) {
                                        SurfaceTexture dsurfaceTexture = surfaceModel.getSurfaceTexture();
                                        if (!SafeExecutor.checkNotNull(dsurfaceTexture, "dsurfaceTexture", "onEglReady")) {
                                            CrashLogger.w(TAG, "onEglReady: SurfaceTexture not created yet, skipping for now");
                                            return;
                                        }
                                        int dwidth = surfaceModel.getWidth();
                                        int dheight = surfaceModel.getHeight();
                                        mEglManager.setPreviewSurface(dsurfaceTexture, dwidth, dheight);
                                        CrashLogger.d(TAG, "Preview surface set with dimensions: " + dwidth + "x" + dheight);
                                    }
                                } catch (Exception e) {
                                    CrashLogger.e(TAG, "Error setting preview surface", e);
                                }
                            } else {
                                CrashLogger.w(TAG, "SharedViewModel is null in onEglReady");
                            }

                            try {
                                mFrameAvailableListener = surfaceTexture -> SafeExecutor.executeVoid("frameAvailable", () -> {
                                    if (SafeExecutor.checkNotNull(mEglManager, "mEglManager", "frameAvailable") && 
                                        SafeExecutor.checkNotNull(mEglManager.getHandler(), "eglHandler", "frameAvailable")) {
                                        mEglManager.getHandler().post(this::drawFrame);
                                    }
                                });
                                mPreviewTexture.setOnFrameAvailableListener(mFrameAvailableListener);
                                CrashLogger.d(TAG, "Frame available listener set");
                            } catch (Exception e) {
                                CrashLogger.e(TAG, "Failed to set frame available listener", e);
                            }
                            
                            SafeExecutor.executeVoid("initCamera", this::initCamera);
                        });
                    }

                    private void drawFrame() {
                        SafeExecutor.executeVoid("drawFrame", () -> {
                            if (SafeExecutor.checkNotNull(mPreviewTexture, "mPreviewTexture", "drawFrame")) {
                                try {
                                    mPreviewTexture.updateTexImage();
                                    float[] tx = new float[16];
                                    mPreviewTexture.getTransformMatrix(tx);
                                } catch (Exception e) {
                                    CrashLogger.e(TAG, "Failed to updateTexImage()", e);
                                    return;
                                }
                            }
                            
                            if (SafeExecutor.checkNotNull(mEglManager, "mEglManager", "drawFrame")) {
                                try {
                                    mEglManager.drawFrame();
                                } catch (Exception e) {
                                    CrashLogger.e(TAG, "Failed to draw frame", e);
                                }
                            }
                        });
                    }
                });
            } else {
                CrashLogger.e(TAG, "Cannot set EGL listener - mEglManager is null");
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        mRunningIntent = intent;
        setStatus(BackgroundNotification.NOTIFICATION_STATUS.SERVICE_STARTED);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Camera-specific cleanup
        safeCloseSessionAndDevice(mCamera2);

        if (mPreviewSurface != null) {
            mPreviewSurface.release();
            mPreviewSurface = null;
        }

        if (mPreviewTexture != null) {
            mPreviewTexture.release();
            mPreviewTexture = null;
        }

        if (isConfigChangeReceiverRegistered && configChangeReceiver != null) {
            try {
                unregisterReceiver(configChangeReceiver);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "ConfigChangeReceiver not registered", e);
            } finally {
                isConfigChangeReceiverRegistered = false;
                configChangeReceiver = null;
            }
        }

        mCameraHandler.removeCallbacks(reopenRunnable);

        try {
            if (mCaptureSession != null) {
                mCaptureSession.stopRepeating();
                mCaptureSession.abortCaptures();
                mCaptureSession.close();
                mCaptureSession = null;
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error stopping capture session", e);
        }

        if (mCamera2 != null) {
            mCamera2.close();
            mCamera2 = null;
        }

        if (mNotifyCallback != null) {
            mNotifyCallback.stopService(ServiceType.BgCamera);
            mNotifyCallback = null;
        }

        if (mBinderRef != null) {
            mBinderRef.clear();
            mBinderRef = null;
        }
        ckearSharedInctance();
        if (mCameraThread != null) {
            mCameraThread.quitSafely();
            try {
                mCameraThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    void ckearSharedInctance() {
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
