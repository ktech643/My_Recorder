package com.checkmate.android.ui.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;

import com.checkmate.android.AppPreference;
import com.checkmate.android.BuildConfig;
import com.checkmate.android.R;
import com.checkmate.android.ui.dialog.MyProgressDialog;
import com.checkmate.android.ui.fragment.LiveFragment;
import com.shasin.notificationbanner.Banner;

public class BaseActivity extends FragmentActivity {

    // UI
    public MyProgressDialog dlg_progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // clear FLAG_TRANSLUCENT_STATUS flag:
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

            // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            // finally change the color
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.blue_dark));
        }

        if (!BuildConfig.DEBUG) {
//            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }

        dlg_progress = new MyProgressDialog(this);
    }


    @Override
    public void startActivity(Intent intent) {
        // TODO Auto-generated method stub
        super.startActivity(intent);
        overridePendingTransition(R.anim.in_left, R.anim.out_left);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        // TODO Auto-generated method stub
        super.startActivityForResult(intent, requestCode);
        overridePendingTransition(R.anim.in_left, R.anim.out_left);
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
    }

    public void myBack() {
        finish();
        overridePendingTransition(R.anim.in_right, R.anim.out_right);
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        if (dlg_progress != null)
            dlg_progress.dismiss();
    }

    @Override
    public void onBackPressed() {
        myBack();
    }

    public void updateSecureFlag() {
        boolean flagSecure = AppPreference.getBool(AppPreference.KEY.SECURE_MULTI_TASK, true);
        Window window = getWindow();
        WindowManager wm = getWindowManager();

        // is change needed?
        int flags = window.getAttributes().flags;
        if (flagSecure && (flags & WindowManager.LayoutParams.FLAG_SECURE) != 0) {
            // already set, change is not needed.
            return;
        } else if (!flagSecure && (flags & WindowManager.LayoutParams.FLAG_SECURE) == 0) {
            // already cleared, change is not needed.
            return;
        }

        // apply (or clear) the FLAG_SECURE flag to/from Activity this Fragment is attached to.
        boolean flagsChanged = false;
        if (flagSecure) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            flagsChanged = true;
        } else {
            // FIXME Do NOT unset FLAG_SECURE flag from Activity's Window if Activity explicitly set it itself.
            // Okay, it is safe to clear FLAG_SECURE flag from Window flags.
            // Activity is (probably) not showing any secure content.
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            flagsChanged = true;
        }

        // Re-apply (re-draw) Window's DecorView so the change to the Window flags will be in place immediately.
        if (flagsChanged && ViewCompat.isAttachedToWindow(window.getDecorView())) {
            // FIXME Removing the View and attaching it back makes visible re-draw on Android 4.x, 5+ is good.
            wm.removeViewImmediate(window.getDecorView());
            wm.addView(window.getDecorView(), window.getAttributes());
        }
    }
}

