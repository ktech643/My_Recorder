package com.checkmate.android.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.checkmate.android.AppPreference;
import com.checkmate.android.R;
import com.checkmate.android.ui.dialog.MyProgressDialog;

public class BaseActionBarActivity extends AppCompatActivity implements OnClickListener {
    // UI
    public ActionBar actionBar;
    public TextView action_text_title;
    //	public View action_image_title;
    // left icon
    public View action_button_back;
    // right icon
    public View action_button_setting;
    public View action_add;

    public MyProgressDialog dlg_progress;

    @SuppressLint({"InflateParams", "NewApi"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // finally change the color
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.blue_dark));

        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.blue)));
            actionBar.setHomeButtonEnabled(false);
            actionBar.setElevation(0);

            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);

            LayoutInflater inflator = LayoutInflater.from(this);
            View v = inflator.inflate(R.layout.actionbar_title, null);
            v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            action_text_title = (TextView) v.findViewById(R.id.action_title);
            action_text_title.setText(this.getTitle());
            action_text_title.setVisibility(View.GONE);
//			action_image_title = v.findViewById(R.id.action_title_image);
//			action_image_title.setVisibility(View.GONE);

            action_button_back = v.findViewById(R.id.action_back);
            action_button_back.setVisibility(View.GONE);

            action_button_setting = v.findViewById(R.id.action_settings);
            action_button_setting.setVisibility(View.GONE);
            action_add = v.findViewById(R.id.action_add);
            action_add.setVisibility(View.GONE);

            action_button_back.setOnClickListener(this);
            action_button_setting.setOnClickListener(this);
            action_add.setOnClickListener(this);

            //assign the view to the action bar
            actionBar.setCustomView(v);

            dlg_progress = new MyProgressDialog(this);

        }
    }

    @Override
    public void startActivity(Intent intent) {
        // TODO Auto-generated method stub
        super.startActivity(intent);
        overridePendingTransition(R.anim.in_left, R.anim.out_left);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        // TODO Auto-generated method stub
        super.startActivityForResult(intent, requestCode);
        overridePendingTransition(R.anim.in_left, R.anim.out_left);
    }

    public void SetTitle(int titleResId, int colorResId) {
        if (titleResId > 0) {
            SetTitle(getString(titleResId), colorResId);
        } else {
            SetTitle("", colorResId);
        }
    }

    public void SetTitle(String title, int imageResId) {
        if (actionBar != null) {
            if (TextUtils.isEmpty(title)) {
                action_text_title.setVisibility(View.GONE);
//				action_image_title.setVisibility(View.VISIBLE);
            } else {
                action_text_title.setVisibility(View.VISIBLE);
//				action_image_title.setVisibility(View.GONE);
                action_text_title.setText(title);
            }
        }
    }

    public void ShowActionBarIcons(boolean showActionBar, int... res_id_arr) {
        if (actionBar != null) {
            if (showActionBar)
                actionBar.show();
            else
                actionBar.hide();

            action_button_back.setVisibility(View.GONE);
            action_button_setting.setVisibility(View.GONE);
            action_add.setVisibility(View.GONE);

            if (res_id_arr != null) {
                for (int i = 0; i < res_id_arr.length; i++) {
                    int id = res_id_arr[i];
                    if (id == R.id.action_back) {
                        action_button_back.setVisibility(View.VISIBLE);
                    } else if (id == R.id.action_add) {
                        action_add.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    }

    @Override
    public void onClick(View view) {
        // TODO Auto-generated method stub
        int id = view.getId();
        if (id == R.id.action_back) {
            myBack();
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        if (dlg_progress != null)
            dlg_progress.dismiss();
    }

    public void myBack() {
        finish();
        overridePendingTransition(R.anim.in_right, R.anim.out_right);
    }

    @Override
    public void onBackPressed() {
        // TODO Auto-generated method stub
        myBack();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
    }
}