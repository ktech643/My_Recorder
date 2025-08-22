package com.checkmate.android.ui.activity;

import android.os.Bundle;

import com.checkmate.android.R;
import com.checkmate.android.database.DBManager;
import com.checkmate.android.model.Camera;
import com.checkmate.android.ui.fragment.SettingsFragment;
import com.checkmate.android.util.MessageUtil;


import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

public class CameraActivity extends BaseActionBarActivity {

    EditText edt_url;

    EditText edt_username;

    EditText edt_password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        ShowActionBarIcons(true, R.id.action_back);
        SetTitle(R.string.new_camera, -1);

                edt_url = findViewById(R.id.edt_url);
        edt_username = findViewById(R.id.edt_username);
        edt_password = findViewById(R.id.edt_password);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_save:
                onSave();
                break;
            case R.id.btn_cancel:
            case R.id.action_back:
                myBack();
                break;
        }
    }

    void onSave() {
        if (TextUtils.isEmpty(edt_url.getText().toString())) {
            MessageUtil.showToast(this, R.string.invalid_url);
            return;
        }
        if (TextUtils.isEmpty(edt_username.getText().toString())) {
            MessageUtil.showToast(this, R.string.invalid_username);
            return;
        }
        if (TextUtils.isEmpty(edt_password.getText().toString())) {
            MessageUtil.showToast(this, R.string.invalid_password);
            return;
        }

        String url = edt_url.getText().toString().trim();
        String username = edt_username.getText().toString().trim();
        String password = edt_password.getText().toString().trim();
        myBack();
    }
}