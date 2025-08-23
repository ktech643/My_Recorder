package com.checkmate.android.util;

import android.content.Context;
import android.graphics.Color;

import com.checkmate.android.ui.components.EnhancedSpinner;

import java.util.Arrays;
import java.util.List;

public class SpinnerUtils {

    public static void setupQualitySpinner(EnhancedSpinner spinner, Context context) {
        List<String> qualities = Arrays.asList("1080p", "720p", "480p", "360p");
        spinner.setTitle("Video Quality");
        spinner.setItems(qualities);
        spinner.setSelection(0); // Default to 1080p
    }

    public static void setupResolutionSpinner(EnhancedSpinner spinner, Context context) {
        List<String> resolutions = Arrays.asList("1920x1080", "1280x720", "854x480", "640x360");
        spinner.setTitle("Resolution");
        spinner.setItems(resolutions);
        spinner.setSelection(0); // Default to 1920x1080
    }

    public static void setupBitrateSpinner(EnhancedSpinner spinner, Context context) {
        List<String> bitrates = Arrays.asList("8000 Kbps", "4000 Kbps", "2000 Kbps", "1000 Kbps");
        spinner.setTitle("Bitrate");
        spinner.setItems(bitrates);
        spinner.setSelection(1); // Default to 4000 Kbps
    }

    public static void setupFramerateSpinner(EnhancedSpinner spinner, Context context) {
        List<String> framerates = Arrays.asList("60 FPS", "30 FPS", "24 FPS", "15 FPS");
        spinner.setTitle("Framerate");
        spinner.setItems(framerates);
        spinner.setSelection(1); // Default to 30 FPS
    }

    public static void setupCameraSpinner(EnhancedSpinner spinner, Context context) {
        List<String> cameras = Arrays.asList("Front Camera", "Back Camera", "USB Camera", "IP Camera");
        spinner.setTitle("Camera");
        spinner.setItems(cameras);
        spinner.setSelection(1); // Default to Back Camera
    }

    public static void setupRotationSpinner(EnhancedSpinner spinner, Context context) {
        List<String> rotations = Arrays.asList("0°", "90°", "180°", "270°");
        spinner.setTitle("Rotation");
        spinner.setItems(rotations);
        spinner.setSelection(0); // Default to 0°
    }

    public static void applyMaterialDesignTheme(EnhancedSpinner spinner) {
        spinner.setPrimaryColor(Color.parseColor("#2196F3"));
        spinner.setAccentColor(Color.parseColor("#FF5722"));
        spinner.setTextColor(Color.parseColor("#333333"));
        spinner.setBackgroundColor(Color.WHITE);
    }

    public static void applyCameraTheme(EnhancedSpinner spinner) {
        spinner.setPrimaryColor(Color.parseColor("#FF5722"));
        spinner.setAccentColor(Color.parseColor("#FF9800"));
        spinner.setTextColor(Color.parseColor("#FFFFFF"));
        spinner.setBackgroundColor(Color.parseColor("#CCFFFFFF"));
    }

    public static void applyRotationTheme(EnhancedSpinner spinner) {
        spinner.setPrimaryColor(Color.parseColor("#4CAF50"));
        spinner.setAccentColor(Color.parseColor("#8BC34A"));
        spinner.setTextColor(Color.parseColor("#FFFFFF"));
        spinner.setBackgroundColor(Color.parseColor("#CCFFFFFF"));
    }

    public static void applyDarkTheme(EnhancedSpinner spinner) {
        spinner.setPrimaryColor(Color.parseColor("#424242"));
        spinner.setAccentColor(Color.parseColor("#757575"));
        spinner.setTextColor(Color.parseColor("#FFFFFF"));
        spinner.setBackgroundColor(Color.parseColor("#212121"));
    }
}
