package com.checkmate.android.util;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Global crash handler that captures and logs crashes to internal storage
 */
public class CrashHandler implements UncaughtExceptionHandler {
    private static final String TAG = "CrashHandler";
    private static CrashHandler instance;
    private UncaughtExceptionHandler defaultHandler;
    private Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean isHandlingCrash = new AtomicBoolean(false);
    
    private CrashHandler() {}
    
    public static synchronized CrashHandler getInstance() {
        if (instance == null) {
            instance = new CrashHandler();
        }
        return instance;
    }
    
    public void init(Context context) {
        this.context = context.getApplicationContext();
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        
        // Clean old crash logs (keep only last 10)
        cleanOldCrashLogs();
    }
    
    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        // Prevent recursive crash handling
        if (isHandlingCrash.getAndSet(true)) {
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
            return;
        }
        
        try {
            // Log the crash
            final String crashInfo = collectCrashInfo(thread, throwable);
            
            // Save crash log asynchronously
            executor.execute(() -> saveCrashLog(crashInfo));
            
            // Give some time for the log to be written
            Thread.sleep(500);
            
            // Try to recover if possible
            if (attemptRecovery(throwable)) {
                isHandlingCrash.set(false);
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling crash", e);
        }
        
        // If recovery failed, pass to default handler
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, throwable);
        } else {
            // Kill the process as last resort
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
        }
    }
    
    private String collectCrashInfo(Thread thread, Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        
        sb.append("=== CRASH REPORT ===\n");
        sb.append("Time: ").append(sdf.format(new Date())).append("\n");
        sb.append("Thread: ").append(thread.getName()).append(" (ID: ").append(thread.getId()).append(")\n");
        sb.append("\n");
        
        // Device info
        sb.append("Device Info:\n");
        sb.append("  Brand: ").append(Build.BRAND).append("\n");
        sb.append("  Model: ").append(Build.MODEL).append("\n");
        sb.append("  Android Version: ").append(Build.VERSION.RELEASE).append("\n");
        sb.append("  SDK: ").append(Build.VERSION.SDK_INT).append("\n");
        sb.append("\n");
        
        // Memory info
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;
        
        sb.append("Memory Info:\n");
        sb.append("  Max Memory: ").append(maxMemory).append(" MB\n");
        sb.append("  Total Memory: ").append(totalMemory).append(" MB\n");
        sb.append("  Used Memory: ").append(usedMemory).append(" MB\n");
        sb.append("  Free Memory: ").append(freeMemory).append(" MB\n");
        sb.append("\n");
        
        // Stack trace
        sb.append("Stack Trace:\n");
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        sb.append(sw.toString());
        
        return sb.toString();
    }
    
    private void saveCrashLog(String crashInfo) {
        try {
            File crashDir = new File(context.getFilesDir(), "crash_logs");
            if (!crashDir.exists()) {
                crashDir.mkdirs();
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
            String fileName = "crash_" + sdf.format(new Date()) + ".log";
            File crashFile = new File(crashDir, fileName);
            
            FileWriter writer = new FileWriter(crashFile);
            writer.write(crashInfo);
            writer.close();
            
            AppLogger.e(TAG, "Crash log saved to: " + crashFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Failed to save crash log", e);
        }
    }
    
    private void cleanOldCrashLogs() {
        executor.execute(() -> {
            try {
                File crashDir = new File(context.getFilesDir(), "crash_logs");
                if (!crashDir.exists()) {
                    return;
                }
                
                File[] files = crashDir.listFiles();
                if (files != null && files.length > 10) {
                    // Sort by last modified
                    java.util.Arrays.sort(files, (f1, f2) -> 
                        Long.compare(f1.lastModified(), f2.lastModified()));
                    
                    // Delete oldest files
                    for (int i = 0; i < files.length - 10; i++) {
                        files[i].delete();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning old crash logs", e);
            }
        });
    }
    
    private boolean attemptRecovery(Throwable throwable) {
        // Check if we can recover from certain types of crashes
        if (throwable instanceof OutOfMemoryError) {
            // Try to free memory and restart the current activity
            System.gc();
            restartCurrentActivity();
            return true;
        }
        
        // Check if it's an ANR-like situation
        if (isMainThreadBlocked()) {
            // Try to recover by posting to main thread
            new Handler(Looper.getMainLooper()).post(() -> {
                AppLogger.e(TAG, "Attempting to recover from main thread block");
            });
            return false; // Still let the default handler manage it
        }
        
        return false;
    }
    
    private boolean isMainThreadBlocked() {
        return Thread.currentThread().getId() == Looper.getMainLooper().getThread().getId();
    }
    
    private void restartCurrentActivity() {
        try {
            // Use handler to restart on main thread
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    Context appContext = context.getApplicationContext();
                    android.content.Intent intent = appContext.getPackageManager()
                        .getLaunchIntentForPackage(appContext.getPackageName());
                    if (intent != null) {
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                        appContext.startActivity(intent);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to restart activity", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to restart current activity", e);
        }
    }
    
    public void shutdown() {
        executor.shutdown();
    }
}