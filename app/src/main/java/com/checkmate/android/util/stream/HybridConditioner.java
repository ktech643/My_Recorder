package com.checkmate.android.util.stream;

import android.util.Log;

/**
 * HybridConditioner - Balanced adaptive approach (Mode 2)
 * 
 * This conditioner combines the best aspects of both Logarithmic Descend and Ladder Ascend
 * strategies, providing a balanced approach that adapts to different network scenarios.
 * It uses intelligent switching between aggressive and conservative modes based on 
 * network behavior patterns.
 * 
 * Algorithm characteristics:
 * - Dynamic strategy selection based on network behavior
 * - Fast response to critical conditions (logarithmic descend)
 * - Careful quality improvements when stable (ladder ascend)
 * - Network pattern learning for optimal strategy selection
 * - Oscillation prevention through intelligent cooldown periods
 * 
 * Best for:
 * - Mixed network environments
 * - General-purpose streaming applications
 * - When optimal balance between quality and stability is needed
 */
public class HybridConditioner extends BaseStreamConditioner {
    
    private static final String TAG = "HybridConditioner";
    
    // Hybrid strategy modes
    enum HybridMode {
        CONSERVATIVE,    // Use ladder-like ascend, moderate descend
        AGGRESSIVE,      // Use logarithmic descend, minimal ascend
        BALANCED,        // Default balanced approach
        LEARNING         // Learning network patterns
    }
    
    // Adaptation parameters for different modes
    private static final float CONSERVATIVE_ASCEND = 1.12f;   // 12% increase
    private static final float CONSERVATIVE_DESCEND = 0.8f;   // 20% decrease
    private static final float AGGRESSIVE_ASCEND = 1.05f;     // 5% increase
    private static final float AGGRESSIVE_DESCEND = 0.65f;    // 35% decrease
    private static final float BALANCED_ASCEND = 1.08f;       // 8% increase
    private static final float BALANCED_DESCEND = 0.75f;      // 25% decrease
    
    // Timing parameters
    private static final long LEARNING_PERIOD_MS = 30000;     // 30 seconds
    private static final long CONSERVATIVE_ASCEND_DELAY = 8000; // 8 seconds
    private static final long AGGRESSIVE_ASCEND_DELAY = 12000; // 12 seconds
    private static final long BALANCED_ASCEND_DELAY = 6000;    // 6 seconds
    private static final long FAST_DESCEND_DELAY = 1500;      // 1.5 seconds
    
    // Network stability thresholds
    private static final float STABLE_PACKET_LOSS = 0.03f;    // 3% packet loss
    private static final long STABLE_RTT = 80;                // 80ms RTT
    private static final float UNSTABLE_PACKET_LOSS = 0.12f;  // 12% packet loss
    private static final long UNSTABLE_RTT = 200;             // 200ms RTT
    private static final float CRITICAL_PACKET_LOSS = 0.25f;  // 25% packet loss
    private static final long CRITICAL_RTT = 400;             // 400ms RTT
    
    // State tracking
    private int mInitialBitrate;
    private HybridMode mCurrentHybridMode = HybridMode.LEARNING;
    private long mModeStartTime = 0;
    private long mLastModeSwitch = 0;
    private long mLastAscendTime = 0;
    private long mLastDescendTime = 0;
    
    // Network behavior learning
    private int mStableConditionCount = 0;
    private int mUnstableConditionCount = 0;
    private int mCriticalConditionCount = 0;
    private float mAveragePacketLoss = 0.0f;
    private long mAverageRtt = 0;
    private int mMeasurementCount = 0;
    
    // Performance tracking
    private int mSuccessfulAscends = 0;
    private int mFailedAscends = 0; // Ascend followed by quick descend
    private boolean mLastAdjustmentWasAscend = false;
    
    public HybridConditioner() {
        super(ConditionerMode.HYBRID);
    }
    
