package com.checkmate.android.service.SharedEGL;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.checkmate.android.AppPreference;
import com.wmspanel.libstream.StreamerSurface;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages streaming and recording state across service transitions.
 * Ensures streams remain active during configuration changes.
 */
public class StreamStateManager {
    private static final String TAG = "StreamStateManager";
    
    private static volatile StreamStateManager sInstance;
    private final Object mLock = new Object();
    
    // Stream states
    private final AtomicBoolean mIsStreaming = new AtomicBoolean(false);
    private final AtomicBoolean mIsRecording = new AtomicBoolean(false);
    private final AtomicBoolean mIsTransitioning = new AtomicBoolean(false);
    
    // Active stream references
    private final AtomicReference<StreamerSurface> mActiveStreamer = new AtomicReference<>();
    private final AtomicReference<StreamerSurface> mActiveRecorder = new AtomicReference<>();
    
    // Transition callbacks
    private final ConcurrentHashMap<String, TransitionCallback> mTransitionCallbacks = new ConcurrentHashMap<>();
    
    // Handler for main thread callbacks
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    
    public interface TransitionCallback {
        void onTransitionStart();
        void onTransitionComplete(boolean success);
        void onTransitionError(String error);
    }
    
    private StreamStateManager() {
        // Private constructor for singleton
    }
    
    public static StreamStateManager getInstance() {
        if (sInstance == null) {
            synchronized (StreamStateManager.class) {
                if (sInstance == null) {
                    sInstance = new StreamStateManager();
                }
            }
        }
        return sInstance;
    }
    
    /**
     * Set streaming state and store active streamer reference.
     */
    public void setStreaming(boolean streaming, StreamerSurface streamer) {
        synchronized (mLock) {
            mIsStreaming.set(streaming);
            mActiveStreamer.set(streaming ? streamer : null);
            
            // Persist state
            AppPreference.setBool(AppPreference.KEY.STREAM_STARTED, streaming);
            
            Log.d(TAG, "Streaming state changed: " + streaming);
        }
    }
    
    /**
     * Set recording state and store active recorder reference.
     */
    public void setRecording(boolean recording, StreamerSurface recorder) {
        synchronized (mLock) {
            mIsRecording.set(recording);
            mActiveRecorder.set(recording ? recorder : null);
            
            // Persist state
            AppPreference.setBool(AppPreference.KEY.RECORD_STARTED, recording);
            
            Log.d(TAG, "Recording state changed: " + recording);
        }
    }
    
    /**
     * Check if streaming is active.
     */
    public boolean isStreaming() {
        return mIsStreaming.get();
    }
    
    /**
     * Check if recording is active.
     */
    public boolean isRecording() {
        return mIsRecording.get();
    }
    
    /**
     * Check if any stream is active.
     */
    public boolean hasActiveStreams() {
        return mIsStreaming.get() || mIsRecording.get();
    }
    
    /**
     * Check if a transition is in progress.
     */
    public boolean isTransitioning() {
        return mIsTransitioning.get();
    }
    
    /**
     * Begin a service transition while maintaining active streams.
     */
    public void beginTransition(String transitionId, TransitionCallback callback) {
        synchronized (mLock) {
            if (mIsTransitioning.get()) {
                Log.w(TAG, "Transition already in progress");
                if (callback != null) {
                    mMainHandler.post(() -> callback.onTransitionError("Transition already in progress"));
                }
                return;
            }
            
            mIsTransitioning.set(true);
            
            if (callback != null) {
                mTransitionCallbacks.put(transitionId, callback);
                mMainHandler.post(callback::onTransitionStart);
            }
            
            Log.d(TAG, "Beginning transition: " + transitionId);
        }
    }
    
    /**
     * Complete a service transition.
     */
    public void completeTransition(String transitionId, boolean success) {
        synchronized (mLock) {
            mIsTransitioning.set(false);
            
            TransitionCallback callback = mTransitionCallbacks.remove(transitionId);
            if (callback != null) {
                mMainHandler.post(() -> callback.onTransitionComplete(success));
            }
            
            Log.d(TAG, "Completed transition: " + transitionId + ", success: " + success);
        }
    }
    
    /**
     * Get the active streamer instance.
     */
    public StreamerSurface getActiveStreamer() {
        return mActiveStreamer.get();
    }
    
    /**
     * Get the active recorder instance.
     */
    public StreamerSurface getActiveRecorder() {
        return mActiveRecorder.get();
    }
    
    /**
     * Preserve stream configuration during transition.
     */
    public StreamConfiguration preserveStreamConfiguration() {
        StreamConfiguration config = new StreamConfiguration();
        
        config.isStreaming = mIsStreaming.get();
        config.isRecording = mIsRecording.get();
        config.streamUrl = AppPreference.getStr(AppPreference.KEY.STREAM_URL, "");
        config.streamKey = AppPreference.getStr(AppPreference.KEY.STREAM_KEY, "");
        config.recordPath = AppPreference.getStr(AppPreference.KEY.RECORD_PATH, "");
        config.videoBitrate = AppPreference.getInt(AppPreference.KEY.VIDEO_BITRATE, 2000000);
        config.audioBitrate = AppPreference.getInt(AppPreference.KEY.AUDIO_BITRATE, 128000);
        config.fps = AppPreference.getInt(AppPreference.KEY.FPS, 30);
        config.resolution = AppPreference.getStr(AppPreference.KEY.RESOLUTION, "1280x720");
        
        Log.d(TAG, "Preserved stream configuration");
        return config;
    }
    
    /**
     * Restore stream configuration after transition.
     */
    public void restoreStreamConfiguration(StreamConfiguration config) {
        if (config == null) {
            Log.w(TAG, "No configuration to restore");
            return;
        }
        
        // Configuration is already persisted in preferences
        // This method is for future extension if needed
        
        Log.d(TAG, "Restored stream configuration");
    }
    
    /**
     * Handle stream error during transition.
     */
    public void handleStreamError(String error) {
        Log.e(TAG, "Stream error: " + error);
        
        // Notify all active transition callbacks
        for (TransitionCallback callback : mTransitionCallbacks.values()) {
            mMainHandler.post(() -> callback.onTransitionError(error));
        }
        
        // Clear transition state
        mIsTransitioning.set(false);
        mTransitionCallbacks.clear();
    }
    
    /**
     * Reset all states (use with caution).
     */
    public void reset() {
        synchronized (mLock) {
            mIsStreaming.set(false);
            mIsRecording.set(false);
            mIsTransitioning.set(false);
            mActiveStreamer.set(null);
            mActiveRecorder.set(null);
            mTransitionCallbacks.clear();
            
            Log.d(TAG, "Stream state manager reset");
        }
    }
    
    /**
     * Configuration holder class.
     */
    public static class StreamConfiguration {
        public boolean isStreaming;
        public boolean isRecording;
        public String streamUrl;
        public String streamKey;
        public String recordPath;
        public int videoBitrate;
        public int audioBitrate;
        public int fps;
        public String resolution;
        
        @Override
        public String toString() {
            return "StreamConfiguration{" +
                    "isStreaming=" + isStreaming +
                    ", isRecording=" + isRecording +
                    ", resolution='" + resolution + '\'' +
                    ", fps=" + fps +
                    ", videoBitrate=" + videoBitrate +
                    ", audioBitrate=" + audioBitrate +
                    '}';
        }
    }
}