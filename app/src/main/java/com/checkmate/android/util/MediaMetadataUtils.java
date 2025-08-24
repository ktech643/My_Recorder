package com.checkmate.android.util;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.util.Log;
import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MediaMetadataUtils {
    private static final String TAG = "MediaMetadataUtils";

    public static class ImageMetadata {
        public String fileName;
        public long fileSize;
        public String createdDate;
        public String modifiedDate;
        public String type;
        public int width;
        public int height;
        public String resolution;
        public String cameraModel;
        public String location;
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("File: ").append(fileName).append("\n");
            sb.append("Size: ").append(formatFileSize(fileSize)).append("\n");
            sb.append("Type: ").append(type).append("\n");
            sb.append("Resolution: ").append(resolution).append("\n");
            sb.append("Created: ").append(createdDate).append("\n");
            if (cameraModel != null && !cameraModel.isEmpty()) {
                sb.append("Camera: ").append(cameraModel).append("\n");
            }
            return sb.toString();
        }
    }

    public static class VideoMetadata {
        public String fileName;
        public long fileSize;
        public String createdDate;
        public String modifiedDate;
        public String type;
        public int width;
        public int height;
        public String resolution;
        public long duration;
        public String durationFormatted;
        public String frameRate;
        public String bitrate;
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("File: ").append(fileName).append("\n");
            sb.append("Size: ").append(formatFileSize(fileSize)).append("\n");
            sb.append("Type: ").append(type).append("\n");
            sb.append("Resolution: ").append(resolution).append("\n");
            sb.append("Duration: ").append(durationFormatted).append("\n");
            if (frameRate != null && !frameRate.isEmpty()) {
                sb.append("Frame Rate: ").append(frameRate).append(" fps\n");
            }
            if (bitrate != null && !bitrate.isEmpty()) {
                sb.append("Bitrate: ").append(bitrate).append("\n");
            }
            sb.append("Created: ").append(createdDate).append("\n");
            return sb.toString();
        }
    }

    public static ImageMetadata extractImageMetadata(Context context, Uri uri) {
        ImageMetadata metadata = new ImageMetadata();
        
        try {
            // Get basic file info
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    
                    if (nameIndex != -1) {
                        metadata.fileName = cursor.getString(nameIndex);
                    }
                    if (sizeIndex != -1) {
                        metadata.fileSize = cursor.getLong(sizeIndex);
                    }
                }
            }
            
            // Get EXIF data
            try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                if (inputStream != null) {
                    ExifInterface exif = new ExifInterface(inputStream);
                    
                    // Get creation date
                    String dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME);
                    if (dateTime != null) {
                        metadata.createdDate = formatDateTime(dateTime);
                    } else {
                        metadata.createdDate = "Unknown";
                    }
                    
                    // Get camera model
                    metadata.cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL);
                    
                    // Get image dimensions
                    metadata.width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0);
                    metadata.height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading EXIF data", e);
            }
            
            // If EXIF dimensions not available, try BitmapFactory
            if (metadata.width == 0 || metadata.height == 0) {
                try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                    if (inputStream != null) {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeStream(inputStream, null, options);
                        metadata.width = options.outWidth;
                        metadata.height = options.outHeight;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error getting image dimensions", e);
                }
            }
            
            // Set resolution string
            if (metadata.width > 0 && metadata.height > 0) {
                metadata.resolution = metadata.width + " × " + metadata.height;
            } else {
                metadata.resolution = "Unknown";
            }
            
            // Determine file type
            String mimeType = context.getContentResolver().getType(uri);
            metadata.type = mimeType != null ? mimeType : "Unknown";
            
            // Set created date if not available from EXIF
            if (metadata.createdDate == null || metadata.createdDate.equals("Unknown")) {
                try {
                    long lastModified = DocumentsContract.getDocumentId(uri) != null ? 
                        System.currentTimeMillis() : System.currentTimeMillis();
                    metadata.createdDate = formatTimestamp(lastModified);
                } catch (Exception e) {
                    metadata.createdDate = "Unknown";
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting image metadata", e);
        }
        
        return metadata;
    }

    public static VideoMetadata extractVideoMetadata(Context context, Uri uri) {
        VideoMetadata metadata = new VideoMetadata();
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        
        try {
            // Get basic file info
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    
                    if (nameIndex != -1) {
                        metadata.fileName = cursor.getString(nameIndex);
                    }
                    if (sizeIndex != -1) {
                        metadata.fileSize = cursor.getLong(sizeIndex);
                    }
                }
            }
            
            // Use MediaMetadataRetriever for video metadata
            retriever.setDataSource(context, uri);
            
            // Get video dimensions
            String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            
            if (width != null && height != null) {
                metadata.width = Integer.parseInt(width);
                metadata.height = Integer.parseInt(height);
                metadata.resolution = metadata.width + " × " + metadata.height;
            } else {
                metadata.resolution = "Unknown";
            }
            
            // Get duration
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr != null) {
                metadata.duration = Long.parseLong(durationStr);
                metadata.durationFormatted = formatDuration(metadata.duration);
            } else {
                metadata.durationFormatted = "Unknown";
            }
            
            // Get frame rate
            String frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE);
            if (frameRate != null) {
                metadata.frameRate = String.format(Locale.getDefault(), "%.1f", Float.parseFloat(frameRate));
            }
            
            // Get bitrate
            String bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            if (bitrate != null) {
                long bitrateValue = Long.parseLong(bitrate);
                metadata.bitrate = formatBitrate(bitrateValue);
            }
            
            // Get creation date
            String date = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);
            if (date != null) {
                metadata.createdDate = formatVideoDate(date);
            } else {
                metadata.createdDate = "Unknown";
            }
            
            // Determine file type
            String mimeType = context.getContentResolver().getType(uri);
            metadata.type = mimeType != null ? mimeType : "Unknown";
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting video metadata", e);
            metadata.createdDate = "Unknown";
            metadata.durationFormatted = "Unknown";
            metadata.resolution = "Unknown";
        } finally {
            try {
                retriever.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing MediaMetadataRetriever", e);
            }
        }
        
        return metadata;
    }

    private static String formatDateTime(String dateTime) {
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            Date date = input.parse(dateTime);
            return output.format(date);
        } catch (Exception e) {
            return dateTime; // Return original if parsing fails
        }
    }

    private static String formatTimestamp(long timestamp) {
        SimpleDateFormat format = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        return format.format(new Date(timestamp));
    }

    private static String formatVideoDate(String date) {
        try {
            // Video date format can vary, try common formats
            String[] formats = {
                "yyyyMMdd'T'HHmmss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyyMMdd'T'HHmmss'Z'",
                "yyyy-MM-dd HH:mm:ss"
            };
            
            SimpleDateFormat output = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            
            for (String format : formats) {
                try {
                    SimpleDateFormat input = new SimpleDateFormat(format, Locale.getDefault());
                    Date parsedDate = input.parse(date);
                    return output.format(parsedDate);
                } catch (Exception ignored) {
                    // Try next format
                }
            }
            
            return date; // Return original if no format matches
        } catch (Exception e) {
            return date;
        }
    }

    public static String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, secs);
        }
    }

    public static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        
        return String.format(Locale.getDefault(), "%.1f %s", 
            bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private static String formatBitrate(long bitrate) {
        if (bitrate < 1000) {
            return bitrate + " bps";
        } else if (bitrate < 1000000) {
            return String.format(Locale.getDefault(), "%.1f Kbps", bitrate / 1000.0);
        } else {
            return String.format(Locale.getDefault(), "%.1f Mbps", bitrate / 1000000.0);
        }
    }
}