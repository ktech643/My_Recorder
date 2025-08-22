package com.checkmate.android.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.checkmate.android.AppConstant;
import com.checkmate.android.R;
import com.checkmate.android.util.CommonUtil;

public class ActivationCompleteDialog extends Dialog {

    Button btn_ok;

    TextView txt_complete;

    public ActivationCompleteDialog(Context context, int theme) {
        super(context, theme);
        // TODO Auto-generated constructor stub
        init(context);
    }

    public ActivationCompleteDialog(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
        init(context);
    }

    private void init(Context context) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_activation_complete);
        setCancelable(false);
                btn_ok = findViewById(R.id.btn_ok);
        txt_complete = findViewById(R.id.txt_complete);

        if (TextUtils.isEmpty(AppConstant.expire_date)) {
            txt_complete.setText("The activation process was successful.\n" + "Your license will not expire.");
        } else {
            txt_complete.setText("The activation process was successful.\n" + "Your license will expire on " + CommonUtil.expire_date(AppConstant.expire_date));
        }
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
        btn_ok.setOnClickListener(listener);
    }
}
