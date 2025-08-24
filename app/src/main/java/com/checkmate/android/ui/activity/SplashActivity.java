package com.checkmate.android.ui.activity;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.Manifest;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.blikoon.qrcodescanner.QrCodeActivity;
import com.checkmate.android.AppConstant;
import com.checkmate.android.AppPreference;
import com.checkmate.android.BuildConfig;
import com.checkmate.android.R;
import com.checkmate.android.networking.HttpApiService;
import com.checkmate.android.util.CommonUtil;
import com.checkmate.android.util.Crypto;
import com.checkmate.android.util.DeviceUtils;
import com.checkmate.android.util.MainActivity;
import com.checkmate.android.util.MessageUtil;
import com.checkmate.android.util.ResourceUtil;
import com.checkmate.android.util.RootCommandExecutor;
import com.checkmate.android.util.PreferenceInitializer;
import com.checkmate.android.ui.dialog.ActivationCompleteDialog;
import com.checkmate.android.ui.dialog.ActivationDialog;
import com.checkmate.android.ui.dialog.CodeDialog;
import com.checkmate.android.ui.dialog.MachineCodeDialog;
import com.checkmate.android.ui.dialog.SerialDialog;
import com.blikoon.qrcodescanner.QrCodeActivity;
import com.checkmate.android.util.StoragePermissionHelper;
import com.checkmate.android.util.SplashStorageHelper;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.view.WindowManager;
import java.util.Arrays;


public class SplashActivity extends BaseActivity {

    static final String TAG = "SplashActivity";
    private static final int PERMISSION_REQUEST_CODE_FOR_PERMISSION = 1;
    private static final int REQUEST_CODE_QR_SCAN_SERIAL_NUMBER = 101;
    private static final int REQUEST_CODE_QR_SCAN_ACTIVATION = 102;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 105; // Define your request code
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 106; // Notification permission request code

    private static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.VIBRATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.CAPTURE_AUDIO_OUTPUT
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Create notification channels for Android 14 compatibility
        createNotificationChannels();

        // Remove notification permission request from onCreate - will be requested after activation