    @Override
    protected void onConditionerStarted(int initialBitrate) {
        mInitialBitrate = initialBitrate;
        mCurrentHybridMode = HybridMode.LEARNING;
        mModeStartTime = System.currentTimeMillis();
        
        Log.d(TAG, "HybridConditioner started:");
        Log.d(TAG, "  Initial bitrate: " + (initialBitrate / 1024) + " Kbps");
        Log.d(TAG, "  Starting in LEARNING mode for " + (LEARNING_PERIOD_MS / 1000) + " seconds");
        Log.d(TAG, "  Will adapt strategy based on network behavior patterns");
        
        // Reset all counters
        resetLearningCounters();
    }
    
    @Override
    protected void onConditionerStopped() {
        Log.d(TAG, "HybridConditioner stopped");
        logFinalStatistics();
    }
    
    @Override
    protected void onNetworkMetricsUpdated(float packetLoss, long rtt, long bandwidth) {
        long currentTime = System.currentTimeMillis();
        
        // Update learning metrics
        updateLearningMetrics(packetLoss, rtt);
        
        // Check if we should switch hybrid mode
        checkModeSwitch(currentTime);
        
        // Determine action based on current conditions and mode
        if (shouldDescendImmediately(packetLoss, rtt)) {
            handleDescend(packetLoss, rtt, currentTime);
        } else if (shouldAscend(packetLoss, rtt, currentTime)) {
            handleAscend(packetLoss, rtt, currentTime);
        }
        
        // Track ascend success/failure
        trackAscendPerformance(packetLoss, rtt);
    }
    
    @Override
    protected void performMonitoringCheck() {
        logCurrentHybridStatus();
        
        // Periodic mode optimization
        optimizeHybridMode();
    }
    
    /**
     * Update learning metrics for network behavior analysis
     */
    private void updateLearningMetrics(float packetLoss, long rtt) {
        mMeasurementCount++;
        
        // Update running averages
        mAveragePacketLoss = (mAveragePacketLoss * (mMeasurementCount - 1) + packetLoss) / mMeasurementCount;
        mAverageRtt = (mAverageRtt * (mMeasurementCount - 1) + rtt) / mMeasurementCount;
        
        // Count condition types
        if (packetLoss <= STABLE_PACKET_LOSS && rtt <= STABLE_RTT) {
            mStableConditionCount++;
        } else if (packetLoss >= CRITICAL_PACKET_LOSS || rtt >= CRITICAL_RTT) {
            mCriticalConditionCount++;
        } else if (packetLoss >= UNSTABLE_PACKET_LOSS || rtt >= UNSTABLE_RTT) {
            mUnstableConditionCount++;
        }
    }
    
    /**
     * Check if hybrid mode should be switched
     */
    private void checkModeSwitch(long currentTime) {
        // Don't switch too frequently
        if (currentTime - mLastModeSwitch < 15000) { // 15 second minimum
            return;
        }
        
        // After learning period, select optimal mode
        if (mCurrentHybridMode == HybridMode.LEARNING && 
            currentTime - mModeStartTime >= LEARNING_PERIOD_MS) {
            selectOptimalMode(currentTime);
        }
        
        // Dynamic mode switching based on recent performance
        if (mCurrentHybridMode != HybridMode.LEARNING) {
            considerDynamicModeSwitch(currentTime);
        }
    }
    
    /**
     * Select optimal mode based on learned network characteristics
     */
    private void selectOptimalMode(long currentTime) {
        float stableRatio = (float) mStableConditionCount / mMeasurementCount;
        float unstableRatio = (float) mUnstableConditionCount / mMeasurementCount;
        float criticalRatio = (float) mCriticalConditionCount / mMeasurementCount;
        
        HybridMode newMode;
        
        if (stableRatio > 0.7f && criticalRatio < 0.1f) {
            // Mostly stable network - use conservative approach for quality
            newMode = HybridMode.CONSERVATIVE;
            Log.d(TAG, "Network learned as STABLE - switching to CONSERVATIVE mode");
        } else if (criticalRatio > 0.3f || unstableRatio > 0.5f) {
            // Unstable network - use aggressive protection
            newMode = HybridMode.AGGRESSIVE;
            Log.d(TAG, "Network learned as UNSTABLE - switching to AGGRESSIVE mode");
        } else {
            // Mixed conditions - use balanced approach
            newMode = HybridMode.BALANCED;
            Log.d(TAG, "Network learned as MIXED - switching to BALANCED mode");
        }
        
        switchToMode(newMode, currentTime);
        
        Log.d(TAG, "Learning complete - Network profile:");
        Log.d(TAG, "  Stable: " + String.format("%.1f", stableRatio * 100) + "%");
        Log.d(TAG, "  Unstable: " + String.format("%.1f", unstableRatio * 100) + "%");
        Log.d(TAG, "  Critical: " + String.format("%.1f", criticalRatio * 100) + "%");
        Log.d(TAG, "  Avg Loss: " + String.format("%.2f", mAveragePacketLoss * 100) + "%");
        Log.d(TAG, "  Avg RTT: " + mAverageRtt + "ms");
    }
    
