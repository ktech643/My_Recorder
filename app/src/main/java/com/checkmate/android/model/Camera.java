package com.checkmate.android.model;

import android.text.TextUtils;

import com.checkmate.android.AppConstant;
import com.checkmate.android.util.CommonUtil;

public class Camera {
    public enum TYPE {
        REAR_CAMERA,
        FRONT_CAMERA,
        WIFI_CAMERA,
        USB_CAMERA,
        SCREEN_CAST,
        AUDIO_ONLY
    }

    public TYPE camera_type;
    public String camera_name;
    public boolean camera_enable;
    public int camera_wifi_type = AppConstant.WIFI_TYPE_NONE;

    // rtsp
    public int id = -1;
    public String url;
    public String username;
    public String password;
    public int port;
    public String uri;
    public String wifi_ssid;
    public int rtsp_type = 1;
    public String wifi_in;
    public String wifi_out;
    public boolean use_full_address;
    public String wifi_password;

    public Camera() {
    }

    public Camera(TYPE type, String name, boolean enabled) {
        this.camera_type = type;
        this.camera_name = name;
        this.camera_enable = enabled;
    }

    public Camera(String name, String url, int port, String uri, String username, String password, String wifi_ssid, int rtsp_type, String wifi_in, String wifi_out, boolean use_full_address, String wifi_password) {
        this.camera_name = name;
        this.url = url;
        this.username = username;
        this.password = password;
        this.wifi_ssid = wifi_ssid;
        this.port = port;
        this.uri = uri;
        this.rtsp_type = rtsp_type;
        this.wifi_in = wifi_in;
        this.wifi_out = wifi_out;
        this.use_full_address = use_full_address;
        this.wifi_password = wifi_password;
    }

    public String getFormattedURL() {
        String result = "";
        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            result = String.format("rtsp://%s:%s@%s:%d/%s", username, password, CommonUtil.getDomainIP(url), port, uri);
        } else if (port == 0 || TextUtils.isEmpty(uri)) {

            result = url;
        } else {
            result = String.format("rtsp://%s:%d/%s", CommonUtil.getDomainIP(url), port, uri);
        }

        return result;
    }

    public String getStreamURL() {
        String result = "";
        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            result = String.format("rtsp://%s:%s@%s:%d/%s", username, password, CommonUtil.getDomainIP(url), port, uri);
        } else if (port == 0 || TextUtils.isEmpty(uri)) {

            result = url;
        } else {
            result = String.format("rtsp://%s:%d/%s", CommonUtil.getDomainIP(url), port, uri);
        }

        return result;
    }
}
