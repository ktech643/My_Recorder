package com.checkmate.android.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.checkmate.android.R;

public class TextProgressDialog extends Dialog {

	TextView message;
	String str_message;

	public TextProgressDialog(Context context, int theme) {
		super(context, theme);
		// TODO Auto-generated constructor stub
		init(context);
	}

	public TextProgressDialog(Context context, String message) {
		super(context);
		// TODO Auto-generated constructor stub
		this.str_message = message;
		init(context);
	}

	public TextProgressDialog(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		init(context);
	}

	private void init(Context context) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.dialog_text);

		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.dimAmount = 0f;
		getWindow().setAttributes(lp);

		setCancelable(false);
		message = findViewById(R.id.message);
		message.setText(str_message);
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
