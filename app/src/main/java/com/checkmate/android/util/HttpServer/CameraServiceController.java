// com.checkmate.android.service.impl.CameraServiceController.java
package com.checkmate.android.util.HttpServer;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import javax.inject.Inject;

import com.checkmate.android.MyApp;
import com.checkmate.android.service.BgCameraService;
import com.checkmate.android.util.HttpServer.BgService;

public class CameraServiceController implements BgService {
    private final BgCameraService androidSvc;

    @Inject
    public CameraServiceController(BgCameraService androidSvc) {
        this.androidSvc = androidSvc;
        Log.d("CameraController", "Created with service: " + androidSvc);
    }

    @Override public void startServiceDI()   {
        Context ctx = MyApp.getContext();
        Intent intent = new Intent(ctx, BgCameraService.class);
        ctx.startForegroundService(intent);
    }
    @Override public void stopServiceDI()    { androidSvc.stopSelf(); }

    @Override public void startStreamDI()    { androidSvc.startStreaming(); }
    @Override public void stopStreamDI()     { androidSvc.stopStreaming(); }

    @Override public void startRecordingDI() { androidSvc.startRecording(); }
    @Override public void stopRecordingDI()  { androidSvc.stopRecording(); }

    @Override public void switchAudioDI(boolean enable) {
        // e.g., send a broadcast the service listens to:
        Intent i = new Intent("audio_toggle");
        i.putExtra("enable", enable);
        androidSvc.sendBroadcast(i);
    }
}
