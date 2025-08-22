package com.checkmate.android.util;

import android.content.Context;
import android.os.Build;

import java.util.List;

abstract public class CameraManager {

    abstract List<CameraInfo> getCameraList(Context context);

    abstract CameraInfo getCameraInfo(Context context, String cameraId);

    public static List<CameraInfo> getCameraList(Context context, boolean camera2) {
        return getCameraManager(camera2).getCameraList(context);
    }

    private static CameraManager getCameraManager(boolean camera2) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return new CameraManager16();
        } else {
            return camera2 ? new CameraManager21() : new CameraManager16();
        }
    }

}
