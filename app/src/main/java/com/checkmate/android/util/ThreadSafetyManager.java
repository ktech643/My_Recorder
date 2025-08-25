package com.checkmate.android.util;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import com.checkmate.android.AppPreference;

/**
 * ThreadSafetyManager - Comprehensive ANR and thread safety monitoring system
 * Provides real-time monitoring, detection, and recovery mechanisms for thread safety
 */
public class ThreadSafetyManager {
    private static final String TAG = "ThreadSafetyManager";
    
    // Singleton instance
    private static volatile ThreadSafetyManager instance;
    private static final Object LOCK = new Object();
    
    // Configuration constants
    private static final long ANR_THRESHOLD = 5000; // 5 seconds
    private static final long WATCHDOG_INTERVAL = 1000; // 1 second
    private static final int MAX_BACKGROUND_THREADS = 20;
    
    // Monitoring state
    private final AtomicBoolean isMonitoring = new AtomicBoolean(false);
    private final AtomicBoolean anrDetected = new AtomicBoolean(false);
    private final AtomicLong lastMainThreadActivity = new AtomicLong(System.currentTimeMillis());
    
    // Thread tracking
    private final AtomicInteger backgroundThreadCount = new AtomicInteger(0);
    private final ConcurrentHashMap<String, ThreadInfo> activeThreads = new ConcurrentHashMap<>();
    
    // Watchdog thread
    private Thread watchdogThread;
    private final AtomicBoolean watchdogRunning = new AtomicBoolean(false);
    
    // Main thread handler for monitoring
    private Handler mainHandler;
    
    /**
     * Get singleton instance
     */
    public static ThreadSafetyManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new ThreadSafetyManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Private constructor
     */
    private ThreadSafetyManager() {
        try {
            mainHandler = new Handler(Looper.getMainLooper());
            InternalLogger.d(TAG, "ThreadSafetyManager initialized");
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error initializing ThreadSafetyManager", e);
        }
    }
    
    /**
     * Initialize and start monitoring
     */
    public void init() {
        try {
            if (isMonitoring.compareAndSet(false, true)) {
                startMainThreadMonitoring();
                startWatchdogThread();
                InternalLogger.i(TAG, "Thread safety monitoring started");
            }
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error starting thread safety monitoring", e);
        }
    }
    
    /**
     * Stop monitoring
     */
    public void shutdown() {
        try {
            isMonitoring.set(false);
            watchdogRunning.set(false);
            
            if (watchdogThread != null && watchdogThread.isAlive()) {
                watchdogThread.interrupt();
            }
            
            activeThreads.clear();
            backgroundThreadCount.set(0);
            
            InternalLogger.i(TAG, "Thread safety monitoring stopped");
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error stopping thread safety monitoring", e);
        }
    }
    
    /**
     * Internal ThreadInfo class for tracking thread state
     */
    private static class ThreadInfo {
        final String name;
        final long createdTime;
        final AtomicLong lastActivity;
        final AtomicBoolean isActive;
        
        ThreadInfo(String name) {
            this.name = name;
            this.createdTime = System.currentTimeMillis();
            this.lastActivity = new AtomicLong(System.currentTimeMillis());
            this.isActive = new AtomicBoolean(true);
        }
        
        void updateActivity() {
            lastActivity.set(System.currentTimeMillis());
        }
        
        boolean isStuck() {
            return isActive.get() && (System.currentTimeMillis() - lastActivity.get()) > 10000; // 10 seconds
        }
    }
    
