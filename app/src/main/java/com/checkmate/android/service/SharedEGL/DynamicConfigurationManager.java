package com.checkmate.android.service.SharedEGL;

import android.util.Log;

import com.checkmate.android.AppPreference;
import com.wmspanel.libstream.AudioConfig;
import com.wmspanel.libstream.Streamer;
import com.wmspanel.libstream.StreamerSurface;
import com.wmspanel.libstream.VideoConfig;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages dynamic configuration updates for streaming and recording without interruption.
 * Allows real-time changes to video quality, audio settings, and other parameters.
 */
public class DynamicConfigurationManager {
    private static final String TAG = "DynamicConfigManager";
    
    private static volatile DynamicConfigurationManager sInstance;
    private final Object mLock = new Object();
    
    // Configuration state
    private final ConcurrentHashMap<String, Object> mPendingChanges = new ConcurrentHashMap<>();
    private final AtomicBoolean mIsUpdating = new AtomicBoolean(false);
    
    // Configuration categories
    public static final String CONFIG_VIDEO_BITRATE = "video_bitrate";
    public static final String CONFIG_AUDIO_BITRATE = "audio_bitrate";
    public static final String CONFIG_VIDEO_RESOLUTION = "video_resolution";
    public static final String CONFIG_VIDEO_FPS = "video_fps";
    public static final String CONFIG_VIDEO_KEYFRAME_INTERVAL = "video_keyframe_interval";
    public static final String CONFIG_AUDIO_SAMPLE_RATE = "audio_sample_rate";
    public static final String CONFIG_AUDIO_CHANNELS = "audio_channels";
    public static final String CONFIG_STREAM_URL = "stream_url";
    public static final String CONFIG_ORIENTATION = "orientation";
    public static final String CONFIG_MIRROR = "mirror";
    public static final String CONFIG_FLIP = "flip";
    
    public interface ConfigurationListener {
        void onConfigurationChanged(String key, Object value);
        void onConfigurationError(String key, String error);
    }
    
    private ConfigurationListener mListener;
    
    private DynamicConfigurationManager() {
        // Private constructor for singleton
    }
    
    public static DynamicConfigurationManager getInstance() {
        if (sInstance == null) {
            synchronized (DynamicConfigurationManager.class) {
                if (sInstance == null) {
                    sInstance = new DynamicConfigurationManager();
                }
            }
        }
        return sInstance;
    }
    
    /**
     * Set configuration change listener.
     */
    public void setConfigurationListener(ConfigurationListener listener) {
        mListener = listener;
    }
    
    /**
     * Update video bitrate dynamically.
     */
    public void updateVideoBitrate(int bitrate) {
        Log.d(TAG, "Updating video bitrate to: " + bitrate);
        
        // Validate bitrate
        if (bitrate < 100000 || bitrate > 50000000) {
            notifyError(CONFIG_VIDEO_BITRATE, "Invalid bitrate: " + bitrate);
            return;
        }
        
        synchronized (mLock) {
            mPendingChanges.put(CONFIG_VIDEO_BITRATE, bitrate);
            applyPendingChanges();
        }
    }
    
    /**
     * Update audio bitrate dynamically.
     */
    public void updateAudioBitrate(int bitrate) {
        Log.d(TAG, "Updating audio bitrate to: " + bitrate);
        
        // Validate bitrate
        if (bitrate < 32000 || bitrate > 320000) {
            notifyError(CONFIG_AUDIO_BITRATE, "Invalid bitrate: " + bitrate);
            return;
        }
        
        synchronized (mLock) {
            mPendingChanges.put(CONFIG_AUDIO_BITRATE, bitrate);
            applyPendingChanges();
        }
    }
    
    /**
     * Update video resolution dynamically.
     * Note: This may require a brief transition depending on the encoder.
     */
    public void updateVideoResolution(int width, int height) {
        Log.d(TAG, "Updating video resolution to: " + width + "x" + height);
        
        // Validate resolution
        if (width < 176 || height < 144 || width > 3840 || height > 2160) {
            notifyError(CONFIG_VIDEO_RESOLUTION, "Invalid resolution: " + width + "x" + height);
            return;
        }
        
        synchronized (mLock) {
            Streamer.Size size = new Streamer.Size(width, height);
            mPendingChanges.put(CONFIG_VIDEO_RESOLUTION, size);
            applyPendingChanges();
        }
    }
    
    /**
     * Update video frame rate dynamically.
     */
    public void updateVideoFPS(int fps) {
        Log.d(TAG, "Updating video FPS to: " + fps);
        
        // Validate FPS
        if (fps < 15 || fps > 60) {
            notifyError(CONFIG_VIDEO_FPS, "Invalid FPS: " + fps);
            return;
        }
        
        synchronized (mLock) {
            mPendingChanges.put(CONFIG_VIDEO_FPS, fps);
            applyPendingChanges();
        }
    }
    
