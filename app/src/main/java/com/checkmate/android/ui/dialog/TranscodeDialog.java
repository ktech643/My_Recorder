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

    onSaveListener listener;

    Context context;

    ViewGroup ly_box;

    EditText edt_bitrate;

    EditText edt_framerate;

    CheckBox chk_overlay;

    CheckBox chk_box;

    EditText edt_font_color;

    EditText edt_width;

    EditText edt_height;

    CheckBox chk_audio;

    CheckBox chk_mp4_audio;

    EditText edt_color;

    EditText edt_font;

    EditText edt_text;

    EditText edt_x0;

    EditText edt_y0;

    EditText edt_font_size;

    CheckBox chk_use_mic;

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
        setContentView(R.layout.dialog_transcode);
        setCancelable(false);

        ly_box = findViewById(R.id.ly_box);
        edt_bitrate = findViewById(R.id.edt_bitrate);
        edt_framerate = findViewById(R.id.edt_framerate);
        chk_overlay = findViewById(R.id.chk_overlay);
        chk_box = findViewById(R.id.chk_box);
        edt_font_color = findViewById(R.id.edt_font_color);
        edt_width = findViewById(R.id.edt_width);
        edt_height = findViewById(R.id.edt_height);
        chk_audio = findViewById(R.id.chk_audio);
        chk_mp4_audio = findViewById(R.id.chk_mp4_audio);
        edt_color = findViewById(R.id.edt_color);
        edt_font = findViewById(R.id.edt_font);
        edt_text = findViewById(R.id.edt_text);
        edt_x0 = findViewById(R.id.edt_x0);
        edt_y0 = findViewById(R.id.edt_y0);
        edt_font_size = findViewById(R.id.edt_font_size);
        chk_use_mic = findViewById(R.id.chk_use_mic);

        boolean box_enabled = AppPreference.getBool(AppPreference.KEY.TRANS_BOX_ENABLE, false);
        chk_box.setChecked(box_enabled);
        chk_box.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppPreference.setBool(AppPreference.KEY.TRANS_BOX_ENABLE, isChecked);
            if (!isChecked) {
                ly_box.setVisibility(View.GONE);
            } else {
                ly_box.setVisibility(View.VISIBLE);
            }
        });

        edt_width.setText(String.valueOf(AppPreference.getInt(AppPreference.KEY.TRANS_WIDTH, 640)));
        edt_height.setText(String.valueOf(AppPreference.getInt(AppPreference.KEY.TRANS_HEIGHT, 360)));
        boolean audio_push = AppPreference.getBool(AppPreference.KEY.TRANS_AUDIO_PUSH, false);
        chk_audio.setChecked(audio_push);
        chk_audio.setOnCheckedChangeListener((buttonView, isChecked) -> AppPreference.setBool(AppPreference.KEY.TRANS_AUDIO_PUSH, isChecked));
        boolean audio_mp4 = AppPreference.getBool(AppPreference.KEY.TRANS_AUDIO_MP4, false);
        chk_mp4_audio.setChecked(audio_mp4);
        chk_mp4_audio.setOnCheckedChangeListener((buttonView, isChecked) -> AppPreference.setBool(AppPreference.KEY.TRANS_AUDIO_MP4, isChecked));
        boolean enable_overlay = AppPreference.getBool(AppPreference.KEY.TRANS_OVERLAY, false);
        chk_overlay.setChecked(enable_overlay);
        chk_overlay.setOnCheckedChangeListener((buttonView, isChecked) -> AppPreference.setBool(AppPreference.KEY.TRANS_OVERLAY, isChecked));

        edt_color.setText(AppPreference.getStr(AppPreference.KEY.TRANS_BOX_COLOR, "white"));
        String path = AppPreference.getStr(AppPreference.KEY.TRNAS_BOX_FONT, "/storage/emulated/0/Fonts/arial.ttf");
        if (TextUtils.isEmpty(path)) {
            path = "/storage/emulated/0/Fonts/arial.ttf";
        }
        edt_font.setText(path);
        edt_text.setText(AppPreference.getStr(AppPreference.KEY.TRANS_BOX_FORMAT, "'%{localtime\\:}%X'"));
        edt_x0.setText(String.valueOf(AppPreference.getInt(AppPreference.KEY.TRANS_BOX_X0, 0)));
        edt_y0.setText(String.valueOf(AppPreference.getInt(AppPreference.KEY.TRANS_BOX_Y0, 0)));
        edt_font_size.setText(String.valueOf(AppPreference.getInt(AppPreference.KEY.TRANS_BOX_FONT_SIZE, 28)));
        chk_use_mic.setChecked(AppPreference.getBool(AppPreference.KEY.TRANS_BOX_USE_MIC, false));
        chk_use_mic.setOnCheckedChangeListener((buttonView, isChecked) -> AppPreference.setBool(AppPreference.KEY.TRANS_BOX_USE_MIC, isChecked));
        edt_font_color.setText(AppPreference.getStr(AppPreference.KEY.TRNAS_BOX_FONT_COLOR, "white"));
        edt_bitrate.setText(String.valueOf(AppPreference.getInt(AppPreference.KEY.TRANS_BITRATE, 200000)));
        edt_framerate.setText(String.valueOf(AppPreference.getInt(AppPreference.KEY.TRANS_FRAMERATE, 15)));
    }

    public void OnClick(View view) {
        switch (view.getId()) {
            case R.id.btn_save:
                onSave();
                break;
            case R.id.btn_close:
                if (listener != null) {
                    listener.onResult(false);
                }
                onClose();
                break;
        }
    }

    void onSave() {
        String width = edt_width.getText().toString().trim();
        if (TextUtils.isEmpty(width)) {
            width = "0";
        }
        AppPreference.setInt(AppPreference.KEY.TRANS_WIDTH, Integer.parseInt(width));
        String height = edt_height.getText().toString().trim();
        if (TextUtils.isEmpty(height)) {
            height = "0";
        }
        AppPreference.setInt(AppPreference.KEY.TRANS_HEIGHT, Integer.parseInt(height));
        AppPreference.setStr(AppPreference.KEY.TRANS_BOX_COLOR, edt_color.getText().toString().trim());
        AppPreference.setStr(AppPreference.KEY.TRNAS_BOX_FONT, edt_font.getText().toString().trim());
        AppPreference.setStr(AppPreference.KEY.TRANS_BOX_FORMAT, edt_text.getText().toString().trim());
        String x0 = edt_x0.getText().toString().trim();
        if (TextUtils.isEmpty(x0)) {
            x0 = "0";
        }
        AppPreference.setInt(AppPreference.KEY.TRANS_BOX_X0, Integer.parseInt(x0));
        String bitrate = edt_bitrate.getText().toString().trim();
        if (TextUtils.isEmpty(bitrate)) {
            bitrate = "0";
        }
        AppPreference.setInt(AppPreference.KEY.TRANS_BITRATE, Integer.parseInt(bitrate));
        String framerate = edt_framerate.getText().toString().trim();
        if (TextUtils.isEmpty(framerate)) {
            framerate = "0";
        }
        AppPreference.setInt(AppPreference.KEY.TRANS_FRAMERATE, Integer.parseInt(framerate));
        String y0 = edt_y0.getText().toString().trim();
        if (TextUtils.isEmpty(y0)) {
            y0 = "0";
        }
        AppPreference.setInt(AppPreference.KEY.TRANS_BOX_Y0, Integer.parseInt(y0));
        AppPreference.setStr(AppPreference.KEY.TRNAS_BOX_FONT_COLOR, edt_font_color.getText().toString().trim());
        String font_size = edt_font_size.getText().toString().trim();
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
