package com.checkmate.android.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.checkmate.android.R;

public class WifiDialog extends Dialog {

    public interface onResultListener{
        void onResult(String ssid);
    }

    Button btn_ok;

    Button btn_close;

    public EditText edt_ssid;

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
        setContentView(R.layout.dialog_wifi);
        setCancelable(true);
        btn_ok = findViewById(R.id.btn_ok);
        btn_close = findViewById(R.id.btn_close);
        edt_ssid = findViewById(R.id.edt_ssid);
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
                onResult();
                break;
            case R.id.btn_close:
                dismiss();
                break;
        }
    }

    void onResult() {
        String ssid = edt_ssid.getText().toString().trim();
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
}
