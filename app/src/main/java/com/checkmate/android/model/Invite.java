package com.checkmate.android.model;

import com.checkmate.android.AppConstant;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Invite {

    @SerializedName("DeviceID")
    public int device_id;

    @SerializedName("DeviceSerial")
    public String device_serial;

    @SerializedName("Username")
    public String username;

    public int invite_status;
    public boolean isValidated() {
        return invite_status == AppConstant.INVITE_ACCEPTED;
    }
    public boolean isPending() {
        return invite_status == AppConstant.INVITE_PENDING;
    }
    public boolean isRejected() {
        return invite_status == AppConstant.INVITE_REJECTED;
    }
}
