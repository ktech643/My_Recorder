package com.checkmate.android.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;

/**
 * Global crash reporter that captures all uncaught exceptions
 * and saves them to local files for debugging
 */
public class CrashReporter implements UncaughtExceptionHandler {
    private static final String TAG = "CrashReporter";

    private final Context context;
    private final UncaughtExceptionHandler defaultHandler;
    private static CrashReporter instance;

    private CrashReporter(Context context) {
        this.context = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    /**
     * Initialize the crash reporter
     */
    public static void init(Context context) {
        if (instance == null) {
            instance = new CrashReporter(context);
            Thread.setDefaultUncaughtExceptionHandler(instance);
            Logger.i(TAG, "CrashReporter initialized");
        }
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        try {
            // Log the crash
            Logger.logCrash(thread, throwable);

            // Collect device and app information
            String deviceInfo = collectDeviceInfo();
            Logger.e(TAG, "Device Info:\n" + deviceInfo);

            // Give some time for the log to be written
            Thread.sleep(100);

        } catch (Exception e) {
            Log.e(TAG, "Error in crash reporter", e);
        } finally {
            // Call the default handler to show crash dialog
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            } else {
                // If no default handler, kill the process
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            }
        }
    }

    /**
     * Manually report an exception (non-fatal)
     */
    public static void reportException(Throwable throwable) {
        reportException("Non-fatal Exception", throwable);
    }

    /**
     * Manually report an exception with custom message
     */
    public static void reportException(String message, Throwable throwable) {
        Logger.e(TAG, message, throwable);

        // Also log as a non-fatal crash for tracking
        String crashInfo = buildNonFatalCrashInfo(message, throwable);
        Logger.e("NON_FATAL_CRASH", crashInfo);
    }

    /**
     * Collect device information
     */
    private String collectDeviceInfo() {
        StringBuilder info = new StringBuilder();

        try {
            // App version
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            info.append("App Version: ").append(pi.versionName)
                    .append(" (").append(pi.versionCode).append(")\n");
        } catch (Exception e) {
            info.append("App Version: Unknown\n");
        }

        // Device info
        info.append("Device: ").append(Build.MANUFACTURER)
                .append(" ").append(Build.MODEL).append("\n");
        info.append("Android Version: ").append(Build.VERSION.RELEASE)
                .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        info.append("Build: ").append(Build.DISPLAY).append("\n");

        // Memory info
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1048576L;
        long totalMemory = runtime.totalMemory() / 1048576L;
        long freeMemory = runtime.freeMemory() / 1048576L;
        long usedMemory = totalMemory - freeMemory;

        info.append("Memory: Used ").append(usedMemory).append("MB / ")
                .append("Total ").append(totalMemory).append("MB / ")
                .append("Max ").append(maxMemory).append("MB\n");

        return info.toString();
    }

    /**
     * Build non-fatal crash info
     */
    private static String buildNonFatalCrashInfo(String message, Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== NON-FATAL EXCEPTION ==========\n");
        sb.append("Message: ").append(message).append("\n");
        sb.append("Exception: ").append(throwable.getClass().getName()).append("\n");
        sb.append("Stack Trace:\n");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        sb.append(sw.toString());

        sb.append("=========================================\n");
        return sb.toString();
    }

    /**
     * Test crash (for debugging purposes only)
     */
    public static void testCrash() {
        throw new RuntimeException("Test crash triggered!");
    }
}