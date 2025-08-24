package com.checkmate.android.util;

import android.content.Context;
import android.util.Log;
import android.app.Activity;

import com.checkmate.android.AppPreference;

/**
 * Enhanced PreferenceInitializer that sets defaults during activation
 * and handles storage permissions and default storage location setup.
 *
 * Call this during activation to guarantee that every preference has a value
 * and storage is properly configured.
 */
public final class PreferenceInitializer {

    private static final String TAG = "PreferenceInitializer";
    private PreferenceInitializer() { /* no-instance */ }

    /**
     * Initialize all defaults - called during app startup
     */
    public static void initDefaults(Context ctx) {
        Log.d(TAG, "Initializing default preferences");

        //---------------------------  GENERAL UI  ----------------------------
        setDefault(AppPreference.KEY.UI_CONVERT_MODE          , false); // bool
        setDefault(AppPreference.KEY.PIN_NUMBER               , "");    // str
        setDefault(AppPreference.KEY.ORIENTATION_LOCK         , false); // bool
        setDefault(AppPreference.KEY.TIMESTAMP                , true);  // bool
        setDefault(AppPreference.KEY.VU_METER                 , true);  // bool
        setDefault(AppPreference.KEY.SECURE_MULTI_TASK        , true);  // bool
        setDefault(AppPreference.KEY.VOLUME_KEY               , false); // bool

        //---------------------------  RECORDING  ----------------------------
        setDefault(AppPreference.KEY.VIDEO_QUALITY            , 1);     // int  (MEDIUM)
        setDefault(AppPreference.KEY.VIDEO_RESOLUTION         , 0);     // int  (index)
        setDefault(AppPreference.KEY.VIDEO_FRAME              , 30);    // int
        setDefault(AppPreference.KEY.VIDEO_BITRATE            , 4096);  // kbps
        setDefault(AppPreference.KEY.VIDEO_KEYFRAME           , 1);     // sec
        setDefault(AppPreference.KEY.SPLIT_TIME               , 10);    // min
        setDefault(AppPreference.KEY.RECORD_AUDIO             , false); // bool
        setDefault(AppPreference.KEY.USE_AUDIO                , true);  // bool

        //---------------------------  STREAMING  ----------------------------
        setDefault(AppPreference.KEY.STREAMING_QUALITY        , 1);     // int (MED)
        setDefault(AppPreference.KEY.STREAMING_RESOLUTION     , 1);     // int (index for 960x540)
        setDefault(AppPreference.KEY.STREAMING_FRAME          , 30);    // int
        setDefault(AppPreference.KEY.STREAMING_BITRATE        , 1024);  // kbps
        setDefault(AppPreference.KEY.STREAMING_KEYFRAME       , 1);     // sec
        setDefault(AppPreference.KEY.STREAMING_AUDIO_BITRATE  , 0);     // idx
        setDefault(AppPreference.KEY.ADAPTIVE_MODE            , 3);     // "Off"
        setDefault(AppPreference.KEY.ADAPTIVE_FRAMERATE       , false); // bool
        setDefault(AppPreference.KEY.STREAMING_RADIO_MODE     , false); // bool

        //---------------------------  CAST / MIRROR  ------------------------
        setDefault(AppPreference.KEY.CAST_RESOLUTION          , 0);     // int
        setDefault(AppPreference.KEY.CAST_BITRATE             , 2048);  // kbps
        setDefault(AppPreference.KEY.CAST_FRAME               , 1);     // sec

        //---------------------------  USB CAMERA  ---------------------------
        setDefault(AppPreference.KEY.CAM_USB                  , false); // bool
        setDefault(AppPreference.KEY.USB_RESOLUTION           , 1);     // int
        setDefault(AppPreference.KEY.CODEC_SRC                , 0);     // int
        setDefault(AppPreference.KEY.USB_MIN_FPS              , 30);    // int
        setDefault(AppPreference.KEY.USB_MAX_FPS              , 30);    // int
        setDefault(AppPreference.KEY.USB_FRAME                , 30);    // int
        setDefault(AppPreference.KEY.USB_SAMPLE_RATE          , 7);     // idx
        setDefault(AppPreference.KEY.USB_AUDIO_SRC            , 0);     // idx
        setDefault(AppPreference.KEY.USB_AUDIO_BITRATE        , 0);     // idx
        setDefault(AppPreference.KEY.BLUETOOTH_USB_MIC        , false); // bool
        setDefault(AppPreference.KEY.USB_BLE_AUDIO_SRC        , 0);     // idx

        //---------------------------  AUDIO  --------------------------------
        setDefault(AppPreference.KEY.AUDIO_SRC                , 0);     // idx
        setDefault(AppPreference.KEY.SAMPLE_RATE              , 7);     // idx
        setDefault(AppPreference.KEY.BLUETOOTH_MIC            , false); // bool
        setDefault(AppPreference.KEY.BLE_AUDIO_SRC            , 0);     // idx
        setDefault(AppPreference.KEY.CHANNEL_COUNT            , 0);     // idx
        // "Audio-options" block
        setDefault(AppPreference.KEY.AUDIO_OPTION_AUDIO_SETTING , 0);
        setDefault(AppPreference.KEY.AUDIO_OPTION_AUDIO_SRC     , 0);
        setDefault(AppPreference.KEY.AUDIO_OPTION_BITRATE       , 0);
        setDefault(AppPreference.KEY.AUDIO_OPTION_SAMPLE_RATE   , 0);
        setDefault(AppPreference.KEY.AUDIO_OPTION_CHANNEL_COUNT , 0);

        //---------------------------  MISC  ---------------------------------
        setDefault(AppPreference.KEY.FIFO                     , true);  // bool
        setDefault(AppPreference.KEY.AUTO_RECORD              , false); // bool
        setDefault(AppPreference.KEY.IS_NATIVE_RESOLUTION     , true);
        setDefault(AppPreference.KEY.IS_NATIVE_STREAMING      , true);
        setDefault(AppPreference.KEY.TRANS_APPLY_SETTINGS     , false); // bool
        setDefault(AppPreference.KEY.FILE_ENCRYPTION          , false); // bool
        setDefault(AppPreference.KEY.ENCRYPTION_KEY           , "");    // str

        //---------------------------  APP UPDATE  ---------------------------
        setDefault(AppPreference.KEY.APP_VERSION              , "");
        setDefault(AppPreference.KEY.APP_URL                  , "");

        //---------------------------  CAMERA FLAGS  -------------------------
        setDefault(AppPreference.KEY.CAM_FRONT_FACING         , true);
        setDefault(AppPreference.KEY.CAM_REAR_FACING          , true);
        setDefault(AppPreference.KEY.CAM_CAST                 , false);
        setDefault(AppPreference.KEY.AUDIO_ONLY               , false);
        setDefault(AppPreference.KEY.SELECTED_POSITION        ,"0");    // int

        //---------------------------  MARK INITIALISED ----------------------
        AppPreference.setBool("PREFS_INITIALISED", true);

        // Ensure streaming resolution consistency
        ensureStreamingResolutionConsistency();

        Log.d(TAG, "Default preferences initialized successfully");
    }

