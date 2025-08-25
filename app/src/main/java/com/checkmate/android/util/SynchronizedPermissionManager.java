package com.checkmate.android.util;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Synchronized Permission Manager for handling all permission requests in a sequential, 
 * non-blocking manner to prevent UI freezing, crashes, and window leaks.
 */
public class SynchronizedPermissionManager {
    private static final String TAG = "SyncPermissionManager";
    
    // Request codes
    private static final int REQUEST_CORE_PERMISSIONS = 1001;
    private static final int REQUEST_STORAGE_PERMISSIONS = 1002;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1003;
    
    // Core permissions required for app functionality
    private static final String[] CORE_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.VIBRATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
    };
    
    // State management
    private final AtomicBoolean isProcessingPermissions = new AtomicBoolean(false);
    private final AtomicBoolean isDialogShowing = new AtomicBoolean(false);
    private final AtomicInteger currentStep = new AtomicInteger(0);
    
    private Activity activity;
    private PermissionCallback callback;
    private DialogManager dialogManager;
    private Handler mainHandler;
    
    public interface PermissionCallback {
        void onAllPermissionsGranted();
        void onPermissionDenied(String permission);
        void onPermissionFlow();
    }
    
    public SynchronizedPermissionManager(Activity activity, PermissionCallback callback) {
        this.activity = activity;
        this.callback = callback;
        this.dialogManager = new DialogManager(activity);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Start the synchronized permission request flow
     */
    public void startPermissionFlow() {
        if (isProcessingPermissions.get()) {
            Log.w(TAG, "Permission flow already in progress, ignoring request");
            return;
        }
        
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.w(TAG, "Activity is not in valid state for permission requests");
            return;
        }
        
        Log.d(TAG, "Starting synchronized permission flow");
        isProcessingPermissions.set(true);
        currentStep.set(0);
        
        // Start with core permissions
        requestNextPermissionGroup();
    }
    
    /**
     * Request the next group of permissions in sequence
     */
    private void requestNextPermissionGroup() {
        if (!isProcessingPermissions.get()) {
            return;
        }
        
        mainHandler.post(() -> {
            try {
                int step = currentStep.get();
                
                switch (step) {
                    case 0:
                        requestCorePermissions();
                        break;
                    case 1:
                        requestStoragePermissions();
                        break;
                    case 2:
                        requestSpecialPermissions();
                        break;
                    case 3:
                        requestNotificationPermission();
                        break;
                    default:
                        completePermissionFlow();
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in permission flow", e);
                handlePermissionError(e);
            }
        });
    }
    
    /**
     * Request core runtime permissions
     */
    private void requestCorePermissions() {
        Log.d(TAG, "Requesting core permissions");
        
        List<String> missingPermissions = new ArrayList<>();
        for (String permission : CORE_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        
        if (missingPermissions.isEmpty()) {
            Log.d(TAG, "All core permissions already granted");
            moveToNextStep();
        } else {
            Log.d(TAG, "Requesting " + missingPermissions.size() + " core permissions");
            ActivityCompat.requestPermissions(
                activity, 
                missingPermissions.toArray(new String[0]), 
                REQUEST_CORE_PERMISSIONS
            );
        }
    }
    
    /**
     * Request storage permissions based on Android version
     */
    private void requestStoragePermissions() {
        Log.d(TAG, "Requesting storage permissions");
        
        if (!StoragePermissionHelper.areStoragePermissionsGranted(activity)) {
            String[] permissions = StoragePermissionHelper.getRequiredStoragePermissions();
            ActivityCompat.requestPermissions(activity, permissions, REQUEST_STORAGE_PERMISSIONS);
        } else {
            Log.d(TAG, "Storage permissions already granted");
            moveToNextStep();
        }
    }
    
    /**
     * Request special system permissions that require settings navigation
     */
    private void requestSpecialPermissions() {
        Log.d(TAG, "Checking special permissions");
        
        // Check if any special permissions are needed
        boolean needsOverlay = !Settings.canDrawOverlays(activity);
        boolean needsBattery = !isIgnoringBatteryOptimizations();
        boolean needsSystemSettings = !Settings.System.canWrite(activity);
        boolean needsDoNotDisturb = !isNotificationPolicyAccessGranted();
        
        if (needsOverlay || needsBattery || needsSystemSettings || needsDoNotDisturb) {
            showSpecialPermissionsDialog(needsOverlay, needsBattery, needsSystemSettings, needsDoNotDisturb);
        } else {
            Log.d(TAG, "All special permissions already granted");
            moveToNextStep();
        }
    }
    
    /**
     * Show dialog for special permissions with clear instructions
     */
    private void showSpecialPermissionsDialog(boolean needsOverlay, boolean needsBattery, 
                                            boolean needsSystemSettings, boolean needsDoNotDisturb) {
        if (isDialogShowing.get() || !dialogManager.isActivityValid()) {
            return;
        }
        
        StringBuilder message = new StringBuilder("The app needs additional permissions:\n\n");
        
        if (needsOverlay) {
            message.append("• Display over other apps\n");
        }
        if (needsBattery) {
            message.append("• Battery optimization settings\n");
        }
        if (needsSystemSettings) {
            message.append("• Modify system settings\n");
        }
        if (needsDoNotDisturb) {
            message.append("• Do not disturb access\n");
        }
        
        message.append("\nThese will open system settings. Please grant the permissions and return to the app.");
        
        isDialogShowing.set(true);
        AlertDialog dialog = dialogManager.createDialogBuilder()
                .setTitle("Additional Permissions Needed")
                .setMessage(message.toString())
                .setPositiveButton("Grant Permissions", (d, which) -> {
                    isDialogShowing.set(false);
                    grantSpecialPermissions(needsOverlay, needsBattery, needsSystemSettings, needsDoNotDisturb);
                })
                .setNegativeButton("Skip", (d, which) -> {
                    isDialogShowing.set(false);
                    moveToNextStep();
                })
                .setCancelable(false)
                .setOnDismissListener(d -> isDialogShowing.set(false))
                .create();
        
        dialogManager.showDialog("special_permissions", dialog);
    }
    
    /**
     * Grant special permissions by opening appropriate settings
     */
    private void grantSpecialPermissions(boolean needsOverlay, boolean needsBattery, 
                                       boolean needsSystemSettings, boolean needsDoNotDisturb) {
        // Start with overlay permission if needed
        if (needsOverlay) {
            requestOverlayPermission();
        } else if (needsBattery) {
            requestBatteryOptimizationPermission();
        } else if (needsSystemSettings) {
            requestSystemSettingsPermission();
        } else if (needsDoNotDisturb) {
            requestDoNotDisturbPermission();
        } else {
            moveToNextStep();
        }
    }
    
    /**
     * Request overlay permission
     */
    private void requestOverlayPermission() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            
            // Show guidance
            showToast("Please enable 'Display over other apps' and return to CheckMate");
            
            // Continue with next special permission after delay
            mainHandler.postDelayed(() -> {
                if (isIgnoringBatteryOptimizations()) {
                    requestBatteryOptimizationPermission();
                } else if (!Settings.System.canWrite(activity)) {
                    requestSystemSettingsPermission();
                } else if (!isNotificationPolicyAccessGranted()) {
                    requestDoNotDisturbPermission();
                } else {
                    moveToNextStep();
                }
            }, 2000);
            
        } catch (Exception e) {
            Log.e(TAG, "Error requesting overlay permission", e);
            moveToNextStep();
        }
    }
    
    /**
     * Request battery optimization permission
     */
    private void requestBatteryOptimizationPermission() {
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            
            showToast("Please disable battery optimization and return to CheckMate");
            
            // Continue with next permission after delay
            mainHandler.postDelayed(() -> {
                if (!Settings.System.canWrite(activity)) {
                    requestSystemSettingsPermission();
                } else if (!isNotificationPolicyAccessGranted()) {
                    requestDoNotDisturbPermission();
                } else {
                    moveToNextStep();
                }
            }, 2000);
            
        } catch (Exception e) {
            Log.e(TAG, "Error requesting battery optimization permission", e);
            if (!Settings.System.canWrite(activity)) {
                requestSystemSettingsPermission();
            } else {
                moveToNextStep();
            }
        }
    }
    
    /**
     * Request system settings permission
     */
    private void requestSystemSettingsPermission() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            
            showToast("Please enable 'Modify system settings' and return to CheckMate");
            
            // Continue with next permission after delay
            mainHandler.postDelayed(() -> {
                if (!isNotificationPolicyAccessGranted()) {
                    requestDoNotDisturbPermission();
                } else {
                    moveToNextStep();
                }
            }, 2000);
            
        } catch (Exception e) {
            Log.e(TAG, "Error requesting system settings permission", e);
            if (!isNotificationPolicyAccessGranted()) {
                requestDoNotDisturbPermission();
            } else {
                moveToNextStep();
            }
        }
    }
    
    /**
     * Request do not disturb permission
     */
    private void requestDoNotDisturbPermission() {
        try {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            
            showToast("Please enable 'Do not disturb access' and return to CheckMate");
            
            // Move to next step after delay
            mainHandler.postDelayed(this::moveToNextStep, 2000);
            
        } catch (Exception e) {
            Log.e(TAG, "Error requesting do not disturb permission", e);
            moveToNextStep();
        }
    }
    
    /**
     * Request notification permission (Android 13+)
     */
    private void requestNotificationPermission() {
        Log.d(TAG, "Checking notification permission");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                
                showNotificationPermissionDialog();
            } else {
                Log.d(TAG, "Notification permission already granted");
                completePermissionFlow();
            }
        } else {
            Log.d(TAG, "Notification permission not required for this Android version");
            completePermissionFlow();
        }
    }
    
    /**
     * Show notification permission dialog
     */
    private void showNotificationPermissionDialog() {
        if (isDialogShowing.get() || !dialogManager.isActivityValid()) {
            return;
        }
        
        isDialogShowing.set(true);
        AlertDialog dialog = dialogManager.createDialogBuilder()
                .setTitle("🔔 Enable Notifications")
                .setMessage("Enable notifications to receive important alerts about your recordings and app status.")
                .setPositiveButton("Enable", (d, which) -> {
                    isDialogShowing.set(false);
                    ActivityCompat.requestPermissions(activity, 
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                        REQUEST_NOTIFICATION_PERMISSION);
                })
                .setNegativeButton("Skip", (d, which) -> {
                    isDialogShowing.set(false);
                    completePermissionFlow();
                })
                .setCancelable(false)
                .setOnDismissListener(d -> isDialogShowing.set(false))
                .create();
        
        dialogManager.showDialog("notification_permission", dialog);
    }
    
    /**
     * Handle permission request results
     */
    public void handlePermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "Handling permission result for request code: " + requestCode);
        
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        
        switch (requestCode) {
            case REQUEST_CORE_PERMISSIONS:
                Log.d(TAG, "Core permissions result: " + allGranted);
                if (!allGranted && callback != null) {
                    callback.onPermissionDenied("Core permissions");
                }
                moveToNextStep();
                break;
                
            case REQUEST_STORAGE_PERMISSIONS:
                Log.d(TAG, "Storage permissions result: " + allGranted);
                if (!allGranted && callback != null) {
                    callback.onPermissionDenied("Storage permissions");
                }
                moveToNextStep();
                break;
                
            case REQUEST_NOTIFICATION_PERMISSION:
                Log.d(TAG, "Notification permission result: " + allGranted);
                if (allGranted) {
                    showToast("Notifications enabled successfully!");
                } else {
                    showToast("You can enable notifications later from app settings");
                }
                completePermissionFlow();
                break;
        }
    }
    
    /**
     * Move to the next step in the permission flow
     */
    private void moveToNextStep() {
        currentStep.incrementAndGet();
        
        // Add small delay to prevent rapid permission dialogs
        mainHandler.postDelayed(this::requestNextPermissionGroup, 500);
    }
    
    /**
     * Complete the permission flow
     */
    private void completePermissionFlow() {
        Log.d(TAG, "Completing permission flow");
        
        if (dialogManager != null) {
            dialogManager.dismissAllDialogs();
        }
        isProcessingPermissions.set(false);
        
        if (callback != null) {
            callback.onAllPermissionsGranted();
        }
    }
    
    /**
     * Handle permission flow errors
     */
    private void handlePermissionError(Exception e) {
        Log.e(TAG, "Permission flow error", e);
        
        if (dialogManager != null) {
            dialogManager.dismissAllDialogs();
        }
        isProcessingPermissions.set(false);
        
        showToast("Permission setup encountered an error. Some features may not work properly.");
        
        if (callback != null) {
            callback.onAllPermissionsGranted(); // Continue anyway
        }
    }
    
    /**
     * Check if app is ignoring battery optimizations
     */
    private boolean isIgnoringBatteryOptimizations() {
        PowerManager powerManager = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        return powerManager != null && powerManager.isIgnoringBatteryOptimizations(activity.getPackageName());
    }
    
    /**
     * Check if notification policy access is granted
     */
    private boolean isNotificationPolicyAccessGranted() {
        NotificationManager notificationManager = 
                (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        return notificationManager != null && notificationManager.isNotificationPolicyAccessGranted();
    }
    
    // DEPRECATED: Now handled by DialogManager
    @Deprecated
    private void dismissCurrentDialog() {
        if (dialogManager != null) {
            dialogManager.dismissAllDialogs();
        }
        isDialogShowing.set(false);
    }
    
    /**
     * Show toast message safely
     */
    private void showToast(String message) {
        mainHandler.post(() -> {
            try {
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.w(TAG, "Error showing toast", e);
            }
        });
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up permission manager");
        
        if (dialogManager != null) {
            dialogManager.cleanup();
            dialogManager = null;
        }
        
        isProcessingPermissions.set(false);
        
        activity = null;
        callback = null;
        
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
            mainHandler = null;
        }
    }
    
    /**
     * Check if permission flow is currently active
     */
    public boolean isProcessing() {
        return isProcessingPermissions.get();
    }
}