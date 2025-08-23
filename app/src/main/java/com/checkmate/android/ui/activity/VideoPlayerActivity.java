package com.checkmate.android.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.checkmate.android.R;
import com.checkmate.android.model.Media;
import com.checkmate.android.util.MetadataExtractor;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.Util;

public class VideoPlayerActivity extends AppCompatActivity {
    
    public static final String EXTRA_VIDEO_URI = "video_uri";
    public static final String EXTRA_VIDEO_NAME = "video_name";
    public static final String EXTRA_FILE_SIZE = "file_size";
    public static final String EXTRA_DATE = "date";
    
    private PlayerView playerView;
    private ImageView btnClose;
    private ImageView btnInfo;
    private View metadataOverlay;
    private ExoPlayer player;
    private boolean playWhenReady = true;
    private int currentWindow = 0;
    private long playbackPosition = 0;
    private Media currentMedia;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        
        initViews();
        setupListeners();
    }
    
    private void initViews() {
        playerView = findViewById(R.id.player_view);
        btnClose = findViewById(R.id.btn_close);
        btnInfo = findViewById(R.id.btn_info);
        metadataOverlay = findViewById(R.id.metadata_overlay);
    }
    
    private void setupListeners() {
        btnClose.setOnClickListener(v -> finish());
        btnInfo.setOnClickListener(v -> toggleMetadataOverlay());
        
        // Hide metadata overlay when close button in overlay is clicked
        ImageView btnCloseMetadata = metadataOverlay.findViewById(R.id.btn_close_metadata);
        if (btnCloseMetadata != null) {
            btnCloseMetadata.setOnClickListener(v -> hideMetadataOverlay());
        }
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT >= 24) {
            initializePlayer();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        hideSystemUi();
        if ((Util.SDK_INT < 24 || player == null)) {
            initializePlayer();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT < 24) {
            releasePlayer();
        }
    }
    
    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT >= 24) {
            releasePlayer();
        }
    }
    
    private void initializePlayer() {
        if (player == null) {
            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);
            
            Intent intent = getIntent();
            String videoUriString = intent.getStringExtra(EXTRA_VIDEO_URI);
            String videoName = intent.getStringExtra(EXTRA_VIDEO_NAME);
            long fileSize = intent.getLongExtra(EXTRA_FILE_SIZE, 0);
            long date = intent.getLongExtra(EXTRA_DATE, 0);
            
            if (videoUriString != null) {
                Uri videoUri = Uri.parse(videoUriString);
                MediaItem mediaItem = MediaItem.fromUri(videoUri);
                
                player.setMediaItem(mediaItem);
                player.setPlayWhenReady(playWhenReady);
                player.seekTo(currentWindow, playbackPosition);
                
                // Set rewind and fast forward increments to 10 seconds
                player.setSeekBackIncrementMs(10000);
                player.setSeekForwardIncrementMs(10000);
                
                player.prepare();
                
                // Create media object for metadata extraction
                currentMedia = new Media();
                currentMedia.contentUri = videoUri;
                currentMedia.name = videoName;
                currentMedia.type = Media.TYPE.VIDEO;
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
            
            // Add listener to handle player events
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    switch (state) {
                        case Player.STATE_ENDED:
                            // Reset to beginning when video ends
                            player.seekTo(0);
                            player.setPlayWhenReady(false);
                            break;
                    }
                }
            });
        }
    }
    
    private void releasePlayer() {
        if (player != null) {
            playWhenReady = player.getPlayWhenReady();
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentWindowIndex();
            player.release();
            player = null;
        }
    }
    
    private void hideSystemUi() {
        playerView.setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );
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
        
        // Video-specific metadata
        TextView txtMetaDuration = metadataOverlay.findViewById(R.id.txt_meta_duration);
        TextView txtMetaFps = metadataOverlay.findViewById(R.id.txt_meta_fps);
        TextView txtMetaBitrate = metadataOverlay.findViewById(R.id.txt_meta_bitrate);
        TextView txtMetaCodec = metadataOverlay.findViewById(R.id.txt_meta_codec);
        
        LinearLayout layoutDuration = metadataOverlay.findViewById(R.id.layout_duration);
        LinearLayout layoutFps = metadataOverlay.findViewById(R.id.layout_fps);
        LinearLayout layoutBitrate = metadataOverlay.findViewById(R.id.layout_bitrate);
        LinearLayout layoutCodec = metadataOverlay.findViewById(R.id.layout_codec);
        
        // Hide image-specific layouts
        View layoutExif = metadataOverlay.findViewById(R.id.layout_exif);
        if (layoutExif != null) layoutExif.setVisibility(View.GONE);
        
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
        
        // Set video-specific metadata
        if (currentMedia.duration > 0 && txtMetaDuration != null && layoutDuration != null) {
            txtMetaDuration.setText(MetadataExtractor.formatDuration(currentMedia.duration));
            layoutDuration.setVisibility(View.VISIBLE);
        }
        
        if (currentMedia.frameRate > 0 && txtMetaFps != null && layoutFps != null) {
            txtMetaFps.setText(MetadataExtractor.formatFrameRate(currentMedia.frameRate));
            layoutFps.setVisibility(View.VISIBLE);
        }
        
        if (currentMedia.bitrate > 0 && txtMetaBitrate != null && layoutBitrate != null) {
            txtMetaBitrate.setText(MetadataExtractor.formatBitrate(currentMedia.bitrate));
            layoutBitrate.setVisibility(View.VISIBLE);
        }
        
        if (currentMedia.codec != null && !currentMedia.codec.isEmpty() && txtMetaCodec != null && layoutCodec != null) {
            txtMetaCodec.setText(currentMedia.codec);
            layoutCodec.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}