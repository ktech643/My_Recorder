package com.checkmate.android.networking;

import android.content.Context;
import android.text.TextUtils;

import com.checkmate.android.AppConstant;
import com.checkmate.android.model.Device;
import com.checkmate.android.model.Invite;
import com.checkmate.android.model.RegisteredDevice;
import com.checkmate.android.model.RegisteringDevice;
import com.checkmate.android.model.SharedPath;
import com.checkmate.android.util.CommonUtil;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class Responses {

    public class BaseResponse {
        @SerializedName("Success")
        public boolean success;

        @SerializedName("Error")
        public String error;
    }

    public class VersionResponse {
        @SerializedName("version")
        public String version;

        @SerializedName("url")
        public String url;

        @SerializedName("date")
        public String date;

        @SerializedName("Error")
        public String error;
    }

    public class CodeValidationResponse {
        @SerializedName("Token")
        public String token;

        @SerializedName("Error")
        public String error;
    }

    public class LoginResponse {
        @SerializedName("Token")
        public String token;

        @SerializedName("Server")
        public String server;

        @SerializedName("TotalLicenses")
        public int total_licenses;

        @SerializedName("AvailableLicenses")
        public int available_license;

        @SerializedName("AccountStatus")
        public String account_status;

        @SerializedName("Expiration")
        public String expiration;

        @SerializedName("RTSPPort")
        public int port;

        @SerializedName("RTSPSPort")
        public int rtspsport;

        @SerializedName("RegisteredDevices")
        public List<RegisteredDevice> registered_devices;

        @SerializedName("SharedPaths")
        public List<SharedPath> shared_paths;

        @SerializedName("UserType")
        public String user_type;

        @SerializedName("Error")
        public String error;

        @Override
        public String toString() {
            return "LoginResponse{" +
                    "token='" + token + '\'' +
                    ", server='" + server + '\'' +
                    ", total_licenses=" + total_licenses +
                    ", available_license=" + available_license +
                    ", account_status='" + account_status + '\'' +
                    ", expiration='" + expiration + '\'' +
                    ", port=" + port +
                    ", rtspsport=" + rtspsport +
                    ", registered_devices=" + registered_devices +
                    ", shared_paths=" + shared_paths +
                    ", user_type='" + user_type + '\'' +
                    ", error='" + error + '\'' +
                    ", invites=" + invites +
                    ", registered_devices_info=" + registered_devices_info +
                    '}';
        }

        public boolean isAdmin() {
            return !TextUtils.isEmpty(user_type) && user_type.toLowerCase().contains("admin");
        }

        public boolean availableLicense(String device_serial) {
            if (available_license > 0) {
                return true;
            }
            boolean is_exist = false;
            for (RegisteredDevice device : registered_devices) {
                if (TextUtils.equals(device.device.device_serial, device_serial)) {
                    is_exist = true;
                }
            }

            return is_exist;
        }

        public List<Invite> invites = new ArrayList<>();
        public GetRegisteredDevicesInfoResponse registered_devices_info;
    }

    public class RegisterDeviceResponse {

        @SerializedName("Server")
        public String server;

        @SerializedName("RTSPPort")
        public int rtsp_port;

        @SerializedName("RTSPSPort")
        public int rtsps_port;

        @SerializedName("TotalLicenses")
        public int total_licenses;

        @SerializedName("AvailableLicenses")
        public int available_licenses;

        @SerializedName("RegisteredDevice")
        RegisteringDevice registeredDevice;

        @SerializedName("SharedPaths")
        public List<SharedPath> shared_paths;

        @SerializedName("Error")
        public String error;

        public boolean availableLicense() {
            return available_licenses > 0;
        }
    }

    public class GetInvitesResponse {
        @SerializedName("PendingInvites")
        public List<Invite> pending_invites = new ArrayList<>();

        @SerializedName("AcceptedInvites")
        public List<Invite> validated_invites = new ArrayList<>();

        @SerializedName("RejectedInvites")
        public List<Invite> rejected_invites = new ArrayList<>();


        public List<Invite> getAllInvites() {
            List<Invite> result = new ArrayList<>();
            for (Invite invite : pending_invites) {
                invite.invite_status = AppConstant.INVITE_PENDING;
                result.add(invite);
            }
            for (Invite invite : validated_invites) {
                invite.invite_status = AppConstant.INVITE_ACCEPTED;
                result.add(invite);
            }
            for (Invite invite : rejected_invites) {
                invite.invite_status = AppConstant.INVITE_REJECTED;
                result.add(invite);
            }
            return result;
        }
    }

    public class GetRegisteredDevicesResponse {
        @SerializedName("Server")
        public String server;

        @SerializedName("RTSPPort")
        public String rtsp_port;

        @SerializedName("RTSPSPort")
        public String rtsps_port;

        @SerializedName("RegisteredDevices")
        public List<RegisteredDevice> registeredDevices;

        @SerializedName("Error")
        public String error;
    }

    public class GetRegisteredDevicesInfoResponse {
        @SerializedName("TotalLicenses")
        public int total_licenses;

        @SerializedName("AvailableLicenses")
        public int available_licenses;

        @SerializedName("Server")
        public String server;

        @SerializedName("RTSPPort")
        public int rtsp_port;

        @SerializedName("RTSPSPort")
        public int rtsps_port;

        @SerializedName("RegisteredDevices")
        public List<RegisteredDevice> registered_devices;

        @SerializedName("SharedPaths")
        public List<SharedPath> shared_paths;

        @SerializedName("Error")
        public String error;
    }
}