        // Prevent screen blinking when notifications appear
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // Additional flags to prevent blinking and ensure stable display
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);

        // Set window to be stable and prevent focus changes from causing blinking
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        // Ensure the window is stable and doesn't get affected by system UI changes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.blue_dark));
        }

        runADBCommands();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        connection_manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // Prevent screen blinking when focus changes (e.g., when notifications appear)
        if (hasFocus) {
            // Ensure the window stays stable
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Keep the screen on even when paused to prevent blinking
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensure window stability when resuming
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);

        if (AppPreference.getBool(AppPreference.KEY.APP_FORCE_QUIT, true)) {
            AppPreference.setBool(AppPreference.KEY.APP_FORCE_QUIT, true);
            verifyPermissions(this);
        } else {
            MessageUtil.showToast(getApplicationContext(), "CheckMate! Will be closing. Please use main icon to open.");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent startMain = new Intent(Intent.ACTION_MAIN);
                    startMain.addCategory(Intent.CATEGORY_HOME);
                    startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(startMain);
                    AppPreference.setBool(AppPreference.KEY.APP_FORCE_QUIT, true);
                    finish();
                }
            }, 2500);
        }
    }

    void runADBCommands(){
        int capOut = ActivityCompat.checkSelfPermission(getApplicationContext(), "android.permission.CAPTURE_AUDIO_OUTPUT");
        if (capOut == PackageManager.PERMISSION_GRANTED) {
            RootCommandExecutor.disableProjectionConfirmation();
            RootCommandExecutor.hideCameraGreenDot();
        }
    }

    ConnectivityManager connection_manager;

    public void verifyPermissions(Activity activity) {
        int permission0 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);
        int permission1 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permission2 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        int permission3 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO);
        int permission4 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.VIBRATE);
        int permission5 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.MODIFY_AUDIO_SETTINGS);
        int permission6 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE);
        int permission7 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_WIFI_STATE);
        int permission8 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CHANGE_WIFI_STATE);
        int permission9 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION);
        int permission10 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION);
        int permission11 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CHANGE_NETWORK_STATE);
        int permission12 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WAKE_LOCK);

        if (permission0 != PackageManager.PERMISSION_GRANTED
                || permission1 != PackageManager.PERMISSION_GRANTED
                || permission2 != PackageManager.PERMISSION_GRANTED
                || permission3 != PackageManager.PERMISSION_GRANTED
                || permission4 != PackageManager.PERMISSION_GRANTED
                || permission5 != PackageManager.PERMISSION_GRANTED
                || permission6 != PackageManager.PERMISSION_GRANTED
                || permission7 != PackageManager.PERMISSION_GRANTED
                || permission8 != PackageManager.PERMISSION_GRANTED
                || permission9 != PackageManager.PERMISSION_GRANTED
                || permission10 != PackageManager.PERMISSION_GRANTED
                || permission11 != PackageManager.PERMISSION_GRANTED
                || permission12 != PackageManager.PERMISSION_GRANTED
        ) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS,
                    PERMISSION_REQUEST_CODE_FOR_PERMISSION
            );

            requestDrawOverAppsPermission(activity);
            requestIgnoreBatteryOptimizationsPermission(activity);
            requestDoNotDisturbPermission(activity);
            verifyStoragePermissions(activity);
            requestModifySystemSettingsPermission(activity);
            checkStoragePermissions();
        } else {
            gotoNextView();
        }
    }

    public void verifyStoragePermissions(Activity activity) {
        List<String> requiredPermissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            requiredPermissions.add(Manifest.permission.READ_MEDIA_VIDEO);
            requiredPermissions.add(Manifest.permission.READ_MEDIA_AUDIO);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requiredPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            requiredPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            requiredPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        List<String> missingPermissions = new ArrayList<>();
        for (String permission : requiredPermissions) {
            if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(activity,
                    missingPermissions.toArray(new String[0]),
                    REQUEST_STORAGE_PERMISSION);
        } else {
            Log.d("Storage", "All permissions granted!");
        }
    }


    private static final int REQUEST_STORAGE_PERMISSION = 1001;

    private void checkStoragePermissions() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Handle storage permissions using StoragePermissionHelper
        StoragePermissionHelper.handlePermissionResult(this, requestCode, permissions, grantResults);

        // Handle storage helper permissions
        if (storageHelper != null) {
            storageHelper.handlePermissionResult(requestCode, permissions, grantResults);
        }

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Camera", "Permission granted!");
            } else {
                Log.d("Camera", "Permission denied!");
            }
        }

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Notification", "Notification permission granted!");
                Toast.makeText(this, "Notifications enabled successfully!", Toast.LENGTH_SHORT).show();
                openMainScreen();
            } else {
                Log.d("Notification", "Notification permission denied!");
                // Show dialog to guide user to settings
                navigateToNotificationSettings();
            }
        }
    }

    public void requestDoNotDisturbPermission(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (!notificationManager.isNotificationPolicyAccessGranted()) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    public void requestIgnoreBatteryOptimizationsPermission(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        if (!powerManager.isIgnoringBatteryOptimizations(context.getPackageName())) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    public void requestDrawOverAppsPermission(Context context) {
        if (!Settings.canDrawOverlays(context)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    public void requestModifySystemSettingsPermission(Context context) {
        if (!Settings.System.canWrite(context)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    private boolean notificationAsked = false;

    private void maybeAskNotificationThenMain() {
        if (notificationAsked) {
            openMainScreen();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            notificationAsked = true;
            navigateToNotificationSettings(); // this calls openMainScreen() on skip/continue
        } else {
            openMainScreen();
        }
    }

    boolean is_dialog_show = false;
    boolean should_show_complete = false;
    private SplashStorageHelper storageHelper;

    public synchronized String getUniqueChannelName(Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String uniqueID = sharedPrefs.getString("PREF_UNIQUE_ID", null);
        if (uniqueID == null) {
            uniqueID =  UUID.randomUUID().toString();
            sharedPrefs.edit().putString("PREF_UNIQUE_ID", uniqueID).apply();
        }
        return uniqueID;
    }
    void gotoNextView() {
        if (is_dialog_show) {
            return;
        }
        // check activation
        String activation = AppPreference.getStr(AppPreference.KEY.ACTIVATION_CODE, "");
        if (TextUtils.isEmpty(activation)) {
            @SuppressLint("HardwareIds") String androidId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (androidId == null || androidId.isEmpty()) {
                // Fallback: Use a random UUID if ANDROID_ID is not available.
                androidId = java.util.UUID.randomUUID().toString();
            }
            // Initialize activation-specific defaults using PreferenceInitializer
            PreferenceInitializer.initActivationDefaults(this);
            // Initialize storage helper
            storageHelper = new SplashStorageHelper(this, new SplashStorageHelper.StorageSelectionCallback() {
                @Override
                public void onStorageSelected(String storagePath, String storageType) {
                    Log.d(TAG, "Storage selected: " + storagePath + " (" + storageType + ")");
                    // After storage is selected, continue with activation
                    continueWithActivation();
                }
                @Override
                public void onStorageSelectionCancelled() {
                    Log.w(TAG, "Storage selection cancelled");
                    // Use default storage and continue
                    PreferenceInitializer.validateStorageLocation(SplashActivity.this);
                    continueWithActivation();
                }
            });
            // Show storage selection dialog and wait for user selection
            storageHelper.showCustomStorageDialog();
            return;
        } else { // check
            if (!isValidActivation(activation)) {
                gotoNextView();
                return;
            }
        }
        // After activation and storage selection, ask for notification permission before main
        if (should_show_complete) {
            should_show_complete = false;
            is_dialog_show = true;
            ActivationCompleteDialog com_dialog = new ActivationCompleteDialog(this);
            com_dialog.setOkListener(view -> {
                is_dialog_show = false;
                com_dialog.dismiss();
                maybeAskNotificationThenMain();
            });
            com_dialog.show();
        } else {
            maybeAskNotificationThenMain();
        }
    }

    /**
     * Continue with activation after storage is configured
     */
    private void continueWithActivation() {
        // Validate and fix storage location if needed
        PreferenceInitializer.validateStorageLocation(this);
        // Check storage permissions and request if needed using StoragePermissionHelper
        if (!StoragePermissionHelper.areStoragePermissionsGranted(this)) {
            Log.d(TAG, "Storage permissions not granted, requesting permissions");
            StoragePermissionHelper.requestStoragePermissions(this);
        }
        should_show_complete = true;
        is_dialog_show = true;
        ActivationDialog activationDialog = new ActivationDialog(this);
        activationDialog.setOkListener(view -> {
            activationDialog.dismiss();
            is_dialog_show = false;
            setSerialNumber();
        });
        activationDialog.setCloseListener(view -> {
            is_dialog_show = false;
            activationDialog.dismiss();
            finish();
        });
        activationDialog.show();
    }


    void openMainScreen() {

        if (AppPreference.getBool(AppPreference.KEY.APP_FIRST_LAUNCH, true)) {
            AlertDialog.Builder  builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.convert_mode)
                    .setMessage(getString(R.string.confirm_convert))
                    .setIcon(R.mipmap.ic_launcher)
                    .setCancelable(false)
                    .setPositiveButton(R.string.configure_covert, (dialog, whichButton) -> {
                        setPinCode();
                    })
                    .setNegativeButton(R.string.without_configure_covert, (dialog, whichButton) -> {
                        chooseAudioOption();
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

        } else {
            new Handler().postDelayed(() -> {
                if (AppPreference.getBool(AppPreference.KEY.UI_CONVERT_MODE, false)) {
                } else {
                    AppPreference.setBool(AppPreference.KEY.UI_CONVERT_MODE, false);
                    AppPreference.setBool(AppPreference.KEY.APP_FIRST_LAUNCH, false);
                }
                openMain();
            }, 2000);
        }
    }

    void chooseAudioOption() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.choose_audio)
                .setMessage(getString(R.string.confirm_audio))
                .setIcon(R.mipmap.ic_launcher)
                .setCancelable(false)
                .setPositiveButton(R.string.YES, (dialog, whichButton) -> {
                    AppPreference.setBool(AppPreference.KEY.USE_AUDIO, true);
                })
                .setNegativeButton(R.string.NO, (dialog, whichButton) -> {
                    AppPreference.setBool(AppPreference.KEY.USE_AUDIO, false);
                })
                .setOnDismissListener(dialogInterface -> {
                    AppPreference.setBool(AppPreference.KEY.APP_FIRST_LAUNCH, false);
                    openMain();
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

    void openMain() {

        float app_version = Float.parseFloat(CommonUtil.getVersionCode(this));
        String old = AppPreference.getStr(AppPreference.KEY.APP_OLD_VERSION, "");
        float old_version = 0f;
        if (!TextUtils.isEmpty(old)) {
            old_version = Float.parseFloat(old);
            if (app_version != old_version) {
                restartApp();
            } else {
                if (BuildConfig.DEBUG) {
                    // check internet connections
                    if (DeviceUtils.isNetworkAvailable(this)) {
                        new Thread(() -> {
                            try {
                                URL url = new URL("https://www.google.com");
                                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                conn.setConnectTimeout(5000);
                                if (conn.getResponseCode() != 200) { // no internet
                                    if (DeviceUtils.isCellularAvailable(this)) {
                                        final NetworkRequest.Builder req = new NetworkRequest.Builder();
                                        req.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
                                        connection_manager.requestNetwork(req.build(), new ConnectivityManager.NetworkCallback() {
                                            @Override
                                            public void onAvailable(@NonNull @NotNull Network network) {
                                                super.onAvailable(network);
                                                connection_manager.bindProcessToNetwork(network);
                                                onNext();
                                            }

                                            @Override
                                            public void onUnavailable() {
                                                super.onUnavailable();
                                                onNext();
                                            }
                                        });
                                    } else {
                                        onNext();
                                    }
                                } else {
                                    onNext();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                if (DeviceUtils.isCellularAvailable(this)) {
                                    final NetworkRequest.Builder req = new NetworkRequest.Builder();
                                    req.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
                                    connection_manager.requestNetwork(req.build(), new ConnectivityManager.NetworkCallback() {
                                        @Override
                                        public void onAvailable(@NonNull @NotNull Network network) {
                                            super.onAvailable(network);
                                            connection_manager.bindProcessToNetwork(network);
                                            onNext();
                                        }

                                        @Override
                                        public void onUnavailable() {
                                            super.onUnavailable();
                                            onNext();
                                        }

                                        @Override
                                        public void onLost(@NonNull @NotNull Network network) {
                                            super.onLost(network);
                                        }

                                        @Override
                                        public void onLosing(@NonNull @NotNull Network network, int maxMsToLive) {
                                            super.onLosing(network, maxMsToLive);
                                        }
                                    });
                                } else {
                                    onNext();
                                }
                            }
                        }).start();
                    } else {
                        onNext();
                    }
                } else {
                    onNext();
                }
            }
        } else {
            onNext();
        }
    }

    void onNext() {
        runOnUiThread(() -> {
            AppPreference.setStr(AppPreference.KEY.APP_OLD_VERSION, CommonUtil.getVersionCode(SplashActivity.this));
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            overridePendingTransition(0, 0); // Disable transition animation
            finish();
        });
    }

    void restartApp() {
        Log.e(TAG, "restartCamera: 2");
        AlertDialog.Builder aboutDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.restart_required)
                .setMessage(R.string.service_restart_info)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(true)
                .setOnDismissListener(dialog -> quitApp());

        AlertDialog alertDialog = aboutDialog.create();
        alertDialog.show();

// Change button text color to black
        Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(Color.BLACK); // Set positive button text color to black
        }
    }

    void quitApp() {
        AppPreference.setStr(AppPreference.KEY.APP_OLD_VERSION, CommonUtil.getVersionCode(this));
        finish();
    }

    void setSerialNumber() {
        if (is_dialog_show) {
            return;
        }
        is_dialog_show = true;

        SerialDialog serialDialog = new SerialDialog(this);
        serialDialog.setOkListener(view -> {
            is_dialog_show = false;
            serialDialog.dismiss();
            String serial_number = serialDialog.edt_serial.getText().toString();
            if (isValidSerial(serial_number)) {
                displayID();
            } else {
                gotoNextView();
            }
        });
        serialDialog.setCloseListener(view -> {
            serialDialog.dismiss();
            is_dialog_show = false;
            gotoNextView();
        });
        serialDialog.setScanListener(view -> {
            is_dialog_show = false;
            serialDialog.dismiss();
            Intent i = new Intent(SplashActivity.this, QrCodeActivity.class);
            startActivityForResult(i, REQUEST_CODE_QR_SCAN_SERIAL_NUMBER);
        });
        serialDialog.setHelpListener(view -> {
            serialDialog.setContentView(R.layout.dialog_help);
            serialDialog.findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    serialDialog.dismiss();
                    is_dialog_show = false;
                    setSerialNumber();
                }
            });
        });
        serialDialog.show();
    }

    void setActivationCode() {
        if (is_dialog_show) {
            return;
        }
        is_dialog_show = true;

        CodeDialog codeDialog = new CodeDialog(this);
        codeDialog.setCloseListener(view -> {
            codeDialog.dismiss();
            is_dialog_show = false;
            gotoNextView();
        });
        codeDialog.setOkListener(view -> {
            codeDialog.dismiss();
            String activation_code = codeDialog.edt_code.getText().toString();
            isValidActivation(activation_code);
            is_dialog_show = false;
            gotoNextView();
        });
        codeDialog.setScanListener(view -> {
            codeDialog.dismiss();
            Intent i = new Intent(SplashActivity.this, QrCodeActivity.class);
            startActivityForResult(i, REQUEST_CODE_QR_SCAN_ACTIVATION);
            is_dialog_show = false;
        });
        codeDialog.show();
    }

    void displayID() {
        if (is_dialog_show) {
            return;
        }
        is_dialog_show = true;
        String machine_code = CommonUtil.getDeviceID(this);
        MachineCodeDialog machineCodeDialog = new MachineCodeDialog(this);
        machineCodeDialog.setMachineCode(machine_code);
        machineCodeDialog.setOkListener(view -> {
            machineCodeDialog.dismiss();
            is_dialog_show = false;
            setActivationCode();
        });
        machineCodeDialog.setCloseListener(view -> {
            machineCodeDialog.dismiss();
            is_dialog_show = false;
            gotoNextView();
        });
        machineCodeDialog.setOnlineListener(view -> {
            // online activation
            if (!DeviceUtils.isNetworkAvailable(this)) {
                Toast.makeText(this,"Please connect to internet continue.", Toast.LENGTH_SHORT).show();
                return;
            }
            dlg_progress.show();
            String serial_number = AppPreference.getStr(AppPreference.KEY.ACTIVATION_SERIAL, "");
            HttpApiService.getHttpApiEndPoint().activate(serial_number, machine_code).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    machineCodeDialog.dismiss();
                    is_dialog_show = false;
                    dlg_progress.dismiss();
                    if (response.code() == 404) { // invalid serial
                        MessageUtil.showToast(getApplicationContext(), R.string.invalid_serial);
                    } else if (response.code() == 400) { // invalid machine code
                        MessageUtil.showToast(getApplicationContext(), R.string.invalid_machine_code);
                    } else if (response.code() == 405) { // method not allowed
//                        MessageUtil.showToast(SplashActivity.this, R.string.method_not_allowed);
                        AlertDialog.Builder aboutDialog = new AlertDialog.Builder(SplashActivity.this)
                                .setTitle(R.string.warning)
                                .setMessage(R.string.license_exceed_mgs)
                                .setPositiveButton(android.R.string.ok, null)
                                .setCancelable(true)
                                .setOnDismissListener(dialog -> {
                                    gotoNextView();
                                });
                        aboutDialog.show();
                        return;
                    } else if (response.code() == 200) { // success
                        isValidActivation(response.body());
                    } else {
                        MessageUtil.showToast(getApplicationContext(), R.string.unknown_error);
                    }
                    gotoNextView();
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    machineCodeDialog.dismiss();
                    is_dialog_show = false;
                    dlg_progress.dismiss();
                    MessageUtil.showToast(getApplicationContext(), t.getLocalizedMessage());
                    gotoNextView();
                }
            });
        });
        machineCodeDialog.show();
    }



    void setPinCode() {
        if (is_dialog_show) {
            return;
        }
        is_dialog_show = true;
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customTitleView = inflater.inflate(R.layout.dialog_title, null);
        View rootview = findViewById(android.R.id.content);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCustomTitle(customTitleView);
        View viewInflated = LayoutInflater.from(SplashActivity.this).inflate(R.layout.text_inpu_password, (ViewGroup) rootview, false);
        final EditText input = (EditText) viewInflated.findViewById(R.id.edt_pin);
        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                is_dialog_show = false;
                String pin = input.getText().toString();
                if (TextUtils.isEmpty(pin) || pin.length() != 4) {
                    MessageUtil.showToast(getApplicationContext(), R.string.invalid_pin);
                    gotoNextView();
                } else {
                    AppPreference.setStr(AppPreference.KEY.PIN_NUMBER, pin);
                    AppPreference.setBool(AppPreference.KEY.APP_FIRST_LAUNCH, false);
                    AppPreference.setBool(AppPreference.KEY.UI_CONVERT_MODE, true);
                    chooseAudioOption();
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                is_dialog_show = false;
                gotoNextView();
            }
        });

        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Handle storage helper folder selection
        if (storageHelper != null) {
            storageHelper.handleFolderSelectionResult(requestCode, resultCode, data);
        }

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_CODE_QR_SCAN_SERIAL_NUMBER) {
                String result = data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult");
                if (isValidSerial(result)) {
                    displayID();
                } else {
                    gotoNextView();
                }
            } else if (requestCode == REQUEST_CODE_QR_SCAN_ACTIVATION) {
                String result = data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult");
                if (!isValidActivation(result)) {
                }
                gotoNextView();
            }
        }
    }

    boolean isValidSerial(String serial_number) {
        AppPreference.removeKey(AppPreference.KEY.ACTIVATION_SERIAL);
        if (TextUtils.isEmpty(serial_number)) {
            MessageUtil.showToast(getApplicationContext(), R.string.no_serial);
            return false;
        }
        if (!TextUtils.isDigitsOnly(serial_number)) {
            MessageUtil.showToast(getApplicationContext(), R.string.invalid_serial);
            return false;
        }
        int length = serial_number.length();
        int checksum = 0;
        int last_val = 0;
        for (int i = 0; i < length; i++) {
            String val = serial_number.substring(i, i + 1);
            int num = Integer.parseInt(val);
            if (i == length - 1) {
                last_val = num;
            } else {
                checksum += num;
            }
        }
        if (checksum % 10 != last_val) {
            MessageUtil.showToast(getApplicationContext(), R.string.invalid_serial);
            return false;
        }
        AppPreference.setStr(AppPreference.KEY.ACTIVATION_SERIAL, serial_number);
        return true;
    }

    boolean isValidActivation(String activation_code) {
        AppPreference.removeKey(AppPreference.KEY.ACTIVATION_CODE);
        String activation = Crypto.Decrypt(activation_code, Crypto.encrypt_key);
        if (TextUtils.isEmpty(activation)) {
            MessageUtil.showToast(getApplicationContext(), R.string.no_activation);
            return false;
        }
        String[] val = activation.split("-");
        if (val.length < 2) {
            MessageUtil.showToast(getApplicationContext(), R.string.invalid_activation);
            return false;
        } else { // no expire
            String serial = val[0];
            if (!TextUtils.equals(serial, AppPreference.getStr(AppPreference.KEY.ACTIVATION_SERIAL, ""))) {
                MessageUtil.showToast(getApplicationContext(), R.string.invalid_activation);
                return false;
            }
            String machine = val[1];
            if (!TextUtils.equals(machine, CommonUtil.getDeviceID(this))) {
                MessageUtil.showToast(getApplicationContext(), R.string.invalid_activation);
                return false;
            }
            if (val.length == 3) {
                AppConstant.expire_date = val[2];
            } else {
                AppConstant.expire_date = "";
            }
        }
        AppPreference.setStr(AppPreference.KEY.ACTIVATION_CODE, activation_code);
        return true;
    }

    public void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                // Main notification channel
                NotificationChannel mainChannel = new NotificationChannel(
                        "main_channel",
                        "Main Notifications",
                        NotificationManager.IMPORTANCE_HIGH
                );
                mainChannel.setDescription("Main app notifications");
                mainChannel.enableVibration(true);
                mainChannel.setVibrationPattern(new long[]{0, 500, 200, 500});

                // Service notification channel
                NotificationChannel serviceChannel = new NotificationChannel(
                        "service_channel",
                        "Service Notifications",
                        NotificationManager.IMPORTANCE_LOW
                );
                serviceChannel.setDescription("Background service notifications");
                serviceChannel.setShowBadge(false);

                notificationManager.createNotificationChannels(Arrays.asList(mainChannel, serviceChannel));
            }
        }
    }

    private void navigateToNotificationSettings() {
        // Show instructions dialog first
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔔 Enable Notifications");
        builder.setMessage("To enable notifications:\n\n" +
                "1. Tap 'Open Settings' below\n" +
                "2. Find 'Notifications' in the app settings\n" +
                "3. Turn ON 'Allow notifications'\n" +
                "4. Return to CheckMate! when done\n\n" +
                "This ensures you receive important alerts about your recordings.");

        builder.setPositiveButton("Open Settings", (dialog, which) -> {
            dialog.dismiss();
            // Navigate to app settings
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);

            // Show a reminder dialog after a delay
            new Handler().postDelayed(() -> {
                showNotificationReminderDialog();
            }, 3000); // 3 seconds delay
        });

        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();

        // Make buttons more visible
        try {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            if (positiveButton != null) {
                positiveButton.setTextColor(Color.BLACK);
            }
            if (negativeButton != null) {
                negativeButton.setTextColor(Color.BLACK);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not set button colors", e);
        }
    }

    /**
     * Show reminder dialog to check if user enabled notifications
     */
    private void showNotificationReminderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔔 Notification Status");
        builder.setMessage("Did you enable notifications in the settings?\n\n" +
                "If yes, tap 'Continue'. If not, you can enable them later from the app settings.");

        builder.setPositiveButton("Continue", (dialog, which) -> {
            dialog.dismiss();
            // Check if notification permission is now granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Notifications enabled successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "You can enable notifications later from app settings", Toast.LENGTH_LONG).show();
                }
            }
            openMainScreen();
        });

        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();

        // Make button more visible
        try {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setTextColor(Color.BLACK);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not set button colors", e);
        }
    }
}