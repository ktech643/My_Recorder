package com.checkmate.android.util.stream;

import android.util.Log;

/**
 * LadderAscendConditioner - Conservative quality improvement mode (Mode 1)
 * 
 * This conditioner gradually improves quality when network conditions are stable,
 * using a ladder-like approach with careful steps. It prioritizes stability and
 * avoids quality oscillations by being very conservative with improvements.
 * 
 * Algorithm characteristics:
 * - Slow, stepped quality improvements (ladder approach)
 * - Fast quality reduction when needed to maintain stability
 * - Long observation periods before quality improvements
 * - Multiple quality levels with validation at each step
 * 
 * Best for:
 * - Networks with periodic congestion
 * - Situations where quality consistency is important
 * - Professional streaming where smooth quality transitions are preferred
 */
public class LadderAscendConditioner extends BaseStreamConditioner {
    
    private static final String TAG = "LadderAscendConditioner";
    
    // Ladder adaptation parameters
    private static final float DESCEND_MULTIPLIER = 0.75f;    // 25% reduction for descend
    private static final float ASCEND_STEP_SIZE = 0.15f;      // 15% increase per ladder step
    private static final long OBSERVATION_PERIOD_MS = 10000;  // 10 seconds observation
    private static final long STABILITY_REQUIRED_MS = 15000;  // 15 seconds stability for ascend
    private static final long FAST_DESCEND_MS = 2000;         // 2 seconds for descend
    
    // Quality ladder levels (percentages of initial bitrate)
    private static final float[] QUALITY_LADDER = {
        0.3f,   // Level 0: 30% (minimum)
        0.45f,  // Level 1: 45%
        0.6f,   // Level 2: 60%
        0.75f,  // Level 3: 75%
        0.9f,   // Level 4: 90%
        1.0f    // Level 5: 100% (maximum)
    };
    
    // Network condition thresholds
    private static final float EXCELLENT_PACKET_LOSS = 0.02f; // 2% packet loss
    private static final long EXCELLENT_RTT = 50;             // 50ms RTT
    private static final float GOOD_PACKET_LOSS = 0.05f;      // 5% packet loss
    private static final long GOOD_RTT = 100;                 // 100ms RTT
    private static final float POOR_PACKET_LOSS = 0.12f;      // 12% packet loss
    private static final long POOR_RTT = 250;                 // 250ms RTT
    
    // State tracking
    private int mInitialBitrate;
    private int mCurrentLadderLevel = 5; // Start at maximum
    private long mStableConditionStartTime = 0;
    private long mLastAscendTime = 0;
    private long mLastDescendTime = 0;
    private int mStableConditionCount = 0;
    private NetworkCondition mStableCondition = NetworkCondition.GOOD;
    
    public LadderAscendConditioner() {
        super(ConditionerMode.LADDER_ASCEND);
    }
    
    @Override
    protected void onConditionerStarted(int initialBitrate) {
        mInitialBitrate = initialBitrate;
        mCurrentLadderLevel = 5; // Start at maximum quality
        
        Log.d(TAG, "LadderAscendConditioner started:");
        Log.d(TAG, "  Initial bitrate: " + (initialBitrate / 1024) + " Kbps");
        Log.d(TAG, "  Quality ladder levels: " + QUALITY_LADDER.length);
        Log.d(TAG, "  Strategy: Conservative ladder ascend, fast descend");
        
        // Initialize state
        mStableConditionStartTime = System.currentTimeMillis();
        mLastAscendTime = 0;
        mLastDescendTime = 0;
        mStableConditionCount = 0;
        mStableCondition = NetworkCondition.GOOD;
        
        logLadderLevels();
    }
    
    @Override
    protected void onConditionerStopped() {
        Log.d(TAG, "LadderAscendConditioner stopped");
        logFinalStatistics();
    }
    
    @Override
    protected void onNetworkMetricsUpdated(float packetLoss, long rtt, long bandwidth) {
        long currentTime = System.currentTimeMillis();
        
        // Determine current network quality tier
        NetworkCondition currentTier = determineNetworkTier(packetLoss, rtt);
        
        // Check for immediate descend needs
        if (shouldDescendImmediately(currentTier, packetLoss, rtt)) {
            handleFastDescend(currentTier, packetLoss, rtt, currentTime);
        } else {
            // Track stability for potential ascend
            trackStabilityForAscend(currentTier, currentTime);
            
            // Check for ladder ascend opportunity
            if (shouldAscendLadder(currentTime)) {
                handleLadderAscend(currentTier, currentTime);
            }
        }
    }
    
    @Override
    protected void performMonitoringCheck() {
        logCurrentLadderStatus();
        
        // Check for stability timeout
        long stabilityDuration = System.currentTimeMillis() - mStableConditionStartTime;
        if (stabilityDuration > STABILITY_REQUIRED_MS * 2) {
            Log.d(TAG, "Extended stability period: " + (stabilityDuration / 1000) + "s - " +
                       "Ladder level: " + mCurrentLadderLevel);
        }
    }
    
