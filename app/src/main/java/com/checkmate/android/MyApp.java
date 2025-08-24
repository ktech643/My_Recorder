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
import com.checkmate.android.util.CrashLogger;
import com.checkmate.android.util.AnrDetector;

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


        // Initialize crash logging system first
        CrashLogger.initialize(mContext);
        CrashLogger.i("MyApp", "Application starting - initializing core systems");
        
        // Initialize ANR detector
        AnrDetector.getInstance().start();
        
        // initialize preferences
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
        AppPreference.initialize(pref);
        CrashLogger.d("MyApp", "AppPreference initialized");

        new DBManager(mContext);
        CrashLogger.d("MyApp", "DBManager initialized");
        
        mContext = this;

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
                    CrashLogger.d("AppState", "App in FOREGROUND");
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
                    CrashLogger.d("AppState", "App in BACKGROUND");
                    Log.d("AppState", "App in BACKGROUND");
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                if (activity == currentActivity) {
                    currentActivity = null;
                }
            }
        });
        
        CrashLogger.i("MyApp", "Application initialization completed successfully");
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        
        // Cleanup logging system
        CrashLogger.i("MyApp", "Application terminating - cleaning up resources");
        AnrDetector.getInstance().stop();
        
        if (CrashLogger.getInstance() != null) {
            CrashLogger.getInstance().shutdown();
        }
    }
    
    public static Context getContext() {
        return mContext != null ? mContext.getApplicationContext() : null;
    }
    
    public Activity getCurrentActivity() {
        return currentActivity;
    }
}
