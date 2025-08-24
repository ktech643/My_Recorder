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
}
