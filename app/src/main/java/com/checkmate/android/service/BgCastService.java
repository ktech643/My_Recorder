package com.checkmate.android.service;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.Surface;
import com.checkmate.android.service.SharedEGL.ServiceType;
import com.checkmate.android.service.SharedEGL.SharedEglManager;
import com.checkmate.android.service.cast.StreamConditionerBase;
import com.checkmate.android.util.CallCapture.DualCallAudioCapture;
import com.checkmate.android.util.CallMicThread;
import com.checkmate.android.util.Connection;
import com.checkmate.android.util.MainActivity;
import com.wmspanel.libstream.AudioConfig;
import com.wmspanel.libstream.Streamer;
import com.wmspanel.libstream.VideoConfig;
import java.lang.ref.WeakReference;
import java.util.List;
import javax.inject.Inject;
import toothpick.Scope;
import toothpick.Toothpick;
import toothpick.config.Module;

public final class BgCastService extends BaseBackgroundService {
    private final String TAG = "BgCastService";
    // Screen-casting-specific fields
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private Streamer.MODE mMode;
    private StreamConditionerBase mConditioner;
    private CallMicThread mMic; // For microphone capture
    private CallMicThread mRecorderMic; // For recording
    private DualCallAudioCapture audioMgr;
    private Streamer.Size videoSize, recordSize;
    // Configuration fields
    private int resultCodeLocal;
    private Intent dataLocal;
    private int screenDensityLocal;
    private List<Connection> connectionsLocal;
    private AudioConfig audioConfigLocal;
    private VideoConfig videoConfigLocal;
    private Streamer.MODE modeLocal;
    private int fpsLocal;
    // Notification constants
    private static final String CHANNEL_ID = "BgCastService";
    // Binder
    public static class CastBinder extends ServiceBinder<BgCastService> {
        public CastBinder(BgCastService service) {
            super(service);
        }
    }
    private WeakReference<BgCastService.CastBinder> mBinderRef = new WeakReference<>(new CastBinder(this));
    @Override
    public IBinder onBind(Intent intent) {
        return mBinderRef.get();
    }
    // Instance management
    public static BgCastService instance;
    public static BgCastService getInstance() {
        return instance;
    }
    // Listener interface
    public interface Listener {
        Handler getHandler();
        void onScreenCastStop();
    }
    @Inject
    public BgCastService() {
        // Constructor logic
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        mCurrentStatus = BackgroundNotification.NOTIFICATION_STATUS.CREATED;

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        String wakeLockTag = "CheckMate:CastLock";
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag);
        wakeLock.acquire(60 * 60 * 2000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        mRunningIntent = intent;
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopScreenCast();
        super.onDestroy();
        instance = null;
        stopSafe();
    }

    // Screen-casting specific methods
    public void startScreenCastWithPermission(int resultCode, Intent data, int screenDensity,
                                              List<Connection> connections, AudioConfig audioConfig,
                                              VideoConfig videoConfig, Streamer.MODE mode,
                                              int fps, boolean isRestart) throws Exception {
        if (!isRestart && mEglManager.mStreamer != null) {
            return;
        }
        // Store configuration
        resultCodeLocal = resultCode;
        dataLocal = data;
        screenDensityLocal = screenDensity;
        connectionsLocal = connections;
        audioConfigLocal = audioConfig;
        videoConfigLocal = videoConfig;
        modeLocal = mode;
        fpsLocal = fps;

        // Initialize EGL manager if not already ready
        int w = videoConfig.videoSize.width;
        int h = videoConfig.videoSize.height;
        videoSize = videoConfig.videoSize;
        recordSize = videoConfig.videoSize;

        if (sharedViewModel != null) {
            sharedViewModel.setScreenCastVideoConfig(videoConfig, audioConfig);
        }
        mEglManager = SharedEglManager.getInstance();
        if (mEglManager != null && !mEglManager.isEglReady()) {
            mEglManager.initialize(getApplicationContext(), ServiceType.BgScreenCast);
        }
        mEglManager.setListener(new SharedEglManager.Listener() {
            @Override
            public void onEglReady() {
                setupScreenCapture(w, h);
            }
        });
    }

