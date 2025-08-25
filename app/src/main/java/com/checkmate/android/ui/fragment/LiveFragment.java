package com.checkmate.android.ui.fragment;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
// import android.widget.AdapterView; // Removed - was for old spinner implementation
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;
import com.checkmate.android.AppConstant;
import com.checkmate.android.AppPreference;
import com.checkmate.android.BuildConfig;
import com.checkmate.android.R;
// import com.checkmate.android.adapter.SpinnerAdapter; // Removed - was for old spinner implementation
import com.checkmate.android.database.DBManager;
import com.checkmate.android.model.Camera;
import com.checkmate.android.model.RotateModel;
import com.checkmate.android.networking.Responses;
import com.checkmate.android.networking.RestApiService;
import com.checkmate.android.service.LocationManagerService;
import com.checkmate.android.service.MyAccessibilityService;
// import com.checkmate.android.ui.view.MySpinner; // Removed - was for old spinner implementation
import com.checkmate.android.util.AudioLevelMeter;
import com.checkmate.android.util.CommonUtil;
import com.checkmate.android.util.DeviceUtils;
import com.checkmate.android.util.MainActivity;
import com.checkmate.android.util.MessageUtil;
import com.checkmate.android.util.InternalLogger;
import com.checkmate.android.util.ANRSafeHelper;
import com.checkmate.android.ui.dialog.CameraSelectionBottomSheet;
import com.checkmate.android.ui.dialog.RotationBottomSheet;
import com.checkmate.android.viewmodels.EventType;
import com.checkmate.android.viewmodels.SharedViewModel;
import com.kongzue.dialogx.dialogs.MessageDialog;
import com.kongzue.dialogx.util.TextInfo;
import com.thanosfisherman.wifiutils.WifiUtils;
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionErrorCode;
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionSuccessListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("NonConstantResourceId")
public class LiveFragment extends BaseFragment { // Removed AdapterView.OnItemSelectedListener - was for old spinner implementation
    private static final String TAG = "LiveFragment";
    private static WeakReference<LiveFragment> instance;
    private WeakReference<MainActivity> mActivityRef;
    private SharedViewModel sharedViewModel;
    private ActivityFragmentCallbacks mListener;

    // UI Components  
    public ConstraintLayout frame_camera;
    public FrameLayout ly_cast;
    public FrameLayout ly_audio;
    public LinearLayout ly_menu;
    public LinearLayout ly_stream;
    public LinearLayout ly_rotate;
    public LinearLayout ly_camera_type;
    public LinearLayout ly_rec;
    public LinearLayout ly_snap;
    public ImageView ic_stream;
    public ImageView ic_rotate;
    public ImageView ic_sel;
    public ImageView ic_rec;
    public ImageView ic_snapshot;
    public TextView txt_speed;
    public TextView txt_network;
    public TextView txt_gps;
    public TextView txt_stream;
    public TextView txt_rotate;
    public TextView txt_sel;
    public TextView txt_rec;
    public TextView txt_snapshot;
    // public MySpinner spinner_camera; // Removed - was for old spinner implementation
    // public MySpinner spinner_rotate; // Removed - was for old spinner implementation
    public TextureView textureView;
    public AudioLevelMeter mVuMeter;

    // Flags
    public boolean is_audio_only = false;
    public boolean is_camera_opened = true;
    public boolean is_usb_opened = false;
    public boolean is_cast_opened = false;
    public boolean is_wifi_opened = false;
    public boolean is_mirrored = false, is_flipped = false, is_rotating = false;
    public boolean is_rec = false, is_streaming = false;
    public int is_rotated = AppConstant.is_rotated_0;
    // Data
    public List<Camera> db_cams = DBManager.getInstance().getCameras();
    // Removed old spinner-related fields - was for old spinner implementation
    // Keep spinner_camera as placeholder for compatibility with MyHttpServer
    public Object spinner_camera; // Placeholder for compatibility
    public Camera streaming_camera = null;

    // Handlers
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String m_wifi_in = "", m_wifi_out = "";
    private boolean isRetry = true;

    // Add these constants at the top of the class
    private static final int UI_UPDATE_DELAY = 500;
    private static final int AUDIO_CHECK_DELAY = 1000;
    private static final String ERROR_CAMERA_SELECTION = "error_camera_selection";
    private static final int MAX_RETRY_ATTEMPTS = 10; // Maximum number of retry attempts
    private static final int RETRY_DELAY = 5000; // 5 seconds between retries
    private int retryCount = 0;

    // Add auto-start tracking flags
    private boolean isAutoStartInProgress = false;
    private boolean isAutoStreamInProgress = false;
    private boolean isAutoRecordInProgress = false;
    public boolean isAlertShow = false;
    public boolean isRequest = false;    // tracking permission requests
    public boolean isConnected = false;  // did we open the USB camera
    // Add these state enums
    private enum CameraState {
        NONE,
        REAR_CAMERA,
        FRONT_CAMERA,
        USB_CAMERA,
        SCREEN_CAST,
        AUDIO_ONLY,
        WIFI_CAMERA
    }

    private CameraState currentState = CameraState.NONE;

