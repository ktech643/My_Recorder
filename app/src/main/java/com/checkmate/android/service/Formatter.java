package com.checkmate.android.service;

import java.util.Locale;

public final class Formatter {
    private Locale mLocale;

    public Formatter(Locale locale) {
        mLocale = locale;
    }

    public String timeToString(long time) {
        long ss = time % 60;
        long mm = (time / 60) % 60;
        long hh = time / 3600;
        return String.format(mLocale, "%02d:%02d:%02d", hh, mm, ss);
    }

    public String trafficToString(long bytes) {
        if (bytes < 1024) {
            // B
            return String.format(mLocale, "%dB", bytes);

        } else if (bytes < 1024 * 1024) {
            // KB
            return String.format(mLocale, "%.1fKB", (double) bytes / 1024);

        } else if (bytes < 1024 * 1024 * 1024) {
            // MB
            return String.format(mLocale, "%.1fMB", (double) bytes / (1024 * 1024));
        } else {
            // GB
            return String.format(mLocale, "%.1fGB", (double) bytes / (1024 * 1024 * 1024));
        }
    }

    public String bandwidthToString(long bps) {
        if (bps < 1000) {
            // bps
            return String.format(mLocale, "%dbps", bps);

        } else if (bps < 1000 * 1000) {
            // Kbps
            return String.format(mLocale, "%.1fKbps", (double) bps / 1000);

        } else if (bps < 1000 * 1000 * 1000) {
            // Mbps
            return String.format(mLocale, "%.1fMbps", (double) bps / (1000 * 1000));
        } else {
            // Gbps
            return String.format(mLocale, "%.1fGbps", (double) bps / (1000 * 1000 * 1000));
        }
    }

}
