package com.checkmate.android.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.checkmate.android.R;

import java.lang.reflect.Method;

public class DeviceUtils {
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) {
            return false;
        }
        boolean isConnected = false;
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        } catch (Exception e) {
            isConnected = false;
        }
        if (!isConnected) {
            try {
//                MessageUtil.showToast(context, R.string.msg_error_network);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return isConnected;
    }

    public static boolean isCellularAvailable(Context context) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//
//        } else {
        TelephonyManager telMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telMgr.getSimState();
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT:
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
            case TelephonyManager.SIM_STATE_UNKNOWN:
                return false;
            case TelephonyManager.SIM_STATE_READY:
                break;
        }
//        }
        boolean mobileDataEnabled = false; // Assume disabled
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            Class cmClass = Class.forName(cm.getClass().getName());
            Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true); // Make the method callable
            // get the setting for "mobile data"
            mobileDataEnabled = (Boolean) method.invoke(cm);
        } catch (Exception e) {
            // Some problem accessible private API
            // TODO do whatever error handling you want here
        }
        return mobileDataEnabled;
    }
}
