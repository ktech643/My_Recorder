package com.checkmate.android.util.HttpServer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@AndroidEntryPoint
public class MyBackgroundService extends Service {
    private MyHttpServer httpServer;
    
    @Inject
    ServiceManager serviceManager;

    @Override
    public void onCreate() {
        super.onCreate();

        // Create HTTP server with ServiceManager
        int port = 8080; // Your server port
        httpServer = new MyHttpServer(port, getApplicationContext());
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