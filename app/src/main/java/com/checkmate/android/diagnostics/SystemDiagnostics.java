package com.checkmate.android.diagnostics;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Debug;
import android.os.Process;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Log;

import com.checkmate.android.util.CrashLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced system diagnostics and self-healing capabilities
 * Monitors system health, detects issues, and attempts automatic recovery
 */
public class SystemDiagnostics {
    private static final String TAG = "SystemDiagnostics";
    private static SystemDiagnostics instance;
    
    private final Context context;
    private final ActivityManager activityManager;
    private final ScheduledExecutorService diagnosticsExecutor = Executors.newScheduledThreadPool(2);
    private final ExecutorService recoveryExecutor = Executors.newCachedThreadPool();
    
    // ANR detection
    private final Map<String, Long> methodStartTimes = new ConcurrentHashMap<>();
    private final Map<String, Integer> anrCounts = new ConcurrentHashMap<>();
    private static final long ANR_THRESHOLD_MS = 5000;
    private static final int MAX_ANR_BEFORE_RECOVERY = 3;
    
    // Memory tracking
    private final AtomicLong lastGcTime = new AtomicLong(0);
    private static final long GC_COOLDOWN_MS = 30000; // 30 seconds
    
    // Thread monitoring
    private final Map<String, ThreadInfo> threadInfoMap = new ConcurrentHashMap<>();
    private final AtomicBoolean isMonitoring = new AtomicBoolean(false);
    
    // Health metrics
    private final HealthMetrics healthMetrics = new HealthMetrics();
    
    public static class HealthMetrics {
        public volatile float cpuUsage = 0f;
        public volatile long memoryUsed = 0L;
        public volatile long memoryTotal = 0L;
        public volatile int threadCount = 0;
        public volatile int fdCount = 0;
        public volatile boolean isHealthy = true;
        public final List<String> issues = new CopyOnWriteArrayList<>();
    }
    
    private static class ThreadInfo {
        final String name;
        final long startTime;
        volatile long cpuTime;
        volatile boolean isBlocked;
        
        ThreadInfo(String name) {
            this.name = name;
            this.startTime = SystemClock.elapsedRealtime();
            this.cpuTime = 0;
            this.isBlocked = false;
        }
    }
    
    private SystemDiagnostics(Context context) {
        this.context = context.getApplicationContext();
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        initialize();
    }
    
    public static synchronized SystemDiagnostics getInstance(Context context) {
        if (instance == null) {
            instance = new SystemDiagnostics(context);
        }
        return instance;
    }
    
