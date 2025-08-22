package com.checkmate.android.networking;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;


/**
 * n
 * Created by Mobile Developer on 2/22/2017.e
 */

public class HttpApiService {
    public final static String BASE_URL = "http://mobileact.t3ther.com/";

    private static HttpApiEndPoint mHttpApiService;

    static {
        setupClient();
    }

    public static HttpApiEndPoint getHttpApiEndPoint() {
        if (mHttpApiService != null) {
            mHttpApiService = null;
        }
        setupClient();
        return mHttpApiService;
    }

    public static void setupClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy'-'MM'-'dd")
                .create();

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS);
        httpClient.addInterceptor(logging);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build();
        mHttpApiService = retrofit.create(HttpApiEndPoint.class);
    }
}
