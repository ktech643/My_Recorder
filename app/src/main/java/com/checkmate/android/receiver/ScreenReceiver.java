package com.checkmate.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import com.checkmate.android.AppPreference;
import com.checkmate.android.ui.dialog.CameraDialog;
import com.checkmate.android.util.MainActivity;

public class ScreenReceiver extends BroadcastReceiver {

    public static boolean screenOff;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            screenOff = true;
            Log.e("screen: ", "off");

            if (CameraDialog.instance != null) {
                CameraDialog.instance.dismiss();
            }
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            screenOff = false;
            Log.e("screen: ", "on");

        }
    }

}
