package com.checkmate.android.util;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

/**
 * Helper class for dynamic encoder configuration updates
 */
public class EncoderDynamicConfig {
    private static final String TAG = "EncoderDynamicConfig";
    
    /**
     * Update video encoder bitrate dynamically
     * @param encoder MediaCodec encoder instance
     * @param newBitrate New bitrate in bits per second
     * @return true if successful
     */
    public static boolean updateVideoBitrate(MediaCodec encoder, int newBitrate) {
        if (encoder == null) {
            Log.e(TAG, "Encoder is null");
            return false;
        }
        
        try {
            Bundle params = new Bundle();
            params.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, newBitrate);
            encoder.setParameters(params);
            Log.d(TAG, "Successfully updated video bitrate to: " + newBitrate);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to update video bitrate", e);
            return false;
        }
    }
    
    /**
     * Request key frame immediately
     * @param encoder MediaCodec encoder instance
     * @return true if successful
     */
    public static boolean requestKeyFrame(MediaCodec encoder) {
        if (encoder == null) {
            Log.e(TAG, "Encoder is null");
            return false;
        }
        
        try {
            Bundle params = new Bundle();
            params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
            encoder.setParameters(params);
            Log.d(TAG, "Key frame requested");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to request key frame", e);
            return false;
        }
    }
    
    /**
     * Update encoder frame rate (API 25+)
     * @param encoder MediaCodec encoder instance
     * @param frameRate New frame rate
     * @return true if successful
     */
    public static boolean updateFrameRate(MediaCodec encoder, float frameRate) {
        if (encoder == null) {
            Log.e(TAG, "Encoder is null");
            return false;
        }
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            Log.w(TAG, "Frame rate update requires API 25+");
            return false;
        }
        
        try {
            Bundle params = new Bundle();
            params.putFloat(MediaFormat.KEY_FRAME_RATE, frameRate);
            encoder.setParameters(params);
            Log.d(TAG, "Successfully updated frame rate to: " + frameRate);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to update frame rate", e);
            return false;
        }
    }
    
    /**
     * Check if a parameter can be updated dynamically
     * @param parameter Parameter key
     * @return true if the parameter supports dynamic updates
     */
    public static boolean isDynamicParameter(String parameter) {
        switch (parameter) {
            case MediaCodec.PARAMETER_KEY_VIDEO_BITRATE:
            case MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME:
                return true;
            case MediaFormat.KEY_FRAME_RATE:
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1;
            default:
                return false;
        }
    }
    
    /**
     * Get recommended bitrate based on resolution and frame rate
     * @param width Video width
     * @param height Video height
     * @param frameRate Frame rate
     * @return Recommended bitrate in bits per second
     */
    public static int getRecommendedBitrate(int width, int height, float frameRate) {
        // Base bitrate calculation
        int pixels = width * height;
        float bitsPerPixel = 0.1f; // Adjust based on quality requirements
        
        // Adjust for frame rate
        float frameRateFactor = frameRate / 30.0f;
        
        // Calculate bitrate
        int bitrate = (int) (pixels * bitsPerPixel * frameRate);
        
        // Apply min/max limits
        int minBitrate = 500_000; // 500 kbps minimum
        int maxBitrate = 50_000_000; // 50 Mbps maximum
        
        return Math.max(minBitrate, Math.min(maxBitrate, bitrate));
    }
}