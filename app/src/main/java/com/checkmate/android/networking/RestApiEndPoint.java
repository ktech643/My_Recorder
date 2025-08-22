package com.checkmate.android.networking;

import com.checkmate.android.model.RegisteredDevice;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Created by Jorge Siu on 3/3/2021.
 */

public interface RestApiEndPoint {

    /*
           Code validation
        */

    @GET("codevalidation")
    Call<Responses.CodeValidationResponse> codeValidation(
            @Query("email") String email,
            @Query("code") String code
    );

    @POST("login")
    Call<Responses.LoginResponse> login(
            @Query("username") String email,
            @Query("password") String password,
            @Query("deviceserial") String device_serial,
            @Body String body
    );

    @GET("setpassword")
    Call<Responses.BaseResponse> setPassword(
            @Query("username") String email,
            @Query("token") String token,
            @Query("password") String password
    );

    @GET("resetpassword")
    Call<Responses.BaseResponse> resetPassword(
            @Query("email") String email
    );

    @GET("sendotc")
    Call<Responses.BaseResponse> requestOTCCode(
            @Query("username") String email
    );

    @GET("registerdevice")
    Call<Responses.RegisterDeviceResponse> registerDevice(
            @Query("deviceserial") String device_i,
            @Query("name") String device_name,
            @Query("licensestotaken") String license,
            @Query("model") String model,
            @Query("softwareversion") String soft_version
    );

    @GET("updatedeviceinfo")
    Call<Responses.BaseResponse> updateDevice(
            @Query("deviceserial") String device_serial,
            @Query("name") String name,
            @Query("latitude") String latitude,
            @Query("longitude") String longitude,
            @Query("batterystatus") int battery_status,
            @Query("streaming") boolean streaming,
            @Query("charging") boolean charging,
            @Query("aspectratio") String aspect_ratio,
            @Query("device_type") int device_type
    );

    @GET("updatedeviceinfo")
    Call<Responses.BaseResponse> updateDeviceWithoutLocation(
            @Query("deviceserial") String device_serial,
            @Query("name") String name,
            @Query("batterystatus") int battery_status,
            @Query("streaming") boolean streaming,
            @Query("charging") boolean charging,
            @Query("aspectratio") String aspect_ratio,
            @Query("device_type") int device_type
    );

    @GET("getinvites")
    Call<Responses.GetInvitesResponse> getInvites(
            @Query("deviceserial") String device_serial
    );

    @GET("getregistereddevices")
    Call<Responses.GetRegisteredDevicesResponse> getRegisteredDevices(

    );

    @GET("getregistereddeviceinfo")
    Call<Responses.GetRegisteredDevicesInfoResponse> getRegisteredDeviceInfo(
            @Query("deviceserial") String device_serial
    );

    @GET("sharedevice")
    Call<Responses.BaseResponse> shareDevice(
            @Query("email") String email,
            @Query("deviceserial") String device_serial
    );

    @GET("removesharedevice")
    Call<Responses.BaseResponse> removeShare(
            @Query("email") String email,
            @Query("deviceid") int device_id
    );

    @GET("removependinginvite")
    Call<Responses.BaseResponse> removeInvite(
            @Query("email") String email,
            @Query("deviceid") int device_id,
            @Query("code") int code
    );

    @GET("logout")
    Call<Responses.BaseResponse> logout(

    );

    @GET("logout")
    Call<Responses.BaseResponse> admin_logout(
            @Query("deviceserial") String device_serial
    );

    @GET("unregisterdevice")
    Call<Responses.BaseResponse> unregister_device(
            @Query("deviceserial") String device_serial
    );
}
