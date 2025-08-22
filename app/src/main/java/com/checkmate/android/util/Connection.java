package com.checkmate.android.util;

import com.orm.SugarRecord;
import com.wmspanel.libstream.Streamer;

public class Connection extends SugarRecord {

    public static Connection m_instance = null;

    public String name;
    public String url;
    public int mode;

    // rtsp and rtmp options
    public String username;
    public String password;
    public int auth;

    // srt options
    public String passphrase; // SRTO_PASSPHRASE
    public int pbkeylen; // SRTO_PBKEYLEN
    public int latency; // SRTO_LATENCY
    public int maxbw; // SRTO_MAXBW
    public String streamid; // SRTO_STREAMID


    public int ristProfile; // RIST profile
    public Boolean active;

    public Connection() {
    }

    public Connection(String name, String url) {
        this.name = name;
        this.url = url;
        this.active = false;
        this.mode = SettingsUtils.streamerMode().ordinal();
        this.auth = Streamer.AUTH.DEFAULT.ordinal();
        this.pbkeylen = 16;
    }
}
