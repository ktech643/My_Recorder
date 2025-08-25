package com.checkmate.android.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.blikoon.qrcodescanner.QrCodeActivity;
import com.checkmate.android.AppConstant;
import com.checkmate.android.AppPreference;
import com.checkmate.android.R;
import com.checkmate.android.model.Device;
import com.checkmate.android.model.Invite;
import com.checkmate.android.model.PushChannel;
import com.checkmate.android.model.RegisteredDevice;
import com.checkmate.android.networking.Responses;
import com.checkmate.android.networking.RestApiService;
import com.checkmate.android.service.LocationManagerService;
import com.checkmate.android.service.SharedEGL.SharedEglManager;
import com.checkmate.android.ui.activity.PinActivity;
import com.checkmate.android.ui.activity.ShareActivity;
import com.checkmate.android.ui.activity.WebActivity;
import com.checkmate.android.ui.dialog.InviteDialog;
import com.checkmate.android.ui.dialog.NameDialog;
import com.checkmate.android.ui.dialog.PasswordDialog;
import com.checkmate.android.ui.dialog.RePasswordDialog;
import com.checkmate.android.ui.dialog.StreamingModeBottomSheet;
import com.checkmate.android.util.CommonUtil;
import com.checkmate.android.util.DateTimeUtils;
import com.checkmate.android.util.DeviceUtils;
import com.checkmate.android.util.MainActivity;
import com.checkmate.android.util.MessageUtil;
import com.checkmate.android.util.InternalLogger;
import com.checkmate.android.util.ANRSafeHelper;
import com.checkmate.android.util.CriticalComponentsMonitor;
import com.checkmate.android.viewmodels.EventType;
import com.checkmate.android.viewmodels.SharedViewModel;
import com.kongzue.dialogx.dialogs.MessageDialog;
import com.kongzue.dialogx.interfaces.OnDialogButtonClickListener;
import com.kongzue.dialogx.util.TextInfo;
import com.wmspanel.libstream.Streamer;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.app.Activity.RESULT_OK;

public class StreamingFragment extends BaseFragment {

    private static final String TAG = "StreamingFragment";
    private final int REQUEST_CODE_QR_SCAN = 101;
    public static StreamingFragment instance;

