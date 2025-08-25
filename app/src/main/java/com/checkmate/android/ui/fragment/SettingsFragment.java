package com.checkmate.android.ui.fragment;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import android.os.FileUtils;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.ListPreference;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethod;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.checkmate.android.service.MyAccessibilityService;
import com.checkmate.android.util.OnStoragePathChangeListener;
import com.checkmate.android.viewmodels.EventType;
import com.checkmate.android.viewmodels.SharedViewModel;
import com.kongzue.dialogx.dialogs.MessageDialog;
import com.kongzue.dialogx.interfaces.OnDialogButtonClickListener;
import com.kongzue.dialogx.util.TextInfo;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.checkmate.android.util.ActionSheet;
import com.blikoon.qrcodescanner.QrCodeActivity;
import com.checkmate.android.AppConstant;
import com.checkmate.android.database.DBManager;
import com.checkmate.android.networking.HttpApiService;
import com.checkmate.android.networking.Responses;
import com.checkmate.android.AppPreference;
import com.checkmate.android.R;
import com.checkmate.android.model.Camera;
import com.checkmate.android.networking.RestApiService;
import com.checkmate.android.service.LocationManagerService;
import com.checkmate.android.ui.activity.SplashActivity;
import com.checkmate.android.ui.dialog.CameraDialog;
import com.checkmate.android.ui.dialog.EncryptionDialog;
import com.checkmate.android.ui.dialog.GeneralDialog;
import com.checkmate.android.ui.dialog.TextProgressDialog;
import com.checkmate.android.ui.dialog.TranscodeDialog;
import com.checkmate.android.ui.dialog.WifiDialog;
import com.checkmate.android.ui.dialog.WifiListDialog;
import com.checkmate.android.ui.dialog.WifiPasswordDialog;
import com.checkmate.android.ui.view.MySpinner;
import com.checkmate.android.util.CameraInfo;
import com.checkmate.android.util.CommonUtil;
import com.checkmate.android.util.DeviceUtils;
import com.checkmate.android.util.MainActivity;
import com.checkmate.android.util.MessageUtil;
import com.checkmate.android.util.ResourceUtil;
import com.checkmate.android.util.SettingsUtils;
import com.codekidlabs.storagechooser.StorageChooser;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.UVCCamera;
import com.shasin.notificationbanner.Banner;
import com.thanosfisherman.wifiutils.WifiUtils;
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionErrorCode;
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionSuccessListener;
import com.wmspanel.libstream.Streamer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.ACCESSIBILITY_SERVICE;
import static com.checkmate.android.util.ResourceUtil.getRecordPath;
import static com.checkmate.android.util.ResourceUtil.getSdCardPath;

// ANR and Thread Safety imports
import com.checkmate.android.util.InternalLogger;
import com.checkmate.android.util.ANRSafeHelper;
import com.checkmate.android.util.CriticalComponentsMonitor;

public class SettingsFragment extends BaseFragment implements OnStoragePathChangeListener {

    private  ActivityFragmentCallbacks mListener;
    //    public static SettingsFragment instance;
    public static WeakReference<SettingsFragment> instance;
    MainActivity mActivity;
    List<Camera> cameraList = new ArrayList<>();
    ListAdapter adapter;
    public static final int REQUEST_CODE_QR_SCAN_WIFI = 10001;

    ListView list_cameras;

    TextView txt_video_details;

    TextView tv_expiration_date;

    TextView txt_stream_details;

    Switch swt_adaptive_fps;

    Switch swt_radio_mode;

    ViewGroup ly_recording_settings;

    ViewGroup ly_streaming_settings;

    Switch swt_radio_bluetooth;

    Switch usb_radio_bluetooth;

    Switch swt_orientation;

    TextView txt_location;

    TextView txt_space;

    TextView txt_update;

    TextView txt_check_update;

    Switch swt_convert_ui;

    Switch swt_rec_audio;

    Switch swt_auto_record;

    Switch swt_fifo;

    TextView txt_version;

    EditText edt_pin;
    
    // Additional UI Components that were missing
    private androidx.cardview.widget.CardView card_beta;
    private EditText edt_cloud;
    private TextView txt_beta_update;
    private TextView txt_wifi_camera;
    // TextView txt_acc_status; // Removed - element not in layout

    TextView txt_new_version;

    Spinner spinner_resolution;

    Spinner streaming_audio_bitrate;

    Spinner usb_audio_bitrate;

    MySpinner spinner_audio_src;

    MySpinner usb_audio_src;

    MySpinner usb_channel_count;

    MySpinner bluetooth_audio_src;

    MySpinner usb_bluetooth_src;

    MySpinner spinner_sample_rate;

    MySpinner usb_sample_rate;

    TextView txt_machine;

    Spinner spinner_frame;

    MySpinner streaming_quality;

    Spinner streaming_resolution;

    Spinner streaming_frame;

    MySpinner spinner_adaptive;



    ViewGroup ly_streaming_custom;

    EditText edt_streaming_bitrate;

    EditText edt_streaming_keyFrame;

    EditText edt_bitrate;

    EditText edt_keyFrame;

    Switch swt_secure_multi;

    TextView txt_storage;

    TextView tv_storage_location;

    LinearLayout ly_video_custom;

    Spinner spinner_quality;

    TextView txt_serial;

    TextView txt_expire;

    LinearLayout ly_expire;

    EditText edt_split;

    Switch swt_key_service;

    Switch swt_timestamp;

    Switch swt_vu_meter;

    Switch swt_transcode;

    Switch swt_radio_accessbility;

    TextView txt_transcode;

    ViewGroup ly_vu_meter;

    ViewGroup ly_audio;

    public Switch swt_encryption;

    // Remove duplicate declarations

    ViewGroup ly_usb;

    LinearLayout usb_path_ll;

    TextView txt_usb_cam;
    TextView txt_camera;
    TextView txt_reactivate;
    TextView txt_exit;
    TextView txt_cast_video_details;

    MySpinner spinner_usb_codec;

    MySpinner spinner_usb_resolution;

    ViewGroup ly_cast_video_settings;

    Spinner spinner_cast_resolution;

    Spinner spinner_cast_frame;

    EditText edt_cast_bitrate;

    EditText edt_cast_keyFrame;

    EditText edt_usb_min_fps;

    EditText edt_usb_max_fps;

    MySpinner audio_src;

    MySpinner audio_pref_mic;

    MySpinner audio_option_bitrate;

    MySpinner audio_option_sample_rate;

    MySpinner audio_option_channel_count;

    private static final int REQUEST_CODE_OPEN_DOCUMENT_TREE = 1;
    private static final int REQUEST_CODE_MOVE_FILES = 11;
    private static final int REQUEST_CODE_SAVE_FILE = 22;
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 100;
    private Uri sourceFileUri;
    private Uri destinationFileUri;

    private SharedViewModel sharedViewModel;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
    @Override
    public void onAttach(Context context) {
        CriticalComponentsMonitor.executeComponentSafely("SettingsFragment.onAttach", () -> {
            try {
                super.onAttach(context);
                instance = new WeakReference<>(this);
                mActivity = ANRSafeHelper.nullSafe(MainActivity.getInstance(), null, "MainActivity.getInstance()");
                
                if (ANRSafeHelper.isNullWithLog(context, "context")) {
                    InternalLogger.e("SettingsFragment", "Context is null in onAttach");
                }
                
                if (context instanceof ActivityFragmentCallbacks) {
                    mListener = (ActivityFragmentCallbacks) context;
                } else {
                    InternalLogger.e("SettingsFragment", context.toString() + " must implement OnFragmentInteractionListener");
                    throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
                }
                
                InternalLogger.d("SettingsFragment", "onAttach completed successfully");
                
                
            } catch (Exception e) {
                InternalLogger.e("SettingsFragment", "Error in onAttach", e);
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }

    int default_size_index = 1;
    int record_size_index = 1;
    boolean isUSBOpen = false;
    private static final String tag = "SettingsFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize UI components
        list_cameras = mView.findViewById(R.id.list_cameras);
        txt_video_details = mView.findViewById(R.id.txt_video_details);
        tv_expiration_date = mView.findViewById(R.id.tv_expiration_date);
        txt_stream_details = mView.findViewById(R.id.txt_stream_details);
        swt_adaptive_fps = mView.findViewById(R.id.swt_adaptive_fps);
        swt_radio_mode = mView.findViewById(R.id.swt_radio_mode);
        ly_recording_settings = mView.findViewById(R.id.ly_recording_settings);
        ly_streaming_settings = mView.findViewById(R.id.ly_streaming_settings);
        swt_radio_bluetooth = mView.findViewById(R.id.swt_radio_bluetooth);
        usb_radio_bluetooth = mView.findViewById(R.id.usb_radio_bluetooth);
        swt_orientation = mView.findViewById(R.id.swt_orientation);
        txt_location = mView.findViewById(R.id.txt_location);
        txt_space = mView.findViewById(R.id.txt_space);
        txt_update = mView.findViewById(R.id.txt_update);
        txt_check_update = mView.findViewById(R.id.txt_check_update);
        swt_convert_ui = mView.findViewById(R.id.swt_convert_ui);
        swt_rec_audio = mView.findViewById(R.id.swt_rec_audio);
        swt_auto_record = mView.findViewById(R.id.swt_auto_record);
        swt_fifo = mView.findViewById(R.id.swt_fifo);
        txt_version = mView.findViewById(R.id.txt_version);
        edt_pin = mView.findViewById(R.id.edt_pin);
        txt_new_version = mView.findViewById(R.id.txt_new_version);
        spinner_resolution = mView.findViewById(R.id.spinner_resolution);
        streaming_audio_bitrate = mView.findViewById(R.id.streaming_audio_bitrate);
        usb_audio_bitrate = mView.findViewById(R.id.usb_audio_bitrate);
        spinner_audio_src = mView.findViewById(R.id.spinner_audio_src);
        usb_audio_src = mView.findViewById(R.id.usb_audio_src);
        usb_channel_count = mView.findViewById(R.id.usb_channel_count);
        bluetooth_audio_src = mView.findViewById(R.id.bluetooth_audio_src);
        usb_bluetooth_src = mView.findViewById(R.id.usb_bluetooth_src);
        spinner_sample_rate = mView.findViewById(R.id.spinner_sample_rate);
        usb_sample_rate = mView.findViewById(R.id.usb_sample_rate);
        txt_machine = mView.findViewById(R.id.txt_machine);
        spinner_frame = mView.findViewById(R.id.spinner_frame);
        streaming_quality = mView.findViewById(R.id.streaming_quality);
        streaming_resolution = mView.findViewById(R.id.streaming_resolution);
        streaming_frame = mView.findViewById(R.id.streaming_frame);
        spinner_adaptive = mView.findViewById(R.id.spinner_adaptive);

        ly_streaming_custom = mView.findViewById(R.id.ly_streaming_custom);
        edt_streaming_bitrate = mView.findViewById(R.id.edt_streaming_bitrate);
        edt_streaming_keyFrame = mView.findViewById(R.id.edt_streaming_keyFrame);
        edt_bitrate = mView.findViewById(R.id.edt_bitrate);
        edt_keyFrame = mView.findViewById(R.id.edt_keyFrame);
        swt_secure_multi = mView.findViewById(R.id.swt_secure_multi);
        swt_encryption = mView.findViewById(R.id.swt_encryption);
        txt_storage = mView.findViewById(R.id.txt_storage);
        tv_storage_location = mView.findViewById(R.id.tv_storage_location);
        ly_video_custom = mView.findViewById(R.id.ly_video_custom);
        spinner_quality = mView.findViewById(R.id.spinner_quality);
        txt_serial = mView.findViewById(R.id.txt_serial);
        txt_expire = mView.findViewById(R.id.txt_expire);
        ly_expire = mView.findViewById(R.id.ly_expire);
        edt_split = mView.findViewById(R.id.edt_split);
        swt_key_service = mView.findViewById(R.id.swt_key_service);
        swt_timestamp = mView.findViewById(R.id.swt_timestamp);
        swt_vu_meter = mView.findViewById(R.id.swt_vu_meter);
        swt_transcode = mView.findViewById(R.id.swt_transcode);
        swt_radio_accessbility = mView.findViewById(R.id.swt_radio_accessbility);
        txt_transcode = mView.findViewById(R.id.txt_transcode);
        ly_vu_meter = mView.findViewById(R.id.ly_vu_meter);
        ly_audio = mView.findViewById(R.id.ly_audio);
        card_beta = mView.findViewById(R.id.card_beta);
        edt_cloud = mView.findViewById(R.id.edt_cloud);
        txt_beta_update = mView.findViewById(R.id.txt_beta_update);
        ly_usb = mView.findViewById(R.id.ly_usb);
        usb_path_ll = mView.findViewById(R.id.usb_path_ll);
        txt_usb_cam = mView.findViewById(R.id.txt_usb_cam);
        spinner_usb_codec = mView.findViewById(R.id.spinner_usb_codec);
        spinner_usb_resolution = mView.findViewById(R.id.spinner_usb_resolution);
        ly_cast_video_settings = mView.findViewById(R.id.ly_cast_video_settings);
        spinner_cast_resolution = mView.findViewById(R.id.spinner_cast_resolution);
        spinner_cast_frame = mView.findViewById(R.id.spinner_cast_frame);
        edt_cast_bitrate = mView.findViewById(R.id.edt_cast_bitrate);
        edt_cast_keyFrame = mView.findViewById(R.id.edt_cast_keyFrame);
        edt_usb_min_fps = mView.findViewById(R.id.edt_usb_min_fps);
        edt_usb_max_fps = mView.findViewById(R.id.edt_usb_max_fps);
        audio_src = mView.findViewById(R.id.audio_src);
        audio_pref_mic = mView.findViewById(R.id.audio_pref_mic);
        audio_option_bitrate = mView.findViewById(R.id.audio_option_bitrate);
        audio_option_sample_rate = mView.findViewById(R.id.audio_option_sample_rate);
        audio_option_channel_count = mView.findViewById(R.id.audio_option_channel_count);
        txt_camera = mView.findViewById(R.id.txt_camera);
        txt_reactivate = mView.findViewById(R.id.txt_reactivate);
        txt_exit = mView.findViewById(R.id.txt_exit);
        txt_cast_video_details = mView.findViewById(R.id.txt_cast_video_details);

        // Remove invalid findViewById calls for non-existent UI elements

        // Set up click listeners for buttons (with null checks)
        if (txt_stream_details != null) txt_stream_details.setOnClickListener(this);
        if (txt_video_details != null) txt_video_details.setOnClickListener(this);
        if (txt_storage != null) txt_storage.setOnClickListener(this);
        if (txt_transcode != null) txt_transcode.setOnClickListener(this);
        if (txt_update != null) txt_update.setOnClickListener(this);
        if (txt_check_update != null) txt_check_update.setOnClickListener(this);
        if (txt_wifi_camera != null) txt_wifi_camera.setOnClickListener(this);
        if (txt_beta_update != null) txt_beta_update.setOnClickListener(this);
        if (txt_camera != null) txt_camera.setOnClickListener(this);
        if (txt_reactivate != null) txt_reactivate.setOnClickListener(this);
        if (txt_exit != null) txt_exit.setOnClickListener(this);
        if (txt_cast_video_details != null) txt_cast_video_details.setOnClickListener(this);

        // Initialize default values and field states
        initializeDefaultValues();

        // Initialize the SharedViewModel
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // Observe LiveData
        sharedViewModel.getEventLiveData().observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                SharedViewModel.EventPayload payload = event.getContentIfNotHandled();
                if (payload != null) {
                    handleEvent(payload);
                }
            }
        });
        if (TextUtils.equals(AppPreference.getStr(AppPreference.KEY.ACTIVATION_SERIAL, ""), AppConstant.BETA_SERIAL)) {
            card_beta.setVisibility(View.VISIBLE);
        } else {
            card_beta.setVisibility(View.GONE);
        }

        tv_expiration_date.setText(convertDateToUSFormat(AppPreference.getStr(AppPreference.KEY.EXPIRY_DATE, "")));

