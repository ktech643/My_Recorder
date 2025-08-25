package com.checkmate.android.startup;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import com.checkmate.android.AppPreference;
import com.checkmate.android.ai.SmartOptimizer;
import com.checkmate.android.diagnostics.SystemDiagnostics;
import com.checkmate.android.network.AdaptiveBitrateManager;
import com.checkmate.android.util.BatteryOptimizer;
import com.checkmate.android.util.CrashLogger;
import com.checkmate.android.util.DynamicSettingsManager;
import com.checkmate.android.util.PerformanceMonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Optimizes app startup to prevent ANR and improve launch time
 * Manages initialization of heavy components in background threads
 */
public class StartupOptimizer {
    private static final String TAG = "StartupOptimizer";
    private static StartupOptimizer instance;
    
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final HandlerThread backgroundThread;
    private final Handler backgroundHandler;
    private final ExecutorService initExecutor = Executors.newFixedThreadPool(4);
    
    // Startup phases
    private final List<StartupPhase> startupPhases = new ArrayList<>();
    private final AtomicInteger currentPhase = new AtomicInteger(0);
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    
    // Callbacks
    private StartupCallback callback;
    private long startTime;
    
    public interface StartupCallback {
        void onPhaseCompleted(String phaseName, long duration);
        void onStartupCompleted(long totalDuration);
        void onStartupError(String phase, Exception error);
    }
    
    private static class StartupPhase {
        final String name;
        final Runnable task;
        final boolean isCritical;
        final long timeoutMs;
        
        StartupPhase(String name, Runnable task, boolean isCritical, long timeoutMs) {
            this.name = name;
            this.task = task;
            this.isCritical = isCritical;
            this.timeoutMs = timeoutMs;
        }
    }
    
    private StartupOptimizer(Context context) {
        this.context = context.getApplicationContext();
        this.backgroundThread = new HandlerThread("StartupOptimizer", 
            Process.THREAD_PRIORITY_BACKGROUND);
        this.backgroundThread.start();
        this.backgroundHandler = new Handler(backgroundThread.getLooper());
        
        initializePhases();
    }
    
    public static synchronized StartupOptimizer getInstance(Context context) {
        if (instance == null) {
            instance = new StartupOptimizer(context);
        }
        return instance;
    }
    
    /**
     * Initialize startup phases
     */
    private void initializePhases() {
        // Phase 1: Critical components (must complete)
        startupPhases.add(new StartupPhase(
            "CrashLogger",
            () -> CrashLogger.initialize(context),
            true,
            1000
        ));
        
        startupPhases.add(new StartupPhase(
            "AppPreference",
            () -> {
                // Initialize preferences in background
                AppPreference.init(context);
                // Pre-load critical preferences
                preloadCriticalPreferences();
            },
            true,
            2000
        ));
        
        // Phase 2: Monitoring systems (can be delayed)
        startupPhases.add(new StartupPhase(
            "DynamicSettings",
            () -> DynamicSettingsManager.initialize(context),
            false,
            3000
        ));
        
        startupPhases.add(new StartupPhase(
            "PerformanceMonitor",
            () -> PerformanceMonitor.getInstance().startMonitoring(),
            false,
            2000
        ));
        
        // Phase 3: Advanced features (non-critical)
        startupPhases.add(new StartupPhase(
            "SystemDiagnostics",
            () -> SystemDiagnostics.getInstance(context),
            false,
            5000
        ));
        
        startupPhases.add(new StartupPhase(
            "BatteryOptimizer",
            () -> BatteryOptimizer.getInstance(context),
            false,
            3000
        ));
        
        startupPhases.add(new StartupPhase(
            "AdaptiveBitrate",
            () -> AdaptiveBitrateManager.getInstance(context),
            false,
            3000
        ));
        
        startupPhases.add(new StartupPhase(
            "SmartOptimizer",
            () -> SmartOptimizer.getInstance(),
            false,
            5000
        ));
    }
    
    /**
     * Start optimized initialization
     */
    public void startOptimizedInitialization(StartupCallback callback) {
        if (isInitialized.getAndSet(true)) {
            Log.w(TAG, "Already initialized");
            return;
        }
        
        this.callback = callback;
        this.startTime = System.currentTimeMillis();
        
        Log.i(TAG, "Starting optimized initialization");
        
        // Execute phases in background
        backgroundHandler.post(this::executeNextPhase);
    }
    
