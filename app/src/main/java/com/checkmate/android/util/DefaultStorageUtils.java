package com.checkmate.android.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;

public class DefaultStorageUtils {
    private static final String TAG = "DefaultStorageUtils";
    private static final String APP_FOLDER_NAME = "checkmate";
    
    /**
     * Get the default storage directory for the app
     * Creates the directory if it doesn't exist
     */
    public static File getDefaultStorageDirectory() {
        File defaultDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), APP_FOLDER_NAME);
        if (!defaultDir.exists()) {
            boolean created = defaultDir.mkdirs();
            if (!created) {
                Log.e(TAG, "Failed to create default storage directory: " + defaultDir.getAbsolutePath());
                // Fallback to app's private directory
                return new File(Environment.getExternalStorageDirectory(), APP_FOLDER_NAME);
            }
        }
        return defaultDir;
    }
    
    /**
     * Get the default storage path as a string
     */
    public static String getDefaultStoragePath() {
        return getDefaultStorageDirectory().getAbsolutePath();
    }
    
    /**
     * Check if the default storage directory is accessible and writable
     */
    public static boolean isDefaultStorageAccessible() {
        File defaultDir = getDefaultStorageDirectory();
        return defaultDir.exists() && defaultDir.canWrite();
    }
    
    /**
     * Create a file in the default storage directory
     */
    public static File createFileInDefaultStorage(String fileName) {
        File defaultDir = getDefaultStorageDirectory();
        return new File(defaultDir, fileName);
    }
    
    /**
     * Get the default storage directory for videos
     */
    public static File getDefaultVideoDirectory() {
        File videoDir = new File(getDefaultStorageDirectory(), "videos");
        if (!videoDir.exists()) {
            videoDir.mkdirs();
        }
        return videoDir;
    }
    
    /**
     * Get the default storage directory for images
     */
    public static File getDefaultImageDirectory() {
        File imageDir = new File(getDefaultStorageDirectory(), "images");
        if (!imageDir.exists()) {
            imageDir.mkdirs();
        }
        return imageDir;
    }
} 