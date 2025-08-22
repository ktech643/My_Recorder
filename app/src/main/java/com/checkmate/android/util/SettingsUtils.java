package com.checkmate.android.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.checkmate.android.AppConstant;
import com.checkmate.android.AppPreference;
import com.checkmate.android.R;
import com.wmspanel.libstream.AudioConfig;
import com.wmspanel.libstream.ConnectionConfig;
import com.wmspanel.libstream.FocusMode;
import com.wmspanel.libstream.RistConfig;
import com.wmspanel.libstream.SrtConfig;
import com.wmspanel.libstream.Streamer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;

public final class SettingsUtils {
    private static final String TAG = "SettingsUtils";

    public static final int AF_MODE_CONTINUOUS_VIDEO = 0;
    public static final int AF_MODE_INFINITY = 1;
    public static final int AWB_MODE_AUTO = 0;
    public static final int AWB_MODE_CLOUDY_DAYLIGHT = 1;
    public static final int AWB_MODE_DAYLIGHT = 2;
    public static final int AWB_MODE_FLUORESCENT = 3;
    public static final int AWB_MODE_INCANDESCENT = 4;
    public static final int AWB_MODE_OFF = 5;
    public static final int AWB_MODE_SHADE = 6;
    public static final int AWB_MODE_TWILIGHT = 7;
    public static final int AWB_MODE_WARM_FLUORESCENT = 8;

    public static final int ANTIBANDING_MODE_OFF = 0;
    public static final int ANTIBANDING_MODE_50HZ = 1;
    public static final int ANTIBANDING_MODE_60HZ = 2;
    public static final int ANTIBANDING_MODE_AUTO = 3;

    public static final int VIDEO_STABILIZATION_MODE_NONE = -1;
    public static final int VIDEO_STABILIZATION_MODE_OFF = 0;
    public static final int VIDEO_STABILIZATION_MODE_ON = 1;

    public static final int OPTICAL_STABILIZATION_MODE_NONE = -1;
    public static final int OPTICAL_STABILIZATION_MODE_OFF = 0;
    public static final int OPTICAL_STABILIZATION_MODE_ON = 1;

    public static final int NOISE_REDUCTION_MODE_NONE = -1;
    public static final int NOISE_REDUCTION_MODE_OFF = 0;
    public static final int NOISE_REDUCTION_MODE_FAST = 1;
    public static final int NOISE_REDUCTION_MODE_HIGHT_QUALITY = 2;
    public static final int NOISE_REDUCTION_MODE_MINIMAL = 3;

    public static final int ADAPTIVE_BITRATE_OFF = 0;
    public static final int ADAPTIVE_BITRATE_MODE1 = 1;
    public static final int ADAPTIVE_BITRATE_MODE2 = 2;
    public static final int ADAPTIVE_BITRATE_HYBRID = 3;

    public static final int ACTION_DO_NOTHING = 0;
    public static final int ACTION_START_STOP = 1;

    public static final int API_CAMERA = 1;
    public static final int API_CAMERA2 = 2;

    private static final Map<Integer, String> AWB_MAP_16 = createAwbMap16();

    private static Map<Integer, String> createAwbMap16() {
        Map<Integer, String> result = new HashMap<Integer, String>();
        result.put(AWB_MODE_AUTO, Camera.Parameters.WHITE_BALANCE_AUTO);
        result.put(AWB_MODE_CLOUDY_DAYLIGHT, Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT);
        result.put(AWB_MODE_DAYLIGHT, Camera.Parameters.WHITE_BALANCE_DAYLIGHT);
        result.put(AWB_MODE_FLUORESCENT, Camera.Parameters.WHITE_BALANCE_FLUORESCENT);
        result.put(AWB_MODE_INCANDESCENT, Camera.Parameters.WHITE_BALANCE_INCANDESCENT);
        result.put(AWB_MODE_OFF, Camera.Parameters.WHITE_BALANCE_AUTO);
        result.put(AWB_MODE_SHADE, Camera.Parameters.WHITE_BALANCE_SHADE);
        result.put(AWB_MODE_TWILIGHT, Camera.Parameters.WHITE_BALANCE_TWILIGHT);
        result.put(AWB_MODE_WARM_FLUORESCENT, Camera.Parameters.WHITE_BALANCE_WARM_FLUORESCENT);
        return Collections.unmodifiableMap(result);
    }