        return mView;
    }
    public String convertDateToUSFormat(String dateString) {
        try {
            // Define the input date format
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", Locale.US);

            // Parse the date string into a Date object
            Date date = inputFormat.parse(dateString);

            // Define the output date format (US format)
            SimpleDateFormat outputFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a", Locale.US);

            // Format the Date object into the desired output format
            return outputFormat.format(date);

        } catch (ParseException e) {
            // Handle parsing errors
            System.err.println("Error parsing date: " + e.getMessage());
            return "N/A"; // or throw an exception
        }
    }
    public void initCamerasList() {
        cameraList = new ArrayList<>();
        adapter = new ListAdapter(getContext());
        list_cameras.setAdapter(adapter);
        cameraList.add(new Camera(Camera.TYPE.FRONT_CAMERA, getString(R.string.front_facing), AppPreference.getBool(AppPreference.KEY.CAM_FRONT_FACING, true)));
        cameraList.add(new Camera(Camera.TYPE.REAR_CAMERA, getString(R.string.rear_facing), AppPreference.getBool(AppPreference.KEY.CAM_REAR_FACING, true)));
        cameraList.add(new Camera(Camera.TYPE.USB_CAMERA, getString(R.string.usb_camera), AppPreference.getBool(AppPreference.KEY.CAM_USB, false)));
        cameraList.add(new Camera(Camera.TYPE.SCREEN_CAST, getString(R.string.screen_cast), AppPreference.getBool(AppPreference.KEY.CAM_CAST, false)));
        cameraList.add(new Camera(Camera.TYPE.AUDIO_ONLY, getString(R.string.audio_only_text), AppPreference.getBool(AppPreference.KEY.AUDIO_ONLY, false)));

        List<Camera> db_cams = DBManager.getInstance().getCameras();
        for (Camera item : db_cams) {
            Camera camera = new Camera(Camera.TYPE.WIFI_CAMERA, getString(R.string.wifi_camera), false);
            camera.camera_name = item.camera_name;
            camera.url = item.url;
            camera.id = item.id;
            camera.username = item.username;
            camera.password = item.password;
            camera.uri = item.uri;
            camera.port = item.port;
            camera.wifi_ssid = item.wifi_ssid;
            camera.rtsp_type = item.rtsp_type;
            camera.wifi_in = item.wifi_in;
            camera.wifi_out = item.wifi_out;
            camera.use_full_address = item.use_full_address;
            cameraList.add(camera);
        }
        adapter.notifyDataSetChanged();
        CommonUtil.setListViewHeightBasedOnItems(list_cameras);

        if (db_cams.size() == 0) {
            swt_transcode.setChecked(false);
            txt_transcode.setEnabled(false);
            AppPreference.setBool(AppPreference.KEY.TRANS_APPLY_SETTINGS, false);
        }
    }
    private void handleEvent(SharedViewModel.EventPayload payload) {
        EventType eventType = payload.getEventType();
        Object data = payload.getData();

        switch (eventType) {
            case INIT_FUN_SETTING: {
                initialize();
            }
            break;

            case STORAGE_PATH_SETTING:
                onStoragePathChanged(data.toString());
                break;
        }
    }

    public String[] camera_sizes;
    public String[] record_sizes;
    public String[] all_sizes;
    // Predefined streaming resolutions (fixed widths)
    private static final int[] STREAMING_WIDTHS = {1280, 960 , 720, 640, 320, 160};
    // Function to calculate aspect ratio in "X:Y" format
    public static String getAspectRatio(int width, int height) {
        int gcd = gcd(width, height);
        return (width / gcd) + ":" + (height / gcd);
    }

    // Helper function to find Greatest Common Divisor (GCD)
    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    // Function to filter resolutions based on 16:9 and 4:3 aspect ratios
    public static String[] filterResolutions(String[] record_sizes) {
        Set<String> filteredSet = new HashSet<>();
        for (String size : record_sizes) {
            String[] parts = size.split("x");
            if (parts.length != 2) continue; // Skip invalid formats
            int width = Integer.parseInt(parts[0]);
            int height = Integer.parseInt(parts[1]);
            // Calculate the aspect ratio
            String aspectRatio = getAspectRatio(width, height);
            // Keep only 16:9 or 4:3 resolutions
            if (aspectRatio.equals("16:9") || aspectRatio.equals("4:3")) {
                filteredSet.add(size);
            }
        }
        // Convert HashSet to List for sorting
        List<String> sortedResolutions = new ArrayList<>(filteredSet);
        // Sort by width (descending) then by height (descending)
        sortedResolutions.sort((a, b) -> {
            String[] aParts = a.split("x");
            String[] bParts = b.split("x");
            int aWidth = Integer.parseInt(aParts[0]);
            int aHeight = Integer.parseInt(aParts[1]);
            int bWidth = Integer.parseInt(bParts[0]);
            int bHeight = Integer.parseInt(bParts[1]);
            // Sort by width first, then by height if widths are equal
            if (bWidth != aWidth) {
                return Integer.compare(bWidth, aWidth);
            } else {
                return Integer.compare(bHeight, aHeight);
            }
        });

        return sortedResolutions.toArray(new String[0]);
    }
    // Function to scale resolution while maintaining aspect ratio
    public String scaleResolution(int originalWidth, int originalHeight, int targetWidth) {
        double aspectRatio = (double) originalWidth / originalHeight;
        int targetHeight = (int) Math.round(targetWidth / aspectRatio);
        return targetWidth + "x" + targetHeight;
    }
    // Function to get the best streaming resolution based on recording resolution
    public String[] getStreamingResolutions(String[] record_sizes) {
        Set<String> uniqueStreamingSet = new HashSet<>();
        for (String res : record_sizes) {
            String[] parts = res.split("x");
            int recordingWidth = Integer.parseInt(parts[0]);
            int recordingHeight = Integer.parseInt(parts[1]);
            for (int width : STREAMING_WIDTHS) {
                uniqueStreamingSet.add(scaleResolution(recordingWidth, recordingHeight, width));
            }
        }
        // Convert HashSet to List for sorting
        List<String> sortedResolutions = new ArrayList<>(uniqueStreamingSet);
        // Sort by width (descending) then by height (descending)
        sortedResolutions.sort((a, b) -> {
            String[] aParts = a.split("x");
            String[] bParts = b.split("x");
            int aWidth = Integer.parseInt(aParts[0]);
            int aHeight = Integer.parseInt(aParts[1]);
            int bWidth = Integer.parseInt(bParts[0]);
            int bHeight = Integer.parseInt(bParts[1]);
            // Sort by width first, then by height if widths are equal
            if (bWidth != aWidth) {
                return Integer.compare(bWidth, aWidth);
            } else {
                return Integer.compare(bHeight, aHeight);
            }
        });

        return sortedResolutions.toArray(new String[0]);
    }

    public int getBestStreamingIndex(String record_size, String[] camera_sizes) {
        int index = 1;
        String[] parts = record_size.split("x");
        int recordingWidth = Integer.parseInt(parts[0]);
        int recordingHeight = Integer.parseInt(parts[1]);

        // Generate possible streaming resolutions
        List<String> streamingResolutions = new ArrayList<>();
        for (int width : STREAMING_WIDTHS) {
            streamingResolutions.add(scaleResolution(recordingWidth, recordingHeight, width));
        }

        // Find the highest matching resolution in camera_sizes
        int matchedIndex = -1;
        for (int i = 0; i < camera_sizes.length; i++) {
            if (streamingResolutions.contains(camera_sizes[i])) {
                matchedIndex = i;
                break; // Found the highest available match
            }
        }

        // If a match is found, return the next lower index if available
        if (matchedIndex != -1 && matchedIndex + 1 < camera_sizes.length) {
       //     setStreamQuality(matchedIndex + 1);
            return matchedIndex + 1;
        }

        // If no lower resolution is found, return the last available resolution
     //   setStreamQuality(camera_sizes.length - 1);
        return camera_sizes.length - 1;
    }

    void initVideos(CameraInfo cameraInfo) {
        if (cameraInfo == null || cameraInfo.recordSizes == null) {
            // Optionally log or handle error
            return;
        }
        record_sizes = new String[cameraInfo.recordSizes.length];
        for (int i = 0; i < cameraInfo.recordSizes.length; i++) {
            record_sizes[i] = cameraInfo.recordSizes[i].toString();
        }
        record_sizes = filterResolutions(record_sizes);
        camera_sizes = new String[cameraInfo.recordSizes.length];
        camera_sizes = getStreamingResolutions(record_sizes);
        all_sizes = record_sizes;
    }

    @SuppressLint("SetTextI18n")
    public void initialize() {
        if (mActivity == null) {
            return;
        }
        isUSBOpen = AppPreference.getBool(AppPreference.KEY.IS_USB_OPENED, false);
        CameraInfo cameraInfo = mActivity.findCameraInfo();
        initVideos(cameraInfo);
        default_size_index = Arrays.asList(camera_sizes).indexOf("960x540");
        record_size_index = Arrays.asList(record_sizes).indexOf("1920x1080");
        initCamerasList();
        swt_convert_ui.setChecked(AppPreference.getBool(AppPreference.KEY.UI_CONVERT_MODE, false));
        edt_pin.setEnabled(swt_convert_ui.isChecked());
        swt_convert_ui.setOnCheckedChangeListener((compoundButton, b) -> {
            AppPreference.setBool(AppPreference.KEY.UI_CONVERT_MODE, b);
            edt_pin.setEnabled(b);
        });
        swt_orientation.setChecked(AppPreference.getBool(AppPreference.KEY.ORIENTATION_LOCK, false));
        swt_orientation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppPreference.setBool(AppPreference.KEY.ORIENTATION_LOCK, isChecked);
            mListener.fragLockOrientation();
        });

        swt_timestamp.setChecked(AppPreference.getBool(AppPreference.KEY.TIMESTAMP, true));
        swt_timestamp.setOnCheckedChangeListener((compoundButton, b) -> {
            AppPreference.setBool(AppPreference.KEY.TIMESTAMP, b);
        });

        swt_vu_meter.setChecked(AppPreference.getBool(AppPreference.KEY.VU_METER, true));
        swt_vu_meter.setOnCheckedChangeListener((compoundButton, b) -> {
            AppPreference.setBool(AppPreference.KEY.VU_METER, b);
            mListener.fragCameraRestart(true);
        });

        swt_transcode.setChecked(AppPreference.getBool(AppPreference.KEY.TRANS_APPLY_SETTINGS, false));
        txt_transcode.setEnabled(swt_transcode.isChecked());
        swt_transcode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppPreference.setBool(AppPreference.KEY.TRANS_APPLY_SETTINGS, isChecked);
            txt_transcode.setEnabled(isChecked);
            mListener.fragCameraRestart(true);
        });
        boolean isEnabled = isAccessibilityServiceEnabled(getContext(), MyAccessibilityService.class);
        // Accessibility status display removed - element not in layout
        // if (isEnabled) {
        //     txt_acc_status.setText("On");
        //     txt_acc_status.setTextColor(ContextCompat.getColor(getContext(), R.color.black));
        // }else {
        //     txt_acc_status.setText("Off");
        //     txt_acc_status.setTextColor(ContextCompat.getColor(getContext(), R.color.RED));
        // }
        swt_radio_accessbility.setChecked(isEnabled);
        swt_radio_accessbility.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            boolean isEnabledNew = isAccessibilityServiceEnabled(getContext(), MyAccessibilityService.class);
            swt_radio_accessbility.setChecked(isChecked);
            // Accessibility status display removed - element not in layout
            // if (isChecked) {
            //     txt_acc_status.setText("On");
            //     txt_acc_status.setTextColor(ContextCompat.getColor(getContext(), R.color.black));
            // }else {
            //     txt_acc_status.setText("Off");
            //     txt_acc_status.setTextColor(ContextCompat.getColor(getContext(), R.color.RED));
            // }
        });

        String storageType = AppPreference.getStr(AppPreference.KEY.Storage_Type, "Storage Location: Default Storage");
        tv_storage_location.setText(storageType);

        if (swt_encryption != null) {
            swt_encryption.setChecked(AppPreference.getBool(AppPreference.KEY.FILE_ENCRYPTION, false));
            swt_encryption.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                enterEncryptionKey();
            } else {
                MessageUtil.showToast(getContext(), "The previously encrypted files will still be encrypted.");
                AppPreference.setBool(AppPreference.KEY.FILE_ENCRYPTION, false);
            }
        });
        }

        if (!AppPreference.getBool(AppPreference.KEY.USE_AUDIO, false)) {
            AppPreference.setBool(AppPreference.KEY.RECORD_AUDIO, false);
            ly_audio.setVisibility(View.GONE);
            ly_vu_meter.setVisibility(View.GONE);
        } else {
            ly_audio.setVisibility(View.VISIBLE);
            ly_vu_meter.setVisibility(View.VISIBLE);
        }
        boolean record_audio = AppPreference.getBool(AppPreference.KEY.RECORD_AUDIO, false);
        swt_rec_audio.setChecked(record_audio);
        swt_vu_meter.setEnabled(record_audio);
        if (!record_audio) {
            swt_vu_meter.setChecked(false);
        }
        swt_rec_audio.setOnClickListener(v -> {
            boolean is_checked = swt_rec_audio.isChecked();
            swt_vu_meter.setEnabled(is_checked);
            swt_vu_meter.setChecked(is_checked);
            AppPreference.setBool(AppPreference.KEY.RECORD_AUDIO, is_checked);
            mListener.fragCameraRestart(true);
        });

        swt_key_service.setChecked(AppPreference.getBool(AppPreference.KEY.VOLUME_KEY, false));
        swt_key_service.setOnCheckedChangeListener((compoundButton, b) -> {
            AppPreference.setBool(AppPreference.KEY.VOLUME_KEY, b);
            if (b) {
                mListener.isDialog(true);
                MessageUtil.showAlertDialog(getContext(), MessageUtil.TYPE_WARNING, getString(R.string.battery_warning), (dialogInterface, i) -> mListener.fragStartVolumeService(), (dialogInterface, i) -> {
                    AppPreference.setBool(AppPreference.KEY.VOLUME_KEY, false);
                    mListener.fragStopVolumeService();
                    swt_key_service.setChecked(false);
                    mListener.isDialog(false);
                });
            } else {
                mListener.fragStopVolumeService();
            }
        });

        swt_auto_record.setChecked(AppPreference.getBool(AppPreference.KEY.AUTO_RECORD, false));
        swt_auto_record.setOnCheckedChangeListener((compoundButton, b) ->
                AppPreference.setBool(AppPreference.KEY.AUTO_RECORD, b)
        );

        swt_fifo.setChecked(AppPreference.getBool(AppPreference.KEY.FIFO, true));
        swt_fifo.setOnCheckedChangeListener((compoundButton, b) ->
                AppPreference.setBool(AppPreference.KEY.FIFO, b)
        );

        swt_secure_multi.setChecked(AppPreference.getBool(AppPreference.KEY.SECURE_MULTI_TASK, true));
        swt_secure_multi.setOnCheckedChangeListener((compoundButton, b) -> {
            AppPreference.setBool(AppPreference.KEY.SECURE_MULTI_TASK, b);
        });

        String version = CommonUtil.getVersionCode(getContext());
        txt_version.setText(version);
        if (!TextUtils.isEmpty(AppPreference.getStr(AppPreference.KEY.APP_VERSION, ""))) {
            txt_new_version.setText(AppPreference.getStr(AppPreference.KEY.APP_VERSION, ""));
            txt_update.setVisibility(View.VISIBLE);
            txt_check_update.setVisibility(View.GONE);
        } else {
            txt_new_version.setText(version);
            txt_update.setVisibility(View.GONE);
            txt_check_update.setVisibility(View.VISIBLE);
        }

        try {
            txt_space.setText(String.format("%s available of %s", ResourceUtil.getAvailableInternalMemorySize(), ResourceUtil.getTotalInternalMemorySize()) + "\nStop recording when 10% Available");
            String storage_location = AppPreference.getStr(AppPreference.KEY.STORAGE_LOCATION, getRecordPath());
            String storagePath = getFullPathFromTreeUri(Uri.parse(storage_location));
            if (storagePath != null) {
                if (storagePath.isEmpty()) {
                    txt_storage.setText("Select Storage Location");
                }else {
                    txt_storage.setText(storagePath);
                }
            }else {
                txt_storage.setText("Select Storage");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (isUSBOpen) {
            if (sharedViewModel != null && sharedViewModel.getCameraResolution() != null) {
                if (!sharedViewModel.getCameraResolution().isEmpty()) {
                    record_sizes = sharedViewModel.getCameraResolution().toArray(new String[0]);
                    int recording_position = AppPreference.getInt(AppPreference.KEY.VIDEO_QUALITY, 0);
                    if (recording_position != 5) {
                        record_size_index = Arrays.asList(record_sizes).indexOf("1920x1080");
                        if (record_size_index == -1) {
                            record_size_index = 0;
                        }
                    }
                }else {
                    record_sizes = all_sizes;
                }
            } else {
                record_sizes = all_sizes;
            }
        }
        if (record_sizes.length < record_size_index) {
            record_size_index = record_sizes.length - 1;
        }

        if (isUSBOpen) {
            if (sharedViewModel != null && sharedViewModel.getCameraResolution() != null) {
                camera_sizes = sharedViewModel.getCameraResolution().toArray(new String[0]);
                int recording_position = AppPreference.getInt(AppPreference.KEY.VIDEO_QUALITY, 0);
                if (recording_position != 5) {
                    default_size_index = Arrays.asList(camera_sizes).indexOf("1280x720");
                    if (default_size_index == -1) {
                        default_size_index = 1;
                    }
                }
            } else {
                camera_sizes = all_sizes;
            }
        }
        if (camera_sizes.length < default_size_index) {
            default_size_index = camera_sizes.length - 1;
        }
        final int numSizes = record_sizes.length;
        int resolution = AppPreference.getInt(AppPreference.KEY.VIDEO_RESOLUTION, record_size_index);
        if (resolution < 0 ) {
            resolution = record_size_index;
            AppPreference.setInt(AppPreference.KEY.VIDEO_RESOLUTION, record_size_index);
        }
        if (resolution >= numSizes) {
            resolution = record_size_index;
            AppPreference.setInt(AppPreference.KEY.VIDEO_RESOLUTION, record_size_index);
        }
        int resolution_streaming = AppPreference.getInt(AppPreference.KEY.STREAMING_RESOLUTION, default_size_index);
        if (resolution_streaming < 0 || resolution_streaming > camera_sizes.length) {
            resolution_streaming = default_size_index;
            AppPreference.setInt(AppPreference.KEY.STREAMING_RESOLUTION, default_size_index);
        }

        List<String> spinnerArray = Arrays.asList(record_sizes);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(
                requireActivity(), android.R.layout.simple_spinner_item, spinnerArray);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        spinner_resolution.setAdapter(spinnerArrayAdapter);
        spinner_resolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                int index = AppPreference.getInt(AppPreference.KEY.VIDEO_QUALITY, 0);
                if (index != AppConstant.QUALITY_CUSTOM) {
                    return;
                }
                String size = record_sizes[position];
                if (!SettingsUtils.verifyResolution(size)) {
                    mListener.isDialog(true);
                    MessageUtil.showToast(requireContext(), getString(R.string.unsupporting_resolution, size));
                    spinner_resolution.setSelection(position);
                    AppPreference.setInt(AppPreference.KEY.VIDEO_RESOLUTION, position);
                } else if (!TextUtils.equals(size, "1280x720")) {
                    int old_position = AppPreference.getInt(AppPreference.KEY.VIDEO_RESOLUTION, record_size_index);
                    if (old_position != position) {
                        mListener.fragCameraRestart(true);
                    }
                    AppPreference.setInt(AppPreference.KEY.VIDEO_RESOLUTION, position);
                } else {
                    int old_position = AppPreference.getInt(AppPreference.KEY.VIDEO_RESOLUTION, record_size_index);
                    if (old_position != position) {
                        mListener.fragCameraRestart(true);
                    }
                    AppPreference.setInt(AppPreference.KEY.VIDEO_RESOLUTION, position);
                }

//                getBestStreamingIndex(size, camera_sizes);
//                if (isUSBOpen) {
//                    spinner_usb_resolution.setSelection(position);
//                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        spinner_resolution.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
            return true;
            }
            return true;
        });

        List<String> spinnerArrayStreaming = Arrays.asList(camera_sizes);
        ArrayAdapter<String> spinnerArrayAdapterStreaming = new ArrayAdapter<>(
                requireActivity(), android.R.layout.simple_spinner_item, spinnerArrayStreaming);
        spinnerArrayAdapterStreaming.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        streaming_resolution.setAdapter(spinnerArrayAdapterStreaming);
        streaming_resolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                int index = AppPreference.getInt(AppPreference.KEY.STREAMING_QUALITY, 1);
                if (index != AppConstant.QUALITY_CUSTOM) {
                    return;
                }
                String size = camera_sizes[position];
                mListener.isDialog(false);
                if (!SettingsUtils.verifyResolution(size)) {
                    MessageUtil.showToast(requireContext(), getString(R.string.unsupporting_resolution, size));
                    streaming_resolution.setSelection(position);
                    AppPreference.setInt(AppPreference.KEY.STREAMING_RESOLUTION, position);
                } else if (!TextUtils.equals(size, "1280x720")) {
                    int old_position = AppPreference.getInt(AppPreference.KEY.STREAMING_RESOLUTION, default_size_index);
                    if (old_position != position) {
                        mListener.fragCameraRestart(true);
                    }
                    AppPreference.setInt(AppPreference.KEY.STREAMING_RESOLUTION, position);
                } else {
                    int old_position = AppPreference.getInt(AppPreference.KEY.STREAMING_RESOLUTION, default_size_index);
                    if (old_position != position) {
                        mListener.fragCameraRestart(true);
                    }
                    AppPreference.setInt(AppPreference.KEY.STREAMING_RESOLUTION, position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        streaming_resolution.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
            return true;
            }
            return true;
        });
        streaming_resolution.setSelection(AppPreference.getInt(AppPreference.KEY.STREAMING_RESOLUTION, default_size_index));
        if (getActivity() == null) {
            return;
        }

        int cast_resolution = AppPreference.getInt(AppPreference.KEY.CAST_RESOLUTION, 0);
        List<String> cast_array = Arrays.asList(getResources().getStringArray(R.array.screencast_sizes));
        ArrayAdapter<String> castAdapter = new ArrayAdapter<>(
                requireActivity(), android.R.layout.simple_spinner_item, cast_array);
        castAdapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        spinner_cast_resolution.setAdapter(castAdapter);
        spinner_cast_resolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int old_position = AppPreference.getInt(AppPreference.KEY.CAST_RESOLUTION, 0);
                if (old_position != position) {
                    mListener.fragCameraRestart(true);
                }
                AppPreference.setInt(AppPreference.KEY.CAST_RESOLUTION, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinner_cast_resolution.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
            return true;
            }
            return true;
        });
        spinner_cast_resolution.setSelection(cast_resolution);

        List<String> qualityArray = Arrays.asList(getResources().getStringArray(R.array.video_quality));
        ArrayAdapter<String> qualityArrayAdapter = new ArrayAdapter<>(
                getActivity(), android.R.layout.simple_spinner_item, qualityArray);
        qualityArrayAdapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        spinner_quality.setAdapter(qualityArrayAdapter);
        int video_quality = AppPreference.getInt(AppPreference.KEY.VIDEO_QUALITY, 0);
        if (video_quality != AppConstant.QUALITY_CUSTOM) {
            ly_video_custom.setVisibility(View.GONE);
            txt_video_details.setText(R.string.show_details);
        } else {
            ly_video_custom.setVisibility(View.VISIBLE);
            txt_video_details.setText(R.string.return_defaults);
        }
        spinner_quality.setSelection(video_quality);
        spinner_quality.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                int video_quality = AppPreference.getInt(AppPreference.KEY.VIDEO_QUALITY, 0);
                AppPreference.setInt(AppPreference.KEY.VIDEO_QUALITY, position);
                mListener.isDialog(false);
                int index = 0;
                switch (position) {
                    case 0: // high
                        AppPreference.setBool(AppPreference.KEY.IS_NATIVE_RESOLUTION, true);
                        onVideoDetails(false);
                        break;
                    case 1:
                        AppPreference.setBool(AppPreference.KEY.IS_NATIVE_RESOLUTION, true);
                        onVideoDetails(false);
                        break;
                    case 2:
                        AppPreference.setBool(AppPreference.KEY.IS_NATIVE_RESOLUTION, true);
                        onVideoDetails(false);
                        break;
                    case 3:
                        AppPreference.setBool(AppPreference.KEY.IS_NATIVE_RESOLUTION, true);
                        onVideoDetails(false);
                        break;
                    case 4:
                        AppPreference.setBool(AppPreference.KEY.IS_NATIVE_RESOLUTION, true);
                        onVideoDetails(false);
                        break;
                    case 5:
                        AppPreference.setBool(AppPreference.KEY.IS_NATIVE_RESOLUTION, false);
                        onVideoDetails(true);
                        break;
                }
                if (video_quality != position) {
                    mListener.fragCameraRestart(true);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        spinner_quality.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
            return true;
            }
            return true;
        });

        List<String> qualityStreaming = Arrays.asList(getResources().getStringArray(R.array.video_quality));
        ArrayAdapter<String> qualityStreamingAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, qualityStreaming);
        qualityStreamingAdapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);

        int streaming_position = AppPreference.getInt(AppPreference.KEY.STREAMING_QUALITY, 1);
        if (streaming_position == AppConstant.QUALITY_CUSTOM) {
            ly_streaming_custom.setVisibility(View.GONE);
            txt_stream_details.setText(R.string.show_details);
        } else {
            ly_streaming_custom.setVisibility(View.VISIBLE);
            txt_stream_details.setText(R.string.return_defaults);
        }
        streaming_quality.setAdapter(qualityStreamingAdapter);
        streaming_quality.setOnItemSelectedEvenIfUnchangedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int streaming_quality = AppPreference.getInt(AppPreference.KEY.STREAMING_QUALITY, 1);
                AppPreference.setInt(AppPreference.KEY.STREAMING_QUALITY, position);
                mListener.isDialog(false);
                int index = 0;
                switch (position) {
                    case 0: // high
                        AppPreference.setBool(AppPreference.KEY.IS_NATIVE_STREAMING, true);
                        onStreamDetails(false);
                        break;
                    case 1:
                        AppPreference.setBool(AppPreference.KEY.IS_NATIVE_STREAMING, true);
                        onStreamDetails(false);
                        break;
                    case 2:
                        AppPreference.setBool(AppPreference.KEY.IS_NATIVE_STREAMING, true);
                        onStreamDetails(false);
                        break;
                    case 3:
                        AppPreference.setBool(AppPreference.KEY.IS_NATIVE_STREAMING, true);
                        onStreamDetails(false);
                        break;
                    case 4:
                        AppPreference.getBool(AppPreference.KEY.IS_NATIVE_STREAMING, true);
                        onStreamDetails(false);
                        break;
                    case 5:
                        AppPreference.getBool(AppPreference.KEY.IS_NATIVE_STREAMING, false);
                        onStreamDetails(true);
                        break;
                }
                if (streaming_quality != position) {
                    mListener.fragCameraRestart(true);
                    if (position == AppConstant.QUALITY_LOW || position == AppConstant.QUALITY_SUPER_LOW) { // low, super low
                        showFPSWarning("");
                    }
                }
                if (streaming_position != position) {
                    mListener.fragCameraRestart(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        streaming_quality.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
            return true;
            }
            return true;
        });
        streaming_quality.setSelection(streaming_position);

        List<String> adaptive_modes = Arrays.asList(getResources().getStringArray(R.array.adaptive_modes));
        ArrayAdapter<String> adaptiveAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, adaptive_modes);
        adaptiveAdapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);

        int adaptive_mode = AppPreference.getInt(AppPreference.KEY.ADAPTIVE_MODE, 0);
        spinner_adaptive.setAdapter(adaptiveAdapter);
        spinner_adaptive.setOnItemSelectedEvenIfUnchangedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int mode = AppPreference.getInt(AppPreference.KEY.ADAPTIVE_MODE, 1);
                AppPreference.setInt(AppPreference.KEY.ADAPTIVE_MODE, position);
                mListener.isDialog(false);
                if (mode != position) {
                    mListener.fragCameraRestart(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinner_adaptive.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
            return true;
            }
            return true;
        });
        spinner_adaptive.setSelection(adaptive_mode);



        List<String> spinnerFrame = Arrays.asList(getResources().getStringArray(R.array.video_frame));
        ArrayAdapter<String> frameAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, spinnerFrame);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        spinner_frame.setAdapter(frameAdapter);
        spinner_frame.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                int value = Integer.parseInt(spinnerFrame.get(i));
                int old = AppPreference.getInt(AppPreference.KEY.VIDEO_FRAME, 30);
                AppPreference.setInt(AppPreference.KEY.VIDEO_FRAME, value);
                mListener.isDialog(false);
                if (value != old)
                    mListener.fragCameraRestart(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        spinner_frame.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
            return true;
            }
            return true;
        });

        List<String> ranges = new ArrayList<>();
        Streamer.FpsRange[] fpsRanges = cameraInfo.fpsRanges;
        for (Streamer.FpsRange range : fpsRanges) {
            if (range.fpsMax == range.fpsMin) {
                ranges.add(String.valueOf(range.fpsMax));
            } else {
                ranges.add(range.fpsMin + "~" + range.fpsMax);
            }
        }
        ArrayAdapter<String> rangesAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, ranges);
        rangesAdapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        streaming_frame.setAdapter(rangesAdapter);
        streaming_frame.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Streamer.FpsRange range = fpsRanges[i];
                int value = 0;
                boolean show_warning = false;
                int old = AppPreference.getInt(AppPreference.KEY.STREAMING_FRAME, 30);
                if (range.fpsMax == range.fpsMin) {
                    value = range.fpsMin;
                    AppPreference.setInt(AppPreference.KEY.STREAMING_FRAME, value);
                    AppPreference.setInt(AppPreference.KEY.FPS_RANGE_MIN, value);
                    AppPreference.setInt(AppPreference.KEY.FPS_RANGE_MAX, value);
                    show_warning = value < 30;
                } else {
                    value = -1;
                    AppPreference.setInt(AppPreference.KEY.STREAMING_FRAME, value);
                    AppPreference.setInt(AppPreference.KEY.FPS_RANGE_MIN, range.fpsMin);
                    AppPreference.setInt(AppPreference.KEY.FPS_RANGE_MAX, range.fpsMax);
                    show_warning = range.fpsMax < 30 || range.fpsMin < 30;
                }

                mListener.isDialog(false);
                if (value != old) {
                    mListener.fragCameraRestart(true);
                    if (show_warning) {
                        showFPSWarning(String.valueOf(value));
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        streaming_frame.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
                return true;
            }
            return false;
        });

        List<String> bitrates = Arrays.asList(getResources().getStringArray(R.array.audio_bitrate));
        ArrayAdapter<String> bitrate_adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, bitrates);
        bitrate_adapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        streaming_audio_bitrate.setAdapter(bitrate_adapter);
        int bitrate_streaming_audio = AppPreference.getInt(AppPreference.KEY.STREAMING_AUDIO_BITRATE, 0);
        streaming_audio_bitrate.setSelection(bitrate_streaming_audio);
        streaming_audio_bitrate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                int old_position = AppPreference.getInt(AppPreference.KEY.STREAMING_AUDIO_BITRATE, 0);
                if (old_position != position) {
                    mListener.fragCameraRestart(true);
                }
                AppPreference.setInt(AppPreference.KEY.STREAMING_AUDIO_BITRATE, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        streaming_audio_bitrate.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
                return true;
            }
            return false;
        });

        usb_audio_bitrate.setAdapter(bitrate_adapter);
        int usb_bitrate_audio = AppPreference.getInt(AppPreference.KEY.USB_AUDIO_BITRATE, 0);
        usb_audio_bitrate.setSelection(usb_bitrate_audio);
        usb_audio_bitrate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                int old_position = AppPreference.getInt(AppPreference.KEY.USB_AUDIO_BITRATE, 0);
                if (old_position != position) {
                    mListener.fragCameraRestart(true);
                }
                AppPreference.setInt(AppPreference.KEY.USB_AUDIO_BITRATE, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        usb_audio_bitrate.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
                return true;
            }
            return false;
        });

        List<String> audio_sources = Arrays.asList(getResources().getStringArray(R.array.audio_source));
        ArrayAdapter<String> source_adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, audio_sources);
        source_adapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        spinner_audio_src.setAdapter(source_adapter);
        int audio_source = AppPreference.getInt(AppPreference.KEY.AUDIO_SRC, 0);
        spinner_audio_src.setSelection(audio_source);
        spinner_audio_src.setOnItemSelectedEvenIfUnchangedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mListener.isDialog(false);
                int old_position = AppPreference.getInt(AppPreference.KEY.AUDIO_SRC, 0);
                if (old_position != position && !AppPreference.getBool(AppPreference.KEY.BLUETOOTH_MIC, false)) {
                    mListener.fragCameraRestart(true);
                }
                AppPreference.setInt(AppPreference.KEY.AUDIO_SRC, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        spinner_audio_src.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
                return true;
            }
            return false;
        });

        usb_audio_src.setAdapter(source_adapter);
        int usb_audio_source = AppPreference.getInt(AppPreference.KEY.USB_AUDIO_SRC, 0);
        usb_audio_src.setSelection(usb_audio_source);
        usb_audio_src.setOnItemSelectedEvenIfUnchangedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mListener.isDialog(false);
                int old_position = AppPreference.getInt(AppPreference.KEY.USB_AUDIO_SRC, 0);
                if (old_position != position && !AppPreference.getBool(AppPreference.KEY.BLUETOOTH_USB_MIC, false)) {
                    mListener.fragCameraRestart(true);
                }
                AppPreference.setInt(AppPreference.KEY.USB_AUDIO_SRC, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        usb_audio_src.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
                return true;
            }
            return false;
        });

        List<String> channel_count = Arrays.asList(getResources().getStringArray(R.array.channel_count));
        ArrayAdapter<String> channel_adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, channel_count);
        channel_adapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        usb_channel_count.setAdapter(channel_adapter);
        int channel_cnt = AppPreference.getInt(AppPreference.KEY.CHANNEL_COUNT, 0);
        usb_channel_count.setSelection(channel_cnt);
        usb_channel_count.setOnItemSelectedEvenIfUnchangedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mListener.isDialog(false);
                int old_position = AppPreference.getInt(AppPreference.KEY.CHANNEL_COUNT, 0);
                if (old_position != position && !AppPreference.getBool(AppPreference.KEY.BLUETOOTH_MIC, false)) {
                    mListener.fragCameraRestart(true);
                }
                AppPreference.setInt(AppPreference.KEY.CHANNEL_COUNT, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        usb_channel_count.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
                return true;
            }
            return false;
        });

        bluetooth_audio_src.setAdapter(source_adapter);
        int ble_source = AppPreference.getInt(AppPreference.KEY.BLE_AUDIO_SRC, 0);
        bluetooth_audio_src.setOnItemSelectedEvenIfUnchangedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mListener.isDialog(false);
                int old_position = AppPreference.getInt(AppPreference.KEY.BLE_AUDIO_SRC, 0);
                if (old_position != position && AppPreference.getBool(AppPreference.KEY.BLUETOOTH_MIC, false)) {
                    mListener.fragCameraRestart(true);
                }
                AppPreference.setInt(AppPreference.KEY.BLE_AUDIO_SRC, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        bluetooth_audio_src.setSelection(ble_source);
        bluetooth_audio_src.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
                return true;
            }
            return false;
        });

        usb_bluetooth_src.setAdapter(source_adapter);
        int usb_ble_source = AppPreference.getInt(AppPreference.KEY.USB_BLE_AUDIO_SRC, 0);
        usb_bluetooth_src.setSelection(usb_ble_source);
        usb_bluetooth_src.setOnItemSelectedEvenIfUnchangedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mListener.isDialog(false);
                int old_position = AppPreference.getInt(AppPreference.KEY.USB_BLE_AUDIO_SRC, 0);
                if (old_position != position && AppPreference.getBool(AppPreference.KEY.BLUETOOTH_USB_MIC, false)) {
                    mListener.fragCameraRestart(true);
                }
                AppPreference.setInt(AppPreference.KEY.USB_BLE_AUDIO_SRC, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        usb_bluetooth_src.setSelection(ble_source);
        usb_bluetooth_src.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
                return true;
            }
            return false;
        });

        List<String> sample_rates = Arrays.asList(getResources().getStringArray(R.array.sample_rates));
        ArrayAdapter<String> sample_adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, sample_rates);
        sample_adapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        spinner_sample_rate.setAdapter(sample_adapter);
        int sample_rate = AppPreference.getInt(AppPreference.KEY.SAMPLE_RATE, 7);
        spinner_sample_rate.setSelection(sample_rate);
        spinner_sample_rate.setOnItemSelectedEvenIfUnchangedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mListener.isDialog(false);
                int old_position = AppPreference.getInt(AppPreference.KEY.SAMPLE_RATE, 7);
                if (old_position != position) {
                    mListener.fragCameraRestart(true);
                }
                AppPreference.setInt(AppPreference.KEY.SAMPLE_RATE, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        spinner_sample_rate.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
                return true;
            }
            return false;
        });
        usb_sample_rate.setAdapter(sample_adapter);
        int usb_rate = AppPreference.getInt(AppPreference.KEY.USB_SAMPLE_RATE, 7);
        usb_sample_rate.setSelection(usb_rate);
        usb_sample_rate.setOnItemSelectedEvenIfUnchangedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mListener.isDialog(false);
                int old_position = AppPreference.getInt(AppPreference.KEY.USB_SAMPLE_RATE, 7);
                if (old_position != position) {
                    mListener.fragCameraRestart(true);
                }
                AppPreference.setInt(AppPreference.KEY.USB_SAMPLE_RATE, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        usb_sample_rate.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
                return true;
            }
            return false;
        });

        swt_radio_mode.setChecked(AppPreference.getBool(AppPreference.KEY.STREAMING_RADIO_MODE, false));
        swt_radio_mode.setOnClickListener(v -> {
            AppPreference.setBool(AppPreference.KEY.STREAMING_RADIO_MODE, swt_radio_mode.isChecked());
            mListener.fragCameraRestart(true);
        });

        swt_adaptive_fps.setChecked(AppPreference.getBool(AppPreference.KEY.ADAPTIVE_FRAMERATE, false));
        swt_adaptive_fps.setOnClickListener(v -> {
            AppPreference.setBool(AppPreference.KEY.ADAPTIVE_FRAMERATE, swt_adaptive_fps.isChecked());
            mListener.fragCameraRestart(true);
        });

        swt_radio_bluetooth.setChecked(AppPreference.getBool(AppPreference.KEY.BLUETOOTH_MIC, false));
        swt_radio_bluetooth.setOnClickListener(v -> {
            boolean isOn = swt_radio_bluetooth.isChecked();
            handleBluetoothAudio(isOn);
            mListener.fragCameraRestart(true);
        });
        handleBluetoothAudio(swt_radio_bluetooth.isChecked());

        usb_radio_bluetooth.setChecked(AppPreference.getBool(AppPreference.KEY.BLUETOOTH_USB_MIC, false));
        usb_radio_bluetooth.setOnClickListener(v -> {
            boolean isOn = usb_radio_bluetooth.isChecked();
            handleUSBBluetoothAudio(isOn);
            mListener.fragCameraRestart(true);
        });
        handleUSBBluetoothAudio(usb_radio_bluetooth.isChecked());

        String split_time = String.valueOf(AppPreference.getInt(AppPreference.KEY.SPLIT_TIME, 10));
        edt_split.setText(String.valueOf(split_time));
        edt_split.setOnClickListener(v -> {
            String splitting_time = String.valueOf(AppPreference.getInt(AppPreference.KEY.SPLIT_TIME, 10));
            openGeneralDialog(AppConstant.GENERAL_SPLIT, splitting_time);
        });

        edt_cloud.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (!TextUtils.isEmpty(charSequence)) {
                    AppPreference.setStr(AppPreference.KEY.BETA_URL, charSequence.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        edt_cloud.setText(AppPreference.getStr(AppPreference.KEY.BETA_URL, RestApiService.DNS));

        String pin = AppPreference.getStr(AppPreference.KEY.PIN_NUMBER, "");
        edt_pin.setText(pin);
        edt_pin.setOnClickListener(v -> {
            String pin_data = AppPreference.getStr(AppPreference.KEY.PIN_NUMBER, "");
            openGeneralDialog(AppConstant.GENERAL_PIN, pin_data);
        });

        int framerate = AppPreference.getInt(AppPreference.KEY.VIDEO_FRAME, 30);
        int bitrate = AppPreference.getInt(AppPreference.KEY.VIDEO_BITRATE, 4096);
        int keyframe = AppPreference.getInt(AppPreference.KEY.VIDEO_KEYFRAME, 1);

        int framerate_streaming = AppPreference.getInt(AppPreference.KEY.STREAMING_FRAME, 30);
        int streaming_bitrate = AppPreference.getInt(AppPreference.KEY.STREAMING_BITRATE, 1024);
        int keyframe_streaming = AppPreference.getInt(AppPreference.KEY.STREAMING_KEYFRAME, 1);

        if (resolution > -1) {
            spinner_resolution.setSelection(resolution);
        }
        if (resolution_streaming > -1) {
            streaming_resolution.setSelection(resolution_streaming);
        }
        if (framerate > -1) {
            for (String frame : spinnerFrame) {
                if (Integer.parseInt(frame) == framerate) {
                    spinner_frame.setSelection(spinnerFrame.indexOf(frame));
                }
            }
        }
        if (framerate_streaming > -1) {
            setStreamFPS(framerate_streaming);
        }
        edt_bitrate.setText(String.valueOf(bitrate));
        edt_keyFrame.setText(String.valueOf(keyframe));
        edt_bitrate.setOnClickListener(v -> {
            int vid_bitrate = AppPreference.getInt(AppPreference.KEY.VIDEO_BITRATE, 4096);
            openGeneralDialog(AppConstant.GENERAL_VIDEO_BITRATE, String.valueOf(vid_bitrate));
        });
        edt_keyFrame.setOnClickListener(v -> {
            int key_frame = AppPreference.getInt(AppPreference.KEY.VIDEO_KEYFRAME, 1);
            openGeneralDialog(AppConstant.GENERAL_VIDEO_KEYFRAME, String.valueOf(key_frame));
        });

        int cast_bitrate = AppPreference.getInt(AppPreference.KEY.CAST_BITRATE, 2048);
        edt_cast_bitrate.setText(String.valueOf(cast_bitrate));
        edt_cast_bitrate.setOnClickListener(v -> {
            int cast_bitrate1 = AppPreference.getInt(AppPreference.KEY.CAST_BITRATE, 2048);
            openGeneralDialog(AppConstant.GENERA_CAST_BITRATE, String.valueOf(cast_bitrate1));
        });
        int cast_frame = AppPreference.getInt(AppPreference.KEY.CAST_FRAME, 1);
        edt_cast_keyFrame.setText(String.valueOf(cast_frame));
        edt_cast_keyFrame.setOnClickListener(v -> {
            int cast_frame1 = AppPreference.getInt(AppPreference.KEY.CAST_FRAME, 1);
            openGeneralDialog(AppConstant.GENERAL_CAST_FRAME, String.valueOf(cast_frame1));
        });

        edt_streaming_bitrate.setText(String.valueOf(streaming_bitrate));
        edt_streaming_keyFrame.setText(String.valueOf(keyframe_streaming));
        edt_streaming_bitrate.setOnClickListener(v -> {
            int stream_bitrate = AppPreference.getInt(AppPreference.KEY.STREAMING_BITRATE, 1024);
            openGeneralDialog(AppConstant.GENERAL_STREAM_BITRATE, String.valueOf(stream_bitrate));
        });
        edt_streaming_keyFrame.setOnClickListener(v -> {
            int keyframe_stream = AppPreference.getInt(AppPreference.KEY.STREAMING_KEYFRAME, 1);
            openGeneralDialog(AppConstant.GENERAL_STREAM_KEYFRAME, String.valueOf(keyframe_stream));
        });

        String serial = AppPreference.getStr(AppPreference.KEY.ACTIVATION_SERIAL, "");
        if (!TextUtils.isEmpty(serial)) {
            txt_serial.setText(CommonUtil.formatSerial(serial));
        }
        txt_machine.setText(CommonUtil.getDeviceID(requireContext()));
        if (!TextUtils.isEmpty(AppConstant.expire_date)) {
            txt_expire.setText(CommonUtil.expire_date(AppConstant.expire_date));
            ly_expire.setVisibility(View.VISIBLE);
        } else {
            ly_expire.setVisibility(View.GONE);
        }

//        mListener.fragUpdateMenu();
        updateLocation();
        spinner_usb_codec.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
                return true;
            }
            return false;
        });
        spinner_usb_resolution.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
                return true;
            }
            return false;
        });

        if (mActivity != null) {
            if (mActivity.isUSBOpened()) {
                usb_path_ll.setVisibility(View.VISIBLE);
                initUSBResolutions();
            } else {
                usb_path_ll.setVisibility(View.GONE);
            }
        }else {
            usb_path_ll.setVisibility(View.GONE);
        }
        initUSBCodec();

        int usb_min_fps = AppPreference.getInt(AppPreference.KEY.USB_MIN_FPS, 30);
        edt_usb_min_fps.setText(String.valueOf(usb_min_fps));
        edt_usb_min_fps.setOnClickListener(v -> {
            int usb_min_fps1 = AppPreference.getInt(AppPreference.KEY.USB_MIN_FPS, 30);
            openGeneralDialog(AppConstant.GENERAL_USB_MIN_FPS, String.valueOf(usb_min_fps1));
        });

        int usb_max_fps = AppPreference.getInt(AppPreference.KEY.USB_MAX_FPS, 30);
        edt_usb_max_fps.setText(String.valueOf(usb_max_fps));
        edt_usb_max_fps.setOnClickListener(v -> {
            int usb_max_fps1 = AppPreference.getInt(AppPreference.KEY.USB_MAX_FPS, 30);
            openGeneralDialog(AppConstant.GENERAL_USB_MAX_FPS, String.valueOf(usb_max_fps1));
        });

        int usb_fps_streaming = AppPreference.getInt(AppPreference.KEY.USB_FRAME, 30);

        if (usb_max_fps >= 1 && usb_min_fps >= 1 && usb_max_fps <= 30 && usb_min_fps <= 30) {
            setUsbFPS(usb_fps_streaming);
        }
        audioOptions();

    }

    private boolean isAccessibilityServiceEnabled(Context context, Class<? extends AccessibilityService> serviceClass) {
        String service = MyAccessibilityService.class.getCanonicalName();
        int accessibilityEnabledNew = 0;
        final String serviceString = context.getPackageName() + "/" + service;
        try {
            accessibilityEnabledNew = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        if (accessibilityEnabledNew == 1) {
            String settingValue = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                colonSplitter.setString(settingValue);
                while (colonSplitter.hasNext()) {
                    String accessibilityService = colonSplitter.next();
                    if (accessibilityService.equalsIgnoreCase(serviceString)) {
                        
                    }
                }
            }
        }

        AccessibilityManager am = (AccessibilityManager) context.getSystemService(ACCESSIBILITY_SERVICE);

        String serviceName = new ComponentName(context, serviceClass).flattenToString();
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        );

        if (enabledServices != null) {
            for (AccessibilityServiceInfo service1 : enabledServices) {
                if (service1.getId().equals(serviceName)) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void audioOptions() {
        List<String> audio_option_audio_setting = Arrays.asList(getResources().getStringArray(R.array.audio_option_audio_setting));
        ArrayAdapter<String> audio_setting_adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, audio_option_audio_setting);
        audio_setting_adapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        audio_src.setAdapter(audio_setting_adapter);
        int audio_source = AppPreference.getInt(AppPreference.KEY.AUDIO_OPTION_AUDIO_SETTING, 0);
        audio_src.setSelection(audio_source);
        audio_src.setOnItemSelectedEvenIfUnchangedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mListener.isDialog(false);
//                int old_position = AppPreference.getInt(AppPreference.KEY.AUDIO_OPTION_AUDIO_SETTING, 0);
//                if (old_position != position && !AppPreference.getBool(AppPreference.KEY.BLUETOOTH_MIC, false)) {
//                    mListener.fragCameraRestart(true);
//                }
                AppPreference.setInt(AppPreference.KEY.AUDIO_OPTION_AUDIO_SETTING, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        audio_src.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
                return true;
            }
            return false;
        });

        List<String> audio_option_audio_pre_mic = Arrays.asList(getResources().getStringArray(R.array.audio_option_audio_source));
        ArrayAdapter<String> source_mic_adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, audio_option_audio_pre_mic);
        source_mic_adapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        audio_pref_mic.setAdapter(source_mic_adapter);
        int audio_mic_source = AppPreference.getInt(AppPreference.KEY.AUDIO_OPTION_AUDIO_SRC, 0);
        audio_pref_mic.setSelection(audio_mic_source);
        audio_pref_mic.setOnItemSelectedEvenIfUnchangedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mListener.isDialog(false);
                int old_position = AppPreference.getInt(AppPreference.KEY.AUDIO_OPTION_AUDIO_SRC, 0);
