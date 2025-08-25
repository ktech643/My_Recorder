package com.checkmate.android.ui.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.checkmate.android.AppPreference;
import com.checkmate.android.R;
import com.checkmate.android.service.SharedEGL.SharedEglManager;
import com.checkmate.android.util.CrashLogger;
import com.checkmate.android.util.DynamicSettingsManager;

/**
 * Extension of SettingsFragment to handle dynamic settings updates
 */
public class DynamicSettingsFragment extends PreferenceFragment implements 
        SharedPreferences.OnSharedPreferenceChangeListener {
    
    private static final String TAG = "DynamicSettingsFragment";
    private DynamicSettingsManager dynamicSettingsManager;
    private SharedPreferences sharedPreferences;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize dynamic settings manager
        dynamicSettingsManager = DynamicSettingsManager.getInstance(getActivity());
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        
        // Set up preference change listeners for specific settings
        setupDynamicSettingsListeners();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Register preference change listener
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(dynamicSettingsManager);
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Unregister preference change listener
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(dynamicSettingsManager);
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        try {
            // Check if this is a dynamic or critical setting
            if (dynamicSettingsManager.isDynamicSetting(key)) {
                handleDynamicSettingChange(key);
            } else if (dynamicSettingsManager.isCriticalSetting(key)) {
                handleCriticalSettingChange(key);
            }
            
            // Update preference summary if needed
            updatePreferenceSummary(key);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling preference change", e);
            if (CrashLogger.getInstance() != null) {
                CrashLogger.getInstance().logError(TAG, "onSharedPreferenceChanged", e);
            }
        }
    }
    
    private void setupDynamicSettingsListeners() {
        // Set up click listeners for settings that need special handling
        
        // Timestamp setting
        Preference timestampPref = findPreference(AppPreference.KEY.TIMESTAMP);
        if (timestampPref != null) {
            timestampPref.setOnPreferenceChangeListener((preference, newValue) -> {
                if (SharedEglManager.getInstance() != null && 
                    SharedEglManager.getInstance().isStreaming()) {
                    showToast("Timestamp overlay will be updated immediately");
                }
                return true;
            });
        }
        
        // Bitrate setting
        Preference bitratePref = findPreference(AppPreference.KEY.VIDEO_BITRATE);
        if (bitratePref != null) {
            bitratePref.setOnPreferenceChangeListener((preference, newValue) -> {
                if (SharedEglManager.getInstance() != null && 
                    SharedEglManager.getInstance().isStreaming()) {
                    showToast("Bitrate will be adjusted dynamically");
                }
                return true;
            });
        }
        
        // Video size setting (critical)
        Preference videoSizePref = findPreference(AppPreference.KEY.VIDEO_SIZE);
        if (videoSizePref != null) {
            videoSizePref.setOnPreferenceChangeListener((preference, newValue) -> {
                if (SharedEglManager.getInstance() != null && 
                    (SharedEglManager.getInstance().isStreaming() || 
                     SharedEglManager.getInstance().isRecording())) {
                    showWarningDialog("Video size change requires stream restart");
                }
                return true;
            });
        }
    }
    
    private void handleDynamicSettingChange(String key) {
        Log.d(TAG, "Dynamic setting changed: " + key);
        
        SharedEglManager eglManager = SharedEglManager.getInstance();
        if (eglManager == null) return;
        
        // Apply specific dynamic changes
        switch (key) {
            case AppPreference.KEY.TIMESTAMP:
                boolean timestampEnabled = AppPreference.getBool(key, true);
                eglManager.updateTimestampOverlay(timestampEnabled);
                showToast("Timestamp " + (timestampEnabled ? "enabled" : "disabled"));
                break;
                
            case AppPreference.KEY.VIDEO_BITRATE:
                int bitrate = AppPreference.getInt(key, 2000000);
                eglManager.updateEncoderBitrate(bitrate);
                showToast("Bitrate updated to " + (bitrate / 1000) + " kbps");
                break;
                
            case AppPreference.KEY.SPLIT_TIME:
                int splitTime = AppPreference.getInt(key, 10);
                eglManager.updateFileSplitTime(splitTime);
                showToast("File split time updated to " + splitTime + " minutes");
                break;
                
            case AppPreference.KEY.FILE_ENCRYPTION:
                boolean encryption = AppPreference.getBool(key, false);
                showToast("Encryption " + (encryption ? "enabled" : "disabled") + 
                         " for next recording");
                break;
        }
    }
    
    private void handleCriticalSettingChange(String key) {
        Log.w(TAG, "Critical setting changed: " + key);
        
        SharedEglManager eglManager = SharedEglManager.getInstance();
        if (eglManager != null && (eglManager.isStreaming() || eglManager.isRecording())) {
            // Show warning that restart is required
            String settingName = getSettingDisplayName(key);
            showWarningDialog(settingName + " change requires stream/recording restart to take effect");
        }
    }
    
    private String getSettingDisplayName(String key) {
        switch (key) {
            case AppPreference.KEY.VIDEO_SIZE:
                return "Video resolution";
            case AppPreference.KEY.FPS:
                return "Frame rate";
            case AppPreference.KEY.SELECTED_POSITION:
                return "Camera selection";
            case AppPreference.KEY.VIDEO_CODEC:
                return "Video codec";
            case AppPreference.KEY.AUDIO_CODEC:
                return "Audio codec";
            default:
                return "Setting";
        }
    }
    
    private void updatePreferenceSummary(String key) {
        Preference preference = findPreference(key);
        if (preference == null) return;
        
        // Update summary based on current value
        switch (key) {
            case AppPreference.KEY.VIDEO_BITRATE:
                int bitrate = AppPreference.getInt(key, 2000000);
                preference.setSummary((bitrate / 1000) + " kbps");
                break;
                
            case AppPreference.KEY.SPLIT_TIME:
                int splitTime = AppPreference.getInt(key, 10);
                preference.setSummary(splitTime + " minutes");
                break;
                
            case AppPreference.KEY.TIMESTAMP:
                boolean timestamp = AppPreference.getBool(key, true);
                preference.setSummary(timestamp ? "Enabled" : "Disabled");
                break;
        }
    }
    
    private void showToast(String message) {
        Context context = getActivity();
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showWarningDialog(String message) {
        Context context = getActivity();
        if (context != null) {
            new android.app.AlertDialog.Builder(context)
                    .setTitle("Setting Change Warning")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .setNeutralButton("Stop Stream", (dialog, which) -> {
                        // Stop current stream/recording
                        SharedEglManager eglManager = SharedEglManager.getInstance();
                        if (eglManager != null) {
                            if (eglManager.isStreaming()) {
                                eglManager.stopStreaming();
                            }
                            if (eglManager.isRecording()) {
                                eglManager.stopRecording();
                            }
                        }
                    })
                    .show();
        }
    }
}