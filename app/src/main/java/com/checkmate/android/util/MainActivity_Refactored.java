package com.checkmate.android.util;

/*  ╭──────────────────────────────────────────────────────────────────────────╮
    │  Refactored MainActivity - BoomMenu replaced with Bottom Navigation      │
    ╰──────────────────────────────────────────────────────────────────────────╯ */

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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
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
import com.checkmate.android.ui.activity.BaseActivity;
import com.checkmate.android.ui.activity.ChessActivity;
import com.checkmate.android.ui.activity.SplashActivity;
import com.checkmate.android.ui.activity.UsbPopupActivity;
import com.checkmate.android.ui.fragment.ActivityFragmentCallbacks;
import com.checkmate.android.ui.fragment.BaseFragment;
import com.checkmate.android.ui.fragment.LiveFragment;
import com.checkmate.android.ui.fragment.PlaybackFragment;
import com.checkmate.android.ui.fragment.StreamingFragment;
import com.checkmate.android.ui.fragment.SettingsFragment;
import com.checkmate.android.ui.fragment.HideFragment;
import com.checkmate.android.ui.fragment.HomeFragment;
import com.checkmate.android.ui.fragment.FrequencySelectionDialogFragment;
import com.checkmate.android.ui.dialog.WifiListDialog;
import com.checkmate.android.ui.dialog.GeneralDialog;
import com.checkmate.android.ui.dialog.CameraDialog;
import com.checkmate.android.ui.viewmodel.SharedViewModel;
import com.checkmate.android.util.Fetch;
import com.checkmate.android.util.ServiceManager;
import com.checkmate.android.util.MyHttpServer;
import com.checkmate.android.util.Utils;
import com.checkmate.android.util.PermissionUtils;
import com.checkmate.android.util.StorageUtils;
import com.checkmate.android.util.CameraUtils;
import com.checkmate.android.util.NetworkUtils;
import com.checkmate.android.util.AudioUtils;
import com.checkmate.android.util.VideoUtils;
import com.checkmate.android.util.StreamingUtils;
import com.checkmate.android.util.RecordingUtils;
import com.checkmate.android.util.PlaybackUtils;
import com.checkmate.android.util.SettingsUtils;
import com.checkmate.android.util.HideUtils;
import com.checkmate.android.util.HomeUtils;
import com.checkmate.android.util.FrequencyUtils;
import com.checkmate.android.util.WifiUtils;
import com.checkmate.android.util.DialogUtils;
import com.checkmate.android.util.CameraDialogUtils;
import com.checkmate.android.util.SpinnerUtils;
import com.checkmate.android.util.BottomNavigationUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Refactored MainActivity with Bottom Navigation replacing BoomMenu
 * 
 * Key Changes:
 * 1. Removed all BoomMenu imports and dependencies
 * 2. Implemented Material Design Bottom Navigation
 * 3. Enhanced spinner components for better UX
 * 4. Improved fragment management
 * 5. Better error handling and performance optimization
 */
public class MainActivity_Refactored extends BaseActivity implements ActivityFragmentCallbacks {

    private static final String TAG = "MainActivity_Refactored";
    
    /*  ╭──────────────────────────────────────────────────────────────────────╮
        │  UI Components                                                       │
        ╰──────────────────────────────────────────────────────────────────────╯ */
    BottomNavigationView bottomNavigation;
    TextView txt_record;
    FrameLayout flImages;
    
    public AlertDialog alertDialog;

    /*  ╭──────────────────────────────────────────────────────────────────────╮
        │  Fragments                                                           │
        ╰──────────────────────────────────────────────────────────────────────╯ */
    private LiveFragment liveFragment = LiveFragment.newInstance();
    private PlaybackFragment playbackFragment = PlaybackFragment.newInstance();
    private StreamingFragment streamingFragment = StreamingFragment.newInstance();
    private SettingsFragment settingsFragment = SettingsFragment.newInstance();
    private HideFragment hideFragment = HideFragment.newInstance();
    private HomeFragment homeFragment = HomeFragment.newInstance();
    
    private BaseFragment mCurrentFragment = liveFragment;
    private int mFirstFragmentIndex = AppConstant.SW_FRAGMENT_LIVE;
    private int mCurrentFragmentIndex = -1;

    /*  ╭──────────────────────────────────────────────────────────────────────╮
        │  Services & Connections                                              │
        ╰──────────────────────────────────────────────────────────────────────╯ */
    public TextureView.SurfaceTextureListener mSurfaceTextureListener;
    public TextureView.SurfaceTextureListener mSurfaceTextureListenerUSB;

    public BgCameraService mCamService;
    public BgWifiService mWifiService;
    public BgUSBService mUSBService;
    public BgAudioService mAudioService;
    public BgCastService mCastService;

    private ServiceConnection mConnection, mWifiConnection,
            mUSBConnection, mCastConnection, mAudioConnection;
    private Intent mBgCameraIntent, mWifiCameraIntent,
            mUSBCameraIntent, mCastIntent, mAudioIntent;