    /**
     * Initialize activation-specific defaults - called during activation
     */
    public static void initActivationDefaults(Context ctx) {
        Log.d(TAG, "Initializing activation-specific defaults");

        // Set default storage location to public folder
        String defaultStoragePath = DefaultStorageUtils.getDefaultStoragePath();
        setDefault(AppPreference.KEY.Storage_Type, "Storage Location: Phone Storage");
        setDefault(AppPreference.KEY.STORAGE_LOCATION, defaultStoragePath);
        setDefault(AppPreference.KEY.GALLERY_PATH, defaultStoragePath);
        setDefault(AppPreference.KEY.IS_STORAGE_INTERNAL, "INTERNAL STORAGE");

        // Set activation-specific defaults
        setDefault(AppPreference.KEY.USB_RESOLUTION, 1);
        setDefault(AppPreference.KEY.STREAMING_MODE, 0);
        setDefault(AppPreference.KEY.STREAMING_QUALITY, 1); // Medium quality (960x540)
        // Note: STREAMING_RESOLUTION index will be calculated by SettingsFragment based on available camera sizes
        setDefault(AppPreference.KEY.LOCAL_STREAM_IP, "172.20.1.1");
        setDefault(AppPreference.KEY.LOCAL_STREAM_IP_TEST, "76.239.139.178");
        setDefault(AppPreference.KEY.LOCAL_STREAM_PORT, 554);
        setDefault(AppPreference.KEY.LOCAL_STREAM_PORT_TEST, 8554);
        setDefault(AppPreference.KEY.LOCAL_STREAM_NAME, "");
        setDefault(AppPreference.KEY.LOCAL_STREAM_PASSWORD, "");

        // Set device-specific channel
        String deviceId = com.checkmate.android.util.CommonUtil.getDeviceID(ctx);
        setDefault(AppPreference.KEY.LOCAL_STREAM_CHANNEL, deviceId);

        // Ensure streaming resolution consistency
        ensureStreamingResolutionConsistency();

        Log.d(TAG, "Activation defaults initialized with storage path: " + defaultStoragePath);
    }

