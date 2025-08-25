package com.checkmate.android.util;

import android.util.Log;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * GlobalResourcePool - Centralized resource management for optimal performance
 * 
 * This class provides optimized resource pooling and management to prevent
 * excessive object allocation and garbage collection, improving overall
 * application performance and reducing memory pressure.
 * 
 * Features:
 * - Thread pool optimization with intelligent sizing
 * - Object pooling for frequently used resources
 * - Memory management with automatic cleanup
 * - Performance monitoring and statistics
 * - Emergency resource cleanup when memory is low
 */
public class GlobalResourcePool {
    
    private static final String TAG = "GlobalResourcePool";
    
    // Singleton instance
    private static volatile GlobalResourcePool sInstance;
    private static final Object sLock = new Object();
    
    // Thread pools for different priority operations
    private final ExecutorService mHighPriorityExecutor;
    private final ExecutorService mNormalPriorityExecutor;
    private final ExecutorService mLowPriorityExecutor;
    
    // Resource pools
    private final BlockingQueue<StringBuilder> mStringBuilderPool;
    private final ConcurrentHashMap<String, Object> mSharedResourceCache;
    
    // Performance monitoring
    private final AtomicInteger mActiveThreadCount = new AtomicInteger(0);
    private final AtomicLong mTotalTasksExecuted = new AtomicLong(0);
    private final AtomicLong mPoolHits = new AtomicLong(0);
    private final AtomicLong mPoolMisses = new AtomicLong(0);
    
    private GlobalResourcePool() {
        // Initialize thread pools with optimal sizing
        int coreCount = Runtime.getRuntime().availableProcessors();
        
        // High priority: Real-time operations (streaming, recording)
        mHighPriorityExecutor = Executors.newFixedThreadPool(
            Math.max(2, coreCount / 2),
            createThreadFactory("HighPriority", Thread.MAX_PRIORITY)
        );
        
        // Normal priority: UI updates, user interactions
        mNormalPriorityExecutor = Executors.newFixedThreadPool(
            Math.max(2, coreCount),
            createThreadFactory("NormalPriority", Thread.NORM_PRIORITY)
        );
        
        // Low priority: Background tasks, cleanup
        mLowPriorityExecutor = Executors.newFixedThreadPool(
            Math.max(1, coreCount / 4),
            createThreadFactory("LowPriority", Thread.MIN_PRIORITY + 2)
        );
        
        // Initialize resource pools
        mStringBuilderPool = new ArrayBlockingQueue<>(50);
        mSharedResourceCache = new ConcurrentHashMap<>();
        
        // Pre-populate StringBuilder pool
        for (int i = 0; i < 20; i++) {
            mStringBuilderPool.offer(new StringBuilder(256));
        }
        
        Log.d(TAG, "🚀 GlobalResourcePool initialized with optimized thread pools and resource pools");
    }
    
    /**
     * Get singleton instance using double-checked locking
     */
    public static GlobalResourcePool getInstance() {
        if (sInstance == null) {
            synchronized (sLock) {
                if (sInstance == null) {
                    sInstance = new GlobalResourcePool();
                }
            }
        }
        return sInstance;
    }
    
    /**
     * Execute high priority task (streaming, recording, real-time operations)
     */
    public void executeHighPriority(Runnable task) {
        mActiveThreadCount.incrementAndGet();
        mHighPriorityExecutor.execute(() -> {
            try {
                task.run();
                mTotalTasksExecuted.incrementAndGet();
            } finally {
                mActiveThreadCount.decrementAndGet();
            }
        });
    }
    
    /**
     * Execute normal priority task (UI updates, user interactions)
     */
    public void executeNormalPriority(Runnable task) {
        mActiveThreadCount.incrementAndGet();
        mNormalPriorityExecutor.execute(() -> {
            try {
                task.run();
                mTotalTasksExecuted.incrementAndGet();
            } finally {
                mActiveThreadCount.decrementAndGet();
            }
        });
    }
    
