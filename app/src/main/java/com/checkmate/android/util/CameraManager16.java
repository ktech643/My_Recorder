package com.checkmate.android.util;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;

import com.wmspanel.libstream.Streamer;

import java.util.ArrayList;
import java.util.List;

public final class CameraManager16 extends CameraManager {
    private static final String TAG = "CameraManager16";

    @Override
    List<CameraInfo> getCameraList(Context context) {

        List<CameraInfo> cameraInfoList = new ArrayList<CameraInfo>();
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            CameraInfo cameraInfo = getCameraInfo(i);
            if (cameraInfo == null) {
                continue;
            }
            cameraInfoList.add(cameraInfo);
        }
        return cameraInfoList;
    }

    @Override
    CameraInfo getCameraInfo(Context context, String cameraIdStr) {
        int cameraId = Integer.valueOf(cameraIdStr);
        return getCameraInfo(cameraId);
    }

    private CameraInfo getCameraInfo(int cameraId) {

        CameraInfo cameraInfo = null;
        Camera camera = null;

        try {

            camera = Camera.open(cameraId);
            Camera.Parameters param = camera.getParameters();

            cameraInfo = new CameraInfo();
            cameraInfo.cameraId = Integer.toString(cameraId);

            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, info);

            List<Camera.Size> previewSizes = param.getSupportedPreviewSizes();
            if (null != previewSizes) {
                cameraInfo.recordSizes = new Streamer.Size[previewSizes.size()];
                for (int j = 0; j < previewSizes.size(); j++) {
                    cameraInfo.recordSizes[j] = new Streamer.Size(previewSizes.get(j).width, previewSizes.get(j).height);
                }
            }

            List<int[]> fpsRanges = param.getSupportedPreviewFpsRange();
            cameraInfo.fpsRanges = new Streamer.FpsRange[fpsRanges.size()];
            for (int i = 0; i < fpsRanges.size(); i++) {
                cameraInfo.fpsRanges[i] = new Streamer.FpsRange(fpsRanges.get(i)[0], fpsRanges.get(i)[1]);
            }

            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraInfo.lensFacing = CameraInfo.LENS_FACING_BACK;
            } else {
                cameraInfo.lensFacing = CameraInfo.LENS_FACING_FRONT;
            }

            cameraInfo.minExposure = param.getMinExposureCompensation();
            cameraInfo.maxExposure = param.getMaxExposureCompensation();
            cameraInfo.exposureStep = param.getExposureCompensationStep();

            cameraInfo.fov = param.getHorizontalViewAngle();

        } catch (Exception e) {
            Log.e(TAG, "failed to get camera info, cameraId=" + cameraId);
            cameraInfo = null;

        } finally {
            if (null != camera) {
                camera.release();
            }
        }
        return cameraInfo;
    }

}