    /**
     * Start main thread monitoring
     */
    private void startMainThreadMonitoring() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isMonitoring.get()) {
                    lastMainThreadActivity.set(System.currentTimeMillis());
                    
                    // Schedule next check
                    mainHandler.postDelayed(this, 100); // Check every 100ms
                }
            }
        });
    }
    
    /**
     * Start watchdog thread for ANR detection
     */
    private void startWatchdogThread() {
        if (watchdogRunning.compareAndSet(false, true)) {
            watchdogThread = new Thread(() -> {
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                
                while (watchdogRunning.get() && !Thread.currentThread().isInterrupted()) {
                    try {
                        // Check for ANR conditions
                        checkForAnrConditions();
                        
                        // Check background threads
                        checkBackgroundThreads();
                        
                        // Clean up inactive threads
                        cleanupInactiveThreads();
                        
                        Thread.sleep(WATCHDOG_INTERVAL);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        InternalLogger.e(TAG, "Error in watchdog thread", e);
                    }
                }
                
                InternalLogger.d(TAG, "Watchdog thread stopped");
            }, "ThreadSafety-Watchdog");
            
            watchdogThread.setDaemon(true);
            watchdogThread.start();
        }
    }
    
    /**
     * Check for ANR conditions
     */
    private void checkForAnrConditions() {
        long timeSinceLastActivity = System.currentTimeMillis() - lastMainThreadActivity.get();
        
        if (timeSinceLastActivity > ANR_THRESHOLD) {
            if (anrDetected.compareAndSet(false, true)) {
                InternalLogger.e(TAG, "ANR condition detected! Main thread unresponsive for " + timeSinceLastActivity + "ms");
                handleAnrCondition(timeSinceLastActivity);
            }
        } else if (anrDetected.get() && timeSinceLastActivity < (ANR_THRESHOLD / 2)) {
            // ANR resolved
            if (anrDetected.compareAndSet(true, false)) {
                InternalLogger.i(TAG, "ANR condition resolved, main thread responsive again");
                handleAnrResolved();
            }
        }
    }
    
    /**
     * Handle ANR condition
     */
    private void handleAnrCondition(long unresponsiveTime) {
        try {
            // Log thread dump
            logThreadDump();
            
            // Update app preferences
            AppPreference.incrementRestartCount();
            
            // Enter recovery mode
            ANRSafeHelper.getInstance().enterRecoveryMode();
            
            // Log system state
            logSystemState();
            
            InternalLogger.e(TAG, "ANR recovery procedures initiated");
            
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error handling ANR condition", e);
        }
    }
    
    /**
     * Handle ANR resolved
     */
    private void handleAnrResolved() {
        try {
            // Exit recovery mode
            ANRSafeHelper.getInstance().exitRecoveryMode();
            
            InternalLogger.i(TAG, "ANR recovery completed successfully");
            
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error handling ANR resolution", e);
        }
    }
    
    /**
     * Check background threads for issues
     */
    private void checkBackgroundThreads() {
        try {
            // Check if we have too many background threads
            if (backgroundThreadCount.get() > MAX_BACKGROUND_THREADS) {
                InternalLogger.w(TAG, "Too many background threads: " + backgroundThreadCount.get());
            }
            
            // Check for stuck threads
            for (ThreadInfo threadInfo : activeThreads.values()) {
                if (threadInfo.isStuck()) {
                    InternalLogger.w(TAG, "Stuck thread detected: " + threadInfo.name + 
                                   ", inactive for " + (System.currentTimeMillis() - threadInfo.lastActivity.get()) + "ms");
                }
            }
            
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error checking background threads", e);
        }
    }
    
    /**
     * Clean up inactive threads from tracking
     */
    private void cleanupInactiveThreads() {
        try {
            long currentTime = System.currentTimeMillis();
            activeThreads.entrySet().removeIf(entry -> {
                ThreadInfo threadInfo = entry.getValue();
                // Remove threads that have been inactive for more than 30 seconds
                boolean shouldRemove = !threadInfo.isActive.get() || 
                                     (currentTime - threadInfo.lastActivity.get()) > 30000;
                
                if (shouldRemove) {
                    InternalLogger.d(TAG, "Removing inactive thread from tracking: " + threadInfo.name);
                }
                
                return shouldRemove;
            });
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error cleaning up inactive threads", e);
        }
    }
    
    /**
     * Register a background thread for monitoring
     */
    public void registerBackgroundThread(String threadName) {
        try {
            backgroundThreadCount.incrementAndGet();
            ThreadInfo threadInfo = new ThreadInfo(threadName);
            activeThreads.put(threadName, threadInfo);
            
            InternalLogger.d(TAG, "Registered background thread: " + threadName + 
                           ", total: " + backgroundThreadCount.get());
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error registering background thread", e);
        }
    }
    
    /**
     * Unregister a background thread
     */
    public void unregisterBackgroundThread(String threadName) {
        try {
            ThreadInfo threadInfo = activeThreads.remove(threadName);
            if (threadInfo != null) {
                threadInfo.isActive.set(false);
                backgroundThreadCount.decrementAndGet();
                
                InternalLogger.d(TAG, "Unregistered background thread: " + threadName + 
                               ", total: " + backgroundThreadCount.get());
            }
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error unregistering background thread", e);
        }
    }
    
    /**
     * Update thread activity
     */
    public void updateThreadActivity(String threadName) {
        try {
            ThreadInfo threadInfo = activeThreads.get(threadName);
            if (threadInfo != null) {
                threadInfo.updateActivity();
            }
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error updating thread activity", e);
        }
    }
    
    /**
     * Log thread dump for debugging
     */
    private void logThreadDump() {
        try {
            StringBuilder dump = new StringBuilder();
            dump.append("=== THREAD DUMP ===\n");
            dump.append("Timestamp: ").append(System.currentTimeMillis()).append("\n");
            dump.append("Active Threads: ").append(activeThreads.size()).append("\n");
            dump.append("Background Thread Count: ").append(backgroundThreadCount.get()).append("\n");
            
            for (ThreadInfo threadInfo : activeThreads.values()) {
                dump.append("Thread: ").append(threadInfo.name)
                    .append(", Created: ").append(threadInfo.createdTime)
                    .append(", Last Activity: ").append(threadInfo.lastActivity.get())
                    .append(", Active: ").append(threadInfo.isActive.get())
                    .append("\n");
            }
            
            dump.append("=== END THREAD DUMP ===\n");
            
            InternalLogger.e(TAG, dump.toString());
            
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error creating thread dump", e);
        }
    }
    
    /**
     * Log system state information
     */
    private void logSystemState() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            StringBuilder state = new StringBuilder();
            state.append("=== SYSTEM STATE ===\n");
            state.append("Max Memory: ").append(maxMemory / 1024 / 1024).append(" MB\n");
            state.append("Total Memory: ").append(totalMemory / 1024 / 1024).append(" MB\n");
            state.append("Used Memory: ").append(usedMemory / 1024 / 1024).append(" MB\n");
            state.append("Free Memory: ").append(freeMemory / 1024 / 1024).append(" MB\n");
            state.append("Available Processors: ").append(runtime.availableProcessors()).append("\n");
            state.append("=== END SYSTEM STATE ===\n");
            
            InternalLogger.i(TAG, state.toString());
            
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error logging system state", e);
        }
    }
    
    /**
     * Get current thread safety status
     */
    public ThreadSafetyStatus getStatus() {
        boolean isMainThreadResponsive = (System.currentTimeMillis() - lastMainThreadActivity.get()) < ANR_THRESHOLD;
        boolean hasStuckThreads = activeThreads.values().stream().anyMatch(ThreadInfo::isStuck);
        boolean tooManyBackgroundThreads = backgroundThreadCount.get() > MAX_BACKGROUND_THREADS;
        
        return new ThreadSafetyStatus(
            isMainThreadResponsive,
            !hasStuckThreads,
            !tooManyBackgroundThreads,
            backgroundThreadCount.get(),
            activeThreads.size(),
            anrDetected.get()
        );
    }
    
    /**
     * Thread safety status information
     */
    public static class ThreadSafetyStatus {
        public final boolean mainThreadResponsive;
        public final boolean noStuckThreads;
        public final boolean backgroundThreadCountOk;
        public final int backgroundThreadCount;
        public final int activeThreadCount;
        public final boolean anrDetected;
        
        ThreadSafetyStatus(boolean mainThreadResponsive, boolean noStuckThreads, 
                          boolean backgroundThreadCountOk, int backgroundThreadCount, 
                          int activeThreadCount, boolean anrDetected) {
            this.mainThreadResponsive = mainThreadResponsive;
            this.noStuckThreads = noStuckThreads;
            this.backgroundThreadCountOk = backgroundThreadCountOk;
            this.backgroundThreadCount = backgroundThreadCount;
            this.activeThreadCount = activeThreadCount;
            this.anrDetected = anrDetected;
        }
        
        public boolean isHealthy() {
            return mainThreadResponsive && noStuckThreads && backgroundThreadCountOk && !anrDetected;
        }
        
        @Override
        public String toString() {
            return "ThreadSafetyStatus{" +
                    "mainThreadResponsive=" + mainThreadResponsive +
                    ", noStuckThreads=" + noStuckThreads +
                    ", backgroundThreadCountOk=" + backgroundThreadCountOk +
                    ", backgroundThreadCount=" + backgroundThreadCount +
                    ", activeThreadCount=" + activeThreadCount +
                    ", anrDetected=" + anrDetected +
                    ", healthy=" + isHealthy() +
                    '}';
        }
    }
}