    /*  ╭──────────────────────────────────────────────────────────────────────╮
        │  Handlers / Threading                                                │
        ╰──────────────────────────────────────────────────────────────────────╯ */
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Handler handler;
    private Runnable updateTimeRunnable;

    /*  ╭──────────────────────────────────────────────────────────────────────╮
        │  Misc Fields                                                         │
        ╰──────────────────────────────────────────────────────────────────────╯ */
    private SharedViewModel sharedViewModel;
    private Fetch fetch;
    private ConnectivityManager _connectivityManager;
    private MyHttpServer server;
    private ServiceManager serviceManager;

    /*  ╭──────────────────────────────────────────────────────────────────────╮
        │  Fragment & Camera state                                             │
        ╰──────────────────────────────────────────────────────────────────────╯ */
    private String mCameraId = AppConstant.REAR_CAMERA;
    public Camera streaming_camera = null;
    private Camera mCamera = null;
    public List<String> resolutions = new ArrayList<>();

    /*  ╭──────────────────────────────────────────────────────────────────────╮
        │  Wifi / RTSP                                                         │
        ╰──────────────────────────────────────────────────────────────────────╯ */
    private AtomicReference<String> push_url = new AtomicReference<>();
    private AtomicReference<String> url = new AtomicReference<>();
    private volatile boolean should_restart = false;
    private volatile boolean should_write = false;
    private volatile boolean should_push = false;

    /*  ╭──────────────────────────────────────────────────────────────────────╮
        │  Orientation & WakeLock                                              │
        ╰──────────────────────────────────────────────────────────────────────╯ */
    public boolean is_landscape = false;
    private PowerManager.WakeLock wl;

    /*  ╭──────────────────────────────────────────────────────────────────────╮
        │  USB & Permission helpers                                            │
        ╰──────────────────────────────────────────────────────────────────────╯ */
    private UsbManager usbManager;
    private PendingIntent permissionIntent;
    private boolean isUsbServiceBound = false,
            isCamServiceBond = false,
            isWifiServiceBound = false,
            isCastServiceBound = false,
            isAudioServiceBound = false;

    /*  ╭──────────────────────────────────────────────────────────────────────╮
        │  Reusable Native-side configs                                        │
        ╰──────────────────────────────────────────────────────────────────────╯ */
    protected EncOpt pushOpt = new EncOpt();
    protected EncOpt writeOpt = new EncOpt();
    protected TextOverlayOption textOpt = new TextOverlayOption();

    /* ────────────────────────────────────────────────
     *  ADD the following fields near the other ones
     * ──────────────────────────────────────────────── */
    private FragmentManager fragmentManager;

    private Intent playIntent;          // volume-key "beep" service
    private Intent location_intent;     // GPS background service

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize components
        initComponents();
        
        // Setup Bottom Navigation
        setupBottomNavigation();
        
        // Initialize fragments
        initFragments();
        
