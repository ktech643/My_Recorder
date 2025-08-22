//package com.checkmate.android.util;
//import android.app.AlarmManager;
//import android.app.PendingIntent;
//import android.content.Context;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.os.Handler;
//import android.os.Looper;
//import android.util.Log;
//
//import androidx.annotation.NonNull;
//import androidx.work.Worker;
//import androidx.work.WorkerParameters;
//
//import com.checkmate.android.AppPreference;
//import com.checkmate.android.RestartActivity;
//import com.checkmate.android.ui.activity.SplashActivity;
//
//public class RestartWorker extends Worker {
//
//    private static final String TAG = "RestartWorker";
//
//    public RestartWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
//        super(context, workerParams);
//    }
//
//    @NonNull
//    @Override
//    public Result doWork() {
//        Log.e(TAG, "doWork: Checking if we should restart app ...");
//
//        // 1. Check your conditions
//        boolean isStreamStarted = AppPreference.getBool(AppPreference.KEY.STREAM_STARTED, false);
//        boolean isRecordingStarted = AppPreference.getBool(AppPreference.KEY.RECORDING_STARTED, false);
//
//        if (isStreamStarted || isRecordingStarted) {
//            // 2. Perform the same logic you had in `performTask()`
//            performTask();
//        }
//        // Return success (or failure) based on whether the work succeeded or not.
//        return Result.success();
//    }
//
//    private void performTask() {
//        Log.e(TAG, "performTask: Attempting to restart the app...");
//        // Your previous logic to restart the app can be used here.
//        // For example:
//        restartAppForeground(getApplicationContext());
//    }
//
//    private void restartApp(Context context) {
//        try {
//            // 1. Cleanup resources if needed
//            cleanupResources();
//            // 3. Save any prefs/flags needed for post-restart handling
//            AppPreference.setBool(AppPreference.KEY.IS_RETURN_FOREGROUND, true);
//            AppPreference.setBool(AppPreference.KEY.IS_RESTART_APP, true);
//            Intent restartIntent = new Intent(context, RestartActivity.class);
//            restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
//                    | Intent.FLAG_ACTIVITY_NEW_TASK
//                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//
//            PendingIntent restartPendingIntent = PendingIntent.getActivity(
//                    context,
//                    0,
//                    restartIntent,
//                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
//            );
//            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//            if (alarmManager != null) {
//                // Schedule the restart ~1 second from now
//                alarmManager.set(
//                        AlarmManager.RTC_WAKEUP,
//                        System.currentTimeMillis() + 1000,
//                        restartPendingIntent
//                );
//            }
//            //    The kill process approach:
//            android.os.Process.killProcess(android.os.Process.myPid());
//            System.exit(0);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void restartAppForeground(Context context) {
//        Handler handler = new Handler(Looper.getMainLooper());
//        handler.postDelayed(() -> {
//            // Your task here
//            AppPreference.setBool(AppPreference.KEY.IS_RETURN_FOREGROUND , true);
//            AppPreference.setBool(AppPreference.KEY.IS_RESTART_APP , true);
//            Intent restartIntent = new Intent(context, SplashActivity.class);
//            restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//            context.startActivity(restartIntent);
//            Log.d("DelayedTask", "Task executed after 2 seconds");
//        }, 500); // 2000 milliseconds = 2 seconds
//        handler.postDelayed(() -> {
//            Intent intentNew = new Intent(Intent.ACTION_MAIN);
//            intentNew.addCategory(Intent.CATEGORY_HOME);
//            intentNew.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            context.startActivity(intentNew);
//            if (MainActivity.getInstance() != null) {
//                MainActivity.getInstance().moveTaskToBack(true);
//            }
//            Log.d("DelayedTask", "Task executed after 20 seconds");
//        }, 5000); // 2000 milliseconds = 2 seconds
//    }
//
//    public void restartMainActivityOnly(Context context) {
//        Handler handler = new Handler(Looper.getMainLooper());
//        AppPreference.setBool(AppPreference.KEY.IS_RETURN_FOREGROUND , true);
//        AppPreference.setBool(AppPreference.KEY.IS_RESTART_APP , true);
//        MainActivity.instance.finishAffinity();
//        Intent intent = new Intent(context, SplashActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
//                | Intent.FLAG_ACTIVITY_NEW_TASK
//                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//
//        context.startActivity(intent);
//    }
//
//    private void cleanupResources() {
//        // If you have any static references or singletons that need shutting down, do it here.
//         if (MainActivity.getInstance() != null) {
//            MainActivity.instance.stopFragUSBService();
//            MainActivity.instance.stopFragWifiService();
//            MainActivity.instance.stopFragBgCast();
//            MainActivity.instance.stopFragBgCamera();
//            MainActivity.instance.finishAffinity();
//         }
//    }
//}
