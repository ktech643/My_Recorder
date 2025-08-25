package com.checkmate.android.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.checkmate.android.AppPreference;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Monitor for critical app components that were not fully enhanced
 * Provides safety net for components with TODO/FIXME comments
 */
public class CriticalComponentsMonitor {
    private static final String TAG = "CriticalComponentsMonitor";
    private static volatile CriticalComponentsMonitor instance;
    
    private final Context context;
    private final Handler mainHandler;
    private final AtomicBoolean isMonitoring;
    private final ConcurrentHashMap<String, ComponentStatus> componentStatuses;
    private final AtomicInteger criticalErrorCount;
    
    // Critical components that need monitoring
    private static final String[] CRITICAL_COMPONENTS = {
        "StreamingFragment",
        "SettingsFragment", 
        "BgUSBService",
        "BgAudioService",
        "BgCastService",
        "BaseActivity",
        "GlCrashReporter"
    };
    
    private static class ComponentStatus {
        final String name;
        final AtomicInteger errorCount;
        final AtomicBoolean isHealthy;
        long lastErrorTime;
        String lastError;
        
        ComponentStatus(String name) {
            this.name = name;
            this.errorCount = new AtomicInteger(0);
            this.isHealthy = new AtomicBoolean(true);
            this.lastErrorTime = 0;
            this.lastError = "";
        }
        
        void recordError(String error) {
            this.errorCount.incrementAndGet();
            this.lastErrorTime = System.currentTimeMillis();
            this.lastError = error;
            
            // Mark as unhealthy if more than 3 errors in 5 minutes
            if (errorCount.get() > 3) {
                isHealthy.set(false);
            }
        }
        
        void markHealthy() {
            if (System.currentTimeMillis() - lastErrorTime > 300000) { // 5 minutes
                isHealthy.set(true);
                errorCount.set(0);
            }
        }
    }
    
    private CriticalComponentsMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.isMonitoring = new AtomicBoolean(false);
        this.componentStatuses = new ConcurrentHashMap<>();
        this.criticalErrorCount = new AtomicInteger(0);
        