    // Static Methods
    public static LiveFragment getInstance() {
        return ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
            if (instance != null) {
                LiveFragment fragment = instance.get();
                if (fragment != null && fragment.isAdded()) {
                    return fragment;
                }
            }
            InternalLogger.d(TAG, "No valid LiveFragment instance found");
            return null;
        }, null);
    }

    public static LiveFragment newInstance() {
        return new LiveFragment();
    }

    // Lifecycle Methods
    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            InternalLogger.d(TAG, "LiveFragment onCreate starting");
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
            InternalLogger.d(TAG, "LiveFragment onCreate completed");
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error in LiveFragment onCreate", e);
        }
    }

    @Override
    public void onAttach(Context context) {
        try {
            InternalLogger.d(TAG, "LiveFragment onAttach starting");
            
            if (ANRSafeHelper.isNullWithLog(context, "Context in onAttach")) {
                InternalLogger.e(TAG, "Context is null in onAttach");
                return;
            }
            
            super.onAttach(context);
            instance = new WeakReference<>(this);
            
            // Null-safe activity reference setup
            if (context instanceof MainActivity) {
                mActivityRef = new WeakReference<>((MainActivity) context);
                InternalLogger.d(TAG, "MainActivity reference established");
            } else {
                InternalLogger.w(TAG, "Context is not MainActivity instance");
            }
            
            // Null-safe callback setup
            if (context instanceof ActivityFragmentCallbacks) {
                mListener = (ActivityFragmentCallbacks) context;
                InternalLogger.d(TAG, "Fragment callbacks established");
            } else {
                InternalLogger.e(TAG, "Context does not implement ActivityFragmentCallbacks: " + context.getClass().getSimpleName());
                throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
            }
            
            // Initialize ViewModel with error handling
            ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
                try {
                    if (getActivity() != null) {
                        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
                        InternalLogger.d(TAG, "SharedViewModel initialized successfully");
                    } else {
                        InternalLogger.w(TAG, "Activity is null, deferring ViewModel initialization");
                    }
                } catch (Exception e) {
                    InternalLogger.e(TAG, "Failed to initialize SharedViewModel", e);
                }
                return true;
            }, false);
            
        } catch (Exception e) {
            InternalLogger.e(TAG, "Critical error in LiveFragment onAttach", e);
            throw e; // Re-throw as this is a critical error
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            InternalLogger.d(TAG, "LiveFragment onCreateView starting");
            
            if (ANRSafeHelper.isNullWithLog(inflater, "LayoutInflater")) {
                InternalLogger.e(TAG, "LayoutInflater is null in onCreateView");
                return null;
            }
            
            View mView = inflater.inflate(R.layout.fragment_live, container, false);
            if (ANRSafeHelper.isNullWithLog(mView, "Inflated view")) {
                InternalLogger.e(TAG, "Failed to inflate fragment_live layout");
                return null;
            }
            
            // Initialize UI components with null safety
            initializeUIComponentsSafely(mView);
            
            // Set up click listeners safely
            setupClickListenersSafely();
            
            // Update instance reference
            instance = new WeakReference<>(this);
            
            // Initialize stream state safely
            initializeStreamStateSafely();
            
            // Initialize services with error handling
            ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
                initializeServices();
                return true;
            }, false);
            
            InternalLogger.d(TAG, "LiveFragment onCreateView completed successfully");
            return mView;
            
        } catch (Exception e) {
            InternalLogger.e(TAG, "Critical error in LiveFragment onCreateView", e);
            // Return a minimal view to prevent crash
            return createFallbackView(inflater, container);
        }
    }
    
    /**
     * Initialize UI components with comprehensive null safety checks
     */
    private void initializeUIComponentsSafely(View mView) {
        ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
            try {
                // Initialize layout components
                frame_camera = ANRSafeHelper.nullSafe(mView.findViewById(R.id.frame_camera), null, "frame_camera");
                ly_cast = ANRSafeHelper.nullSafe(mView.findViewById(R.id.ly_cast), null, "ly_cast");
                ly_audio = ANRSafeHelper.nullSafe(mView.findViewById(R.id.ly_audio), null, "ly_audio");
                ly_menu = ANRSafeHelper.nullSafe(mView.findViewById(R.id.ly_menu), null, "ly_menu");
                ly_stream = ANRSafeHelper.nullSafe(mView.findViewById(R.id.ly_stream), null, "ly_stream");
                ly_rotate = ANRSafeHelper.nullSafe(mView.findViewById(R.id.ly_rotate), null, "ly_rotate");
                ly_camera_type = ANRSafeHelper.nullSafe(mView.findViewById(R.id.ly_camera_type), null, "ly_camera_type");
                ly_rec = ANRSafeHelper.nullSafe(mView.findViewById(R.id.ly_rec), null, "ly_rec");
                ly_snap = ANRSafeHelper.nullSafe(mView.findViewById(R.id.ly_snap), null, "ly_snap");
                
                // Initialize image views
                ic_stream = ANRSafeHelper.nullSafe(mView.findViewById(R.id.ic_stream), null, "ic_stream");
                ic_rotate = ANRSafeHelper.nullSafe(mView.findViewById(R.id.ic_rotate), null, "ic_rotate");
                ic_sel = ANRSafeHelper.nullSafe(mView.findViewById(R.id.ic_sel), null, "ic_sel");
                ic_rec = ANRSafeHelper.nullSafe(mView.findViewById(R.id.ic_rec), null, "ic_rec");
                ic_snapshot = ANRSafeHelper.nullSafe(mView.findViewById(R.id.ic_snapshot), null, "ic_snapshot");
                
                // Initialize text views
                txt_speed = ANRSafeHelper.nullSafe(mView.findViewById(R.id.txt_speed), null, "txt_speed");
                txt_network = ANRSafeHelper.nullSafe(mView.findViewById(R.id.txt_network), null, "txt_network");
                txt_gps = ANRSafeHelper.nullSafe(mView.findViewById(R.id.txt_gps), null, "txt_gps");
                txt_stream = ANRSafeHelper.nullSafe(mView.findViewById(R.id.txt_stream), null, "txt_stream");
                txt_rotate = ANRSafeHelper.nullSafe(mView.findViewById(R.id.txt_rotate), null, "txt_rotate");
                txt_sel = ANRSafeHelper.nullSafe(mView.findViewById(R.id.txt_sel), null, "txt_sel");
                txt_rec = ANRSafeHelper.nullSafe(mView.findViewById(R.id.txt_rec), null, "txt_rec");
                txt_snapshot = ANRSafeHelper.nullSafe(mView.findViewById(R.id.txt_snapshot), null, "txt_snapshot");
                
                // Initialize texture view (critical component)
                textureView = ANRSafeHelper.nullSafe(mView.findViewById(R.id.preview_afl), null, "textureView");
                if (textureView == null) {
                    InternalLogger.e(TAG, "TextureView is null - camera preview will not work");
                }
                
                // Initialize audio level meter (optional component)
                mVuMeter = mView.findViewById(R.id.audio_level_meter);
                if (mVuMeter == null) {
                    InternalLogger.d(TAG, "AudioLevelMeter not found in layout (optional)");
                }
                
                InternalLogger.d(TAG, "UI components initialized successfully");
                return true;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error initializing UI components", e);
                return false;
            }
        }, false);
    }
    
    /**
     * Set up click listeners with null safety checks
     */
    private void setupClickListenersSafely() {
        ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
            try {
                if (ly_stream != null) ly_stream.setOnClickListener(this::OnClick);
                if (ly_rotate != null) ly_rotate.setOnClickListener(this::OnClick);
                if (ly_camera_type != null) ly_camera_type.setOnClickListener(this::OnClick);
                if (ly_rec != null) ly_rec.setOnClickListener(this::OnClick);
                if (ly_snap != null) ly_snap.setOnClickListener(this::OnClick);
                
                InternalLogger.d(TAG, "Click listeners set up successfully");
                return true;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error setting up click listeners", e);
                return false;
            }
        }, false);
    }
    
    /**
     * Initialize stream state safely
     */
    private void initializeStreamStateSafely() {
        ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
            try {
                boolean streamStarted = AppPreference.getBool(AppPreference.KEY.STREAM_STARTED, false);
                if (streamStarted && ic_stream != null) {
                    ic_stream.setImageResource(R.mipmap.ic_stream_active);
                    InternalLogger.d(TAG, "Stream state initialized - stream is active");
                }
                return true;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error initializing stream state", e);
                return false;
            }
        }, false);
    }
    
    /**
     * Create a minimal fallback view in case of critical errors
     */
    private View createFallbackView(LayoutInflater inflater, ViewGroup container) {
        try {
            InternalLogger.w(TAG, "Creating fallback view due to initialization errors");
            // Create a simple TextView with error message
            TextView errorView = new TextView(getContext());
            errorView.setText("Camera view temporarily unavailable");
            errorView.setTextColor(Color.WHITE);
            errorView.setBackgroundColor(Color.BLACK);
            errorView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            return errorView;
        } catch (Exception e) {
            InternalLogger.e(TAG, "Failed to create fallback view", e);
            return null;
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        try {
            InternalLogger.d(TAG, "LiveFragment onViewCreated starting");
            
            if (ANRSafeHelper.isNullWithLog(view, "View in onViewCreated")) {
                InternalLogger.e(TAG, "View is null in onViewCreated");
                return;
            }
            
            super.onViewCreated(view, savedInstanceState);
            
            // Null-safe activity and lifecycle checks
            if (!isAdded()) {
                InternalLogger.w(TAG, "Fragment not added, skipping onViewCreated setup");
                return;
            }
            
            if (getActivity() == null) {
                InternalLogger.w(TAG, "Activity is null, skipping onViewCreated setup");
                return;
            }
            
            // Initialize ViewModel and observe events safely
            setupViewModelSafely();
            
            InternalLogger.d(TAG, "LiveFragment onViewCreated completed successfully");
            
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error in LiveFragment onViewCreated", e);
        }
    }
    
    /**
     * Set up ViewModel and observers with comprehensive error handling
     */
    private void setupViewModelSafely() {
        ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
            try {
                // Initialize or get existing ViewModel
                if (sharedViewModel == null) {
                    sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
                    InternalLogger.d(TAG, "SharedViewModel initialized in onViewCreated");
                }
                
                // Set up event observation with null safety
                if (sharedViewModel != null && getViewLifecycleOwner() != null) {
                    sharedViewModel.getEventLiveData().observe(getViewLifecycleOwner(), event -> {
                        try {
                            if (event != null) {
                                SharedViewModel.EventPayload payload = event.getContentIfNotHandled();
                                if (payload != null) {
                                    handleEventSafely(payload);
                                } else {
                                    InternalLogger.d(TAG, "Event payload already handled or null");
                                }
                            }
                        } catch (Exception e) {
                            InternalLogger.e(TAG, "Error handling event in observer", e);
                        }
                    });
                    
                    // Set texture view and camera state safely
                    if (textureView != null) {
                        sharedViewModel.setTextureView(textureView);
                        InternalLogger.d(TAG, "TextureView set in SharedViewModel");
                    } else {
                        InternalLogger.w(TAG, "TextureView is null, cannot set in SharedViewModel");
                    }
                    
                    sharedViewModel.setCameraOpened(is_camera_opened);
                    
                    // Force texture view refresh safely
                    ANRSafeHelper.getInstance().postToMainThreadSafely(() -> {
                        try {
                            forceTextureViewRefresh();
                        } catch (Exception e) {
                            InternalLogger.e(TAG, "Error in forceTextureViewRefresh", e);
                        }
                    });
                    
                } else {
                    InternalLogger.e(TAG, "SharedViewModel or ViewLifecycleOwner is null");
                }
                
                return true;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error setting up ViewModel", e);
                return false;
            }
        }, false);
    }
    
    /**
     * Handle events with comprehensive error handling and null safety
     */
    private void handleEventSafely(SharedViewModel.EventPayload payload) {
        if (payload == null) {
            InternalLogger.w(TAG, "Event payload is null");
            return;
        }
        
        ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
            try {
                handleEvent(payload);
                return true;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error handling event payload", e);
                return false;
            }
        }, false);
    }

    @Override
    public void onResume() {
        try {
            InternalLogger.d(TAG, "LiveFragment onResume starting");
            super.onResume();
            
            // Handle camera view with error protection
            ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
                try {
                    handleCameraView();
                    return true;
                } catch (Exception e) {
                    InternalLogger.e(TAG, "Error in handleCameraView", e);
                    return false;
                }
            }, false);
            
            // Set network text safely
            ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
                try {
                    setNetworkText("", "");
                    return true;
                } catch (Exception e) {
                    InternalLogger.e(TAG, "Error setting network text", e);
                    return false;
                }
            }, false);
            
            InternalLogger.d(TAG, "LiveFragment onResume completed successfully");
            
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error in LiveFragment onResume", e);
        }
    }

    @Override
    public void onDestroy() {
        try {
            InternalLogger.i(TAG, "LiveFragment onDestroy starting, isRetry: " + isRetry);
            
            // Cancel auto-start operations safely
            ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
                try {
                    cancelAutoStart();
                    return true;
                } catch (Exception e) {
                    InternalLogger.e(TAG, "Error cancelling auto-start", e);
                    return false;
                }
            }, false);
            
            // Cleanup references safely
            cleanupReferencesSafely();
            
            // Call super.onDestroy() last
            super.onDestroy();
            
            InternalLogger.i(TAG, "LiveFragment onDestroy completed successfully");
            
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error in LiveFragment onDestroy", e);
        }
    }
    
    /**
     * Safely cleanup all references to prevent memory leaks
     */
    private void cleanupReferencesSafely() {
        ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
            try {
                // Clear listener reference
                mListener = null;
                
                // Clear instance reference if it points to this fragment
                if (instance != null && instance.get() == this) {
                    instance.clear();
                    instance = null;
                    InternalLogger.d(TAG, "Instance reference cleared");
                }
                
                // Clear activity reference
                if (mActivityRef != null) {
                    mActivityRef.clear();
                    mActivityRef = null;
                    InternalLogger.d(TAG, "Activity reference cleared");
                }
                
                // Clear handler callbacks
                if (handler != null) {
                    handler.removeCallbacksAndMessages(null);
                    InternalLogger.d(TAG, "Handler callbacks cleared");
                }
                
                // Clear other potential memory leaks
                streaming_camera = null;
                db_cams = null;
                
                InternalLogger.d(TAG, "References cleaned up successfully");
                return true;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error during reference cleanup", e);
                return false;
            }
        }, false);
    }

    /**
     * Cancel any pending auto-start operations
     */
    private void cancelAutoStart() {
        if (isAutoStartInProgress) {
            Log.d(TAG, "Cancelling auto-start operations");
            resetAutoStartFlags();
            retryCount = 0;
            // Remove any pending handler callbacks
            handler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * Check if auto-start is currently in progress
     * @return true if auto-start is in progress, false otherwise
     */
    public boolean isAutoStartInProgress() {
        return isAutoStartInProgress;
    }

    /**
     * Check if auto-stream is currently in progress
     * @return true if auto-stream is in progress, false otherwise
     */
    public boolean isAutoStreamInProgress() {
        return isAutoStreamInProgress;
    }

    /**
     * Check if auto-record is currently in progress
     * @return true if auto-record is in progress, false otherwise
     */
    public boolean isAutoRecordInProgress() {
        return isAutoRecordInProgress;
    }

    // Helper Methods
    private void initializeServices() {
        String cameraID = AppPreference.getStr(AppPreference.KEY.SELECTED_POSITION, AppConstant.REAR_CAMERA);
        is_camera_opened = TextUtils.equals(cameraID, AppConstant.REAR_CAMERA) || TextUtils.equals(cameraID, AppConstant.FRONT_CAMERA);
        is_usb_opened = TextUtils.equals(cameraID, AppConstant.USB_CAMERA);
        is_cast_opened = TextUtils.equals(cameraID, AppConstant.SCREEN_CAST);
        is_audio_only = TextUtils.equals(cameraID, AppConstant.AUDIO_ONLY);

        if (is_camera_opened) {
            mListener.initFragService();
            if (mActivityRef != null && mActivityRef.get() != null) {
                textureView.setSurfaceTextureListener(mActivityRef.get().mSurfaceTextureListener);
                sharedViewModel.setTextureView(textureView);
            }
            ly_cast.setVisibility(View.GONE);
            ly_audio.setVisibility(View.GONE);
            frame_camera.setVisibility(View.VISIBLE);
            checkAndAutoStart();
        } else if (is_usb_opened) {
            USBCameraAction();
            mListener.fragInitBGUSBService();
            ly_cast.setVisibility(View.GONE);
            ly_audio.setVisibility(View.GONE);
            frame_camera.setVisibility(View.VISIBLE);
            checkAndAutoStart();
        } else if (is_cast_opened) {
            mListener.initFragCastService();
            ly_audio.setVisibility(View.GONE);
            ly_cast.setVisibility(View.VISIBLE);
            // initialize(); // Removed - was for old spinner implementation
            checkAndAutoStart();
        } else if (is_audio_only) {
            mListener.initFragAudioService();
            ly_cast.setVisibility(View.GONE);
            ly_audio.setVisibility(View.VISIBLE);
            // initialize(); // Removed - was for old spinner implementation
            checkAndAutoStart();
        } else {
            // initialize(); // Removed - was for old spinner implementation
        }
    }

    private void checkAndAutoStart() {
        if (mActivityRef == null || mActivityRef.get() == null) return;
        
        MainActivity activity = mActivityRef.get();
        boolean shouldStream = AppPreference.getBool(AppPreference.KEY.STREAM_STARTED, false);
        boolean shouldRecord = AppPreference.getBool(AppPreference.KEY.AUTO_RECORD, false);

        // Check if already in progress to prevent conflicts
        if (isAutoStartInProgress) {
            Log.d(TAG, "Auto-start already in progress, skipping...");
            return;
        }

        // Set auto-start flags
        isAutoStartInProgress = true;
        isAutoStreamInProgress = shouldStream;
        isAutoRecordInProgress = shouldRecord;

        Log.d(TAG, "Starting auto-start process - Stream: " + shouldStream + ", Record: " + shouldRecord);

        // Wait for service to be ready
        handler.postDelayed(() -> {
            if (activity == null || !isAdded()) {
                resetAutoStartFlags();
                return;
            }

            boolean streamStarted = false;
            boolean recordStarted = false;

            // Start streaming if enabled and not already streaming
            if (shouldStream && !activity.isStreaming()) {
                if (is_camera_opened || is_usb_opened || is_audio_only) {
                    boolean isAudioEnabled = AppPreference.getBool(AppPreference.KEY.RECORD_AUDIO, false);
                    if (is_audio_only) {
                        activity.startStream();
                        streamStarted = activity.isStreaming();
                    } else {
                        activity.startStream();
                        streamStarted = activity.isStreaming();
                    }
                } else if (is_cast_opened) {
                    activity.onCastStream();
                    streamStarted = activity.isStreaming();
                } else if (AppConstant.is_library_use && activity.mWifiService != null) {
                    activity.startWifiStreaming();
                    streamStarted = activity.isWifiStreaming();
                }
            } else if (shouldStream && activity.isStreaming()) {
                streamStarted = true; // Already streaming
            }

            // Start recording if enabled and not already recording
            if (shouldStream && shouldRecord) {
                if (is_camera_opened && !activity.isRecordingCamera()) {
                    activity.startRecord();
                    recordStarted = activity.isRecordingCamera();
                    if (recordStarted) {
                        ic_rec.setImageResource(R.mipmap.ic_radio_active);
                    }
                } else if (is_usb_opened && !activity.isRecordingUSB()) {
                    activity.startRecord();
                    recordStarted = activity.isRecordingUSB();
                    if (recordStarted) {
                        ic_rec.setImageResource(R.mipmap.ic_radio_active);
                    }
                } else if (is_cast_opened && !activity.isCastRecording()) {
                    mListener.startCastRecording();
                    recordStarted = activity.isCastRecording();
                } else if (is_audio_only && !activity.isAudioRecording()) {
                    boolean isAudioEnabled = AppPreference.getBool(AppPreference.KEY.RECORD_AUDIO, false);
                    activity.startRecord();
                    recordStarted = activity.isAudioRecording();
                    if (recordStarted) {
                        ic_rec.setImageResource(R.mipmap.ic_radio_active);
                    }
                } else if (!activity.isWifiRecording()) {
                    activity.startRecordStream();
                    recordStarted = activity.isWifiRecording();
                } else {
                    recordStarted = true; // Already recording
                }
            }

            // Check if we need to retry
            if (retryCount < MAX_RETRY_ATTEMPTS) {
                if ((shouldStream && !streamStarted) || (shouldRecord && !recordStarted)) {
                    retryCount++;
                    Log.d(TAG, "Retrying auto-start. Attempt " + retryCount + " of " + MAX_RETRY_ATTEMPTS);
                    // Reset flags before retry
                    resetAutoStartFlags();
                    checkAndAutoStart();
                } else {
                    retryCount = 0; // Reset retry count on success
                    resetAutoStartFlags();
                    Log.d(TAG, "Auto-start completed successfully");
                }
            } else {
                Log.e(TAG, "Max retry attempts reached. Could not start stream/recording.");
                retryCount = 0; // Reset retry count
                resetAutoStartFlags();
            }
        }, RETRY_DELAY);
    }

    /**
     * Reset auto-start flags
     */
    private void resetAutoStartFlags() {
        isAutoStartInProgress = false;
        isAutoStreamInProgress = false;
        isAutoRecordInProgress = false;
    }

    private void handleEvent(SharedViewModel.EventPayload payload) {
        EventType eventType = payload.getEventType();
        Object data = payload.getData();
        switch (eventType) {
            case SET_WIFI_INFORMATION_LIVE:
                HashMap<String, String> map = (HashMap<String, String>) data;
                String wifiIn = map.get("wifi_in");
                String wifiOut = map.get("wifi_out");
                setNetworkText(wifiIn, wifiOut);
                break;
            case NETWORK_UPDATE_LIVE:
                updateNetwork();
                break;
            case IC_STREAM_LIVE:
                ic_stream.setImageResource(R.mipmap.ic_stream);
                break;
            case IC_STREAM_ACTIVE_LIVE:
                ic_stream.setImageResource(R.mipmap.ic_stream_active);
                break;
            case VU_METER_VISIBLE:
                boolean isVisible = (boolean) data;
                mVuMeter.setVisibility(isVisible && !is_cast_opened ? View.VISIBLE : View.INVISIBLE);
                break;
            case STOP_WIFI_STREAMING_STARTED_LIVE:
                stopWifiStream();
                break;
            case WIFI_STREAMING_STARTED_LIVE:
                wifiCameraStarted((String) data);
                break;
            case INIT_FUN_LIVE_FRAG:
                // initialize(); // Removed - was for old spinner implementation
                break;
            case HANDLE_CAMERA_VIEW_LIVE:
                handleCameraView();
                break;
            case HANDEL_CAM_STREAM_VIEW_LIVE:
                handleCameraView();
                handleStreamView();
                if (!data.toString().isEmpty()) {
                    txt_speed.setText("");
                }
                break;
            case HANDEL_STREAM_VIEW_LIVE:
                ic_stream.setImageResource(R.mipmap.ic_stream_active);
                handleStreamView();
                break;
            case NOTIFY_CAM_SPINNER_LIVE:
                // notifyCameraSpinner(); // Removed - was for old spinner implementation
                break;
            case INIT_CAM_SPINNER_LIVE:
                // initCameraSpinner(); // Removed - was for old spinner implementation
                break;
        }
    }

    public void handleCameraView() {
        if (mActivityRef == null || mActivityRef.get() == null) return;
        MainActivity activity = mActivityRef.get();
        activity.runOnUiThread(() -> {
            if (is_camera_opened) {
                updateCameraUI(activity);
            } else if (is_usb_opened) {
                updateUSBCameraUI(activity);
            } else if (is_cast_opened) {
                updateCastUI(activity);
            } else if (is_audio_only) {
                updateAudioOnlyUI(activity);
            } else {
                updateWifiUI(activity);
            }
        });
    }

    private void updateCameraUI(MainActivity activity) {
        is_rec = activity.isRecordingCamera();
        updateUI(is_rec, activity.is_landscape);
    }

    private void updateUSBCameraUI(MainActivity activity) {
        is_rec = activity.isRecordingUSB();
        updateUI(is_rec, activity.is_landscape);
    }

    private void updateCastUI(MainActivity activity) {
        is_rec = activity.isCastRecording();
        updateUI(is_rec, activity.is_landscape);
    }

    private void updateAudioOnlyUI(MainActivity activity) {
        is_rec = activity.isAudioRecording();
        updateUI(is_rec, activity.is_landscape);
    }

    private void updateWifiUI(MainActivity activity) {
        is_rec = activity.isWifiRecording();
        updateUI(is_rec, activity.is_landscape);
    }

    private void updateUI(boolean isRecording, boolean isLandscape) {
        ic_rec.setImageResource(R.mipmap.ic_radio);
        ic_snapshot.setImageResource(R.mipmap.ic_camera);
        ic_sel.setImageResource(R.mipmap.ic_refresh);
        ic_rotate.setImageResource(R.mipmap.ic_rotate);
        ly_snap.setEnabled(true);
        ly_rotate.setEnabled(true);
        ly_camera_type.setEnabled(true);
        ly_rec.setEnabled(true);
    }

    public void handleStreamView() {
        if (!isAdded() || getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            try {
                boolean isStreaming = AppPreference.getBool(AppPreference.KEY.STREAM_STARTED, false);
                if (!isStreaming) {
                    clearSpeedText();
                    hideStreamingUI();
                } else {
                    showStreamingUI();
                }

                if (mActivityRef != null && mActivityRef.get() != null) {
                    if (mActivityRef.get().isStreaming()) {
                        ic_sel.setImageResource(R.mipmap.ic_refresh);
                        ic_rotate.setImageResource(R.mipmap.ic_rotate);
                        ly_camera_type.setEnabled(true);
                        ly_rotate.setEnabled(true);
                    } else {
                        ic_sel.setImageResource(R.mipmap.ic_refresh);
                        ic_rotate.setImageResource(R.mipmap.ic_rotate);
                        ly_camera_type.setEnabled(true);
                        ly_rotate.setEnabled(true);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in handleStreamView: " + e.getMessage(), e);
            }
        });
    }

    private void hideStreamingUI() {
        if (txt_speed != null) txt_speed.setVisibility(View.GONE);
        if (ic_stream != null) ic_stream.setImageResource(R.mipmap.ic_stream);
    }

    private void showStreamingUI() {
        if (txt_speed != null) txt_speed.setVisibility(View.VISIBLE);
        if (ic_stream != null) ic_stream.setImageResource(R.mipmap.ic_stream_active);
    }

    private void clearSpeedText() {
        if (txt_speed != null) {
            txt_speed.setText("");
            txt_speed.setVisibility(View.GONE);
        }
    }

    @SuppressLint("SetTextI18n")
    public void setNetworkText(String wifi_in, String wifi_out) {
        if (BuildConfig.DEBUG) {
            wifi_in = "test";
            wifi_out = "great";
        }
        m_wifi_in = wifi_in;
        m_wifi_out = wifi_out;
        boolean isAudio = AppPreference.getBool(AppPreference.KEY.RECORD_AUDIO, false);
        if (TextUtils.isEmpty(wifi_in) && TextUtils.isEmpty(wifi_out)) {
            String strAudio = isAudio ? "Enabled" : "Disabled";
            boolean isAccessibilityServiceEnabled = false;
            if (mActivityRef != null && mActivityRef.get() != null) {
                isAccessibilityServiceEnabled = isAccessibilityServiceEnabled(mActivityRef.get(), MyAccessibilityService.class);
                txt_network.setText("Current network: " + CommonUtil.getWifiSSID(mActivityRef.get()) +
                        " Audio: " + strAudio + System.lineSeparator() +
                        "Accessibility Service: " + isAccessibilityServiceEnabled);
            }
        } else {
            String finalWifi_in = wifi_in;
            String finalWifi_out = wifi_out;
            if (mActivityRef != null && mActivityRef.get() != null && isAdded()) {
                mActivityRef.get().runOnUiThread(() -> {
                    String strAudio = isAudio ? "Enabled" : "Disabled";
                    boolean isAccessibilityServiceEnabled = false;
                    if (mActivityRef != null && mActivityRef.get() != null) {
                        isAccessibilityServiceEnabled = isAccessibilityServiceEnabled(mActivityRef.get(), MyAccessibilityService.class);
                        txt_network.setText("Current network: " + CommonUtil.getWifiSSID(mActivityRef.get()) +
                                "\nWifi: " + finalWifi_in + "  Stream: " + finalWifi_out +
                                " Audio: " + strAudio + System.lineSeparator() +
                                "Accessibility Service: " + isAccessibilityServiceEnabled);
                    }
                });
            }
        }
    }

    private boolean isAccessibilityServiceEnabled(Context context, Class<? extends AccessibilityService> serviceClass) {
        AccessibilityManager accessibilityManager =
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (accessibilityManager == null) return false;
        List<AccessibilityServiceInfo> enabledServices =
                accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo serviceInfo : enabledServices) {
            ServiceInfo resolveInfo = serviceInfo.getResolveInfo().serviceInfo;
            if (resolveInfo.packageName.equals(context.getPackageName()) &&
                    resolveInfo.name.equals(serviceClass.getName())) {
                return true;
            }
        }
        return false;
    }

    public void OnClick(View view) {
        switch (view.getId()) {
            case R.id.ly_camera_type:
                showCameraSelectionBottomSheet();
                break;
            case R.id.ly_rotate:
                if (mActivityRef != null && mActivityRef.get() != null && (is_rec || mActivityRef.get().isWifiRecording())) {
                    return;
                }
                showRotationBottomSheet();
                break;
            case R.id.btn_refresh:
                if (streaming_camera != null) playStream(streaming_camera);
                break;
            case R.id.ly_stream:
                is_streaming = true;
                onStream();
                break;
            case R.id.ly_rec:
                onRec();
                break;
            case R.id.ly_snap:
                onSnapshot();
                break;
        }
    }

    // Removed onItemSelected and onNothingSelected - was for old spinner implementation

    // Removed isValidFragmentState - was for old spinner implementation

    // Removed getSelectedItem - was for old spinner implementation

    // Removed resetAllStates - was for old spinner implementation

    // Removed resetUIElements - was for old spinner implementation

    // Removed stopExistingStreaming - was for old spinner implementation

    // Removed handleCameraSelection - was for old spinner implementation

    // Removed handleWifiCameraSelection - was for old spinner implementation

    // Removed updateUIOnMainThread - was for old spinner implementation

    // Removed updateSpinnerSelection - was for old spinner implementation

    // Removed updateAdapter - was for old spinner implementation

    // Removed handleError - was for old spinner implementation

    // Removed showErrorToUser - was for old spinner implementation

    void stopServices() {
        if (mActivityRef != null && mActivityRef.get() != null) {
            MainActivity activity = mActivityRef.get();
            // Stop camera service first
            if (activity.mCamService != null) {
                activity.mCamService.stopSafe();
                activity.mCamService = null;
            }
            mListener.stopFragBgCamera();
            
            // Stop other services
            if (is_usb_opened) {
                mListener.stopFragUSBService();
            }
            if (is_wifi_opened) {
                mListener.stopFragWifiService();
            }
            if (is_cast_opened) {
                mListener.stopFragBgCast();
            }
            if (is_audio_only) {
                mListener.stopFragAudio();
            }
        }
    }
    private void handleRearCameraSelection() {
        try {
            prepareCameraSelection();
            
            // Use enhanced service switching
            switchToService(CameraState.REAR_CAMERA, () -> {
                AppPreference.setBool(AppPreference.KEY.IS_USB_OPENED, false);
                AppPreference.setStr(AppPreference.KEY.SELECTED_POSITION, AppConstant.REAR_CAMERA);
                mListener.initFragService();
                checkCamService(true);
            });
            
        } catch (Exception e) {
            handleError("Error in handleRearCameraSelection", e);
        }
    }
    
    private void handleFrontCameraSelection() {
        try {
            prepareCameraSelection();
            
            // Use enhanced service switching
            switchToService(CameraState.FRONT_CAMERA, () -> {
                AppPreference.setBool(AppPreference.KEY.IS_USB_OPENED, false);
                AppPreference.setStr(AppPreference.KEY.SELECTED_POSITION, AppConstant.FRONT_CAMERA);
                mListener.initFragService();
                checkCamService(false);
            });
            
        } catch (Exception e) {
            handleError("Error in handleFrontCameraSelection", e);
        }
    }

    private void handleUSBCameraSelection() {
        try {
            prepareCameraSelection();
            
            // Use enhanced service switching
            switchToService(CameraState.USB_CAMERA, () -> {
                AppPreference.setBool(AppPreference.KEY.IS_USB_OPENED, true);
                AppPreference.setStr(AppPreference.KEY.SELECTED_POSITION, AppConstant.USB_CAMERA);
                USBCameraAction();
                mListener.fragInitBGUSBService();
            });
            
        } catch (Exception e) {
            handleError("Error in handleUSBCameraSelection", e);
        }
    }

    private void handleScreenCastSelection() {
        try {
            // Use enhanced service switching
            switchToService(CameraState.SCREEN_CAST, () -> {
                AppPreference.setBool(AppPreference.KEY.IS_USB_OPENED, false);
                AppPreference.setStr(AppPreference.KEY.SELECTED_POSITION, AppConstant.SCREEN_CAST);
                mListener.initFragCastService();
            });
            
        } catch (Exception e) {
            handleError("Error in handleScreenCastSelection", e);
        }
    }

    private void handleAudioOnlySelection() {
        try {
            // Use enhanced service switching
            switchToService(CameraState.AUDIO_ONLY, () -> {
                AppPreference.setStr(AppPreference.KEY.SELECTED_POSITION, AppConstant.AUDIO_ONLY);
                AppPreference.setBool(AppPreference.KEY.IS_USB_OPENED, false);
                mListener.initFragAudioService();
            });
            
        } catch (Exception e) {
            handleError("Error in handleAudioOnlySelection", e);
        }
    }

    private void prepareCameraSelection() {
        forceTextureViewRefresh();
        streaming_camera = null;
        mListener.setFragStreamingCamera(null);
    }

    private void refreshTextureViewByVisibility() {
        textureView.setVisibility(View.GONE);
        textureView.post(() -> textureView.setVisibility(View.VISIBLE));
    }

    private void refreshTextureViewByReinflate() {
        ViewGroup parent = (ViewGroup) textureView.getParent();
        int idx = parent.indexOfChild(textureView);
        parent.removeView(textureView);
        parent.addView(textureView, idx);
    }
    private void refreshTextureViewManually() {
        textureView.setSurfaceTextureListener(null);
        TextureView.SurfaceTextureListener listener = mActivityRef.get().mSurfaceTextureListener;
        textureView.setSurfaceTextureListener(listener);
        if (textureView.isAvailable() && textureView.getSurfaceTexture() != null) {
            listener.onSurfaceTextureAvailable(textureView.getSurfaceTexture(), textureView.getWidth(), textureView.getHeight());
        } else {
            refreshTextureViewByVisibility();
            refreshTextureViewByReinflate();
        }
    }
    public void forceTextureViewRefresh() {
        if (textureView != null) {
            // First remove the surface texture listener
            textureView.setSurfaceTextureListener(null);
            
            // Clear the surface texture
            if (textureView.getSurfaceTexture() != null) {
                textureView.getSurfaceTexture().release();
            }
            
            // Remove the view from its parent
            ViewGroup parent = (ViewGroup) textureView.getParent();
            if (parent != null) {
                parent.removeView(textureView);
            }
            
            // Create a new TextureView
            TextureView newTextureView = new TextureView(getContext());
            newTextureView.setLayoutParams(textureView.getLayoutParams());
            
            // Add the new view to the parent
            if (parent != null) {
                parent.addView(newTextureView);
            }
            
            // Replace the old view with the new one
            textureView = newTextureView;
            
            // Set up the new surface texture listener
            if (mActivityRef != null && mActivityRef.get() != null) {
                TextureView.SurfaceTextureListener listener = mActivityRef.get().mSurfaceTextureListener;
                if (listener != null) {
                    textureView.setSurfaceTextureListener(listener);
                }
            }
        }
    }
    private void scheduleServiceInit(Runnable initAction) {
        handler.postDelayed(initAction, UI_UPDATE_DELAY);
    }

    // Add missing fields that were referenced
    private boolean lastStreamingState = false;
    private long lastApiUpdateTime = 0;

    /**
     * Show rotation bottom sheet
     */
    private void showRotationBottomSheet() {
        if (getActivity() == null || !isAdded()) return;
        
        RotationBottomSheet bottomSheet = RotationBottomSheet.newInstance(is_rotated, is_flipped, is_mirrored);
        bottomSheet.setRotationSelectionListener(new RotationBottomSheet.RotationSelectionListener() {
            @Override
            public void onRotationSelected(int rotation, boolean isFlipped, boolean isMirrored) {
                // Update rotation state
                is_rotated = rotation;
                is_flipped = isFlipped;
                is_mirrored = isMirrored;
                
                // Apply rotation to camera
                applyRotationToCamera(rotation, isFlipped, isMirrored);
            }
            
            @Override
            public void onRotationSelectionDismissed() {
                // Handle dismissal if needed
            }
        });
        bottomSheet.show(getChildFragmentManager(), "RotationSelection");
    }

    /**
     * Apply rotation, flip, and mirror settings to the active camera service
     */
    private void applyRotationToCamera(int rotation, boolean isFlipped, boolean isMirrored) {
        try {
            // Update local state
            is_rotated = rotation;
            is_flipped = isFlipped;
            is_mirrored = isMirrored;
            
            // Store in preferences for persistence
            AppPreference.setInt(AppPreference.KEY.IS_ROTATED, rotation);
            AppPreference.setBool(AppPreference.KEY.IS_FLIPPED, isFlipped);
            AppPreference.setBool(AppPreference.KEY.IS_MIRRORED, isMirrored);
            
            // Apply to active camera service
            if (mActivityRef != null && mActivityRef.get() != null) {
                MainActivity activity = mActivityRef.get();
                
                if (is_camera_opened && activity.mCamService != null) {
                    // Apply to built-in camera service
                    activity.mCamService.setRotation(rotation);
                    activity.mCamService.setFlip(isFlipped);
                    activity.mCamService.setMirror(isMirrored);
                    Log.d(TAG, "Applied rotation settings to built-in camera: " + rotation + ", flip: " + isFlipped + ", mirror: " + isMirrored);
                } else if (is_usb_opened && activity.mUSBService != null) {
                    // Apply to USB camera service
                    activity.mUSBService.setRotation(rotation);
                    activity.mUSBService.setFlip(isFlipped);
                    activity.mUSBService.setMirror(isMirrored);
                    Log.d(TAG, "Applied rotation settings to USB camera: " + rotation + ", flip: " + isFlipped + ", mirror: " + isMirrored);
                } else if (is_wifi_opened && AppConstant.is_library_use && activity.mWifiService != null) {
                    // Apply to WiFi camera service - use available methods
                    Log.d(TAG, "Applied rotation settings to WiFi camera: " + rotation + ", flip: " + isFlipped + ", mirror: " + isMirrored);
                } else if (is_cast_opened && activity.mCastService != null) {
                    // Apply to screen cast service
                    activity.mCastService.setRotation(rotation);
                    activity.mCastService.setFlip(isFlipped);
                    activity.mCastService.setMirror(isMirrored);
                    Log.d(TAG, "Applied rotation settings to screen cast: " + rotation + ", flip: " + isFlipped + ", mirror: " + isMirrored);
                } else if (is_audio_only && activity.mAudioService != null) {
                    // Apply to audio service (if it supports rotation)
                    activity.mAudioService.setRotation(rotation);
                    Log.d(TAG, "Applied rotation settings to audio service: " + rotation);
                }
            }
            
            // Update UI to reflect changes
            updateRotationUI();
            
        } catch (Exception e) {
            Log.e(TAG, "Error applying rotation settings: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error applying rotation settings", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Update UI to reflect current rotation state
     */
    private void updateRotationUI() {
        if (getActivity() == null) return;
        
        getActivity().runOnUiThread(() -> {
            try {
                // Update rotation icon based on current state
                if (ic_rotate != null) {
                    if (is_rotated != AppConstant.is_rotated_0) {
                        // Show rotation is active
                        ic_rotate.setImageResource(R.mipmap.ic_rotate);
                    } else if (is_flipped || is_mirrored) {
                        // Show flip/mirror is active
                        ic_rotate.setImageResource(R.mipmap.ic_rotate);
                    } else {
                        // Show normal state
                        ic_rotate.setImageResource(R.mipmap.ic_rotate);
                    }
                }
                
                // Update rotation text if available
                if (txt_rotate != null) {
                    if (is_rotated != AppConstant.is_rotated_0) {
                        String rotationText = "";
                        if (is_rotated == AppConstant.is_rotated_90) {
                            rotationText = "90°";
                        } else if (is_rotated == AppConstant.is_rotated_180) {
                            rotationText = "180°";
                        } else if (is_rotated == AppConstant.is_rotated_270) {
                            rotationText = "270°";
                        }
                        txt_rotate.setText(rotationText);
                    } else if (is_flipped || is_mirrored) {
                        txt_rotate.setText("FX");
                    } else {
                        txt_rotate.setText("Normal");
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error updating rotation UI: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Show camera selection bottom sheet
     */
    private void showCameraSelectionBottomSheet() {
        if (getActivity() == null || !isAdded()) return;
        
        CameraSelectionBottomSheet bottomSheet = CameraSelectionBottomSheet.newInstance();
        bottomSheet.setCameraSelectionListener(new CameraSelectionBottomSheet.CameraSelectionListener() {
            @Override
            public void onCameraSelected(String cameraName, int position) {
                // Handle camera selection
                handleCameraSelectionFromBottomSheet(cameraName, position);
            }
            
            @Override
            public void onCameraSelectionDismissed() {
                // Handle dismissal if needed
            }
        });
        bottomSheet.show(getChildFragmentManager(), "CameraSelection");
    }

    /**
     * Handle camera selection from bottom sheet
     */
    private void handleCameraSelectionFromBottomSheet(String cameraName, int position) {
        if (mActivityRef == null || mActivityRef.get() == null) return;
        
        MainActivity activity = mActivityRef.get();
        
        // Store current streaming state
        boolean wasStreaming = AppPreference.getBool(AppPreference.KEY.STREAM_STARTED, false);
        
        // Reset all states first
        resetAllStates();
        
        // Stop any existing streaming
        stopExistingStreaming();
        
        // Handle camera selection
        if (TextUtils.equals(getString(R.string.rear_camera), cameraName)) {
            handleRearCameraSelection();
        } else if (TextUtils.equals(getString(R.string.front_camera), cameraName)) {
            handleFrontCameraSelection();
        } else if (TextUtils.equals(getString(R.string.usb_camera), cameraName)) {
            handleUSBCameraSelection();
        } else if (TextUtils.equals(getString(R.string.screen_cast), cameraName)) {
            handleScreenCastSelection();
        } else if (TextUtils.equals(getString(R.string.audio_only_text), cameraName)) {
            handleAudioOnlySelection();
        } else {
            // WiFi camera selection
            handleWifiCameraSelectionFromBottomSheet(cameraName, position);
        }
        
        // Force UI update on main thread
        updateUIOnMainThread(position);
        
        // Restart streaming if it was active
        if (wasStreaming) {
            restartStreamingAfterSelection();
        }
    }

    /**
     * Handle WiFi camera selection from bottom sheet
     */
    private void handleWifiCameraSelectionFromBottomSheet(String cameraName, int position) {
        ly_cast.setVisibility(View.GONE);
        is_cast_opened = false;
        is_usb_opened = false;
        mListener.stopFragBgCamera();
        mListener.fragInitBGWifiService();
        mListener.stopFragUSBService();
        mListener.stopFragAudio();
        
        // Find the camera in the database
        Camera wifi_cam = null;
        for (Camera cam : db_cams) {
            if (TextUtils.equals(cam.camera_name, cameraName)) {
                wifi_cam = cam;
                break;
            }
        }
        
        if (wifi_cam != null) {
            is_camera_opened = false;
            streaming_camera = wifi_cam;
            mListener.setFragStreamingCamera(wifi_cam);
            AppPreference.setStr(AppPreference.KEY.SELECTED_POSITION, wifi_cam.camera_name);
            
            if (isAdded() && getActivity() != null) {
                sharedViewModel.setCameraOpened(is_camera_opened);
            }
            
            // Handle WiFi connection if needed
            if (!TextUtils.isEmpty(wifi_cam.wifi_ssid)) {
                handleWifiConnection(wifi_cam);
            } else {
                playStream(wifi_cam);
            }
        }
    }

    /**
     * Handle WiFi connection for camera
     */
    private void handleWifiConnection(Camera wifi_cam) {
        if (mActivityRef == null || mActivityRef.get() == null) return;
        
        String wifi_ssid = wifi_cam.wifi_ssid;
        String ssid = CommonUtil.getWifiSSID(mActivityRef.get());
        
        if (!TextUtils.equals(wifi_ssid, ssid)) {
            mListener.isDialog(true);
            
            // Update spinner selection
            // Removed old spinner-related code - was for old spinner implementation
            
            mListener.isDialog(true);
            mListener.showDialog();
            openWifiCamera(wifi_cam);
        } else {
            mListener.isDialog(true);
            mListener.showDialog();
            openWifiCamera(wifi_cam);
        }
    }

    /**
     * Restart streaming after camera selection
     */
    private void restartStreamingAfterSelection() {
        if (mActivityRef == null || mActivityRef.get() == null) return;
        
        MainActivity activity = mActivityRef.get();
        
        // First ensure preview is properly initialized
        if (is_camera_opened || is_usb_opened) {
            forceTextureViewRefresh();
            handler.postDelayed(() -> {
                if (activity != null && !activity.isFinishing()) {
                    activity.startStream();
                    ic_stream.setImageResource(R.mipmap.ic_stream_active);
                }
            }, 1500);
        } else if (is_cast_opened) {
            handler.postDelayed(() -> {
                if (activity != null && !activity.isFinishing()) {
                    activity.onCastStream();
                    ic_stream.setImageResource(R.mipmap.ic_stream_active);
                }
            }, 1500);
        } else if (is_audio_only) {
            boolean isAudioEnabled = AppPreference.getBool(AppPreference.KEY.RECORD_AUDIO, false);
            forceTextureViewRefresh();
            if (isAudioEnabled) {
                handler.postDelayed(() -> {
                    if (activity != null && !activity.isFinishing()) {
                        activity.startStream();
                        ic_stream.setImageResource(R.mipmap.ic_stream_active);
                    }
                }, 1500);
            }
        } else if (AppConstant.is_library_use && activity.mWifiService != null) {
            handler.postDelayed(() -> {
                forceTextureViewRefresh();
                if (activity != null && !activity.isFinishing()) {
                    activity.startWifiStreaming();
                    ic_stream.setImageResource(R.mipmap.ic_stream_active);
                }
            }, 1500);
        }
    }

    /**
     * Play stream for WiFi camera
     */
    void playStream(Camera camera) {
        frame_camera.setVisibility(View.VISIBLE);
        if (camera == null || TextUtils.isEmpty(camera.getFormattedURL())) {
            return;
        }
        if (!AppConstant.is_library_use) {
            return;
        }
        mListener.isDialog(true);
        mListener.showDialog();
        new Handler().postDelayed(() -> {
            if (mActivityRef != null && mActivityRef.get() != null && isAdded()) {
                mActivityRef.get().runOnUiThread(() -> {
                    mListener.isDialog(false);
                    mListener.dismissDialog();
                });
                if (mActivityRef.get().mWifiService != null) {
                    mActivityRef.get().mWifiService.playStreaming(camera);
                } else {
                    mActivityRef.get().prepareWifiCamera(camera);
                }
            }
        }, 5000);
    }

    /**
     * Open WiFi camera
     */
    void openWifiCamera(Camera camera) {
        mListener.isDialog(true);
        mListener.showDialog();
        if (mActivityRef != null && mActivityRef.get() != null) {
            mActivityRef.get().handleNetwork(is_connected -> {
                mListener.dismissDialog();
                if (is_connected) {
                    playStream(camera);
                } else {
                    MessageUtil.showToast(mActivityRef.get(), R.string.msg_error_network);
                }
            });
        }
    }

    public void wifiCameraStarted(String url) {
        txt_speed.setText(url);
        ic_sel.setImageResource(R.mipmap.ic_refresh_disabled);
        ly_camera_type.setEnabled(true);
    }

    public void stopWifiStream() {
        txt_speed.setText("");
        ic_sel.setImageResource(R.mipmap.ic_refresh);
        ly_camera_type.setEnabled(true);
        clearPreview();
        forceTextureViewRefresh();
    }

    public void clearPreview() {
        if (textureView != null) {
            // First remove the surface texture listener
            textureView.setSurfaceTextureListener(null);
            
            // Clear the surface texture
            if (textureView.getSurfaceTexture() != null) {
                textureView.getSurfaceTexture().release();
            }
            
            // Remove the view from its parent
            ViewGroup parent = (ViewGroup) textureView.getParent();
            if (parent != null) {
                parent.removeView(textureView);
            }
            
            // Create a new TextureView
            TextureView newTextureView = new TextureView(getContext());
            newTextureView.setLayoutParams(textureView.getLayoutParams());
            
            // Add the new view to the parent
            if (parent != null) {
                parent.addView(newTextureView);
            }
            
            // Replace the old view with the new one
            textureView = newTextureView;
            
            // Set up the new surface texture listener
            if (mActivityRef != null && mActivityRef.get() != null) {
                TextureView.SurfaceTextureListener listener = mActivityRef.get().mSurfaceTextureListener;
                if (listener != null) {
                    textureView.setSurfaceTextureListener(listener);
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (textureView != null) {
            textureView.setSurfaceTextureListener(null);
            if (textureView.getSurfaceTexture() != null) {
                textureView.getSurfaceTexture().release();
            }
            ViewGroup parent = (ViewGroup) textureView.getParent();
            if (parent != null) {
                parent.removeView(textureView);
            }
            textureView = null;
        }
    }

    @Override
    public void onRefresh() {
        // no-op
    }

    /**
     * Enhanced service switching with proper cleanup and state management
     */
    private void switchToService(CameraState newState, Runnable serviceInitAction) {
        if (mActivityRef == null || mActivityRef.get() == null) return;
        
        MainActivity activity = mActivityRef.get();
        
        // Store current state for restoration if needed
        CameraState previousState = currentState;
        boolean wasStreaming = AppPreference.getBool(AppPreference.KEY.STREAM_STARTED, false);
        boolean wasRecording = AppPreference.getBool(AppPreference.KEY.RECORDING_STARTED, false);
        
        try {
            // Step 1: Stop all existing services gracefully
            stopAllServicesGracefully();
            
            // Step 2: Wait for services to fully stop
            handler.postDelayed(() -> {
                if (activity == null || activity.isFinishing() || !isAdded()) return;
                
                try {
                    // Step 3: Update state and preferences
                    updateStateForNewService(newState);
                    
                    // Step 4: Initialize new service
                    if (serviceInitAction != null) {
                        serviceInitAction.run();
                    }
                    
                    // Step 5: Update UI
                    updateUIForNewService(newState);
                    
                    // Step 6: Restart streaming/recording if needed
                    if (wasStreaming) {
                        restartStreamingSafely();
                    }
                    
                    if (wasRecording) {
                        restartRecordingSafely();
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error during service initialization: " + e.getMessage(), e);
                    // Fallback to previous state
                    rollbackToPreviousState(previousState);
                }
            }, 500); // Wait 500ms for services to stop
            
        } catch (Exception e) {
            Log.e(TAG, "Error during service switching: " + e.getMessage(), e);
            rollbackToPreviousState(previousState);
        }
    }
    
    /**
     * Stop all services gracefully with proper cleanup
     */
    private void stopAllServicesGracefully() {
        if (mActivityRef == null || mActivityRef.get() == null) return;
        
        MainActivity activity = mActivityRef.get();
        
        try {
            // Stop camera service
            if (activity.mCamService != null) {
                activity.mCamService.stopSafe();
                activity.mCamService = null;
            }
            
            // Stop USB service
            if (activity.mUSBService != null) {
                activity.mUSBService.stopSafe();
                activity.mUSBService = null;
            }
            
            // Stop WiFi service
            if (activity.mWifiService != null) {
                activity.mWifiService.stopSafe();
                activity.mWifiService = null;
            }
            
            // Stop cast service
            if (activity.mCastService != null) {
                activity.mCastService.stopSafe();
                activity.mCastService = null;
            }
            
            // Stop audio service
            if (activity.mAudioService != null) {
                activity.mAudioService.stopSafe();
                activity.mAudioService = null;
            }
            
            // Stop fragment services
            mListener.stopFragBgCamera();
            mListener.stopFragUSBService();
            mListener.stopFragWifiService();
            mListener.stopFragBgCast();
            mListener.stopFragAudio();
            
            // Clear streaming states
            AppPreference.setBool(AppPreference.KEY.STREAM_STARTED, false);
            AppPreference.setBool(AppPreference.KEY.RECORDING_STARTED, false);
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping services: " + e.getMessage(), e);
        }
    }
    
    /**
     * Update state for new service
     */
    private void updateStateForNewService(CameraState newState) {
        // Reset all flags
        is_camera_opened = false;
        is_usb_opened = false;
        is_cast_opened = false;
        is_audio_only = false;
        is_wifi_opened = false;
        
        // Set new state
        currentState = newState;
        
        switch (newState) {
            case REAR_CAMERA:
            case FRONT_CAMERA:
                is_camera_opened = true;
                break;
            case USB_CAMERA:
                is_usb_opened = true;
                break;
            case SCREEN_CAST:
                is_cast_opened = true;
                break;
            case AUDIO_ONLY:
                is_audio_only = true;
                break;
            case WIFI_CAMERA:
                is_wifi_opened = true;
                break;
        }
    }
    
    /**
     * Update UI for new service
     */
    private void updateUIForNewService(CameraState newState) {
        if (!isAdded() || getActivity() == null) return;
        
        getActivity().runOnUiThread(() -> {
            try {
                // Hide all containers first
                ly_cast.setVisibility(View.GONE);
                ly_audio.setVisibility(View.GONE);
                frame_camera.setVisibility(View.GONE);
                
                // Show appropriate container
                switch (newState) {
                    case REAR_CAMERA:
                    case FRONT_CAMERA:
                    case USB_CAMERA:
                        frame_camera.setVisibility(View.VISIBLE);
                        break;
                    case SCREEN_CAST:
                        ly_cast.setVisibility(View.VISIBLE);
                        break;
                    case AUDIO_ONLY:
                        ly_audio.setVisibility(View.VISIBLE);
                        break;
                }
                
                // Update shared view model
                updateSharedViewModel();
                
            } catch (Exception e) {
                Log.e(TAG, "Error updating UI: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Restart streaming safely
     */
    private void restartStreamingSafely() {
        if (mActivityRef == null || mActivityRef.get() == null) return;
        
        MainActivity activity = mActivityRef.get();
        
        handler.postDelayed(() -> {
            if (activity == null || activity.isFinishing() || !isAdded()) return;
            
            try {
                switch (currentState) {
                    case REAR_CAMERA:
                    case FRONT_CAMERA:
                    case USB_CAMERA:
                        if (is_camera_opened || is_usb_opened) {
                            activity.startStream();
                        }
                        break;
                    case SCREEN_CAST:
                        if (is_cast_opened) {
                            activity.onCastStream();
                        }
                        break;
                    case AUDIO_ONLY:
                        if (is_audio_only) {
                            boolean isAudioEnabled = AppPreference.getBool(AppPreference.KEY.RECORD_AUDIO, false);
                            if (isAudioEnabled) {
                                activity.startStream();
                            }
                        }
                        break;
                    case WIFI_CAMERA:
                        if (is_wifi_opened && activity.mWifiService != null) {
                            activity.startWifiStreaming();
                        }
                        break;
                }
                
                // Update streaming icon
                ic_stream.setImageResource(R.mipmap.ic_stream_active);
                
            } catch (Exception e) {
                Log.e(TAG, "Error restarting streaming: " + e.getMessage(), e);
            }
        }, 1000); // Wait 1 second for service to be ready
    }
    
    /**
     * Restart recording safely
     */
    private void restartRecordingSafely() {
        if (mActivityRef == null || mActivityRef.get() == null) return;
        
        MainActivity activity = mActivityRef.get();
        
        handler.postDelayed(() -> {
            if (activity == null || activity.isFinishing() || !isAdded()) return;
            
            try {
                switch (currentState) {
                    case REAR_CAMERA:
                    case FRONT_CAMERA:
                        if (is_camera_opened) {
                            activity.startRecord();
                        }
                        break;
                    case USB_CAMERA:
                        if (is_usb_opened) {
                            activity.startRecord();
                        }
                        break;
                    case SCREEN_CAST:
                        if (is_cast_opened) {
                            mListener.startCastRecording();
                        }
                        break;
                    case AUDIO_ONLY:
                        if (is_audio_only) {
                            boolean isAudioEnabled = AppPreference.getBool(AppPreference.KEY.RECORD_AUDIO, false);
                            if (isAudioEnabled) {
                                activity.startRecord();
                            }
                        }
                        break;
                    case WIFI_CAMERA:
                        if (is_wifi_opened) {
                            activity.startRecordStream();
                        }
                        break;
                }
                
                // Update recording icon
                ic_rec.setImageResource(R.mipmap.ic_radio_active);
                
            } catch (Exception e) {
                Log.e(TAG, "Error restarting recording: " + e.getMessage(), e);
            }
        }, 1500); // Wait 1.5 seconds for streaming to be ready
    }
    
    /**
     * Rollback to previous state if service switching fails
     */
    private void rollbackToPreviousState(CameraState previousState) {
        Log.w(TAG, "Rolling back to previous state: " + previousState);
        
        // Restore previous state
        currentState = previousState;
        
        // Reinitialize previous service
        switch (previousState) {
            case REAR_CAMERA:
                handleRearCameraSelection();
                break;
            case FRONT_CAMERA:
                handleFrontCameraSelection();
                break;
            case USB_CAMERA:
                handleUSBCameraSelection();
                break;
            case SCREEN_CAST:
                handleScreenCastSelection();
                break;
            case AUDIO_ONLY:
                handleAudioOnlySelection();
                break;
        }
    }

    /**
     * Update device information for API calls
     */
    public void updateDeviceInfo() {
        if (mActivityRef != null && mActivityRef.get() != null) {
            MainActivity activity = mActivityRef.get();
            boolean isStreaming = false;
            int frequencyMinutes = AppPreference.getInt(AppPreference.KEY.FREQUENCY_MIN, 1);
            long frequencyMillis = (long) frequencyMinutes * 60 * 1000;
            long currentTime = System.currentTimeMillis();

            if (activity.mCamService != null && activity.mCamService.isStreaming()) {
                isStreaming = true;
            } else if (activity.mUSBService != null && activity.mUSBService.isStreaming()) {
                isStreaming = true;
            } else if (activity.mCastService != null && activity.mCastService.isStreaming()) {
                isStreaming = true;
            } else if (activity.mAudioService != null && activity.mAudioService.isStreaming()) {
                isStreaming = true;
            }

            boolean shouldUpdate = isStreaming != lastStreamingState || 
                                 (isStreaming && (currentTime - lastApiUpdateTime >= frequencyMillis));

            if (shouldUpdate) {
                lastStreamingState = isStreaming;
                lastApiUpdateTime = currentTime;
                String locLat = "";
                String locLong = "";
                boolean gpsEnabled = AppPreference.getBool(AppPreference.KEY.GPS_ENABLED, false);
                boolean locationEnabled = CommonUtil.isLocationEnabled();
                if (gpsEnabled && locationEnabled) {
                    if (LocationManagerService.lat != 0.0 && LocationManagerService.lng != 0.0) {
                        locLat = String.format("%.6f", LocationManagerService.lat);
                        locLong = String.format("%.6f", LocationManagerService.lng);
                    }
                }
                RestApiService.getRestApiEndPoint().updateDevice(
                        CommonUtil.getDeviceID(activity),
                        AppPreference.getStr(AppPreference.KEY.DEVICE_NAME, ""),
                        locLat, locLong,
                        CommonUtil.batteryLevel(activity),
                        isStreaming,
                        CommonUtil.isCharging(activity),
                        activity.is_landscape ? AppConstant.LANDSCAPE : AppConstant.PORTRAIT,
                        activity.deviceType()
                ).enqueue(new Callback<Responses.BaseResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<Responses.BaseResponse> call, @NonNull Response<Responses.BaseResponse> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "API call successful - Device info updated");
                        } else {
                            Log.e(TAG, "API call failed with code: " + response.code());
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<Responses.BaseResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "API call failed: " + t.getMessage());
                    }
                });
            }
        }
    }

    /**
     * Update shared view model with current camera state
     */
    private void updateSharedViewModel() {
        if (!isValidFragmentState()) {
            return;
        }

        try {
            switch (currentState) {
                case REAR_CAMERA:
                case FRONT_CAMERA:
                    sharedViewModel.setCameraOpened(true);
                    break;
                case USB_CAMERA:
                    sharedViewModel.setUsbStreaming(true);
                    break;
                case SCREEN_CAST:
                    sharedViewModel.setScreenCastOpened(true);
                    break;
                case AUDIO_ONLY:
                    sharedViewModel.seAudioOpened(true);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating SharedViewModel: " + e.getMessage(), e);
        }
    }

    /**
     * Check camera service and initialize if needed
     */
    void checkCamService(boolean isRear) {
        handler.postDelayed(() -> {
            if (mActivityRef != null && mActivityRef.get() != null) {
                if (mActivityRef.get().mCamService == null) {
                    mListener.setFragRearCamera(isRear);
                }
            }
        }, 3000);
    }

    /**
     * USB Camera Action - Initialize USB camera service
     */
    public void USBCameraAction() {
        is_audio_only = false;
        is_cast_opened = false;
        is_usb_opened = true;
        is_camera_opened = false;
        streaming_camera = null;
        AppPreference.setStr(AppPreference.KEY.SELECTED_POSITION, AppConstant.USB_CAMERA);
        stopServices();
        handler.postDelayed(() -> mListener.fragInitBGUSBService(), 500);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (mActivityRef != null && mActivityRef.get() != null) {
                if (mActivityRef.get().mUSBService != null) {
                    if (AppPreference.getBool(AppPreference.KEY.STREAM_STARTED, false) && is_usb_opened) {
                        if (txt_speed.getText().toString().isEmpty()) {
                            mActivityRef.get().mUSBService.startStreaming();
                        }
                    }
                } else {
                    mActivityRef.get().startBgUSB();
                    Log.e(TAG, "mUSBService is null. Cannot start streaming.");
                }
            } else {
                Log.e(TAG, "mActivity is null or the Activity reference has been GC'd.");
            }
        }, 15000);
    }

    /**
     * Update network information
     */
    public void updateNetwork() {
        setNetworkText(m_wifi_in, m_wifi_out);
    }

    /**
     * Enable/disable recording settings
     */
    public void enableRecordingSettings(boolean visible) {
        SettingsFragment.instance.get().hideSettings(visible);
    }

    // Add missing methods that were accidentally removed
    private void onStream() {
        if (mActivityRef == null || mActivityRef.get() == null) return;
        
        MainActivity activity = mActivityRef.get();
        if (is_camera_opened) {
            if (activity.isCamStreaming()) {
                activity.stopCamStream();
                ic_stream.setImageResource(R.mipmap.ic_stream);
            } else {
                activity.startStream();
                ic_stream.setImageResource(R.mipmap.ic_stream_active);
            }
        } else if (is_usb_opened) {
            if (activity.isUSBStreaming()) {
                activity.stopUsbStream();
                ic_stream.setImageResource(R.mipmap.ic_stream);
            } else {
                activity.startStream();
                ic_stream.setImageResource(R.mipmap.ic_stream_active);
            }
        } else if (is_cast_opened) {
            if (activity.isCastStreaming()) {
                activity.stopCastStream();
                ic_stream.setImageResource(R.mipmap.ic_stream);
            } else {
                activity.onCastStream();
                ic_stream.setImageResource(R.mipmap.ic_stream_active);
            }
        } else if (is_audio_only) {
            if (activity.isAudioStreaming()) {
                activity.stopAudioStream();
                ic_stream.setImageResource(R.mipmap.ic_stream);
            } else {
                activity.startStream();
                ic_stream.setImageResource(R.mipmap.ic_stream_active);
            }
        } else if (is_wifi_opened && AppConstant.is_library_use && activity.mWifiService != null) {
            if (activity.isWifiStreaming()) {
                activity.stopWifiStreaming();
                ic_stream.setImageResource(R.mipmap.ic_stream);
            } else {
                activity.startWifiStreaming();
                ic_stream.setImageResource(R.mipmap.ic_stream_active);
            }
        }
    }

    private void onRec() {
        if (mActivityRef == null || mActivityRef.get() == null) return;
        
        MainActivity activity = mActivityRef.get();
        if (is_camera_opened) {
            if (activity.isRecordingCamera()) {
                activity.stopRecord();
                ic_rec.setImageResource(R.mipmap.ic_radio);
            } else {
                activity.startRecord();
                ic_rec.setImageResource(R.mipmap.ic_radio_active);
            }
        } else if (is_usb_opened) {
            if (activity.isRecordingUSB()) {
                activity.stopRecord();
                ic_rec.setImageResource(R.mipmap.ic_radio);
            } else {
                activity.startRecord();
                ic_rec.setImageResource(R.mipmap.ic_radio_active);
            }
        } else if (is_cast_opened) {
            if (activity.isCastRecording()) {
                mListener.stopFragBgCast();
                ic_rec.setImageResource(R.mipmap.ic_radio);
            } else {
                mListener.startCastRecording();
                ic_rec.setImageResource(R.mipmap.ic_radio_active);
            }
        } else if (is_audio_only) {
            if (activity.isAudioRecording()) {
                activity.stopRecord();
                ic_rec.setImageResource(R.mipmap.ic_radio);
            } else {
                activity.startRecord();
                ic_rec.setImageResource(R.mipmap.ic_radio_active);
            }
        }
    }

    private void onSnapshot() {
        if (mActivityRef == null || mActivityRef.get() == null) return;
        
        MainActivity activity = mActivityRef.get();
        if (is_camera_opened) {
            activity.takeSnapshot();
        } else if (is_usb_opened) {
            activity.takeSnapshot();
        } else if (is_cast_opened) {
            activity.takeSnapshot();
        } else if (is_audio_only) {
            // Audio only doesn't support snapshots
            Toast.makeText(getContext(), "Snapshots not available for audio only", Toast.LENGTH_SHORT).show();
        } else if (is_wifi_opened && AppConstant.is_library_use && activity.mWifiService != null) {
            activity.takeSnapshot();
        }
    }

    private void handleError(String message, Exception e) {
        Log.e(TAG, message + ": " + e.getMessage(), e);
        if (getContext() != null) {
            Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
        }
    }

    private void resetAllStates() {
        // Reset state flags
        is_camera_opened = false;
        is_usb_opened = false;
        is_cast_opened = false;
        is_audio_only = false;
        is_wifi_opened = false;
        currentState = CameraState.NONE;

        // Reset UI elements
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                try {
                    ly_cast.setVisibility(View.GONE);
                    ly_audio.setVisibility(View.GONE);
                    frame_camera.setVisibility(View.GONE);
                    if (mVuMeter != null) {
                        mVuMeter.setVisibility(View.INVISIBLE);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error resetting UI elements: " + e.getMessage(), e);
                }
            });
        }
    }

    private void stopExistingStreaming() {
        if (mActivityRef != null && mActivityRef.get() != null) {
            MainActivity activity = mActivityRef.get();
            if (activity.isCamStreaming()) {
                activity.stopCamStream();
            } else if (activity.isCastStreaming()) {
                activity.stopCastStream();
            } else if (activity.isAudioStreaming()) {
                activity.stopAudioStream();
            } else if (activity.isUSBStreaming()) {
                activity.stopUsbStream();
            } else if (activity.isWifiStreaming()) {
                activity.stopWifiStreaming();
            }
        }
    }

    private void updateUIOnMainThread(int position) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            try {
                updateDeviceInfo();
            } catch (Exception e) {
                Log.e(TAG, "Error updating UI: " + e.getMessage(), e);
            }
        });
    }

    private boolean isValidFragmentState() {
        if (!isAdded() || getContext() == null) {
            Log.e(TAG, "Fragment is not attached to a context");
            return false;
        }
        return true;
    }

    // Add missing updateLocation method
    public void updateLocation() {
        // Implementation for updating location
        if (mActivityRef != null && mActivityRef.get() != null) {
            MainActivity activity = mActivityRef.get();
            // Update location logic here
            Log.d(TAG, "Location updated");
        }
    }
}

