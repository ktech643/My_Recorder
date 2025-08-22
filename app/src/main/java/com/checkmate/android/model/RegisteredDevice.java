package com.checkmate.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class RegisteredDevice {

    @SerializedName("Device")
    public Device device;

    @SerializedName("PushChannels")
    public List<PushChannel> push_channels = new ArrayList<>();
}