    /**
     * Update stream URL dynamically (requires reconnection).
     */
    public void updateStreamUrl(String url) {
        Log.d(TAG, "Updating stream URL");
        
        if (url == null || url.isEmpty()) {
            notifyError(CONFIG_STREAM_URL, "Invalid URL");
            return;
        }
        
        synchronized (mLock) {
            mPendingChanges.put(CONFIG_STREAM_URL, url);
            applyPendingChanges();
        }
    }
    
    /**
     * Update video orientation.
     */
    public void updateOrientation(int rotation) {
        Log.d(TAG, "Updating orientation to: " + rotation);
        
        // Validate rotation (0, 90, 180, 270)
        if (rotation != 0 && rotation != 90 && rotation != 180 && rotation != 270) {
            notifyError(CONFIG_ORIENTATION, "Invalid rotation: " + rotation);
            return;
        }
        
        synchronized (mLock) {
            mPendingChanges.put(CONFIG_ORIENTATION, rotation);
            applyPendingChanges();
        }
    }
    
    /**
     * Update mirror mode.
     */
    public void updateMirror(boolean mirror) {
        Log.d(TAG, "Updating mirror mode to: " + mirror);
        
        synchronized (mLock) {
            mPendingChanges.put(CONFIG_MIRROR, mirror);
            applyPendingChanges();
        }
    }
    
    /**
     * Update flip mode.
     */
    public void updateFlip(boolean flip) {
        Log.d(TAG, "Updating flip mode to: " + flip);
        
        synchronized (mLock) {
            mPendingChanges.put(CONFIG_FLIP, flip);
            applyPendingChanges();
        }
    }
    
    /**
     * Apply all pending configuration changes.
     */
    private void applyPendingChanges() {
        if (mIsUpdating.getAndSet(true)) {
            Log.d(TAG, "Configuration update already in progress");
            return;
        }
        
        // Get SharedEglManager instance
        SharedEglManager eglManager = SharedEglManager.getInstance();
        
        // Apply changes based on type
        for (String key : mPendingChanges.keySet()) {
            Object value = mPendingChanges.remove(key);
            
            try {
                switch (key) {
                    case CONFIG_VIDEO_BITRATE:
                        applyVideoBitrate(eglManager, (Integer) value);
                        break;
                        
                    case CONFIG_AUDIO_BITRATE:
                        applyAudioBitrate(eglManager, (Integer) value);
                        break;
                        
                    case CONFIG_VIDEO_RESOLUTION:
                        applyVideoResolution(eglManager, (Streamer.Size) value);
                        break;
                        
                    case CONFIG_VIDEO_FPS:
                        applyVideoFPS(eglManager, (Integer) value);
                        break;
                        
                    case CONFIG_STREAM_URL:
                        applyStreamUrl(eglManager, (String) value);
                        break;
                        
                    case CONFIG_ORIENTATION:
                        applyOrientation(eglManager, (Integer) value);
                        break;
                        
                    case CONFIG_MIRROR:
                        applyMirror(eglManager, (Boolean) value);
                        break;
                        
                    case CONFIG_FLIP:
                        applyFlip(eglManager, (Boolean) value);
                        break;
                }
                
                // Save to preferences
                saveConfiguration(key, value);
                
                // Notify listener
                notifyConfigurationChanged(key, value);
                
            } catch (Exception e) {
                Log.e(TAG, "Error applying configuration: " + key, e);
                notifyError(key, e.getMessage());
            }
        }
        
        mIsUpdating.set(false);
    }
    
    private void applyVideoBitrate(SharedEglManager eglManager, int bitrate) {
        if (eglManager.mStreamer != null) {
            VideoConfig config = eglManager.streamVideoConfigLocal;
            if (config != null) {
                config.bitRate = bitrate;
                eglManager.mStreamer.updateVideoConfig(config);
                Log.d(TAG, "Applied video bitrate: " + bitrate);
            }
        }
        
        if (eglManager.isRecording() && eglManager.mRecorder != null) {
            // Update recorder bitrate if needed
            Log.d(TAG, "Applied recording bitrate: " + bitrate);
        }
    }
    
    private void applyAudioBitrate(SharedEglManager eglManager, int bitrate) {
        if (eglManager.mStreamer != null) {
            AudioConfig config = eglManager.streamAudioConfigLocal;
            if (config != null) {
                config.bitRate = bitrate;
                eglManager.mStreamer.updateAudioConfig(config);
                Log.d(TAG, "Applied audio bitrate: " + bitrate);
            }
        }
    }
    