    /**
     * Determine network tier based on packet loss and RTT
     */
    private NetworkCondition determineNetworkTier(float packetLoss, long rtt) {
        if (packetLoss <= EXCELLENT_PACKET_LOSS && rtt <= EXCELLENT_RTT) {
            return NetworkCondition.EXCELLENT;
        } else if (packetLoss <= GOOD_PACKET_LOSS && rtt <= GOOD_RTT) {
            return NetworkCondition.GOOD;
        } else if (packetLoss <= POOR_PACKET_LOSS && rtt <= POOR_RTT) {
            return NetworkCondition.FAIR;
        } else {
            return NetworkCondition.POOR;
        }
    }
    
    /**
     * Determine if immediate quality reduction is needed
     */
    private boolean shouldDescendImmediately(NetworkCondition tier, float packetLoss, long rtt) {
        // Immediate descend for poor/critical conditions
        if (tier == NetworkCondition.POOR || tier == NetworkCondition.CRITICAL) {
            return canDescend();
        }
        
        // Also descend if we're at a high ladder level but conditions deteriorated
        if (tier == NetworkCondition.FAIR && mCurrentLadderLevel >= 4) {
            return canDescend();
        }
        
        return false;
    }
    
    /**
     * Handle fast quality reduction
     */
    private void handleFastDescend(NetworkCondition tier, float packetLoss, long rtt, long currentTime) {
        if (!canDescend()) {
            return;
        }
        
        // Calculate how many ladder levels to descend
        int levelsToDescend = calculateDescendLevels(tier, packetLoss, rtt);
        int newLadderLevel = Math.max(0, mCurrentLadderLevel - levelsToDescend);
        
        // Apply ladder level
        applyLadderLevel(newLadderLevel, String.format(
            "FAST_DESCEND: Network tier %s, Loss %.1f%%, RTT %dms",
            tier, packetLoss * 100, rtt
        ));
        
        mLastDescendTime = currentTime;
        mCurrentLadderLevel = newLadderLevel;
        
        // Reset stability tracking
        resetStabilityTracking(tier, currentTime);
        
        Log.d(TAG, "Fast descend: Level " + (mCurrentLadderLevel + levelsToDescend) + 
                   " -> " + mCurrentLadderLevel + " (" + levelsToDescend + " levels)");
    }
    
    /**
     * Track network stability for potential ascend
     */
    private void trackStabilityForAscend(NetworkCondition tier, long currentTime) {
        if (tier == mStableCondition) {
            mStableConditionCount++;
        } else {
            // Condition changed - reset tracking
            resetStabilityTracking(tier, currentTime);
        }
    }
    
    /**
     * Determine if ladder ascend should occur
     */
    private boolean shouldAscendLadder(long currentTime) {
        // Must be at a lower ladder level
        if (mCurrentLadderLevel >= QUALITY_LADDER.length - 1) {
            return false;
        }
        
        // Must have stable good conditions
        if (mStableCondition != NetworkCondition.EXCELLENT && mStableCondition != NetworkCondition.GOOD) {
            return false;
        }
        
        // Must have been stable for required duration
        long stabilityDuration = currentTime - mStableConditionStartTime;
        if (stabilityDuration < STABILITY_REQUIRED_MS) {
            return false;
        }
        
        // Must respect ascend timing
        return canAscend(currentTime);
    }
    
    /**
     * Handle ladder quality improvement
     */
    private void handleLadderAscend(NetworkCondition tier, long currentTime) {
        if (!shouldAscendLadder(currentTime)) {
            return;
        }
        
        // Move up one ladder level
        int newLadderLevel = Math.min(QUALITY_LADDER.length - 1, mCurrentLadderLevel + 1);
        
        // Apply ladder level
        applyLadderLevel(newLadderLevel, String.format(
            "LADDER_ASCEND: Stable %s for %ds, %d measurements",
            tier, (currentTime - mStableConditionStartTime) / 1000, mStableConditionCount
        ));
        
        mLastAscendTime = currentTime;
        mCurrentLadderLevel = newLadderLevel;
        
        // Reset stability tracking for next level
        resetStabilityTracking(tier, currentTime);
        
        Log.d(TAG, "Ladder ascend: Level " + (newLadderLevel - 1) + " -> " + newLadderLevel +
                   " (" + (int)(QUALITY_LADDER[newLadderLevel] * 100) + "% quality)");
    }
    
