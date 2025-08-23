package com.checkmate.android.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.checkmate.android.R;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.Util;

public class VideoPlayerActivity extends AppCompatActivity {
    
    public static final String EXTRA_VIDEO_URI = "video_uri";
    public static final String EXTRA_VIDEO_NAME = "video_name";
    
    private PlayerView playerView;
    private ImageView btnClose;
    private ExoPlayer player;
    private boolean playWhenReady = true;
    private int currentWindow = 0;
    private long playbackPosition = 0;
    
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
    }
    
    private void setupListeners() {
        btnClose.setOnClickListener(v -> finish());
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
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}