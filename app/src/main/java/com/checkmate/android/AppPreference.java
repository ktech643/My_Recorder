package com.checkmate.android;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.checkmate.android.util.InternalLogger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class AppPreference {
    private static final String TAG = "AppPreference";
    private static final int OPERATION_TIMEOUT_SECONDS = 5;
    private static volatile SharedPreferences instance = null;
    private static final Object instanceLock = new Object();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static class KEY {
        // settings
        final public static String API_KEY = "VkNTIENoZWNrbWF0ZSBBbmRyb2lkIEFwcA==";
        final public static String EGL_RESTART_FLAG = "EGL_RESTART_FLAG";
        final public static String CAM_FRONT_FACING = "CAM_FRONT_FACING";
        final public static String CAM_REAR_FACING = "CAM_REAR_FACING";
        final public static String CAM_USB = "CAM_USB";
        final public static String IS_APP_BACKGROUND = "IS_APP_BACKGROUND";
        final public static String DEVICE_ID = "DEVICE_ID";
        final public static String CAM_CAST = "CAM_CAST";
        final public static String AUDIO_ONLY = "AUDIO_ONLY";
        final public static String USER_TOKEN = "USER_TOKEN";
        final public static String PWD_TOKEN = "PWD_TOKEN";
        final public static String UI_CONVERT_MODE = "UI_CONVERT_MODE";
        final public static String IS_RESTART_APP = "IS_RESTART_APP";
        final public static String IS_BYPASS_COVERTMODE = "IS_BYPASS_COVERTMODE";
        final public static String IS_RETURN_FOREGROUND = "IS_RETURN_FOREGROUND";
        final public static String IS_FOR_STORAGE_LOCATION = "IS_FOR_STORAGE_LOCATION";
        final public static String IS_FOR_PLAYBACK_LOCATION = "IS_FOR_PLAYBACK_LOCATION";
        final public static String USB_CAMERA_NAME = "usb_camera_name";
        final public static String ORIENTATION_LOCK = "ORIENTATION_LOCK";
        final public static String TIMESTAMP = "TIMESTAMP";
        final public static String VU_METER = "VU_METER";
        final public static String FILE_ENCRYPTION = "FILE_ENCRYPTION";
        final public static String ENCRYPTION_KEY = "ENCRYPTION_KEY";
        final public static String CURRENT_SSID = "CURRENT_SSID";
        final public static String SECRET_KEY = "SECRET_KEY";
        final public static String PIN_NUMBER = "PIN_NUMBER";
        final public static String SPLIT_TIME = "SPLIT_TIME";
        final public static String BETA_URL = "BETA_URL";
        final public static String TAPPED_NUMBER = "TAPPED_NUMBER";
        final public static String RECORD_AUDIO = "RECORD_AUDIO";
        final public static String USE_AUDIO = "USE_AUDIO";
        final public static String VOLUME_KEY = "VOLUME_KEY";
        final public static String AUTO_RECORD = "AUTO_RECORD";
        final public static String FIFO = "FIFO";
        final public static String SELECTED_POSITION = "SELECTED_POSITION";
        final public static String VIDEO_RESOLUTION = "VIDEO_RESOLUTION";
        final public static String IS_NATIVE_RESOLUTION = "IS_NATIVE_RESOLUTION";
        final public static String STREAMING_RESOLUTION = "STREAMING_RESOLUTION";
        final public static String IS_NATIVE_STREAMING = "IS_NATIVE_STREAMING";
        final public static String CAST_RESOLUTION = "CAST_RESOLUTION";

        final public static String VIDEO_QUALITY = "VIDEO_QUALITY";
        final public static String STREAMING_QUALITY = "STREAMING_QUALITY";

        final public static String VIDEO_FRAME = "VIDEO_FRAME";
        final public static String STREAMING_FRAME = "STREAMING_FRAME";
        final public static String USB_FRAME = "USB_FRAME";
        final public static String CAST_FRAME = "CAST_FRAME";
        final public static String VIDEO_BITRATE = "VIDEO_BITRATE";
        final public static String CAST_BITRATE = "CAST_BITRATE";
        final public static String STREAMING_BITRATE = "STREAMING_BITRATE";
        final public static String VIDEO_KEYFRAME = "VIDEO_KEYFRAME";
        final public static String STREAMING_KEYFRAME = "STREAMING_KEYFRAME";
        final public static String STREAMING_SETTINGS = "STREAMING_SETTINGS";
        final public static String SECURE_MULTI_TASK = "SECURE_MULTI_TASK";
        final public static String APP_FIRST_LAUNCH = "APP_FIRST_LAUNCH";
        final public static String APP_MAIN_FIRST_LAUNCH = "APP_MAIN_FIRST_LAUNCH";
        final public static String APP_FORCE_QUIT = "APP_FORCE_QUIT";
        final public static String VIDEO_CUSTOM = "VIDEO_CUSTOM";
        final public static String STREAMING_CUSTOM = "STREAMING_CUSTOM";
        final public static String STREAMING_AUDIO_BITRATE = "STREAMING_AUDIO_BITRATE";
        final public static String USB_AUDIO_BITRATE = "USB_AUDIO_BITRATE";
        final public static String USB_BITRATE = "USB_BITRATE";
        final public static String AUDIO_BITRATE = "AUDIO_BITRATE";
        final public static String STREAMING_RADIO_MODE = "STREAMING_RADIO_MODE";
        final public static String ADAPTIVE_FRAMERATE = "ADAPTIVE_FRAMERATE";
        final public static String BLUETOOTH_MIC = "BLUETOOTH_MIC";
        final public static String BLUETOOTH_USB_MIC = "BLUETOOTH_USB_MIC";
        final public static String AUDIO_SRC = "AUDIO_SRC";
        final public static String CHANNEL_COUNT = "CHANNEL_COUNT";
        final public static String USB_AUDIO_SRC = "USB_AUDIO_SRC";
        final public static String BLE_AUDIO_SRC = "BLE_AUDIO_SRC";
        final public static String USB_BLE_AUDIO_SRC = "BLE_AUDIO_SRC";
        final public static String SAMPLE_RATE = "SAMPLE_RATE";
        final public static String USB_SAMPLE_RATE = "USB_SAMPLE_RATE";
        final public static String ADAPTIVE_MODE = "ADAPTIVE_MODE";
        final public static String ADAPTIVE_MODE_CAST = "ADAPTIVE_MODE_CAST";
        final public static String USB_RESOLUTION = "USB_RESOLUTION";
        final public static String STREAMING_MODE = "STREAMING_MODE";
        final public static String LOCAL_STREAM_IP = "LOCAL_STREAM_IP";
        final public static String LOCAL_STREAM_IP_TEST = "LOCAL_STREAM_IP_TEST";
        final public static String LOCAL_STREAM_PORT = "LOCAL_STREAM_PORT";
        final public static String LOCAL_STREAM_PORT_TEST = "LOCAL_STREAM_PORT_TEST";
        final public static String LOCAL_STREAM_NAME = "LOCAL_STREAM_NAME";
        final public static String LOCAL_STREAM_PASSWORD = "LOCAL_STREAM_PASSWORD";
        final public static String LOCAL_STREAM_CHANNEL = "LOCAL_STREAM_CHANNEL";
        final public static String First_Launch = "First_Launch";

        final public static String APP_VERSION = "APP_VERSION";
        final public static String APP_URL = "APP_URL";
        final public static String EXPIRY_DATE = "EXPIRY_DATE";
        final public static String STREAM_TYPE = "STREAM_TYPE";
        final public static String VIDEO_PATH = "VIDEO_PATH";
        final public static String STREAM_STARTED = "STREAM_STARTED";
        final public static String IS_USB_OPENED = "IS_USB_OPENED";
        final public static String CHESS_MODE_PIN = "CHESS_MODE_PIN";
        final public static String USB_STREAM_STARTED = "USB_STREAM_STARTED";
        final public static String GALLERY_PATH = "GALLERY_PATH";
        final public static String STORAGE_LOCATION = "STORAGE_LOCATION";
        final public static String temp_PATH = "temp_PATH";
        final public static String APP_OLD_VERSION = "APP_OLD_VERSION";
        final public static String BROADCAST = "BROADCAST";
        final public static String RECORD_BROADCAST = "RECORD_BROADCAST";
        final public static String GPS_ENABLED = "GPS_ENABLED";
        final public static String Storage_Type = "Storage_Type";;
        final public static String IS_STORAGE_INTERNAL = "IS_STORAGE_INTERNAL";;
        final public static String IS_STORAGE_SDCARD = "SDCARD";
        final public static String IS_STORAGE_EXTERNAL = "EXTERNAL";
        final public static String RECORDING_STARTED = "RECORDING_STARTED";

        final public static String ACTIVATION_CODE = "ACTIVATION_CODE";
        final public static String ACTIVATION_SERIAL = "ACTIVATION_SERIAL";

        final public static String UPDATE_COMPLETED = "UPDATE_COMPLETED";

        final public static String STREAM_USERNAME = "STREAM_USERNAME";
        final public static String STREAM_PASSWORD = "STREAM_PASSWORD";
        final public static String STREAM_BASE = "STREAM_BASE";
        final public static String STREAM_CHANNEL = "STREAM_CHANNEL";

        final public static String DB_VERSION = "DB_VERSION";

        final public static String LOGIN_EMAIL = "LOGIN_EMAIL";
        final public static String LOGIN_PASSWORD = "LOGIN_PASSWORD";
        final public static String DEVICE_NAME = "DEVICE_NAME";

        final public static String FREQUENCY_MIN = "FREQUENCY_MIN";
        final public static String FPS_RANGE_MIN = "FPS_RANGE_MIN";
        final public static String FPS_RANGE_MAX = "FPS_RANGE_MAX";
        final public static String FPS_RANGE = "FPS_RANGE";

        final public static String CODEC_SRC = "CODEC_SRC";
        final public static String USB_MIN_FPS = "USB_MIN_FPS";
        final public static String USB_MAX_FPS = "USB_MAX_FPS";
        final public static String RESTART_COUNT = "RESTART_COUNT";
        final public static String LAST_RESTART_TIME = "LAST_RESTART_TIME";


        // transcode options
        final public static String TRANS_WIDTH = "TRANS_WIDTH";
        final public static String TRANS_HEIGHT = "TRANS_HEIGHT";
        final public static String TRANS_AUDIO_PUSH = "TRANS_AUDIO_PUSH";
        final public static String TRANS_AUDIO_MP4 = "TRANS_AUDIO_MP4";
        final public static String TRANS_BOX = "TRANS_BOX";
        final public static String TRANS_BOX_COLOR = "TRANS_BOX_COLOR";
        final public static String TRNAS_BOX_FONT = "TRNAS_BOX_FONT";
        final public static String TRANS_BOX_FONT_SIZE = "TRANS_BOX_FONT_SIZE";
        final public static String TRNAS_BOX_FONT_COLOR = "TRNAS_BOX_FONT_COLOR";
        final public static String TRANS_BOX_X0 = "TRANS_BOX_X0";
        final public static String TRANS_BOX_Y0 = "TRANS_BOX_Y0";
        final public static String TRANS_BOX_FORMAT = "TRANS_BOX_FORMAT";
        final public static String TRANS_BOX_ENABLE = "TRANS_BOX_ENABLE";
        final public static String TRANS_BOX_USE_MIC = "TRANS_BOX_USE_MIC";
        final public static String TRANS_APPLY_SETTINGS = "TRANS_APPLY_SETTINGS";
        final public static String TRANS_BITRATE = "TRANS_BITRATE";
        final public static String TRANS_FRAMERATE = "TRANS_FRAMERATE";
        final public static String TRANS_FPS = "TRANS_FPS";
        final public static String TRANS_OVERLAY = "TRANS_OVERLAY";
        final public static String RESOLUTION_PARAM = "RESOLUTION_PARAM";
        final public static String AUDIO_OPTION_AUDIO_SRC = "AUDIO_OPTION_AUDIO_SRC";
        final public static String AUDIO_OPTION_AUDIO_SETTING = "AUDIO_OPTION_AUDIO_SETTING";
        final public static String AUDIO_OPTION_CHANNEL_COUNT = "AUDIO_OPTION_CHANNEL_COUNT";
        final public static String AUDIO_OPTION_SAMPLE_RATE = "AUDIO_OPTION_SAMPLE_RATE";
        final public static String AUDIO_OPTION_BITRATE = "AUDIO_OPTION_BITRATE";
        
        // Rotation settings
        final public static String IS_ROTATED = "IS_ROTATED";
        final public static String IS_FLIPPED = "IS_FLIPPED";
        final public static String IS_MIRRORED = "IS_MIRRORED";
    }

    /**
     * Thread-safe initialization of SharedPreferences instance
     * @param pref SharedPreferences instance to use
     */
    public static void initialize(SharedPreferences pref) {
        if (pref == null) {
            InternalLogger.e(TAG, "Attempted to initialize with null SharedPreferences");
            return;
        }
        
        synchronized (instanceLock) {
            instance = pref;
            InternalLogger.i(TAG, "AppPreference initialized successfully");
        }
    }
    
    /**
     * Check if AppPreference is properly initialized
     * @return true if initialized, false otherwise
     */
    public static boolean isInitialized() {
        return instance != null;
    }
    
    /**
     * Get SharedPreferences instance with null safety
     * @return SharedPreferences instance or null if not initialized
     */
    private static SharedPreferences getInstance() {
        if (instance == null) {
            InternalLogger.w(TAG, "AppPreference not initialized, returning null");
        }
        return instance;
    }
    
    /**
     * Execute preference operation with ANR prevention and error recovery
     * @param operation The operation to execute
     * @param defaultValue Default value to return on failure
     * @param <T> Return type
     * @return Result of operation or default value
     */
    private static <T> T executeWithTimeout(PreferenceOperation<T> operation, T defaultValue) {
        if (instance == null) {
            InternalLogger.w(TAG, "SharedPreferences not initialized, returning default value");
            return defaultValue;
        }
        
        // If we're on the main thread, execute directly to avoid deadlock
        if (Looper.myLooper() == Looper.getMainLooper()) {
            try {
                return operation.execute(instance);
            } catch (Exception e) {
                InternalLogger.e(TAG, "Preference operation failed on main thread", e);
                return defaultValue;
            }
        }
        
        // For background threads, use timeout mechanism
        final AtomicReference<T> result = new AtomicReference<>(defaultValue);
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Exception> exception = new AtomicReference<>();
        
        mainHandler.post(() -> {
            try {
                T value = operation.execute(instance);
                result.set(value);
            } catch (Exception e) {
                exception.set(e);
                InternalLogger.e(TAG, "Preference operation failed", e);
            } finally {
                latch.countDown();
            }
        });
        
        try {
            if (latch.await(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                Exception ex = exception.get();
                if (ex != null) {
                    InternalLogger.w(TAG, "Preference operation completed with error, using default value");
                    return defaultValue;
                }
                return result.get();
            } else {
                InternalLogger.e(TAG, "Preference operation timed out, using default value");
                return defaultValue;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            InternalLogger.e(TAG, "Preference operation interrupted", e);
            return defaultValue;
        }
    }
    
    /**
     * Interface for preference operations
     */
    private interface PreferenceOperation<T> {
        T execute(SharedPreferences prefs) throws Exception;
    }

    /**
     * Thread-safe check if preference contains key
     * @param key Preference key to check
     * @return true if key exists, false otherwise
     */
    public static boolean contains(String key) {
        if (key == null || key.trim().isEmpty()) {
            InternalLogger.w(TAG, "contains() called with null or empty key");
            return false;
        }
        
        return executeWithTimeout(prefs -> prefs.contains(key), false);
    }

    /**
     * Thread-safe boolean getter with null safety and type checking
     * @param key Preference key
     * @param def Default value
     * @return Boolean value or default if error occurs
     */
    public static boolean getBool(String key, boolean def) {
        if (key == null || key.trim().isEmpty()) {
            InternalLogger.w(TAG, "getBool() called with null or empty key, returning default");
            return def;
        }
        
        return executeWithTimeout(prefs -> {
            try {
                return prefs.getBoolean(key, def);
            } catch (ClassCastException e) {
                InternalLogger.w(TAG, "Type mismatch for key: " + key + ", expected boolean, returning default");
                return def;
            }
        }, def);
    }

    /**
     * Thread-safe boolean setter with error recovery
     * @param key Preference key
     * @param value Boolean value to set
     */
    public static void setBool(String key, boolean value) {
        if (key == null || key.trim().isEmpty()) {
            InternalLogger.w(TAG, "setBool() called with null or empty key, ignoring");
            return;
        }
        
        executeWithTimeout(prefs -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(key, value);
            
            // Use apply() for better performance and ANR prevention
            editor.apply();
            
            // Verify write was successful
            if (!prefs.getBoolean(key, !value)) {
                InternalLogger.w(TAG, "Failed to verify boolean write for key: " + key + ", attempting commit");
                editor = prefs.edit();
                editor.putBoolean(key, value);
                if (!editor.commit()) {
                    InternalLogger.e(TAG, "Failed to commit boolean preference for key: " + key);
                }
            }
            return null;
        }, null);
    }

    /**
     * Thread-safe integer getter with null safety and type checking
     * @param key Preference key
     * @param def Default value
     * @return Integer value or default if error occurs
     */
    public static int getInt(String key, int def) {
        if (key == null || key.trim().isEmpty()) {
            InternalLogger.w(TAG, "getInt() called with null or empty key, returning default");
            return def;
        }
        
        return executeWithTimeout(prefs -> {
            try {
                return prefs.getInt(key, def);
            } catch (ClassCastException e) {
                InternalLogger.w(TAG, "Type mismatch for key: " + key + ", expected int, returning default");
                return def;
            }
        }, def);
    }

    /**
     * Thread-safe integer setter with error recovery
     * @param key Preference key
     * @param value Integer value to set
     */
    public static void setInt(String key, int value) {
        if (key == null || key.trim().isEmpty()) {
            InternalLogger.w(TAG, "setInt() called with null or empty key, ignoring");
            return;
        }
        
        executeWithTimeout(prefs -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(key, value);
            
            // Use apply() for better performance and ANR prevention
            editor.apply();
            
            // Verify write was successful
            if (prefs.getInt(key, value - 1) != value) {
                InternalLogger.w(TAG, "Failed to verify int write for key: " + key + ", attempting commit");
                editor = prefs.edit();
                editor.putInt(key, value);
                if (!editor.commit()) {
                    InternalLogger.e(TAG, "Failed to commit int preference for key: " + key);
                }
            }
            return null;
        }, null);
    }

    /**
     * Thread-safe long getter with null safety and type checking
     * @param key Preference key
     * @param def Default value
     * @return Long value or default if error occurs
     */
    public static long getLong(String key, long def) {
        if (key == null || key.trim().isEmpty()) {
            InternalLogger.w(TAG, "getLong() called with null or empty key, returning default");
            return def;
        }
        
        return executeWithTimeout(prefs -> {
            try {
                return prefs.getLong(key, def);
            } catch (ClassCastException e) {
                InternalLogger.w(TAG, "Type mismatch for key: " + key + ", expected long, returning default");
                return def;
            }
        }, def);
    }

    /**
     * Thread-safe long setter with error recovery
     * @param key Preference key
     * @param value Long value to set
     */
    public static void setLong(String key, long value) {
        if (key == null || key.trim().isEmpty()) {
            InternalLogger.w(TAG, "setLong() called with null or empty key, ignoring");
            return;
        }
        
        executeWithTimeout(prefs -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(key, value);
            
            // Use apply() for better performance and ANR prevention
            editor.apply();
            
            // Verify write was successful
            if (prefs.getLong(key, value - 1) != value) {
                InternalLogger.w(TAG, "Failed to verify long write for key: " + key + ", attempting commit");
                editor = prefs.edit();
                editor.putLong(key, value);
                if (!editor.commit()) {
                    InternalLogger.e(TAG, "Failed to commit long preference for key: " + key);
                }
            }
            return null;
        }, null);
    }

    /**
     * Thread-safe string getter with null safety and type checking
     * @param key Preference key
     * @param def Default value
     * @return String value or default if error occurs
     */
    public static String getStr(String key, String def) {
        if (key == null || key.trim().isEmpty()) {
            InternalLogger.w(TAG, "getStr() called with null or empty key, returning default");
            return def;
        }
        
        return executeWithTimeout(prefs -> {
            try {
                String result = prefs.getString(key, def);
                return result != null ? result : def;
            } catch (ClassCastException e) {
                InternalLogger.w(TAG, "Type mismatch for key: " + key + ", expected string, returning default");
                return def;
            }
        }, def);
    }

    /**
     * Thread-safe string setter with error recovery
     * @param key Preference key
     * @param value String value to set (null values are converted to empty string)
     */
    public static void setStr(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            InternalLogger.w(TAG, "setStr() called with null or empty key, ignoring");
            return;
        }
        
        // Convert null to empty string for consistency
        final String safeValue = value != null ? value : "";
        
        executeWithTimeout(prefs -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(key, safeValue);
            
            // Use apply() for better performance and ANR prevention
            editor.apply();
            
            // Verify write was successful
            String stored = prefs.getString(key, null);
            if (!safeValue.equals(stored)) {
                InternalLogger.w(TAG, "Failed to verify string write for key: " + key + ", attempting commit");
                editor = prefs.edit();
                editor.putString(key, safeValue);
                if (!editor.commit()) {
                    InternalLogger.e(TAG, "Failed to commit string preference for key: " + key);
                }
            }
            return null;
        }, null);
    }

    /**
     * Thread-safe float setter with error recovery and ANR protection
     * @param key Preference key
     * @param value Float value to set
     */
    public static void setFloat(String key, float value) {
        if (key == null || key.trim().isEmpty()) {
            InternalLogger.w(TAG, "setFloat() called with null or empty key, ignoring");
            return;
        }
        
        executeWithTimeout(prefs -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putFloat(key, value);
            
            // Use apply() for better performance and ANR prevention
            editor.apply();
            
            // Verify write was successful
            float stored = prefs.getFloat(key, Float.MIN_VALUE);
            if (stored != value) {
                InternalLogger.w(TAG, "Failed to verify float write for key: " + key + ", attempting commit");
                editor = prefs.edit();
                editor.putFloat(key, value);
                if (!editor.commit()) {
                    InternalLogger.e(TAG, "Failed to commit float preference for key: " + key);
                }
            }
            return null;
        }, null);
    }

    /**
     * Thread-safe key removal with error recovery
     * @param key Preference key to remove
     */
    public static void removeKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            InternalLogger.w(TAG, "removeKey() called with null or empty key, ignoring");
            return;
        }
        
        executeWithTimeout(prefs -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(key);
            
            // Use apply() for better performance and ANR prevention
            editor.apply();
            
            // Verify removal was successful
            if (prefs.contains(key)) {
                InternalLogger.w(TAG, "Failed to verify key removal for: " + key + ", attempting commit");
                editor = prefs.edit();
                editor.remove(key);
                if (!editor.commit()) {
                    InternalLogger.e(TAG, "Failed to commit key removal for: " + key);
                }
            }
            return null;
        }, null);
    }
    
    /**
     * Thread-safe batch rotation settings save with validation
     * @param rotation Rotation angle (validated to be 0, 90, 180, or 270)
     * @param isFlipped Whether image is flipped
     * @param isMirrored Whether image is mirrored
     */
    public static void saveRotationSettings(int rotation, boolean isFlipped, boolean isMirrored) {
        // Validate rotation value
        if (rotation != 0 && rotation != 90 && rotation != 180 && rotation != 270) {
            InternalLogger.w(TAG, "Invalid rotation value: " + rotation + ", using 0");
            rotation = 0;
        }
        
        // Use batch operation for consistency
        final int finalRotation = rotation;
        executeWithTimeout(prefs -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY.IS_ROTATED, finalRotation);
            editor.putBoolean(KEY.IS_FLIPPED, isFlipped);
            editor.putBoolean(KEY.IS_MIRRORED, isMirrored);
            
            // Use apply() for better performance
            editor.apply();
            
            // Verify batch write was successful
            if (prefs.getInt(KEY.IS_ROTATED, -1) != finalRotation ||
                prefs.getBoolean(KEY.IS_FLIPPED, !isFlipped) != isFlipped ||
                prefs.getBoolean(KEY.IS_MIRRORED, !isMirrored) != isMirrored) {
                
                InternalLogger.w(TAG, "Failed to verify rotation settings write, attempting commit");
                editor = prefs.edit();
                editor.putInt(KEY.IS_ROTATED, finalRotation);
                editor.putBoolean(KEY.IS_FLIPPED, isFlipped);
                editor.putBoolean(KEY.IS_MIRRORED, isMirrored);
                
                if (!editor.commit()) {
                    InternalLogger.e(TAG, "Failed to commit rotation settings");
                }
            }
            return null;
        }, null);
    }
    
    /**
     * Thread-safe rotation getter with validation
     * @return Rotation angle (0, 90, 180, or 270)
     */
    public static int getRotation() {
        int rotation = getInt(KEY.IS_ROTATED, 0);
        
        // Validate retrieved value
        if (rotation != 0 && rotation != 90 && rotation != 180 && rotation != 270) {
            InternalLogger.w(TAG, "Invalid stored rotation value: " + rotation + ", returning 0");
            return 0;
        }
        
        return rotation;
    }
    
    /**
     * Thread-safe flip status getter
     * @return true if image is flipped, false otherwise
     */
    public static boolean isFlipped() {
        return getBool(KEY.IS_FLIPPED, false);
    }
    
    /**
     * Thread-safe mirror status getter
     * @return true if image is mirrored, false otherwise
     */
    public static boolean isMirrored() {
        return getBool(KEY.IS_MIRRORED, false);
    }
    
    /**
     * Thread-safe rotation settings reset
     */
    public static void resetRotationSettings() {
        saveRotationSettings(0, false, false);
    }
    
    /**
     * Get current restart count for ANR tracking
     * @return Number of app restarts
     */
    public static int getRestartCount() {
        return getInt(KEY.RESTART_COUNT, 0);
    }
    
    /**
     * Increment restart count for ANR tracking
     */
    public static void incrementRestartCount() {
        int currentCount = getRestartCount();
        setInt(KEY.RESTART_COUNT, currentCount + 1);
        setLong(KEY.LAST_RESTART_TIME, System.currentTimeMillis());
    }
    
    /**
     * Reset restart count (called after successful stable operation)
     */
    public static void resetRestartCount() {
        setInt(KEY.RESTART_COUNT, 0);
        removeKey(KEY.LAST_RESTART_TIME);
    }
    
    /**
     * Check if app is in ANR recovery mode based on restart count
     * @return true if multiple recent restarts detected
     */
    public static boolean isInAnrRecoveryMode() {
        int restartCount = getRestartCount();
        long lastRestartTime = getLong(KEY.LAST_RESTART_TIME, 0);
        
        // If more than 3 restarts in the last 5 minutes, consider ANR mode
        if (restartCount >= 3 && lastRestartTime > 0) {
            long timeSinceLastRestart = System.currentTimeMillis() - lastRestartTime;
            return timeSinceLastRestart < (5 * 60 * 1000); // 5 minutes
        }
        
        return false;
    }

}
