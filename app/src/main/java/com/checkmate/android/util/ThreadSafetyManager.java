package com.checkmate.android.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import com.checkmate.android.AppPreference;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread Safety Manager for the CheckMate Android app
 * Monitors thread safety, detects potential ANR conditions, and implements recovery mechanisms
 */
public class ThreadSafetyManager {
    private static final String TAG = "ThreadSafetyManager";
    private static final long ANR_THRESHOLD = 5000; // 5 seconds
    private static final long WATCHDOG_INTERVAL = 1000; // 1 second
    private static final int MAX_BACKGROUND_THREADS = 10;
    
    private static volatile ThreadSafetyManager instance;
    private final Context context;
    private final Handler mainHandler;
    private final AtomicBoolean isMonitoring;
    private final AtomicLong lastMainThreadActivity;
    private final AtomicInteger backgroundThreadCount;
    private final ConcurrentHashMap<String, ThreadInfo> activeThreads;
    private final AtomicBoolean anrDetected;
    
    // Watchdog thread for ANR detection
    private Thread watchdogThread;
    private final AtomicBoolean watchdogRunning;
    
    private static class ThreadInfo {
        final String name;
        final long createdTime;
        final AtomicLong lastActivity;
        final AtomicBoolean isActive;
        
        ThreadInfo(String name) {
            this.name = name;
            this.createdTime = System.currentTimeMillis();
            this.lastActivity = new AtomicLong(createdTime);
            this.isActive = new AtomicBoolean(true);
        }
        
        void updateActivity() {
            lastActivity.set(System.currentTimeMillis());
        }
        
        boolean isStuck() {
            return System.currentTimeMillis() - lastActivity.get() > ANR_THRESHOLD;
        }
    }
    
    private ThreadSafetyManager(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.isMonitoring = new AtomicBoolean(false);
        this.lastMainThreadActivity = new AtomicLong(System.currentTimeMillis());
        this.backgroundThreadCount = new AtomicInteger(0);
        this.activeThreads = new ConcurrentHashMap<>();
        this.anrDetected = new AtomicBoolean(false);
        this.watchdogRunning = new AtomicBoolean(false);
    }
    
