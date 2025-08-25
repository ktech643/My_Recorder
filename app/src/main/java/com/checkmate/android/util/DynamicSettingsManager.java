package com.checkmate.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.checkmate.android.AppPreference;
import com.checkmate.android.service.SharedEGL.SharedEglManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages dynamic settings updates without interrupting active streams or recordings
 */
public class DynamicSettingsManager implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "DynamicSettingsManager";
    private static volatile DynamicSettingsManager instance;
    private final Context context;
    private final Handler mainHandler;
    private final Map<String, SettingChangeListener> listeners = new ConcurrentHashMap<>();
    private final Set<String> dynamicSettings = new HashSet<>();
    private final Set<String> criticalSettings = new HashSet<>();
    
    // Settings that can be changed dynamically without stream interruption
    static {
        
    }
    
    public interface SettingChangeListener {
        void onSettingChanged(String key, Object newValue);
    }
    
    private DynamicSettingsManager(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        initializeDynamicSettings();
        initializeCriticalSettings();
    }
    
    public static DynamicSettingsManager getInstance(Context context) {
        if (instance == null) {
            synchronized (DynamicSettingsManager.class) {
                if (instance == null) {
                    instance = new DynamicSettingsManager(context);
                }
            }
        }
        return instance;
    }
    
    private void initializeDynamicSettings() {
        // Settings that can be changed without restarting stream
        dynamicSettings.add(AppPreference.KEY.TIMESTAMP);
        dynamicSettings.add(AppPreference.KEY.SPLIT_TIME);
        dynamicSettings.add(AppPreference.KEY.FILE_ENCRYPTION);
        dynamicSettings.add(AppPreference.KEY.ENCRYPTION_KEY);
        dynamicSettings.add(AppPreference.KEY.RECORD_BROADCAST);
        dynamicSettings.add(AppPreference.KEY.STORAGE_LOCATION);
        dynamicSettings.add(AppPreference.KEY.STREAMING_RADIO_MODE);
        
        // Audio settings that can be dynamically adjusted
        dynamicSettings.add(AppPreference.KEY.AUDIO_OPTION_AUDIO_SETTING);
        dynamicSettings.add(AppPreference.KEY.AUDIO_OPTION_SAMPLE_RATE);
        dynamicSettings.add(AppPreference.KEY.AUDIO_OPTION_BITRATE);
        dynamicSettings.add(AppPreference.KEY.STREAMING_AUDIO_BITRATE);
        dynamicSettings.add(AppPreference.KEY.USB_AUDIO_BITRATE);
        
        // Some video settings can be dynamic with proper handling
        dynamicSettings.add(AppPreference.KEY.VIDEO_BITRATE);
        
        
    }
    
    private void initializeCriticalSettings() {
        // Settings that require stream restart
        criticalSettings.add(AppPreference.KEY.VIDEO_RESOLUTION);
        criticalSettings.add(AppPreference.KEY.STREAMING_RESOLUTION);
        criticalSettings.add(AppPreference.KEY.VIDEO_FRAME);
        criticalSettings.add(AppPreference.KEY.STREAMING_FRAME);
        criticalSettings.add(AppPreference.KEY.SELECTED_POSITION);
        criticalSettings.add(AppPreference.KEY.USB_MIN_FPS);
        criticalSettings.add(AppPreference.KEY.USB_MAX_FPS);
        criticalSettings.add(AppPreference.KEY.CODEC_SRC);
    }
    
    public void registerListener(String key, SettingChangeListener listener) {
        listeners.put(key, listener);
    }
    
    public void unregisterListener(String key) {
        listeners.remove(key);
    }
    
    public boolean isDynamicSetting(String key) {
        return dynamicSettings.contains(key);
    }
    
    public boolean isCriticalSetting(String key) {
        return criticalSettings.contains(key);
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == null) return;
        
        try {
            Log.d(TAG, "Setting changed: " + key);
            
            // Get the new value
            Object newValue = getPreferenceValue(sharedPreferences, key);
            
            // Check if this is a dynamic setting
            if (isDynamicSetting(key)) {
                handleDynamicSettingChange(key, newValue);
            } else if (isCriticalSetting(key)) {
                handleCriticalSettingChange(key, newValue);
            }
            
            // Notify specific listeners
            SettingChangeListener listener = listeners.get(key);
            if (listener != null) {
                mainHandler.post(() -> listener.onSettingChanged(key, newValue));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling setting change", e);
            if (CrashLogger.getInstance() != null) {
                CrashLogger.getInstance().logError(TAG, "onSharedPreferenceChanged", e);
            }
        }
    }
    
    private Object getPreferenceValue(SharedPreferences prefs, String key) {
        Map<String, ?> all = prefs.getAll();
        return all.get(key);
    }
    
    private void handleDynamicSettingChange(String key, Object newValue) {
        Log.d(TAG, "Applying dynamic setting: " + key + " = " + newValue);
        
        // Handle specific dynamic settings
        SharedEglManager eglManager = SharedEglManager.getInstance();
        if (eglManager == null) return;
        
        switch (key) {
            case AppPreference.KEY.TIMESTAMP:
                // Timestamp can be toggled immediately
                break;
                
            case AppPreference.KEY.VIDEO_BITRATE:
                if (newValue instanceof Integer) {
                    updateBitrateOnTheFly((Integer) newValue);
                }
                break;
                
            case AppPreference.KEY.SPLIT_TIME:
                // Update split time for next file
                break;
                
            case AppPreference.KEY.FILE_ENCRYPTION:
            case AppPreference.KEY.ENCRYPTION_KEY:
                // Apply to next recording file
                break;
                
            case AppPreference.KEY.AUDIO_OPTION_BITRATE:
            case AppPreference.KEY.STREAMING_AUDIO_BITRATE:
            case AppPreference.KEY.USB_AUDIO_BITRATE:
            case AppPreference.KEY.AUDIO_OPTION_SAMPLE_RATE:
                if (newValue instanceof Integer) {
                    updateAudioSettingsOnTheFly(key, (Integer) newValue);
                }
                break;
        }
    }
    
    private void handleCriticalSettingChange(String key, Object newValue) {
        Log.w(TAG, "Critical setting changed: " + key + ". Stream/recording restart may be required.");
        
        // Notify user that this setting requires restart
        SharedEglManager eglManager = SharedEglManager.getInstance();
        if (eglManager != null && (eglManager.isStreaming() || eglManager.isRecording())) {
            // Show notification or dialog to user
            notifyUserAboutCriticalChange(key);
        }
    }
    
    private void updateBitrateOnTheFly(int newBitrate) {
        try {
            SharedEglManager eglManager = SharedEglManager.getInstance();
            if (eglManager != null && eglManager.isStreaming()) {
                // Update encoder bitrate dynamically
                eglManager.updateEncoderBitrate(newBitrate);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to update bitrate", e);
        }
    }
    
    private void updateAudioSettingsOnTheFly(String key, int newValue) {
        try {
            SharedEglManager eglManager = SharedEglManager.getInstance();
            if (eglManager != null && eglManager.isStreaming()) {
                // Update audio settings dynamically
                if (key.contains("BITRATE")) {
                    eglManager.updateAudioBitrate(newValue);
                } else if (key.contains("SAMPLE_RATE")) {
                    // Sample rate usually requires restart
                    notifyUserAboutCriticalChange(key);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to update audio settings", e);
        }
    }
    
    private void notifyUserAboutCriticalChange(String key) {
        // This would typically show a notification or dialog
        Log.i(TAG, "Critical setting " + key + " changed. Please restart stream/recording to apply.");
    }
    
    /**
     * Apply settings that can be changed during active streaming/recording
     */
    public void applyDynamicSettings() {
        SharedEglManager eglManager = SharedEglManager.getInstance();
        if (eglManager == null) return;
        
        // Apply all current dynamic settings
        for (String key : dynamicSettings) {
            // Get value based on key type
            Object value = null;
            if (key.equals(AppPreference.KEY.TIMESTAMP) || 
                key.equals(AppPreference.KEY.FILE_ENCRYPTION) ||
                key.equals(AppPreference.KEY.RECORD_BROADCAST)) {
                value = AppPreference.getBool(key, false);
            } else if (key.contains("BITRATE") || key.contains("TIME")) {
                value = AppPreference.getInt(key, 0);
            } else {
                value = AppPreference.getStr(key, "");
            }
            
            if (value != null) {
                handleDynamicSettingChange(key, value);
            }
        }
    }
    
    /**
     * Check if a setting change requires stream restart
     */
    public boolean requiresStreamRestart(String key) {
        return criticalSettings.contains(key);
    }
    
    /**
     * Get pending critical changes that haven't been applied
     */
    public Set<String> getPendingCriticalChanges() {
        Set<String> pending = new HashSet<>();
        // Track which critical settings have changed since stream started
        // This would require storing the values at stream start
        return pending;
    }
}