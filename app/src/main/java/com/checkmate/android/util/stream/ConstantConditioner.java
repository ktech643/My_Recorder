package com.checkmate.android.util.stream;

import android.util.Log;

/**
 * ConstantConditioner - Fixed bitrate and FPS mode (Mode 3)
 * 
 * This conditioner maintains constant bitrate and frame rate as configured by the user.
 * It does not perform any adaptive adjustments based on network conditions.
 * 
 * Use cases:
 * - Stable, high-quality networks
 * - Professional broadcasting with guaranteed bandwidth
 * - Testing with known network conditions
 * - When manual control is preferred over automatic adaptation
 */
public class ConstantConditioner extends BaseStreamConditioner {
    
    private static final String TAG = "ConstantConditioner";
    
    // Fixed configuration
    private int mFixedBitrate;
    private int mFixedFrameRate;
    
    public ConstantConditioner() {
        super(ConditionerMode.CONSTANT);
    }
    
    @Override
    protected void onConditionerStarted(int initialBitrate) {
        mFixedBitrate = initialBitrate;
        mFixedFrameRate = 30; // Default 30 FPS, could be made configurable
        
        Log.d(TAG, "ConstantConditioner started with fixed settings:");
        Log.d(TAG, "  Bitrate: " + (mFixedBitrate / 1024) + " Kbps");
        Log.d(TAG, "  Frame Rate: " + mFixedFrameRate + " FPS");
        Log.d(TAG, "  No adaptive adjustments will be made");
        
        // Set the fixed values
        mCurrentBitrate.set(mFixedBitrate);
        mCurrentFrameRate.set(mFixedFrameRate);
    }
    
    @Override
    protected void onConditionerStopped() {
        Log.d(TAG, "ConstantConditioner stopped");
        Log.d(TAG, "Final statistics:");
        Log.d(TAG, "  Uptime: " + (mStats.uptime / 1000) + " seconds");
        Log.d(TAG, "  Network condition changes observed: " + getNetworkConditionChanges());
        Log.d(TAG, "  Forced adjustments applied: " + mStats.totalAdjustments);
    }
    
    @Override
    protected void onNetworkMetricsUpdated(float packetLoss, long rtt, long bandwidth) {
        // Constant mode does not react to network changes
        // Only log significant network issues as warnings
        
        if (mCurrentCondition == NetworkCondition.CRITICAL) {
            Log.w(TAG, "CRITICAL network conditions detected but maintaining constant quality:");
            Log.w(TAG, "  Packet Loss: " + String.format("%.1f", packetLoss * 100) + "%");
            Log.w(TAG, "  RTT: " + rtt + "ms");
            Log.w(TAG, "  Consider switching to adaptive mode or reducing quality manually");
        } else if (mCurrentCondition == NetworkCondition.POOR) {
            Log.w(TAG, "POOR network conditions detected - constant mode active");
        }
    }
    
    @Override
    protected void performMonitoringCheck() {
        // Constant mode monitoring just tracks network condition
        // No quality adjustments are made
        
        if (mCurrentCondition == NetworkCondition.CRITICAL) {
            // Log warning every monitoring cycle if network is critical
            Log.w(TAG, "Network remains in CRITICAL condition - " +
                       "consider manual quality reduction or switching to adaptive mode");
            
            if (mCallback != null) {
                mCallback.onConditionerError(
                    "Network in critical condition but constant mode cannot adapt automatically"
                );
            }
        }
        
        // Update statistics tracking
        updateNetworkConditionHistory();
    }
    
    /**
     * Get number of network condition changes for statistics
     */
    private int getNetworkConditionChanges() {
        // This would typically be tracked, but for simplicity we'll return 0
        // In a full implementation, you'd track condition changes over time
        return 0;
    }
    
    /**
     * Update network condition history for statistics
     */
    private void updateNetworkConditionHistory() {
        // Track average network condition
        // For simplicity, just update the current condition
        mStats.averageCondition = mCurrentCondition;
        
        // Log periodic status for constant mode
        if (mStats.totalAdjustments == 0) {
            Log.d(TAG, "Constant mode status - Bitrate: " + (mCurrentBitrate.get() / 1024) + 
                       " Kbps, FPS: " + mCurrentFrameRate.get() + 
                       ", Network: " + mCurrentCondition);
        }
    }
    
    /**
     * Override to provide constant-mode specific behavior
     */
    @Override
    public void forceQualityAdjustment(int bitrate, int frameRate, String reason) {
        Log.d(TAG, "Force adjustment in constant mode - this will change the fixed settings");
        
        // Update fixed values
        mFixedBitrate = clampBitrate(bitrate);
        mFixedFrameRate = clampFrameRate(frameRate);
        
        Log.d(TAG, "New fixed settings - Bitrate: " + (mFixedBitrate / 1024) + 
                   " Kbps, FPS: " + mFixedFrameRate);
        
        // Apply the adjustment
        super.forceQualityAdjustment(mFixedBitrate, mFixedFrameRate, 
                                   "CONSTANT_MODE_UPDATE: " + reason);
    }
    
    /**
     * Get the fixed bitrate setting
     */
    public int getFixedBitrate() {
        return mFixedBitrate;
    }
    
    /**
     * Get the fixed frame rate setting
     */
    public int getFixedFrameRate() {
        return mFixedFrameRate;
    }
    
    /**
     * Check if this conditioner is in true constant mode (no forced adjustments)
     */
    public boolean isInConstantMode() {
        return mStats.totalAdjustments == 0;
    }
}