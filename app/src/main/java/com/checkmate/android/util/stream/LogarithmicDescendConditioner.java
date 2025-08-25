package com.checkmate.android.util.stream;

import android.util.Log;

/**
 * LogarithmicDescendConditioner - Aggressive quality reduction mode (Mode 0)
 * 
 * This conditioner quickly reduces quality when network conditions deteriorate
 * using a logarithmic scale for rapid adaptation. It prioritizes stream stability
 * over maximum quality by aggressively dropping bitrate and FPS when needed.
 * 
 * Algorithm characteristics:
 * - Fast response to network degradation (within 2-3 seconds)
 * - Logarithmic quality reduction for rapid stabilization
 * - Conservative quality improvement to avoid oscillation
 * - Prioritizes stream continuity over peak quality
 * 
 * Best for:
 * - Unstable or limited bandwidth networks
 * - Mobile streaming with variable connectivity
 * - Situations where stream reliability is more important than quality
 */
public class LogarithmicDescendConditioner extends BaseStreamConditioner {
    
    private static final String TAG = "LogarithmicDescendConditioner";
    
    // Aggressive adaptation parameters
    private static final float DESCEND_MULTIPLIER = 0.7f;     // 30% reduction per step
    private static final float ASCEND_MULTIPLIER = 1.1f;      // 10% increase per step
    private static final long FAST_ADAPTATION_MS = 1000;      // 1 second for descend
    private static final long SLOW_ADAPTATION_MS = 8000;      // 8 seconds for ascend
    
    // Quality thresholds
    private static final float CRITICAL_PACKET_LOSS = 0.15f;  // 15% packet loss
    private static final long CRITICAL_RTT = 300;             // 300ms RTT
    private static final float POOR_PACKET_LOSS = 0.08f;      // 8% packet loss
    private static final long POOR_RTT = 150;                 // 150ms RTT
    
    // State tracking
    private int mInitialBitrate;
    private long mLastDescendTime = 0;
    private long mLastAscendTime = 0;
    private int mConsecutiveGoodMeasurements = 0;
    private boolean mInAggressiveMode = false;
    
    public LogarithmicDescendConditioner() {
        super(ConditionerMode.LOGARITHMIC_DESCEND);
    }
    
    @Override
    protected void onConditionerStarted(int initialBitrate) {
        mInitialBitrate = initialBitrate;
        
        Log.d(TAG, "LogarithmicDescendConditioner started:");
        Log.d(TAG, "  Initial bitrate: " + (initialBitrate / 1024) + " Kbps");
        Log.d(TAG, "  Strategy: Aggressive descend, conservative ascend");
        Log.d(TAG, "  Fast adaptation for quality reduction");
        
        // Reset state
        mLastDescendTime = 0;
        mLastAscendTime = 0;
        mConsecutiveGoodMeasurements = 0;
        mInAggressiveMode = false;
    }
    
    @Override
    protected void onConditionerStopped() {
        Log.d(TAG, "LogarithmicDescendConditioner stopped");
        logFinalStatistics();
    }
    
    @Override
    protected void onNetworkMetricsUpdated(float packetLoss, long rtt, long bandwidth) {
        long currentTime = System.currentTimeMillis();
        
        // Determine if immediate action is needed
        if (shouldDescendAggressively(packetLoss, rtt)) {
            handleAggressiveDescend(packetLoss, rtt, currentTime);
        } else if (shouldAscendConservatively(packetLoss, rtt)) {
            handleConservativeAscend(packetLoss, rtt, currentTime);
        } else {
            // Stable conditions - reset consecutive counter
            if (mCurrentCondition == NetworkCondition.GOOD || mCurrentCondition == NetworkCondition.EXCELLENT) {
                mConsecutiveGoodMeasurements++;
            } else {
                mConsecutiveGoodMeasurements = 0;
            }
        }
    }
    
    @Override
    protected void performMonitoringCheck() {
        // Check if we've been in aggressive mode too long
        if (mInAggressiveMode && (System.currentTimeMillis() - mLastDescendTime) > 30000) {
            Log.d(TAG, "Exiting aggressive mode after 30 seconds");
            mInAggressiveMode = false;
        }
        
        // Log current status
        logCurrentStatus();
    }
    
    /**
     * Determine if aggressive quality reduction is needed
     */
    private boolean shouldDescendAggressively(float packetLoss, long rtt) {
        // Immediate triggers for aggressive reduction
        if (packetLoss >= CRITICAL_PACKET_LOSS || rtt >= CRITICAL_RTT) {
            return true;
        }
        
        // Secondary triggers for poor conditions
        if (packetLoss >= POOR_PACKET_LOSS || rtt >= POOR_RTT) {
            return canDescend();
        }
        
        return false;
    }
    
    /**
     * Determine if conservative quality improvement is possible
     */
    private boolean shouldAscendConservatively(float packetLoss, long rtt) {
        // Only ascend if conditions are good for a sustained period
        if (mCurrentCondition == NetworkCondition.EXCELLENT && mConsecutiveGoodMeasurements >= 3) {
            return canAscend();
        } else if (mCurrentCondition == NetworkCondition.GOOD && mConsecutiveGoodMeasurements >= 5) {
            return canAscend();
        }
        
        return false;
    }
    
