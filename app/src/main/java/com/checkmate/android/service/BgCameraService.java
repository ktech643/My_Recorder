package com.checkmate.android.service;

import android.os.IBinder;
import android.os.PowerManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.Surface;
import android.graphics.SurfaceTexture;
import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

import com.checkmate.android.service.SharedEGL.SharedEglManager;
import com.checkmate.android.service.SharedEGL.ServiceType;
import com.checkmate.android.model.SurfaceModel;

public class BgCameraService extends BaseBackgroundService {
    private static final String TAG = "BgCameraService";

    private final HandlerThread mCameraThread = new HandlerThread("BgCamera");
    private final Handler mCameraHandler = new Handler(Looper.getMainLooper());
    private boolean mClosing;

    // Binder
    public static class CameraBinder extends ServiceBinder<BgCameraService> {
        public CameraBinder(BgCameraService service) { super(service); }
    }
    private WeakReference<CameraBinder> mBinderRef = new WeakReference<>(new CameraBinder(this));

    @Override
    public IBinder onBind(android.content.Intent intent) {
        return mBinderRef.get();
    }

    @Override
    protected ServiceType getServiceType() {
        return ServiceType.BgCamera;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mCurrentStatus = BackgroundNotification.NOTIFICATION_STATUS.CREATED;

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        String wakeLockTag = "CheckMate:CameraLock";
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag);
        wakeLock.acquire(60 * 60 * 2000);

        mCameraThread.start();

        // Use shared EGL managed centrally
        mEglManager = SharedEglManager.getInstance();
        if (mEglManager != null) {
            mEglManager.setListener(() -> {
                if (mEglManager == null) return;
                if (mPreviewTexture != null) return;

                mPreviewTexture = mEglManager.getCameraTexture();
                if (mPreviewTexture == null) {
                    Log.e(TAG, "Failed to get camera texture from EglManager");
                    return;
                }

                mPreviewSurface = new Surface(mPreviewTexture);
                mPreviewTexture.setDefaultBufferSize(1280, 720);
                Log.d(TAG, "Preview surface created and buffer size set");

                if (sharedViewModel != null && sharedViewModel.getSurfaceModel() != null) {
                    SurfaceModel surfaceModel = sharedViewModel.getSurfaceModel();
                    SurfaceTexture dsurfaceTexture = surfaceModel.getSurfaceTexture();
                    int dwidth = surfaceModel.getWidth();
                    int dheight = surfaceModel.getHeight();
                    if (dsurfaceTexture != null && dwidth > 0 && dheight > 0) {
                        mEglManager.setPreviewSurface(dsurfaceTexture, dwidth, dheight);
                    }
                }

                mFrameAvailableListener = st -> {
                    if (mEglManager != null && mEglManager.getHandler() != null) {
                        mEglManager.getHandler().post(this::drawFrame);
                    }
                };
                mPreviewTexture.setOnFrameAvailableListener(mFrameAvailableListener);
                initCamera();
            });
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

    @Override
    public int onStartCommand(android.content.Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        mRunningIntent = intent;
        setStatus(BackgroundNotification.NOTIFICATION_STATUS.SERVICE_STARTED);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mPreviewSurface != null) {
            mPreviewSurface.release();
            mPreviewSurface = null;
        }
        if (mPreviewTexture != null) {
            mPreviewTexture.release();
            mPreviewTexture = null;
        }
        if (mBinderRef != null) {
            mBinderRef.clear();
            mBinderRef = null;
        }
        if (mCameraThread != null) {
            mCameraThread.quitSafely();
            try { mCameraThread.join(); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
    }

    // Stubs for camera management (hook up to Camera2 if needed)
    private void initCamera() {}
}

