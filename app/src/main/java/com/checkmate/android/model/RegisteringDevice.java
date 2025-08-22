package com.checkmate.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class RegisteringDevice {

    @SerializedName("Device")
    public Device device;

    @SerializedName("Channels")
    public List<Channel> channels = new ArrayList<>();
}
