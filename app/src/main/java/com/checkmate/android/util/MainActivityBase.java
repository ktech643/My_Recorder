package com.checkmate.android.util;

import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.checkmate.android.R;
import com.wmspanel.libstream.Streamer;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// Base class for Streamer activities
// Holds connection logic, preferences, UI and Activity state transition
public abstract class MainActivityBase extends ActivityCommons implements Streamer.Listener {
    private final String TAG = "MainActivityBase";

    private Handler mHandler;
    protected Streamer mStreamer;
    protected boolean mBroadcastOn;
    protected AtomicInteger mRetryPending = new AtomicInteger();

    private final Map<Integer, Connection> mConnectionId = new ConcurrentHashMap<>();
    private final Map<Integer, Streamer.CONNECTION_STATE> mConnectionState = new ConcurrentHashMap<>();
    private final Map<Integer, ConnectionStatistics> mConnectionStatistics = new ConcurrentHashMap<>();

    protected Streamer.CAPTURE_STATE mVideoCaptureState = Streamer.CAPTURE_STATE.FAILED;
    protected Streamer.CAPTURE_STATE mAudioCaptureState = Streamer.CAPTURE_STATE.FAILED;

    private long mBroadcastStartTime;


    protected boolean mIsMuted;
    private boolean mUseBluetooth;

    private int mRestartRecordInterval;

    protected AlertDialog mAlert;

    protected ScaleGestureDetector mScaleGestureDetector;
    protected float mScaleFactor;

    protected StreamConditionerBase mConditioner;

    private final Runnable mStopRecord = new Runnable() {
        @Override
        public void run() {
            if (mStreamer != null) {
                mStreamer.stopRecord();
            }
        }
    };

    protected final Runnable mUpdateStatistics = new Runnable() {
        @Override
        public void run() {
            if (mStreamer == null) {
                return;
            }

            mFpsView.setText(mFormatter.fpsToString(mStreamer.getFps()));

            // Overlay demo: draw live clock over camera image
            updateOverlays();

            if (mConnectionId.keySet().isEmpty()) {
                return;
            }

            for (int id : mConnectionId.keySet()) {
                Streamer.CONNECTION_STATE state = mConnectionState.get(id);
                if (state == null) {
                    continue;
                }

                // some auth schemes require reconnection to same url multiple times
                // app should not query connection statistics while auth phase is in progress
                if (state == Streamer.CONNECTION_STATE.RECORD) {
                    ConnectionStatistics statistics = mConnectionStatistics.get(id);
                    if (statistics != null) {
                        statistics.update(mStreamer, id);
                    }
                }
            }

            final long curTime = System.currentTimeMillis();
            final long duration = (curTime - mBroadcastStartTime) / 1000L;
            mBroadcastTime.setText(mFormatter.timeToString(duration));

            updateConnectionInfo();

            int idx = 0;
            for (int displayId : mConnectionId.keySet()) {
                if (idx < mConnectionName.size() && idx < mConnectionStatus.size()) {
                    mConnectionName.get(idx).setText(mConnectionId.get(displayId).name);
                    ConnectionStatistics display = mConnectionStatistics.get(displayId);
                    if (display != null) {
                        final int color = display.isPacketLossIncreasing() ? Color.YELLOW : Color.WHITE;
                        mConnectionStatus.get(idx).setTextColor(color);
                        mConnectionStatus.get(idx).setText(String.format(getString(R.string.connection_stats),
                                mFormatter.bandwidthToString(display.getBandwidth()),
                                mFormatter.trafficToString(display.getTraffic())
                        ));
                    }
                    idx++;
                }
            }
        }
    };

    protected class RetryRunnable implements Runnable {
        private final Connection connection;

        public RetryRunnable(Connection connection) {
            this.connection = connection;
        }

        @Override
        public void run() {
            int connectionId = -1;
            mRetryPending.decrementAndGet();

            if (mBroadcastOn) {
                if (canConnect()) {
                    connectionId = createConnection(connection);
                }
                if (connectionId == -1) {
                    mHandler.postDelayed(new RetryRunnable(connection), RETRY_TIMEOUT);
                    mRetryPending.incrementAndGet();
                }
            }
        }
    }

    protected final static int RETRY_TIMEOUT = 3000; // 3 sec.

