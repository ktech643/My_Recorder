package com.checkmate.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Channel {
    @SerializedName("DeviceID")
    public int device_id;

    @SerializedName("ChannelID")
    public String channel_id;

    @SerializedName("RemoteOutputNumber")
    public int remote_output;

    @SerializedName("PublishUser")
    public String publish_user;

    @SerializedName("PublishPass")
    public String publish_pass;

    @SerializedName("ReadUser")
    public String read_user;

    @SerializedName("ReadPass")
    public String read_pass;

}
