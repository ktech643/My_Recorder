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
        List<String> resolutions = Arrays.asList(
            "1920x1080", "1280x720", "854x480", "640x360"
        );
        spinner.setTitle("Resolution");
        spinner.setItems(resolutions);
        spinner.setSelection(0);
    }
    
    public static void setupBitrateSpinner(EnhancedSpinner spinner, Context context) {
        List<String> bitrates = Arrays.asList(
            "8000 kbps", "4000 kbps", "2000 kbps", "1000 kbps"
        );
        spinner.setTitle("Bitrate");
        spinner.setItems(bitrates);
        spinner.setSelection(1); // Default to 4000 kbps
    }
    
    public static void setupFramerateSpinner(EnhancedSpinner spinner, Context context) {
        List<String> framerates = Arrays.asList("60 fps", "30 fps", "24 fps", "15 fps");
        spinner.setTitle("Frame Rate");
        spinner.setItems(framerates);
        spinner.setSelection(1); // Default to 30 fps
    }
    
    public static void applyMaterialDesignTheme(EnhancedSpinner spinner) {
        spinner.setPrimaryColor(Color.parseColor("#2196F3"));
        spinner.setAccentColor(Color.parseColor("#FF5722"));
        spinner.setTextColor(Color.parseColor("#333333"));
        spinner.setBackgroundColor(Color.WHITE);
    }
    
    public static void applyDarkTheme(EnhancedSpinner spinner) {
        spinner.setPrimaryColor(Color.parseColor("#BB86FC"));
        spinner.setAccentColor(Color.parseColor("#03DAC6"));
        spinner.setTextColor(Color.WHITE);
        spinner.setBackgroundColor(Color.parseColor("#1E1E1E"));
    }
}
