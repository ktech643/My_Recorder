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
        IMAGE,  // Added IMAGE type
    }

    // Database fields
    public long id;  // Added id field for database operations
    public String path;  // Added path field for file path storage
    
    // Media properties
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
    
    // Constructor
    public Media() {
        // Default constructor
    }
    
    // Constructor with basic fields
    public Media(String name, String path, TYPE type, Date date) {
        this.name = name;
        this.path = path;
        this.type = type;
        this.date = date;
    }
    
    // Constructor with all fields
    public Media(long id, String name, String path, TYPE type, Date date, 
                 long duration, int resolutionWidth, int resolutionHeight, 
                 long fileSize, boolean is_encrypted) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.type = type;
        this.date = date;
        this.duration = duration;
        this.resolutionWidth = resolutionWidth;
        this.resolutionHeight = resolutionHeight;
        this.fileSize = fileSize;
        this.is_encrypted = is_encrypted;
    }
}
