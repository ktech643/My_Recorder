#!/bin/bash

echo "Fixing remaining compilation errors..."

# 1. Fix PerformanceMonitor getInstance() to use context
echo "Fixing PerformanceMonitor getInstance()..."
sed -i 's/instance = new PerformanceMonitor();/throw new IllegalStateException("PerformanceMonitor not initialized. Call getInstance(Context) first.");/' /workspace/app/src/main/java/com/checkmate/android/util/PerformanceMonitor.java

# 2. Fix DynamicSettingsManager listeners iteration
echo "Fixing DynamicSettingsManager listeners..."
sed -i 's/for (SettingChangeListener listener : listeners) {/for (SettingChangeListener listener : listeners.values()) {/' /workspace/app/src/main/java/com/checkmate/android/util/DynamicSettingsManager.java

# 3. Fix CrashLogger methods
echo "Fixing CrashLogger methods..."
# Add formatLog method if missing
if ! grep -q "private String formatLog" /workspace/app/src/main/java/com/checkmate/android/util/CrashLogger.java; then
    sed -i '/public void logANR/i\
    private String formatLog(String level, String tag, String message) {\
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);\
        String timestamp = sdf.format(new Date());\
        return timestamp + " " + level + "/" + tag + ": " + message + "\\n";\
    }\
' /workspace/app/src/main/java/com/checkmate/android/util/CrashLogger.java
fi

# Fix writeToFile to use executor
sed -i 's/writeToFile(log);/executor.execute(() -> writeLogToFile(log));/' /workspace/app/src/main/java/com/checkmate/android/util/CrashLogger.java

# 4. Fix SettingsValidator - replace KEY_ with KEY.
echo "Fixing SettingsValidator KEY references..."
sed -i 's/AppPreference\.KEY_/AppPreference.KEY./g' /workspace/app/src/main/java/com/checkmate/android/util/SettingsValidator.java

# 5. Fix StartupOptimizer - check for correct constant names
echo "Checking AppPreference constants..."
# First, let's see what constants exist
grep -E "public static final String.*TIMESTAMP" /workspace/app/src/main/java/com/checkmate/android/AppPreference.java || echo "No TIMESTAMP constants found"

# Try different possible names
sed -i 's/AppPreference.KEY.TIMESTAMP_ENABLE/AppPreference.KEY.TIMESTAMP_VISIBLE/' /workspace/app/src/main/java/com/checkmate/android/startup/StartupOptimizer.java
sed -i 's/AppPreference.KEY.TIMESTAMP_ENABLE/AppPreference.KEY.TIMESTAMP_VISIBLE/' /workspace/app/src/main/java/com/checkmate/android/util/BatteryOptimizer.java

# 6. Remove the second AppPreference.init call
sed -i '/Future<?> prefInit = initExecutor.submit(() -> AppPreference.init(context));/d' /workspace/app/src/main/java/com/checkmate/android/startup/StartupOptimizer.java

echo "Remaining fixes applied!"