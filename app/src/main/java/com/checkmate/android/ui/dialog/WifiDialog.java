package com.checkmate.android.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.checkmate.android.R;
import com.checkmate.android.databinding.DialogWifiBinding;

public class WifiDialog extends Dialog {

    public interface onResultListener{
        void onResult(String ssid);
    }

    private DialogWifiBinding binding;
    onResultListener listener;

    public WifiDialog(Context context, int theme) {
        super(context, theme);
        // TODO Auto-generated constructor stub
        init(context);
    }

    public WifiDialog(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
        init(context);
    }

    private void init(Context context) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogWifiBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setCancelable(true);

        // Set up click listeners
        binding.btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onResult();
            }
        });

        binding.btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
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

    void onResult() {
        String ssid = binding.edtSsid.getText().toString().trim();
        if (!TextUtils.isEmpty(ssid)) {
            if (listener != null) {
                listener.onResult(ssid);
            }
        }
        dismiss();
    }

    public void setOnSetResultListener(onResultListener listener) {
        this.listener = listener;
    }

    // Getter for the EditText
    public EditText getEdtSsid() {
        return binding.edtSsid;
    }
}
