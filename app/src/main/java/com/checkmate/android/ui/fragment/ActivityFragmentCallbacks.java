package com.checkmate.android.ui.fragment;

import com.checkmate.android.model.Camera;

public interface ActivityFragmentCallbacks {
    void setFragStreamingCamera(Camera wifi_cam);
    void fragSetWifiCamera(Camera wifi_cam);
    void setFragRearCamera(boolean isCam);
    void isDialog(boolean isDialog);
    void showDialog();
    void dismissDialog();
    void initFragService();
    void initFragCastService();
    void stopFragBgCast();
    void stopFragUSBService();
    void stopFragBgCamera();
    void stopFragRecordingTime();
    void stopFragWifiService();
    void saveFragUSBCameraResolutions();
    void fragWifiSnapshot();
    void fragTakeSnapshot();
    void fragInitBGUSBService();
    void fragStopStreaming();
    void fragInitBGWifiService();
    void fragLockOrientation();
    void fragStartVolumeService();
    void fragStopVolumeService();
    void fragCameraRestart(boolean isRestart);
    void fragUpdateMenu(boolean isUpdate);
    void fragUpdateApp(String url);
    void fragStartStream();
    void fragStopLocationService();
    void fragStartLocationService();
    void initFragAudioService();
    void stopFragAudio();

    void startCastRecording();
}
