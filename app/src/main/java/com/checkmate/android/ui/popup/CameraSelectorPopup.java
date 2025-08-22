package com.checkmate.android.ui.popup;

import android.content.Context;

import androidx.annotation.NonNull;

import com.checkmate.android.AppPreference;
import com.checkmate.android.R;

public class CameraSelectorPopup extends BaseSpinnerPopup {
    public CameraSelectorPopup(@NonNull Context context) {
        super(context);
        initOptions();
    }

    private void initOptions() {
        options.clear();
        if (AppPreference.getBool(AppPreference.KEY.CAM_REAR_FACING, true)) {
            options.add(new SpinnerOption("0", getContext().getString(R.string.rear_camera), R.drawable.ic_camera_rear));
        }
        if (AppPreference.getBool(AppPreference.KEY.CAM_FRONT_FACING, true)) {
            options.add(new SpinnerOption("1", getContext().getString(R.string.front_camera), R.drawable.ic_camera_front));
        }
        if (AppPreference.getBool(AppPreference.KEY.CAM_USB, true)) {
            options.add(new SpinnerOption("2", getContext().getString(R.string.usb_camera), R.drawable.ic_camera_front));
        }
        if (AppPreference.getBool(AppPreference.KEY.CAM_CAST, true)) {
            options.add(new SpinnerOption("3", getContext().getString(R.string.screen_cast), R.drawable.ic_cast));
        }
        if (AppPreference.getBool(AppPreference.KEY.AUDIO_ONLY, true)) {
            options.add(new SpinnerOption("4", getContext().getString(R.string.audio_only), R.drawable.ic_audio));
        }
    }
}