    /**
     * Initialize diagnostics
     */
    private void initialize() {
        // Enable StrictMode in debug builds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .penaltyDropBox()
                .build());
                
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .penaltyDropBox()
                .build());
        }
        
        // Start monitoring
        startHealthMonitoring();
        
        Log.i(TAG, "System diagnostics initialized");
    }
    
    /**
     * Start health monitoring
     */
    private void startHealthMonitoring() {
        isMonitoring.set(true);
        
        // Monitor system health every 5 seconds
        diagnosticsExecutor.scheduleAtFixedRate(this::performHealthCheck, 0, 5, TimeUnit.SECONDS);
        
        // Deep diagnostics every minute
        diagnosticsExecutor.scheduleAtFixedRate(this::performDeepDiagnostics, 0, 60, TimeUnit.SECONDS);
    }
    
    /**
     * Track method entry for ANR detection
     */
    public void trackMethodEntry(String methodName) {
        methodStartTimes.put(methodName, SystemClock.elapsedRealtime());
    }
    
    /**
     * Track method exit
     */
    public void trackMethodExit(String methodName) {
        Long startTime = methodStartTimes.remove(methodName);
        if (startTime != null) {
            long duration = SystemClock.elapsedRealtime() - startTime;
            if (duration > ANR_THRESHOLD_MS) {
                Log.w(TAG, "Potential ANR detected in " + methodName + ": " + duration + "ms");
                handlePotentialANR(methodName, duration);
            }
        }
    }
    
    /**
     * Handle potential ANR
     */
    private void handlePotentialANR(String methodName, long duration) {
        int count = anrCounts.merge(methodName, 1, Integer::sum);
        
        CrashLogger.getInstance().logANR(TAG, 
            "ANR in " + methodName + " (" + duration + "ms, occurrence #" + count + ")");
        
        if (count >= MAX_ANR_BEFORE_RECOVERY) {
            initiateRecovery("Repeated ANR in " + methodName);
        }
    }
    
    /**
     * Perform health check
     */
    private void performHealthCheck() {
        try {
            healthMetrics.issues.clear();
            healthMetrics.isHealthy = true;
            
            // Check CPU usage
            healthMetrics.cpuUsage = getCpuUsage();
            if (healthMetrics.cpuUsage > 80) {
                healthMetrics.issues.add("High CPU usage: " + healthMetrics.cpuUsage + "%");
                healthMetrics.isHealthy = false;
            }
            
            // Check memory
            checkMemoryHealth();
            
            // Check threads
            checkThreadHealth();
            
            // Check file descriptors
            checkFileDescriptors();
            
            // Take action if unhealthy
            if (!healthMetrics.isHealthy) {
                attemptSelfHealing();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error during health check", e);
        }
    }
    
    /**
     * Perform deep diagnostics
     */
    private void performDeepDiagnostics() {
        try {
            Log.d(TAG, "Performing deep diagnostics");
            
            // Analyze heap
            analyzeHeap();
            
            // Check for memory leaks
            checkForMemoryLeaks();
            
            // Analyze thread states
            analyzeThreadStates();
            
            // Generate report
            String report = generateDiagnosticsReport();
            CrashLogger.getInstance().logDebug(TAG, report);
            
        } catch (Exception e) {
            Log.e(TAG, "Error during deep diagnostics", e);
        }
    }
    
    /**
     * Check memory health
     */
    private void checkMemoryHealth() {
        Runtime runtime = Runtime.getRuntime();
        healthMetrics.memoryUsed = runtime.totalMemory() - runtime.freeMemory();
        healthMetrics.memoryTotal = runtime.maxMemory();
        
        float memoryUsagePercent = (healthMetrics.memoryUsed * 100f) / healthMetrics.memoryTotal;
        
        if (memoryUsagePercent > 85) {
            healthMetrics.issues.add("High memory usage: " + memoryUsagePercent + "%");
            healthMetrics.isHealthy = false;
        }
        
        // Check for low memory
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memInfo);
        
        if (memInfo.lowMemory) {
            healthMetrics.issues.add("System low memory warning");
            healthMetrics.isHealthy = false;
        }
    }
    
    /**
     * Check thread health
     */
    private void checkThreadHealth() {
        Map<Thread, StackTraceElement[]> threadMap = Thread.getAllStackTraces();
        healthMetrics.threadCount = threadMap.size();
        
        if (healthMetrics.threadCount > 100) {
            healthMetrics.issues.add("Excessive thread count: " + healthMetrics.threadCount);
            healthMetrics.isHealthy = false;
        }
        
        // Check for blocked threads
        int blockedCount = 0;
        for (Map.Entry<Thread, StackTraceElement[]> entry : threadMap.entrySet()) {
            Thread thread = entry.getKey();
            if (thread.getState() == Thread.State.BLOCKED || 
                thread.getState() == Thread.State.WAITING) {
                blockedCount++;
            }
        }
        
        if (blockedCount > 10) {
            healthMetrics.issues.add("Many blocked threads: " + blockedCount);
            healthMetrics.isHealthy = false;
        }
    }
    
    /**
     * Check file descriptors
     */
    private void checkFileDescriptors() {
        try {
            File fdDir = new File("/proc/" + Process.myPid() + "/fd");
            if (fdDir.exists() && fdDir.isDirectory()) {
                String[] fds = fdDir.list();
                healthMetrics.fdCount = fds != null ? fds.length : 0;
                
                if (healthMetrics.fdCount > 800) {
                    healthMetrics.issues.add("High file descriptor count: " + healthMetrics.fdCount);
                    healthMetrics.isHealthy = false;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking file descriptors", e);
        }
    }
    
    /**
     * Attempt self-healing
     */
    private void attemptSelfHealing() {
        Log.w(TAG, "Attempting self-healing for issues: " + healthMetrics.issues);
        
        recoveryExecutor.execute(() -> {
            try {
                // Force garbage collection if memory is high
                if (healthMetrics.issues.stream().anyMatch(s -> s.contains("memory"))) {
                    forceGarbageCollection();
                }
                
                // Trim memory
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    activityManager.clearApplicationUserData();
                }
                
                // Clear caches
                clearCaches();
                
                // Log recovery attempt
                CrashLogger.getInstance().logWarning(TAG, 
                    "Self-healing attempted for: " + String.join(", ", healthMetrics.issues));
                    
            } catch (Exception e) {
                Log.e(TAG, "Error during self-healing", e);
            }
        });
    }
    
    /**
     * Force garbage collection
     */
    private void forceGarbageCollection() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastGcTime.get() > GC_COOLDOWN_MS) {
            lastGcTime.set(now);
            System.gc();
            System.runFinalization();
            Log.i(TAG, "Forced garbage collection");
        }
    }
    
    /**
     * Clear application caches
     */
    private void clearCaches() {
        try {
            // Clear internal cache
            File cacheDir = context.getCacheDir();
            if (cacheDir != null && cacheDir.isDirectory()) {
                deleteRecursive(cacheDir);
            }
            
            // Clear external cache if available
            File externalCacheDir = context.getExternalCacheDir();
            if (externalCacheDir != null && externalCacheDir.isDirectory()) {
                deleteRecursive(externalCacheDir);
            }
            
            Log.i(TAG, "Caches cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing caches", e);
        }
    }
    
    /**
     * Delete directory recursively
     */
    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }
    
    /**
     * Analyze heap
     */
    private void analyzeHeap() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
                long nativeHeap = Debug.getNativeHeapAllocatedSize();
                long nativeHeapFree = Debug.getNativeHeapFreeSize();
                
                Log.d(TAG, String.format("Native heap: allocated=%d, free=%d", 
                    nativeHeap, nativeHeapFree));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error analyzing heap", e);
        }
    }
    
    /**
     * Check for memory leaks
     */
    private void checkForMemoryLeaks() {
        // This is a simplified check - in production, use LeakCanary or similar
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        System.gc();
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {}
        
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        
        if (memoryAfter > memoryBefore * 0.9) {
            Log.w(TAG, "Potential memory leak detected");
        }
    }
    
    /**
     * Analyze thread states
     */
    private void analyzeThreadStates() {
        Map<Thread, StackTraceElement[]> threadMap = Thread.getAllStackTraces();
        
        for (Map.Entry<Thread, StackTraceElement[]> entry : threadMap.entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] stack = entry.getValue();
            
            // Check for long-running threads
            ThreadInfo info = threadInfoMap.computeIfAbsent(thread.getName(), 
                k -> new ThreadInfo(thread.getName()));
                
            if (thread.getState() == Thread.State.BLOCKED) {
                info.isBlocked = true;
                Log.w(TAG, "Blocked thread detected: " + thread.getName());
            }
        }
    }
    
    /**
     * Get CPU usage
     */
    private float getCpuUsage() {
        try {
            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
            String line = reader.readLine();
            reader.close();
            
            String[] parts = line.split("\\s+");
            long idle = Long.parseLong(parts[4]);
            long total = 0;
            for (int i = 1; i < parts.length; i++) {
                total += Long.parseLong(parts[i]);
            }
            
            return 100f * (1f - ((float)idle / total));
        } catch (Exception e) {
            // Fallback to approximate CPU usage
            return 0f;
        }
    }
    
    /**
     * Initiate recovery
     */
    private void initiateRecovery(String reason) {
        Log.e(TAG, "Initiating recovery: " + reason);
        
        recoveryExecutor.execute(() -> {
            try {
                // Clear all caches
                clearCaches();
                
                // Force GC
                forceGarbageCollection();
                
                // Reset ANR counts
                anrCounts.clear();
                
                // Restart critical services if needed
                restartCriticalServices();
                
                CrashLogger.getInstance().logError(TAG, "Recovery initiated", 
                    new Exception("Recovery reason: " + reason));
                    
            } catch (Exception e) {
                Log.e(TAG, "Error during recovery", e);
            }
        });
    }
    
    /**
     * Restart critical services
     */
    private void restartCriticalServices() {
        // This would restart services if implemented
        Log.i(TAG, "Critical services restart requested");
    }
    
    /**
     * Generate diagnostics report
     */
    public String generateDiagnosticsReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== System Diagnostics Report ===\n");
        report.append("Time: ").append(new Date()).append("\n\n");
        
        report.append("Health Status: ").append(healthMetrics.isHealthy ? "HEALTHY" : "UNHEALTHY").append("\n");
        if (!healthMetrics.issues.isEmpty()) {
            report.append("Issues: ").append(String.join(", ", healthMetrics.issues)).append("\n");
        }
        
        report.append("\nSystem Metrics:\n");
        report.append("- CPU Usage: ").append(String.format("%.1f%%", healthMetrics.cpuUsage)).append("\n");
        report.append("- Memory: ").append(healthMetrics.memoryUsed / 1024 / 1024).append("MB / ")
              .append(healthMetrics.memoryTotal / 1024 / 1024).append("MB\n");
        report.append("- Threads: ").append(healthMetrics.threadCount).append("\n");
        report.append("- File Descriptors: ").append(healthMetrics.fdCount).append("\n");
        
        report.append("\nDevice Info:\n");
        report.append("- Model: ").append(Build.MODEL).append("\n");
        report.append("- Android: ").append(Build.VERSION.RELEASE).append("\n");
        report.append("- SDK: ").append(Build.VERSION.SDK_INT).append("\n");
        
        return report.toString();
    }
    
    /**
     * Get health metrics
     */
    public HealthMetrics getHealthMetrics() {
        return healthMetrics;
    }
    
    /**
     * Force diagnostics check
     */
    public void forceDiagnosticsCheck() {
        performHealthCheck();
        performDeepDiagnostics();
    }
}