package com.checkmate.android;

import android.app.Application;
import android.content.Context;

import com.checkmate.android.anr.ANRWatchdog;
import com.checkmate.android.logging.InternalLogger;

/**
 * Custom Application class to initialize logging and ANR detection
 */
public class CheckmateApplication extends Application {
    private static final String TAG = "CheckmateApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
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
        
        // Initialize SharedPreferences
        initializePreferences();
        
        // Set up crash handler
        setupCrashHandler();
    }
    
    private void initializePreferences() {
        try {
            AppPreference.initialize(getSharedPreferences("app_prefs", Context.MODE_PRIVATE));
        } catch (Exception e) {
            InternalLogger.e(TAG, "Failed to initialize preferences", e);
        }
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