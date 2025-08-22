package com.checkmate.android.util;

import android.os.Handler;
import android.os.Looper;
import okhttp3.OkHttpClient;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.Request;
import okhttp3.Response;

public class NetworkUtil {

    public static void checkIfIpIsReachable(String ipAddress, int port, String channel ,ReachabilityListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isReachable = false;
                String url = "http://" + ipAddress + ":" + port + "/" + channel;
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(5, TimeUnit.SECONDS)
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        // Disable connection reuse
                        .header("Connection", "close")
                        .build();

                try {
                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        isReachable = true;
                    }
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                final boolean finalIsReachable = isReachable;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onResult(finalIsReachable);
                    }
                });
            }
        }).start();
    }

    public interface ReachabilityListener {
        void onResult(boolean isReachable);
    }
}
