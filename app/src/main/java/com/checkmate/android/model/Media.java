package com.checkmate.android.model;

import android.graphics.Bitmap;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.util.Date;

public class Media {
    public enum TYPE {
        VIDEO,
        PHOTO,
    }

    public TYPE type;
    public Bitmap bitmap;
    public String video_path;
    public String name;
    public Date date;
    public long duration;
    public int resolutionWidth;
    public int resolutionHeight;
    public long fileSize;
    public DocumentFile file;
    public Uri contentUri;
    public boolean is_selected;
    public boolean is_encrypted = false;
    
    // Additional metadata fields
    public String mimeType;
    public String format; // File extension/format
    public float frameRate; // FPS for videos
    public int bitrate; // Bitrate for videos
    public String codec; // Video codec
    public String cameraModel; // EXIF camera model
    public String iso; // EXIF ISO value
    public String aperture; // EXIF aperture
    public String shutterSpeed; // EXIF shutter speed
    public String focalLength; // EXIF focal length
}
