package com.checkmate.android.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * PermissionManager handles all permission requests in a synchronized manner
 * to prevent UI freezing, crashes, and window leaks.
 */
public class PermissionManager {
    private static final String TAG = "PermissionManager";
    
    // Request codes for different permission groups
    public static final int REQUEST_CODE_ESSENTIAL_PERMISSIONS = 1001;
    public static final int REQUEST_CODE_STORAGE_PERMISSIONS = 1002;
    public static final int REQUEST_CODE_NOTIFICATION_PERMISSION = 1003;
    public static final int REQUEST_CODE_SPECIAL_PERMISSIONS = 1004;
    
    private final Activity activity;
    private final PermissionCallback callback;
    private final Handler mainHandler;
    private Queue<PermissionRequest> permissionQueue;
    private boolean isProcessing = false;
    private PermissionRequest currentRequest;
    
    // Essential permissions needed for basic app functionality
    private static final String[] ESSENTIAL_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.VIBRATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
    };
    
    // Network and location permissions
    private static final String[] NETWORK_PERMISSIONS = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CHANGE_NETWORK_STATE
    };
    
    public interface PermissionCallback {
        void onAllPermissionsGranted();
        void onPermissionsDenied(List<String> deniedPermissions);
        void onPermissionRequestCancelled();
    }
    
    private static class PermissionRequest {
        String[] permissions;
        int requestCode;
        String description;
        boolean isSpecialPermission;
        
        PermissionRequest(String[] permissions, int requestCode, String description, boolean isSpecialPermission) {
            this.permissions = permissions;
            this.requestCode = requestCode;
            this.description = description;
            this.isSpecialPermission = isSpecialPermission;
        }
    }
    
    public PermissionManager(Activity activity, PermissionCallback callback) {
        this.activity = activity;
        this.callback = callback;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.permissionQueue = new LinkedList<>();
    }
    
    /**
     * Start the permission request flow
     */
    public void requestAllPermissions() {
        // Clear any existing queue
        permissionQueue.clear();
        
        // Add permissions to queue in order of importance
        // 1. Essential permissions first
        addEssentialPermissions();
        
        // 2. Storage permissions
        addStoragePermissions();
        
        // 3. Network permissions
        addNetworkPermissions();
        
        // 4. Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addNotificationPermission();
        }
        
        // 5. Special permissions last
        addSpecialPermissions();
        
        // Start processing the queue
        processNextPermission();
    }
    
    private void addEssentialPermissions() {
        List<String> missingPermissions = getMissingPermissions(ESSENTIAL_PERMISSIONS);
        if (!missingPermissions.isEmpty()) {
            permissionQueue.offer(new PermissionRequest(
                missingPermissions.toArray(new String[0]),
                REQUEST_CODE_ESSENTIAL_PERMISSIONS,
                "Essential permissions for camera and audio",
                false
            ));
        }
    }
    
    private void addStoragePermissions() {
        List<String> storagePermissions = new ArrayList<>();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses granular media permissions
            storagePermissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            storagePermissions.add(Manifest.permission.READ_MEDIA_VIDEO);
            storagePermissions.add(Manifest.permission.READ_MEDIA_AUDIO);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ - check if MANAGE_EXTERNAL_STORAGE is needed
            // For now, we'll use READ_EXTERNAL_STORAGE
            storagePermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            // Android 10 and below
            storagePermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            storagePermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        
        List<String> missingPermissions = getMissingPermissions(storagePermissions.toArray(new String[0]));
        if (!missingPermissions.isEmpty()) {
            permissionQueue.offer(new PermissionRequest(
                missingPermissions.toArray(new String[0]),
                REQUEST_CODE_STORAGE_PERMISSIONS,
                "Storage permissions for saving recordings",
                false
            ));
        }
    }
    
    private void addNetworkPermissions() {
        List<String> missingPermissions = getMissingPermissions(NETWORK_PERMISSIONS);
        if (!missingPermissions.isEmpty()) {
            permissionQueue.offer(new PermissionRequest(
                missingPermissions.toArray(new String[0]),
                REQUEST_CODE_ESSENTIAL_PERMISSIONS,
                "Network and location permissions",
                false
            ));
        }
    }
    
    private void addNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionQueue.offer(new PermissionRequest(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_CODE_NOTIFICATION_PERMISSION,
                    "Notification permission for alerts",
                    false
                ));
            }
        }
    }
    
    private void addSpecialPermissions() {
        // Add special permissions that need to be requested via settings
        // These will be handled differently
        
        // Draw over other apps
        if (!Settings.canDrawOverlays(activity)) {
            permissionQueue.offer(new PermissionRequest(
                new String[]{"SYSTEM_ALERT_WINDOW"},
                REQUEST_CODE_SPECIAL_PERMISSIONS,
                "Display over other apps",
                true
            ));
        }
        
        // Ignore battery optimizations
        if (!isIgnoringBatteryOptimizations()) {
            permissionQueue.offer(new PermissionRequest(
                new String[]{"REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"},
                REQUEST_CODE_SPECIAL_PERMISSIONS,
                "Ignore battery optimizations",
                true
            ));
        }
        
        // Do not disturb access
        if (!hasNotificationPolicyAccess()) {
            permissionQueue.offer(new PermissionRequest(
                new String[]{"ACCESS_NOTIFICATION_POLICY"},
                REQUEST_CODE_SPECIAL_PERMISSIONS,
                "Do Not Disturb access",
                true
            ));
        }
        
        // Modify system settings
        if (!Settings.System.canWrite(activity)) {
            permissionQueue.offer(new PermissionRequest(
                new String[]{"WRITE_SETTINGS"},
                REQUEST_CODE_SPECIAL_PERMISSIONS,
                "Modify system settings",
                true
            ));
        }
    }
    
    private List<String> getMissingPermissions(String[] permissions) {
        List<String> missingPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        return missingPermissions;
    }
    
    /**
     * Process the next permission in the queue
     */
    private void processNextPermission() {
        if (isProcessing || permissionQueue.isEmpty()) {
            if (permissionQueue.isEmpty() && !isProcessing) {
                // All permissions processed
                mainHandler.post(() -> callback.onAllPermissionsGranted());
            }
            return;
        }
        
        isProcessing = true;
        currentRequest = permissionQueue.poll();
        
        if (currentRequest == null) {
            isProcessing = false;
            processNextPermission();
            return;
        }
        
        // Add a small delay to ensure UI stability
        mainHandler.postDelayed(() -> {
            if (currentRequest.isSpecialPermission) {
                requestSpecialPermission(currentRequest);
            } else {
                requestNormalPermission(currentRequest);
            }
        }, 300); // 300ms delay between permission requests
    }
    
    private void requestNormalPermission(PermissionRequest request) {
        Log.d(TAG, "Requesting permissions: " + Arrays.toString(request.permissions));
        ActivityCompat.requestPermissions(
            activity,
            request.permissions,
            request.requestCode
        );
    }
    
    private void requestSpecialPermission(PermissionRequest request) {
        String permission = request.permissions[0];
        Intent intent = null;
        
        switch (permission) {
            case "SYSTEM_ALERT_WINDOW":
                intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                break;
                
            case "REQUEST_IGNORE_BATTERY_OPTIMIZATIONS":
                intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                break;
                
            case "ACCESS_NOTIFICATION_POLICY":
                intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                break;
                
            case "WRITE_SETTINGS":
                intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                break;
        }
        
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            
            // For special permissions, we can't get a direct callback
            // So we continue after a delay
            mainHandler.postDelayed(() -> {
                isProcessing = false;
                processNextPermission();
            }, 1000);
        } else {
            isProcessing = false;
            processNextPermission();
        }
    }
    
    /**
     * Handle permission result from activity
     */
    public void handlePermissionResult(int requestCode, @NonNull String[] permissions, 
                                     @NonNull int[] grantResults) {
        if (currentRequest == null || currentRequest.requestCode != requestCode) {
            return;
        }
        
        List<String> deniedPermissions = new ArrayList<>();
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permissions[i]);
            }
        }
        
        if (!deniedPermissions.isEmpty()) {
            Log.w(TAG, "Permissions denied: " + deniedPermissions);
            // Continue with next permission even if some are denied
        }
        
        // Process next permission after a short delay
        isProcessing = false;
        mainHandler.postDelayed(this::processNextPermission, 500);
    }
    
    /**
     * Check if battery optimizations are ignored
     */
    private boolean isIgnoringBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.os.PowerManager pm = (android.os.PowerManager) activity.getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(activity.getPackageName());
        }
        return true;
    }
    
    /**
     * Check if app has notification policy access
     */
    private boolean hasNotificationPolicyAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.app.NotificationManager nm = (android.app.NotificationManager) 
                activity.getSystemService(Context.NOTIFICATION_SERVICE);
            return nm != null && nm.isNotificationPolicyAccessGranted();
        }
        return true;
    }
    
    /**
     * Cancel all pending permissions
     */
    public void cancelPermissionFlow() {
        permissionQueue.clear();
        isProcessing = false;
        currentRequest = null;
        callback.onPermissionRequestCancelled();
    }
}