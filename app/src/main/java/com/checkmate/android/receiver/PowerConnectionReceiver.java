package com.checkmate.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.checkmate.android.ui.dialog.CameraDialog;
import com.checkmate.android.ui.fragment.StreamingFragment;
import com.checkmate.android.util.MessageUtil;

public class PowerConnectionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)) {
            if (StreamingFragment.instance != null) {
                StreamingFragment.instance.updateDeviceBattery();
            }
        } else if (intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED)) {
            if (StreamingFragment.instance != null) {
                StreamingFragment.instance.updateDeviceBattery();
            }
        }
    }

}
