package com.checkmate.android.service.SharedEGL;

import static android.content.Context.POWER_SERVICE;

import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.net.Uri;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Range;
import android.view.Surface;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.ViewModelProvider;
import com.checkmate.android.AppConstant;
import com.checkmate.android.AppPreference;
import com.checkmate.android.MyApp;
import com.checkmate.android.R;
import com.checkmate.android.SoftRestartActivity;
import com.checkmate.android.service.BaseBackgroundService;
import com.checkmate.android.service.cast.StreamConditionerMode1;
import com.checkmate.android.service.cast.StreamConditionerMode2;
import com.checkmate.android.ui.fragment.StreamingFragment;
import com.checkmate.android.util.CallCapture.DualCallAudioCapture;
import com.checkmate.android.util.CallMicThread;
import com.checkmate.android.util.CameraInfo;
import com.checkmate.android.util.CameraManager;
import com.checkmate.android.util.Connection;
import com.checkmate.android.util.ConnectionStatistics;
import com.checkmate.android.util.ErrorMessage;
import com.checkmate.android.util.Formatter;
import com.checkmate.android.util.MainActivity;
import com.checkmate.android.util.MessageUtil;
import com.checkmate.android.util.MicThread;
import com.checkmate.android.util.SettingsUtils;
import com.checkmate.android.util.StreamConditionerBase;
import com.checkmate.android.util.libgraph.EglCoreNew;
import com.checkmate.android.util.libgraph.FullFrameRectLetterboxNew;
import com.checkmate.android.util.libgraph.SurfaceImageNew;
import com.checkmate.android.util.libgraph.Texture2dProgramNew;
import com.checkmate.android.util.libgraph.WindowSurfaceNew;
import com.checkmate.android.viewmodels.SharedViewModel;
import com.wmspanel.libstream.AudioConfig;
import com.wmspanel.libstream.Streamer;
import com.wmspanel.libstream.StreamerSurface;
import com.wmspanel.libstream.StreamerSurfaceBuilder;
import com.wmspanel.libstream.VideoConfig;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.ref.WeakReference;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Singleton;
import androidx.annotation.GuardedBy;
import com.checkmate.android.database.FileStoreDb;
import android.media.MediaMetadataRetriever;
import android.graphics.BitmapFactory;

@Singleton
public class SharedEglManager {
    private static final String TAG = "SharedEglManager";
    // Singleton instance management
    private static volatile SharedEglManager sInstance;
    private static final Object sLock = new Object();
    
    // Service management
    private final Map<ServiceType, WeakReference<BaseBackgroundService>> mRegisteredServices = new ConcurrentHashMap<>();
    private ServiceType mCurrentActiveService = null;
    private final Object mServiceLock = new Object();
    
    // EGL context management
    private volatile boolean mIsInitialized = false;
    private volatile boolean mIsShuttingDown = false;
    private final CountDownLatch mInitLatch = new CountDownLatch(1);
    
    private ServiceType mServiceType;
    private static final long ERROR_LOG_COOLDOWN_MS = 5000;
    private static final long SLOW_FRAME_THRESHOLD_NS = 16_666_666;
    private static final long PERFORMANCE_LOG_INTERVAL_MS = 5000;
    private static final float BASE_HEIGHT = 720f;
    private static final int TEXT_SIZE_DP = 32;
    private static final int PADDING_DP = 8;
    private static final int REOPEN_DELAY_MS = 500;
    private static final int MAX_RETRIES = 5;
    private static final int RETRY_TIMEOUT = 3000;
    private static final long MIN_FRAME_INTERVAL = 33; // 30 FPS in ms
    private static final long MEM_THRESHOLD = 100 * 1024 * 1024; // 100MB
    private static final long CONNECTION_TIMEOUT = 10000; // 10 seconds

    // Streamer components
    public StreamerSurface mStreamer;
    private StreamerSurface mRecorder;
    private MicThread mMic;
    public AudioConfig streamAudioConfigLocal;
    public VideoConfig streamVideoConfigLocal;
    private long mBroadcastStartTime;
    private boolean mRadioMode = false;
    private boolean mUseBluetooth = false;
    private boolean isBlueToothReceiverRegistered = false;
    public boolean mStreaming = false;
    public boolean mRecording = false;
    public Streamer.Size videoSize, recordSize;

    private final Map<Integer, Connection> mConnectionId = new ConcurrentHashMap<>();
    private final Map<String, String> mConnectionErrors = new ConcurrentHashMap<>();
    private final Map<Integer, Streamer.CONNECTION_STATE> mConnectionState = new ConcurrentHashMap<>();
    private final Map<Integer, ConnectionStatistics> mConnectionStatistics = new ConcurrentHashMap<>();
    private final AtomicInteger mRetryPending = new AtomicInteger();
    private int reopenAttempts = 0;
    private long SPLIT_INTERVAL_MS;
    private StreamConditionerBase mConditioner;
    private StreamConditionerMode1 mStreamConditioner1;
    private StreamConditionerMode2 mStreamConditioner2;
    // EGL/GL components
    public EglCoreNew eglCore;
    private WindowSurfaceNew displaySurface;
    private WindowSurfaceNew encoderSurface;
    private WindowSurfaceNew recorderSurface;
    private FullFrameRectLetterboxNew fullFrameBlit;
    private int textureId;
    private SurfaceTexture cameraTexture;
    private int oesTextureId;
    public final float[] mTmpMatrix = new float[16];
    private boolean mClosing = false;
    private int mDisplayOrientation;
    private int mRotation = 0;
    private boolean mIsMirrored = false;
    private boolean mIsFlipped = false;
    private boolean should_snapshot = false;
    private int mScreenWidth = 1280;
    private int mScreenHeight = 720;
    private Formatter mFormatter;
    private final Map<Integer, Integer> mReconnectAttempts = new ConcurrentHashMap<>();
    private final Map<Integer, Long> mLastReconnectTime = new ConcurrentHashMap<>();
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final long INITIAL_RECONNECT_DELAY_MS = 1000; // 1 second
    private static final long MAX_RECONNECT_DELAY_MS = 30000; // 30 seconds
    // Overlay components
    private final Object overlayLock = new Object();
    @GuardedBy("overlayLock") private SurfaceImageNew overlay;
    @GuardedBy("overlayLock") private Paint textPaint;
    @GuardedBy("overlayLock") private Paint bgPaint;
    @GuardedBy("overlayLock") private Rect textBounds;
    @GuardedBy("overlayLock") private Bitmap overlayBmp;
    @GuardedBy("overlayLock") private Canvas overlayCanvas;
    @GuardedBy("overlayLock") private String lastOverlayText;
    private int overlayPadding;
    private float textSize;
    private int overlayMarginX = 16;
    private int overlayMarginY = 16;
    public SharedViewModel sharedViewModel;
    // File management
    private File tempRecordingFile;
    private Uri selectedTreeUri;
    private FileStoreDb fileStoreDb;
    // Performance tracking
    private long lastFrameTime = 0;
    private long textureUpdateTime = 0;
    private long drawTime = 0;
    private long swapBufferTime = 0;
    private volatile boolean drawErrorLogged;
    private long lastDrawErrorTime;
    private long slowFrameCount;
    private long totalFrameCount;
    private long lastPerformanceLogTime;
    private final Queue<Bitmap> bitmapPool = new ArrayDeque<>(3);
    Runnable updateRunnable;
    // Transformation matrices
    private final float[] mBaseMatrix = new float[16];
    // Handlers and threading
    public static final ThreadSafeHandlerManager mCameraHandler = new ThreadSafeHandlerManager();
    public HandlerThread mCameraThread;
    public Context context;
    private EGLContext sharedContext;
    private final Runnable reopenRunnable = this::runnableInit;
    private final Runnable mSplitRunnable = this::restartRecording;
    private final Runnable mConnectionCheckRunnable = this::checkConnectionStatus;
    public boolean eglIsReady = false;
    private TimeZone timeZone;
    int srcW;
    int srcH;
    DisplayMetrics dm;
    float px;
    String strTime = "";
    public CallMicThread mCastStreamMic; // For recording
    public CallMicThread mCastRecorderMic; // For recording
    public DualCallAudioCapture audioMgr;
    public MediaProjection mMediaProjection;
    private final static int STATISTICS_TIMEOUT = 1000;
    private CountDownLatch shutdownLatch;
    
    // Enhanced EGL Surface Management
    private volatile boolean mEglSurfaceValid = false;
    private final Object mEglSurfaceLock = new Object();
    private volatile Surface mCurrentPreviewSurface = null;
    private volatile boolean mIsServiceSwitching = false;
    private final Object mServiceSwitchingLock = new Object();
    
    // Enhanced Performance monitoring
    private long mLastFrameTimeNs = 0;
    private long mFrameCount = 0;
    private long mDroppedFrames = 0;
    
    // Buffer management
    private final Queue<Runnable> mPendingOperations = new ArrayDeque<>();
    private volatile boolean mBufferOverflow = false;
    private static final int MAX_PENDING_OPERATIONS = 10;
    
    private final Runnable mUpdateStatistics = new Runnable() {
        @Override
        public void run() {
            final int count = mConnectionId.size();

            if (mStreamer == null) {
                return;
            }
            boolean needUpdate = true;
            StreamerStats stats = new StreamerStats();
            stats.fps = mStreamer.getFps();
            final long curTime = System.currentTimeMillis();
            stats.duration = (curTime - mBroadcastStartTime) / 1000L;
            stats.connName = new String[count];
            stats.connStatus = new String[count];
            stats.isPacketLossIncreasing = new boolean[count];
            buildStreamerStats(stats);
            needUpdate = false;
            CameraInfo cameraInfo = findCameraInfo();
            if (!mStreaming) {
                return;
            }

            final StringBuilder sb = new StringBuilder();
            buildNotificationContent(sb, needUpdate);
            final String body = sb.length() > 0 ? sb.toString() : "Connecting";
            if (StreamingFragment.instance != null) {
                String status = sb.length() > 0 ? "Online" : "Connecting";
                if (body.contains("bps")) {
                    String[] values = body.split(" ");
                    for (String val : values) {
                        if (val.contains("bps")) {
                            StreamingFragment.instance.updateSpeed("Streaming: " + val.replaceAll(",", ""));
                            break;
                        }
                    }
                } else {
                    status = "Error";
                }
                StreamingFragment.instance.updateStatus(status);
            }
        }
    };

    private final Runnable timestampUpdater = new Runnable() {
        @Override
        public void run() {
            setTextForTime(getCurrentDateTime());
            mCameraHandler.postDelayed(this, 1_000);
        }
    };

    void buildNotificationContent(StringBuilder sb, boolean needUpdate) {
        List<String> disconnectedErrors = new ArrayList<>(mConnectionErrors.keySet());

        for (int displayId : mConnectionId.keySet()) {
            final String name = mConnectionId.get(displayId).name;
            final Streamer.CONNECTION_STATE state = mConnectionState.get(displayId);
            final ConnectionStatistics statistics = mConnectionStatistics.get(displayId);
            if (state == null) {
                continue;
            }
            if (needUpdate && statistics != null && state == Streamer.CONNECTION_STATE.RECORD) {
                statistics.update(mStreamer, displayId);
            }

            if (statistics != null && state == Streamer.CONNECTION_STATE.RECORD && statistics.getTraffic() > 0) {
                mConnectionErrors.remove(name);
            }
            disconnectedErrors.remove(name);
            if (mConnectionErrors.containsKey(name)) {
                String errorMsg = mConnectionErrors.get(name);
                if (errorMsg != null && !errorMsg.isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append("<br>");
                    }
                    sb.append(errorMsg);
                }
            } else if (statistics != null) {
                if (sb.length() > 0) {
                    sb.append("<br>");
                }
                sb.append(String.format(Locale.US, "&lt;b>%1$s&lt;/b>: %2$s, %3$s", name, mFormatter.bandwidthToString(statistics.getBandwidth()), mFormatter.trafficToString(statistics.getTraffic())));
            }
        }

