package com.checkmate.android.util;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Memory optimization utility for 24/7 camera service operation
 */
public class MemoryOptimizer {
    private static final String TAG = "MemoryOptimizer";
    
    // Memory thresholds
    private static final long LOW_MEMORY_THRESHOLD = 50 * 1024 * 1024; // 50MB
    private static final long CRITICAL_MEMORY_THRESHOLD = 30 * 1024 * 1024; // 30MB
    private static final float MAX_HEAP_USAGE_RATIO = 0.85f; // 85% of max heap
    
    // Monitoring intervals
    private static final long MEMORY_CHECK_INTERVAL = 30000L; // 30 seconds
    private static final long AGGRESSIVE_GC_INTERVAL = 60000L; // 1 minute
    
    private static volatile MemoryOptimizer instance;
    
    private final WeakReference<Context> contextRef;
    private final Handler memoryCheckHandler;
    private final AtomicBoolean isMonitoring = new AtomicBoolean(false);
    private final List<MemoryListener> listeners = new ArrayList<>();
    
    private Runnable memoryCheckRunnable;
    private long lastGcTime = 0;
    
    public interface MemoryListener {
        void onLowMemory();
        void onCriticalMemory();
        void onMemoryRecovered();
    }
    
    private MemoryOptimizer(Context context) {
        this.contextRef = new WeakReference<>(context.getApplicationContext());
        this.memoryCheckHandler = new Handler(Looper.getMainLooper());
    }
    
    public static MemoryOptimizer getInstance(Context context) {
        if (instance == null) {
            synchronized (MemoryOptimizer.class) {
                if (instance == null) {
                    instance = new MemoryOptimizer(context);
                }
            }
        }
        return instance;
    }
    
