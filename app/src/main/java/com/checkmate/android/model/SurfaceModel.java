package com.checkmate.android.model;

import android.graphics.SurfaceTexture;

public class SurfaceModel {
    private SurfaceTexture surfaceTexture;
    private int width;
    private int height;

    // Constructor to initialize all values
    public SurfaceModel(SurfaceTexture surfaceTexture, int width, int height) {
        this.surfaceTexture = surfaceTexture;
        this.width = width;
        this.height = height;
    }

    public void setPreviewSurface(final SurfaceTexture surfaceTexture){
        this.surfaceTexture = surfaceTexture;
    }

    // Getter for SurfaceTexture
    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }

    // Setter for SurfaceTexture
    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        this.surfaceTexture = surfaceTexture;
    }

    // Getter for width
    public int getWidth() {
        return width;
    }

    // Setter for width
    public void setWidth(int width) {
        this.width = width;
    }

    // Getter for height
    public int getHeight() {
        return height;
    }

    // Setter for height
    public void setHeight(int height) {
        this.height = height;
    }
}