        for (String connName : disconnectedErrors) {
            String errorMsg = mConnectionErrors.get(connName);
            if (errorMsg != null && !errorMsg.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append("<error>");
                }
                sb.append(errorMsg);
            }
        }
    }

    public interface Listener {
        void onEglReady();
    }

    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
        if (eglIsReady) {
            listener.onEglReady();
        }
    }

    public void notifyEglReady() {
        if (listener != null) {
            listener.onEglReady();
        }
    }

    private final BroadcastReceiver mBluetoothScoStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED.equals(intent.getAction())) {
                int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
                Log.d(TAG, "Audio SCO state: " + state);
                if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                    MessageUtil.showToast(context, "Bluetooth SCO connected.");
                    startAudioCapture();
                }
            } else {
                int btState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                if (btState == BluetoothProfile.STATE_CONNECTED) {
                    startAudioCapture();
                }
            }
        }
    };

    Streamer.AudioCallback mAudioCallback = (audioFormat, data, audioInputLength, channelCount, sampleRate, samplesPerFrame) -> {
        if (MainActivity.instance != null) {
            MainActivity.instance.onAudioDelivered(data, channelCount, sampleRate);
        }
    };

    @Inject
    public SharedEglManager() {
    }

    /**
     * Get the singleton instance of SharedEglManager
     * This ensures only one EGL context is active across all services
     */
    public static SharedEglManager getInstance() {
        if (sInstance == null) {
            synchronized (sLock) {
                if (sInstance == null) {
                    sInstance = new SharedEglManager();
                }
            }
        }
        return sInstance;
    }

    /**
     * Register a service with the shared EGL manager
     * @param serviceType The type of service being registered
     * @param service The service instance
     * @return true if registration was successful
     */
    public boolean registerService(ServiceType serviceType, BaseBackgroundService service) {
        if (serviceType == null || service == null) {
            Log.e(TAG, "Cannot register null service type or service");
            return false;
        }
        
        synchronized (mServiceLock) {
            Log.d(TAG, "Registering service: " + serviceType);
            
            // Clean up any dead references
            cleanupDeadReferences();
            
            // Register the new service
            mRegisteredServices.put(serviceType, new WeakReference<>(service));
            
            // If this is the first service or we're switching services, make it active
            if (mCurrentActiveService == null || mCurrentActiveService != serviceType) {
                if (mCurrentActiveService != null) {
                    Log.d(TAG, "Switching from " + mCurrentActiveService + " to " + serviceType);
                    // Notify the previous service that it's being deactivated
                    notifyServiceDeactivated(mCurrentActiveService);
                }
                mCurrentActiveService = serviceType;
                Log.d(TAG, "Service " + serviceType + " is now active");
            }
            
            return true;
        }
    }

    /**
     * Unregister a service from the shared EGL manager
     * @param serviceType The type of service being unregistered
     */
    public void unregisterService(ServiceType serviceType) {
        synchronized (mServiceLock) {
            Log.d(TAG, "Unregistering service: " + serviceType);
            
            mRegisteredServices.remove(serviceType);
            
            if (mCurrentActiveService == serviceType) {
                mCurrentActiveService = null;
                
                // Find another service to activate ONLY if not streaming/recording
                if (!mStreaming && !mRecording) {
                    for (Map.Entry<ServiceType, WeakReference<BaseBackgroundService>> entry : mRegisteredServices.entrySet()) {
                        if (entry.getValue().get() != null) {
                            mCurrentActiveService = entry.getKey();
                            Log.d(TAG, "Auto-switched active service to: " + mCurrentActiveService);
                            break;
                        }
                    }
                }
            }
            
            // Only shutdown if no services left AND not streaming/recording
            cleanupDeadReferences();
            if (mRegisteredServices.isEmpty() && !mStreaming && !mRecording) {
                Log.d(TAG, "No active services remaining, shutting down EGL context");
                shutdown();
            }
        }
    }

    /**
     * Get the currently active service type
     * @return The active service type or null if none
     */
    public ServiceType getCurrentActiveService() {
        synchronized (mServiceLock) {
            return mCurrentActiveService;
        }
    }

    /**
     * Check if a specific service is currently active
     * @param serviceType The service type to check
     * @return true if the service is active
     */
    public boolean isServiceActive(ServiceType serviceType) {
        synchronized (mServiceLock) {
            return mCurrentActiveService == serviceType;
        }
    }

    /**
     * Get the number of registered services
     * @return The number of registered services
     */
    public int getRegisteredServiceCount() {
        synchronized (mServiceLock) {
            cleanupDeadReferences();
            return mRegisteredServices.size();
        }
    }

    /**
     * Clean up dead references to services
     */
    private void cleanupDeadReferences() {
        mRegisteredServices.entrySet().removeIf(entry -> entry.getValue().get() == null);
    }

    /**
     * Notify a service that it's being deactivated
     * @param serviceType The service type being deactivated
     */
    private void notifyServiceDeactivated(ServiceType serviceType) {
        WeakReference<BaseBackgroundService> ref = mRegisteredServices.get(serviceType);
        if (ref != null) {
            BaseBackgroundService service = ref.get();
            if (service != null) {
                // The service can handle deactivation if needed
                Log.d(TAG, "Notified service " + serviceType + " of deactivation");
            }
        }
    }

    /**
     * Wait for the EGL manager to be initialized
     * @param timeoutMs Timeout in milliseconds
     * @return true if initialized within timeout
     */
    public boolean waitForInitialization(long timeoutMs) {
        try {
            return mInitLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Check if the EGL manager is shutting down
     * @return true if shutting down
     */
    public boolean isShuttingDown() {
        return mIsShuttingDown;
    }

    /**
     * Check if the EGL context is ready for surface creation
     * @return true if EGL is ready
     */
    public boolean isEglReady() {
        return mIsInitialized && !mIsShuttingDown && eglCore != null && eglIsReady;
    }

    /**
     * Retry setting preview surface after a delay
     * @param surfaceTexture The surface texture to use
     * @param width The width
     * @param height The height
     * @param retryCount Current retry count
     */
    private void retrySetPreviewSurface(final SurfaceTexture surfaceTexture, int width, int height, int retryCount) {
        if (retryCount >= 3) {
            Log.e(TAG, "Failed to set preview surface after " + retryCount + " retries");
            return;
        }
        
        Log.d(TAG, "Retrying setPreviewSurface, attempt " + (retryCount + 1));
        mCameraHandler.postDelayed(() -> {
            if (isEglReady()) {
                setPreviewSurface(surfaceTexture, width, height);
            } else {
                retrySetPreviewSurface(surfaceTexture, width, height, retryCount + 1);
            }
        }, 500); // 500ms delay between retries
    }

    /**
     * Update the preview surface for the currently active service
     * This method should be called when a service becomes active and needs to update its display surface
     * @param surfaceTexture The surface texture to use for preview
     * @param width The width of the preview surface
     * @param height The height of the preview surface
     */
    public void updateActiveServiceSurface(SurfaceTexture surfaceTexture, int width, int height) {
        if (!mIsInitialized || mIsShuttingDown) {
            Log.w(TAG, "Cannot update surface - EGL manager not ready");
            return;
        }
        
        synchronized (mServiceLock) {
            if (mCurrentActiveService == null) {
                Log.w(TAG, "No active service to update surface for");
                return;
            }
            
            Log.d(TAG, "Updating surface for active service: " + mCurrentActiveService);
            setPreviewSurface(surfaceTexture, width, height);
        }
    }

    /**
     * Get the current service type that was used for initialization
     * @return The service type
     */
    public ServiceType getServiceType() {
        return mServiceType;
    }

    public void initialize(Context ctx, ServiceType serviceType) {
        synchronized (SharedEglManager.class) {
            if (mIsInitialized && !mIsShuttingDown) {
                Log.d(TAG, "SharedEglManager already initialized, skipping initialization");
                return;
            }
            
            if (mIsShuttingDown) {
                Log.w(TAG, "SharedEglManager is shutting down, cannot initialize");
                return;
            }
            
            mServiceType = serviceType;
            context = ctx;
            mIsShuttingDown = false;
        }

        mCameraHandler.postDelayed(this::internalInit, 500);
    }

    private void internalInit() {
        if (MainActivity.getInstance() != null) {
            sharedViewModel = new ViewModelProvider(MainActivity.getInstance()).get(SharedViewModel.class);
        }
        mFormatter = new Formatter(context);
        timeZone = TimeZone.getDefault();
        
        // Initialize database
        fileStoreDb = new FileStoreDb(context);
        
        if (mServiceType == ServiceType.BgUSBCamera) {
            if (sharedViewModel != null) {
                String[] arr = sharedViewModel.getCameraResolution().toArray(new String[0]);
                videoSize = SettingsUtils.getStreamVideoSizeUSBNew(arr);
            } else {
                videoSize = SettingsUtils.getStreamVideoSizeWithInfoUSB(findCameraInfo());
            }
        } else if (mServiceType == ServiceType.BgScreenCast) {
            streamAudioConfigLocal = sharedViewModel.getAudioConfig();
            streamVideoConfigLocal = sharedViewModel.getVideoConfig();
            videoSize = streamVideoConfigLocal.videoSize;
            recordSize = streamVideoConfigLocal.videoSize;
        } else {
            videoSize = SettingsUtils.getStreamVideoSize(findCameraInfo());
            recordSize = SettingsUtils.getVideoSize(findCameraInfo());
        }
        mScreenWidth = videoSize.width;
        mScreenHeight = videoSize.height;
        sharedContext = EGL14.eglGetCurrentContext();
        mCameraThread = new HandlerThread("BgCamera");
        mCameraThread.start();
        mCameraHandler.post(() -> {
            release();
            try {
                initRequiredValues();
                createStreamer();
                createRecorder();
                initializeEGL();
                if (MainActivity.getInstance() != null && SettingsUtils.isAllowedAudio()) {
                    if (MainActivity.instance.isCastStreaming()) {
                        mMic = new MicThread(SettingsUtils.audioOptionConfig(context));
                    } else {
                        mMic = new MicThread(SettingsUtils.audioConfig(context));
                    }
                    mMic.mStreamer = mStreamer;
                    mMic.mRecorder = mRecorder;
                    mMic.start();
                }
                lastPerformanceLogTime = SystemClock.elapsedRealtime();
            } catch (Exception e) {
                handleError("Initialization failed", e);
                release();
            }
        });
    }

    public void initRequiredValues() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (mCameraHandler != null) {
                    mCameraHandler.post(mUpdateStatistics);
                    mCameraHandler.postDelayed(this, 1000);
                }
            }
        };
        if (MainActivity.instance != null) {
            sharedViewModel = new ViewModelProvider(MainActivity.instance).get(SharedViewModel.class);
        }
    }

    public void setSourceSize(int w, int h) {
        srcW = w;
        srcH = h;
    }

    private void initializeEGL() {
        try {
            Log.d(TAG, "Starting EGL initialization...");
            float scaleFactor = Math.max(1.0f, mScreenHeight / BASE_HEIGHT);
            textSize = TEXT_SIZE_DP * scaleFactor;
            overlayPadding = (int) (PADDING_DP * scaleFactor);

            try {
                Log.d(TAG, "Creating EGL core with FLAG_RECORDABLE...");
                eglCore = new EglCoreNew(sharedContext, EglCoreNew.FLAG_RECORDABLE);
                Log.d(TAG, "EGL core created successfully with FLAG_RECORDABLE");
            } catch (RuntimeException e) {
                Log.w(TAG, "FLAG_RECORDABLE not supported — retrying without it", e);
                eglCore = new EglCoreNew(sharedContext, 0);
                Log.d(TAG, "EGL core created successfully without FLAG_RECORDABLE");
            }

            try {
                Log.d(TAG, "Creating temporary surface for texture program setup...");
                WindowSurfaceNew tempSurface = new WindowSurfaceNew(eglCore, new SurfaceTexture(0));
                tempSurface.makeCurrent();

                fullFrameBlit = new FullFrameRectLetterboxNew(
                        new Texture2dProgramNew(Texture2dProgramNew.ProgramType.TEXTURE_EXT));
                Log.d(TAG, "TEXTURE_EXT program created successfully");

                tempSurface.release();
            } catch (RuntimeException e) {
                Log.e(TAG, "TEXTURE_EXT failed, trying TEXTURE_2D", e);
                fullFrameBlit = new FullFrameRectLetterboxNew(
                        new Texture2dProgramNew(Texture2dProgramNew.ProgramType.TEXTURE_2D));
                Log.d(TAG, "TEXTURE_2D program created successfully");
            }
            
            textureId = fullFrameBlit.createTextureObject();
            cameraTexture = new SurfaceTexture(textureId);
            cameraTexture.setDefaultBufferSize(srcW, srcH); // Use source dimensions
            Log.d(TAG, "Camera texture created with ID: " + textureId);

            Surface displaySurfaceSurface = new Surface(cameraTexture);
            displaySurface = new WindowSurfaceNew(eglCore, displaySurfaceSurface, true);
            Log.d(TAG, "Display surface created successfully");

            if (mStreamer != null && mStreamer.getEncoderSurface() != null) {
                encoderSurface = new WindowSurfaceNew(eglCore, mStreamer.getEncoderSurface(), false);
                Log.d(TAG, "Encoder surface created successfully");
            } else {
                Log.w(TAG, "Streamer encoder surface is null");
            }

            if (mRecorder != null && mRecorder.getEncoderSurface() != null) {
                recorderSurface = new WindowSurfaceNew(eglCore, mRecorder.getEncoderSurface(), false);
                Log.d(TAG, "Recorder surface created successfully");
            } else {
                Log.w(TAG, "Recorder encoder surface is null");
            }
            
            eglIsReady = true;
            Log.d(TAG, "EGL is now ready");
            
            initializeOverlay();
            updateDisplayOrientation();
            precalculateTransforms();
            oesTextureId = initOESTexture();
            
            // Set initialization state
            synchronized (SharedEglManager.class) {
                mIsInitialized = true;
                mInitLatch.countDown();
                Log.d(TAG, "EGL initialization completed successfully");
            }
            
            notifyEglReady();
            mCameraHandler.removeCallbacks(timestampUpdater);
            mCameraHandler.postDelayed(timestampUpdater, 1_000);
        } catch (Exception e) {
            Log.e(TAG, "EGL initialization failed", e);
            releaseEgl();
            
            // Reset initialization state on failure
            synchronized (SharedEglManager.class) {
                mIsInitialized = false;
                mInitLatch.countDown();
                Log.e(TAG, "EGL initialization state reset due to failure");
            }
        }
    }

    private int initOESTexture() {
        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        int oesTexId = tex[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTexId);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        return oesTexId;
    }


    private void initializeOverlay() {
        synchronized (overlayLock) {
            overlay = new SurfaceImageNew();
            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setTextSize(textSize);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            textPaint.setColor(Color.WHITE); // Default to white text
            textBounds = new Rect();
        }
    }

    void buildStreamerStats(StreamerStats stats) {
        int idx = 0;
        for (int displayId : mConnectionId.keySet()) {
            final String name = mConnectionId.get(displayId).name;
            final Streamer.CONNECTION_STATE state = mConnectionState.get(displayId);
            final ConnectionStatistics statistics = mConnectionStatistics.get(displayId);
            if (state == null) {
                continue;
            }
            if (statistics != null && state == Streamer.CONNECTION_STATE.RECORD) {
                statistics.update(mStreamer, displayId);
            }
            stats.connName[idx] = name;
            if (statistics != null) {
                if (mFormatter == null) {
                    mFormatter = new Formatter(context);
                }
                stats.connStatus[idx] = String.format("%1$s, %2$s",
                        mFormatter.bandwidthToString(statistics.getBandwidth()),
                        mFormatter.trafficToString(statistics.getTraffic()));
            }
            idx++;
        }
        stats.isRecording = isRecording();
    }

    public WindowSurfaceNew getRecorderSurface() {
        return recorderSurface;
    }

    private void makeTextureCurrent() {
        safeUpdateTexture();
    }

    private void checkThermalState() {
        PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
        if (pm != null && pm.isPowerSaveMode()) {
            Log.w(TAG, "Power save mode enabled - reducing frame rate");
        }
    }

    private void precalculateTransforms() {
        Matrix.setIdentityM(mBaseMatrix, 0);
        Matrix.scaleM(mBaseMatrix, 0, mIsMirrored ? -1 : 1, mIsFlipped ? -1 : 1, 1);
    }

    private void monitorResources() {
        long memUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        if (memUsage > MEM_THRESHOLD) {
            Log.w(TAG, "High memory usage: " + (memUsage / (1024 * 1024)) + "MB");
            clearBitmapPool();
        }
    }

    private boolean shouldSkipFrame() {
        long now = SystemClock.elapsedRealtime();
        return (now - lastFrameTime) < MIN_FRAME_INTERVAL;
    }

    void updateTexture() {
        safeUpdateTexture();
    }

    private int[] adjustSrcForOrientation(int w, int h) {
        boolean portrait = context.getResources()
                .getConfiguration()
                .orientation == Configuration.ORIENTATION_PORTRAIT;
        return portrait ? new int[]{h, w} : new int[]{w, h};
    }

    public void drawFrame() {
        if (mClosing || eglCore == null || !eglIsReady) return;
        
        // Check for buffer overflow
        if (mBufferOverflow) {
            Log.w(TAG, "Buffer overflow detected, clearing pending frames");
            clearPendingFrames();
            mBufferOverflow = false;
        }

        mCameraHandler.post(() -> {
            if (shouldSkipFrame()) {
                return;
            }
            
            // Track frame timing for performance monitoring
            long currentFrameTime = System.nanoTime();
            if (mLastFrameTimeNs != 0) {
                long frameInterval = currentFrameTime - mLastFrameTimeNs;
                if (frameInterval > SLOW_FRAME_THRESHOLD_NS * 2) {
                    mDroppedFrames++;
                    Log.w(TAG, "Dropped frame detected, interval: " + (frameInterval / 1_000_000) + "ms");
                }
            }
            mLastFrameTimeNs = currentFrameTime;
            mFrameCount++;

            if (mServiceType == ServiceType.BgAudio) {
                // Draw a blank frame with overlay for BgAudio
                drawBlankFrameWithOverlay();
                return;
            }

            if (mServiceType == ServiceType.BgScreenCast) {
                try {
                    // Draw to encoder surface
                    if (encoderSurface != null) {
                        encoderSurface.makeCurrent();
                        GLES20.glViewport(0, 0, videoSize.width, videoSize.height);
                        GLES20.glClearColor(0, 0, 0, 1f);
                        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                        safeUpdateTexture();
                        fullFrameBlit.drawFrame(textureId, mTmpMatrix);
                        // Draw timestamp on encoder surface
                        if (AppPreference.getBool(AppPreference.KEY.TIMESTAMP, true)) {
                            drawTimestampOverlay(videoSize.width,videoSize.height,srcW,srcH);
                        }
                        encoderSurface.setPresentationTime(System.nanoTime());
                        encoderSurface.swapBuffers();
                    }

                    // Draw to recorder surface
                    if (recorderSurface != null) {
                        recorderSurface.makeCurrent();
                        GLES20.glViewport(0, 0, recordSize.width, recordSize.height);
                        GLES20.glClearColor(0, 0, 0, 1f);
                        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                        fullFrameBlit.drawFrame(textureId, mTmpMatrix);

                        // Draw timestamp on recorder surface
                        if (AppPreference.getBool(AppPreference.KEY.TIMESTAMP, true)) {
                            drawTimestampOverlay(recordSize.width,recordSize.height,srcW,srcH);
                        }

                        // Handle screenshot if requested
                        if (should_snapshot) {
                            doSnapshot();
                        }

                        recorderSurface.setPresentationTime(System.nanoTime());
                        recorderSurface.swapBuffers();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Draw frame failed", e);
                }
                return;
            }
            try {
                makeTextureCurrent();

                if (mServiceType == ServiceType.BgAudio) {
                    drawBlankFrameWithOverlay();
                } else {
                    updateTexture();
                    if (displaySurface != null) {
                        if (mServiceType == ServiceType.BgUSBCamera) {
                            drawToSurface(displaySurface,
                                    srcW, srcH,
                                    mScreenWidth, mScreenHeight);
                        } else {
                            drawToSurface(displaySurface,
                                    srcW, srcH,
                                    srcW, srcH);
                        }
                    }

                    if (encoderSurface != null) {
                        int[] src = adjustSrcForOrientation(srcW, srcH);
                        if (mServiceType == ServiceType.BgUSBCamera) {
                            drawToSurface(encoderSurface,
                                    videoSize.width, videoSize.height,
                                    src[0], src[1]);
                        }else {
                            drawToSurface(encoderSurface,
                                    videoSize.width, videoSize.height,
                                    srcW, srcH);
                        }
                    }

                    if (recorderSurface != null) {
                        int[] src = adjustSrcForOrientation(srcW, srcH);
                        if (mServiceType == ServiceType.BgUSBCamera) {
                            drawToSurface(recorderSurface,
                                    recordSize.width, recordSize.height,
                                    src[0], src[1]);
                        }else {
                            drawToSurface(recorderSurface,
                                    recordSize.width, recordSize.height,
                                    srcW, srcH);
                        }

                        if (should_snapshot) {
                            doSnapshot();
                        }
                    }
                }
                GLES20.glFlush();
                GLES20.glFinish();
                Matrix.setIdentityM(mBaseMatrix, 0);
            } catch (Exception e) {
                handleError("Frame drawing failed", e);
            } finally {
                // Safely release texture image with proper error handling
                safeReleaseTextureImage();
                monitorResources();
            }
        });
    }

    private void drawToSurface(WindowSurfaceNew surface, int dstW, int dstH, int srcW, int srcH) {
        mCameraHandler.post(() -> {
            if (surface == null) return;
            //   logSurfaceInfo(TAG);
            try {
                surface.makeCurrent();

                // Clear to black
                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                if (srcW > 0 && srcH > 0) {
                    // Calculate aspect-correct viewport
                    float srcAspect = (float)srcW / srcH;
                    float dstAspect = (float)dstW / dstH;
                    int vpWidth, vpHeight, vpX, vpY;

                    if (srcAspect > dstAspect) {
                        // Source is wider - fit to width
                        vpWidth = dstW;
                        vpHeight = (int)(dstW / srcAspect);
                        vpX = 0;
                        vpY = (dstH - vpHeight) / 2;
                    } else {
                        // Source is taller - fit to height
                        vpHeight = dstH;
                        vpWidth = (int)(dstH * srcAspect);
                        vpX = (dstW - vpWidth) / 2;
                        vpY = 0;
                    }

                    // Set viewport to maintain aspect ratio
                    GLES20.glViewport(vpX, vpY, vpWidth, vpHeight);

                    // Apply transformations
                    boolean wantMirror = mIsMirrored;
                    boolean wantFlip = mIsFlipped;
                    if (wantMirror && wantFlip) {
                        fullFrameBlit.drawFlipMirror(textureId, mTmpMatrix, mRotation);
                    } else if (wantMirror) {
                        fullFrameBlit.drawFrameMirrorY(textureId, mTmpMatrix, (mRotation + 180) % 360, 1f);
                    } else if (wantFlip) {
                        fullFrameBlit.drawFrameMirrorY(textureId, mTmpMatrix, mRotation, 1f);
                    } else {
                        fullFrameBlit.drawFrameX(textureId, mTmpMatrix, mRotation, 1f);
                    }
                }

                // Draw overlay if enabled
                if (AppPreference.getBool(AppPreference.KEY.TIMESTAMP, true)) {
                    drawTimestampOverlay(dstW, dstH,srcW,srcH);     // NEW – single call
                }


                surface.setPresentationTime(System.nanoTime());
                surface.swapBuffers();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Surface invalid - releasing", e);
                surface.release();
                if (surface == displaySurface) displaySurface = null;
            }
        });
    }

    public void drawBlankFrameWithOverlay() {
        if (displaySurface != null) {
            drawToSurface(displaySurface,
                    mScreenWidth, mScreenHeight,
                    0, 0);
        }

        if (encoderSurface != null) {
            try {
                encoderSurface.makeCurrent();
                // Full‐screen clear with black color
                GLES20.glViewport(0, 0, videoSize.width, videoSize.height);
                GLES20.glDisable(GLES20.GL_DEPTH_TEST);
                GLES20.glClearColor(0, 0, 0, 1);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                // Draw timestamp overlay if enabled
                if (AppPreference.getBool(AppPreference.KEY.TIMESTAMP, true)) {
                    drawTimestampOverlay(recordSize.width, recordSize.height, srcW, srcH);
                }

                // Set presentation time
                long ts = System.nanoTime();
                encoderSurface.setPresentationTime(ts);
                encoderSurface.swapBuffers();
            } catch (Exception e) {
                Log.e(TAG, "Error rendering blank+overlay frame", e);
            }
        }

        if (recorderSurface != null) {
            drawToSurface(recorderSurface,
                    recordSize.width, recordSize.height,
                    srcW, srcH);
            if (should_snapshot) {
                doSnapshot();
            }
        }
    }

    public boolean isTextureReleased() {
        return cameraTexture == null || cameraTexture.isReleased();
    }

    private void doSnapshot() {
        try {
            if (!eglIsReady || eglCore == null) {
                Log.e(TAG, "Cannot take snapshot - EGL not ready");
                should_snapshot = false;
                return;
            }

            if (recorderSurface == null) {
                Log.e(TAG, "Recorder surface is null");
                should_snapshot = false;
                return;
            }

            // Make sure we're on the correct thread
            mCameraHandler.post(() -> {
                try {
                    if (!eglIsReady || eglCore == null) {
                        Log.e(TAG, "EGL context became invalid during snapshot");
                        should_snapshot = false;
                        return;
                    }

                    // Make the surface current and ensure frame is rendered
                    recorderSurface.makeCurrent();

                    // Clear to black and draw the frame
                    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                    // Draw the frame
                    if (cameraTexture != null && !cameraTexture.isReleased()) {
                        cameraTexture.updateTexImage();
                        cameraTexture.getTransformMatrix(mTmpMatrix);
                        fullFrameBlit.drawFrame(textureId, mTmpMatrix);
                    } else {
                        Log.e(TAG, "Camera texture is null or released");
                        should_snapshot = false;
                        return;
                    }

                    // Draw overlay if enabled
                    if (AppPreference.getBool(AppPreference.KEY.TIMESTAMP, true)) {
                        drawTimestampOverlay(recordSize.width, recordSize.height,srcW,srcH);
                    }

                    // Flush and finish to ensure frame is complete
                    GLES20.glFlush();
                    GLES20.glFinish();

                    // Now take the snapshot
                    File file = getTempImageFile(context);
                    if (file == null) {
                        Log.e(TAG, "Failed to create temporary image file");
                        should_snapshot = false;
                        return;
                    }

                    recorderSurface.saveFrame(file);

                    String storage_location = AppPreference.getStr(AppPreference.KEY.STORAGE_LOCATION, "");
                    if (storage_location.isEmpty()) {
                        Log.e(TAG, "Storage location not set");
                        should_snapshot = false;
                        return;
                    }

                    Uri treeUri = Uri.parse(storage_location);
                    boolean encrypt = AppPreference.getBool(AppPreference.KEY.FILE_ENCRYPTION, false);
                    String key = encrypt ? 
                        AppPreference.getStr(AppPreference.KEY.ENCRYPTION_KEY, "12345678") : 
                        null;
                    
                    final String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                    final String fileExtension = encrypt ? ".t3j" : ".jpg";
                    final String fileType = "photo";

                    // Save and log file
                    saveAndLogFile(file, treeUri, fileName, fileExtension, fileType, encrypt, key);
                    
                    MessageUtil.showToast(context, R.string.Screenshot_taken);
                } catch (Exception e) {
                    Log.e(TAG, "Error taking snapshot", e);
                } finally {
                    should_snapshot = false;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Snapshot failed", e);
            should_snapshot = false;
        }
    }

    private boolean shouldAttemptReconnect(Streamer.STATUS status) {
        switch (status) {
            case AUTH_FAIL:
            case CONN_FAIL:
            case UNKNOWN_FAIL:
                return false;
            default:
                return true;
        }
    }

    private void scheduleReconnect(int connectionId, Connection connection) {
        mCameraHandler.post(() -> {
            int attempts = mReconnectAttempts.getOrDefault(connectionId, 0);
            long lastAttemptTime = mLastReconnectTime.getOrDefault(connectionId, 0L);
            long now = System.currentTimeMillis();

            if (attempts >= MAX_RECONNECT_ATTEMPTS) {
                releaseConnection(connectionId);
                return;
            }

            long delay = Math.min(
                    INITIAL_RECONNECT_DELAY_MS * (1 << attempts),
                    MAX_RECONNECT_DELAY_MS
            );

            long timeSinceLastAttempt = now - lastAttemptTime;
            if (timeSinceLastAttempt < delay) {
                delay = delay - timeSinceLastAttempt;
            }

            mCameraHandler.postDelayed(() -> {
                if (!mConnectionId.containsKey(connectionId)) {
                    return;
                }

                int newConnectionId = createConnection(connection);
                if (newConnectionId != -1) {
                    mConnectionId.remove(connectionId);
                    mConnectionId.put(newConnectionId, connection);
                    mReconnectAttempts.put(newConnectionId, attempts + 1);
                    mLastReconnectTime.put(newConnectionId, System.currentTimeMillis());

                    ConnectionStatistics stats = mConnectionStatistics.remove(connectionId);
                    if (stats != null) {
                        mConnectionStatistics.put(newConnectionId, stats);
                    }
                } else {
                    scheduleReconnect(connectionId, connection);
                }
            }, delay);

            mReconnectAttempts.put(connectionId, attempts + 1);
            mLastReconnectTime.put(connectionId, now);
        });
    }

    private void createStreamer() {
        Streamer.Listener streamerListener = new Streamer.Listener() {
            @Override
            public Handler getHandler() {
                return mCameraHandler.getMainThreadHandler();
            }

            @Override
            public void onConnectionStateChanged(int connectionId, Streamer.CONNECTION_STATE state,
                                                 Streamer.STATUS status, JSONObject info) {
                if (mConnectionId.get(connectionId) == null) {
                    if (state == Streamer.CONNECTION_STATE.DISCONNECTED) {
                        Connection connection = mConnectionId.get(connectionId);
                        if (connection == null) {
                            return;
                        }

                        String errorMsg = ErrorMessage.connectionErrorMsg(context, connection, status, info);
                        Log.w(TAG, "Disconnected: " + errorMsg);

                        if (shouldAttemptReconnect(status)) {
                            scheduleReconnect(connectionId, connection);
                        } else {
                            releaseConnection(connectionId);
                        }
                    } else if (state == Streamer.CONNECTION_STATE.CONNECTED) {
                        mReconnectAttempts.remove(connectionId);
                        mLastReconnectTime.remove(connectionId);
                    }
                    return;
                }
                mConnectionState.put(connectionId, state);
                switch (state) {
                    case CONNECTED:
                        ConnectionStatistics statistics = mConnectionStatistics.get(connectionId);
                        if (statistics != null) {
                            statistics.init(mStreamer, connectionId);
                        }
                        break;
                    case DISCONNECTED:
                        final Connection connection = mConnectionId.get(connectionId);
                        releaseConnection(connectionId);
                        String errorText = ErrorMessage.connectionErrorMsg(context, connection, status, info);
                        if (!errorText.isEmpty()) {
                            // Store error per connection name
                        }
                        if (status != Streamer.STATUS.AUTH_FAIL) {
                            mCameraHandler.postDelayed(new RetryRunnable(connection), RETRY_TIMEOUT);
                            mRetryPending.incrementAndGet();
                        }
                        break;
                }
            }

            @Override
            public void onVideoCaptureStateChanged(Streamer.CAPTURE_STATE state) {
                Log.d(TAG, "Video capture state: " + state);
            }

            @Override
            public void onAudioCaptureStateChanged(Streamer.CAPTURE_STATE state) {
                Log.d(TAG, "Audio capture state: " + state);
            }

            @Override
            public void onRecordStateChanged(Streamer.RECORD_STATE state, Uri uri, Streamer.SAVE_METHOD method) {}

            @Override
            public void onSnapshotStateChanged(Streamer.RECORD_STATE state, Uri uri, Streamer.SAVE_METHOD method) {}
        };

        StreamerSurfaceBuilder builder = new StreamerSurfaceBuilder();
        builder.setContext(context);
        builder.setListener(streamerListener);

        VideoConfig videoConfig = new VideoConfig();
        AudioConfig audioConfig = new AudioConfig();
        Streamer.MODE mode = null;
        int audioSetting = AppPreference.getInt(AppPreference.KEY.AUDIO_OPTION_AUDIO_SETTING, 0);

        switch (mServiceType) {
            case BgCamera:
                videoConfig.videoSize = SettingsUtils.getStreamVideoSize(findCameraInfo());
                setSourceSize(videoSize.width, videoSize.height);
                videoConfig.keyFrameInterval = SettingsUtils.keyFrameIntervalStream(context);
                videoConfig.bitRate = SettingsUtils.streamingBitRate(context);
                videoConfig.fps = SettingsUtils.findStreamFps(context, Objects.requireNonNull(findCameraInfo()).fpsRanges);
                builder.setVideoConfig(videoConfig);
                builder.setAudioConfig(SettingsUtils.audioConfig(context));
                mode = SettingsUtils.streamerMode();
                break;
            case BgUSBCamera:
                if (sharedViewModel != null) {
                    String[] arr = sharedViewModel.getCameraResolution().toArray(new String[0]);
                    videoConfig.videoSize = SettingsUtils.getStreamVideoSizeUSBNew(arr);
                } else {
                    videoConfig.videoSize = SettingsUtils.getStreamVideoSizeWithInfoUSB(findCameraInfo());
                }
                setSourceSize(videoConfig.videoSize.width, videoConfig.videoSize.height);
                videoConfig.keyFrameInterval = SettingsUtils.keyFrameIntervalStream(context);
                videoConfig.bitRate = SettingsUtils.getUSBStreamBitrate(context);
                int min = AppPreference.getInt(AppPreference.KEY.USB_MIN_FPS, 30);
                int max = AppPreference.getInt(AppPreference.KEY.USB_MAX_FPS, 30);
                videoConfig.fps = (float) (min + max) / 2;
                builder.setVideoConfig(videoConfig);
                audioConfig = SettingsUtils.audioConfig(context);
                builder.setAudioConfig(audioConfig);
                break;
            case BgScreenCast:
                videoConfig.videoSize = videoSize;
                videoConfig.keyFrameInterval = SettingsUtils.keyFrameIntervalVideo(context);
                videoConfig.bitRate = SettingsUtils.castBitRate(context);
                videoConfig.fps = SettingsUtils.fpsVideo(context);
                builder.setVideoConfig(videoConfig);
                if (audioSetting == 0) {
                    mode = Streamer.MODE.VIDEO_ONLY;
                } else {
                    mode = Streamer.MODE.AUDIO_VIDEO;
                }
                AudioConfig pcmCfg = new AudioConfig();
                if (audioSetting == 4) {
                    pcmCfg.type = AudioConfig.INPUT_TYPE.PCM;
                    pcmCfg.sampleRate = 320000;
                    pcmCfg.channelCount = 2;
                    builder.setAudioConfig(pcmCfg);
                } else {
                    builder.setAudioConfig(SettingsUtils.audioOptionConfig(context));
                }
                if (audioSetting == 1) {
                    toggleMediaVolume(true);
                    mStreamer.startAudioCapture();
                } else if (audioSetting == 2) {
                    toggleMediaVolume(false);
                } else if (audioSetting == 3) {
                    toggleMediaVolume(false);
                } else if (audioSetting == 4) {
                    toggleMediaVolume(false);
                } else {
                    toggleMediaVolume(false);
                }
                break;
            case BgAudio:
                videoConfig.videoSize = SettingsUtils.getStreamVideoSize(findCameraInfo());
                videoConfig.keyFrameInterval = SettingsUtils.keyFrameIntervalStream(context);
                videoConfig.bitRate = SettingsUtils.audioBitRate(context);
                CameraInfo activeCamera = findCameraInfo();
                videoConfig.fps = SettingsUtils.findStreamFps(context, activeCamera.fpsRanges);
                builder.setVideoConfig(videoConfig);
                audioConfig = SettingsUtils.audioConfig(context);
                builder.setAudioConfig(audioConfig);
                mode = SettingsUtils.streamerMode();
                break;
        }

        mStreamer = builder.build(mode);
        if (mStreamer == null) {
            mStreamer = builder.build(Streamer.MODE.VIDEO_ONLY);
        }

        mRadioMode = AppPreference.getBool(AppPreference.KEY.STREAMING_RADIO_MODE, false);
        if (!mRadioMode) {
            mStreamer.startVideoCapture();
        }
        if (mServiceType == ServiceType.BgScreenCast) {
            if (audioSetting == 4) {
                configureAudioForPro();
            } else if (audioSetting == 2) {
                AudioPlaybackCaptureConfiguration config = new AudioPlaybackCaptureConfiguration.Builder(mMediaProjection)
                        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                        .addMatchingUsage(AudioAttributes.USAGE_GAME)
                        .build();
                mStreamer.setAudioPlaybackCaptureConfiguration(config);
                mStreamer.startAudioCapture();
            } else {
                mStreamer.startAudioCapture();
            }
            createStreamConditioner();
        } else {
            if (SettingsUtils.isAllowedAudio() || (mode == Streamer.MODE.AUDIO_VIDEO || mode == Streamer.MODE.AUDIO_ONLY)) {
                startAudioCapture();
            }
            setConditioner(StreamConditionerBase.newInstance(context));
        }
    }

    private void createRecorder() {
        Streamer.Listener recListener = new Streamer.Listener() {
            @Override
            public Handler getHandler() {
                return mCameraHandler.getMainThreadHandler();
            }

            @Override
            public void onConnectionStateChanged(int connectionId, Streamer.CONNECTION_STATE state,
                                                 Streamer.STATUS status, JSONObject info) {
                Log.d(TAG, "Recorder state: " + state);
            }

            @Override
            public void onVideoCaptureStateChanged(Streamer.CAPTURE_STATE state) {
                Log.d(TAG, "Recorder video state: " + state);
            }

            @Override
            public void onAudioCaptureStateChanged(Streamer.CAPTURE_STATE state) {
                Log.d(TAG, "Recorder audio state: " + state);
            }

            @Override
            public void onRecordStateChanged(Streamer.RECORD_STATE state, Uri uri, Streamer.SAVE_METHOD method) {}

            @Override
            public void onSnapshotStateChanged(Streamer.RECORD_STATE state, Uri uri, Streamer.SAVE_METHOD method) {}
        };

        StreamerSurfaceBuilder builder = new StreamerSurfaceBuilder();
        builder.setContext(context);
        builder.setListener(recListener);

        VideoConfig videoConfig = new VideoConfig();
        Streamer.MODE mode = null;
        int audioSetting = AppPreference.getInt(AppPreference.KEY.AUDIO_OPTION_AUDIO_SETTING, 0);

        switch (mServiceType) {
            case BgCamera:
                videoConfig.videoSize = SettingsUtils.getVideoSize(findCameraInfo());
                videoConfig.keyFrameInterval = SettingsUtils.keyFrameIntervalVideo(context);
                videoConfig.bitRate = SettingsUtils.videoBitRate(context);
                videoConfig.fps = SettingsUtils.fpsVideo(context);
                builder.setVideoConfig(videoConfig);
                builder.setAudioConfig(SettingsUtils.audioConfig(context));
                mode = SettingsUtils.streamerMode();
                break;
            case BgUSBCamera:
                videoConfig.videoSize = recordSize;
                videoConfig.keyFrameInterval = SettingsUtils.keyFrameIntervalStream(context);
                videoConfig.bitRate = SettingsUtils.getUSBStreamBitrate(context);
                int min = AppPreference.getInt(AppPreference.KEY.USB_MIN_FPS, 30);
                int max = AppPreference.getInt(AppPreference.KEY.USB_MAX_FPS, 30);
                videoConfig.fps = (float) (min + max) / 2;
                builder.setVideoConfig(videoConfig);
                builder.setAudioConfig(SettingsUtils.audioConfig(context));
                mode = SettingsUtils.streamerMode();
                break;
            case BgScreenCast:
                videoConfig.videoSize = videoSize;
                videoConfig.keyFrameInterval = SettingsUtils.keyFrameIntervalVideo(context);
                videoConfig.bitRate = SettingsUtils.castBitRate(context);
                videoConfig.fps = SettingsUtils.fpsVideo(context);
                builder.setVideoConfig(videoConfig);

                if (audioSetting == 0) {
                    mode = Streamer.MODE.VIDEO_ONLY;
                } else {
                    mode = Streamer.MODE.AUDIO_VIDEO;
                }

                AudioConfig pcmCfg = new AudioConfig();
                if (audioSetting == 4) {
                    pcmCfg.type = AudioConfig.INPUT_TYPE.PCM;
                    pcmCfg.sampleRate = 320000;
                    pcmCfg.channelCount = 2;
                    builder.setAudioConfig(pcmCfg);
                } else {
                    builder.setAudioConfig(SettingsUtils.audioOptionConfig(context));
                }
                break;
            case BgAudio:
                videoConfig.videoSize = SettingsUtils.getVideoSize(findCameraInfo());
                videoConfig.keyFrameInterval = SettingsUtils.keyFrameIntervalVideo(context);
                videoConfig.bitRate = SettingsUtils.audioBitRate(context);
                videoConfig.fps = SettingsUtils.fpsVideo(context);
                builder.setVideoConfig(videoConfig);
                builder.setAudioConfig(SettingsUtils.audioConfig(context));
                mode = SettingsUtils.streamerMode();
                break;
        }

        if (MainActivity.instance != null) {
            if (MainActivity.instance.isCastStreaming()) {
                builder.setAudioConfig(SettingsUtils.audioOptionConfig(context));
            } else {
                builder.setAudioConfig(SettingsUtils.audioConfig(context));
            }
        }

        mRecorder = builder.build(mode);
        if (mRecorder == null) {
            mRecorder = builder.build(Streamer.MODE.VIDEO_ONLY);
        }

        mRecorder.startVideoCapture();
        if (mServiceType == ServiceType.BgScreenCast) {
            if (audioSetting == 4) {
                // configureAudioForPro handles both streamer and recorder
            } else if (audioSetting == 2) {
                AudioPlaybackCaptureConfiguration config = new AudioPlaybackCaptureConfiguration.Builder(mMediaProjection)
                        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                        .addMatchingUsage(AudioAttributes.USAGE_GAME)
                        .build();
                mRecorder.setAudioPlaybackCaptureConfiguration(config);
                mRecorder.startAudioCapture();
            } else if (audioSetting != 0) {
                mRecorder.startAudioCapture();
            }


        } else {
            if (SettingsUtils.isAllowedAudio() || (mode == Streamer.MODE.AUDIO_VIDEO || mode == Streamer.MODE.AUDIO_ONLY)) {
                mRecorder.startAudioCapture();
            }
        }
    }

    private void configureAudioForPro() {
        int capOut = ActivityCompat.checkSelfPermission(context.getApplicationContext(), "android.permission.CAPTURE_AUDIO_OUTPUT");
        if (capOut == PackageManager.PERMISSION_GRANTED) {
            mStreamer.startAudioCapture();
            mStreamer.writePcmData(new byte[2048]);
            audioMgr = new DualCallAudioCapture(context.getApplicationContext(), mMediaProjection, mStreamer, mRecorder);
            audioMgr.start();
        } else {
            if (mCastRecorderMic != null) {
                mCastRecorderMic.interrupt();
                mCastRecorderMic = null;
            }
            if (mCastStreamMic != null) {
                mCastStreamMic.interrupt();
                mCastStreamMic = null;
            }
            mStreamer.startAudioCapture();
            mStreamer.writePcmData(new byte[2048]);
            streamAudioConfigLocal.type = AudioConfig.INPUT_TYPE.PCM;
            streamAudioConfigLocal.audioSource = MediaRecorder.AudioSource.VOICE_RECOGNITION;
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                audioManager.setSpeakerphoneOn(true);
            }
            streamAudioConfigLocal.sampleRate = 16000;
            streamAudioConfigLocal.channelCount = 1;
            streamAudioConfigLocal.bitRate = 32000;

            mCastStreamMic = new CallMicThread(context, streamAudioConfigLocal);
            mCastStreamMic.mStreamer = mStreamer;
            mCastStreamMic.start();

            AudioConfig localConfig = new AudioConfig();
            localConfig.type = AudioConfig.INPUT_TYPE.PCM;
            localConfig.audioSource = MediaRecorder.AudioSource.VOICE_RECOGNITION;
            localConfig.sampleRate = 44100;
            localConfig.channelCount = 1;
            localConfig.bitRate = 32000;
            if (mCastRecorderMic != null) {
                mCastRecorderMic.interrupt();
                mCastRecorderMic = null;
            }
            mCastRecorderMic = new CallMicThread(context, localConfig, true);
            mCastRecorderMic.mRecorder = mRecorder;
            mCastRecorderMic.mStreamer = null;
            mCastRecorderMic.start();
        }
    }
    private void createConnection() {
        List<Connection> list = SettingsUtils.connections();
        if (list.isEmpty()) {
            Log.w(TAG, "createConnection(): no profiles configured");
            return;
        }
        Connection first = list.get(0);
        int id = createConnection(first);
        if (id != -1) {
            if (mServiceType == ServiceType.BgScreenCast) {
                mBroadcastStartTime = System.currentTimeMillis();
                if (mStreamConditioner1 != null) {
                    mStreamConditioner1.addConnection(id);
                }else if (mStreamConditioner2 != null) {
                    mStreamConditioner2.addConnection(id);
                }
            }else {

                if (mConditioner != null) {
                    mBroadcastStartTime = System.currentTimeMillis();
                    mConditioner.addConnection(id);
                }
            }

        }
    }

    private int createConnection(Connection connection) {
        final String scheme = Uri.parse(connection.url).getScheme();
        if (!SettingsUtils.UriResult.isSupported(scheme)) {
            return -1;
        }

        if (mStreamer == null) {
            return -1;
        }

        try {
            final int connectionId;
            if (SettingsUtils.UriResult.isSrt(scheme)) {
                connectionId = mStreamer.createConnection(SettingsUtils.toSrtConfig(connection));
            } else if (SettingsUtils.UriResult.isRist(scheme)) {
                connectionId = mStreamer.createConnection(SettingsUtils.toRistConfig(connection));
            } else {
                connectionId = mStreamer.createConnection(SettingsUtils.toConnectionConfig(connection));
            }

            if (connectionId != -1) {
                mConnectionId.put(connectionId, connection);
                mConnectionStatistics.put(connectionId, new ConnectionStatistics());
            }
            return connectionId;
        } catch (Exception e) {
            handleError("Connection failed", e);
            restartService();
            return -1;
        }
    }

    public static void softRestart(Context ctx) {
        Intent restartIntent = new Intent(ctx, SoftRestartActivity.class);
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        ctx.startActivity(restartIntent);
    }
    private void restartService() {
        if (mServiceType == null) {
            Log.e(TAG, "Service type is null, cannot restart service");
            return;
        }
        resetSharedEgl();
    }

    public void resetSharedEgl() {
        // Otherwise post to the EGL thread and wait until it finishes
        CountDownLatch latch = new CountDownLatch(1);

        SharedEglManager.mCameraHandler.post(() -> {
            SharedEglManager.cleanAndReset();
            latch.countDown();                       // notify caller that we're done
        });

        try {
            latch.await();                           // "lock till clean"
        } catch (InterruptedException ignored) { }
    }

    public void toggleMediaVolume(boolean mute) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) return;

        if (mute) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
        } else {
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
        }
    }

    private void releaseConnection(int connectionId) {
        mReconnectAttempts.remove(connectionId);
        mLastReconnectTime.remove(connectionId);
        if (mStreamer != null && connectionId != -1) {
            mStreamer.releaseConnection(connectionId);
            mConnectionId.remove(connectionId);
            mConnectionState.remove(connectionId);
            mConnectionStatistics.remove(connectionId);
        }
    }

    public void startStreaming() {
        mCameraHandler.post(() -> {
            if (mStreamer == null) {
                restartService();;
                return;
            }

            mStreaming = true;
            createConnection();
            mCameraHandler.postDelayed(mConnectionCheckRunnable, CONNECTION_TIMEOUT);
            mCameraHandler.post(updateRunnable);
            if (AppPreference.getBool(AppPreference.KEY.RECORD_BROADCAST, false)) {
                startRecording();
            }
            mCameraHandler.postDelayed(mUpdateStatistics, STATISTICS_TIMEOUT);
        });
    }

    public void runnableInit() {
        releaseEgl();
        initializeEGL();
        reopenAttempts = 0;
    }

    private void checkConnectionStatus() {
        if (mConnectionId.isEmpty()) {
            handleError("Connection timeout");
            scheduleReopen();
        }
    }

    public void stopStreaming() {
        mCameraHandler.post(() -> {
            for (Integer id : new ArrayList<>(mConnectionId.keySet())) {
                releaseConnection(id);
            }

            if (mConditioner != null) {
                mConditioner.stop();
            }else if (mStreamConditioner1 != null) {
                mStreamConditioner1.stop();
            }else if (mStreamConditioner2 != null) {
                mStreamConditioner2.stop();
            }
            mStreaming = false;
            mConnectionId.clear();
            mConnectionState.clear();
            mConnectionStatistics.clear();

            if (AppPreference.getBool(AppPreference.KEY.RECORD_BROADCAST, false)) {
                stopRecording(false);
            }
        });
    }

    public boolean startRecording() {
        mCameraHandler.post(() -> {
            if (mRecorder == null) {
                Log.e(TAG, "Recorder is null, cannot start recording");
                return;
            }

            // Only stop if we're already recording
            if (mRecording) {
                stopRecording(true);
            }

            try {
                String storage_location = AppPreference.getStr(AppPreference.KEY.STORAGE_LOCATION, "");
                if (storage_location.isEmpty()) {
                    Log.e(TAG, "Storage location not set");
                    return;
                }

                selectedTreeUri = Uri.parse(storage_location);
                tempRecordingFile = getTempRecordingFile(context);

                mRecorder.startRecord(tempRecordingFile);
                mRecording = true;

                int splitTime = AppPreference.getInt(AppPreference.KEY.SPLIT_TIME, 10);
                SPLIT_INTERVAL_MS = (long) splitTime * 60 * 1000;
                mCameraHandler.removeCallbacks(mSplitRunnable);
                mCameraHandler.postDelayed(mSplitRunnable, SPLIT_INTERVAL_MS);

                Log.d(TAG, "Recording started successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to start recording", e);
                mRecording = false;
            }
        });
        return true;
    }

    public void stopRecording(boolean isFromStartRecording) {
        mCameraHandler.post(() -> {
            if (!isFromStartRecording) {
                mCameraHandler.removeCallbacks(mSplitRunnable);
            }

            if (mRecorder != null) {
                mRecorder.stopRecord();
                mRecording = false;

                if (!isFromStartRecording) {
                    finishRecording();
                }
            }
        });
    }

    private void restartRecording() {
        if (mRecorder != null && mRecording) {  // Add check for mRecording
            stopRecording(false);
            startRecording();
        }
    }

    private void finishRecording() {
        if (tempRecordingFile == null || selectedTreeUri == null) return;

        final long fileSize = tempRecordingFile.length();
        if (fileSize <= 512) {
            tempRecordingFile.delete();
            return;
        }

        final boolean encrypt = AppPreference.getBool(AppPreference.KEY.FILE_ENCRYPTION, false);
        final String key = encrypt ? 
            AppPreference.getStr(AppPreference.KEY.ENCRYPTION_KEY, "12345678") : 
            null;
        
        final String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        final String fileExtension = encrypt ? ".t3v" : ".mp4";
        final String fileType = "video";

        // Save and log file
        saveAndLogFile(tempRecordingFile, selectedTreeUri, fileName, fileExtension, fileType, encrypt, key);
    }

    // In your timestamp update method
    public void setTextForTime(String text) {
        if (text == null || text.equals(lastOverlayText) || overlay == null) {
            return;
        }

        mCameraHandler.post(() -> {
            synchronized (overlayLock) {
                if (overlay == null) return;

                // Create bitmap with text
                Bitmap timestampBitmap = createTimestampBitmap(text);

                // Set the bitmap to the overlay
                overlay.setImage(timestampBitmap);
                lastOverlayText = text;
            }
        });
    }

    private Bitmap createTimestampBitmap(String text) {
        // ---------- paint for the text (BLACK) -----------------------
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(40f);                    // change size if needed
        paint.setColor(Color.BLACK);               // <<< TEXT = BLACK
        paint.setTypeface(Typeface.DEFAULT_BOLD);

        // ---------- measure the text ---------------------------------
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        int pad = 10;                              // uniform padding
        int bmpW = bounds.width()  + pad * 2;
        int bmpH = bounds.height() + pad * 2;

        Bitmap bitmap = Bitmap.createBitmap(bmpW, bmpH,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // ---------- background (WHITE) -------------------------------
        canvas.drawColor(Color.WHITE);             // <<< BG = WHITE

        // ---------- draw the text ------------------------------------
        float x = pad;
        float y = pad + bounds.height();           // baseline = +height
        canvas.drawText(text, x, y, paint);

        return bitmap;
    }
    public void setUseBluetooth(boolean enabled) {
        if (mUseBluetooth == enabled) return;
        mUseBluetooth = enabled;

        if (enabled) registerBluetoothReceiver();
        else unregisterBluetoothReceiver();

        if (mStreaming || mRecording) startAudioCapture();
    }

    private void logPerformanceMetrics() {
        Log.d(TAG, String.format("PERF: Texture=%dms, Draw=%dms, Swap=%dms",
                textureUpdateTime, drawTime, swapBufferTime));
    }

    public void setRotation(int rotation) {
        mRotation = rotation;
    }

    public void setMirror(boolean mirror) {
        mIsMirrored = mirror;
    }

    public void setFlip(boolean flip) {
        mIsFlipped = flip;
    }

    public void setNormal() {
        mIsFlipped = false;
        mIsMirrored = false;
        mRotation = 0;
        mDisplayOrientation = 0;
    }

    public void updateDisplayOrientation() {
        mDisplayOrientation = 0;
    }

    public static File getTempRecordingFile(Context context) {
        File tempDir = new File(context.getExternalFilesDir(null), "temp");
        if (!tempDir.exists()) tempDir.mkdirs();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return new File(tempDir, timeStamp + ".mp4");
    }

    public static File getTempImageFile(Context context) {
        File tempDir = new File(context.getExternalFilesDir(null), "temp");
        if (!tempDir.exists()) tempDir.mkdirs();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return new File(tempDir, timeStamp + ".jpg");
    }

    public static boolean copyTempFileToDestination(File sourceFile, Uri destinationTreeUri) {
        if (destinationTreeUri == null || !DocumentsContract.isTreeUri(destinationTreeUri)) {
            return false;
        }

        DocumentFile pickedDir = DocumentFile.fromTreeUri(MyApp.getContext(), destinationTreeUri);
        if (pickedDir == null || !pickedDir.canWrite()) {
            return false;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = timeStamp + ".mp4";
        DocumentFile destFile = pickedDir.createFile("video/mp4", fileName);
        if (destFile == null) {
            return false;
        }

        try (InputStream in = new FileInputStream(sourceFile);
             OutputStream out = MyApp.getContext().getContentResolver().openOutputStream(destFile.getUri())) {
            if (out == null) {
                return false;
            }

            byte[] buffer = new byte[65536];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "File copy failed", e);
            return false;
        }
    }

    public static boolean copyAndEncryptTempFileToDestination(File sourceFile, Uri destinationTreeUri,
                                                              String password, String fileType) {
        if (destinationTreeUri == null || !DocumentsContract.isTreeUri(destinationTreeUri)) {
            return false;
        }

        DocumentFile pickedDir = DocumentFile.fromTreeUri(MyApp.getContext(), destinationTreeUri);
        if (pickedDir == null || !pickedDir.canWrite()) {
            return false;
        }

        String ext = ".t3v";
        if ("photo".equalsIgnoreCase(fileType)) {
            ext = ".t3j";
        }

        String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ext;
        DocumentFile destFile = pickedDir.createFile("application/octet-stream", fileName);
        if (destFile == null) {
            return false;
        }

        try (InputStream in = new FileInputStream(sourceFile);
             OutputStream fileOut = MyApp.getContext().getContentResolver().openOutputStream(destFile.getUri())) {
            if (fileOut == null) {
                return false;
            }

            byte[] salt = new byte[16];
            byte[] iv = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(salt);
            random.nextBytes(iv);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 10000, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));

            fileOut.write(salt);
            fileOut.write(iv);

            try (CipherOutputStream cipherOut = new CipherOutputStream(fileOut, cipher)) {
                byte[] buffer = new byte[65536];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    cipherOut.write(buffer, 0, bytesRead);
                }
                cipherOut.flush();
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Encryption failed", e);
            return false;
        }
    }

    public static boolean copyImageFileToDestination(Context context, File sourceFile, Uri destinationTreeUri) {
        if (destinationTreeUri == null) {
            return false;
        }

        DocumentFile pickedDir = DocumentFile.fromTreeUri(context, destinationTreeUri);
        if (pickedDir == null || !pickedDir.canWrite()) {
            return false;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = timeStamp + ".jpg";
        DocumentFile destFile = pickedDir.createFile("image/jpeg", fileName);
        if (destFile == null) {
            return false;
        }

        try (InputStream in = new FileInputStream(sourceFile);
             OutputStream out = context.getContentResolver().openOutputStream(destFile.getUri())) {
            if (out == null) {
                return false;
            }

            byte[] buffer = new byte[65536];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Image copy failed", e);
            return false;
        }
    }

    public void registerBluetoothReceiver() {
        if (context == null || isBlueToothReceiverRegistered) return;

        IntentFilter filter = new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        context.registerReceiver(mBluetoothScoStateReceiver, filter);
        isBlueToothReceiverRegistered = true;
    }

    public void unregisterBluetoothReceiver() {
        if (context == null || !isBlueToothReceiverRegistered) return;

        try {
            context.unregisterReceiver(mBluetoothScoStateReceiver);
            isBlueToothReceiverRegistered = false;
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Bluetooth receiver not registered", e);
        }
    }

    protected void startAudioCapture() {
        if (mServiceType == ServiceType.BgAudio) {
            mStreamer.startAudioCapture(mAudioCallback);
        } else {
            if (!isBlueToothReceiverRegistered && mUseBluetooth) {
                registerBluetoothReceiver();
            }
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (mStreamer == null || am == null) return;

            if (mUseBluetooth) {
                try {
                    am.startBluetoothSco();
                } catch (Exception e) {
                    Log.w(TAG, "Bluetooth error:" + e.getLocalizedMessage());
                }
            } else if (SettingsUtils.isAllowedAudio()) {
                mStreamer.startAudioCapture(mAudioCallback);
            }
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        boolean newLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
        if (newLandscape != isLandscape()) {
            updateOverlayLayout(newLandscape);
        }
    }

    private boolean isLandscape() {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public void updateOverlayLayout(boolean isLandscape) {
        if (isLandscape) {
            overlayMarginX = 50;
            overlayMarginY = 30;
        } else {
            overlayMarginX = 20;
            overlayMarginY = 15;
        }
    }

    public void setPreviewSurface(final SurfaceTexture surfaceTexture, int width, int height) {
        if (mCameraHandler == null) {
            Log.e(TAG, "Camera handler is null, cannot set preview surface");
            return;
        }
        
        mCameraHandler.post(() -> {
            Log.d(TAG, String.format("setPreviewSurface %d x %d", width, height));

            // Validate EGL state
            if (!isEglReady()) {
                Log.w(TAG, "Cannot set preview surface - EGL not ready. Initialized: " + mIsInitialized +
                          ", ShuttingDown: " + mIsShuttingDown + ", EglCore: " + (eglCore != null) +
                          ", EglIsReady: " + eglIsReady);

                // Retry after a delay if EGL is not ready
                if (!mIsShuttingDown) {
                    retrySetPreviewSurface(surfaceTexture, width, height, 0);
                }
                return;
            }

            // Validate surface texture
            if (surfaceTexture == null) {
                Log.w(TAG, "SurfaceTexture is null, cannot create window surface");
                return;
            }

            // Validate dimensions
            if (width <= 0 || height <= 0) {
                Log.w(TAG, "Invalid dimensions: " + width + "x" + height + ", releasing display surface");
                if (displaySurface != null) {
                    displaySurface.release();
                    displaySurface = null;
                }
                setSourceSize(width, height);
                return;
            }

            try {
                // Release existing display surface
                if (displaySurface != null) {
                    displaySurface.release();
                    displaySurface = null;
                }

                // Validate SurfaceTexture before creating EGL surface
                if (!isSurfaceTextureValid(surfaceTexture)) {
                    Log.e(TAG, "SurfaceTexture is not valid, cannot create display surface");
                    return;
                }

                // Create new surface
                Log.d(TAG, "Creating new display surface with dimensions: " + width + "x" + height);
                try {
                    displaySurface = new WindowSurfaceNew(
                            eglCore,
                            new Surface(surfaceTexture),
                            true
                    );
                } catch (RuntimeException e) {
                    if (e.getMessage() != null && e.getMessage().contains("0x3003")) {
                        Log.e(TAG, "EGL_BAD_ALLOC error: SurfaceTexture is abandoned or invalid. " +
                                  "This usually happens during rapid service switching. Error: " + e.getMessage());
                    } else {
                        Log.e(TAG, "Failed to create EGL window surface: " + e.getMessage());
                    }
                    // Don't retry if the surface is abandoned
                    return;
                }

                // Make current and clear
                displaySurface.makeCurrent();
                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                displaySurface.swapBuffers();

                Log.d(TAG, "Successfully created and configured display surface");

            } catch (Exception e) {
                Log.e(TAG, "Failed to create or make current display surface.", e);
                // Clean up on failure
                if (displaySurface != null) {
                    try {
                        displaySurface.release();
                    } catch (Exception releaseEx) {
                        Log.e(TAG, "Error releasing display surface after creation failure", releaseEx);
                    }
                    displaySurface = null;
                }

                // Don't retry if the error indicates the surface is abandoned
                if (e.getMessage() != null &&
                    (e.getMessage().contains("0x3003") ||
                     e.getMessage().contains("abandoned") ||
                     e.getMessage().contains("BufferQueue has been abandoned"))) {
                    Log.w(TAG, "Surface is abandoned, not retrying surface creation");
                    return;
                }

                // Retry on failure if not shutting down
                if (!mIsShuttingDown) {
                    retrySetPreviewSurface(surfaceTexture, width, height, 0);
                }
            }

            setSourceSize(width, height);
        });
    }

    public synchronized void shutdown() {
        synchronized (SharedEglManager.class) {
            if (mIsShuttingDown) {
                Log.d(TAG, "SharedEglManager already shutting down");
                return;
            }
            mIsShuttingDown = true;
        }

        Log.d(TAG, "Shutting down SharedEglManager");

        if (mCameraHandler != null) {
            shutdownLatch = new CountDownLatch(1);
            mCameraHandler.post(() -> {
                try {
                    stopStreaming();
                    stopRecording(false);
                    release();
                } catch (Exception e) {
                    Log.e(TAG, "Error during shutdown", e);
                } finally {
                    shutdownLatch.countDown();
                }
            });
            try {
                shutdownLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Reset singleton instance
        synchronized (sLock) {
            sInstance = null;
        }

        Log.d(TAG, "SharedEglManager shutdown complete");
    }

    public void cleanup() {
        if (displaySurface != null) {
            displaySurface.release();
            displaySurface = null;
        }

        if (cameraTexture != null) {
            cameraTexture.release();
            cameraTexture = null;
        }

        // Release any other GL resources
        releaseEGLContext();
    }

    public void resetForNewCapture() {
        // Reset all state variables
        cameraTexture = null;
        displaySurface = null;
        // Add any other necessary state resets
    }

    private void releaseEGLContext() {
        // Implement EGL context cleanup logic here
        // This should release all GL resources and context
    }

    public void release() {
        mClosing = true;
        if (mCameraHandler != null) {
            mCameraHandler.removeCallbacks(timestampUpdater);
            mCameraHandler.removeCallbacks(updateRunnable);
            mCameraHandler.removeCallbacks(mSplitRunnable);
            mCameraHandler.removeCallbacks(mConnectionCheckRunnable);
            mCameraHandler.removeCallbacks(reopenRunnable);
            mCameraHandler.removeCallbacksFromBackgroundThread(null);

            for (Integer id : new ArrayList<>(mConnectionId.keySet())) {
                releaseConnection(id);
            }
            mConnectionId.clear();
            mConnectionState.clear();
            mConnectionStatistics.clear();
            mRetryPending.set(0);
            mReconnectAttempts.clear();
            mLastReconnectTime.clear();

            mCameraHandler.post(() -> {
                releaseEgl();

                if (mStreamer != null) {
                    mStreamer.release();
                    mStreamer = null;
                }
                if (mRecorder != null) {
                    mRecorder.release();
                    mRecorder = null;
                }

                if (mMic != null) {
                    mMic.interrupt();
                    try {
                        mMic.join();
                    } catch (InterruptedException ignored) {
                    }
                    mMic = null;
                }

                if (isBlueToothReceiverRegistered) {
                    unregisterBluetoothReceiver();
                }

                synchronized (overlayLock) {
                    if (overlay != null) {
                        overlay.release();
                        overlay = null;
                    }
                    recycleBitmap(overlayBmp);
                    overlayBmp = null;
                }
                clearBitmapPool();

                if (mCameraThread != null) {
                    mCameraThread.quitSafely();
                    try {
                        mCameraThread.join();
                    } catch (InterruptedException ignored) {
                    }
                    mCameraThread = null;
                }

                mClosing = false;
            });
        }

        // Reset initialization state
        synchronized (SharedEglManager.class) {
            mIsInitialized = false;
            mIsShuttingDown = false;
        }
    }

    private void releaseEgl() {
        if (cameraTexture != null) {
            cameraTexture.release();
            cameraTexture = null;
        }

        if (displaySurface != null) {
            displaySurface.release();
            displaySurface = null;
        }

        if (eglCore != null) {
            eglCore.makeNothingCurrent();
            eglCore.release();
            eglCore = null;
        }
        eglIsReady = false;

        if (encoderSurface != null) {
            encoderSurface.release();
            encoderSurface = null;
        }

        if (recorderSurface != null) {
            recorderSurface.release();
            recorderSurface = null;
        }

        if (fullFrameBlit != null) {
            fullFrameBlit.release(false);
            fullFrameBlit = null;
        }
    }

    public String getCurrentDateTime() {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(timeZone);
        return sdf.format(now);
    }

    private void handleError(String message) {
        handleError(message, null);
    }

    private void handleError(String message, Throwable e) {
        Log.e(TAG, message, e);
        mCameraHandler.removeCallbacks(timestampUpdater);
        unregisterBluetoothReceiver();

        long now = System.currentTimeMillis();
        if (!drawErrorLogged || (now - lastDrawErrorTime) > ERROR_LOG_COOLDOWN_MS) {
            drawErrorLogged = true;
            lastDrawErrorTime = now;
        }

        if (mStreamer == null || eglCore == null) {
            scheduleReopen();
        }
    }

    private void scheduleReopen() {
        if (reopenAttempts++ < MAX_RETRIES && !mClosing) {
            long delay = (long) REOPEN_DELAY_MS * (1L << reopenAttempts);
            mCameraHandler.postDelayed(reopenRunnable, delay);
        } else {
            Log.e(TAG, "Max reopen attempts reached");
        }
    }

    private void monitorPerformance(long frameStart) {
        totalFrameCount++;
        long frameTime = System.nanoTime() - frameStart;

        if (frameTime > SLOW_FRAME_THRESHOLD_NS) {
            slowFrameCount++;
        }

        if (SystemClock.elapsedRealtime() - lastPerformanceLogTime > PERFORMANCE_LOG_INTERVAL_MS) {
            double slowPercent = (double) slowFrameCount / totalFrameCount * 100;
            Log.i(TAG, String.format("Performance: %.1f%% slow frames (%d/%d)",
                    slowPercent, slowFrameCount, totalFrameCount));

            slowFrameCount = 0;
            totalFrameCount = 0;
            lastPerformanceLogTime = SystemClock.elapsedRealtime();
        }
    }

    private void recycleBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) return;

        if (bitmapPool.size() < 3) {
            bitmapPool.offer(bitmap);
        } else {
            bitmap.recycle();
        }
    }

    private void clearBitmapPool() {
        for (Bitmap bmp : bitmapPool) {
            if (!bmp.isRecycled()) {
                bmp.recycle();
            }
        }
        bitmapPool.clear();
    }

    public CameraInfo findCameraInfo() {
        CameraInfo cameraInfo = null;
        List<CameraInfo> mCameraList = CameraManager.getCameraList(context, true);

        if (mCameraList == null || mCameraList.isEmpty()) {
            Log.e(TAG, "No cameras found");
            return null;
        }

        String cameraId = AppPreference.getStr(AppPreference.KEY.SELECTED_POSITION, AppConstant.REAR_CAMERA);
        if (TextUtils.isEmpty(cameraId)) {
            cameraId = AppConstant.REAR_CAMERA;
        }

        for (CameraInfo info : mCameraList) {
            if (cameraId.equals(info.cameraId)) {
                cameraInfo = info;
                break;
            }
        }

        return cameraInfo != null ? cameraInfo : mCameraList.get(0);
    }

    public boolean isStreaming() {
        return mStreaming;
    }

    public boolean isRecording() {
        return mRecording;
    }

    public boolean isStreamerReady() {
        return mStreamer != null;
    }

    public SurfaceTexture getCameraTexture() {
        return cameraTexture;
    }

    public int getTextureId() {
        return textureId;
    }

    public boolean isInitialized() {
        return eglCore != null;
    }

    public void setConditioner(StreamConditionerBase conditioner) {
        this.mStreamConditioner1 = null;
        this.mStreamConditioner2 = null;
        this.mConditioner = conditioner;
        startConditioner();
    }

    void startConditioner() {
        switch (mServiceType) {
            case BgCamera:
                if (mConditioner != null) {
                    mConditioner.start(mStreamer, SettingsUtils.streamingBitRate(context), mConnectionId.keySet());
                }
                break;
            case BgUSBCamera:
                if (mConditioner != null) {
                    mConditioner.start(mStreamer, SettingsUtils.getUSBStreamBitrate(context), mConnectionId.keySet());
                }
                break;
            case BgScreenCast:
                if (mStreamConditioner1 != null) {
                    mStreamConditioner1.start(mStreamer, SettingsUtils.castBitRate(context), mConnectionId.keySet());
                } else if (mStreamConditioner2 != null) {
                    mStreamConditioner2.start(mStreamer, SettingsUtils.castBitRate(context), mConnectionId.keySet());
                }
                break;
            case BgAudio:
                if (mConditioner != null) {
                    mConditioner.start(mStreamer, SettingsUtils.audioBitRate(context), mConnectionId.keySet());
                }
                break;
        }
    }

    public void takeSnapshot() {
        if (!eglIsReady || eglCore == null) {
            Log.e(TAG, "Cannot take snapshot - EGL not ready");
            return;
        }
        should_snapshot = true;
    }

    public boolean getRadioMode() {
        return mRadioMode;
    }

    public Handler getHandler() {
        return mCameraHandler.getMainThreadHandler();
    }

    private class RetryRunnable implements Runnable {
        private final Connection connection;

        RetryRunnable(Connection connection) {
            this.connection = connection;
        }

        @Override
        public void run() {
            if (isStreamerReady() && mRetryPending.get() > 0) {
                mRetryPending.decrementAndGet();
                final int connectionId = createConnection(connection);
                if (connectionId == -1) {
                    mCameraHandler.postDelayed(new RetryRunnable(connection), RETRY_TIMEOUT);
                    mRetryPending.incrementAndGet();
                }
            }
        }
    }

    public static class StreamerStats {
        long duration;
        double fps;
        String[] connName;
        String[] connStatus;
        boolean[] isPacketLossIncreasing;
        boolean isRecording;
    }

    private void createStreamConditioner() {
        switch (SettingsUtils.castAdaptiveBitrate(context)) {
            case SettingsUtils.ADAPTIVE_BITRATE_MODE1:
                mStreamConditioner1 = new StreamConditionerMode1();
                mConditioner = null;
                break;
            case SettingsUtils.ADAPTIVE_BITRATE_MODE2:
                mStreamConditioner2 = new StreamConditionerMode2();
                mConditioner = null;
                break;
            case SettingsUtils.ADAPTIVE_BITRATE_OFF:
            default:
                mStreamConditioner1 = null;
                mStreamConditioner2 = null;
                mConditioner = null;
                break;
        }

        startConditioner();
    }


    /**
     * Draw the timestamp into the letterboxed viewport, preserving aspect.
     */

    // 1) Replace your existing helper with this version:
    private void drawTimestampOverlay(int dstW, int dstH, int srcW, int srcH) {
        // 1) bail if disabled
        if (!AppPreference.getBool(AppPreference.KEY.TIMESTAMP, true)) return;

        // Ensure overlay is created and text is set
        if (overlay == null || lastOverlayText == null) return;

        // 4) draw with alpha blending
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Use the new method that takes camera dimensions into account
        if (mServiceType == ServiceType.BgAudio) {
            overlay.draw(dstW,dstH);
        }else {
            overlay.drawTimeStampOnFrame(srcW, srcH);
        }

        GLES20.glDisable(GLES20.GL_BLEND);
    }
    /**
     * Safely release texture image with proper error handling
     * This prevents crashes when SurfaceTexture is in an invalid state
     */
    private void safeReleaseTextureImage() {
        if (cameraTexture != null && !cameraTexture.isReleased()) {
            try {
                // Check if EGL context is still valid before releasing texture
                if (eglCore != null && eglIsReady && !mIsShuttingDown) {
                    cameraTexture.releaseTexImage();
                } else {
                    Log.w(TAG, "Skipping texture release - EGL context not ready or shutting down");
                }
            } catch (RuntimeException e) {
                // Log the error but don't crash the app
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastDrawErrorTime > ERROR_LOG_COOLDOWN_MS) {
                    Log.e(TAG, "Error during updateTexImage (SurfaceTexture release failed): " + e.getMessage());
                    lastDrawErrorTime = currentTime;
                }
                
                // Mark texture as released to prevent further operations
                try {
                    if (cameraTexture != null && !cameraTexture.isReleased()) {
                        cameraTexture.release();
                    }
                } catch (Exception releaseException) {
                    Log.e(TAG, "Error releasing camera texture", releaseException);
                }
                cameraTexture = null;
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during texture release", e);
            }
        }
    }

    /**
     * Safely update texture with proper error handling
     * This prevents crashes when SurfaceTexture is in an invalid state
     */
    private void safeUpdateTexture() {
        if (cameraTexture != null && !cameraTexture.isReleased()) {
            try {
                // Check if EGL context is still valid before updating texture
                if (eglCore != null && eglIsReady && !mIsShuttingDown) {
                    cameraTexture.updateTexImage();
                    cameraTexture.getTransformMatrix(mTmpMatrix);
                } else {
                    Log.w(TAG, "Skipping texture update - EGL context not ready or shutting down");
                }
            } catch (RuntimeException e) {
                // Log the error but don't crash the app
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastDrawErrorTime > ERROR_LOG_COOLDOWN_MS) {
                    Log.e(TAG, "Error during updateTexImage (texture update failed): " + e.getMessage());
                    lastDrawErrorTime = currentTime;
                }
                
                // Mark texture as released to prevent further operations
                try {
                    if (cameraTexture != null && !cameraTexture.isReleased()) {
                        cameraTexture.release();
                    }
                } catch (Exception releaseException) {
                    Log.e(TAG, "Error releasing camera texture", releaseException);
                }
                cameraTexture = null;
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during texture update", e);
            }
        }
    }

    /**
     * Check if a SurfaceTexture is valid and can be used for EGL operations
     * @param surfaceTexture The SurfaceTexture to check
     * @return true if the SurfaceTexture is valid
     */
    private boolean isSurfaceTextureValid(SurfaceTexture surfaceTexture) {
        if (surfaceTexture == null) {
            Log.w(TAG, "SurfaceTexture is null");
            return false;
        }
        
        if (surfaceTexture.isReleased()) {
            Log.w(TAG, "SurfaceTexture is released");
            return false;
        }
        
        // Test if SurfaceTexture is still valid by trying to get its timestamp
        try {
            surfaceTexture.getTimestamp();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "SurfaceTexture is invalid or abandoned: " + e.getMessage());
            return false;
        }
    }

    public static void cleanAndReset() {
        cleanAndResetInternal(/*wait*/ true, /*callback*/ null);
    }
    public static void cleanAndResetAsync(@Nullable Runnable onFinished) {
        cleanAndResetInternal(/*wait*/ false, onFinished);
    }
    private static void cleanAndResetInternal(boolean synchronous,
                                              @Nullable Runnable onFinished) {

        final SharedEglManager mgr;
        synchronized (sLock) {                       // only one cleaner at once
            mgr = sInstance;
        }
        if (mgr == null) {                           // nothing to do
            if (onFinished != null) onFinished.run();
            return;
        }

        /* count-down latch is used iff caller wants to wait */
        final CountDownLatch finished = synchronous ? new CountDownLatch(1) : null;

        /* -- actual heavy cleanup runnable executed on EGL thread -- */
        Runnable heavyCleanup = () -> {
            try { mgr.doFullCleanupInsideEglThread(); }
            finally {
                if (finished != null) finished.countDown();
                if (onFinished != null) onFinished.run();
            }
        };

        /* post to EGL / camera thread if it is still alive */
        if (mgr.mCameraHandler != null) {
            mgr.mCameraHandler.post(heavyCleanup);
        } else {                                    // handler thread already gone
            heavyCleanup.run();
        }

        /* --- if synchronous: wait max 5 s for completion --- */
        if (synchronous) {
            try { finished.await(5, TimeUnit.SECONDS); }
            catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
    }

    private void doFullCleanupInsideEglThread() {

        /* 1. stop user visible jobs */
        try { stopStreaming(); }  catch (Exception ignored) {}
        try { stopRecording(false); } catch (Exception ignored) {}

        /* 2. stop conditioners */
        try { if (mConditioner != null) mConditioner.stop(); } catch (Exception ignored) {}
        try { if (mStreamConditioner1 != null) mStreamConditioner1.stop(); } catch (Exception ignored) {}
        try { if (mStreamConditioner2 != null) mStreamConditioner2.stop(); } catch (Exception ignored) {}
        mConditioner = null;
        mStreamConditioner1 = null;
        mStreamConditioner2 = null;

        /* 3. stop / join audio threads */
        stopAndJoinThread(mMic);
        mMic = null;
        stopAndJoinThread(mCastStreamMic);
        mCastStreamMic = null;
        stopAndJoinThread(mCastRecorderMic);
        mCastRecorderMic = null;
        if (audioMgr != null) { audioMgr.stop(); audioMgr = null; }

        /* 4. release streamer & recorder */
        try { if (mStreamer != null) mStreamer.release(); } catch (Exception ignored) {}
        mStreamer = null;
        try { if (mRecorder != null) mRecorder.release(); } catch (Exception ignored) {}
        mRecorder = null;

        /* 5. release EGL / GL resources */
        releaseEgl();                               // existing helper

        /* 6. overlay & bitmap pool */
        synchronized (overlayLock) {
            if (overlay != null) overlay.release();
            overlay = null;
            recycleBitmap(overlayBmp);
            overlayBmp = null;
        }
        clearBitmapPool();

        /* 7. unregister receivers */
        unregisterBluetoothReceiver();

        /* 8. collections & state */
        mRegisteredServices.clear();
        mConnectionId.clear();
        mConnectionState.clear();
        mConnectionStatistics.clear();
        mConnectionErrors.clear();
        mReconnectAttempts.clear();
        mLastReconnectTime.clear();
        mRetryPending.set(0);

        /* 9. remove callbacks */
        if (mCameraHandler != null) {
            mCameraHandler.removeCallbacksFromBackgroundThread(null);
        }

        /* 10. kill handler thread itself */
        if (mCameraThread != null) {
            mCameraThread.quitSafely();
            try { mCameraThread.join(); } catch (InterruptedException ignored) {}
        }
        mCameraThread  = null;

        /* 11. flags */
        eglIsReady      = false;
        mIsInitialized  = false;
        mIsShuttingDown = false;
        mClosing        = false;

        /* 12. finally forget the singleton */
        synchronized (sLock) { sInstance = null; }
    }

    /* helper */
    private static void stopAndJoinThread(Thread t) {
        if (t == null) return;
        try { t.interrupt(); } catch (Exception ignored) {}
        try { t.join(500); }   catch (Exception ignored) {}
    }

    // Helper method to save a file and log to database
    private void saveAndLogFile(File sourceFile, Uri treeUri, String fileName, String fileExtension, 
                               String fileType, boolean encrypt, String encryptionKey) {
        // Copy file to destination
        boolean copySuccess = encrypt ?
            copyAndEncryptFileToDestination(sourceFile, treeUri, fileName, fileExtension, encryptionKey) :
            copyFileToDestination(sourceFile, treeUri, fileName, fileExtension);

        if (!copySuccess) {
            Log.e(TAG, "Failed to copy file to destination: " + fileName);
            return;
        }

        // Get destination path
        String destPath = getDocumentFilePath(treeUri, fileName + fileExtension);

        // Get file metadata
        long fileSize = sourceFile.length();
        int[] resolution = null;
        long duration = 0;

        if ("video".equals(fileType)) {
            resolution = getVideoResolution(sourceFile);
            duration = getVideoDuration(sourceFile);
        } else if ("photo".equals(fileType)) {
            resolution = getImageResolution(sourceFile);
        }

        // Add to database
        if (fileStoreDb != null) {
            fileStoreDb.logFile(
                fileName + fileExtension,
                destPath,
                System.currentTimeMillis(),
                fileType,
                encrypt,
                duration,
                resolution != null ? resolution[0] : 0,
                resolution != null ? resolution[1] : 0,
                fileSize
            );
        }

        // Delete temp file
        boolean isDel = sourceFile.delete();
        if (!isDel) {
            Log.w(TAG, "Failed to delete temp file: " + sourceFile.getAbsolutePath());
        }
    }

    // Generic file copy method
    public static boolean copyFileToDestination(File sourceFile, Uri treeUri, 
                                               String fileName, String fileExtension) {
        String mimeType = getMimeTypeForExtension(fileExtension);
        return copyFileToDestination(sourceFile, treeUri, fileName, fileExtension, mimeType);
    }

    // Generic encrypted file copy method
    public static boolean copyAndEncryptFileToDestination(File sourceFile, Uri treeUri, 
                                                          String fileName, String fileExtension, 
                                                          String password) {
        String mimeType = "application/octet-stream";
        return copyAndEncryptFileToDestination(sourceFile, treeUri, fileName, fileExtension, mimeType, password);
    }

    // MIME type helper
    private static String getMimeTypeForExtension(String extension) {
        switch (extension.toLowerCase()) {
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            case ".mp4":
                return "video/mp4";
            case ".t3v":
            case ".t3j":
                return "application/octet-stream";
            default:
                return "application/octet-stream";
        }
    }

    // Get document file path helper
    private String getDocumentFilePath(Uri treeUri, String fileName) {
        if (treeUri == null || !DocumentsContract.isTreeUri(treeUri)) {
            return "";
        }
        DocumentFile pickedDir = DocumentFile.fromTreeUri(MyApp.getContext(), treeUri);
        if (pickedDir == null) {
            return "";
        }
        DocumentFile file = pickedDir.findFile(fileName);
        return file != null ? file.getUri().toString() : "";
    }

    // Video resolution helper
    private int[] getVideoResolution(File videoFile) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(videoFile.getAbsolutePath());
            String widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            
            if (widthStr != null && heightStr != null) {
                return new int[]{Integer.parseInt(widthStr), Integer.parseInt(heightStr)};
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting video resolution", e);
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    // Video duration helper
    private long getVideoDuration(File videoFile) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(videoFile.getAbsolutePath());
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr != null) {
                return Long.parseLong(durationStr);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting video duration", e);
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    // Image resolution helper
    private int[] getImageResolution(File imageFile) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
            return new int[]{options.outWidth, options.outHeight};
        } catch (Exception e) {
            Log.e(TAG, "Error getting image resolution", e);
        }
        return null;
    }

    // Updated file copy methods
    public static boolean copyFileToDestination(File sourceFile, Uri treeUri, 
                                               String fileName, String fileExtension, 
                                               String mimeType) {
        if (treeUri == null || !DocumentsContract.isTreeUri(treeUri)) {
            return false;
        }

        DocumentFile pickedDir = DocumentFile.fromTreeUri(MyApp.getContext(), treeUri);
        if (pickedDir == null || !pickedDir.canWrite()) {
            return false;
        }

        DocumentFile destFile = pickedDir.createFile(mimeType, fileName + fileExtension);
        if (destFile == null) {
            return false;
        }

        try (InputStream in = new FileInputStream(sourceFile);
             OutputStream out = MyApp.getContext().getContentResolver().openOutputStream(destFile.getUri())) {
            if (out == null) {
                return false;
            }

            byte[] buffer = new byte[65536];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "File copy failed", e);
            return false;
        }
    }

    public static boolean copyAndEncryptFileToDestination(File sourceFile, Uri treeUri, 
                                                          String fileName, String fileExtension, 
                                                          String mimeType, String password) {
        if (treeUri == null || !DocumentsContract.isTreeUri(treeUri)) {
            return false;
        }

        DocumentFile pickedDir = DocumentFile.fromTreeUri(MyApp.getContext(), treeUri);
        if (pickedDir == null || !pickedDir.canWrite()) {
            return false;
        }

        DocumentFile destFile = pickedDir.createFile(mimeType, fileName + fileExtension);
        if (destFile == null) {
            return false;
        }

        try (InputStream in = new FileInputStream(sourceFile);
             OutputStream fileOut = MyApp.getContext().getContentResolver().openOutputStream(destFile.getUri())) {
            if (fileOut == null) {
                return false;
            }

            byte[] salt = new byte[16];
            byte[] iv = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(salt);
            random.nextBytes(iv);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 10000, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));

            fileOut.write(salt);
            fileOut.write(iv);

            try (CipherOutputStream cipherOut = new CipherOutputStream(fileOut, cipher)) {
                byte[] buffer = new byte[65536];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    cipherOut.write(buffer, 0, bytesRead);
                }
                cipherOut.flush();
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Encryption failed", e);
            return false;
        }
    }

    /**
     * Switch to a new active service without stopping streams/recordings
     * @param newServiceType The new service type to activate
     * @param newSurface The surface texture for the new service
     * @param width The width of the surface
     * @param height The height of the surface
     */
    public void switchActiveService(ServiceType newServiceType, SurfaceTexture newSurface, int width, int height) {
        synchronized (mServiceLock) {
            // Skip if already the active service
            if (newServiceType.equals(mCurrentActiveService)) return;

            Log.d(TAG, "Switching active service to: " + newServiceType);
            
            // Update the active service
            mCurrentActiveService = newServiceType;
            
            // Update the preview surface
            if (newSurface != null) {
                setPreviewSurface(newSurface, width, height);
            }
            
            // Update orientation and mirror settings from new service
            BaseBackgroundService newService = getServiceInstance(newServiceType);
            if (newService != null) {
                setRotation(newService.getRotation());
                setMirror(newService.getMirrorState());
                setFlip(newService.getFlipState());
            }
        }
    }
    
    /**
     * Enhanced method to change active service without destroying EGL context
     * This method ensures that only the necessary parameters are modified while
     * keeping the EGL surface intact for seamless transitions
     * 
     * @param newServiceType The new service type to activate
     * @param newSurface The surface texture for the new service
     * @param width The width of the surface
     * @param height The height of the surface
     */
    public void eglChangeActiveService(ServiceType newServiceType, SurfaceTexture newSurface, int width, int height) {
        synchronized (mServiceSwitchingLock) {
            // Skip if already the active service
            if (newServiceType != null && newServiceType.equals(mCurrentActiveService)) {
                Log.d(TAG, "Service already active: " + newServiceType);
                return;
            }
            
            mIsServiceSwitching = true;
            
            try {
                Log.d(TAG, "EGL Change Active Service to: " + newServiceType);
                
                // Validate EGL state before switching
                if (!isEglReady()) {
                    Log.w(TAG, "EGL not ready for service switch, attempting initialization");
                    if (!initializeEglIfNeeded()) {
                        Log.e(TAG, "Failed to initialize EGL for service switch");
                        return;
                    }
                }
                
                // Get the new service instance
                BaseBackgroundService newService = getServiceInstance(newServiceType);
                if (newService == null) {
                    Log.e(TAG, "New service instance not found: " + newServiceType);
                    return;
                }
                
                // Queue pending operations to prevent buffer overflow
                synchronized (mPendingOperations) {
                    if (mPendingOperations.size() > MAX_PENDING_OPERATIONS) {
                        Log.w(TAG, "Clearing pending operations due to overflow");
                        mPendingOperations.clear();
                        mBufferOverflow = true;
                    }
                }
                
                // Update active service type
                mCurrentActiveService = newServiceType;
                
                // Update surface only if changed
                if (newSurface != null) {
                    
                    mCameraHandler.post(() -> {
                        try {
                            // Preserve existing EGL context
                            synchronized (mEglSurfaceLock) {
                                // Release old display surface carefully
                                if (displaySurface != null) {
                                    displaySurface.releaseEglSurface();
                                }
                                
                                // Create new display surface with existing EGL core
                                if (eglCore != null && newSurface != null) {
                                    displaySurface = new WindowSurfaceNew(eglCore, newSurface);
                                    mEglSurfaceValid = true;
                                    
                                    // Update dimensions
                                    setSourceSize(width, height);
                                    
                                    Log.d(TAG, "EGL surface updated for service: " + newServiceType);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating EGL surface", e);
                            mEglSurfaceValid = false;
                        }
                    });
                } else {
                    // Just update dimensions if surface hasn't changed
                    setSourceSize(width, height);
                }
                
                // Apply service-specific configurations
                applyServiceConfiguration(newService);
                
                // Process any pending operations
                processPendingOperations();
                
            } finally {
                mIsServiceSwitching = false;
            }
        }
    }
    
    /**
     * Check if two SurfaceTextures are the same
     */
    private boolean isSameSurface(SurfaceTexture surface1, SurfaceTexture surface2) {
        if (surface1 == null || surface2 == null) return false;
        return surface1 == surface2;
    }
    
    /**
     * Initialize EGL if needed
     */
    private boolean initializeEglIfNeeded() {
        if (eglCore == null || !eglIsReady) {
            try {
                initializeEGL();
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize EGL", e);
                return false;
            }
        }
        return true;
    }
    
    /**
     * Apply service-specific configuration
     */
    private void applyServiceConfiguration(BaseBackgroundService service) {
        if (service != null) {
            // Apply orientation settings
            setRotation(service.getRotation());
            setMirror(service.getMirrorState());
            setFlip(service.getFlipState());
            
            // Apply service-specific parameters
            ServiceType serviceType = MainActivity.getServiceType();
            if (serviceType == ServiceType.BgUSBCamera) {
                // USB camera specific settings
                Log.d(TAG, "Applying USB camera configuration");
            } else if (serviceType == ServiceType.BgScreenCast) {
                // Screen cast specific settings
                Log.d(TAG, "Applying screen cast configuration");
            }
        }
    }

    /**
     * Process pending operations
     */
    private void processPendingOperations() {
        synchronized (mPendingOperations) {
            while (!mPendingOperations.isEmpty()) {
                Runnable operation = mPendingOperations.poll();
                if (operation != null) {
                    mCameraHandler.post(operation);
                }
            }
        }
    }
    
    /**
     * Clear pending frames to prevent buffer overflow
     */
    private void clearPendingFrames() {
        if (cameraTexture != null) {
            try {
                // Clear the handler queue of pending frame updates
                mCameraHandler.removeCallbacks(null);
                
                // Update texture to latest frame
                cameraTexture.updateTexImage();
                
                // Clear the transform matrix
                cameraTexture.getTransformMatrix(mTmpMatrix);
            } catch (Exception e) {
                Log.e(TAG, "Error clearing pending frames", e);
            }
        }
    }

    public BaseBackgroundService getServiceInstance(ServiceType type) {
        synchronized (mServiceLock) {
            WeakReference<BaseBackgroundService> ref = mRegisteredServices.get(type);
            return ref != null ? ref.get() : null;
        }
    }

    /**
     * Get all registered service types
     * @return Array of registered service types
     */
    public ServiceType[] getRegisteredServiceTypes() {
        synchronized (mServiceLock) {
            cleanupDeadReferences();
            return mRegisteredServices.keySet().toArray(new ServiceType[0]);
        }
    }
    
    /**
     * Reinitialize EGL instance when leaving settings screen
     * This ensures that shared EGL instances and streaming/recording restart correctly
     */
    public void reinitializeEglOnSettingsExit() {
        Log.d(TAG, "Reinitializing EGL on settings exit");
        
        mCameraHandler.post(() -> {
            try {
                // Save current streaming/recording states
                final boolean wasStreaming = mStreaming;
                final boolean wasRecording = mRecording;
                
                // Stop current operations temporarily
                if (wasStreaming) {
                    pauseStreaming();
                }
                if (wasRecording) {
                    pauseRecording();
                }
                
                // Release and reinitialize EGL
                releaseEgl();
                initializeEGL();
                
                // Restore streaming/recording if they were active
                if (wasStreaming) {
                    resumeStreaming();
                }
                if (wasRecording) {
                    resumeRecording();
                }
                
                Log.d(TAG, "EGL reinitialized successfully on settings exit");
                
            } catch (Exception e) {
                Log.e(TAG, "Error reinitializing EGL on settings exit", e);
                handleError("Failed to reinitialize EGL: " + e.getMessage());
            }
        });
    }
    
    /**
     * Pause streaming temporarily (without fully stopping)
     */
    private void pauseStreaming() {
        Log.d(TAG, "Pausing streaming temporarily");
        // Implementation depends on your streaming logic
        // This should preserve connection but pause data flow
    }
    
    /**
     * Resume streaming after pause
     */
    private void resumeStreaming() {
        Log.d(TAG, "Resuming streaming");
        // Implementation depends on your streaming logic
        // This should resume data flow on existing connections
    }
    
    /**
     * Pause recording temporarily (without fully stopping)
     */
    private void pauseRecording() {
        Log.d(TAG, "Pausing recording temporarily");
        // Implementation depends on your recording logic
        // This should preserve file handles but pause writing
    }
    
    /**
     * Resume recording after pause
     */
    private void resumeRecording() {
        Log.d(TAG, "Resuming recording");
        // Implementation depends on your recording logic
        // This should resume writing to existing files
    }
    
    /**
     * Reset EGL instance for configuration changes
     * @param config The new configuration type (e.g., USB, front camera, etc.)
     */
    public void resetEglForConfiguration(ServiceType config) {
        Log.d(TAG, "Resetting EGL for configuration: " + config);
        
        synchronized (mServiceSwitchingLock) {
            mIsServiceSwitching = true;
            
            try {
                // Only reinitialize if necessary
                if (requiresEglReset(config)) {
                    mCameraHandler.post(() -> {
                        try {
                            // Save states
                            final boolean wasStreaming = mStreaming;
                            final boolean wasRecording = mRecording;
                            
                            // Reinitialize with minimal disruption
                            if (displaySurface != null) {
                                displaySurface.releaseEglSurface();
                            }
                            
                            // Reconfigure for new service type
                            configureEglForService(config);
                            
                            // Restore states
                            if (wasStreaming || wasRecording) {
                                Log.d(TAG, "Restoring streaming/recording states");
                            }
                            
                        } catch (Exception e) {
                            Log.e(TAG, "Error resetting EGL for configuration", e);
                        }
                    });
                }
            } finally {
                mIsServiceSwitching = false;
            }
        }
    }
    
    /**
     * Check if service type requires EGL reset
     */
    private boolean requiresEglReset(ServiceType serviceType) {
        // USB and Screen Cast typically require different EGL configurations
        return serviceType == ServiceType.BgUSBCamera || 
               serviceType == ServiceType.BgScreenCast;
    }
    
    /**
     * Configure EGL for specific service type
     */
    private void configureEglForService(ServiceType serviceType) {
        Log.d(TAG, "Configuring EGL for service: " + serviceType);
        
        // Service-specific EGL configurations
        switch (serviceType) {
            case BgUSBCamera:
                // USB camera specific EGL settings
                if (cameraTexture != null) {
                    cameraTexture.setDefaultBufferSize(1920, 1080); // Example USB resolution
                }
                break;
                
            case BgScreenCast:
                // Screen cast specific EGL settings
                if (cameraTexture != null) {
                    cameraTexture.setDefaultBufferSize(mScreenWidth, mScreenHeight);
                }
                break;
                
            case BgCamera:
            case BgAudio:
                // Standard camera/audio EGL settings
                if (cameraTexture != null) {
                    cameraTexture.setDefaultBufferSize(srcW, srcH);
                }
                break;
        }
    }
    
    /**
     * Check memory usage and log warnings if necessary
     */
    private void checkMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        float memoryUsagePercent = (usedMemory * 100f) / maxMemory;
        
        if (memoryUsagePercent > 80) {
            Log.w(TAG, String.format("High memory usage: %.1f%% (Used: %dMB, Max: %dMB)",
                    memoryUsagePercent, usedMemory / (1024 * 1024), maxMemory / (1024 * 1024)));
            
            // Suggest garbage collection if memory is critically low
            if (memoryUsagePercent > 90) {
                System.gc();
                Log.w(TAG, "Requested garbage collection due to high memory usage");
            }
        }
    }
}

