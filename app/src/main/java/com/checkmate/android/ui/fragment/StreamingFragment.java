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
import com.checkmate.android.databinding.FragmentStreamingBinding;
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
import com.checkmate.android.util.CommonUtil;
import com.checkmate.android.util.DateTimeUtils;
import com.checkmate.android.util.DeviceUtils;
import com.checkmate.android.util.MainActivity;
import com.checkmate.android.util.MessageUtil;
import com.checkmate.android.viewmodels.EventType;
import com.checkmate.android.viewmodels.SharedViewModel;
import com.checkmate.android.service.FragmentVisibilityListener;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.app.Activity.RESULT_OK;

public class StreamingFragment extends BaseFragment implements FragmentVisibilityListener {

    private final int REQUEST_CODE_QR_SCAN = 101;
    public static StreamingFragment instance;

    public static StreamingFragment getInstance() {
        return instance;
    }

    MainActivity mActivity;
    Responses.LoginResponse user_info;
    private FragmentStreamingBinding binding;
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
    public void onDestroyView() {
        super.onDestroyView();
        rootView = null; // Avoid memory leaks
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentStreamingBinding.inflate(inflater, container, false);
        rootView = binding.getRoot();

        showInitialLogin();
        String login_email = AppPreference.getStr(AppPreference.KEY.LOGIN_EMAIL, "");
        String login_password = AppPreference.getStr(AppPreference.KEY.LOGIN_PASSWORD, "");
        if (!TextUtils.isEmpty(login_email) && !TextUtils.isEmpty(login_password)) {
            binding.edtUsername.setText(login_email);
            binding.edtPassword.setText(login_password);
            onLogin();
        } else {
            initialize();
        }

        if (mActivity != null) {
            binding.btnBack.setColorFilter(ContextCompat.getColor(mActivity, R.color.black));
        }

        // Store channel changes for Cloud streaming
        binding.edtChannel.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!TextUtils.isEmpty(s.toString().trim())) {
                    AppPreference.setStr(AppPreference.KEY.STREAM_CHANNEL, s.toString().trim());
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        String channel = AppPreference.getStr(AppPreference.KEY.STREAM_CHANNEL, "");
        binding.edtChannel.setText(channel);
        // Setup share list
        if (mActivity != null) {
            adapter = new ListAdapter(mActivity);
        }
        binding.listShare.setAdapter(adapter);

        binding.buttonFrequency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFrequencySelectionDialog();
            }
        });

        int freq_min = AppPreference.getInt(AppPreference.KEY.FREQUENCY_MIN, 1);
        switch (freq_min) {
            case 1:  binding.buttonFrequency.setText("1 Minute"); break;
            case 5:  binding.buttonFrequency.setText("5 Minutes"); break;
            case 10: binding.buttonFrequency.setText("10 Minutes"); break;
            case 15: binding.buttonFrequency.setText("15 Minutes"); break;
            case 30: binding.buttonFrequency.setText("30 Minutes"); break;
        }

        // Swipe refresh
        binding.swipeRefresh.setOnRefreshListener(() -> {
            binding.swipeRefresh.setRefreshing(false);
            refreshToken();
        });

        // Setup spinner for Cloud vs. Local
        setupViewsForConfigs();
        setupStreamingModeSpinner();
        setupLocalStreaming();
        initMonitoring();
        
        // Setup all click listeners
        setupClickListeners();
        
        return rootView;
    }
    
    private void setupClickListeners() {
        binding.btnLocalUpdate.setOnClickListener(v -> onLocalUpdateClicked());
        binding.txtChangePassword.setOnClickListener(v -> changePassword());
        binding.txtUpdate.setOnClickListener(v -> updateName());
        binding.btnStart.setOnClickListener(v -> startBroadCast());
        binding.txtSpeedTest.setOnClickListener(v -> speedTest());
        
        // Multiple click listeners
        binding.btnShare.setOnClickListener(v -> onShare());
        binding.btnLogout.setOnClickListener(v -> onLogout());
        binding.lyShare.setOnClickListener(v -> onShare());
        binding.btnRefresh.setOnClickListener(v -> {
            binding.edtUsername.setText(AppPreference.getStr(AppPreference.KEY.LOGIN_EMAIL, ""));
            binding.edtPassword.setText(AppPreference.getStr(AppPreference.KEY.LOGIN_PASSWORD, ""));
            onLogin();
        });
        binding.btnBack.setOnClickListener(v -> {
            binding.viewLogin.setVisibility(View.VISIBLE);
            binding.lyUsername.setVisibility(View.GONE);
            binding.btnNewUser.setVisibility(View.VISIBLE);
            binding.btnPassword.setVisibility(View.VISIBLE);
            binding.btnResetPassword.setVisibility(View.VISIBLE);
            binding.btnCode.setVisibility(View.GONE);
            binding.btnBack.setVisibility(View.GONE);
            binding.btnSend.setVisibility(View.GONE);
            binding.viewStream.setVisibility(View.GONE);
        });
        binding.btnLogin.setOnClickListener(v -> onLogin());
        binding.btnPassword.setOnClickListener(v -> needPassword());
        binding.btnResetPassword.setOnClickListener(v -> resetPassword());
        binding.btnNewUser.setOnClickListener(v -> doLogin());
        binding.btnCode.setOnClickListener(v -> onPassword());
        
        // QR code scanning
        View txtQr = rootView.findViewById(R.id.txt_qr);
        if (txtQr != null) {
            txtQr.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.isDialog(true);
                }
                if (mActivity != null) {
                    Intent i = new Intent(mActivity, QrCodeActivity.class);
                    startActivityForResult(i, REQUEST_CODE_QR_SCAN);
                }
            });
        }
    }

    private void showFrequencySelectionDialog() {
        if (getContext() == null) return;
        FrequencySelectionDialogFragment dialogFragment = new FrequencySelectionDialogFragment();
        dialogFragment.setFrequencySelectionListener(new FrequencySelectionDialogFragment.FrequencySelectionListener() {
            @Override
            public void onFrequencySelected(String frequency) {
                // Handle the selected frequency here
                binding.buttonFrequency.setText(frequency);
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
        binding.edtServerIp.setText(ip);
        binding.edtPort.setText(String.format(Locale.getDefault(), "%d", port));
        binding.edtLocalUser.setText(username);
        binding.edtLocalPassword.setText(password);
        binding.edtLocalChannel.setText(wifiDirectLocalChannel);
        String finalUrl;
        if (!TextUtils.isEmpty(username)) {
            finalUrl = String.format(Locale.getDefault(), "rtsp://%s:%s@%s:%d/%s", username, password, ip, port, wifiDirectLocalChannel);
        } else {
            finalUrl = String.format(Locale.getDefault(), "rtsp://%s:%d/%s", ip, port, wifiDirectLocalChannel);
        }
        binding.localPathValue.setText(finalUrl);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof ActivityFragmentCallbacks) {
            mListener = (ActivityFragmentCallbacks) getActivity();
        } else {
            Log.e("StreamingFragment", "Activity does not implement ActivityFragmentCallbacks");
        }
    }

    void onLocalUpdateClicked() {
        // Grab the local fields...
        String ip   = binding.edtServerIp.getText().toString().trim();
        String port = binding.edtPort.getText().toString().trim();
        String user = binding.edtLocalUser.getText().toString().trim();
        String pass = binding.edtLocalPassword.getText().toString().trim();
        String channel = binding.edtLocalChannel.getText().toString().trim();

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
        binding.localPathValue.setText(finalUrl);
        MessageUtil.showToast(getContext(), "Updating local settings with IP: " + ip);
    }

    void setupViewsForConfigs(){
       new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
           @Override
           public void run() {
               int streamingMode = AppPreference.getInt(AppPreference.KEY.STREAMING_MODE, 0);
               if (streamingMode == 1) {
                   binding.buttonMode.setText("Local Streaming");
                   binding.localFieldsContainer.setVisibility(View.VISIBLE);
                   binding.rowGps.setVisibility(View.GONE);
                   binding.rowFrequency.setVisibility(View.GONE);
                   binding.viewShare.setVisibility(View.GONE);
                   binding.lyShare.setVisibility(View.GONE);
                   View txtSpeedTest = (rootView != null) ? rootView.findViewById(R.id.txt_speed_test) : null;
                   if (txtSpeedTest != null) {
                       txtSpeedTest.setVisibility(View.GONE);
                   } else {
                       Log.e("StreamingFragment", "View not available");
                   }
               } else {
                   Log.e(TAG, "drawing:");
                   binding.buttonMode.setText("Cloud Streaming");
                   binding.localFieldsContainer.setVisibility(View.GONE);
                   binding.rowGps.setVisibility(View.VISIBLE);
                   binding.rowFrequency.setVisibility(View.VISIBLE);
                   binding.viewShare.setVisibility(View.VISIBLE);
                   binding.lyShare.setVisibility(View.VISIBLE);
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
    // Spinner logic for Cloud vs Local
    private void setupStreamingModeSpinner() {
        binding.buttonMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MessageDialog messageDialog = MessageDialog.show("Streaming Mode", "Please choose the streaming Mode","Local Streaming" , "Cloud Streaming").setCancelButton(new OnDialogButtonClickListener<MessageDialog>() {
                    @Override
                    public boolean onClick(MessageDialog dialog, View v) {
                        dialog.dismiss();
                        // Show Cloud stuff
                        binding.buttonMode.setText("Cloud Streaming");
                        binding.localFieldsContainer.setVisibility(View.GONE);
                        binding.rowGps.setVisibility(View.VISIBLE);
                        binding.rowFrequency.setVisibility(View.VISIBLE);
                        binding.viewShare.setVisibility(View.VISIBLE);
                        binding.lyShare.setVisibility(View.VISIBLE);
                        View txtSpeedTest = (rootView != null) ? rootView.findViewById(R.id.txt_speed_test) : null;
                        if (txtSpeedTest != null) {
                            txtSpeedTest.setVisibility(View.GONE);
                        } else {
                            Log.e("StreamingFragment", "View not available");
                        }
                        if (txtSpeedTest != null) {
                            txtSpeedTest.setVisibility(View.VISIBLE);
                        }
                        AppPreference.setInt(AppPreference.KEY.STREAMING_MODE, 0);
                        return false;
                    }
                }).setOkButton(new OnDialogButtonClickListener<MessageDialog>() {
                    @Override
                    public boolean onClick(MessageDialog baseDialog, View v) {
                        baseDialog.dismiss();
                        binding.buttonMode.setText("Local Streaming");
                        binding.localFieldsContainer.setVisibility(View.VISIBLE);
                        binding.rowGps.setVisibility(View.GONE);
                        binding.rowFrequency.setVisibility(View.GONE);
                        binding.viewShare.setVisibility(View.GONE);
                        binding.lyShare.setVisibility(View.GONE);
                        View txtSpeedTest = (rootView != null) ? rootView.findViewById(R.id.txt_speed_test) : null;
                        if (txtSpeedTest != null) {
                            txtSpeedTest.setVisibility(View.GONE);
                        } else {
                            Log.e("StreamingFragment", "View not available");
                        }
                        if (txtSpeedTest != null) {
                            txtSpeedTest.setVisibility(View.GONE);
                        }
                        AppPreference.setInt(AppPreference.KEY.STREAMING_MODE, 1);
                        return false;
                    }
                });
                messageDialog.setOkTextInfo(new TextInfo().setFontColor(Color.parseColor("#000000")).setBold(true));
                messageDialog.setCancelTextInfo(new TextInfo().setFontColor(Color.parseColor("#000000")).setBold(true));
                messageDialog.show();
            }
        });
    }

    // Show initial login UI
    void showInitialLogin() {
        binding.lyUsername.setVisibility(View.GONE);
        binding.lyPassword.setVisibility(View.GONE);
        binding.btnLogin.setVisibility(View.GONE);
        binding.btnPassword.setVisibility(View.VISIBLE);
        binding.btnNewUser.setVisibility(View.VISIBLE);
        binding.btnResetPassword.setVisibility(View.VISIBLE);
        binding.btnCode.setVisibility(View.GONE);
        binding.viewLogin.setVisibility(View.VISIBLE);
        binding.btnBack.setVisibility(View.GONE);
        binding.btnSend.setVisibility(View.GONE);
        binding.viewStream.setVisibility(View.GONE);
    }

    // Basic initialization for streaming UI
    void initialize() {
        if (user_info == null) {
            binding.viewShare.setVisibility(View.GONE);
            binding.viewStream.setVisibility(View.GONE);
            showInitialLogin();
            return;
        }
        if (myDevice() != null) {
            binding.edtName.setText(myDevice().name);
            AppPreference.setStr(AppPreference.KEY.DEVICE_NAME, binding.edtName.getText().toString());
        }
        binding.viewStream.setVisibility(View.VISIBLE);
        if (!user_info.isAdmin()) {
            binding.lyShare.setVisibility(View.GONE);
        }

        binding.swtStreaming.setChecked(AppPreference.getBool(AppPreference.KEY.BROADCAST, true));
        binding.swtStreaming.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            AppPreference.setBool(AppPreference.KEY.BROADCAST, isChecked);
            if (!isChecked) {
                if (mActivity != null && mActivity.mCamService != null) {
                    if (mListener != null) {
                       // mListener.fragStopStreaming();
                    }
                }
                binding.swtGps.setChecked(true);
                AppPreference.setBool(AppPreference.KEY.GPS_ENABLED, false);
                stopTimer();
            }
        });

        binding.swtGps.setChecked(AppPreference.getBool(AppPreference.KEY.GPS_ENABLED, false));
        binding.swtGps.setOnCheckedChangeListener((buttonView, isChecked) -> {
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

        if (binding.swtGps.isChecked()) {
            if (mListener != null) {
                mListener.fragStartLocationService();
                // Add a small delay to ensure service is started
                new Handler().postDelayed(() -> {
                    updateGPSLocation();
                }, 1000);
            }
        }

        binding.swtRecording.setChecked(AppPreference.getBool(AppPreference.KEY.RECORD_BROADCAST, false));
        binding.swtRecording.setOnCheckedChangeListener((compoundButton, b) ->
                AppPreference.setBool(AppPreference.KEY.RECORD_BROADCAST, b)
        );

        if (user_info != null) {
            binding.txtUser.setText(AppPreference.getStr(AppPreference.KEY.LOGIN_EMAIL, ""));
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
            binding.txtAccountType.setText(user_info.account_status);
        } else {
            binding.txtAccountType.setText(String.format("%s(Expires %s)",
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
        binding.viewShare.setVisibility(View.VISIBLE);
        adapter.notifyDataSetChanged();
        CommonUtil.setListViewHeightBasedOnItems(binding.listShare);
    }

    // Starts location update timer
    private long lastManualUpdateTime = 0;

    private long lastApiUpdateTime = 0;

    public void updateGPSLocation() {
        if (mActivity != null) {
            String latitude = "", longitude = "";
            if (binding.swtGps.isChecked()) {
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
        binding.viewLogin.setVisibility(View.GONE);
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

    // Show the login form
    void showLoginForm() {
        binding.viewLogin.setVisibility(View.VISIBLE);
        binding.viewStream.setVisibility(View.GONE);
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
        binding.edtUsername.setText("");
        binding.edtPassword.setText("");
        binding.edtName.setText("");
        if (mActivity != null && mActivity.mCamService != null) {
            mActivity.mCamService.stopStreaming();
        }
        if (LiveFragment.getInstance() != null) {
            LiveFragment.getInstance().getTxtSpeed().setText("");
        }
    }

    // Called when user tries to log in
    void onLogin() {
        if (mActivity == null) return;
        String email = binding.edtUsername.getText().toString().trim();
        String password = binding.edtPassword.getText().toString().trim();
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
                    binding.viewLogin.setVisibility(View.GONE);
                    binding.viewStream.setVisibility(View.VISIBLE);

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
        String email = binding.edtUsername.getText().toString().trim();
        String password = binding.edtPassword.getText().toString().trim();
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
                                binding.viewLogin.setVisibility(View.GONE);
                                binding.viewStream.setVisibility(View.VISIBLE);

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
        binding.viewLogin.setVisibility(View.VISIBLE);
        binding.viewStream.setVisibility(View.GONE);
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
            String name = nameDialog.getEdtCode().getText().toString().trim();
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
                            binding.edtName.setText(name);
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

    // For "New User" click
    void doLogin() {
        binding.viewLogin.setVisibility(View.VISIBLE);
        binding.lyUsername.setVisibility(View.VISIBLE);
        binding.lyPassword.setVisibility(View.VISIBLE);
        binding.btnLogin.setVisibility(View.VISIBLE);
        binding.btnPassword.setVisibility(View.GONE);
        binding.btnResetPassword.setVisibility(View.GONE);
        binding.btnNewUser.setVisibility(View.GONE);
        binding.btnBack.setVisibility(View.VISIBLE);
        binding.btnSend.setVisibility(View.GONE);
        binding.viewStream.setVisibility(View.GONE);
    }

    // For "Reset Password" click
    void resetPassword() {
        binding.lyUsername.setVisibility(View.VISIBLE);
        binding.btnNewUser.setVisibility(View.GONE);
        binding.btnPassword.setVisibility(View.GONE);
        binding.btnResetPassword.setVisibility(View.GONE);
        binding.btnBack.setVisibility(View.VISIBLE);
        binding.btnSend.setVisibility(View.VISIBLE);
        binding.viewStream.setVisibility(View.GONE);
        binding.viewLogin.setVisibility(View.VISIBLE);
    }

    // For "Need Password" click
    void needPassword() {
        binding.lyUsername.setVisibility(View.VISIBLE);
        binding.btnNewUser.setVisibility(View.GONE);
        binding.btnResetPassword.setVisibility(View.GONE);
        binding.btnPassword.setVisibility(View.GONE);
        binding.btnCode.setVisibility(View.VISIBLE);
        binding.btnBack.setVisibility(View.VISIBLE);
        binding.btnSend.setVisibility(View.GONE);
        binding.viewStream.setVisibility(View.GONE);
        binding.viewLogin.setVisibility(View.VISIBLE);
    }

    // Clicking "Share new" invites user by email
    void onShare() {
        if (mActivity != null) {
            InviteDialog codeDialog = new InviteDialog(mActivity);
            if (mListener != null) {
                mListener.isDialog(true);
            }
            codeDialog.setCloseListener(view -> {
                if (mListener != null) {
                    mListener.isDialog(false);
                }
                codeDialog.dismiss();
            });
            codeDialog.setOkListener(view -> {
                if (mListener != null) {
                    mListener.isDialog(false);
                }
                String email = codeDialog.getEdtEmail().getText().toString();
                if (!CommonUtil.isValidEmail(email)) {
                    MessageUtil.showToast(mActivity, R.string.invalid_email);
                } else {
                    share(email);
                }
                codeDialog.dismiss();
            });
            codeDialog.show();
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
            String email = binding.edtUsername.getText().toString().trim();
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
        String email = binding.edtUsername.getText().toString().trim();
        binding.edtPassword.setText("");
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
        String email = binding.edtUsername.getText().toString().trim();
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
            String password = pwdDialog.getEdtPassword().getText().toString();
            String re_password = pwdDialog.getEdtRepassword().getText().toString();
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
        String email = binding.edtUsername.getText().toString().trim();
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
                                binding.edtPassword.setText(password);
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
                String password = pwdDialog.getEdtPassword().getText().toString();
                String re_password = pwdDialog.getEdtRepassword().getText().toString();
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
            String name = binding.edtName.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                MessageUtil.showToast(mActivity, R.string.no_name);
                return;
            }
            String latitude = "", longitude = "";
            if (binding.swtGps.isChecked()) {
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
        if (binding.swtGps.isChecked()) {
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
        if (binding.swtGps.isChecked()) {
            lat = String.valueOf(LocationManagerService.lat);
            lng = String.valueOf(LocationManagerService.lng);
        }
        updateDeviceInfo(AppPreference.getStr(AppPreference.KEY.DEVICE_NAME, ""), lat, lng, streaming, true);
    }

    public void updateDeviceStreaming(boolean streaming, boolean showing) {
        String lat = "", lng = "";
        if (binding.swtGps.isChecked()) {
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
            String serverIP = binding.edtServerIp.getText().toString().trim();
            String port = binding.edtPort.getText().toString().trim();
            String localUser = binding.edtLocalUser.getText().toString().trim();
            String localPassword = binding.edtLocalPassword.getText().toString().trim();

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
            if (!binding.swtStreaming.isChecked()) {
                MessageUtil.showToast(mActivity, R.string.stream_disabled);
                return;
            }
            // If you still needed these old fields:
            //   edt_url is hidden in your new UI
            //   We have `edt_channel`, `edt_username`, `edt_password` for Cloud
            String channel = binding.edtChannel.getText().toString().trim();
            if (TextUtils.isEmpty(channel)) {
                MessageUtil.showToast(mActivity, R.string.no_channel);
                return;
            }
            String uname = binding.edtUsername.getText().toString().trim();
            if (TextUtils.isEmpty(uname)) {
                MessageUtil.showToast(mActivity, R.string.invalid_username);
                return;
            }
            String pwd = binding.edtPassword.getText().toString().trim();
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

    // Additional OnClick references from your layout
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_share) {
            if (mListener != null) {
                mListener.isDialog(true);
            }
            if (mActivity != null) {
                mActivity.startActivity(new Intent(mActivity, ShareActivity.class));
            }
        } else if (id == R.id.btn_logout) {
            if (mListener != null) {
                mListener.isDialog(true);
            }
            MessageDialog logoutConfirm = MessageDialog.show(
                            getString(R.string.unregister),
                            getString(R.string.label_logout),
                            getString(R.string.Okay),
                            getString(R.string.cancel))
                    .setCancelButton((dialog, v1) -> {
                        dialog.dismiss();
                        return false;
                    })
                    .setOkButton((baseDialog, v12) -> {
                        onLogout();
                        baseDialog.dismiss();
                        return false;
                    });
            logoutConfirm.setOkTextInfo(new TextInfo().setFontColor(Color.parseColor("#000000")).setBold(true));
            logoutConfirm.setCancelTextInfo(new TextInfo().setFontColor(Color.parseColor("#000000")).setBold(true));
        } else if (id == R.id.ly_share) {
            onShare();
        } else if (id == R.id.btn_refresh) {
            binding.edtUsername.setText(AppPreference.getStr(AppPreference.KEY.LOGIN_EMAIL, ""));
            binding.edtPassword.setText(AppPreference.getStr(AppPreference.KEY.LOGIN_PASSWORD, ""));
            onLogin();
        } else if (id == R.id.btn_back) {
            showInitialLogin();
        } else if (id == R.id.btn_login) {
            onLogin();
        } else if (id == R.id.btn_password) {
            needPassword();
        } else if (id == R.id.btn_reset_password) {
            resetPassword();
        } else if (id == R.id.btn_new_user) {
            doLogin();
        } else if (id == R.id.btn_code) {
            onPassword();
        } else if (id == R.id.txt_qr) {
            if (mListener != null) {
                mListener.isDialog(true);
            }
            if (mActivity != null) {
                Intent i = new Intent(mActivity, QrCodeActivity.class);
                startActivityForResult(i, REQUEST_CODE_QR_SCAN);
            }
        }
    }

    // Handle results, e.g. scanning a channel QR
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mListener != null) {
            mListener.isDialog(false);
        }
        if (resultCode == RESULT_OK && data != null && requestCode == REQUEST_CODE_QR_SCAN) {
            String result = data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult");
            binding.edtChannel.setText(result);
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
                    binding.btnStart.setEnabled(true);
                    binding.btnStart.setText(R.string.action_title_start);
                    if (LiveFragment.getInstance() != null && LiveFragment.getInstance().isAdded()) {
                        if (!AppPreference.getBool(AppPreference.KEY.STREAM_STARTED, false)) {
                            LiveFragment.getInstance().getIcStream().setImageResource(R.mipmap.ic_stream);
                        }
                        LiveFragment.getInstance().is_streaming = false;
                        stopStream();
                    }
                } else if (TextUtils.equals(body, onlineStr)) {
                    binding.btnStart.setEnabled(true);
                    binding.btnStart.setText(R.string.action_title_stop);
                    if (LiveFragment.getInstance() != null && LiveFragment.getInstance().isAdded()) {
                        LiveFragment.getInstance().getIcStream().setImageResource(R.mipmap.ic_stream_active);
                        startStream();
                    }
                } else if (TextUtils.equals(body, connectingStr)) {
                    binding.btnStart.setEnabled(false);
                    binding.btnStart.setText(R.string.action_title_stop);
                    if (LiveFragment.getInstance() != null && LiveFragment.getInstance().isAdded()) {
                        if (mActivity.isStreaming()) {
                            startStream();
                        } else {
                            stopStream();
                        }
                    }
                }
                binding.txtStatus.setText(body);
            } finally {
                isUpdatingStatus = false;
            }
        });
    }
    void startStream(){
        boolean isStreaming = AppPreference.getBool(AppPreference.KEY.STREAM_STARTED, false);
        if (isStreaming) {
            if (LiveFragment.getInstance() != null && LiveFragment.getInstance().isAdded()) {
                LiveFragment.getInstance().getIcStream().setImageResource(R.mipmap.ic_stream_active);
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
                LiveFragment.getInstance().getIcStream().setImageResource(R.mipmap.ic_stream);
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
        
        // Guard: Don't update speed if streaming is not active or service is null
        boolean eglManagerExists = mActivity.sharedEglManager != null;
        boolean eglStreaming = eglManagerExists && mActivity.sharedEglManager.mStreaming;
        boolean camServiceExists = mActivity.mCamService != null;
        boolean camServiceStreaming = camServiceExists && mActivity.mCamService.isStreaming();
        
        // Throttle speed updates to reduce frequency
        long currentTime = System.currentTimeMillis();
        if (currentTime - mLastSpeedUpdateTime < SPEED_UPDATE_THROTTLE_MS) {
            return; // Skip this update
        }
        mLastSpeedUpdateTime = currentTime;
        
        Log.d(TAG, "updateSpeed: eglManager=" + eglManagerExists + ", eglStreaming=" + eglStreaming + 
                   ", camService=" + camServiceExists + ", camStreaming=" + camServiceStreaming + ", speed=" + speed);
        
        if (!eglManagerExists || !eglStreaming || !camServiceExists || !camServiceStreaming) {
            Log.d(TAG, "updateSpeed: Ignoring speed update - conditions not met");
            return;
        }
        
        mActivity.runOnUiThread(() -> {
            binding.txtSpeed.setText(speed);
            binding.txtSpeed.setTextColor(Color.WHITE);
            if (LiveFragment.getInstance() != null) {
                LiveFragment.getInstance().getTxtSpeed().setTextColor(Color.WHITE);
                String channel = AppPreference.getStr(AppPreference.KEY.STREAM_CHANNEL, "");
                if (!TextUtils.isEmpty(speed) && !TextUtils.isEmpty(channel)) {
                                    Streamer.Size size = mActivity.sharedEglManager.videoSize;
                String chInfo = String.format("Channel: %s %s", channel, speed);
                if (mActivity.sharedEglManager.mStreamer != null && size != null) {
                    double fps = mActivity.sharedEglManager.mStreamer.getFps();
                    String resInfo = String.format("Resolution: %d x %d, %.2f fps", size.width, size.height, fps);
                    LiveFragment.getInstance().getTxtSpeed().setText(String.format("%s\n%s", chInfo, resInfo));
                } else if (size == null) {
                    // If videoSize is null, just show channel info without resolution
                    LiveFragment.getInstance().getTxtSpeed().setText(chInfo);
                }
                } else {
                    LiveFragment.getInstance().getTxtSpeed().setText("");
                }
                // Don't call handleStreamView here - it's causing UI flicker
                // The stream view should only be updated when stream state actually changes
                // LiveFragment.getInstance().handleStreamView();
            }
            Log.d(TAG, "Raw speed: " + speed);
            processSpeedValue(speed);
        });
    }

    // Add these member variables
    private long mLowSpeedStartTime = -1;
    private long mLastSpeedUpdateTime = 0;
    private static final long SPEED_UPDATE_THROTTLE_MS = 5000; // Only update every 5 seconds
    private final Handler mSpeedHandler = new Handler(Looper.getMainLooper());
    private final Runnable mSpeedCheckRunnable = new Runnable() {
        @Override
        public void run() {
            checkSpeedDuration();
            mSpeedHandler.postDelayed(this, 1000);
        }
    };

    private void processSpeedValue(String speed) {
        double currentSpeed = parseSpeed(speed);
        Log.d(TAG, "Parsed speed: " + currentSpeed);

        if (currentSpeed > 36.0) {
            // Speed is good - reset tracking
            if (mLowSpeedStartTime != -1) {
                Log.d(TAG, "Speed improved to " + currentSpeed + ", resetting timer");
                mLowSpeedStartTime = -1;
            }
        } else if (currentSpeed > 0) {  // Valid speed ≤30
            long now = System.currentTimeMillis();

            if (mLowSpeedStartTime == -1) {
                // Start tracking low speed
                mLowSpeedStartTime = now;
                Log.d(TAG, "Low speed detected: " + currentSpeed + ", starting timer");
            } else {
                // Log ongoing low speed duration
                long duration = now - mLowSpeedStartTime;
                Log.d(TAG, "Low speed ongoing: " + currentSpeed +
                        " for " + duration + "ms");
            }
        }
    }

    private void checkSpeedDuration() {
        if (mLowSpeedStartTime == -1) return;

        long now = System.currentTimeMillis();
        long duration = now - mLowSpeedStartTime;
        Log.d(TAG, "Checking low speed duration: " + duration + "ms");

        if (duration >= 30000) {
            Log.d(TAG, "LOW SPEED CONDITION MET: 6+ seconds");
            onLowSpeedDetected();
            // Reset to detect new periods
            mLowSpeedStartTime = -1;
        }
    }


    private double parseSpeed(String speed) {
        if (TextUtils.isEmpty(speed)) {
            return -1;
        }
        try {
            // Normalize the string to lowercase for easier handling
            String lowerSpeed = speed.toLowerCase();

            // Check if value is in Mbps (e.g., "1.23 Mbps")
            if (lowerSpeed.contains("mbps")) {
                String numberPart = lowerSpeed.replace("mbps", "")
                        .replaceAll("[^\\d.]", "")
                        .trim();
                if (!numberPart.isEmpty()) {
                    double mbps = Double.parseDouble(numberPart);
                    // Convert Mbps to Kbps (1 Mbps = 1000 Kbps)
                    return mbps * 1000;
                }
            }
            // Check if value is in Kbps (e.g., "28.1Kbps")
            else if (lowerSpeed.contains("kbps")) {
                String numberPart = lowerSpeed.replace("kbps", "")
                        .replaceAll("[^\\d.]", "")
                        .trim();
                if (!numberPart.isEmpty()) {
                    return Double.parseDouble(numberPart);
                }
            }
            // Fallback: extract first numeric value
            else {
                Pattern pattern = Pattern.compile("(\\d+\\.?\\d*)");
                Matcher matcher = pattern.matcher(lowerSpeed);
                if (matcher.find()) {
                    return Double.parseDouble(matcher.group(1));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Speed parsing error: " + speed, e);
        }
        return -1;
    }

    private void onLowSpeedDetected() {
        // YOUR ACTION HERE
        int position = AppPreference.getSafeIntPreference(AppPreference.KEY.STREAMING_MODE, 0);
        if (position != 3) {
            Log.w(TAG, "Speed ≤30 kbps for 6+ seconds - PERFORMING ACTION");
            // Example: mActivity.showLowSpeedWarning();
            LiveFragment fragment = LiveFragment.getInstance();
            if (fragment != null && fragment.isAdded()) {
                if (!fragment.is_cast_opened) {
                    fragment.onItemSelected(null,null, position, 0);
                    fragment.isItemRestart = true;
                }
            } else {
                Log.w(TAG, "LiveFragment not available for low speed warning");
            }
        }
    }

    public void startSpeedMonitoring() {
        Log.d(TAG, "Starting speed monitoring");
        mLowSpeedStartTime = -1;
        mSpeedHandler.post(mSpeedCheckRunnable);
    }

    public void stopSpeedMonitoring() {
        Log.d(TAG, "Stopping speed monitoring");
        mSpeedHandler.removeCallbacks(mSpeedCheckRunnable);
    }

    // Call this when you start receiving speed updates
    public void initMonitoring() {
        startSpeedMonitoring();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSpeedMonitoring();
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
            TextView txt_action;
            TextView txt_share;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.row_text, parent, false);
                holder = new ViewHolder();
                holder.txt_email = convertView.findViewById(R.id.txt_email);
                holder.txt_status = convertView.findViewById(R.id.txt_status);
                holder.txt_action = convertView.findViewById(R.id.txt_action);
                holder.txt_share = convertView.findViewById(R.id.txt_share);
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
                                CommonUtil.setListViewHeightBasedOnItems(binding.listShare);
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
    
    // ===== FRAGMENT VISIBILITY METHODS =====
    
    @Override
    public void onFragmentVisible() {
        Log.d("StreamingFragment", "Fragment became visible");
        if (isAdded() && getActivity() != null) {
            // Update streaming UI
        }
    }
    
    @Override
    public void onFragmentHidden() {
        Log.d("StreamingFragment", "Fragment became hidden");
        // Simple cleanup
    }
}
