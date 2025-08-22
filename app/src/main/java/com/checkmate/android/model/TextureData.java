package com.checkmate.android.model;

import android.graphics.SurfaceTexture;

public class TextureData {
    private SurfaceTexture surface;
    private int width;
    private int height;

    // Constructor to initialize all values
    public TextureData(SurfaceTexture surface, int width, int height) {
        this.surface = surface;
        this.width = width;
        this.height = height;
    }

    // Getter for SurfaceTexture
    public SurfaceTexture getSurface() {
        return surface;
    }

    // Setter for SurfaceTexture
    public void setSurface(SurfaceTexture surface) {
        this.surface = surface;
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
