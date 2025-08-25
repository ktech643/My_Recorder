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
import com.checkmate.android.util.AppLogger;
import com.checkmate.android.util.CrashHandler;
import com.checkmate.android.util.ANRWatchdog;
import com.checkmate.android.util.ThreadUtils;

import toothpick.Scope;
import toothpick.Toothpick;
import toothpick.configuration.Configuration;

public class MyApp extends Application {
    private static final String TAG = "MyApp";
    public static Context mContext;
    Activity currentActivity;
    public static int mScreenWidth;
    public static int mScreenHeight;
    private static int activityReferences = 0;
    private static boolean isActivityChangingConfigurations = false;
    private ANRWatchdog anrWatchdog;

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = getApplicationContext();
        
        // Initialize crash handler and logger first
        initializeSafetyComponents();


        // Configure Toothpick with error handling
        try {
            if (BuildConfig.DEBUG) {
                Toothpick.setConfiguration(Configuration.forDevelopment());
            } else {
                Toothpick.setConfiguration(Configuration.forProduction());
            }
            // Initialize scopes
            Toothpick.openScope(this).installModules(new GraphicsModule());
            // Open root scope and install modules
            Scope appScope = Toothpick.openScope("APP_SCOPE");
            appScope.installModules(new ServiceModule());
        } catch (Exception e) {
            AppLogger.e(TAG, "Error configuring Toothpick", e);
        }


        // initialize preferences with error handling
        try {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
            AppPreference.initialize(pref);
        } catch (Exception e) {
            AppLogger.e(TAG, "Error initializing preferences", e);
        }

        // Initialize database with error handling
        try {
            new DBManager(mContext);
        } catch (Exception e) {
            AppLogger.e(TAG, "Error initializing database", e);
        }
        
        mContext = this;

        // Get window size with null safety
        try {
            WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) {
                Display display = wm.getDefaultDisplay();
                if (display != null) {
                    mScreenWidth = display.getWidth();
                    mScreenHeight = display.getHeight();
                } else {
                    AppLogger.w(TAG, "Display is null, using default values");
                    mScreenWidth = 1920;
                    mScreenHeight = 1080;
                }
            } else {
                AppLogger.w(TAG, "WindowManager is null, using default values");
                mScreenWidth = 1920;
                mScreenHeight = 1080;
            }
        } catch (Exception e) {
            AppLogger.e(TAG, "Error getting screen dimensions", e);
            mScreenWidth = 1920;
            mScreenHeight = 1080;
        }

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
                    AppLogger.d("AppState", "App in FOREGROUND");
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
                    AppLogger.d("AppState", "App in BACKGROUND");
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {}
        });
    }
    private void initializeSafetyComponents() {
        // Initialize logger
        AppLogger.init(this);
        AppLogger.i(TAG, "Application starting...");
        
        // Initialize crash handler
        CrashHandler.getInstance().init(this);
        
        // Start ANR watchdog
        anrWatchdog = new ANRWatchdog(5000, error -> {
            AppLogger.e(TAG, "ANR detected!", error);
            // Try to recover by clearing any pending operations
            System.gc();
        });
        anrWatchdog.start();
        
        AppLogger.i(TAG, "Safety components initialized");
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        if (anrWatchdog != null) {
            anrWatchdog.stopWatchdog();
        }
        AppLogger.shutdown();
        CrashHandler.getInstance().shutdown();
        ThreadUtils.shutdown();
    }
    
    public static Context getContext() {
        return mContext != null ? mContext.getApplicationContext() : null;
    }
    
    public Activity getCurrentActivity() {
        return currentActivity;
    }
}
