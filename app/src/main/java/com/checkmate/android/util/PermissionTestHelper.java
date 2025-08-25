package com.checkmate.android.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for testing and validating permission states
 */
public class PermissionTestHelper {
    private static final String TAG = "PermissionTestHelper";
    
    /**
     * Test all permission states and log results
     */
    public static void testAllPermissions(Activity activity) {
        Log.d(TAG, "=== PERMISSION TEST REPORT ===");
        
        // Test core permissions
        testCorePermissions(activity);
        
        // Test storage permissions
        testStoragePermissions(activity);
        
        // Test special permissions
        testSpecialPermissions(activity);
        
        // Test notification permissions
        testNotificationPermissions(activity);
        
        Log.d(TAG, "=== END PERMISSION TEST ===");
    }
    
    /**
     * Test core runtime permissions
     */
    private static void testCorePermissions(Activity activity) {
        Log.d(TAG, "--- Core Permissions ---");
        
        String[] corePermissions = {
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
        
        int granted = 0;
        int total = corePermissions.length;
        
        for (String permission : corePermissions) {
            boolean isGranted = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, permission + ": " + (isGranted ? "GRANTED" : "DENIED"));
            if (isGranted) granted++;
        }
        
        Log.d(TAG, "Core Permissions: " + granted + "/" + total + " granted");
    }
    
    /**
     * Test storage permissions based on Android version
     */
    private static void testStoragePermissions(Activity activity) {
        Log.d(TAG, "--- Storage Permissions ---");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            testPermission(activity, Manifest.permission.READ_MEDIA_IMAGES, "Read Media Images");
            testPermission(activity, Manifest.permission.READ_MEDIA_VIDEO, "Read Media Video");
            testPermission(activity, Manifest.permission.READ_MEDIA_AUDIO, "Read Media Audio");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+
            testPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE, "Read External Storage");
        } else {
            // Android 9 and below
            testPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE, "Write External Storage");
            testPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE, "Read External Storage");
        }
        
        // Test if storage helper reports correct state
        boolean storageGranted = StoragePermissionHelper.areStoragePermissionsGranted(activity);
        Log.d(TAG, "StoragePermissionHelper reports: " + (storageGranted ? "GRANTED" : "DENIED"));
    }
    
    /**
     * Test special system permissions
     */
    private static void testSpecialPermissions(Activity activity) {
        Log.d(TAG, "--- Special Permissions ---");
        
        // Overlay permission
        boolean canDrawOverlays = android.provider.Settings.canDrawOverlays(activity);
        Log.d(TAG, "Display over other apps: " + (canDrawOverlays ? "GRANTED" : "DENIED"));
        
        // Battery optimization
        android.os.PowerManager powerManager = (android.os.PowerManager) activity.getSystemService(android.content.Context.POWER_SERVICE);
        boolean ignoringBattery = powerManager != null && powerManager.isIgnoringBatteryOptimizations(activity.getPackageName());
        Log.d(TAG, "Ignore battery optimizations: " + (ignoringBattery ? "GRANTED" : "DENIED"));
        
        // System settings
        boolean canWriteSettings = android.provider.Settings.System.canWrite(activity);
        Log.d(TAG, "Modify system settings: " + (canWriteSettings ? "GRANTED" : "DENIED"));
        
        // Do not disturb
        android.app.NotificationManager notificationManager = 
                (android.app.NotificationManager) activity.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        boolean hasDoNotDisturb = notificationManager != null && notificationManager.isNotificationPolicyAccessGranted();
        Log.d(TAG, "Do not disturb access: " + (hasDoNotDisturb ? "GRANTED" : "DENIED"));
    }
    
    /**
     * Test notification permissions
     */
    private static void testNotificationPermissions(Activity activity) {
        Log.d(TAG, "--- Notification Permissions ---");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            testPermission(activity, Manifest.permission.POST_NOTIFICATIONS, "Post Notifications");
        } else {
            Log.d(TAG, "Notification permission not required for Android " + Build.VERSION.SDK_INT);
        }
    }
    
    /**
     * Test a single permission
     */
    private static void testPermission(Activity activity, String permission, String displayName) {
        boolean isGranted = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, displayName + ": " + (isGranted ? "GRANTED" : "DENIED"));
    }
    
    /**
     * Get summary of missing permissions
     */
    public static List<String> getMissingPermissions(Activity activity) {
        List<String> missing = new ArrayList<>();
        
        // Check core permissions
        String[] corePermissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        };
        
        for (String permission : corePermissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission);
            }
        }
        
        // Check storage permissions
        if (!StoragePermissionHelper.areStoragePermissionsGranted(activity)) {
            missing.add("Storage permissions");
        }
        
        // Check notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                missing.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        
        return missing;
    }
    
    /**
     * Check if all critical permissions are granted
     */
    public static boolean areAllCriticalPermissionsGranted(Activity activity) {
        List<String> missing = getMissingPermissions(activity);
        
        // Filter out non-critical permissions
        missing.removeIf(permission -> 
            permission.equals(Manifest.permission.POST_NOTIFICATIONS) || 
            permission.equals("Storage permissions"));
        
        return missing.isEmpty();
    }
    
    /**
     * Get user-friendly permission status summary
     */
    public static String getPermissionSummary(Activity activity) {
        StringBuilder summary = new StringBuilder();
        
        List<String> missing = getMissingPermissions(activity);
        
        if (missing.isEmpty()) {
            summary.append("✅ All permissions granted");
        } else {
            summary.append("⚠️ Missing permissions: ");
            for (int i = 0; i < missing.size(); i++) {
                String permission = missing.get(i);
                // Convert to user-friendly name
                String friendlyName = getFriendlyPermissionName(permission);
                summary.append(friendlyName);
                if (i < missing.size() - 1) {
                    summary.append(", ");
                }
            }
        }
        
        return summary.toString();
    }
    
    /**
     * Convert permission to user-friendly name
     */
    private static String getFriendlyPermissionName(String permission) {
        switch (permission) {
            case Manifest.permission.CAMERA:
                return "Camera";
            case Manifest.permission.RECORD_AUDIO:
                return "Microphone";
            case Manifest.permission.ACCESS_FINE_LOCATION:
                return "Location";
            case Manifest.permission.READ_PHONE_STATE:
                return "Phone State";
            case Manifest.permission.POST_NOTIFICATIONS:
                return "Notifications";
            case "Storage permissions":
                return "Storage";
            default:
                return permission.substring(permission.lastIndexOf('.') + 1);
        }
    }
}