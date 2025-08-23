package com.checkmate.android.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.checkmate.android.R;
import com.checkmate.android.model.Media;
import com.checkmate.android.util.MetadataExtractor;
import com.github.chrisbanes.photoview.PhotoView;

public class ImageViewerActivity extends AppCompatActivity {
    
    public static final String EXTRA_IMAGE_URI = "image_uri";
    public static final String EXTRA_IMAGE_NAME = "image_name";
    public static final String EXTRA_IMAGE_INFO = "image_info";
    public static final String EXTRA_FILE_SIZE = "file_size";
    public static final String EXTRA_DATE = "date";
    
    private PhotoView photoView;
    private ImageView btnClose;
    private ImageView btnInfo;
    private TextView txtImageName;
    private TextView txtImageInfo;
    private View metadataOverlay;
    private Media currentMedia;
    
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
        btnInfo = findViewById(R.id.btn_info);
        txtImageName = findViewById(R.id.txt_image_name);
        txtImageInfo = findViewById(R.id.txt_image_info);
        metadataOverlay = findViewById(R.id.metadata_overlay);
    }
    
    private void handleIntent() {
        Intent intent = getIntent();
        
        String imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI);
        String imageName = intent.getStringExtra(EXTRA_IMAGE_NAME);
        String imageInfo = intent.getStringExtra(EXTRA_IMAGE_INFO);
        long fileSize = intent.getLongExtra(EXTRA_FILE_SIZE, 0);
        long date = intent.getLongExtra(EXTRA_DATE, 0);
        
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            
            Glide.with(this)
                    .load(imageUri)
                    .into(photoView);
                    
            // Create media object for metadata extraction
            currentMedia = new Media();
            currentMedia.contentUri = imageUri;
            currentMedia.name = imageName;
            currentMedia.type = Media.TYPE.PHOTO;
            currentMedia.fileSize = fileSize;
            if (date > 0) {
                currentMedia.date = new java.util.Date(date);
            }
            
            // Extract additional metadata in background
            new Thread(() -> {
                MetadataExtractor.extractAndPopulateMetadata(this, currentMedia);
                runOnUiThread(this::setupMetadataOverlay);
            }).start();
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
        btnInfo.setOnClickListener(v -> toggleMetadataOverlay());
        
        // Hide metadata overlay when close button in overlay is clicked
        ImageView btnCloseMetadata = metadataOverlay.findViewById(R.id.btn_close_metadata);
        if (btnCloseMetadata != null) {
            btnCloseMetadata.setOnClickListener(v -> hideMetadataOverlay());
        }
        
        // Hide/show info overlay on tap
        photoView.setOnPhotoTapListener((view, x, y) -> {
            View infoOverlay = findViewById(R.id.info_overlay);
            View topControls = findViewById(R.id.top_controls);
            if (infoOverlay != null && topControls != null) {
                if (infoOverlay.getVisibility() == View.VISIBLE) {
                    infoOverlay.setVisibility(View.GONE);
                    topControls.setVisibility(View.GONE);
                } else {
                    infoOverlay.setVisibility(View.VISIBLE);
                    topControls.setVisibility(View.VISIBLE);
                }
            }
        });
    }
    
    private void toggleMetadataOverlay() {
        if (metadataOverlay.getVisibility() == View.VISIBLE) {
            hideMetadataOverlay();
        } else {
            showMetadataOverlay();
        }
    }
    
    private void showMetadataOverlay() {
        metadataOverlay.setVisibility(View.VISIBLE);
    }
    
    private void hideMetadataOverlay() {
        metadataOverlay.setVisibility(View.GONE);
    }
    
    private void setupMetadataOverlay() {
        if (currentMedia == null) return;
        
        // Populate metadata fields
        TextView txtMetaFilename = metadataOverlay.findViewById(R.id.txt_meta_filename);
        TextView txtMetaFilesize = metadataOverlay.findViewById(R.id.txt_meta_filesize);
        TextView txtMetaDate = metadataOverlay.findViewById(R.id.txt_meta_date);
        TextView txtMetaFormat = metadataOverlay.findViewById(R.id.txt_meta_format);
        TextView txtMetaResolution = metadataOverlay.findViewById(R.id.txt_meta_resolution);
        
        // Image-specific metadata
        TextView txtMetaCamera = metadataOverlay.findViewById(R.id.txt_meta_camera);
        TextView txtMetaIso = metadataOverlay.findViewById(R.id.txt_meta_iso);
        LinearLayout layoutExif = metadataOverlay.findViewById(R.id.layout_exif);
        LinearLayout layoutCamera = metadataOverlay.findViewById(R.id.layout_camera);
        LinearLayout layoutIso = metadataOverlay.findViewById(R.id.layout_iso);
        
        // Hide video-specific layouts
        View layoutDuration = metadataOverlay.findViewById(R.id.layout_duration);
        View layoutFps = metadataOverlay.findViewById(R.id.layout_fps);
        View layoutBitrate = metadataOverlay.findViewById(R.id.layout_bitrate);
        View layoutCodec = metadataOverlay.findViewById(R.id.layout_codec);
        
        if (layoutDuration != null) layoutDuration.setVisibility(View.GONE);
        if (layoutFps != null) layoutFps.setVisibility(View.GONE);
        if (layoutBitrate != null) layoutBitrate.setVisibility(View.GONE);
        if (layoutCodec != null) layoutCodec.setVisibility(View.GONE);
        
        // Set basic metadata
        if (txtMetaFilename != null && currentMedia.name != null) {
            txtMetaFilename.setText(currentMedia.name);
        }
        
        if (txtMetaFilesize != null && currentMedia.fileSize > 0) {
            txtMetaFilesize.setText(MetadataExtractor.formatFileSize(currentMedia.fileSize));
        }
        
        if (txtMetaDate != null && currentMedia.date != null) {
            txtMetaDate.setText(MetadataExtractor.formatDate(currentMedia.date));
        }
        
        if (txtMetaFormat != null && currentMedia.format != null) {
            txtMetaFormat.setText(currentMedia.format);
        }
        
        if (txtMetaResolution != null && currentMedia.resolutionWidth > 0 && currentMedia.resolutionHeight > 0) {
            txtMetaResolution.setText(currentMedia.resolutionWidth + " × " + currentMedia.resolutionHeight);
        }
        
        // Set EXIF data if available
        boolean hasExifData = false;
        
        if (currentMedia.cameraModel != null && !currentMedia.cameraModel.isEmpty()) {
            if (txtMetaCamera != null && layoutCamera != null) {
                txtMetaCamera.setText(currentMedia.cameraModel);
                layoutCamera.setVisibility(View.VISIBLE);
                hasExifData = true;
            }
        }
        
        if (currentMedia.iso != null && !currentMedia.iso.isEmpty()) {
            if (txtMetaIso != null && layoutIso != null) {
                txtMetaIso.setText("ISO " + currentMedia.iso);
                layoutIso.setVisibility(View.VISIBLE);
                hasExifData = true;
            }
        }
        
        // Show EXIF section if we have any EXIF data
        if (layoutExif != null) {
            layoutExif.setVisibility(hasExifData ? View.VISIBLE : View.GONE);
        }
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}