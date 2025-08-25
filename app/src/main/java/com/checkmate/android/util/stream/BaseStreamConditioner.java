package com.checkmate.android.util.stream;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;

import com.wmspanel.libstream.Streamer;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * BaseStreamConditioner - Base implementation for all stream conditioners
 * 
 * Provides common functionality for:
 * - Network monitoring
 * - Quality adjustment application
 * - Statistics tracking
 * - Thread management
 * - Callback handling
 */
public abstract class BaseStreamConditioner implements StreamConditioner {
    
    protected static final String TAG = "BaseStreamConditioner";
    
    // Quality bounds
    protected static final int MIN_BITRATE = 128 * 1024;      // 128 Kbps
    protected static final int MAX_BITRATE = 8 * 1024 * 1024; // 8 Mbps
    protected static final int MIN_FRAMERATE = 10;            // 10 FPS
    protected static final int MAX_FRAMERATE = 60;            // 60 FPS
    
    // Timing constants
    protected static final long MONITORING_INTERVAL_MS = 2000;    // 2 seconds
    protected static final long ADJUSTMENT_COOLDOWN_MS = 5000;    // 5 seconds between adjustments
    protected static final long NETWORK_TIMEOUT_MS = 10000;      // 10 seconds network timeout
    
    // State tracking
    protected final AtomicBoolean mIsActive = new AtomicBoolean(false);
    protected final AtomicInteger mCurrentBitrate = new AtomicInteger(0);
    protected final AtomicInteger mCurrentFrameRate = new AtomicInteger(30);
    protected final AtomicLong mLastAdjustmentTime = new AtomicLong(0);
    protected final AtomicLong mStartTime = new AtomicLong(0);
    
    // Components
    protected Streamer mStreamer;
    protected Set<Integer> mConnectionIds;
    protected ConditionerCallback mCallback;
    protected ConditionerMode mMode;
    
    // Threading
    protected HandlerThread mMonitoringThread;
    protected Handler mMonitoringHandler;
    
    // Network metrics
    protected volatile float mPacketLoss = 0.0f;
    protected volatile long mRtt = 50; // Default 50ms
    protected volatile long mBandwidth = 0;
    protected volatile NetworkCondition mCurrentCondition = NetworkCondition.GOOD;
    
    // Statistics
    protected final ConditionerStats mStats = new ConditionerStats();
    
    public BaseStreamConditioner(ConditionerMode mode) {
        mMode = mode;
        Log.d(TAG, "Created " + getClass().getSimpleName() + " for mode: " + mode);
    }
    
    @Override
    public void start(Streamer streamer, int initialBitrate, Set<Integer> connectionIds) {
        if (mIsActive.get()) {
            Log.w(TAG, "Conditioner already active");
            return;
        }
        
        Log.d(TAG, "Starting " + getClass().getSimpleName() + 
                   " - Initial bitrate: " + (initialBitrate / 1024) + " Kbps");
        
        mStreamer = streamer;
        mConnectionIds = connectionIds;
        mCurrentBitrate.set(initialBitrate);
        mStartTime.set(SystemClock.elapsedRealtime());
        
        // Initialize monitoring thread
        initializeMonitoringThread();
        
        // Start mode-specific initialization
        onConditionerStarted(initialBitrate);
        
        mIsActive.set(true);
        
        // Begin monitoring
        startMonitoring();
        
        Log.d(TAG, "StreamConditioner started successfully");
    }
    
    @Override
    public void stop() {
        if (!mIsActive.get()) {
            return;
        }
        
        Log.d(TAG, "Stopping " + getClass().getSimpleName());
        
        mIsActive.set(false);
        
        // Stop monitoring
        stopMonitoring();
        
        // Cleanup mode-specific resources
        onConditionerStopped();
        
        // Cleanup threading
        cleanupMonitoringThread();
        
        // Update statistics
        mStats.uptime = SystemClock.elapsedRealtime() - mStartTime.get();
        
        Log.d(TAG, "StreamConditioner stopped - Uptime: " + (mStats.uptime / 1000) + "s, " +
                   "Total adjustments: " + mStats.totalAdjustments);
    }
    
