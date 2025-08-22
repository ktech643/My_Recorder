package com.checkmate.android.ui.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

import com.chaos.view.PinView;
import com.checkmate.android.R;
import com.checkmate.android.databinding.ActivityPinBinding;
import com.checkmate.android.ui.fragment.StreamingFragment;
import com.checkmate.android.util.CommonUtil;

public class PinActivity extends BaseActionBarActivity {

    private ActivityPinBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPinBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ShowActionBarIcons(true, R.id.action_back);
        SetTitle(R.string.otc, -1);

        binding.pinView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 6) {
                    CommonUtil.hideKeyboard(PinActivity.this, binding.pinView);
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