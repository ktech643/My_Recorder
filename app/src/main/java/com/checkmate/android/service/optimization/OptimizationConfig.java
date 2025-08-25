package com.checkmate.android.service.optimization;

/**
 * Configuration constants for live stream optimization.
 */
public class OptimizationConfig {
    
    // EGL Configuration
    public static final int EGL_INIT_TIMEOUT_SECONDS = 5;
    public static final boolean EGL_RECORDABLE = true;
    
    // Transition Configuration
    public static final int TRANSITION_TIMEOUT_MS = 3000;
    public static final int PRELOAD_DELAY_MS = 100;
    public static final int MAX_PRELOAD_SERVICES = 2;
    
    // Blank Frame Configuration
    public static final int BLANK_FRAME_RATE_MS = 33; // ~30 FPS
    public static final int BLANK_FRAME_TEXT_SIZE_DP = 48;
    public static final int BLANK_FRAME_PADDING_DP = 20;
    
    // Service Management
    public static final int SERVICE_INIT_RETRY_COUNT = 10;
    public static final int SERVICE_INIT_RETRY_DELAY_MS = 100;
    
    // Performance
    public static final long MIN_FRAME_INTERVAL_MS = 33;
    public static final long SLOW_FRAME_THRESHOLD_NS = 16_666_666;
    public static final long PERFORMANCE_LOG_INTERVAL_MS = 5000;
    
    // Dynamic Configuration
    public static final int MIN_VIDEO_BITRATE = 100000;
    public static final int MAX_VIDEO_BITRATE = 50000000;
    public static final int MIN_AUDIO_BITRATE = 32000;
    public static final int MAX_AUDIO_BITRATE = 320000;
    public static final int MIN_FPS = 15;
    public static final int MAX_FPS = 60;
    public static final int MIN_WIDTH = 176;
    public static final int MIN_HEIGHT = 144;
    public static final int MAX_WIDTH = 3840;
    public static final int MAX_HEIGHT = 2160;
    
    // Preference Keys
    public static final String PREF_AUTO_PRELOAD = "auto_preload_services";
    public static final String PREF_LAST_USED_SERVICE = "last_used_service";
    public static final String PREF_STREAM_STARTED = "stream_started";
    public static final String PREF_RECORD_STARTED = "record_started";
    
    private OptimizationConfig() {
        // Private constructor to prevent instantiation
    }
}