    /**
     * Handle storage selection during activation
     * This method can be called to prompt user for storage selection
     */
    public static void handleStorageSelection(Context ctx, SplashStorageHelper.StorageSelectionCallback callback) {
        Log.d(TAG, "Handling storage selection during activation");

        // Create storage helper and show selection dialog
        SplashStorageHelper storageHelper = new SplashStorageHelper((Activity) ctx, callback);
        storageHelper.showStorageSelectionDialog();
    }

    /**
     * Set storage to default public folder
     */
    public static void setDefaultStorage(Context ctx) {
        Log.d(TAG, "Setting default storage location");

        String defaultStoragePath = DefaultStorageUtils.getDefaultStoragePath();
        AppPreference.setStr(AppPreference.KEY.STORAGE_LOCATION, defaultStoragePath);
        AppPreference.setStr(AppPreference.KEY.GALLERY_PATH, defaultStoragePath);
        AppPreference.setStr(AppPreference.KEY.Storage_Type, "Storage Location: Phone Storage");
        AppPreference.setStr(AppPreference.KEY.IS_STORAGE_INTERNAL, "INTERNAL STORAGE");

        Log.d(TAG, "Default storage set to: " + defaultStoragePath);
    }

    /**
     * Validate and fix storage location if needed
     */
    public static void validateStorageLocation(Context ctx) {
        String currentLocation = AppPreference.getStr(AppPreference.KEY.STORAGE_LOCATION, "");

        // Check if storage location is valid
        if (currentLocation == null || currentLocation.isEmpty() ||
                !DefaultStorageUtils.isDefaultStorageAccessible()) {

            Log.w(TAG, "Invalid storage location detected: " + currentLocation +
                    ", setting to default");

            // Set to default public folder
            String defaultPath = DefaultStorageUtils.getDefaultStoragePath();
            AppPreference.setStr(AppPreference.KEY.STORAGE_LOCATION, defaultPath);
            AppPreference.setStr(AppPreference.KEY.GALLERY_PATH, defaultPath);
            AppPreference.setStr(AppPreference.KEY.Storage_Type, "Storage Location: Phone Storage");
            AppPreference.setStr(AppPreference.KEY.IS_STORAGE_INTERNAL, "INTERNAL STORAGE");

            Log.d(TAG, "Storage location updated to: " + defaultPath);
        }
    }

    /**
     * Ensure streaming resolution consistency
     * This method ensures that the streaming resolution index matches the streaming quality
     * Note: The actual resolution index calculation is handled by SettingsFragment based on available camera sizes
     */
    public static void ensureStreamingResolutionConsistency() {
        int streamingQuality = AppPreference.getInt(AppPreference.KEY.STREAMING_QUALITY, 1);

        Log.d(TAG, "Ensuring streaming resolution consistency - Quality: " + streamingQuality +
                " (0=High, 1=Medium, 2=Low, 3=SuperLow, 4=USB, 5=Custom)");

        // For preset qualities (0-4), the resolution index will be calculated by SettingsFragment
        // based on available camera sizes. We don't hardcode indices here.
        // The SettingsFragment will call the appropriate quality method (onStreamHigh, onStreamMedium, etc.)
        // which will find the correct resolution index dynamically.

        Log.d(TAG, "Streaming quality set to: " + streamingQuality +
                " - Resolution index will be calculated by SettingsFragment");
    }

    /* -------------------------------------------------------------------- */
    /* ----- Helper that writes only when the key has never been set.  -----*/
    /* -------------------------------------------------------------------- */
    private static void setDefault(String key, boolean def) {
        if (!AppPreference.contains(key)) {
            AppPreference.setBool(key, def);
            Log.d(TAG, "Set default boolean: " + key + " = " + def);
        }
    }

    private static void setDefault(String key, int def) {
        if (!AppPreference.contains(key)) {
            AppPreference.setInt(key, def);
            Log.d(TAG, "Set default int: " + key + " = " + def);
        }
    }

    private static void setDefault(String key, String def) {
        if (!AppPreference.contains(key)) {
            AppPreference.setStr(key, def);
            Log.d(TAG, "Set default string: " + key + " = " + def);
        }
    }
}