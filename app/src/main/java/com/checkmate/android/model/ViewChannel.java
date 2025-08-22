package com.checkmate.android.model;

import com.google.gson.annotations.SerializedName;

public class ViewChannel {
    @SerializedName("ChannelID")
    public String channel_id;

    @SerializedName("RemoteOutputNumber")
    public int remote_output_number;

    @SerializedName("ReadUser")
    public String read_user;

    @SerializedName("ReadPass")
    public String read_pass;
}
