package com.checkmate.android.ui.activity;

import android.content.Intent;
import android.os.Bundle;

import com.checkmate.android.R;
import com.checkmate.android.util.MainActivity;
import com.volcaniccoder.bottomify.BottomifyNavigationView;



public class HomeActivity extends BaseActivity {

    public static HomeActivity instance;

    BottomifyNavigationView bottom_tab;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
                bottom_tab = findViewById(R.id.bottom_tab);
        Intent intent1=new Intent(HomeActivity.this, MainActivity.class);
        startActivity(intent1);
        overridePendingTransition(0, 0); // Disable transition animation


    }


    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onStop() {
        super.onStop();

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}