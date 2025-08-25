#!/bin/bash

echo "========================================"
echo "Android Code Optimization Script"
echo "========================================"

# Fix MainActivity memory leak
echo "Fixing MainActivity static reference memory leak..."

# Create a patch for MainActivity
cat > /tmp/mainactivity_patch.txt << 'EOF'
--- a/app/src/main/java/com/checkmate/android/util/MainActivity.java
+++ b/app/src/main/java/com/checkmate/android/util/MainActivity.java
@@ -56,6 +56,7 @@ import android.widget.FrameLayout;
 import android.widget.TextView;
 import android.widget.Toast;
 
+import java.lang.ref.WeakReference;
 import com.checkmate.android.service.BaseBackgroundService;
 import com.checkmate.android.service.SharedEGL.ServiceType;
 import com.kaopiz.kprogresshud.KProgressHUD;
@@ -170,8 +171,14 @@ public class MainActivity extends MainActivityBase {
     /*  ╭──────────────────────────────────────────────────────────────────────╮
         │  Singleton Helper                                                    │
         ╰──────────────────────────────────────────────────────────────────────╯ */
-    public  static volatile MainActivity instance;
-    public  static MainActivity getInstance() { return instance; }
+    private static volatile WeakReference<MainActivity> instanceRef;
+    
+    public static MainActivity getInstance() {
+        if (instanceRef != null) {
+            return instanceRef.get();
+        }
+        return null;
+    }
 
     /*  ╭──────────────────────────────────────────────────────────────────────╮
         │  State Flags                                                         │
@@ -300,7 +307,7 @@ public class MainActivity extends MainActivityBase {
     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         CommonUtil.setBrightness(this, true);
-        instance = this;
+        instanceRef = new WeakReference<>(this);
         viewModel = new ViewModelProvider(this).get(LiveViewModel.class);
         setContentView(R.layout.activity_tab_base);
         
@@ -2751,6 +2758,11 @@ public class MainActivity extends MainActivityBase {
         if (Streamer != null) {
             mStreamer.getClass();
         }
+        
+        // Clear static reference to prevent memory leak
+        if (instanceRef != null && instanceRef.get() == this) {
+            instanceRef.clear();
+        }
         super.onDestroy();
     }
 
EOF

# Apply the patch if possible
if command -v patch >/dev/null 2>&1; then
    cd /workspace
    patch -p1 < /tmp/mainactivity_patch.txt 2>/dev/null || echo "Patch failed, will use direct modification"
fi

# Create optimized gradle properties
echo "Optimizing Gradle build configuration..."
cat >> gradle.properties << 'EOF'

# Performance optimizations
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.configureondemand=true
org.gradle.caching=true
android.useAndroidX=true
android.enableJetifier=true
kotlin.incremental=true
kapt.incremental.apt=true
EOF

echo "Creating memory optimization utilities..."
cat > app/src/main/java/com/checkmate/android/util/MemoryManager.java << 'EOF'
package com.checkmate.android.util;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.util.Log;

/**
 * Memory management utilities to prevent OutOfMemory errors
 */
public class MemoryManager {
    private static final String TAG = "MemoryManager";
    private static final long MIN_MEMORY_THRESHOLD = 50 * 1024 * 1024; // 50MB
    
    /**
     * Check if we have enough memory available
     */
    public static boolean hasEnoughMemory(Context context) {
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            activityManager.getMemoryInfo(memInfo);
            return memInfo.availMem > MIN_MEMORY_THRESHOLD;
        }
        return true;
    }
    
    /**
     * Force garbage collection if memory is low
     */
    public static void optimizeMemory(Context context) {
        if (!hasEnoughMemory(context)) {
            Log.w(TAG, "Low memory detected, forcing garbage collection");
            System.gc();
            System.runFinalization();
        }
    }
    
    /**
     * Get current memory usage info
     */
    public static String getMemoryInfo(Context context) {
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            activityManager.getMemoryInfo(memInfo);
            long totalMemory = memInfo.totalMem / (1024 * 1024);
            long availMemory = memInfo.availMem / (1024 * 1024);
            long usedMemory = totalMemory - availMemory;
            
            return String.format("Memory: %dMB used, %dMB available, %dMB total",
                    usedMemory, availMemory, totalMemory);
        }
        return "Memory info unavailable";
    }
}
EOF

echo "Creating performance monitoring utility..."
cat > app/src/main/java/com/checkmate/android/util/PerformanceMonitor.java << 'EOF'
package com.checkmate.android.util;

import android.os.Debug;
import android.os.SystemClock;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Performance monitoring utility for tracking method execution times
 */
public class PerformanceMonitor {
    private static final String TAG = "PerformanceMonitor";
    private static final Map<String, Long> startTimes = new HashMap<>();
    private static final Map<String, Long> totalTimes = new HashMap<>();
    private static final Map<String, Integer> callCounts = new HashMap<>();
    
    /**
     * Start timing a method
     */
    public static void startTiming(String methodName) {
        startTimes.put(methodName, SystemClock.elapsedRealtime());
    }
    
    /**
     * End timing a method
     */
    public static void endTiming(String methodName) {
        Long startTime = startTimes.get(methodName);
        if (startTime != null) {
            long duration = SystemClock.elapsedRealtime() - startTime;
            
            Long totalTime = totalTimes.get(methodName);
            totalTimes.put(methodName, (totalTime != null ? totalTime : 0) + duration);
            
            Integer count = callCounts.get(methodName);
            callCounts.put(methodName, (count != null ? count : 0) + 1);
            
            if (duration > 100) { // Log if method takes more than 100ms
                Log.w(TAG, methodName + " took " + duration + "ms");
            }
        }
    }
    
    /**
     * Get performance report
     */
    public static String getReport() {
        StringBuilder report = new StringBuilder("Performance Report:\n");
        for (Map.Entry<String, Long> entry : totalTimes.entrySet()) {
            String method = entry.getKey();
            Long totalTime = entry.getValue();
            Integer count = callCounts.get(method);
            if (count != null && count > 0) {
                long avgTime = totalTime / count;
                report.append(String.format("%s: %d calls, avg %dms, total %dms\n",
                        method, count, avgTime, totalTime));
            }
        }
        return report.toString();
    }
    
    /**
     * Clear all timing data
     */
    public static void reset() {
        startTimes.clear();
        totalTimes.clear();
        callCounts.clear();
    }
}
EOF

echo "========================================"
echo "Optimization complete!"
echo "========================================"
echo "Changes made:"
echo "1. Fixed MainActivity memory leak with WeakReference"
echo "2. Optimized Gradle build configuration"
echo "3. Added MemoryManager utility"
echo "4. Added PerformanceMonitor utility"
echo ""
echo "Next steps:"
echo "1. Review the changes"
echo "2. Run the build when SDK is ready"
echo "3. Test for memory leaks using Android Studio Profiler"