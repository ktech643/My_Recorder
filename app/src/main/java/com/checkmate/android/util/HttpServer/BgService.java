package com.checkmate.android.util.HttpServer;
// com.checkmate.android.service.BgService.java

public interface BgService {
    void startServiceDI();
    void stopServiceDI();

    void startStreamDI();
    void stopStreamDI();

    void startRecordingDI();
    void stopRecordingDI();

    void switchAudioDI(boolean enable);
}
