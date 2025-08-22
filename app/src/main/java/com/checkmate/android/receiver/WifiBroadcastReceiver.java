package com.checkmate.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;

import com.checkmate.android.AppPreference;
import com.checkmate.android.ui.dialog.CameraDialog;
import com.checkmate.android.util.MainActivity;
import com.checkmate.android.util.MessageUtil;

public class WifiBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION .equals(action)) {
            SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
            if (SupplicantState.isValidState(state)
                    && state == SupplicantState.COMPLETED) {

                boolean connected = checkConnectedToDesiredWifi(context);
                if (!connected) {
                    if (MainActivity.instance != null) {
                        MainActivity.instance.networkChanged();
                    }
                }
            }
        }
    }

    /** Detect you are connected to a specific network. */
    private boolean checkConnectedToDesiredWifi(Context context) {
        boolean connected = false;

        String desiredMacAddress = "router mac address";
        String old_ssid = AppPreference.getStr(AppPreference.KEY.CURRENT_SSID, "");
        if (TextUtils.isEmpty(old_ssid)) {
            return true;
        }

        WifiManager wifiManager =
                (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        WifiInfo wifi = wifiManager.getConnectionInfo();
        if (wifi != null) {
            // get current router Mac address
            String bssid = wifi.getBSSID();
            String ssid = wifi.getSSID();
            connected = old_ssid.equals(bssid);
        }

        return connected;
    }
}