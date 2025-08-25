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

import com.checkmate.android.anr.ANRWatchdog;
import com.checkmate.android.database.DBManager;
import com.checkmate.android.logging.InternalLogger;
import com.checkmate.android.service.SharedEGL.GraphicsModule;
import com.checkmate.android.util.HttpServer.ServiceModule;

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

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = getApplicationContext();
        
        // Initialize logging first
        InternalLogger.initialize(this);
        InternalLogger.i(TAG, "Application starting");
        
        // Initialize ANR watchdog
        ANRWatchdog.initialize();
        ANRWatchdog.getInstance().setANRListener(new ANRWatchdog.ANRListener() {
            @Override
            public void onAppNotResponding(ANRWatchdog.ANRError error) {
                InternalLogger.e(TAG, "ANR detected", error);
                // You can add custom recovery logic here
                // For example, restart problematic services or clear certain states
            }
            
            @Override
            public void onANRRecovered() {
                InternalLogger.i(TAG, "ANR recovered");
            }
        });
        ANRWatchdog.getInstance().startWatching();
        
        // Set up crash handler
        setupCrashHandler();


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
                    InternalLogger.d(TAG, "App in FOREGROUND");
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
                    InternalLogger.d(TAG, "App in BACKGROUND");
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
    
    private void setupCrashHandler() {
        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            InternalLogger.e(TAG, "Uncaught exception in thread: " + thread.getName(), throwable);
            
            // Try to save critical state before crashing
            try {
                AppPreference.setBool(AppPreference.KEY.APP_FORCE_QUIT, true);
            } catch (Exception e) {
                // Ignore errors during crash handling
            }
            
            // Call default handler
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        });
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        
        // Stop ANR watchdog
        ANRWatchdog.getInstance().stopWatching();
        
        // Shutdown logging
        InternalLogger.getInstance().shutdown();
        
        // Shutdown preferences
        AppPreference.shutdown();
    }
}