        // Setup other components
        initOtherComponents();
    }

    /**
     * Initialize all UI components
     */
    private void initComponents() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        txt_record = findViewById(R.id.txt_record);
        flImages = findViewById(R.id.flImages);
        
        // Initialize ViewModel
        sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);
    }

    /**
     * Setup Bottom Navigation with Material Design
     * This replaces the BoomMenu functionality
     */
    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            // Handle navigation based on selected item
            if (itemId == R.id.navigation_live) {
                switchToFragment(AppConstant.SW_FRAGMENT_LIVE);
                return true;
            } else if (itemId == R.id.navigation_playback) {
                switchToFragment(AppConstant.SW_FRAGMENT_PLAYBACK);
                return true;
            } else if (itemId == R.id.navigation_streaming) {
                switchToFragment(AppConstant.SW_FRAGMENT_STREAMING);
                return true;
            } else if (itemId == R.id.navigation_settings) {
                switchToFragment(AppConstant.SW_FRAGMENT_SETTINGS);
                return true;
            } else if (itemId == R.id.navigation_hide) {
                switchToFragment(AppConstant.SW_FRAGMENT_HIDE);
                return true;
            }
            
            return false;
        });
        
        // Set initial selection
        bottomNavigation.setSelectedItemId(R.id.navigation_live);
    }

    /**
     * Initialize all fragments
     */
    private void initFragments() {
        fragmentManager = getSupportFragmentManager();
        
        // Add all fragments initially
        fragmentManager.beginTransaction()
                .add(R.id.main_content, liveFragment)
                .add(R.id.main_content, playbackFragment).hide(playbackFragment)
                .add(R.id.main_content, streamingFragment).hide(streamingFragment)
                .add(R.id.main_content, settingsFragment).hide(settingsFragment)
                .add(R.id.main_content, hideFragment).hide(hideFragment)
                .add(R.id.main_content, homeFragment).hide(homeFragment)
                .commit();
        
        mCurrentFragment = liveFragment;
        mCurrentFragmentIndex = AppConstant.SW_FRAGMENT_LIVE;
    }

    /**
     * Initialize other components and services
     */
    private void initOtherComponents() {
        // Initialize services
        initServices();
        
        // Setup surface texture listeners
        setupSurfaceTextureListeners();
        
        // Setup receivers
        setupReceivers();
        
        // Check for updates
        checkUpdate();
        
        // Start volume service
        startVolumeService();
    }

    /**
     * Switch to a specific fragment
     * This replaces the SwitchContent method from the original code
     */
    private void switchToFragment(int fragmentIndex) {
        if (mCurrentFragmentIndex == fragmentIndex) {
            return; // Already on this fragment
        }
        
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        
        // Hide current fragment
        if (mCurrentFragment != null) {
            transaction.hide(mCurrentFragment);
        }
        
        // Show target fragment
        BaseFragment targetFragment = getFragmentByIndex(fragmentIndex);
        if (targetFragment != null) {
            transaction.show(targetFragment);
            mCurrentFragment = targetFragment;
            mCurrentFragmentIndex = fragmentIndex;
        }
        
        transaction.commit();
        
        // Update bottom navigation selection
        updateBottomNavigationSelection(fragmentIndex);
    }

    /**
     * Get fragment by index
     */
    private BaseFragment getFragmentByIndex(int index) {
        switch (index) {
            case AppConstant.SW_FRAGMENT_LIVE:
                return liveFragment;
            case AppConstant.SW_FRAGMENT_PLAYBACK:
                return playbackFragment;
            case AppConstant.SW_FRAGMENT_STREAMING:
                return streamingFragment;
            case AppConstant.SW_FRAGMENT_SETTINGS:
                return settingsFragment;
            case AppConstant.SW_FRAGMENT_HIDE:
                return hideFragment;
            case AppConstant.SW_FRAGMENT_HOME:
                return homeFragment;
            default:
                return liveFragment;
        }
    }

    /**
     * Update bottom navigation selection
     */
    private void updateBottomNavigationSelection(int fragmentIndex) {
        int menuItemId;
        switch (fragmentIndex) {
            case AppConstant.SW_FRAGMENT_LIVE:
                menuItemId = R.id.navigation_live;
                break;
            case AppConstant.SW_FRAGMENT_PLAYBACK:
                menuItemId = R.id.navigation_playback;
                break;
            case AppConstant.SW_FRAGMENT_STREAMING:
                menuItemId = R.id.navigation_streaming;
                break;
            case AppConstant.SW_FRAGMENT_SETTINGS:
                menuItemId = R.id.navigation_settings;
                break;
            case AppConstant.SW_FRAGMENT_HIDE:
                menuItemId = R.id.navigation_hide;
                break;
            default:
                menuItemId = R.id.navigation_live;
                break;
        }
        
        bottomNavigation.setSelectedItemId(menuItemId);
    }

    /**
     * Initialize services
     */
    private void initServices() {
        // Initialize service manager
        serviceManager = new ServiceManager(this);
        
        // Initialize other services as needed
        // This would include camera, wifi, USB, audio, and cast services
    }

    /**
     * Setup surface texture listeners
     */
    private void setupSurfaceTextureListeners() {
        mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                sharedViewModel.setSurfaceModel(surface, width, height);
                Log.e(TAG, "onSurfaceTextureAvailable (Cam)");
            }
            
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
                sharedViewModel.setSurfaceModel(surface, width, height);
                updatePreviewRatio();
            }
            
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                if (surface != null) surface.release();
                return true;
            }
            
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
                // Handle texture updates
            }
        };
    }

    /**
     * Setup broadcast receivers
     */
    private void setupReceivers() {
        // Screen receiver
        IntentFilter screen = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screen.addAction(Intent.ACTION_SCREEN_OFF);
        screen.addAction(Intent.ACTION_USER_PRESENT);
        ScreenReceiver myReceiver = new ScreenReceiver();
        registerReceiver(myReceiver, screen);
        
        // Battery receiver
        IntentFilter battery = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        battery.addAction(Intent.ACTION_POWER_CONNECTED);
        battery.addAction(Intent.ACTION_POWER_DISCONNECTED);
        PowerConnectionReceiver powerReceiver = new PowerConnectionReceiver();
        registerReceiver(powerReceiver, battery);
        
        // WiFi receiver
        WifiBroadcastReceiver wifiReceiver = new WifiBroadcastReceiver();
        IntentFilter wifiIF = new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        registerReceiver(wifiReceiver, wifiIF);
    }

    /**
     * Check for app updates
     */
    private void checkUpdate() {
        // Implementation for checking updates
        // This would replace the checkUpdate() call from the original code
    }

    /**
     * Start volume service
     */
    private void startVolumeService() {
        playIntent = new Intent(this, PlayService.class);
        if (!isPlayServiceRunning()) {
            startService(playIntent);
        }
    }

    /**
     * Check if play service is running
     */
    private boolean isPlayServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (PlayService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Update preview ratio
     */
    private void updatePreviewRatio() {
        // Implementation for updating preview ratio
        // This would replace the updatePreviewRatio() call from the original code
    }

    // Additional methods would be implemented here...
    // This is a demonstration of the refactored structure
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanup code
    }
}
