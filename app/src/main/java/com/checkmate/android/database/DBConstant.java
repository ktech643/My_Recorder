package com.checkmate.android.database;

public class DBConstant {
    /*
     * Common
     */
    public static String SPLIT_STRING = "#";

    /* Related DB */
    public static final String DATABASE_NAME = "db_vcsrecorder";
    public static final int DATABASE_VERSION = 1;


    public static final String CREATE_DB_SQL_PREFIX = "CREATE TABLE IF NOT EXISTS ";
    public static final String DELETE_DB_SQL_PREFIX = "DROP TABLE IF EXISTS ";

    /*
     *  DB Table cameras
     */
    public static final String ID = "id";

    public static final String TBL_CAMERAS = "cameras";

    public static final String CAM_NAME = "camera_name";
    public static final String CAM_URL = "host_url";
    public static final String CAM_USERNAME = "username";
    public static final String CAM_PASSWORD = "password";
    public static final String CAM_PORT = "port";
    public static final String CAM_URI = "uri";
    public static final String CAM_WIFI_SSID = "wifi_ssid";
    public static final String CAM_RTSP_TYPE = "rtsp_type";
    public static final String CAM_WIFI_IN = "wifi_in";
    public static final String CAM_WIFI_OUT = "wifi_out";
    public static final String CAM_FULL_ADDRESS = "use_full_address";
    public static final String CAM_WIFI_PASSWORD = "wifi_password";
    public static final String CAM_WIFI_TYPE = "CAM_WIFI_TYPE";

}
