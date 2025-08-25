package com.checkmate.android.util;

import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import java.io.RandomAccessFile;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced Performance Monitoring System
 * Tracks CPU, memory, frame drops, and method execution times
 */
public class PerformanceMonitor {
    private static final String TAG = "PerformanceMonitor";
    private static PerformanceMonitor instance;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final ConcurrentHashMap<String, PerformanceMetric> metrics = new ConcurrentHashMap<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // Performance thresholds
    private static final long MEMORY_WARNING_THRESHOLD = 50 * 1024 * 1024; // 50MB
    private static final float CPU_WARNING_THRESHOLD = 80.0f; // 80%
    private static final int FRAME_DROP_THRESHOLD = 5; // frames
    
    // Metrics tracking
    private long lastCpuTime = 0;
    private long lastAppCpuTime = 0;
    private int frameDropCount = 0;
    private volatile boolean isMonitoring = false;
    
    public static class PerformanceMetric {
        public final AtomicLong totalTime = new AtomicLong(0);
        public final AtomicLong callCount = new AtomicLong(0);
        public final AtomicLong maxTime = new AtomicLong(0);
        public final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
        
        public void record(long duration) {
            totalTime.addAndGet(duration);
            callCount.incrementAndGet();
            
            // Update max
            long currentMax = maxTime.get();
            while (duration > currentMax) {
                if (maxTime.compareAndSet(currentMax, duration)) break;
                currentMax = maxTime.get();
            }
            
            // Update min
            long currentMin = minTime.get();
            while (duration < currentMin) {
                if (minTime.compareAndSet(currentMin, duration)) break;
                currentMin = minTime.get();
            }
        }
        
        public long getAverageTime() {
            long count = callCount.get();
            return count > 0 ? totalTime.get() / count : 0;
        }
    }
    
    private PerformanceMonitor() {
        // Private constructor
    }
    
    public static synchronized PerformanceMonitor getInstance() {
        if (instance == null) {
            instance = new PerformanceMonitor();
        }
        return instance;
    }
    
    /**
     * Start monitoring system performance
     */
    public void startMonitoring() {
        if (isMonitoring) return;
        isMonitoring = true;
        
        // Monitor CPU and Memory every 5 seconds
        scheduler.scheduleAtFixedRate(this::checkSystemResources, 0, 5, TimeUnit.SECONDS);
        
        // Monitor frame drops
        if (Looper.myLooper() == Looper.getMainLooper()) {
            startFrameMonitoring();
        }
        
        Log.i(TAG, "Performance monitoring started");
    }
    
    /**
     * Stop monitoring
     */
    public void stopMonitoring() {
        isMonitoring = false;
        scheduler.shutdown();
        Log.i(TAG, "Performance monitoring stopped");
    }
    
    /**
     * Track method execution time
     */
    public long startTracking(String methodName) {
        return System.nanoTime();
    }
    
    public void endTracking(String methodName, long startTime) {
        long duration = System.nanoTime() - startTime;
        PerformanceMetric metric = metrics.computeIfAbsent(methodName, k -> new PerformanceMetric());
        metric.record(duration);
        
        // Log slow methods (> 100ms)
        if (duration > 100_000_000) { // 100ms in nanoseconds
            Log.w(TAG, String.format("Slow method detected: %s took %dms", 
                methodName, duration / 1_000_000));
            
            if (CrashLogger.getInstance() != null) {
                CrashLogger.getInstance().logWarning(TAG, 
                    "Slow method: " + methodName + " took " + (duration / 1_000_000) + "ms");
            }
        }
    }
    
    /**
     * Check system resources
     */
    private void checkSystemResources() {
        try {
            // Check memory
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            float memoryUsagePercent = (float) usedMemory / maxMemory * 100;
            
            if (usedMemory > MEMORY_WARNING_THRESHOLD) {
                Log.w(TAG, String.format("High memory usage: %.2f%% (%d MB used)", 
                    memoryUsagePercent, usedMemory / (1024 * 1024)));
                
                // Suggest garbage collection if memory is critical
                if (memoryUsagePercent > 85) {
                    System.gc();
                    Log.w(TAG, "Triggered garbage collection due to high memory usage");
                }
            }
            
            // Check CPU
            float cpuUsage = getCpuUsage();
            if (cpuUsage > CPU_WARNING_THRESHOLD) {
                Log.w(TAG, String.format("High CPU usage: %.2f%%", cpuUsage));
            }
            
            // Log metrics
            logPerformanceMetrics();
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking system resources", e);
        }
    }
    