    /**
     * Consider dynamic mode switching based on performance
     */
    private void considerDynamicModeSwitch(long currentTime) {
        // Check ascend success rate
        if (mSuccessfulAscends + mFailedAscends >= 5) {
            float ascendSuccessRate = (float) mSuccessfulAscends / (mSuccessfulAscends + mFailedAscends);
            
            if (ascendSuccessRate < 0.3f && mCurrentHybridMode == HybridMode.CONSERVATIVE) {
                // Too many failed ascends in conservative mode - switch to aggressive
                switchToMode(HybridMode.AGGRESSIVE, currentTime);
                Log.d(TAG, "Poor ascend success rate - switching to AGGRESSIVE mode");
            } else if (ascendSuccessRate > 0.8f && mCurrentHybridMode == HybridMode.AGGRESSIVE) {
                // High ascend success - can afford to be more conservative
                switchToMode(HybridMode.CONSERVATIVE, currentTime);
                Log.d(TAG, "High ascend success rate - switching to CONSERVATIVE mode");
            }
        }
    }
    
    /**
     * Switch to new hybrid mode
     */
    private void switchToMode(HybridMode newMode, long currentTime) {
        mCurrentHybridMode = newMode;
        mLastModeSwitch = currentTime;
        mModeStartTime = currentTime;
        
        // Reset performance tracking for new mode
        mSuccessfulAscends = 0;
        mFailedAscends = 0;
    }
    
    /**
     * Determine if immediate quality reduction is needed
     */
    private boolean shouldDescendImmediately(float packetLoss, long rtt) {
        // Critical conditions always trigger immediate descend
        if (packetLoss >= CRITICAL_PACKET_LOSS || rtt >= CRITICAL_RTT) {
            return canDescend();
        }
        
        // Mode-specific descend triggers
        switch (mCurrentHybridMode) {
            case AGGRESSIVE:
                return (packetLoss >= UNSTABLE_PACKET_LOSS || rtt >= UNSTABLE_RTT) && canDescend();
            case CONSERVATIVE:
                return (packetLoss >= CRITICAL_PACKET_LOSS * 0.8f || rtt >= CRITICAL_RTT * 0.8f) && canDescend();
            case BALANCED:
            default:
                return (packetLoss >= UNSTABLE_PACKET_LOSS * 1.2f || rtt >= UNSTABLE_RTT * 1.2f) && canDescend();
        }
    }
    
    /**
     * Determine if quality improvement should occur
     */
    private boolean shouldAscend(float packetLoss, long rtt, long currentTime) {
        // Must have stable conditions
        if (packetLoss > STABLE_PACKET_LOSS || rtt > STABLE_RTT) {
            return false;
        }
        
        // Must not be at maximum quality
        if (mCurrentBitrate.get() >= mInitialBitrate) {
            return false;
        }
        
        // Mode-specific ascend timing
        long requiredDelay = getAscendDelayForMode();
        return canAscend(currentTime, requiredDelay);
    }
    
