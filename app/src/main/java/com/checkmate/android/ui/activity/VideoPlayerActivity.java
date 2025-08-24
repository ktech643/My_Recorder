package com.checkmate.android.ui.activity;

import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.checkmate.android.R;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.Util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VideoPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_URI = "video_uri";
    public static final String EXTRA_VIDEO_NAME = "video_name";
    public static final String EXTRA_IS_ENCRYPTED = "is_encrypted";
    public static final String EXTRA_VIDEO_DATE = "video_date";
    public static final String EXTRA_VIDEO_SIZE = "video_size";
    public static final String EXTRA_VIDEO_DURATION = "video_duration";
    public static final String EXTRA_VIDEO_WIDTH = "video_width";
    public static final String EXTRA_VIDEO_HEIGHT = "video_height";
    
    private PlayerView playerView;
    private ExoPlayer player;
    private ImageView btnClose;
    private ImageView btnInfo;
    private View metadataContainer;
    private TextView tvMetadata;
    
    private Uri videoUri;
    private String videoName;
    private boolean isEncrypted;
    private long videoDate;
    private long videoSize;
    private long videoDuration;
    private int videoWidth;
    private int videoHeight;
    
    private boolean playWhenReady = true;
    private int currentWindow = 0;
    private long playbackPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        
        // Get data from intent
        videoUri = getIntent().getParcelableExtra(EXTRA_VIDEO_URI);
        videoName = getIntent().getStringExtra(EXTRA_VIDEO_NAME);
        isEncrypted = getIntent().getBooleanExtra(EXTRA_IS_ENCRYPTED, false);
        videoDate = getIntent().getLongExtra(EXTRA_VIDEO_DATE, 0);
        videoSize = getIntent().getLongExtra(EXTRA_VIDEO_SIZE, 0);
        videoDuration = getIntent().getLongExtra(EXTRA_VIDEO_DURATION, 0);
        videoWidth = getIntent().getIntExtra(EXTRA_VIDEO_WIDTH, 0);
        videoHeight = getIntent().getIntExtra(EXTRA_VIDEO_HEIGHT, 0);
        
        if (videoUri == null) {
            finish();
            return;
        }
        
        initViews();
        setupMetadata();
    }
    
    private void initViews() {
        playerView = findViewById(R.id.player_view);
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
    
    private void setupMetadata() {
        StringBuilder metadata = new StringBuilder();
        
        // File name
        metadata.append("Name: ").append(videoName).append("\n\n");
        
        // Created date
        if (videoDate > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            metadata.append("Created: ").append(sdf.format(new Date(videoDate))).append("\n\n");
        }
        
        // File size
        if (videoSize > 0) {
            String formattedSize = formatFileSize(videoSize);
            metadata.append("Size: ").append(formattedSize).append("\n\n");
        }
        
        // Type
        String fileExtension = "";
        if (videoName.contains(".")) {
            fileExtension = videoName.substring(videoName.lastIndexOf(".") + 1).toUpperCase();
        }
        metadata.append("Type: ").append(fileExtension).append(" Video\n\n");
        
        // Duration
        if (videoDuration > 0) {
            String duration = formatDuration(videoDuration);
            metadata.append("Duration: ").append(duration).append("\n\n");
        }
        
        // Resolution
        if (videoWidth > 0 && videoHeight > 0) {
            metadata.append("Resolution: ").append(videoWidth).append(" x ").append(videoHeight).append(" pixels\n\n");
        }
        
        // Try to get additional metadata if not already provided
        if ((videoWidth == 0 || videoHeight == 0 || videoDuration == 0) && !isEncrypted) {
            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(this, videoUri);
                
                String fps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE);
                if (fps != null) {
                    metadata.append("FPS: ").append(fps).append("\n\n");
                }
                
                String bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
                if (bitrate != null) {
                    try {
                        long bitrateValue = Long.parseLong(bitrate);
                        metadata.append("Bitrate: ").append(formatBitrate(bitrateValue)).append("\n\n");
                    } catch (NumberFormatException e) {
                        // Ignore
                    }
                }
                
                String codec = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_CODEC);
                if (codec != null) {
                    metadata.append("Codec: ").append(codec).append("\n\n");
                }
                
                retriever.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        if (isEncrypted) {
            metadata.append("Status: Encrypted\n");
        }
        
        tvMetadata.setText(metadata.toString().trim());
    }
    
    private void initializePlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        
        MediaItem mediaItem = MediaItem.fromUri(videoUri);
        player.setMediaItem(mediaItem);
        
        player.setPlayWhenReady(playWhenReady);
        player.seekTo(currentWindow, playbackPosition);
        player.prepare();
        
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                // Handle player errors
                error.printStackTrace();
            }
        });
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
        if (Util.SDK_INT < 24 || player == null) {
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
    
    private void releasePlayer() {
        if (player != null) {
            playWhenReady = player.getPlayWhenReady();
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentWindowIndex();
            player.release();
            player = null;
        }
    }
    
    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format(Locale.US, "%.2f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
    
    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;
        
        if (hours > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }
    
    private String formatBitrate(long bitrate) {
        if (bitrate <= 0) return "0 bps";
        if (bitrate < 1000) return bitrate + " bps";
        if (bitrate < 1000000) return String.format(Locale.US, "%.1f Kbps", bitrate / 1000.0);
        return String.format(Locale.US, "%.1f Mbps", bitrate / 1000000.0);
    }
}