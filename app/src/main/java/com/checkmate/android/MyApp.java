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
import com.checkmate.android.util.ANRSafeHelper;
import com.checkmate.android.util.CriticalComponentsMonitor;
import com.checkmate.android.util.CrashLogger;
import com.checkmate.android.util.DynamicSettingsManager;

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
    private InternalLogger internalLogger;

    @Override
    public void onCreate() {
        try {
            super.onCreate();
            
            mContext = getApplicationContext();
            
            // Initialize logging system first
            initializeLoggingSystem();
            
            InternalLogger.i(TAG, "CheckMate Android App starting up");
            InternalLogger.i(TAG, "App Version: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
            InternalLogger.i(TAG, "Debug Mode: " + BuildConfig.DEBUG);
            
            // Initialize preferences early
            initializePreferencesSafely();
            
            // Configure Toothpick dependency injection
            configureToothpickSafely();
            
            // Initialize database
            initializeDatabaseSafely();
            
            // Get screen dimensions
            initializeScreenDimensionsSafely();
            
            // Set up lifecycle callbacks
            setupActivityLifecycleCallbacks();
            
            // Initialize monitoring systems
            initializeMonitoringSystems();
            
            InternalLogger.i(TAG, "CheckMate Android App initialization completed successfully");
            
        
        CrashLogger.initialize(mContext);
        DynamicSettingsManager.initialize(this);} catch (Exception e) {
            // Even if internal logger fails, try to log to system
            Log.e(TAG, "Critical error during app initialization", e);
            
            // Try to initialize InternalLogger if not already done
            try {
                if (internalLogger == null) {
                    internalLogger = InternalLogger.getInstance(getApplicationContext());
                }
                InternalLogger.e(TAG, "Critical error during app initialization", e);
            } catch (Exception loggerError) {
                Log.e(TAG, "Failed to initialize logger during error handling", loggerError);
            }
            
            // Continue with minimal initialization to prevent complete app failure
            try {
                performMinimalInitialization();
            } catch (Exception minimalError) {
                Log.e(TAG, "Even minimal initialization failed", minimalError);
                // At this point, let the app crash as it's not recoverable
                throw new RuntimeException("App initialization failed completely", e);
            }
        }
    }
    
    /**
     * Initialize the internal logging system
     */
    private void initializeLoggingSystem() {
        try {
            // Initialize InternalLogger as early as possible
            internalLogger = InternalLogger.getInstance(getApplicationContext());
            Log.i(TAG, "Internal logging system initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize internal logging system", e);
            // Continue without internal logging
        }
    }
    
    /**
     * Initialize SharedPreferences safely
     */
    private void initializePreferencesSafely() {
        ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
            try {
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
                if (pref != null) {
                    AppPreference.initialize(pref);
                    InternalLogger.i(TAG, "SharedPreferences initialized successfully");
                } else {
                    InternalLogger.e(TAG, "Failed to get default SharedPreferences");
                }
                return true;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error initializing SharedPreferences", e);
                return false;
            }
        }, false);
    }
    
    /**
     * Configure Toothpick dependency injection safely
     */
    private void configureToothpickSafely() {
        ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
            try {
                // Configure Toothpick
                if (BuildConfig.DEBUG) {
                    Toothpick.setConfiguration(Configuration.forDevelopment());
                    InternalLogger.d(TAG, "Toothpick configured for development");
                } else {
                    Toothpick.setConfiguration(Configuration.forProduction());
                    InternalLogger.d(TAG, "Toothpick configured for production");
                }
                
                // Initialize scopes
                Toothpick.openScope(this).installModules(new GraphicsModule());
                
                // Open root scope and install modules
                Scope appScope = Toothpick.openScope("APP_SCOPE");
                appScope.installModules(new ServiceModule());
                
                InternalLogger.i(TAG, "Toothpick dependency injection configured successfully");
                return true;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error configuring Toothpick", e);
                return false;
            }
        }, false);
    }
    
    /**
     * Initialize database safely
     */
    private void initializeDatabaseSafely() {
        ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
            try {
                new DBManager(mContext);
                InternalLogger.i(TAG, "Database initialized successfully");
                return true;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error initializing database", e);
                return false;
            }
        }, false);
    }
    
    /**
     * Initialize screen dimensions safely
     */
    private void initializeScreenDimensionsSafely() {
        ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
            try {
                WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
                if (wm != null) {
                    Display display = wm.getDefaultDisplay();
                    if (display != null) {
                        mScreenWidth = display.getWidth();
                        mScreenHeight = display.getHeight();
                        InternalLogger.i(TAG, "Screen dimensions: " + mScreenWidth + "x" + mScreenHeight);
                    } else {
                        InternalLogger.w(TAG, "Display is null, using default dimensions");
                        mScreenWidth = 1080;
                        mScreenHeight = 1920;
                    }
                } else {
                    InternalLogger.w(TAG, "WindowManager is null, using default dimensions");
                    mScreenWidth = 1080;
                    mScreenHeight = 1920;
                }
                return true;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error getting screen dimensions", e);
                // Set default values
                mScreenWidth = 1080;
                mScreenHeight = 1920;
                return false;
            }
        }, false);
    }
    
    /**
     * Set up activity lifecycle callbacks with comprehensive monitoring
     */
    private void setupActivityLifecycleCallbacks() {
        try {

            registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
                    try {
                        currentActivity = activity;
                        InternalLogger.d(TAG, "Activity created: " + activity.getClass().getSimpleName());
                    } catch (Exception e) {
                        InternalLogger.e(TAG, "Error in onActivityCreated", e);
                    }
                }

                @Override
                public void onActivityStarted(@NonNull Activity activity) {
                    try {
                        if (++activityReferences == 1 && !isActivityChangingConfigurations) {
                            // App enters foreground
                            ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
                                AppPreference.setBool(AppPreference.KEY.IS_APP_BACKGROUND, false);
                                return true;
                            }, false);
                            
                            InternalLogger.i(TAG, "App entered FOREGROUND");
                            
                            // Reset restart count when app comes to foreground successfully
                            if (AppPreference.isInAnrRecoveryMode()) {
                                ANRSafeHelper.getInstance().exitRecoveryMode();
                                AppPreference.resetRestartCount();
                                InternalLogger.i(TAG, "Exited ANR recovery mode - app stable");
                            }
                        }
                        InternalLogger.d(TAG, "Activity started: " + activity.getClass().getSimpleName() + ", refs: " + activityReferences);
                    } catch (Exception e) {
                        InternalLogger.e(TAG, "Error in onActivityStarted", e);
                    }
                }

                @Override
                public void onActivityResumed(@NonNull Activity activity) {
                    try {
                        InternalLogger.d(TAG, "Activity resumed: " + activity.getClass().getSimpleName());
                    } catch (Exception e) {
                        InternalLogger.e(TAG, "Error in onActivityResumed", e);
                    }
                }

                @Override
                public void onActivityPaused(@NonNull Activity activity) {
                    try {
                        InternalLogger.d(TAG, "Activity paused: " + activity.getClass().getSimpleName());
                    } catch (Exception e) {
                        InternalLogger.e(TAG, "Error in onActivityPaused", e);
                    }
                }

                @Override
                public void onActivityStopped(@NonNull Activity activity) {
                    try {
                        isActivityChangingConfigurations = activity.isChangingConfigurations();
                        if (--activityReferences == 0 && !isActivityChangingConfigurations) {
                            // App enters background
                            ANRSafeHelper.getInstance().executeWithANRProtection(() -> {
                                AppPreference.setBool(AppPreference.KEY.IS_APP_BACKGROUND, true);
                                return true;
                            }, false);
                            
                            InternalLogger.i(TAG, "App entered BACKGROUND");
                            
                            // Flush logs when app goes to background
                            if (internalLogger != null) {
                                internalLogger.flush();
                            }
                        }
                        InternalLogger.d(TAG, "Activity stopped: " + activity.getClass().getSimpleName() + ", refs: " + activityReferences);
                    } catch (Exception e) {
                        InternalLogger.e(TAG, "Error in onActivityStopped", e);
                    }
                }

                @Override
                public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
                    try {
                        InternalLogger.d(TAG, "Activity save instance state: " + activity.getClass().getSimpleName());
                    } catch (Exception e) {
                        InternalLogger.e(TAG, "Error in onActivitySaveInstanceState", e);
                    }
                }

                @Override
                public void onActivityDestroyed(@NonNull Activity activity) {
                    try {
                        if (currentActivity == activity) {
                            currentActivity = null;
                        }
                        InternalLogger.d(TAG, "Activity destroyed: " + activity.getClass().getSimpleName());
                    } catch (Exception e) {
                        InternalLogger.e(TAG, "Error in onActivityDestroyed", e);
                    }
                }
            });
            
            InternalLogger.i(TAG, "Activity lifecycle callbacks registered successfully");
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error setting up activity lifecycle callbacks", e);
        }
    }
    
    /**
     * Perform minimal initialization when full initialization fails
     */
    private void performMinimalInitialization() {
        try {
            mContext = getApplicationContext();
            
            // Try to at least initialize preferences
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
            if (pref != null) {
                AppPreference.initialize(pref);
            }
            
            // Set default screen dimensions
            mScreenWidth = 1080;
            mScreenHeight = 1920;
            
            Log.i(TAG, "Minimal initialization completed");
        } catch (Exception e) {
            Log.e(TAG, "Minimal initialization failed", e);
            throw e;
        }
    }
    
    @Override
    public void onTerminate() {
        try {
            InternalLogger.i(TAG, "App terminating");
            
            // Cleanup ANR helper
            ANRSafeHelper.getInstance().shutdown();
            
            // Shutdown internal logger
            if (internalLogger != null) {
                internalLogger.shutdown();
            }
            
            super.onTerminate();
        } catch (Exception e) {
            Log.e(TAG, "Error during app termination", e);
            super.onTerminate();
        }
    }
    public static Context getContext() {
        try {
            return mContext != null ? mContext.getApplicationContext() : null;
        } catch (Exception e) {
            Log.e(TAG, "Error getting application context", e);
            return null;
        }
    }
    
    public Activity getCurrentActivity() {
        return currentActivity;
    }
    
    /**
     * Get current activity safely with null check
     */
    public Activity getCurrentActivitySafe() {
        try {
            Activity activity = getCurrentActivity();
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                return activity;
            }
        } catch (Exception e) {
            InternalLogger.w(TAG, "Error getting current activity", e);
        }
        return null;
    }
    
    /**
     * Check if app is currently in foreground
     */
    public static boolean isAppInForeground() {
        return activityReferences > 0;
    }
    
    /**
     * Initialize monitoring systems for thread safety and ANR detection
     */
    private void initializeMonitoringSystems() {
        try {
            InternalLogger.i(TAG, "Initializing monitoring systems");
            
            // Initialize Critical Components Monitor
            CriticalComponentsMonitor.getInstance(this).startMonitoring();
            
            
            InternalLogger.i(TAG, "Monitoring systems initialized successfully");
            
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error initializing monitoring systems", e);
            // Continue app startup even if monitoring fails
        }
    }
}
