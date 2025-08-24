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
import com.checkmate.android.util.ANRWatchdog;
import com.checkmate.android.util.BackgroundTaskManager;
import com.checkmate.android.util.MainThreadGuard;

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
        
        // Initialize crash logging and ANR monitoring early
        try {
            CrashLogger.initialize(mContext);
            
            // Initialize ANR Watchdog with listener
            ANRWatchdog anrWatchdog = ANRWatchdog.getInstance();
            anrWatchdog.setANRListener(new ANRWatchdog.ANRListener() {
                @Override
                public void onANRDetected(long duration) {
                    Log.w("MyApp", "ANR detected in app - duration: " + duration + "ms");
                    // Attempt automatic recovery
                    anrWatchdog.triggerRecovery();
                }
                
                @Override
                public void onANRResolved() {
                    Log.i("MyApp", "ANR resolved - app is responsive again");
                }
                
                @Override
                public void onCriticalANR(long duration) {
                    Log.e("MyApp", "Critical ANR detected - duration: " + duration + "ms");
                    // Could implement more aggressive recovery here
                }
            });
            anrWatchdog.start();
            
            // Initialize background task manager
            BackgroundTaskManager.getInstance();
            
            // Start main thread monitoring
            MainThreadGuard.startMainThreadMonitoring();
            
            CrashLogger.logEvent("MyApp", "All safety systems initialized successfully");
            
        } catch (Exception e) {
            Log.e("MyApp", "Failed to initialize crash reporting/ANR monitoring", e);
            // Even if initialization fails, log it
            try {
                CrashLogger.logCrash("MyApp.onCreate", e);
            } catch (Exception logError) {
                Log.e("MyApp", "Failed to log initialization error", logError);
            }
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
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        shutdown();
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        CrashLogger.logEvent("System", "Low memory warning received");
        
        // Could implement memory cleanup here
        System.gc();
    }
    
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        CrashLogger.logEvent("System", "Memory trim requested - level: " + level);
        
        // Could implement different levels of memory cleanup based on trim level
        if (level >= TRIM_MEMORY_MODERATE) {
            System.gc();
        }
    }
    
    /**
     * Shutdown all safety systems
     */
    private void shutdown() {
        try {
            CrashLogger.logEvent("MyApp", "Shutting down safety systems");
            
            // Stop monitoring
            MainThreadGuard.stopMainThreadMonitoring();
            ANRWatchdog.getInstance().stop();
            
            // Shutdown background task manager
            BackgroundTaskManager.getInstance().shutdown();
            
            // Final log before shutdown
            CrashLogger.logEvent("MyApp", "Shutdown completed");
            CrashLogger.shutdown();
            
        } catch (Exception e) {
            Log.e("MyApp", "Error during shutdown", e);
        }
    }
    public static Context getContext() {
        return mContext.getApplicationContext();
    }
    public Activity getCurrentActivity() {
        return currentActivity;
    }
}
