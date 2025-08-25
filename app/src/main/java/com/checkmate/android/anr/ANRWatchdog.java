package com.checkmate.android.anr;

import android.os.Handler;
import android.os.Looper;

import com.checkmate.android.logging.InternalLogger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ANR Watchdog to detect and recover from Application Not Responding issues
 * Monitors the main thread and triggers recovery mechanisms when ANR is detected
 */
public class ANRWatchdog extends Thread {
    private static final String TAG = "ANRWatchdog";
    private static final int DEFAULT_TIMEOUT = 5000; // 5 seconds
    private static final int CHECK_INTERVAL = 1000; // 1 second
    
    private static volatile ANRWatchdog instance;
    private final Handler mainHandler;
    private final int timeoutInterval;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicLong lastResponseTime = new AtomicLong(0);
    private volatile ANRListener anrListener;
    
    public interface ANRListener {
        void onAppNotResponding(ANRError error);
        void onANRRecovered();
    }
    
    private ANRWatchdog(int timeoutInterval) {
        super("ANRWatchdog");
        this.timeoutInterval = timeoutInterval;
        this.mainHandler = new Handler(Looper.getMainLooper());
        setDaemon(true);
    }
    
    public static void initialize() {
        initialize(DEFAULT_TIMEOUT);
    }
    
    public static void initialize(int timeoutMillis) {
        if (instance == null) {
            synchronized (ANRWatchdog.class) {
                if (instance == null) {
                    instance = new ANRWatchdog(timeoutMillis);
                }
            }
        }
    }
    
    public static ANRWatchdog getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ANRWatchdog must be initialized first");
        }
        return instance;
    }
    
    public void setANRListener(ANRListener listener) {
        this.anrListener = listener;
    }
    
    public void startWatching() {
        if (isRunning.compareAndSet(false, true)) {
            InternalLogger.i(TAG, "Starting ANR watchdog");
            start();
        }
    }
    
    public void stopWatching() {
        InternalLogger.i(TAG, "Stopping ANR watchdog");
        isRunning.set(false);
        interrupt();
    }
    
    @Override
    public void run() {
        while (isRunning.get()) {
            try {
                final long startTime = System.currentTimeMillis();
                lastResponseTime.set(startTime);
                
                // Post a task to the main thread
                mainHandler.post(() -> {
                    // Update the response time when this runs
                    lastResponseTime.set(System.currentTimeMillis());
                });
                
                // Wait for the check interval
                Thread.sleep(CHECK_INTERVAL);
                
                // Check if the main thread responded
                long responseTime = lastResponseTime.get();
                long elapsedTime = System.currentTimeMillis() - startTime;
                
                if (elapsedTime > timeoutInterval && responseTime == startTime) {
                    // ANR detected
                    handleANR();
                }
                
            } catch (InterruptedException e) {
                InternalLogger.w(TAG, "ANR watchdog interrupted");
                break;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error in ANR watchdog", e);
            }
        }
    }
    
    private void handleANR() {
        InternalLogger.e(TAG, "ANR detected! Main thread blocked for more than " + timeoutInterval + "ms");
        
        // Capture the main thread stack trace
        ANRError error = ANRError.captureMainThreadError();
        
        // Log the ANR
        InternalLogger.getInstance().logCrash(error);
        
        // Notify listener
        if (anrListener != null) {
            // Run on a separate thread to avoid blocking
            new Thread(() -> {
                try {
                    anrListener.onAppNotResponding(error);
                } catch (Exception e) {
                    InternalLogger.e(TAG, "Error in ANR listener", e);
                }
            }).start();
        }
        
        // Try to recover
        attemptRecovery();
    }
    
    private void attemptRecovery() {
        InternalLogger.i(TAG, "Attempting ANR recovery");
        
        // Post a recovery task with high priority
        mainHandler.postAtFrontOfQueue(() -> {
            InternalLogger.i(TAG, "ANR recovery task executed");
            if (anrListener != null) {
                anrListener.onANRRecovered();
            }
        });
        
        // Wait a bit before continuing monitoring
        try {
            Thread.sleep(timeoutInterval);
        } catch (InterruptedException e) {
            // Ignore
        }
    }
    
    /**
     * Custom error class for ANR errors
     */
    public static class ANRError extends Error {
        private static final long serialVersionUID = 1L;
        
        private ANRError(String message, StackTraceElement[] mainThreadStackTrace) {
            super(message);
            setStackTrace(mainThreadStackTrace);
        }
        
        static ANRError captureMainThreadError() {
            Thread mainThread = Looper.getMainLooper().getThread();
            StackTraceElement[] stackTrace = mainThread.getStackTrace();
            
            StringBuilder message = new StringBuilder("Application Not Responding\n");
            message.append("Main thread state: ").append(mainThread.getState()).append("\n");
            message.append("Main thread stack trace:");
            
            return new ANRError(message.toString(), stackTrace);
        }
        
        @Override
        public Throwable fillInStackTrace() {
            // Don't fill in stack trace as we're using the main thread's stack trace
            return this;
        }
    }
}