    private void setupScreenCapture(int width, int height) {
        if (mEglManager == null) return;
        mEglManager.videoSize = videoSize;
        mEglManager.recordSize = recordSize;
        mEglManager.setSourceSize(width, height);

        mPreviewTexture = mEglManager.getCameraTexture();
        if (mPreviewTexture == null) {
            Log.e(TAG, "Failed to get camera texture from EglManager");
            return;
        }

        mPreviewSurface = new Surface(mPreviewTexture);
        mPreviewTexture.setDefaultBufferSize(width, height);

        if (mPreviewSurface == null || !mPreviewSurface.isValid()) {
            Log.e(TAG, "Invalid preview surface!");
            return;
        }

        mFrameAvailableListener = surfaceTexture -> {
            if (mPreviewTexture != null) {
                try {
                    mPreviewTexture.updateTexImage();
                    float[] tx = new float[16];
                    mPreviewTexture.getTransformMatrix(tx);
                } catch (Exception e) {
                    Log.e(TAG, "DrawFrame error: " + e.getMessage(), e);
                    return;
                }
            }
            if (mEglManager != null && mEglManager.getHandler() != null) {
                mEglManager.getHandler().post(mEglManager::drawFrame);
            }
        };

        mPreviewTexture.setOnFrameAvailableListener(mFrameAvailableListener, mEglManager.getHandler());

        MediaProjectionManager manager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mMediaProjection = manager.getMediaProjection(resultCodeLocal, dataLocal);

        mMediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                stopScreenCast();
            }
        }, mHandler);

        mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                "com.checkmate.screencast",
                width,
                height,
                screenDensityLocal,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mPreviewSurface,
                null,
                null
        );

        if (mVirtualDisplay == null) {
            Log.e(TAG, "Virtual display creation failed!");
            stopScreenCast();
            return;
        }

        mEglManager.mMediaProjection = mMediaProjection;
        mEglManager.videoSize = videoSize;
        mEglManager.recordSize = recordSize;
        mEglManager.streamAudioConfigLocal = audioConfigLocal;
        mEglManager.streamVideoConfigLocal = videoConfigLocal;
        mEglManager.startStreaming();
    }

    public void stopScreenCast() {
        releaseWakeLock();
        releasePreviewResources();      // <-- add here

        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }

        if (audioMgr != null) {
            audioMgr.stop();
            audioMgr = null;
        }

        if (mMic != null) {
            mMic.interrupt();
            try {
                mMic.join();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            mMic = null;
        }

        if (mRecorderMic != null) {
            mRecorderMic.interrupt();
            try {
                mRecorderMic.join();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            mRecorderMic = null;
        }

        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }

        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        if (MainActivity.getInstance() != null) {
            MainActivity.getInstance().updateStreamIcon(false);
        }
    }

    private void releasePreviewResources() {
        if (mPreviewTexture != null) {
            mPreviewTexture.setOnFrameAvailableListener(null);
            try { mPreviewTexture.release(); } catch (Exception ignore) {}
            mPreviewTexture = null;
        }
        if (mPreviewSurface != null) {
            mPreviewSurface.release();
            mPreviewSurface = null;
        }
        // Tell EGL thread to drop GL objects
        if (mEglManager != null) {
            mEglManager.stopStreaming();   // add this in SharedEglManager
            mEglManager.cleanup();
            mEglManager = null;
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

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        wakeLock = null;
    }

    public void stopSafe() {
        if (mNotifyCallback != null) {
            mNotifyCallback.stopService(ServiceType.BgScreenCast);
        } else {
            stopSelf();
        }
    }

    @Override
    protected ServiceType getServiceType() {
        return ServiceType.BgScreenCast;
    }
}