    @Override
    public void updateNetworkMetrics(float packetLoss, long rtt, long bandwidth) {
        mPacketLoss = Math.max(0.0f, Math.min(1.0f, packetLoss));
        mRtt = Math.max(0, rtt);
        mBandwidth = Math.max(0, bandwidth);
        
        // Update network condition
        NetworkCondition newCondition = calculateNetworkCondition();
        if (newCondition != mCurrentCondition) {
            mCurrentCondition = newCondition;
            Log.d(TAG, "Network condition changed to: " + newCondition +
                       " (Loss: " + String.format("%.1f", packetLoss * 100) + "%, " +
                       "RTT: " + rtt + "ms, Bandwidth: " + (bandwidth / 1024) + " Kbps)");
            
            if (mCallback != null) {
                mCallback.onNetworkConditionChanged(newCondition);
            }
        }
        
        // Trigger adaptation decision
        if (mIsActive.get()) {
            onNetworkMetricsUpdated(packetLoss, rtt, bandwidth);
        }
    }
    
    @Override
    public void forceQualityAdjustment(int bitrate, int frameRate, String reason) {
        Log.d(TAG, "Force quality adjustment: " + (bitrate / 1024) + " Kbps, " + 
                   frameRate + " FPS - " + reason);
        
        QualityAdjustment adjustment = new QualityAdjustment(
            clampBitrate(bitrate),
            clampFrameRate(frameRate),
            "FORCED: " + reason,
            mCurrentCondition
        );
        
        applyQualityAdjustment(adjustment);
    }
    
    @Override
    public void setCallback(ConditionerCallback callback) {
        mCallback = callback;
    }
    
    @Override
    public ConditionerMode getMode() {
        return mMode;
    }
    
    @Override
    public NetworkCondition getCurrentNetworkCondition() {
        return mCurrentCondition;
    }
    
    @Override
    public boolean isActive() {
        return mIsActive.get();
    }
    
    @Override
    public int getCurrentBitrate() {
        return mCurrentBitrate.get();
    }
    
    @Override
    public int getCurrentFrameRate() {
        return mCurrentFrameRate.get();
    }
    
    @Override
    public ConditionerStats getStats() {
        // Update uptime
        if (mIsActive.get()) {
            mStats.uptime = SystemClock.elapsedRealtime() - mStartTime.get();
        }
        return mStats;
    }
    
    /**
     * Initialize monitoring thread
     */
    private void initializeMonitoringThread() {
        mMonitoringThread = new HandlerThread("StreamConditioner-" + mMode);
        mMonitoringThread.start();
        mMonitoringHandler = new Handler(mMonitoringThread.getLooper());
    }
    
    /**
     * Cleanup monitoring thread
     */
    private void cleanupMonitoringThread() {
        if (mMonitoringHandler != null) {
            mMonitoringHandler.removeCallbacksAndMessages(null);
            mMonitoringHandler = null;
        }
        
        if (mMonitoringThread != null) {
            mMonitoringThread.quitSafely();
            mMonitoringThread = null;
        }
    }
    
    /**
     * Start periodic monitoring
     */
    private void startMonitoring() {
        if (mMonitoringHandler != null) {
            mMonitoringHandler.post(mMonitoringRunnable);
        }
    }
    
    /**
     * Stop periodic monitoring
     */
    private void stopMonitoring() {
        if (mMonitoringHandler != null) {
            mMonitoringHandler.removeCallbacks(mMonitoringRunnable);
        }
    }
    
