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
import com.checkmate.android.util.ANRProtectionManager;
import com.checkmate.android.util.GlobalResourcePool;
import com.checkmate.android.util.StartupOptimizer;

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
        
        try {
            Log.d("MyApp", "🚀 Starting optimized app initialization with ANR protection");
            
            mContext = getApplicationContext();

            // Initialize global resource pool for optimal performance
            GlobalResourcePool.getInstance();
            
            // Initialize startup optimizer first
            StartupOptimizer.getInstance().initializeOptimizedStartup(this);
            
            // Initialize ANR protection manager
            ANRProtectionManager anrManager = ANRProtectionManager.getInstance();
            
            // Configure Toothpick with ANR protection
            anrManager.executeCriticalOperation("configure_toothpick", () -> {
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
                
                return null;
            }, null);
            
            // Initialize preferences with ANR protection
            anrManager.executeCriticalOperation("initialize_preferences", () -> {
                SharedPreferences pref = anrManager.getSafeSharedPreferences(mContext, "default_prefs");
                AppPreference.initialize(pref);
                return null;
            }, null);
            
            // Initialize database with ANR protection
            anrManager.executeDatabaseOperation("initialize_database", () -> {
                new DBManager(mContext);
                return null;
            }, null);
            
            mContext = this;

            // Initialize window size with ANR protection
            anrManager.executeCriticalOperation("initialize_window_size", () -> {
                WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
                Display display = wm.getDefaultDisplay();
                mScreenWidth = display.getWidth();
                mScreenHeight = display.getHeight();
                return null;
            }, null);
            
            Log.i("MyApp", "✅ App initialization completed successfully with ANR protection");
            
        } catch (Exception e) {
            Log.e("MyApp", "💥 Error during app initialization", e);
            // Fallback initialization
            initializeFallback();
        }

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
                currentActivity = activity;
                Log.d("MyApp", "📱 Activity created: " + activity.getClass().getSimpleName());
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                try {
                    if (++activityReferences == 1 && !isActivityChangingConfigurations) {
                        // App enters foreground
                        ANRProtectionManager.getInstance().executeOnMainThreadSafely(
                            "app_foreground",
                            () -> {
                                AppPreference.setBool(AppPreference.KEY.IS_APP_BACKGROUND, false);
                                Log.i("MyApp", "📱 App entered FOREGROUND");
                            }
                        );
                    }
                    Log.d("MyApp", "📱 Activity started: " + activity.getClass().getSimpleName() + ", refs: " + activityReferences);
                } catch (Exception e) {
                    Log.e("MyApp", "Error in onActivityStarted", e);
                }
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                Log.d("MyApp", "📱 Activity resumed: " + activity.getClass().getSimpleName());
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                Log.d("MyApp", "📱 Activity paused: " + activity.getClass().getSimpleName());
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                try {
                    isActivityChangingConfigurations = activity.isChangingConfigurations();
                    if (--activityReferences == 0 && !isActivityChangingConfigurations) {
                        // App enters background
                        ANRProtectionManager.getInstance().executeOnMainThreadSafely(
                            "app_background",
                            () -> {
                                AppPreference.setBool(AppPreference.KEY.IS_APP_BACKGROUND, true);
                                Log.i("MyApp", "📱 App entered BACKGROUND");
                            }
                        );
                    }
                    Log.d("MyApp", "📱 Activity stopped: " + activity.getClass().getSimpleName() + ", refs: " + activityReferences);
                } catch (Exception e) {
                    Log.e("MyApp", "Error in onActivityStopped", e);
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                if (currentActivity == activity) {
                    currentActivity = null;
                }
                Log.d("MyApp", "📱 Activity destroyed: " + activity.getClass().getSimpleName());
            }
        });
    }
    
    /**
     * Fallback initialization for error cases
     */
    private void initializeFallback() {
        try {
            Log.w("MyApp", "🚨 Using fallback initialization");
            
            // Basic initialization without optimization
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
            AppPreference.initialize(pref);
            
            new DBManager(mContext);
            mContext = this;
            
            WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            mScreenWidth = display.getWidth();
            mScreenHeight = display.getHeight();
            
            Log.w("MyApp", "✅ Fallback initialization completed");
            
        } catch (Exception e) {
            Log.e("MyApp", "💥 Error in fallback initialization", e);
        }
    }
    
    public static Context getContext() {
        return mContext.getApplicationContext();
    }
    
    public Activity getCurrentActivity() {
        return currentActivity;
    }
    
    /**
     * Get performance statistics
     */
    public String getAppPerformanceStats() {
        try {
            return "App Performance: " + 
                   ANRProtectionManager.getInstance().getPerformanceStats() + 
                   ", " + StartupOptimizer.getInstance().getStartupSummary() +
                   ", " + GlobalResourcePool.getInstance().getPerformanceStats();
        } catch (Exception e) {
            return "Performance stats unavailable";
        }
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        try {
            Log.w("MyApp", "🚨 Low memory warning - performing emergency cleanup");
            GlobalResourcePool.getInstance().emergencyCleanup();
            ANRProtectionManager.getInstance().emergencyCleanup();
            StartupOptimizer.getInstance().emergencyCleanup();
        } catch (Exception e) {
            Log.e("MyApp", "Error during low memory cleanup", e);
        }
    }
}
