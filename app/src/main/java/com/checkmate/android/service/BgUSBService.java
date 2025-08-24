package com.checkmate.android.service;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;
import com.checkmate.android.AppPreference;
import com.checkmate.android.R;
import com.checkmate.android.model.SurfaceModel;
import com.checkmate.android.service.SharedEGL.ServiceType;
import com.checkmate.android.service.SharedEGL.SharedEglManager;
import com.checkmate.android.ui.fragment.LiveFragment;
import com.checkmate.android.ui.fragment.SettingsFragment;
import com.checkmate.android.util.MainActivity;
import com.checkmate.android.util.SettingsUtils;
import com.checkmate.android.viewmodels.SharedViewModel;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.usb.USBMonitor;
import com.wmspanel.libstream.Streamer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;

public class BgUSBService extends BaseBackgroundService {

    private SurfaceTexture mPreviewTexture;
    private Surface mPreviewSurface;
    private SurfaceTexture dsurfaceTexture;
    private int dwidth;
    private int dheight;
    private static final String TAG = "BgUSBService";
    public static boolean isRunning = false;
    String channelId = "BgUSBService";
    // Handlers
    private HandlerThread mServiceHandlerThread;
    private final Handler mServiceHandler = new Handler(Looper.getMainLooper());
    // Binder
    private WeakReference<CameraBinder> mBinderRef = new WeakReference<>(new CameraBinder(this));
    private USBMonitor mUSBMonitor;
    private UVCCameraHandler mUVCCameraHandler;
    // UI Elements
    private int mScreenWidth = 640;
    private int mScreenHeight = 480;
    public static int mPreviewWidth = 640;
    public static int mPreviewHeight = 480;
    private boolean mClosing = false;
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private SharedViewModel sharedViewModel;
    List<String> codec_sources;

    int usbCodec = AppPreference.getInt(AppPreference.KEY.CODEC_SRC, 0);
    public boolean isAlertShow = false;
    List<String> resolutionList = new ArrayList<>();

    public boolean isRequest = false;    // tracking permission requests
    public boolean isConnected = false;  // did we open the USB camera
    public UsbDevice activeCameraDevice;
    private Set<UsbDevice> mCameraDeviceList = new LinkedHashSet<>();
    public boolean isServiceStart = true;
    boolean isUVCameraDisconnect = false;
    private volatile boolean isShuttingDown = false;
    private boolean isInitEGL = false;

    private static final String EXTRA_RESTART_CAMERA = "restart_camera";
    private static final int MAX_RESTART_ATTEMPTS = 3;
    private int mRestartCount = 0;
    private long mLastRestartTime = 0;
    private boolean isRestartDueToEGLFailure = false;
    private volatile boolean isRestarting = false;
    public SharedEglManager mEglManager;

    @Inject
    public BgUSBService() {
        // Constructor logic
    }

