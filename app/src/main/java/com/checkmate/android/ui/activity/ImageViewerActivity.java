package com.checkmate.android.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.checkmate.android.R;
import com.github.chrisbanes.photoview.PhotoView;

public class ImageViewerActivity extends AppCompatActivity {
    
    public static final String EXTRA_IMAGE_URI = "image_uri";
    public static final String EXTRA_IMAGE_NAME = "image_name";
    public static final String EXTRA_IMAGE_INFO = "image_info";
    
    private PhotoView photoView;
    private ImageView btnClose;
    private TextView txtImageName;
    private TextView txtImageInfo;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);
        
        initViews();
        handleIntent();
        setupListeners();
    }
    
    private void initViews() {
        photoView = findViewById(R.id.photo_view);
        btnClose = findViewById(R.id.btn_close);
        txtImageName = findViewById(R.id.txt_image_name);
        txtImageInfo = findViewById(R.id.txt_image_info);
    }
    
    private void handleIntent() {
        Intent intent = getIntent();
        
        String imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI);
        String imageName = intent.getStringExtra(EXTRA_IMAGE_NAME);
        String imageInfo = intent.getStringExtra(EXTRA_IMAGE_INFO);
        
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            
            Glide.with(this)
                    .load(imageUri)
                    .into(photoView);
        }
        
        if (imageName != null) {
            txtImageName.setText(imageName);
        }
        
        if (imageInfo != null) {
            txtImageInfo.setText(imageInfo);
        }
    }
    
    private void setupListeners() {
        btnClose.setOnClickListener(v -> finish());
        
        // Hide/show info overlay on tap
        photoView.setOnPhotoTapListener((view, x, y) -> {
            if (findViewById(R.id.info_overlay).getVisibility() == android.view.View.VISIBLE) {
                findViewById(R.id.info_overlay).setVisibility(android.view.View.GONE);
                btnClose.setVisibility(android.view.View.GONE);
            } else {
                findViewById(R.id.info_overlay).setVisibility(android.view.View.VISIBLE);
                btnClose.setVisibility(android.view.View.VISIBLE);
            }
        });
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}