package com.checkmate.android.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Helper class for handling storage permissions across different Android versions
 */
public class StoragePermissionHelper {
    private static final String TAG = "StoragePermissionHelper";

    public static final int REQUEST_STORAGE_PERMISSION = 1001;

    /**
     * Check if storage permissions are granted
     */
    public static boolean areStoragePermissionsGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+) - Use granular media permissions
            boolean hasImages = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
            boolean hasVideo = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
            boolean hasAudio = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;

            boolean allGranted = hasImages && hasVideo && hasAudio;
            Log.d(TAG, "Android 13+ storage permissions - Images: " + hasImages +
                    ", Video: " + hasVideo + ", Audio: " + hasAudio);
            return allGranted;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ - Use READ_EXTERNAL_STORAGE
            boolean hasRead = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, "Android 10+ storage permission - Read: " + hasRead);
            return hasRead;
        } else {
            // Android 9 and below - Use WRITE_EXTERNAL_STORAGE
            boolean hasWrite = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            boolean hasRead = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            boolean allGranted = hasWrite && hasRead;
            Log.d(TAG, "Legacy storage permissions - Write: " + hasWrite + ", Read: " + hasRead);
            return allGranted;
        }
    }

    /**
     * Get the required storage permissions for the current Android version
     */
    public static String[] getRequiredStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        } else {
            return new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }
    }

    /**
     * Request storage permissions if not already granted
     */
    public static void requestStoragePermissions(Activity activity) {
        if (!areStoragePermissionsGranted(activity)) {
            Log.d(TAG, "Requesting storage permissions");
            String[] permissions = getRequiredStoragePermissions();
            ActivityCompat.requestPermissions(activity, permissions, REQUEST_STORAGE_PERMISSION);
        } else {
            Log.d(TAG, "Storage permissions already granted");
        }
    }

    /**
     * Handle permission result and validate storage location
     */
    public static void handlePermissionResult(Context context, int requestCode,
                                              String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Log.d(TAG, "Storage permissions granted!");
                // Validate and update storage location after permissions are granted
                PreferenceInitializer.validateStorageLocation(context);
            } else {
                Log.w(TAG, "Storage permissions denied!");
                // Even if permission is denied, try to use default storage
                PreferenceInitializer.validateStorageLocation(context);
            }
        }
    }

    /**
     * Check if we should show permission rationale
     */
    public static boolean shouldShowPermissionRationale(Activity activity) {
        String[] permissions = getRequiredStoragePermissions();
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a user-friendly description of required permissions
     */
    public static String getPermissionDescription() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return "This app needs access to your photos, videos, and audio to save recordings.";
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return "This app needs access to your files to save recordings.";
        } else {
            return "This app needs access to your storage to save recordings.";
        }
    }
}