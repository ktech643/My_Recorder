package com.checkmate.android.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.checkmate.android.AppConstant;
import com.checkmate.android.AppPreference;
import com.checkmate.android.BuildConfig;
import com.checkmate.android.R;
import com.checkmate.android.database.DBManager;
import com.checkmate.android.databinding.DialogTranscodeBinding;
import com.checkmate.android.model.Camera;
import com.checkmate.android.ui.fragment.SettingsFragment;
import com.checkmate.android.ui.view.MySpinner;
import com.checkmate.android.util.MainActivity;
import com.checkmate.android.util.MessageUtil;

import java.util.List;

public class TranscodeDialog extends Dialog {

    public interface onSaveListener {
        void onResult(boolean is_changed);
    }

    private DialogTranscodeBinding binding;
    onSaveListener listener;
    Context context;

    public static TranscodeDialog instance = null;

    public TranscodeDialog(Context context, Camera camera) {
        super(context);
        // TODO Auto-generated constructor stub
        init(context);
    }

    public TranscodeDialog(Context context, String ssid) {
        super(context);
        // TODO Auto-generated constructor stub
        init(context);
    }

    public TranscodeDialog(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
        init(context);
        instance = this;
    }

    public TranscodeDialog(Context context, int camera_type) {
        super(context);
        // TODO Auto-generated constructor stub
        init(context);
        instance = this;
    }

    public TranscodeDialog(Context context, int camera_type, String ssid) {
        super(context);
        // TODO Auto-generated constructor stub
        init(context);
        instance = this;
    }

