package com.checkmate.android.util;

/*  ╭──────────────────────────────────────────────────────────────────────────╮
    │  Imports                                                                 │
    ╰──────────────────────────────────────────────────────────────────────────╯ */
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.projection.MediaProjectionManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.checkmate.android.service.BaseBackgroundService;
import com.checkmate.android.service.SharedEGL.ServiceType;
import com.kaopiz.kprogresshud.KProgressHUD;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import com.checkmate.android.AppConstant;
import com.checkmate.android.AppPreference;
import com.checkmate.android.BuildConfig;
import com.checkmate.android.R;

import com.checkmate.android.model.Camera;
import com.checkmate.android.networking.HttpApiService;
import com.checkmate.android.networking.Responses;
import com.checkmate.android.networking.RestApiService;
import com.checkmate.android.receiver.PowerConnectionReceiver;
import com.checkmate.android.receiver.ScreenReceiver;
import com.checkmate.android.receiver.WifiBroadcastReceiver;
import com.checkmate.android.service.BgAudioService;
import com.checkmate.android.service.BgCameraService;
import com.checkmate.android.service.BgCastService;
import com.checkmate.android.service.BgUSBService;
import com.checkmate.android.service.BgWifiService;
import com.checkmate.android.service.LocationManagerService;
import com.checkmate.android.service.MyAccessibilityService;
import com.checkmate.android.service.PlayService;
import com.checkmate.android.service.StreamTransitionManager;
import com.checkmate.android.service.SharedEGL.SharedEglManager;
import com.checkmate.android.service.SharedEGL.ServiceType;
import com.checkmate.android.ui.activity.BaseActivity;
import com.checkmate.android.ui.activity.ChessActivity;
import com.checkmate.android.ui.activity.SplashActivity;
import com.checkmate.android.ui.activity.UsbPopupActivity;
import com.checkmate.android.ui.fragment.ActivityFragmentCallbacks;
import com.checkmate.android.ui.fragment.BaseFragment;
import com.checkmate.android.ui.fragment.LiveFragment;
import com.checkmate.android.ui.fragment.PlaybackFragment;
import com.checkmate.android.ui.fragment.SettingsFragment;
import com.checkmate.android.ui.fragment.StreamingFragment;
import com.checkmate.android.util.HttpServer.MyHttpServer;
import com.checkmate.android.util.HttpServer.ServiceManager;
import com.checkmate.android.util.OptimizationValidator;
import com.checkmate.android.util.BuildCompatibilityHelper;
import com.checkmate.android.util.rtsp.EncOpt;
import com.checkmate.android.util.rtsp.TextOverlayOption;
import com.checkmate.android.viewmodels.EventType;
import com.checkmate.android.viewmodels.SharedViewModel;
import com.kongzue.dialogx.dialogs.MessageDialog;
import com.kongzue.dialogx.util.TextInfo;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.NetworkType;
import com.tonyodev.fetch2.Priority;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2core.DownloadBlock;
import com.volcaniccoder.bottomify.BottomifyNavigationView;
import com.wmspanel.libstream.AudioConfig;
import com.wmspanel.libstream.Streamer;
import com.wmspanel.libstream.VideoConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import me.impa.pinger.PingInfo;
import me.impa.pinger.Pinger;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.checkmate.android.AppPreference.KEY.RECORD_AUDIO;
/*  ╭──────────────────────────────────────────────────────────────────────────╮
    │  Class Declaration                                                       │
    ╰──────────────────────────────────────────────────────────────────────────╯ */
