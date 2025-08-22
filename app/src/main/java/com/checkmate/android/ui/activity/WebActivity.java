package com.checkmate.android.ui.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.checkmate.android.R;
import com.checkmate.android.ui.view.MyWebView;


public class WebActivity extends BaseActionBarActivity {

    MyWebView web_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        ShowActionBarIcons(true, R.id.action_back);
        SetTitle(R.string.speed_test, -1);
                web_view = findViewById(R.id.web_view);
        web_view.loadUrl("https://speedsmart.net/");
    }
}