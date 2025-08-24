package com.checkmate.android.util.HttpServer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.checkmate.android.AppConstant;
import com.checkmate.android.AppPreference;
import com.checkmate.android.R;
import com.checkmate.android.service.LocationManagerService;
import com.checkmate.android.ui.activity.SplashActivity;
import com.checkmate.android.ui.fragment.LiveFragment;
import com.checkmate.android.util.CommonUtil;
import com.checkmate.android.util.MainActivity;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import fi.iki.elonen.NanoHTTPD;
import toothpick.Toothpick;

public final class MyHttpServer extends NanoHTTPD {

    private static final String TAG = "MyHttpServer";
    private static final String API_PREFIX = "/api/v1/";
    private static final String MIME_JSON = "application/json";
    private static final String MIME_MP4 = "video/mp4";
    private static final int SOCKET_TIMEOUT_MS = 30_000;
    private static final String DEFAULT_KEY_B64 = "VkNTIENoZWNrbWF0ZSBBbmRyb2lkIEFwcA==";
    private static final double BYTES_IN_GB = 1024.0 * 1024.0 * 1024.0;

    // Action constants
    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";

    private final Context ctx;
    private final File recordingsDir;
    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final String apiKey;
    private ServiceManager serviceManager;
    private boolean isRunningAsDaemon = false;

    // Route handlers
    private final ConcurrentMap<String, Supplier<Response>> simpleHandlers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ParamHandler> paramHandlers = new ConcurrentHashMap<>();

    @FunctionalInterface
    private interface ParamHandler {
        Response handle(IHTTPSession session, Map<String, List<String>> params) throws Exception;
    }

