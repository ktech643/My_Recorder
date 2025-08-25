package com.checkmate.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.checkmate.android.AppPreference;
import com.checkmate.android.service.SharedEGL.SharedEglManager;

/**
 * Helper class to integrate dynamic settings throughout the app
 */
public class DynamicSettingsIntegration {
    private static final String TAG = "DynamicSettingsIntegration";
    
    /**
     * Initialize dynamic settings system in MyApp
     */
    public static void initialize(Context context) {
        try {
            // Get the dynamic settings manager instance
            DynamicSettingsManager manager = DynamicSettingsManager.getInstance(context);
            
            // Register as a preference change listener
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.registerOnSharedPreferenceChangeListener(manager);
            
            Log.d(TAG, "Dynamic settings system initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize dynamic settings", e);
        }
    }
    
    /**
     * Handle setting change without restart
     */
    public static boolean handleSettingChange(String key, Object value) {
        DynamicSettingsManager manager = DynamicSettingsManager.getInstance(null);
        if (manager == null) return false;
        
        SharedEglManager eglManager = SharedEglManager.getInstance();
        boolean isActiveStream = eglManager != null && (eglManager.isStreaming() || eglManager.isRecording());
        
        if (manager.isDynamicSetting(key)) {
            // Apply immediately
            applyDynamicSetting(key, value);
            return true;
        } else if (manager.isCriticalSetting(key) && isActiveStream) {
            // Defer until stream stops
            Log.w(TAG, "Critical setting " + key + " will be applied after stream stops");
            return false;
        }
        
        return true;
    }
    
    private static void applyDynamicSetting(String key, Object value) {
        SharedEglManager eglManager = SharedEglManager.getInstance();
        if (eglManager == null) return;
        
        switch (key) {
            case AppPreference.KEY.VIDEO_BITRATE:
                if (value instanceof Integer) {
                    eglManager.updateEncoderBitrate((Integer) value);
                }
                break;
                
            case AppPreference.KEY.TIMESTAMP:
                // Timestamp is already checked dynamically in drawFrame
                Log.d(TAG, "Timestamp setting updated");
                break;
                
            case AppPreference.KEY.SPLIT_TIME:
                if (value instanceof Integer) {
                    eglManager.updateFileSplitTime((Integer) value);
                }
                break;
                
            case AppPreference.KEY.AUDIO_OPTION_BITRATE:
                if (value instanceof Integer) {
                    eglManager.updateAudioBitrate((Integer) value);
                }
                break;
                
            default:
                Log.d(TAG, "Dynamic setting updated: " + key);
                break;
        }
    }
    
    /**
     * Wrap preference setters to handle dynamic updates
     */
    public static class DynamicPreference {
        
        public static void setBool(String key, boolean value) {
            AppPreference.setBool(key, value);
            handleSettingChange(key, value);
        }
        
        public static void setInt(String key, int value) {
            AppPreference.setInt(key, value);
            handleSettingChange(key, value);
        }
        
        public static void setStr(String key, String value) {
            AppPreference.setStr(key, value);
            handleSettingChange(key, value);
        }
        
        public static void setFloat(String key, float value) {
            // AppPreference.setFloat(key, value); // Method not available
            handleSettingChange(key, value);
        }
    }
}