    /**
     * Handle aggressive quality reduction
     */
    private void handleAggressiveDescend(float packetLoss, long rtt, long currentTime) {
        if (!canDescend()) {
            return;
        }
        
        // Calculate logarithmic reduction
        int currentBitrate = mCurrentBitrate.get();
        int currentFrameRate = mCurrentFrameRate.get();
        
        // Aggressive bitrate reduction using logarithmic scale
        int newBitrate = (int) (currentBitrate * DESCEND_MULTIPLIER);
        
        // Also reduce frame rate if bitrate gets very low
        int newFrameRate = currentFrameRate;
        if (newBitrate < (mInitialBitrate * 0.3f) && currentFrameRate > 15) {
            newFrameRate = Math.max(15, (int) (currentFrameRate * 0.8f));
        }
        
        // Ensure minimum quality
        newBitrate = Math.max(MIN_BITRATE, newBitrate);
        newFrameRate = Math.max(MIN_FRAMERATE, newFrameRate);
        
        String reason = String.format(
            "LOGARITHMIC_DESCEND: Loss %.1f%% (>%.1f%%), RTT %dms (>%dms)",
            packetLoss * 100, CRITICAL_PACKET_LOSS * 100, rtt, CRITICAL_RTT
        );
        
        QualityAdjustment adjustment = new QualityAdjustment(
            newBitrate, newFrameRate, reason, mCurrentCondition
        );
        
        applyQualityAdjustment(adjustment);
        
        mLastDescendTime = currentTime;
        mInAggressiveMode = true;
        mConsecutiveGoodMeasurements = 0;
        
        Log.d(TAG, "Aggressive descend applied: " + (currentBitrate / 1024) + " -> " + 
                   (newBitrate / 1024) + " Kbps");
    }
    
    /**
     * Handle conservative quality improvement
     */
    private void handleConservativeAscend(float packetLoss, long rtt, long currentTime) {
        if (!canAscend()) {
            return;
        }
        
        int currentBitrate = mCurrentBitrate.get();
        int currentFrameRate = mCurrentFrameRate.get();
        
        // Conservative bitrate increase
        int newBitrate = (int) (currentBitrate * ASCEND_MULTIPLIER);
        
        // Don't exceed initial bitrate
        newBitrate = Math.min(mInitialBitrate, newBitrate);
        
        // Conservative frame rate increase
        int newFrameRate = currentFrameRate;
        if (newBitrate >= (mInitialBitrate * 0.8f) && currentFrameRate < 30) {
            newFrameRate = Math.min(30, currentFrameRate + 2);
        }
        
        String reason = String.format(
            "CONSERVATIVE_ASCEND: Stable conditions for %d measurements",
            mConsecutiveGoodMeasurements
        );
        
        QualityAdjustment adjustment = new QualityAdjustment(
            newBitrate, newFrameRate, reason, mCurrentCondition
        );
        
        applyQualityAdjustment(adjustment);
        
        mLastAscendTime = currentTime;
        mConsecutiveGoodMeasurements = 0; // Reset after ascend
        
        Log.d(TAG, "Conservative ascend applied: " + (currentBitrate / 1024) + " -> " + 
                   (newBitrate / 1024) + " Kbps");
    }
    
    /**
     * Check if descend is allowed (respects fast adaptation timing)
     */
    private boolean canDescend() {
        long timeSinceLastDescend = System.currentTimeMillis() - mLastDescendTime;
        return timeSinceLastDescend >= FAST_ADAPTATION_MS;
    }
    
    /**
     * Check if ascend is allowed (respects slow adaptation timing)
     */
    private boolean canAscend() {
        long timeSinceLastAscend = System.currentTimeMillis() - mLastAscendTime;
        return timeSinceLastAscend >= SLOW_ADAPTATION_MS && 
               mCurrentBitrate.get() < mInitialBitrate;
    }
    
    /**
     * Log current status for monitoring
     */
    private void logCurrentStatus() {
        Log.d(TAG, String.format(
            "Status: %d Kbps, %d FPS, %s, Consecutive good: %d, Aggressive: %s",
            mCurrentBitrate.get() / 1024,
            mCurrentFrameRate.get(),
            mCurrentCondition,
            mConsecutiveGoodMeasurements,
            mInAggressiveMode
        ));
    }
    
    /**
     * Log final statistics
     */
    private void logFinalStatistics() {
        int finalBitrate = mCurrentBitrate.get();
        float qualityRetention = (float) finalBitrate / mInitialBitrate;
        
        Log.d(TAG, "Final Statistics:");
        Log.d(TAG, "  Initial bitrate: " + (mInitialBitrate / 1024) + " Kbps");
        Log.d(TAG, "  Final bitrate: " + (finalBitrate / 1024) + " Kbps");
        Log.d(TAG, "  Quality retention: " + String.format("%.1f", qualityRetention * 100) + "%");
        Log.d(TAG, "  Total adjustments: " + mStats.totalAdjustments);
        Log.d(TAG, "  Bitrate increases: " + mStats.bitrateIncreases);
        Log.d(TAG, "  Bitrate decreases: " + mStats.bitrateDecreases);
        Log.d(TAG, "  Uptime: " + (mStats.uptime / 1000) + " seconds");
    }
}