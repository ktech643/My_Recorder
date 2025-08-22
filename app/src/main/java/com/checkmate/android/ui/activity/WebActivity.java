package com.checkmate.android.ui.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.checkmate.android.R;
import com.checkmate.android.databinding.ActivityWebBinding;
import com.checkmate.android.ui.view.MyWebView;

public class WebActivity extends BaseActionBarActivity {

    private ActivityWebBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWebBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ShowActionBarIcons(true, R.id.action_back);
        SetTitle(R.string.speed_test, -1);
        binding.webView.loadUrl("https://speedsmart.net/");
    }
}