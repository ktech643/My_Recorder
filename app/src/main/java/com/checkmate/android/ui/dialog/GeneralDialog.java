package com.checkmate.android.ui.dialog;

import static android.view.View.TEXT_ALIGNMENT_CENTER;

import android.app.Dialog;
import android.content.Context;
import android.text.Html;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.checkmate.android.AppConstant;
import com.checkmate.android.R;
import com.checkmate.android.util.MessageUtil;


public class GeneralDialog extends Dialog {

    TextView txt_title;

    TextView txt_desc;

    EditText edt_value;

    Button btn_ok;

    Button btn_close;

    String old_value = "";
    int dialog_type;

    public interface ResultListener {
        void onResult(String result, boolean is_changed);
    }

    public ResultListener resultListener;

    public GeneralDialog(Context context, int theme) {
        super(context, theme);
    }

    public GeneralDialog(Context context) {
        super(context);
        init(context);
    }

    public GeneralDialog(Context context, int title_id, int desc_id, int dialog_type, String value) {
        super(context);
        init(context, title_id, desc_id, dialog_type, value);
    }

    private void init(Context context, int title_id, int desc_id, int dialog_type, String value) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_general);
        setCancelable(false);
                txt_title = findViewById(R.id.txt_title);
        txt_desc = findViewById(R.id.txt_desc);
        edt_value = findViewById(R.id.edt_value);
        btn_ok = findViewById(R.id.btn_ok);
        btn_close = findViewById(R.id.btn_close);

        txt_title.setText(title_id);
        txt_desc.setText(desc_id);
        edt_value.setHint(title_id);
        edt_value.setText(value);
        old_value = value;
        this.dialog_type = dialog_type;
        if (dialog_type == AppConstant.GENERAL_SPLIT) {
            edt_value.setInputType(InputType.TYPE_CLASS_NUMBER);
            edt_value.setKeyListener(DigitsKeyListener.getInstance("1234567890"));
        } else if (dialog_type == AppConstant.GENERAL_PIN) {
            String strDesc = getContext().getString(desc_id);
            txt_desc.setText(strDesc);
            txt_desc.setTextAlignment(TEXT_ALIGNMENT_CENTER);
            txt_desc.setTextSize(14);
            edt_value.setInputType(InputType.TYPE_CLASS_NUMBER);
            edt_value.setKeyListener(DigitsKeyListener.getInstance("123456789"));
            edt_value.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        } else if (dialog_type == AppConstant.GENERAL_VIDEO_BITRATE || dialog_type == AppConstant.GENERAL_STREAM_BITRATE) {
            edt_value.setInputType(InputType.TYPE_CLASS_NUMBER);
            edt_value.setKeyListener(DigitsKeyListener.getInstance("1234567890"));
            edt_value.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        } else if (dialog_type == AppConstant.GENERAL_VIDEO_KEYFRAME || dialog_type == AppConstant.GENERAL_STREAM_KEYFRAME) {
            edt_value.setInputType(InputType.TYPE_CLASS_NUMBER);
            edt_value.setKeyListener(DigitsKeyListener.getInstance("1234567890"));
            edt_value.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
        } else if (dialog_type == AppConstant.GENERAL_USB_MIN_FPS) {
            edt_value.setInputType(InputType.TYPE_CLASS_NUMBER);
            edt_value.setKeyListener(DigitsKeyListener.getInstance("1234567890"));
            edt_value.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
        } else if (dialog_type == AppConstant.GENERAL_USB_MAX_FPS) {
            edt_value.setInputType(InputType.TYPE_CLASS_NUMBER);
            edt_value.setKeyListener(DigitsKeyListener.getInstance("1234567890"));
            edt_value.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
        }

        // Set up click listeners
        btn_ok.setOnClickListener(this::OnClick);
        btn_close.setOnClickListener(this::OnClick);
    }

    private void init(Context context) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_general);
        setCancelable(false);
                txt_title = findViewById(R.id.txt_title);
        txt_desc = findViewById(R.id.txt_desc);
        edt_value = findViewById(R.id.edt_value);
        btn_ok = findViewById(R.id.btn_ok);
        btn_close = findViewById(R.id.btn_close);
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

    public void OnClick(View view) {
        switch (view.getId()) {
            case R.id.btn_ok:
                handleResult();
                break;
            case R.id.btn_close:
                dismiss();
                break;
        }
    }

    void handleResult() {
        String value = edt_value.getText().toString().trim();
        if (TextUtils.isEmpty(value)) {
            MessageUtil.showToast(getContext(), R.string.invalid_value);
            return;
        }
        int num_value = Integer.parseInt(value);
        if (TextUtils.isEmpty(old_value)) {
            old_value = "0";
        }
        boolean is_changed = (Integer.parseInt(value) != Integer.parseInt(old_value));
        if (dialog_type == AppConstant.GENERAL_PIN) {
            if (value.length() != 4) {
                MessageUtil.showToast(getContext(), R.string.invalid_pin);
                return;
            }
        } else if (dialog_type == AppConstant.GENERAL_SPLIT) {
            if (num_value <= 0) {
                MessageUtil.showToast(getContext(), R.string.invalid_value);
                return;
            }
        } else if (dialog_type == AppConstant.GENERAL_VIDEO_BITRATE) {
            if (num_value < 32 || num_value > 4096) {
                MessageUtil.showToast(getContext(), R.string.invalid_bitrate);
                return;
            }
        } else if (dialog_type == AppConstant.GENERAL_VIDEO_KEYFRAME) {
            if (num_value < 1 || num_value > 100) {
                MessageUtil.showToast(getContext(), R.string.invalid_keyframe);
                return;
            }
        } else if (dialog_type == AppConstant.GENERAL_STREAM_BITRATE) {
            if (num_value < 32 || num_value > 4096) {
                MessageUtil.showToast(getContext(), R.string.invalid_bitrate);
                return;
            }
        } else if (dialog_type == AppConstant.GENERAL_STREAM_KEYFRAME) {
            if (num_value < 1 || num_value > 100) {
                MessageUtil.showToast(getContext(), R.string.invalid_keyframe);
                return;
            }
        } else if (dialog_type == AppConstant.GENERAL_USB_MIN_FPS) {
            if (num_value < 1 || num_value > 30) {
                MessageUtil.showToast(getContext(), R.string.invalid_usb_fps);
                return;
            }
        } else if (dialog_type == AppConstant.GENERAL_USB_MAX_FPS) {
            if (num_value < 1 || num_value > 30) {
                MessageUtil.showToast(getContext(), R.string.invalid_usb_fps);
                return;
            }
        }
        resultListener.onResult(String.valueOf(num_value), is_changed);
        dismiss();
    }

    public void setResultListener(ResultListener resultListener) {
        this.resultListener = resultListener;
    }

}
