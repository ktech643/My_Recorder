package com.checkmate.android.util;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import com.checkmate.android.R;

import java.util.Locale;

public final class Formatter {
    private Locale mLocale;

    private String mFmtFps;

    public Formatter(Context context) {
        final Configuration configuration = context.getResources().getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mLocale = configuration.getLocales().get(0);
        } else {
            mLocale = configuration.locale;
        }
        mFmtFps = context.getString(R.string.stats_fps);
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
            return String.format(mLocale, "%4dB", bytes);

        } else if (bytes < 1024 * 1024) {
            // KB
            return String.format(mLocale, "%3.1fKB", (double) bytes / 1024);

        } else if (bytes < 1024 * 1024 * 1024) {
            // MB
            return String.format(mLocale, "%3.1fMB", (double) bytes / (1024 * 1024));
        } else {
            // GB
            return String.format(mLocale, "%3.1fGB", (double) bytes / (1024 * 1024 * 1024));
        }
    }

    public String bandwidthToString(long bps) {
        if (bps < 1000) {
            // bps
            return String.format(mLocale, "%4dbps", bps);

        } else if (bps < 1000 * 1000) {
            // Kbps
            return String.format(mLocale, "%3.1fKbps", (double) bps / 1000);

        } else if (bps < 1000 * 1000 * 1000) {
            // Mbps
            return String.format(mLocale, "%3.1fMbps", (double) bps / (1000 * 1000));
        } else {
            // Gbps
            return String.format(mLocale, "%3.1fGbps", (double) bps / (1000 * 1000 * 1000));
        }
    }

    public String fpsToString(double fpsValue) {
        final int fps = (int) Math.ceil(fpsValue);
        return fps > 0 ? String.format(mFmtFps, fps) : String.format(mFmtFps, 0);
    }

}