    /**
     * Handle quality reduction
     */
    private void handleDescend(float packetLoss, long rtt, long currentTime) {
        if (!canDescend()) {
            return;
        }
        
        // Get descend multiplier for current mode
        float descendMultiplier = getDescendMultiplierForMode();
        
        int currentBitrate = mCurrentBitrate.get();
        int newBitrate = (int) (currentBitrate * descendMultiplier);
        
        // Frame rate adjustment for severe conditions
        int currentFrameRate = mCurrentFrameRate.get();
        int newFrameRate = currentFrameRate;
        if (packetLoss >= CRITICAL_PACKET_LOSS && currentFrameRate > 20) {
            newFrameRate = Math.max(15, (int)(currentFrameRate * 0.85f));
        }
        
        String reason = String.format(
            "HYBRID_%s_DESCEND: Loss %.1f%%, RTT %dms",
            mCurrentHybridMode, packetLoss * 100, rtt
        );
        
        QualityAdjustment adjustment = new QualityAdjustment(
            clampBitrate(newBitrate), clampFrameRate(newFrameRate), reason, mCurrentCondition
        );
        
        applyQualityAdjustment(adjustment);
        
        mLastDescendTime = currentTime;
        mLastAdjustmentWasAscend = false;
        
        Log.d(TAG, "Hybrid descend (" + mCurrentHybridMode + "): " + 
                   (currentBitrate / 1024) + " -> " + (newBitrate / 1024) + " Kbps");
    }
    
    /**
     * Handle quality improvement
     */
    private void handleAscend(float packetLoss, long rtt, long currentTime) {
        long requiredDelay = getAscendDelayForMode();
        if (!canAscend(currentTime, requiredDelay)) {
            return;
        }
        
        // Get ascend multiplier for current mode
        float ascendMultiplier = getAscendMultiplierForMode();
        
        int currentBitrate = mCurrentBitrate.get();
        int newBitrate = (int) (currentBitrate * ascendMultiplier);
        
        // Don't exceed initial bitrate
        newBitrate = Math.min(mInitialBitrate, newBitrate);
        
        // Frame rate improvement for high quality
        int currentFrameRate = mCurrentFrameRate.get();
        int newFrameRate = currentFrameRate;
        if (newBitrate >= (mInitialBitrate * 0.8f) && currentFrameRate < 30) {
            newFrameRate = Math.min(30, currentFrameRate + 2);
        }
        
        String reason = String.format(
            "HYBRID_%s_ASCEND: Stable conditions, Loss %.1f%%, RTT %dms",
            mCurrentHybridMode, packetLoss * 100, rtt
        );
        
        QualityAdjustment adjustment = new QualityAdjustment(
            clampBitrate(newBitrate), clampFrameRate(newFrameRate), reason, mCurrentCondition
        );
        
        applyQualityAdjustment(adjustment);
        
        mLastAscendTime = currentTime;
        mLastAdjustmentWasAscend = true;
        
        Log.d(TAG, "Hybrid ascend (" + mCurrentHybridMode + "): " + 
                   (currentBitrate / 1024) + " -> " + (newBitrate / 1024) + " Kbps");
    }
    
    /**
     * Track ascend performance for dynamic mode switching
     */
    private void trackAscendPerformance(float packetLoss, long rtt) {
        // Check if recent ascend failed (followed by poor conditions)
        if (mLastAdjustmentWasAscend && (packetLoss > UNSTABLE_PACKET_LOSS || rtt > UNSTABLE_RTT)) {
            mFailedAscends++;
            mLastAdjustmentWasAscend = false;
            Log.d(TAG, "Ascend followed by poor conditions - marking as failed");
        } else if (mLastAdjustmentWasAscend && (packetLoss <= STABLE_PACKET_LOSS && rtt <= STABLE_RTT)) {
            mSuccessfulAscends++;
            mLastAdjustmentWasAscend = false;
            Log.d(TAG, "Ascend followed by stable conditions - marking as successful");
        }
    }
    
    /**
     * Get descend multiplier for current mode
     */
    private float getDescendMultiplierForMode() {
        switch (mCurrentHybridMode) {
            case CONSERVATIVE: return CONSERVATIVE_DESCEND;
            case AGGRESSIVE: return AGGRESSIVE_DESCEND;
            case BALANCED:
            default: return BALANCED_DESCEND;
        }
    }
    
