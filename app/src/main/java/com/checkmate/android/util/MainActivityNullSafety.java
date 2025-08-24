package com.checkmate.android.util;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Null safety helper methods for MainActivity
 * These methods provide safe wrappers around potentially null operations
 */
public class MainActivityNullSafety {
    private static final String TAG = "MainActivityNullSafety";
    private static final CrashLogger crashLogger = CrashLogger.getInstance();
    
    /**
     * Safely get MainActivity instance with null check
     */
    @Nullable
    public static MainActivity getInstanceSafe() {
        MainActivity instance = MainActivity.getInstance();
        if (instance == null) {
            Log.w(TAG, "MainActivity instance is null");
            if (crashLogger != null) {
                crashLogger.logWarning(TAG, "Attempted to access null MainActivity instance");
            }
        }
        return instance;
    }
    
    /**
     * Safely execute operation on MainActivity instance
     */
    public static void executeOnInstance(@NonNull SafeExecutor executor) {
        MainActivity instance = getInstanceSafe();
        if (instance != null) {
            try {
                executor.execute(instance);
            } catch (Exception e) {
                Log.e(TAG, "Error executing on MainActivity instance", e);
                if (crashLogger != null) {
                    crashLogger.logError(TAG, "Error in executeOnInstance", e);
                }
            }
        }
    }
    
    /**
     * Safely check if service is available
     */
    public static boolean isServiceAvailable(@NonNull ServiceType type) {
        MainActivity instance = getInstanceSafe();
        if (instance == null) return false;
        
        try {
            switch (type) {
                case CAMERA:
                    return instance.mCamService != null;
                case USB:
                    return instance.mUSBService != null;
                case CAST:
                    return instance.mCastService != null;
                case AUDIO:
                    return instance.mAudioService != null;
                case WIFI:
                    return instance.mWifiService != null;
                default:
                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking service availability", e);
            return false;
        }
    }
    
    /**
     * Safely stop service with null checks
     */
    public static void stopServiceSafe(@NonNull ServiceType type) {
        executeOnInstance(instance -> {
            switch (type) {
                case CAMERA:
                    if (instance.mCamService != null) {
                        instance.stopFragBgCamera();
                    }
                    break;
                case USB:
                    if (instance.mUSBService != null) {
                        instance.stopFragUSBService();
                    }
                    break;
                case CAST:
                    if (instance.mCastService != null) {
                        instance.stopFragBgCast();
                    }
                    break;
                case AUDIO:
                    if (instance.mAudioService != null) {
                        instance.stopFragAudio();
                    }
                    break;
                case WIFI:
                    if (instance.mWifiService != null) {
                        instance.stopFragWifiService();
                    }
                    break;
            }
        });
    }
    
    /**
     * Safely update UI elements
     */
    public static void updateUISafe(@NonNull UIUpdater updater) {
        MainActivity instance = getInstanceSafe();
        if (instance != null && instance.mHandler != null) {
            instance.mHandler.post(() -> {
                try {
                    updater.update(instance);
                } catch (Exception e) {
                    Log.e(TAG, "Error updating UI", e);
                    if (crashLogger != null) {
                        crashLogger.logError(TAG, "Error in updateUISafe", e);
                    }
                }
            });
        }
    }
    
    /**
     * Safely show dialog
     */
    public static void showDialogSafe() {
        updateUISafe(instance -> {
            if (instance.dlg_progress != null && !instance.isFinishing() && !instance.isDestroyed()) {
                instance.is_dialog = true;
                instance.dlg_progress.show();
            }
        });
    }
    
    /**
     * Safely dismiss dialog
     */
    public static void dismissDialogSafe() {
        updateUISafe(instance -> {
            if (instance.dlg_progress != null && instance.dlg_progress.isShowing()) {
                instance.is_dialog = false;
                instance.dlg_progress.dismiss();
            }
        });
    }
    
    /**
     * Safely access fragments
     */
    @Nullable
    public static <T extends BaseFragment> T getFragmentSafe(Class<T> fragmentClass) {
        MainActivity instance = getInstanceSafe();
        if (instance == null) return null;
        
        try {
            if (LiveFragment.class.equals(fragmentClass) && instance.liveFragment != null) {
                return fragmentClass.cast(instance.liveFragment);
            } else if (PlaybackFragment.class.equals(fragmentClass) && instance.playbackFragment != null) {
                return fragmentClass.cast(instance.playbackFragment);
            } else if (StreamingFragment.class.equals(fragmentClass) && instance.streamingFragment != null) {
                return fragmentClass.cast(instance.streamingFragment);
            } else if (SettingsFragment.class.equals(fragmentClass) && instance.settingsFragment != null) {
                return fragmentClass.cast(instance.settingsFragment);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error accessing fragment", e);
            if (crashLogger != null) {
                crashLogger.logError(TAG, "Error in getFragmentSafe", e);
            }
        }
        return null;
    }
    
    /**
     * Service types enum
     */
    public enum ServiceType {
        CAMERA, USB, CAST, AUDIO, WIFI
    }
    
    /**
     * Functional interface for safe execution
     */
    public interface SafeExecutor {
        void execute(@NonNull MainActivity instance) throws Exception;
    }
    
    /**
     * Functional interface for UI updates
     */
    public interface UIUpdater {
        void update(@NonNull MainActivity instance) throws Exception;
    }
}