package com.checkmate.android.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.checkmate.android.R;
import com.checkmate.android.util.MediaMetadataUtils;
import com.github.chrisbanes.photoview.PhotoView;

import java.lang.ref.WeakReference;

public class ImageViewerActivity extends AppCompatActivity {
    
    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    public static final String EXTRA_IS_ENCRYPTED = "extra_is_encrypted";
    
    private PhotoView photoView;
    private ImageView btnClose;
    private ImageView btnInfo;
    private ScrollView metadataPanel;
    private TextView txtMetadata;
    private View toolbar;
    
    private Uri imageUri;
    private boolean isEncrypted;
    private boolean isMetadataVisible = false;
    private MediaMetadataUtils.ImageMetadata metadata;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide system UI for immersive experience
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        
        setContentView(R.layout.activity_image_viewer);
        
        initViews();
        handleIntent();
        setupClickListeners();
    }
    
    private void initViews() {
        photoView = findViewById(R.id.photo_view);
        btnClose = findViewById(R.id.btn_close);
        btnInfo = findViewById(R.id.btn_info);
        metadataPanel = findViewById(R.id.metadata_panel);
        txtMetadata = findViewById(R.id.txt_metadata);
        toolbar = findViewById(R.id.toolbar);
        
        // Initially hide toolbar after a delay
        toolbar.postDelayed(() -> {
            if (!isMetadataVisible) {
                toolbar.setVisibility(View.GONE);
            }
        }, 3000);
    }
    
    private void handleIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            String uriString = intent.getStringExtra(EXTRA_IMAGE_URI);
            isEncrypted = intent.getBooleanExtra(EXTRA_IS_ENCRYPTED, false);
            
            if (uriString != null) {
                imageUri = Uri.parse(uriString);
                loadImage();
                loadMetadata();
            } else {
                finish();
            }
        } else {
            finish();
        }
    }
    
    private void setupClickListeners() {
        btnClose.setOnClickListener(v -> finish());
        
        btnInfo.setOnClickListener(v -> toggleMetadata());
        
        // Toggle toolbar visibility on photo tap
        photoView.setOnPhotoTapListener((view, x, y) -> toggleToolbar());
        
        // Hide metadata when clicking outside
        metadataPanel.setOnClickListener(v -> {
            if (isMetadataVisible) {
                toggleMetadata();
            }
        });
    }
    
    private void loadImage() {
        if (imageUri != null) {
            Glide.with(this)
                    .load(imageUri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(R.mipmap.ic_launcher) // fallback image
                    .into(photoView);
        }
    }
    
    private void loadMetadata() {
        if (imageUri != null) {
            new LoadMetadataTask(this).execute(imageUri);
        }
    }
    
    private void toggleToolbar() {
        if (toolbar.getVisibility() == View.VISIBLE) {
            toolbar.setVisibility(View.GONE);
            if (isMetadataVisible) {
                metadataPanel.setVisibility(View.GONE);
                isMetadataVisible = false;
            }
        } else {
            toolbar.setVisibility(View.VISIBLE);
        }
    }
    
    private void toggleMetadata() {
        if (isMetadataVisible) {
            metadataPanel.setVisibility(View.GONE);
            isMetadataVisible = false;
        } else {
            if (metadata != null) {
                txtMetadata.setText(metadata.toString());
                metadataPanel.setVisibility(View.VISIBLE);
                isMetadataVisible = true;
                toolbar.setVisibility(View.VISIBLE);
            } else {
                txtMetadata.setText("Loading metadata...");
                metadataPanel.setVisibility(View.VISIBLE);
                isMetadataVisible = true;
                toolbar.setVisibility(View.VISIBLE);
                loadMetadata(); // Retry loading metadata
            }
        }
    }
    
    private static class LoadMetadataTask extends AsyncTask<Uri, Void, MediaMetadataUtils.ImageMetadata> {
        private final WeakReference<ImageViewerActivity> activityRef;
        
        LoadMetadataTask(ImageViewerActivity activity) {
            activityRef = new WeakReference<>(activity);
        }
        
        @Override
        protected MediaMetadataUtils.ImageMetadata doInBackground(Uri... uris) {
            ImageViewerActivity activity = activityRef.get();
            if (activity == null || uris.length == 0) {
                return null;
            }
            
            return MediaMetadataUtils.extractImageMetadata(activity, uris[0]);
        }
        
        @Override
        protected void onPostExecute(MediaMetadataUtils.ImageMetadata result) {
            ImageViewerActivity activity = activityRef.get();
            if (activity != null && result != null) {
                activity.metadata = result;
                
                // Update metadata display if it's currently visible
                if (activity.isMetadataVisible) {
                    activity.txtMetadata.setText(result.toString());
                }
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Ensure immersive mode is maintained
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
    
    @Override
    public void onBackPressed() {
        if (isMetadataVisible) {
            toggleMetadata();
        } else {
            super.onBackPressed();
        }
    }
}