#!/bin/bash

echo "Fixing all compilation errors..."

# 1. Fix PerformanceMonitor.java
echo "Fixing PerformanceMonitor.java..."
# Add missing imports
sed -i '1a\
import android.app.ActivityManager;\
import android.content.Context;\
import java.io.FileNotFoundException;' /workspace/app/src/main/java/com/checkmate/android/util/PerformanceMonitor.java

# Add context field
sed -i '/private static PerformanceMonitor instance;/a\    private final Context context;' /workspace/app/src/main/java/com/checkmate/android/util/PerformanceMonitor.java

# Fix constructor
sed -i 's/private PerformanceMonitor() {/private PerformanceMonitor(Context context) {\n        this.context = context.getApplicationContext();/' /workspace/app/src/main/java/com/checkmate/android/util/PerformanceMonitor.java

# Add getInstance with context
sed -i '/public static synchronized PerformanceMonitor getInstance() {/i\
    public static synchronized PerformanceMonitor getInstance(Context context) {\
        if (instance == null) {\
            instance = new PerformanceMonitor(context);\
        }\
        return instance;\
    }\
' /workspace/app/src/main/java/com/checkmate/android/util/PerformanceMonitor.java

# 2. Fix StartupOptimizer.java
echo "Fixing StartupOptimizer.java..."
# Remove AppPreference.init calls
sed -i 's/AppPreference.init(context);/\/\/ AppPreference initialization handled elsewhere/' /workspace/app/src/main/java/com/checkmate/android/startup/StartupOptimizer.java

# Fix TIMESTAMP_OVERLAY to TIMESTAMP_ENABLE
sed -i 's/AppPreference.KEY.TIMESTAMP_OVERLAY/AppPreference.KEY.TIMESTAMP_ENABLE/' /workspace/app/src/main/java/com/checkmate/android/startup/StartupOptimizer.java

# 3. Fix MyApp.java - add missing method
echo "Fixing MyApp.java..."
# Remove last closing brace and add method
sed -i '$ d' /workspace/app/src/main/java/com/checkmate/android/MyApp.java
echo '
    /**
     * Called when optimized startup is complete
     */
    private void onOptimizedStartupComplete() {
        try {
            InternalLogger.i(TAG, "Optimized startup complete, performing post-initialization tasks");
            
            // Initialize any remaining non-critical components
            initializeMonitoringSystems();
            
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error in post-initialization", e);
        }
    }
}' >> /workspace/app/src/main/java/com/checkmate/android/MyApp.java

# 4. Fix DynamicSettingsManager.java - add notifyListeners method
echo "Fixing DynamicSettingsManager.java..."
# Check if method exists, if not add it
if ! grep -q "public void notifyListeners" /workspace/app/src/main/java/com/checkmate/android/util/DynamicSettingsManager.java; then
    sed -i '$ d' /workspace/app/src/main/java/com/checkmate/android/util/DynamicSettingsManager.java
    echo '
    /**
     * Notify all listeners about a setting change
     */
    public void notifyListeners(String key, Object value) {
        if (listeners.isEmpty()) return;
        
        mainHandler.post(() -> {
            for (SettingChangeListener listener : listeners) {
                try {
                    listener.onSettingChanged(key, value);
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying listener", e);
                }
            }
        });
    }
}' >> /workspace/app/src/main/java/com/checkmate/android/util/DynamicSettingsManager.java
fi

# 5. Fix CrashLogger.java - add logANR method
echo "Fixing CrashLogger.java..."
if ! grep -q "public void logANR" /workspace/app/src/main/java/com/checkmate/android/util/CrashLogger.java; then
    sed -i '$ d' /workspace/app/src/main/java/com/checkmate/android/util/CrashLogger.java
    echo '
    /**
     * Log ANR event
     */
    public void logANR(String tag, String message) {
        String log = formatLog("ANR", tag, message);
        writeToFile(log);
    }
}' >> /workspace/app/src/main/java/com/checkmate/android/util/CrashLogger.java
fi

# 6. Fix BatteryOptimizer.java
echo "Fixing BatteryOptimizer.java..."
sed -i 's/AppPreference.KEY.TIMESTAMP_OVERLAY/AppPreference.KEY.TIMESTAMP_ENABLE/' /workspace/app/src/main/java/com/checkmate/android/util/BatteryOptimizer.java

echo "All fixes applied!"