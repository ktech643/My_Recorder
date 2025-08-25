package com.checkmate.android.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * ANR Watchdog to detect when the main thread is blocked
 */
public class ANRWatchdog extends Thread {
    private static final String TAG = "ANRWatchdog";
    private static final int DEFAULT_TIMEOUT = 5000; // 5 seconds
    
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final int timeoutInterval;
    private final AtomicInteger tick = new AtomicInteger(0);
    private volatile boolean stopped = false;
    private final ANRListener anrListener;
    
    public interface ANRListener {
        void onAppNotResponding(ANRError error);
    }
    
    public static class ANRError extends Error {
        private ANRError(Thread thread) {
            super("Application Not Responding", new Throwable(thread.getName(), thread.getStackTrace()[0]));
            
            // Add main thread stack trace
            StackTraceElement[] mainStackTrace = Looper.getMainLooper().getThread().getStackTrace();
            StackTraceElement[] thisStackTrace = new StackTraceElement[mainStackTrace.length];
            System.arraycopy(mainStackTrace, 0, thisStackTrace, 0, mainStackTrace.length);
            setStackTrace(thisStackTrace);
        }
    }
    
    public ANRWatchdog() {
        this(DEFAULT_TIMEOUT, null);
    }
    
    public ANRWatchdog(int timeout) {
        this(timeout, null);
    }
    
    public ANRWatchdog(ANRListener listener) {
        this(DEFAULT_TIMEOUT, listener);
    }
    
    public ANRWatchdog(int timeout, ANRListener listener) {
        super("ANRWatchdog");
        this.timeoutInterval = timeout;
        this.anrListener = listener;
    }
    
    @Override
    public void run() {
        setName("ANRWatchdog");
        
        int lastTick;
        while (!isInterrupted() && !stopped) {
            lastTick = tick.get();
            
            // Post a runnable to the main thread
            mainHandler.post(() -> tick.incrementAndGet());
            
            try {
                Thread.sleep(timeoutInterval);
            } catch (InterruptedException e) {
                AppLogger.w(TAG, "ANRWatchdog interrupted");
                return;
            }
            
            // Check if main thread processed our runnable
            if (tick.get() == lastTick) {
                // Main thread is blocked
                ANRError error = new ANRError(Looper.getMainLooper().getThread());
                
                if (anrListener != null) {
                    anrListener.onAppNotResponding(error);
                } else {
                    // Default behavior: log the ANR
                    AppLogger.e(TAG, "ANR detected! Main thread blocked for " + timeoutInterval + "ms", error);
                    
                    // Try to recover
                    attemptRecovery();
                }
            }
        }
    }
    
    private void attemptRecovery() {
        AppLogger.w(TAG, "Attempting to recover from ANR...");
        
        // Force garbage collection
        System.gc();
        
        // Try to clear any pending messages that might be blocking
        mainHandler.post(() -> {
            AppLogger.i(TAG, "Main thread responsive again");
        });
        
        // Log thread states for debugging
        logThreadStates();
    }
    
    private void logThreadStates() {
        Thread[] threads = new Thread[Thread.activeCount()];
        Thread.enumerate(threads);
        
        StringBuilder sb = new StringBuilder("Thread states:\n");
        for (Thread thread : threads) {
            if (thread != null) {
                sb.append("  ").append(thread.getName())
                  .append(" (").append(thread.getState()).append(")\n");
            }
        }
        AppLogger.d(TAG, sb.toString());
    }
    
    public void stopWatchdog() {
        stopped = true;
        interrupt();
    }
    
    /**
     * Check if the main thread is responsive
     */
    public static boolean isMainThreadResponsive() {
        final AtomicInteger result = new AtomicInteger(0);
        Handler handler = new Handler(Looper.getMainLooper());
        
        handler.post(() -> result.set(1));
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            return false;
        }
        
        return result.get() == 1;
    }
}