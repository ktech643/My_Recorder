package com.checkmate.android.util.stream;

import com.wmspanel.libstream.Streamer;
import java.util.Set;

/**
 * StreamConditioner - Adaptive bitrate and FPS control interface
 * 
 * This interface defines the contract for different adaptive streaming modes:
 * - LOGARITHMIC_DESCEND: Aggressive quality reduction when needed
 * - LADDER_ASCEND: Conservative quality improvement
 * - HYBRID: Balanced approach combining both strategies  
 * - CONSTANT: Fixed bitrate/FPS mode (no adaptation)
 */
public interface StreamConditioner {
    
    /**
     * Conditioner modes available for adaptive streaming
     */
    enum ConditionerMode {
        LOGARITHMIC_DESCEND(0),  // Mode 0: Aggressive descend
        LADDER_ASCEND(1),        // Mode 1: Conservative ascend  
        HYBRID(2),               // Mode 2: Hybrid approach
        CONSTANT(3);             // Mode 3: Constant bitrate/FPS
        
        private final int value;
        
        ConditionerMode(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        public static ConditionerMode fromValue(int value) {
            for (ConditionerMode mode : values()) {
                if (mode.getValue() == value) {
                    return mode;
                }
            }
            return CONSTANT; // Default fallback
        }
    }
    
    /**
     * Network condition indicators
     */
    enum NetworkCondition {
        EXCELLENT,   // >95% packet delivery, <50ms RTT
        GOOD,        // >90% packet delivery, <100ms RTT  
        FAIR,        // >80% packet delivery, <200ms RTT
        POOR,        // >60% packet delivery, <500ms RTT
        CRITICAL     // <60% packet delivery, >500ms RTT
    }
    
    /**
     * Quality adjustment decision
     */
    class QualityAdjustment {
        public int newBitrate;
        public int newFrameRate;
        public String reason;
        public NetworkCondition condition;
        public long timestamp;
        
        public QualityAdjustment(int bitrate, int frameRate, String reason, NetworkCondition condition) {
            this.newBitrate = bitrate;
            this.newFrameRate = frameRate;
            this.reason = reason;
            this.condition = condition;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * Callback interface for quality adjustments
     */
    interface ConditionerCallback {
        void onQualityAdjusted(QualityAdjustment adjustment);
        void onNetworkConditionChanged(NetworkCondition condition);
        void onConditionerError(String error);
    }
    
    /**
     * Start the stream conditioner
     * 
     * @param streamer The streamer instance to control
     * @param initialBitrate Initial bitrate in bps
     * @param connectionIds Set of active connection IDs
     */
    void start(Streamer streamer, int initialBitrate, Set<Integer> connectionIds);
    
    /**
     * Stop the stream conditioner
     */
    void stop();
    
    /**
     * Update network metrics for decision making
     * 
     * @param packetLoss Packet loss percentage (0.0-1.0)
     * @param rtt Round trip time in milliseconds
     * @param bandwidth Available bandwidth in bps
     */
    void updateNetworkMetrics(float packetLoss, long rtt, long bandwidth);
    
    /**
     * Force a quality adjustment
     * 
     * @param bitrate Target bitrate in bps
     * @param frameRate Target frame rate
     * @param reason Reason for adjustment
     */
    void forceQualityAdjustment(int bitrate, int frameRate, String reason);
    
    /**
     * Set callback for quality adjustments
     */
    void setCallback(ConditionerCallback callback);
    
    /**
     * Get current conditioner mode
     */
    ConditionerMode getMode();
    
    /**
     * Get current network condition
     */
    NetworkCondition getCurrentNetworkCondition();
    
    /**
     * Check if conditioner is currently active
     */
    boolean isActive();
    
    /**
     * Get current target bitrate
     */
    int getCurrentBitrate();
    
    /**
     * Get current target frame rate  
     */
    int getCurrentFrameRate();
    
    /**
     * Get adjustment statistics
     */
    ConditionerStats getStats();
    
    /**
     * Statistics for conditioner performance
     */
    class ConditionerStats {
        public int totalAdjustments;
        public int bitrateIncreases;
        public int bitrateDecreases;
        public int frameRateAdjustments;
        public long averageResponseTime;
        public NetworkCondition averageCondition;
        public long uptime;
        
        public ConditionerStats() {
            totalAdjustments = 0;
            bitrateIncreases = 0;
            bitrateDecreases = 0;
            frameRateAdjustments = 0;
            averageResponseTime = 0;
            averageCondition = NetworkCondition.GOOD;
            uptime = 0;
        }
    }
}