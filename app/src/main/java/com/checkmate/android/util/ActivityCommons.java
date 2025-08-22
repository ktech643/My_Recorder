package com.checkmate.android.util;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.camera2.CaptureRequest;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import com.checkmate.android.R;
import com.checkmate.android.ui.activity.BaseActivity;
import com.checkmate.android.ui.dialog.MyProgressDialog;
import com.checkmate.android.ui.view.AspectFrameLayout;
import com.wmspanel.libstream.FocusMode;
import com.wmspanel.libstream.Streamer;
import com.wmspanel.libstream.StreamerGL;
import com.wmspanel.libstream.VideoConfig;

import java.util.ArrayList;
import java.util.List;

public abstract class ActivityCommons extends BaseActivity {

    public MyProgressDialog dlg_progress;

    protected AspectFrameLayout mPreviewFrame;
    protected ImageButton mFlashButton;
    protected ImageButton mFlipButton;
    protected ImageButton mMuteButton;
    protected ImageButton mSettingsButton;
    protected ImageButton mShootButton;

    protected Button mCaptureButton;

    protected TextView mFpsView;
    protected TextView mZoomRatio;
    protected TextView mBroadcastTime;

    protected List<TextView> mConnectionStatus = new ArrayList<>();
    protected List<TextView> mConnectionName = new ArrayList<>();

