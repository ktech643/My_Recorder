package com.checkmate.android.ui.activity;

import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.checkmate.android.R;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageViewerActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URI = "image_uri";
    public static final String EXTRA_IMAGE_NAME = "image_name";
    public static final String EXTRA_IS_ENCRYPTED = "is_encrypted";
    public static final String EXTRA_IMAGE_DATE = "image_date";
    public static final String EXTRA_IMAGE_SIZE = "image_size";
    
    private PhotoView photoView;
    private ImageView btnClose;
    private ImageView btnInfo;
    private View metadataContainer;
    private TextView tvMetadata;
    private Uri imageUri;
    private String imageName;
    private boolean isEncrypted;
    private long imageDate;
    private long imageSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);
        
        // Get data from intent
        imageUri = getIntent().getParcelableExtra(EXTRA_IMAGE_URI);
        imageName = getIntent().getStringExtra(EXTRA_IMAGE_NAME);
        isEncrypted = getIntent().getBooleanExtra(EXTRA_IS_ENCRYPTED, false);
        imageDate = getIntent().getLongExtra(EXTRA_IMAGE_DATE, 0);
        imageSize = getIntent().getLongExtra(EXTRA_IMAGE_SIZE, 0);
        
        if (imageUri == null) {
            finish();
            return;
        }
        
        initViews();
        loadImage();
        setupMetadata();
    }
    
    private void initViews() {
        photoView = findViewById(R.id.photo_view);
        btnClose = findViewById(R.id.btn_close);
        btnInfo = findViewById(R.id.btn_info);
        metadataContainer = findViewById(R.id.metadata_container);
        tvMetadata = findViewById(R.id.tv_metadata);
        
        btnClose.setOnClickListener(v -> finish());
        
        btnInfo.setOnClickListener(v -> {
            if (metadataContainer.getVisibility() == View.VISIBLE) {
                metadataContainer.setVisibility(View.GONE);
            } else {
                metadataContainer.setVisibility(View.VISIBLE);
            }
        });
        
        metadataContainer.setOnClickListener(v -> metadataContainer.setVisibility(View.GONE));
    }
    
    private void loadImage() {
        Glide.with(this)
                .load(imageUri)
                .into(photoView);
    }
    
    private void setupMetadata() {
        StringBuilder metadata = new StringBuilder();
        
        // File name
        metadata.append("Name: ").append(imageName).append("\n\n");
        
        // Created date
        if (imageDate > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            metadata.append("Created: ").append(sdf.format(new Date(imageDate))).append("\n\n");
        }
        
        // File size
        if (imageSize > 0) {
            String formattedSize = formatFileSize(imageSize);
            metadata.append("Size: ").append(formattedSize).append("\n\n");
        }
        
        // Type
        String fileExtension = "";
        if (imageName.contains(".")) {
            fileExtension = imageName.substring(imageName.lastIndexOf(".") + 1).toUpperCase();
        }
        metadata.append("Type: ").append(fileExtension).append(" Image\n\n");
        
        // Try to get image dimensions and other EXIF data
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(inputStream, null, options);
                inputStream.close();
                
                int width = options.outWidth;
                int height = options.outHeight;
                
                metadata.append("Resolution: ").append(width).append(" x ").append(height).append(" pixels\n\n");
                
                // Try to get EXIF data if not encrypted
                if (!isEncrypted && imageUri.getScheme() != null && imageUri.getScheme().equals("file")) {
                    ExifInterface exif = new ExifInterface(imageUri.getPath());
                    
                    String camera = exif.getAttribute(ExifInterface.TAG_MAKE);
                    String model = exif.getAttribute(ExifInterface.TAG_MODEL);
                    if (camera != null || model != null) {
                        metadata.append("Camera: ");
                        if (camera != null) metadata.append(camera);
                        if (model != null) metadata.append(" ").append(model);
                        metadata.append("\n\n");
                    }
                    
                    String iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS);
                    if (iso != null) {
                        metadata.append("ISO: ").append(iso).append("\n\n");
                    }
                    
                    String aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER);
                    if (aperture != null) {
                        metadata.append("Aperture: f/").append(aperture).append("\n\n");
                    }
                    
                    String exposure = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);
                    if (exposure != null) {
                        metadata.append("Exposure: ").append(exposure).append("s\n\n");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        if (isEncrypted) {
            metadata.append("Status: Encrypted\n");
        }
        
        tvMetadata.setText(metadata.toString().trim());
    }
    
    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format(Locale.US, "%.2f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}