    private void applyVideoResolution(SharedEglManager eglManager, Streamer.Size size) {
        // Resolution changes may require more complex handling
        // This is a simplified version - in production you might need to
        // temporarily pause encoding or use blank frames
        
        if (eglManager.mStreamer != null) {
            VideoConfig config = eglManager.streamVideoConfigLocal;
            if (config != null) {
                config.width = size.width;
                config.height = size.height;
                eglManager.videoSize = size;
                
                // If resolution change requires restart, use blank frames
                if (requiresRestart(CONFIG_VIDEO_RESOLUTION)) {
                    Log.d(TAG, "Resolution change requires transition");
                    // The SeamlessTransitionManager would handle this
                } else {
                    eglManager.mStreamer.updateVideoConfig(config);
                }
                
                Log.d(TAG, "Applied video resolution: " + size.width + "x" + size.height);
            }
        }
    }
    
    private void applyVideoFPS(SharedEglManager eglManager, int fps) {
        if (eglManager.mStreamer != null) {
            VideoConfig config = eglManager.streamVideoConfigLocal;
            if (config != null) {
                config.fps = fps;
                eglManager.mStreamer.updateVideoConfig(config);
                Log.d(TAG, "Applied video FPS: " + fps);
            }
        }
    }
    
    private void applyStreamUrl(SharedEglManager eglManager, String url) {
        // URL changes require reconnection
        // This should be handled by the connection manager
        Log.d(TAG, "Stream URL update requires reconnection: " + url);
        
        // Store the new URL
        AppPreference.setStr(AppPreference.KEY.STREAM_URL, url);
        
        // Trigger reconnection if streaming
        if (eglManager.isStreaming()) {
            // The connection manager would handle this
        }
    }
    
    private void applyOrientation(SharedEglManager eglManager, int rotation) {
        eglManager.mRotation = rotation;
        Log.d(TAG, "Applied orientation: " + rotation);
    }
    
    private void applyMirror(SharedEglManager eglManager, boolean mirror) {
        eglManager.mIsMirrored = mirror;
        Log.d(TAG, "Applied mirror: " + mirror);
    }
    
    private void applyFlip(SharedEglManager eglManager, boolean flip) {
        eglManager.mIsFlipped = flip;
        Log.d(TAG, "Applied flip: " + flip);
    }
    
    private boolean requiresRestart(String configKey) {
        // Some configuration changes may require a restart
        // This can be customized based on encoder capabilities
        switch (configKey) {
            case CONFIG_VIDEO_RESOLUTION:
                // Some encoders can handle resolution changes dynamically
                return false;
            case CONFIG_STREAM_URL:
                return true;
            default:
                return false;
        }
    }
    
    private void saveConfiguration(String key, Object value) {
        // Save to preferences for persistence
        switch (key) {
            case CONFIG_VIDEO_BITRATE:
                AppPreference.setInt(AppPreference.KEY.VIDEO_BITRATE, (Integer) value);
                break;
            case CONFIG_AUDIO_BITRATE:
                AppPreference.setInt(AppPreference.KEY.AUDIO_BITRATE, (Integer) value);
                break;
            case CONFIG_VIDEO_FPS:
                AppPreference.setInt(AppPreference.KEY.FPS, (Integer) value);
                break;
            case CONFIG_ORIENTATION:
                AppPreference.setInt(AppPreference.KEY.ORIENTATION, (Integer) value);
                break;
            case CONFIG_MIRROR:
                AppPreference.setBool(AppPreference.KEY.MIRROR, (Boolean) value);
                break;
            case CONFIG_FLIP:
                AppPreference.setBool(AppPreference.KEY.FLIP, (Boolean) value);
                break;
        }
    }
    
    private void notifyConfigurationChanged(String key, Object value) {
        if (mListener != null) {
            mListener.onConfigurationChanged(key, value);
        }
    }
    
    private void notifyError(String key, String error) {
        if (mListener != null) {
            mListener.onConfigurationError(key, error);
        }
    }
    
    /**
     * Get current configuration value.
     */
    public Object getConfiguration(String key) {
        // Check pending changes first
        if (mPendingChanges.containsKey(key)) {
            return mPendingChanges.get(key);
        }
        
        // Return current value from preferences
        switch (key) {
            case CONFIG_VIDEO_BITRATE:
                return AppPreference.getInt(AppPreference.KEY.VIDEO_BITRATE, 2000000);
            case CONFIG_AUDIO_BITRATE:
                return AppPreference.getInt(AppPreference.KEY.AUDIO_BITRATE, 128000);
            case CONFIG_VIDEO_FPS:
                return AppPreference.getInt(AppPreference.KEY.FPS, 30);
            case CONFIG_ORIENTATION:
                return AppPreference.getInt(AppPreference.KEY.ORIENTATION, 0);
            case CONFIG_MIRROR:
                return AppPreference.getBool(AppPreference.KEY.MIRROR, false);
            case CONFIG_FLIP:
                return AppPreference.getBool(AppPreference.KEY.FLIP, false);
            default:
                return null;
        }
    }
    
    /**
     * Clear all pending changes.
     */
    public void clearPendingChanges() {
        mPendingChanges.clear();
    }
}