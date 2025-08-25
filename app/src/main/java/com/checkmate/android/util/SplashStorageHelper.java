package com.checkmate.android.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.checkmate.android.AppPreference;

import java.util.List;
import java.util.ArrayList;

/**
 * Helper class for handling storage selection during splash screen activation
 * Copies functionality from SettingsFragment for consistent behavior
 */
public class SplashStorageHelper {
    private static final String TAG = "SplashStorageHelper";

    public static final int REQUEST_STORAGE_PERMISSION = 1001;
    public static final int REQUEST_CODE_PICK_FOLDER = 0x4242;

    private Activity activity;
    private StorageSelectionCallback callback;
    private AlertDialog currentDialog; // Track current dialog to prevent window leaks

    public interface StorageSelectionCallback {
        void onStorageSelected(String storagePath, String storageType);
        void onStorageSelectionCancelled();
    }

    public SplashStorageHelper(Activity activity, StorageSelectionCallback callback) {
        this.activity = activity;
        this.callback = callback;
    }

    /**
     * Check and request storage permissions, then show folder selection
     */
    public void checkPermissionsAndSelectStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+) - Use MANAGE_EXTERNAL_STORAGE
            if (!Environment.isExternalStorageManager()) {
                showManageStoragePermissionDialog();
            } else {
                openDirectoryPicker();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+) - Use granular media permissions
            List<String> permissionsToRequest = new ArrayList<>();
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO);
            }

            if (!permissionsToRequest.isEmpty()) {
                ActivityCompat.requestPermissions(activity,
                        permissionsToRequest.toArray(new String[0]),
                        REQUEST_STORAGE_PERMISSION);
            } else {
                openDirectoryPicker();
            }
        } else {
            // Android 12 and below - Use legacy storage permissions
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            } else {
                openDirectoryPicker();
            }
        }
    }

    /**
     * Show dialog to request MANAGE_EXTERNAL_STORAGE permission
     */
    private void showManageStoragePermissionDialog() {
        new AlertDialog.Builder(activity)
                .setTitle("Storage Permission Required")
                .setMessage("This app needs access to all files to save recordings. Please grant the permission in the next screen.")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" + activity.getPackageName()));
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to request MANAGE_EXTERNAL_STORAGE", e);
                        Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        activity.startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    if (callback != null) {
                        callback.onStorageSelectionCancelled();
                    }
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Open directory picker for folder selection
     */
    public void openDirectoryPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        activity.startActivityForResult(intent, REQUEST_CODE_PICK_FOLDER);
    }

    /**
     * Handle permission result
     */
    public void handlePermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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
                openDirectoryPicker();
            } else {
                Log.w(TAG, "Storage permissions denied!");
                Toast.makeText(activity, "Storage permission is required to save recordings", Toast.LENGTH_LONG).show();
                if (callback != null) {
                    callback.onStorageSelectionCancelled();
                }
            }
        }
    }

    /**
     * Handle folder selection result
     */
    public void handleFolderSelectionResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_FOLDER && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedFolderUri = data.getData();
            if (selectedFolderUri != null) {
                handleSelectedFolder(selectedFolderUri);
            } else {
                Log.w(TAG, "No folder selected");
                if (callback != null) {
                    callback.onStorageSelectionCancelled();
                }
            }
        } else if (requestCode == REQUEST_CODE_PICK_FOLDER && resultCode == Activity.RESULT_CANCELED) {
            Log.w(TAG, "User cancelled folder selection");
            if (callback != null) {
                callback.onStorageSelectionCancelled();
            }
        }
    }

    /**
     * Handle the selected folder and set storage preferences
     */
    private void handleSelectedFolder(Uri treeUri) {
        try {
            // Persist permissions for future access
            final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            activity.getContentResolver().takePersistableUriPermission(treeUri, takeFlags);

            // Save the selected folder
            String uriString = treeUri.toString();
            AppPreference.setStr(AppPreference.KEY.GALLERY_PATH, uriString);
            AppPreference.setStr(AppPreference.KEY.STORAGE_LOCATION, uriString);

            // Determine storage type and set preferences
            String storageType = determineStorageType(treeUri);
            String storagePath = getFullPathFromTreeUri(treeUri);

            Log.d(TAG, "Storage selected - Type: " + storageType + ", Path: " + storagePath);

            // Notify callback
            if (callback != null) {
                callback.onStorageSelected(storagePath, storageType);
            }

            Toast.makeText(activity, "Storage location set to: " + storageType, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error handling selected folder", e);
            Toast.makeText(activity, "Error setting storage location", Toast.LENGTH_SHORT).show();
            if (callback != null) {
                callback.onStorageSelectionCancelled();
            }
        }
    }

    /**
     * Determine storage type from URI
     */
    private String determineStorageType(Uri treeUri) {
        String docId = DocumentsContract.getTreeDocumentId(treeUri);
        String[] split = docId.split(":");
        String volumeId = split[0]; // This is "primary" for internal storage, or a UUID for external.

        // Check if the storage is internal
        if ("primary".equalsIgnoreCase(volumeId)) {
            AppPreference.setStr(AppPreference.KEY.IS_STORAGE_INTERNAL, "INTERNAL STORAGE");
            AppPreference.setStr(AppPreference.KEY.Storage_Type, "Storage Location: Phone Storage");
            return "Phone Storage";
        } else {
            // For non-primary storage, use StorageManager/StorageVolume
            StorageManager storageManager = (StorageManager) activity.getSystemService(Context.STORAGE_SERVICE);
            if (storageManager != null) {
                List<StorageVolume> volumes = storageManager.getStorageVolumes();
                for (StorageVolume volume : volumes) {
                    String uuid = volume.getUuid();
                    if (uuid != null && uuid.equals(volumeId)) {
                        String description = volume.getDescription(activity);
                        if (description != null) {
                            description = description.toLowerCase();
                            if (description.contains("usb") || description.contains("otg") ||
                                    description.contains("mass storage")) {
                                AppPreference.setStr(AppPreference.KEY.IS_STORAGE_EXTERNAL, "EXTERNAL");
                                AppPreference.setStr(AppPreference.KEY.Storage_Type, "Storage Location: USB Storage");
                                return "USB Storage";
                            } else if (description.contains("sd")) {
                                AppPreference.setStr(AppPreference.KEY.IS_STORAGE_SDCARD, "SDCARD");
                                AppPreference.setStr(AppPreference.KEY.Storage_Type, "Storage Location: SDCARD Storage");
                                return "SDCARD Storage";
                            }
                        }
                    }
                }
            }

            // Default to external storage
            AppPreference.setStr(AppPreference.KEY.IS_STORAGE_EXTERNAL, "EXTERNAL");
            AppPreference.setStr(AppPreference.KEY.Storage_Type, "Storage Location: External Storage");
            return "External Storage";
        }
    }

    /**
     * Get full path from tree URI
     */
    private String getFullPathFromTreeUri(Uri treeUri) {
        if (treeUri == null || treeUri.toString().isEmpty()) return null;

        try {
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
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid tree URI: " + treeUri, e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing tree URI: " + treeUri, e);
            return null;
        }
    }

    /**
     * Show storage selection dialog with options
     */
    public void showStorageSelectionDialog() {
        new AlertDialog.Builder(activity)
                .setTitle("Choose Storage Location")
                .setMessage("Select a folder to save your recordings")
                .setPositiveButton("Select Storage", (dialog, which) -> {
                    // Select custom folder
                    Log.d(TAG, "Storage selection requested");
                    checkPermissionsAndSelectStorage();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Show storage selection dialog with detailed options
     */
    public void showDetailedStorageSelectionDialog() {
        Log.d(TAG, "Showing detailed storage selection dialog");

        try {
            AlertDialog dialog = new AlertDialog.Builder(activity)
                    .setTitle("📂 Choose Storage Location")
                    .setMessage("Select a folder to save your recordings")
                    .setPositiveButton("📁 Select Storage", (dialogInterface, which) -> {
                        Log.d(TAG, "User selected storage");
                        checkPermissionsAndSelectStorage();
                    })
                    .setCancelable(false)
                    .create();

            Log.d(TAG, "Dialog created successfully");
            dialog.show();
            Log.d(TAG, "Dialog shown successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error showing storage selection dialog", e);
            // Fallback to simple dialog
            showSimpleStorageDialog();
        }
    }

    /**
     * Fallback simple storage dialog
     */
    private void showSimpleStorageDialog() {
        Log.d(TAG, "Showing simple storage dialog as fallback");

        try {
            new AlertDialog.Builder(activity)
                    .setTitle("Storage Selection")
                    .setMessage("Select a folder to save your recordings")
                    .setPositiveButton("Select Storage", (dialog, which) -> {
                        checkPermissionsAndSelectStorage();
                    })
                    .setCancelable(false)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing simple dialog", e);
            // Last resort - use default storage
            String defaultPath = DefaultStorageUtils.getDefaultStoragePath();
            AppPreference.setStr(AppPreference.KEY.STORAGE_LOCATION, defaultPath);
            AppPreference.setStr(AppPreference.KEY.GALLERY_PATH, defaultPath);
            AppPreference.setStr(AppPreference.KEY.Storage_Type, "Storage Location: Phone Storage");
            AppPreference.setStr(AppPreference.KEY.IS_STORAGE_INTERNAL, "INTERNAL STORAGE");

            if (callback != null) {
                callback.onStorageSelected(defaultPath, "Phone Storage");
            }
        }
    }

    /**
     * Show custom storage selection dialog with two options: Default and Select Location
     */
    public void showCustomStorageDialog() {
        Log.d(TAG, "Showing custom storage selection dialog with two options");

        try {
            // Create custom dialog with two options
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("📂 Choose Storage Location");
            builder.setMessage("Select how you want to save your recordings:");

            // Set custom buttons with two options
            builder.setPositiveButton("📁 Select Location", (dialog, which) -> {
                Log.d(TAG, "User selected custom storage location");
                checkPermissionsAndSelectStorage();
            });

            builder.setNeutralButton("📂 Use Default", (dialog, which) -> {
                Log.d(TAG, "User selected default storage");
                setDefaultStorage();
            });

            builder.setCancelable(false);

            currentDialog = builder.create();
            currentDialog.show();

            // Try to make buttons more visible
            try {
                currentDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(16);
                currentDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextSize(16);
            } catch (Exception e) {
                Log.w(TAG, "Could not set button text size", e);
            }

            Log.d(TAG, "Custom dialog with two options shown successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error showing custom dialog with two options", e);
            showSimpleStorageDialog();
        }
    }

    /**
     * Set default storage location
     */
    private void setDefaultStorage() {
        Log.d(TAG, "Setting default storage location");

        String defaultPath = DefaultStorageUtils.getDefaultStoragePath();
        AppPreference.setStr(AppPreference.KEY.STORAGE_LOCATION, defaultPath);
        AppPreference.setStr(AppPreference.KEY.GALLERY_PATH, defaultPath);
        AppPreference.setStr(AppPreference.KEY.Storage_Type, "Storage Location: Phone Storage");
        AppPreference.setStr(AppPreference.KEY.IS_STORAGE_INTERNAL, "INTERNAL STORAGE");

        Log.d(TAG, "Default storage set to: " + defaultPath);

        // Notify callback
        if (callback != null) {
            callback.onStorageSelected(defaultPath, "Phone Storage");
        }

        Toast.makeText(activity, "Default storage location set", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Cleanup method to prevent window leaks
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up SplashStorageHelper");
        
        // Dismiss any current dialog to prevent window leak
        if (currentDialog != null && currentDialog.isShowing()) {
            try {
                currentDialog.dismiss();
            } catch (Exception e) {
                Log.w(TAG, "Error dismissing dialog during cleanup", e);
            }
            currentDialog = null;
        }
        
        // Clear references
        activity = null;
        callback = null;
    }
}