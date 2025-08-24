package com.checkmate.android.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.opengl.GLES20;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Log;
import android.util.Range;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.checkmate.android.AppConstant;
import com.checkmate.android.AppPreference;
import com.checkmate.android.R;
import com.checkmate.android.model.Camera;
import com.checkmate.android.ui.fragment.LiveFragment;
import com.checkmate.android.ui.fragment.StreamingFragment;
import com.checkmate.android.ui.view.SurfaceImageText;
import com.checkmate.android.util.CameraInfo;
import com.checkmate.android.util.Connection;
import com.checkmate.android.util.ConnectionStatistics;
import com.checkmate.android.util.ErrorMessage;
import com.checkmate.android.util.Formatter;
import com.checkmate.android.util.MainActivity;
import com.checkmate.android.util.MessageUtil;
import com.checkmate.android.util.MicThread;
import com.checkmate.android.util.ResourceUtil;
import com.checkmate.android.util.SettingsUtils;
import com.checkmate.android.util.StorageUtils;
import com.checkmate.android.util.StreamConditionerBase;
import com.wmspanel.libstream.AudioConfig;
import com.wmspanel.libstream.ConnectionConfig;
import com.wmspanel.libstream.RistConfig;
import com.wmspanel.libstream.SrtConfig;
import com.wmspanel.libstream.Streamer;
import com.wmspanel.libstream.StreamerSurface;
import com.wmspanel.libstream.StreamerSurfaceBuilder;
import com.wmspanel.libstream.VideoConfig;

import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static androidx.core.app.NotificationCompat.PRIORITY_MIN;
import static com.wmspanel.libstream.Streamer.CONNECTION_STATE.DISCONNECTED;

public class BgWifiService extends Service {

    private static final String TAG = "bgWifiSvc";
    private static final int NOTIFICATION_ID = R.string.wifi_camera_running;

    private Intent mRunningIntent;
    private NotificationManager nm;


    public Intent getRunningIntent() {
        return mRunningIntent;
    }

    // WeakReference to avoid holding a strong reference to the Service
    public static class WifiCameraBinder extends Binder {
        private final WeakReference<BgWifiService> mServiceWeakRef;

        public WifiCameraBinder(BgWifiService service) {
            mServiceWeakRef = new WeakReference<>(service);
        }

        public BgWifiService getService() {
            // Return the service only if it's still available (i.e., not garbage collected)
            return mServiceWeakRef.get();
        }
    }

    private final IBinder mBinder = new WifiCameraBinder(this);
    @Override
    public void onCreate() {

        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent,
                              int flags,
                              int startId) {

        showNotification();
        mRunningIntent = intent;

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        cancelNotitfication();
        Log.d(TAG, "onDestroy");

        stopAPICalling();
    }

    public void stopSafe() {
        BgWifiService.this.stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void setActions(boolean isRecording) {
    }

    private void showNotification() {
        startForeground();
    }

    private void startForeground() {
        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel("wifi_service", "Wifi camera Service");
        } else {

        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        Notification notification = notificationBuilder.setOngoing(true).setSmallIcon(R.mipmap.ic_launcher).setPriority(PRIORITY_MIN).setCategory(Notification.CATEGORY_SERVICE).build();
        startForeground(102, notification);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(String channelId, String channelName) {
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE);
        channel.setLightColor(Color.BLUE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(channel);
        return channelId;
    }

    private void cancelNotitfication() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(R.string.wifi_camera_running);
    }

    public void playStreaming(Camera camera) {
        if (MainActivity.instance != null) {
            MainActivity.instance.playStream(camera);
        }
    }

    public void pushStreaming() {
        if (MainActivity.instance != null) {
            MainActivity.instance.startWifiStreaming();
        }
    }

    public void stopStreaming() {
        if (MainActivity.instance != null) {
            MainActivity.instance.stopWifiStream();
        }
    }

    public void stopPushing() {
        if (MainActivity.instance != null) {
            MainActivity.instance.stopWifiStreaming();
        }
    }

    public void startAPICalling() {
        handler = new Handler();
        updateTimeRunnable = new Runnable() {
            @Override
            public void run() {
                new Task().execute();
                handler.postDelayed(this, 3000); // Schedule next update
            }
        };
        handler.post(updateTimeRunnable);
    }
    Runnable updateTimeRunnable;
    Handler handler;
    class Task extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                NetworkRequest.Builder builder;
                builder = new NetworkRequest.Builder();
                //set the transport type do WIFI
                builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                connectivityManager.requestNetwork(builder.build(), new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        connectivityManager.bindProcessToNetwork(network);
                        try {
                            URL url = new URL("http://192.168.1.254?custom=1&cmd=3036&par=0");
                            Log.e("API call wakeup: ", "true");
                            HttpURLConnection uc = (HttpURLConnection) url.openConnection();
                            uc.setReadTimeout(10 * 1000);
                            int rc = uc.getResponseCode();
                            Log.d("rc", rc + "");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        connectivityManager.unregisterNetworkCallback(this);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public void stopAPICalling() {

                if (handler!=null){
            handler.removeCallbacks(updateTimeRunnable);
        }
        Log.e("wakeup: ", "Disconnect");
    }

    private Runnable mUpdateAPI = () -> {
        Log.e("API call wakeup: ", "True");
        HttpURLConnection urlConnection = null;
        String request_url = null;

        try {
            request_url = "http://192.168.1.254/?custom=1&cmd=3036&par=0";
            URL requestedUrl = new URL(request_url);
            urlConnection = (HttpURLConnection) requestedUrl.openConnection();
            urlConnection.addRequestProperty("Content-Type", "text/plain; charset=UTF-8");

            urlConnection.setRequestMethod("GET");
            urlConnection.setUseCaches(false);
            int status_code = urlConnection.getResponseCode();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    };
}