//                if (old_position != position && !AppPreference.getBool(AppPreference.KEY.BLUETOOTH_MIC, false)) {
//                    mListener.fragCameraRestart(true);
//                }
                AppPreference.setInt(AppPreference.KEY.AUDIO_OPTION_AUDIO_SRC, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        audio_pref_mic.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
                return true;
            }
            return false;
        });

        List<String> bitrates = Arrays.asList(getResources().getStringArray(R.array.audio_option_audio_bitrate));
        ArrayAdapter<String> bitrate_adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, bitrates);
        bitrate_adapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        audio_option_bitrate.setAdapter(bitrate_adapter);
        int bitrate_audio_option = AppPreference.getInt(AppPreference.KEY.AUDIO_OPTION_BITRATE, 0);
        audio_option_bitrate.setSelection(bitrate_audio_option);
        audio_option_bitrate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                int old_position = AppPreference.getInt(AppPreference.KEY.AUDIO_OPTION_BITRATE, 0);
//                if (old_position != position) {
//                    mListener.fragCameraRestart(true);
//                }
                AppPreference.setInt(AppPreference.KEY.AUDIO_OPTION_BITRATE, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        audio_option_bitrate.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
                return true;
            }
            return false;
        });

        List<String> sample_rates = Arrays.asList(getResources().getStringArray(R.array.audio_option_sample_rates));
        ArrayAdapter<String> sample_adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, sample_rates);
        sample_adapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        audio_option_sample_rate.setAdapter(sample_adapter);
        int sample_rate = AppPreference.getInt(AppPreference.KEY.AUDIO_OPTION_SAMPLE_RATE, 0);
        audio_option_sample_rate.setSelection(sample_rate);
        audio_option_sample_rate.setOnItemSelectedEvenIfUnchangedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mListener.isDialog(false);
                int old_position = AppPreference.getInt(AppPreference.KEY.AUDIO_OPTION_SAMPLE_RATE, 0);
