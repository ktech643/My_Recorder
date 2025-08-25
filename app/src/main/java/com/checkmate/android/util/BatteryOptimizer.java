package com.checkmate.android.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import com.checkmate.android.AppPreference;
import com.checkmate.android.ai.SmartOptimizer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Smart battery optimization system that automatically adjusts app behavior
 * based on battery level, charging state, and thermal conditions
 */
public class BatteryOptimizer {
    private static final String TAG = "BatteryOptimizer";
    private static BatteryOptimizer instance;
    
    private final Context context;
    private final PowerManager powerManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    // Battery state tracking
    private volatile int currentBatteryLevel = 100;
    private volatile boolean isCharging = false;
    private volatile int temperature = 0;
    private volatile float batteryVoltage = 0f;
    
    // Optimization state
    private PowerProfile currentProfile = PowerProfile.BALANCED;
    private final Map<String, Object> originalSettings = new ConcurrentHashMap<>();
    private volatile boolean isOptimizationActive = false;
    
    // Thresholds
    private static final int CRITICAL_BATTERY_LEVEL = 15;
    private static final int LOW_BATTERY_LEVEL = 30;
    private static final int TEMPERATURE_WARNING_CELSIUS = 40;
    private static final int TEMPERATURE_CRITICAL_CELSIUS = 45;
    
    public enum PowerProfile {
        MAX_PERFORMANCE("Maximum Performance", 1.0f),
        BALANCED("Balanced", 0.7f),
        POWER_SAVER("Power Saver", 0.5f),
        ULTRA_POWER_SAVER("Ultra Power Saver", 0.3f);
        
        public final String name;
        public final float performanceMultiplier;
        
