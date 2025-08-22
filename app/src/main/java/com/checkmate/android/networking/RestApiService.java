package com.checkmate.android.networking;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.checkmate.android.AppConstant;
import com.checkmate.android.AppPreference;
import com.checkmate.android.BuildConfig;
import com.checkmate.android.MyApp;
import com.checkmate.android.R;
import com.checkmate.android.util.MessageUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;


/**
 * n
 * Created by Jorge Siu on 3/3/2021.e
 */

public class RestApiService {
    //    public static String DNS = "70.35.203.243";
    public static String DNS = "stream01.vcsrelay.com";
    //    public static String BASE_URL = "https://" + DNS + ":6009/v1/";
    public static String BASE_URL = "https://" + DNS + ":6010/v1/";
    //    static String BASE_URL = "https://testcloud.vcsrelay.com:6009/v1/";
    private static RestApiEndPoint mRestApiService;

    static {
        if (BuildConfig.DEBUG) {
            DNS = "70.35.203.243";
            DNS = "stream01.vcsrelay.com";
        } else {
            DNS = "stream01.vcsrelay.com";
        }

        if (Build.VERSION.SDK_INT >= 33) {
            BASE_URL = "https://" + DNS + ":6010/v1/";
        } else {
            BASE_URL = "https://" + DNS + ":6010/v1/";
        }
        try {
            setupClient();
        } catch (Exception e) {
            e.printStackTrace();
            MessageUtil.showToast(MyApp.getContext(), e.getLocalizedMessage());
        }
    }

    private static SSLContext getSSLConfig(Context context) throws CertificateException, IOException,
            KeyStoreException, NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException {

        // Loading CAs from an InputStream
        InputStream stream = context.getResources().openRawResource(R.raw.t3thercloudworker);
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(stream, AppConstant.CertPassword.toCharArray());
        stream.close();

        // Creating a TrustManager that trusts the CAs in our KeyStore.
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);

        // Retrieve the X509TrustManager from the factory
        TrustManager[] trustManagers = tmf.getTrustManagers();
        X509TrustManager trustManager = null;
        for (TrustManager trustManagerCandidate : trustManagers) {
            if (trustManagerCandidate instanceof X509TrustManager) {
                trustManager = (X509TrustManager) trustManagerCandidate;
                break;
            }
        }
        if (trustManager == null) {
            throw new IllegalStateException("No X509TrustManager found");
        }

        // Creating keymanager
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X509");
        keyManagerFactory.init(keyStore, AppConstant.CertPassword.toCharArray());

        // Creating an SSLSocketFactory that uses our TrustManager
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), new TrustManager[]{trustManager}, null);

        return sslContext;
    }

    public static RestApiEndPoint getRestApiEndPoint() {
        if (mRestApiService != null) {
            mRestApiService = null;
        }
        try {
            setupClient();
        } catch (Exception e) {
            e.printStackTrace();
            MessageUtil.showToast(MyApp.getContext(), e.getLocalizedMessage());
        }
        return mRestApiService;
    }

    public static void setupClient() throws Exception {
        // Initialize logging interceptor
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Initialize Gson
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy'-'MM'-'dd")
                .create();

        // Load the custom certificate from raw resources
        Context context = MyApp.getContext();
        int certificate = 0;

        if (Build.VERSION.SDK_INT >= 33) {
            certificate = R.raw.t3thercloudworker2023a;
        } else {
            certificate = R.raw.t3thercloudworker2023a;
        }

//        InputStream stream = context.getResources().openRawResource(R.raw.t3thercloudworker);
        InputStream stream = context.getResources().openRawResource(certificate);
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(stream, AppConstant.CertPassword.toCharArray());
        stream.close();

        // Initialize TrustManagerFactory with the KeyStore
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);

        // Extract X509TrustManager
        X509TrustManager trustManager = null;
        for (javax.net.ssl.TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                trustManager = (X509TrustManager) tm;
                break;
            }
        }
        if (trustManager == null) {
            throw new IllegalStateException("No X509TrustManager found");
        }

        // Initialize KeyManagerFactory with the KeyStore
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, AppConstant.CertPassword.toCharArray());

        // Initialize SSLContext with KeyManagers and TrustManagers
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), new javax.net.ssl.TrustManager[]{trustManager}, null);
        javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        // Build OkHttpClient with both sslSocketFactory and trustManager
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .sslSocketFactory(sslSocketFactory, trustManager) // Provide both SSLSocketFactory and X509TrustManager
                .hostnameVerifier((hostname, session) -> true); // Replace with proper verifier in production

        // Add logging interceptor
        httpClient.addInterceptor(logging);

        // Add Authorization header if token exists
        String token = AppPreference.getStr(AppPreference.KEY.USER_TOKEN, "");
        if (!TextUtils.isEmpty(token)) {
            httpClient.addInterceptor(chain -> {
                Request request = chain.request().newBuilder()
                        .addHeader("Authorization", String.format("Bearer %s", token))
                        .build();
                return chain.proceed(request);
            });
        }

        // Adjust BASE_URL if activation serial matches beta serial
        String url = BASE_URL;
        if (TextUtils.equals(AppPreference.getStr(AppPreference.KEY.ACTIVATION_SERIAL, ""), AppConstant.BETA_SERIAL)) {
            if (Build.VERSION.SDK_INT >= 33) {
                url = "https://" + AppPreference.getStr(AppPreference.KEY.BETA_URL, DNS) + ":6010/v1/";
            } else {
                url = "https://" + AppPreference.getStr(AppPreference.KEY.BETA_URL, DNS) + ":6010/v1/";
            }
        }

        // Initialize Retrofit with the configured OkHttpClient
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient.build())
                .build();

        // Create the API service
        mRestApiService = retrofit.create(RestApiEndPoint.class);
        Log.d("RestApiService", "Retrofit and RestApiEndPoint initialized successfully");
    }
}