    /**
     * Monitoring runnable for periodic checks
     */
    private final Runnable mMonitoringRunnable = new Runnable() {
        @Override
        public void run() {
            if (mIsActive.get()) {
                try {
                    // Perform monitoring check
                    performMonitoringCheck();
                    
                    // Schedule next check
                    if (mMonitoringHandler != null) {
                        mMonitoringHandler.postDelayed(this, MONITORING_INTERVAL_MS);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error during monitoring check", e);
                    if (mCallback != null) {
                        mCallback.onConditionerError("Monitoring error: " + e.getMessage());
                    }
                }
            }
        }
    };
    
    /**
     * Calculate network condition based on current metrics
     */
    protected NetworkCondition calculateNetworkCondition() {
        if (mPacketLoss < 0.05f && mRtt < 50) {
            return NetworkCondition.EXCELLENT;
        } else if (mPacketLoss < 0.10f && mRtt < 100) {
            return NetworkCondition.GOOD;
        } else if (mPacketLoss < 0.20f && mRtt < 200) {
            return NetworkCondition.FAIR;
        } else if (mPacketLoss < 0.40f && mRtt < 500) {
            return NetworkCondition.POOR;
        } else {
            return NetworkCondition.CRITICAL;
        }
    }
    
    /**
     * Apply quality adjustment to the streamer
     */
    protected void applyQualityAdjustment(QualityAdjustment adjustment) {
        if (!canAdjustQuality()) {
            Log.d(TAG, "Quality adjustment skipped - cooldown active");
            return;
        }
        
        try {
            // Update current values
            int oldBitrate = mCurrentBitrate.getAndSet(adjustment.newBitrate);
            int oldFrameRate = mCurrentFrameRate.getAndSet(adjustment.newFrameRate);
            
            // Apply to streamer
            if (mStreamer != null) {
                mStreamer.setBitrate(adjustment.newBitrate);
                // Note: Frame rate would typically be set during encoder initialization
                // For runtime changes, this might require encoder restart in some implementations
            }
            
            // Update statistics
            updateAdjustmentStats(oldBitrate, adjustment.newBitrate, oldFrameRate, adjustment.newFrameRate);
            
            // Update last adjustment time
            mLastAdjustmentTime.set(SystemClock.elapsedRealtime());
            
            Log.d(TAG, "Quality adjusted: " + (oldBitrate / 1024) + " -> " + 
                       (adjustment.newBitrate / 1024) + " Kbps, " +
                       oldFrameRate + " -> " + adjustment.newFrameRate + " FPS - " + 
                       adjustment.reason);
            
            // Notify callback
            if (mCallback != null) {
                mCallback.onQualityAdjusted(adjustment);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to apply quality adjustment", e);
            if (mCallback != null) {
                mCallback.onConditionerError("Failed to apply adjustment: " + e.getMessage());
            }
        }
    }
    
    /**
     * Check if quality can be adjusted (respects cooldown)
     */
    protected boolean canAdjustQuality() {
        long timeSinceLastAdjustment = SystemClock.elapsedRealtime() - mLastAdjustmentTime.get();
        return timeSinceLastAdjustment >= ADJUSTMENT_COOLDOWN_MS;
    }
    
    /**
     * Clamp bitrate to valid range
     */
    protected int clampBitrate(int bitrate) {
        return Math.max(MIN_BITRATE, Math.min(MAX_BITRATE, bitrate));
    }
    
    /**
     * Clamp frame rate to valid range
     */
    protected int clampFrameRate(int frameRate) {
        return Math.max(MIN_FRAMERATE, Math.min(MAX_FRAMERATE, frameRate));
    }
    
    /**
     * Update adjustment statistics
     */
    private void updateAdjustmentStats(int oldBitrate, int newBitrate, int oldFrameRate, int newFrameRate) {
        mStats.totalAdjustments++;
        
        if (newBitrate > oldBitrate) {
            mStats.bitrateIncreases++;
        } else if (newBitrate < oldBitrate) {
            mStats.bitrateDecreases++;
        }
        
        if (newFrameRate != oldFrameRate) {
            mStats.frameRateAdjustments++;
        }
    }
    
    // Abstract methods for mode-specific implementations
    
    /**
     * Called when conditioner starts - mode-specific initialization
     */
    protected abstract void onConditionerStarted(int initialBitrate);
    
    /**
     * Called when conditioner stops - mode-specific cleanup
     */
    protected abstract void onConditionerStopped();
    
    /**
     * Called when network metrics are updated - triggers adaptation logic
     */
    protected abstract void onNetworkMetricsUpdated(float packetLoss, long rtt, long bandwidth);
    
    /**
     * Called periodically for monitoring checks
     */
    protected abstract void performMonitoringCheck();
}