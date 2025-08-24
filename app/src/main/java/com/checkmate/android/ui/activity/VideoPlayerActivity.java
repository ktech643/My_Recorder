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

import com.checkmate.android.R;
import com.checkmate.android.util.MediaMetadataUtils;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.Util;

import java.lang.ref.WeakReference;

public class VideoPlayerActivity extends AppCompatActivity {
    
    public static final String EXTRA_VIDEO_URI = "extra_video_uri";
    public static final String EXTRA_IS_ENCRYPTED = "extra_is_encrypted";
    
    private PlayerView playerView;
    private ExoPlayer player;
    private ImageView btnClose;
    private ImageView btnInfo;
    private ScrollView metadataPanel;
    private TextView txtMetadata;
    private View toolbar;
    
    private Uri videoUri;
    private boolean isEncrypted;
    private boolean isMetadataVisible = false;
    private MediaMetadataUtils.VideoMetadata metadata;
    private boolean playWhenReady = true;
    private int currentWindow = 0;
    private long playbackPosition = 0;

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
        
        setContentView(R.layout.activity_video_player);
        
        initViews();
        handleIntent();
        setupClickListeners();
    }
    
    private void initViews() {
        playerView = findViewById(R.id.player_view);
        btnClose = findViewById(R.id.btn_close);
        btnInfo = findViewById(R.id.btn_info);
        metadataPanel = findViewById(R.id.metadata_panel);
        txtMetadata = findViewById(R.id.txt_metadata);
        toolbar = findViewById(R.id.toolbar);
        
        // Configure PlayerView
        playerView.setUseController(true);
        playerView.setControllerAutoShow(true);
        playerView.setControllerHideOnTouch(true);
        
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
            String uriString = intent.getStringExtra(EXTRA_VIDEO_URI);
            isEncrypted = intent.getBooleanExtra(EXTRA_IS_ENCRYPTED, false);
            
            if (uriString != null) {
                videoUri = Uri.parse(uriString);
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
        
        // Hide metadata when clicking outside
        metadataPanel.setOnClickListener(v -> {
            if (isMetadataVisible) {
                toggleMetadata();
            }
        });
        
        // Set up player view listener to toggle toolbar
        playerView.setControllerVisibilityListener(visibility -> {
            if (visibility == View.VISIBLE) {
                toolbar.setVisibility(View.VISIBLE);
            } else if (!isMetadataVisible) {
                toolbar.setVisibility(View.GONE);
            }
        });
    }
    
    private void initializePlayer() {
        if (player == null && videoUri != null) {
            player = new ExoPlayer.Builder(this).build();
            
            playerView.setPlayer(player);
            
            MediaItem mediaItem = MediaItem.fromUri(videoUri);
            player.setMediaItem(mediaItem);
            
            player.setPlayWhenReady(playWhenReady);
            player.seekTo(currentWindow, playbackPosition);
            
            player.addListener(new Player.Listener() {
                @Override
                public void onPlayerError(PlaybackException error) {
                    // Handle playback errors
                    txtMetadata.setText("Error playing video: " + error.getMessage());
                    metadataPanel.setVisibility(View.VISIBLE);
                    isMetadataVisible = true;
                }
                
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY) {
                        // Video is ready to play
                    }
                }
            });
            
            player.prepare();
        }
    }
    
    private void loadMetadata() {
        if (videoUri != null) {
            new LoadMetadataTask(this).execute(videoUri);
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
    
    private static class LoadMetadataTask extends AsyncTask<Uri, Void, MediaMetadataUtils.VideoMetadata> {
        private final WeakReference<VideoPlayerActivity> activityRef;
        
        LoadMetadataTask(VideoPlayerActivity activity) {
            activityRef = new WeakReference<>(activity);
        }
        
        @Override
        protected MediaMetadataUtils.VideoMetadata doInBackground(Uri... uris) {
            VideoPlayerActivity activity = activityRef.get();
            if (activity == null || uris.length == 0) {
                return null;
            }
            
            return MediaMetadataUtils.extractVideoMetadata(activity, uris[0]);
        }
        
        @Override
        protected void onPostExecute(MediaMetadataUtils.VideoMetadata result) {
            VideoPlayerActivity activity = activityRef.get();
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
    protected void onStart() {
        super.onStart();
        if (Util.SDK_INT >= 24) {
            initializePlayer();
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
        
        if (Util.SDK_INT < 24 || player == null) {
            initializePlayer();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (Util.SDK_INT < 24) {
            releasePlayer();
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        if (Util.SDK_INT >= 24) {
            releasePlayer();
        }
    }
    
    private void releasePlayer() {
        if (player != null) {
            playWhenReady = player.getPlayWhenReady();
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentMediaItemIndex();
            player.release();
            player = null;
        }
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