    private void init(Context context) {
        this.context = context;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogTranscodeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setCancelable(false);

        // Set up click listeners
        binding.btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSave();
            }
        });

        binding.btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onResult(false);
                }
                onClose();
            }
        });

        boolean box_enabled = AppPreference.getBool(AppPreference.KEY.TRANS_BOX_ENABLE, false);
        binding.chkBox.setChecked(box_enabled);
        binding.chkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppPreference.setBool(AppPreference.KEY.TRANS_BOX_ENABLE, isChecked);
            if (!isChecked) {
                binding.lyBox.setVisibility(View.GONE);
            } else {
                binding.lyBox.setVisibility(View.VISIBLE);
            }
        });

        binding.edtWidth.setText(String.valueOf(AppPreference.getInt(AppPreference.KEY.TRANS_WIDTH, 640)));
        binding.edtHeight.setText(String.valueOf(AppPreference.getInt(AppPreference.KEY.TRANS_HEIGHT, 360)));
        boolean audio_push = AppPreference.getBool(AppPreference.KEY.TRANS_AUDIO_PUSH, false);
        binding.chkAudio.setChecked(audio_push);
        binding.chkAudio.setOnCheckedChangeListener((buttonView, isChecked) -> AppPreference.setBool(AppPreference.KEY.TRANS_AUDIO_PUSH, isChecked));
        boolean audio_mp4 = AppPreference.getBool(AppPreference.KEY.TRANS_AUDIO_MP4, false);
        binding.chkMp4Audio.setChecked(audio_mp4);
        binding.chkMp4Audio.setOnCheckedChangeListener((buttonView, isChecked) -> AppPreference.setBool(AppPreference.KEY.TRANS_AUDIO_MP4, isChecked));
        boolean enable_overlay = AppPreference.getBool(AppPreference.KEY.TRANS_OVERLAY, false);
        binding.chkOverlay.setChecked(enable_overlay);
        binding.chkOverlay.setOnCheckedChangeListener((buttonView, isChecked) -> AppPreference.setBool(AppPreference.KEY.TRANS_OVERLAY, isChecked));

        binding.edtColor.setText(AppPreference.getStr(AppPreference.KEY.TRANS_BOX_COLOR, "white"));
        String path = AppPreference.getStr(AppPreference.KEY.TRNAS_BOX_FONT, "/storage/emulated/0/Fonts/arial.ttf");
        if (TextUtils.isEmpty(path)) {
            path = "/storage/emulated/0/Fonts/arial.ttf";
        }
        binding.edtFont.setText(path);
        binding.edtText.setText(AppPreference.getStr(AppPreference.KEY.TRANS_BOX_FORMAT, "'%{localtime\\:}%X'"));
        binding.edtX0.setText(String.valueOf(AppPreference.getInt(AppPreference.KEY.TRANS_BOX_X0, 0)));
        binding.edtY0.setText(String.valueOf(AppPreference.getInt(AppPreference.KEY.TRANS_BOX_Y0, 0)));
        binding.edtFontSize.setText(String.valueOf(AppPreference.getInt(AppPreference.KEY.TRANS_BOX_FONT_SIZE, 28)));
        binding.chkUseMic.setChecked(AppPreference.getBool(AppPreference.KEY.TRANS_BOX_USE_MIC, false));
        binding.chkUseMic.setOnCheckedChangeListener((buttonView, isChecked) -> AppPreference.setBool(AppPreference.KEY.TRANS_BOX_USE_MIC, isChecked));
        binding.edtFontColor.setText(AppPreference.getStr(AppPreference.KEY.TRNAS_BOX_FONT_COLOR, "white"));
        binding.edtBitrate.setText(String.valueOf(AppPreference.getInt(AppPreference.KEY.TRANS_BITRATE, 200000)));
        binding.edtFramerate.setText(String.valueOf(AppPreference.getInt(AppPreference.KEY.TRANS_FRAMERATE, 15)));
    }

    void onSave() {
        String width = binding.edtWidth.getText().toString().trim();
        if (TextUtils.isEmpty(width)) {
            width = "0";
        }
        AppPreference.setInt(AppPreference.KEY.TRANS_WIDTH, Integer.parseInt(width));
        String height = binding.edtHeight.getText().toString().trim();
        if (TextUtils.isEmpty(height)) {
            height = "0";
        }
        AppPreference.setInt(AppPreference.KEY.TRANS_HEIGHT, Integer.parseInt(height));
        AppPreference.setStr(AppPreference.KEY.TRANS_BOX_COLOR, binding.edtColor.getText().toString().trim());
        AppPreference.setStr(AppPreference.KEY.TRNAS_BOX_FONT, binding.edtFont.getText().toString().trim());
        AppPreference.setStr(AppPreference.KEY.TRANS_BOX_FORMAT, binding.edtText.getText().toString().trim());
        String x0 = binding.edtX0.getText().toString().trim();
        if (TextUtils.isEmpty(x0)) {
            x0 = "0";
        }
        AppPreference.setInt(AppPreference.KEY.TRANS_BOX_X0, Integer.parseInt(x0));
        String bitrate = binding.edtBitrate.getText().toString().trim();
        if (TextUtils.isEmpty(bitrate)) {
            bitrate = "0";
        }
        AppPreference.setInt(AppPreference.KEY.TRANS_BITRATE, Integer.parseInt(bitrate));
        String framerate = binding.edtFramerate.getText().toString().trim();
        if (TextUtils.isEmpty(framerate)) {
            framerate = "0";
        }
        AppPreference.setInt(AppPreference.KEY.TRANS_FRAMERATE, Integer.parseInt(framerate));
        String y0 = binding.edtY0.getText().toString().trim();
        if (TextUtils.isEmpty(y0)) {
            y0 = "0";
        }
        AppPreference.setInt(AppPreference.KEY.TRANS_BOX_Y0, Integer.parseInt(y0));
        AppPreference.setStr(AppPreference.KEY.TRNAS_BOX_FONT_COLOR, binding.edtFontColor.getText().toString().trim());
        String font_size = binding.edtFontSize.getText().toString().trim();
        if (TextUtils.isEmpty(font_size)) {
            font_size = "0";
        }
        AppPreference.setInt(AppPreference.KEY.TRANS_BOX_FONT_SIZE, Integer.parseInt(font_size));
        if (listener != null) {
            listener.onResult(true);
        }
        this.onClose();
    }

    void onClose() {
        instance = null;
        dismiss();
    }

    public void setResultLisetner(onSaveListener listener) {
        this.listener = listener;
    }

    @Override
    public void show() {
        // we are using try - catch in order to prevent crashing issue
        // when the activity is finished but the AsyncTask is still processing
        try {
            super.show();
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }
}
