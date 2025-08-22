package com.checkmate.android.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import com.checkmate.android.AppPreference;
import com.checkmate.android.RestartActivity;
import com.checkmate.android.service.SharedEGL.ServiceType;
import com.checkmate.android.ui.activity.SplashActivity;
import com.checkmate.android.util.HttpServer.MyHttpServer;
import com.checkmate.android.util.HttpServer.ServiceManager;
import com.checkmate.android.util.MainActivity;
import com.checkmate.android.service.*;

import toothpick.Toothpick;

public class MyAccessibilityService extends AccessibilityService {

    private static MyAccessibilityService instance;

    public static MyAccessibilityService getInstance() {
        return instance != null ? instance : null;
    }
    public boolean isServerRunning = false;
    private static final double HIGH_MEMORY_THRESHOLD_PERCENT = 0.85; // 85%
    private static final String TAG = "MyAccessibilityService";
    FrameLayout mLayout;
    private Handler handler;
    private Runnable runnable;
    private static final long INTERVAL = 60 * 60 * 1000; // 2 hours in milliseconds //10 * 60 * 1000;
    private MyHttpServer server;
    private static final int SERVER_PORT = 8080;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MyAccessibilityService onCreate");
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        info.notificationTimeout = 100;

        setServiceInfo(info);
        Log.d(TAG, "Accessibility Service connected!");
        instance = this;
        handler = new Handler(Looper.getMainLooper());
        startRepeatingTask();
    }

    void startHTTPServer(){
        if(!isServerRunning) {
            ServiceManager serviceManager = Toothpick
                    .openScope("APP_SCOPE")
                    .getInstance(ServiceManager.class);
            server = new MyHttpServer(SERVER_PORT,getApplicationContext(),serviceManager);
            server.startServer();
            isServerRunning = true;
        }
    }

    void stopHTTPServer(){
        if (server != null) {
            server.stopServer();
            isServerRunning = false;
        }
    }

    private void startRepeatingTask() {
        runnable = new Runnable() {
            @Override
            public void run() {
                if (AppPreference.getBool(AppPreference.KEY.STREAM_STARTED, false) || AppPreference.getBool(AppPreference.KEY.RECORDING_STARTED, false)) {
                    performTask(); // Your task here
                }
                if (handler != null) {
                    handler.postDelayed(this, INTERVAL);
                }else {
                    handler = new Handler(Looper.getMainLooper());
                    if (runnable != null) {
                        handler.postDelayed(runnable, INTERVAL); // Start the task
                    }
                }
            }
        };
        if (handler != null) {
            handler.postDelayed(runnable, INTERVAL); // Start the task
        }else {
            handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(runnable, INTERVAL); // Start the task
        }
    }

    private void performTask() {
        Log.e(TAG, "performTask: ");
        // Add the code for the task to perform every 5 minutes
        restartApp(getApplicationContext());
    }

    public void restartApp(Context context) {
        try {
            // 1. Cleanup resources
            cleanupResources();
            // 2. Disable your Accessibility Service
            ComponentName componentName = new ComponentName(context, MyAccessibilityService.class);
            context.getPackageManager().setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
            );
            // 3. Save any prefs/flags needed for post-restart handling
            AppPreference.setBool(AppPreference.KEY.IS_RETURN_FOREGROUND, true);
            AppPreference.setBool(AppPreference.KEY.IS_RESTART_APP, true);
            // 4. Schedule an Alarm to relaunch the SplashActivity
            Intent restartIntent = new Intent(context, RestartActivity.class);
            restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            PendingIntent restartPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    restartIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                // Schedule the restart ~1 second from now
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + 1000,
                        restartPendingIntent
                );
            }

            context.getPackageManager().setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
            );
            Log.e(TAG, "restartingApp: " + "Accessbility restating app");
            // 5. Kill the current process so that AlarmManager can restart it.
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cleanupResources() {
        if (MainActivity.getInstance() != null) {
            MainActivity.instance.stopFragUSBService();
            MainActivity.instance.stopFragWifiService();
            MainActivity.instance.stopFragBgCast();
            MainActivity.instance.stopFragBgCamera();
            MainActivity.instance.stopService(ServiceType.BgAudio);
            MainActivity.instance.finishAffinity();
        }
    }

    public void restartAppForeground(Context context) {
        handler.postDelayed(() -> {
            // Your task here
            AppPreference.setBool(AppPreference.KEY.IS_RETURN_FOREGROUND, true);
            AppPreference.setBool(AppPreference.KEY.IS_RESTART_APP, true);
            Intent restartIntent = new Intent(getApplicationContext(), SplashActivity.class);
            restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(restartIntent);
            Log.d("DelayedTask", "Task executed after 2 seconds");
        }, 500); // 2000 milliseconds = 2 seconds
        handler.postDelayed(() -> {
            Intent intentNew = new Intent(Intent.ACTION_MAIN);
            intentNew.addCategory(Intent.CATEGORY_HOME);
            intentNew.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentNew);
            if (MainActivity.getInstance() != null) {
                MainActivity.getInstance().moveTaskToBack(true);
            }
            Log.d("DelayedTask", "Task executed after 20 seconds");
        }, 1000); // 2000 milliseconds = 2 seconds
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Accessibility Service interrupted");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(isServerRunning) {
            stopHTTPServer();
        }
        Log.d(TAG, "MyAccessibilityService onDestroy");
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
        if (handler != null) {
            handler = null;
        }

        if (instance != null) {
            instance = null;
        }

    }

    public boolean isMemoryUsageHigh(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            // Check if the system is low on memory
            if (memoryInfo.lowMemory) {
                return true;
            }
            Debug.MemoryInfo appMemoryInfo = new Debug.MemoryInfo();
            Debug.getMemoryInfo(appMemoryInfo);
            // Total private dirty memory in MB
            double totalMemoryUsageMb = appMemoryInfo.getTotalPrivateDirty() / 1024.0;
            // Total available system memory in MB
            double totalDeviceMemoryMb = memoryInfo.availMem / (1024.0 * 1024.0);
            // Calculate the percentage of memory usage
            double memoryUsagePercent = totalMemoryUsageMb / totalDeviceMemoryMb;
            // Threshold check
            return memoryUsagePercent > HIGH_MEMORY_THRESHOLD_PERCENT; // Replace 0.8 with your desired threshold
        }
        return false;
    }
}


