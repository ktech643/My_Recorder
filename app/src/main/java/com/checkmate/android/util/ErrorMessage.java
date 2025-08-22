package com.checkmate.android.util;

import android.content.Context;

import com.checkmate.android.R;
import com.wmspanel.libstream.Streamer;

import org.json.JSONObject;

public final class ErrorMessage {

    public static String connectionErrorMsg(final Context ctx,
                                            final Connection connection,
                                            final Streamer.STATUS status,
                                            final JSONObject info) {
        String msg;

        if (status == Streamer.STATUS.CONN_FAIL) {
            msg = String.format(ctx.getString(R.string.connection_status_fail), connection.name);

        } else if (status == Streamer.STATUS.AUTH_FAIL) {
            final String details = info.toString();

            boolean badType = false;
            if (details.contains("authmod=adobe")
                    && connection.auth != Streamer.AUTH.RTMP.ordinal()
                    && connection.auth != Streamer.AUTH.AKAMAI.ordinal()) {
                badType = true;
            } else if (details.contains("authmod=llnw")
                    && connection.auth != Streamer.AUTH.LLNW.ordinal()) {
                badType = true;
            }

            if (badType) {
                msg = String.format(ctx.getString(R.string.connection_status), connection.name, ctx.getString(R.string.unsupported_auth));
            } else {
                msg = String.format(ctx.getString(R.string.connection_status_auth_fail), connection.name);
            }

        } else {
            if (info.length() == 0) {
                msg = String.format(ctx.getString(R.string.connection_status_unknown_fail), connection.name);
            } else {
                msg = String.format(ctx.getString(R.string.connection_status_fail_with_info), connection.name, info.toString());
            }
        }

        return msg;
    }

}
