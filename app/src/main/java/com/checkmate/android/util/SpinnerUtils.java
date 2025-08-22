package com.checkmate.android.util;

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.checkmate.android.ui.view.EnhancedSpinner;

import java.util.List;

/**
 * Utility class for managing enhanced spinners throughout the application
 * Provides helper methods for spinner operations and conversions
 */
public class SpinnerUtils {

    /**
     * Convert a standard Spinner to EnhancedSpinner
     * This method helps migrate existing spinners to the new enhanced version
     */
    public static EnhancedSpinner convertToEnhancedSpinner(Context context, Spinner originalSpinner) {
        EnhancedSpinner enhancedSpinner = new EnhancedSpinner(context);
        
        // Copy adapter data
        if (originalSpinner.getAdapter() != null) {
            ArrayAdapter<?> adapter = (ArrayAdapter<?>) originalSpinner.getAdapter();
            List<String> items = extractItemsFromAdapter(adapter);
            enhancedSpinner.setItems(items);
        }
        
        // Copy selection
        enhancedSpinner.setSelection(originalSpinner.getSelectedItemPosition());
        
        // Copy layout parameters
        enhancedSpinner.setLayoutParams(originalSpinner.getLayoutParams());
        
        return enhancedSpinner;
    }

    /**
     * Extract items from an ArrayAdapter
     */
    private static List<String> extractItemsFromAdapter(ArrayAdapter<?> adapter) {
        List<String> items = new java.util.ArrayList<>();
        for (int i = 0; i < adapter.getCount(); i++) {
            Object item = adapter.getItem(i);
            items.add(item != null ? item.toString() : "");
        }
        return items;
    }

    /**
     * Setup enhanced spinner with items and listener
     */
    public static void setupEnhancedSpinner(EnhancedSpinner spinner, List<String> items, 
                                          EnhancedSpinner.OnItemSelectedListener listener) {
        spinner.setItems(items);
        spinner.setOnItemSelectedListener(listener);
    }

    /**
     * Setup enhanced spinner with items, listener, and custom colors
     */
    public static void setupEnhancedSpinner(EnhancedSpinner spinner, List<String> items,
                                          EnhancedSpinner.OnItemSelectedListener listener,
                                          int primaryColor, int accentColor) {
        setupEnhancedSpinner(spinner, items, listener);
        spinner.setPrimaryColor(primaryColor);
        spinner.setAccentColor(accentColor);
    }

    /**
     * Create a list of common camera resolutions
     */
    public static List<String> getCameraResolutions() {
        List<String> resolutions = new java.util.ArrayList<>();
        resolutions.add("1920x1080 (Full HD)");
        resolutions.add("1280x720 (HD)");
        resolutions.add("854x480 (SD)");
        resolutions.add("640x480 (VGA)");
        resolutions.add("320x240 (QVGA)");
        return resolutions;
    }

    /**
     * Create a list of common frame rates
     */
    public static List<String> getFrameRates() {
        List<String> frameRates = new java.util.ArrayList<>();
        frameRates.add("30 FPS");
        frameRates.add("25 FPS");
        frameRates.add("24 FPS");
        frameRates.add("20 FPS");
        frameRates.add("15 FPS");
        frameRates.add("10 FPS");
        return frameRates;
    }

    /**
     * Create a list of common audio sample rates
     */
    public static List<String> getAudioSampleRates() {
        List<String> sampleRates = new java.util.ArrayList<>();
        sampleRates.add("48000 Hz");
        sampleRates.add("44100 Hz");
        sampleRates.add("22050 Hz");
        sampleRates.add("16000 Hz");
        sampleRates.add("8000 Hz");
        return sampleRates;
    }

    /**
     * Create a list of common audio bitrates
     */
    public static List<String> getAudioBitrates() {
        List<String> bitrates = new java.util.ArrayList<>();
        bitrates.add("320 kbps");
        bitrates.add("256 kbps");
        bitrates.add("192 kbps");
        bitrates.add("128 kbps");
        bitrates.add("96 kbps");
        bitrates.add("64 kbps");
        return bitrates;
    }

    /**
     * Create a list of common video qualities
     */
    public static List<String> getVideoQualities() {
        List<String> qualities = new java.util.ArrayList<>();
        qualities.add("Ultra High");
        qualities.add("High");
        qualities.add("Medium");
        qualities.add("Low");
        qualities.add("Ultra Low");
        return qualities;
    }

    /**
     * Create a list of common codecs
     */
    public static List<String> getVideoCodecs() {
        List<String> codecs = new java.util.ArrayList<>();
        codecs.add("H.264");
        codecs.add("H.265 (HEVC)");
        codecs.add("VP9");
        codecs.add("VP8");
        codecs.add("MJPEG");
        return codecs;
    }

    /**
     * Create a list of common audio codecs
     */
    public static List<String> getAudioCodecs() {
        List<String> codecs = new java.util.ArrayList<>();
        codecs.add("AAC");
        codecs.add("MP3");
        codecs.add("Opus");
        codecs.add("Vorbis");
        codecs.add("PCM");
        return codecs;
    }

    /**
     * Find index of item in list (case-insensitive)
     */
    public static int findItemIndex(List<String> items, String searchItem) {
        if (items == null || searchItem == null) {
            return -1;
        }
        
        for (int i = 0; i < items.size(); i++) {
            if (searchItem.equalsIgnoreCase(items.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get item at index safely
     */
    public static String getItemSafely(List<String> items, int index) {
        if (items != null && index >= 0 && index < items.size()) {
            return items.get(index);
        }
        return "";
    }

    /**
     * Validate spinner selection
     */
    public static boolean isValidSelection(int position, int itemCount) {
        return position >= 0 && position < itemCount;
    }
}