    public static StreamingFragment getInstance() {
        return ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
            return instance;
        }, null);
    }

    MainActivity mActivity;
    Responses.LoginResponse user_info;

    // Old RTSP fields (still in layout but hidden)
    
    // UI Components
    private SwipeRefreshLayout swipe_refresh;
    private LinearLayout view_stream;
    private Button button_mode;
    private TextView txt_speed_test;
    private EditText edt_name;
    private TextView txt_update;
    private SwitchCompat swt_streaming;
    private SwitchCompat swt_recording;
    private LinearLayout row_gps;
    private SwitchCompat swt_gps;
    private LinearLayout row_frequency;
    private Button button_frequency;
    private LinearLayout local_fields_container;
    private EditText edt_server_ip;
    private EditText edt_port;
    private EditText edt_local_user;
    private EditText edt_local_password;
    private EditText edt_local_channel;
    private TextView local_path_value;
    private Button btn_local_update;
    private TextView txt_status;
    private TextView txt_account_type;
    private TextView txt_licenses;
    private TextView txt_user;
    private TextView txt_change_password;
    private Button btn_refresh;
    private Button btn_logout;
    private androidx.cardview.widget.CardView view_share;
    private ListView list_share;
    private LinearLayout ly_share;
    private EditText edt_url;
    private TextView txt_qr;
    private EditText edt_channel;
    private Button btn_start;
    private TextView txt_speed;
    private Button btn_share;
    private androidx.cardview.widget.CardView view_login;
    private ImageView btn_back;
    private TextView txt_login;
    private LinearLayout ly_username;
    private EditText edt_username;
    private LinearLayout ly_password;
    private EditText edt_password;
    private Button btn_login;
    private Button btn_code;
    private Button btn_send;
    private Button btn_password;
    private Button btn_new_user;
    private Button btn_reset_password;
    private Button spinner_mode;

    // Username/password for Cloud login

    // Spinner for Cloud vs. Local streaming

    // Rows hidden when in Local mode
    ListAdapter adapter;
    List<Invite> mDataList = new ArrayList<>();
    private ActivityFragmentCallbacks mListener;
    private SharedViewModel sharedViewModel;

    private final String TAG = "StreamingFragment";
    private View rootView;

    // Handler for location updates
    Handler mHandler = null;

    Runnable mTimer = new Runnable() {
        @Override
        public void run() {
            updateDeviceInfo(
                    AppPreference.getStr(AppPreference.KEY.DEVICE_NAME, ""),
                    String.valueOf(LocationManagerService.lat),
                    String.valueOf(LocationManagerService.lng),
                    MainActivity.isStreaming,
                    false
            );
            int mins = AppPreference.getInt(AppPreference.KEY.FREQUENCY_MIN, 1);
            mHandler.postDelayed(this, (long) mins * 60 * 1000);
        }
    };

    public static StreamingFragment newInstance() {
        return new StreamingFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        instance = this;
        try {
            mActivity = MainActivity.instance;
        } catch (NullPointerException e) {
            new Handler().postDelayed(() -> {
                try {
                    mActivity = MainActivity.instance;
                } catch (NullPointerException e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
            }, 2500);
        }

        if (context instanceof ActivityFragmentCallbacks) {
            mListener = (ActivityFragmentCallbacks) context;
        } else {
            Log.e("StreamingFragment", "Parent activity does not implement ActivityFragmentCallbacks");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Ensure mActivity is properly initialized
        if (mActivity == null) {
            mActivity = MainActivity.instance;
        }

        if (mActivity != null && isAdded()) {
            sharedViewModel = new ViewModelProvider(getActivity()).get(SharedViewModel.class);
            // Observe LiveData events
            sharedViewModel.getEventLiveData().observe(getViewLifecycleOwner(), event -> {
                if (event != null) {
                    SharedViewModel.EventPayload payload = event.getContentIfNotHandled();
                    if (payload != null) {
                        handleEvent(payload);
                    }
                }
            });
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        try {
            InternalLogger.d(TAG, "StreamingFragment onAttach starting");
            CriticalComponentsMonitor.executeComponentSafely("StreamingFragment", () -> {
                super.onAttach(context);
                if (context instanceof ActivityFragmentCallbacks) {
                    mListener = (ActivityFragmentCallbacks) context;
                } else {
                    InternalLogger.e(TAG, "Context does not implement ActivityFragmentCallbacks");
                }
                InternalLogger.d(TAG, "StreamingFragment onAttach completed successfully");
            });
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error in StreamingFragment onAttach", e);
            CriticalComponentsMonitor.recordComponentError("StreamingFragment", "onAttach failed", e);
        }
    }

    @Override
    public void onDestroyView() {
        try {
            InternalLogger.d(TAG, "StreamingFragment onDestroyView starting");
            super.onDestroyView();
            rootView = null; // Avoid memory leaks
            InternalLogger.d(TAG, "StreamingFragment onDestroyView completed successfully");
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error in StreamingFragment onDestroyView", e);
        }
    }
    
    @Override
    public void onDestroy() {
        try {
            InternalLogger.d(TAG, "StreamingFragment onDestroy starting");
            CriticalComponentsMonitor.executeComponentSafely("StreamingFragment", () -> {
                super.onDestroy();
                // Clean up resources
                mActivity = null;
                mListener = null;
                InternalLogger.d(TAG, "StreamingFragment onDestroy completed successfully");
            });
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error in StreamingFragment onDestroy", e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            InternalLogger.d(TAG, "StreamingFragment onCreateView starting");
            
            if (ANRSafeHelper.isNullWithLog(inflater, "LayoutInflater in StreamingFragment")) {
                return createFallbackView();
            }
            
            mView = CriticalComponentsMonitor.executeComponentSafely("StreamingFragment", () -> {
                return inflater.inflate(R.layout.fragment_streaming, container, false);
            }, null);
            
            if (ANRSafeHelper.isNullWithLog(mView, "Inflated view in StreamingFragment")) {
                return createFallbackView();
            }
        
            // Ensure mActivity is initialized safely
            if (mActivity == null) {
                mActivity = ANRSafeHelper.nullSafe(MainActivity.instance, null, "MainActivity.instance");
                if (mActivity == null) {
                    InternalLogger.w(TAG, "MainActivity instance is null in StreamingFragment");
                }
            }
            
            // Initialize UI components safely
            initializeStreamingUIComponentsSafely();
            
            InternalLogger.d(TAG, "StreamingFragment onCreateView completed successfully");
            return mView;
            
        } catch (Exception e) {
            InternalLogger.e(TAG, "Critical error in StreamingFragment onCreateView", e);
            CriticalComponentsMonitor.recordComponentError("StreamingFragment", "onCreateView failed", e);
            return createFallbackView();
        }
    }
    
    private void initializeStreamingUIComponentsSafely() {
        ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
            try {
                // Initialize UI components with null safety
                if (mView != null) {
                    swipe_refresh = ANRSafeHelper.nullSafe(mView.findViewById(R.id.swipe_refresh), null, "swipe_refresh");
                    view_stream = ANRSafeHelper.nullSafe(mView.findViewById(R.id.view_stream), null, "view_stream");
                    button_mode = ANRSafeHelper.nullSafe(mView.findViewById(R.id.button_mode), null, "button_mode");
                    txt_speed_test = ANRSafeHelper.nullSafe(mView.findViewById(R.id.txt_speed_test), null, "txt_speed_test");
                    edt_name = ANRSafeHelper.nullSafe(mView.findViewById(R.id.edt_name), null, "edt_name");
                    txt_update = ANRSafeHelper.nullSafe(mView.findViewById(R.id.txt_update), null, "txt_update");
                    swt_streaming = ANRSafeHelper.nullSafe(mView.findViewById(R.id.swt_streaming), null, "swt_streaming");
                    swt_recording = ANRSafeHelper.nullSafe(mView.findViewById(R.id.swt_recording), null, "swt_recording");
                    
                    InternalLogger.d(TAG, "Streaming UI components initialized successfully");
                }
                return true;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error initializing streaming UI components", e);
                return false;
            }
        }, false);
    }
    
    private View createFallbackView() {
        try {
            InternalLogger.w(TAG, "Creating fallback view for StreamingFragment");
            android.widget.TextView errorView = new android.widget.TextView(getContext());
            errorView.setText("Streaming view temporarily unavailable");
            errorView.setTextColor(android.graphics.Color.WHITE);
            errorView.setBackgroundColor(android.graphics.Color.BLACK);
            errorView.setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER);
            return errorView;
        } catch (Exception e) {
            InternalLogger.e(TAG, "Failed to create fallback view", e);
            return null;
        }
    
    // Continue with existing initialization  
    private void initializeRestOfUIComponents() {
            btn_local_update = ANRSafeHelper.nullSafe(mView.findViewById(R.id.btn_local_update), null, "btn_local_update");
            txt_status = ANRSafeHelper.nullSafe(mView.findViewById(R.id.txt_status), null, "txt_status");
            txt_account_type = ANRSafeHelper.nullSafe(mView.findViewById(R.id.txt_account_type), null, "txt_account_type");
            txt_licenses = ANRSafeHelper.nullSafe(mView.findViewById(R.id.txt_licenses), null, "txt_licenses");
            txt_user = ANRSafeHelper.nullSafe(mView.findViewById(R.id.txt_user), null, "txt_user");
            txt_change_password = ANRSafeHelper.nullSafe(mView.findViewById(R.id.txt_change_password), null, "txt_change_password");
            btn_refresh = ANRSafeHelper.nullSafe(mView.findViewById(R.id.btn_refresh), null, "btn_refresh");
            btn_logout = ANRSafeHelper.nullSafe(mView.findViewById(R.id.btn_logout), null, "btn_logout");
            view_share = ANRSafeHelper.nullSafe(mView.findViewById(R.id.view_share), null, "view_share");
            list_share = ANRSafeHelper.nullSafe(mView.findViewById(R.id.list_share), null, "list_share");
            ly_share = ANRSafeHelper.nullSafe(mView.findViewById(R.id.ly_share), null, "ly_share");
            edt_url = ANRSafeHelper.nullSafe(mView.findViewById(R.id.edt_url), null, "edt_url");
            txt_qr = ANRSafeHelper.nullSafe(mView.findViewById(R.id.txt_qr), null, "txt_qr");
            edt_channel = ANRSafeHelper.nullSafe(mView.findViewById(R.id.edt_channel), null, "edt_channel");
            btn_start = ANRSafeHelper.nullSafe(mView.findViewById(R.id.btn_start), null, "btn_start");
            txt_speed = ANRSafeHelper.nullSafe(mView.findViewById(R.id.txt_speed), null, "txt_speed");
            btn_share = ANRSafeHelper.nullSafe(mView.findViewById(R.id.btn_share), null, "btn_share");
            view_login = ANRSafeHelper.nullSafe(mView.findViewById(R.id.view_login), null, "view_login");
            btn_back = ANRSafeHelper.nullSafe(mView.findViewById(R.id.btn_back), null, "btn_back");
            
            // Ensure back button is properly visible
            if (btn_back != null) {
                btn_back.setVisibility(View.VISIBLE);
                btn_back.setClickable(true);
                btn_back.setFocusable(true);
            }
            
            txt_login = ANRSafeHelper.nullSafe(mView.findViewById(R.id.txt_login), null, "txt_login");
            ly_username = ANRSafeHelper.nullSafe(mView.findViewById(R.id.ly_username), null, "ly_username");
            edt_username = ANRSafeHelper.nullSafe(mView.findViewById(R.id.edt_username), null, "edt_username");
            ly_password = ANRSafeHelper.nullSafe(mView.findViewById(R.id.ly_password), null, "ly_password");
            edt_password = ANRSafeHelper.nullSafe(mView.findViewById(R.id.edt_password), null, "edt_password");
            btn_login = ANRSafeHelper.nullSafe(mView.findViewById(R.id.btn_login), null, "btn_login");
            btn_code = ANRSafeHelper.nullSafe(mView.findViewById(R.id.btn_code), null, "btn_code");
            btn_send = ANRSafeHelper.nullSafe(mView.findViewById(R.id.btn_send), null, "btn_send");
            btn_password = ANRSafeHelper.nullSafe(mView.findViewById(R.id.btn_password), null, "btn_password");
            btn_new_user = ANRSafeHelper.nullSafe(mView.findViewById(R.id.btn_new_user), null, "btn_new_user");
            btn_reset_password = ANRSafeHelper.nullSafe(mView.findViewById(R.id.btn_reset_password), null, "btn_reset_password");
            spinner_mode = ANRSafeHelper.nullSafe(mView.findViewById(R.id.button_mode), null, "spinner_mode");
            
            rootView = mView;
        }
        
        // Complete the onCreateView method by calling the rest of initialization
        try {
            initializeRestOfUIComponents();
            showInitialLogin();
        String login_email = AppPreference.getStr(AppPreference.KEY.LOGIN_EMAIL, "");
        String login_password = AppPreference.getStr(AppPreference.KEY.LOGIN_PASSWORD, "");
        if (!TextUtils.isEmpty(login_email) && !TextUtils.isEmpty(login_password)) {
            edt_username.setText(login_email);
            edt_password.setText(login_password);
            onLogin();
        } else {
            initialize();
        }

        if (mActivity != null) {
            // btn_back.setColorFilter(ContextCompat.getColor(mActivity, R.color.black));
        }

        // Store channel changes for Cloud streaming
        edt_channel.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!TextUtils.isEmpty(s.toString().trim())) {
                    AppPreference.setStr(AppPreference.KEY.STREAM_CHANNEL, s.toString().trim());
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        String channel = AppPreference.getStr(AppPreference.KEY.STREAM_CHANNEL, "");
        edt_channel.setText(channel);
        // Setup share list
        if (mActivity != null) {
            adapter = new ListAdapter(mActivity);
        }
        list_share.setAdapter(adapter);

        button_frequency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFrequencySelectionDialog();
            }
        });

        // Set up click listeners for all buttons
        btn_local_update.setOnClickListener(v -> onRefresh());
        btn_refresh.setOnClickListener(v -> onRefresh());
        btn_logout.setOnClickListener(v -> onLogout());
        btn_start.setOnClickListener(v -> onShare());
        btn_share.setOnClickListener(v -> {
            Log.d("StreamingFragment", "Share button clicked!");
            MessageUtil.showToast(getContext(), "Share button clicked!");
            onShare();
        });
        btn_back.setOnClickListener(v -> onBackPressed());
        btn_login.setOnClickListener(v -> onLogin());
        btn_code.setOnClickListener(v -> onCode());
        btn_send.setOnClickListener(v -> onSend());
        btn_password.setOnClickListener(v -> onNewUser());
        btn_new_user.setOnClickListener(v -> onExistingUser());
        btn_reset_password.setOnClickListener(v -> onResetPassword());

        int freq_min = AppPreference.getInt(AppPreference.KEY.FREQUENCY_MIN, 1);
        switch (freq_min) {
            case 1:  button_frequency.setText("1 Minute"); break;
            case 5:  button_frequency.setText("5 Minutes"); break;
            case 10: button_frequency.setText("10 Minutes"); break;
            case 15: button_frequency.setText("15 Minutes"); break;
            case 30: button_frequency.setText("30 Minutes"); break;
        }

        // Swipe refresh
        swipe_refresh.setOnRefreshListener(() -> {
            swipe_refresh.setRefreshing(false);
            refreshToken();
        });

        // Setup spinner for Cloud vs. Local
        setupViewsForConfigs();
        setupStreamingModeSpinner();
        setupLocalStreaming();
        return mView;
    }

    private void showFrequencySelectionDialog() {
        if (getContext() == null) return;
        FrequencySelectionDialogFragment dialogFragment = new FrequencySelectionDialogFragment();
        dialogFragment.setFrequencySelectionListener(new FrequencySelectionDialogFragment.FrequencySelectionListener() {
            @Override
            public void onFrequencySelected(String frequency) {
                // Handle the selected frequency here
                button_frequency.setText(frequency);
                // Optionally update another view with the selected frequency.
                String[] parts = frequency.split(" ");
                int number = Integer.parseInt(parts[0]);
                AppPreference.setInt(AppPreference.KEY.FREQUENCY_MIN, number);
            }
        });
        dialogFragment.showFrequencySelectionDialog();
    }
    public synchronized String getUniqueChannelName(Context context) {
        if (context == null) {
            return UUID.randomUUID().toString();
        }
        SharedPreferences sharedPrefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String uniqueID = sharedPrefs.getString("PREF_UNIQUE_ID", null);
        if (uniqueID == null) {
            uniqueID =  UUID.randomUUID().toString();
            sharedPrefs.edit().putString("PREF_UNIQUE_ID", uniqueID).apply();
        }
        return uniqueID;
    }
    public void setupLocalStreaming() {
        String ip = AppPreference.getStr(AppPreference.KEY.LOCAL_STREAM_IP,"172.20.1.1");
        Integer port =  AppPreference.getInt(AppPreference.KEY.LOCAL_STREAM_PORT,554);
        String username =AppPreference.getStr(AppPreference.KEY.LOCAL_STREAM_NAME,"");
        String password =AppPreference.getStr(AppPreference.KEY.LOCAL_STREAM_PASSWORD,"");//
        String wifiDirectLocalChannel =AppPreference.getStr(AppPreference.KEY.LOCAL_STREAM_CHANNEL,getUniqueChannelName(getContext()));
        edt_server_ip.setText(ip);
        edt_port.setText(String.format(Locale.getDefault(), "%d", port));
        edt_local_user.setText(username);
        edt_local_password.setText(password);
        edt_local_channel.setText(wifiDirectLocalChannel);
        String finalUrl;
        if (!TextUtils.isEmpty(username)) {
            finalUrl = String.format(Locale.getDefault(), "rtsp://%s:%s@%s:%d/%s", username, password, ip, port, wifiDirectLocalChannel);
        } else {
            finalUrl = String.format(Locale.getDefault(), "rtsp://%s:%d/%s", ip, port, wifiDirectLocalChannel);
        }
        local_path_value.setText(finalUrl);
    }

    @Override
    public void onResume() {
        try {
            InternalLogger.d(TAG, "StreamingFragment onResume starting");
            
            CriticalComponentsMonitor.executeComponentSafely("StreamingFragment", () -> {
                super.onResume();
                
                // Safe callback assignment
                if (getActivity() instanceof ActivityFragmentCallbacks) {
                    mListener = (ActivityFragmentCallbacks) getActivity();
                } else {
                    InternalLogger.e(TAG, "Activity does not implement ActivityFragmentCallbacks");
                }
                
                InternalLogger.d(TAG, "StreamingFragment onResume completed successfully");
            });
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error in StreamingFragment onResume", e);
            CriticalComponentsMonitor.recordComponentError("StreamingFragment", "onResume failed", e);
        }
    }

    void onLocalUpdateClicked() {
        // Grab the local fields...
        String ip   = edt_server_ip.getText().toString().trim();
        String port = edt_port.getText().toString().trim();
        String user = edt_local_user.getText().toString().trim();
        String pass = edt_local_password.getText().toString().trim();
        String channel = edt_local_channel.getText().toString().trim();

        // Validate / do something with them...
        if (TextUtils.isEmpty(ip)) {
            MessageUtil.showToast(getContext(), "Please enter Server IP");
            return;
        }

        if (TextUtils.isEmpty(port)) {
            MessageUtil.showToast(getContext(), "Please enter Server port");
            return;
        }

        if (!TextUtils.isEmpty(user)) {
            if (TextUtils.isEmpty(pass)) {
                MessageUtil.showToast(getContext(), "Please enter password also");
                return;
            }
        }

        if (TextUtils.isEmpty(channel)) {
            MessageUtil.showToast(getContext(), "Please enter channel name");
            return;
        }

        int portValue = Integer.parseInt(port);
        // ... and so on.
        AppPreference.setStr(AppPreference.KEY.LOCAL_STREAM_IP,ip);
        AppPreference.setInt(AppPreference.KEY.LOCAL_STREAM_PORT,portValue);
        AppPreference.setStr(AppPreference.KEY.LOCAL_STREAM_NAME,user);
        AppPreference.setStr(AppPreference.KEY.LOCAL_STREAM_PASSWORD,pass);
        AppPreference.setStr(AppPreference.KEY.LOCAL_STREAM_CHANNEL,channel);
        // Then do your local update logic...
        String finalUrl;
        String portString = Integer.toString(portValue); // Convert port to string
        if (!TextUtils.isEmpty(user)) {
            finalUrl = String.format(Locale.getDefault(), "rtsp://%s:%s@%s:%s/%s", user, pass, ip, portString, channel);
        } else {
            finalUrl = String.format(Locale.getDefault(), "rtsp://%s:%s/%s", ip, portString, channel);
        }
        local_path_value.setText(finalUrl);
        MessageUtil.showToast(getContext(), "Updating local settings with IP: " + ip);
    }

    void setupViewsForConfigs(){
       new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
           @Override
           public void run() {
               int streamingMode = AppPreference.getInt(AppPreference.KEY.STREAMING_MODE, 0);
               if (streamingMode == 1) {
                   spinner_mode.setText("Local Streaming");
                   local_fields_container.setVisibility(View.VISIBLE);
                   row_gps.setVisibility(View.GONE);
                   row_frequency.setVisibility(View.GONE);
                   view_share.setVisibility(View.GONE);
                   ly_share.setVisibility(View.GONE);
                   View txtSpeedTest = (rootView != null) ? rootView.findViewById(R.id.txt_speed_test) : null;
                   if (txtSpeedTest != null) {
                       txtSpeedTest.setVisibility(View.GONE);
                   } else {
                       Log.e("StreamingFragment", "View not available");
                   }
               } else {
                   Log.e(TAG, "drawing:");
                   spinner_mode.setText("Cloud Streaming");
                   local_fields_container.setVisibility(View.GONE);
                   row_gps.setVisibility(View.VISIBLE);
                   row_frequency.setVisibility(View.VISIBLE);
                   view_share.setVisibility(View.VISIBLE);
                   ly_share.setVisibility(View.VISIBLE);
                   View txtSpeedTest = (rootView != null) ? rootView.findViewById(R.id.txt_speed_test) : null;
                   if (txtSpeedTest != null) {
                       txtSpeedTest.setVisibility(View.GONE);
                   } else {
                       Log.e("StreamingFragment", "View not available");
                   }
                   if (txtSpeedTest != null) {
                       txtSpeedTest.setVisibility(View.VISIBLE);
                   }
               }
           }
       },1000);
    }
    // Bottom sheet logic for Cloud vs Local streaming mode selection
    private void setupStreamingModeSpinner() {
        spinner_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentMode = AppPreference.getInt(AppPreference.KEY.STREAMING_MODE, 0);
                StreamingModeBottomSheet bottomSheet = StreamingModeBottomSheet.newInstance(currentMode);
                bottomSheet.setListener(new StreamingModeBottomSheet.StreamingModeListener() {
                    @Override
                    public void onStreamingModeSelected(StreamingModeBottomSheet.StreamingModeOption option) {
                        // Handle the selected streaming mode
                        if (option.modeValue == 0) {
                            // Cloud Streaming
                            spinner_mode.setText("Cloud Streaming");
                            local_fields_container.setVisibility(View.GONE);
                            row_gps.setVisibility(View.VISIBLE);
                            row_frequency.setVisibility(View.VISIBLE);
                            view_share.setVisibility(View.VISIBLE);
                            ly_share.setVisibility(View.VISIBLE);
                            View txtSpeedTest = (rootView != null) ? rootView.findViewById(R.id.txt_speed_test) : null;
                            if (txtSpeedTest != null) {
                                txtSpeedTest.setVisibility(View.VISIBLE);
                            }
                            AppPreference.setInt(AppPreference.KEY.STREAMING_MODE, 0);
                        } else {
                            // Local Streaming
                            spinner_mode.setText("Local Streaming");
                            local_fields_container.setVisibility(View.VISIBLE);
                            row_gps.setVisibility(View.GONE);
                            row_frequency.setVisibility(View.GONE);
                            view_share.setVisibility(View.GONE);
                            ly_share.setVisibility(View.GONE);
                            View txtSpeedTest = (rootView != null) ? rootView.findViewById(R.id.txt_speed_test) : null;
                            if (txtSpeedTest != null) {
                                txtSpeedTest.setVisibility(View.GONE);
                            }
                            AppPreference.setInt(AppPreference.KEY.STREAMING_MODE, 1);
                        }
                    }
                });
                bottomSheet.show(getChildFragmentManager(), "streaming_mode_bottom_sheet");
            }
        });
    }

    // Show initial login UI - only action buttons, no input fields
    void showInitialLogin() {
        Log.d("StreamingFragment", "showInitialLogin() called");
        currentFormState = FormState.INITIAL;
        ly_username.setVisibility(View.GONE);  // Hide username field initially
        ly_password.setVisibility(View.GONE);  // Hide password field initially
        btn_login.setVisibility(View.GONE);   // Hide login button initially
        btn_password.setVisibility(View.VISIBLE);      // "I am a new user" button
        btn_new_user.setVisibility(View.VISIBLE);     // "Existing user" button  
        btn_reset_password.setVisibility(View.VISIBLE); // "Reset password" button
        btn_code.setVisibility(View.GONE);    // Hide validate code button
        btn_send.setVisibility(View.GONE);    // Hide send code button
        btn_back.setVisibility(View.GONE);    // Hide back button initially
        
        // Reset title and button text to default
        txt_login.setText(R.string.login);
        btn_login.setText(R.string.login);
        btn_send.setText(R.string.send_code);
        btn_code.setText(R.string.validate);
        
        view_login.setVisibility(View.VISIBLE);
        view_stream.setVisibility(View.GONE);
        Log.d("StreamingFragment", "Initial login UI shown - action buttons visible, input fields hidden. State: " + currentFormState);
    }

    // Show the login form - username, password, and login button
    void showLoginForm() {
        Log.d("StreamingFragment", "showLoginForm() called");
        currentFormState = FormState.LOGIN;
        ly_username.setVisibility(View.VISIBLE);
        ly_password.setVisibility(View.VISIBLE);
        btn_login.setVisibility(View.VISIBLE);
        btn_password.setVisibility(View.GONE);      // Hide "I am a new user" button
        btn_new_user.setVisibility(View.GONE);     // Hide "Existing user" button
        btn_reset_password.setVisibility(View.GONE); // Hide "Reset password" button
        btn_code.setVisibility(View.GONE);    // Hide validate code button
        btn_send.setVisibility(View.GONE);    // Hide send code button
        btn_back.setVisibility(View.VISIBLE); // Show back button
        
        // Update the title to show "Login" and button text
        txt_login.setText(R.string.login);
        btn_login.setText(R.string.login);
        
        // Debug: Check back button state and ensure it's visible
        if (btn_back != null) {
            Log.d("StreamingFragment", "Back button found, setting visibility to VISIBLE");
            btn_back.setVisibility(View.VISIBLE);
            btn_back.setClickable(true);
            btn_back.setFocusable(true);
            // Force refresh the view
            btn_back.invalidate();
            // Also ensure parent container is visible
            if (btn_back.getParent() instanceof View) {
                ((View) btn_back.getParent()).setVisibility(View.VISIBLE);
            }
        } else {
            Log.e("StreamingFragment", "Back button is null!");
        }
        
        view_login.setVisibility(View.VISIBLE);
        view_stream.setVisibility(View.GONE);
        Log.d("StreamingFragment", "Login form shown - username/password visible, login button visible. State: " + currentFormState);
    }

    // Show registration form for new users - username, password, and send code button
    void showRegisterForm() {
        Log.d("StreamingFragment", "showRegisterForm() called");
        currentFormState = FormState.REGISTER;
        ly_username.setVisibility(View.VISIBLE);
        ly_password.setVisibility(View.VISIBLE);
        btn_login.setVisibility(View.GONE);    // Hide login button
        btn_password.setVisibility(View.GONE);      // Hide "I am a new user" button
        btn_new_user.setVisibility(View.GONE);     // Hide "Existing user" button
        btn_reset_password.setVisibility(View.GONE); // Hide "Reset password" button
        btn_code.setVisibility(View.GONE);    // Hide validate code button
        btn_send.setVisibility(View.VISIBLE); // Show send code button
        btn_back.setVisibility(View.VISIBLE); // Show back button
        
        // Update the title to show "Register" and button text
        txt_login.setText(R.string.register);
        btn_send.setText(R.string.send_code);
        
        view_login.setVisibility(View.VISIBLE);
        view_stream.setVisibility(View.GONE);
        Log.d("StreamingFragment", "Registration form shown - username/password visible, send button visible. State: " + currentFormState);
    }

    // Show password form for code validation - username, password, and validate code button
    void showPasswordForm() {
        Log.d("StreamingFragment", "showPasswordForm() called");
        currentFormState = FormState.PASSWORD;
        ly_username.setVisibility(View.VISIBLE);
        ly_password.setVisibility(View.VISIBLE);
        btn_login.setVisibility(View.GONE);    // Hide login button
        btn_password.setVisibility(View.GONE);      // Hide "I am a new user" button
        btn_new_user.setVisibility(View.GONE);     // Hide "Existing user" button
        btn_reset_password.setVisibility(View.GONE); // Hide "Reset password" button
        btn_code.setVisibility(View.VISIBLE); // Show validate code button
        btn_send.setVisibility(View.GONE);    // Hide send code button
        btn_back.setVisibility(View.VISIBLE); // Show back button
        
        // Update the title to show "Validate Code" and button text
        txt_login.setText(R.string.validate_code);
        btn_code.setText(R.string.validate);
        
        view_login.setVisibility(View.VISIBLE);
        view_stream.setVisibility(View.GONE);
        Log.d("StreamingFragment", "Password form shown - username/password visible, validate code button visible. State: " + currentFormState);
    }

    // Show reset password form - username, password, and reset password functionality
    void showResetPasswordForm() {
        Log.d("StreamingFragment", "showResetPasswordForm() called");
        currentFormState = FormState.RESET_PASSWORD;
        ly_username.setVisibility(View.VISIBLE);
        ly_password.setVisibility(View.VISIBLE);
        btn_login.setVisibility(View.GONE);    // Hide login button
        btn_password.setVisibility(View.GONE);      // Hide "I am a new user" button
        btn_new_user.setVisibility(View.GONE);     // Hide "Existing user" button
        btn_reset_password.setVisibility(View.VISIBLE); // Show "Reset password" button
        btn_code.setVisibility(View.GONE);    // Hide validate code button
        btn_send.setVisibility(View.GONE);    // Hide send code button
        btn_back.setVisibility(View.VISIBLE); // Show back button
        view_login.setVisibility(View.VISIBLE);
        view_stream.setVisibility(View.GONE);
        
        // Update the title to show "Reset Password" instead of "Login"
        txt_login.setText(R.string.reset_password);
        
        Log.d("StreamingFragment", "Reset password form shown - username/password visible, reset button visible. State: " + currentFormState);
        
        // Set the reset password button to actually call the reset password functionality
        btn_reset_password.setOnClickListener(v -> {
            String email = edt_username.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                MessageUtil.showToast(mActivity, R.string.no_email);
                return;
            }
            if (!CommonUtil.isValidEmail(email)) {
                MessageUtil.showToast(mActivity, R.string.invalid_email);
                return;
            }
            // Call the actual reset password functionality
            callResetPassword();
        });
    }

    // Form state tracking
    private enum FormState {
        INITIAL,        // Initial state with action buttons
        LOGIN,          // Login form
        REGISTER,       // Registration form  
        PASSWORD,       // Password validation form
        RESET_PASSWORD  // Reset password form
    }
    
    private FormState currentFormState = FormState.INITIAL;
    
    // Back button functionality - navigate back one step in form flow
    void onBackPressed() {
        Log.d("StreamingFragment", "onBackPressed() called from state: " + currentFormState);
        
        switch (currentFormState) {
            case LOGIN:
            case REGISTER:
            case PASSWORD:
            case RESET_PASSWORD:
                // Go back to initial state
                Log.d("StreamingFragment", "Going back to initial state");
                currentFormState = FormState.INITIAL;
                ly_username.setVisibility(View.GONE);
                ly_password.setVisibility(View.GONE);
                edt_username.setText(""); // Clear username
                edt_password.setText(""); // Clear password
                showInitialLogin();
                break;
                
            case INITIAL:
            default:
                // Already at initial state, do nothing
                Log.d("StreamingFragment", "Already at initial state, doing nothing");
                break;
        }
    }

    // For "I am a new user" button click
    void onNewUser() {
        Log.d("StreamingFragment", "onNewUser() called");
        if (mActivity == null) return;
        // Show username field and registration form
        ly_username.setVisibility(View.VISIBLE);
        edt_password.setText(""); // Clear password field
        Log.d("StreamingFragment", "Calling showRegisterForm()");
        showRegisterForm();
    }

    // For "Existing user" button click
    void onExistingUser() {
        if (mActivity == null) return;
        // Show username field and login form
        ly_username.setVisibility(View.VISIBLE);
        edt_password.setText(""); // Clear password field
        showLoginForm();
    }

    // For "Reset password" button click
    void onResetPassword() {
        if (mActivity == null) return;
        // Show username field and reset password form
        ly_username.setVisibility(View.VISIBLE);
        edt_password.setText(""); // Clear password field
        showResetPasswordForm();
    }

    // For "Send code" button click
    void onSend() {
        if (mActivity == null) return;
        String email = edt_username.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            MessageUtil.showToast(mActivity, R.string.no_email);
            return;
        }
        if (!CommonUtil.isValidEmail(email)) {
            MessageUtil.showToast(mActivity, R.string.invalid_email);
            return;
        }
        // Send OTC code
        onPassword(); // This method handles sending the code
    }

    // For "Validate code" button click
    void onCode() {
        if (mActivity == null) return;
        String email = edt_username.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            MessageUtil.showToast(mActivity, R.string.no_email);
            return;
        }
        if (!CommonUtil.isValidEmail(email)) {
            MessageUtil.showToast(mActivity, R.string.invalid_email);
            return;
        }
        // Show password input for code validation
        showPasswordForm();
    }

    // Basic initialization for streaming UI
    void initialize() {
        Log.d("StreamingFragment", "initialize() called");
        if (user_info == null) {
            Log.d("StreamingFragment", "user_info is null, showing initial login");
            view_share.setVisibility(View.GONE);
            view_stream.setVisibility(View.GONE);
            showInitialLogin();
            return;
        }
        if (myDevice() != null) {
            edt_name.setText(myDevice().name);
            AppPreference.setStr(AppPreference.KEY.DEVICE_NAME, edt_name.getText().toString());
        }
        view_stream.setVisibility(View.VISIBLE);
        if (!user_info.isAdmin()) {
            ly_share.setVisibility(View.GONE);
        }

        swt_streaming.setChecked(AppPreference.getBool(AppPreference.KEY.BROADCAST, true));
        swt_streaming.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            AppPreference.setBool(AppPreference.KEY.BROADCAST, isChecked);
            if (!isChecked) {
                if (mActivity != null && mActivity.mCamService != null) {
                    if (mListener != null) {
                       // mListener.fragStopStreaming();
                    }
                }
                swt_gps.setChecked(true);
                AppPreference.setBool(AppPreference.KEY.GPS_ENABLED, false);
                stopTimer();
            }
        });

        swt_gps.setChecked(AppPreference.getBool(AppPreference.KEY.GPS_ENABLED, false));
        swt_gps.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppPreference.setBool(AppPreference.KEY.GPS_ENABLED, isChecked);
            if (isChecked) {
                if (mListener != null) {
                    mListener.fragStartLocationService();
                    // Add a small delay to ensure service is started
                    new Handler().postDelayed(() -> {
                        updateGPSLocation();
                    }, 1000);
                }
            } else {
                if (mListener != null) {
                    mListener.fragStopLocationService();
                }
            }
            if (mListener != null) {
                mListener.fragCameraRestart(true);
            }
        });

        if (swt_gps.isChecked()) {
            if (mListener != null) {
                mListener.fragStartLocationService();
                // Add a small delay to ensure service is started
                new Handler().postDelayed(() -> {
                    updateGPSLocation();
                }, 1000);
            }
        }

        swt_recording.setChecked(AppPreference.getBool(AppPreference.KEY.RECORD_BROADCAST, false));
        swt_recording.setOnCheckedChangeListener((compoundButton, b) ->
                AppPreference.setBool(AppPreference.KEY.RECORD_BROADCAST, b)
        );

        if (user_info != null) {
            txt_user.setText(AppPreference.getStr(AppPreference.KEY.LOGIN_EMAIL, ""));
        }

        // Setup push channel if found
        if (user_info.registered_devices_info != null && myDevice() != null) {
            Device device = myDevice();
            RegisteredDevice deviceInfo = myDeviceInfo();
            if (deviceInfo != null && device != null && deviceInfo.push_channels.size() > 0) {
                PushChannel push_channel = deviceInfo.push_channels.get(0);
                AppPreference.setStr(AppPreference.KEY.STREAM_BASE, "rtsps://" + user_info.server + ":" + user_info.rtspsport);
                AppPreference.setStr(AppPreference.KEY.STREAM_USERNAME, push_channel.publish_user);
                AppPreference.setStr(AppPreference.KEY.STREAM_PASSWORD, push_channel.publish_pass);
                AppPreference.setStr(AppPreference.KEY.STREAM_CHANNEL, push_channel.channel_id);
                AppPreference.setStr(AppPreference.KEY.LOCAL_STREAM_CHANNEL,push_channel.channel_id);
            } else {
                AppPreference.removeKey(AppPreference.KEY.STREAM_BASE);
                AppPreference.removeKey(AppPreference.KEY.STREAM_USERNAME);
                AppPreference.removeKey(AppPreference.KEY.STREAM_PASSWORD);
                AppPreference.removeKey(AppPreference.KEY.STREAM_CHANNEL);
                MessageUtil.showToast(mActivity, "No channels assigned for this device.");
            }
        } else {
            AppPreference.removeKey(AppPreference.KEY.STREAM_BASE);
            AppPreference.removeKey(AppPreference.KEY.STREAM_USERNAME);
            AppPreference.removeKey(AppPreference.KEY.STREAM_PASSWORD);
            AppPreference.removeKey(AppPreference.KEY.STREAM_CHANNEL);
        }

        if (TextUtils.isEmpty(user_info.expiration)) {
            txt_account_type.setText(user_info.account_status);
        } else {
            txt_account_type.setText(String.format("%s(Expires %s)",
                    user_info.account_status,
                    DateTimeUtils.dateToString(
                            DateTimeUtils.stringToDate(user_info.expiration, DateTimeUtils.DATE_FULL_FORMATTER),
                            DateTimeUtils.DATE_PDF_FORMAT)));
        }

        mDataList.clear();
        if (user_info.invites != null) {
            mDataList.addAll(user_info.invites);
        }
        updateUI();
    }

    // Update share UI
    void updateUI() {
        view_share.setVisibility(View.VISIBLE);
        adapter.notifyDataSetChanged();
        CommonUtil.setListViewHeightBasedOnItems(list_share);
    }

    // Starts location update timer
    private long lastManualUpdateTime = 0;

    private long lastApiUpdateTime = 0;

    public void updateGPSLocation() {
        if (mActivity != null) {
            String latitude = "", longitude = "";
            if (swt_gps.isChecked()) {
                latitude = String.valueOf(LocationManagerService.lat);
                longitude = String.valueOf(LocationManagerService.lng);
            }
            updateDeviceInfo(AppPreference.getStr(AppPreference.KEY.DEVICE_NAME, ""), latitude, longitude, mActivity.isStreaming(), false);
        }
    }

    void initTimer() {
        stopTimer();
        mHandler = new Handler();
        new Handler().postDelayed(this::startTimer, 2000);
    }

    void startTimer() {
        mTimer.run();
    }

    void stopTimer() {
        if (mHandler != null) {
            mHandler.removeCallbacks(mTimer);
        }
    }

    // Return the device that matches local serial
    Device myDevice() {
        Device result = null;
        if (user_info != null && user_info.registered_devices != null) {
            for (RegisteredDevice device : user_info.registered_devices) {
                if (device.device != null && mActivity != null) {
                    if (TextUtils.equals(device.device.device_serial, CommonUtil.getDeviceID(mActivity))) {
                        result = device.device;
                    }
                }
            }
        }
        return result;
    }

    // Return the full info
    RegisteredDevice myDeviceInfo() {
        RegisteredDevice result = null;
        Device dev = myDevice();
        if (dev == null) return null;
        if (user_info != null && user_info.registered_devices != null) {
            for (RegisteredDevice registeredDevice : user_info.registered_devices) {
                if (registeredDevice.device != null) {
                    if (registeredDevice.device.device_id == dev.device_id) {
                        result = registeredDevice;
                    }
                }
            }
        }
        return result;
    }

    // Called by code that refreshes the share list
    void getSharedDevice() {
        view_login.setVisibility(View.GONE);
        if (mListener != null) {
            mListener.isDialog(true);
            mListener.showDialog();
        }
        if (mActivity != null) {
            mActivity.handleNetwork(is_connected -> {
                if (is_connected) {
                    updateDeviceBattery();
                    RestApiService.getRestApiEndPoint().getInvites(CommonUtil.getDeviceID(mActivity)).enqueue(new Callback<Responses.GetInvitesResponse>() {
                        @Override
                        public void onResponse(Call<Responses.GetInvitesResponse> call, Response<Responses.GetInvitesResponse> response) {
                            if (response.isSuccessful()) {
                                if (user_info != null) {
                                    user_info.invites = response.body().getAllInvites();
                                }
                                RestApiService.getRestApiEndPoint().getRegisteredDevices().enqueue(new Callback<Responses.GetRegisteredDevicesResponse>() {
                                    @Override
                                    public void onResponse(Call<Responses.GetRegisteredDevicesResponse> call, Response<Responses.GetRegisteredDevicesResponse> response2) {
                                        if (response2.isSuccessful() && user_info != null) {
                                            user_info.registered_devices = response2.body().registeredDevices;
                                            RestApiService.getRestApiEndPoint().getRegisteredDeviceInfo(CommonUtil.getDeviceID(getContext())).enqueue(new Callback<Responses.GetRegisteredDevicesInfoResponse>() {
                                                @Override
                                                public void onResponse(Call<Responses.GetRegisteredDevicesInfoResponse> call, Response<Responses.GetRegisteredDevicesInfoResponse> response3) {
                                                    if (mListener != null) {
                                                        mListener.dismissDialog();
                                                        mListener.isDialog(false);
                                                    }
                                                    if (response3.isSuccessful() && user_info != null) {
                                                        user_info.registered_devices_info = response3.body();
                                                        initialize();
                                                    } else {
                                                        try {
                                                            JSONObject jObjError = new JSONObject(response3.errorBody().string());
                                                            String error = jObjError.getString("Error");
                                                            if (TextUtils.equals(error, "Account expired!")) {
                                                                refreshToken();
                                                            }
                                                            MessageUtil.showToast(mActivity, error);
                                                        } catch (Exception e) {
                                                            MessageUtil.showToast(mActivity, "Failed to get registered devices");
                                                        }
                                                    }
                                                }
                                                @Override
                                                public void onFailure(Call<Responses.GetRegisteredDevicesInfoResponse> call, Throwable t) {
                                                    if (mListener != null) {
                                                        mListener.dismissDialog();
                                                    }
                                                    MessageUtil.showToast(mActivity, "Failed to get registered devices");
                                                }
                                            });
                                        } else {
                                            if (mListener != null) {
                                                mListener.isDialog(false);
                                                mListener.dismissDialog();
                                            }
                                            try {
                                                JSONObject jObjError = new JSONObject(response2.errorBody().string());
                                                String error = jObjError.getString("Error");
                                                if (TextUtils.equals(error, "Account expired!")) {
                                                    refreshToken();
                                                }
                                                MessageUtil.showToast(mActivity, error);
                                            } catch (Exception e) {
                                                MessageUtil.showToast(mActivity, "Failed to get registered devices");
                                            }
                                        }
                                    }
                                    @Override
                                    public void onFailure(Call<Responses.GetRegisteredDevicesResponse> call, Throwable t) {
                                        if (mListener != null) {
                                            mListener.dismissDialog();
                                        }
                                        MessageUtil.showToast(mActivity, "Failed to get registered devices");
                                    }
                                });
                            } else {
                                if (mListener != null) {
                                    mListener.dismissDialog();
                                    mListener.isDialog(false);
                                }
                                MessageUtil.showToast(mActivity, "Failed to get invites");
                            }
                        }

                        @Override
                        public void onFailure(Call<Responses.GetInvitesResponse> call, Throwable t) {
                            if (mListener != null) {
                                mListener.dismissDialog();
                                mListener.isDialog(false);
                            }
                            MessageUtil.showToast(mActivity, "Failed to get invites");
                        }
                    });
                } else {
                    if (mListener != null) {
                        mListener.dismissDialog();
                        mListener.isDialog(false);
                    }
                    MessageUtil.showToast(mActivity, R.string.msg_error_network);
                }
            });
        }
    }
    // Called on user logout
    public void onLogout() {
        if (mListener != null) {
            mListener.isDialog(true);
        }
        if (mActivity != null) {
            if (!DeviceUtils.isNetworkAvailable(mActivity)) {
                clearInform();
                initialize();
                return;
            }
        }
        updateDeviceBattery();
        if (mListener != null) {
            mListener.showDialog();
        }
        if (mActivity != null) {
            mActivity.handleNetwork(is_connected -> {
                if (is_connected) {
                    if (user_info != null && user_info.isAdmin()) {
                        RestApiService.getRestApiEndPoint().unregister_device(CommonUtil.getDeviceID(mActivity)).enqueue(new Callback<Responses.BaseResponse>() {
                            @Override
                            public void onResponse(Call<Responses.BaseResponse> call, Response<Responses.BaseResponse> response) {
                                if (mListener != null) {
                                    mListener.dismissDialog();
                                }
                                if (response.isSuccessful()) {
                                    MessageUtil.showToast(mActivity, R.string.Success);
                                    clearInform();
                                    initialize();
                                } else {
                                    MessageUtil.showToast(mActivity, R.string.failed_unregister);
                                }
                            }
                            @Override
                            public void onFailure(Call<Responses.BaseResponse> call, Throwable t) {
                                if (mListener != null) {
                                    mListener.dismissDialog();
                                }
                                MessageUtil.showToast(mActivity, R.string.failed_unregister);
                            }
                        });
                    } else {
                        RestApiService.getRestApiEndPoint().logout().enqueue(new Callback<Responses.BaseResponse>() {
                            @Override
                            public void onResponse(Call<Responses.BaseResponse> call, Response<Responses.BaseResponse> response) {
                                if (mListener != null) {
                                    mListener.dismissDialog();
                                }
                                if (response.isSuccessful()) {
                                    MessageUtil.showToast(mActivity, R.string.Success);
                                }
                                clearInform();
                                initialize();
                            }
                            @Override
                            public void onFailure(Call<Responses.BaseResponse> call, Throwable t) {
                                if (mListener != null) {
                                    mListener.dismissDialog();
                                }
                                clearInform();
                                initialize();
                            }
                        });
                    }
                } else {
                    if (mListener != null) {
                        mListener.isDialog(false);
                    }
                    MessageUtil.showToast(mActivity, R.string.msg_error_network);
                }
            });
        }
    }

    // Clears stored info
    void clearInform() {
        user_info = null;
        AppPreference.removeKey(AppPreference.KEY.LOGIN_PASSWORD);
        AppPreference.removeKey(AppPreference.KEY.LOGIN_EMAIL);
        AppPreference.removeKey(AppPreference.KEY.DEVICE_NAME);
        AppPreference.removeKey(AppPreference.KEY.USER_TOKEN);
        AppPreference.removeKey(AppPreference.KEY.PWD_TOKEN);
        AppPreference.removeKey(AppPreference.KEY.STREAM_BASE);
        AppPreference.removeKey(AppPreference.KEY.STREAM_USERNAME);
        AppPreference.removeKey(AppPreference.KEY.STREAM_PASSWORD);
        AppPreference.removeKey(AppPreference.KEY.STREAM_CHANNEL);
        AppPreference.removeKey(AppPreference.KEY.FREQUENCY_MIN);
        AppPreference.removeKey(AppPreference.KEY.BROADCAST);
        AppPreference.removeKey(AppPreference.KEY.GPS_ENABLED);
        AppPreference.removeKey(AppPreference.KEY.RECORD_BROADCAST);
        edt_username.setText("");
        edt_password.setText("");
        edt_name.setText("");
        if (mActivity != null && mActivity.mCamService != null) {
            mActivity.mCamService.stopStreaming();
        }
        if (LiveFragment.getInstance() != null) {
            LiveFragment.getInstance().txt_speed.setText("");
        }
    }

    // Called when user tries to log in
    void onLogin() {
        if (mActivity == null) return;
        String email = edt_username.getText().toString().trim();
        String password = edt_password.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            MessageUtil.showToast(mActivity, R.string.no_email);
            return;
        }
        if (!CommonUtil.isValidEmail(email)) {
            MessageUtil.showToast(mActivity, R.string.invalid_email);
            return;
        }
        if (TextUtils.isEmpty(password)) {
            MessageUtil.showToast(mActivity, R.string.no_password);
            return;
        }
        if (!DeviceUtils.isNetworkAvailable(mActivity)) {
            MessageUtil.showToast(mActivity, R.string.msg_error_network);
            return;
        }
        if (!isAdded()) {
            return;
        }
        mActivity.handleNetwork(is_connected -> {
            if (!is_connected) {
                if (mActivity != null) {
                    mActivity.runOnUiThread(() -> {
                        MessageUtil.showToast(mActivity, R.string.msg_error_network);
                    });
                }
            } else {
                callLogin(email, password);
            }
        });
    }

    // Actually calls the login API
    public void callLogin(String email, String password) {
        if (mActivity == null) return;
        if (mListener != null) {
            mListener.isDialog(true);
            mListener.showDialog();
        }
        RestApiService.getRestApiEndPoint().login(
                email,
                password,
                CommonUtil.getDeviceID(mActivity),
                "grant_type=password").enqueue(new Callback<Responses.LoginResponse>() {
            @Override
            public void onResponse(Call<Responses.LoginResponse> call, Response<Responses.LoginResponse> response) {
                if (mListener != null) {
                    mListener.isDialog(false);
                    mListener.dismissDialog();
                }
                if (response.isSuccessful()) {
                    user_info = response.body();
                    updateDeviceBattery();
                    view_login.setVisibility(View.GONE);
                    view_stream.setVisibility(View.VISIBLE);

                    AppPreference.setStr(AppPreference.KEY.LOGIN_EMAIL, email);
                    AppPreference.setStr(AppPreference.KEY.LOGIN_PASSWORD, password);
                    AppPreference.setStr(AppPreference.KEY.USER_TOKEN, user_info.token);
                    AppPreference.setStr(AppPreference.KEY.EXPIRY_DATE, user_info.expiration);

                    if (!user_info.isAdmin()) {
                        MessageUtil.showError(mActivity, R.string.no_permission);
                        clearInform();
                        showInitialLogin();
                        return;
                    }
                    if (!user_info.availableLicense(CommonUtil.getDeviceID(mActivity))) {
                        clearInform();
                        showInitialLogin();
                        if (mActivity != null) {
                            mActivity.licensesFull();
                        }
                        return;
                    }
                    initialize();
                    if (myDevice() != null) {
                        getSharedDevice();
                    } else {
                        enterDeviceName();
                    }
                } else {
                    try {
                        JSONObject jObjError = new JSONObject(response.errorBody().string());
                        String error = jObjError.getString("Error");
                        if (TextUtils.equals(error, "Account expired!")) {
                            refreshToken();
                        }
                        MessageUtil.showToast(mActivity, error);
                    } catch (Exception e) {
                        MessageUtil.showToast(mActivity, "Failed to login");
                    }
                    showInitialLogin();
                }
            }

            @Override
            public void onFailure(Call<Responses.LoginResponse> call, Throwable t) {
                if (mListener != null) {
                    mListener.isDialog(false);
                    mListener.dismissDialog();
                }
                if (mActivity != null) {
                    MessageUtil.showToast(mActivity, "Failed to login");
                }
                showInitialLogin();
            }
        });
    }

    // Refresh token logic
    public void refreshToken() {
        String email = edt_username.getText().toString().trim();
        String password = edt_password.getText().toString().trim();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || !CommonUtil.isValidEmail(email)) {
            initLogin();
            return;
        }
        if (mListener != null) {
            mListener.isDialog(true);
        }
        if (mActivity != null) {
            mActivity.handleNetwork(is_connected -> {
                if (is_connected) {
                    RestApiService.getRestApiEndPoint().login(
                            email,
                            password,
                            CommonUtil.getDeviceID(mActivity),
                            "grant_type=password").enqueue(new Callback<Responses.LoginResponse>() {
                        @Override
                        public void onResponse(Call<Responses.LoginResponse> call, Response<Responses.LoginResponse> response) {
                            if (response.isSuccessful()) {
                                user_info = response.body();
                                updateDeviceBattery();
                                view_login.setVisibility(View.GONE);
                                view_stream.setVisibility(View.VISIBLE);

                                AppPreference.setStr(AppPreference.KEY.LOGIN_EMAIL, email);
                                AppPreference.setStr(AppPreference.KEY.LOGIN_PASSWORD, password);
                                AppPreference.setStr(AppPreference.KEY.USER_TOKEN, user_info.token);

                                initialize();
                                if (myDevice() != null) {
                                    getSharedDevice();
                                } else {
                                    enterDeviceName();
                                }
                            } else {
                                initLogin();
                            }
                        }
                        @Override
                        public void onFailure(Call<Responses.LoginResponse> call, Throwable t) {
                            initLogin();
                        }
                    });
                } else {
                    MessageUtil.showToast(mActivity, R.string.msg_error_network);
                }
            });
        }
    }

    void initLogin() {
        view_login.setVisibility(View.VISIBLE);
        view_stream.setVisibility(View.GONE);
        AppPreference.removeKey(AppPreference.KEY.LOGIN_EMAIL);
        AppPreference.removeKey(AppPreference.KEY.LOGIN_PASSWORD);
        AppPreference.removeKey(AppPreference.KEY.USER_TOKEN);
    }

    boolean should_enter_device = true;

    public void enterDeviceName() {
        if (!should_enter_device) return;
        if (mActivity == null) return;
        NameDialog nameDialog = new NameDialog(mActivity);
        if (mListener != null) {
            mListener.isDialog(true);
        }
        nameDialog.setOkListener(view -> {
            String name = nameDialog.edt_code.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                MessageUtil.showToast(getContext(), R.string.device_desc);
                return;
            }
            nameDialog.dismiss();
            if (mListener != null) {
                mListener.isDialog(false);
            }
            should_enter_device = true;
            registerDevice(name);
        });
        nameDialog.setCloseListener(view -> {
            should_enter_device = true;
            nameDialog.dismiss();
            if (mListener != null) {
                mListener.isDialog(false);
            }
            clearInform();
            initialize();
        });
        nameDialog.show();
        should_enter_device = false;
    }

    void registerDevice(String name) {
        if (mListener != null) {
            mListener.isDialog(true);
        }
        if (mActivity == null) {
            return;
        }
        MessageUtil.showToast(mActivity, R.string.registering_device);
        if (mListener != null) {
            mListener.showDialog();
        }
        mActivity.handleNetwork(is_connected -> {
            if (!is_connected) {
                if (mListener != null) {
                    mListener.dismissDialog();
                    mListener.isDialog(true);
                }
                MessageUtil.showToast(mActivity, R.string.msg_error_network);
            } else {
                RestApiService.getRestApiEndPoint().registerDevice(
                        CommonUtil.getDeviceID(mActivity),
                        name,
                        "1",
                        CommonUtil.getDeviceName(),
                        CommonUtil.getVersionCode(mActivity)).enqueue(new Callback<Responses.RegisterDeviceResponse>() {
                    @Override
                    public void onResponse(Call<Responses.RegisterDeviceResponse> call, Response<Responses.RegisterDeviceResponse> response) {
                        if (mListener != null) {
                            mListener.dismissDialog();
                            mListener.isDialog(true);
                        }
                        if (response.isSuccessful()) {
                            AppPreference.setStr(AppPreference.KEY.DEVICE_NAME, name);
                            edt_name.setText(name);
                            MessageUtil.showToast(mActivity, R.string.Success);
                            getSharedDevice();
                        } else {
                            try {
                                JSONObject jObjError = new JSONObject(response.errorBody().string());
                                String error = jObjError.getString("Error");
                                if (TextUtils.equals(error, "Account expired!")) {
                                    refreshToken();
                                }
                                MessageUtil.showToast(mActivity, error);
                            } catch (Exception e) {
                                MessageUtil.showToast(mActivity, "Failed to register device");
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<Responses.RegisterDeviceResponse> call, Throwable t) {
                        if (mListener != null) {
                            mListener.dismissDialog();
                            mListener.isDialog(false);
                        }
                        MessageUtil.showToast(mActivity, "Failed to register device");
                    }
                });
            }
        });
    }

    // Old doLogin method removed - now using new form management system

    // Old methods removed - now using new form management system

    // Clicking "Share new" invites user by email
    void onShare() {
        Log.d("StreamingFragment", "onShare() called");
        
        // Check if mActivity is available
        if (mActivity == null) {
            Log.e("StreamingFragment", "mActivity is null, trying to reinitialize");
            mActivity = MainActivity.instance;
        }
        
        if (mActivity == null) {
            Log.e("StreamingFragment", "mActivity is still null, cannot show share dialog");
            MessageUtil.showToast(getContext(), "Activity not available. Please try again.");
            return;
        }
        
        Log.d("StreamingFragment", "mActivity found, showing InviteDialog");
        
        try {
            InviteDialog codeDialog = new InviteDialog(mActivity);
            
            if (mListener != null) {
                Log.d("StreamingFragment", "mListener found, setting dialog state");
                mListener.isDialog(true);
            } else {
                Log.w("StreamingFragment", "mListener is null");
            }
            
            codeDialog.setCloseListener(view -> {
                Log.d("StreamingFragment", "Close button clicked");
                if (mListener != null) {
                    mListener.isDialog(false);
                }
                codeDialog.dismiss();
            });
            
            codeDialog.setOkListener(view -> {
                Log.d("StreamingFragment", "OK button clicked");
                if (mListener != null) {
                    mListener.isDialog(false);
                }
                String email = codeDialog.edt_email.getText().toString();
                Log.d("StreamingFragment", "Email entered: " + email);
                
                if (!CommonUtil.isValidEmail(email)) {
                    MessageUtil.showToast(mActivity, R.string.invalid_email);
                } else {
                    share(email);
                }
                codeDialog.dismiss();
            });
            
            Log.d("StreamingFragment", "Showing InviteDialog");
            codeDialog.show();
            
        } catch (Exception e) {
            Log.e("StreamingFragment", "Error showing InviteDialog", e);
            MessageUtil.showToast(mActivity, "Error showing share dialog: " + e.getMessage());
        }
    }

    void share(String email) {
        if (mListener != null) {
            mListener.isDialog(true);
            mListener.showDialog();
        }
        if (mActivity != null) {
            mActivity.handleNetwork(is_connected -> {
                if (is_connected) {
                    updateDeviceBattery();
                    RestApiService.getRestApiEndPoint().shareDevice(email, CommonUtil.getDeviceID(mActivity)).enqueue(new Callback<Responses.BaseResponse>() {
                        @Override
                        public void onResponse(Call<Responses.BaseResponse> call, Response<Responses.BaseResponse> response) {
                            if (response.isSuccessful()) {
                                MessageUtil.showToast(mActivity, R.string.Success);
                                getSharedDevice();
                            } else {
                                if (mListener != null) {
                                    mListener.dismissDialog();
                                }
                                try {
                                    JSONObject jObjError = new JSONObject(response.errorBody().string());
                                    String error = jObjError.getString("Error");
                                    if (TextUtils.equals(error, "Account expired!")) {
                                        refreshToken();
                                    }
                                    MessageUtil.showToast(mActivity, error);
                                } catch (Exception e) {
                                    MessageUtil.showToast(mActivity, "Failed to share device");
                                }
                            }
                        }
                        @Override
                        public void onFailure(Call<Responses.BaseResponse> call, Throwable t) {
                            if (mListener != null) {
                                mListener.dismissDialog();
                                mListener.isDialog(false);
                            }
                            MessageUtil.showToast(mActivity, "Failed to share device");
                        }
                    });
                } else {
                    if (mListener != null) {
                        mListener.isDialog(false);
                        mListener.dismissDialog();
                    }
                }
            });
        }
    }

    // For "Send code" click
    void callResetPassword() {
        if (mActivity != null) {
            String email = edt_username.getText().toString().trim();
            if (!CommonUtil.isValidEmail(email)) {
                MessageUtil.showToast(mActivity, R.string.invalid_email);
                return;
            }
            if (mListener != null) {
                mListener.isDialog(true);
                mListener.showDialog();
            }
            mActivity.handleNetwork(is_connected -> {
                if (is_connected) {
                    RestApiService.getRestApiEndPoint().resetPassword(email).enqueue(new Callback<Responses.BaseResponse>() {
                        @Override
                        public void onResponse(Call<Responses.BaseResponse> call, Response<Responses.BaseResponse> response) {
                            if (mListener != null) {
                                mListener.isDialog(false);
                                mListener.dismissDialog();
                            }
                            if (response.isSuccessful()) {
                                MessageUtil.showToast(mActivity, "New code is sent to your email.");
                                if (mListener != null) {
                                    mListener.isDialog(true);
                                    mActivity.startActivity(new Intent(mActivity, PinActivity.class));
                                }
                            } else {
                                try {
                                    JSONObject jObjError = new JSONObject(response.errorBody().string());
                                    String error = jObjError.getString("Error");
                                    MessageUtil.showToast(mActivity, error);
                                } catch (Exception e) {
                                    MessageUtil.showToast(mActivity, "Failed to reset password");
                                }
                            }
                        }
                        @Override
                        public void onFailure(Call<Responses.BaseResponse> call, Throwable t) {
                            if (mListener != null) {
                                mListener.isDialog(false);
                                mListener.dismissDialog();
                            }
                            MessageUtil.showToast(mActivity, "Failed to reset password");
                        }
                    });
                } else {
                    if (mListener != null) {
                        mListener.isDialog(false);
                        mListener.dismissDialog();
                    }
                    MessageUtil.showToast(mActivity, R.string.msg_error_network);
                }
            });
        }
    }

    // For "Validate code" click
    void onPassword() {
        if (mActivity == null) return;
        String email = edt_username.getText().toString().trim();
        edt_password.setText("");
        if (TextUtils.isEmpty(email)) {
            MessageUtil.showToast(mActivity, R.string.no_email);
            return;
        }
        if (!CommonUtil.isValidEmail(email)) {
            MessageUtil.showToast(mActivity, R.string.invalid_email);
            return;
        }
        if (mListener != null) {
            mListener.dismissDialog();
            mListener.isDialog(true);
        }
        mActivity.handleNetwork(is_connected -> {
            if (is_connected) {
                RestApiService.getRestApiEndPoint().requestOTCCode(email).enqueue(new Callback<Responses.BaseResponse>() {
                    @Override
                    public void onResponse(Call<Responses.BaseResponse> call, Response<Responses.BaseResponse> response) {
                        if (mListener != null) {
                            mListener.dismissDialog();
                        }
                        if (response.isSuccessful() && response.body().success) {
                            if (mListener != null) {
                                mListener.isDialog(true);
                            }
                            mActivity.startActivity(new Intent(mActivity, PinActivity.class));
                        } else {
                            if (mListener != null) {
                                mListener.isDialog(false);
                            }
                            try {
                                JSONObject jObjError = new JSONObject(response.errorBody().string());
                                String error = jObjError.getString("Error");
                                MessageUtil.showError(mActivity, error);
                            } catch (Exception e) {
                                MessageUtil.showError(mActivity, "Failed to send OTC code");
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<Responses.BaseResponse> call, Throwable t) {
                        if (mListener != null) {
                            mListener.isDialog(false);
                            mListener.dismissDialog();
                        }
                        MessageUtil.showError(mActivity, "Failed to send OTC");
                    }
                });
            } else {
                if (mListener != null) {
                    mListener.isDialog(false);
                    mListener.dismissDialog();
                }
                MessageUtil.showToast(mActivity, R.string.msg_error_network);
            }
        });
    }

    // Called after user enters code
    public void loginCode(String pin_code) {
        String email = edt_username.getText().toString().trim();
        loginWithCode(email, pin_code);
    }

    void loginWithCode(String email, String code) {
        if (mListener != null) {
            mListener.isDialog(true);
            mListener.showDialog();
        }
        mActivity.handleNetwork(is_connected -> {
            if (is_connected) {
                RestApiService.getRestApiEndPoint().codeValidation(email, code).enqueue(new Callback<Responses.CodeValidationResponse>() {
                    @Override
                    public void onResponse(Call<Responses.CodeValidationResponse> call, Response<Responses.CodeValidationResponse> response) {
                        if (mListener != null) {
                            mListener.isDialog(false);
                            mListener.dismissDialog();
                        }
                        if (response.isSuccessful()) {
                            MessageUtil.showToast(mActivity, R.string.Success);
                            setPassword(response.body().token);
                        } else {
                            try {
                                MessageUtil.showToast(mActivity, response.errorBody().string());
                            } catch (Exception e) {
                                MessageUtil.showToast(mActivity, "Authentication failed");
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<Responses.CodeValidationResponse> call, Throwable t) {
                        if (mListener != null) {
                            mListener.isDialog(false);
                            mListener.dismissDialog();
                        }
                        MessageUtil.showToast(mActivity, "Failed to validate code");
                    }
                });
            } else {
                if (mListener != null) {
                    mListener.isDialog(false);
                    mListener.dismissDialog();
                }
                MessageUtil.showToast(mActivity, R.string.msg_error_network);
            }
        });
    }

    void setPassword(String token) {
        if (mActivity == null) return;
        PasswordDialog pwdDialog = new PasswordDialog(mActivity);
        if (mListener != null) {
            mListener.isDialog(true);
        }
        pwdDialog.setCloseListener(view -> {
            pwdDialog.dismiss();
            if (mListener != null) {
                mListener.isDialog(false);
            }
        });
        pwdDialog.setOkListener(view -> {
            if (mListener != null) {
                mListener.isDialog(false);
            }
            String password = pwdDialog.edt_password.getText().toString();
            String re_password = pwdDialog.edt_repassword.getText().toString();
            if (TextUtils.isEmpty(password)) {
                MessageUtil.showToast(mActivity, R.string.invalid_password);
                return;
            }
            if (TextUtils.isEmpty(re_password)) {
                MessageUtil.showToast(mActivity, R.string.password_confirm);
                return;
            }
            if (!TextUtils.equals(password, re_password)) {
                MessageUtil.showToast(mActivity, R.string.no_match_password);
                return;
            }
            setPasswordAPI(token, password);
            pwdDialog.dismiss();
        });
        pwdDialog.show();
    }

    void setPasswordAPI(String token, String password) {
        AppPreference.setStr(AppPreference.KEY.PWD_TOKEN, token);
        String email = edt_username.getText().toString().trim();
        if (mListener != null) {
            mListener.isDialog(true);
            mListener.showDialog();
        }
        mActivity.handleNetwork(is_connected -> {
            if (is_connected) {
                RestApiService.getRestApiEndPoint().setPassword(email, token, password).enqueue(new Callback<Responses.BaseResponse>() {
                    @Override
                    public void onResponse(Call<Responses.BaseResponse> call, Response<Responses.BaseResponse> response) {
                        if (mListener != null) {
                            mListener.isDialog(false);
                            mListener.dismissDialog();
                        }
                        if (response.isSuccessful()) {
                            if (response.body().success) {
                                MessageUtil.showToast(mActivity, "Set Password Success!");
                                edt_password.setText(password);
                                onLogin();
                            } else {
                                MessageUtil.showToast(mActivity, "Failed to set your password.");
                            }
                        } else {
                            MessageUtil.showToast(mActivity, "Failed to set your password.");
                        }
                    }
                    @Override
                    public void onFailure(Call<Responses.BaseResponse> call, Throwable t) {
                        if (mListener != null) {
                            mListener.isDialog(false);
                            mListener.dismissDialog();
                        }
                        MessageUtil.showToast(mActivity, "Failed to set your password.");
                    }
                });
            } else {
                if (mListener != null) {
                    mListener.isDialog(false);
                    mListener.dismissDialog();
                }
                MessageUtil.showToast(mActivity, R.string.msg_error_network);
            }
        });
    }

    // "Change password" button
    void changePassword() {
        if (mActivity != null) {
            String token = AppPreference.getStr(AppPreference.KEY.PWD_TOKEN, "");
            if (TextUtils.isEmpty(token)) {
                callResetPassword();
                return;
            }
            RePasswordDialog pwdDialog = new RePasswordDialog(mActivity);
            if (mListener != null) {
                mListener.isDialog(true);
            }
            pwdDialog.setCloseListener(view -> {
                if (mListener != null) {
                    pwdDialog.dismiss();
                    mListener.isDialog(false);
                }
            });
            pwdDialog.setOkListener(view -> {
                if (mListener != null) {
                    mListener.isDialog(false);
                }
                String password = pwdDialog.edt_password.getText().toString();
                String re_password = pwdDialog.edt_repassword.getText().toString();
                if (TextUtils.isEmpty(password)) {
                    MessageUtil.showToast(mActivity, R.string.invalid_password);
                    return;
                }
                if (TextUtils.isEmpty(re_password)) {
                    MessageUtil.showToast(mActivity, R.string.password_confirm);
                    return;
                }
                if (!TextUtils.equals(password, re_password)) {
                    MessageUtil.showToast(mActivity, R.string.no_match_password);
                    return;
                }
                setPasswordAPI(AppPreference.getStr(AppPreference.KEY.PWD_TOKEN, ""), password);
                pwdDialog.dismiss();
            });
            pwdDialog.show();
        }
    }

    // "Update" button next to device name
    void updateName() {
        if (mActivity != null) {
            String name = edt_name.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                MessageUtil.showToast(mActivity, R.string.no_name);
                return;
            }
            String latitude = "", longitude = "";
            if (swt_gps.isChecked()) {
                latitude = String.valueOf(LocationManagerService.lat);
                longitude = String.valueOf(LocationManagerService.lng);
            }
            updateDeviceInfo(name, latitude, longitude, mActivity.isStreaming(), true);
        }
    }

    // General method to update device info
    void updateDeviceInfo(String name, String lat, String lng, boolean streaming, boolean show_dialog) {
        if (TextUtils.isEmpty(name)) return;
        long currentTime = System.currentTimeMillis();
        int frequencyMinutes = AppPreference.getInt(AppPreference.KEY.FREQUENCY_MIN, 1);
        long frequencyMillis = (long) frequencyMinutes * 60 * 1000;
        if (currentTime - lastApiUpdateTime < frequencyMillis) {
            return;
        }
        lastApiUpdateTime = currentTime;
        if (show_dialog && mListener != null) {
            mListener.showDialog();
        }
        if (mListener != null) {
            mListener.isDialog(true);
        }
        if (mActivity != null) {
            mActivity.handleNetwork(is_connected -> {
                if (!is_connected) {
                    if (mListener != null) {
                        mListener.isDialog(false);
                        mListener.dismissDialog();
                    }
                    return;
                }
                if (TextUtils.isEmpty(lat) || TextUtils.isEmpty(lng)) {
                    RestApiService.getRestApiEndPoint().updateDeviceWithoutLocation(
                            CommonUtil.getDeviceID(mActivity),
                            name,
                            CommonUtil.batteryLevel(mActivity),
                            streaming,
                            CommonUtil.isCharging(mActivity),
                            mActivity.is_landscape ? AppConstant.LANDSCAPE : AppConstant.PORTRAIT,
                            mActivity.deviceType()
                    ).enqueue(new Callback<Responses.BaseResponse>() {
                        @Override
                        public void onResponse(Call<Responses.BaseResponse> call, Response<Responses.BaseResponse> response) {
                            if (mListener != null) {
                                mListener.dismissDialog();
                                mListener.isDialog(false);
                            }
                            if (response.isSuccessful()) {
                                Log.d(TAG, "API call successful - Device info updated");
                            } else {
                                Log.e(TAG, "API call failed with code: " + response.code());
                            }
                        }
                        @Override
                        public void onFailure(Call<Responses.BaseResponse> call, Throwable t) {
                            if (mListener != null) {
                                mListener.dismissDialog();
                                mListener.isDialog(false);
                            }
                            Log.e(TAG, "API call failed: " + t.getMessage());
                        }
                    });
                } else {
                    RestApiService.getRestApiEndPoint().updateDevice(
                            CommonUtil.getDeviceID(mActivity),
                            name,
                            lat,
                            lng,
                            CommonUtil.batteryLevel(mActivity),
                            streaming,
                            CommonUtil.isCharging(mActivity),
                            mActivity.is_landscape ? AppConstant.LANDSCAPE : AppConstant.PORTRAIT,
                            mActivity.deviceType()
                    ).enqueue(new Callback<Responses.BaseResponse>() {
                        @Override
                        public void onResponse(Call<Responses.BaseResponse> call, Response<Responses.BaseResponse> response) {
                            if (mListener != null) {
                                mListener.dismissDialog();
                                mListener.isDialog(false);
                            }
                            if (response.isSuccessful()) {
                                Log.d(TAG, "API call successful - Device info updated");
                            } else {
                                Log.e(TAG, "API call failed with code: " + response.code());
                            }
                        }
                        @Override
                        public void onFailure(Call<Responses.BaseResponse> call, Throwable t) {
                            if (mListener != null) {
                                mListener.dismissDialog();
                                mListener.isDialog(false);
                            }
                            Log.e(TAG, "API call failed: " + t.getMessage());
                        }
                    });
                }
            });
        }
    }

    // Update device battery info
    public void updateDeviceBattery() {
        String latitude = "", longitude = "";
        if (swt_gps.isChecked()) {
            latitude = String.valueOf(LocationManagerService.lat);
            longitude = String.valueOf(LocationManagerService.lng);
        }
        if (mActivity != null) {
            updateDeviceInfo(AppPreference.getStr(AppPreference.KEY.DEVICE_NAME, ""), latitude, longitude, mActivity.isStreaming(), false);
        }
    }

    // Called from outside for streaming updates
    public void updateDeviceStreaming(boolean streaming) {
        String lat = "", lng = "";
        if (swt_gps.isChecked()) {
            lat = String.valueOf(LocationManagerService.lat);
            lng = String.valueOf(LocationManagerService.lng);
        }
        updateDeviceInfo(AppPreference.getStr(AppPreference.KEY.DEVICE_NAME, ""), lat, lng, streaming, true);
    }

    public void updateDeviceStreaming(boolean streaming, boolean showing) {
        String lat = "", lng = "";
        if (swt_gps.isChecked()) {
            lat = String.valueOf(LocationManagerService.lat);
            lng = String.valueOf(LocationManagerService.lng);
        }
        updateDeviceInfo(AppPreference.getStr(AppPreference.KEY.DEVICE_NAME, ""), lat, lng, streaming, showing);
    }

    // Called from layout "Start" button or outside
    void startBroadCast() {
        if (mActivity == null) return;

        // Check spinner to see if user is in Local mode or Cloud mode
        int modePosition = AppPreference.getInt(AppPreference.KEY.STREAMING_MODE,0);

        boolean isLocalMode = (modePosition == 1);

        if (isLocalMode) {
            // Local streaming logic
            String serverIP = edt_server_ip.getText().toString().trim();
            String port = edt_port.getText().toString().trim();
            String localUser = edt_local_user.getText().toString().trim();
            String localPassword = edt_local_password.getText().toString().trim();

            if (TextUtils.isEmpty(serverIP)) {
                MessageUtil.showToast(mActivity, "Please enter Server IP");
                return;
            }
            if (TextUtils.isEmpty(port)) {
                MessageUtil.showToast(mActivity, "Please enter Port");
                return;
            }
            // Possibly check user/pass if needed
            MessageUtil.showToast(mActivity, "Starting Local Streaming to " + serverIP + ":" + port);

            // If you have a local streaming service, call it here
            // e.g. if (mListener != null) mListener.fragStartLocalStreaming(...);

        } else {
            // Cloud streaming logic
            if (!swt_streaming.isChecked()) {
                MessageUtil.showToast(mActivity, R.string.stream_disabled);
                return;
            }
            // If you still needed these old fields:
            //   edt_url is hidden in your new UI
            //   We have `edt_channel`, `edt_username`, `edt_password` for Cloud
            String channel = edt_channel.getText().toString().trim();
            if (TextUtils.isEmpty(channel)) {
                MessageUtil.showToast(mActivity, R.string.no_channel);
                return;
            }
            String uname = edt_username.getText().toString().trim();
            if (TextUtils.isEmpty(uname)) {
                MessageUtil.showToast(mActivity, R.string.invalid_username);
                return;
            }
            String pwd = edt_password.getText().toString().trim();
            if (TextUtils.isEmpty(pwd)) {
                MessageUtil.showToast(mActivity, R.string.invalid_password);
                return;
            }
            // Store them if needed
            AppPreference.setStr(AppPreference.KEY.STREAM_CHANNEL, channel);
            AppPreference.setStr(AppPreference.KEY.STREAM_USERNAME, uname);
            AppPreference.setStr(AppPreference.KEY.STREAM_PASSWORD, pwd);
            AppPreference.setStr(AppPreference.KEY.LOCAL_STREAM_CHANNEL,channel);
            if (LiveFragment.getInstance() != null) {
                if (LiveFragment.getInstance().is_camera_opened || LiveFragment.getInstance().is_usb_opened || LiveFragment.getInstance().is_audio_only || LiveFragment.getInstance().is_cast_opened) {
                    if (mListener != null) {
                        mListener.fragStartStream();
                    }
                } else {
                    if (mListener != null) {
                        mListener.isDialog(true);
                        mListener.showDialog();
                    }
                    new Handler().postDelayed(() -> {
                        if (mListener != null) {
                            mListener.dismissDialog();
                            mListener.isDialog(false);
                        }
                    }, 3000);
                }
            }
        }
    }

    // "Speed Test" click
    void speedTest() {
        if (mListener != null) {
            mListener.isDialog(true);
        }
        if (mActivity != null) {
            mActivity.startActivity(new Intent(mActivity, WebActivity.class));
        }
    }


    // Old onClick method removed - now using individual button click listeners

    // Handle results, e.g. scanning a channel QR
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mListener != null) {
            mListener.isDialog(false);
        }
        if (resultCode == RESULT_OK && data != null && requestCode == REQUEST_CODE_QR_SCAN) {
            String result = data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult");
            edt_channel.setText(result);
        }
    }
    private boolean isUpdatingStatus = false;

    // Called from streaming service to update status text
    public void updateStatus(String body) {
        if (mActivity == null) return;
        if (isUpdatingStatus) return; // prevents recursive calls
        isUpdatingStatus = true;

        if (!isAdded()) return;
        final String offlineStr = getString(R.string.offline);
        final String onlineStr = getString(R.string.online);
        final String connectingStr = getString(R.string.connecting);
        final String errorStr = getString(R.string.error);
        mActivity.runOnUiThread(() -> {
            try {
                if (!isAdded()) return;
                if (TextUtils.equals(body, offlineStr)) {
                    btn_start.setEnabled(true);
                    btn_start.setText(R.string.action_title_start);
                    if (LiveFragment.getInstance() != null && LiveFragment.getInstance().isAdded()) {
                        if (!AppPreference.getBool(AppPreference.KEY.STREAM_STARTED, false)) {
                            LiveFragment.getInstance().ic_stream.setImageResource(R.mipmap.ic_stream);
                        }
                        LiveFragment.getInstance().is_streaming = false;
                        stopStream();
                    }
                } else if (TextUtils.equals(body, onlineStr)) {
                    btn_start.setEnabled(true);
                    btn_start.setText(R.string.action_title_stop);
                    if (LiveFragment.getInstance() != null && LiveFragment.getInstance().isAdded()) {
                        LiveFragment.getInstance().ic_stream.setImageResource(R.mipmap.ic_stream_active);
                        startStream();
                    }
                } else if (TextUtils.equals(body, connectingStr)) {
                    btn_start.setEnabled(false);
                    btn_start.setText(R.string.action_title_stop);
                    if (LiveFragment.getInstance() != null && LiveFragment.getInstance().isAdded()) {
                        if (mActivity.isStreaming()) {
                            startStream();
                        } else {
                            stopStream();
                        }
                    }
                }
                txt_status.setText(body);
            } finally {
                isUpdatingStatus = false;
            }
        });
    }
    void startStream(){
        boolean isStreaming = AppPreference.getBool(AppPreference.KEY.STREAM_STARTED, false);
        if (isStreaming) {
            if (LiveFragment.getInstance() != null && LiveFragment.getInstance().isAdded()) {
                LiveFragment.getInstance().ic_stream.setImageResource(R.mipmap.ic_stream_active);
            }
//            if (MainActivity.getInstance() != null ) {
//                if (MainActivity.getInstance().mCamService != null) {
//                    MainActivity.getInstance().mCamService.startStreaming();
//                } else if (MainActivity.getInstance().mUSBService != null) {
//                    MainActivity.getInstance().mUSBService.startStreaming();
//                } else if (MainActivity.getInstance().mAudioService != null) {
//                    MainActivity.getInstance().mAudioService.startStreaming();
//                } else if (MainActivity.getInstance().mCastService != null) {
//                    MainActivity.getInstance().startCastStreaming();
//                }
//            }
        }else {
            stopStream();
        }

    }

    private void stopStream() {
        if (MainActivity.getInstance() != null) {
            if (LiveFragment.getInstance() != null && LiveFragment.getInstance().isAdded()) {
                LiveFragment.getInstance().ic_stream.setImageResource(R.mipmap.ic_stream);
            }
//            MainActivity.instance.stopFragUSBService();
//            MainActivity.instance.stopFragWifiService();
//            MainActivity.instance.stopFragBgCast();
//            MainActivity.instance.stopFragBgCamera();
//            MainActivity.instance.stopBgAudio();
        }
    }

    // Update speed text on UI
    @SuppressLint("DefaultLocale")
    public void updateSpeed(String speed) {
        if (mActivity == null || !isAdded()) return;
        mActivity.runOnUiThread(() -> {
            txt_speed.setText(speed);
            txt_speed.setTextColor(Color.WHITE);
            if (LiveFragment.getInstance() != null) {
                LiveFragment.getInstance().txt_speed.setTextColor(Color.WHITE);
                String channel = AppPreference.getStr(AppPreference.KEY.STREAM_CHANNEL, "");
                if (!TextUtils.isEmpty(speed) && !TextUtils.isEmpty(channel)) {
                    if (LiveFragment.getInstance().is_camera_opened && mActivity.mCamService != null) {
                        Streamer.Size size = mActivity.mCamService.mEglManager.videoSize;
                        String chInfo = String.format("Channel: %s %s", channel, speed);
                        if (mActivity.mCamService.mEglManager.mStreamer != null) {
                            double fps = mActivity.mCamService.mEglManager.mStreamer.getFps();
                            String resInfo = String.format("Resolution: %d x %d, %.2f fps", size.width, size.height, fps);
                            LiveFragment.getInstance().txt_speed.setText(String.format("%s\n%s", chInfo, resInfo));
                        }
                    }else if (LiveFragment.getInstance().is_usb_opened && mActivity.mUSBService != null) {
                        Streamer.Size size = mActivity.mUSBService.mEglManager.videoSize;
                        String chInfo = String.format("Channel: %s %s", channel, speed);
                        if (mActivity.mUSBService.mEglManager.mStreamer != null) {
                            double fps = mActivity.mUSBService.mEglManager.mStreamer.getFps();
                            String resInfo = String.format("Resolution: %d x %d, %.2f fps", size.width, size.height, fps);
                            LiveFragment.getInstance().txt_speed.setText(String.format("%s\n%s", chInfo, resInfo));
                        }
                    }else if (LiveFragment.getInstance().is_audio_only && mActivity.mAudioService != null) {
                        Streamer.Size size = mActivity.mAudioService.mEglManager.videoSize;
                        String chInfo = String.format("Channel: %s %s", channel, speed);
                        if (mActivity.mAudioService.mEglManager.mStreamer != null) {
                            double fps = mActivity.mAudioService.mEglManager.mStreamer.getFps();
                            String resInfo = String.format("Resolution: %d x %d, %.2f fps", 0, 0, fps);
                            LiveFragment.getInstance().txt_speed.setText(String.format("%s\n%s", chInfo, resInfo));
                        }
                    }else if (LiveFragment.getInstance().is_cast_opened && mActivity.mCastService != null) {
                        Streamer.Size size = mActivity.mCastService.mEglManager.videoSize;
                        String chInfo = String.format("Channel: %s %s", channel, speed);
                        if (mActivity.mCastService.mEglManager.mStreamer != null) {
                            double fps = mActivity.mCastService.mEglManager.mStreamer.getFps();
                            String resInfo = String.format("Resolution: %d x %d, %.2f fps", size.width, size.height, fps);
                            LiveFragment.getInstance().txt_speed.setText(String.format("%s\n%s", chInfo, resInfo));
                        }
                    }
                } else {
                    LiveFragment.getInstance().txt_speed.setText("");
                }
                LiveFragment.getInstance().handleStreamView();
            }
        });
    }
    @Override
    public void onRefresh() {
        // Not used
    }

    // Handle viewmodel events
    private void handleEvent(SharedViewModel.EventPayload payload) {
        EventType eventType = payload.getEventType();
        Object data = payload.getData();
        switch (eventType) {
            case UPDATE_DEVICE_STREAMING: {
                updateDeviceStreaming((Boolean) data);
            } break;
            case UPDATE_DEVICE_STREAMING_DOUBLE_VAL: {
                if (data instanceof HashMap) {
                    HashMap<String, Boolean> map = (HashMap<String, Boolean>) data;
                    boolean streaming = map.getOrDefault("streaming", false);
                    boolean showing = map.getOrDefault("showing", false);
                    updateDeviceStreaming(streaming, showing);
                }
            } break;
            default:
                // no-op
                break;
        }
    }

    // List adapter for invite sharing
    class ListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        ListAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }
        public int getCount() { return mDataList.size(); }
        public Object getItem(int position) { return position; }
        public long getItemId(int position) { return position; }

        class ViewHolder {
            TextView txt_email;
            TextView txt_status;
            TextView txt_share;
            TextView txt_action;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.row_text, parent, false);
                holder = new ViewHolder();
                
                holder.txt_email = convertView.findViewById(R.id.txt_email);
                holder.txt_status = convertView.findViewById(R.id.txt_status);
                holder.txt_share = convertView.findViewById(R.id.txt_share);
                holder.txt_action = convertView.findViewById(R.id.txt_action);
                
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Invite invite = mDataList.get(position);
            if (invite.isValidated()) {
                holder.txt_status.setText(R.string.accepted);
                holder.txt_status.setTextColor(Color.GREEN);
            } else if (invite.isPending()) {
                holder.txt_status.setText(R.string.pending);
                holder.txt_status.setTextColor(Color.RED);
            } else if (invite.isRejected()) {
                holder.txt_status.setText(R.string.rejected);
                holder.txt_status.setTextColor(Color.RED);
                holder.txt_share.setVisibility(View.GONE);
            }
            String email = invite.username;
            holder.txt_email.setText(email);

            holder.txt_action.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.isDialog(true);
                }
                MessageDialog removeConfirm = MessageDialog.show(
                                getString(R.string.remove_share),
                                getString(R.string.label_remove_share),
                                getString(R.string.Okay),
                                getString(R.string.cancel))
                        .setCancelButton((dialog, v13) -> {
                            dialog.dismiss();
                            return false;
                        })
                        .setOkButton((baseDialog, v14) -> {
                            deleteShare(invite);
                            baseDialog.dismiss();
                            return false;
                        });
                removeConfirm.setOkTextInfo(new TextInfo().setFontColor(Color.parseColor("#000000")).setBold(true));
                removeConfirm.setCancelTextInfo(new TextInfo().setFontColor(Color.parseColor("#000000")).setBold(true));
            });

            holder.txt_share.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.isDialog(true);
                }
                if (mActivity != null) {
                    new AlertDialog.Builder(mActivity)
                            .setTitle(R.string.resend_share)
                            .setMessage(R.string.label_resend_share)
                            .setPositiveButton(R.string.Okay, (dialog, which) -> {
                                share(email);
                            })
                            .setNegativeButton(R.string.cancel, (dialog, which) -> {
                                if (mListener != null) {
                                    mListener.isDialog(false);
                                }
                            }).show();
                }
            });

            return convertView;
        }
    }

    // Removing a share
    void deleteShare(Invite invite) {
        if (mListener != null) {
            mListener.isDialog(true);
            mListener.showDialog();
        }
        if (mActivity != null) {
            mActivity.handleNetwork(is_connected -> {
                if (is_connected) {
                    RestApiService.getRestApiEndPoint().removeShare(invite.username, invite.device_id).enqueue(new Callback<Responses.BaseResponse>() {
                        @Override
                        public void onResponse(Call<Responses.BaseResponse> call, Response<Responses.BaseResponse> response) {
                            if (mListener != null) {
                                mListener.dismissDialog();
                                mListener.isDialog(false);
                            }
                            if (response.isSuccessful()) {
                                mDataList.remove(invite);
                                adapter.notifyDataSetChanged();
                                CommonUtil.setListViewHeightBasedOnItems(list_share);
                            }
                        }
                        @Override
                        public void onFailure(Call<Responses.BaseResponse> call, Throwable t) {
                            if (mListener != null) {
                                mListener.dismissDialog();
                                mListener.isDialog(false);
                            }
                            MessageUtil.showToast(mActivity, t.getLocalizedMessage());
                        }
                    });
                } else {
                    MessageUtil.showToast(mActivity, R.string.msg_error_network);
                    if (mListener != null) {
                        mListener.isDialog(false);
                        mListener.dismissDialog();
                    }
                }
            });
        }
    }
}
