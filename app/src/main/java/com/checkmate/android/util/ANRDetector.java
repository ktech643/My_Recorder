package com.checkmate.android.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import com.checkmate.android.AppPreference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ANR (Application Not Responding) detection and recovery utility.
 * Monitors main thread responsiveness and provides recovery mechanisms.
 */
public class ANRDetector {
    private static final String TAG = "ANRDetector";
    private static final long ANR_TIMEOUT_MS = 5000; // 5 seconds
    private static final long CHECK_INTERVAL_MS = 1000; // Check every second
    private static final int MAX_RECOVERY_ATTEMPTS = 3;
    
    private static volatile ANRDetector instance;
    private static final Object lock = new Object();
    
    private final Context context;
    private final Handler mainHandler;
    private final ExecutorService executor;
    private final AtomicBoolean isMonitoring;
    private final AtomicLong lastMainThreadResponse;
    private final AtomicBoolean anrDetected;
    private final AtomicLong recoveryAttempts;
    
    private ANRWatchdog watchdog;
    private ANRListener listener;
    
    public interface ANRListener {
        void onANRDetected(long blockTimeMs);
        void onANRRecovered();
        void onRecoveryFailed(int attempts);
    }
    
    private static class ANRWatchdog implements Runnable {
        private final ANRDetector detector;
        private volatile boolean running = true;
        
        ANRWatchdog(ANRDetector detector) {
            this.detector = detector;
        }
        
        @Override
        public void run() {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    detector.checkMainThread();
                    Thread.sleep(CHECK_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    InternalLogger.logError(TAG, "Error in ANR watchdog", e);
                }
            }
        }
        
