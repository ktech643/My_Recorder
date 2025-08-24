package com.checkmate.android;

import android.content.SharedPreferences;

public class AppPreference {
    private static SharedPreferences instance = null;

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

    public static void initialize(SharedPreferences pref) {
        instance = pref;
    }

    // check contain
    public static boolean contains(String key) {
        return instance.contains(key);
    }

    // boolean
    public static boolean getBool(String key, boolean def) {
        return instance.getBoolean(key, def);
    }

    public static void setBool(String key, boolean value) {
        SharedPreferences.Editor editor = instance.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    // int
    public static int getInt(String key, int def) {
        return instance.getInt(key, def);
    }

    public static void setInt(String key, int value) {
        SharedPreferences.Editor editor = instance.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    // long
    public static long getLong(String key, long def) {
        return instance.getLong(key, def);
    }

    public static void setLong(String key, long value) {
        SharedPreferences.Editor editor = instance.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    // string
    public static String getStr(String key, String def) {
        return instance.getString(key, def);
    }

    public static void setStr(String key, String value) {
        SharedPreferences.Editor editor = instance.edit();
        editor.putString(key, value);
        editor.apply();
    }

    // remove
    public static void removeKey(String key) {
        SharedPreferences.Editor editor = instance.edit();
        editor.remove(key);
        editor.apply();
    }

}
