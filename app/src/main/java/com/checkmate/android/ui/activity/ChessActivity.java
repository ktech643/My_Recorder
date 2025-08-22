package com.checkmate.android.ui.activity;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import com.checkmate.android.AppPreference;
import com.checkmate.android.R;
import com.checkmate.android.model.GameObjects.Match;
import com.checkmate.android.util.MainActivity;

import java.lang.ref.WeakReference;

public class ChessActivity extends BaseActivity {

    GridView gridView;
    Button button_reset;
    TextView textView;
    Match match;

    public static WeakReference<ChessActivity> instanceRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        boolean isForPlayback = AppPreference.getBool(AppPreference.KEY.IS_FOR_PLAYBACK_LOCATION,false);
        if (isForPlayback) {
            AppPreference.setBool(AppPreference.KEY.IS_FOR_PLAYBACK_LOCATION,false);
            OpenMain();
            return;
        }
        AppPreference.setBool(AppPreference.KEY.CHESS_MODE_PIN, true);
        setContentView(R.layout.activity_chess);
        AppPreference.setBool(AppPreference.KEY.IS_FOR_STORAGE_LOCATION,false);
        AppPreference.setBool(AppPreference.KEY.IS_FOR_PLAYBACK_LOCATION,false);
        instanceRef = new WeakReference<>(this);
        AppPreference.setInt(AppPreference.KEY.TAPPED_NUMBER, -1);

        textView = findViewById(R.id.textView);
        button_reset = findViewById(R.id.button_reset);
        gridView = findViewById(R.id.chessBoard);
        gridView.setHorizontalSpacing(3);
        gridView.setVerticalSpacing(3);
        match = new Match(this, gridView, textView);
        match.start();
        button_reset.setOnClickListener(view -> {
            match.refresh();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
//                    match = new Match(instance, gridView, textView);
//                    match.start();
                }
            }, 300);
//            match = new Match(this, gridView, textView);
//            match.start();
//            ProcessPhoenix.triggerRebirth(this);

        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (instanceRef != null && instanceRef.get() != null) {
            if (instanceRef.get() == this) {
                instanceRef.clear();
                instanceRef = null;
            }
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    public void onBackPressed() {
        return;
    }

    public void OpenMain() {
        MainActivity.is_passed = true;
        MainActivity.ResumePreview();
        finish();
        if (instanceRef != null && instanceRef.get() != null) {
            if (instanceRef.get() == this) {
                instanceRef.clear();
                instanceRef = null;
            }
        }
    }
}