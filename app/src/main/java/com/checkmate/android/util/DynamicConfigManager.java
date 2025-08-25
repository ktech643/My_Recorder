package com.checkmate.android.util;

import android.util.Log;

import com.checkmate.android.AppPreference;
import com.checkmate.android.service.SharedEGL.SharedEglManager;
import com.wmspanel.libstream.AudioConfig;
import com.wmspanel.libstream.VideoConfig;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manager for dynamically updating streaming and recording configurations
 * without interrupting active streams
 */
public class DynamicConfigManager {
    private static final String TAG = "DynamicConfigManager";
    private static DynamicConfigManager sInstance;
    
    private final AtomicBoolean mIsUpdating = new AtomicBoolean(false);
    
    private DynamicConfigManager() {}
    
    public static synchronized DynamicConfigManager getInstance() {
        if (sInstance == null) {
            sInstance = new DynamicConfigManager();
        }
        return sInstance;
    }
    
    /**
     * Update video resolution dynamically
     * @param width New width
     * @param height New height
     * @param callback Update callback
     */
    public void updateVideoResolution(int width, int height, ConfigUpdateCallback callback) {
        if (mIsUpdating.getAndSet(true)) {
            Log.w(TAG, "Configuration update already in progress");
            if (callback != null) {
                callback.onUpdateComplete(false, "Update already in progress");
            }
            return;
        }
        
        new Thread(() -> {
            try {
                SharedEglManager eglManager = SharedEglManager.getInstance();
                
                // Check if streaming or recording is active
                boolean isStreaming = eglManager.isStreaming();
                boolean isRecording = eglManager.isRecording();
                
                if (!isStreaming && !isRecording) {
                    // No active streams, can update directly
                    AppPreference.getInstance().setVideoResolution(width, height);
                    eglManager.setSurfaceSize(width, height);
                    notifySuccess(callback, "Resolution updated to " + width + "x" + height);
                } else {
                    // Active stream/recording - use dynamic update
                    Log.d(TAG, "Updating resolution dynamically: " + width + "x" + height);
                    
                    // Update preferences
                    AppPreference.getInstance().setVideoResolution(width, height);
                    
                    // Apply changes dynamically
                    eglManager.updateConfiguration();
                    
                    notifySuccess(callback, "Resolution updated dynamically");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to update resolution", e);
                notifyError(callback, e.getMessage());
            } finally {
                mIsUpdating.set(false);
            }
        }).start();
    }
    
    /**
     * Update video bitrate dynamically
     * @param bitrate New bitrate in bps
     * @param callback Update callback
     */
    public void updateVideoBitrate(int bitrate, ConfigUpdateCallback callback) {
        if (mIsUpdating.getAndSet(true)) {
            Log.w(TAG, "Configuration update already in progress");
            if (callback != null) {
                callback.onUpdateComplete(false, "Update already in progress");
            }
            return;
        }
        
        new Thread(() -> {
            try {
                SharedEglManager eglManager = SharedEglManager.getInstance();
                
                // Update preference
                AppPreference.getInstance().setBitrate(bitrate);
                
                if (eglManager.isStreaming() || eglManager.isRecording()) {
                    // Apply dynamically
                    eglManager.updateConfiguration();
                    notifySuccess(callback, "Bitrate updated to " + formatBitrate(bitrate));
                } else {
                    notifySuccess(callback, "Bitrate set to " + formatBitrate(bitrate));
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to update bitrate", e);
                notifyError(callback, e.getMessage());
            } finally {
                mIsUpdating.set(false);
            }
        }).start();
    }
    
    /**
     * Update frame rate dynamically
     * @param fps New frame rate
     * @param callback Update callback
     */
    public void updateFrameRate(int fps, ConfigUpdateCallback callback) {
        if (mIsUpdating.getAndSet(true)) {
            Log.w(TAG, "Configuration update already in progress");
            if (callback != null) {
                callback.onUpdateComplete(false, "Update already in progress");
            }
            return;
        }
        
        new Thread(() -> {
            try {
                SharedEglManager eglManager = SharedEglManager.getInstance();
                
                // Update preference
                AppPreference.getInstance().setFps(fps);
                
                if (eglManager.isStreaming() || eglManager.isRecording()) {
                    // Apply dynamically
                    eglManager.updateConfiguration();
                    notifySuccess(callback, "Frame rate updated to " + fps + " FPS");
                } else {
                    notifySuccess(callback, "Frame rate set to " + fps + " FPS");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to update frame rate", e);
                notifyError(callback, e.getMessage());
            } finally {
                mIsUpdating.set(false);
            }
        }).start();
    }
    
    /**
     * Update audio settings dynamically
     * @param sampleRate Audio sample rate
     * @param channels Number of channels (1 or 2)
     * @param bitrate Audio bitrate
     * @param callback Update callback
     */
    public void updateAudioSettings(int sampleRate, int channels, int bitrate, 
                                   ConfigUpdateCallback callback) {
        if (mIsUpdating.getAndSet(true)) {
            Log.w(TAG, "Configuration update already in progress");
            if (callback != null) {
                callback.onUpdateComplete(false, "Update already in progress");
            }
            return;
        }
        
        new Thread(() -> {
            try {
                SharedEglManager eglManager = SharedEglManager.getInstance();
                
                // Update preferences
                AppPreference.getInstance().setAudioSamplerate(sampleRate);
                AppPreference.getInstance().setAudioChannels(channels);
                AppPreference.getInstance().setAudioBitrate(bitrate);
                
                if (eglManager.isStreaming() || eglManager.isRecording()) {
                    // Apply dynamically
                    eglManager.updateConfiguration();
                    notifySuccess(callback, "Audio settings updated");
                } else {
                    notifySuccess(callback, "Audio settings configured");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to update audio settings", e);
                notifyError(callback, e.getMessage());
            } finally {
                mIsUpdating.set(false);
            }
        }).start();
    }
    
    /**
     * Apply multiple configuration changes at once
     * @param updates Configuration updates to apply
     * @param callback Update callback
     */
    public void batchUpdate(ConfigUpdate updates, ConfigUpdateCallback callback) {
        if (mIsUpdating.getAndSet(true)) {
            Log.w(TAG, "Configuration update already in progress");
            if (callback != null) {
                callback.onUpdateComplete(false, "Update already in progress");
            }
            return;
        }
        
        new Thread(() -> {
            try {
                SharedEglManager eglManager = SharedEglManager.getInstance();
                boolean needsUpdate = false;
                
                // Apply all preference updates
                if (updates.width != null && updates.height != null) {
                    AppPreference.getInstance().setVideoResolution(updates.width, updates.height);
                    eglManager.setSurfaceSize(updates.width, updates.height);
                    needsUpdate = true;
                }
                
                if (updates.bitrate != null) {
                    AppPreference.getInstance().setBitrate(updates.bitrate);
                    needsUpdate = true;
                }
                
                if (updates.fps != null) {
                    AppPreference.getInstance().setFps(updates.fps);
                    needsUpdate = true;
                }
                
                if (updates.audioSampleRate != null) {
                    AppPreference.getInstance().setAudioSamplerate(updates.audioSampleRate);
                    needsUpdate = true;
                }
                
                if (updates.audioChannels != null) {
                    AppPreference.getInstance().setAudioChannels(updates.audioChannels);
                    needsUpdate = true;
                }
                
                if (updates.audioBitrate != null) {
                    AppPreference.getInstance().setAudioBitrate(updates.audioBitrate);
                    needsUpdate = true;
                }
                
                // Apply changes if needed
                if (needsUpdate && (eglManager.isStreaming() || eglManager.isRecording())) {
                    eglManager.updateConfiguration();
                    notifySuccess(callback, "Configuration updated successfully");
                } else if (needsUpdate) {
                    notifySuccess(callback, "Configuration saved");
                } else {
                    notifySuccess(callback, "No changes to apply");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to apply batch update", e);
                notifyError(callback, e.getMessage());
            } finally {
                mIsUpdating.set(false);
            }
        }).start();
    }
    
    private void notifySuccess(ConfigUpdateCallback callback, String message) {
        if (callback != null) {
            callback.onUpdateComplete(true, message);
        }
        Log.d(TAG, "Configuration update successful: " + message);
    }
    
    private void notifyError(ConfigUpdateCallback callback, String message) {
        if (callback != null) {
            callback.onUpdateComplete(false, message);
        }
        Log.e(TAG, "Configuration update failed: " + message);
    }
    
    private String formatBitrate(int bitrate) {
        if (bitrate >= 1000000) {
            return String.format("%.1f Mbps", bitrate / 1000000.0);
        } else if (bitrate >= 1000) {
            return String.format("%.0f Kbps", bitrate / 1000.0);
        } else {
            return bitrate + " bps";
        }
    }
    
    /**
     * Configuration update callback
     */
    public interface ConfigUpdateCallback {
        void onUpdateComplete(boolean success, String message);
    }
    
    /**
     * Configuration update bundle
     */
    public static class ConfigUpdate {
        public Integer width;
        public Integer height;
        public Integer bitrate;
        public Integer fps;
        public Integer audioSampleRate;
        public Integer audioChannels;
        public Integer audioBitrate;
        
        public ConfigUpdate() {}
        
        public ConfigUpdate setVideoResolution(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }
        
        public ConfigUpdate setVideoBitrate(int bitrate) {
            this.bitrate = bitrate;
            return this;
        }
        
        public ConfigUpdate setFrameRate(int fps) {
            this.fps = fps;
            return this;
        }
        
        public ConfigUpdate setAudioSettings(int sampleRate, int channels, int bitrate) {
            this.audioSampleRate = sampleRate;
            this.audioChannels = channels;
            this.audioBitrate = bitrate;
            return this;
        }
    }
}