package com.checkmate.android.service;

import android.util.Log;
import com.checkmate.android.service.SharedEGL.SharedEglManager;
import com.wmspanel.libstream.AudioConfig;
import com.wmspanel.libstream.VideoConfig;
import com.wmspanel.libstream.Streamer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages dynamic configuration updates without interrupting active streams
 */
public class DynamicConfigManager {
    private static final String TAG = "DynamicConfigManager";
    private static final int CONFIG_TRANSITION_FRAMES = 3;
    private static final int FRAME_DELAY_MS = 33;
    
    private final AtomicBoolean isUpdating = new AtomicBoolean(false);
    
    public interface ConfigUpdateCallback {
        void onUpdateStart();
        void onUpdateProgress(String status);
        void onUpdateComplete();
        void onUpdateError(String error);
    }
    
    /**
     * Update video configuration without stopping stream
     */
    public void updateVideoConfig(VideoConfig newConfig, ConfigUpdateCallback callback) {
        if (isUpdating.get()) {
            callback.onUpdateError("Configuration update already in progress");
            return;
        }
        
        isUpdating.set(true);
        callback.onUpdateStart();
        
        new Thread(() -> {
            try {
                SharedEglManager eglManager = SharedEglManager.getInstance();
                
                if (!eglManager.isStreaming() && !eglManager.isRecording()) {
                    // Not active, can update directly
                    updateVideoConfigDirect(newConfig);
                    callback.onUpdateComplete();
                } else {
                    // Active stream, perform smooth update
                    updateVideoConfigSmooth(newConfig, callback);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to update video config", e);
                callback.onUpdateError(e.getMessage());
            } finally {
                isUpdating.set(false);
            }
        }).start();
    }
    
    /**
     * Update audio configuration without stopping stream
     */
    public void updateAudioConfig(AudioConfig newConfig, ConfigUpdateCallback callback) {
        if (isUpdating.get()) {
            callback.onUpdateError("Configuration update already in progress");
            return;
        }
        
        isUpdating.set(true);
        callback.onUpdateStart();
        
        new Thread(() -> {
            try {
                SharedEglManager eglManager = SharedEglManager.getInstance();
                
                if (!eglManager.isStreaming() && !eglManager.isRecording()) {
                    // Not active, can update directly
                    updateAudioConfigDirect(newConfig);
                    callback.onUpdateComplete();
                } else {
                    // Active stream, perform smooth update
                    updateAudioConfigSmooth(newConfig, callback);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to update audio config", e);
                callback.onUpdateError(e.getMessage());
            } finally {
                isUpdating.set(false);
            }
        }).start();
    }
    
    /**
     * Update both video and audio configurations
     */
    public void updateConfigs(VideoConfig videoConfig, AudioConfig audioConfig, ConfigUpdateCallback callback) {
        if (isUpdating.get()) {
            callback.onUpdateError("Configuration update already in progress");
            return;
        }
        
        isUpdating.set(true);
        callback.onUpdateStart();
        
        new Thread(() -> {
            try {
                SharedEglManager eglManager = SharedEglManager.getInstance();
                
                // Draw transition frames
                callback.onUpdateProgress("Preparing configuration update...");
                for (int i = 0; i < CONFIG_TRANSITION_FRAMES; i++) {
                    eglManager.drawBlankFrameWithOverlay("Updating configuration... " + (i + 1) + "/" + CONFIG_TRANSITION_FRAMES);
                    Thread.sleep(FRAME_DELAY_MS);
                }
                
                // Update configurations
                if (videoConfig != null) {
                    callback.onUpdateProgress("Updating video configuration...");
                    if (eglManager.isStreaming() || eglManager.isRecording()) {
                        updateVideoConfigSmooth(videoConfig, null);
                    } else {
                        updateVideoConfigDirect(videoConfig);
                    }
                }
                
                if (audioConfig != null) {
                    callback.onUpdateProgress("Updating audio configuration...");
                    if (eglManager.isStreaming() || eglManager.isRecording()) {
                        updateAudioConfigSmooth(audioConfig, null);
                    } else {
                        updateAudioConfigDirect(audioConfig);
                    }
                }
                
                // Stabilization period
                callback.onUpdateProgress("Stabilizing...");
                Thread.sleep(100);
                
                callback.onUpdateComplete();
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to update configurations", e);
                callback.onUpdateError(e.getMessage());
            } finally {
                isUpdating.set(false);
            }
        }).start();
    }
    
    private void updateVideoConfigDirect(VideoConfig config) {
        Log.d(TAG, "Direct video config update");
        // Direct update implementation
        SharedEglManager.getInstance().updateStreamingConfigs(config, null);
    }
    
    private void updateVideoConfigSmooth(VideoConfig config, ConfigUpdateCallback callback) throws InterruptedException {
        Log.d(TAG, "Smooth video config update");
        SharedEglManager eglManager = SharedEglManager.getInstance();
        
        // Draw transition frames before update
        for (int i = 0; i < CONFIG_TRANSITION_FRAMES; i++) {
            eglManager.drawBlankFrameWithOverlay("Updating video settings...");
            Thread.sleep(FRAME_DELAY_MS);
        }
        
        // Perform update
        eglManager.updateStreamingConfigs(config, null);
        
        // Draw transition frames after update
        for (int i = 0; i < CONFIG_TRANSITION_FRAMES; i++) {
            eglManager.drawBlankFrameWithOverlay("Video settings updated");
            Thread.sleep(FRAME_DELAY_MS);
        }
        
        if (callback != null) {
            callback.onUpdateProgress("Video configuration updated");
        }
    }
    
    private void updateAudioConfigDirect(AudioConfig config) {
        Log.d(TAG, "Direct audio config update");
        // Direct update implementation
        SharedEglManager.getInstance().updateStreamingConfigs(null, config);
    }
    
    private void updateAudioConfigSmooth(AudioConfig config, ConfigUpdateCallback callback) throws InterruptedException {
        Log.d(TAG, "Smooth audio config update");
        SharedEglManager eglManager = SharedEglManager.getInstance();
        
        // Audio updates are typically less disruptive
        eglManager.updateStreamingConfigs(null, config);
        
        if (callback != null) {
            callback.onUpdateProgress("Audio configuration updated");
        }
    }
    
    /**
     * Update resolution without stopping stream
     */
    public void updateResolution(int width, int height, ConfigUpdateCallback callback) {
        VideoConfig newConfig = new VideoConfig();
        newConfig.videoSize = new Streamer.Size(width, height);
        updateVideoConfig(newConfig, callback);
    }
    
    /**
     * Update bitrates without stopping stream
     */
    public void updateBitrates(int videoBitrate, int audioBitrate, ConfigUpdateCallback callback) {
        VideoConfig videoConfig = null;
        AudioConfig audioConfig = null;
        
        if (videoBitrate > 0) {
            videoConfig = new VideoConfig();
            videoConfig.bitRate = videoBitrate;
        }
        
        if (audioBitrate > 0) {
            audioConfig = new AudioConfig();
            audioConfig.bitRate = audioBitrate;
        }
        
        updateConfigs(videoConfig, audioConfig, callback);
    }
    
    /**
     * Update frame rate without stopping stream
     */
    public void updateFrameRate(float fps, ConfigUpdateCallback callback) {
        VideoConfig newConfig = new VideoConfig();
        newConfig.fps = fps;
        updateVideoConfig(newConfig, callback);
    }
}