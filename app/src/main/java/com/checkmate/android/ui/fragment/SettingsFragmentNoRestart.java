package com.checkmate.android.ui.fragment;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.CompoundButton;

import com.checkmate.android.AppPreference;
import com.checkmate.android.util.DynamicSettingsIntegration;
import com.checkmate.android.util.DynamicSettingsManager;

/**
 * Example modifications for SettingsFragment to prevent restarts
 * These methods should be integrated into the existing SettingsFragment
 */
public class SettingsFragmentNoRestart {
    
    /**
     * Example: Update timestamp setting without restart
     */
    private void setupTimestampSetting() {
        // Original code that causes restart:
        // swt_timestamp.setOnCheckedChangeListener((compoundButton, b) -> {
        //     AppPreference.setBool(AppPreference.KEY.TIMESTAMP, b);
        //     restartActivity(); // This causes restart!
        // });
        
        // New code that applies dynamically:
        swt_timestamp.setOnCheckedChangeListener((compoundButton, b) -> {
            // Use dynamic preference setter
            DynamicSettingsIntegration.DynamicPreference.setBool(AppPreference.KEY.TIMESTAMP, b);
            // No restart needed - timestamp is checked dynamically in drawFrame
            showToast("Timestamp " + (b ? "enabled" : "disabled"));
        });
    }
    
    /**
     * Example: Update video bitrate without restart
     */
    private void setupBitrateSetting() {
        // When bitrate spinner changes
        spinner_bitrate.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int bitrate = getBitrateFromPosition(position);
                
                // Use dynamic preference setter
                DynamicSettingsIntegration.DynamicPreference.setInt(
                    AppPreference.KEY.VIDEO_BITRATE, bitrate);
                
                // Check if we need to show warning
                DynamicSettingsManager manager = DynamicSettingsManager.getInstance(getContext());
                if (manager != null && manager.isDynamicSetting(AppPreference.KEY.VIDEO_BITRATE)) {
                    showToast("Bitrate updated to " + (bitrate / 1000) + " kbps");
                }
            }
        });
    }
    
    /**
     * Example: Update video resolution (critical setting)
     */
    private void setupResolutionSetting() {
        spinner_resolution.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String resolution = getResolutionFromPosition(position);
                
                // Save the preference
                AppPreference.setStr(AppPreference.KEY.VIDEO_SIZE, resolution);
                
                // Check if stream is active
                SharedEglManager eglManager = SharedEglManager.getInstance();
                if (eglManager != null && (eglManager.isStreaming() || eglManager.isRecording())) {
                    // Show dialog instead of restarting
                    showCriticalSettingDialog("Video resolution", 
                        "This change will take effect when you restart the stream.");
                } else {
                    // Apply immediately if not streaming
                    applyResolutionChange(resolution);
                }
            }
        });
    }
    
    /**
     * Example: Generic setting change handler
     */
    private void handleSettingChange(String key, Object value) {
        DynamicSettingsManager manager = DynamicSettingsManager.getInstance(getContext());
        if (manager == null) return;
        
        if (manager.isDynamicSetting(key)) {
            // Apply immediately
            showToast("Setting applied");
        } else if (manager.isCriticalSetting(key)) {
            // Check if streaming
            SharedEglManager eglManager = SharedEglManager.getInstance();
            if (eglManager != null && (eglManager.isStreaming() || eglManager.isRecording())) {
                showCriticalSettingDialog(getSettingName(key), 
                    "This change requires stopping the current stream/recording.");
            }
        }
    }
    
    /**
     * Show dialog for critical settings instead of restarting
     */
    private void showCriticalSettingDialog(String settingName, String message) {
        new AlertDialog.Builder(getContext())
            .setTitle("Setting Change")
            .setMessage(settingName + ": " + message)
            .setPositiveButton("OK", null)
            .setNegativeButton("Stop Stream", (dialog, which) -> {
                // Stop streaming to apply the change
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
    
    /**
     * Convert all restart calls to dynamic updates
     */
    private void removeRestartCalls() {
        // Instead of:
        // getActivity().recreate();
        // getActivity().finish();
        // restartActivity();
        
        // Use:
        // DynamicSettingsIntegration.handleSettingChange(key, value);
        // Show appropriate feedback to user
    }
}