    @Override
    protected ServiceType getServiceType() {
        return ServiceType.BgUSBCamera;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        // Initialize Notification
        mServiceHandlerThread = new HandlerThread("BgUSBService");
        mServiceHandlerThread.start();
        initUSBMonitor();

        // Check for restart flags
        isRestartDueToEGLFailure = AppPreference.getBool(AppPreference.KEY.EGL_RESTART_FLAG, false);
        if (isRestartDueToEGLFailure) {
            mRestartCount = AppPreference.getInt(AppPreference.KEY.RESTART_COUNT, 0);
            mLastRestartTime = AppPreference.getLong(AppPreference.KEY.LAST_RESTART_TIME, 0);
        }

        // Get the singleton instance
        mEglManager = SharedEglManager.getInstance();
    }


    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        startForeground(NOTIFICATION_ID,buildNotification());
        isRunning = true;
        codec_sources = Arrays.asList(getResources().getStringArray(R.array.usb_codec));
        mRunningIntent = intent;
        // Handle restart intents
        if (intent != null && intent.getBooleanExtra(EXTRA_RESTART_CAMERA, false)) {
            isRestartDueToEGLFailure = true;
            Log.w(TAG, "Restarting due to EGL failure");
        }
        setStatus(BackgroundNotification.NOTIFICATION_STATUS.SERVICE_STARTED);
        return START_STICKY;
    }

    public void setSharedViewModel(SharedViewModel vm) {
        this.sharedViewModel = vm;
    }

    public void stopSafe() {
        if (mNotifyCallback != null) {
            mNotifyCallback.stopService(ServiceType.BgUSBCamera);
        } else {
            BgUSBService.this.stopSelf();
        }
    }

    void loadUSBCameraSettings() {
        Log.e(TAG, "Setting Selected Resolution1: " + mScreenWidth + "x" + mScreenHeight);
        if (MainActivity.instance != null) {
            if (!MainActivity.instance.getUSBCameraResolutions().isEmpty()) {
                int resolution = AppPreference.getInt(AppPreference.KEY.USB_RESOLUTION, 1);
                if (resolution < MainActivity.instance.getUSBCameraResolutions().size()) {
                    String size = MainActivity.instance.getUSBCameraResolutions().get(resolution);
                    mPreviewWidth = Integer.parseInt(size.split("x")[0]);
                    mPreviewHeight = Integer.parseInt(size.split("x")[1]);
                    int usbMinFps = AppPreference.getInt(AppPreference.KEY.USB_MIN_FPS, 30);
                    int usbMaxFps = AppPreference.getInt(AppPreference.KEY.USB_MAX_FPS, 30);
                    Streamer.FpsRange[] mFpsRanges = new Streamer.FpsRange[1];
                    mFpsRanges[0] = new Streamer.FpsRange(usbMinFps, usbMaxFps);
                    mFpsRanges[0].fpsMax = usbMaxFps;
                    mFpsRanges[0].fpsMin = usbMinFps;

                }
            }
        }

        // Parameters
        int usb_sampleRate = getAudioSampleRate();
        int usb_channelCount = AppPreference.getInt(AppPreference.KEY.CHANNEL_COUNT, 0);
        if (usb_channelCount == 0) {
            usb_channelCount = AudioFormat.CHANNEL_IN_MONO;
        } else {
            usb_channelCount = AudioFormat.CHANNEL_IN_STEREO;
        }
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT; // or PCM_8BIT if needed
        int bufferSize = AudioRecord.getMinBufferSize(usb_sampleRate, usb_channelCount, audioFormat);
        if (bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Toast.makeText(MainActivity.instance, "Invalid audio record parameters.", Toast.LENGTH_LONG).show();
        }

    }

    // BgUSBService.java
    private void setupEglInject() {
        if (mEglManager != null) {
            mEglManager.setListener(new SharedEglManager.Listener() {
                @Override public void onEglReady() {
                    // 🔴 Critical null checks with restart mechanism
                    if (mEglManager == null) {
                        Log.e(TAG, "EGL Manager is null in setup");
                        scheduleServiceRestart("EGL Manager null in setup");
                        return;
                    }

                    if (mEglManager.getCameraTexture() == null) {
                        Log.e(TAG, "Camera texture is null in onEglReady");
                        scheduleServiceRestart("Camera texture null");
                        return;
                    }

                    mPreviewTexture = mEglManager.getCameraTexture();
                    mPreviewSurface = new Surface(mPreviewTexture);

                    if (mPreviewSurface == null) {
                        Log.e(TAG, "Failed to create preview surface");
                        scheduleServiceRestart("Preview surface null");
                        return;
                    }

                    mPreviewTexture.setDefaultBufferSize(mPreviewWidth, mPreviewHeight);
                    if (mEglManager != null && mEglManager.getHandler() != null) {
                        Handler eglHandler = mEglManager.getHandler();
                        mPreviewTexture.setOnFrameAvailableListener(surfaceTexture ->
                                eglHandler.post(this::drawFrame));
                    }

                    // Start preview if possible
                    if (mUVCCameraHandler != null && !mUVCCameraHandler.isPreviewing()) {
                        mUVCCameraHandler.startPreview(mPreviewSurface);
                    }

                    if (mEglManager != null) {
                        SurfaceModel surfaceModel = mEglManager.sharedViewModel.getSurfaceModel();
                        dsurfaceTexture = surfaceModel.getSurfaceTexture();
                        if (dsurfaceTexture == null) {
                            Log.w(TAG, "SurfaceTexture not created yet");
                            scheduleServiceRestart("SurfaceTexture null");
                            return;
                        }
                        dwidth = surfaceModel.getWidth();
                        dheight = surfaceModel.getHeight();
                        mEglManager.setPreviewSurface(dsurfaceTexture,dwidth,dheight);
                    }
                }

                private void drawFrame() {
                    // 🔴 Critical null check for texture
                    if (mPreviewTexture == null) {
                        Log.e(TAG, "Preview texture null in drawFrame");
                        scheduleServiceRestart("Preview texture null in draw");
                        return;
                    }

                    try {
                        mPreviewTexture.updateTexImage();
                    } catch (Throwable t) {
                        Log.e(TAG, "updateTexImage failed", t);
                        scheduleServiceRestart("Texture update failed");
                        return;
                    }

                    float[] tx = new float[16];
                    mPreviewTexture.getTransformMatrix(tx);

                    if (mEglManager != null) {
                        mEglManager.drawFrame();
                    }
                }
            });

            if (!isInitEGL) {
                isInitEGL = true;
                // Get the singleton instance
                mEglManager = SharedEglManager.getInstance();
                mEglManager.initialize(getApplicationContext(), ServiceType.BgUSBCamera);
            } else {
                if (mEglManager != null) {
                    LiveFragment fragment = LiveFragment.getInstance();
                    if (fragment != null) {
                        fragment.clearPreview();
                    }
                    mServiceHandler.postDelayed(() -> {
                        if (mEglManager != null) {
                            mEglManager.notifyEglReady();
                        }
                    }, 500);
                }
            }
        } else {
            Log.e(TAG, "EGL Manager is null in setup");
            scheduleServiceRestart("EGL Manager null in setup");
        }
    }
    private void scheduleServiceRestart(String reason) {
        if (isShuttingDown || isRestarting) {
            Log.w(TAG, "Restart already in progress. Ignoring: " + reason);
            return;
        }

        // Prevent restart loops
        long currentTime = SystemClock.elapsedRealtime();
        if (currentTime - mLastRestartTime < 5000) { // 5 second cooldown
            mRestartCount++;
        } else {
            mRestartCount = 1;
        }

        if (mRestartCount > MAX_RESTART_ATTEMPTS) {
            Log.e(TAG, "Max restart attempts reached. Giving up.");
            stopSafe();
            return;
        }

        Log.w(TAG, "Scheduling service restart due to: " + reason);
        isRestarting = true;
        mLastRestartTime = currentTime;

        // Save state for restart
        AppPreference.setBool(AppPreference.KEY.EGL_RESTART_FLAG, true);
        AppPreference.setInt(AppPreference.KEY.RESTART_COUNT, mRestartCount);
        AppPreference.setLong(AppPreference.KEY.LAST_RESTART_TIME, mLastRestartTime);
        if (activeCameraDevice != null) {
            AppPreference.setStr(AppPreference.KEY.USB_CAMERA_NAME, activeCameraDevice.getDeviceName());
        }

        mServiceHandler.postDelayed(() -> {
            SharedEglManager.cleanAndResetAsync(() -> {
                mEglManager = SharedEglManager.getInstance();
                mEglManager.initialize(getApplicationContext(), ServiceType.BgUSBCamera);
            });
        }, 1000); // Delay to allow cleanup
    }

    public int getAudioBitRate() {
        // Retrieve the USB_AUDIO_BITRATE value from preferences
        int bitRatePref = AppPreference.getInt(AppPreference.KEY.USB_AUDIO_BITRATE, 0);

        // Map the preference value to the corresponding bit depth
        switch (bitRatePref) {
            case 0:
                return 16;  // If USB_AUDIO_BITRATE is 0, return 16-bit
            case 1:
                return 24;  // If USB_AUDIO_BITRATE is 1, return 24-bit
            case 2:
                return 32;  // If USB_AUDIO_BITRATE is 2, return 32-bit
            case 3:
                return 64;  // If USB_AUDIO_BITRATE is 3, return 64-bit
            case 4:
                return 128; // If USB_AUDIO_BITRATE is 4, return 128-bit
            case 5:
                return 256; // If USB_AUDIO_BITRATE is 5, return 256-bit
            case 6:
                return 512; // If USB_AUDIO_BITRATE is 6, return 512-bit
            case 7:
                return 1024; // If USB_AUDIO_BITRATE is 7, return 1024-bit
            default:
                return 16;  // Default to 16-bit if the value is out of range
        }
    }

    public int getAudioSampleRate() {
        // Retrieve the sample rate value from preferences
        int sampleRatePref = AppPreference.getInt(AppPreference.KEY.USB_SAMPLE_RATE, 0);

        // Map the preference value to the corresponding sample rate
        switch (sampleRatePref) {
            case 0:
                return 8000;   // 8000 Hz
            case 1:
                return 11025;  // 11025 Hz
            case 2:
                return 12000;  // 12000 Hz
            case 3:
                return 16000;  // 16000 Hz
            case 4:
                return 22050;  // 22050 Hz
            case 5:
                return 24000;  // 24000 Hz
            case 6:
                return 32000;  // 32000 Hz
            case 7:
                return 44100;  // 44100 Hz
            case 8:
                return 48000;  // 48000 Hz
            default:
                return 44100;  // Default to 44100 Hz if the value is out of range
        }
    }
    public void setPreviewSurface(final SurfaceTexture surfaceTexture, int width, int height) {
        dsurfaceTexture = surfaceTexture;
        dwidth = width;
        dheight = height;
    }

    public Handler getHandler() {
        return mServiceHandler;
    }
    /**
     * Defines the service binder.
     */
    public static class CameraBinder extends Binder {
        private final WeakReference<BgUSBService> serviceReference;

        public CameraBinder(BgUSBService service) {
            serviceReference = new WeakReference<>(service);
        }

        public BgUSBService getService() {
            return serviceReference.get();
        }
    }

    //region ====== USB Monitor + UVCCameraHandler Setup ======
    private void initUSBMonitor() {
        mUSBMonitor = new USBMonitor(getApplicationContext(), mOnDeviceConnectListener);
        mUSBMonitor.register(); // scanning for USB devices
    }

    private boolean isCameraDevice(UsbDevice device) {
        if (device == null) return false;
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface usbInterface = device.getInterface(i);
            if (usbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_VIDEO && usbInterface.getInterfaceSubclass() == 0x02)  {
                return true;
            }
        }
        return false;
    }
    public void showCameraSelectionDialog() {
        if (MainActivity.getInstance() == null || mCameraDeviceList.isEmpty()) {
            mServiceHandler.postDelayed(this::showCameraSelectionDialog, 1000);
            return;  // no activity or no devices
        }
        if (isAlertShow) {
            return;
        }
        boolean isUSBOpen = AppPreference.getBool(AppPreference.KEY.IS_USB_OPENED,false);
        if (!isUSBOpen) {
            return;
        }
        isAlertShow = true;

        MainActivity.instance.runOnUiThread(() -> {
            // Build a list of device names
            if (mCameraDeviceList == null || mCameraDeviceList.isEmpty()) {
                // Handle the empty list case
                return;
            }
            List<UsbDevice> deviceList = new ArrayList<>(mCameraDeviceList);
            final String[] deviceNames = new String[deviceList.size()];
            for (int i = 0; i < deviceList.size(); i++) {
                int vendorId = deviceList.get(i).getVendorId();
                String productName = deviceList.get(i).getProductName();
                String commonName = "USB Camera: " + i + 1;
                // Optionally, add more info:
                commonName += " Product Id: " + productName + " (" + vendorId + ")"; //+ " (Path:" + deviceName + ")";
                deviceNames[i] = commonName;
            }

            checkCamStatus(deviceNames);
        });
    }
    boolean isNavFromStream = false;
    void checkCamStatus(String[] deviceNames) {
        if (MainActivity.instance != null) {
            boolean isRestart = AppPreference.getBool(AppPreference.KEY.IS_RESTART_APP , false);
            boolean isUSBCam = AppPreference.getBool(AppPreference.KEY.IS_USB_OPENED, false);
            boolean checkModePin = AppPreference.getBool(AppPreference.KEY.CHESS_MODE_PIN, false);
            boolean covertMode = AppPreference.getBool(AppPreference.KEY.UI_CONVERT_MODE, false);
            boolean isStreaming = AppPreference.getBool(AppPreference.KEY.STREAM_STARTED, false);
            List<UsbDevice> deviceList = new ArrayList<>(mCameraDeviceList);
            if (isUSBCam) {
                if (isRestart) {
                    AppPreference.setBool(AppPreference.KEY.IS_RESTART_APP, false);
                    int index = 0;
                    if (activeCameraDevice != null) {
                        index = deviceList.indexOf(activeCameraDevice);
                        if (index < 0 ){
                            index = 0;
                        }
                    }
                    selectedPositionForCameraList(index);
                    Log.e(TAG, "showCameraSelectionDialog: 1");
                }else if (isServiceStart) {
                    isServiceStart = false;
                    AppPreference.setInt(AppPreference.KEY.USB_RESOLUTION, 1);
                    MainActivity.instance.showCamerasList(deviceNames);
                    Log.e(TAG, "showCameraSelectionDialog: 4");
                }else if (covertMode && checkModePin) {
                    if (isStreaming) {
                        int index = 0;
                        if (activeCameraDevice != null) {
                            index = deviceList.indexOf(activeCameraDevice);
                            if (index < 0) {
                                index = 0;
                            }
                        }
                        isNavFromStream = true;
                        isRequest = false;
                        selectedPositionForCameraList(index);
                    }else {
                        int index = 0;
                        if (activeCameraDevice != null) {
                            index = deviceList.indexOf(activeCameraDevice);
                            if (index < 0 ){
                                index = 0;
                            }
                        }
                        selectedPositionForCameraList(index);
                        Log.e(TAG, "showCameraSelectionDialog: 3");
                    }
                }else if (isStreaming) {
                    int index = 0;
                    if (activeCameraDevice != null) {
                        index = deviceList.indexOf(activeCameraDevice);
                        if (index < 0 ){
                            index = 0;
                        }
                    }
                    isNavFromStream = true;
                    isRequest = false;
                    selectedPositionForCameraList(index);
                }else {
                    AppPreference.setInt(AppPreference.KEY.USB_RESOLUTION, 1);
                    MainActivity.instance.showCamerasList(deviceNames);
                    Log.e(TAG, "showCameraSelectionDialog: 4");
                }
            }
        }
    }

    public void selectedPositionForCameraList(int position) {
        List<UsbDevice> deviceList = new ArrayList<>(mCameraDeviceList);
        isAlertShow = false;
        if (!deviceList.isEmpty()) {
            UsbDevice selectedDevice = deviceList.get(position);
            Activity activity = MainActivity.instance;
            if (activity != null) {
                isRequest = false;
                activity.runOnUiThread(() -> requestUsbCameraPermission(selectedDevice));
            } else {
                Log.e("BgUSBService", "Activity is null, cannot request USB camera permission.");
            }
        } else {
            Activity activity = MainActivity.instance;
            if (activity != null) {
                Toast.makeText(activity,"Camera not Initialized. Please retry", Toast.LENGTH_SHORT).show();
            }
        }

    }

    private void requestUsbCameraPermission(UsbDevice device) {
        if (activeCameraDevice != null && !device.equals(activeCameraDevice)) {
            Log.d(TAG, "Stopping old camera before switching to: " + device.getDeviceName());
            seamlessCameraSwitch(device);
            return;
        }
        activeCameraDevice = device;
        if (!isRequest) {
            isRequest = true;
            if (mUSBMonitor != null) {
                if (MainActivity.instance != null) MainActivity.instance.runOnUiThread(() -> {
                    mUSBMonitor.requestPermission(device);
                });
            }
        }
    }

    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Log.d(TAG, "USB onAttach: " + device.getDeviceName());
            if (!isCameraDevice(device)) return;
            if (isCameraDevice(device)) {
                // Store it in a list of camera devices if it's not already in there
                if (!mCameraDeviceList.contains(device) && !isNavFromStream) {
                    mCameraDeviceList.add(device);
                }
                // 🔴 Auto-reconnect for EGL restarts
                if (isRestartDueToEGLFailure) {
                    String savedDevice = AppPreference.getStr(AppPreference.KEY.USB_CAMERA_NAME, "");
                    if (device.getDeviceName().equals(savedDevice)) {
                        Log.i(TAG, "Reconnecting to camera after EGL restart");
                        isRestartDueToEGLFailure = false;
                        requestUsbCameraPermission(device);
                        return;
                    }
                }
                // Now show the popup so the user can pick which camera they want
                showCameraSelectionDialog();
            } else {
                // Not a camera device
                Log.d(TAG, "onAttach: This device is NOT a camera. Ignoring.");
            }

        }
        @Override
        public void onConnect(android.hardware.usb.UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
            openCameraOnBackground(device,ctrlBlock);
        }

        @Override
        public void onDisconnect(android.hardware.usb.UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
            Log.d(TAG, "USB onDisconnect: " + device.getDeviceName());
            cameraDisconnected(device);
        }

        @Override
        public void onDettach(final android.hardware.usb.UsbDevice device) {
            Log.d(TAG, "USB onDettach: " + device.getDeviceName());
            cameraDisconnected(device);
        }

        @Override
        protected void finalize() throws Throwable {
            try {
                if (mUVCCameraHandler != null) {
                    mUVCCameraHandler.close();
                }
            } finally {
                super.finalize();
            }
        }

        @Override
        public void onCancel(final android.hardware.usb.UsbDevice device) {
            isRequest = false;
        }
    };

    ExecutorService executor = Executors.newSingleThreadExecutor();
    private void cameraDisconnected(UsbDevice device) {
        if (isUVCameraDisconnect) {
            isUVCameraDisconnect = false;
            return;
        }

        Log.d(TAG, "USB cameraDisconnected: " + device.getDeviceName());
        setupDefaultResolutions();
        // Mark that we're shutting down
        isShuttingDown = true;
        isConnected = false;
        isRequest = false;
        // Stop streaming if active
        if (mEglManager != null && mEglManager.isStreaming()) {
            MainActivity.instance.runOnUiThread(() ->
                    Toast.makeText(MainActivity.instance,
                            "Camera disconnected, switching to audio-only mode",
                            Toast.LENGTH_SHORT).show()
            );
        }
        if (isStreaming()) {
            if (SettingsUtils.isAllowedAudio()) {
                switchToAudioOnlyMode();
            } else {
                mEglManager.drawBlankFrameWithOverlay();
            }
        }

        // Now stop preview. We want to guarantee the libuvc thread stops
        if (mUVCCameraHandler != null) {
            mUVCCameraHandler.stopPreview();
            mUVCCameraHandler.close();
            mUVCCameraHandler.release();
            mUVCCameraHandler = null;
            isShuttingDown = false;
        }
    }

    private void switchToAudioOnlyMode() {
        if (mEglManager != null) {
            MainActivity.instance.runOnUiThread(() ->
                    Toast.makeText(this, "Switched to audio-only mode", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void seamlessCameraSwitch(UsbDevice newDevice) {
        // Preserve stream state
        boolean wasStreaming = isStreaming();
        boolean wasRecording = isRecording();


        // Add EGL cleanup before switching
        if (mEglManager != null) {
            mEglManager.shutdown(); // Properly release EGL resources
            isInitEGL = false; // Reset initialization flag
        }
        // Release current camera
        if (mUVCCameraHandler != null) {
            mUVCCameraHandler.stopPreview();
            mUVCCameraHandler.close();
        }

        // Switch to new camera
        activeCameraDevice = newDevice;
        requestUsbCameraPermission(activeCameraDevice);

        // Restore stream state
        if (wasStreaming) startStreaming();
        if (wasRecording) startRecording();
    }
    void setupDefaultResolutions() {
        if (SettingsFragment.instance != null && SettingsFragment.instance.get() != null) {
            if (SettingsFragment.instance.get().camera_sizes != null) {
                SettingsFragment.instance.get().onStreamMedium();
            }
            if (SettingsFragment.instance.get().record_sizes != null) {
                SettingsFragment.instance.get().onVideoHigh();
            }
        }
    }
    private void openCameraOnBackground(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
        executor.execute(() -> {
            try {
                isConnected = true;
                mServiceHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isRequest = false;
                        Log.d(TAG, "USB onConnect: " + device.getDeviceName());
                        String deviceName = device.getDeviceName();
                        AppPreference.setStr(AppPreference.KEY.USB_CAMERA_NAME, device.getDeviceName());
                        Log.d("USB", "Device Name: " + deviceName);
                        USBMonitor.UsbControlBlock ctrlBlockNew = ctrlBlock;
                        UVCCamera mUVCCamera = new UVCCamera();
                        try {
                            mUVCCamera.open(ctrlBlock);
                            String supportedSizeJson = mUVCCamera.getSupportedSize();
                            parseFormatsJson(supportedSizeJson);
                            isUVCameraDisconnect = true;
                            mUVCCamera.stopPreview();
                            mUVCCamera.destroy();
                            mUVCCamera.close();
                            mUVCCamera = null;
                            loadUSBCameraSettings();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            mServiceHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Log.e(TAG, "ensureCameraHandler: Starting cam");
                                    if (MainActivity.instance != null) {
                                        Log.e(TAG, "ensureCameraHandler: Starting cam2");
                                        String codec = codec_sources.get(usbCodec);
                                        if (Objects.equals(codec, "MJPEG") || Objects.equals(codec, "Default")) { // MJPEG selected
                                            Log.e(TAG, "run init usb camera: 1 ");
                                            usbCodec = UVCCamera.FRAME_FORMAT_MJPEG;
                                        } else { // YUYV selected
                                            usbCodec = UVCCamera.FRAME_FORMAT_YUYV;
                                            Log.e(TAG, "run init usb camera: 2 ");
                                        }
                                        mUVCCameraHandler = UVCCameraHandler.createHandler(MainActivity.instance, null, 0, mPreviewWidth, mPreviewHeight, usbCodec);
                                        mUVCCameraHandler.open(ctrlBlockNew);
                                        setStatus(BackgroundNotification.NOTIFICATION_STATUS.OPENED);
                                        setupEglInject();
                                    }

                                    if (MainActivity.instance != null) {
                                        if (sharedViewModel != null) {
                                            sharedViewModel.setUsbRecording(isRecording());
                                            sharedViewModel.setUsbStreaming(isStreaming());
                                            sharedViewModel.setCameraOpened(true);
                                        }
                                    }
                                }
                            }, 500);
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                        }
                        setStatus(BackgroundNotification.NOTIFICATION_STATUS.OPENED);
                        AppPreference.setStr(AppPreference.KEY.USB_CAMERA_NAME, device.getDeviceName());

                        if (SettingsFragment.instance != null && SettingsFragment.instance.get() != null) {
                            SettingsFragment.instance.get().setCameraname();
                        }
                        isNavFromStream = false;
                        mServiceHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!isStreaming()) {
                                    if (AppPreference.getBool(AppPreference.KEY.STREAM_STARTED, false)) {
                                        startStreaming();
                                    }
                                }
                            }
                        }, 3000);
                    }
                }, 500); // 2000 milliseconds = 2 seconds
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void parseFormatsJson(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return;  // Return empty if no data
        }
        try {
            JSONObject root = new JSONObject(jsonStr);
            JSONArray formatsArray = root.optJSONArray("formats");
            if (formatsArray != null) {
                // Loop through all "formats" objects
                for (int i = 0; i < formatsArray.length(); i++) {
                    JSONObject formatObj = formatsArray.optJSONObject(i);
                    if (formatObj != null) {
                        // Each format has a "size" array
                        JSONArray sizeArr = formatObj.optJSONArray("size");
                        if (sizeArr != null) {
                            for (int j = 0; j < sizeArr.length(); j++) {
                                String sizeString = sizeArr.optString(j);
                                // e.g., "640x480"
                                resolutionList.add(sizeString);
                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (sharedViewModel != null) {
            sharedViewModel.saveCameraResolution(resolutionList);
            if (MainActivity.instance != null) {
                MainActivity.instance.saveUSBCameraResolutions();
            }
        }
        if (SettingsFragment.instance != null && SettingsFragment.instance.get() != null) {
            SettingsFragment.instance.get().initUSBResolutions();
        }
        int resSource = AppPreference.getInt(AppPreference.KEY.USB_RESOLUTION, 0);
        List<String> sortedResolutions = sharedViewModel.getCameraResolution();
        if (resSource >= sortedResolutions.size()) {
            resSource = sortedResolutions.size() - 1;
            if (resSource < 0) {
                resSource = 0;
            }
            AppPreference.setInt(AppPreference.KEY.USB_RESOLUTION, resSource);
        }

        String sizeString = sortedResolutions.get(resSource);
        String[] dimensions = sizeString.split("x");
        mPreviewWidth = Integer.parseInt(dimensions[0]);
        mPreviewHeight = Integer.parseInt(dimensions[1]);
        int resolution = AppPreference.getInt(AppPreference.KEY.VIDEO_RESOLUTION, 0);
        if (resolution >= sortedResolutions.size()) {
            resolution = sortedResolutions.size() - 1;
            AppPreference.setInt(AppPreference.KEY.VIDEO_RESOLUTION, resolution);
        }

        int recordIndex = sortedResolutions.indexOf("1920x1080");
        int streamIndex = sortedResolutions.indexOf("1280x720");
        if (recordIndex< 0) {
            recordIndex = 0;
        }
        if (streamIndex < 0) {
            streamIndex = 1;
        }
        int streaming_position = AppPreference.getInt(AppPreference.KEY.STREAMING_QUALITY, 1);
        int recording_position = AppPreference.getInt(AppPreference.KEY.VIDEO_QUALITY, 0);
        if (activeCameraDevice != null && streaming_position == 5 && recording_position == 5) {
            int record = AppPreference.getInt(AppPreference.KEY.VIDEO_RESOLUTION, recordIndex);
            String sizeResolutionString = sortedResolutions.get(record);
            String[] resolutionDimensions = sizeResolutionString.split("x");
            int rWidth = Integer.parseInt(resolutionDimensions[0]);
            int rHeight = Integer.parseInt(resolutionDimensions[1]);
            mEglManager.recordSize = new Streamer.Size(rWidth,rHeight);
        }else if (activeCameraDevice != null && streaming_position == 5) {
            int record = AppPreference.getInt(AppPreference.KEY.VIDEO_RESOLUTION, recordIndex);
            String sizeResolutionString = sortedResolutions.get(record);
            String[] resolutionDimensions = sizeResolutionString.split("x");
            int rWidth = Integer.parseInt(resolutionDimensions[0]);
            int rHeight = Integer.parseInt(resolutionDimensions[1]);
            mEglManager.recordSize = new Streamer.Size(rWidth,rHeight);
            AppPreference.setInt(AppPreference.KEY.VIDEO_RESOLUTION, recordIndex);
        }else if (activeCameraDevice != null && recordIndex == 5) {
            int record = AppPreference.getInt(AppPreference.KEY.VIDEO_RESOLUTION, recordIndex);
            String sizeResolutionString = sortedResolutions.get(record);
            String[] resolutionDimensions = sizeResolutionString.split("x");
            int rWidth = Integer.parseInt(resolutionDimensions[0]);
            int rHeight = Integer.parseInt(resolutionDimensions[1]);
            mEglManager.recordSize = new Streamer.Size(rWidth,rHeight);
            AppPreference.setInt(AppPreference.KEY.STREAMING_RESOLUTION, streamIndex);
        }else {
            String sizeResolutionString = sortedResolutions.get(recordIndex);
            String[] resolutionDimensions = sizeResolutionString.split("x");
            int rWidth = Integer.parseInt(resolutionDimensions[0]);
            int rHeight = Integer.parseInt(resolutionDimensions[1]);
            mEglManager.recordSize = new Streamer.Size(rWidth,rHeight);
            AppPreference.setInt(AppPreference.KEY.VIDEO_RESOLUTION, recordIndex);
            AppPreference.setInt(AppPreference.KEY.STREAMING_RESOLUTION, streamIndex);
        }
    }
    @Override
    public void onDestroy() {
        // 1. Release WakeLock
        mCameraDeviceList.clear();

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (!isRestarting) {
            AppPreference.setBool(AppPreference.KEY.EGL_RESTART_FLAG, false);
            AppPreference.setInt(AppPreference.KEY.RESTART_COUNT, 0);
        }


        // 2. Unregister and destroy USBMonitor
        if (mUSBMonitor != null) {
            mUSBMonitor.unregister();
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }

        // 3. Stop preview & release UVC camera handler
        if (mUVCCameraHandler != null) {
            mUVCCameraHandler.stopPreview();
            mUVCCameraHandler.close();
            mUVCCameraHandler.release();
            mUVCCameraHandler = null;
        }

        // 6. Clean up the service handler thread
        if (mServiceHandlerThread != null) {
            mServiceHandlerThread.quitSafely();
            try {
                mServiceHandlerThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            mServiceHandlerThread = null;
        }
        // 7. Shutdown executors
        backgroundExecutor.shutdownNow();
        executor.shutdownNow();

        // 8. Clear any notification callbacks
        if (mNotifyCallback != null) {
            mNotifyCallback.stopService(ServiceType.BgUSBCamera);
            mNotifyCallback = null;
        }

        // 9. Clear the binder reference
        if (mBinderRef != null) {
            mBinderRef.clear();
            mBinderRef = null;
        }
        ckearSharedInctance();
        // 10. Reset state flags
        isRunning    = false;
        isConnected  = false;
        activeCameraDevice = null;
        isRestarting = false;
        isRestartDueToEGLFailure = false;
        super.onDestroy();
        stopSafe();
    }

    void ckearSharedInctance() {
        if (mEglManager != null) {
            mEglManager.shutdown();           // frees GL/streams but leaves sInstance
            SharedEglManager.cleanAndReset();   // synchronous
            // SharedEglManager.cleanAndResetAsync(null); // non-blocking
            mEglManager = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (mBinderRef != null && mBinderRef.get() != null) {
            return mBinderRef.get();
        }else {
            mBinderRef = new WeakReference<>(new CameraBinder(this));
            return mBinderRef.get();
        }
    }

}