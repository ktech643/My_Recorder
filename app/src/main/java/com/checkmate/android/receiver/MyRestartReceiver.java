package com.checkmate.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.checkmate.android.AppPreference;
import com.checkmate.android.ui.activity.SplashActivity;

public class MyRestartReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
       Intent i = new Intent(context, SplashActivity.class);
       i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
               | Intent.FLAG_ACTIVITY_NEW_TASK
               | Intent.FLAG_ACTIVITY_CLEAR_TASK);
       context.startActivity(i);

    }
}