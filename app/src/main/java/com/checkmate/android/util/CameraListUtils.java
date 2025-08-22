package com.checkmate.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.checkmate.android.R;
import com.wmspanel.libstream.CameraConfig;
import com.wmspanel.libstream.Streamer;
import com.wmspanel.libstream.StreamerGLBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CameraListUtils {
    private static final String TAG = "CameraListUtils";

    // Start your app implementation from calling addDefaultCameras unless you want to split modern
    // multi-lens cameras into separate instances (maybe Wide and Ultra-wide, depending on device).
    // There is no other info except camera id, so use sensor parameters to determine camera
    // type (for example, Larix uses horizontal field-of-view in setup screen).

    public static boolean addCameras(final Context context,
                                     final StreamerGLBuilder builder,
                                     final List<CameraInfo> cameraList,
                                     final CameraInfo activeCameraInfo,
                                     final Streamer.Size videoSize,
                                     final boolean useCamera2) {

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            // In API level 28, the physical cameras must also be exposed to the application
            // via CameraManager.getCameraIdList().
            // Starting from API level 29 some or all physical cameras may not be independently
            // exposed to the application, in which case the physical camera IDs will not be available
            // in CameraManager.getCameraIdList().
            final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            final String selectedCameras = sp.getString(context.getString(R.string.sub_cameras_list_key), null);
            if (selectedCameras == null
                    || !useCamera2
                    || activeCameraInfo.lensFacing == CameraInfo.LENS_FACING_EXTERNAL) {
                return CameraListUtils.addDefaultCameras(context,
                        builder, cameraList, activeCameraInfo, videoSize);
            } else {
                return CameraListUtils.addSelectedCameras(context,
                        builder, cameraList, activeCameraInfo, videoSize, selectedCameras);
            }
        } else {
            return CameraListUtils.addDefaultCameras(context,
                    builder, cameraList, activeCameraInfo, videoSize);
        }
    }

    public static boolean addDefaultCameras(final Context context,
                                            final StreamerGLBuilder builder,
                                            final List<CameraInfo> cameraList,
                                            final CameraInfo activeCameraInfo,
                                            final Streamer.Size videoSize) {
        // start adding cameras from default camera, then add second camera
        // larix uses same resolution for camera preview and stream to simplify setup

        // add first camera to flip list, make sure you called setVideoConfig before
        final CameraConfig cameraConfig = new CameraConfig();
        cameraConfig.cameraId = activeCameraInfo.cameraId;
        cameraConfig.videoSize = videoSize;
        cameraConfig.fpsRange = SettingsUtils.findFpsRange(context, activeCameraInfo.fpsRanges);

        builder.addCamera(cameraConfig);
        Log.d(TAG, "Camera #" + cameraConfig.cameraId + " resolution: " + cameraConfig.videoSize);

        // set start position in flip list to camera id
        builder.setCameraId(activeCameraInfo.cameraId);

        final boolean canFlip = cameraList.size() > 1;
        if (canFlip) {
            // loop through the available cameras
            for (CameraInfo cameraInfo : cameraList) {
                if (cameraInfo.cameraId.equals(activeCameraInfo.cameraId)) {
                    continue;
                }
                // add next camera to flip list
                final CameraConfig flipCameraConfig = new CameraConfig();
                flipCameraConfig.cameraId = cameraInfo.cameraId;
                flipCameraConfig.videoSize = SettingsUtils.findFlipSize(cameraInfo, videoSize);
                flipCameraConfig.fpsRange = SettingsUtils.findFpsRange(context, cameraInfo.fpsRanges);

                builder.addCamera(flipCameraConfig);
                Log.d(TAG, "Camera #" + flipCameraConfig.cameraId + " resolution: " + flipCameraConfig.videoSize);
            }
        }
        return canFlip;
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    public static boolean addSelectedCameras(final Context context,
                                             final StreamerGLBuilder builder,
                                             final List<CameraInfo> cameraList,
                                             final CameraInfo activeCameraInfo,
                                             final Streamer.Size videoSize,
                                             final String selectedCameras) {
        try {
            Log.d(TAG, selectedCameras);
            final JSONArray cams = new JSONArray(selectedCameras);
            final List<CameraInfo.CamId> flipCamIds = new ArrayList<>();
            final CameraInfo.CamId activeCamId = new CameraInfo.CamId(activeCameraInfo.cameraId, "");
            boolean activeCamIdFound = false;
            for (int idx = 0; idx < cams.length(); idx++) {
                final JSONObject cam = cams.getJSONObject(idx);
                final String id = cam.getString(CameraInfo.ID);
                final String physicalId = cam.optString(CameraInfo.PHYSICAL_ID);
                final CameraInfo.CamId camId = new CameraInfo.CamId(id, physicalId);
                if (activeCamId.equals(camId)) {
                    activeCamIdFound = true;
                } else {
                    flipCamIds.add(camId);
                }
            }
            if (!activeCamIdFound) {
                for (CameraInfo.CamId camId : flipCamIds) {
                    if (camId.id.equals(activeCameraInfo.cameraId)) {
                        activeCamId.physicalId = camId.physicalId;
                        flipCamIds.remove(camId);
                        break;
                    }
                }
            }

            builder.addCamera(toCameraConfig(context, activeCamId, activeCameraInfo, videoSize));
            builder.setCameraId(activeCamId.id, activeCamId.physicalId);

            final Map<String, CameraInfo> map = CameraInfo.toMap(cameraList);

            // First add all start camera sub-devices
            for (CameraInfo.CamId camId : flipCamIds) {
                final CameraInfo cameraInfo = map.get(camId.id.concat(camId.physicalId));
                if (cameraInfo != null && cameraInfo.cameraId.equals(activeCamId.id)) {
                    builder.addCamera(toCameraConfig(context, camId, cameraInfo, videoSize));
                }
            }
            // Then add all other cameras, list is sorted already
            for (CameraInfo.CamId camId : flipCamIds) {
                final CameraInfo cameraInfo = map.get(camId.id.concat(camId.physicalId));
                if (cameraInfo != null && !cameraInfo.cameraId.equals(activeCamId.id)) {
                    builder.addCamera(toCameraConfig(context, camId, cameraInfo, videoSize));
                }
            }

            return !flipCamIds.isEmpty();

        } catch (JSONException e) {
            final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().remove(context.getString(R.string.sub_cameras_list_key)).apply();
            return addDefaultCameras(context, builder, cameraList, activeCameraInfo, videoSize);
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private static CameraConfig toCameraConfig(final Context context,
                                               CameraInfo.CamId camId,
                                               CameraInfo cameraInfo,
                                               Streamer.Size videoSize) {
        final CameraConfig cameraConfig = new CameraConfig();
        cameraConfig.cameraId = camId.id;
        cameraConfig.physicalCameraId = camId.physicalId;
        cameraConfig.videoSize = SettingsUtils.findFlipSize(cameraInfo, videoSize);
        cameraConfig.fpsRange = SettingsUtils.findFpsRange(context, cameraInfo.fpsRanges);
        Log.d(TAG, cameraConfig.toString());
        return cameraConfig;
    }

}
