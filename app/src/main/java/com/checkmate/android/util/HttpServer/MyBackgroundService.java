package com.checkmate.android.util.HttpServer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import toothpick.Toothpick;

public class MyBackgroundService extends Service {
    private MyHttpServer httpServer;

    @Override
    public void onCreate() {
        super.onCreate();

        // Get ServiceManager instance from Toothpick
        ServiceManager serviceManager = Toothpick
                .openScope("APP_SCOPE")  // Use your application's scope
                .getInstance(ServiceManager.class);

        // Create HTTP server with ServiceManager
        int port = 8080; // Your server port
        httpServer = new MyHttpServer(port, getApplicationContext(), serviceManager);
        httpServer.startServer();
    }

    @Override
    public void onDestroy() {
        if (httpServer != null) {
            httpServer.stopServer();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}