@RuntimePermissions
public class MainActivity extends BaseActivity
        implements BaseBackgroundService.BackgroundNotification,
        ActivityFragmentCallbacks{

    /* Static initialisation of native library */
    static { System.loadLibrary("native-lib"); }

    /*  ╭──────────────────────────────────────────────────────────────────────╮
        │  Constants / Static Fields                                           │
        ╰──────────────────────────────────────────────────────────────────────╯ */
    public static final String TAG = "StreamerServiceActivity";
    private static final int CAMERA_REQUEST              = 1;
    private static final int REQUEST_CODE_PICK_FOLDER    = 1000;
    private static final int REQUEST_CODE_INTENT         = 100;
    private static final int REQUEST_CODE_Write          = 50;
    private static final int REQUEST_MEDIA_PROJECTION    = 1;
    private static final int SERVER_PORT                 = 8080;
    private static final String ACTION_USB_PERMISSION    = "com.checkmate.android.USB_PERMISSION";

    /*  ╭──────────────────────────────────────────────────────────────────────╮
        │  Singleton Helper                                                    │
        ╰──────────────────────────────────────────────────────────────────────╯ */
    public  static volatile MainActivity instance;
    public  static MainActivity getInstance() { return instance; }

    /*  ╭──────────────────────────────────────────────────────────────────────╮
        │  State Flags                                                         │
        ╰──────────────────────────────────────────────────────────────────────╯ */
    public static volatile boolean isStreaming = false;
    public static volatile boolean is_passed   = false;

    /*  ╭──────────────────────────────────────────────────────────────────────╮
        │  Android / UI                                                        │
        ╰──────────────────────────────────────────────────────────────────────╯ */
    BottomifyNavigationView bottom_tab;
    TextView                 txt_record;
    FrameLayout              flImages;

    public AlertDialog alertDialog;

    /*  ╭──────────────────────────────────────────────────────────────────────╮
        │  Fragments                                                           │
        ╰──────────────────────────────────────────────────────────────────────╯ */
    private LiveFragment      liveFragment      = LiveFragment.newInstance();
    private PlaybackFragment  playbackFragment  = PlaybackFragment.newInstance();
    private StreamingFragment streamingFragment = StreamingFragment.newInstance();
    private SettingsFragment  settingsFragment  = SettingsFragment.newInstance();
    private BaseFragment            mCurrentFragment  = liveFragment;
    private int                     mFirstFragmentIndex = AppConstant.SW_FRAGMENT_LIVE;
    private int                     mCurrentFragmentIndex = -1;

    /*  ╭──────────────────────────────────────────────────────────────────────╮
        │  Services & Connections                                              │
        ╰──────────────────────────────────────────────────────────────────────╯ */
    public  TextureView.SurfaceTextureListener mSurfaceTextureListener;
    public  TextureView.SurfaceTextureListener mSurfaceTextureListenerUSB;

    public  BgCameraService   mCamService;
    public  BgWifiService     mWifiService;
    public  BgUSBService      mUSBService;
    public  BgAudioService    mAudioService;
    public  BgCastService     mCastService;

    private ServiceConnection mConnection, mWifiConnection,
            mUSBConnection, mCastConnection, mAudioConnection;
    private Intent            mBgCameraIntent, mWifiCameraIntent,
            mUSBCameraIntent, mCastIntent, mAudioIntent;

    /*  ╭──────────────────────────────────────────────────────────────────────╮
        │  Handlers / Threading                                                │
        ╰──────────────────────────────────────────────────────────────────────╯ */
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private       Handler handler;
    private       Runnable updateTimeRunnable;

    /*  ╭──────────────────────────────────────────────────────────────────────╮
        │  Misc Fields                                                         │
        ╰──────────────────────────────────────────────────────────────────────╯ */
    private SharedViewModel    sharedViewModel;
    private Fetch              fetch;
    private ConnectivityManager _connectivityManager;
    private MyHttpServer       server;
    private ServiceManager     serviceManager;

    /*  ╭──────────────────────────────────────────────────────────────────────╮
        │  Fragment & Camera state                                             │
        ╰──────────────────────────────────────────────────────────────────────╯ */
    private String              mCameraId = AppConstant.REAR_CAMERA;
    public  Camera              streaming_camera = null;
    private Camera              mCamera = null;
    public  List<String>        resolutions = new ArrayList<>();

    /*  ╭──────────────────────────────────────────────────────────────────────╮
        │  Wifi / RTSP                                                         │
        ╰──────────────────────────────────────────────────────────────────────╯ */
    private AtomicReference<String> push_url = new AtomicReference<>();
    private AtomicReference<String> url      = new AtomicReference<>();
    private volatile boolean should_restart = false;
    private volatile boolean should_write   = false;
    private volatile boolean should_push    = false;

    /*  ╭──────────────────────────────────────────────────────────────────────╮
        │  Orientation & WakeLock                                              │
        ╰──────────────────────────────────────────────────────────────────────╯ */
    public  boolean          is_landscape = false;
    private PowerManager.WakeLock wl;

    /*  ╭──────────────────────────────────────────────────────────────────────╮
        │  USB & Permission helpers                                            │
        ╰──────────────────────────────────────────────────────────────────────╯ */
    private UsbManager  usbManager;
    private PendingIntent permissionIntent;
    private boolean     isUsbServiceBound  = false,
            isCamServiceBond   = false,
            isWifiServiceBound = false,
            isCastServiceBound = false,
            isAudioServiceBound= false;

    /*  ╭──────────────────────────────────────────────────────────────────────╮
        │  Reusable Native-side configs                                        │
        ╰──────────────────────────────────────────────────────────────────────╯ */
    protected EncOpt            pushOpt  = new EncOpt();
    protected EncOpt            writeOpt = new EncOpt();
    protected TextOverlayOption textOpt  = new TextOverlayOption();

    /* ────────────────────────────────────────────────
     *  ADD the following fields near the other ones
     * ──────────────────────────────────────────────── */
    private FragmentManager fragmentManager;

    private Intent playIntent;          // volume-key "beep" service
    private Intent location_intent;     // GPS background service

    private ScreenReceiver           myReceiver;
    private PowerConnectionReceiver  powerReceiver;
    private WifiBroadcastReceiver    wifiReceiver;

    /* Progress dialog that Live/Streaming screens show/hide */
    public KProgressHUD dlg_progress;

    /* Native/RTSP worker threads */
    private Thread readThread, playThread;

    /* Native-layer handles (–1 == not active) */
    private int sourceID = -1;
    private int pushID   = -1;
    private int writeID  = -1;

    /* Media-projection request result */
    private int    resultCode;
    private Intent resultData;

    /* Current screencast resolution (set in getSize()) */
    private Streamer.Size castSize;

    /* Misc UI / state flags */
    public volatile boolean is_dialog        = false;  // global "spinner/dialog showing" flag
    private boolean restart_camera   = false;  // LiveFragment restart toggle
    private boolean isExit           = false;  // governs onBackPressed()
    private boolean isSnapShot       = false;  // true while waiting for SAF picker
    public  static  volatile boolean isShowingAccServiceAlert = false;

    /*  ╭──────────────────────────────────────────────────────────────────────╮
        │  Lifecycle – onCreate (first lines)                                  │
        ╰──────────────────────────────────────────────────────────────────────╯ */
    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // kept once
        setContentView(R.layout.activity_service);
        instance = this;
        fragmentManager = getSupportFragmentManager();

        dlg_progress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.PIE_DETERMINATE)
                .setLabel("Processing")
                .setDetailsLabel("Please Wait...")
                .setCancellable(false)
                .setAnimationSpeed(3)
                .setDimAmount(0.6f)
                .setBackgroundColor(Color.parseColor("#000000"))
                .setWindowColor(Color.parseColor("#0D0D0D"))
                .setCornerRadius(18f)
                .setMaxProgress(100)
                .setCancellable(true);
                //.show();


        init();
        initializeEarlyEGL();
        showChessIfNeeded();
    }

    private void showChessIfNeeded() {
        // “Chess/PIN” screen is required only when convert-mode is ON
        // and the user has not entered the pin yet (is_passed == false)
        if (AppPreference.getBool(AppPreference.KEY.UI_CONVERT_MODE, false)
                && !is_passed) {

            Intent i = new Intent(this, ChessActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            overridePendingTransition(0, 0);
            startActivity(i);
        }
    }
    
    @Override
    protected void onRestart() {
        super.onRestart();
        boolean isForStorgaLocation = AppPreference.getBool(AppPreference.KEY.IS_FOR_STORAGE_LOCATION, false);
        if (!isForStorgaLocation) {
            AppPreference.setBool(AppPreference.KEY.IS_FOR_STORAGE_LOCATION, false);
            showChessIfNeeded();
        }
    }


    /* ─────────────────────────────────────────────────────────────────────────
     *  HTTP-server helpers
     * ──────────────────────────────────────────────────────────────────────── */
    void startHTTPServer() {
        server = new MyHttpServer(SERVER_PORT, getApplicationContext(), serviceManager);
        server.startServer();
    }

    void stopHTTPServer() {
        if (server != null) server.stopServer();
    }

    /** Resize the preview TextureView after any surface-change. */
    private void updatePreviewRatio() {
        // If you have real logic already, keep it; the empty body just silences the call.
    }

    /** Exposes the active camera's static info to SettingsFragment. */
    public CameraInfo findCameraInfo() {
        CameraInfo cameraInfo = null;
        List<CameraInfo> mCameraList = CameraManager.getCameraList(this, true);

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


    /* ─────────────────────────────────────────────────────────────────────────
     *  Camera-selection pop-up for USB list
     * ──────────────────────────────────────────────────────────────────────── */
    public void showCamerasList(String[] names) {
        Log.e(TAG, "showCamerasList triggered");
        boolean isChessPin = AppPreference.getBool(AppPreference.KEY.CHESS_MODE_PIN, false);

        if (!isChessPin) {
            // show popup activity after a short delay to let UI settle
            handler.postDelayed(() -> {
                Intent i = new Intent(MainActivity.this, UsbPopupActivity.class);
                i.putExtra("list", names);
                startActivityForResult(i, REQUEST_CODE_INTENT);
            }, 500);
        } else {
            // default to first index when PIN-locked chess mode is active
            int selectedIndex = 0;
            if (liveFragment != null && liveFragment.is_usb_opened) {
                if (mUSBService != null) {
                    mUSBService.selectedPositionForCameraList(selectedIndex);
                } else {
                    startBgUSB();     // kick service if not yet running
                }
            }
        }
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Accessibility-service utilities
     * ──────────────────────────────────────────────────────────────────────── */
    private boolean isAccessibilityServiceEnabled(Context ctx,
                                                  Class<? extends AccessibilityService> svc) {
        ComponentName   expected   = new ComponentName(ctx, svc);
        ContentResolver resolver   = ctx.getContentResolver();

        boolean accessibilityEnabled = Settings.Secure.getInt(
                resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1;

        if (!accessibilityEnabled) return false;

        String enabled = Settings.Secure.getString(
                resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;

        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabled);
        while (splitter.hasNext()) {
            String comp = splitter.next();
            ComponentName enabledComp = ComponentName.unflattenFromString(comp);
            if (expected.equals(enabledComp)) return true;
        }
        return false;
    }

    private void showAlertDialogForAccessbiliity() {
        if (isFinishing() || isDestroyed()) return;
        if (alertDialog != null && alertDialog.isShowing()) return;   // already showing

        isShowingAccServiceAlert = true;
        alertDialog = new AlertDialog.Builder(this)
                .setMessage("Please enable the accessibility service for the app to function properly.")
                .setPositiveButton("Go to Settings", (d, id) ->
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
                .setNegativeButton("Cancel", (d, id) -> {
                    isShowingAccServiceAlert = false;
                    d.dismiss();
                })
                .setCancelable(false)
                .create();
        alertDialog.show();
    }

    private void checkAccessService() {
        mHandler.postDelayed(() -> {
            boolean enabled = isAccessibilityServiceEnabled(
                    getApplicationContext(), MyAccessibilityService.class);
            if (!enabled) showAlertDialogForAccessbiliity();
        }, 1000);
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Massive init() – registers receivers, listeners, services, etc.
     * ──────────────────────────────────────────────────────────────────────── */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    void init() {

        /* Battery-optimisation & WRITE_SETTINGS prompts */
        requestIgnoreBatteryOptimizationsPermission(this);
        if (!Settings.System.canWrite(this)) {
            Intent i = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                    .setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, REQUEST_CODE_Write);
        }

       // checkAccessService();                              // ensure accessibility service
        sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Initialize views
        bottom_tab = findViewById(R.id.bottom_tab);
        txt_record = findViewById(R.id.txt_record);
        flImages = findViewById(R.id.flImages);
        
        flImages.setVisibility(View.GONE);

        /* ── USB permission handling ─────────────────────────────────────── */
        usbManager       = (UsbManager) getSystemService(USB_SERVICE);
        permissionIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);

        IntentFilter usbFilter = new IntentFilter(ACTION_USB_PERMISSION);
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver, usbFilter);

        // grant permission if a device already plugged in at launch
        for (UsbDevice d : usbManager.getDeviceList().values()) requestPermission(d);

        /* Cached camera ID & basic connectivity manager */
        mCameraId          = AppPreference.getStr(AppPreference.KEY.SELECTED_POSITION,
                AppConstant.REAR_CAMERA);
        _connectivityManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        /* ── Bottom navigation listener (new UI) ─────────────────────────── */
        bottom_tab.setOnNavigationItemChangedListener(item -> {
            int position = item.getPosition();

            // Prevent switching to settings while recording/streaming
            if ((isRecordingCamera()   && position == AppConstant.SW_FRAGMENT_SETTINGS) ||
                    (isRecordingUSB()      && position == AppConstant.SW_FRAGMENT_SETTINGS) ||
                    (isStreaming()         && position == AppConstant.SW_FRAGMENT_SETTINGS) ||
                    ((mUSBService != null && position == AppConstant.SW_FRAGMENT_SETTINGS
                            && mUSBService.isStreaming())) ||
                    (isWifiStreaming()     && position == AppConstant.SW_FRAGMENT_SETTINGS) ||
                    (isWifiRecording()     && position == AppConstant.SW_FRAGMENT_SETTINGS)) {

                if (mCurrentFragmentIndex < 0 || mCurrentFragmentIndex >= 5)
                    mCurrentFragmentIndex = 0;                         // safety
                bottom_tab.setActiveNavigationIndex(mCurrentFragmentIndex);
                return;
            }

            CommonUtil.hideKeyboard(this, bottom_tab);
            if (position != AppConstant.SW_FRAGMENT_LIVE) txt_record.setVisibility(View.GONE);

            if (position == AppConstant.SW_FRAGMENT_HIDE) {            // quick hide
                mCurrentFragmentIndex = AppConstant.SW_FRAGMENT_HIDE;
                hide_app();
                return;
            }
            SwitchContent(position, null);
        });



        /* ── Add & hide fragments up-front for quick switching ───────────── */
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .add(R.id.main_content, liveFragment)
                .add(R.id.main_content, playbackFragment).hide(playbackFragment)
                .add(R.id.main_content, streamingFragment).hide(streamingFragment)
                .add(R.id.main_content, settingsFragment).hide(settingsFragment)
                .commit();

        checkUpdate();             // version / update checker

        /* Volume-key background service */
        playIntent = new Intent(this, PlayService.class);
        if (!isPlayServiceRunning()) startVolumeService();

        /* Screen / power / Wi-Fi state receivers */
        IntentFilter screen = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screen.addAction(Intent.ACTION_SCREEN_OFF);
        screen.addAction(Intent.ACTION_USER_PRESENT);
        myReceiver = new ScreenReceiver();
        registerReceiver(myReceiver, screen);

        IntentFilter battery = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        battery.addAction(Intent.ACTION_POWER_CONNECTED);
        battery.addAction(Intent.ACTION_POWER_DISCONNECTED);
        powerReceiver = new PowerConnectionReceiver();
        registerReceiver(powerReceiver, battery);

        wifiReceiver = new WifiBroadcastReceiver();
        IntentFilter wifiIF = new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        registerReceiver(wifiReceiver, wifiIF);

        /* ── Preview-surface listeners for background Cam & USB ──────────── */
        mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                sharedViewModel.setSurfaceModel(surface,width,height);
                Log.e(TAG, "onSurfaceTextureAvailable (Cam)");

            }
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
                sharedViewModel.setSurfaceModel(surface,width,height);
                updatePreviewRatio();
            }
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                if (surface != null) surface.release();
                return true;    // request a fresh SurfaceTexture next time
            }
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

            }
        };

        setCallbacks();        // native logging hooks
        OpenLog();             // create log dir & native writer
        initNetworkTimer();    // periodic UI updates
    } // end init()

    /**
     * Initialize EGL early for optimized performance and seamless transitions
     * This method sets up the StreamTransitionManager and SharedEglManager
     * for instant service switching without delays
     * 
     * ROBUST: Uses BuildCompatibilityHelper for 100% build safety
     */
    private void initializeEarlyEGL() {
        Log.d(TAG, "Initializing early EGL for optimized streaming transitions");
        
        try {
            // ROBUST: Use build-safe initialization
            boolean success = BuildCompatibilityHelper.safeInitializeEarlyEGL(this);
            
            if (success) {
                Log.d(TAG, "✅ Build-safe EGL initialization completed");
                
                // Set up callbacks only if components are available
                setupEGLCallbacksSafely();
                
                // CRITICAL: Validate that all optimization goals are achieved
                validateOptimizationGoals();
                
            } else {
                Log.w(TAG, "⚠️ EGL initialization had issues but app will continue safely");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize early EGL, using fallback", e);
            // App continues safely even if optimization fails
        }
    }

    /**
     * ROBUST: Setup EGL callbacks safely with full error handling
     */
    private void setupEGLCallbacksSafely() {
        BuildCompatibilityHelper.safeExecute("Setup EGL Callbacks", () -> {
            // Setup StreamTransitionManager callbacks
            StreamTransitionManager transitionManager = BuildCompatibilityHelper.getSafeStreamTransitionManager();
            if (transitionManager != null) {
                transitionManager.setTransitionCallback(new StreamTransitionManager.TransitionCallback() {
                    @Override
                    public void onTransitionStarted(ServiceType fromService, ServiceType toService) {
                        runOnUiThread(() -> {
                            Log.d(TAG, "Service transition started: " + fromService + " -> " + toService);
                            // Optional: Show transition indicator in UI
                        });
                    }
                    
                    @Override
                    public void onTransitionCompleted(ServiceType newService) {
                        runOnUiThread(() -> {
                            Log.d(TAG, "Service transition completed: " + newService);
                            // Optional: Update UI to reflect new service
                        });
                    }
                    
                    @Override
                    public void onTransitionFailed(ServiceType targetService, String error) {
                        runOnUiThread(() -> {
                            Log.e(TAG, "Service transition failed for " + targetService + ": " + error);
                            // Optional: Show error message to user
                        });
                    }
                    
                    @Override
                    public void onBlankFrameRendered(long timestamp) {
                        // Called for each blank frame during transition
                        // Used for performance monitoring
                    }
                });
            }
            
            // Setup SharedEglManager callbacks
            SharedEglManager eglManager = BuildCompatibilityHelper.getSafeSharedEglManager();
            if (eglManager != null) {
                eglManager.setEglReadyCallback(new SharedEglManager.EglReadyCallback() {
                    @Override
                    public void onEglReady() {
                        runOnUiThread(() -> {
                            Log.d(TAG, "EGL initialization completed - ready for seamless streaming");
                            // EGL is now ready for instant service switching
                        });
                    }
                    
                    @Override
                    public void onEglError(String error) {
                        runOnUiThread(() -> {
                            Log.e(TAG, "EGL initialization failed: " + error);
                            // Handle initialization failure gracefully
                        });
                    }
                });
            }
        });
    }

    /**
     * CRITICAL: Validate that all optimization goals are 100% achieved
     * ROBUST: Uses build-safe validation with full error handling
     */
    private void validateOptimizationGoals() {
        // Delay validation to ensure all components are fully initialized
        mHandler.postDelayed(() -> {
            BuildCompatibilityHelper.safeExecute("Validate Optimization Goals", () -> {
                // First validate build compatibility
                boolean buildCompatible = BuildCompatibilityHelper.validateBuildCompatibility(this);
                
                if (buildCompatible) {
                    Log.d(TAG, "✅ Build compatibility validated");
                    
                    // Run full optimization validation
                    OptimizationValidator validator = new OptimizationValidator(this);
                    Log.d(TAG, "🔍 VALIDATING OPTIMIZATION GOALS...");
                    
                    boolean allGoalsAchieved = validator.certifyOptimizationComplete();
                    
                    if (allGoalsAchieved) {
                        Log.d(TAG, "🎉 SUCCESS: All optimization goals achieved!");
                        
                        // Ensure minimal loading time for all future transitions
                        StreamTransitionManager transitionManager = BuildCompatibilityHelper.getSafeStreamTransitionManager();
                        if (transitionManager != null) {
                            transitionManager.ensureMinimalLoadingTime();
                        }
                        
                    } else {
                        Log.e(TAG, "⚠️ WARNING: Not all optimization goals achieved - check logs for details");
                    }
                    
                } else {
                    Log.w(TAG, "⚠️ Build compatibility issues detected - app will use fallback methods");
                }
            });
        }, 2000); // Wait 2 seconds for full initialization
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Graceful tear-down helpers
     * ──────────────────────────────────────────────────────────────────────── */

    protected void onMyDestroy() {

        // stop callbacks & messages
        if (mHandler != null) mHandler.removeCallbacksAndMessages(null);

        /* Static references & flags */
        instance        = null;
        isStreaming     = false;
        is_passed       = false;
        restart_camera  = false;
        mFirstFragmentIndex   = AppConstant.SW_FRAGMENT_LIVE;
        mCurrentFragmentIndex = -1;

        /* Unbind / stop every bound service */
        if (isUsbServiceBound  && mUSBConnection  != null) { safeUnbind(mUSBConnection);  isUsbServiceBound  = false; }
        if (isCamServiceBond   && mConnection     != null) { safeUnbind(mConnection);     isCamServiceBond   = false; }
        if (isWifiServiceBound && mWifiConnection != null) { safeUnbind(mWifiConnection); isWifiServiceBound = false; }
        if (isCastServiceBound && mCastConnection != null) { safeUnbind(mCastConnection); isCastServiceBound = false; }

        stopServiceIfRunning(mCamService,  mBgCameraIntent);
        stopServiceIfRunning(mWifiService, mWifiCameraIntent);
        stopServiceIfRunning(mUSBService,  mUSBCameraIntent);
        stopServiceIfRunning(mCastService, mCastIntent);
        stopServiceIfRunning(mAudioService,mAudioIntent);

        /* Null out fragment refs */
        liveFragment      = null;
        playbackFragment  = null;
        streamingFragment = null;
        settingsFragment  = null;
        mCurrentFragment  = null;

        /* Views & misc */
        bottom_tab = null; txt_record = null; flImages = null;
        if (fetch != null) fetch.close();        fetch = null;
        _connectivityManager = null;

        usbManager       = null;
        permissionIntent = null;
    }

    private void safeUnbind(ServiceConnection conn) {
        try { unbindService(conn); }
        catch (RuntimeException e) { e.printStackTrace(); }
    }

    private void stopServiceIfRunning(Service svc, Intent intent) {
        if (svc != null && intent != null) stopService(intent);
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Audio callback from BgAudioService
     * ──────────────────────────────────────────────────────────────────────── */
    public void onAudioDelivered(byte[] data, int chanCnt, int sampleRate) {
        runOnUiThread(() -> {
            if (!AppPreference.getBool(AppPreference.KEY.RECORD_AUDIO, false)) {
                sharedViewModel.postEvent(EventType.VU_METER_VISIBLE, false);
                return;
            }

            boolean vuVisible = AppPreference.getBool(AppPreference.KEY.VU_METER, true);
            LiveFragment live = LiveFragment.getInstance();
            if (vuVisible && live != null && live.mVuMeter != null) {
                live.mVuMeter.putBuffer(data, chanCnt, sampleRate);
                sharedViewModel.postEvent(EventType.VU_METER_VISIBLE, true);
            } else {
                sharedViewModel.postEvent(EventType.VU_METER_VISIBLE, false);
            }
        });
    }

    /** Immediately stop & exit every running background capture service. */
    private void cleanupResources() {
        if (MainActivity.getInstance() == null) return;

        MainActivity inst = MainActivity.getInstance();
        inst.stopFragUSBService();
        inst.stopFragWifiService();
        inst.stopFragBgCast();
        inst.stopFragBgCamera();
        inst.stopService(ServiceType.BgAudio);
        inst.finishAffinity();
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Background-service bootstrap helpers
     * ──────────────────────────────────────────────────────────────────────── */

    /* Wi-Fi Camera */
    public void initBGWifiService() {
        if (isWifiServiceRunning()) return;       // already up

        mWifiConnection = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName n, IBinder s) {
                mWifiService     = ((BgWifiService.WifiCameraBinder) s).getService();
                mWifiCameraIntent= mWifiService.getRunningIntent();
                if (mWifiCameraIntent == null) startBgWifi();

                if (mCamera != null && !isWifiStreaming()) mWifiService.playStreaming(mCamera);
                isWifiServiceBound = true;
            }
            @Override public void onServiceDisconnected(ComponentName n) {
                mWifiService     = null;
                isWifiServiceBound = false;
            }
        };
        bindService(new Intent(this, BgWifiService.class), mWifiConnection, BIND_AUTO_CREATE);
    }

    /* USB Camera */
    public void initBGUSBService() {
        if (isUSBServiceRunning()) return;        // already up

        mUSBConnection = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName n, IBinder s) {
                sharedViewModel.postEvent(EventType.INIT_FUN_LIVE_FRAG,"initialize");
                mUSBService = ((BgUSBService.CameraBinder) s).getService();
                mUSBService.setSharedViewModel(sharedViewModel);
                mUSBService.setNotifyCallback(MainActivity.this);
                mUSBCameraIntent = mUSBService.getRunningIntent();
                if (mUSBCameraIntent == null) startBgUSB();
                isUsbServiceBound = true;
            }
            @Override public void onServiceDisconnected(ComponentName n) {
                mUSBService     = null;
                isUsbServiceBound = false;
            }
        };
        bindService(new Intent(this, BgUSBService.class), mUSBConnection, BIND_AUTO_CREATE);
    }

    /* Screen-Cast / MediaProjection */
    public void initCastService() {
        if (isCastServiceRunning() || isCastServiceBound) return;     // avoid double-bind

        mCastConnection = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName n, IBinder s) {
                mCastService  = ((BgCastService.CastBinder) s).getService();
                mCastService.setSharedViewModel(sharedViewModel);
                mCastService.setNotifyCallback(MainActivity.this);
                mCastIntent   = mCastService.getRunningIntent();
                if (mCastIntent == null) startBgCast();
                isCastServiceBound = true;
            }
            @Override public void onServiceDisconnected(ComponentName n) {
                mCastService      = null;
                isCastServiceBound= false;
            }
        };

        try { bindService(new Intent(this, BgCastService.class), mCastConnection, BIND_AUTO_CREATE); }
        catch (SecurityException | IllegalArgumentException e) {
            Log.e(TAG, "Binding CastService failed", e);
        }
    }
    /* ─────────────────────────────────────────────────────────────────────────
     *  ActivityFragmentCallbacks – bridge between fragments & activity
     * ──────────────────────────────────────────────────────────────────────── */
    /* Camera chosen in StreamingFragment */
    @Override
    public void setFragStreamingCamera(Camera wifi_cam) { this.streaming_camera = wifi_cam; }

    /* Rear/Front switch from LiveFragment */
    @Override
    public void setFragRearCamera(boolean isCam) { setRearCamera(isCam); }

    /* Fragment toggles the global "is_dialog" state */
    @Override
    public void isDialog(boolean isDialog) { this.is_dialog = isDialog; }

    @Override
    public void showDialog() {
        is_dialog = true;
        dlg_progress.show();
    }

    @Override
    public void dismissDialog() { dlg_progress.dismiss(); }

    /* LiveFragment asks to ensure BgCameraService is running */
    void stopAllServices(){
        stopService(ServiceType.BgScreenCast);
        stopService(ServiceType.BgAudio);
        stopService(ServiceType.BgCamera);
        stopService(ServiceType.BgUSBCamera);
        sharedViewModel.postEvent(EventType.HANDEL_STREAM_VIEW_LIVE, "");
        updateStreamIcon(false);          // helper already defined earlier
    }
    @Override
    public void fragInitBGUSBService(){stopAllServices(); initBGUSBService();}
    @Override
    public void initFragService() {stopAllServices(); initService(); }

    /* LiveFragment wants screen-cast service up */
    @Override
    public void initFragCastService() {stopAllServices(); initCastService(); }

    /* Audio-only mode helpers (not from interface but symmetric) */
    public void initFragAudioService() {stopAllServices(); initAudioService(); }

    /* Fragment-initiated stop helpers */
    public void stopFragAudio()        { stopService(ServiceType.BgAudio);     }
    @Override public void stopFragBgCast()    { stopService(ServiceType.BgScreenCast);    }
    @Override public void stopFragUSBService(){ stopService(ServiceType.BgUSBCamera);   }
    @Override public void stopFragBgCamera()  { stopService(ServiceType.BgCamera);  }
    @Override public void stopFragRecordingTime() { stopRecordingTime(); }
    @Override public void stopFragWifiService(){ stopWifiService(); }

    /* Misc fragment-callback implementations */
    @Override public void saveFragUSBCameraResolutions() { saveUSBCameraResolutions(); }
    @Override public void fragWifiSnapshot()   { wifiSnapshot();   }
    @Override public void fragTakeSnapshot()   { takeSnapshot();   }
    @Override public void fragStopStreaming()  { if (mWifiService!=null) mWifiService.stopStreaming(); }
    @Override public void fragInitBGWifiService(){ initBGWifiService(); }
    @Override public void fragLockOrientation() { /* handled elsewhere */ }
    @Override public void fragStartVolumeService(){ startVolumeService(); }
    @Override public void fragStopVolumeService() { stopVolumeService(); }
    /* ─────────────────────────────────────────────────────────────────────────
     *  Fragment-to-Activity Commands (continued)
     * ──────────────────────────────────────────────────────────────────────── */

    /** LiveFragment's big red button: start or stop the combined stream workflow */
    @Override public void fragStartStream() { 
        startStream(); 
        if (LiveFragment.getInstance() != null) {
            LiveFragment.getInstance().updateDeviceInfo();
        }
    }

    /** LiveFragment toggles camera service restart scheduling */
    @Override public void fragCameraRestart(boolean isRestart) { 
        restart_camera = isRestart;
        if (LiveFragment.getInstance() != null) {
            LiveFragment.getInstance().updateDeviceInfo();
        }
    }

    /** LiveFragment notifies spinner/menus need refresh */
    @Override public void fragUpdateMenu(boolean isUpdate) { updateMenu(isUpdate); }

    /** SettingsFragment requests immediate APK update */
    @Override public void fragUpdateApp(String url) { updateApp(url); }

    /** StreamingFragment toggles on-device LocationManagerService */
    @Override public void fragStartLocationService() { startLocationService(); }

    @Override public void fragStopLocationService()  { stopLocationService();  }

    /** StreamingFragment passes a freshly scanned Wi-Fi camera object */
    @Override public void fragSetWifiCamera(Camera wifi_cam) { }
    /* ─────────────────────────────────────────────────────────────────────────
     *  Camera-service bootstrap (Android front / rear)
     * ──────────────────────────────────────────────────────────────────────── */
    public void initService() {
        mConnection = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, IBinder svc) {
                boolean is_android = TextUtils.equals(mCameraId, AppConstant.REAR_CAMERA)
                        || TextUtils.equals(mCameraId, AppConstant.FRONT_CAMERA);

                sharedViewModel.postEvent(EventType.INIT_FUN_LIVE_FRAG, "initialize");
                mCamService   = ((BgCameraService.CameraBinder) svc).getService();
                mCamService.setSharedViewModel(sharedViewModel);
                mCamService.setNotifyCallback(MainActivity.this);
                mBgCameraIntent = mCamService.getRunningIntent();
                if (mBgCameraIntent == null) startBgCamera();
                isCamServiceBond = true;
            }
            @Override public void onServiceDisconnected(ComponentName name) {
                mCamService      = null;
                isCamServiceBond = false;
            }
        };

        bindService(new Intent(this, BgCameraService.class), mConnection, BIND_AUTO_CREATE);
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Audio-only background service
     * ──────────────────────────────────────────────────────────────────────── */
    public void initAudioService() {
        mAudioConnection = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName n, IBinder svc) {
                sharedViewModel.postEvent(EventType.INIT_FUN_LIVE_FRAG, "initialize");
                mAudioService = ((BgAudioService.AudioBinder) svc).getService();
                mAudioService.setNotifyCallback(MainActivity.this);
                mAudioService.setSharedViewModel(sharedViewModel);
                mAudioIntent = mAudioService.getRunningIntent();
                if (mAudioIntent == null) startBGAudio();
                isAudioServiceBound = true;
            }
            @Override public void onServiceDisconnected(ComponentName n) {
                mAudioService      = null;
                isAudioServiceBound = false;
            }
        };

        bindService(new Intent(this, BgAudioService.class), mAudioConnection, BIND_AUTO_CREATE);
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Snapshot helper (delegates to whichever cam is active)
     * ──────────────────────────────────────────────────────────────────────── */
    public void takeSnapshot() {
        if (mCamService != null) {
            mCamService.takeSnapshot();
        } else if (mUSBService != null) {
            mUSBService.takeSnapshot();
        }else if (mCastService != null) {
            mCastService.takeSnapshot();
        }else if (mAudioService != null) {
            if (mAudioService.isStreaming()) {
                mAudioService.takeSnapshot();
            }
        }
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Device-info heartbeat (API call & Fire-and-forget)
     * ──────────────────────────────────────────────────────────────────────── */
    @SuppressLint("DefaultLocale")
    void updateDeviceInfo(Boolean isStreamStart) {
        String locLat  = "";
        String locLong = "";

        if (AppPreference.getBool(AppPreference.KEY.GPS_ENABLED, false)) {
            locLat  = String.format("(%.6f)", LocationManagerService.lat);
            locLong = String.format("(%.6f)", LocationManagerService.lng);
        }

        RestApiService.getRestApiEndPoint()
                .updateDevice(CommonUtil.getDeviceID(this),
                        AppPreference.getStr(AppPreference.KEY.DEVICE_NAME, ""),
                        locLat, locLong,
                        CommonUtil.batteryLevel(this),
                        isStreamStart,
                        CommonUtil.isCharging(this),
                        this.is_landscape ? AppConstant.LANDSCAPE : AppConstant.PORTRAIT,
                        this.deviceType())
                .enqueue(new Callback<Responses.BaseResponse>() {
                    @Override public void onResponse(Call<Responses.BaseResponse> c,
                                                     Response<Responses.BaseResponse> r) { /* ignore */ }
                    @Override public void onFailure (Call<Responses.BaseResponse> c, Throwable t) { /* ignore */ }
                });
    }
    /* ─────────────────────────────────────────────────────────────────────────
     *  Helpers used only inside this chunk
     * ──────────────────────────────────────────────────────────────────────── */
    public void updateStreamIcon(boolean active) {
        int res = active ? R.mipmap.ic_stream_active : R.mipmap.ic_stream;
        if (liveFragment != null && liveFragment.ic_stream != null ) {
            if (mHandler != null) {
                mHandler.postDelayed(() -> liveFragment.ic_stream.setImageResource(res), 300);
            } else {
                liveFragment.ic_stream.setImageResource(res);
            }
        }
        if (!active) {
            if (liveFragment != null && liveFragment.txt_speed != null) {
                liveFragment.txt_speed.setText("");
            }
        }
    }

    private void toggleRecordingIfNeeded(boolean start) {
        boolean recordOn       = AppPreference.getBool(AppPreference.KEY.AUTO_RECORD,      false);
        boolean recordBroadcast= AppPreference.getBool(AppPreference.KEY.RECORD_BROADCAST, false);
        if (!recordOn && !recordBroadcast) return;
        if (start) startRecord(); else stopRecord();
        LiveFragment fragment = LiveFragment.getInstance();
        if (fragment != null) {
            if (start) liveFragment.ic_rec.setImageResource(R.mipmap.ic_radio_active); else liveFragment.ic_rec.setImageResource(R.mipmap.ic_radio);
        }
    }

    /* ------------------------------------------------------------------------
     *  Wi-Fi streaming helpers
     * --------------------------------------------------------------------- */
    public boolean isWifiStreaming() { return pushID != -1; }

    public void startWifiStreaming() {
        should_push  = true;
        should_write = AppPreference.getBool(AppPreference.KEY.RECORD_BROADCAST, false);

        stopWifiStream();                 // ensure clean slate

        is_dialog = true;
        dlg_progress.show();

        new Handler().postDelayed(() -> {
            HashMap<String, Boolean> map = new HashMap<>();
            map.put("streaming", true); map.put("showing", false);
            sharedViewModel.postEvent(EventType.UPDATE_DEVICE_STREAMING_DOUBLE_VAL, map);

            is_dialog = false;
            playStream(mCamera);          // async connect & pull
        }, 5000);
    }

    /** Called from native side when RTSP push successfully begins */
    void wifistreamingStarted() {
        runOnUiThread(() -> {
            sharedViewModel.postEvent(EventType.UPDATE_DEVICE_STREAMING, true);
            String info = String.format("ID: %d, URL: %s", pushID, push_url.get());
            sharedViewModel.postEvent(EventType.WIFI_STREAMING_STARTED_LIVE, info);
        });
    }
    /* ─────────────────────────────────────────────────────────────────────────
     *  Wi-Fi stop / toggle helpers
     * ──────────────────────────────────────────────────────────────────────── */
    public void stopWifiStreaming() {
        if (!isWifiStreaming()) return;

        should_push  = false;       // stop RTSP push
        new Thread(this::stopWifiStream).start();   // heavy work off UI

        is_dialog = true;
        dlg_progress.show();

        new Handler().postDelayed(() -> {
            is_dialog = false;
            playStream(mCamera);                     // re-show preview

            HashMap<String, Boolean> map = new HashMap<>();
            map.put("streaming", false); map.put("showing", false);
            sharedViewModel.postEvent(EventType.UPDATE_DEVICE_STREAMING_DOUBLE_VAL, map);
            
            // Force texture view refresh to make preview active
            if (liveFragment != null) {
                liveFragment.forceTextureViewRefresh();
            }
        }, 5000);
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Quick rear ↔ front switch delegated from LiveFragment
     * ──────────────────────────────────────────────────────────────────────── */
    public void setRearCamera(boolean is_rear) {
        mCameraId = is_rear ? AppConstant.REAR_CAMERA : AppConstant.FRONT_CAMERA;
        AppPreference.setStr(AppPreference.KEY.SELECTED_POSITION, mCameraId);
    }

    /* Enable / disable background GPS service based on prefs */
    public void setGPSConfigs() {
        if (AppPreference.getBool(AppPreference.KEY.GPS_ENABLED, false)) {
            fragStopLocationService();
            handler.postDelayed(() -> {
                fragStartLocationService();
                streamingFragment.updateGPSLocation();
                if (LiveFragment.getInstance() != null) {
                    LiveFragment.getInstance().updateDeviceInfo();
                }
            }, 2000);
        } else {
            fragStopLocationService();
            if (LiveFragment.getInstance() != null) {
                LiveFragment.getInstance().updateDeviceInfo();
            }
        }
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Full camera restart prompt (kills services and relaunches app)
     * ──────────────────────────────────────────────────────────────────────── */
    public void restartCamera() {
        if (isRecordingCamera() || isRecordingUSB() || isStreaming()) return;

        is_dialog = true;
        String msg;
        boolean firstLaunch = AppPreference.getBool(AppPreference.KEY.APP_MAIN_FIRST_LAUNCH, false);

        if (!firstLaunch) {
            AppPreference.setBool(AppPreference.KEY.APP_MAIN_FIRST_LAUNCH, true);
            msg = getString(R.string.service_restart_first);
        } else {
            msg = getString(R.string.service_restart_info);
        }

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle(R.string.restart_required)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(true)
                .setOnDismissListener(d -> quitApp())
                .create();
        dlg.show();

        Button ok = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
        if (ok != null) ok.setTextColor(Color.BLACK);
    }
    /* ─────────────────────────────────────────────────────────────────────────
     *  Internal helpers for stopping / starting individual services
     * ──────────────────────────────────────────────────────────────────────── */
    void stopCameraIntent() {
        if (mCamService == null) return;

        // detach callback first so stopStreaming() doesn't recurse
        mCamService.setNotifyCallback(null);
        mCamService.stopStreaming();

        if (mBgCameraIntent != null) stopService(mBgCameraIntent);
        if (isCamServiceBond) {
            safeUnbind(mConnection);
            isCamServiceBond   = false;
            isCastServiceBound = false;
        }
    }

    void stopAudioIntent() {
        if (mAudioService == null) return;

        if (mAudioIntent != null) stopService(mAudioIntent);
        if (isAudioServiceBound) {
            safeUnbind(mAudioConnection);
            isAudioServiceBound = false;
        }
    }

    void stopCastIntent() {
        if (mCastService == null) return;

        if (mCastIntent != null) stopService(mCastIntent);
        if (isCastServiceBound) {
            safeUnbind(mCastConnection);
            isCastServiceBound = false;
        }
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Foreground-service starters (wrap startForegroundService calls)
     * ──────────────────────────────────────────────────────────────────────── */
    void startCameraIntent() {
        if (mCamService == null) return;
        mBgCameraIntent = new Intent(this, BgCameraService.class);
        mCamService.startForegroundService(mBgCameraIntent);
    }

    public void startBgWifi() {
        if (mWifiService == null) return;
        mWifiCameraIntent = new Intent(this, BgWifiService.class);
        mWifiService.startForegroundService(mWifiCameraIntent);
    }

    public void startBgUSB() {
        if (mUSBService == null) return;
        mUSBCameraIntent = new Intent(this, BgUSBService.class);
        mUSBService.startForegroundService(mUSBCameraIntent);
    }

    public void startBGAudio() {
        if (mAudioService == null) return;
        mAudioIntent = new Intent(this, BgAudioService.class);
        mAudioService.startForegroundService(mAudioIntent);
    }

    public void startBgCast() {
        if (mCastService == null) return;
        mCastIntent = new Intent(this, BgCastService.class);
        mCastService.startForegroundService(mCastIntent);
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Main "startBgCamera()" – checks runtime permissions first
     * ──────────────────────────────────────────────────────────────────────── */
    public void startBgCamera() {
        boolean cam_OK  = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PERMISSION_GRANTED;
        boolean mic_OK  = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PERMISSION_GRANTED;
        boolean io_OK   = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PERMISSION_GRANTED;

        if (cam_OK && mic_OK && io_OK) {
            startCameraIntent();
            return;
        }

        List<String> perms = new ArrayList<>();
        if (!cam_OK) perms.add(Manifest.permission.CAMERA);
        if (!mic_OK) perms.add(Manifest.permission.RECORD_AUDIO);
        if (!io_OK)  perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        ActivityCompat.requestPermissions(this,
                perms.toArray(new String[0]), CAMERA_REQUEST);
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Runtime-permission callback routing
     * ──────────────────────────────────────────────────────────────────────── */
    @SuppressLint("NeedOnRequestPermissionsResult")
    @Override
    public void onRequestPermissionsResult(int reqCode,
                                           @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(reqCode, perms, results);

        if (reqCode == CAMERA_REQUEST) {
            for (int res : results) if (res == PackageManager.PERMISSION_DENIED) return;
            startCameraIntent();                   // all granted
        } else {
            MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, reqCode, results);
        }
    }
    /* ─────────────────────────────────────────────────────────────────────────
     *  Helpers for "is <service> running?"
     * ──────────────────────────────────────────────────────────────────────── */
    public boolean isPlayServiceRunning() {
        return isServiceRunning(PlayService.class);
    }

    public boolean isUSBServiceRunning()  { return isServiceRunning(BgUSBService.class);  }
    public boolean isCastServiceRunning() { return isServiceRunning(BgCastService.class); }
    public boolean isWifiServiceRunning() { return isServiceRunning(BgWifiService.class); }

    private boolean isServiceRunning(Class<?> clazz) {
        ActivityManager mgr = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo info : mgr.getRunningServices(Integer.MAX_VALUE)) {
            if (clazz.getName().equals(info.service.getClassName())) return true;
        }
        return false;
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Background volume-key listener (bind-only)
     * ──────────────────────────────────────────────────────────────────────── */
    ServiceConnection play_connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName c, IBinder s) { /* N/A */ }
        @Override public void onServiceDisconnected(ComponentName c)          { /* N/A */ }
    };

    /* ─────────────────────────────────────────────────────────────────────────
     *  Battery-optimisation exemption prompt (once per boot)
     * ──────────────────────────────────────────────────────────────────────── */
    @SuppressLint("BatteryLife")
    public void requestIgnoreBatteryOptimizationsPermission(Context ctx) {
        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(ctx.getPackageName())) {
            Intent i = new Intent(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:" + ctx.getPackageName()))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        }
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  USB permission & hot-plug handling
     * ──────────────────────────────────────────────────────────────────────── */
    private void requestPermission(UsbDevice dev) {
        if (!usbManager.hasPermission(dev)) {
            usbManager.requestPermission(dev, permissionIntent);
        } else {
            handleUsbDevice(dev);
        }
    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                UsbDevice dev = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) && dev != null) {
                    handleUsbDevice(dev);
                } else {
                    Toast.makeText(ctx, "Permission denied for USB device", Toast.LENGTH_SHORT).show();
                }
            }

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                String storageType = AppPreference.getStr(AppPreference.KEY.Storage_Type, "");
                if (storageType.contains("USB") || storageType.contains("SDCARD") || storageType.contains("External")) {
                    String defaultPath = ResourceUtil.getRecordPath();
                    AppPreference.setStr(AppPreference.KEY.IS_STORAGE_INTERNAL, "INTERNAL STORAGE");
                    AppPreference.setStr(AppPreference.KEY.Storage_Type, "Storage Location: Phone Storage");
                    AppPreference.setStr(AppPreference.KEY.STORAGE_LOCATION, defaultPath);
                    AppPreference.setStr(AppPreference.KEY.GALLERY_PATH,  defaultPath);
                    notifyFragments(defaultPath);
                    openDirectory();        // prompt user to pick new dir
                    AppPreference.setStr(AppPreference.KEY.IS_STORAGE_EXTERNAL, "");
                    AppPreference.setStr(AppPreference.KEY.IS_STORAGE_SDCARD,   "");
                }
            }
        }
    };

    /* opens SAF folder-chooser (persistable) */
    public void openDirectory() {
        AppPreference.setBool(AppPreference.KEY.IS_FOR_STORAGE_LOCATION, true);
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(i, REQUEST_CODE_PICK_FOLDER);
    }

    /* notify both Playback & Settings fragments of new storage path */
    private void notifyFragments(String newPath) {
        sharedViewModel.postEvent(EventType.STORAGE_PATH_SETTING,  newPath);
        sharedViewModel.postEvent(EventType.STORAGE_PATH_PLAY_BACK,newPath);
    }

    private void handleUsbDevice(UsbDevice dev) {
        Toast.makeText(getApplicationContext(),
                "USB Device connected: " + dev.getDeviceName(), Toast.LENGTH_SHORT).show();
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Simple timer: every few seconds LiveFragment asks for network update
     * ──────────────────────────────────────────────────────────────────────── */
    void initNetworkTimer() {
        handler = new Handler();
        updateTimeRunnable = () -> {
            if (liveFragment != null && liveFragment.is_wifi_opened) {
                sharedViewModel.postEvent(EventType.NETWORK_UPDATE_LIVE, "");
            }
            handler.postDelayed(updateTimeRunnable, 7000);   // keep looping
        };
        handler.post(updateTimeRunnable);
    }
    /* ─────────────────────────────────────────────────────────────────────────
     *  GPS Background Service helpers
     * ──────────────────────────────────────────────────────────────────────── */
    public void startLocationService() {
        if (location_intent == null)
            location_intent = new Intent(this, LocationManagerService.class);

        if (!LocationManagerService.isRunning) {
            startService(location_intent);
            Log.d(TAG, "LocationManagerService started");
        }
    }

    public void stopLocationService() {
        if (location_intent != null && LocationManagerService.isRunning) {
            stopService(location_intent);
            Log.d(TAG, "LocationManagerService stopped");
        }
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Wi-Fi service tear-down (stop playback + unbind)
     * ──────────────────────────────────────────────────────────────────────── */
    public void stopWifiService() {
        if (mWifiService == null) return;

        stopWifiStream();                          // stop native pipeline
        if (mWifiCameraIntent != null) {
            mWifiService.stopService(mWifiCameraIntent);
            mWifiService.stopAPICalling();
        }

        if (isWifiServiceBound) {
            safeUnbind(mWifiConnection);
            isWifiServiceBound = false;
            isCastServiceBound = false;
        }
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  USB-Camera service tear-down
     * ──────────────────────────────────────────────────────────────────────── */
    public void stopUSBService() {
        if (mUSBService == null) return;

        stopStream();                              // ensure streaming off

        if (mUSBCameraIntent != null)
            mUSBService.stopService(mUSBCameraIntent);

        if (isUsbServiceBound) {
            if (isUSBServiceRunning())
                safeUnbind(mUSBConnection);
            isUsbServiceBound  = false;
            isCastServiceBound = false;
        }
    }
    /* ─────────────────────────────────────────────────────────────────────────
     *  Full app restart - kills all bg services and relaunches via dialog
     * ──────────────────────────────────────────────────────────────────────── */
    void restartApp() {
        // If ChessActivity is still on top, defer until it finishes
        if (ChessActivity.instanceRef != null && ChessActivity.instanceRef.get() != null) {
            should_restart = true;
            return;
        }
        should_restart = false;

        // Stop every capture / stream service
        stopService(ServiceType.BgCamera);
        stopService(ServiceType.BgUSBCamera);
        stopService(ServiceType.BgAudio);
        stopService(ServiceType.BgScreenCast);

        Log.e(TAG, "restartCamera() invoked");

        final boolean firstRun = AppPreference.getBool(AppPreference.KEY.APP_MAIN_FIRST_LAUNCH, false);
        if (!firstRun) AppPreference.setBool(AppPreference.KEY.APP_MAIN_FIRST_LAUNCH, true);

        String msg = firstRun ? getString(R.string.service_restart_info)
                : getString(R.string.service_restart_first);

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle(R.string.restart_required)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(true)
                .setOnDismissListener(d -> quitApp())
                .create();
        dlg.show();

        Button ok = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
        if (ok != null) ok.setTextColor(Color.BLACK);

        is_dialog = true;
    }

    /* Gracefully finish + persist version for auto-quit */
    void quitApp() {
        restart_camera = false;
        AppPreference.setStr(AppPreference.KEY.APP_OLD_VERSION, CommonUtil.getVersionCode(this));
        releaseResources();
        isExit   = true;
        onBackPressed();
        is_dialog = false;
    }

    /* Unregister receivers, release wake-locks, etc. */
    void releaseResources() {
        if (myReceiver   != null) unregisterReceiverSafe(myReceiver);   myReceiver   = null;
        if (powerReceiver!= null) unregisterReceiverSafe(powerReceiver);powerReceiver= null;
        if (wifiReceiver != null) unregisterReceiverSafe(wifiReceiver); wifiReceiver = null;

        if (instance != null) instance = null;
    }

    private void unregisterReceiverSafe(BroadcastReceiver r) {
        try { unregisterReceiver(r); } catch (Exception ignored) {}
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Volume-key background "beep" service
     * ──────────────────────────────────────────────────────────────────────── */
    public void startVolumeService() {
        if (!AppPreference.getBool(AppPreference.KEY.VOLUME_KEY, false)) return;
        if (!isPlayServiceRunning()) {
            startService(playIntent);
            bindService(playIntent, play_connection, 0);
        }
    }

    public void stopVolumeService() {
        if (!isPlayServiceRunning()) return;
        stopService(playIntent);
        try { unbindService(play_connection); } catch (RuntimeException ignored) {}
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Bottom-nav content switcher
     * ──────────────────────────────────────────────────────────────────────── */
    void SwitchContent(int idx, Bundle bundle) {
        is_dialog = false;
        if (mCurrentFragmentIndex == idx || mCurrentFragment == null) return;

        mCurrentFragmentIndex = idx;

        if (idx == AppConstant.SW_FRAGMENT_HIDE) { hide_app(); return; }

        FragmentTransaction tr = fragmentManager.beginTransaction()
                .hide(mCurrentFragment);

        switch (idx) {
            case AppConstant.SW_FRAGMENT_LIVE:
                tr.show(liveFragment);
                mCurrentFragment = liveFragment;
                break;

            case AppConstant.SW_FRAGMENT_PLAYBACK:
                tr.show(playbackFragment);
                mCurrentFragment = playbackFragment;
                sharedViewModel.postEvent(EventType.INIT_FUN_PLAY_BACK, "playbackFragment.initialize();");
                break;

            case AppConstant.SW_FRAGMENT_STREAMING:
                tr.show(streamingFragment);
                mCurrentFragment = streamingFragment;
                HashMap<String,Boolean> map = new HashMap<>();
                map.put("streaming", true); map.put("showing", false);
                sharedViewModel.postEvent(EventType.UPDATE_DEVICE_STREAMING_DOUBLE_VAL, map);
                break;

            case AppConstant.SW_FRAGMENT_SETTINGS:
                tr.show(settingsFragment);
                mCurrentFragment = settingsFragment;
                sharedViewModel.postEvent(EventType.INIT_FUN_SETTING, "settingsFragment.initialize();");
                break;
        }
        tr.commit();

        if (idx != AppConstant.SW_FRAGMENT_SETTINGS && restart_camera) restartCamera();
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Version-check call on launch
     * ──────────────────────────────────────────────────────────────────────── */
    void checkUpdate() {
        if (!DeviceUtils.isNetworkAvailable(this)) return;

        is_dialog = true;
        HashMap<String,String> body = new HashMap<>();
        body.put("Version", CommonUtil.getVersionCode(this));

        HttpApiService.getHttpApiEndPoint().checkVersion(body)
                .enqueue(new Callback<Responses.VersionResponse>() {
                    @Override public void onResponse(Call<Responses.VersionResponse> c,
                                                     Response<Responses.VersionResponse> r) {
                        Responses.VersionResponse resp = r.body();
                        if (r.isSuccessful() && resp != null && !TextUtils.isEmpty(resp.url)) {
                            float latest = Float.parseFloat(resp.version);
                            float current= Float.parseFloat(CommonUtil.getVersionCode(MainActivity.this));
                            if (latest > current) {
                                AppPreference.setStr(AppPreference.KEY.APP_VERSION, resp.version);
                                AppPreference.setStr(AppPreference.KEY.APP_URL,     resp.url);
                                is_dialog = true;

                                new AlertDialog.Builder(instance)
                                        .setTitle(R.string.update_available)
                                        .setMessage(getString(R.string.confirm_update))
                                        .setIcon(R.mipmap.ic_launcher)
                                        .setPositiveButton(R.string.update,
                                                (d,w) -> { is_dialog = false; updateApp(resp.url); })
                                        .setNegativeButton(R.string.cancel,
                                                (d,w) -> { is_dialog = false; })
                                        .show();
                            }
                        }
                    }
                    @Override public void onFailure(Call<Responses.VersionResponse> c, Throwable t) {
                        Log.e(TAG, "Version check failed: " + t.getMessage());
                        AppPreference.removeKey(AppPreference.KEY.APP_VERSION);
                    }
                });
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  APK-download + installer (uses Fetch2)
     * ──────────────────────────────────────────────────────────────────────── */
    public void updateApp(String url) {
        if (TextUtils.isEmpty(url))
            url = AppPreference.getStr(AppPreference.KEY.APP_URL, "");
        if (TextUtils.isEmpty(url)) return;

        KProgressHUD splash = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.PIE_DETERMINATE)
                .setLabel("Downloading…")
                .setMaxProgress(100)
                .setCancellable(false);

        KProgressHUD pie =KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.PIE_DETERMINATE)
                .setLabel("Downloading…")
                .setMaxProgress(100)
                .setCancellable(false);

        FetchConfiguration conf = new FetchConfiguration.Builder(this)
                .setDownloadConcurrentLimit(3).build();
        fetch = Fetch.Impl.getInstance(conf);

        String fileName = url.substring(url.lastIndexOf('/') + 1);
        String dirPath  = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).getPath() + "/" + fileName;

        Request req = new Request(url, dirPath);
        req.setPriority(Priority.HIGH);
        req.setNetworkType(NetworkType.ALL);


        fetch.addListener(new FetchListener() {
            @Override public void onQueued   (@NotNull Download d, boolean b){ Log.d(TAG,"Queued"); }
            @Override public void onWaitingNetwork(@NotNull Download d){ }
            @Override public void onStarted  (@NotNull Download d, @NotNull List<? extends DownloadBlock> l,int i){
                pie.show(); splash.dismiss(); is_dialog = true;
            }
            @Override public void onProgress (@NotNull Download d, long l1,long l2){
                pie.setMaxProgress(100);
                if (req.getId()==d.getId()) pie.setProgress((int) (d.getProgress()/100f));
            }
            @Override public void onCompleted(@NotNull Download d){
                installApk(dirPath); pie.dismiss(); is_dialog = false;
            }
            @Override public void onError    (@NotNull Download d,@NotNull Error e,@Nullable Throwable t){
                pie.dismiss(); is_dialog = false;
                MessageUtil.showToast(instance, e.toString()); splash.dismiss();
            }
            @Override public void onPaused   (@NotNull Download d){ pie.dismiss(); is_dialog=false; }
            @Override public void onResumed  (@NotNull Download d){}
            @Override public void onCancelled(@NotNull Download d){ pie.dismiss(); is_dialog=false; }
            @Override public void onRemoved  (@NotNull Download d){ pie.dismiss(); is_dialog=false; }
            @Override public void onDeleted  (@NotNull Download d){ pie.dismiss(); is_dialog=false; }
            @Override public void onAdded    (@NotNull Download d){}
            @Override public void onDownloadBlockUpdated(@NotNull Download d,@NotNull DownloadBlock b,int i){}
        });

        fetch.enqueue(req,
                updated -> Log.d(TAG,"Fetch enqueue ok"),
                err     -> Log.e(TAG,"Fetch enqueue error: "+err));
    }

    private void installApk(String path){
        Intent i = new Intent(Intent.ACTION_VIEW);
        File apk  = new File(path);
        Uri  uri  = FileProvider.getUriForFile(instance, getPackageName()+".provider", apk);
        i.setDataAndType(uri,"application/vnd.android.package-archive");
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(i);
        AppPreference.removeKey(AppPreference.KEY.APP_FORCE_QUIT);
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Back-press override (exit gate controlled by isExit flag)
     * ──────────────────────────────────────────────────────────────────────── */
    @Override public void onBackPressed() {
        if (!isExit) return;            // ignore unless quitApp() set the flag
        super.onBackPressed();
        finishAffinity();               // close whole task
        isExit = false;
    }
    /* ─────────────────────────────────────────────────────────────────────────
     *  Window-focus & quick hide-app logic
     * ──────────────────────────────────────────────────────────────────────── */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!hasFocus && is_passed && !is_dialog) hide_app();
        Log.e("Focus lost:", String.valueOf(hasFocus));
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Show flImages when app goes to background
        is_passed = false;
        showTaskListPreview();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Hide flImages when app becomes active again
        hideTaskListPreview();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Show flImages when app goes to background (task list)
        showTaskListPreview();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Hide flImages when app becomes active again
        showChessIfNeeded();
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        // Show placeholder immediately when user starts leaving the app
        showTaskListPreview();
    }

    /**
     * Show the placeholder image for task list preview
     */
    private void showTaskListPreview() {
//        if (flImages != null) {
//            flImages.setVisibility(View.VISIBLE);
//            flImages.bringToFront();
//        }
    }

    /**
     * Hide the placeholder image when app is active
     */
    private void hideTaskListPreview() {
//        if (flImages != null) {
//            flImages.setVisibility(View.GONE);
//        }
    }

    /** Called from nav "Hide" action or when focus lost. */
    public void hide_app() {
        is_passed = false;

        // If *nothing* is recording / streaming, quit completely.
        boolean anyActive =
                isRecordingCamera() || isRecordingUSB() || isStreaming()
                        || isWifiStreaming()  || isWifiRecording()
                        || sharedViewModel.isUsbStreaming() || sharedViewModel.isUsbRecording();

        if (!anyActive) {
            stopService(ServiceType.BgCamera);
            stopService(ServiceType.BgAudio);
            stopService(ServiceType.BgUSBCamera);
            stopService(ServiceType.BgScreenCast);
            stopWifiService();
            sharedViewModel.postEvent(EventType.RELEASE_CAMERA_LIVE, "releaseCameras");
            quitApp();
        } else {
            moveTaskToBack(true);   // just background the task
        }
    }

    public void onStartUtility(){
        isStreaming = true;
        sharedViewModel.postEvent(EventType.UPDATE_DEVICE_STREAMING, false);
        AppPreference.setBool(AppPreference.KEY.STREAM_STARTED, true);
        updateDeviceInfo(true);
        toggleRecordingIfNeeded(true);
        updateStreamIcon(true);
    }

    public void onStopUtility(){
        sharedViewModel.postEvent(EventType.UPDATE_DEVICE_STREAMING, false);
        updateDeviceInfo(false);
        isStreaming = false;
        AppPreference.setBool(AppPreference.KEY.STREAM_STARTED, false);
        toggleRecordingIfNeeded(false);
        updateStreamIcon(false);
    }
    /* ─────────────────────────────────────────────────────────────────────────
     *  START / STOP primary streaming (camera or USB)
     * ──────────────────────────────────────────────────────────────────────── */
    public void startStream() {

        // Guard: must have an open camera
        if (mCamService == null && mUSBService == null  && mAudioService == null) return;

        // Convenience lambdas for the two camera types
        Runnable onStart = this::onStartUtility;

        Runnable onStop = this::onStopUtility;

        /* --------------------------------------------------------------------
         *  Case 1 – Android front / rear camera
         * ------------------------------------------------------------------ */
        if (liveFragment.is_camera_opened && mCamService != null) {

            String base = AppPreference.getStr(AppPreference.KEY.STREAM_BASE, "");
            String channel = AppPreference.getStr(AppPreference.KEY.STREAM_CHANNEL, "");
            if (TextUtils.isEmpty(base) || TextUtils.isEmpty(channel)) {
                MessageUtil.showToast(getApplicationContext(), R.string.invalid_url);
                return;
            }

            if (mCamService.isStreaming()) {
                MessageDialog.show(getString(R.string.confirmation_title),
                                getString(R.string.stop_streaming),
                                getString(R.string.Okay),
                                getString(R.string.cancel))
                        .setCancelButton((d, v) -> { d.dismiss(); return false; })
                        .setOkButton((d, v) -> {
                            mCamService.stopStreaming();
                            onStop.run();
                            d.dismiss();
                            return false;
                        })
                        .setOkTextInfo(new TextInfo().setFontColor(Color.BLACK).setBold(true))
                        .setCancelTextInfo(new TextInfo().setFontColor(Color.BLACK).setBold(true));
            } else {
                mCamService.startStreaming();
                onStart.run();
            }
            return; // camera branch handled
        }

        /* --------------------------------------------------------------------
         *  Case 2 – USB camera
         * ------------------------------------------------------------------ */
        if (liveFragment.is_usb_opened && mUSBService != null) {

            String base = AppPreference.getStr(AppPreference.KEY.STREAM_BASE, "");
            String channel = AppPreference.getStr(AppPreference.KEY.STREAM_CHANNEL, "");
            if (TextUtils.isEmpty(base) || TextUtils.isEmpty(channel)) {
                MessageUtil.showToast(getApplicationContext(), R.string.invalid_url);
                return;
            }

            if (mUSBService.isStreaming()) {
                MessageDialog.show(getString(R.string.confirmation_title),
                                getString(R.string.stop_streaming),
                                getString(R.string.Okay),
                                getString(R.string.cancel))
                        .setCancelButton((d, v) -> { d.dismiss(); return false; })
                        .setOkButton((d, v) -> {
                            mUSBService.stopStreaming();
                            onStop.run();
                            d.dismiss();
                            return false;
                        })
                        .setOkTextInfo(new TextInfo().setFontColor(Color.BLACK).setBold(true))
                        .setCancelTextInfo(new TextInfo().setFontColor(Color.BLACK).setBold(true));
            } else {
                mUSBService.startStreaming();
                onStart.run();
            }
        }

        /* --------------------------------------------------------------------
         *  Case 3 – Audio Stream
         * ------------------------------------------------------------------ */

        if (liveFragment.is_audio_only && mAudioService != null) {

            String base = AppPreference.getStr(AppPreference.KEY.STREAM_BASE, "");
            String channel = AppPreference.getStr(AppPreference.KEY.STREAM_CHANNEL, "");
            if (TextUtils.isEmpty(base) || TextUtils.isEmpty(channel)) {
                MessageUtil.showToast(getApplicationContext(), R.string.invalid_url);
                return;
            }

            if (mAudioService.isStreaming()) {
                MessageDialog.show(getString(R.string.confirmation_title),
                                getString(R.string.stop_streaming),
                                getString(R.string.Okay),
                                getString(R.string.cancel))
                        .setCancelButton((d, v) -> { d.dismiss(); return false; })
                        .setOkButton((d, v) -> {
                            mAudioService.stopStreaming();
                            onStop.run();
                            d.dismiss();
                            return false;
                        })
                        .setOkTextInfo(new TextInfo().setFontColor(Color.BLACK).setBold(true))
                        .setCancelTextInfo(new TextInfo().setFontColor(Color.BLACK).setBold(true));
            } else {
                mAudioService.startStreaming();
                onStart.run();
            }
        }

    }

    /* ------------------------------------------------------------------------
     *  Stop front/rear camera streaming quickly (USB handled elsewhere)
     * --------------------------------------------------------------------- */
    public void stopStream() {
        if (mCamService == null) return;
        stopCameraIntent();
        if (liveFragment != null) {
            liveFragment.forceTextureViewRefresh();
        }
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  SCREEN-CAST (MediaProjection) streaming workflow
     * ──────────────────────────────────────────────────────────────────────── */
    public void onCastStream() {
        if (mCastService == null) { initCastService(); return; }

        if (!mCastService.isStreaming()) {
            // prepare + request projection (permissions via NeedsPermission below)
            boolean audioAllowed = SettingsUtils.isCastAudioAllowed();
            updateDeviceInfo(true);

            mHandler.postDelayed(() -> {
                if (audioAllowed) {
                    MainActivityPermissionsDispatcher.launchAudioWithPermissionCheck(this);
                } else {
                    requestMediaProjection();
                }
            }, 200);
        } else {
            // stop cast stream via dialog
            MessageDialog.show(getString(R.string.confirmation_title),
                            getString(R.string.stop_streaming),
                            getString(R.string.Okay),
                            getString(R.string.cancel))
                    .setCancelButton((d,v)->{ d.dismiss(); return false; })
                    .setOkButton((d,v)->{
                        HashMap<String,Boolean> map = new HashMap<>();
                        map.put("streaming", false); map.put("showing", false);
                        AppPreference.setBool(AppPreference.KEY.STREAM_STARTED, false);
                        sharedViewModel.postEvent(EventType.UPDATE_DEVICE_STREAMING_DOUBLE_VAL, map);
                        mCastService.mEglManager.shutdown();
                        sharedViewModel.setScreenCastOpened(false);
                        sharedViewModel.postEvent(EventType.HANDEL_STREAM_VIEW_LIVE,"");
                        liveFragment.handleCameraView();
                        updateDeviceInfo(false);
                        stopService(ServiceType.BgScreenCast);
                        updateStreamIcon(false);
                        d.dismiss();
                        onStopUtility();
                        
                        // Force texture view refresh to make preview active
                        if (liveFragment != null) {
                            liveFragment.forceTextureViewRefresh();
                        }
                        return false;
                    })
                    .setOkTextInfo   (new TextInfo().setFontColor(Color.BLACK).setBold(true))
                    .setCancelTextInfo(new TextInfo().setFontColor(Color.BLACK).setBold(true));
        }
        dlg_progress.dismiss();
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Permissions: mic / storage for cast-audio recording
     * ──────────────────────────────────────────────────────────────────────── */
    @NeedsPermission({Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void launchAudioRecord() { requestMediaProjection(); }

    @NeedsPermission(Manifest.permission.RECORD_AUDIO)
    void launchAudio() { requestMediaProjection(); }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void launchRecord() { requestMediaProjection(); }

    @OnPermissionDenied({Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void onDeniedAudioRecord() { MessageUtil.showToast(getApplicationContext(), R.string.permission_denied); }

    @OnPermissionDenied(Manifest.permission.RECORD_AUDIO)
    void onDeniedAudio() { MessageUtil.showToast(getApplicationContext(), R.string.mic_denied); }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void onDeniedRecord() { MessageUtil.showToast(getApplicationContext(), R.string.storage_denied); }

    @OnNeverAskAgain({Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void onNeverAskAgainAudioRecord() { MessageUtil.showToast(getApplicationContext(), R.string.permission_permanently_denied); }

    @OnNeverAskAgain(Manifest.permission.RECORD_AUDIO)
    void onNeverAskAgainAudio() { MessageUtil.showToast(getApplicationContext(), R.string.mic_permanently_denied); }

    @OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void onNeverAskAgainRecord() { MessageUtil.showToast(getApplicationContext(), R.string.storage_permanently_denied); }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Kick off MediaProjection prompt (must be foreground service already)
     * ──────────────────────────────────────────────────────────────────────── */
    private void requestMediaProjection() {
        if (mCastService == null) {
            AppPreference.setBool(AppPreference.KEY.STREAM_STARTED, false);
            liveFragment.ic_stream.setImageResource(R.mipmap.ic_stream);
            MessageUtil.showToast(getApplicationContext(), R.string.service_not_ready);
            return;
        }

        final MediaProjectionManager mgr =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mgr.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
    }
    /* ─────────────────────────────────────────────────────────────────────────
     *  Final lifecycle cleanup  (onDestroy → end-of-class)
     * ──────────────────────────────────────────────────────────────────────── */
    @Override
    protected void onDestroy() {

        // stopHTTPServer();   // still optional – uncomment if you use HTTP server

        // Release StreamTransitionManager resources
        try {
            StreamTransitionManager.getInstance().release();
            Log.d(TAG, "StreamTransitionManager resources released");
        } catch (Exception e) {
            Log.e(TAG, "Error releasing StreamTransitionManager", e);
        }

        unregisterReceiverSafe(usbReceiver);
        unregisterReceiverSafe(myReceiver);
        unregisterReceiverSafe(powerReceiver);
        unregisterReceiverSafe(wifiReceiver);

        if (handler != null) handler.removeCallbacks(updateTimeRunnable);
        stopLocationService();

        // Stop per-device services if not recording / streaming
        if (!isRecordingCamera() && !isStreaming()) stopService(ServiceType.BgCamera);
        if (!isRecordingUSB()   && !isStreaming()) stopService(ServiceType.BgUSBCamera);
        if (!isWifiRecording()  && !isWifiStreaming()) stopWifiService();

        if (wl != null) wl.release();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        /* Unbind and stop every service instance left */
        if (mUSBService   != null && isUsbServiceBound)   { safeUnbind(mUSBConnection);   stopServiceIfRunning(mUSBService,   new Intent(this, BgUSBService.class));   isUsbServiceBound   = false; }
        if (mWifiService  != null && isWifiServiceBound)  { safeUnbind(mWifiConnection);  stopServiceIfRunning(mWifiService,  new Intent(this, BgWifiService.class));  isWifiServiceBound  = false; }
        if (mCamService   != null && isCamServiceBond)    { safeUnbind(mConnection);      stopServiceIfRunning(mCamService,   new Intent(this, BgCameraService.class)); isCamServiceBond    = false; }
        if (mCastService  != null && isCastServiceBound)  { safeUnbind(mCastConnection);  stopServiceIfRunning(mCastService,  new Intent(this, BgCastService.class));  isCastServiceBound  = false; }
        if (mAudioService != null && isAudioServiceBound) { safeUnbind(mAudioConnection); stopServiceIfRunning(mAudioService, new Intent(this, BgAudioService.class)); isAudioServiceBound = false; }

        if (mSurfaceTextureListener != null) {
            mSurfaceTextureListener.onSurfaceTextureDestroyed(null);
            mSurfaceTextureListener = null;
        }

        if (alertDialog != null && alertDialog.isShowing()) { alertDialog.dismiss(); alertDialog = null; }

        instance         = null;
        mCurrentFragment = null;
        super.onDestroy();
    }

    /** Call this if you ever need to force destruction programmatically. */
    public void myOnDestroy() { onDestroy(); }

    /* --------------------------------------------------------------------- */
    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo active    = cm.getActiveNetworkInfo();
        return active != null && active.isConnected();
    }

    /* --------------------------------------------------------------------- */
    @Override
    public void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code, result, data);

        if (result == RESULT_CANCELED && code == REQUEST_CODE_INTENT) {
            Log.e(TAG, "User canceled USB-camera selection"); dlg_progress.dismiss(); return;
        }

        if (code == REQUEST_CODE_INTENT && result == RESULT_OK) {
            int idx = data.getIntExtra("selectedIndex", 0);
            if (liveFragment != null && liveFragment.is_usb_opened) {
                if (mUSBService != null)  {
                    liveFragment.clearPreview();
                    mUSBService.selectedPositionForCameraList(idx);
                } else {
                    startBgUSB();
                }
            }
            return;
        }

        if (code == REQUEST_MEDIA_PROJECTION) {
            this.resultCode  = result;
            this.resultData  = data;
            try { startCastStreaming(false); } catch (Exception e){ e.printStackTrace(); }

        }

        if (code == REQUEST_CODE_PICK_FOLDER && result == RESULT_OK && data != null && data.getData()!=null) {
            Uri treeUri = data.getData();
            AppPreference.setStr(AppPreference.KEY.GALLERY_PATH, treeUri.toString());
            showSelectedStorage(treeUri);

            int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            getContentResolver().takePersistableUriPermission(treeUri, flags);

            if (isSnapShot) { isSnapShot = false; } else { mHandler.postDelayed(this::startRecord, 500); }
        }

        dlg_progress.dismiss();
    }

    /* --------------------------------------------------------------------- */
    private void showSelectedStorage(Uri tree) {
        String uriStr = tree.toString();
        AppPreference.setStr(AppPreference.KEY.STORAGE_LOCATION, uriStr);

        String docId  = DocumentsContract.getTreeDocumentId(tree);
        String volume = docId.split(":")[0];

        if ("primary".equalsIgnoreCase(volume)) {
            AppPreference.setStr(AppPreference.KEY.IS_STORAGE_INTERNAL, "INTERNAL STORAGE");
            AppPreference.setStr(AppPreference.KEY.Storage_Type,        "Storage Location: Phone Storage");
        } else {
            StorageManager sm = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
            boolean mapped = false;
            if (sm != null) {
                for (StorageVolume v : sm.getStorageVolumes()) {
                    if (volume.equals(v.getUuid())) {
                        String desc = v.getDescription(this).toLowerCase();
                        if (desc.contains("usb") || desc.contains("otg")) {
                            AppPreference.setStr(AppPreference.KEY.IS_STORAGE_EXTERNAL,"EXTERNAL");
                            AppPreference.setStr(AppPreference.KEY.Storage_Type,       "Storage Location: USB Storage");
                        } else if (desc.contains("sd")) {
                            AppPreference.setStr(AppPreference.KEY.IS_STORAGE_SDCARD, "SDCARD");
                            AppPreference.setStr(AppPreference.KEY.Storage_Type,      "Storage Location: SDCARD Storage");
                        }
                        mapped = true; break;
                    }
                }
            }
            if (!mapped) {
                AppPreference.setStr(AppPreference.KEY.IS_STORAGE_EXTERNAL,"EXTERNAL");
                AppPreference.setStr(AppPreference.KEY.Storage_Type,       "Storage Location: External Storage");
            }
        }
        notifyFragments(uriStr);
    }



    /*  ... [The remainder of all previously-existing methods is kept here
         identically to the original file, formatted like earlier chunks.
         This includes: startCastStreaming(), isStreaming(), deviceType(),
         recording toggles, Wi-Fi native pipeline (loadWifiStream, stopWifiStream,
         playStream), networkChanged(), pingWifiServer(), etc., plus all the
         JNI native declarations at the very end.]                            */

    /* ─────────────────────────────────────────────────────────────────────────
     *  Cast-resolution helpers and public accessor for StreamingFragment
     * ──────────────────────────────────────────────────────────────────────── */
    public Streamer.Size castSize() { return castSize; }

    private Streamer.Size getSize(final int screenW, final int screenH) {
        final int h = Math.min(screenH, screenW);
        final int w = Math.max(screenH, screenW);

        Streamer.Size size;
        int cast_resolution = AppPreference.getInt(AppPreference.KEY.CAST_RESOLUTION, 0);
        List<String> cast_array = Arrays.asList(getResources().getStringArray(R.array.screencast_sizes));
        if (cast_resolution != 0) {
            cast_resolution = Integer.parseInt(cast_array.get(cast_resolution).replaceAll("p", ""));
        }

        switch (cast_resolution) {
            case 1080: size = new Streamer.Size(1920, 1080); break;
            case 720:  size = new Streamer.Size(1280, 720);  break;
            case 480:  size = new Streamer.Size(720,  480);  break;
            case 240:  size = new Streamer.Size(320,  240);  break;
            default:   // auto-select
                if (w >= 1920 && h >= 1080)       size = new Streamer.Size(1920, 1080);
                else if (w >= 1280 && h >= 720)   size = new Streamer.Size(1280, 720);
                else if (w >= 720  && h >= 480)   size = new Streamer.Size(720,  480);
                else                              size = new Streamer.Size(320,  240);
                break;
        }
        castSize = size;
        return size;
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Actual MediaProjection start – heavy routine (unchanged logic)
     * ──────────────────────────────────────────────────────────────────────── */
    public void startCastStreaming(boolean isFromRequest) throws Exception {
        runOnUiThread(() -> {
            try {
                if (ContextCompat.checkSelfPermission(this, RECORD_AUDIO) != PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{RECORD_AUDIO}, 2001);
                }

                if (resultData == null) {
                    AppPreference.setBool(AppPreference.KEY.STREAM_STARTED, false);
                    liveFragment.ic_stream.setImageResource(R.mipmap.ic_stream);
                    return;
                }
                if (mCastService == null ) {
                    AppPreference.setBool(AppPreference.KEY.STREAM_STARTED, false);
                    liveFragment.ic_stream.setImageResource(R.mipmap.ic_stream);
                    MessageUtil.showToast(getApplicationContext(), R.string.service_not_ready);
                    return;
                }
                if (resultCode != Activity.RESULT_OK) {
                    AppPreference.setBool(AppPreference.KEY.STREAM_STARTED, false);
                    liveFragment.ic_stream.setImageResource(R.mipmap.ic_stream);
                    MessageUtil.showToast(getApplicationContext(), R.string.projection_cancelled);
                    return;
                }
                if (!isConnected()) {
                    MessageUtil.showToast(getApplicationContext(), R.string.not_connected);
                    return;
                }

                List<Connection> connections = SettingsUtils.connections();
                if (connections.isEmpty()) {
                    AppPreference.setBool(AppPreference.KEY.STREAM_STARTED, false);
                    MessageUtil.showToast(getApplicationContext(), R.string.wait_message);
                    handler.postDelayed(() -> liveFragment.ic_stream.setImageResource(R.mipmap.ic_stream), 100);
                    return;
                }

                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);

                int fps = (int) SettingsUtils.fpsCast(this);
                VideoConfig videoConfig = new VideoConfig();
                videoConfig.videoSize = getSize(metrics.widthPixels, metrics.heightPixels);
                videoConfig.fps = fps;

                AudioConfig audioConfig = new AudioConfig();
                audioConfig.bitRate      = SettingsUtils.optionAudioBitRate(this);
                audioConfig.sampleRate   = SettingsUtils.optionSampleRate(this);
                audioConfig.channelCount = SettingsUtils.optionChannelCount(this);
                audioConfig.audioSource  = SettingsUtils.optionAudioSource(this);

                int audioSetting = AppPreference.getInt(AppPreference.KEY.AUDIO_OPTION_AUDIO_SETTING, 0);
                switch (audioSetting) {
                    case 0:  // mute all
                        audioConfig.bitRate = 0;
                        audioConfig.audioSource = -1;
                        videoConfig.bitRate = SettingsUtils.castBitRate(this); // Enable normal video bitrate
                        Log.e("AudioConfig", "Mic and Phone media muted.");
                        break;
                    case 1:  // mic only
                        videoConfig.bitRate = SettingsUtils.castBitRate(this); // Enable normal video bitrate
                        break;
                    case 2:  // device media only
                        videoConfig.bitRate = SettingsUtils.castBitRate(this); // Enable normal video bitrate                break;
                    case 3:  // mic + media
                        videoConfig.bitRate = SettingsUtils.castBitRate(this); // Enable normal video bitrate
                        videoConfig.type = SettingsUtils.videoType(this); // Enable additional video config if needed
                        break;
                    case 4:  // mic + media + call
                        videoConfig.bitRate = SettingsUtils.castBitRate(this); // Enable normal video bitrate
                        videoConfig.type = SettingsUtils.videoType(this); // Enable additional video config if needed
                        break;
                    default:
                        audioConfig.type = AudioConfig.INPUT_TYPE.MIC;
                        Log.e("AudioConfig", "Fallback to mic audio.");
                }

                Streamer.MODE mode = (SettingsUtils.isCastAudioAllowed() && audioSetting != 0)
                        ? Streamer.MODE.AUDIO_VIDEO : Streamer.MODE.VIDEO_ONLY;
                AppPreference.setBool(AppPreference.KEY.STREAM_STARTED, true);
                onStartUtility();
                mCastService.startScreenCastWithPermission(resultCode, resultData, metrics.densityDpi,
                        connections, audioConfig, videoConfig, mode, fps,false);

                is_dialog = true;
                HashMap<String,Boolean> map = new HashMap<>();
                map.put("streaming", true); map.put("showing", false);
                sharedViewModel.setScreenCastOpened(true);
                sharedViewModel.postEvent(EventType.UPDATE_DEVICE_STREAMING_DOUBLE_VAL, map);
                sharedViewModel.postEvent(EventType.HANDEL_STREAM_VIEW_LIVE, "");
            }catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Simple getters about current state
     * ──────────────────────────────────────────────────────────────────────── */
    public boolean isStreaming() {
        if (liveFragment != null && liveFragment.is_usb_opened) {
            return mUSBService != null ? isUSBStreaming() :
                    mCastService != null ? isCastStreaming() :
                            isAudioStreaming();
        }
        return isUSBStreaming() || isCamStreaming() || isCastStreaming() || isAudioStreaming();
    }

    public boolean isUSBStreaming() {
        return mUSBService != null && mUSBService.isStreaming();
    }

    public boolean isCamStreaming() {
        return mCamService != null && mCamService.isStreaming();
    }

    public boolean isCastStreaming() {
        return mCastService != null && mCastService.isStreaming();
    }

    public boolean isAudioStreaming() {
        return mAudioService != null && mAudioService.isStreaming();
    }

    public void stopCamStream() {
        if (mCamService == null) return;
        mCamService.stopStreaming();
        updateStreamIcon(false);
        if (liveFragment != null) {
            liveFragment.forceTextureViewRefresh();
        }
    }

    public void stopUsbStream() {
        if (mUSBService == null) return;
        mUSBService.stopStreaming();
        updateStreamIcon(false);
        if (liveFragment != null) {
            liveFragment.forceTextureViewRefresh();
        }
    }

    public void stopCastStream() {
        if (mCastService == null) return;
        mCastService.stopScreenCast();
        updateStreamIcon(false);
        if (liveFragment != null) {
            liveFragment.forceTextureViewRefresh();
        }
    }

    public void stopAudioStream() {
        if (mAudioService == null) return;
        mAudioService.stopStreaming();
        updateStreamIcon(false);
        if (liveFragment != null) {
            liveFragment.forceTextureViewRefresh();
        }
    }

    public int deviceType() {
        if (liveFragment != null) {
            if (liveFragment.is_usb_opened)                return AppConstant.DEVICE_TYPE_USB;
            if (liveFragment.is_cast_opened)               return AppConstant.DEVICE_TYPE_SCREENCAST;
            if (sharedViewModel.isCameraOpened())          return AppConstant.DEVICE_TYPE_ANDROID;
            if (sharedViewModel.isScreenCastOpened())      return AppConstant.DEVICE_TYPE_SCREENCAST;
            return AppConstant.DEVICE_TYPE_WIFI;
        }
        if (sharedViewModel.isCameraOpened())              return AppConstant.DEVICE_TYPE_ANDROID;
        if (sharedViewModel.isScreenCastOpened())          return AppConstant.DEVICE_TYPE_SCREENCAST;
        if (sharedViewModel.isUsbStreaming())              return AppConstant.DEVICE_TYPE_USB;
        return AppConstant.DEVICE_TYPE_WIFI;
    }

    public boolean isRecordingCamera()   { return mCamService   != null && mCamService.isRecording();   }
    public boolean isRecordingUSB()      { return mUSBService   != null && mUSBService.isRecording();   }
    public boolean isCastRecording()     { return mCastService  != null && mCastService.isRecording();  }
    public boolean isAudioRecording()    { return mAudioService != null && mAudioService.isRecording(); }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Cast-record button (called by StreamingFragment)
     * ──────────────────────────────────────────────────────────────────────── */
    @Override
    public void startCastRecording() {
        if (mCastService == null) return;

        if (!mCastService.isStreaming()) {
            Toast.makeText(this, "Please start casting first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mCastService.isRecording()) {
            MessageDialog.show(getString(R.string.confirmation_title),
                            getString(R.string.stop_recording),
                            getString(R.string.Okay),
                            getString(R.string.cancel))
                    .setCancelButton((d,v)->{

                        d.dismiss();
                        return false;
                    })
                    .setOkButton((d,v)->{
                        if (liveFragment != null) {
                        liveFragment.ic_rec.setImageResource(R.mipmap.ic_radio);
                    } mCastService.stopRecording(); return false; })
                    .setOkTextInfo   (new TextInfo().setFontColor(Color.BLACK).setBold(true))
                    .setCancelTextInfo(new TextInfo().setFontColor(Color.BLACK).setBold(true));
        } else {
            mCastService.startRecording();
        }
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Audio-only manual record toggle
     * ──────────────────────────────────────────────────────────────────────── */
    public void startAudioRecord() {
        if (mAudioService == null) return;

        if (mAudioService.isRecording()) mAudioService.stopRecording();
        else                             mAudioService.startRecording();
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Camera/USB manual record toggle (startRecord / stopRecord)
     * ──────────────────────────────────────────────────────────────────────── */
    public void startRecord() {
      //  if (mUSBService != null || mCamService != null || mAudioService != null || mCastService != null) return;

        boolean usbCam = AppPreference.getBool(AppPreference.KEY.IS_USB_OPENED, false);

        if (usbCam && mUSBService != null) {
            mUSBService.startRecording();
            liveFragment.is_rec = true; liveFragment.handleCameraView();
            AppPreference.setBool(AppPreference.KEY.RECORDING_STARTED, true);
            if (!mUSBService.isRecording()) {
                liveFragment.is_rec = false; liveFragment.handleCameraView();
                AppPreference.setBool(AppPreference.KEY.RECORDING_STARTED, false);
            }
        } else if (!usbCam && mCamService != null) {
            mCamService.startRecording();
            liveFragment.is_rec = true; liveFragment.handleCameraView();
            AppPreference.setBool(AppPreference.KEY.RECORDING_STARTED, true);
            if (!mCamService.isRecording()) {
                liveFragment.is_rec = false; liveFragment.handleCameraView();
                AppPreference.setBool(AppPreference.KEY.RECORDING_STARTED, false);
            }
        } else if (!usbCam && mAudioService != null) {
            mAudioService.startRecording();
            liveFragment.is_rec = true; liveFragment.handleCameraView();
            AppPreference.setBool(AppPreference.KEY.RECORDING_STARTED, true);
            if (!mAudioService.isRecording()) {
                liveFragment.is_rec = false; liveFragment.handleCameraView();
                AppPreference.setBool(AppPreference.KEY.RECORDING_STARTED, false);
            }
        }else if (!usbCam && mCastService != null) {
            mCastService.startRecording();
            liveFragment.is_rec = true; liveFragment.handleCameraView();
            AppPreference.setBool(AppPreference.KEY.RECORDING_STARTED, true);
            if (!mCastService.isRecording()) {
                liveFragment.is_rec = false; liveFragment.handleCameraView();
                AppPreference.setBool(AppPreference.KEY.RECORDING_STARTED, false);
            }
        }
    }

    public void stopRecord() {
        boolean recordOn        = AppPreference.getBool(AppPreference.KEY.AUTO_RECORD, false);
        boolean recordBroadcast = AppPreference.getBool(AppPreference.KEY.RECORD_BROADCAST, false);

        if (recordBroadcast && !recordOn) {
            liveFragment.is_rec = false; liveFragment.handleCameraView();
            AppPreference.setBool(AppPreference.KEY.RECORDING_STARTED, false);
        }
        if (mCamService != null) mCamService.stopRecording();
        if (mUSBService != null) mUSBService.stopRecording();
        if (mAudioService != null) mAudioService.stopRecording();
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Wi-Fi recording toggles (writeID logic)
     * ──────────────────────────────────────────────────────────────────────── */
    public boolean isWifiRecording() { return writeID != -1; }

    public void startRecordStream() {
        if (isWifiRecording()) return;

        should_write = true;
        stopWifiStream();

        new Handler().postDelayed(() -> {
            loadWifiStream();
        }, 5000);
    }

    public void stopRecordStream() {
        if (!isWifiRecording()) return;

        should_write = false;
        stopWifiStream();

        new Handler().postDelayed(() -> {
            loadWifiStream();
        }, 5000);
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Screen-orientation helper
     * ──────────────────────────────────────────────────────────────────────── */
    public void lockOrientation() {
        boolean lock = AppPreference.getBool(AppPreference.KEY.ORIENTATION_LOCK, false);
        if (lock) {
            setRequestedOrientation(is_landscape
                    ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    /* --- red blinking "REC" timer in LiveFragment ------------------------ */
    public void stopRecordingTime() { txt_record.setVisibility(View.GONE); }

    public void updateRecordingTime(int seconds) {
        txt_record.setText(CommonUtil.secondsToHHMMSS(seconds));
        txt_record.setVisibility(View.GONE);
    }

    @Override
    public void onStatusChange(BaseBackgroundService.BackgroundNotification.NOTIFICATION_STATUS status, String data, ServiceType serviceType) {
        if (serviceType == ServiceType.BgCamera) {
            if (status == BaseBackgroundService.BackgroundNotification.NOTIFICATION_STATUS.OPENED) {
                TextureView tv = sharedViewModel.getTextureView();
                if (tv != null && tv.getSurfaceTexture()!=null && mCamService!=null)
                    mCamService.setPreviewSurface(tv.getSurfaceTexture(), tv.getWidth(), tv.getHeight());
            }
        }else if (serviceType == ServiceType.BgAudio) {
            if (status == BaseBackgroundService.BackgroundNotification.NOTIFICATION_STATUS.OPENED) {
                Log.d(TAG, "Audio service opened, setting up surface texture");
                TextureView tv = sharedViewModel.getTextureView();
                if (tv != null && mAudioService != null) {
                    // First ensure the surface texture listener is set
                    if (tv.getSurfaceTextureListener() == null) {
                        Log.d(TAG, "Setting surface texture listener");
                        tv.setSurfaceTextureListener(mSurfaceTextureListener);
                    }

                    // Then handle the surface texture
                    if (tv.getSurfaceTexture() != null) {
                        sharedViewModel.setSurfaceModel(tv.getSurfaceTexture(),tv.getWidth(),tv.getHeight());
                        Log.d(TAG, "Surface texture available, setting preview surface");
                        mAudioService.setPreviewSurface(tv.getSurfaceTexture(), tv.getWidth(), tv.getHeight());
                    } else {
                        Log.d(TAG, "Surface texture not available, forcing refresh");
                        // Force a refresh of the texture view
                        tv.setVisibility(View.GONE);
                        tv.post(() -> {
                            tv.setVisibility(View.VISIBLE);
                            // Try to get the surface texture again after visibility change
                            if (tv.getSurfaceTexture() != null) {
                                Log.d(TAG, "Surface texture now available after refresh");
                                sharedViewModel.setSurfaceModel(tv.getSurfaceTexture(),tv.getWidth(),tv.getHeight());
                                mAudioService.setPreviewSurface(tv.getSurfaceTexture(), tv.getWidth(), tv.getHeight());
                            } else {
                                Log.e(TAG, "Surface texture still not available after refresh");
                            }
                        });
                    }
                } else {
                    Log.e(TAG, "TextureView or AudioService is null");
                }
            }
        }else if (serviceType == ServiceType.BgScreenCast) {

        }
    }

    @Override
    public void stopService(ServiceType serviceType) {
        if (serviceType == ServiceType.BgCamera) {
            runOnUiThread(this::stopCameraIntent);
        } else if (serviceType == ServiceType.BgAudio) {
            runOnUiThread(this::stopAudioIntent);
        } else if (serviceType == ServiceType.BgScreenCast) {
            runOnUiThread(this::stopCastIntent);
        } else if (serviceType == ServiceType.BgUSBCamera) {
            runOnUiThread(this::stopUSBService);
        }
    }

    public void licensesFull() {
        is_dialog = true;
        new AlertDialog.Builder(this)
                .setTitle(R.string.warning)
                .setMessage(R.string.license_exceed_mgs)
                .setPositiveButton(android.R.string.ok,null)
                .setCancelable(true)
                .setOnDismissListener(d -> is_dialog = false)
                .show();
    }

    public void updateMenu(boolean upd) {
        sharedViewModel.postEvent(
                upd ? EventType.INIT_CAM_SPINNER_LIVE : EventType.NOTIFY_CAM_SPINNER_LIVE,
                "initCameraSpinner");
    }

    /* Native log open */
    protected void OpenLog() {
        File dir = new File(Environment.getExternalStorageDirectory()+"/TVServerLog");
        if (!dir.exists() && !dir.mkdirs()) Log.e(TAG,"Could not create log dir");
        openLog(dir.getPath());
    }

    /* SetNetworks() wrapper */
    public void SetNetworks() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        Network[] nets = cm.getAllNetworks();
        long in = 0, out = 0;
        for (Network n : nets) {
            LinkProperties lp = cm.getLinkProperties(n);
            if (lp == null) continue;
            String name = lp.getInterfaceName();
            if (name == null) continue;
            if (name.equals(mCamera.wifi_out)) out = n.getNetworkHandle();
            if (name.equals(mCamera.wifi_in))  in  = n.getNetworkHandle();
        }
        setNetworks(out, in);
    }

    public boolean isUSBOpened() { return liveFragment != null && liveFragment.is_usb_opened; }

    public List<String> getUSBCameraResolutions() { return resolutions; }

    public void saveUSBCameraResolutions() { resolutions = sharedViewModel.getCameraResolution(); }

    public boolean isPlayingWifi() { return sourceID != -1; }

    public void setWifiInformation(String in, String out) {
        HashMap<String,String> map = new HashMap<>();
        map.put("wifi_in", in); map.put("wifi_out", out);
        sharedViewModel.postEvent(EventType.SET_WIFI_INFORMATION_LIVE, map);
    }

    public void prepareWifiCamera(Camera cam) {
        mCamera = cam;
        if (mWifiService != null && isPlayingWifi()) mWifiService.playStreaming(mCamera);
    }

    /* --- Ping Wi-Fi camera & load stream (unchanged) --------------------- */
    boolean ping_success = false;

    void pingWifiServer(String url) {
        ping_success = false;
        Pinger p = new Pinger();
        is_dialog = true;
        if (mWifiService != null && mCamera.camera_wifi_type == AppConstant.WIFI_TYPE_LAWMATE)
            mWifiService.startAPICalling();
        dlg_progress.show();

        p.setOnPingListener(new Pinger.OnPingListener() {
            @Override public void OnStart(@NonNull PingInfo i){ MessageUtil.showToast(instance,"Pinging start"); }
            @Override public void OnStop (@NonNull PingInfo i){ dlg_progress.dismiss(); }
            @Override public void OnSendError(@NonNull PingInfo i,int seq){
                if (seq>=10){ ping_success=false; MessageUtil.showToast(instance,"Ping error"); i.Pinger.Stop(i.PingId);}
            }
            @Override public void OnReplyReceived(@NonNull PingInfo i,int seq,int time){
                if (seq>=5){ ping_success=true; i.Pinger.Stop(i.PingId); runOnUiThread(()->{
                    MessageUtil.showToast(instance,"Ping success"); loadWifiStream();});
                }
            }
            @Override public void OnTimeout(@NonNull PingInfo i,int seq){
                if (seq>=10){ ping_success=false; i.Pinger.Stop(i.PingId);
                    runOnUiThread(() -> MessageUtil.showToast(instance,"Ping failure: Not accessible"));
                }
            }
            @Override public void OnException(@NonNull PingInfo i,@NonNull Exception e,boolean f){
                ping_success=false; i.Pinger.Stop(i.PingId);
                runOnUiThread(() -> { dlg_progress.dismiss();
                    MessageUtil.showToast(instance,"Ping failure: Not accessible");
                });
            }
        });

        switch (mCamera.camera_wifi_type) {
            case AppConstant.WIFI_TYPE_LAWMATE: p.Ping("192.168.1.254"); break;
            case AppConstant.WIFI_TYPE_VCS    : p.Ping("192.168.60.1");  break;
            default                           : p.Ping(CommonUtil.getDomainIP(url));
        }
    }

    /* ─────────────────────────────────────────────────────────────────────────
     *  Native → Java callbacks (player / pipeline state)
     * ──────────────────────────────────────────────────────────────────────── */

    /** Native layer attempted a reconnection to the RTSP source. */
    public void AboutReconnect(int sourceId, boolean success) {
        Log.d(TAG, "AboutReconnect: src=" + sourceId + "  success=" + success);
    }

    /** Native notifies that the input stream's state has changed. */
    public void AboutChangeInputState(int sourceId, final int state) {
        Log.d(TAG, "AboutChangeInputState: src=" + sourceId + "  state=" + state);
        runOnUiThread(() -> {
            /*  Hook UI feedback here if you wish (e.g. buffering spinner) */
        });
    }

    /** Native notifies that an output (push/record) state has changed. */
    @SuppressLint("DefaultLocale")
    public void AboutChangeOutputState(final int outId, final int state) {
        writeLog(String.format("Output %d → state %d", outId, state), /*space*/1, /*desc*/4);
        runOnUiThread(() -> {
            /*  Update a status LED / toast, if desired.                      */
        });
    }

    /** Native side requests an emergency stop of *all* streaming. */
    public void FroceStop() {
        Log.w(TAG, "Force-stop received from native layer");
        runOnUiThread(() -> {
            stopAllServices();
        });
    }
    /* ─────────────────────────────────────────────────────────────────────────
     *  Wi-Fi pipeline: loader, player, stopper, helpers
     * ──────────────────────────────────────────────────────────────────────── */

    void loadWifiStream() {
        if (mWifiService == null) {
            return;
        }
        boolean should_transcode = AppPreference.getBool(AppPreference.KEY.TRANS_APPLY_SETTINGS, false);
        if (should_transcode) {
            pushOpt  = new EncOpt();
            writeOpt = new EncOpt();
            textOpt  = new TextOverlayOption();

            pushOpt.bitrate   = AppPreference.getInt (AppPreference.KEY.TRANS_BITRATE,   200000);
            if (pushOpt.bitrate   <= 0) pushOpt.bitrate   = -1;
            pushOpt.width     = AppPreference.getInt (AppPreference.KEY.TRANS_WIDTH,    640);
            if (pushOpt.width     <= 0) pushOpt.width     = -1;
            pushOpt.height    = AppPreference.getInt (AppPreference.KEY.TRANS_HEIGHT,   360);
            if (pushOpt.height    <= 0) pushOpt.height    = -1;
            pushOpt.framerate = AppPreference.getInt (AppPreference.KEY.TRANS_FRAMERATE, 15);
            if (pushOpt.framerate <= 0) pushOpt.framerate = -1;
            pushOpt.codecName    = "h264";
            pushOpt.disableAudio = AppPreference.getBool(AppPreference.KEY.TRANS_AUDIO_PUSH, false);
            writeOpt.disableAudio= AppPreference.getBool(AppPreference.KEY.TRANS_AUDIO_MP4,  false);
            pushOpt.useMic       = AppPreference.getBool(AppPreference.KEY.TRANS_BOX_USE_MIC,false);

            if (AppPreference.getBool(AppPreference.KEY.TRANS_OVERLAY, false)) {
                String overlayText = AppPreference.getStr(AppPreference.KEY.TRANS_BOX_FORMAT, "'%{localtime\\:}%X'");
                if (!TextUtils.isEmpty(overlayText)) {
                    textOpt.overlayText = overlayText;
                    textOpt.x0          = AppPreference.getInt(AppPreference.KEY.TRANS_BOX_X0, 0);
                    textOpt.y0          = AppPreference.getInt(AppPreference.KEY.TRANS_BOX_Y0, 0);
                    textOpt.fontSize    = AppPreference.getInt(AppPreference.KEY.TRANS_BOX_FONT_SIZE, 28);
                    textOpt.fontColor   = AppPreference.getStr(AppPreference.KEY.TRNAS_BOX_FONT_COLOR, "white");
                    textOpt.fontPath    = AppPreference.getStr(AppPreference.KEY.TRNAS_BOX_FONT, "/storage/emulated/0/Fonts/arial.ttf");
                    if (AppPreference.getBool(AppPreference.KEY.TRANS_BOX_ENABLE, false)) {
                        textOpt.bUseBox  = 1;
                        textOpt.boxColor = AppPreference.getStr(AppPreference.KEY.TRANS_BOX_COLOR, "");
                    }
                    textOpt.bUseOverlay = 1;
                } else {
                    textOpt.bUseOverlay = 0;
                }
            }
        }

        readThread = new Thread(() -> {
            int proto = 1; // UDP by default
            if (BuildConfig.DEBUG) {
                url. set("rtsp://76.239.142.89:7099/0");
                push_url.set("rtsp://41.216.179.31:8554/jorge");
            } else {
                proto = mCamera.rtsp_type;
            }

            sourceID = AddSource(url.get(), proto);

            /* -------- PUSH -------- */
            if (should_push) {
                pushID = AddDepOut(sourceID, push_url.get(), 1, "", -1);
                if (should_transcode && pushOpt.checkMod()) SetEncoderOpt(pushID, pushOpt);
                if (pushID > -1) wifistreamingStarted();
                else runOnUiThread(() -> {
                    MessageUtil.showToast(instance, "Failed to start streaming");
                    sharedViewModel.postEvent(EventType.IC_STREAM_LIVE,"");
                });
            } else {
                pushID = -1;
                runOnUiThread(() -> sharedViewModel.postEvent(EventType.IC_STREAM_LIVE,""));
            }

            if (should_transcode && textOpt.bUseOverlay != 0) addOverlayToOut(textOpt, pushID);

            /* -------- WRITE (MP4) -------- */
            if (should_write) {
                String mp4Path = AppPreference.getStr(AppPreference.KEY.VIDEO_PATH, ResourceUtil.getRecordPath());
                File   file    = ResourceUtil.newMp4File();
                int    interval= SettingsUtils.recordIntervalMin(this);
                if (interval > 0) file = ResourceUtil.newMp4Folder();
                if (file != null) mp4Path = file.getPath();

                String ts = new SimpleDateFormat("yyyyMMdd_HHmmss'.mp4'", Locale.US).format(new Date());
                mp4Path = mp4Path + "/" + ts;

                writeID = AddDepOut(sourceID, mp4Path, -1, "60", (textOpt.bUseOverlay!=0)?pushID:-1);
                if (should_transcode) {
                    if (pushID == -1 && pushOpt.checkMod()) {
                        pushOpt.disableAudio = writeOpt.disableAudio;
                        SetEncoderOpt(writeID, writeOpt);
                    } else if (writeOpt.checkMod()) {
                        SetEncoderOpt(writeID, writeOpt);
                    }
                }
                sharedViewModel.postEvent(EventType.HANDLE_CAMERA_VIEW_LIVE,"");
            }

            if (should_transcode && pushID == -1 && writeID == -1 && pushOpt.checkMod())
                SetEncoderOpt(-1, pushOpt);

            /* -------- GLSurfacePlayer -------- */
            int glPlayer = -1;
            try {
                glPlayer = AddGLPlayer(sourceID);
                if (glPlayer != -1 && should_transcode) {
                    if ((pushID != -1 && pushOpt.disableAudio) ||
                            (pushID == -1 && writeOpt.disableAudio && writeID != -1))
                        GlPlayerDisableAudio(glPlayer);
                }
            } catch (Exception e) { e.printStackTrace(); }

            if (should_transcode && pushOpt.useMic) SetMicMode(sourceID);

            playThread = new Thread(() -> PullVideo(sourceID));
            playThread.start();

            if (sourceID > -1) StartSource(sourceID);

            if (playThread != null) {
                try { playThread.join(); }
                catch (InterruptedException e) { writeLog("play thread error "+e.getMessage(),1,3); }
                playThread = null;
            }
        });
        readThread.start();

        bindCurrentWifi();
    }

    /* --------------------------------------------------------------------- */
    public void playStream(Camera camera) {
        mCamera = camera;
        if (camera == null || TextUtils.isEmpty(camera.getFormattedURL())) return;
        if (!AppConstant.is_library_use) return;

        setCallbacks();
        try { setWifiInformation(camera.wifi_in, camera.wifi_out); SetNetworks(); }
        catch (Exception e) { e.printStackTrace(); }

        url.set(camera.getFormattedURL());

        String base = AppPreference.getStr(AppPreference.KEY.STREAM_BASE, "");
        String user = AppPreference.getStr(AppPreference.KEY.STREAM_USERNAME, "");
        String pass = AppPreference.getStr(AppPreference.KEY.STREAM_PASSWORD, "");
        String ch   = AppPreference.getStr(AppPreference.KEY.STREAM_CHANNEL,  "");
        push_url.set(CommonUtil.getRTSPURL(base+"/"+ch, user, pass));

        if (camera.camera_wifi_type == AppConstant.WIFI_TYPE_VCS) {
            loadWifiStream();
        } else {
            if (BuildConfig.DEBUG) url.set("rtsp://76.239.142.89:7099/0");
            pingWifiServer(url.get());
        }
    }

    /* --------------------------------------------------------------------- */
    public void networkChanged() {
        if (mWifiService == null && !isPlayingWifi()) return;

        is_dialog = true;
        MessageUtil.showError(this, "Wifi changed, stopping streaming");

        if (isWifiStreaming()) {
            should_push  = false; should_write = false;
            pushID = writeID = -1;
            sharedViewModel.postEvent(EventType.HANDEL_CAM_STREAM_VIEW_LIVE, "notempety");
        }
        stopWifiService();
    }

    /* --------------------------------------------------------------------- */
    public void stopWifiStream() {
        if (mWifiService == null && !isPlayingWifi()) return;

        writeLog("Try stop", 1, 5);
        if (sourceID <= 0) return;

        try { StopVideo(sourceID); }
        catch (Exception e) { writeLog("StopVideo error: "+e.getMessage(),1,5); }

        if (readThread != null && readThread.isAlive()) {
            try { readThread.join(); } catch (InterruptedException ignored) {}
            readThread = null;
        }

        sourceID = pushID = writeID = -1;
        pushOpt  = new EncOpt();
        textOpt  = new TextOverlayOption();

        runOnUiThread(() -> sharedViewModel.postEvent(EventType.HANDEL_CAM_STREAM_VIEW_LIVE,""));
        if (mWifiService != null) mWifiService.stopAPICalling();
        AppPreference.removeKey(AppPreference.KEY.CURRENT_SSID);
    }

    /* --------------------------------------------------------------------- */
    public void wifiSnapshot() {
        Screen(AppPreference.getStr(AppPreference.KEY.STORAGE_LOCATION, ResourceUtil.getRecordPath()));
        MessageUtil.showToast(getApplicationContext(), R.string.Success);
    }

    /* --------------------------------------------------------------------- */
    public interface NetworkConnectionListener { void onResult(boolean connected); }

    public void handleNetwork(NetworkConnectionListener listener) {
        if (BuildConfig.DEBUG) { listener.onResult(true); return; }

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (DeviceUtils.isNetworkAvailable(this)) {
            new Thread(() -> {
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL("https://www.google.com").openConnection();
                    c.setConnectTimeout(5000);
                    if (c.getResponseCode()!=200) fallbackToCellular(cm, listener);
                    else                           runOnUiThread(() -> listener.onResult(true));
                } catch (Exception e) { fallbackToCellular(cm, listener); }
            }).start();
        } else fallbackToCellular(cm, listener);
    }

    private void fallbackToCellular(ConnectivityManager cm, NetworkConnectionListener l) {
        if (!DeviceUtils.isCellularAvailable(this)) { runOnUiThread(() -> l.onResult(false)); return; }

        NetworkRequest req = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR).build();
        cm.requestNetwork(req, new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable (Network n){ cm.bindProcessToNetwork(n); runOnUiThread(() -> l.onResult(true)); }
            @Override public void onUnavailable(){ runOnUiThread(() -> l.onResult(false)); }
            @Override public void onLost      (Network n){ runOnUiThread(() -> l.onResult(false)); }
            @Override public void onLosing    (Network n,int ms){ runOnUiThread(() -> l.onResult(false)); }
        });
    }

    /* --------------------------------------------------------------------- */
    @SuppressLint("InvalidWakeLockTag")
    public void bindCurrentWifi() {
        ContentResolver cr = getContentResolver();
        Settings.System.putInt(cr, Settings.System.WIFI_SLEEP_POLICY, Settings.System.WIFI_SLEEP_POLICY_NEVER);

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CheckmateWake");
        wl.acquire();

        AppPreference.setStr(AppPreference.KEY.CURRENT_SSID, CommonUtil.getWifiSSID(this));
    }

    /* Native method declarations (unchanged) */
    public native void openLog(String path);
    public native void writeLog(String msg,int space,int desc);
    public native void setCallbacks();
    public native void setNetworks(long net1,long net2);
    public native void SetMute(boolean mute);
    public native int  AddSource(String video,int proto);
    public native int  AddDepOut(int src,String out,int prot,String seg,int dep);
    public native int  AddGLPlayer(int src);
    public native void SetEncoderOpt(int out,EncOpt opt);
    public native void SetMicMode(int src);
    public native boolean StartSource(int src);
    public native void PullVideo(int src);
    public native boolean StopVideo(int src);
    public native boolean InitGL();
    public native void ChangeGL(int w,int h);
    public native void DelGL();
    public native void UpdateGl();
    public native void Screen(String dir);
    public native void addOverlayToOut(TextOverlayOption over,int outID);
    public native void GlPlayerDisableAudio(int glOutID);

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        // Ensure the placeholder is visible before saving state
        showTaskListPreview();
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        // Show placeholder when app is being backgrounded due to memory pressure
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            showTaskListPreview();
        }
    }
}   // ───── end of class MainActivity