//                if (old_position != position) {
//                    mListener.fragCameraRestart(true);
//                }
                AppPreference.setInt(AppPreference.KEY.AUDIO_OPTION_SAMPLE_RATE, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        audio_option_sample_rate.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
                return true;
            }
            return false;
        });

        List<String> channel_count = Arrays.asList(getResources().getStringArray(R.array.channel_count));
        ArrayAdapter<String> channel_adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, channel_count);
        channel_adapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        audio_option_channel_count.setAdapter(channel_adapter);
        int channel_cnt = AppPreference.getInt(AppPreference.KEY.AUDIO_OPTION_CHANNEL_COUNT, 0);
        audio_option_channel_count.setSelection(channel_cnt);
        audio_option_channel_count.setOnItemSelectedEvenIfUnchangedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mListener.isDialog(false);
                int old_position = AppPreference.getInt(AppPreference.KEY.AUDIO_OPTION_CHANNEL_COUNT, 0);
//                if (old_position != position && !AppPreference.getBool(AppPreference.KEY.BLUETOOTH_MIC, false)) {
//                    mListener.fragCameraRestart(true);
//                }
                AppPreference.setInt(AppPreference.KEY.AUDIO_OPTION_CHANNEL_COUNT, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        audio_option_channel_count.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.isDialog(true);
                return true;
            }
            return false;
        });

    }

    @Override
    public void onStoragePathChanged(String newPath) {
        txt_storage.setText(getFullPathFromTreeUri(Uri.parse(newPath)));
    }
    void setUsbFPS(int fps) {
        int index = -1;
        int fps_min = AppPreference.getInt(AppPreference.KEY.USB_MIN_FPS, 30);
        int fps_max = AppPreference.getInt(AppPreference.KEY.USB_MAX_FPS, 30);
        if (mActivity != null) {
            for (int i = 0; i < mActivity.findCameraInfo().fpsRanges.length; i++) {
                Streamer.FpsRange range = mActivity.findCameraInfo().fpsRanges[i];
                if (range.fpsMax == range.fpsMin) {
                    if (fps == range.fpsMax) {
                        index = i;
                        AppPreference.setInt(AppPreference.KEY.USB_FRAME, range.fpsMax);
                        AppPreference.setInt(AppPreference.KEY.FPS_RANGE_MAX, range.fpsMax);
                        AppPreference.setInt(AppPreference.KEY.FPS_RANGE_MIN, range.fpsMin);
                    }
                } else {
                    if (fps_min == range.fpsMin && fps_max == range.fpsMax) {
                        index = i;
                        AppPreference.setInt(AppPreference.KEY.STREAMING_FRAME, -1);
                        AppPreference.setInt(AppPreference.KEY.FPS_RANGE_MAX, range.fpsMax);
                        AppPreference.setInt(AppPreference.KEY.FPS_RANGE_MIN, range.fpsMin);
                    }
                }
            }
            if (index == -1) {
                index = mActivity.findCameraInfo().fpsRanges.length - 1;
            }
            streaming_frame.setSelection(index);
        }

    }

    public void initUSBCodec() {
        List<String> codec_sources = Arrays.asList(getResources().getStringArray(R.array.usb_codec));
        ArrayAdapter<String> source_adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, codec_sources);
        source_adapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        spinner_usb_codec.setAdapter(source_adapter);
        int codec_source = AppPreference.getInt(AppPreference.KEY.CODEC_SRC, 0);
        spinner_usb_codec.setSelection(codec_source);
        spinner_usb_codec.setOnItemSelectedEvenIfUnchangedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mListener.isDialog(false);
                int old_position = AppPreference.getInt(AppPreference.KEY.CODEC_SRC, 0);
                if (old_position != position) {
                    mListener.fragCameraRestart(true);
                }
                AppPreference.setInt(AppPreference.KEY.CODEC_SRC, position);

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    public void initUSBResolutions() {
        if (mActivity != null) {
            List<String> resolutions = mActivity.getUSBCameraResolutions();
            if (resolutions.isEmpty()) {
                return;
            }
            ArrayAdapter<String> res_adapter = new ArrayAdapter<>(
                    requireContext(), android.R.layout.simple_spinner_item, resolutions);
            res_adapter.setDropDownViewResource(android.R.layout
                    .simple_spinner_dropdown_item);
            spinner_usb_resolution.setAdapter(res_adapter);

            int res_source = AppPreference.getInt(AppPreference.KEY.USB_RESOLUTION, 0);
            spinner_usb_resolution.setSelection(res_source);
            spinner_usb_resolution.setOnItemSelectedEvenIfUnchangedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                    mListener.isDialog(false);
                    int old_position = AppPreference.getInt(AppPreference.KEY.USB_RESOLUTION, 0);
                    if (old_position != position) {
                        mListener.fragCameraRestart(true);
                    }
                    if (isUSBOpen) {
                      //  spinner_resolution.setSelection(position);
                    }
                    AppPreference.setInt(AppPreference.KEY.USB_RESOLUTION, position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
        }
    }

    void handleBluetoothAudio(boolean bluetooth_enabled) {
        AppPreference.setBool(AppPreference.KEY.BLUETOOTH_MIC, bluetooth_enabled);
        spinner_audio_src.setEnabled(!bluetooth_enabled);
        spinner_sample_rate.setEnabled(!bluetooth_enabled);
        bluetooth_audio_src.setEnabled(bluetooth_enabled);
    }

    void handleUSBBluetoothAudio(boolean bluetooth_enabled) {
        AppPreference.setBool(AppPreference.KEY.BLUETOOTH_USB_MIC, bluetooth_enabled);
        usb_audio_src.setEnabled(!bluetooth_enabled);
        usb_sample_rate.setEnabled(!bluetooth_enabled);
        usb_bluetooth_src.setEnabled(bluetooth_enabled);
    }

    void showFPSWarning(String fps) {
        int stream_quality = AppPreference.getInt(AppPreference.KEY.STREAMING_QUALITY, 1);
        if (stream_quality == AppConstant.QUALITY_LOW) {
            fps = "LOW";
        } else if (stream_quality == AppConstant.QUALITY_SUPER_LOW) {
            fps = "SUPER LOW";
        } else {
            fps = "CUSTOM";
        }
        mListener.isDialog(true);
        if (!isAdded() || getActivity() == null) {
            Log.e("StreamingFragment", "Fragment detached, skipping onLogin callback");
            return;
        }
        MessageUtil.showAlertDialog(getActivity(), MessageUtil.TYPE_WARNING, getString(R.string.warning_fps, fps), (dialog, which) -> {
            mListener.isDialog(false);
        });
    }

    void setStreamFPS(int fps) {
        if (mActivity == null) {
            return;
        }
        int index = -1;
        int fps_min = AppPreference.getInt(AppPreference.KEY.FPS_RANGE_MIN, 30);
        int fps_max = AppPreference.getInt(AppPreference.KEY.FPS_RANGE_MAX, 30);
        for (int i = 0; i < mActivity.findCameraInfo().fpsRanges.length; i++) {
            Streamer.FpsRange range = mActivity.findCameraInfo().fpsRanges[i];
            if (range.fpsMax == range.fpsMin) {
                if (fps == range.fpsMax) {
                    index = i;
                    AppPreference.setInt(AppPreference.KEY.STREAMING_FRAME, range.fpsMax);
                    AppPreference.setInt(AppPreference.KEY.FPS_RANGE_MAX, range.fpsMax);
                    AppPreference.setInt(AppPreference.KEY.FPS_RANGE_MIN, range.fpsMin);
                }
            } else {
                if (fps_min == range.fpsMin && fps_max == range.fpsMax) {
                    index = i;
                    AppPreference.setInt(AppPreference.KEY.STREAMING_FRAME, -1);
                    AppPreference.setInt(AppPreference.KEY.FPS_RANGE_MAX, range.fpsMax);
                    AppPreference.setInt(AppPreference.KEY.FPS_RANGE_MIN, range.fpsMin);
                }
            }
        }
        if (index == -1) {
            index = mActivity.findCameraInfo().fpsRanges.length - 1;
        }
        streaming_frame.setSelection(index);
    }

    void openGeneralDialog(int type, String value) {
        GeneralDialog dialog = new GeneralDialog(requireContext());
        mListener.isDialog(true);
        if (type == AppConstant.GENERAL_SPLIT) {
            dialog = new GeneralDialog(requireContext(), R.string.split_title, R.string.split_desc, type, value);
        } else if (type == AppConstant.GENERAL_PIN) {
            dialog = new GeneralDialog(requireContext(), R.string.pin_title, R.string.pin_desc, type, value);
        } else if (type == AppConstant.GENERAL_VIDEO_BITRATE) {
            dialog = new GeneralDialog(requireContext(), R.string.bitrate_title, R.string.bitrate_desc, type, value);
        } else if (type == AppConstant.GENERAL_VIDEO_KEYFRAME) {
            dialog = new GeneralDialog(requireContext(), R.string.key_frame_interval_title, R.string.key_frame_desc, type, value);
        } else if (type == AppConstant.GENERAL_STREAM_BITRATE) {
            dialog = new GeneralDialog(requireContext(), R.string.bitrate_title, R.string.bitrate_desc, type, value);
        } else if (type == AppConstant.GENERAL_STREAM_KEYFRAME) {
            dialog = new GeneralDialog(requireContext(), R.string.key_frame_interval_title, R.string.key_frame_desc, type, value);
        } else if (type == AppConstant.GENERA_CAST_BITRATE) {
            dialog = new GeneralDialog(requireContext(), R.string.bitrate_title, R.string.bitrate_desc, type, value);
        } else if (type == AppConstant.GENERAL_CAST_FRAME) {
            dialog = new GeneralDialog(requireContext(), R.string.key_frame_interval_title, R.string.key_frame_desc, type, value);
        } else if (type == AppConstant.GENERAL_USB_MIN_FPS) {
            dialog = new GeneralDialog(requireContext(), R.string.usb_fps_title, R.string.usb_fps_desc, type, value);
        } else if (type == AppConstant.GENERAL_USB_MAX_FPS) {
            dialog = new GeneralDialog(requireContext(), R.string.usb_fps_title, R.string.usb_fps_desc, type, value);
        }
        dialog.setResultListener((result, is_changed) -> {
            if (type == AppConstant.GENERAL_SPLIT) {
                AppPreference.setInt(AppPreference.KEY.SPLIT_TIME, Integer.parseInt(result));
                edt_split.setText(result);
            } else if (type == AppConstant.GENERAL_PIN) {
                AppPreference.setStr(AppPreference.KEY.PIN_NUMBER, result);
                edt_pin.setText(result);
            } else if (type == AppConstant.GENERAL_VIDEO_BITRATE) {
                AppPreference.setInt(AppPreference.KEY.VIDEO_BITRATE, Integer.parseInt(result));
                edt_bitrate.setText(result);
            } else if (type == AppConstant.GENERAL_VIDEO_KEYFRAME) {
                AppPreference.setInt(AppPreference.KEY.VIDEO_KEYFRAME, Integer.parseInt(result));
                edt_keyFrame.setText(result);
            } else if (type == AppConstant.GENERAL_STREAM_BITRATE) {
                AppPreference.setInt(AppPreference.KEY.STREAMING_BITRATE, Integer.parseInt(result));
                edt_streaming_bitrate.setText(result);
            } else if (type == AppConstant.GENERAL_STREAM_KEYFRAME) {
                AppPreference.setInt(AppPreference.KEY.STREAMING_KEYFRAME, Integer.parseInt(result));
                edt_streaming_keyFrame.setText(result);
            } else if (type == AppConstant.GENERA_CAST_BITRATE) {
                AppPreference.setInt(AppPreference.KEY.CAST_BITRATE, Integer.parseInt(result));
                edt_cast_bitrate.setText(result);
            } else if (type == AppConstant.GENERAL_CAST_FRAME) {
                AppPreference.setInt(AppPreference.KEY.CAST_FRAME, Integer.parseInt(result));
                edt_cast_keyFrame.setText(result);
            } else if (type == AppConstant.GENERAL_USB_MIN_FPS) {
                AppPreference.setInt(AppPreference.KEY.USB_MIN_FPS, Integer.parseInt(result));
                edt_usb_min_fps.setText(result);
            } else if (type == AppConstant.GENERAL_USB_MAX_FPS) {
                AppPreference.setInt(AppPreference.KEY.USB_MAX_FPS, Integer.parseInt(result));
                edt_usb_max_fps.setText(result);
            }
            if (is_changed) {
                mListener.fragCameraRestart(true);
            }
        });
        dialog.setOnDismissListener(dialog1 -> mListener.isDialog(false));
        dialog.show();
    }

    public void updateLocation() {
        txt_location.setText(String.format("(%.6f, %.6f)", LocationManagerService.lat, LocationManagerService.lng));
    }

    @Override
    public void onPause() {

        super.onPause();
        if (swt_convert_ui.isChecked()) {
            String pin = AppPreference.getStr(AppPreference.KEY.PIN_NUMBER, "");
            if (TextUtils.isEmpty(pin)) {
                swt_convert_ui.setChecked(false);
                AppPreference.setBool(AppPreference.KEY.UI_CONVERT_MODE, false);
            }
        }
    }

    @Override
    public void onResume() {
        CriticalComponentsMonitor.executeComponentSafely("SettingsFragment.onResume", () -> {
            try {
                super.onResume();

                // Update location with null safety
                updateLocation();
                
                String usbCamName = ANRSafeHelper.nullSafe(
                    AppPreference.getStr(AppPreference.KEY.USB_CAMERA_NAME, ""), 
                    "", 
                    "USB_CAMERA_NAME preference"
                );
                
                if (txt_usb_cam != null) {
                    txt_usb_cam.setText(usbCamName);
                } else {
                    InternalLogger.w("SettingsFragment", "txt_usb_cam is null in onResume");
                }
                
                boolean isEnabledNew = isAccessibilityServiceEnabled(getContext(), MyAccessibilityService.class);
                if (swt_radio_accessbility != null) {
                    swt_radio_accessbility.setChecked(isEnabledNew);
                } else {
                    InternalLogger.w("SettingsFragment", "swt_radio_accessbility is null in onResume");
                }
                
                InternalLogger.d("SettingsFragment", "onResume completed successfully");
                
                
            } catch (Exception e) {
                InternalLogger.e("SettingsFragment", "Error in onResume", e);
            }
        });
    }

    public void setCameraname(String name ){
        usb_path_ll.setVisibility(View.VISIBLE);
        txt_usb_cam.setText(name);
    }

    @Override
    public void onDestroy() {
        CriticalComponentsMonitor.executeComponentSafely("SettingsFragment.onDestroy", () -> {
            try {
                InternalLogger.i("SettingsFragment", "onDestroy starting");
                
                // Clean up instance reference
                if (instance != null) {
                    instance.clear();
                    instance = null;
                }
                
                // Clean up other references
                mListener = null;
                mActivity = null;
                
                super.onDestroy();
                
                InternalLogger.d("SettingsFragment", "onDestroy completed successfully");
                
                
            } catch (Exception e) {
                InternalLogger.e("SettingsFragment", "Error in onDestroy", e);
            }
        });
    }

    void onAddCamera() {
        if (DBManager.getInstance().getCameras().size() > 0) {
            MessageUtil.showToast(requireContext(), R.string.unable_add_wifi);
            return;
        }
        mListener.isDialog(true);
//        new CameraDialog(requireContext()).show();
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Camera")
                .setItems(new CharSequence[]{"VCS WiFi Cam", "Lawmate WiFi Cam", "ATN Binox", "Generic WiFi Camera", "RTSP Camera"},
                        (dialog, which) -> {
                            switch (which) {
                                case 0: // VCS WiFi Camera
                                    onAddWifiCamera(AppConstant.WIFI_TYPE_VCS);
                                    break;
                                case 1: // Lawmate WiFi Camera
                                    onAddWifiCamera(AppConstant.WIFI_TYPE_LAWMATE);
                                    break;
                                case 2: // ATN Binox
                                    onAddWifiCamera(AppConstant.WIFI_TYPE_ATN);
                                    break;
                                case 3: // Generic WiFi Camera
                                    onAddWifiCamera(AppConstant.WIFI_TYPE_GENERIC);
                                    break;
                                case 4: // RTSP Camera
                                    mListener.isDialog(true);
                                    new CameraDialog(requireContext(), AppConstant.WIFI_TYPE_RTSP).show();
                                    break;
                            }
                        })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Dismiss dialog and reset the flag
                    mListener.isDialog(false);
                    dialog.dismiss();
                })
                .setCancelable(true);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    void onAddWifiCamera(int camera_type) {
        mListener.isDialog(true);
        if (!isAdded() || getActivity() == null) {
            return;
        }
        getActivity().setTheme(R.style.ActionSheetStyleiOS7);
        ActionSheet.createBuilder(getContext(), getActivity().getSupportFragmentManager())
                .setCancelButtonTitle("Cancel")
                .setOtherButtonTitles("Scan QR", "Scan Wifi", "Add a wifi")
                .setCancelableOnTouchOutside(true)
                .setListener(new ActionSheet.ActionSheetListener() {
                    @Override
                    public void onDismiss(ActionSheet actionSheet, boolean isCancel) {
                        if (isCancel)
                            mListener.isDialog(false);
                    }

                    @Override
                    public void onOtherButtonClick(ActionSheet actionSheet, int index) {
                        switch (index) {
                            case 0:
                                scanWifiQR(camera_type);
                                break;
                            case 1:
                                scanWifiList(camera_type);
                                break;
                            case 2:
                                addWifi(camera_type);
                                break;
                        }
                    }
                }).show();
    }

    public void OnClick(View view) {
        switch (view.getId()) {
            case R.id.txt_camera:
                onAddCamera();
                break;
            case R.id.txt_update:
                mListener.fragUpdateApp("");
                break;
            case R.id.txt_storage:
                mListener.isDialog(true);
                if (android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.Q) {
                    checkStoragePermissions();
                } else {
                    openDirectory();
                }
                break;
            case R.id.txt_reactivate:
                onReactivate();
                break;
            case R.id.txt_check_update:
                checkUpdate();
                break;
            case R.id.txt_exit:
                exitApp();
                break;
            case R.id.txt_wifi_camera:
//                onAddWifiCamera();
                break;
            case R.id.txt_beta_update:
                mListener.fragUpdateApp(AppConstant.BETA_UPDATE);
                break;
            case R.id.txt_stream_details:
                if (txt_stream_details.getText().equals(getString(R.string.show_details))) {
                    // Show custom streaming fields
                    ly_streaming_custom.setVisibility(View.VISIBLE);
                    txt_stream_details.setText(R.string.return_defaults);
                    // Enable all streaming fields
                    enableStreamingFields(true);
                } else {
                    // Hide custom streaming fields and reset to defaults
                    ly_streaming_custom.setVisibility(View.GONE);
                    txt_stream_details.setText(R.string.show_details);
                    // Set default streaming values (Medium - index 1)
                    AppPreference.setBool(AppPreference.KEY.IS_NATIVE_STREAMING, true);
                    AppPreference.setInt(AppPreference.KEY.STREAMING_QUALITY, 1);
                    // Reset spinners to default values
                    resetStreamingToDefaults();
                    onStreamDetails(false);
                }
                break;
            case R.id.txt_video_details:
                if (txt_video_details.getText().equals(getString(R.string.show_details))) {
                    // Show custom video fields
                    ly_video_custom.setVisibility(View.VISIBLE);
                    txt_video_details.setText(R.string.return_defaults);
                    // Enable all video fields
                    enableVideoFields(true);
                } else {
                    // Hide custom video fields and reset to defaults
                    ly_video_custom.setVisibility(View.GONE);
                    txt_video_details.setText(R.string.show_details);
                    // Set default video values (High - index 0)
                    AppPreference.setBool(AppPreference.KEY.IS_NATIVE_RESOLUTION, true);
                    AppPreference.setInt(AppPreference.KEY.VIDEO_QUALITY, 0);
                    // Reset spinners to default values
                    resetVideoToDefaults();
                    onVideoDetails(false);
                }
                break;
            case R.id.txt_transcode:
                mListener.isDialog(true);
                TranscodeDialog dlg = new TranscodeDialog(requireContext());
                dlg.setResultLisetner(is_changed -> {
                    if (is_changed) {
                        mListener.fragCameraRestart(true);
                        mListener.stopFragWifiService();

                    }
                });
                dlg.show();
                break;
        }
    }

    private void checkStoragePermissions() {
        if (ContextCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_PERMISSION);
        } else {
            openDirectory();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openDirectory();
            } else {
                if (!isAdded() || getActivity() == null) {
                    Log.e("StreamingFragment", "Fragment detached, skipping onLogin callback");
                    return;
                }
                Toast.makeText(getActivity(), "Storage permission is required to save the file", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private static final int REQUEST_CODE_PICK_FOLDER = 100;

    private void openDirectory() {
        AppPreference.setBool(AppPreference.KEY.IS_FOR_STORAGE_LOCATION,true);
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        // Request read and write permission on the tree URI.
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_CODE_PICK_FOLDER);
    }

    public static String saveImageFromUrl(Context context, String imageUrl) {
        try {
            // Create custom folder in external storage
            File folder = new File(context.getExternalFilesDir(null).getAbsolutePath());
            if (!folder.exists()) {
                folder.mkdirs(); // Create the folder if it doesn't exist
            }

            // Create the file where the image will be saved
            File imageFile = new File(folder, ".jpg");

            // Download the image from URL
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream inputStream = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Save the bitmap to the file
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            return imageFile.getAbsolutePath(); // Return the saved file path
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Return null if an error occurs
        }
    }

    void onVideoDetails(boolean isShowDetail) {
        int video_quality = AppPreference.getInt(AppPreference.KEY.VIDEO_QUALITY, 0);
        boolean isNative = AppPreference.getBool(AppPreference.KEY.IS_NATIVE_RESOLUTION, false);
        if (video_quality == 0 && isNative) { // High
            onVideoHigh();
        } else if (video_quality == 1 && isNative) { // Medium
            onVideoMedium();
        } else if (video_quality == 2 && isNative) { // Low
            onVideoLow();
        } else if (video_quality == 3 && isNative) { // super Low
            onVideoSuperLow();
        }else if (video_quality == 4 && isNative) {  // USB
            onVideoUSB();
        }else if (video_quality == 5) { // custom
            setStreamValue(5);
            AppPreference.setBool(AppPreference.KEY.IS_NATIVE_RESOLUTION,false);
        }
        if (video_quality >= 5 || isShowDetail) {
            ly_video_custom.setVisibility(View.VISIBLE);
        }else {
            ly_video_custom.setVisibility(View.GONE);
        }
        spinner_resolution.setEnabled(video_quality >= AppConstant.QUALITY_CUSTOM);
        spinner_frame.setEnabled(video_quality >= AppConstant.QUALITY_CUSTOM);
        edt_bitrate.setEnabled(video_quality >= AppConstant.QUALITY_CUSTOM);
        edt_keyFrame.setEnabled(video_quality >= AppConstant.QUALITY_CUSTOM);
    }
    boolean isUserInteracting = false; // Prevents unnecessary UI loopss

    void setStreamQuality(int index) {
        streaming_resolution.post(() -> {
            Log.d("DEBUG", "Restoring Streaming Quality Position: " + index);
            isUserInteracting = false;
            AppPreference.setInt(AppPreference.KEY.STREAMING_RESOLUTION,index);
            streaming_resolution.setSelection(index, false);
            streaming_resolution.postDelayed(() -> isUserInteracting = true, 200);
            AppPreference.setBool(AppPreference.KEY.IS_NATIVE_STREAMING, false);
            onStreamDetails(false);
        });
    }

    void setStreamValue(int index) {
//        streaming_quality.post(() -> {
//            Log.d("DEBUG", "Restoring Streaming Quality Position: " + index);
//            isUserInteracting = false;
//            AppPreference.setInt(AppPreference.KEY.STREAMING_QUALITY,index);
//            streaming_quality.setSelection(index, false);
//            streaming_quality.postDelayed(() -> isUserInteracting = true, 200);
//            AppPreference.setBool(AppPreference.KEY.IS_NATIVE_STREAMING, true);
//            onStreamDetails(false);
//        });
    }

    void setRecordValue(int index) {
//        spinner_quality.post(() -> {
//            Log.d("DEBUG", "Restoring Recording Quality Position: " + index);
//            isUserInteracting = false;
//            spinner_quality.setSelection(index, false);
//            spinner_quality.postDelayed(() -> isUserInteracting = true, 200);
//        });
    }

    void onVideoLow() {
        int index = Arrays.asList(record_sizes).indexOf("640x360");
        boolean isUSBOpen = AppPreference.getBool(AppPreference.KEY.IS_USB_OPENED, false);
        if (isUSBOpen) {
            if (index < 0) {
                int resolution = AppPreference.getInt(AppPreference.KEY.VIDEO_RESOLUTION, 0);
                List<String> sortedResolutions = sharedViewModel.getCameraResolution();
                if (resolution >= sortedResolutions.size()) {
                    resolution = 0;
                    AppPreference.setInt(AppPreference.KEY.VIDEO_RESOLUTION, resolution);
                }
                index = resolution;
            }
        }
        AppPreference.setInt(AppPreference.KEY.VIDEO_RESOLUTION, index); // 720x480
        spinner_resolution.setSelection(index);
        AppPreference.setInt(AppPreference.KEY.VIDEO_FRAME, 15);
        spinner_frame.setSelection(3);
        AppPreference.setInt(AppPreference.KEY.VIDEO_BITRATE, 768);
        edt_bitrate.setText("768");
        AppPreference.setInt(AppPreference.KEY.VIDEO_KEYFRAME, 1);
        edt_keyFrame.setText("1");
        Log.d("DEBUG", "Saved Position: " + 3);
        setStreamValue(3);
        AppPreference.setBool(AppPreference.KEY.IS_NATIVE_RESOLUTION, true);
        ly_video_custom.setVisibility(View.GONE);
        txt_video_details.setText(R.string.show_details);
    }

    void onVideoSuperLow() {
        int index = Arrays.asList(record_sizes).indexOf("320x180");
        AppPreference.setInt(AppPreference.KEY.VIDEO_RESOLUTION, index); // 640x360
        spinner_resolution.setSelection(index);
        AppPreference.setInt(AppPreference.KEY.VIDEO_FRAME, 15);
        spinner_frame.setSelection(3);
        AppPreference.setInt(AppPreference.KEY.VIDEO_BITRATE, 400);
        edt_bitrate.setText("400");
        AppPreference.setInt(AppPreference.KEY.VIDEO_KEYFRAME, 1);
        edt_keyFrame.setText("1");
        Log.d("DEBUG", "Saved Position: " + 3);
        setStreamValue(3);
        AppPreference.setBool(AppPreference.KEY.IS_NATIVE_RESOLUTION, true);
        ly_video_custom.setVisibility(View.GONE);
        txt_video_details.setText(R.string.show_details);
    }

    void onVideoUSB() {
        if (sharedViewModel != null) {
            List<String> sortedResolutions = sharedViewModel.getCameraResolution();
            if (sortedResolutions.isEmpty()) {
             //   return;
                Toast.makeText(getContext(),"USB Device not connected",Toast.LENGTH_SHORT).show();
            }
            int index = Arrays.asList(record_sizes).indexOf("1920x1080");
            if (index < 0) {
                index = Arrays.asList(record_sizes).indexOf("1280x720");
            }
            if (index < 0) {
                index = Arrays.asList(record_sizes).indexOf("640x480");
            }
            if (index < 0) {
                index = Arrays.asList(record_sizes).indexOf("640x360");
            }
            if (index < 0) {
                index = Arrays.asList(record_sizes).indexOf("320x180");
            }
            AppPreference.setInt(AppPreference.KEY.VIDEO_RESOLUTION, index);
            spinner_resolution.setSelection(index);
            AppPreference.setInt(AppPreference.KEY.VIDEO_FRAME, 30);
            spinner_frame.setSelection(6);
            AppPreference.setInt(AppPreference.KEY.VIDEO_BITRATE, 4096);
            edt_bitrate.setText("4096");
            AppPreference.setInt(AppPreference.KEY.VIDEO_KEYFRAME, 1);
            edt_keyFrame.setText("1");
        }
        Log.d("DEBUG", "Saved Position: " + 4);
        setStreamValue(4);
        AppPreference.setBool(AppPreference.KEY.IS_NATIVE_RESOLUTION, true);
        ly_video_custom.setVisibility(View.GONE);
        txt_video_details.setText(R.string.show_details);
    }

    void onVideoMedium() {
        int index = Arrays.asList(record_sizes).indexOf("1280x720");
        boolean isUSBOpen = AppPreference.getBool(AppPreference.KEY.IS_USB_OPENED, false);
        if (isUSBOpen) {
            if (index < 0) {
                int resolution = AppPreference.getInt(AppPreference.KEY.VIDEO_RESOLUTION, 0);
                List<String> sortedResolutions = sharedViewModel.getCameraResolution();
                if (resolution >= sortedResolutions.size()) {
                    resolution = 0;
                    AppPreference.setInt(AppPreference.KEY.VIDEO_RESOLUTION, resolution);
                }
                index = resolution;
            }
        }
        AppPreference.setInt(AppPreference.KEY.VIDEO_RESOLUTION, index);
        spinner_resolution.setSelection(index);
        AppPreference.setInt(AppPreference.KEY.VIDEO_FRAME, 30);
        spinner_frame.setSelection(6);
        AppPreference.setInt(AppPreference.KEY.VIDEO_BITRATE, 1496);
        edt_bitrate.setText("1496");
        AppPreference.setInt(AppPreference.KEY.VIDEO_KEYFRAME, 1);
        edt_keyFrame.setText("1");
        Log.d("DEBUG", "Saved Position: " + 2);
        setStreamValue(2);
        AppPreference.setBool(AppPreference.KEY.IS_NATIVE_RESOLUTION, true);
        ly_video_custom.setVisibility(View.GONE);
        txt_video_details.setText(R.string.show_details);
    }

    public void onVideoHigh() {
        int index = Arrays.asList(record_sizes).indexOf("1920x1080");
        boolean isUSBOpen = AppPreference.getBool(AppPreference.KEY.IS_USB_OPENED, false);
        if (isUSBOpen) {
            if (index < 0) {
                int resolution = AppPreference.getInt(AppPreference.KEY.VIDEO_RESOLUTION, 0);
                List<String> sortedResolutions = sharedViewModel.getCameraResolution();
                if (resolution >= sortedResolutions.size()) {
                    resolution = 0;
                    AppPreference.setInt(AppPreference.KEY.VIDEO_RESOLUTION, resolution);
                }
                index = resolution;
            }
        }
        AppPreference.setInt(AppPreference.KEY.VIDEO_RESOLUTION, index); // 1920x1080
        spinner_resolution.setSelection(index);
        AppPreference.setInt(AppPreference.KEY.VIDEO_FRAME, 30);
        spinner_frame.setSelection(6);
        AppPreference.setInt(AppPreference.KEY.VIDEO_BITRATE, 3048);
        edt_bitrate.setText("3048");
        AppPreference.setInt(AppPreference.KEY.VIDEO_KEYFRAME, 1);
        edt_keyFrame.setText("1");
        Log.d("DEBUG", "Saved Position: " + 1);
        setStreamValue(1);
        AppPreference.setBool(AppPreference.KEY.IS_NATIVE_RESOLUTION, true);
        ly_video_custom.setVisibility(View.GONE);
        txt_video_details.setText(R.string.show_details);
    }

    void onStreamDetails(boolean isShowDetail) {
        int stream_quality = AppPreference.getInt(AppPreference.KEY.STREAMING_QUALITY, 1);
        boolean isNative = AppPreference.getBool(AppPreference.KEY.IS_NATIVE_STREAMING, false);
        if (stream_quality == 0 && isNative) { // High
            onStreamHigh();
        } else if (stream_quality == 1 && isNative) { // Medium
            onStreamMedium();
        } else if (stream_quality == 2 && isNative) { // Low
            onStreamLow();
        } else if (stream_quality == 3 && isNative) { // super Low
            onStreamSuperLow();
        }else if (stream_quality == 4 && isNative) { // USB
            onStreamUSBHigh();
        }else if (stream_quality == 5) { // custom}
            AppPreference.setBool(AppPreference.KEY.IS_NATIVE_STREAMING, false);
        }
        if (stream_quality >= 5 || isShowDetail) {
            ly_streaming_custom.setVisibility(View.VISIBLE);
        }else {
            ly_streaming_custom.setVisibility(View.GONE);
        }
        streaming_resolution.setEnabled(stream_quality >= AppConstant.QUALITY_CUSTOM);
        streaming_frame.setEnabled(stream_quality >= AppConstant.QUALITY_CUSTOM);
        streaming_audio_bitrate.setEnabled(stream_quality >= AppConstant.QUALITY_CUSTOM);
        edt_streaming_bitrate.setEnabled(stream_quality >= AppConstant.QUALITY_CUSTOM);
        edt_streaming_keyFrame.setEnabled(stream_quality >= AppConstant.QUALITY_CUSTOM);
        swt_radio_mode.setEnabled(stream_quality >= AppConstant.QUALITY_CUSTOM);
        swt_radio_bluetooth.setEnabled(stream_quality >= AppConstant.QUALITY_CUSTOM);
        spinner_audio_src.setEnabled(stream_quality >= AppConstant.QUALITY_CUSTOM);
        spinner_sample_rate.setEnabled(stream_quality >= AppConstant.QUALITY_CUSTOM);
        spinner_adaptive.setEnabled(stream_quality >= AppConstant.QUALITY_CUSTOM);
        swt_adaptive_fps.setEnabled(stream_quality >= AppConstant.QUALITY_CUSTOM);
    }

    void onStreamLow() {
        int index = Arrays.asList(camera_sizes).indexOf("640x360");
        boolean isUSBOpen = AppPreference.getBool(AppPreference.KEY.IS_USB_OPENED, false);
        if (isUSBOpen) {
            if (index < 0) {
                int resolution = AppPreference.getInt(AppPreference.KEY.STREAMING_RESOLUTION, 0);
                List<String> sortedResolutions = sharedViewModel.getCameraResolution();
                if (resolution >= sortedResolutions.size()) {
                    resolution = 0;
                    AppPreference.setInt(AppPreference.KEY.STREAMING_RESOLUTION, resolution);
                }
                index = resolution;
            }
        }
        AppPreference.setInt(AppPreference.KEY.STREAMING_RESOLUTION, index); // 640x480
        streaming_resolution.setSelection(index);
        AppPreference.setInt(AppPreference.KEY.STREAMING_FRAME, 15);
        setStreamFPS(15);
        AppPreference.setInt(AppPreference.KEY.STREAMING_BITRATE, 400);
        edt_streaming_bitrate.setText("400");
        AppPreference.setInt(AppPreference.KEY.STREAMING_KEYFRAME, 1);
        edt_streaming_keyFrame.setText("1");
        setRecordValue(1);
        AppPreference.setBool(AppPreference.KEY.IS_NATIVE_STREAMING,true);
        ly_streaming_custom.setVisibility(View.GONE);
        txt_stream_details.setText(R.string.show_details);
    }

    void onStreamSuperLow() {
        int index = Arrays.asList(camera_sizes).indexOf("320x180");
        boolean isUSBOpen = AppPreference.getBool(AppPreference.KEY.IS_USB_OPENED, false);
        if (isUSBOpen) {
            if (index < 0) {
                int resolution = AppPreference.getInt(AppPreference.KEY.STREAMING_RESOLUTION, 0);
                List<String> sortedResolutions = sharedViewModel.getCameraResolution();
                if (resolution >= sortedResolutions.size()) {
                    resolution = 0;
                    AppPreference.setInt(AppPreference.KEY.STREAMING_RESOLUTION, resolution);
                }
                index = resolution;
            }
        }
        AppPreference.setInt(AppPreference.KEY.STREAMING_RESOLUTION, index); // 640x480
        streaming_resolution.setSelection(index);
        AppPreference.setInt(AppPreference.KEY.STREAMING_FRAME, 15);
        setStreamFPS(15);
        AppPreference.setInt(AppPreference.KEY.STREAMING_BITRATE, 200);
        edt_streaming_bitrate.setText("200");
        AppPreference.setInt(AppPreference.KEY.STREAMING_KEYFRAME, 1);
        edt_streaming_keyFrame.setText("1");
        setRecordValue(2);
        AppPreference.setBool(AppPreference.KEY.IS_NATIVE_STREAMING,true);
        ly_streaming_custom.setVisibility(View.GONE);
        txt_stream_details.setText(R.string.show_details);
    }

    public void onStreamMedium() {
        int index = Arrays.asList(camera_sizes).indexOf("960x540");
        boolean isUSBOpen = AppPreference.getBool(AppPreference.KEY.IS_USB_OPENED, false);
        if (isUSBOpen) {
            if (index < 0) {
                int resolution = AppPreference.getInt(AppPreference.KEY.STREAMING_RESOLUTION, 0);
                List<String> sortedResolutions = sharedViewModel.getCameraResolution();
                if (resolution >= sortedResolutions.size()) {
                    resolution = 0;
                    AppPreference.setInt(AppPreference.KEY.STREAMING_RESOLUTION, resolution);
                }
                index = resolution;
            }
        }
        AppPreference.setInt(AppPreference.KEY.STREAMING_RESOLUTION, index); // 720x480
        streaming_resolution.setSelection(index);
        AppPreference.setInt(AppPreference.KEY.STREAMING_FRAME, 30);
        setStreamFPS(30);
        AppPreference.setInt(AppPreference.KEY.STREAMING_BITRATE, 1024);
        edt_streaming_bitrate.setText("1024");
        AppPreference.setInt(AppPreference.KEY.STREAMING_KEYFRAME, 1);
        edt_streaming_keyFrame.setText("1");
        setRecordValue(0);
        AppPreference.setBool(AppPreference.KEY.IS_NATIVE_STREAMING,true);
        ly_streaming_custom.setVisibility(View.GONE);
        txt_stream_details.setText(R.string.show_details);
    }

    void onStreamHigh() {
        int index = Arrays.asList(camera_sizes).indexOf("1280x720");
        boolean isUSBOpen = AppPreference.getBool(AppPreference.KEY.IS_USB_OPENED, false);
        if (isUSBOpen) {
            if (index < 0) {
                int resolution = AppPreference.getInt(AppPreference.KEY.STREAMING_RESOLUTION, 0);
                List<String> sortedResolutions = sharedViewModel.getCameraResolution();
                if (resolution >= sortedResolutions.size()) {
                    resolution = 0;
                    AppPreference.setInt(AppPreference.KEY.STREAMING_RESOLUTION, resolution);
                }
                index = resolution;
            }
        }
        AppPreference.setInt(AppPreference.KEY.STREAMING_RESOLUTION, index);
        streaming_resolution.setSelection(index);
        AppPreference.setInt(AppPreference.KEY.STREAMING_FRAME, 30);
        setStreamFPS(30);
        AppPreference.setInt(AppPreference.KEY.STREAMING_BITRATE, 1496);
        edt_streaming_bitrate.setText("1496");
        AppPreference.setInt(AppPreference.KEY.STREAMING_KEYFRAME, 1);
        edt_streaming_keyFrame.setText("1");
        setRecordValue(0);
        AppPreference.setBool(AppPreference.KEY.IS_NATIVE_STREAMING,true);
        ly_streaming_custom.setVisibility(View.GONE);
        txt_stream_details.setText(R.string.show_details);
    }

    void onStreamUSBHigh() {
        if (sharedViewModel != null) {
            List<String> sortedResolutions = sharedViewModel.getCameraResolution();
            if (sortedResolutions.isEmpty()) {
                //   return;
                Toast.makeText(getContext(), "USB Device not connected", Toast.LENGTH_SHORT).show();
            }
            int index = Arrays.asList(camera_sizes).indexOf("1280x720");
            if (index < 0) {
                index = Arrays.asList(camera_sizes).indexOf("640x480");
            }
            if (index < 0) {
                index = Arrays.asList(camera_sizes).indexOf("640x360");
            }
            if (index < 0) {
                index = Arrays.asList(camera_sizes).indexOf("320x180");
            }
            AppPreference.setInt(AppPreference.KEY.STREAMING_RESOLUTION, index);
            streaming_resolution.setSelection(index);
            AppPreference.setInt(AppPreference.KEY.STREAMING_FRAME, 30);
            setStreamFPS(30);
            AppPreference.setInt(AppPreference.KEY.STREAMING_BITRATE, 1496);
            edt_streaming_bitrate.setText("1496");
            AppPreference.setInt(AppPreference.KEY.STREAMING_KEYFRAME, 1);
            edt_streaming_keyFrame.setText("1");
            setRecordValue(4);
            ly_streaming_custom.setVisibility(View.GONE);
            txt_stream_details.setText(R.string.show_details);
        }
    }

    int wifi_type = 0;

    void scanWifiQR(int camera_type) {
        mListener.isDialog(true);
//        LiveFragment.instance.captureView.Close();
        Intent i = new Intent(getContext(), QrCodeActivity.class);
        wifi_type = camera_type;
        startActivityForResult(i, REQUEST_CODE_QR_SCAN_WIFI);
    }

    void scanWifiList(int camera_type) {
        if (statusCheck()) {
            WifiListDialog dlg = new WifiListDialog(requireContext());
            dlg.setResultListener(ssid -> connectWIFI(ssid, camera_type, ""));
            mListener.isDialog(true);
            dlg.show();
        } else {
            buildAlertMessageNoGps();
        }
    }

    public boolean statusCheck() {
        if (!isAdded() || getActivity() == null) {
            Log.e("StreamingFragment", "Fragment detached, skipping onLogin callback");
            return false;
        }
        final LocationManager manager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
            return false;
        }
        return true;
    }

    private void buildAlertMessageNoGps() {
        MessageDialog messageDialog = MessageDialog
                .show(getString(R.string.confirmation_title), "Your GPS seems to be disabled, do you want to enable it?", "Yes","No")                .setCancelButton(new OnDialogButtonClickListener<MessageDialog>() {
                    @Override
                    public boolean onClick(MessageDialog dialog, View v) {
                        dialog.dismiss();
                        return true;
                    }
                }).setOkButton(new OnDialogButtonClickListener<MessageDialog>() {
                    @Override
                    public boolean onClick(MessageDialog baseDialog, View v) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        baseDialog.dismiss();
                        return true;
                    }
                });
        messageDialog.setOkTextInfo(new TextInfo().setFontColor(Color.parseColor("#000000")).setBold(true));
        messageDialog.setCancelTextInfo(new TextInfo().setFontColor(Color.parseColor("#000000")).setBold(true));
    }

    void addWifi(int wifi_type) {
        mListener.isDialog(true);
        WifiDialog dlg = new WifiDialog(requireContext());
        dlg.setOnSetResultListener(ssid -> connectWIFI(ssid, wifi_type, ""));
        dlg.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // Check if it's the QR scan result
        if (resultCode == RESULT_OK && data != null && requestCode == REQUEST_CODE_QR_SCAN_WIFI) {
            String ssid = data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult");
            connectWIFI(ssid, wifi_type, "");
            wifi_type = 0;
        }
        // Check if it's the storage chooser result (SAF)
        else if (requestCode == REQUEST_CODE_PICK_FOLDER && resultCode == Activity.RESULT_OK && data != null) {
            Uri mSelectedFolderUri = data.getData();
            AppPreference.setStr(AppPreference.KEY.GALLERY_PATH, mSelectedFolderUri.toString());
            txt_storage.setText(getFullPathFromTreeUri(mSelectedFolderUri));
            showSelectedStorage(mSelectedFolderUri);
            // Persist permissions across device reboots.
            final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            mActivity.getContentResolver().takePersistableUriPermission(mSelectedFolderUri, takeFlags);
        }
        else if (requestCode == REQUEST_CODE_MOVE_FILES && resultCode == RESULT_OK && data != null) {
            sourceFileUri = data.getData(); // Get the URI of the selected file

            if (sourceFileUri != null && "content".equals(sourceFileUri.getScheme())) {
                // Open the destination file picker
                openDestinationFilePicker(); // Call to open destination file picker
            } else {
                Toast.makeText(requireContext(), "Invalid source file URI", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CODE_SAVE_FILE && resultCode == RESULT_OK && data != null) {
            destinationFileUri = data.getData(); // Get the URI where the user wants to save the file

            if (destinationFileUri != null) {
                try {
                    // Create a temporary file to hold the content
                    if (!isAdded() || getActivity() == null) {
                        Log.e("StreamingFragment", "Fragment detached, skipping onLogin callback");
                        return;
                    }
                    File tempFile = File.createTempFile("temp_", ".extension", getActivity().getExternalFilesDir(null)); // Change the extension as needed
                    // Copy the content from the source URI to the temp file
                    copyUriToFile(sourceFileUri, tempFile);
                    // Copy from tempFile to the user-defined destination URI
                    copyFile(tempFile, destinationFileUri);
                    // Optionally delete the temporary file
                    tempFile.delete();

                    Toast.makeText(requireContext(), "File moved successfully to " + destinationFileUri.toString(), Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(requireContext(), "Error moving file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "Invalid destination file URI", Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showSelectedStorage(Uri treeUri) {
        String uriString = treeUri.toString();
        AppPreference.setStr(AppPreference.KEY.STORAGE_LOCATION, uriString);

        String docId = DocumentsContract.getTreeDocumentId(treeUri);
        String[] split = docId.split(":");
        String volumeId = split[0]; // This is "primary" for internal storage, or a UUID for external.

        // Check if the storage is internal.
        if ("primary".equalsIgnoreCase(volumeId)) {
            tv_storage_location.setText("Storage Location: Phone Storage");
            AppPreference.setStr(AppPreference.KEY.IS_STORAGE_INTERNAL, "INTERNAL STORAGE");
            AppPreference.setStr(AppPreference.KEY.Storage_Type, "Storage Location: Phone Storage");
        } else {
            // For non-primary storage, use StorageManager/StorageVolume.
            StorageManager storageManager = (StorageManager) mActivity.getSystemService(Context.STORAGE_SERVICE);
            boolean found = false;
            if (storageManager != null) {
                List<StorageVolume> volumes = storageManager.getStorageVolumes();
                for (StorageVolume volume : volumes) {
                    // For non-primary volumes, volume.getUuid() should match the volumeId.
                    String uuid = volume.getUuid(); // May be null for internal storage.
                    if (uuid != null && uuid.equals(volumeId)) {
                        // Use the volume description to guess the type.
                        String description = volume.getDescription(mActivity);
                        if (description != null) {
                            description = description.toLowerCase();
                            if (description.contains("usb") || description.contains("otg") ||
                                    description.contains("mass storage")) {
                                tv_storage_location.setText("Storage Location: USB Storage");
                                AppPreference.setStr(AppPreference.KEY.IS_STORAGE_EXTERNAL, "EXTERNAL");
                                AppPreference.setStr(AppPreference.KEY.Storage_Type, "Storage Location: USB Storage");
                                found = true;
                                break;
                            } else if (description.contains("sd")) {
                                tv_storage_location.setText("Storage Location: SDCARD Storage");
                                AppPreference.setStr(AppPreference.KEY.IS_STORAGE_SDCARD, "SDCARD");
                                AppPreference.setStr(AppPreference.KEY.Storage_Type, "Storage Location: SDCARD Storage");
                                found = true;
                                break;
                            }
                        }
                    }
                }
            }else {
                tv_storage_location.setText("Storage Location: EXTERNAL Storage");
                AppPreference.setStr(AppPreference.KEY.IS_STORAGE_EXTERNAL, "EXTERNAL");
                AppPreference.setStr(AppPreference.KEY.Storage_Type, "Storage Location: External Storage");
            }
        }
    }

    public String getFullPathFromTreeUri(Uri treeUri) {
        if (treeUri == null || treeUri.toString().isEmpty() == true) return null;
        if (treeUri.toString().contains("/0/")) {
            return treeUri.toString();
        }
        String docId = DocumentsContract.getTreeDocumentId(treeUri);
        String[] split = docId.split(":");
        String storageType = split[0];
        String relativePath = "";

        if (split.length >= 2) {
            relativePath = split[1];
        }
        return "/storage/" + storageType + "/" + relativePath;
    }

    private void copyUriToFile(Uri uri, File dst) throws IOException {
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(dst)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
    }

    private void copyFile(File src, Uri dst) throws IOException {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = requireContext().getContentResolver().openOutputStream(dst)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }

    // Helper method to extract the file name from the Uri
    private void openDirectoryPicker() {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // All file types
        startActivityForResult(intent, REQUEST_CODE_MOVE_FILES);

    }

    private void openDestinationFilePicker() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // Set the type of files the user can create
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "video_" + timestamp + ".mp4";
        intent.putExtra(Intent.EXTRA_TITLE, fileName); // Suggest a filename
        startActivityForResult(intent, REQUEST_CODE_SAVE_FILE);
    }

    void connectWIFI(String ssid, int camera_type, String password) {
        if (TextUtils.isEmpty(password)) {
            password = "12345678";
            if (camera_type == AppConstant.WIFI_TYPE_LAWMATE) {
                password = "88888888";
            }
        }
        mListener.isDialog(true);
        TextProgressDialog dlg = new TextProgressDialog(requireContext(), "Adding Camera...");
        dlg.show();
        String finalPassword = password;
//        WifiUtils.withContext(RecordApp.getContext())
        WifiUtils.withContext(requireContext())
                .connectWith(ssid, password)
                .setTimeout(15000)
                .onConnectionResult(new ConnectionSuccessListener() {
                    @Override
                    public void success() {
                        dlg.dismiss();
                        mListener.isDialog(true);
//                        Camera camera = new Camera("", "", 554, "main_ch", "", "", ssid, AppConstant.RTSP_UDP, "wlan0", "wlan0");
                        new CameraDialog(requireContext(), camera_type, ssid, finalPassword).show();

                        NetworkRequest.Builder builder;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            builder = new NetworkRequest.Builder();
                            //set the transport type do WIFI
                            builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
                            ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                            connectivityManager.requestNetwork(builder.build(), new ConnectivityManager.NetworkCallback() {
                                @Override
                                public void onAvailable(Network network) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        if (Build.VERSION.RELEASE.equalsIgnoreCase("6.0")) {
                                            if (!Settings.System.canWrite(requireContext())) {
                                                Intent goToSettings = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                                goToSettings.setData(Uri.parse("package:" + getContext().getPackageName()));
                                                mListener.isDialog(true);
                                                getContext().startActivity(goToSettings);
                                            }
                                        }
                                        connectivityManager.bindProcessToNetwork(null);
//                                        if (mSsid.contains("my_iot_device-xxxxxxxxx")) {
                                        connectivityManager.bindProcessToNetwork(network);
//                                        } else {

//                                        }
                                    } else {
                                        //This method was deprecated in API level 23
                                        ConnectivityManager.setProcessDefaultNetwork(null);
//                                        if (mSsid.contains("my_iot_device-xxxxxxxxx")) {
                                        ConnectivityManager.setProcessDefaultNetwork(network);
//                                        } else {
//
//                                        }
                                    }
                                    try {
                                        //do a callback or something else to alert your code that it's ok to send the message through socket now
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    connectivityManager.unregisterNetworkCallback(this);
                                }
                            });
                        }
                    }

                    @Override
                    public void failed(@NonNull ConnectionErrorCode errorCode) {
                        try {
                            dlg.dismiss();
                            MessageUtil.showToast(requireContext(), R.string.connection_fail);
                            mListener.isDialog(false);
                            getWifiPassword(ssid, camera_type);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
    }

    void getWifiPassword(String ssid, int camera_type) {
        mListener.isDialog(true);
        WifiPasswordDialog serialDialog = new WifiPasswordDialog(requireContext());
        serialDialog.setOkListener(view -> {
            mListener.isDialog(false);
            serialDialog.dismiss();
            String password = serialDialog.edt_password.getText().toString();
            connectWIFI(ssid, camera_type, password);
        });
        serialDialog.setCloseListener(view -> {
            serialDialog.dismiss();
            mListener.isDialog(false);
        });
        serialDialog.show();
    }

    void exitApp() {
        mListener.isDialog(true);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.exit_app);
        builder.setMessage(R.string.exit_message);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.isDialog(false);
                dialog.dismiss();
                if (!isAdded() || getActivity() == null) {
                    Log.e("StreamingFragment", "Fragment detached, skipping onLogin callback");
                    return;
                }
                getActivity().finish();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                mListener.isDialog(false);
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

// Change button text color to black
        Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(Color.BLACK); // Set positive button text color to black
        }

        if (negativeButton != null) {
            negativeButton.setTextColor(Color.BLACK); // Set negative button text color to black
        }
    }

    void checkUpdate() {
        mListener.isDialog(true);
        if (!DeviceUtils.isNetworkAvailable(requireContext())) {
            return;
        }
        mListener.showDialog();
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("Version", CommonUtil.getVersionCode(requireContext()));
        HttpApiService.getHttpApiEndPoint().checkVersion(hashMap).enqueue(new Callback<Responses.VersionResponse>() {
            @Override
            public void onResponse(Call<Responses.VersionResponse> call, Response<Responses.VersionResponse> response) {
                mListener.isDialog(false);
                mListener.dismissDialog();
                if (response.isSuccessful() && response.body() != null && !TextUtils.isEmpty(response.body().url)) {
                    float version = Float.valueOf(response.body().version);
                    Toast.makeText(requireContext(), "to:"+response.body().date, Toast.LENGTH_SHORT).show();
                    float app_version = Float.valueOf(CommonUtil.getVersionCode(requireContext()));
                    if (version > app_version) {
                        AppPreference.setStr(AppPreference.KEY.APP_VERSION, response.body().version);
                        AppPreference.setStr(AppPreference.KEY.APP_URL, response.body().url);
                        MessageDialog messageDialog = MessageDialog
                                .show(getString(R.string.update_available), getString(R.string.confirm_update), getString(R.string.update),getString(R.string.cancel))                                .setCancelButton(new OnDialogButtonClickListener<MessageDialog>() {
                                    @Override
                                    public boolean onClick(MessageDialog dialog, View v) {
                                        dialog.dismiss();
                                        return true;
                                    }
                                }).setOkButton(new OnDialogButtonClickListener<MessageDialog>() {
                                    @Override
                                    public boolean onClick(MessageDialog baseDialog, View v) {
                                        mActivity.updateApp(response.body().url);
                                        baseDialog.dismiss();
                                        return true;
                                    }
                                });
                        messageDialog.setOkTextInfo(new TextInfo().setFontColor(Color.parseColor("#000000")).setBold(true));
                        messageDialog.setCancelTextInfo(new TextInfo().setFontColor(Color.parseColor("#000000")).setBold(true));

                    } else {
                        MessageUtil.showToast(requireContext(), R.string.no_update);
                    }
                } else {
                    MessageUtil.showToast(requireContext(), R.string.no_update);
                }
            }

            @Override
            public void onFailure(Call<Responses.VersionResponse> call, Throwable t) {
                mListener.dismissDialog();
                mListener.isDialog(false);
                MessageUtil.showToast(requireContext(), R.string.no_update);
                AppPreference.removeKey(AppPreference.KEY.APP_VERSION);
            }
        });
    }

    void onReactivate() {
        mListener.isDialog(true);
        MessageDialog messageDialog = MessageDialog
                .show(getString(R.string.warning_new), getString(R.string.reactivate_message), getString(R.string.Okay),getString(R.string.cancel))                .setCancelButton(new OnDialogButtonClickListener<MessageDialog>() {
                    @Override
                    public boolean onClick(MessageDialog dialog, View v) {
                        dialog.dismiss();
                        return true;
                    }
                }).setOkButton(new OnDialogButtonClickListener<MessageDialog>() {
                    @Override
                    public boolean onClick(MessageDialog baseDialog, View v) {
                        baseDialog.dismiss();
                        AppPreference.removeKey(AppPreference.KEY.ACTIVATION_SERIAL);
                        AppPreference.removeKey(AppPreference.KEY.ACTIVATION_CODE);
                        startActivity(new Intent(requireContext(), SplashActivity.class));
                        requireActivity().finish();
                        return true;
                    }
                });
        messageDialog.setOkTextInfo(new TextInfo().setFontColor(Color.parseColor("#000000")).setBold(true));
        messageDialog.setCancelTextInfo(new TextInfo().setFontColor(Color.parseColor("#000000")).setBold(true));

    }

    @Override
    public void onRefresh() {

    }

    public class ListAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        public ListAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        public int getCount() {
            if (cameraList == null) {
                return 0;
            }
            return cameraList.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public class ViewHolder {
            @Nullable
            TextView txt_name;

            @Nullable
            Switch swt_camera;

            @Nullable
            ImageView ic_edit;

            @Nullable
            ImageView ic_delete;
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            Camera camera = cameraList.get(position);
            final ViewHolder holder;
            if (convertView == null) {
                if (camera.camera_type == Camera.TYPE.WIFI_CAMERA) {
                    convertView = mInflater.inflate(R.layout.row_wifi_camera, parent, false);
                } else {
                    convertView = mInflater.inflate(R.layout.row_camera, parent, false);
                }
                holder = new ViewHolder();
                
                // Initialize ViewHolder UI elements
                holder.txt_name = convertView.findViewById(R.id.txt_name);
                holder.swt_camera = convertView.findViewById(R.id.swt_camera);
                holder.ic_edit = convertView.findViewById(R.id.ic_edit);
                holder.ic_delete = convertView.findViewById(R.id.ic_delete);
                
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.txt_name.setText(camera.camera_name);
            if (holder.swt_camera != null) {
                holder.swt_camera.setChecked(camera.camera_enable);
                holder.swt_camera.setOnCheckedChangeListener((compoundButton, b) -> {
                    if (position == 0) { // front camera
                        if (!AppPreference.getBool(AppPreference.KEY.CAM_REAR_FACING, true)) {
                            MessageUtil.showToast(requireContext(), R.string.no_cameras);
                            compoundButton.setChecked(true);
                            return;
                        }
                    } else if (position == 1) { // rear camera
                        if (!AppPreference.getBool(AppPreference.KEY.CAM_FRONT_FACING, true)) {
                            MessageUtil.showToast(requireContext(), R.string.no_cameras);
                            compoundButton.setChecked(true);
                            return;
                        }
                    } else if (position == 2) { // usb camera
                        AppPreference.setBool(AppPreference.KEY.CAM_USB, b);
                        AppPreference.removeKey(AppPreference.KEY.SELECTED_POSITION);
                        compoundButton.setChecked(b);
                    } else if (position == 3) {
                        AppPreference.setBool(AppPreference.KEY.CAM_CAST, b);
                    } else if (position == 4) {
                        AppPreference.setBool(AppPreference.KEY.AUDIO_ONLY, b);
                    }
                    new Handler().postDelayed(() -> mListener.fragUpdateMenu(false), 100);
                });
            }

            if (holder.ic_edit != null) {
                holder.ic_edit.setOnClickListener(view -> {
                    mListener.isDialog(true);
                    new CameraDialog(requireContext(), camera).show();
                });
            }
            if (holder.ic_delete != null) {
                holder.ic_delete.setOnClickListener(view -> {
                    mListener.isDialog(true);
                    MessageDialog messageDialog = MessageDialog
                            .show(getString(R.string.delete_camera), getString(R.string.confirm_delete_camera), getString(R.string.delete),getString(R.string.cancel)).setCancelButton(new OnDialogButtonClickListener<MessageDialog>() {
                                @Override
                                public boolean onClick(MessageDialog dialog, View v) {
                                    dialog.dismiss();
                                    return true;
                                }
                            }).setOkButton(new OnDialogButtonClickListener<MessageDialog>() {
                                @Override
                                public boolean onClick(MessageDialog baseDialog, View v) {
                                    AppPreference.removeKey(AppPreference.KEY.SELECTED_POSITION);
                                    mListener.stopFragWifiService();
                                    DBManager.getInstance().deleteCamera(camera.id);
                                    initialize();
                                    mListener.fragUpdateMenu(true);
                                    baseDialog.dismiss();
                                    return true;
                                }
                            });
                    messageDialog.setOkTextInfo(new TextInfo().setFontColor(Color.parseColor("#000000")).setBold(true));
                    messageDialog.setCancelTextInfo(new TextInfo().setFontColor(Color.parseColor("#000000")).setBold(true));
                });
            }
            return convertView;
        }
    }

    public void hideSettings(boolean visible) {
        ly_recording_settings.setVisibility(visible ? View.VISIBLE : View.GONE);
//        ly_streaming_settings.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    void enterEncryptionKey() {
        EncryptionDialog codeDialog = new EncryptionDialog(requireContext());
        mListener.isDialog(true);
        codeDialog.setCloseListener(view -> {
            codeDialog.dismiss();
            mListener.isDialog(false);
            AppPreference.setBool(AppPreference.KEY.FILE_ENCRYPTION, false);
            if (swt_encryption != null) {
                swt_encryption.setChecked(false);
            }
            MessageUtil.showToast(requireContext(), "File encryption is not enabled.");
        });
        codeDialog.setOkListener(view -> {
            codeDialog.dismiss();
            mListener.isDialog(false);
            String encryption_code = codeDialog.edt_code.getText().toString();
            if (!TextUtils.isEmpty(encryption_code)) {
                AppPreference.setStr(AppPreference.KEY.ENCRYPTION_KEY, encryption_code);
                AppPreference.setBool(AppPreference.KEY.FILE_ENCRYPTION, true);
                if (swt_encryption != null) {
                    swt_encryption.setChecked(true);
                }
            } else {
                AppPreference.setBool(AppPreference.KEY.FILE_ENCRYPTION, false);
                if (swt_encryption != null) {
                    swt_encryption.setChecked(false);
                }
                MessageUtil.showToast(requireContext(), "Encryption will be not working without key.");
            }
        });
        codeDialog.show();
    }

    // Initialize default values and field states
    private void initializeDefaultValues() {
        // Set default streaming quality to Medium (index 1)
        if (streaming_quality != null) {
            streaming_quality.setSelection(1);
        }
        
        // Set default video quality to High (index 0)
        if (spinner_quality != null) {
            spinner_quality.setSelection(0);
        }
        
        // Initially disable custom fields
        enableStreamingFields(false);
        enableVideoFields(false);
        
        // Set default text for show details buttons
        if (txt_stream_details != null) {
            txt_stream_details.setText(R.string.show_details);
        }
        if (txt_video_details != null) {
            txt_video_details.setText(R.string.show_details);
        }
    }

    // Helper methods for enabling/disabling fields and setting defaults
    private void enableStreamingFields(boolean enable) {
        if (streaming_resolution != null) {
            streaming_resolution.setEnabled(enable);
        }
        if (streaming_frame != null) {
            streaming_frame.setEnabled(enable);
        }
        if (streaming_audio_bitrate != null) {
            streaming_audio_bitrate.setEnabled(enable);
        }
        if (spinner_audio_src != null) {
            spinner_audio_src.setEnabled(enable);
        }
        if (spinner_sample_rate != null) {
            spinner_sample_rate.setEnabled(enable);
        }
        if (spinner_adaptive != null) {
            spinner_adaptive.setEnabled(enable);
        }
    }

    private void enableVideoFields(boolean enable) {
        if (spinner_resolution != null) {
            spinner_resolution.setEnabled(enable);
        }
        if (spinner_frame != null) {
            spinner_frame.setEnabled(enable);
        }
    }

    private void resetStreamingToDefaults() {
        // Set streaming quality to Medium (index 1)
        if (streaming_quality != null) {
            streaming_quality.setSelection(1);
        }
        // Reset other fields to default values
        if (streaming_resolution != null) {
            streaming_resolution.setSelection(0);
        }
        if (streaming_frame != null) {
            streaming_frame.setSelection(0);
        }
        if (streaming_audio_bitrate != null) {
            streaming_audio_bitrate.setSelection(0);
        }
        if (spinner_audio_src != null) {
            spinner_audio_src.setSelection(0);
        }
        if (spinner_sample_rate != null) {
            spinner_sample_rate.setSelection(0);
        }
        if (spinner_adaptive != null) {
            spinner_adaptive.setSelection(0);
        }
    }

    private void resetVideoToDefaults() {
        // Set video quality to High (index 0)
        if (spinner_quality != null) {
            spinner_quality.setSelection(0);
        }
        // Reset other fields to default values
        if (spinner_resolution != null) {
            spinner_resolution.setSelection(0);
        }
        if (spinner_frame != null) {
            spinner_frame.setSelection(0);
        }
    }
}
