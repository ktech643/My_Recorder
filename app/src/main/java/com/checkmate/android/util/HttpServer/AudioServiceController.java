package com.checkmate.android.util.HttpServer;

import android.content.Context;
import android.content.Intent;

import com.checkmate.android.MyApp;
import com.checkmate.android.service.BgAudioService;
import com.checkmate.android.service.BgCameraService;

import javax.inject.Inject;

public class AudioServiceController implements BgService  {
    private final BgAudioService androidSvc;

    @Inject
    public AudioServiceController(BgAudioService androidSvc) {
        this.androidSvc = androidSvc;
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
