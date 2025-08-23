package com.checkmate.android.util;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import androidx.exifinterface.media.ExifInterface;
import com.checkmate.android.model.Media;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MetadataExtractor {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault());
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.#");
    
    public static void extractAndPopulateMetadata(Context context, Media media) {
        if (media.contentUri == null) return;
        
        try {
            // Extract format from filename
            if (media.name != null) {
                int dotIndex = media.name.lastIndexOf('.');
                if (dotIndex > 0 && dotIndex < media.name.length() - 1) {
                    media.format = media.name.substring(dotIndex + 1).toUpperCase();
                }
            }
            
            if (media.type == Media.TYPE.VIDEO) {
                extractVideoMetadata(context, media);
            } else if (media.type == Media.TYPE.PHOTO) {
                extractImageMetadata(context, media);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void extractVideoMetadata(Context context, Media media) {
        MediaMetadataRetriever retriever = null;
        try {
            retriever = new MediaMetadataRetriever();
            retriever.setDataSource(context, media.contentUri);
            
            // Basic video information
            String mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
            media.mimeType = mimeType;
            
            // Duration
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr != null) {
                media.duration = Long.parseLong(durationStr);
            }
            
            // Resolution
            String widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            if (widthStr != null && heightStr != null) {
                media.resolutionWidth = Integer.parseInt(widthStr);
                media.resolutionHeight = Integer.parseInt(heightStr);
            }
            
            // Frame Rate
            String frameRateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE);
            if (frameRateStr != null) {
                try {
                    media.frameRate = Float.parseFloat(frameRateStr);
                } catch (NumberFormatException e) {
                    media.frameRate = 0;
                }
            }
            
            // Bitrate
            String bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            if (bitrateStr != null) {
                try {
                    media.bitrate = Integer.parseInt(bitrateStr);
                } catch (NumberFormatException e) {
                    media.bitrate = 0;
                }
            }
            
            // Codec information - METADATA_KEY_VIDEO_CODEC_MIME_TYPE not available in this API level
            // Try to infer codec from MIME type or file extension
            if (mimeType != null) {
                if (mimeType.contains("video/avc") || mimeType.contains("video/mp4")) {
                    media.codec = "H.264/AVC";
                } else if (mimeType.contains("video/hevc") || mimeType.contains("video/mp4")) {
                    media.codec = "H.265/HEVC";
                } else if (mimeType.contains("video/x-vnd.on2.vp8")) {
                    media.codec = "VP8";
                } else if (mimeType.contains("video/x-vnd.on2.vp9")) {
                    media.codec = "VP9";
                } else if (mimeType.contains("video/av01")) {
                    media.codec = "AV1";
                } else {
                    media.codec = mimeType;
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (retriever != null) {
                try {
                    retriever.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private static void extractImageMetadata(Context context, Media media) {
        try {
            // First try to get basic metadata using MediaMetadataRetriever
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(context, media.contentUri);
                
                String mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
                media.mimeType = mimeType;
                
                String widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_IMAGE_WIDTH);
                String heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_IMAGE_HEIGHT);
                if (widthStr != null && heightStr != null) {
                    media.resolutionWidth = Integer.parseInt(widthStr);
                    media.resolutionHeight = Integer.parseInt(heightStr);
                }
            } catch (Exception e) {
                // Ignore if MediaMetadataRetriever fails for images
            } finally {
                try {
                    retriever.release();
                } catch (Exception e) {
                    // Ignore
                }
            }
            
            // Try to extract EXIF data
            extractExifData(context, media);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void extractExifData(Context context, Media media) {
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(media.contentUri);
            if (inputStream != null) {
                ExifInterface exif = new ExifInterface(inputStream);
                
                // Camera information
                String make = exif.getAttribute(ExifInterface.TAG_MAKE);
                String model = exif.getAttribute(ExifInterface.TAG_MODEL);
                if (make != null && model != null) {
                    media.cameraModel = make + " " + model;
                } else if (model != null) {
                    media.cameraModel = model;
                }
                
                // ISO
                String iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY);
                if (iso == null) {
                    iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED);
                }
                if (iso == null) {
                    iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS);
                }
                media.iso = iso;
                
                // Aperture
                String aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER);
                if (aperture != null) {
                    try {
                        double apertureValue = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0);
                        if (apertureValue > 0) {
                            media.aperture = "f/" + DECIMAL_FORMAT.format(apertureValue);
                        }
                    } catch (Exception e) {
                        media.aperture = "f/" + aperture;
                    }
                }
                
                // Shutter Speed
                String shutterSpeed = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);
                if (shutterSpeed != null) {
                    try {
                        double exposureTime = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0);
                        if (exposureTime > 0) {
                            if (exposureTime >= 1) {
                                media.shutterSpeed = DECIMAL_FORMAT.format(exposureTime) + "s";
                            } else {
                                // Convert to fraction for fast shutter speeds
                                int fraction = (int) Math.round(1.0 / exposureTime);
                                media.shutterSpeed = "1/" + fraction + "s";
                            }
                        }
                    } catch (Exception e) {
                        media.shutterSpeed = shutterSpeed + "s";
                    }
                }
                
                // Focal Length
                String focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH);
                if (focalLength != null) {
                    try {
                        double focalLengthValue = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0);
                        if (focalLengthValue > 0) {
                            media.focalLength = DECIMAL_FORMAT.format(focalLengthValue) + "mm";
                        }
                    } catch (Exception e) {
                        media.focalLength = focalLength + "mm";
                    }
                }
                
                // If we didn't get resolution from MediaMetadataRetriever, try EXIF
                if (media.resolutionWidth == 0 || media.resolutionHeight == 0) {
                    int width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0);
                    int height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0);
                    if (width > 0 && height > 0) {
                        media.resolutionWidth = width;
                        media.resolutionHeight = height;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }
    
    public static String formatFileSize(long sizeInBytes) {
        if (sizeInBytes == 0) return "0 B";
        
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(sizeInBytes) / Math.log10(1024));
        
        return DECIMAL_FORMAT.format(sizeInBytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
    
    public static String formatDuration(long durationMs) {
        if (durationMs <= 0) return "00:00";
        
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        seconds = seconds % 60;
        minutes = minutes % 60;
        
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }
    
    public static String formatBitrate(int bitrateInBps) {
        if (bitrateInBps <= 0) return "Unknown";
        
        if (bitrateInBps >= 1000000) {
            return DECIMAL_FORMAT.format(bitrateInBps / 1000000.0) + " Mbps";
        } else if (bitrateInBps >= 1000) {
            return DECIMAL_FORMAT.format(bitrateInBps / 1000.0) + " Kbps";
        } else {
            return bitrateInBps + " bps";
        }
    }
    
    public static String formatFrameRate(float fps) {
        if (fps <= 0) return "Unknown";
        
        if (fps == (int) fps) {
            return (int) fps + " fps";
        } else {
            return DECIMAL_FORMAT.format(fps) + " fps";
        }
    }
    
    public static String formatDate(java.util.Date date) {
        if (date == null) return "Unknown";
        return DATE_FORMAT.format(date);
    }
}