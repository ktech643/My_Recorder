package com.checkmate.android.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableString;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.checkmate.android.R;
import com.checkmate.android.databinding.DialogSerialBinding;

public class SerialDialog extends Dialog {

    private DialogSerialBinding binding;

    public SerialDialog(Context context, int theme) {
        super(context, theme);
        // TODO Auto-generated constructor stub
        init(context);
    }

    public SerialDialog(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
        init(context);
    }

    private void init(Context context) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogSerialBinding.inflate(getLayoutInflater());
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

    public void setScanListener(View.OnClickListener listener) {
        binding.btnQr.setOnClickListener(listener);
    }

    public void setHelpListener(View.OnClickListener listener) {
        binding.imgHelp.setOnClickListener(listener);
    }

    // Getter for the EditText
    public EditText getEdtSerial() {
        return binding.edtSerial;
    }

    // Getter for the TextView
    public TextView getTxtContent() {
        return binding.txtContent;
    }
}