    /**
     * Get ascend multiplier for current mode
     */
    private float getAscendMultiplierForMode() {
        switch (mCurrentHybridMode) {
            case CONSERVATIVE: return CONSERVATIVE_ASCEND;
            case AGGRESSIVE: return AGGRESSIVE_ASCEND;
            case BALANCED:
            default: return BALANCED_ASCEND;
        }
    }
    
    /**
     * Get ascend delay for current mode
     */
    private long getAscendDelayForMode() {
        switch (mCurrentHybridMode) {
            case CONSERVATIVE: return CONSERVATIVE_ASCEND_DELAY;
            case AGGRESSIVE: return AGGRESSIVE_ASCEND_DELAY;
            case BALANCED:
            default: return BALANCED_ASCEND_DELAY;
        }
    }
    
    /**
     * Check if descend is allowed
     */
    private boolean canDescend() {
        long timeSinceLastDescend = System.currentTimeMillis() - mLastDescendTime;
        return timeSinceLastDescend >= FAST_DESCEND_DELAY;
    }
    
    /**
     * Check if ascend is allowed
     */
    private boolean canAscend(long currentTime, long requiredDelay) {
        long timeSinceLastAscend = currentTime - mLastAscendTime;
        return timeSinceLastAscend >= requiredDelay;
    }
    
    /**
     * Reset learning counters
     */
    private void resetLearningCounters() {
        mStableConditionCount = 0;
        mUnstableConditionCount = 0;
        mCriticalConditionCount = 0;
        mAveragePacketLoss = 0.0f;
        mAverageRtt = 0;
        mMeasurementCount = 0;
        mSuccessfulAscends = 0;
        mFailedAscends = 0;
    }
    
    /**
     * Optimize hybrid mode based on performance
     */
    private void optimizeHybridMode() {
        // Periodic optimization could be added here
        // For now, just log current performance
        if (mMeasurementCount > 0) {
            Log.d(TAG, "Performance: " + mSuccessfulAscends + " successful ascends, " + 
                       mFailedAscends + " failed ascends");
        }
    }
    
    /**
     * Log current hybrid status
     */
    private void logCurrentHybridStatus() {
        long modeUptime = System.currentTimeMillis() - mModeStartTime;
        Log.d(TAG, String.format(
            "Hybrid Status: %s mode for %ds, %d Kbps, %d FPS, %s",
            mCurrentHybridMode,
            modeUptime / 1000,
            mCurrentBitrate.get() / 1024,
            mCurrentFrameRate.get(),
            mCurrentCondition
        ));
    }
    
    /**
     * Log final statistics
     */
    private void logFinalStatistics() {
        float qualityRetention = (float) mCurrentBitrate.get() / mInitialBitrate;
        float ascendSuccessRate = mSuccessfulAscends + mFailedAscends > 0 ? 
            (float) mSuccessfulAscends / (mSuccessfulAscends + mFailedAscends) : 0;
        
        Log.d(TAG, "Final Hybrid Statistics:");
        Log.d(TAG, "  Final mode: " + mCurrentHybridMode);
        Log.d(TAG, "  Quality retention: " + String.format("%.1f", qualityRetention * 100) + "%");
        Log.d(TAG, "  Ascend success rate: " + String.format("%.1f", ascendSuccessRate * 100) + "%");
        Log.d(TAG, "  Network measurements: " + mMeasurementCount);
        Log.d(TAG, "  Avg packet loss: " + String.format("%.2f", mAveragePacketLoss * 100) + "%");
        Log.d(TAG, "  Avg RTT: " + mAverageRtt + "ms");
        Log.d(TAG, "  Total adjustments: " + mStats.totalAdjustments);
        Log.d(TAG, "  Uptime: " + (mStats.uptime / 1000) + " seconds");
    }
    
    /**
     * Get current hybrid mode
     */
    public HybridMode getCurrentHybridMode() {
        return mCurrentHybridMode;
    }
    
    /**
     * Get ascend success rate
     */
    public float getAscendSuccessRate() {
        if (mSuccessfulAscends + mFailedAscends == 0) {
            return 0.0f;
        }
        return (float) mSuccessfulAscends / (mSuccessfulAscends + mFailedAscends);
    }
}