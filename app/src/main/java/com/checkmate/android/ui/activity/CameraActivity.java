package com.checkmate.android.ui.activity;

import android.os.Bundle;

import com.checkmate.android.R;
import com.checkmate.android.database.DBManager;
import com.checkmate.android.databinding.ActivityCameraBinding;
import com.checkmate.android.model.Camera;
import com.checkmate.android.ui.fragment.SettingsFragment;
import com.checkmate.android.util.MessageUtil;

import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

public class CameraActivity extends BaseActionBarActivity {

    private ActivityCameraBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ShowActionBarIcons(true, R.id.action_back);
        SetTitle(R.string.new_camera, -1);

        // Set up click listeners
        binding.btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSave();
            }
        });

        binding.btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myBack();
            }
        });
    }

    void onSave() {
        if (TextUtils.isEmpty(binding.edtUrl.getText().toString())) {
            MessageUtil.showToast(this, R.string.invalid_url);
            return;
        }
        if (TextUtils.isEmpty(binding.edtUsername.getText().toString())) {
            MessageUtil.showToast(this, R.string.invalid_username);
            return;
        }
        if (TextUtils.isEmpty(binding.edtPassword.getText().toString())) {
            MessageUtil.showToast(this, R.string.invalid_password);
            return;
        }

        String url = binding.edtUrl.getText().toString().trim();
        String username = binding.edtUsername.getText().toString().trim();
        String password = binding.edtPassword.getText().toString().trim();
        myBack();
    }
}