        PowerProfile(String name, float performanceMultiplier) {
            this.name = name;
            this.performanceMultiplier = performanceMultiplier;
        }
    }
    
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                updateBatteryState(intent);
            }
        }
    };
    
    private BatteryOptimizer(Context context) {
        this.context = context.getApplicationContext();
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        initialize();
    }
    
    public static synchronized BatteryOptimizer getInstance(Context context) {
        if (instance == null) {
            instance = new BatteryOptimizer(context);
        }
        return instance;
    }
    
    /**
     * Initialize battery monitoring
     */
    private void initialize() {
        // Register battery receiver
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(batteryReceiver, filter);
        if (batteryStatus != null) {
            updateBatteryState(batteryStatus);
        }
        
        // Start periodic optimization checks
        scheduler.scheduleAtFixedRate(this::performOptimizationCheck, 0, 30, TimeUnit.SECONDS);
        
        Log.i(TAG, "Battery optimizer initialized");
    }
    
    /**
     * Update battery state from intent
     */
    private void updateBatteryState(Intent intent) {
        try {
            // Extract battery information
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            currentBatteryLevel = (level * 100) / scale;
            
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL;
            
            temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10; // Convert to Celsius
            batteryVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000f; // Convert to volts
            
            Log.d(TAG, String.format("Battery update: %d%%, Charging: %b, Temp: %d°C, Voltage: %.2fV",
                    currentBatteryLevel, isCharging, temperature, batteryVoltage));
                    
        } catch (Exception e) {
            Log.e(TAG, "Error updating battery state", e);
        }
    }
    
    /**
     * Perform optimization check
     */
    private void performOptimizationCheck() {
        try {
            // Determine optimal power profile
            PowerProfile targetProfile = determineOptimalProfile();
            
            // Apply profile if changed
            if (targetProfile != currentProfile) {
                applyPowerProfile(targetProfile);
            }
            
            // Check thermal conditions
            if (temperature >= TEMPERATURE_CRITICAL_CELSIUS) {
                applyThermalThrottling(true);
            } else if (temperature < TEMPERATURE_WARNING_CELSIUS) {
                applyThermalThrottling(false);
            }
            
            // Update AI optimizer
            updateAIOptimizer();
            
        } catch (Exception e) {
            Log.e(TAG, "Error during optimization check", e);
        }
    }
    
    /**
     * Determine optimal power profile based on current conditions
     */
    private PowerProfile determineOptimalProfile() {
        // If charging, use max performance
        if (isCharging) {
            return PowerProfile.MAX_PERFORMANCE;
        }
        
        // If temperature is critical, force power saving
        if (temperature >= TEMPERATURE_CRITICAL_CELSIUS) {
            return PowerProfile.ULTRA_POWER_SAVER;
        }
        
        // Based on battery level
        if (currentBatteryLevel <= CRITICAL_BATTERY_LEVEL) {
            return PowerProfile.ULTRA_POWER_SAVER;
        } else if (currentBatteryLevel <= LOW_BATTERY_LEVEL) {
            return PowerProfile.POWER_SAVER;
        } else if (currentBatteryLevel >= 80) {
            return PowerProfile.MAX_PERFORMANCE;
        } else {
            return PowerProfile.BALANCED;
        }
    }
    
    /**
     * Apply power profile
     */
    private void applyPowerProfile(PowerProfile profile) {
        Log.i(TAG, "Applying power profile: " + profile.name);
        currentProfile = profile;
        
        mainHandler.post(() -> {
            try {
                // Save original settings if not already saved
                if (!isOptimizationActive) {
                    saveOriginalSettings();
                    isOptimizationActive = true;
                }
                
                // Apply profile-specific settings
                switch (profile) {
                    case ULTRA_POWER_SAVER:
                        applyUltraPowerSaverSettings();
                        break;
                        
                    case POWER_SAVER:
                        applyPowerSaverSettings();
                        break;
                        
                    case BALANCED:
                        applyBalancedSettings();
                        break;
                        
                    case MAX_PERFORMANCE:
                        restoreOriginalSettings();
                        isOptimizationActive = false;
                        break;
                }
                
                // Notify user
                UserFeedbackManager.getInstance(context).showInfo(
                    "Power mode: " + profile.name
                );
                
            } catch (Exception e) {
                Log.e(TAG, "Error applying power profile", e);
            }
        });
    }
    
    /**
     * Save original settings
     */
    private void saveOriginalSettings() {
        originalSettings.put(AppPreference.KEY.VIDEO_BITRATE, 
            AppPreference.getInt(AppPreference.KEY.VIDEO_BITRATE, 4000000));
        originalSettings.put(AppPreference.KEY.VIDEO_FRAME, 
            AppPreference.getInt(AppPreference.KEY.VIDEO_FRAME, 30));
        originalSettings.put(AppPreference.KEY.VIDEO_RESOLUTION, 
            AppPreference.getStr(AppPreference.KEY.VIDEO_RESOLUTION, "1920x1080"));
        originalSettings.put(AppPreference.KEY.AUDIO_OPTION_BITRATE, 
            AppPreference.getInt(AppPreference.KEY.AUDIO_OPTION_BITRATE, 128000));
    }
    
    /**
     * Apply ultra power saver settings
     */
    private void applyUltraPowerSaverSettings() {
        DynamicSettingsManager manager = DynamicSettingsManager.getInstance(context);
        
        // Reduce video quality to minimum
        manager.notifyListeners(AppPreference.KEY.VIDEO_BITRATE, 500000); // 500 kbps
        manager.notifyListeners(AppPreference.KEY.VIDEO_FRAME, 15); // 15 fps
        manager.notifyListeners(AppPreference.KEY.VIDEO_RESOLUTION, "640x480");
        
        // Reduce audio quality
        manager.notifyListeners(AppPreference.KEY.AUDIO_OPTION_BITRATE, 64000); // 64 kbps
        
        // Disable non-essential features
        manager.notifyListeners(AppPreference.KEY.TIMESTAMP, false);
    }
    
    /**
     * Apply power saver settings
     */
    private void applyPowerSaverSettings() {
        DynamicSettingsManager manager = DynamicSettingsManager.getInstance(context);
        
        // Moderate reduction in quality
        manager.notifyListeners(AppPreference.KEY.VIDEO_BITRATE, 1000000); // 1 Mbps
        manager.notifyListeners(AppPreference.KEY.VIDEO_FRAME, 20); // 20 fps
        manager.notifyListeners(AppPreference.KEY.VIDEO_RESOLUTION, "1280x720");
        
        // Moderate audio quality
        manager.notifyListeners(AppPreference.KEY.AUDIO_OPTION_BITRATE, 96000); // 96 kbps
    }
    
    /**
     * Apply balanced settings
     */
    private void applyBalancedSettings() {
        DynamicSettingsManager manager = DynamicSettingsManager.getInstance(context);
        
        // Balanced quality
        manager.notifyListeners(AppPreference.KEY.VIDEO_BITRATE, 2000000); // 2 Mbps
        manager.notifyListeners(AppPreference.KEY.VIDEO_FRAME, 25); // 25 fps
        manager.notifyListeners(AppPreference.KEY.VIDEO_RESOLUTION, "1280x720");
        
        // Standard audio quality
        manager.notifyListeners(AppPreference.KEY.AUDIO_OPTION_BITRATE, 128000); // 128 kbps
    }
    
    /**
     * Restore original settings
     */
    private void restoreOriginalSettings() {
        if (originalSettings.isEmpty()) return;
        
        DynamicSettingsManager manager = DynamicSettingsManager.getInstance(context);
        
        for (Map.Entry<String, Object> entry : originalSettings.entrySet()) {
            manager.notifyListeners(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Apply thermal throttling
     */
    private void applyThermalThrottling(boolean enable) {
        if (enable) {
            Log.w(TAG, "Applying thermal throttling - temperature: " + temperature + "°C");
            
            // Reduce processing load
            DynamicSettingsManager manager = DynamicSettingsManager.getInstance(context);
            
            // Reduce frame rate to minimize heat generation
            int currentFps = AppPreference.getInt(AppPreference.KEY.VIDEO_FRAME, 30);
            manager.notifyListeners(AppPreference.KEY.VIDEO_FRAME, Math.min(currentFps, 20));
            
            // Show warning
            UserFeedbackManager.getInstance(context).showWarning(
                "Device temperature high - reducing performance"
            );
        }
    }
    
    /**
     * Update AI optimizer with battery information
     */
    private void updateAIOptimizer() {
        try {
            SmartOptimizer optimizer = SmartOptimizer.getInstance();
            
            // Set optimization goal based on battery state
            SmartOptimizer.OptimizationGoal goal;
            if (currentBatteryLevel <= LOW_BATTERY_LEVEL) {
                goal = SmartOptimizer.OptimizationGoal.BATTERY_PRIORITY;
            } else if (isCharging) {
                goal = SmartOptimizer.OptimizationGoal.QUALITY_PRIORITY;
            } else {
                goal = SmartOptimizer.OptimizationGoal.BALANCED;
            }
            
            optimizer.setOptimizationGoal(goal);
            
            // Record battery event for learning
            Map<String, Object> context = new HashMap<>();
            context.put("battery_level", currentBatteryLevel);
            context.put("is_charging", isCharging);
            context.put("temperature", temperature);
            context.put("power_profile", currentProfile.name);
            
            optimizer.recordEvent("battery_optimization", context);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating AI optimizer", e);
        }
    }
    
    /**
     * Get battery statistics
     */
    public String getBatteryStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== Battery Statistics ===\n");
        stats.append("Level: ").append(currentBatteryLevel).append("%\n");
        stats.append("Charging: ").append(isCharging ? "Yes" : "No").append("\n");
        stats.append("Temperature: ").append(temperature).append("°C\n");
        stats.append("Voltage: ").append(String.format("%.2f", batteryVoltage)).append("V\n");
        stats.append("Power Profile: ").append(currentProfile.name).append("\n");
        stats.append("Optimization Active: ").append(isOptimizationActive).append("\n");
        
        // Add power management info
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            stats.append("\nPower Save Mode: ")
                .append(powerManager.isPowerSaveMode() ? "Enabled" : "Disabled").append("\n");
        }
        
        return stats.toString();
    }
    
    /**
     * Get current power profile
     */
    public PowerProfile getCurrentProfile() {
        return currentProfile;
    }
    
    /**
     * Force power profile (for manual override)
     */
    public void forceProfile(PowerProfile profile) {
        Log.i(TAG, "Forcing power profile: " + profile.name);
        applyPowerProfile(profile);
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        try {
            context.unregisterReceiver(batteryReceiver);
            scheduler.shutdown();
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }
}