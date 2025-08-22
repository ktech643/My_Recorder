package com.checkmate.android.util;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaCodec;
import android.os.Build;
import android.util.Log;
import android.util.Range;
import android.util.Rational;
import android.util.Size;
import android.util.SizeF;

import androidx.annotation.RequiresApi;

import com.wmspanel.libstream.Streamer;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.atan;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public final class CameraManager21 extends CameraManager {
    private static final String TAG = "CameraManager21";

    @Override
    List<CameraInfo> getCameraList(Context context) {

        List<CameraInfo> cameraList = new ArrayList<>();

        try {

            android.hardware.camera2.CameraManager cameraManager = (android.hardware.camera2.CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

            String[] cameraIdList = cameraManager.getCameraIdList();

            for (String cameraId : cameraIdList) {
                CameraInfo camera = getCameraInfo(context, cameraId);
                if (camera == null) {
                    continue;
                }
                cameraList.add(camera);
            }

        } catch (CameraAccessException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            cameraList = null;
        }
        return cameraList;
    }

    @Override
    CameraInfo getCameraInfo(Context context, String cameraId) {

        CameraInfo cameraInfo = new CameraInfo();
        cameraInfo.cameraId = cameraId;

        try {
            android.hardware.camera2.CameraManager cameraManager = (android.hardware.camera2.CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            Range<Integer>[] fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            cameraInfo.fpsRanges = new Streamer.FpsRange[fpsRanges.length];
            for (int i = 0; i < fpsRanges.length; i++) {
                cameraInfo.fpsRanges[i] = new Streamer.FpsRange(fpsRanges[i].getLower(), fpsRanges[i].getUpper());
            }

            Size[] recordSizes = map.getOutputSizes(MediaCodec.class);
            cameraInfo.recordSizes = new Streamer.Size[recordSizes.length];
            for (int j = 0; j < recordSizes.length; j++) {
                cameraInfo.recordSizes[j] = new Streamer.Size(recordSizes[j].getWidth(), recordSizes[j].getHeight());
            }

            if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                cameraInfo.lensFacing = CameraInfo.LENS_FACING_BACK;
            } else if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                cameraInfo.lensFacing = CameraInfo.LENS_FACING_FRONT;
            } else {
                cameraInfo.lensFacing = CameraInfo.LENS_FACING_EXTERNAL;
            }

            Range<Integer> aeRange = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
            Rational aeStep = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP);
            cameraInfo.minExposure = aeRange.getLower();
            cameraInfo.maxExposure = aeRange.getUpper();
            cameraInfo.exposureStep = aeStep.floatValue();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                for (int capability : characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)) {
                    if (capability == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA) {
                        // Prior to API level 29, all returned IDs are guaranteed to be returned by
                        // CameraManager#getCameraIdList, and can be opened directly by CameraManager#openCamera.
                        for (String physicalCameraId : characteristics.getPhysicalCameraIds()) {
                            CameraInfo physicalCamera = getCameraInfo(context, physicalCameraId);
                            if (physicalCamera == null) {
                                continue;
                            }
                            cameraInfo.physicalCameras.add(physicalCamera);
                        }
                        break;
                    }
                }
            }

            double angrad = 1.1f;
            SizeF sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
            float[] focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
            if (focalLengths != null && focalLengths.length > 0) {
                angrad = 2.0f * atan(sensorSize.getWidth() / (2.0f * focalLengths[0]));
            }
            cameraInfo.fov = (float) Math.toDegrees(angrad);

        } catch (NullPointerException | CameraAccessException e) {
            Log.e(TAG, "failed to get camera info, cameraId=" + cameraId);
            cameraInfo = null;
        }
        return cameraInfo;
    }
}
