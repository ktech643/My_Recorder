package com.checkmate.android.networking;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Created by Mobile Developer on 2/22/2017.
 */

public interface HttpApiEndPoint {

    /*
       Check app update
    */
    @POST("Updates/CheckVersion")
    Call<Responses.VersionResponse> checkVersion(
            @Body HashMap<String, String> body
    );

    /*
           Check app update
        */
    @POST("Devices/MobileActivation")
    @FormUrlEncoded
    Call<String> activate(
            @Field("SerialNumber") String serial_number,
            @Field("MachineCode") String machine_code
    );
}
