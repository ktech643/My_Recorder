package com.checkmate.android;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.checkmate.android.database.DBManager;
import com.checkmate.android.service.SharedEGL.GraphicsModule;
import com.checkmate.android.util.HttpServer.ServiceModule;
import com.checkmate.android.util.InternalLogger;
import com.checkmate.android.util.ANRDetector;
import com.checkmate.android.util.ThreadSafetyUtils;

import toothpick.Scope;
import toothpick.Toothpick;
import toothpick.configuration.Configuration;

public class MyApp extends Application {
    public static Context mContext;
    Activity currentActivity;
    public static int mScreenWidth;
    public static int mScreenHeight;
    private static int activityReferences = 0;
    private static boolean isActivityChangingConfigurations = false;

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = getApplicationContext();

        // Initialize thread safety and logging systems early
        try {
            InternalLogger.initialize(this);
            ANRDetector.initialize(this);
            InternalLogger.logInfo("MyApp", "Application started with thread safety systems");
        } catch (Exception e) {
            Log.e("MyApp", "Failed to initialize thread safety systems", e);
        }

        // Configure Toothpick
        if (BuildConfig.DEBUG) {
            Toothpick.setConfiguration(Configuration.forDevelopment());
        }else {
            Toothpick.setConfiguration(Configuration.forProduction());
        }
        // Initialize scopes
        Toothpick.openScope(this).installModules(new GraphicsModule());
        // Open root scope and install modules
        Scope appScope = Toothpick.openScope("APP_SCOPE");
        appScope.installModules(new ServiceModule());


        // initialize
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
        AppPreference.initialize(pref);

        new DBManager(mContext);
        mContext =this;

        // window size
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        mScreenWidth = display.getWidth();
        mScreenHeight = display.getHeight();

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
                currentActivity = activity;
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                if (++activityReferences == 1 && !isActivityChangingConfigurations) {
                    // App enters foreground
                    AppPreference.setBool(AppPreference.KEY.IS_APP_BACKGROUND, false);
                    Log.d("AppState", "App in FOREGROUND");
                }
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {}

            @Override
            public void onActivityPaused(@NonNull Activity activity) {}

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                isActivityChangingConfigurations = activity.isChangingConfigurations();
                if (--activityReferences == 0 && !isActivityChangingConfigurations) {
                    // App enters background
                    AppPreference.setBool(AppPreference.KEY.IS_APP_BACKGROUND, true);
                    Log.d("AppState", "App in BACKGROUND");
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {}
        });
    }
    public static Context getContext() {
        return mContext.getApplicationContext();
    }
    public Activity getCurrentActivity() {
        return currentActivity;
    }
}