    /**
     * Execute low priority task (background cleanup, maintenance)
     */
    public void executeLowPriority(Runnable task) {
        mActiveThreadCount.incrementAndGet();
        mLowPriorityExecutor.execute(() -> {
            try {
                task.run();
                mTotalTasksExecuted.incrementAndGet();
            } finally {
                mActiveThreadCount.decrementAndGet();
            }
        });
    }
    
    /**
     * Get a StringBuilder from the pool for efficient string operations
     */
    public StringBuilder borrowStringBuilder() {
        StringBuilder sb = mStringBuilderPool.poll();
        if (sb != null) {
            sb.setLength(0); // Reset for reuse
            mPoolHits.incrementAndGet();
            return sb;
        } else {
            mPoolMisses.incrementAndGet();
            return new StringBuilder(256);
        }
    }
    
    /**
     * Return a StringBuilder to the pool for reuse
     */
    public void returnStringBuilder(StringBuilder sb) {
        if (sb != null && sb.capacity() <= 1024) { // Don't pool overly large builders
            sb.setLength(0);
            mStringBuilderPool.offer(sb);
        }
    }
    
    /**
     * Cache a shared resource to avoid repeated creation
     */
    public void cacheResource(String key, Object resource) {
        if (key != null && resource != null) {
            mSharedResourceCache.put(key, resource);
        }
    }
    
    /**
     * Get a cached shared resource
     */
    @SuppressWarnings("unchecked")
    public <T> T getCachedResource(String key, Class<T> type) {
        Object resource = mSharedResourceCache.get(key);
        if (resource != null && type.isInstance(resource)) {
            return (T) resource;
        }
        return null;
    }
    
    /**
     * Emergency cleanup when system is under memory pressure
     */
    public void emergencyCleanup() {
        Log.w(TAG, "🧹 Performing emergency resource cleanup");
        
        // Clear resource cache
        mSharedResourceCache.clear();
        
        // Reduce StringBuilder pool size
        while (mStringBuilderPool.size() > 10) {
            mStringBuilderPool.poll();
        }
        
        // Request garbage collection
        System.gc();
        
        Log.d(TAG, "Emergency cleanup completed");
    }
    
    /**
     * Get performance statistics for monitoring
     */
    public String getPerformanceStats() {
        StringBuilder stats = borrowStringBuilder();
        try {
            stats.append("🔥 GlobalResourcePool Performance Stats:\n")
                 .append("Active Threads: ").append(mActiveThreadCount.get()).append("\n")
                 .append("Total Tasks: ").append(mTotalTasksExecuted.get()).append("\n")
                 .append("Pool Hits: ").append(mPoolHits.get()).append("\n")
                 .append("Pool Misses: ").append(mPoolMisses.get()).append("\n")
                 .append("Hit Rate: ").append(calculateHitRate()).append("%\n")
                 .append("StringBuilder Pool Size: ").append(mStringBuilderPool.size()).append("\n")
                 .append("Cached Resources: ").append(mSharedResourceCache.size());
            return stats.toString();
        } finally {
            returnStringBuilder(stats);
        }
    }
    
    /**
     * Calculate pool hit rate for performance monitoring
     */
    private double calculateHitRate() {
        long hits = mPoolHits.get();
        long misses = mPoolMisses.get();
        long total = hits + misses;
        return total > 0 ? (hits * 100.0) / total : 0.0;
    }
    
    /**
     * Create optimized thread factory with custom naming and priority
     */
    private ThreadFactory createThreadFactory(String namePrefix, int priority) {
        return new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "ResourcePool-" + namePrefix + "-" + threadNumber.getAndIncrement());
                t.setDaemon(false);
                t.setPriority(priority);
                return t;
            }
        };
    }
    
    /**
     * Shutdown all thread pools and cleanup resources
     */
    public void shutdown() {
        Log.d(TAG, "🛑 Shutting down GlobalResourcePool");
        
        mHighPriorityExecutor.shutdown();
        mNormalPriorityExecutor.shutdown();
        mLowPriorityExecutor.shutdown();
        
        mStringBuilderPool.clear();
        mSharedResourceCache.clear();
        
        Log.d(TAG, "GlobalResourcePool shutdown completed");
    }
}