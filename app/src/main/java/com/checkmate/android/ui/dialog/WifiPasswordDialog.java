package com.checkmate.android.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.checkmate.android.R;
import com.checkmate.android.databinding.DialogWifiPasswordBinding;

public class WifiPasswordDialog extends Dialog {

    public DialogWifiPasswordBinding binding;

    public WifiPasswordDialog(Context context, int theme) {
        super(context, theme);
        // TODO Auto-generated constructor stub
        init(context);
    }

    public WifiPasswordDialog(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
        init(context);
    }

    private void init(Context context) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogWifiPasswordBinding.inflate(getLayoutInflater());
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

    public void setOkListener(View.OnClickListener listener) {
        binding.btnOk.setOnClickListener(listener);
    }

    public void setCloseListener(View.OnClickListener listner) {
        binding.btnClose.setOnClickListener(listner);
    }

    // Getter for the EditText
    public EditText getEdtPassword() {
        return binding.edtPassword;
    }
}
