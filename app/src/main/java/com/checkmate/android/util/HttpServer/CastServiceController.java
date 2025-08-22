package com.checkmate.android.util.HttpServer;

import android.content.Context;
import android.content.Intent;

import com.checkmate.android.MyApp;
import com.checkmate.android.service.BgCameraService;
import com.checkmate.android.service.BgCastService;

import javax.inject.Inject;

public class CastServiceController implements BgService {
    private final BgCastService androidSvc;

    @Inject
    public CastServiceController(BgCastService androidSvc) {
        this.androidSvc = androidSvc;
    }

    @Override public void startServiceDI()   {
        Context ctx = MyApp.getContext();
        Intent intent = new Intent(ctx, BgCameraService.class);
        ctx.startForegroundService(intent);
    }
    @Override public void stopServiceDI()    { androidSvc.stopSelf(); }

    @Override public void startStreamDI()    { /*androidSvc.startScreenCast();*/ }
    @Override public void stopStreamDI()     { androidSvc.stopScreenCast(); }

    @Override public void startRecordingDI() { androidSvc.startRecording(); }
    @Override public void stopRecordingDI()  { androidSvc.stopRecording(); }

    @Override public void switchAudioDI(boolean enable) {
        // e.g., send a broadcast the service listens to:
        Intent i = new Intent("audio_toggle");
        i.putExtra("enable", enable);
        androidSvc.sendBroadcast(i);
    }
}