    protected AudioLevelMeter mVuMeter;
    protected LinearLayout mRecIndicator;

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        bind();
    }

    protected void bind() {
        mPreviewFrame = findViewById(R.id.preview_afl);

        mFlashButton = findViewById(R.id.btn_flash);
        mFlashButton.setOnClickListener(v -> flashClick());

        mFlipButton = findViewById(R.id.btn_flip);
        mFlipButton.setOnClickListener(v -> flipClick());

        mMuteButton = findViewById(R.id.btn_mute);
        mMuteButton.setOnClickListener(v -> muteClick());

        mSettingsButton = findViewById(R.id.btn_settings);
        mSettingsButton.setOnClickListener(v -> settingsClick());

        mShootButton = findViewById(R.id.btn_shoot);
        mShootButton.setOnClickListener(v -> shootClick());

        mCaptureButton = findViewById(R.id.btn_capture);
        mCaptureButton.setOnClickListener(v -> broadcastClick());
        mCaptureButton.setOnLongClickListener(v -> pauseBroadcastClick());

        mFpsView = findViewById(R.id.fps);
        mZoomRatio = findViewById(R.id.zoom_ratio);
        mBroadcastTime = findViewById(R.id.broadcast_time);

        mConnectionStatus.add(0, findViewById(R.id.connection_status));
        mConnectionStatus.add(1, findViewById(R.id.connection_status1));
        mConnectionStatus.add(2, findViewById(R.id.connection_status2));

        mConnectionName.add(0, findViewById(R.id.connection_name));
        mConnectionName.add(1, findViewById(R.id.connection_name1));
        mConnectionName.add(2, findViewById(R.id.connection_name2));

        mVuMeter = findViewById((R.id.audio_level_meter));
        mRecIndicator = findViewById(R.id.rec_indicator);
    }

    protected void flashClick() {
        throw new UnsupportedOperationException();
    }

    protected void flipClick() {
        throw new UnsupportedOperationException();
    }

    protected void muteClick() {
        throw new UnsupportedOperationException();
    }

    protected void settingsClick() {

    }

    protected void shootClick() {
        throw new UnsupportedOperationException();
    }

    protected void broadcastClick() {
        throw new UnsupportedOperationException();
    }

    protected boolean pauseBroadcastClick() {
        return false;
    }

    protected Formatter mFormatter;

    protected FocusMode mFocusMode = new FocusMode();
    protected GestureDetectorCompat mDetector;

    protected int mVolumeKeysAction = SettingsUtils.ACTION_START_STOP;

    protected class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final String DEBUG_TAG = "Gestures";

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            //Log.d(DEBUG_TAG, "onDoubleTap: " + event.toString());
            if (SettingsUtils.radioMode(ActivityCommons.this)) {
                return false;
            }
            mFocusMode.focusMode16 = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mFocusMode.focusMode = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO;
            }
            focus(getString(R.string.new_focus_continuous_video));
            return true;
        }

        @Override
        public void onLongPress(MotionEvent event) {
            //Log.d(DEBUG_TAG, "onLongPress: " + event.toString());
            if (SettingsUtils.radioMode(ActivityCommons.this)) {
                return;
            }
            mFocusMode.focusMode16 = Camera.Parameters.FOCUS_MODE_INFINITY;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mFocusMode.focusMode = CaptureRequest.CONTROL_AF_MODE_OFF;
                mFocusMode.focusDistance = 0.0f; // A value of 0.0f means infinity focus.
            }
            focus(getString(R.string.new_focus_infinity));
        }
    }

    protected void focus(String message) {
        showToast(message);
    }

    protected Toast mToast;

    protected void showToast(String text, int length) {
        if (!isFinishing()) {
            dismissToast();
            mToast = Toast.makeText(this, text, length);
            mToast.show();
        }
    }

    protected void showToast(String text) {
        showToast(text, Toast.LENGTH_SHORT);
    }

    protected void showToast(@StringRes int resId) {
        showToast(getString(resId), Toast.LENGTH_SHORT);
    }

    protected void dismissToast() {
        if (mToast != null) {
            mToast.cancel();
            mToast = null;
        }
    }

    // https://developer.android.com/training/monitoring-device-state/connectivity-monitoring.html
    protected boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    protected boolean isPortrait() {
        boolean portrait;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !isInMultiWindowMode()) {
            final int orientation = getResources().getConfiguration().orientation;
            portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
        } else {
            final Context app = getApplicationContext();
            WindowManager manager = (WindowManager) app.getSystemService(Context.WINDOW_SERVICE);
            Display display = manager.getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);
            int width = metrics.widthPixels;
            int height = metrics.heightPixels;
            portrait = height >= width;
        }
        return portrait;
    }

    protected int videoOrientation() {
        if (isPortrait()) {
            return StreamerGL.ORIENTATIONS.PORTRAIT;
        } else {
            return StreamerGL.ORIENTATIONS.LANDSCAPE;
        }
    }

    protected int displayRotation() {
        return getWindowManager().getDefaultDisplay().getRotation();
    }

    protected void updatePreviewRatio(AspectFrameLayout frame, Streamer.Size size) {
        if (frame == null || size == null) {
            return;
        }

        if (!isPortrait()) {
            frame.setAspectRatio(size.getRatio());
        } else {
            // Vertical video, so reverse aspect ratio
            frame.setAspectRatio(size.getVerticalRatio());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.v(TAG, "onCreate()");

//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
 //       getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        dlg_progress = new MyProgressDialog(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean consumeEvent = false;
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (mVolumeKeysAction == SettingsUtils.ACTION_DO_NOTHING) {
                    return super.onKeyDown(keyCode, event);
                }
                break;
            // FALLTHROUGH
            case KeyEvent.KEYCODE_CAMERA:
                if (event.getRepeatCount() == 0) {
                    broadcastClick();
                }
                consumeEvent = true;
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS: // media codes are for "selfie sticks" buttons
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_STOP:
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                if (am != null && am.isWiredHeadsetOn()) {
                    if (event.getRepeatCount() == 0) {
                        broadcastClick();
                    }
                    consumeEvent = true;
                }
                break;
            default:
                break;
        }
        return consumeEvent || super.onKeyDown(keyCode, event);
    }

    protected void enableSettingsButton(ImageButton button, boolean enable) {
        button.setEnabled(enable);
        button.setAlpha(enable ? 0.3f : 0.1f);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);

    }

    // Show video info to user
    protected void showVideoInfo(VideoConfig videoConfig, boolean camera2) {
        int resId = camera2 ? R.string.video_info_camera2 : R.string.video_info;
        String content = String.format(getString(resId),
                SettingsUtils.codecDisplayName(videoConfig.type),
                videoConfig.videoSize);

        if (DeepLink.getInstance().hasImportedSettings()) {
            String import_status = DeepLink.getInstance().getImportResultBody(this, false);
            if (import_status != null) {
                content = import_status + "\n\n" + content;
            }
            DeepLink.getInstance().reset();
        }
        showToast(content, Toast.LENGTH_LONG);
    }

    protected void updatePaused(boolean paused, boolean broadcasting) {
        enableSettingsButton(mFlashButton, !paused);
        enableSettingsButton(mFlipButton, !paused);
        enableSettingsButton(mMuteButton, !paused);
        enableSettingsButton(mSettingsButton, !broadcasting);
        enableSettingsButton(mShootButton, !paused);
        if (!broadcasting) {
            mCaptureButton.setBackgroundResource(R.drawable.btn_start);
        } else {
            mCaptureButton.setBackgroundResource(paused ? R.drawable.button_resume : R.drawable.btn_stop);
        }
    }

}