        void stop() {
            running = false;
        }
    }
    
    private ANRDetector(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ANRDetector");
            t.setDaemon(true);
            return t;
        });
        this.isMonitoring = new AtomicBoolean(false);
        this.lastMainThreadResponse = new AtomicLong(System.currentTimeMillis());
        this.anrDetected = new AtomicBoolean(false);
        this.recoveryAttempts = new AtomicLong(0);
    }
    
    public static void initialize(@NonNull Context context) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ANRDetector(context);
                }
            }
        }
    }
    
    public static ANRDetector getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ANRDetector not initialized. Call initialize() first.");
        }
        return instance;
    }
    
    public void setANRListener(ANRListener listener) {
        this.listener = listener;
    }
    
    public void startMonitoring() {
        if (isMonitoring.compareAndSet(false, true)) {
            lastMainThreadResponse.set(System.currentTimeMillis());
            anrDetected.set(false);
            recoveryAttempts.set(0);
            
            watchdog = new ANRWatchdog(this);
            executor.execute(watchdog);
            
            InternalLogger.logInfo(TAG, "ANR monitoring started");
        }
    }
    
    public void stopMonitoring() {
        if (isMonitoring.compareAndSet(true, false)) {
            if (watchdog != null) {
                watchdog.stop();
                watchdog = null;
            }
            InternalLogger.logInfo(TAG, "ANR monitoring stopped");
        }
    }
    
    public void updateMainThreadActivity() {
        lastMainThreadResponse.set(System.currentTimeMillis());
        if (anrDetected.compareAndSet(true, false)) {
            recoveryAttempts.set(0);
            InternalLogger.logInfo(TAG, "ANR recovered - main thread responsive again");
            if (listener != null) {
                listener.onANRRecovered();
            }
        }
    }
    
    private void checkMainThread() {
        if (!isMonitoring.get()) return;
        
        final long currentTime = System.currentTimeMillis();
        final long lastResponse = lastMainThreadResponse.get();
        final long blockTime = currentTime - lastResponse;
        
        if (blockTime > ANR_TIMEOUT_MS) {
            if (!anrDetected.get()) {
                // First detection of ANR
                anrDetected.set(true);
                InternalLogger.logWarn(TAG, "ANR detected - main thread blocked for " + blockTime + "ms");
                
                if (listener != null) {
                    listener.onANRDetected(blockTime);
                }
                
                // Start recovery process
                attemptRecovery();
            } else {
                // ANR is ongoing
                InternalLogger.logWarn(TAG, "ANR ongoing - main thread blocked for " + blockTime + "ms");
                
                // Continue recovery attempts
                if (recoveryAttempts.get() < MAX_RECOVERY_ATTEMPTS) {
                    attemptRecovery();
                }
            }
        } else {
            // Send a ping to main thread to verify it's responsive
            pingMainThread();
        }
    }
    
    private void pingMainThread() {
        final long pingTime = System.currentTimeMillis();
        
        mainHandler.post(() -> {
            // Update the response time when this runs
            lastMainThreadResponse.set(System.currentTimeMillis());
            
            // Check if this ping took too long to execute
            long executionDelay = System.currentTimeMillis() - pingTime;
            if (executionDelay > 1000) { // More than 1 second delay
                InternalLogger.logWarn(TAG, "Main thread ping delayed by " + executionDelay + "ms");
            }
        });
    }
    
    private void attemptRecovery() {
        long attempts = recoveryAttempts.incrementAndGet();
        
        InternalLogger.logInfo(TAG, "Attempting ANR recovery (attempt " + attempts + "/" + MAX_RECOVERY_ATTEMPTS + ")");
        
        try {
            // Strategy 1: Try to clear message queue and force garbage collection
            mainHandler.post(() -> {
                try {
                    // Force garbage collection
                    System.gc();
                    
                    // Update app preference with recovery flag
                    AppPreference.setBool(AppPreference.KEY.IS_RESTART_APP, true);
                    
                    // Try to recover app preferences
                    AppPreference.recoverFromANR();
                    
                    InternalLogger.logInfo(TAG, "ANR recovery operations completed");
                    updateMainThreadActivity();
                    
                } catch (Exception e) {
                    InternalLogger.logError(TAG, "Error during ANR recovery", e);
                }
            });
            
            // Strategy 2: If main thread is completely blocked, try alternative approaches
            executor.execute(() -> {
                try {
                    Thread.sleep(2000); // Wait a bit to see if recovery worked
                    
                    if (anrDetected.get() && attempts >= MAX_RECOVERY_ATTEMPTS) {
                        InternalLogger.logError(TAG, "ANR recovery failed after " + attempts + " attempts");
                        
                        if (listener != null) {
                            listener.onRecoveryFailed((int) attempts);
                        }
                        
                        // Last resort: Trigger app restart
                        triggerAppRestart();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    InternalLogger.logError(TAG, "Error in ANR recovery monitoring", e);
                }
            });
            
        } catch (Exception e) {
            InternalLogger.logError(TAG, "Error attempting ANR recovery", e);
        }
    }
    
    private void triggerAppRestart() {
        try {
            InternalLogger.logError(TAG, "Triggering app restart due to unrecoverable ANR");
            
            // Set restart flag
            AppPreference.setBool(AppPreference.KEY.IS_RESTART_APP, true);
            AppPreference.setLong(AppPreference.KEY.LAST_RESTART_TIME, System.currentTimeMillis());
            AppPreference.setInt(AppPreference.KEY.RESTART_COUNT, 
                AppPreference.getInt(AppPreference.KEY.RESTART_COUNT, 0) + 1);
            
            // Force close the app (will be restarted by Android if configured)
            mainHandler.post(() -> {
                try {
                    android.os.Process.killProcess(android.os.Process.myPid());
                } catch (Exception e) {
                    InternalLogger.logError(TAG, "Error killing process", e);
                    System.exit(1);
                }
            });
            
        } catch (Exception e) {
            InternalLogger.logError(TAG, "Error triggering app restart", e);
            // Fallback: Force exit
            System.exit(1);
        }
    }
    
    public boolean isANRDetected() {
        return anrDetected.get();
    }
    
    public long getLastMainThreadResponseTime() {
        return lastMainThreadResponse.get();
    }
    
    public long getMainThreadBlockTime() {
        if (anrDetected.get()) {
            return System.currentTimeMillis() - lastMainThreadResponse.get();
        }
        return 0;
    }
    
    public boolean isMonitoring() {
        return isMonitoring.get();
    }
    
    public void shutdown() {
        stopMonitoring();
        try {
            executor.shutdown();
        } catch (Exception e) {
            InternalLogger.logError(TAG, "Error during shutdown", e);
        }
    }
    
    // Utility method for components to report they're alive
    public static void reportMainThreadActivity() {
        if (instance != null && instance.isMonitoring()) {
            instance.updateMainThreadActivity();
        }
    }
    
    // Method to check if current thread is main thread
    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }
    
    // Method to ensure operation runs on main thread
    public static void runOnMainThread(Runnable runnable) {
        if (isMainThread()) {
            runnable.run();
        } else {
            new Handler(Looper.getMainLooper()).post(runnable);
        }
    }
    
    // Method to ensure operation runs on background thread
    public static void runOnBackgroundThread(Runnable runnable) {
        if (isMainThread()) {
            new Thread(runnable, "BackgroundTask").start();
        } else {
            runnable.run();
        }
    }
}