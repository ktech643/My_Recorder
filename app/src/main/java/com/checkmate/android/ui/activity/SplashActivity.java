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

    // Core permissions - requesting only what's absolutely essential to prevent dialog overload
    private static final String[] CORE_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };
    
    // Storage permissions - handled separately to prevent conflicts
    private static final String[] STORAGE_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    
    // Legacy - keeping for backward compatibility
    private static final String[] PERMISSIONS = CORE_PERMISSIONS;
    
    // Permission state tracking to prevent multiple simultaneous requests
    private boolean isRequestingPermissions = false;
    private boolean hasRequestedCorePermissions = false;
    private boolean hasRequestedStoragePermissions = false;
    private boolean hasRequestedSystemPermissions = false;
    
    // Dialog state tracking to prevent overlapping popups
    private boolean isDialogShowing = false;
    private boolean isNavigatingToNextView = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Create notification channels for Android 14 compatibility
        createNotificationChannels();

        // Remove notification permission request from onCreate - will be requested after activation

        // Optimized window flags to prevent screen jerking and maintain stability
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        
        // Prevent focus loss during permission requests
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LOCAL_FOCUS_MODE);

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
        
        // Prevent screen jerking when focus changes during permission dialogs
        if (hasFocus && !isRequestingPermissions) {
            // Only apply window flags when not in permission flow to prevent conflicts
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Keep the screen on even when paused to prevent blinking
        if (!isRequestingPermissions) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Minimize window flag changes to prevent jerking
        if (!isRequestingPermissions) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        if (AppPreference.getBool(AppPreference.KEY.APP_FORCE_QUIT, true)) {
            AppPreference.setBool(AppPreference.KEY.APP_FORCE_QUIT, true);
            // Add delay to prevent immediate permission requests that can cause freezing
            new Handler().postDelayed(() -> {
                if (!isRequestingPermissions && !isDialogShowing) {
                    verifyPermissions(this);
                }
            }, 1000); // Increased delay to 1 second
        } else {
            MessageUtil.showToast(getApplicationContext(), "CheckMate! Will be closing. Please use main icon to open.");
            new Handler().postDelayed(() -> {
                Intent startMain = new Intent(Intent.ACTION_MAIN);
                startMain.addCategory(Intent.CATEGORY_HOME);
                startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startMain);
                AppPreference.setBool(AppPreference.KEY.APP_FORCE_QUIT, true);
                finish();
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
        if (isRequestingPermissions) {
            Log.w(TAG, "Permission request already in progress, skipping to prevent conflicts");
            return;
        }
        
        Log.d(TAG, "Starting controlled permission verification");
        isRequestingPermissions = true;
        
        // Start with core permissions only to prevent dialog overload
        requestCorePermissions(activity);
    }
    
    /**
     * Request only core permissions first (Camera, Audio) - minimal set to prevent freezing
     */
    private void requestCorePermissions(Activity activity) {
        if (hasRequestedCorePermissions) {
            Log.d(TAG, "Core permissions already requested, proceeding to storage");
            requestStoragePermissions(activity);
            return;
        }
        
        List<String> missingCore = new ArrayList<>();
        
        for (String permission : CORE_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                missingCore.add(permission);
            }
        }
        
        if (!missingCore.isEmpty()) {
            Log.d(TAG, "Requesting core permissions: " + missingCore.size() + " permissions");
            hasRequestedCorePermissions = true;
            ActivityCompat.requestPermissions(
                    activity,
                    missingCore.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE_FOR_PERMISSION
            );
        } else {
            Log.d(TAG, "Core permissions granted, proceeding to storage permissions");
            hasRequestedCorePermissions = true;
            requestStoragePermissions(activity);
        }
    }
    
    /**
     * Request storage permissions separately to prevent conflicts
     */
    private void requestStoragePermissions(Activity activity) {
        if (hasRequestedStoragePermissions) {
            Log.d(TAG, "Storage permissions already requested, proceeding to system permissions");
            requestSystemPermissions(activity);
            return;
        }
        
        List<String> missingStorage = new ArrayList<>();
        
        for (String permission : STORAGE_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                missingStorage.add(permission);
            }
        }
        
        if (!missingStorage.isEmpty()) {
            Log.d(TAG, "Requesting storage permissions: " + missingStorage.size() + " permissions");
            hasRequestedStoragePermissions = true;
            // Delay to prevent dialog conflicts
            new Handler().postDelayed(() -> {
                ActivityCompat.requestPermissions(
                        activity,
                        missingStorage.toArray(new String[0]),
                        PERMISSION_REQUEST_CODE_FOR_PERMISSION + 1
                );
            }, 2000); // 2 second delay to ensure previous dialog closed
        } else {
            Log.d(TAG, "Storage permissions granted, proceeding to system permissions");
            hasRequestedStoragePermissions = true;
            requestSystemPermissions(activity);
        }
    }

    public void verifyStoragePermissions(Activity activity) {
        // This method is kept for compatibility but should not trigger new permission requests
        // to prevent conflicts with our controlled permission flow
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
            Log.d(TAG, "Storage permissions missing but not requesting to avoid conflicts");
        } else {
            Log.d("Storage", "All permissions granted!");
        }
    }


    private static final int REQUEST_STORAGE_PERMISSION = 1001;

    private void checkStoragePermissions() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Storage permission missing but not requesting to avoid conflicts");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Handle storage permissions using StoragePermissionHelper (but don't let it trigger new requests)
        try {
            StoragePermissionHelper.handlePermissionResult(this, requestCode, permissions, grantResults);
        } catch (Exception e) {
            Log.w(TAG, "StoragePermissionHelper error: " + e.getMessage());
        }

        // Handle storage helper permissions
        if (storageHelper != null) {
            try {
                storageHelper.handlePermissionResult(requestCode, permissions, grantResults);
            } catch (Exception e) {
                Log.w(TAG, "StorageHelper error: " + e.getMessage());
            }
        }

        if (requestCode == PERMISSION_REQUEST_CODE_FOR_PERMISSION) {
            // Core permissions handled, now request storage permissions
            Log.d(TAG, "Core permissions result received, proceeding to storage permissions");
            new Handler().postDelayed(() -> {
                requestStoragePermissions(this);
            }, 1500); // Delay to prevent dialog conflicts
        } else if (requestCode == PERMISSION_REQUEST_CODE_FOR_PERMISSION + 1) {
            // Storage permissions handled, now request system permissions
            Log.d(TAG, "Storage permissions result received, proceeding to system permissions");
            new Handler().postDelayed(() -> {
                requestSystemPermissions(this);
            }, 1500); // Delay to prevent dialog conflicts
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

    /**
     * Request system permissions one at a time to prevent overlapping intents
     */
    private void requestSystemPermissions(Activity activity) {
        if (hasRequestedSystemPermissions) {
            Log.d(TAG, "System permissions already requested, continuing to main view");
            isRequestingPermissions = false;
            safeGotoNextView();
            return;
        }
        
        hasRequestedSystemPermissions = true;
        Log.d(TAG, "Starting controlled system permission flow");
        
        // Request system permissions one at a time with longer delays to prevent freezing
        requestOverlayPermissionSequential(activity, () -> {
            requestBatteryOptimizationSequential(activity, () -> {
                requestSystemSettingsSequential(activity, () -> {
                    Log.d(TAG, "All system permissions processed, continuing to main");
                    isRequestingPermissions = false;
                    safeGotoNextView();
                });
            });
        });
    }
    
    private void requestOverlayPermissionSequential(Activity activity, Runnable onComplete) {
        try {
            if (!Settings.canDrawOverlays(activity)) {
                Log.d(TAG, "Requesting overlay permission");
                requestDrawOverAppsPermission(activity);
                // Longer delay to ensure system settings intent completes
                new Handler().postDelayed(onComplete, 6000); // Increased to 6 seconds
            } else {
                Log.d(TAG, "Overlay permission already granted");
                // Still use delay to prevent rapid transitions
                new Handler().postDelayed(onComplete, 1000);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error requesting overlay permission", e);
            new Handler().postDelayed(onComplete, 1000);
        }
    }
    
    private void requestBatteryOptimizationSequential(Activity activity, Runnable onComplete) {
        try {
            PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(activity.getPackageName())) {
                Log.d(TAG, "Requesting battery optimization");
                requestIgnoreBatteryOptimizationsPermission(activity);
                // Longer delay to ensure system settings intent completes
                new Handler().postDelayed(onComplete, 6000); // Increased to 6 seconds
            } else {
                Log.d(TAG, "Battery optimization already granted");
                // Still use delay to prevent rapid transitions
                new Handler().postDelayed(onComplete, 1000);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error requesting battery optimization", e);
            new Handler().postDelayed(onComplete, 1000);
        }
    }
    
    private void requestSystemSettingsSequential(Activity activity, Runnable onComplete) {
        try {
            if (!Settings.System.canWrite(activity)) {
                Log.d(TAG, "Requesting system settings access");
                requestModifySystemSettingsPermission(activity);
                // Longer delay to ensure system settings intent completes
                new Handler().postDelayed(onComplete, 6000); // Increased to 6 seconds
            } else {
                Log.d(TAG, "System settings permission already granted");
                // Still use delay to prevent rapid transitions
                new Handler().postDelayed(onComplete, 1000);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error requesting system settings", e);
            new Handler().postDelayed(onComplete, 1000);
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
    
    /**
     * Safe navigation to next view with recursive call protection
     */
    void safeGotoNextView() {
        if (isNavigatingToNextView) {
            Log.w(TAG, "Already navigating to next view, skipping to prevent recursive calls");
            return;
        }
        isNavigatingToNextView = true;
        
        // Add delay to ensure previous dialogs are dismissed
        new Handler().postDelayed(() -> {
            gotoNextView();
        }, 500);
    }
    
    void gotoNextView() {
        if (is_dialog_show || isDialogShowing) {
            Log.w(TAG, "Dialog already showing, skipping navigation");
            isNavigatingToNextView = false;
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
                    isNavigatingToNextView = false;
                    continueWithActivation();
                }
                @Override
                public void onStorageSelectionCancelled() {
                    Log.w(TAG, "Storage selection cancelled");
                    // Use default storage and continue
                    PreferenceInitializer.validateStorageLocation(SplashActivity.this);
                    isNavigatingToNextView = false;
                    continueWithActivation();
                }
            });
            // Show storage selection dialog and wait for user selection
            isDialogShowing = true;
            storageHelper.showCustomStorageDialog();
            isNavigatingToNextView = false;
            return;
        } else { // check
            if (!isValidActivation(activation)) {
                // FIXED: This was causing recursive loops - don't call gotoNextView again
                Log.w(TAG, "Invalid activation, returning to avoid recursion");
                isNavigatingToNextView = false;
                return;
            }
        }
        // After activation and storage selection, ask for notification permission before main
        if (should_show_complete) {
            should_show_complete = false;
            is_dialog_show = true;
            isDialogShowing = true;
            ActivationCompleteDialog com_dialog = new ActivationCompleteDialog(this);
            com_dialog.setOkListener(view -> {
                is_dialog_show = false;
                isDialogShowing = false;
                com_dialog.dismiss();
                isNavigatingToNextView = false;
                maybeAskNotificationThenMain();
            });
            com_dialog.show();
        } else {
            isNavigatingToNextView = false;
            maybeAskNotificationThenMain();
        }
    }

    /**
     * Continue with activation after storage is configured
     */
    private void continueWithActivation() {
        // Validate and fix storage location if needed
        PreferenceInitializer.validateStorageLocation(this);
        // Check storage permissions but don't trigger new requests to avoid conflicts
        if (!StoragePermissionHelper.areStoragePermissionsGranted(this)) {
            Log.d(TAG, "Storage permissions not granted, but skipping request to avoid conflicts");
        }
        should_show_complete = true;
        is_dialog_show = true;
        isDialogShowing = true;
        ActivationDialog activationDialog = new ActivationDialog(this);
        activationDialog.setOkListener(view -> {
            activationDialog.dismiss();
            is_dialog_show = false;
            isDialogShowing = false;
            setSerialNumber();
        });
        activationDialog.setCloseListener(view -> {
            is_dialog_show = false;
            isDialogShowing = false;
            activationDialog.dismiss();
            finish();
        });
        activationDialog.show();
    }


    void openMainScreen() {

        if (AppPreference.getBool(AppPreference.KEY.APP_FIRST_LAUNCH, true)) {
            isDialogShowing = true;
            AlertDialog.Builder  builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.convert_mode)
                    .setMessage(getString(R.string.confirm_convert))
                    .setIcon(R.mipmap.ic_launcher)
                    .setCancelable(false)
                    .setPositiveButton(R.string.configure_covert, (dialog, whichButton) -> {
                        isDialogShowing = false;
                        setPinCode();
                    })
                    .setNegativeButton(R.string.without_configure_covert, (dialog, whichButton) -> {
                        isDialogShowing = false;
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
        isDialogShowing = true;
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
                    isDialogShowing = false;
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
            overridePendingTransition(0, 0); // Disable transition animation to prevent jerking
            finish();
        });
    }

    void restartApp() {
        Log.e(TAG, "restartCamera: 2");
        isDialogShowing = true;
        AlertDialog.Builder aboutDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.restart_required)
                .setMessage(R.string.service_restart_info)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(true)
                .setOnDismissListener(dialog -> {
                    isDialogShowing = false;
                    quitApp();
                });

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
        if (is_dialog_show || isDialogShowing) {
            return;
        }
        is_dialog_show = true;
        isDialogShowing = true;

        SerialDialog serialDialog = new SerialDialog(this);
        serialDialog.setOkListener(view -> {
            is_dialog_show = false;
            isDialogShowing = false;
            serialDialog.dismiss();
            String serial_number = serialDialog.edt_serial.getText().toString();
            if (isValidSerial(serial_number)) {
                displayID();
            } else {
                safeGotoNextView();
            }
        });
        serialDialog.setCloseListener(view -> {
            serialDialog.dismiss();
            is_dialog_show = false;
            isDialogShowing = false;
            safeGotoNextView();
        });
        serialDialog.setScanListener(view -> {
            is_dialog_show = false;
            isDialogShowing = false;
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
                    isDialogShowing = false;
                    setSerialNumber();
                }
            });
        });
        serialDialog.show();
    }

    void setActivationCode() {
        if (is_dialog_show || isDialogShowing) {
            return;
        }
        is_dialog_show = true;
        isDialogShowing = true;

        CodeDialog codeDialog = new CodeDialog(this);
        codeDialog.setCloseListener(view -> {
            codeDialog.dismiss();
            is_dialog_show = false;
            isDialogShowing = false;
            safeGotoNextView();
        });
        codeDialog.setOkListener(view -> {
            codeDialog.dismiss();
            String activation_code = codeDialog.edt_code.getText().toString();
            isValidActivation(activation_code);
            is_dialog_show = false;
            isDialogShowing = false;
            safeGotoNextView();
        });
        codeDialog.setScanListener(view -> {
            codeDialog.dismiss();
            Intent i = new Intent(SplashActivity.this, QrCodeActivity.class);
            startActivityForResult(i, REQUEST_CODE_QR_SCAN_ACTIVATION);
            is_dialog_show = false;
            isDialogShowing = false;
        });
        codeDialog.show();
    }

    void displayID() {
        if (is_dialog_show || isDialogShowing) {
            return;
        }
        is_dialog_show = true;
        isDialogShowing = true;
        String machine_code = CommonUtil.getDeviceID(this);
        MachineCodeDialog machineCodeDialog = new MachineCodeDialog(this);
        machineCodeDialog.setMachineCode(machine_code);
        machineCodeDialog.setOkListener(view -> {
            machineCodeDialog.dismiss();
            is_dialog_show = false;
            isDialogShowing = false;
            setActivationCode();
        });
        machineCodeDialog.setCloseListener(view -> {
            machineCodeDialog.dismiss();
            is_dialog_show = false;
            isDialogShowing = false;
            safeGotoNextView();
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
                    isDialogShowing = false;
                    dlg_progress.dismiss();
                    if (response.code() == 404) { // invalid serial
                        MessageUtil.showToast(getApplicationContext(), R.string.invalid_serial);
                    } else if (response.code() == 400) { // invalid machine code
                        MessageUtil.showToast(getApplicationContext(), R.string.invalid_machine_code);
                    } else if (response.code() == 405) { // method not allowed
//                        MessageUtil.showToast(SplashActivity.this, R.string.method_not_allowed);
                        isDialogShowing = true;
                        AlertDialog.Builder aboutDialog = new AlertDialog.Builder(SplashActivity.this)
                                .setTitle(R.string.warning)
                                .setMessage(R.string.license_exceed_mgs)
                                .setPositiveButton(android.R.string.ok, null)
                                .setCancelable(true)
                                .setOnDismissListener(dialog -> {
                                    isDialogShowing = false;
                                    safeGotoNextView();
                                });
                        aboutDialog.show();
                        return;
                    } else if (response.code() == 200) { // success
                        isValidActivation(response.body());
                    } else {
                        MessageUtil.showToast(getApplicationContext(), R.string.unknown_error);
                    }
                    safeGotoNextView();
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    machineCodeDialog.dismiss();
                    is_dialog_show = false;
                    isDialogShowing = false;
                    dlg_progress.dismiss();
                    MessageUtil.showToast(getApplicationContext(), t.getLocalizedMessage());
                    safeGotoNextView();
                }
            });
        });
        machineCodeDialog.show();
    }



    void setPinCode() {
        if (is_dialog_show || isDialogShowing) {
            return;
        }
        is_dialog_show = true;
        isDialogShowing = true;
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
                isDialogShowing = false;
                String pin = input.getText().toString();
                if (TextUtils.isEmpty(pin) || pin.length() != 4) {
                    MessageUtil.showToast(getApplicationContext(), R.string.invalid_pin);
                    safeGotoNextView();
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
                isDialogShowing = false;
                safeGotoNextView();
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
                    safeGotoNextView();
                }
            } else if (requestCode == REQUEST_CODE_QR_SCAN_ACTIVATION) {
                String result = data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult");
                if (!isValidActivation(result)) {
                }
                safeGotoNextView();
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
        if (isDialogShowing) {
            Log.w(TAG, "Dialog already showing, skipping notification settings");
            return;
        }
        // Show instructions dialog first
        isDialogShowing = true;
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
            isDialogShowing = false;
            // Navigate to app settings
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);

            // Show a reminder dialog after a delay
            new Handler().postDelayed(() -> {
                showNotificationReminderDialog();
            }, 4000); // 4 seconds delay to allow settings to load
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
        if (isDialogShowing) {
            Log.w(TAG, "Dialog already showing, skipping notification reminder");
            return;
        }
        isDialogShowing = true;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔔 Notification Status");
        builder.setMessage("Did you enable notifications in the settings?\n\n" +
                "If yes, tap 'Continue'. If not, you can enable them later from the app settings.");

        builder.setPositiveButton("Continue", (dialog, which) -> {
            dialog.dismiss();
            isDialogShowing = false;
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