package com.checkmate.android.ui.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

import com.chaos.view.PinView;
import com.checkmate.android.R;
import com.checkmate.android.ui.fragment.StreamingFragment;
import com.checkmate.android.util.CommonUtil;



public class PinActivity extends BaseActionBarActivity {

    PinView pin_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        ShowActionBarIcons(true, R.id.action_back);
        SetTitle(R.string.otc, -1);

        pin_view = findViewById(R.id.pin_view);

        pin_view.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 6) {
                    CommonUtil.hideKeyboard(PinActivity.this, pin_view);
                    myBack();
                    StreamingFragment.instance.loginCode(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }
}