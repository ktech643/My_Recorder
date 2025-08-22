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
import android.widget.AdapterView;
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
import com.checkmate.android.adapter.SpinnerAdapter;
import com.checkmate.android.database.DBManager;
import com.checkmate.android.model.Camera;
import com.checkmate.android.model.RotateModel;
import com.checkmate.android.networking.Responses;
import com.checkmate.android.networking.RestApiService;
import com.checkmate.android.service.LocationManagerService;
import com.checkmate.android.service.MyAccessibilityService;
import com.checkmate.android.ui.view.MySpinner;
import com.checkmate.android.util.AudioLevelMeter;
import com.checkmate.android.util.CommonUtil;
import com.checkmate.android.util.DeviceUtils;
import com.checkmate.android.util.MainActivity;
import com.checkmate.android.util.MessageUtil;
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
public class LiveFragment extends BaseFragment implements AdapterView.OnItemSelectedListener {
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
    public MySpinner spinner_camera;
    public MySpinner spinner_rotate;
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
    public List<String> cam_spinnerArray = new ArrayList<>();
    public List<RotateModel> cam_models;
    public SpinnerAdapter cam_adapter;
    public SpinnerAdapter adapter;
    public Camera streaming_camera = null;

    // Handlers
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int origin_count = 0;
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
        if (instance != null) {
            LiveFragment fragment = instance.get();
            if (fragment != null && fragment.isAdded()) {
                return fragment;
            }
        }
        return null;
    }

    public static LiveFragment newInstance() {
        return new LiveFragment();
    }

    // Lifecycle Methods
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        instance = new WeakReference<>(this);
        if (context instanceof MainActivity) {
            mActivityRef = new WeakReference<>((MainActivity) context);
        }
        if (context instanceof ActivityFragmentCallbacks) {
            mListener = (ActivityFragmentCallbacks) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_live, container, false);
        
        // Initialize UI components
        frame_camera = mView.findViewById(R.id.frame_camera);
        ly_cast = mView.findViewById(R.id.ly_cast);
        ly_audio = mView.findViewById(R.id.ly_audio);
        ly_menu = mView.findViewById(R.id.ly_menu);
        ly_stream = mView.findViewById(R.id.ly_stream);
        ly_rotate = mView.findViewById(R.id.ly_rotate);
        ly_camera_type = mView.findViewById(R.id.ly_camera_type);
        ly_rec = mView.findViewById(R.id.ly_rec);
        ly_snap = mView.findViewById(R.id.ly_snap);
        
        ic_stream = mView.findViewById(R.id.ic_stream);
        ic_rotate = mView.findViewById(R.id.ic_rotate);
        ic_sel = mView.findViewById(R.id.ic_sel);
        ic_rec = mView.findViewById(R.id.ic_rec);
        ic_snapshot = mView.findViewById(R.id.ic_snapshot);
        
        txt_speed = mView.findViewById(R.id.txt_speed);
        txt_network = mView.findViewById(R.id.txt_network);
        txt_gps = mView.findViewById(R.id.txt_gps);
        txt_stream = mView.findViewById(R.id.txt_stream);
        txt_rotate = mView.findViewById(R.id.txt_rotate);
        txt_sel = mView.findViewById(R.id.txt_sel);
        txt_rec = mView.findViewById(R.id.txt_rec);
        txt_snapshot = mView.findViewById(R.id.txt_snapshot);
        
        spinner_camera = mView.findViewById(R.id.spinner_camera);
        spinner_rotate = mView.findViewById(R.id.spinner_rotate);
        
        // TextureView is in the included layout
        textureView = mView.findViewById(R.id.preview_afl);
        
        // Initialize AudioLevelMeter (may not be in this layout, check if exists)
        mVuMeter = mView.findViewById(R.id.audio_level_meter);

        // Set up click listeners for buttons
        ly_stream.setOnClickListener(this::OnClick);
        ly_rotate.setOnClickListener(this::OnClick);
        ly_camera_type.setOnClickListener(this::OnClick);
        ly_rec.setOnClickListener(this::OnClick);
        ly_snap.setOnClickListener(this::OnClick);

        instance = new WeakReference<>(this);

        if (AppPreference.getBool(AppPreference.KEY.STREAM_STARTED, false)) {
            ic_stream.setImageResource(R.mipmap.ic_stream_active);
        }
        initializeServices();
        return mView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (isAdded() && getActivity() != null) {
            sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
            sharedViewModel.getEventLiveData().observe(getViewLifecycleOwner(), event -> {
                if (event != null) {
                    SharedViewModel.EventPayload payload = event.getContentIfNotHandled();
                    if (payload != null) {
                        handleEvent(payload);
                    }
                }
            });
            sharedViewModel.setTextureView(textureView);
            sharedViewModel.setCameraOpened(is_camera_opened);
            forceTextureViewRefresh();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        handleCameraView();
        setNetworkText("", "");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy: isRetry:" + isRetry);
        // Cancel any pending auto-start operations
        cancelAutoStart();
        mListener = null;
        if (instance != null && instance.get() == this) {
            instance.clear();
            instance = null;
        }
        mActivityRef = null;
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
            initialize();
            checkAndAutoStart();
        } else if (is_audio_only) {
            mListener.initFragAudioService();
            ly_cast.setVisibility(View.GONE);
            ly_audio.setVisibility(View.VISIBLE);
            initialize();
            checkAndAutoStart();
        } else {
            initialize();
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
                initialize();
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
                notifyCameraSpinner();
                break;
            case INIT_CAM_SPINNER_LIVE:
                initCameraSpinner();
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
                if (cam_adapter == null) {
                    initCameraSpinner();
                }
                mListener.isDialog(true);
                spinner_camera.performClick();
                break;
            case R.id.ly_rotate:
                if (adapter == null) {
                    initialize();
                }
                if (mActivityRef != null && mActivityRef.get() != null && (is_rec || mActivityRef.get().isWifiRecording())) {
                    return;
                }
                mListener.isDialog(true);
                if (is_camera_opened) {
                    spinner_rotate.performClick();
                } else {
                    rotateStream();
                }
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

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
        if (!isValidFragmentState()) {
            return;
        }

        try {
            // Store current streaming state
            boolean wasStreaming = AppPreference.getBool(AppPreference.KEY.STREAM_STARTED, false);

            // Reset all states first
            resetAllStates();

            // Stop any existing streaming
            stopExistingStreaming();

            String item = getSelectedItem(position);
            if (item == null) {
                return;
            }

            enableRecordingSettings(true);
            handleCameraSelection(item, position);

            // Force UI update on main thread
            updateUIOnMainThread(position);

            // Restart streaming if it was active
            if (wasStreaming && mActivityRef != null && mActivityRef.get() != null) {
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

        } catch (Exception e) {
            handleError("Error in onItemSelected", e);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private boolean isValidFragmentState() {
        if (!isAdded() || getContext() == null) {
            Log.e(TAG, "Fragment is not attached to a context in onItemSelected()");
            return false;
        }
        return true;
    }

    private String getSelectedItem(int position) {
        if (position < 0 || position >= cam_spinnerArray.size()) {
            Log.e(TAG, "Invalid position: " + position);
            return null;
        }
        return cam_spinnerArray.get(position);
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
        resetUIElements();
    }

    private void resetUIElements() {
        if (!isAdded() || getActivity() == null) {
            return;
        }

        getActivity().runOnUiThread(() -> {
            try {
                ly_cast.setVisibility(View.GONE);
                ly_audio.setVisibility(View.GONE);
                frame_camera.setVisibility(View.GONE);
                mVuMeter.setVisibility(View.INVISIBLE);
            } catch (Exception e) {
                Log.e(TAG, "Error resetting UI elements: " + e.getMessage(), e);
            }
        });
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

    private void handleCameraSelection(String item, int position) {
        if (TextUtils.equals(getString(R.string.rear_camera), item)) {
            handleRearCameraSelection();
        } else if (TextUtils.equals(getString(R.string.front_camera), item)) {
            handleFrontCameraSelection();
        } else if (TextUtils.equals(getString(R.string.usb_camera), item)) {
            handleUSBCameraSelection();
        } else if (TextUtils.equals(getString(R.string.screen_cast), item)) {
            handleScreenCastSelection();
        } else if (TextUtils.equals(getString(R.string.audio_only_text), item)) {
            handleAudioOnlySelection();
        } else {
            handleWifiCameraSelection(position);
        }
    }

    private void handleWifiCameraSelection(int position) {
        ly_cast.setVisibility(View.GONE);
        is_cast_opened = false;
        is_usb_opened = false;
        mListener.stopFragBgCamera();
        mListener.fragInitBGWifiService();
        mListener.stopFragUSBService();
        mListener.stopFragAudio();
        Camera wifi_cam = db_cams.get(position - origin_count);
        is_camera_opened = false;
        streaming_camera = wifi_cam;
        mListener.setFragStreamingCamera(wifi_cam);
        AppPreference.setStr(AppPreference.KEY.SELECTED_POSITION, wifi_cam.camera_name);
        if (isAdded() && getActivity() != null) {
            sharedViewModel.setCameraOpened(is_camera_opened);
        }
        if (!TextUtils.isEmpty(wifi_cam.wifi_ssid)) {
            String wifi_ssid = wifi_cam.wifi_ssid;
            String ssid = CommonUtil.getWifiSSID(mActivityRef.get());
            if (!TextUtils.equals(wifi_ssid, ssid)) {
                mListener.isDialog(true);
                for (int i = 0; i < cam_spinnerArray.size(); i++) {
                    cam_models.get(i).is_selected = (i == position);
                }
                cam_adapter.notifyDataSetChanged();
                mListener.isDialog(true);
                if (TextUtils.isEmpty(wifi_cam.wifi_password)) {
                    wifi_cam.wifi_password = "12345678";
                }
                new AlertDialog.Builder(mActivityRef.get())
                        .setTitle(R.string.app_name)
                        .setMessage(String.format(getString(R.string.wifi_warning), wifi_cam.camera_name, wifi_ssid))
                        .setIcon(R.mipmap.ic_launcher)
                        .setPositiveButton(R.string.OK, (dialog, whichButton) -> {
                            dialog.dismiss();
                            mListener.isDialog(true);
                            mListener.showDialog();
                            WifiUtils.withContext(mActivityRef.get())
                                    .connectWith(wifi_ssid, wifi_cam.wifi_password)
                                    .setTimeout(15000)
                                    .onConnectionResult(new ConnectionSuccessListener() {
                                        @Override
                                        public void success() {
                                            mListener.isDialog(false);
                                            openWifiCamera(wifi_cam);
                                            setNetworkText(wifi_cam.wifi_in, wifi_cam.wifi_out);
                                            mListener.dismissDialog();
                                        }
                                        @Override
                                        public void failed(@NonNull ConnectionErrorCode errorCode) {
                                            MessageUtil.showToast(mActivityRef.get(), R.string.connection_fail);
                                            mListener.isDialog(false);
                                            mListener.dismissDialog();
                                        }
                                    })
                                    .start();
                        })
                        .setNegativeButton(R.string.CANCEL, (dialog, whichButton) -> {
                            dialog.dismiss();
                            mListener.isDialog(false);
                            openWifiCamera(wifi_cam);
                            setNetworkText(wifi_ssid, wifi_cam.wifi_out);
                        })
                        .show();
            } else {
                mListener.isDialog(true);
                mListener.showDialog();
                openWifiCamera(wifi_cam);
            }
        } else {
            playStream(wifi_cam);
        }
    }

    private void updateUIOnMainThread(int position) {
        if (!isValidFragmentState()) {
            return;
        }

        getActivity().runOnUiThread(() -> {
            try {
                updateSpinnerSelection(position);
                updateAdapter();
                updateDeviceInfo();
            } catch (Exception e) {
                Log.e(TAG, "Error updating UI: " + e.getMessage(), e);
            }
        });
    }

    private void updateSpinnerSelection(int position) {
        if (spinner_camera != null) {
            spinner_camera.setSelection(position);
        }
    }

    private void updateAdapter() {
        if (cam_adapter != null) {
            cam_adapter.notifyDataSetChanged();
        }
    }

    private void handleError(String message, Exception e) {
        Log.e(TAG, message + ": " + e.getMessage(), e);
        showErrorToUser();
    }

    private void showErrorToUser() {
        if (!isValidFragmentState()) {
            return;
        }

        getActivity().runOnUiThread(() -> {
            try {
                Toast.makeText(getContext(),
                    getString(R.string.error_camera_selection),
                    Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing error message: " + e.getMessage(), e);
            }
        });
    }

    void stopServices() {
        if (mActivityRef != null && mActivityRef.get() != null) {
            MainActivity activity = mActivityRef.get();
            // Stop camera service first
            // Stop other services
            if (is_usb_opened) {
                mListener.stopFragUSBService();
            }
            if (is_camera_opened) {
                mListener.stopFragBgCamera();
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
            currentState = CameraState.REAR_CAMERA;
            is_camera_opened = true;
            frame_camera.setVisibility(View.VISIBLE);

            AppPreference.setBool(AppPreference.KEY.IS_USB_OPENED, false);
            AppPreference.setStr(AppPreference.KEY.SELECTED_POSITION, AppConstant.REAR_CAMERA);

            stopServices();
            scheduleServiceInit(() -> mListener.initFragService());

            updateDeviceInfo();
            checkCamService(true);
            updateSharedViewModel();
        } catch (Exception e) {
            handleError("Error in handleRearCameraSelection", e);
        }
    }
    private void handleFrontCameraSelection() {
        try {
            prepareCameraSelection();
            currentState = CameraState.FRONT_CAMERA;
            is_camera_opened = true;
            frame_camera.setVisibility(View.VISIBLE);

            AppPreference.setBool(AppPreference.KEY.IS_USB_OPENED, false);
            AppPreference.setStr(AppPreference.KEY.SELECTED_POSITION, AppConstant.FRONT_CAMERA);

            stopServices();
            scheduleServiceInit(() -> mListener.initFragService());

            updateDeviceInfo();
            checkCamService(false);
            updateSharedViewModel();
        } catch (Exception e) {
            handleError("Error in handleFrontCameraSelection", e);
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

    private void handleUSBCameraSelection() {
        try {
            prepareCameraSelection();
            currentState = CameraState.USB_CAMERA;
            is_usb_opened = true;

            AppPreference.setBool(AppPreference.KEY.IS_USB_OPENED, true);
            USBCameraAction();

            updateUSBCameraUI();
            updateDeviceInfo();
            updateSharedViewModel();
        } catch (Exception e) {
            handleError("Error in handleUSBCameraSelection", e);
        }
    }

    private void updateUSBCameraUI() {
        frame_camera.setVisibility(View.VISIBLE);

        MainActivity activity = mActivityRef.get();
        if (activity != null && activity.mUSBService != null) {
            activity.mUSBService.isAlertShow = false;
            activity.mUSBService.isServiceStart = true;
            activity.mUSBService.isRequest = true;
            activity.mUSBService.showCameraSelectionDialog();
        }
    }

    private void handleScreenCastSelection() {
        try {
            currentState = CameraState.SCREEN_CAST;
            is_cast_opened = true;

            AppPreference.setBool(AppPreference.KEY.IS_USB_OPENED, false);
            AppPreference.setStr(AppPreference.KEY.SELECTED_POSITION, AppConstant.SCREEN_CAST);

            // Update spinner selection
            for (int i = 0; i < cam_spinnerArray.size(); i++) {
                String val = cam_spinnerArray.get(i);
                cam_models.get(i).is_selected = TextUtils.equals(val, getString(R.string.screen_cast));
            }
            if (cam_adapter != null) {
                cam_adapter.notifyDataSetChanged();
            }

            updateScreenCastUI();
            stopServices();
            scheduleServiceInit(() -> mListener.initFragCastService());
            updateSharedViewModel();
        } catch (Exception e) {
            handleError("Error in handleScreenCastSelection", e);
        }
    }

    private void updateScreenCastUI() {
        mVuMeter.setVisibility(View.INVISIBLE);
        ly_cast.setVisibility(View.VISIBLE);
        ic_stream.setImageResource(R.mipmap.ic_stream);
        clearSpeedText();
    }

    private void handleAudioOnlySelection() {
        try {
            // First stop all existing services
            stopServices();
            
            // Reset all states
            prepareCameraSelection();
            currentState = CameraState.AUDIO_ONLY;
            is_audio_only = true;
            is_camera_opened = false;
            is_usb_opened = false;
            is_cast_opened = false;
            is_wifi_opened = false;

            // Update preferences
            AppPreference.setStr(AppPreference.KEY.SELECTED_POSITION, AppConstant.AUDIO_ONLY);
            AppPreference.setBool(AppPreference.KEY.IS_USB_OPENED, false);

            // Update UI
            updateAudioOnlyUI();
            
            // Initialize audio service with a delay to ensure previous services are stopped
            handler.postDelayed(() -> {
                if (isAdded() && getActivity() != null) {
                    // Double check that camera service is stopped
                    if (mActivityRef != null && mActivityRef.get() != null) {
                        MainActivity activity = mActivityRef.get();
                        if (activity.mCamService != null) {
                            activity.mCamService.stopSafe();
                            activity.mCamService = null;
                        }
                    }
                    mListener.initFragAudioService();
                    updateSharedViewModel();
                }
            }, UI_UPDATE_DELAY);

        } catch (Exception e) {
            handleError("Error in handleAudioOnlySelection", e);
        }
    }

    private void updateAudioOnlyUI() {
        ly_audio.setVisibility(View.VISIBLE);
        ic_stream.setImageResource(R.mipmap.ic_stream);
    }


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

    @SuppressLint("ClickableViewAccessibility")
    public void initialize() {
        initCameraSpinner();
        if (mActivityRef != null && mActivityRef.get() != null) {
            List<RotateModel> models = RotateModel.initialize(mActivityRef.get(), is_rotated, is_flipped, is_mirrored);
            adapter = new SpinnerAdapter(mActivityRef.get(), R.layout.cell_dropdown_rotate, R.id.txt_item, models);
            spinner_rotate.setAdapter(adapter);
            spinner_rotate.setOnItemSelectedEvenIfUnchangedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                    mListener.isDialog(false);
                    switch (position) {
                        case 0:
                            onRotate(AppConstant.is_rotated_90);
                            break;
                        case 1:
                            onRotate(AppConstant.is_rotated_180);
                            break;
                        case 2:
                            onRotate(AppConstant.is_rotated_270);
                            break;
                        case 3:
                            onFlip();
                            break;
                        case 4:
                            onMirror();
                            break;
                        case 5:
                            onNormal();
                            break;
                    }
                    models.get(0).is_selected = is_rotated == AppConstant.is_rotated_90;
                    models.get(1).is_selected = is_rotated == AppConstant.is_rotated_180;
                    models.get(2).is_selected = is_rotated == AppConstant.is_rotated_270;
                    models.get(3).is_selected = is_flipped;
                    models.get(4).is_selected = is_mirrored;
                    models.get(5).is_selected = (is_rotated == AppConstant.is_rotated_0) && !is_flipped && !is_mirrored;
                    adapter.notifyDataSetChanged();
                }
                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                    // no-op
                }
            });
            spinner_rotate.setOnTouchListener((view, motionEvent) -> {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    mListener.isDialog(true);
                }
                return false;
            });
        }
        String cameraID = AppPreference.getStr(AppPreference.KEY.SELECTED_POSITION, AppConstant.REAR_CAMERA);
        if (TextUtils.equals(cameraID, AppConstant.REAR_CAMERA)) {
            for (int i = 0; i < cam_spinnerArray.size(); i++) {
                String val = cam_spinnerArray.get(i);
                cam_models.get(i).is_selected = TextUtils.equals(val, getString(R.string.rear_camera));
            }
        } else if (TextUtils.equals(cameraID, AppConstant.FRONT_CAMERA)) {
            for (int i = 0; i < cam_spinnerArray.size(); i++) {
                String val = cam_spinnerArray.get(i);
                cam_models.get(i).is_selected = TextUtils.equals(val, getString(R.string.front_camera));
            }
        } else if (TextUtils.equals(cameraID, AppConstant.SCREEN_CAST)) {
            for (int i = 0; i < cam_spinnerArray.size(); i++) {
                String val = cam_spinnerArray.get(i);
                cam_models.get(i).is_selected = TextUtils.equals(val, getString(R.string.screen_cast));
            }
        }
        cam_adapter.notifyDataSetChanged();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void initCameraSpinner() {
        cam_spinnerArray = new ArrayList<>();
        if (AppPreference.getBool(AppPreference.KEY.CAM_REAR_FACING, true)) {
            cam_spinnerArray.add(getString(R.string.rear_camera));
        }
        if (AppPreference.getBool(AppPreference.KEY.CAM_FRONT_FACING, true)) {
            cam_spinnerArray.add(getString(R.string.front_camera));
        }
        if (AppPreference.getBool(AppPreference.KEY.CAM_USB, false)) {
            cam_spinnerArray.add(getString(R.string.usb_camera));
        }
        if (AppPreference.getBool(AppPreference.KEY.CAM_CAST, false)) {
            cam_spinnerArray.add(getString(R.string.screen_cast));
        }
        if (AppPreference.getBool(AppPreference.KEY.AUDIO_ONLY, false)) {
            cam_spinnerArray.add(getString(R.string.audio_only_text));
        }
        origin_count = cam_spinnerArray.size();
        if (origin_count == 0) {
            is_camera_opened = false;
        }
        db_cams = DBManager.getInstance().getCameras();
        for (Camera cam : db_cams) {
            cam_spinnerArray.add(cam.camera_name);
        }
        String selected_cam = AppPreference.getStr(AppPreference.KEY.SELECTED_POSITION, AppConstant.REAR_CAMERA);
        is_camera_opened = TextUtils.equals(selected_cam, AppConstant.REAR_CAMERA) || TextUtils.equals(selected_cam, AppConstant.FRONT_CAMERA);
        is_usb_opened = TextUtils.equals(selected_cam, AppConstant.USB_CAMERA);
        is_cast_opened = TextUtils.equals(selected_cam, AppConstant.SCREEN_CAST);
        is_audio_only = TextUtils.equals(selected_cam, AppConstant.AUDIO_ONLY);
        is_wifi_opened = !is_camera_opened && !is_usb_opened && !is_cast_opened && !is_audio_only;

        cam_models = RotateModel.cameraModels(cam_spinnerArray);
        if (isAdded() && getActivity() != null) {
            sharedViewModel.setCameraOpened(is_camera_opened);
        }
        int selected_index = 0;
        for (int i = 0; i < cam_models.size(); i++) {
            RotateModel model = cam_models.get(i);
            model.is_selected = false;
            if (is_camera_opened && TextUtils.equals(model.title, selected_cam)) {
                model.is_selected = true;
                selected_index = i;
            } else if (is_usb_opened && TextUtils.equals(model.title, getString(R.string.usb_camera))) {
                model.is_selected = true;
                selected_index = i;
            } else if (is_cast_opened && TextUtils.equals(model.title, getString(R.string.screen_cast))) {
                model.is_selected = true;
                selected_index = i;
            } else if (is_audio_only && TextUtils.equals(model.title, getString(R.string.audio_only_text))) {
                model.is_selected = true;
                selected_index = i;
            } else if (is_wifi_opened && TextUtils.equals(model.title, selected_cam)) {
                model.is_selected = true;
                selected_index = i;
            }
        }
        if (mActivityRef != null && mActivityRef.get() != null) {
            cam_adapter = new SpinnerAdapter(mActivityRef.get(), R.layout.cell_dropdown_rotate, R.id.txt_item, cam_models);
        }
        spinner_camera.setAdapter(cam_adapter);
        spinner_camera.setOnItemSelectedEvenIfUnchangedListener(this);
        spinner_camera.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
            }
            return false;
        });
        if (!is_camera_opened && !is_wifi_opened && !is_audio_only && !is_usb_opened) {
            int finalSelected_index = selected_index;
            handler.postDelayed(() -> spinner_camera.setSelectionNew(finalSelected_index), 1000);
        }
        spinner_camera.requestFocus();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void notifyCameraSpinner() {
        cam_spinnerArray = new ArrayList<>();
        if (AppPreference.getBool(AppPreference.KEY.CAM_REAR_FACING, true)) {
            cam_spinnerArray.add(getString(R.string.rear_camera));
        }
        if (AppPreference.getBool(AppPreference.KEY.CAM_FRONT_FACING, true)) {
            cam_spinnerArray.add(getString(R.string.front_camera));
        }
        if (AppPreference.getBool(AppPreference.KEY.CAM_USB, false)) {
            cam_spinnerArray.add(getString(R.string.usb_camera));
        }
        if (AppPreference.getBool(AppPreference.KEY.CAM_CAST, false)) {
            cam_spinnerArray.add(getString(R.string.screen_cast));
        }
        if (AppPreference.getBool(AppPreference.KEY.AUDIO_ONLY, false)) {
            cam_spinnerArray.add(getString(R.string.audio_only_text));
        }
        origin_count = cam_spinnerArray.size();
        if (origin_count == 0) {
            is_camera_opened = false;
        }
        db_cams = DBManager.getInstance().getCameras();
        for (Camera cam : db_cams) {
            cam_spinnerArray.add(cam.camera_name);
        }
        String selected_cam = AppPreference.getStr(AppPreference.KEY.SELECTED_POSITION, AppConstant.REAR_CAMERA);
        is_camera_opened = TextUtils.equals(selected_cam, AppConstant.REAR_CAMERA) || TextUtils.equals(selected_cam, AppConstant.FRONT_CAMERA);
        is_usb_opened = TextUtils.equals(selected_cam, AppConstant.USB_CAMERA);
        is_cast_opened = TextUtils.equals(selected_cam, AppConstant.SCREEN_CAST);
        is_audio_only = TextUtils.equals(selected_cam, AppConstant.AUDIO_ONLY);
        is_wifi_opened = !is_camera_opened && !is_usb_opened && !is_cast_opened && !is_audio_only;

        cam_models = RotateModel.cameraModels(cam_spinnerArray);
        if (isAdded() && getActivity() != null) {
            sharedViewModel.setCameraOpened(is_camera_opened);
        }
        int selected_index = 0;
        for (int i = 0; i < cam_models.size(); i++) {
            RotateModel model = cam_models.get(i);
            model.is_selected = false;
            if (is_camera_opened && TextUtils.equals(model.title, selected_cam)) {
                model.is_selected = true;
                selected_index = i;
            } else if (is_usb_opened && TextUtils.equals(model.title, getString(R.string.usb_camera))) {
                model.is_selected = true;
                selected_index = i;
            } else if (is_cast_opened && TextUtils.equals(model.title, getString(R.string.screen_cast))) {
                model.is_selected = true;
                selected_index = i;
            } else if (is_audio_only && TextUtils.equals(model.title, getString(R.string.audio_only_text))) {
                model.is_selected = true;
                selected_index = i;
            } else if (is_wifi_opened && TextUtils.equals(model.title, selected_cam)) {
                model.is_selected = true;
                selected_index = i;
            }
        }
        if (mActivityRef != null && mActivityRef.get() != null) {
            cam_adapter = new SpinnerAdapter(mActivityRef.get(), R.layout.cell_dropdown_rotate, R.id.txt_item, cam_models);
        }
        spinner_camera.setAdapter(cam_adapter);
        spinner_camera.setOnItemSelectedEvenIfUnchangedListener(this);
        spinner_camera.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
            }
            return false;
        });
    }

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

    void checkCamService(boolean isRear) {
        handler.postDelayed(() -> {
            if (mActivityRef != null && mActivityRef.get() != null) {
                if (mActivityRef.get().mCamService == null) {
                    mListener.setFragRearCamera(isRear);
                }
            }
        }, 3000);
    }

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

    void rotateStream() {
        is_rotating = true;
    }

    void onRotate(int angle_index) {
        if (mActivityRef != null && mActivityRef.get() != null && !(is_rec || mActivityRef.get().isWifiRecording())) {
            is_rotated = angle_index;
            if (is_camera_opened) {
                if (angle_index == AppConstant.is_rotated_90) {
                    mActivityRef.get().mCamService.setRotation(90);
                } else if (angle_index == AppConstant.is_rotated_180) {
                    mActivityRef.get().mCamService.setRotation(180);
                } else if (angle_index == AppConstant.is_rotated_270) {
                    mActivityRef.get().mCamService.setRotation(270);
                }
            } else if (is_usb_opened && mActivityRef.get().mUSBService != null) {
                if (angle_index == AppConstant.is_rotated_90) {
                    mActivityRef.get().mUSBService.setRotation(90);
                } else if (angle_index == AppConstant.is_rotated_180) {
                    mActivityRef.get().mUSBService.setRotation(180);
                } else if (angle_index == AppConstant.is_rotated_270) {
                    mActivityRef.get().mUSBService.setRotation(270);
                }
            }
        }
    }

    void onFlip() {
        if (mActivityRef != null && mActivityRef.get() != null) {
            if (is_camera_opened && mActivityRef.get().mCamService != null) {
                is_flipped = !is_flipped;
                mActivityRef.get().mCamService.setFlip(is_flipped);
            }
            if (is_usb_opened && mActivityRef.get().mUSBService != null) {
                is_flipped = !is_flipped;
                mActivityRef.get().mUSBService.setFlip(is_flipped);
            }
        }
    }

    void onMirror() {
        if (mActivityRef != null && mActivityRef.get() != null) {
            if (is_camera_opened && mActivityRef.get().mCamService != null) {
                is_mirrored = !is_mirrored;
                mActivityRef.get().mCamService.setMirror(is_mirrored);
            }
            if (is_usb_opened && mActivityRef.get().mUSBService != null) {
                is_mirrored = !is_mirrored;
                mActivityRef.get().mUSBService.setMirror(is_mirrored);
            }
        }
    }

    void onNormal() {
        if (mActivityRef != null && mActivityRef.get() != null) {
            is_rotated = AppConstant.is_rotated_0;
            is_flipped = false;
            is_mirrored = false;
            if (is_camera_opened && mActivityRef.get().mCamService != null) {
                mActivityRef.get().mCamService.setNormal();
            }
            if (is_usb_opened && mActivityRef.get().mUSBService != null) {
                mActivityRef.get().mUSBService.setNormal();
            }
        }
    }

    void onStream() {

        if (mActivityRef != null && mActivityRef.get() != null) {
            if (is_usb_opened && mActivityRef.get().mUSBService != null && !mActivityRef.get().mUSBService.isConnected) {
                AppPreference.setBool(AppPreference.KEY.STREAM_STARTED, false);
                MessageUtil.showToast(mActivityRef.get(), R.string.no_usb_device);
                ic_stream.setImageResource(R.mipmap.ic_stream);
                is_streaming = false;
                clearPreview();
                forceTextureViewRefresh();
                return;
            }
            if (!DeviceUtils.isNetworkAvailable(mActivityRef.get())) {
                MessageUtil.showToast(mActivityRef.get(), R.string.not_connected);
                AppPreference.setBool(AppPreference.KEY.STREAM_STARTED, false);
                ic_stream.setImageResource(R.mipmap.ic_stream);
                is_streaming = false;
                clearPreview();
                forceTextureViewRefresh();
                return;
            }
        }
        AppPreference.setBool(AppPreference.KEY.IS_USB_OPENED, is_usb_opened);
        if (mListener != null) {
            mListener.isDialog(true);
            mListener.isDialog(false);
        }
        if (mActivityRef != null && mActivityRef.get() != null) {
            if (TextUtils.isEmpty(AppPreference.getStr(AppPreference.KEY.LOGIN_EMAIL, "")) ||
                    TextUtils.isEmpty(AppPreference.getStr(AppPreference.KEY.LOGIN_PASSWORD, "'"))) {
                MessageUtil.showToast(mActivityRef.get(), R.string.login_required);
                return;
            }
            if (!AppPreference.getBool(AppPreference.KEY.BROADCAST, true)) {
                MessageUtil.showToast(mActivityRef.get(), R.string.stream_disabled);
                return;
            }
            if (TextUtils.isEmpty(AppPreference.getStr(AppPreference.KEY.STREAM_CHANNEL, AppConstant.STREAM_CHANNEL))) {
                MessageUtil.showToast(mActivityRef.get(), R.string.no_channel);
                return;
            }
        }
        if (mActivityRef != null && mActivityRef.get() != null) {
            MainActivity activity = mActivityRef.get();
            if (is_camera_opened || is_usb_opened) {
                activity.startStream();
            } else if (is_cast_opened) {
                activity.onCastStream();
            } else if (is_audio_only) {
                mActivityRef.get().startStream();
            } else {
                if (!AppConstant.is_library_use || mActivityRef.get().mWifiService == null) {
                    return;
                }
                if (mActivityRef.get().isWifiStreaming()) {
                    mActivityRef.get().stopWifiStreaming();
                } else {
                    mActivityRef.get().startWifiStreaming();
                }
            }
        }
    }

    void onRec() {

        if (is_camera_opened) {
            if (mActivityRef != null && mActivityRef.get() != null) {
                if (!mActivityRef.get().isRecordingCamera()) {
                    if (mListener != null) {
                        mListener.isDialog(true);
                    }
                    mActivityRef.get().startRecord();
                    ic_rec.setImageResource(R.mipmap.ic_radio_active);
                } else {
                    if (mListener != null) {
                        mListener.isDialog(true);
                    }
                    MessageDialog messageDialog = MessageDialog
                            .show(getString(R.string.confirmation_title), getString(R.string.stop_recording), getString(R.string.Okay), getString(R.string.cancel))
                            .setCancelButton((dialog, v) -> {
                                if (instance != null && instance.get() != null && instance.get().isAdded()) {
                                    dialog.dismiss();
                                }
                                return false;
                            })
                            .setOkButton((baseDialog, v) -> {
                                if (instance != null && instance.get() != null && instance.get().isAdded()) {
                                    mActivityRef.get().stopRecord();
                                    ic_rec.setImageResource(R.mipmap.ic_radio);
                                    AppPreference.setBool(AppPreference.KEY.RECORDING_STARTED, false);
                                    baseDialog.dismiss();
                                }
                                return false;
                            });
                    messageDialog.setOkTextInfo(new TextInfo().setFontColor(Color.parseColor("#000000")).setBold(true));
                    messageDialog.setCancelTextInfo(new TextInfo().setFontColor(Color.parseColor("#000000")).setBold(true));
                }
            }
        } else if (is_usb_opened) {
            if (mActivityRef != null && mActivityRef.get() != null && mActivityRef.get().mUSBService != null && !mActivityRef.get().mUSBService.isConnected) {
                Toast.makeText(mActivityRef.get(), "Please attach camera first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mActivityRef != null && mActivityRef.get() != null) {
                if (!mActivityRef.get().isRecordingUSB()) {
                    mListener.isDialog(true);
                    mActivityRef.get().startRecord();
                    ic_rec.setImageResource(R.mipmap.ic_radio_active);
                } else {
                    mListener.isDialog(true);
                    MessageDialog messageDialog = MessageDialog
                            .show(getString(R.string.confirmation_title), getString(R.string.stop_recording), getString(R.string.Okay), getString(R.string.cancel))
                            .setCancelButton((dialog, v) -> {
                                if (instance != null && instance.get() != null && instance.get().isAdded()) {
                                    dialog.dismiss();
                                }
                                return false;
                            })
                            .setOkButton((baseDialog, v) -> {
                                if (instance != null && instance.get() != null && instance.get().isAdded()) {
                                    ic_rec.setImageResource(R.mipmap.ic_radio);
                                    mActivityRef.get().stopRecord();
                                    AppPreference.setBool(AppPreference.KEY.RECORDING_STARTED, false);
                                    baseDialog.dismiss();
                                }
                                return false;
                            });
                    messageDialog.setOkTextInfo(new TextInfo().setFontColor(Color.parseColor("#000000")).setBold(true));
                    messageDialog.setCancelTextInfo(new TextInfo().setFontColor(Color.parseColor("#000000")).setBold(true));
                }
            }
        } else if (is_cast_opened) {
            if (mActivityRef != null && mActivityRef.get() != null) {
                mListener.startCastRecording();
            }
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    MainActivity activity = mActivityRef.get();
                    if (activity != null) {
                        if (activity.isCastRecording()) {
                            ic_rec.setImageResource(R.mipmap.ic_radio_active);
                        } else {
                            ic_rec.setImageResource(R.mipmap.ic_radio);
                        }
                    }
                }
            },500);
        } else if (is_audio_only) {
            boolean isAudioEnabled = AppPreference.getBool(AppPreference.KEY.RECORD_AUDIO, false);
            if (isAudioEnabled) {
                if (mActivityRef != null && mActivityRef.get() != null && mActivityRef.get().mAudioService != null && !mActivityRef.get().mAudioService.isStreamerReady()) {
                    Toast.makeText(mActivityRef.get(), "Please attach camera first", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mActivityRef != null && mActivityRef.get() != null) {
                    if (!mActivityRef.get().isAudioRecording()) {
                        mListener.isDialog(true);
                        mActivityRef.get().startRecord();
                        ic_rec.setImageResource(R.mipmap.ic_radio_active);
                    } else {
                        mListener.isDialog(true);
                        MessageDialog messageDialog = MessageDialog
                                .show(getString(R.string.confirmation_title), getString(R.string.stop_recording), getString(R.string.Okay), getString(R.string.cancel))
                                .setCancelButton((dialog, v) -> {
                                    if (instance != null && instance.get() != null && instance.get().isAdded()) {
                                        dialog.dismiss();
                                    }
                                    return false;
                                })
                                .setOkButton((baseDialog, v) -> {
                                    if (instance != null && instance.get() != null && instance.get().isAdded()) {
                                        ic_rec.setImageResource(R.mipmap.ic_radio);
                                        mActivityRef.get().stopRecord();
                                        AppPreference.setBool(AppPreference.KEY.RECORDING_STARTED, false);
                                        baseDialog.dismiss();
                                    }
                                    return false;
                                });
                        messageDialog.setOkTextInfo(new TextInfo().setFontColor(Color.parseColor("#000000")).setBold(true));
                        messageDialog.setCancelTextInfo(new TextInfo().setFontColor(Color.parseColor("#000000")).setBold(true));
                    }
                }
            }
        } else {
            recordStream();
        }
    }
    void recordStream() {
        if (mActivityRef != null && mActivityRef.get() != null && !mActivityRef.get().isWifiRecording()) {
            mActivityRef.get().startRecordStream();
        } else if (mActivityRef != null && mActivityRef.get() != null) {
            mActivityRef.get().stopRecordStream();
        }
    }

    void onSnapshot() {
        if (is_camera_opened) {
            if (mActivityRef != null && mActivityRef.get() != null && mActivityRef.get().mCamService != null) {
                mListener.fragTakeSnapshot();
            }
        } else if (is_usb_opened) {
            if (mActivityRef != null && mActivityRef.get() != null && mActivityRef.get().mUSBService != null && !mActivityRef.get().mUSBService.isConnected) {
                mListener.fragTakeSnapshot();
                Toast.makeText(mActivityRef.get(), "Please attach camera first", Toast.LENGTH_SHORT).show();
                return;
            }
            mListener.fragTakeSnapshot();
        } else if (is_audio_only) {
            mListener.fragTakeSnapshot();
        } else if (is_cast_opened) {
            mListener.fragTakeSnapshot();
        } else {
            mListener.fragWifiSnapshot();
        }
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    public void updateLocation() {
        if (mActivityRef != null && mActivityRef.get() != null) {
            MainActivity activity = mActivityRef.get();
            boolean isLocationEnabled = CommonUtil.isLocationEnabled();
            boolean isGpsEnabled = AppPreference.getBool(AppPreference.KEY.GPS_ENABLED, false);

            if (isGpsEnabled) {
                if (!isLocationEnabled) {
                    txt_gps.setText("GPS:ON - Enable location from settings");
                    // Show dialog to enable location
                    new AlertDialog.Builder(activity)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.enable_location_message)
                        .setPositiveButton(R.string.OK, (dialog, which) -> {
                            dialog.dismiss();
                            activity.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        })
                        .setNegativeButton(R.string.CANCEL, (dialog, which) -> dialog.dismiss())
                        .show();
                } else {
                    if (!LocationManagerService.isRunning) {
                        activity.startLocationService();
                    }
                    if (LocationManagerService.lat != 0.0 && LocationManagerService.lng != 0.0) {
                        String gpsText = "GPS:ON Lat/Long:" + String.format("(%.6f, %.6f)", LocationManagerService.lat, LocationManagerService.lng);
                        txt_gps.setText(gpsText);
                        updateDeviceInfo();
                    } else {
                        txt_gps.setText("GPS:ON - Waiting for location...");
                    }
                }
            } else {
                txt_gps.setText("GPS:OFF");
                if (LocationManagerService.isRunning) {
                    activity.stopLocationService();
                }
                updateDeviceInfo();
            }
        }
    }

    public void updateNetwork() {
        setNetworkText(m_wifi_in, m_wifi_out);
    }

    private boolean lastStreamingState = false;
    private long lastApiUpdateTime = 0;

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

    public void enableRecordingSettings(boolean visible) {
        SettingsFragment.instance.get().hideSettings(visible);
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
}