        // Initialize component statuses
        for (String component : CRITICAL_COMPONENTS) {
            componentStatuses.put(component, new ComponentStatus(component));
        }
    }
    
    public static CriticalComponentsMonitor getInstance(Context context) {
        if (instance == null) {
            synchronized (CriticalComponentsMonitor.class) {
                if (instance == null) {
                    instance = new CriticalComponentsMonitor(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * Start monitoring critical components
     */
    public void startMonitoring() {
        if (isMonitoring.compareAndSet(false, true)) {
            InternalLogger.i(TAG, "Starting critical components monitoring");
            
            // Start periodic health checks
            startPeriodicHealthCheck();
        }
    }
    
    /**
     * Record an error for a component
     */
    public static void recordComponentError(String componentName, String error, Throwable throwable) {
        try {
            if (instance != null) {
                ComponentStatus status = instance.componentStatuses.get(componentName);
                if (status != null) {
                    status.recordError(error);
                    instance.criticalErrorCount.incrementAndGet();
                    
                    InternalLogger.e(TAG, "Component error in " + componentName + ": " + error, throwable);
                    
                    // Check if we need to enter emergency mode
                    instance.checkEmergencyMode();
                } else {
                    InternalLogger.w(TAG, "Unknown component reported error: " + componentName);
                }
            }
        } catch (Exception e) {
            // Fail silently to prevent cascading errors
        }
    }
    
    /**
     * Mark a component as healthy
     */
    public static void markComponentHealthy(String componentName) {
        try {
            if (instance != null) {
                ComponentStatus status = instance.componentStatuses.get(componentName);
                if (status != null) {
                    status.markHealthy();
                }
            }
        } catch (Exception e) {
            // Fail silently
        }
    }
    
    /**
     * Safe execution wrapper for critical components
     */
    public static <T> T executeComponentSafely(String componentName, ComponentOperation<T> operation, T defaultValue) {
        try {
            T result = operation.execute();
            markComponentHealthy(componentName);
            return result;
        } catch (Exception e) {
            recordComponentError(componentName, "Operation failed: " + e.getMessage(), e);
            return defaultValue;
        }
    }
    
    /**
     * Safe execution wrapper with void return
     */
    public static void executeComponentSafely(String componentName, ComponentVoidOperation operation) {
        executeComponentSafely(componentName, () -> {
            operation.execute();
            return null;
        }, null);
    }
    
    public interface ComponentOperation<T> {
        T execute() throws Exception;
    }
    
    public interface ComponentVoidOperation {
        void execute() throws Exception;
    }
    
    /**
     * Check if emergency mode should be activated
     */
    private void checkEmergencyMode() {
        int unhealthyComponents = 0;
        for (ComponentStatus status : componentStatuses.values()) {
            if (!status.isHealthy.get()) {
                unhealthyComponents++;
            }
        }
        
        // If more than half of critical components are unhealthy, enter emergency mode
        if (unhealthyComponents > CRITICAL_COMPONENTS.length / 2) {
            enterEmergencyMode();
        }
    }
    
    /**
     * Enter emergency mode with minimal functionality
     */
    private void enterEmergencyMode() {
        InternalLogger.e(TAG, "Entering emergency mode due to multiple component failures");
        
        // Set emergency mode flag
        AppPreference.setBool("EMERGENCY_MODE", true);
        AppPreference.incrementRestartCount();
        
        // Log system state
        logSystemState();
    }
    
    /**
     * Start periodic health checks
     */
    private void startPeriodicHealthCheck() {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isMonitoring.get()) {
                    performHealthCheck();
                    mainHandler.postDelayed(this, 60000); // Check every minute
                }
            }
        }, 60000);
    }
    
    /**
     * Perform health check on all components
     */
    private void performHealthCheck() {
        try {
            int healthyComponents = 0;
            int totalComponents = componentStatuses.size();
            
            for (ComponentStatus status : componentStatuses.values()) {
                status.markHealthy(); // This will mark as healthy if enough time has passed
                
                if (status.isHealthy.get()) {
                    healthyComponents++;
                }
            }
            
            float healthRatio = (float) healthyComponents / totalComponents;
            InternalLogger.d(TAG, "Component health check: " + healthyComponents + "/" + totalComponents + " healthy (" + (healthRatio * 100) + "%)");
            
            // Exit emergency mode if most components are healthy
            if (healthRatio > 0.8 && AppPreference.getBool("EMERGENCY_MODE", false)) {
                exitEmergencyMode();
            }
            
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error during health check", e);
        }
    }
    
    /**
     * Exit emergency mode
     */
    private void exitEmergencyMode() {
        InternalLogger.i(TAG, "Exiting emergency mode - components stabilized");
        AppPreference.setBool("EMERGENCY_MODE", false);
        AppPreference.resetRestartCount();
    }
    
    /**
     * Log system state for debugging
     */
    private void logSystemState() {
        try {
            StringBuilder state = new StringBuilder();
            state.append("=== CRITICAL COMPONENTS STATE ===\n");
            
            for (ComponentStatus status : componentStatuses.values()) {
                state.append("Component: ").append(status.name)
                     .append(", Healthy: ").append(status.isHealthy.get())
                     .append(", Errors: ").append(status.errorCount.get())
                     .append(", Last Error: ").append(status.lastError)
                     .append("\n");
            }
            
            state.append("Total Critical Errors: ").append(criticalErrorCount.get()).append("\n");
            state.append("=== END STATE ===\n");
            
            InternalLogger.i(TAG, state.toString());
        } catch (Exception e) {
            InternalLogger.e(TAG, "Error logging system state", e);
        }
    }
    
    /**
     * Get monitoring status
     */
    public MonitorStatus getStatus() {
        int healthyCount = 0;
        int totalCount = componentStatuses.size();
        
        for (ComponentStatus status : componentStatuses.values()) {
            if (status.isHealthy.get()) {
                healthyCount++;
            }
        }
        
        return new MonitorStatus(
            healthyCount,
            totalCount,
            criticalErrorCount.get(),
            AppPreference.getBool("EMERGENCY_MODE", false)
        );
    }
    
    public static class MonitorStatus {
        public final int healthyComponents;
        public final int totalComponents;
        public final int totalErrors;
        public final boolean emergencyMode;
        
        MonitorStatus(int healthyComponents, int totalComponents, int totalErrors, boolean emergencyMode) {
            this.healthyComponents = healthyComponents;
            this.totalComponents = totalComponents;
            this.totalErrors = totalErrors;
            this.emergencyMode = emergencyMode;
        }
        
        public boolean isHealthy() {
            return !emergencyMode && (float) healthyComponents / totalComponents > 0.8;
        }
        
        @Override
        public String toString() {
            return "MonitorStatus{" +
                    "healthy=" + healthyComponents + "/" + totalComponents +
                    ", errors=" + totalErrors +
                    ", emergency=" + emergencyMode +
                    '}';
        }
    }
}