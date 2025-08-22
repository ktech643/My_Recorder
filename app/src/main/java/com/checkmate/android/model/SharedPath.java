package com.checkmate.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class SharedPath {
    @SerializedName("Device")
    public Device device;

    @SerializedName("ViewChannels")
    public List<ViewChannel> view_channels = new ArrayList<>();

}
