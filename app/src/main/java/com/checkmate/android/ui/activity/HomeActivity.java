package com.checkmate.android.ui.activity;

import android.content.Intent;
import android.os.Bundle;

import com.checkmate.android.R;
import com.checkmate.android.databinding.ActivityHomeBinding;
import com.checkmate.android.util.MainActivity;
import com.volcaniccoder.bottomify.BottomifyNavigationView;

public class HomeActivity extends BaseActivity {

    public static HomeActivity instance;
    private ActivityHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        Intent intent1=new Intent(HomeActivity.this, MainActivity.class);
        startActivity(intent1);
        overridePendingTransition(0, 0); // Disable transition animation
    }

    // Getter for the binding to access views
    public ActivityHomeBinding getBinding() {
        return binding;
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