    protected BroadcastReceiver mBluetoothScoStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED.equals(intent.getAction())) {
                int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
                //Log.d(TAG, "Audio SCO state: " + state);
                switch (state) {
                    case AudioManager.SCO_AUDIO_STATE_CONNECTED:
                        if (mStreamer != null) {
                            showToast(getString(R.string.bluetooth_connected));
                            mStreamer.startAudioCapture(mAudioCallback);
                        }
                        break;
                    default:
                        break;
                }
            } else {
                int btState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                if (btState == BluetoothProfile.STATE_CONNECTED) {
                    startAudioCapture();
                }
            }
        }
    };

    /**
     * Demo of custom audio input processing in app code, implementing
     * {@link Streamer#getRawAmplitude()} and {@link Streamer#setSilence(boolean)}.
     * <p>
     * Changing the volume of an audio signal must be done by applying a gain (multiplication)
     * and optionally clipping if your system has a limited dynamic range.
     * <p>
     * Audio callback runs on separate thread.
     */
    protected Streamer.AudioCallback mAudioCallback = new Streamer.AudioCallback() {
        /**
         * @param audioFormat {@link android.media.AudioFormat#ENCODING_PCM_16BIT}
         * @param data
         * @param audioInputLength {@link android.media.AudioRecord#read(byte[], int, int)}
         * @param channelCount
         * @param sampleRate
         * @param samplesPerFrame AAC frame size (1024 samples)
         */
        @Override
        public void onAudioDelivered(int audioFormat, byte[] data, int audioInputLength,
                                     int channelCount, int sampleRate, int samplesPerFrame) {

            mVuMeter.putBuffer(data, channelCount, sampleRate);

            // If your app needs advanced audio processing (boost input volume, etc.), you can modify
            // raw pcm data before it goes to aac encoder.
            //Arrays.fill(data, (byte) 0); // "Mute" audio
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.v(TAG, "onCreate()");

        mHandler = new Handler(Looper.getMainLooper());
        mFormatter = new Formatter(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Log.v(TAG, "onStart(), orientation=" + getResources().getConfiguration().orientation);
        mVuMeter.setChannels(SettingsUtils.channelCount(this));



        handler = new Handler();
        updateTimeRunnable = new Runnable() {
            @Override
            public void run() {
                mHandler.post(mUpdateStatistics);

                handler.postDelayed(this, 1000); // Schedule next update
            }
        };
        handler.post(updateTimeRunnable);
    }
    Runnable updateTimeRunnable;
    Handler handler;
    @Override
    protected void onRestart() {
        super.onRestart();
        //Log.v(TAG, "onRestart(), orientation=" + getResources().getConfiguration().orientation);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Log.v(TAG, "onResume(), orientation=" + getResources().getConfiguration().orientation);


        mUseBluetooth = SettingsUtils.useBluetooth(this);
        if (mUseBluetooth) {
            IntentFilter filter = new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
            filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
            registerReceiver(mBluetoothScoStateReceiver, filter);
        }

        mVolumeKeysAction = SettingsUtils.volumeKeysAction(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Log.v(TAG, "onPause(), orientation=" + getResources().getConfiguration().orientation);

        stopRespondingToTouchEvents();


        // Applications should release the camera immediately in onPause()
        // https://developer.android.com/guide/topics/media/camera.html#release-camera
        releaseStreamer();

        // discard adaptive bitrate calculator
        mConditioner = null;

        if (mUseBluetooth) {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                am.stopBluetoothSco();
            }
            unregisterReceiver(mBluetoothScoStateReceiver);
        }

        dismissDialog();
        dismissToast();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Log.v(TAG, "onStop(), orientation=" + getResources().getConfiguration().orientation);
        // stop UI update
        if (handler!=null){
            handler.removeCallbacks(updateTimeRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Log.v(TAG, "onDestroy(), orientation=" + getResources().getConfiguration().orientation);
    }

    // Applications should release the camera immediately in onPause()
    // https://developer.android.com/guide/topics/media/camera.html#release-camera
    protected void releaseStreamer() {
        // check if Streamer instance exists
        if (mStreamer == null) {
            return;
        }
        // stop broadcast
        releaseConnections();
        // stop mp4 recording
        mStreamer.stopRecord();
        // cancel audio and video capture
        mStreamer.stopAudioCapture();
        mStreamer.stopVideoCapture();
        // finally release streamer, after release(), the object is no longer available
        // if a Streamer is in released state, all methods will throw an IllegalStateException
        mStreamer.release();
        // sanitize Streamer object holder
        mStreamer = null;
    }

    protected boolean canConnect() {
        if (mStreamer == null) {
            return false;
        }

        // Larix always wants to get both video and audio capture running,
        // regardless of connection modes; But if you need only audio in your app
        // (you should only call to startAudioCapture() in this case),
        // then you can skip video state check
        final boolean isStreamerReady = isAudioCaptureStarted() && isVideoCaptureStarted();

        if (!isStreamerReady) {
            Log.d(TAG, "AudioCaptureState=" + mAudioCaptureState);
            Log.d(TAG, "VideoCaptureState=" + mVideoCaptureState);
            if (mUseBluetooth && !isAudioCaptureStarted()) {
                showToast(getString(R.string.no_bluetooth));
            } else {
                showToast(getString(R.string.please_wait));
            }
            return false;
        }

        if (!isConnected()) {
            showToast(getString(R.string.not_connected));
            return false;
        }

        return true;
    }

    protected boolean isAudioCaptureStarted() {
        return mAudioCaptureState == Streamer.CAPTURE_STATE.STARTED;
    }

    protected boolean isVideoCaptureStarted() {
        return mVideoCaptureState == Streamer.CAPTURE_STATE.STARTED;
    }

    protected boolean createConnections() {
        if (!canConnect()) {
            return false;
        }

        List<Connection> connectionsList = SettingsUtils.connections();
        if (connectionsList.isEmpty()) {
            showToast(getString(R.string.no_uri));
            return false;
        }

        for (Connection connection : connectionsList) {
            createConnection(connection);
        }

        if (mConnectionId.isEmpty()) {
            return false;
        }

        startRecord();

        mBroadcastStartTime = System.currentTimeMillis();
        if (mConditioner != null) {
            mConditioner.start(mStreamer, SettingsUtils.videoBitRate(this), mConnectionId.keySet());
        }
        setConnectionInfo(true);
        return true;
    }

    protected int createConnection(Connection connection) {
        final String scheme = Uri.parse(connection.url).getScheme();
        if (!SettingsUtils.UriResult.isSupported(scheme)) {
            return -1;
        }

        final int connectionId;
        try {
            if (SettingsUtils.UriResult.isSrt(scheme)) {
                // https://github.com/Haivision/srt/blob/master/docs/API.md
                // https://github.com/Haivision/srt/blob/master/docs/AccessControl.md

                connectionId = mStreamer.createConnection(SettingsUtils.toSrtConfig(connection));

            } else if (SettingsUtils.UriResult.isRist(scheme)) {
                connectionId = mStreamer.createConnection(SettingsUtils.toRistConfig(connection));

            } else {
                // Aliyun CDN supports private RTMP codec id for HEVC
                // There is a patch for FFMPEG to publish 265 over FLV:
                // https://github.com/CDN-Union/Code/tree/master/flv265-ChinaNetCenter
                if (SettingsUtils.UriResult.isRtmp(scheme) &&
                        MediaFormat.MIMETYPE_VIDEO_HEVC.equals(mStreamer.getVideoCodecType())) {
                    showToast(String.format(getString(R.string.hevc_over_rtmp_warning), connection.name));
                }

                connectionId = mStreamer.createConnection(SettingsUtils.toConnectionConfig(connection));
            }
        } catch (URISyntaxException e) {
            return -1;
        }

        if (connectionId != -1) {
            mConnectionId.put(connectionId, connection);
            mConnectionStatistics.put(connectionId, new ConnectionStatistics());
            if (mConditioner != null) {
                mConditioner.addConnection(connectionId);
            }
        } else {
            showToast(String.format(getString(R.string.try_again_later), connection.name));
        }
        return connectionId;
    }

    protected void releaseConnections() {
        mBroadcastOn = false;

        for (Integer id : mConnectionId.keySet()) {
            releaseConnection(id);
        }
        mRetryPending.set(0);

        mCaptureButton.setBackgroundResource(R.drawable.btn_start);
        enableSettingsButton(mSettingsButton, true);

        // we don't plan to reconnect automatically, so stop stream recording
        stopRecord();

        if (mConditioner != null) {
            mConditioner.stop();
        }
        // don't keep mute state after restart
        mute(false);
        setConnectionInfo(false);
    }

    protected void releaseConnection(int connectionId) {
        if (mStreamer != null && connectionId != -1) {
            mConnectionId.remove(connectionId);
            mConnectionState.remove(connectionId);
            mConnectionStatistics.remove(connectionId);
            mStreamer.releaseConnection(connectionId);
            if (mConditioner != null) {
                mConditioner.removeConnection(connectionId);
            }
        }
        updateConnectionInfo();
    }

    protected void updateConnectionInfo() {
        final int activeNum = mConnectionId.size();

        for (int idx = 0; idx < mConnectionName.size() && idx < mConnectionStatus.size(); idx++) {
            if (idx < activeNum) {
                mConnectionName.get(idx).setVisibility(View.VISIBLE);
                mConnectionStatus.get(idx).setVisibility(View.VISIBLE);
            } else {
                mConnectionName.get(idx).setVisibility(View.GONE);
                mConnectionName.get(idx).setText("");
                mConnectionStatus.get(idx).setVisibility(View.GONE);
                mConnectionStatus.get(idx).setText(getString(R.string.connecting));
            }
        }
    }

    protected void setConnectionInfo(boolean show) {
        final int isVisible = show ? View.VISIBLE : View.GONE;

        mBroadcastTime.setText(show ? "00:00:00" : "");
        mBroadcastTime.setVisibility(isVisible);

        final int count = show ? mConnectionId.size() : SettingsUtils.CONN_MAX;
        if (count > mConnectionName.size() || count > mConnectionStatus.size()) {
            return;
        }

        for (int idx = 0; idx < count; idx++) {
            final TextView name = mConnectionName.get(idx);
            name.setTextColor(Color.WHITE);
            name.setText("");
            name.setVisibility(isVisible);
        }

        for (int idx = 0; idx < count; idx++) {
            final TextView status = mConnectionStatus.get(idx);
            status.setTextColor(Color.WHITE);
            status.setText(show ? getString(R.string.connecting) : "");
            status.setVisibility(isVisible);
        }
    }

    @Override
    protected void broadcastClick() {
        if (mStreamer == null) {
            // preventing accidental touch issues
            return;
        }
        if (!mBroadcastOn) {
            if (createConnections()) {
                mBroadcastOn = true;
                enableSettingsButton(mSettingsButton, false);
                mCaptureButton.setBackgroundResource(R.drawable.btn_stop);
            }
        } else {
            releaseConnections();
        }
    }

    @Override
    protected void flashClick() {
        if (mStreamer != null && mVideoCaptureState == Streamer.CAPTURE_STATE.STARTED) {
            mStreamer.toggleTorch();
        }
    }

    @Override
    protected void muteClick() {
        mute(!mIsMuted);
    }

    protected void mute(boolean mute) {
        if (mStreamer == null) {
            return;
        }
        // How to mute audio:
        // Option 1 - stop audio capture and as result stop sending audio packets to server
        // Some players can stop playback if client keeps sending video, but sends no audio packets
        // Option 2 (workaround) - set PCM sound level to zero and encode
        // This produces silence in audio stream
        if (isAudioCaptureStarted()) {
            mIsMuted = mute;
            mStreamer.setSilence(mIsMuted);

            if (mIsMuted) {
                mMuteButton.setAlpha(1.0f);
                mMuteButton.setBackgroundResource(R.drawable.button_inverse);
                mMuteButton.setImageResource(R.drawable.button_mute_on);
            } else {
                mMuteButton.setAlpha(0.3f);
                mMuteButton.setBackgroundResource(R.drawable.button);
                mMuteButton.setImageResource(R.drawable.button_mute_off);
            }
        }
    }

    @Override
    public void onConnectionStateChanged(int connectionId, Streamer.CONNECTION_STATE state, Streamer.STATUS status, JSONObject info) {
        Log.d(TAG, "onConnectionStateChanged, connectionId=" + connectionId + ", state=" + state + ", status=" + status);

        if (mStreamer == null) {
            return;
        }

        if (!mConnectionId.containsKey(connectionId)) {
            return;
        }

        mConnectionState.put(connectionId, state);

        switch (state) {
            case INITIALIZED:
                break;
            case CONNECTED:
                ConnectionStatistics statistics = mConnectionStatistics.get(connectionId);
                if (statistics != null) {
                    statistics.init(mStreamer, connectionId);
                }
                break;
            case SETUP:
                break;
            case RECORD:
                break;
            case IDLE:
                // connection established successfully, but no data is flowing
                // Larix app expect data always flowing, so this is error for us
                // but in some special cases app can pause capture and keep connection alive
                //
                // real-life example: video chat app opens full screen Gallery to select picture
                // camera will be closed in onPause, but app keeps connection alive to keep
                // ongoing stream recording on server; so idle state is expected and ignored
            case DISCONNECTED:
            default:
                // save info for auto-retry and error message
                final Connection connection = mConnectionId.get(connectionId);
                // remove from active connections list
                releaseConnection(connectionId);

                // show error message including connection name
                showToast(ErrorMessage.connectionErrorMsg(this, connection, status, info));


                // do not try to reconnect in case of wrong credentials
                if (status != Streamer.STATUS.AUTH_FAIL) {
                    mHandler.postDelayed(new RetryRunnable(connection), RETRY_TIMEOUT);
                    mRetryPending.incrementAndGet();
                }

                // all connections totally failed, stop broadcast
                if (mConnectionId.isEmpty() && mRetryPending.get() == 0) {
                    releaseConnections();
                }
                break;
        }
    }

    @Override
    public void onVideoCaptureStateChanged(Streamer.CAPTURE_STATE state) {
        Log.d(TAG, "onVideoCaptureStateChanged, state=" + state);

        mVideoCaptureState = state;

        switch (state) {
            case STARTED:
                // can start broadcasting video
                // mVideoCaptureState will be checked in createConnections()
                break;
            case STOPPED:
                // stop confirmation
                break;
            case ENCODER_FAIL:
            case FAILED:
            default:
                stopRespondingToTouchEvents();
                if (mStreamer != null) {
                    stopRecord();
                    mStreamer.stopVideoCapture();
                }
                showToast(state == Streamer.CAPTURE_STATE.ENCODER_FAIL
                        ? getString(R.string.video_status_encoder_fail) : getString(R.string.video_status_fail));
                break;
        }
    }

    @Override
    public void onAudioCaptureStateChanged(Streamer.CAPTURE_STATE state) {
        Log.d(TAG, "onAudioCaptureStateChanged, state=" + state);

        mAudioCaptureState = state;

        switch (state) {
            case STARTED:
                // can start broadcasting audio
                // mAudioCaptureState will be checked in createConnection()
                break;
            case STOPPED:
                // stop confirmation
                break;
            case ENCODER_FAIL:
            case FAILED:
            default:
                if (mStreamer != null) {
                    stopRecord();
                    mStreamer.stopAudioCapture();
                }
                showToast(state == Streamer.CAPTURE_STATE.ENCODER_FAIL
                        ? getString(R.string.audio_status_encoder_fail) : getString(R.string.audio_status_fail));
                break;
        }
    }

    @Override
    public void onRecordStateChanged(Streamer.RECORD_STATE state, Uri uri, Streamer.SAVE_METHOD method) {
        Log.d(TAG, "onRecordStateChanged, state=" + state);

        mRecIndicator.setVisibility(state == Streamer.RECORD_STATE.STARTED ? View.VISIBLE : View.GONE);

        switch (state) {
            case INITIALIZED:
                //showToast("INFO: new MediaMuxer created");
                break;
            case STARTED:
                //showToast("INFO: MediaMuxer got key frame");
                if (mRestartRecordInterval > 0) {
                    mHandler.postDelayed(mStopRecord, mRestartRecordInterval * 60 * 1000);
                }
                break;
            case STOPPED:
                //showToast("INFO: MediaMuxer destroyed");
                StorageUtils.onSaveFinished(this, uri, method, null);
                if (mRestartRecordInterval > 0) {
                    StorageUtils.startRecord(this, mStreamer);
                }
                break;
            case FAILED:
                showToast(getString(R.string.err_record_failed));
                break;
            default:
                break;
        }
    }

    @Override
    public void onSnapshotStateChanged(Streamer.RECORD_STATE state, Uri uri, Streamer.SAVE_METHOD method) {
        if (state == Streamer.RECORD_STATE.STOPPED) {
            final String displayName = StorageUtils.onSaveFinished(this, uri, method, (p, u) -> {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (u != null) {
                        showToast(String.format(getString(R.string.saved_to), p));
                    }
                });
            });
            if (displayName != null && !displayName.isEmpty()) {
                showToast(String.format(getString(R.string.saved_to), displayName));
            }
        }
    }

    @Override
    public Handler getHandler() {
        return mHandler;
    }

    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        //Log.v(TAG, "onBackPressed");
        if (doubleBackToExitPressedOnce || isFinishing()) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.press_again_to_quit, Toast.LENGTH_SHORT).show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
    }

    protected void startAudioCapture() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (mStreamer == null || am == null) {
            return;
        }
        if (mUseBluetooth) {
            try {
                // On Lollipop 5.0, the method throws an exception if there is no Bluetooth mic connected.
                // This behavior does not happen on earlier versions of Android.
                // https://issuetracker.google.com/issues/37029016
                am.startBluetoothSco();
            } catch (Exception e) {
            }
            // Wait for mBluetoothScoStateReceiver -> AudioManager.SCO_AUDIO_STATE_CONNECTED
        } else {
            // Pass Streamer.AudioCallback instance to access raw pcm audio and calculate audio level
            mStreamer.startAudioCapture(mAudioCallback);
            // If your app doesn't need it, then you can bypass Streamer.AudioCallback implementation
            //mStreamer.startAudioCapture();
        }
    }

    protected void startVideoCapture() {
        if (mStreamer == null) {
            return;
        }
        mStreamer.startVideoCapture();
    }

    protected void showDialog(AlertDialog.Builder dialog) {
        if (!isFinishing()) {
            dismissDialog();
            mAlert = dialog.show();
        }
    }

    private void dismissDialog() {
        if (mAlert != null && mAlert.isShowing()) {
            mAlert.dismiss();
        }
    }

    protected boolean zoom1(float scaleFactor) {
        if (mStreamer == null || mVideoCaptureState != Streamer.CAPTURE_STATE.STARTED) {
            return false;
        }

        mScaleFactor *= scaleFactor;
        // Don't let the object get too small or too large.
        mScaleFactor = Math.max(0.7f, Math.min(mScaleFactor, mStreamer.getMaxZoom()));
        //Log.d(TAG, "Max zoom=" + (int) mStreamer.getMaxZoom() + " new zoom=" + mScaleFactor);

        final int zoom = (int) mScaleFactor;
        mStreamer.zoomTo(zoom);

        Camera.Parameters params = mStreamer.getCameraParameters();
        if (params != null) {
            List<Integer> zoomRatios = params.getZoomRatios();
            if (zoomRatios != null && zoomRatios.size() > zoom) {
                final float zoomRatio = zoomRatios.get(zoom) / 100f;
                mZoomRatio.setText(String.format(getString(R.string.zoom_ratio1), zoomRatio));
                mZoomRatio.setVisibility(zoom > 0 ? View.VISIBLE : View.GONE);
            }
        }

        return true; // consume touch event
    }

    protected boolean zoom2(float scaleFactor) {
        if (mStreamer == null || mVideoCaptureState != Streamer.CAPTURE_STATE.STARTED) {
            return false;
        }

        mScaleFactor *= scaleFactor;
        // Don't let the object get too small or too large.
        mScaleFactor = Math.max(1.0f, Math.min(mScaleFactor, mStreamer.getMaxZoom()));
        //Log.d(TAG, "Max zoom=" + mStreamer.getMaxZoom() + " new zoom=" + mScaleFactor);

        final float delta = Math.abs(mScaleFactor - mStreamer.getZoom());
        if (mScaleFactor > 1.0f && delta < 0.1f) {
            return false;
        }

        mScaleFactor = Math.round(mScaleFactor * 10) / 10f;
        mStreamer.zoomTo(mScaleFactor);
        mZoomRatio.setText(String.format(getString(R.string.zoom_ratio2), mStreamer.getZoom()));
        mZoomRatio.setVisibility(mScaleFactor > 1.0f ? View.VISIBLE : View.GONE);

        return true; // consume touch event
    }

    protected void stopRespondingToTouchEvents() {
    }

    protected void updateOverlays() {
    }

    private void startRecord() {
        stopRecord();
        mRestartRecordInterval = SettingsUtils.recordIntervalMin(this);
        StorageUtils.startRecord(this, mStreamer);
    }

    private void stopRecord() {
        MessageUtil.showToast(this, "Stop record at MainBase");
        mHandler.removeCallbacks(mStopRecord);
        mRestartRecordInterval = 0;
        mRecIndicator.setVisibility(View.GONE);
        mStreamer.stopRecord();
    }


}
