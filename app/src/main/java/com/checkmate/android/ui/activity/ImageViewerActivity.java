package com.checkmate.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageButton;
import com.github.chrisbanes.photoview.PhotoView;

import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.RequestListener;
import androidx.annotation.Nullable;
import android.graphics.drawable.Drawable;
import com.checkmate.android.R;
import com.checkmate.android.model.Media;
import com.checkmate.android.util.MessageUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageViewerActivity extends AppCompatActivity {
    private static final String TAG = "ImageViewerActivity";

    // Intent extras
    public static final String EXTRA_IMAGE_URI = "image_uri";
    public static final String EXTRA_IMAGE_NAME = "image_name";
    public static final String EXTRA_IMAGE_WIDTH = "image_width";
    public static final String EXTRA_IMAGE_HEIGHT = "image_height";
    public static final String EXTRA_IMAGE_SIZE = "image_size";
    public static final String EXTRA_IMAGE_DATE = "image_date";
    public static final String EXTRA_IS_ENCRYPTED = "is_encrypted";
    public static final String EXTRA_DECRYPTED_FILE_PATH = "decrypted_file_path";

    // UI components
    private PhotoView imageView;
    private ImageButton btnClose;
    private ImageButton btnInfo;
    private ImageButton btnZoomIn;
    private ImageButton btnZoomOut;
    private ImageButton btnRotate;
    private TextView txtImageName;
    private TextView txtImageDetails;
    private View controlsLayout;
    private View detailsLayout;

    // Image info
    private Uri imageUri;
    private String imageName;
    private int imageWidth;
    private int imageHeight;
    private long imageSize;
    private Date imageDate;
    private boolean isEncrypted;
    private String decryptedFilePath;

    // Zoom and pan
    private GestureDetector gestureDetector;
    private boolean isDetailsVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        // Hide action bar for fullscreen experience
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initViews();
        extractIntentData();
        setupImage();
        setupControls();
        setupGestures();
        updateImageDetails();
    }

    private void initViews() {
        imageView = findViewById(R.id.image_view);
        btnClose = findViewById(R.id.btn_close);
        btnInfo = findViewById(R.id.btn_info);
        btnZoomIn = findViewById(R.id.btn_zoom_in);
        btnZoomOut = findViewById(R.id.btn_zoom_out);
        btnRotate = findViewById(R.id.btn_rotate);
        txtImageName = findViewById(R.id.txt_image_name);
        txtImageDetails = findViewById(R.id.txt_image_details);
        controlsLayout = findViewById(R.id.controls_layout);
        detailsLayout = findViewById(R.id.details_layout);
    }

    private void extractIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            imageUri = intent.getParcelableExtra(EXTRA_IMAGE_URI);
            imageName = intent.getStringExtra(EXTRA_IMAGE_NAME);
            imageWidth = intent.getIntExtra(EXTRA_IMAGE_WIDTH, 0);
            imageHeight = intent.getIntExtra(EXTRA_IMAGE_HEIGHT, 0);
            imageSize = intent.getLongExtra(EXTRA_IMAGE_SIZE, 0);
            long dateTime = intent.getLongExtra(EXTRA_IMAGE_DATE, 0);
            imageDate = dateTime > 0 ? new Date(dateTime) : new Date();
            isEncrypted = intent.getBooleanExtra(EXTRA_IS_ENCRYPTED, false);
            decryptedFilePath = intent.getStringExtra(EXTRA_DECRYPTED_FILE_PATH);

            if (decryptedFilePath != null) {
                imageUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", new File(decryptedFilePath));
            }
        }
    }

    private void setupImage() {
        if (imageUri == null) {
            MessageUtil.showToast(this, "Invalid image URI");
            finish();
            return;
        }

        try {
            // Use Glide for efficient image loading
            RequestOptions options = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .fitCenter()
                    .error(R.drawable.ic_error_image);

            Glide.with(this)
                    .load(imageUri)
                    .apply(options)
                    .listener(new com.bumptech.glide.request.RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e(TAG, "Failed to load image", e);
                            MessageUtil.showToast(ImageViewerActivity.this, "Error loading image");
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            // Update dimensions if not provided
                            if (imageWidth == 0 || imageHeight == 0) {
                                imageWidth = resource.getIntrinsicWidth();
                                imageHeight = resource.getIntrinsicHeight();
                                updateImageDetails();
                            }
                            return false;
                        }
                    })
                    .into(imageView);

        } catch (Exception e) {
            Log.e(TAG, "Error setting up image", e);
            MessageUtil.showToast(this, "Error loading image");
            finish();
        }
    }

    private void setupControls() {
        // `imageView` is already a PhotoView from the layout (id: image_view)
        PhotoView photoView = imageView;
        if (photoView != null) {
            photoView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            photoView.setMaximumScale(4.0f); // Example max scale
            photoView.setMinimumScale(1.0f); // Example min scale
            // Enable pinch-to-zoom (handled internally by PhotoView)
            photoView.setZoomable(true);
        }
        // Close button
        btnClose.setOnClickListener(v -> finish());

        // Info button
        btnInfo.setOnClickListener(v -> toggleDetails());

        // Zoom buttons
        btnZoomIn.setOnClickListener(v -> zoomIn());
        btnZoomOut.setOnClickListener(v -> zoomOut());

        // Rotate button
        btnRotate.setOnClickListener(v -> rotateImage());

        // Image click to toggle controls
        imageView.setOnClickListener(v -> toggleControls());

        // Set image name
        if (txtImageName != null) {
            txtImageName.setText(imageName != null ? imageName : "Unknown Image");
        }
    }

    private void setupGestures() {
        // Let PhotoView handle all gestures; just listen for double-tap to toggle UI
        imageView.setOnDoubleTapListener(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (detailsLayout.getVisibility() == View.VISIBLE) {
                    toggleDetails();
                } else {
                    toggleControls();
                }
                return true; // consume
            }
        });
    }

    private void zoomIn() {
        float currentScale = imageView.getScale();
        float newScale = currentScale * 1.5f; // Example scaling factor

        // Clamp the new scale to the valid range
        float minScale = imageView.getMinimumScale();
        float maxScale = imageView.getMaximumScale();
        newScale = Math.max(minScale, Math.min(newScale, maxScale));

        imageView.setScale(newScale, true); // Animate the zoom
    }
    private void zoomOut(){ imageView.setScale(Math.max(1f,imageView.getScale()/1.2f),true); }
    private void rotateImage(){ imageView.setRotation(imageView.getRotation()+90); }

    private void toggleControls() {
        boolean isVisible = controlsLayout.getVisibility() == View.VISIBLE;
        controlsLayout.setVisibility(isVisible ? View.GONE : View.VISIBLE);
    }

    private void toggleDetails() {
        isDetailsVisible = !isDetailsVisible;

        if (isDetailsVisible) {
            detailsLayout.setVisibility(View.VISIBLE);
            detailsLayout.setAlpha(0f);
            detailsLayout.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start();
        } else {
            detailsLayout.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> detailsLayout.setVisibility(View.GONE))
                    .start();
        }
    }

    private void updateImageDetails() {
        if (txtImageDetails == null) return;

        StringBuilder details = new StringBuilder();

        // File name
        if (imageName != null) {
            details.append("File: ").append(imageName).append("\n\n");
        }

        // Resolution
        if (imageWidth > 0 && imageHeight > 0) {
            details.append("Resolution: ").append(imageWidth).append(" x ").append(imageHeight).append("\n");
        }

        // File size
        if (imageSize > 0) {
            details.append("File Size: ").append(formatFileSize(imageSize)).append("\n");
        }

        // Date
        if (imageDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            details.append("Date: ").append(sdf.format(imageDate)).append("\n");
        }

        // Encryption status
        if (isEncrypted) {
            details.append("Status: Encrypted");
        } else {
            details.append("Status: Unencrypted");
        }

        txtImageDetails.setText(details.toString());
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    @Override
    public void onBackPressed() {
        // Ensure we finish this activity properly without affecting main app
        finish();
    }

    public static void start(Context context, Media media, String decryptedFilePath) {
        Intent intent = new Intent(context, ImageViewerActivity.class);
        intent.putExtra(EXTRA_IMAGE_URI, media.contentUri);
        intent.putExtra(EXTRA_IMAGE_NAME, media.name);
        intent.putExtra(EXTRA_IMAGE_WIDTH, media.resolutionWidth);
        intent.putExtra(EXTRA_IMAGE_HEIGHT, media.resolutionHeight);
        intent.putExtra(EXTRA_IMAGE_SIZE, media.fileSize);
        intent.putExtra(EXTRA_IMAGE_DATE, media.date.getTime());
        intent.putExtra(EXTRA_IS_ENCRYPTED, media.is_encrypted);
        if (decryptedFilePath != null) {
            intent.putExtra(EXTRA_DECRYPTED_FILE_PATH, decryptedFilePath);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); // Ensure stays in same task
        context.startActivity(intent);
    }
}