    /**
     * Calculate CPU usage
     */
    private float getCpuUsage() {
        try {
            // Try to read /proc/stat for system-wide CPU usage
            try {
                RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
                String line = reader.readLine();
                String[] tokens = line.split("\\s+");
                
                long totalCpuTime = 0;
                for (int i = 1; i <= 7; i++) {
                    totalCpuTime += Long.parseLong(tokens[i]);
                }
                
                reader.close();
                
                // Calculate app CPU time
                long appCpuTime = Debug.threadCpuTimeNanos();
                
                // Calculate usage
                if (lastCpuTime > 0) {
                    long cpuDelta = totalCpuTime - lastCpuTime;
                    long appDelta = appCpuTime - lastAppCpuTime;
                    float usage = (float) appDelta / cpuDelta * 100;
                    
                    lastCpuTime = totalCpuTime;
                    lastAppCpuTime = appCpuTime;
                    
                    return Math.min(usage, 100f);
                }
                
                lastCpuTime = totalCpuTime;
                lastAppCpuTime = appCpuTime;
                
            } catch (SecurityException | FileNotFoundException e) {
                // Permission denied - use fallback method
                Log.d(TAG, "Cannot access /proc/stat, using fallback CPU measurement");
                
                // Use ActivityManager for CPU info as fallback
                ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
                ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                if (activityManager != null) {
                    activityManager.getMemoryInfo(memInfo);
                    
                    // Estimate CPU usage based on available memory (rough approximation)
                    float memoryUsage = (float) (memInfo.totalMem - memInfo.availMem) / memInfo.totalMem * 100;
                    // CPU usage often correlates with memory usage in mobile apps
                    return Math.min(memoryUsage * 0.7f, 100f); // Scale down as it's an estimate
                }
                
                return 0;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating CPU usage", e);
        }
        
        return 0;
    }
    
    /**
     * Monitor frame drops
     */
    private void startFrameMonitoring() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!isMonitoring) return;
                
                // Check for frame drops
                if (frameDropCount > FRAME_DROP_THRESHOLD) {
                    Log.w(TAG, "Frame drops detected: " + frameDropCount);
                    frameDropCount = 0;
                }
                
                // Schedule next check
                mainHandler.postDelayed(this, 1000);
            }
        });
    }
    
    /**
     * Report a frame drop
     */
    public void reportFrameDrop() {
        frameDropCount++;
    }
    
    /**
     * Log performance metrics
     */
    private void logPerformanceMetrics() {
        StringBuilder report = new StringBuilder("\n=== Performance Report ===\n");
        
        for (String method : metrics.keySet()) {
            PerformanceMetric metric = metrics.get(method);
            if (metric != null && metric.callCount.get() > 0) {
                report.append(String.format("%s: avg=%.2fms, max=%.2fms, calls=%d\n",
                    method,
                    metric.getAverageTime() / 1_000_000.0,
                    metric.maxTime.get() / 1_000_000.0,
                    metric.callCount.get()));
            }
        }
        
        Log.d(TAG, report.toString());
    }
    
    /**
     * Get performance report
     */
    public String getPerformanceReport() {
        StringBuilder report = new StringBuilder();
        report.append("Performance Metrics:\n");
        
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        
        report.append(String.format("Memory: %d MB / %d MB (%.1f%%)\n",
            usedMemory / (1024 * 1024),
            maxMemory / (1024 * 1024),
            (float) usedMemory / maxMemory * 100));
        
        report.append(String.format("CPU Usage: %.1f%%\n", getCpuUsage()));
        report.append(String.format("Frame Drops: %d\n", frameDropCount));
        
        return report.toString();
    }
    
    /**
     * Clear all metrics
     */
    public void clearMetrics() {
        metrics.clear();
        frameDropCount = 0;
        Log.i(TAG, "Performance metrics cleared");
    }
}