    /**
     * Apply specific ladder level
     */
    private void applyLadderLevel(int ladderLevel, String reason) {
        if (ladderLevel < 0 || ladderLevel >= QUALITY_LADDER.length) {
            Log.e(TAG, "Invalid ladder level: " + ladderLevel);
            return;
        }
        
        float qualityRatio = QUALITY_LADDER[ladderLevel];
        int newBitrate = (int) (mInitialBitrate * qualityRatio);
        
        // Frame rate adjustments based on ladder level
        int newFrameRate = calculateFrameRateForLevel(ladderLevel);
        
        // Ensure bounds
        newBitrate = clampBitrate(newBitrate);
        newFrameRate = clampFrameRate(newFrameRate);
        
        QualityAdjustment adjustment = new QualityAdjustment(
            newBitrate, newFrameRate, reason, mCurrentCondition
        );
        
        applyQualityAdjustment(adjustment);
    }
    
    /**
     * Calculate frame rate for specific ladder level
     */
    private int calculateFrameRateForLevel(int ladderLevel) {
        // Frame rate scaling based on ladder level
        if (ladderLevel <= 1) {
            return 15; // Low quality: 15 FPS
        } else if (ladderLevel <= 3) {
            return 24; // Medium quality: 24 FPS
        } else {
            return 30; // High quality: 30 FPS
        }
    }
    
    /**
     * Calculate how many levels to descend based on network conditions
     */
    private int calculateDescendLevels(NetworkCondition tier, float packetLoss, long rtt) {
        if (tier == NetworkCondition.CRITICAL || packetLoss > 0.25f) {
            return 3; // Major descend
        } else if (tier == NetworkCondition.POOR || packetLoss > 0.15f) {
            return 2; // Moderate descend
        } else {
            return 1; // Minor descend
        }
    }
    
    /**
     * Reset stability tracking
     */
    private void resetStabilityTracking(NetworkCondition newCondition, long currentTime) {
        mStableCondition = newCondition;
        mStableConditionStartTime = currentTime;
        mStableConditionCount = 1;
    }
    
    /**
     * Check if descend is allowed
     */
    private boolean canDescend() {
        long timeSinceLastDescend = System.currentTimeMillis() - mLastDescendTime;
        return timeSinceLastDescend >= FAST_DESCEND_MS;
    }
    
    /**
     * Check if ascend is allowed
     */
    private boolean canAscend(long currentTime) {
        long timeSinceLastAscend = currentTime - mLastAscendTime;
        return timeSinceLastAscend >= OBSERVATION_PERIOD_MS;
    }
    
    /**
     * Log ladder levels for reference
     */
    private void logLadderLevels() {
        Log.d(TAG, "Quality Ladder Levels:");
        for (int i = 0; i < QUALITY_LADDER.length; i++) {
            int bitrate = (int) (mInitialBitrate * QUALITY_LADDER[i]);
            int frameRate = calculateFrameRateForLevel(i);
            Log.d(TAG, String.format("  Level %d: %d%% quality, %d Kbps, %d FPS",
                i, (int)(QUALITY_LADDER[i] * 100), bitrate / 1024, frameRate));
        }
    }
    
    /**
     * Log current ladder status
     */
    private void logCurrentLadderStatus() {
        long stabilityDuration = System.currentTimeMillis() - mStableConditionStartTime;
        Log.d(TAG, String.format(
            "Ladder Status: Level %d (%d%%), %d Kbps, %d FPS, Stable %s for %ds",
            mCurrentLadderLevel,
            (int)(QUALITY_LADDER[mCurrentLadderLevel] * 100),
            mCurrentBitrate.get() / 1024,
            mCurrentFrameRate.get(),
            mStableCondition,
            stabilityDuration / 1000
        ));
    }
    
    /**
     * Log final statistics
     */
    private void logFinalStatistics() {
        float finalQuality = QUALITY_LADDER[mCurrentLadderLevel];
        
        Log.d(TAG, "Final Statistics:");
        Log.d(TAG, "  Initial bitrate: " + (mInitialBitrate / 1024) + " Kbps (Level 5)");
        Log.d(TAG, "  Final ladder level: " + mCurrentLadderLevel);
        Log.d(TAG, "  Final quality: " + (int)(finalQuality * 100) + "%");
        Log.d(TAG, "  Final bitrate: " + (mCurrentBitrate.get() / 1024) + " Kbps");
        Log.d(TAG, "  Total adjustments: " + mStats.totalAdjustments);
        Log.d(TAG, "  Ladder ascends: " + mStats.bitrateIncreases);
        Log.d(TAG, "  Ladder descends: " + mStats.bitrateDecreases);
        Log.d(TAG, "  Uptime: " + (mStats.uptime / 1000) + " seconds");
    }
    
    /**
     * Get current ladder level (0-5)
     */
    public int getCurrentLadderLevel() {
        return mCurrentLadderLevel;
    }
    
    /**
     * Get quality percentage for current ladder level
     */
    public float getCurrentQualityPercentage() {
        return QUALITY_LADDER[mCurrentLadderLevel] * 100;
    }
}