    public MyHttpServer(int port, @NonNull Context applicationContext, ServiceManager serviceManager) {
        super(port);
        this.ctx = applicationContext.getApplicationContext();
        this.recordingsDir = initRecordingDir();
        this.apiKey = resolveApiKey();
        serviceManager = Toothpick
                .openScope("APP_SCOPE")
                .getInstance(ServiceManager.class);

        ServiceManager finalServiceManager = serviceManager;
        uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    finalServiceManager.switchServiceDI("usb");
                    Log.d("Test", "Service switch successful");
                } catch (Exception e) {
                    Log.e("Test", "Service switch failed", e);
                }
            }
        },5000);
        initRoutes();
    }

    @SuppressLint("NewApi")
    private String resolveApiKey() {
        String envKey = System.getenv("CHECKMATE_API_KEY");
        String prefKey = AppPreference.getStr(AppPreference.KEY.API_KEY, null);
        return Objects.requireNonNullElseGet(envKey,
                () -> Objects.requireNonNullElse(prefKey,
                        new String(Base64.decode(DEFAULT_KEY_B64, Base64.NO_WRAP))));
    }

    private File initRecordingDir() {
        File base = ctx.getExternalFilesDir(null);
        if (base == null) {
            Log.e(TAG, "External files directory is null. Cannot create recording directory.");
            return null;
        }
        File dir = new File(base, "CheckMate/Recordings");
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e(TAG, "Unable to create recording directory: " + dir.getAbsolutePath());
            } else {
                Log.i(TAG, "Recording directory created: " + dir.getAbsolutePath());
            }
        } else {
            Log.i(TAG, "Recording directory already exists: " + dir.getAbsolutePath());
        }
        return dir;
    }

    private void initRoutes() {
        // GET endpoints
        simpleHandlers.put("system/info", this::systemInfo);
        simpleHandlers.put("camera/status", this::cameraStatus);
        simpleHandlers.put("audio/status", this::audioStatus);
        simpleHandlers.put("streaming/status", this::streamingStatus);
        simpleHandlers.put("recording/status", this::recordingStatus);
        paramHandlers.put("playback/list", this::playbackList);
        simpleHandlers.put("gps/status", this::gpsStatus);

        // POST/PUT endpoints
        paramHandlers.put("camera/set", this::cameraSet);
        paramHandlers.put("audio/set", this::audioSet);
        paramHandlers.put("streaming/set", this::streamingSet);
        paramHandlers.put("recording/set", this::recordingSet);
        paramHandlers.put("playback/download", this::playbackDownload);
        paramHandlers.put("gps/set", this::gpsSet);
        paramHandlers.put("app/update", this::appUpdate);
    }

    @Override
    public Response serve(IHTTPSession session) {
        try {
            if (Method.OPTIONS.equals(session.getMethod())) {
                return cors(jsonOk(Collections.emptyMap()));
            }
            String clientApiKey = session.getHeaders().getOrDefault("x-api-key", "");
            if (!apiKey.equals(clientApiKey)) {
                Log.w(TAG, "Unauthorized API access attempt. URI: " + session.getUri());
                return cors(jsonErr(Response.Status.UNAUTHORIZED, "Unauthorized"));
            }
            if (Method.HEAD.equals(session.getMethod())) {
                return cors(newFixedLengthResponse(Response.Status.OK, MIME_JSON, ""));
            }
            String uri = session.getUri();
            if (!uri.startsWith(API_PREFIX)) {
                return cors(jsonErr(Response.Status.NOT_FOUND, "Invalid API path"));
            }
            String endpoint = uri.substring(API_PREFIX.length());

            if (Method.GET.equals(session.getMethod())) {
                Supplier<Response> handler = simpleHandlers.get(endpoint);
                if (handler != null) return cors(handler.get());
            }

            ParamHandler handler = paramHandlers.get(endpoint);
            if (handler != null) {
                Map<String, List<String>> params = parseParams(session);
                return cors(handler.handle(session, params));
            }

            Log.w(TAG, "Endpoint not found: " + endpoint + " for method " + session.getMethod());
            if (MainActivity.getInstance() != null) {
                if (MainActivity.getInstance().isShowingAccServiceAlert) {
                    MainActivity.getInstance().isShowingAccServiceAlert = false;
                    MainActivity.getInstance().alertDialog.dismiss();
                }
            }
            return cors(jsonErr(Response.Status.NOT_FOUND, "Endpoint not found"));
        } catch (ResponseException e) {
            Log.e(TAG, "ResponseException during request processing: " + e.getStatus() + " - " + e.getMessage(), e.getStatus() == Response.Status.BAD_REQUEST ? null : e);
            return cors(jsonErr(e.getStatus(), e.getMessage()));
        } catch (IOException e) {
            Log.e(TAG, "IOException during request processing. URI: " + session.getUri(), e);
            return cors(jsonErr(Response.Status.INTERNAL_ERROR, "Internal server error (IO)"));
        } catch (Exception ex) {
            Log.e(TAG, "Unhandled error during request processing. URI: " + session.getUri(), ex);
            return cors(jsonErr(Response.Status.INTERNAL_ERROR, "Internal server error"));
        }
    }

    private Map<String, List<String>> parseParams(IHTTPSession session) throws IOException, ResponseException {
        String contentType = session.getHeaders().get("content-type");

        if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("application/json")) {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String rawJson = files.getOrDefault("postData", session.getParameters().get("postData") != null && !session.getParameters().get("postData").isEmpty() ? session.getParameters().get("postData").get(0) : "{}");

            try {
                Type type = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> jsonMap = gson.fromJson(rawJson, type);
                Map<String, List<String>> result = new HashMap<>();
                if (jsonMap != null) {
                    jsonMap.forEach((key, value) -> {
                        if (value instanceof List) {
                            try {
                                @SuppressWarnings("unchecked")
                                List<?> originalList = (List<?>) value;
                                result.put(key, originalList.stream().map(String::valueOf).collect(Collectors.toList()));
                            } catch (ClassCastException e) {
                                result.put(key, Collections.singletonList(String.valueOf(value)));
                            }
                        } else {
                            result.put(key, Collections.singletonList(String.valueOf(value)));
                        }
                    });
                }
                session.getParameters().forEach((key, valueList) -> {
                    if (!result.containsKey(key) && valueList != null && !valueList.isEmpty() && !key.equals("postData")) {
                        result.put(key, valueList);
                    }
                });
                return result;
            } catch (com.google.gson.JsonSyntaxException e) {
                Log.w(TAG, "Failed to parse JSON body for Content-Type application/json. Raw: " + rawJson, e);
                throw new ResponseException(Response.Status.BAD_REQUEST, "Malformed JSON in request body.");
            }
        }
        return session.getParameters();
    }

    private Response systemInfo() {
        BatteryManager bm = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
        int battery = bm != null ? bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) : -1;
        boolean charging = bm != null && (
                bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING ||
                        bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_FULL
        );

        String appVersionName = "N/A";
        long appVersionCode = -1;
        try {
            PackageInfo pInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            appVersionName = pInfo.versionName;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                appVersionCode = pInfo.getLongVersionCode();
            } else {
                @SuppressWarnings("deprecation")
                long vc = pInfo.versionCode;
                appVersionCode = vc;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not get package info", e);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("device_name", AppPreference.getStr(AppPreference.KEY.DEVICE_NAME, Build.MODEL));
        data.put("device_id", CommonUtil.getDeviceID(ctx));
        data.put("make_model", Build.MANUFACTURER + " " + Build.MODEL);
        data.put("serial", AppPreference.getStr(AppPreference.KEY.ACTIVATION_SERIAL, AppConstant.BETA_SERIAL));
        data.put("activation_id", AppPreference.getStr(AppPreference.KEY.ACTIVATION_CODE, ""));
        data.put("email", AppPreference.getStr(AppPreference.KEY.LOGIN_EMAIL, ""));
        data.put("battery_percent", battery);
        data.put("is_charging", charging);
        data.put("expiration_date", CommonUtil.expire_date(AppConstant.expire_date));
        data.put("app_version_name", appVersionName);
        data.put("app_version_code", appVersionCode);
        data.put("android_version", Build.VERSION.RELEASE);
        data.put("api_level", Build.VERSION.SDK_INT);

        File internalStorage = ctx.getFilesDir().getParentFile();
        if (internalStorage != null) {
            long totalInternalBytes = internalStorage.getTotalSpace();
            long freeInternalBytes = internalStorage.getFreeSpace();
            data.put("internal_storage_total_gb", String.format(Locale.US, "%.2f", totalInternalBytes / BYTES_IN_GB));
            data.put("internal_storage_free_gb", String.format(Locale.US, "%.2f", freeInternalBytes / BYTES_IN_GB));
        }

        if (recordingsDir != null && recordingsDir.exists()) {
            long totalRecordingsBytes = recordingsDir.getTotalSpace();
            long freeRecordingsBytes = recordingsDir.getFreeSpace();
            data.put("recordings_dir_total_gb", String.format(Locale.US, "%.2f", totalRecordingsBytes / BYTES_IN_GB));
            data.put("recordings_dir_free_gb", String.format(Locale.US, "%.2f", freeRecordingsBytes / BYTES_IN_GB));
        }
        return jsonOk(data);
    }

    private Response audioStatus() {
        return jsonOk(Map.of(
                "enabled", AppPreference.getBool(AppPreference.KEY.RECORD_AUDIO, true),
                "type", "Microphone"
        ));
    }

    private Response streamingStatus() {
        boolean isStreaming = AppPreference.getBool(AppPreference.KEY.STREAM_STARTED, false);
        return jsonOk(Map.of(
                "is_streaming", isStreaming,
                "streaming_mode", humanReadableCamera(AppPreference.getStr(AppPreference.KEY.SELECTED_POSITION, "0"))
        ));
    }

    private Response recordingStatus() {
        boolean isRecording = AppPreference.getBool(AppPreference.KEY.RECORDING_STARTED, false);
        return jsonOk(Map.of(
                "is_recording", isRecording,
                "recording_source", humanReadableCamera(AppPreference.getStr(AppPreference.KEY.SELECTED_POSITION, "0"))
        ));
    }

    private Response cameraStatus() {
        String pref = AppPreference.getStr(AppPreference.KEY.SELECTED_POSITION, "0");
        return jsonOk(Map.of(
                "selected_camera_id", pref,
                "selected_camera_name", humanReadableCamera(pref),
                "is_usb_camera_attached", usbCameraAttached()
        ));
    }

    private boolean isAppInForeground() {
        ActivityManager activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            Log.w(TAG, "ActivityManager is null, cannot determine foreground state.");
            return false;
        }
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            Log.w(TAG, "RunningAppProcessInfo is null, cannot determine foreground state.");
            return false;
        }
        final String packageName = ctx.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private Response cameraSet(IHTTPSession session, Map<String, List<String>> params) {
        try {
            String type = first(params, "type", null);
            if (type == null) {
                return jsonErr(Response.Status.BAD_REQUEST, "Missing 'type' parameter for camera selection.");
            }

            // Convert request parameter to camera position
            int pos = cameraStringToPos(type);
            if (pos < 0) {
                return jsonErr(Response.Status.BAD_REQUEST, "Unknown camera type specified: " + type);
            }

            // Map UI position to service type
            String serviceType = mapCameraPosToServiceType(pos);
            Log.d(TAG, "Switching to service: " + serviceType);

            // Use ServiceManager to switch services via DI
            serviceManager.switchServiceDI(serviceType);

            // Update preferences
            String newCameraPrefKey = posToPrefKey(pos);
            AppPreference.setStr(AppPreference.KEY.SELECTED_POSITION, newCameraPrefKey);
            Log.i(TAG, "Camera service switched to: " + serviceType + " (ID: " + newCameraPrefKey + ")");

            // Reset streaming/recording states
            AppPreference.setBool(AppPreference.KEY.STREAM_STARTED, false);
            AppPreference.setBool(AppPreference.KEY.RECORDING_STARTED, false);

            // Update UI if app is in foreground
            if (isAppInForeground()) {
                MainActivity mainActivityInstance = MainActivity.getInstance();
                if (mainActivityInstance != null && !mainActivityInstance.isFinishing()) {
                    uiHandler.post(() -> {
                        try {
                            LiveFragment liveFragmentInstance = LiveFragment.getInstance();
                            if (liveFragmentInstance != null && liveFragmentInstance.isAdded()) {
                                Log.d(TAG, "Updating LiveFragment spinner to position: " + pos);
                            }

                        } catch (Exception e) {
                            Log.e(TAG, "UI update failed: " + e.getMessage(), e);
                        }
                    });
                }
            }

            return jsonOk(Map.of(
                    "status", "Camera service switched to " + serviceType,
                    "message", "Service changed. Streaming/recording stopped."
            ));
        } catch (Exception e) {
            Log.e(TAG, "Camera switch failed: " + e.getMessage(), e);
            return jsonErr(Response.Status.INTERNAL_ERROR, "Camera switch failed: " + e.getMessage());
        }
    }

    // Helper to map UI positions to service types
    private String mapCameraPosToServiceType(int pos) {
        switch (pos) {
            case 0: // Rear Camera
            case 1: // Front Camera
                return "camera";
            case 2: // USB Camera
                return "usb";
            case 3: // Screen Cast
                return "cast";
            case 4: // Audio Only
                return "audio";
            default:
                throw new IllegalArgumentException("Invalid camera position: " + pos);
        }
    }

    private Response audioSet(IHTTPSession session, Map<String, List<String>> params) {
        String enableStr = first(params, "enable", null);
        if (enableStr == null) {
            return jsonErr(Response.Status.BAD_REQUEST, "Missing 'enable' parameter (true/false).");
        }
        boolean enable = Boolean.parseBoolean(enableStr);
        AppPreference.setBool(AppPreference.KEY.RECORD_AUDIO, enable);

        try {
            serviceManager.switchAudioDI(enable);
            Log.i(TAG, "Audio switched to: " + enable);
            return jsonOk(Map.of(
                    "status", enable ? "Audio enabled" : "Audio disabled",
                    "message", "Audio state updated"
            ));
        } catch (IllegalStateException e) {
            Log.e(TAG, "Audio switch failed: " + e.getMessage());
            return jsonErr(Response.Status.INTERNAL_ERROR, "Audio operation failed: " + e.getMessage());
        }
    }

    private Response streamingSet(IHTTPSession session, Map<String, List<String>> params) {
        String action = first(params, "action", "").toLowerCase(Locale.US);
        boolean wantStart = ACTION_START.equals(action);
        boolean wantStop = ACTION_STOP.equals(action);

        if (!wantStart && !wantStop) {
            return jsonErr(Response.Status.BAD_REQUEST, "Invalid 'action'. Must be '" + ACTION_START + "' or '" + ACTION_STOP + "'.");
        }

        try {
            if (wantStart) {
                serviceManager.startStreamDI();
                AppPreference.setBool(AppPreference.KEY.STREAM_STARTED, true);
                Log.i(TAG, "Streaming started via ServiceManager");
                return jsonOk(Map.of("status", "Streaming started"));
            } else {
                serviceManager.stopStreamDI();
                AppPreference.setBool(AppPreference.KEY.STREAM_STARTED, false);
                Log.i(TAG, "Streaming stopped via ServiceManager");
                return jsonOk(Map.of("status", "Streaming stopped"));
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Streaming operation failed: " + e.getMessage());
            return jsonErr(Response.Status.INTERNAL_ERROR, "Streaming operation failed: " + e.getMessage());
        }
    }

    private Response recordingSet(IHTTPSession session, Map<String, List<String>> params) {
        String action = first(params, "action", "").toLowerCase(Locale.US);
        boolean wantStart = ACTION_START.equals(action);
        boolean wantStop = ACTION_STOP.equals(action);

        if (!wantStart && !wantStop) {
            return jsonErr(Response.Status.BAD_REQUEST, "Invalid 'action'. Must be '" + ACTION_START + "' or '" + ACTION_STOP + "'.");
        }

        try {
            if (wantStart) {
                serviceManager.startRecordDI();
                AppPreference.setBool(AppPreference.KEY.RECORDING_STARTED, true);
                Log.i(TAG, "Recording started via ServiceManager");
                return jsonOk(Map.of("status", "Recording started"));
            } else {
                serviceManager.stopRecordDI();
                AppPreference.setBool(AppPreference.KEY.RECORDING_STARTED, false);
                Log.i(TAG, "Recording stopped via ServiceManager");
                return jsonOk(Map.of("status", "Recording stopped"));
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Recording operation failed: " + e.getMessage());
            return jsonErr(Response.Status.INTERNAL_ERROR, "Recording operation failed: " + e.getMessage());
        }
    }

    private Response playbackList(IHTTPSession session, Map<String, List<String>> params) {
        if (recordingsDir == null || !recordingsDir.exists() || !recordingsDir.isDirectory()) {
            Log.w(TAG, "PlaybackList: Recordings directory not available: " + (recordingsDir == null ? "null" : recordingsDir.getAbsolutePath()));
            return jsonOk(Map.of(
                    "files", Collections.emptyList(),
                    "message", "Recordings directory not available or not configured."
            ));
        }
        File[] filesArray = recordingsDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".mp4"));
        List<Map<String, Object>> fileList = new java.util.ArrayList<>();
        if (filesArray != null) {
            java.util.Arrays.sort(filesArray, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            for (File file : filesArray) {
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("filename", file.getName());
                fileInfo.put("size_bytes", file.length());
                fileInfo.put("size_mb", String.format(Locale.US, "%.2f", file.length() / (1024.0 * 1024.0)));
                fileInfo.put("last_modified_timestamp", file.lastModified());
                fileInfo.put("last_modified_date", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US).format(new java.util.Date(file.lastModified())));
                fileList.add(fileInfo);
            }
        }
        return jsonOk(Map.of(
                "files", fileList,
                "total_files", fileList.size(),
                "directory", recordingsDir.getAbsolutePath()
        ));
    }

    private Response playbackDownload(IHTTPSession session, Map<String, List<String>> params) {
        String filename = first(params, "filename", null);
        if (filename == null || filename.trim().isEmpty()) {
            return jsonErr(Response.Status.BAD_REQUEST, "Missing 'filename' parameter.");
        }
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            Log.w(TAG, "PlaybackDownload: Invalid characters in filename: " + filename);
            return jsonErr(Response.Status.BAD_REQUEST, "Invalid filename format.");
        }
        if (recordingsDir == null) {
            Log.e(TAG, "PlaybackDownload: Recordings directory is not initialized.");
            return jsonErr(Response.Status.INTERNAL_ERROR, "Recordings storage not available.");
        }
        try {
            File f = secureRecording(filename);
            return streamRange(session, f);
        } catch (FileNotFoundException fnf) {
            Log.w(TAG, "PlaybackDownload: File not found or not accessible: " + filename, fnf);
            return jsonErr(Response.Status.NOT_FOUND, fnf.getMessage());
        } catch (SecurityException sec) {
            Log.w(TAG, "PlaybackDownload: Security exception for file: " + filename, sec);
            return jsonErr(Response.Status.FORBIDDEN, sec.getMessage());
        } catch (IOException ex) {
            Log.e(TAG, "PlaybackDownload: IO failure for file: " + filename, ex);
            return jsonErr(Response.Status.INTERNAL_ERROR, "Could not stream file due to IO error.");
        } catch (Exception ex) {
            Log.e(TAG, "PlaybackDownload: Unexpected failure for file: " + filename, ex);
            return jsonErr(Response.Status.INTERNAL_ERROR, "Could not stream file due to an unexpected error.");
        }
    }

    private Response gpsSet(IHTTPSession session, Map<String, List<String>> params) {
        String action = first(params, "action", null);
        String intervalStr = first(params, "interval", null);

        if (action == null && intervalStr == null) {
            return jsonErr(Response.Status.BAD_REQUEST, "Missing parameters. Provide 'action' ("+ACTION_START+"/"+ACTION_STOP+") or 'interval' (minutes).");
        }
        if (action != null) {
            boolean enable;
            if (ACTION_START.equalsIgnoreCase(action)) enable = true;
            else if (ACTION_STOP.equalsIgnoreCase(action)) enable = false;
            else return jsonErr(Response.Status.BAD_REQUEST, "Invalid 'action'. Must be '"+ACTION_START+"' or '"+ACTION_STOP+"'.");

            AppPreference.setBool(AppPreference.KEY.GPS_ENABLED, enable);
            Log.i(TAG, "GPS tracking preference set to: " + enable);
            return jsonOk(Map.of("status", "GPS tracking " + (enable ? "enabled" : "disabled") + " in preferences. Service notified."));
        }
        if (intervalStr != null) {
            try {
                int intervalMinutes = Integer.parseInt(intervalStr);
                if (intervalMinutes <= 0) return jsonErr(Response.Status.BAD_REQUEST, "Invalid 'interval'. Must be a positive integer > 0.");
                AppPreference.setInt(AppPreference.KEY.FREQUENCY_MIN, intervalMinutes);
                Log.i(TAG, "GPS update interval preference set to: " + intervalMinutes + " minutes.");
                return jsonOk(Map.of("status", "GPS update interval preference set to " + intervalMinutes + " minutes. Service notified."));
            } catch (NumberFormatException e) {
                return jsonErr(Response.Status.BAD_REQUEST, "Invalid 'interval' format. Must be an integer.");
            }
        }
        return jsonErr(Response.Status.BAD_REQUEST, "No valid GPS parameters processed.");
    }

    private Response gpsStatus() {
        boolean enabled = AppPreference.getBool(AppPreference.KEY.GPS_ENABLED, false);
        String lat = String.format(Locale.US, "%.6f", LocationManagerService.lat);
        String lng = String.format(Locale.US, "%.6f", LocationManagerService.lng);
        String intervalDesc = getGpsIntervalString();
        int intervalMinutes = AppPreference.getInt(AppPreference.KEY.FREQUENCY_MIN, 1);

        return jsonOk(Map.of(
                "is_gps_enabled", enabled,
                "latitude", enabled ? lat : "N/A",
                "longitude", enabled ? lng : "N/A",
                "update_interval_minutes_preference", intervalMinutes,
                "update_interval_description", intervalDesc
        ));
    }

    private Response appUpdate(IHTTPSession session, Map<String, List<String>> params) {
        String url = first(params, "url", null);
        if (url == null || url.trim().isEmpty()) {
            return jsonErr(Response.Status.BAD_REQUEST, "Missing 'url' parameter for app update.");
        }
        String lowerUrl = url.trim().toLowerCase(Locale.ROOT);
        if (!lowerUrl.startsWith("http://") && !lowerUrl.startsWith("https://")) {
            Log.w(TAG, "AppUpdate: Invalid URL scheme: " + url);
            return jsonErr(Response.Status.BAD_REQUEST, "Invalid 'url' scheme. Must be HTTP or HTTPS.");
        }

        Log.i(TAG, "App update triggered via API with URL: " + url);
        Log.e(TAG, "SECURITY WARNING: RootCommandExecutor invoked for app update. Ensure extreme caution and validation.");
        return jsonOk(Map.of(
                "status", "App update process initiated with URL: " + url,
                "message", "Further status depends on the execution of the update mechanism."
        ));
    }

    public String getUtcDateTimeString() {
        long currentMillis = System.currentTimeMillis();
        Instant instant = Instant.ofEpochMilli(currentMillis);
        ZonedDateTime utcDateTime = instant.atZone(ZoneId.of("UTC"));
        return utcDateTime.format(DateTimeFormatter.ISO_INSTANT);
    }

    private Response jsonOk(Object data) {
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("success", true);
        envelope.put("timestamp", getUtcDateTimeString());
        envelope.put("data", data);
        return newFixedLengthResponse(Response.Status.OK, MIME_JSON, gson.toJson(envelope));
    }

    private Response jsonErr(Response.Status status, String errorMsg) {
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("success", false);
        envelope.put("timestamp", getUtcDateTimeString());
        envelope.put("error", Map.of(
                "code", status.getRequestStatus(),
                "message", errorMsg
        ));
        return newFixedLengthResponse(status, MIME_JSON, gson.toJson(envelope));
    }

    private Response cors(Response r) {
        r.addHeader("Access-Control-Allow-Origin", "*");
        r.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        r.addHeader("Access-Control-Allow-Headers", "Content-Type, Range, X-API-Key, Authorization");
        r.addHeader("Access-Control-Max-Age", "86400");
        return r;
    }

    private String first(Map<String, List<String>> params, String key, String def) {
        if (params == null) return def;
        List<String> v = params.get(key);
        return (v == null || v.isEmpty() || v.get(0) == null) ? def : v.get(0);
    }

    private int parseInt(String value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String getGpsIntervalString() {
        int freqMinutes = AppPreference.getInt(AppPreference.KEY.FREQUENCY_MIN, 1);
        if (freqMinutes <= 0) freqMinutes = 1;
        if (freqMinutes == 1) return "1 minute";
        return freqMinutes + " minutes";
    }

    private static int cameraStringToPos(String cameraIdentifier) {
        if (cameraIdentifier == null) return -1;
        String lowerId = cameraIdentifier.toLowerCase(Locale.US).trim();
        switch (lowerId) {
            case "0": case "rear": case "rearcamera": case "back": case "backcamera": return 0;
            case "1": case "front": case "frontcamera": return 1;
            case "2": case "usb": case "usbcamera": return 2;
            case "3": case "cast": case "screencast": case "screen": return 3;
            case "4": case "audio": case "audioonly": case "mic": case "microphone": return 4;
            default:
                try {
                    int id = Integer.parseInt(cameraIdentifier);
                    if (id >= 0 && id <= 4) return id;
                } catch (NumberFormatException e) {}
                return -1;
        }
    }

    private static String posToPrefKey(int pos) {
        return String.valueOf(pos);
    }

    private static String humanReadableCamera(String prefKey) {
        if (prefKey == null) return "Unknown";
        switch (prefKey) {
            case "0": return "Rear Camera";
            case "1": return "Front Camera";
            case "2": return "USB Camera";
            case "3": return "Screen Cast";
            case "4": return "Audio Only";
            default: return "Unknown (ID: " + prefKey + ")";
        }
    }

    private boolean usbCameraAttached() {
        UsbManager usbManager = (UsbManager) ctx.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            Log.w(TAG, "UsbManager not available to check for USB camera.");
            return false;
        }
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        if (deviceList.isEmpty()) return false;
        for (UsbDevice device : deviceList.values()) {
            if (device.getDeviceClass() == UsbConstants.USB_CLASS_VIDEO) return true;
            for (int i = 0; i < device.getInterfaceCount(); i++) {
                if (device.getInterface(i).getInterfaceClass() == UsbConstants.USB_CLASS_VIDEO) {
                    return true;
                }
            }
        }
        return false;
    }

    private Response streamRange(IHTTPSession session, File file) throws IOException {
        long fileSize = file.length();
        String rangeHeader = session.getHeaders().get("range");
        long[] range = parseRange(rangeHeader, fileSize);

        RandomAccessFile raf = null;
        FileChannel channel = null;
        ChannelRangeInputStream cis = null;

        try {
            raf = new RandomAccessFile(file, "r");
            channel = raf.getChannel();
            cis = new ChannelRangeInputStream(channel, raf, range[0], range[2]);

            Response.Status status = (range[2] == fileSize) ? Response.Status.OK : Response.Status.PARTIAL_CONTENT;
            Response response = newFixedLengthResponse(status, MIME_MP4, cis, range[2]);
            response.addHeader("Accept-Ranges", "bytes");
            response.addHeader("Content-Length", String.valueOf(range[2]));
            if (status == Response.Status.PARTIAL_CONTENT) {
                response.addHeader("Content-Range", String.format(Locale.US, "bytes %d-%d/%d", range[0], range[1], fileSize));
            }
            response.addHeader("Content-Disposition", "inline; filename=\"" + file.getName() + "\"");
            return response;
        } catch (IOException e) {
            if (cis != null) { try { cis.close(); } catch (IOException ignored) {} }
            else if (channel != null && channel.isOpen()) { try { channel.close(); } catch (IOException ignored) {} }
            else if (raf != null) { try { raf.close(); } catch (IOException ignored) {} }
            throw e;
        }
    }

    private long[] parseRange(String rangeHeader, long fileSize) {
        long start = 0;
        long end = fileSize - 1;
        if (rangeHeader != null && rangeHeader.toLowerCase(Locale.ROOT).startsWith("bytes=") && fileSize > 0) {
            try {
                String rangeValue = rangeHeader.substring(6);
                String[] parts = rangeValue.split("-");
                if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                    start = Long.parseLong(parts[0].trim());
                }
                if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                    end = Long.parseLong(parts[1].trim());
                }
                if (start < 0) start = 0;
                if (end >= fileSize) end = fileSize - 1;
                if (start > end) {
                    Log.w(TAG, "Invalid byte range requested: " + rangeHeader + ", fileSize: " + fileSize + ". Resetting to full file.");
                    start = 0; end = fileSize - 1;
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, "Malformed Range header: " + rangeHeader + ". Serving full file.", e);
                start = 0; end = fileSize - 1;
            }
        }
        long length = (end - start) + 1;
        if (length < 0) length = 0;
        return new long[]{start, end, length};
    }

    private File secureRecording(String filename) throws IOException, SecurityException {
        if (recordingsDir == null) {
            throw new FileNotFoundException("Recordings directory not configured or accessible.");
        }
        if (filename == null || filename.trim().isEmpty()){
            throw new IllegalArgumentException("Filename cannot be null or empty.");
        }
        if (filename.contains("/") || filename.contains("\\")) {
            throw new SecurityException("Invalid filename: contains path separators.");
        }
        File file = new File(recordingsDir, filename);
        String canonicalDirPath = recordingsDir.getCanonicalPath();
        String canonicalFilePath = file.getCanonicalPath();
        if (!canonicalFilePath.startsWith(canonicalDirPath)) {
            throw new SecurityException("Invalid file path (Path Traversal Attempt). Access denied to: " + filename);
        }
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + filename);
        }
        if (!file.isFile()){
            throw new FileNotFoundException("Requested path is not a file: " + filename);
        }
        if (!file.canRead()){
            throw new SecurityException("File cannot be read: " + filename);
        }
        return file;
    }

    private static class ChannelRangeInputStream extends InputStream {
        private final FileChannel channel;
        private final RandomAccessFile raf;
        private long position;
        private long remaining;

        ChannelRangeInputStream(FileChannel channel, RandomAccessFile raf, long start, long length) {
            this.channel = channel;
            this.raf = raf;
            this.position = start;
            this.remaining = length;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) return -1;
            ByteBuffer singleByteBuffer = ByteBuffer.allocate(1);
            int bytesRead = channel.read(singleByteBuffer, position);
            if (bytesRead <= 0) {
                remaining = 0;
                return -1;
            }
            position += bytesRead;
            remaining -= bytesRead;
            singleByteBuffer.flip();
            return singleByteBuffer.get() & 0xFF;
        }

        @Override
        public int read(@NonNull byte[] buffer, int offset, int length) throws IOException {
            if (buffer == null) throw new NullPointerException("Buffer cannot be null.");
            if (offset < 0 || length < 0 || length > buffer.length - offset) throw new IndexOutOfBoundsException();
            if (length == 0) return 0;
            if (remaining <= 0) return -1;
            int bytesToRead = (int) Math.min(length, remaining);
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, offset, bytesToRead);
            int bytesRead = channel.read(byteBuffer, position);
            if (bytesRead > 0) {
                position += bytesRead;
                remaining -= bytesRead;
            }
            if (bytesRead == -1) remaining = 0;
            return bytesRead;
        }

        @Override
        public long skip(long n) throws IOException {
            if (n <= 0) return 0;
            long bytesToSkip = Math.min(n, remaining);
            position += bytesToSkip;
            remaining -= bytesToSkip;
            return bytesToSkip;
        }

        @Override
        public int available() throws IOException {
            return (int) Math.min(remaining, Integer.MAX_VALUE);
        }

        @Override
        public void close() throws IOException {
            IOException firstEx = null;
            try {
                if (channel != null && channel.isOpen()) {
                    channel.close();
                }
            } catch (IOException e) {
                firstEx = e;
            }
            try {
                if (raf != null) {
                    raf.close();
                }
            } catch (IOException e) {
                if (firstEx == null) firstEx = e;
                else firstEx.addSuppressed(e);
            }
            if (firstEx != null) throw firstEx;
        }
    }

    public void startServer() {
        boolean daemon = false;
        this.isRunningAsDaemon = daemon;
        try {
            start(SOCKET_TIMEOUT_MS, daemon);
            Log.i(TAG, "HTTP server started on port " + getListeningPort() + ". IsDaemon: " + this.isRunningAsDaemon);
        } catch (IOException e) {
            Log.e(TAG, "Unable to start HTTP server", e);
        }
    }

    public void stopServer() {
        super.stop();
        Log.i(TAG, "HTTP server stopped");
    }
}