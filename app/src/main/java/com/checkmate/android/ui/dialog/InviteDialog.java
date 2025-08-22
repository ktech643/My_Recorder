package com.checkmate.android.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.checkmate.android.R;
import com.checkmate.android.databinding.DialogEmailBinding;

public class InviteDialog extends Dialog {

    private DialogEmailBinding binding;

    public InviteDialog(Context context, int theme) {
        super(context, theme);
        // TODO Auto-generated constructor stub
        init(context);
    }

    public InviteDialog(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
        init(context);
    }

    private void init(Context context) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogEmailBinding.inflate(getLayoutInflater());
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
    public EditText getEdtEmail() {
        return binding.edtEmail;
    }
}
