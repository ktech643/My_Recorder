package com.checkmate.android.model;

public class CameraSelectionModel {
    private String name;
    private String type;
    private String status;
    private int iconResource;
    private boolean isSelected;
    private Camera wifiCamera; // For wifi cameras
    
    public CameraSelectionModel(String name, String type, int iconResource) {
        this.name = name;
        this.type = type;
        this.iconResource = iconResource;
        this.isSelected = false;
    }
    
    public CameraSelectionModel(String name, String type, int iconResource, Camera wifiCamera) {
        this.name = name;
        this.type = type;
        this.iconResource = iconResource;
        this.wifiCamera = wifiCamera;
        this.isSelected = false;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getIconResource() {
        return iconResource;
    }

    public void setIconResource(int iconResource) {
        this.iconResource = iconResource;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public Camera getWifiCamera() {
        return wifiCamera;
    }

    public void setWifiCamera(Camera wifiCamera) {
        this.wifiCamera = wifiCamera;
    }
}