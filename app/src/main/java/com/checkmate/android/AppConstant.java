package com.checkmate.android;

public class AppConstant {

    // Fragments
    public static final int SW_FRAGMENT_LIVE = 0;
    public static final int SW_FRAGMENT_PLAYBACK = 1;
    public static final int SW_FRAGMENT_STREAMING = 2;
    public static final int SW_FRAGMENT_SETTINGS = 3;
    public static final int SW_FRAGMENT_HIDE = 4;

    public static final String REAR_CAMERA = "0";
    public static final String FRONT_CAMERA = "1";
    public static final String USB_CAMERA = "2";
    public static final String SCREEN_CAST = "3";
    public static final String AUDIO_ONLY = "4";
    public static final String CertPassword = "1Q87$%#kj762Op(*C4sWad3ZmF";
    public static String expire_date = "2020-09-30";
    public static final int is_rotated_0 = 0;
    public static final int is_rotated_90 = 1;
    public static final int is_rotated_180 = 2;
    public static final int is_rotated_270 = 3;

    //    public static String STREAM_USERNAME = "bNh0PJ7j";
//    public static String STREAM_PASSWORD = "2rcLDsiv";
    public static String STREAM_USERNAME = "";
    public static String STREAM_PASSWORD = "";
    public static String STREAM_CHANNEL = "y4gsCmmR";
    //    public static String STREAM_BASE = "rtsp://testcloud.vcsrelay.com:8554";
    public static String STREAM_BASE = "rtsp://testcloud2.vcsrelay.com:8554";

    final public static int RTSP_UDP = 0;
    final public static int RTSP_TCP = 1;

    final public static String LANDSCAPE = "landscape";
    final public static String PORTRAIT = "portrait";

    public static boolean is_show_log = false;
    public static boolean is_library_use = true;

    public static String out_demo = "rtsp://admin:admin@testcloud2.vcsrelay.com:8554/ch101";
    public static String in_demo = "rtsp://76.239.142.89:7099/0";
    public static String rtsp_demo = "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_175k.mov";


    // constants for encryption/decription
    public static final String FILE_NAME = "encrypt.mp4";
    public static final String FILE_NAME_DE = "decrypt.mp4";
    public static final String TEMP_FILE_NAME = "temp";
    public static final String FILE_EXT = ".mp4";
    public static final String DIR_NAME = "Video";
    public static final int OUTPUT_KEY_LENGTH = 256;
    // Algorithm
    public static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    public static final String KEY_SPEC_ALGORITHM = "AES";
    public static final String PROVIDER = "BC";

    public static final String BETA_SERIAL = "481457350647";
    public static final String BETA_UPDATE = "http://mobileact.t3ther.com/Admin/UpdatesBeta";

    public static final int INVITE_PENDING = 0;
    public static final int INVITE_ACCEPTED = 1;
    public static final int INVITE_REJECTED = 2;

    public static final int GENERAL_PIN = 0;
    public static final int GENERAL_SPLIT = 1;
    public static final int GENERAL_VIDEO_BITRATE = 2;
    public static final int GENERAL_VIDEO_KEYFRAME = 3;
    public static final int GENERAL_STREAM_BITRATE = 4;
    public static final int GENERAL_STREAM_KEYFRAME = 5;
    public static final int GENERA_CAST_BITRATE = 6;
    public static final int GENERAL_CAST_FRAME = 7;
    public static final int GENERAL_USB_MIN_FPS = 8;
    public static final int GENERAL_USB_MAX_FPS = 9;

    public static final int QUALITY_HIGH = 0;
    public static final int QUALITY_MEDIUM = 1;
    public static final int QUALITY_LOW = 2;
    public static final int QUALITY_SUPER_LOW = 3;
    public static final int QUALITY_USB = 4;
    public static final int QUALITY_CUSTOM = 5;

    public static final int DEVICE_TYPE_ANDROID = 0;
    public static final int DEVICE_TYPE_WIFI = 1;

    public static final int DEVICE_TYPE_USB = 2;
    public static final int DEVICE_TYPE_SCREENCAST = 3;

    public static final int WIFI_TYPE_VCS = 0;
    public static final int WIFI_TYPE_LAWMATE = 1;
    public static final int WIFI_TYPE_ATN = 2;
    public static final int WIFI_TYPE_GENERIC = 3;
    public static final int WIFI_TYPE_RTSP = 4;
    public static final int WIFI_TYPE_NONE = -1;
}
