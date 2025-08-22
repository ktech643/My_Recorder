package com.checkmate.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.AspectRatioFrameLayout;

import com.checkmate.android.model.Media;
import com.checkmate.android.R;

/**
 * Lightweight video‐player activity that relies entirely on Google's Media3 / ExoPlayer
 * for rendering, controls, and aspect‐ratio handling.
 */
public class VideoPlayerActivity extends AppCompatActivity {

    // Static launcher — same signature the rest of the codebase already uses.
    public static final String EXTRA_VIDEO_URI = "video_uri";
    public static final String EXTRA_VIDEO_NAME = "video_name";
    public static final String EXTRA_DECRYPTED_FILE_PATH = "decrypted_file_path";

    public static void start(Context context, Media media, @Nullable String decryptedFilePath) {
        Intent intent = new Intent(context, VideoPlayerActivity.class);
        intent.putExtra(EXTRA_VIDEO_URI, media.contentUri);
        intent.putExtra(EXTRA_VIDEO_NAME, media.name);
        if (decryptedFilePath != null) {
            intent.putExtra(EXTRA_DECRYPTED_FILE_PATH, decryptedFilePath);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); // Ensure stays in same task
        context.startActivity(intent);
    }

    private PlayerView playerView;
    private ExoPlayer player;
    private ImageButton btnClose;
    private TextView txtVideoName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        
        // Initialize UI components
        playerView = findViewById(R.id.player_view);
        btnClose = findViewById(R.id.btn_close);
        txtVideoName = findViewById(R.id.txt_video_name);

        // Set up close button
        btnClose.setOnClickListener(v -> finish());

        // Center-crop video instead of letterboxing
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);

        // Build the player
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        // Extract Uri and video name (use decrypted path if supplied)
        Intent intent = getIntent();
        Uri uri;
        String videoName = intent.getStringExtra(EXTRA_VIDEO_NAME);
        String decrypted = intent.getStringExtra(EXTRA_DECRYPTED_FILE_PATH);
        
        if (decrypted != null) {
            uri = Uri.parse("file://" + decrypted);
        } else {
            uri = intent.getParcelableExtra(EXTRA_VIDEO_URI);
        }
        
        if (uri == null) {
            finish();
            return;
        }

        // Set video name
        if (videoName != null && !videoName.isEmpty()) {
            txtVideoName.setText(videoName);
        } else {
            txtVideoName.setText("Video");
        }

        // Prepare media
        player.setMediaItem(MediaItem.fromUri(uri));
        player.prepare();
        player.play();
    }

    // Keep screen on during playback (playerView attribute already set but double-ensure)
    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.play();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
    
    @Override
    public void onBackPressed() {
        // Ensure we finish this activity properly without affecting main app
        finish();
    }
}
