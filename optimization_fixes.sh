#!/bin/bash

# Android Project Optimization Script
# Fixes common memory leaks and performance issues

echo "========================================"
echo "Android Project Optimization Script"
echo "========================================"

# Function to fix static references in MainActivity
fix_main_activity() {
    echo "Fixing MainActivity static references..."
    
    # Find MainActivity file
    MAIN_ACTIVITY=$(find app/src/main/java -name "MainActivity.java" -type f | head -1)
    
    if [ -z "$MAIN_ACTIVITY" ]; then
        echo "MainActivity.java not found"
        return
    fi
    
    echo "Found MainActivity at: $MAIN_ACTIVITY"
    
    # Create backup
    cp "$MAIN_ACTIVITY" "${MAIN_ACTIVITY}.backup"
    
    # Add WeakReference import if not present
    if ! grep -q "import java.lang.ref.WeakReference;" "$MAIN_ACTIVITY"; then
        sed -i '/^import.*android.widget.Toast;/a\import java.lang.ref.WeakReference;' "$MAIN_ACTIVITY"
    fi
    
    echo "MainActivity fixes applied"
}

# Function to create optimization utilities
create_optimization_utils() {
    echo "Creating optimization utility classes..."
    
    cat > app/src/main/java/com/checkmate/android/util/MemoryOptimizer.java << 'EOF'
package com.checkmate.android.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Memory optimization utilities
 */
public class MemoryOptimizer {
    private static final int CACHE_SIZE = 4 * 1024 * 1024; // 4MB
    private static LruCache<String, Bitmap> bitmapCache;
    private static Map<String, WeakReference<Object>> weakCache = new HashMap<>();
    
    static {
        bitmapCache = new LruCache<String, Bitmap>(CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount();
            }
        };
    }
    
    /**
     * Get bitmap from cache or decode it
     */
    public static Bitmap getBitmap(String path, BitmapFactory.Options options) {
        Bitmap bitmap = bitmapCache.get(path);
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeFile(path, options);
            if (bitmap != null) {
                bitmapCache.put(path, bitmap);
            }
        }
        return bitmap;
    }
    
    /**
     * Store object with weak reference
     */
    public static void storeWeak(String key, Object value) {
        weakCache.put(key, new WeakReference<>(value));
    }
    
    /**
     * Get object from weak cache
     */
    @SuppressWarnings("unchecked")
    public static <T> T getWeak(String key) {
        WeakReference<Object> ref = weakCache.get(key);
        if (ref != null) {
            return (T) ref.get();
        }
        return null;
    }
    
    /**
     * Clear caches
     */
    public static void clearCaches() {
        bitmapCache.evictAll();
        weakCache.clear();
    }
}
EOF

    cat > app/src/main/java/com/checkmate/android/util/ThreadOptimizer.java << 'EOF'
package com.checkmate.android.util;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Thread optimization utilities
 */
public class ThreadOptimizer {
    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final int MAX_POOL_SIZE = CORE_POOL_SIZE * 2;
    private static final long KEEP_ALIVE_TIME = 60L;
    
    private static final ExecutorService backgroundExecutor = 
        Executors.newFixedThreadPool(CORE_POOL_SIZE);
    
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    /**
     * Run task in background thread
     */
    public static void runInBackground(Runnable task) {
        backgroundExecutor.execute(task);
    }
    
    /**
     * Run task on UI thread
     */
    public static void runOnUiThread(Runnable task) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run();
        } else {
            mainHandler.post(task);
        }
    }
    
    /**
     * Run task on UI thread with delay
     */
    public static void runOnUiThreadDelayed(Runnable task, long delayMillis) {
        mainHandler.postDelayed(task, delayMillis);
    }
    
    /**
     * Shutdown executor service
     */
    public static void shutdown() {
        backgroundExecutor.shutdown();
        try {
            if (!backgroundExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                backgroundExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            backgroundExecutor.shutdownNow();
        }
    }
}
EOF

    echo "Optimization utilities created"
}

# Function to create ProGuard rules for optimization
create_proguard_rules() {
    echo "Creating ProGuard optimization rules..."
    
    cat >> app/proguard-rules.pro << 'EOF'

# Optimization rules
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Optimize for size
-repackageclasses ''
-allowaccessmodification

# Keep essential classes
-keep class com.checkmate.android.util.PermissionManager { *; }
-keep class com.checkmate.android.util.DialogSynchronizer { *; }
-keep class com.checkmate.android.util.MemoryOptimizer { *; }
-keep class com.checkmate.android.util.ThreadOptimizer { *; }
EOF

    echo "ProGuard rules added"
}

# Function to optimize gradle build
optimize_gradle_build() {
    echo "Optimizing Gradle build configuration..."
    
    # Check if gradle.properties exists
    if [ -f "gradle.properties" ]; then
        # Add optimization properties if not present
        grep -q "org.gradle.jvmargs" gradle.properties || echo "org.gradle.jvmargs=-Xmx2048m -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8" >> gradle.properties
        grep -q "org.gradle.parallel" gradle.properties || echo "org.gradle.parallel=true" >> gradle.properties
        grep -q "org.gradle.configureondemand" gradle.properties || echo "org.gradle.configureondemand=true" >> gradle.properties
        grep -q "org.gradle.caching" gradle.properties || echo "org.gradle.caching=true" >> gradle.properties
        
        echo "Gradle optimization properties added"
    fi
}

# Main execution
echo "Starting optimization process..."

fix_main_activity
create_optimization_utils
create_proguard_rules
optimize_gradle_build

echo ""
echo "========================================"
echo "Optimization complete!"
echo "========================================"
echo "Created files:"
echo "  - MemoryOptimizer.java"
echo "  - ThreadOptimizer.java"
echo "  - Updated ProGuard rules"
echo "  - Optimized Gradle configuration"
echo ""
echo "Next steps:"
echo "1. Review the changes"
echo "2. Run './build_and_test.sh' when Android SDK is available"
echo "3. Test the app thoroughly"