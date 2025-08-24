package com.checkmate.android.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ANR (Application Not Responding) detection and recovery mechanism
 * Monitors main thread responsiveness and provides recovery options
 */
public class ANRWatchdog {
    private static final String TAG = "ANRWatchdog";
    private static final long DEFAULT_ANR_TIMEOUT = 5000; // 5 seconds
    private static final long WATCHDOG_INTERVAL = 1000; // 1 second
    
    private static ANRWatchdog instance;
    private final Handler mainHandler;
    private final Thread watchdogThread;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicLong lastResponseTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicBoolean mainThreadResponsive = new AtomicBoolean(true);
    private final long anrTimeout;
    private ANRListener anrListener;
    
    public interface ANRListener {
        void onANRDetected(long duration);
        void onANRResolved();
        void onCriticalANR(long duration);
    }
    
    private ANRWatchdog(long anrTimeout) {
        this.anrTimeout = anrTimeout;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.watchdogThread = new Thread(this::watchdogLoop, "ANRWatchdog");
        this.watchdogThread.setDaemon(true);
        this.watchdogThread.setPriority(Thread.MAX_PRIORITY);
    }
    
    /**
     * Get or create the ANR watchdog instance
     */
    public static synchronized ANRWatchdog getInstance() {
        if (instance == null) {
            instance = new ANRWatchdog(DEFAULT_ANR_TIMEOUT);
        }
        return instance;
    }
    
    /**
     * Start monitoring for ANRs
     */
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            try {
                watchdogThread.start();
                CrashLogger.logEvent("ANRWatchdog", "Started monitoring for ANRs");
                Log.d(TAG, "ANR Watchdog started");
            } catch (Exception e) {
                Log.e(TAG, "Failed to start ANR Watchdog", e);
                CrashLogger.logCrash("ANRWatchdog.start", e);
                isRunning.set(false);
            }
        }
    }
    
    /**
     * Stop monitoring for ANRs
     */
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            watchdogThread.interrupt();
            CrashLogger.logEvent("ANRWatchdog", "Stopped monitoring for ANRs");
            Log.d(TAG, "ANR Watchdog stopped");
        }
    }
    
    /**
     * Set the ANR listener for callbacks
     */
    public void setANRListener(ANRListener listener) {
        this.anrListener = listener;
    }
    
    /**
     * Check if the main thread is currently responsive
     */
    public boolean isMainThreadResponsive() {
        return mainThreadResponsive.get();
    }
    
    /**
     * Get the last response time from main thread
     */
    public long getLastResponseTime() {
        return lastResponseTime.get();
    }
    
    /**
     * Manually trigger ANR recovery mechanisms
     */
    public void triggerRecovery() {
        try {
            Log.w(TAG, "Manually triggering ANR recovery");
            CrashLogger.logANR("Manual Recovery", "User triggered ANR recovery");
            
            // Attempt to recover AppPreference
            com.checkmate.android.AppPreference.recoverFromANR();
            
            // Clear any pending operations on main thread
            mainHandler.removeCallbacksAndMessages(null);
            
            // Post a recovery runnable
            mainHandler.post(() -> {
                Log.i(TAG, "ANR recovery runnable executed on main thread");
                lastResponseTime.set(System.currentTimeMillis());
                mainThreadResponsive.set(true);
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to trigger ANR recovery", e);
            CrashLogger.logCrash("ANRWatchdog.triggerRecovery", e);
        }
    }
    
    /**
     * Main watchdog monitoring loop
     */
    private void watchdogLoop() {
        long lastLogTime = 0;
        boolean wasUnresponsive = false;
        
        while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
            try {
                // Post a heartbeat to main thread
                postHeartbeat();
                
                // Wait for the watchdog interval
                Thread.sleep(WATCHDOG_INTERVAL);
                
                // Check if main thread responded within timeout
                long currentTime = System.currentTimeMillis();
                long timeSinceLastResponse = currentTime - lastResponseTime.get();
                
                if (timeSinceLastResponse > anrTimeout) {
                    // ANR detected
                    if (mainThreadResponsive.compareAndSet(true, false)) {
                        Log.w(TAG, "ANR detected! Main thread unresponsive for " + timeSinceLastResponse + "ms");
                        CrashLogger.logANR("ANR Detection", "Main thread unresponsive for " + timeSinceLastResponse + "ms");
                        
                        if (anrListener != null) {
                            anrListener.onANRDetected(timeSinceLastResponse);
                        }
                        wasUnresponsive = true;
                    }
                    
                    // Log periodically during ANR
                    if (currentTime - lastLogTime > 5000) { // Every 5 seconds
                        Log.w(TAG, "ANR continues - unresponsive for " + timeSinceLastResponse + "ms");
                        CrashLogger.logANR("ANR Continues", "Duration: " + timeSinceLastResponse + "ms");
                        lastLogTime = currentTime;
                    }
                    
                    // Critical ANR threshold (30 seconds)
                    if (timeSinceLastResponse > 30000 && anrListener != null) {
                        anrListener.onCriticalANR(timeSinceLastResponse);
                    }
                    
                } else {
                    // Main thread is responsive
                    if (wasUnresponsive && mainThreadResponsive.compareAndSet(false, true)) {
                        Log.i(TAG, "ANR resolved - main thread responsive again");
                        CrashLogger.logEvent("ANR Recovery", "Main thread responsive after " + timeSinceLastResponse + "ms");
                        
                        if (anrListener != null) {
                            anrListener.onANRResolved();
                        }
                        wasUnresponsive = false;
                    }
                }
                
            } catch (InterruptedException e) {
                Log.d(TAG, "ANR Watchdog interrupted");
                break;
            } catch (Exception e) {
                Log.e(TAG, "Error in ANR watchdog loop", e);
                CrashLogger.logCrash("ANRWatchdog.watchdogLoop", e);
            }
        }
        
        Log.d(TAG, "ANR Watchdog monitoring stopped");
    }
    
    /**
     * Post a heartbeat to the main thread
     */
    private void postHeartbeat() {
        try {
            mainHandler.post(() -> {
                // Update response time when this runs on main thread
                lastResponseTime.set(System.currentTimeMillis());
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to post heartbeat", e);
        }
    }
    
    /**
     * Get ANR statistics
     */
    public String getANRStats() {
        long timeSinceLastResponse = System.currentTimeMillis() - lastResponseTime.get();
        return String.format(
            "ANR Watchdog Status:\n" +
            "Running: %s\n" +
            "Main Thread Responsive: %s\n" +
            "Time Since Last Response: %dms\n" +
            "ANR Timeout: %dms",
            isRunning.get(),
            mainThreadResponsive.get(),
            timeSinceLastResponse,
            anrTimeout
        );
    }
    
    /**
     * Force a test ANR (for debugging purposes only)
     */
    public void simulateANR(long duration) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.w(TAG, "Simulating ANR for " + duration + "ms");
            try {
                Thread.sleep(duration);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            mainHandler.post(() -> simulateANR(duration));
        }
    }
}