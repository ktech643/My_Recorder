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
import com.checkmate.android.databinding.DialogGeneralBinding;
import com.checkmate.android.util.MessageUtil;

public class GeneralDialog extends Dialog {

    private DialogGeneralBinding binding;
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
        binding = DialogGeneralBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setCancelable(false);

        binding.txtTitle.setText(title_id);
        binding.txtDesc.setText(desc_id);
        binding.edtValue.setHint(title_id);
        binding.edtValue.setText(value);
        old_value = value;
        this.dialog_type = dialog_type;
        
        // Set up click listeners
        binding.btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleResult();
            }
        });

        binding.btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        if (dialog_type == AppConstant.GENERAL_SPLIT) {
            binding.edtValue.setInputType(InputType.TYPE_CLASS_NUMBER);
            binding.edtValue.setKeyListener(DigitsKeyListener.getInstance("1234567890"));
        } else if (dialog_type == AppConstant.GENERAL_PIN) {
            String strDesc = getContext().getString(desc_id);
            binding.txtDesc.setText(strDesc);
            binding.txtDesc.setTextAlignment(TEXT_ALIGNMENT_CENTER);
            binding.txtDesc.setTextSize(14);
            binding.edtValue.setInputType(InputType.TYPE_CLASS_NUMBER);
            binding.edtValue.setKeyListener(DigitsKeyListener.getInstance("123456789"));
            binding.edtValue.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        } else if (dialog_type == AppConstant.GENERAL_VIDEO_BITRATE || dialog_type == AppConstant.GENERAL_STREAM_BITRATE) {
            binding.edtValue.setInputType(InputType.TYPE_CLASS_NUMBER);
            binding.edtValue.setKeyListener(DigitsKeyListener.getInstance("1234567890"));
            binding.edtValue.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        } else if (dialog_type == AppConstant.GENERAL_VIDEO_KEYFRAME || dialog_type == AppConstant.GENERAL_STREAM_KEYFRAME) {
            binding.edtValue.setInputType(InputType.TYPE_CLASS_NUMBER);
            binding.edtValue.setKeyListener(DigitsKeyListener.getInstance("1234567890"));
            binding.edtValue.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
        } else if (dialog_type == AppConstant.GENERAL_USB_MIN_FPS) {
            binding.edtValue.setInputType(InputType.TYPE_CLASS_NUMBER);
            binding.edtValue.setKeyListener(DigitsKeyListener.getInstance("1234567890"));
            binding.edtValue.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
        } else if (dialog_type == AppConstant.GENERAL_USB_MAX_FPS) {
            binding.edtValue.setInputType(InputType.TYPE_CLASS_NUMBER);
            binding.edtValue.setKeyListener(DigitsKeyListener.getInstance("1234567890"));
            binding.edtValue.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
        }
    }

    private void init(Context context) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogGeneralBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setCancelable(false);
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

    void handleResult() {
        String value = binding.edtValue.getText().toString().trim();
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