    private static final Map<Integer, Integer> AWB_MAP_21 = createAwbMap21();

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Map<Integer, Integer> createAwbMap21() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return null;
        }
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        result.put(AWB_MODE_AUTO, CaptureRequest.CONTROL_AWB_MODE_AUTO);
        result.put(AWB_MODE_CLOUDY_DAYLIGHT, CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT);
        result.put(AWB_MODE_DAYLIGHT, CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT);
        result.put(AWB_MODE_FLUORESCENT, CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT);
        result.put(AWB_MODE_INCANDESCENT, CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT);
        result.put(AWB_MODE_OFF, CaptureRequest.CONTROL_AWB_MODE_OFF);
        result.put(AWB_MODE_SHADE, CaptureRequest.CONTROL_AWB_MODE_SHADE);
        result.put(AWB_MODE_TWILIGHT, CaptureRequest.CONTROL_AWB_MODE_TWILIGHT);
        result.put(AWB_MODE_WARM_FLUORESCENT, CaptureRequest.CONTROL_AWB_MODE_WARM_FLUORESCENT);
        return Collections.unmodifiableMap(result);
    }

    private static final Map<Integer, String> ANTIBANDING_MAP_16 = createAntibandingMap16();

    private static Map<Integer, String> createAntibandingMap16() {
        Map<Integer, String> result = new HashMap<Integer, String>();
        result.put(ANTIBANDING_MODE_OFF, Camera.Parameters.ANTIBANDING_OFF);
        result.put(ANTIBANDING_MODE_50HZ, Camera.Parameters.ANTIBANDING_50HZ);
        result.put(ANTIBANDING_MODE_60HZ, Camera.Parameters.ANTIBANDING_60HZ);
        result.put(ANTIBANDING_MODE_AUTO, Camera.Parameters.ANTIBANDING_AUTO);
        return Collections.unmodifiableMap(result);
    }

    private static final Map<Integer, Integer> VIDEO_STABILIZATION_MODE_MAP_21 = createVideoStabilizationMap21();


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Map<Integer, Integer> createVideoStabilizationMap21() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return null;
        }
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        result.put(VIDEO_STABILIZATION_MODE_NONE, FocusMode.DONT_SET);
        result.put(VIDEO_STABILIZATION_MODE_OFF, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF);
        result.put(VIDEO_STABILIZATION_MODE_ON, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);
        return Collections.unmodifiableMap(result);
    }

    private static final Map<Integer, Integer> OPTICAL_STABILIZATION_MODE_MAP_21 = createOpticalStabilizationMap21();

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Map<Integer, Integer> createOpticalStabilizationMap21() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return null;
        }
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        result.put(OPTICAL_STABILIZATION_MODE_NONE, FocusMode.DONT_SET);
        result.put(OPTICAL_STABILIZATION_MODE_OFF, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF);
        result.put(OPTICAL_STABILIZATION_MODE_ON, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);
        return Collections.unmodifiableMap(result);
    }

    private static final Map<Integer, Integer> ANTIBANDING_MAP_21 = createAntibandingMap21();

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Map<Integer, Integer> createAntibandingMap21() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return null;
        }
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        result.put(ANTIBANDING_MODE_OFF, CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_OFF);
        result.put(ANTIBANDING_MODE_50HZ, CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_50HZ);
        result.put(ANTIBANDING_MODE_60HZ, CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_60HZ);
        result.put(ANTIBANDING_MODE_AUTO, CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO);
        return Collections.unmodifiableMap(result);
    }

    private static final Map<Integer, Integer> NOISE_REDUCTION_MAP_21 = createNoiseReductionMap21();

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Map<Integer, Integer> createNoiseReductionMap21() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return null;
        }
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        result.put(NOISE_REDUCTION_MODE_NONE, FocusMode.DONT_SET);
        result.put(NOISE_REDUCTION_MODE_OFF, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
        result.put(NOISE_REDUCTION_MODE_FAST, CaptureRequest.NOISE_REDUCTION_MODE_FAST);
        result.put(NOISE_REDUCTION_MODE_HIGHT_QUALITY, CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY);
        result.put(NOISE_REDUCTION_MODE_MINIMAL, CaptureRequest.NOISE_REDUCTION_MODE_MINIMAL);
        return Collections.unmodifiableMap(result);
    }

    private static final String[] GROVE_MODE_MAP = {"va", "v", "a"};
    private static final String[] GROVE_TARGET_MAP = {"d", "lime", "peri", "rtmp", "ala"};
    private static final String[] GROVE_RISTPROFILE_MAP = {"simple", "main", "advanced"};

    public static Bitmap.CompressFormat snapshotFormat(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(R.string.pref_snapshot_format_key);
        String defValue = context.getString(R.string.snapshot_jpeg);
        String value = sp.getString(key, defValue);
        //Log.d(TAG, value);
        if (value.equals(defValue))
            return Bitmap.CompressFormat.JPEG;
        else if (value.equals(context.getString(R.string.snapshot_png)))
            return Bitmap.CompressFormat.PNG;
        else if (value.equals(context.getString(R.string.snapshot_webp)))
            return Bitmap.CompressFormat.WEBP;
        return Bitmap.CompressFormat.JPEG;
    }

    public static int snapshotQuality(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(R.string.pref_snapshot_quality_key);
        String defValue = context.getString(R.string.snapshot_quality_90);
        String value = sp.getString(key, defValue);
        //Log.d(TAG, value);
        return Integer.parseInt(value);
    }

    public static CameraInfo getActiveCameraInfo(Context context, List<CameraInfo> cameraList) {
        CameraInfo cameraInfo = null;

        if (cameraList == null || cameraList.size() == 0) {
            Log.e(TAG, "no camera found");
            return null;
        }

        if (DeepLink.getInstance().hasImportedActiveCamera()) {
            CameraInfo info = DeepLink.getInstance().getActiveCameraInfo(cameraList, context);
            if (info != null) {
                return info;
            }
        }

        String cameraId = AppPreference.getStr(AppPreference.KEY.SELECTED_POSITION, AppConstant.REAR_CAMERA);
        for (CameraInfo cursor : cameraList) {
            if (cursor.cameraId.equals(cameraId)) {
                cameraInfo = cursor;
            }
        }
        if (cameraInfo == null) {
            cameraInfo = cameraList.get(0);
        }
        return cameraInfo;
    }

    static String onVideoLow() {
        AppPreference.setInt(AppPreference.KEY.VIDEO_FRAME, 15);
        AppPreference.setInt(AppPreference.KEY.VIDEO_BITRATE, 768);
        AppPreference.setInt(AppPreference.KEY.VIDEO_KEYFRAME, 1);
        Log.d("DEBUG", "Saved Position: " + 3);
        return "640x360";
    }

    static String onVideoSuperLow() {
        AppPreference.setInt(AppPreference.KEY.VIDEO_FRAME, 15);
        AppPreference.setInt(AppPreference.KEY.VIDEO_BITRATE, 400);
        AppPreference.setInt(AppPreference.KEY.VIDEO_KEYFRAME, 1);
        return "320x180";
    }
    //List<String> recordSizes
    static String onVideoUSB(Streamer.Size[] sortedResolutions) {
        List<String> record_sizes = Arrays.stream(sortedResolutions)
                .map(Streamer.Size::toString) // or use .map(size -> size.toString()) if preferred
                .collect(Collectors.toList());
        AppPreference.setInt(AppPreference.KEY.VIDEO_FRAME, 30);
        AppPreference.setInt(AppPreference.KEY.VIDEO_BITRATE, 3048);
        AppPreference.setInt(AppPreference.KEY.VIDEO_KEYFRAME, 1);
        int index = record_sizes.indexOf("1920x1080");
        if (index > 0) {
            return onVideoHigh();
        }
        index = record_sizes.indexOf("1280x720");
        if (index > 0) {
            return onVideoMedium();
        }
        index = record_sizes.indexOf("640x480");
        if (index > 0) {
            return onVideoLow();
        }
        index = record_sizes.indexOf("640x360");
        if (index > 0) {
            return onVideoLow();
        }
        index = record_sizes.indexOf("320x180");
        if (index > 0) {
            return onVideoSuperLow();
        }
        AppPreference.setInt(AppPreference.KEY.VIDEO_FRAME, 30);
        AppPreference.setInt(AppPreference.KEY.VIDEO_BITRATE, 3048);
        AppPreference.setInt(AppPreference.KEY.VIDEO_KEYFRAME, 1);
        return  "1920x1080";
    }

    static String onVideoMedium() {
        AppPreference.setInt(AppPreference.KEY.VIDEO_FRAME, 30);
        AppPreference.setInt(AppPreference.KEY.VIDEO_BITRATE, 1496);
        AppPreference.setInt(AppPreference.KEY.VIDEO_KEYFRAME, 1);
        return "1280x720";
    }

    static String onVideoHigh() {
        AppPreference.setInt(AppPreference.KEY.VIDEO_FRAME, 30);
        AppPreference.setInt(AppPreference.KEY.VIDEO_BITRATE, 3048);
        AppPreference.setInt(AppPreference.KEY.VIDEO_KEYFRAME, 1);
        return "1920x1080";
    }

    public static Streamer.Size getVideoSize(CameraInfo cameraInfo) {
        boolean isNative = AppPreference.getBool(AppPreference.KEY.IS_NATIVE_RESOLUTION, false);
        int video_resolution = AppPreference.getInt(AppPreference.KEY.VIDEO_RESOLUTION, 0);
        int video_Quality = AppPreference.getInt(AppPreference.KEY.VIDEO_QUALITY, 0);
        if (video_Quality == 0) {
            String size = onVideoHigh();
            String[] values = size.split("x");
            int width = Integer.parseInt(values[0]);
            int height = Integer.parseInt(values[1]);
            return new Streamer.Size(width, height);
        } else if (video_Quality == 1) {
            String size = onVideoMedium();
            String[] values = size.split("x");
            int width = Integer.parseInt(values[0]);
            int height = Integer.parseInt(values[1]);
            return new Streamer.Size(width, height);
        }else if (video_Quality == 2) {
            String size = onVideoLow();
            String[] values = size.split("x");
            int width = Integer.parseInt(values[0]);
            int height = Integer.parseInt(values[1]);
            return new Streamer.Size(width, height);
        }else if (video_Quality == 3) {
            String size = onVideoSuperLow();
            String[] values = size.split("x");
            int width = Integer.parseInt(values[0]);
            int height = Integer.parseInt(values[1]);
            return new Streamer.Size(width, height);
        }else if (video_Quality == 4) {
            if (cameraInfo == null || cameraInfo.recordSizes == null) {
                String size = onVideoHigh();
                String[] values = size.split("x");
                int width = Integer.parseInt(values[0]);
                int height = Integer.parseInt(values[1]);
                return new Streamer.Size(width, height);
            }else {
                String size = onVideoUSB(cameraInfo.recordSizes);
                String[] values = size.split("x");
                int width = Integer.parseInt(values[0]);
                int height = Integer.parseInt(values[1]);
                return new Streamer.Size(width, height);
            }
        }else {
            if (cameraInfo == null || cameraInfo.recordSizes == null) {
                Log.e("SettingsUtils", "CameraInfo or recordSizes is null in getStreamVideoSize()");
                AppPreference.setInt(AppPreference.KEY.VIDEO_FRAME, 30);
                AppPreference.setInt(AppPreference.KEY.VIDEO_BITRATE, 3048);
                AppPreference.setInt(AppPreference.KEY.VIDEO_KEYFRAME, 1);
                return new Streamer.Size(1920, 1080); // Return a default size instead of crashing
            }
            Streamer.Size videoSize = null;
            String[] camera_sizes = new String[cameraInfo.recordSizes.length];
            for (int i = 0; i < cameraInfo.recordSizes.length; i++) {
                camera_sizes[i] = cameraInfo.recordSizes[i].toString();
            }
            camera_sizes = filterResolutions(camera_sizes);

            if (video_resolution == -1 || video_resolution >= camera_sizes.length) {
                video_resolution = 0;
            }
            String size = "";
            if (camera_sizes.length <= video_resolution) {
                AppPreference.setInt(AppPreference.KEY.VIDEO_FRAME, 30);
                AppPreference.setInt(AppPreference.KEY.VIDEO_BITRATE, 3048);
                AppPreference.setInt(AppPreference.KEY.VIDEO_KEYFRAME, 1);
                return new Streamer.Size(1920, 1080);
            }
            try {
                size = camera_sizes[video_resolution];
                String[] values = size.split("x");
                int width = Integer.parseInt(values[0]);
                int height = Integer.parseInt(values[1]);
                videoSize = new Streamer.Size(width, height);
                videoSize = verifyResolution("video/avc", videoSize);
                return videoSize;
            } catch (Exception e) {
                AppPreference.setInt(AppPreference.KEY.VIDEO_FRAME, 30);
                AppPreference.setInt(AppPreference.KEY.VIDEO_BITRATE, 3048);
                AppPreference.setInt(AppPreference.KEY.VIDEO_KEYFRAME, 1);
                return new Streamer.Size(1920, 1080);
            }
        }

    }

    static String onStreamLow() {
        AppPreference.setInt(AppPreference.KEY.STREAMING_FRAME, 15);
        AppPreference.setInt(AppPreference.KEY.STREAMING_BITRATE, 400);
        AppPreference.setInt(AppPreference.KEY.STREAMING_KEYFRAME, 1);
        return "640x360";
    }

    static String onStreamSuperLow() {
        AppPreference.setInt(AppPreference.KEY.STREAMING_FRAME, 15);
        AppPreference.setInt(AppPreference.KEY.STREAMING_BITRATE, 200);
        AppPreference.setInt(AppPreference.KEY.STREAMING_KEYFRAME, 1);
        return "320x180";
    }

    static String onStreamMedium() {
        AppPreference.setInt(AppPreference.KEY.STREAMING_FRAME, 30);
        AppPreference.setInt(AppPreference.KEY.STREAMING_BITRATE, 1024);
        AppPreference.setInt(AppPreference.KEY.STREAMING_KEYFRAME, 1);
        return "960x540";
    }

    static String onStreamHigh() {
        AppPreference.setInt(AppPreference.KEY.STREAMING_FRAME, 30);
        AppPreference.setInt(AppPreference.KEY.STREAMING_BITRATE, 1496);
        AppPreference.setInt(AppPreference.KEY.STREAMING_KEYFRAME, 1);
        return "1280x720";
    }

    static String onStreamUSBHigh(Streamer.Size[] sortedResolutions) {
        List<String> record_sizes = Arrays.stream(sortedResolutions)
                .map(Streamer.Size::toString) // or use .map(size -> size.toString()) if preferred
                .collect(Collectors.toList());
        int index = record_sizes.indexOf("1280x720");
        if (index > 0) {
            return onStreamHigh();
        }
        index = record_sizes.indexOf("960x540");
        if (index > 0) {
            return onStreamMedium();
        }
        index = record_sizes.indexOf("640x360");
        if (index > 0) {
            return onStreamLow();
        }
        index = record_sizes.indexOf("320x180");
        if (index > 0) {
            return onStreamSuperLow();
        }
        AppPreference.setInt(AppPreference.KEY.STREAMING_FRAME, 30);
        AppPreference.setInt(AppPreference.KEY.STREAMING_BITRATE, 1496);
        AppPreference.setInt(AppPreference.KEY.STREAMING_KEYFRAME, 1);
        return "1280x720";
    }

    public static Streamer.Size getStreamVideoSize(CameraInfo cameraInfo) {
        boolean isNative = AppPreference.getBool(AppPreference.KEY.IS_NATIVE_RESOLUTION, false);
        int video_resolution = AppPreference.getInt(AppPreference.KEY.STREAMING_RESOLUTION, 0);
        int video_Quality = AppPreference.getInt(AppPreference.KEY.STREAMING_QUALITY, 0);
        if (video_Quality == 0) {
            String size = onStreamHigh();
            String[] values = size.split("x");
            int width = Integer.parseInt(values[0]);
            int height = Integer.parseInt(values[1]);
            return new Streamer.Size(width, height);
        } else if (video_Quality == 1) {
            String size = onStreamMedium();
            String[] values = size.split("x");
            int width = Integer.parseInt(values[0]);
            int height = Integer.parseInt(values[1]);
            return new Streamer.Size(width, height);
        }else if (video_Quality == 2) {
            String size = onStreamLow();
            String[] values = size.split("x");
            int width = Integer.parseInt(values[0]);
            int height = Integer.parseInt(values[1]);
            return new Streamer.Size(width, height);
        }else if (video_Quality == 3) {
            String size = onStreamSuperLow();
            String[] values = size.split("x");
            int width = Integer.parseInt(values[0]);
            int height = Integer.parseInt(values[1]);
            return new Streamer.Size(width, height);
        }else if (video_Quality == 4) {
            if (cameraInfo == null || cameraInfo.recordSizes == null) {
                String size = onStreamHigh();
                String[] values = size.split("x");
                int width = Integer.parseInt(values[0]);
                int height = Integer.parseInt(values[1]);
                return new Streamer.Size(width, height);
            }else {
                String size = onStreamUSBHigh(cameraInfo.recordSizes);
                String[] values = size.split("x");
                int width = Integer.parseInt(values[0]);
                int height = Integer.parseInt(values[1]);
                return new Streamer.Size(width, height);
            }
        }else {
            if (cameraInfo == null || cameraInfo.recordSizes == null) {
                Log.e("SettingsUtils", "CameraInfo or recordSizes is null in getStreamVideoSize()");
                AppPreference.setInt(AppPreference.KEY.STREAMING_FRAME, 30);
                AppPreference.setInt(AppPreference.KEY.STREAMING_BITRATE, 1496);
                AppPreference.setInt(AppPreference.KEY.STREAMING_KEYFRAME, 1);
                return new Streamer.Size(1280, 720); // Return a default size instead of crashing
            }
            Streamer.Size videoSize = null;
            String[] camera_sizes = new String[cameraInfo.recordSizes.length];
            for (int i = 0; i < cameraInfo.recordSizes.length; i++) {
                camera_sizes[i] = cameraInfo.recordSizes[i].toString();
            }
            camera_sizes = filterResolutions(camera_sizes);
            camera_sizes = getStreamingResolutions(camera_sizes);
            if (video_resolution == -1 || video_resolution >= camera_sizes.length) {
                video_resolution = 0;
            }
            String size = camera_sizes[video_resolution];
            String[] values = size.split("x");
            int width = Integer.parseInt(values[0]);
            int height = Integer.parseInt(values[1]);
            videoSize = new Streamer.Size(width, height);
//        "video/avc"
            videoSize = verifyResolution("video/avc", videoSize);
            return videoSize;
        }

    }
    private static final int[] STREAMING_WIDTHS = {1280, 960 , 720, 640, 320, 160};

    public static String[] filterResolutions(String[] record_sizes) {
        Set<String> filteredSet = new HashSet<>();
        for (String size : record_sizes) {
            String[] parts = size.split("x");
            if (parts.length != 2) continue; // Skip invalid formats
            int width = Integer.parseInt(parts[0]);
            int height = Integer.parseInt(parts[1]);
            // Calculate the aspect ratio
            String aspectRatio = getAspectRatio(width, height);
            // Keep only 16:9 or 4:3 resolutions
            if (aspectRatio.equals("16:9") || aspectRatio.equals("4:3")) {
                filteredSet.add(size);
            }
        }
        // Convert HashSet to List for sorting
        List<String> sortedResolutions = new ArrayList<>(filteredSet);
        // Sort by width (descending) then by height (descending)
        sortedResolutions.sort((a, b) -> {
            String[] aParts = a.split("x");
            String[] bParts = b.split("x");
            int aWidth = Integer.parseInt(aParts[0]);
            int aHeight = Integer.parseInt(aParts[1]);
            int bWidth = Integer.parseInt(bParts[0]);
            int bHeight = Integer.parseInt(bParts[1]);
            // Sort by width first, then by height if widths are equal
            if (bWidth != aWidth) {
                return Integer.compare(bWidth, aWidth);
            } else {
                return Integer.compare(bHeight, aHeight);
            }
        });

        return sortedResolutions.toArray(new String[0]);
    }
    public static String getAspectRatio(int width, int height) {
        int gcd = gcd(width, height);
        return (width / gcd) + ":" + (height / gcd);
    }
    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    // Function to scale resolution while maintaining aspect ratio
    public static String scaleResolution(int originalWidth, int originalHeight, int targetWidth) {
        double aspectRatio = (double) originalWidth / originalHeight;
        int targetHeight = (int) Math.round(targetWidth / aspectRatio);
        return targetWidth + "x" + targetHeight;
    }
    // Function to get the best streaming resolution based on recording resolution
    public static String[] getStreamingResolutions(String[] record_sizes) {
        Set<String> uniqueStreamingSet = new HashSet<>();
        for (String res : record_sizes) {
            String[] parts = res.split("x");
            int recordingWidth = Integer.parseInt(parts[0]);
            int recordingHeight = Integer.parseInt(parts[1]);
            for (int width : STREAMING_WIDTHS) {
                uniqueStreamingSet.add(scaleResolution(recordingWidth, recordingHeight, width));
            }
        }
        // Convert HashSet to List for sorting
        List<String> sortedResolutions = new ArrayList<>(uniqueStreamingSet);
        // Sort by width (descending) then by height (descending)
        sortedResolutions.sort((a, b) -> {
            String[] aParts = a.split("x");
            String[] bParts = b.split("x");
            int aWidth = Integer.parseInt(aParts[0]);
            int aHeight = Integer.parseInt(aParts[1]);
            int bWidth = Integer.parseInt(bParts[0]);
            int bHeight = Integer.parseInt(bParts[1]);
            // Sort by width first, then by height if widths are equal
            if (bWidth != aWidth) {
                return Integer.compare(bWidth, aWidth);
            } else {
                return Integer.compare(bHeight, aHeight);
            }
        });

        return sortedResolutions.toArray(new String[0]);
    }

    public static Streamer.Size getStreamVideoSizeWithInfoUSB(CameraInfo cameraInfo) {
        if (cameraInfo == null || cameraInfo.recordSizes == null) {
            Log.e("SettingsUtils", "CameraInfo or recordSizes is null in getStreamVideoSize()");
            return new Streamer.Size(1280, 720); // Return a default size instead of crashing
        }
        Streamer.Size videoSize = null;
        String[] camera_sizes = new String[cameraInfo.recordSizes.length];
        for (int i = 0; i < cameraInfo.recordSizes.length; i++) {
            camera_sizes[i] = cameraInfo.recordSizes[i].toString();
        }
        int video_resolution = AppPreference.getInt(AppPreference.KEY.STREAMING_RESOLUTION, 0);
        if (video_resolution == -1 || video_resolution >= camera_sizes.length) {
            video_resolution = 0;
        }
        String size = camera_sizes[video_resolution];
        String[] values = size.split("x");
        int width = Integer.parseInt(values[0]);
        int height = Integer.parseInt(values[1]);
        videoSize = new Streamer.Size(width, height);
//        "video/avc"
        videoSize = verifyResolution("video/avc", videoSize);
        return videoSize;
    }

    public static Streamer.Size getStreamVideoSizeUSBNew(String[] camera_sizes) {
        Log.e(TAG, "getStreamVideoSizeUSBNew: setting from new");
        int video_resolution = AppPreference.getInt(AppPreference.KEY.STREAMING_RESOLUTION, 0);
        if (video_resolution == -1 || video_resolution >= camera_sizes.length) {
            video_resolution = camera_sizes.length - 1;
        }
        if (camera_sizes.length > 0) {
            String size = camera_sizes[video_resolution];
            String[] values = size.split("x");
            int width = Integer.parseInt(values[0]);
            int height = Integer.parseInt(values[1]);
            Streamer.Size videoSize = new Streamer.Size(width, height);
            videoSize = verifyResolution("video/avc", videoSize);
            return videoSize;
        }else {
            Streamer.Size videoSize = new Streamer.Size(640, 480);
            videoSize = verifyResolution("video/avc", videoSize);
            return videoSize;
        }
    }

    public static Streamer.Size getStreamVideoSizeUSB(List<String> recordSizes) {
        if (recordSizes == null || recordSizes.isEmpty()) {
            Log.e("SettingsUtils", "CameraInfo or recordSizes is null in getStreamVideoSize()");
            return new Streamer.Size(620, 480); // Return a default size instead of crashing
        }
        Streamer.Size videoSize = null;
        String[] camera_sizes = new String[recordSizes.size()];
        for (int i = 0; i < recordSizes.size(); i++) {
            camera_sizes[i] = recordSizes.get(i);
        }
        int video_resolution = AppPreference.getInt(AppPreference.KEY.STREAMING_RESOLUTION, 0);
        if (video_resolution == -1 || video_resolution >= camera_sizes.length) {
            video_resolution = 0;
        }
        String size = camera_sizes[video_resolution];
        String[] values = size.split("x");
        int width = Integer.parseInt(values[0]);
        int height = Integer.parseInt(values[1]);
        videoSize = new Streamer.Size(width, height);
//        "video/avc"
        videoSize = verifyResolution("video/avc", videoSize);
        return videoSize;
    }

    public static Streamer.Size getVideoSize(Context context, CameraInfo cameraInfo) {
        Streamer.Size videoSize = null;
        if (cameraInfo == null || cameraInfo.recordSizes == null || cameraInfo.recordSizes.length == 0) {
            return videoSize;
        }

        if (DeepLink.getInstance().hasImportedVideoSize()) {
            return DeepLink.getInstance().getVideoSize(cameraInfo, context);
        }

        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        String videoSizeList = sp.getString(context.getString(R.string.video_size_key), null);
        int sizeIndex = (videoSizeList != null) ? Integer.parseInt(videoSizeList) : 0;
        if (sizeIndex < 0 || sizeIndex >= cameraInfo.recordSizes.length) {
            videoSize = cameraInfo.recordSizes[0];
        } else {
            videoSize = cameraInfo.recordSizes[sizeIndex];
        }
        // Some resolution was previously selected, return it as is and later check if encoder can handle it
        if (videoSizeList != null) {
            return videoSize;
        }
        // Reduce 4K to FullHD, because some encoders can fail with 4K frame size.
        // https://source.android.com/compatibility/android-cdd.html#5_2_video_encoding
        // Video resolution: 320x240px, 720x480px, 1280x720px, 1920x1080px.
        // If no FullHD support found, leave video size as is.
        if (videoSize.width > 1920 || videoSize.height > 1088) {
            for (Streamer.Size size : cameraInfo.recordSizes) {
                if (size.width == 1920 && (size.height == 1080 || size.height == 1088)) {
                    videoSize = size;
                    Log.d(TAG, "Reduce 4K to " + size.height + "p");
                    break;
                }
            }
        }
        return videoSize;
    }

    public static String videoType(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        final String value = sp.getString(context.getString(R.string.video_codec_key), MediaFormat.MIMETYPE_VIDEO_AVC);

        if (!MediaFormat.MIMETYPE_VIDEO_AVC.equals(value) && !MediaFormat.MIMETYPE_VIDEO_HEVC.equals(value)) {
            sp.edit().remove(context.getString(R.string.video_codec_key)).commit();
            return MediaFormat.MIMETYPE_VIDEO_AVC;
        }

        if (MediaFormat.MIMETYPE_VIDEO_HEVC.equals(value)) {
            final MediaCodecInfo info = SettingsUtils.selectCodec(MediaFormat.MIMETYPE_VIDEO_HEVC);
            if (info != null) {
                return MediaFormat.MIMETYPE_VIDEO_HEVC;
            }
        }
        return MediaFormat.MIMETYPE_VIDEO_AVC;
    }

    public static String codecDisplayName(String mimeType) {
        switch (mimeType) {
            case MediaFormat.MIMETYPE_VIDEO_AVC:
                return "H.264";
            case MediaFormat.MIMETYPE_VIDEO_HEVC:
                return "HEVC";
            default:
                return mimeType;
        }
    }

    // Important note: "device runs at least Android Lollipop" is not equal to "use Camera2 API".
    // Refer to SettingsUtils / allowCamera2Support and keep this logic in your app.
    public static boolean isUsingCamera2(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        switch (Integer.parseInt(sp.getString(context.getString(R.string.camera_api_key), "0"))) {
            case API_CAMERA:
                return false;
            case API_CAMERA2:
                return true;
            default:
                return allowCamera2Support(context);
        }
    }

    // Please note: you can't use only Camera, because even if Camera api still
    // works on new devices, it is better to use modern Camera2 api if possible.
    // For example, Nexus 5X must use Camera2:
    // http://www.theverge.com/2015/11/9/9696774/google-nexus-5x-upside-down-camera
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static boolean allowCamera2Support(Context context) {

        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;

        Log.d(TAG, manufacturer + " " + model);

        // Some known camera api dependencies and issues:

        // Moto X Pure Edition, Android 6.0; Screen freeze reported with Camera2
        if (manufacturer.equalsIgnoreCase("motorola") && model.equalsIgnoreCase("clark_retus")) {
            return false;
        }

        /*
         LEGACY Camera2 implementation has problem with aspect ratio.
         Rather than allowing Camera2 API on all Android 5+ devices, we restrict it to
         cases where all cameras have at least LIMITED support.
         (E.g., Nexus 6 has FULL support on back camera, LIMITED support on front camera.)
         For now, devices with only LEGACY support should still use Camera API.
        */
        boolean result = true;
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                int support = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);

                switch (support) {
                    case CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY:
                        Log.d(TAG, "Camera " + cameraId + " has LEGACY Camera2 support");
                        break;
                    case CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED:
                        Log.d(TAG, "Camera " + cameraId + " has LIMITED Camera2 support");
                        break;
                    case CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL:
                        Log.d(TAG, "Camera " + cameraId + " has FULL Camera2 support");
                        break;
                    default:
                        Log.d(TAG, "Camera " + cameraId + " has LEVEL_3 or greater Camera2 support");
                        break;
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N
                        && support == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                    // Can't use Camera2, bul let other cameras info to log
                    result = false;
                }
            }
        } catch (CameraAccessException | IllegalArgumentException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            result = false;
        }
        return result;
    }

    public static int focusMode(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        final String value = sp.getString(context.getString(R.string.pref_focus_mode_key), context.getString(R.string.focus_mode_continuous_video));
        final int mode = Integer.parseInt(value);
        //Log.d(TAG, "focus_mode=" + mode);
        return mode;
    }

    public static String awbMode16(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        final String value = sp.getString(context.getString(R.string.pref_awb_mode_key), context.getString(R.string.awb_mode_auto));
        final int mode = Integer.parseInt(value);
        //Log.d(TAG, "awb_mode=" + AWB_MAP_16.get(mode));
        return AWB_MAP_16.get(mode);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static int awbMode21(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        final String value = sp.getString(context.getString(R.string.pref_awb_mode_key), context.getString(R.string.awb_mode_auto));
        final int mode = Integer.parseInt(value);
        return AWB_MAP_21.get(mode);
    }

    public static String antibandingMode16(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        final String value = sp.getString(context.getString(R.string.pref_antibanding_mode_key), context.getString(R.string.antibanding_mode_off));
        final int mode = Integer.parseInt(value);
        //Log.d(TAG, "antibanding_mode=" + ANTIBANDING_MAP_16.get(mode));
        return ANTIBANDING_MAP_16.get(mode);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static int antibandingMode21(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        final String value = sp.getString(context.getString(R.string.pref_antibanding_mode_key), context.getString(R.string.antibanding_mode_off));
        final int mode = Integer.parseInt(value);
        //Log.d(TAG, "antibanding_mode=" + ANTIBANDING_MAP_21.get(mode));
        return ANTIBANDING_MAP_21.get(mode);
    }

    public static boolean videoStabilizationMode16(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        final String value = sp.getString(context.getString(R.string.video_stabilization_mode_key), context.getString(R.string.video_stabilization_mode_off));
        final boolean mode = context.getString(R.string.video_stabilization_mode_on).equals(value);
        //Log.d(TAG, "video_stabilization_mode=" + mode);
        return mode;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static int videoStabilizationMode21(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        final String value = sp.getString(context.getString(R.string.video_stabilization_mode_key), context.getString(R.string.camera2_option_mode_none));
        final int mode = Integer.parseInt(value);
        //Log.d(TAG, "video_stabilization_mode=" + VIDEO_STABILIZATION_MODE_MAP_21.get(mode));
        return VIDEO_STABILIZATION_MODE_MAP_21.get(mode);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static int opticalStabilizationMode21(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        final String value = sp.getString(context.getString(R.string.optical_stabilization_mode_key), context.getString(R.string.camera2_option_mode_none));
        final int mode = Integer.parseInt(value);
        //Log.d(TAG, "optical_stabilization_mode=" + OPTICAL_STABILIZATION_MODE_MAP_21.get(mode));
        return OPTICAL_STABILIZATION_MODE_MAP_21.get(mode);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static int noiseReductionMode21(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        final String value = sp.getString(context.getString(R.string.noise_reduction_mode_key), context.getString(R.string.camera2_option_mode_none));
        final int mode = Integer.parseInt(value);
        //Log.d(TAG, "noise_reduction_mode=" + NOISE_REDUCTION_MAP_21.get(mode));
        return NOISE_REDUCTION_MAP_21.get(mode);
    }

    public static int exposureCompensation(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        final String value = sp.getString(context.getString(R.string.pref_exposure_compensation_key), "0");
        final int exposure = Integer.parseInt(value);
        //Log.d(TAG, "exposure_compensation=" + exposure);
        return exposure;
    }

    public static boolean useBluetooth(Context context) {
        return AppPreference.getBool(AppPreference.KEY.BLUETOOTH_MIC, false);
    }

    public static boolean useUSBBluetooth(Context context) {
        return AppPreference.getBool(AppPreference.KEY.BLUETOOTH_USB_MIC, false);
    }

    //    private static int audioSourceToInt(Context context, String value) {
//        int source = MediaRecorder.AudioSource.CAMCORDER;
//
//        if (value.equals(context.getString(R.string.audio_src_camcorder)))
//            source = MediaRecorder.AudioSource.CAMCORDER;
//        else if (value.equals(context.getString(R.string.audio_src_mic)))
//            source = MediaRecorder.AudioSource.MIC;
//        else if (value.equals(context.getString(R.string.audio_src_default)))
//            source = MediaRecorder.AudioSource.DEFAULT;
//        else if (value.equals(context.getString(R.string.audio_src_voice_communication)))
//            source = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
//
//        return source;
//    }
    private static int audioSourceToInt(Context context, int index) {
        switch (index) {
            case 0:
                return MediaRecorder.AudioSource.DEFAULT;
            case 1:
                return MediaRecorder.AudioSource.CAMCORDER;
            case 2:
                return MediaRecorder.AudioSource.MIC;
            case 3:
                return MediaRecorder.AudioSource.VOICE_COMMUNICATION;
        }
        return MediaRecorder.AudioSource.DEFAULT;
    }

    private static String getDefaultAudioSourceBluetooth(Context context) {
        // On Lollipop 5.0, recording from the input mic requires that the input source
        // be set to AudioSource.VOICE_COMMUNICATIONS. On earlier versions of android,
        // the input source should be AudioSource.MIC.
        // https://issuetracker.google.com/issues/37029016
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            context.getString(R.string.audio_src_mic);
        }
        if (Build.MANUFACTURER.equalsIgnoreCase("samsung")) {
            context.getString(R.string.audio_src_mic);
        }
        return context.getString(R.string.audio_src_voice_communication);
    }

    private static int getAudioSourceBluetooth(Context context) {
        int index = AppPreference.getInt(AppPreference.KEY.BLE_AUDIO_SRC, 0);
        return audioSourceToInt(context, index);
    }

    private static int getUSBAudioSourceBluetooth(Context context) {
        int index = AppPreference.getInt(AppPreference.KEY.USB_BLE_AUDIO_SRC, 0);
        return audioSourceToInt(context, index);
    }

    private static int getAudioSource(Context context) {
        int index = AppPreference.getInt(AppPreference.KEY.AUDIO_SRC, 0);
        return audioSourceToInt(context, index);
    }

    private static int getUSBAudioSource(Context context) {
        int index = AppPreference.getInt(AppPreference.KEY.USB_AUDIO_SRC, 0);
        return audioSourceToInt(context, index);
    }

    public static int audioSource(Context context) {
        if (useBluetooth(context)) {
            return getAudioSourceBluetooth(context);
        } else {
            return getAudioSource(context);
        }
    }

    public static int usbAudioSource(Context context) {
        if (useUSBBluetooth(context)) {
            return getUSBAudioSourceBluetooth(context);
        } else {
            return getUSBAudioSource(context);
        }
    }

    public static int sampleRate(Context context) {
        if (context == null) {
            Log.e("SettingsUtils", "Context is null in sampleRate, using default sample rate");
            return 44100; // Default sample rate
        }
        
        if (useBluetooth(context)) {
            return 8000;
        }

        String[] sample_rates = context.getResources().getStringArray(R.array.sample_rates);
        int index = AppPreference.getInt(AppPreference.KEY.SAMPLE_RATE, 7);
        return Integer.parseInt(sample_rates[index]);
    }

    public static int usbSampleRate(Context context) {
        if (useUSBBluetooth(context)) {
            return 8000;
        }

        String[] sample_rates = context.getResources().getStringArray(R.array.sample_rates);
        int index = AppPreference.getInt(AppPreference.KEY.USB_SAMPLE_RATE, 7);
        return Integer.parseInt(sample_rates[index]);
    }

    public static int channelCount(Context context) {
//        if (useBluetooth(context)) {
//            return 1;
//        }

//        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
//        final int channelCount = Integer.parseInt(sp.getString(context.getString(R.string.channel_count_key), context.getString(R.string.channel_count_default)));
//        return channelCount;
        // bluetooth, mono - 1, stereo - 2
        return 1;
    }

    public static int usbChannelCount(Context context) {
        if (useUSBBluetooth(context)) {
            return 1;
        }
        if (AppPreference.getInt(AppPreference.KEY.CHANNEL_COUNT, 0) == 1) {
            return 1;
        } else {
            return 2;
        }

    }

    public static int audioBitRate(Context context) {
        String[] bitrates = context.getResources().getStringArray(R.array.audio_bitrate);
        int position = AppPreference.getInt(AppPreference.KEY.STREAMING_AUDIO_BITRATE, 0);
        String bitrate = bitrates[position].toLowerCase().replaceAll("kbps", "").replaceAll(" ", "");
        // Convert from kbps to bps
        return Integer.parseInt(bitrate) * 1000;
    }

    public static int usbAudioBitrate(Context context) {
        String[] bitrates = context.getResources().getStringArray(R.array.audio_bitrate);
        int position = AppPreference.getInt(AppPreference.KEY.USB_AUDIO_BITRATE, 0);
        String bitrate = bitrates[position].toLowerCase().replaceAll("kbps", "").replaceAll(" ", "");
        // Convert from kbps to bps
        return Integer.parseInt(bitrate) * 1000;
    }

    public static int videoBitRate(Context context) {
        int bitRate = AppPreference.getInt(AppPreference.KEY.VIDEO_BITRATE, 1024);
        // Convert from kbps to bps
        return bitRate * 1000;
    }

    public static int castBitRate(Context context) {
        int bitRate = AppPreference.getInt(AppPreference.KEY.CAST_BITRATE, 2048);
        // Convert from kbps to bps
        return bitRate * 1000;
    }

    public static int streamingBitRate(Context context) {
        //Log.d(TAG, "video_bitrate=" + bitRate);1496
        int bitRate = AppPreference.getInt(AppPreference.KEY.STREAMING_BITRATE, 1024);
        // Convert from kbps to bps
        return bitRate * 1000;
    }

    public static int getUSBStreamBitrate(Context context) {
        //Log.d(TAG, "video_bitrate=" + bitRate);1496
        int vs = AppPreference.getInt(AppPreference.KEY.STREAMING_QUALITY,0);
        if (vs == 5) {
            int bitRate = AppPreference.getInt(AppPreference.KEY.STREAMING_BITRATE, 1024);
            Log.e(TAG, "streamingBitRate: " + bitRate);
            // Convert from kbps to bps
            return bitRate * 1000;
        }else {
            // Convert from kbps to bps
            return 1496 * 1000;
        }
    }

    public static int streamingAudioBitRate(Context context) {
        // Return 100 kbps converted to bps
        return 100 * 1000;
    }

    public static int maxBufferItems(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        final String value = sp.getString(context.getString(R.string.max_buffer_items_key), Integer.toString(Streamer.MAX_BUFFER_ITEMS));
        int maxBufferItems;
        try {
            maxBufferItems = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            maxBufferItems = Streamer.MAX_BUFFER_ITEMS;
            sp.edit().remove(context.getString(R.string.max_buffer_items_key)).apply();
        }
        Log.d(TAG, "max_buffer_items=" + maxBufferItems);
        return maxBufferItems;
    }

    public static int keyFrameIntervalVideo(Context context) {
        return AppPreference.getInt(AppPreference.KEY.VIDEO_KEYFRAME, 2);
    }

    public static int keyFrameIntervalStream(Context context) {
        return AppPreference.getInt(AppPreference.KEY.STREAMING_KEYFRAME, 2);
    }

    public static float fpsVideo(Context context) {
        return (float) AppPreference.getInt(AppPreference.KEY.VIDEO_FRAME, 30);
    }

    public static float fpsStream(Context context) {
        return (float) AppPreference.getInt(AppPreference.KEY.STREAMING_FRAME, 30);
    }

    public static float fpsCast(Context context) {
        return (float) AppPreference.getInt(AppPreference.KEY.CAST_FRAME, 30);
    }

    public static Streamer.FpsRange fpsRange(Context context) {
        final int fpsMin = AppPreference.getInt(AppPreference.KEY.FPS_RANGE_MIN, 30);
        final int fpsMax = AppPreference.getInt(AppPreference.KEY.FPS_RANGE_MAX, 30);

        if (fpsMin < 0 || fpsMax < 0) {
            return null;
        }

        Streamer.FpsRange range = new Streamer.FpsRange(fpsMin, fpsMax);
        Log.d(TAG, "fps_range=" + range);
        return range;
    }

    public static Streamer.FpsRange streamerfpsRange(Context context) {
        float fps = fpsStream(context);
        Streamer.FpsRange range = new Streamer.FpsRange((int) fps, (int) fps);
        Log.d(TAG, "fps_range=" + range);
        return range;
    }

    public static int adaptiveBitrate(Context context) {
        int mode = AppPreference.getInt(AppPreference.KEY.ADAPTIVE_MODE, 3); // Default to "Off" mode
        if (mode == 0) { // logarithmic
            return SettingsUtils.ADAPTIVE_BITRATE_MODE1;
        } else if (mode == 1) { // ladder ascend
            return SettingsUtils.ADAPTIVE_BITRATE_MODE2;
        } else if (mode == 2) { // hybrid
            return SettingsUtils.ADAPTIVE_BITRATE_HYBRID;
        } else if (mode == 3) { // off
            return SettingsUtils.ADAPTIVE_BITRATE_OFF;
        }
        return SettingsUtils.ADAPTIVE_BITRATE_OFF; // Default to "Off" mode
    }

    public static int castAdaptiveBitrate(Context context) {
        int mode = AppPreference.getInt(AppPreference.KEY.ADAPTIVE_MODE_CAST, 3); // Default to "Off" mode
        if (mode == 0) { // logarithmic
            return SettingsUtils.ADAPTIVE_BITRATE_MODE1;
        } else if (mode == 1) { // ladder ascend
            return SettingsUtils.ADAPTIVE_BITRATE_MODE2;
        } else if (mode == 2) { // hybrid
            return SettingsUtils.ADAPTIVE_BITRATE_HYBRID;
        } else if (mode == 3) { // off
            return SettingsUtils.ADAPTIVE_BITRATE_OFF;
        }
        return SettingsUtils.ADAPTIVE_BITRATE_OFF; // Default to "Off" mode
    }

    public static boolean adaptiveFps(Context context) {
        return AppPreference.getBool(AppPreference.KEY.ADAPTIVE_FRAMERATE, false);
    }

    public static boolean isAllowedAudio() {
        return SettingsUtils.isUseAudio() && SettingsUtils.isRecordAudio();
    }

    public static boolean isCastAudioAllowed(){
        int audioSource = AppPreference.getInt(AppPreference.KEY.AUDIO_OPTION_AUDIO_SETTING, 0);
        if (audioSource == 0) {
            return false;
        }else {
            return true;
        }
    }

    public static Streamer.MODE streamerMode() {
        if (isAllowedAudio()) {
            return Streamer.MODE.AUDIO_VIDEO;
        } else {
            return Streamer.MODE.VIDEO_ONLY;
        }
    }

    public static Streamer.MODE streamerAudioMode() {
        return Streamer.MODE.AUDIO_ONLY;
    }

    public static boolean record(Context context) {
        return true;
//        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
//        return sp.getBoolean(context.getString(R.string.pref_mp4rec_key), Boolean.parseBoolean(context.getString(R.string.pref_mp4rec_default)));
    }

    public static boolean verticalVideo(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(context.getString(R.string.vertical_video_key), Boolean.parseBoolean(context.getString(R.string.vertical_video_default)));
    }

    public static boolean lockedOrientation(Context context) {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return !sp.getBoolean(context.getString(R.string.adjust_stream_orientation_key), Boolean.parseBoolean(context.getString(R.string.adjust_stream_orientation_default)));
    }

    // limit active connections
    public static final int CONN_MAX = 3;

    public static List<Connection> connections() {
//        return Connection.find(Connection.class,
//                "active=?", new String[]{"1"}, null, "name ASC", Integer.toString(CONN_MAX));
        List<Connection> result = new ArrayList<>();
        Connection connection = new Connection();
        connection.name = "rtsp-stream";
        connection.username = AppPreference.getStr(AppPreference.KEY.STREAM_USERNAME, "");
        connection.password = AppPreference.getStr(AppPreference.KEY.STREAM_PASSWORD, "");
        connection.mode = SettingsUtils.streamerMode().ordinal();
        String channel = AppPreference.getStr(AppPreference.KEY.STREAM_CHANNEL, "");
        if (!TextUtils.isEmpty(channel)) {
            connection.url = String.format("%s/%s", AppPreference.getStr(AppPreference.KEY.STREAM_BASE, ""), channel);
            if (false) {
//                connection.url = "rtsp://41.216.179.31:8554/jorge";
            }
            result.add(connection);
        }
        Integer streamingMode = AppPreference.getInt(AppPreference.KEY.STREAMING_MODE, 0);
        if (streamingMode == 1) {
            String serverIpTest = AppPreference.getStr(AppPreference.KEY.LOCAL_STREAM_IP_TEST,"76.239.139.178");
            Integer portTest = AppPreference.getInt(AppPreference.KEY.LOCAL_STREAM_PORT_TEST,8554);
            String wifiDirectChannelTest = "harrytest";
            // "rtsp://76.239.139.178:8554/harrytest";
            String serverIp = AppPreference.getStr(AppPreference.KEY.LOCAL_STREAM_IP,"172.20.1.1");
            Integer port =  AppPreference.getInt(AppPreference.KEY.LOCAL_STREAM_PORT,554);
            String username =AppPreference.getStr(AppPreference.KEY.LOCAL_STREAM_NAME,"");
            String password =AppPreference.getStr(AppPreference.KEY.LOCAL_STREAM_PASSWORD,"");
            String wifiDirectchannel = AppPreference.getStr(AppPreference.KEY.LOCAL_STREAM_CHANNEL, "");
            List<Connection> wifiDirectResult = new ArrayList<>();
            Connection wifiDirectconnection = new Connection();
            wifiDirectconnection.name = "rtsp-wifi-stream";
            wifiDirectconnection.username = username;
            wifiDirectconnection.password = password;
            wifiDirectconnection.mode = SettingsUtils.streamerMode().ordinal();
            String finalUrl;
            if (!TextUtils.isEmpty(username)) {
                finalUrl = String.format(Locale.getDefault(), "rtsp://%s:%s@%s:%d/%s", username, password, serverIp, port, wifiDirectchannel);
            } else {
                finalUrl = String.format(Locale.getDefault(), "rtsp://%s:%d/%s", serverIp, port, wifiDirectchannel);
            }
            wifiDirectconnection.url = finalUrl;// "rtsp://76.239.139.178:8554/harrytest";
            Log.e(TAG, "connection url: " + finalUrl );
            wifiDirectResult.add(wifiDirectconnection);
            return wifiDirectResult;
        }else {
            Log.e(TAG, "connection url: " + connection.url );
            return result;
        }
    }

    public static class UriResult {
        public String uri;
        public String scheme;
        public String host;
        public String error;

        public static boolean isRtmp(final String s) {
            return "rtmp".equalsIgnoreCase(s) || "rtmps".equalsIgnoreCase(s);
        }

        public static boolean isRtsp(final String s) {
            return "rtsp".equalsIgnoreCase(s) || "rtsps".equalsIgnoreCase(s);
        }

        public static boolean isSrt(final String s) {
            return "srt".equalsIgnoreCase(s);
        }

        public static boolean isRist(final String s) {
            return "rist".equalsIgnoreCase(s);
        }

        public static boolean isSupported(final String s) {
            return isRtmp(s) || isRtsp(s) || isSrt(s) || isRist(s);
        }

        public boolean isRtmp() {
            return isRtmp(scheme);
        }

        public boolean isRtsp() {
            return isRtsp(scheme);
        }

        public boolean isSrt() {
            return isSrt(scheme);
        }

        public boolean isRist() {
            return isRist(scheme);
        }
    }

    public static UriResult parseUrl(Context context, final String originalUri) {

        // android.net.Uri breaks IPv6 addresses in the wrong places, use Java's own URI class
        final UriResult connection = new UriResult();
        String newUri = originalUri;

        final URI uri;
        try {
            uri = new URI(originalUri);
        } catch (URISyntaxException e) {
            connection.error = e.getMessage();
            return connection;
        }

        final String host = uri.getHost();
        if (host == null) {
            connection.error = context.getString(R.string.no_host);
            return connection;
        }
        connection.host = host;

        final String scheme = uri.getScheme();
        if (scheme == null) {
            connection.error = context.getString(R.string.no_scheme);
            return connection;
        }
        if (!UriResult.isSupported(scheme)) {
            connection.error = String.format(context.getString(R.string.unsupported_scheme), scheme);
            return connection;
        }
        connection.scheme = scheme;

        if (connection.isRtmp()) {
            final String[] splittedPath = originalUri.split("/");
            if (splittedPath.length < 5) {
                connection.error = context.getString(R.string.no_app_stream);
                return connection;
            }
        }

        final int port = uri.getPort();
        if ((connection.isSrt() || connection.isRist()) && port <= 0) {
            connection.error = context.getString(R.string.no_port);
            return connection;
        }

        if (connection.isSrt()) {
            try {
                final URI builder = new URI(scheme, null, host, port, null, null, null);
                newUri = builder.toString();
            } catch (URISyntaxException e) {
                connection.error = e.getMessage();
                return connection;
            }

        } else {
            final String userInfo = uri.getUserInfo();
            if (userInfo != null) {
                connection.error = String.format(context.getString(R.string.userinfo_found), userInfo);
                return connection;
            }
        }

        connection.uri = newUri;
        return connection;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static MediaCodecInfo selectCodec(String mimeType) {
        MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        for (MediaCodecInfo codecInfo : mediaCodecList.getCodecInfos()) {
            if (!codecInfo.isEncoder()) {
                continue;
            }
            for (String type : codecInfo.getSupportedTypes()) {
                if (type.equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    public static boolean verifyResolution(String size) {
        String[] values = size.split("x");
        int width = Integer.parseInt(values[0]);
        int height = Integer.parseInt(values[1]);
        Streamer.Size streamerSize = verifyResolution("video/avc", new Streamer.Size(width, height));
        if (width != 1280 && height != 720) {
            if (streamerSize.width == 1280 && streamerSize.height == 720) {
                return false;
            }
        }
        return true;
    }

    public static Streamer.Size verifyResolution(String type, Streamer.Size videoSize) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final MediaCodecInfo info = selectCodec(type);
            final MediaCodecInfo.CodecCapabilities capabilities = info.getCapabilitiesForType(type);
            final MediaCodecInfo.VideoCapabilities videoCapabilities = capabilities.getVideoCapabilities();
            if (!videoCapabilities.isSizeSupported(videoSize.width, videoSize.height)) {
                // 1280x720 should be supported by every device running Android 4.1+
                // https://source.android.com/compatibility/4.1/android-4.1-cdd.pdf [chapter 5.2]
//                final String msg = String.format(RecordApp.getContext().getString(R.string.unsupported_resolution), videoSize);
//                MessageUtil.showToast(RecordApp.getContext(), msg);
                return new Streamer.Size(1280, 720);
            }
        }
        return videoSize;
    }

    public static boolean isUseAudio() {
        return AppPreference.getBool(AppPreference.KEY.USE_AUDIO, false);
    }

    public static boolean isRecordAudio() {
        return AppPreference.getBool(AppPreference.KEY.RECORD_AUDIO, false);
    }

    public static int optionSampleRate(Context context) {
        if (context == null) {
            Log.e("SettingsUtils", "Context is null in optionSampleRate, using default sample rate");
            return 44100; // Default sample rate
        }
        
        if (useBluetooth(context)) {
            return 8000;
        }

        String[] sample_rates = context.getResources().getStringArray(R.array.audio_option_sample_rates);
        int index = AppPreference.getInt(AppPreference.KEY.AUDIO_OPTION_SAMPLE_RATE, 0);
        String sampleRate = sample_rates[index].toLowerCase().replaceAll("khz", "").replaceAll(" ", "");
        return Integer.parseInt(sampleRate);
    }

    public static int optionChannelCount(Context context) {
        if (useUSBBluetooth(context)) {
            return 1;
        }
        if (AppPreference.getInt(AppPreference.KEY.AUDIO_OPTION_CHANNEL_COUNT, 0) == 1) {
            return 1;
        } else {
            return 2;
        }

    }

    private static int getOptionAudioSource(Context context) {
        int index = AppPreference.getInt(AppPreference.KEY.AUDIO_OPTION_AUDIO_SRC, 0);
        return audioSourceToInt(context, index);
    }

    public static int optionAudioSource(Context context) {
        if (useBluetooth(context)) {
            return getAudioSourceBluetooth(context);
        } else {
            return getOptionAudioSource(context);
        }
    }

    public static int optionAudioBitRate(Context context) {
        if (context == null) {
            Log.e("SettingsUtils", "Context is null in optionAudioBitRate, using default bitrate");
            return 16 * 1000; // Default bitrate
        }
        
        String[] bitrates = context.getResources().getStringArray(R.array.audio_option_audio_bitrate);
        int position = AppPreference.getInt(AppPreference.KEY.AUDIO_OPTION_BITRATE, 0);
        if (position == 0) {
            return 16 * 1000;
        } else {
            String bitrate = bitrates[position].toLowerCase().replaceAll("kbps", "").replaceAll(" ", "");
            return Integer.parseInt(bitrate) * 1000;
        }
    }

    public static AudioConfig audioConfig(Context context) {
        AudioConfig audioConfig = new AudioConfig();
        audioConfig.type = AudioConfig.INPUT_TYPE.PCM;
        audioConfig.sampleRate = sampleRate(context);
        audioConfig.channelCount = channelCount(context);
        audioConfig.audioSource = audioSource(context);
        audioConfig.bitRate = audioBitRate(context);
        return audioConfig;
    }

    public static AudioConfig usbAudioConfig(Context context) {
        AudioConfig audioConfig = new AudioConfig();
        audioConfig.type = AudioConfig.INPUT_TYPE.PCM;
        audioConfig.sampleRate = usbSampleRate(context);
        audioConfig.channelCount = usbChannelCount(context);
        audioConfig.audioSource = usbAudioSource(context);
        audioConfig.bitRate = usbAudioBitrate(context);
        return audioConfig;
    }

    public static AudioConfig audioOptionConfig(Context context) {
        AudioConfig audioConfig = new AudioConfig();
        audioConfig.sampleRate = optionSampleRate(context);
        audioConfig.channelCount = optionChannelCount(context);
        audioConfig.audioSource = optionAudioSource(context);
        audioConfig.bitRate = optionAudioBitRate(context);
        return audioConfig;
    }

    public static Streamer.FpsRange findFpsRange(Context context, Streamer.FpsRange[] fpsRanges) {
        if (fpsRanges == null || fpsRanges.length < 2) {
            // old devices usually provide single fps range per camera
            // so app don't need to set it explicitly
            return null;
        }

        if (DeepLink.getInstance().hasImportedFpsRange()) {
            return DeepLink.getInstance().findFpsRange(context, fpsRanges);
        }

        Streamer.FpsRange fpsRange = fpsRange(context);
        for (Streamer.FpsRange range : fpsRanges) {
            if (range.equals(fpsRange)) {
                return range;
            }
        }
        // sometimes front camera's ranges set doesn't match back camera's ranges set
        // use default fps range
        return null;
    }

    public static Streamer.FpsRange findStreamFpsRange(Context context, Streamer.FpsRange[] fpsRanges) {
        if (fpsRanges == null || fpsRanges.length < 2) {
            // old devices usually provide single fps range per camera
            // so app don't need to set it explicitly
            return null;
        }

        if (DeepLink.getInstance().hasImportedFpsRange()) {
            return DeepLink.getInstance().findFpsRange(context, fpsRanges);
        }

        Streamer.FpsRange fpsRange = fpsRange(context);
        for (Streamer.FpsRange range : fpsRanges) {
            if (range.equals(fpsRange)) {
                return range;
            }
        }
        // sometimes front camera's ranges set doesn't match back camera's ranges set
        // use default fps range
        return null;
    }

    public static float findFps(Context context, Streamer.FpsRange[] fpsRanges) {
        Streamer.FpsRange fpsRange = findFpsRange(context, fpsRanges);
        if (fpsRange == null) {
            return fpsVideo(context);
        }
        // For Camera API: The values are multiplied by 1000 and represented in integers.
        // For Camera2 API: Units: Frames per second (FPS)
        return fpsRange.fpsMax < 1000 ? fpsRange.fpsMax : fpsRange.fpsMax / 1000.0f;
    }

    public static float findStreamFps(Context context, Streamer.FpsRange[] fpsRanges) {
        Streamer.FpsRange fpsRange = findStreamFpsRange(context, fpsRanges);
        if (fpsRange == null) {
            return fpsStream(context);
        }
        // For Camera API: The values are multiplied by 1000 and represented in integers.
        // For Camera2 API: Units: Frames per second (FPS)
        return fpsRange.fpsMax < 1000 ? fpsRange.fpsMax : fpsRange.fpsMax / 1000.0f;
    }

    // Set the same video size for both cameras
    // If not possible (for example front camera has no FullHD support)
    // try to find video size with the same aspect ratio
    public static Streamer.Size findFlipSize(CameraInfo cameraInfo, Streamer.Size videoSize) {
        Streamer.Size flipSize = null;

        // If secondary camera supports same resolution, use it
        for (Streamer.Size size : cameraInfo.recordSizes) {
            if (size.equals(videoSize)) {
                flipSize = size;
                break;
            }
        }

        // If same resolution not found, search for same aspect ratio
        if (flipSize == null) {
            final double targetAspectRatio = (double) videoSize.width / videoSize.height;
            for (Streamer.Size size : cameraInfo.recordSizes) {
                if (size.width < videoSize.width) {
                    final double aspectRatio = (double) size.width / size.height;
                    final double aspectDiff = targetAspectRatio / aspectRatio - 1;
                    if (Math.abs(aspectDiff) < 0.01) {
                        flipSize = size;
                        break;
                    }
                }
            }
        }

        // Same aspect ratio not found, search for less or similar frame sides
        if (flipSize == null) {
            for (Streamer.Size size : cameraInfo.recordSizes) {
                if (size.height <= videoSize.height && size.width <= videoSize.width) {
                    flipSize = size;
                    break;
                }
            }
        }

        // Nothing found, use default
        if (flipSize == null) {
            flipSize = cameraInfo.recordSizes[0];
        }

        return flipSize;
    }

    public static ConnectionConfig toConnectionConfig(Connection connection) {
        ConnectionConfig config = new ConnectionConfig();

        config.uri = connection.url;
        config.mode = Streamer.MODE.values()[connection.mode];
        config.auth = Streamer.AUTH.values()[connection.auth];
        config.username = connection.username;
        config.password = connection.password;

        return config;
    }

    public static SrtConfig toSrtConfig(Connection connection) throws URISyntaxException {
        SrtConfig config = new SrtConfig();

        // android.net.Uri breaks IPv6 addresses in the wrong places, use Java's own URI class
        final URI uri = new URI(connection.url);

        final IPAddress address = new HostName(uri.getHost()).asAddress();
        if (address != null && address.isIPv6()) {
            // Convert literal IPv6 address to IPv6 address
            // An IPv6 address is represented as eight groups of four hexadecimal digits
            // 2001:0db8:85a3:0000:0000:8a2e:0370:7334
            // Literal IPv6 addresses are enclosed in square brackets
            // https://[2001:db8:85a3:8d3:1319:8a2e:370:7348]:443
            config.host = address.toFullString();
        } else {
            config.host = uri.getHost();
        }

        config.port = uri.getPort();
        config.mode = Streamer.MODE.values()[connection.mode];
        config.passphrase = connection.passphrase; // SRTO_PASSPHRASE
        config.pbkeylen = connection.pbkeylen; // SRTO_PBKEYLEN
        config.latency = connection.latency; // SRTO_PEERLATENCY
        config.maxbw = connection.maxbw; // SRTO_MAXBW
        config.streamid = connection.streamid; // SRTO_STREAMID
        Log.e(TAG, "https:config:rtsp "+config.host );
        return config;
    }

    public static RistConfig toRistConfig(Connection connection) {
        RistConfig config = new RistConfig();
        config.uri = connection.url;
        config.mode = Streamer.MODE.values()[connection.mode];
        config.profile = RistConfig.RIST_PROFILE.values()[connection.ristProfile];

        return config;
    }

    // Find best matching FPS range
    // (fpsMax is much important to be closer to target, so we squared it)
    // In strict mode targetFps will be exact within range, otherwise just as close as possible
    public static Streamer.FpsRange nearestFpsRange(Streamer.FpsRange[] fpsRanges, float targetFps, boolean strict) {
        //Find best matching FPS range
        // (fpsMax is much important to be closer to target, so we squared it)
        float minDistance = 1e10f;
        Streamer.FpsRange range = new Streamer.FpsRange(0, 0);
        for (Streamer.FpsRange r : fpsRanges) {
            if (strict && (r.fpsMin > targetFps || r.fpsMax < targetFps)) {
                continue;
            }
            float distance = ((r.fpsMax - targetFps) * (r.fpsMax - targetFps) + Math.abs(r.fpsMin - targetFps));
            if (distance < minDistance) {
                range = r;
                if (distance < 0.01f) {
                    break;
                }
                minDistance = distance;
            }
        }
        return range;
    }

    public static int recordIntervalMin(Context context) {
        return AppPreference.getInt(AppPreference.KEY.SPLIT_TIME, 10);
    }

    public static int volumeKeysAction(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        final String value = sp.getString(context.getString(R.string.volume_keys_action_key), context.getString(R.string.volume_keys_action_start_stop));
        final int action = Integer.parseInt(value);
        //Log.d(TAG, "volume_keys_action=" + action);
        return action;
    }

    public static boolean foregroundService(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return false;
        }
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(context.getString(R.string.pref_foreground_service_key), Boolean.parseBoolean(context.getString(R.string.pref_foreground_service_default)));
    }

    public static boolean radioMode(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return false;
        }
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(context.getString(R.string.radio_mode_key), Boolean.parseBoolean(context.getString(R.string.radio_mode_default)));
    }

    public static boolean picturesAsPreviewed(Context context) {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(context.getString(R.string.pictures_as_previewed_key), Boolean.parseBoolean(context.getString(R.string.pictures_as_previewed_default)));
    }

    public static void fillCameraParameters(Context context, FocusMode cameraParameters, boolean camera2) {
        final int focusMode = SettingsUtils.focusMode(context);
        if (camera2) {
            switch (focusMode) {
                case SettingsUtils.AF_MODE_INFINITY:
                    cameraParameters.focusMode = CaptureRequest.CONTROL_AF_MODE_OFF;
                    cameraParameters.focusDistance = 0.0f; // A value of 0.0f means infinity focus.
                    break;
                case SettingsUtils.AF_MODE_CONTINUOUS_VIDEO:
                default:
                    cameraParameters.focusMode = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO;
                    break;
            }
            cameraParameters.awbMode = SettingsUtils.awbMode21(context);
            cameraParameters.antibandingMode = SettingsUtils.antibandingMode21(context);
            cameraParameters.videoStabilizationMode = SettingsUtils.videoStabilizationMode21(context);
            cameraParameters.opticalStabilizationMode = SettingsUtils.opticalStabilizationMode21(context);
            cameraParameters.noiseReductionMode = SettingsUtils.noiseReductionMode21(context);

        } else {
            switch (focusMode) {
                case SettingsUtils.AF_MODE_INFINITY:
                    cameraParameters.focusMode16 = Camera.Parameters.FOCUS_MODE_INFINITY;
                    break;
                case SettingsUtils.AF_MODE_CONTINUOUS_VIDEO:
                default:
                    cameraParameters.focusMode16 = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;
                    break;
            }
            cameraParameters.awbMode16 = SettingsUtils.awbMode16(context);
            cameraParameters.antibandingMode16 = SettingsUtils.antibandingMode16(context);
            cameraParameters.videoStabilizationMode16 = SettingsUtils.videoStabilizationMode16(context);
        }
        cameraParameters.exposureCompensation = SettingsUtils.exposureCompensation(context);
    }

    public static void groveVideoConfig(Context context, Uri.Builder uri) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        boolean cam2 = isUsingCamera2(context);
        final List<CameraInfo> cameraList = com.checkmate.android.util.CameraManager.getCameraList(context, cam2);
        if (cameraList == null || cameraList.size() == 0) {
            return;
        }
        final CameraInfo cameraInfo = SettingsUtils.getActiveCameraInfo(context, cameraList);

        String cameraID = cameraInfo.lensFacing == CameraInfo.LENS_FACING_BACK ? "0" : "1";
        uri.appendQueryParameter("enc[vid][camera]", cameraID);

        Streamer.Size size = getVideoSize(context, cameraInfo);
        String sizeStr = String.format("%dx%d", size.width, size.height);
        uri.appendQueryParameter("enc[vid][res]", sizeStr);

        boolean vertical = verticalVideo(context);
        String orientationStr = vertical ? "vertical" : "horizontal";
        uri.appendQueryParameter("enc[vid][orientation]", orientationStr);

        boolean rotation = lockedOrientation(context);
        String rotaionStr = rotation ? "follow" : "lock";
        uri.appendQueryParameter("enc[vid][liveRotation]", rotaionStr);

        Streamer.FpsRange range = fpsRange(context);
        if (range != null) {
            uri.appendQueryParameter("enc[vid][fps]", String.format("%d", range.fpsMax));
        }
        final String bitrate = sp.getString(context.getString(R.string.bitrate_key), "");
        if (!bitrate.isEmpty()) {
            uri.appendQueryParameter("enc[vid][bitrate]", bitrate);
        }

        final String videoCodecType = videoType(context);
        String codecName = videoCodecType.equals(MediaFormat.MIMETYPE_VIDEO_HEVC) ? "hevc" : "avc";
        uri.appendQueryParameter("enc[vid][format]", codecName);

        final String keyframeInterval = sp.getString(context.getString(R.string.key_frame_interval_key), "");
        if (!keyframeInterval.isEmpty()) {
            uri.appendQueryParameter("enc[vid][keyframe]", keyframeInterval);
        }

        final String abrOff = context.getString(R.string.adaptive_bitrate_value_off);
        final String abrMode = sp.getString(context.getString(R.string.adaptive_bitrate_key), abrOff);
        if (!abrMode.equals(abrOff)) {
            uri.appendQueryParameter("enc[vid][adaptiveBitrate]", abrMode);
            boolean adaptiveFps = adaptiveFps(context);
            String adaptiveFpsStr = adaptiveFps ? "1" : "0";
            uri.appendQueryParameter("enc[vid][adaptiveFps]", abrMode);
        }

        final boolean background = sp.getBoolean("pref_foreground_service_key", false);
        String bgStr = background ? "1" : "0";
        uri.appendQueryParameter("enc[vid][background]", bgStr);
    }

    public static void groveAudioConfig(Context context, Uri.Builder uri) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        final String channelCount = sp.getString(context.getString(R.string.channel_count_key), "");
        if (!channelCount.isEmpty()) {
            uri.appendQueryParameter("enc[aud][channels]", channelCount);
        }

        String audioBitrate = sp.getString(context.getString(R.string.pref_audio_bitrate_key), "");
        if (!audioBitrate.isEmpty() && audioBitrate.endsWith("000")) {
            // Make Kbps from bps by truncating trailing "000"
            audioBitrate = audioBitrate.substring(0, audioBitrate.length() - 3);
            uri.appendQueryParameter("enc[aud][bitrate]", audioBitrate);
        }
        final String sampleRate = sp.getString(context.getString(R.string.sample_rate_key), "");
        if (!sampleRate.isEmpty()) {
            uri.appendQueryParameter("enc[aud][samples]", sampleRate);
        }
        if (radioMode(context)) {
            uri.appendQueryParameter("enc[aud][audioOnly]", "on");
        }
    }

    public static void groveRecordConfig(Context context, Uri.Builder uri) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isRecord = record(context);
        String recordEnabled = isRecord ? "on" : "off";
        uri.appendQueryParameter("enc[record][enable]", recordEnabled);
        if (isRecord) {
            final String duration = sp.getString(context.getString(R.string.record_duration_key), context.getString(R.string.record_duration_default));
            uri.appendQueryParameter("enc[record][duration]", duration);
        }
    }


    public static void connnToGrove(Connection conn, Uri.Builder uri) {
        uri.appendQueryParameter("conn[][url]", conn.url);
        uri.appendQueryParameter("conn[][name]", conn.name);
        uri.appendQueryParameter("conn[][rewrite]", "on");

        if (conn.username != null && !conn.username.isEmpty()) {
            uri.appendQueryParameter("conn[][user]", "username");
        }
        if (conn.password != null && !conn.password.isEmpty()) {
            uri.appendQueryParameter("conn[][pass]", conn.password);
        }
        if (conn.mode < GROVE_MODE_MAP.length) {
            String mode = GROVE_MODE_MAP[conn.mode];
            uri.appendQueryParameter("conn[][mode]", mode);
        }
        if (conn.url.startsWith("rtmp") && conn.auth < GROVE_TARGET_MAP.length) {
            String target = GROVE_TARGET_MAP[conn.auth];
            uri.appendQueryParameter("conn[][target]", target);
        }
        if (conn.url.startsWith("srt")) {
            uri.appendQueryParameter("conn[][srtlatency]", String.format("%d", conn.latency));
            uri.appendQueryParameter("conn[][srtmaxbw]", String.format("%d", conn.maxbw));
            if (conn.passphrase != null && !conn.passphrase.isEmpty()) {
                uri.appendQueryParameter("conn[][srtpass]", conn.passphrase);
                uri.appendQueryParameter("conn[][srtpbkl]", String.format("%d", conn.pbkeylen));
            }
            if (conn.streamid != null && !conn.streamid.isEmpty()) {
                uri.appendQueryParameter("conn[][srtstreamid]", conn.streamid);
            }
        }
        if (conn.url.startsWith("rist") && conn.ristProfile < GROVE_RISTPROFILE_MAP.length) {
            String profile = GROVE_RISTPROFILE_MAP[conn.ristProfile];
            uri.appendQueryParameter("conn[][ristProfile]", profile);
        }
    }
}