    public static ThreadSafetyManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ThreadSafetyManager.class) {
                if (instance == null) {
                    instance = new ThreadSafetyManager(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * Start monitoring thread safety and ANR conditions
     */
    public void startMonitoring() {
        if (isMonitoring.compareAndSet(false, true)) {
            InternalLogger.i(TAG, "Starting thread safety monitoring");
            
            // Start main thread activity monitoring
            startMainThreadMonitoring();
            
            // Start watchdog thread
            startWatchdogThread();
            
            InternalLogger.i(TAG, "Thread safety monitoring started successfully");
        }
    }
    
    /**
     * Stop monitoring
     */
    public void stopMonitoring() {
        if (isMonitoring.compareAndSet(true, false)) {
            InternalLogger.i(TAG, "Stopping thread safety monitoring");
            
            // Stop watchdog thread
            if (watchdogRunning.compareAndSet(true, false) && watchdogThread != null) {
                watchdogThread.interrupt();
            }
            
            // Clear active threads
            activeThreads.clear();
            
            InternalLogger.i(TAG, "Thread safety monitoring stopped");
        }
    }
    
    /**
     * Start monitoring main thread activity
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
            }\n        });\n    }\n    \n    /**\n     * Start watchdog thread for ANR detection\n     */\n    private void startWatchdogThread() {\n        if (watchdogRunning.compareAndSet(false, true)) {\n            watchdogThread = new Thread(() -> {\n                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);\n                \n                while (watchdogRunning.get() && !Thread.currentThread().isInterrupted()) {\n                    try {\n                        // Check for ANR conditions\n                        checkForAnrConditions();\n                        \n                        // Check background threads\n                        checkBackgroundThreads();\n                        \n                        // Clean up inactive threads\n                        cleanupInactiveThreads();\n                        \n                        Thread.sleep(WATCHDOG_INTERVAL);\n                    } catch (InterruptedException e) {\n                        Thread.currentThread().interrupt();\n                        break;\n                    } catch (Exception e) {\n                        InternalLogger.e(TAG, \"Error in watchdog thread\", e);\n                    }\n                }\n                \n                InternalLogger.d(TAG, \"Watchdog thread stopped\");\n            }, \"ThreadSafety-Watchdog\");\n            \n            watchdogThread.setDaemon(true);\n            watchdogThread.start();\n        }\n    }\n    \n    /**\n     * Check for ANR conditions\n     */\n    private void checkForAnrConditions() {\n        long timeSinceLastActivity = System.currentTimeMillis() - lastMainThreadActivity.get();\n        \n        if (timeSinceLastActivity > ANR_THRESHOLD) {\n            if (anrDetected.compareAndSet(false, true)) {\n                InternalLogger.e(TAG, \"ANR condition detected! Main thread unresponsive for \" + timeSinceLastActivity + \"ms\");\n                handleAnrCondition(timeSinceLastActivity);\n            }\n        } else if (anrDetected.get() && timeSinceLastActivity < (ANR_THRESHOLD / 2)) {\n            // ANR resolved\n            if (anrDetected.compareAndSet(true, false)) {\n                InternalLogger.i(TAG, \"ANR condition resolved, main thread responsive again\");\n                handleAnrResolved();\n            }\n        }\n    }\n    \n    /**\n     * Handle ANR condition\n     */\n    private void handleAnrCondition(long unresponsiveTime) {\n        try {\n            // Log thread dump\n            logThreadDump();\n            \n            // Update app preferences\n            AppPreference.incrementRestartCount();\n            \n            // Enter recovery mode\n            ANRSafeHelper.getInstance().exitRecoveryMode(); // This will enter recovery mode\n            \n            // Log system state\n            logSystemState();\n            \n            InternalLogger.e(TAG, \"ANR recovery procedures initiated\");\n            \n        } catch (Exception e) {\n            InternalLogger.e(TAG, \"Error handling ANR condition\", e);\n        }\n    }\n    \n    /**\n     * Handle ANR resolved\n     */\n    private void handleAnrResolved() {\n        try {\n            // Exit recovery mode\n            ANRSafeHelper.getInstance().exitRecoveryMode();\n            \n            InternalLogger.i(TAG, \"ANR recovery completed successfully\");\n            \n        } catch (Exception e) {\n            InternalLogger.e(TAG, \"Error handling ANR resolution\", e);\n        }\n    }\n    \n    /**\n     * Check background threads for issues\n     */\n    private void checkBackgroundThreads() {\n        try {\n            // Check if we have too many background threads\n            if (backgroundThreadCount.get() > MAX_BACKGROUND_THREADS) {\n                InternalLogger.w(TAG, \"Too many background threads: \" + backgroundThreadCount.get());\n            }\n            \n            // Check for stuck threads\n            for (ThreadInfo threadInfo : activeThreads.values()) {\n                if (threadInfo.isStuck()) {\n                    InternalLogger.w(TAG, \"Stuck thread detected: \" + threadInfo.name + \n                                   \", inactive for \" + (System.currentTimeMillis() - threadInfo.lastActivity.get()) + \"ms\");\n                }\n            }\n            \n        } catch (Exception e) {\n            InternalLogger.e(TAG, \"Error checking background threads\", e);\n        }\n    }\n    \n    /**\n     * Clean up inactive threads from tracking\n     */\n    private void cleanupInactiveThreads() {\n        try {\n            long currentTime = System.currentTimeMillis();\n            activeThreads.entrySet().removeIf(entry -> {\n                ThreadInfo threadInfo = entry.getValue();\n                // Remove threads that have been inactive for more than 30 seconds\n                boolean shouldRemove = !threadInfo.isActive.get() || \n                                     (currentTime - threadInfo.lastActivity.get()) > 30000;\n                \n                if (shouldRemove) {\n                    InternalLogger.d(TAG, \"Removing inactive thread from tracking: \" + threadInfo.name);\n                }\n                \n                return shouldRemove;\n            });\n        } catch (Exception e) {\n            InternalLogger.e(TAG, \"Error cleaning up inactive threads\", e);\n        }\n    }\n    \n    /**\n     * Register a background thread for monitoring\n     */\n    public void registerBackgroundThread(String threadName) {\n        try {\n            backgroundThreadCount.incrementAndGet();\n            ThreadInfo threadInfo = new ThreadInfo(threadName);\n            activeThreads.put(threadName, threadInfo);\n            \n            InternalLogger.d(TAG, \"Registered background thread: \" + threadName + \n                           \", total: \" + backgroundThreadCount.get());\n        } catch (Exception e) {\n            InternalLogger.e(TAG, \"Error registering background thread\", e);\n        }\n    }\n    \n    /**\n     * Unregister a background thread\n     */\n    public void unregisterBackgroundThread(String threadName) {\n        try {\n            ThreadInfo threadInfo = activeThreads.remove(threadName);\n            if (threadInfo != null) {\n                threadInfo.isActive.set(false);\n                backgroundThreadCount.decrementAndGet();\n                \n                InternalLogger.d(TAG, \"Unregistered background thread: \" + threadName + \n                               \", total: \" + backgroundThreadCount.get());\n            }\n        } catch (Exception e) {\n            InternalLogger.e(TAG, \"Error unregistering background thread\", e);\n        }\n    }\n    \n    /**\n     * Update thread activity\n     */\n    public void updateThreadActivity(String threadName) {\n        try {\n            ThreadInfo threadInfo = activeThreads.get(threadName);\n            if (threadInfo != null) {\n                threadInfo.updateActivity();\n            }\n        } catch (Exception e) {\n            InternalLogger.e(TAG, \"Error updating thread activity\", e);\n        }\n    }\n    \n    /**\n     * Log thread dump for debugging\n     */\n    private void logThreadDump() {\n        try {\n            StringBuilder dump = new StringBuilder();\n            dump.append(\"=== THREAD DUMP ===\\n\");\n            dump.append(\"Timestamp: \").append(System.currentTimeMillis()).append(\"\\n\");\n            dump.append(\"Active Threads: \").append(activeThreads.size()).append(\"\\n\");\n            dump.append(\"Background Thread Count: \").append(backgroundThreadCount.get()).append(\"\\n\");\n            \n            for (ThreadInfo threadInfo : activeThreads.values()) {\n                dump.append(\"Thread: \").append(threadInfo.name)\n                    .append(\", Created: \").append(threadInfo.createdTime)\n                    .append(\", Last Activity: \").append(threadInfo.lastActivity.get())\n                    .append(\", Active: \").append(threadInfo.isActive.get())\n                    .append(\"\\n\");\n            }\n            \n            dump.append(\"=== END THREAD DUMP ===\\n\");\n            \n            InternalLogger.e(TAG, dump.toString());\n            \n        } catch (Exception e) {\n            InternalLogger.e(TAG, \"Error creating thread dump\", e);\n        }\n    }\n    \n    /**\n     * Log system state information\n     */\n    private void logSystemState() {\n        try {\n            Runtime runtime = Runtime.getRuntime();\n            long maxMemory = runtime.maxMemory();\n            long totalMemory = runtime.totalMemory();\n            long freeMemory = runtime.freeMemory();\n            long usedMemory = totalMemory - freeMemory;\n            \n            StringBuilder state = new StringBuilder();\n            state.append(\"=== SYSTEM STATE ===\\n\");\n            state.append(\"Max Memory: \").append(maxMemory / 1024 / 1024).append(\" MB\\n\");\n            state.append(\"Total Memory: \").append(totalMemory / 1024 / 1024).append(\" MB\\n\");\n            state.append(\"Used Memory: \").append(usedMemory / 1024 / 1024).append(\" MB\\n\");\n            state.append(\"Free Memory: \").append(freeMemory / 1024 / 1024).append(\" MB\\n\");\n            state.append(\"Available Processors: \").append(runtime.availableProcessors()).append(\"\\n\");\n            state.append(\"=== END SYSTEM STATE ===\\n\");\n            \n            InternalLogger.i(TAG, state.toString());\n            \n        } catch (Exception e) {\n            InternalLogger.e(TAG, \"Error logging system state\", e);\n        }\n    }\n    \n    /**\n     * Get current thread safety status\n     */\n    public ThreadSafetyStatus getStatus() {\n        boolean isMainThreadResponsive = (System.currentTimeMillis() - lastMainThreadActivity.get()) < ANR_THRESHOLD;\n        boolean hasStuckThreads = activeThreads.values().stream().anyMatch(ThreadInfo::isStuck);\n        boolean tooManyBackgroundThreads = backgroundThreadCount.get() > MAX_BACKGROUND_THREADS;\n        \n        return new ThreadSafetyStatus(\n            isMainThreadResponsive,\n            !hasStuckThreads,\n            !tooManyBackgroundThreads,\n            backgroundThreadCount.get(),\n            activeThreads.size(),\n            anrDetected.get()\n        );\n    }\n    \n    /**\n     * Thread safety status information\n     */\n    public static class ThreadSafetyStatus {\n        public final boolean mainThreadResponsive;\n        public final boolean noStuckThreads;\n        public final boolean backgroundThreadCountOk;\n        public final int backgroundThreadCount;\n        public final int activeThreadCount;\n        public final boolean anrDetected;\n        \n        ThreadSafetyStatus(boolean mainThreadResponsive, boolean noStuckThreads, \n                          boolean backgroundThreadCountOk, int backgroundThreadCount, \n                          int activeThreadCount, boolean anrDetected) {\n            this.mainThreadResponsive = mainThreadResponsive;\n            this.noStuckThreads = noStuckThreads;\n            this.backgroundThreadCountOk = backgroundThreadCountOk;\n            this.backgroundThreadCount = backgroundThreadCount;\n            this.activeThreadCount = activeThreadCount;\n            this.anrDetected = anrDetected;\n        }\n        \n        public boolean isHealthy() {\n            return mainThreadResponsive && noStuckThreads && backgroundThreadCountOk && !anrDetected;\n        }\n        \n        @Override\n        public String toString() {\n            return \"ThreadSafetyStatus{\" +\n                    \"mainThreadResponsive=\" + mainThreadResponsive +\n                    \", noStuckThreads=\" + noStuckThreads +\n                    \", backgroundThreadCountOk=\" + backgroundThreadCountOk +\n                    \", backgroundThreadCount=\" + backgroundThreadCount +\n                    \", activeThreadCount=\" + activeThreadCount +\n                    \", anrDetected=\" + anrDetected +\n                    \", healthy=\" + isHealthy() +\n                    '}';\n        }\n    }\n}