    public void addMemoryListener(MemoryListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    
    public void removeMemoryListener(MemoryListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    
    /**
     * Start memory monitoring
     */
    public void startMonitoring() {
        if (isMonitoring.compareAndSet(false, true)) {
            Log.d(TAG, "Starting memory monitoring");
            setupMemoryCheck();
            memoryCheckHandler.post(memoryCheckRunnable);
        }
    }
    
    /**
     * Stop memory monitoring
     */
    public void stopMonitoring() {
        if (isMonitoring.compareAndSet(true, false)) {
            Log.d(TAG, "Stopping memory monitoring");
            memoryCheckHandler.removeCallbacksAndMessages(null);
        }
    }
    
    /**
     * Setup memory check runnable
     */
    private void setupMemoryCheck() {
        memoryCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isMonitoring.get()) return;
                
                checkMemoryStatus();
                
                // Schedule next check
                if (isMonitoring.get()) {
                    memoryCheckHandler.postDelayed(this, MEMORY_CHECK_INTERVAL);
                }
            }
        };
    }
    
    /**
     * Check current memory status
     */
    private void checkMemoryStatus() {
        MemoryInfo memInfo = getMemoryInfo();
        
        Log.d(TAG, String.format("Memory: Used=%dMB, Free=%dMB, Max=%dMB, Usage=%.1f%%",
                memInfo.usedMemory / 1024 / 1024,
                memInfo.freeMemory / 1024 / 1024,
                memInfo.maxMemory / 1024 / 1024,
                memInfo.usageRatio * 100));
        
        // Check if we're in critical memory state
        if (memInfo.availableMemory < CRITICAL_MEMORY_THRESHOLD) {
            Log.w(TAG, "Critical memory state detected!");
            handleCriticalMemory();
            notifyListeners(MemoryState.CRITICAL);
        }
        // Check if we're in low memory state
        else if (memInfo.availableMemory < LOW_MEMORY_THRESHOLD || 
                 memInfo.usageRatio > MAX_HEAP_USAGE_RATIO) {
            Log.w(TAG, "Low memory state detected!");
            handleLowMemory();
            notifyListeners(MemoryState.LOW);
        }
        // Memory recovered
        else if (memInfo.usageRatio < 0.7f) {
            notifyListeners(MemoryState.NORMAL);
        }
    }
    
    /**
     * Handle low memory situation
     */
    private void handleLowMemory() {
        // Perform garbage collection if enough time has passed
        if (System.currentTimeMillis() - lastGcTime > AGGRESSIVE_GC_INTERVAL) {
            Log.d(TAG, "Performing garbage collection");
            System.gc();
            System.runFinalization();
            lastGcTime = System.currentTimeMillis();
        }
        
        // Request memory trim
        Context context = contextRef.get();
        if (context != null) {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                am.clearApplicationUserData();
            }
        }
    }
    
    /**
     * Handle critical memory situation
     */
    private void handleCriticalMemory() {
        Log.w(TAG, "Handling critical memory - aggressive cleanup");
        
        // Force immediate garbage collection
        System.gc();
        System.runFinalization();
        System.gc(); // Second pass
        
        // Clear all bitmap caches
        clearBitmapCaches();
        
        // Trim memory aggressively
        trimMemory(80); // TRIM_MEMORY_RUNNING_CRITICAL
        
        lastGcTime = System.currentTimeMillis();
    }
    
    /**
     * Get current memory info
     */
    public MemoryInfo getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long availableMemory = maxMemory - usedMemory;
        
        float usageRatio = (float) usedMemory / maxMemory;
        
        // Get native memory info
        Debug.MemoryInfo debugMemInfo = new Debug.MemoryInfo();
        Debug.getMemoryInfo(debugMemInfo);
        
        return new MemoryInfo(
            usedMemory,
            freeMemory,
            maxMemory,
            availableMemory,
            usageRatio,
            debugMemInfo.getTotalPss() * 1024L // Convert KB to bytes
        );
    }
    
    /**
     * Optimize bitmap for memory efficiency
     */
    public static Bitmap optimizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        if (bitmap == null) return null;
        
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        // Calculate scale factor
        float scale = Math.min(
            (float) maxWidth / width,
            (float) maxHeight / height
        );
        
        if (scale >= 1.0f) {
            return bitmap; // No need to scale down
        }
        
        // Scale down bitmap
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);
        
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        
        // Recycle original if different
        if (scaledBitmap != bitmap) {
            bitmap.recycle();
        }
        
        return scaledBitmap;
    }
    
    /**
     * Clear bitmap caches
     */
    private void clearBitmapCaches() {
        // This would clear any bitmap caches in your app
        // Implementation depends on your caching strategy
        Log.d(TAG, "Clearing bitmap caches");
    }
    
    /**
     * Trim memory
     */
    public void trimMemory(int level) {
        Context context = contextRef.get();
        if (context instanceof android.app.Application) {
            ((android.app.Application) context).onTrimMemory(level);
        }
    }
    
    /**
     * Notify listeners about memory state
     */
    private void notifyListeners(MemoryState state) {
        synchronized (listeners) {
            for (MemoryListener listener : listeners) {
                try {
                    switch (state) {
                        case CRITICAL:
                            listener.onCriticalMemory();
                            break;
                        case LOW:
                            listener.onLowMemory();
                            break;
                        case NORMAL:
                            listener.onMemoryRecovered();
                            break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying listener", e);
                }
            }
        }
    }
    
    /**
     * Force cleanup
     */
    public void forceCleanup() {
        Log.d(TAG, "Forcing memory cleanup");
        handleCriticalMemory();
    }
    
    /**
     * Memory state enum
     */
    private enum MemoryState {
        NORMAL,
        LOW,
        CRITICAL
    }
    
    /**
     * Memory info class
     */
    public static class MemoryInfo {
        public final long usedMemory;
        public final long freeMemory;
        public final long maxMemory;
        public final long availableMemory;
        public final float usageRatio;
        public final long nativeMemory;
        
        public MemoryInfo(long usedMemory, long freeMemory, long maxMemory, 
                         long availableMemory, float usageRatio, long nativeMemory) {
            this.usedMemory = usedMemory;
            this.freeMemory = freeMemory;
            this.maxMemory = maxMemory;
            this.availableMemory = availableMemory;
            this.usageRatio = usageRatio;
            this.nativeMemory = nativeMemory;
        }
    }
    
    /**
     * Check if memory is low
     */
    public boolean isMemoryLow() {
        MemoryInfo info = getMemoryInfo();
        return info.availableMemory < LOW_MEMORY_THRESHOLD || 
               info.usageRatio > MAX_HEAP_USAGE_RATIO;
    }
    
    /**
     * Check if memory is critical
     */
    public boolean isMemoryCritical() {
        MemoryInfo info = getMemoryInfo();
        return info.availableMemory < CRITICAL_MEMORY_THRESHOLD;
    }
}