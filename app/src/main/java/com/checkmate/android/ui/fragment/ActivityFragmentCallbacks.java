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
    void stopFragService();
    void stopFragRecordingTime();
    void saveFragUSBCameraResolutions();
    void fragTakeSnapshot();
    void fragLockOrientation();
    void fragStartVolumeService();
    void fragStopVolumeService();
    void fragCameraRestart(boolean isRestart);
    void fragUpdateMenu(boolean isUpdate);
    void fragUpdateApp(String url);
    void fragStartStream();
    void fragStopLocationService();
    void fragStartLocationService();
}