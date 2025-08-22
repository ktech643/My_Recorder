package com.checkmate.android;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.checkmate.android.service.MyAccessibilityService;
import com.checkmate.android.ui.activity.SplashActivity;

public class RestartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restart);
         ComponentName componentName = new ComponentName(this, MyAccessibilityService.class);
         getPackageManager().setComponentEnabledSetting(
             componentName,
             PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
             PackageManager.DONT_KILL_APP
         );

        // Immediately push the app to the background
        Intent intent = new Intent(this, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Finish this activity so it's not visible or in the back stack
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                moveTaskToBack(true);
                finish();
            }
        },22000);
    }
}