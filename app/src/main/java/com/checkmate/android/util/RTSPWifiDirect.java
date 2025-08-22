package com.checkmate.android.util;

import java.util.Locale;

public class RTSPWifiDirect {
    /**
     * Builds an RTSP URL of the form:
     *   rtsp://[username[:password]@]serverIp[:port]/path
     *
     * Defaults if fields are blank:
     *   - serverIp: 172.20.1.1
     *   - port: 554
     *   - username/password: (omitted if empty)
     *   - path: "abc1234" or some unique device ID
     *
     * @param serverIp  e.g. "172.20.1.1"
     * @param port      e.g. 554
     * @param username  e.g. ""
     * @param password  e.g. ""
     * @param path      e.g. "abc1234"
     * @return          final RTSP URL
     */
    public static String buildLocalRtspUrl(
            String serverIp,
            int port,
            String username,
            String password,
            String path
    ) {
        // Fallback defaults
        if (serverIp == null || serverIp.isEmpty()) {
            serverIp = "172.20.1.1";
        }
        if (port <= 0) {
            port = 554;
        }
        if (path == null || path.isEmpty()) {
            path = "abc1234"; // or however you get your unique phone ID
        }
        if (username == null) username = "";
        if (password == null) password = "";

        // Build optional credentials
        String credentials = "";
        if (!username.isEmpty() || !password.isEmpty()) {
            credentials = username;
            if (!password.isEmpty()) {
                credentials += ":" + password;
            }
            credentials += "@";  // user[:pass]@
        }

        // Build the final string
        // Always include port in the example: rtsp://user[:pass]@serverIp:port/path
        return String.format(
                Locale.US,
                "rtsp://%s%s:%d/%s",
                credentials,   // may be empty
                serverIp,
                port,
                path
        );
    }
}
