package com.checkmate.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Device {
    @SerializedName("DeviceID")
    public int device_id;

    @SerializedName("DeviceSerial")
    public String device_serial;

    @SerializedName("Model")
    public String model;

    @SerializedName("Name")
    public String name;

    @SerializedName("SoftwareVersion")
    public String software_version;

    @SerializedName("Latitude")
    public String latitude;

    @SerializedName("Longitude")
    public String longitude;

    @SerializedName("BatteryStatus")
    public double battery_status;

    @SerializedName("Streaming")
    public boolean streaming;

    @SerializedName("Charging")
    public boolean charging;

    @SerializedName("AspectRatio")
    public String aspect_ratio;
}