    /**
     * Execute next phase
     */
    private void executeNextPhase() {
        int phaseIndex = currentPhase.getAndIncrement();
        
        if (phaseIndex >= startupPhases.size()) {
            // All phases completed
            completeStartup();
            return;
        }
        
        StartupPhase phase = startupPhases.get(phaseIndex);
        long phaseStartTime = System.currentTimeMillis();
        
        Log.d(TAG, "Executing phase: " + phase.name);
        
        // Execute with timeout
        Future<?> future = initExecutor.submit(() -> {
            try {
                phase.task.run();
                
                long duration = System.currentTimeMillis() - phaseStartTime;
                Log.i(TAG, "Phase completed: " + phase.name + " (" + duration + "ms)");
                
                // Notify callback on main thread
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onPhaseCompleted(phase.name, duration);
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error in phase: " + phase.name, e);
                handlePhaseError(phase, e);
            }
        });
        
        // Schedule timeout check
        backgroundHandler.postDelayed(() -> {
            if (!future.isDone()) {
                Log.w(TAG, "Phase timeout: " + phase.name);
                future.cancel(true);
                
                if (phase.isCritical) {
                    handlePhaseError(phase, new TimeoutException("Phase timeout"));
                } else {
                    // Continue with next phase for non-critical components
                    executeNextPhase();
                }
            } else {
                // Continue with next phase
                executeNextPhase();
            }
        }, phase.timeoutMs);
    }
    
    /**
     * Handle phase error
     */
    private void handlePhaseError(StartupPhase phase, Exception error) {
        Log.e(TAG, "Phase error: " + phase.name, error);
        
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onStartupError(phase.name, error);
            }
        });
        
        if (phase.isCritical) {
            // Critical error - stop initialization
            Log.e(TAG, "Critical phase failed, stopping initialization");
        } else {
            // Non-critical - continue
            executeNextPhase();
        }
    }
    
    /**
     * Complete startup
     */
    private void completeStartup() {
        long totalDuration = System.currentTimeMillis() - startTime;
        Log.i(TAG, "Startup completed in " + totalDuration + "ms");
        
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onStartupCompleted(totalDuration);
            }
        });
        
        // Cleanup
        initExecutor.shutdown();
    }
    
    /**
     * Preload critical preferences to avoid ANR
     */
    private void preloadCriticalPreferences() {
        try {
            // Preload frequently accessed preferences
            AppPreference.getInt(AppPreference.KEY.VIDEO_BITRATE, 0);
            AppPreference.getStr(AppPreference.KEY.VIDEO_RESOLUTION, "");
            AppPreference.getBool(AppPreference.KEY.TIMESTAMP_OVERLAY, false);
            AppPreference.getInt(AppPreference.KEY.VIDEO_FRAME, 30);
            
            Log.d(TAG, "Critical preferences preloaded");
        } catch (Exception e) {
            Log.e(TAG, "Error preloading preferences", e);
        }
    }
    
    /**
     * Quick initialization for immediate UI display
     */
    public void quickInitForUI() {
        // Initialize only the bare minimum for UI
        mainHandler.post(() -> {
            try {
                // Initialize crash logger synchronously (very fast)
                if (CrashLogger.getInstance() == null) {
                    CrashLogger.initialize(context);
                }
                
                // Initialize preferences with shorter timeout
                Future<?> prefInit = initExecutor.submit(() -> AppPreference.init(context));
                
                try {
                    prefInit.get(500, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    Log.w(TAG, "Preference init timeout, continuing anyway");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in quick init", e);
            }
        });
    }
    
    /**
     * Check if initialization is complete
     */
    public boolean isInitialized() {
        return isInitialized.get() && currentPhase.get() >= startupPhases.size();
    }
    
    /**
     * Wait for critical components with timeout
     */
    public boolean waitForCriticalComponents(long timeoutMs) {
        long startTime = System.currentTimeMillis();
        
        while (!isInitialized() && currentPhase.get() < 2) { // First 2 phases are critical
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                return false;
            }
            
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get initialization progress
     */
    public float getProgress() {
        return (float) currentPhase.get() / startupPhases.size();
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        try {
            backgroundThread.quitSafely();
            initExecutor.shutdownNow();
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }
}