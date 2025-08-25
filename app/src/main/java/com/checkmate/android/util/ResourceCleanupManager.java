package com.checkmate.android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.Closeable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages resource cleanup and prevents memory leaks
 */
public class ResourceCleanupManager {
    private static final String TAG = "ResourceCleanupManager";
    private static ResourceCleanupManager instance;
    
    private final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();
    private final Set<ResourceReference> trackedReferences = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    private static final long CLEANUP_INTERVAL = 60000; // 1 minute
    
    private static class ResourceReference extends WeakReference<Object> {
        private final String resourceName;
        private final Runnable cleanupAction;
        
        ResourceReference(Object referent, ReferenceQueue<Object> queue, String name, Runnable cleanup) {
            super(referent, queue);
            this.resourceName = name;
            this.cleanupAction = cleanup;
        }
    }
    
    private ResourceCleanupManager() {
        startCleanupMonitor();
    }
    
    public static synchronized ResourceCleanupManager getInstance() {
        if (instance == null) {
            instance = new ResourceCleanupManager();
        }
        return instance;
    }
    
    /**
     * Track a resource for automatic cleanup
     */
    public void trackResource(Object resource, String name, Runnable cleanupAction) {
        if (resource == null) return;
        
        ResourceReference ref = new ResourceReference(resource, referenceQueue, name, cleanupAction);
        trackedReferences.add(ref);
        
        Log.d(TAG, "Tracking resource: " + name);
    }
    
    /**
     * Track a bitmap for automatic recycling
     */
    public void trackBitmap(Bitmap bitmap, String name) {
        if (bitmap == null || bitmap.isRecycled()) return;
        
        trackResource(bitmap, name, () -> {
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
                Log.d(TAG, "Recycled bitmap: " + name);
            }
        });
    }
    
    /**
     * Track a closeable resource
     */
    public void trackCloseable(Closeable closeable, String name) {
        if (closeable == null) return;
        
        trackResource(closeable, name, () -> {
            try {
                closeable.close();
                Log.d(TAG, "Closed resource: " + name);
            } catch (Exception e) {
                Log.e(TAG, "Error closing resource: " + name, e);
            }
        });
    }
    
    /**
     * Start monitoring for resources that need cleanup
     */
    private void startCleanupMonitor() {
        // Monitor reference queue for garbage collected objects
        cleanupExecutor.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Reference<?> ref = referenceQueue.remove();
                    if (ref instanceof ResourceReference) {
                        ResourceReference resourceRef = (ResourceReference) ref;
                        performCleanup(resourceRef);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        // Periodic cleanup check
        cleanupExecutor.scheduleAtFixedRate(this::performPeriodicCleanup, 
            CLEANUP_INTERVAL, CLEANUP_INTERVAL, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Perform cleanup for a specific resource
     */
    private void performCleanup(ResourceReference ref) {
        try {
            trackedReferences.remove(ref);
            
            if (ref.cleanupAction != null) {
                ref.cleanupAction.run();
            }
            
            Log.d(TAG, "Cleaned up resource: " + ref.resourceName);
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup: " + ref.resourceName, e);
        }
    }
    
    /**
     * Perform periodic cleanup of dead references
     */
    private void performPeriodicCleanup() {
        int cleaned = 0;
        Set<ResourceReference> toRemove = new HashSet<>();
        
        for (ResourceReference ref : trackedReferences) {
            if (ref.get() == null) {
                toRemove.add(ref);
                performCleanup(ref);
                cleaned++;
            }
        }
        
        trackedReferences.removeAll(toRemove);
        
        if (cleaned > 0) {
            Log.d(TAG, "Periodic cleanup: cleaned " + cleaned + " resources");
        }
    }
    
    /**
     * Force cleanup of all tracked resources
     */
    public void forceCleanup() {
        Log.d(TAG, "Forcing cleanup of all tracked resources");
        
        for (ResourceReference ref : trackedReferences) {
            performCleanup(ref);
        }
        
        trackedReferences.clear();
    }
    
    /**
     * Safely execute cleanup on appropriate thread
     */
    public static void safeCleanup(Runnable cleanup) {
        if (cleanup == null) return;
        
        try {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                cleanup.run();
            } else {
                new Handler(Looper.getMainLooper()).post(cleanup);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during safe cleanup", e);
        }
    }
    
    /**
     * Shutdown the cleanup manager
     */
    public void shutdown() {
        try {
            forceCleanup();
            cleanupExecutor.shutdown();
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during shutdown", e);
        }
    }
}