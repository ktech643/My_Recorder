package com.checkmate.android.util;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;

import java.util.Arrays;

/**
 * Utility class for camera-related operations and validation
 */
public class CameraUtils {
    private static final String TAG = "CameraUtils";
    
    public static boolean isCameraAvailable(Context context, String cameraId) {
        try {
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String[] cameras = cameraManager.getCameraIdList();
            
            for (String id : cameras) {
                if (id.equals(cameraId)) {
                    // Double check by getting characteristics
                    try {
                        cameraManager.getCameraCharacteristics(id);
                        return true;
                    } catch (Exception e) {
                        Log.e(TAG, "Camera " + id + " exists but characteristics unavailable: " + e.getMessage());
                        return false;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking camera availability: " + e.getMessage(), e);
            return false;
        }
    }
    
    public static void logAvailableCameras(Context context) {
        try {
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String[] cameras = cameraManager.getCameraIdList();
            Log.d(TAG, "Available cameras: " + Arrays.toString(cameras));
            
            for (String cameraId : cameras) {
                try {
                    CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    String facingStr = facing == CameraCharacteristics.LENS_FACING_FRONT ? "FRONT" : "BACK";
                    Log.d(TAG, "Camera " + cameraId + " facing: " + facingStr);
                } catch (Exception e) {
                    Log.e(TAG, "Error getting characteristics for camera " + cameraId + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error logging cameras: " + e.getMessage(), e);
        }
    }
} 