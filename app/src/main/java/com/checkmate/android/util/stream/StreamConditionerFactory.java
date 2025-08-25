package com.checkmate.android.util.stream;

import android.util.Log;

/**
 * StreamConditionerFactory - Creates appropriate stream conditioners
 * 
 * Factory class that creates different types of stream conditioners based on the selected mode.
 * Each conditioner implements a different adaptive streaming strategy:
 * 
 * - LOGARITHMIC_DESCEND: Quickly reduces quality when network degrades
 * - LADDER_ASCEND: Gradually improves quality when network is stable
 * - HYBRID: Combines both approaches for optimal balance
 * - CONSTANT: No adaptation, uses fixed bitrate/FPS
 */
public class StreamConditionerFactory {
    
    private static final String TAG = "StreamConditionerFactory";
    
    /**
     * Create a stream conditioner based on the specified mode
     * 
     * @param mode The conditioner mode to create
     * @return StreamConditioner instance for the specified mode
     */
    public static StreamConditioner create(StreamConditioner.ConditionerMode mode) {
        Log.d(TAG, "Creating StreamConditioner for mode: " + mode);
        
        switch (mode) {
            case LOGARITHMIC_DESCEND:
                return new LogarithmicDescendConditioner();
                
            case LADDER_ASCEND:
                return new LadderAscendConditioner();
                
            case HYBRID:
                return new HybridConditioner();
                
            case CONSTANT:
            default:
                return new ConstantConditioner();
        }
    }
    
    /**
     * Create a stream conditioner based on integer mode value
     * 
     * @param modeValue Integer value representing the mode (0-3)
     * @return StreamConditioner instance for the specified mode
     */
    public static StreamConditioner create(int modeValue) {
        StreamConditioner.ConditionerMode mode = StreamConditioner.ConditionerMode.fromValue(modeValue);
        return create(mode);
    }
    
    /**
     * Get description for a conditioner mode
     * 
     * @param mode The conditioner mode
     * @return Human-readable description of the mode
     */
    public static String getModeDescription(StreamConditioner.ConditionerMode mode) {
        switch (mode) {
            case LOGARITHMIC_DESCEND:
                return "Logarithmic Descend - Aggressive quality reduction for poor networks";
                
            case LADDER_ASCEND:
                return "Ladder Ascend - Conservative quality improvement for stable networks";
                
            case HYBRID:
                return "Hybrid - Balanced approach combining descend and ascend strategies";
                
            case CONSTANT:
                return "Constant - Fixed bitrate and FPS, no adaptation";
                
            default:
                return "Unknown mode";
        }
    }
    
    /**
     * Get all available conditioner modes
     * 
     * @return Array of all available conditioner modes
     */
    public static StreamConditioner.ConditionerMode[] getAvailableModes() {
        return StreamConditioner.ConditionerMode.values();
    }
    
    /**
     * Validate if a mode value is supported
     * 
     * @param modeValue Integer mode value to validate
     * @return true if the mode is supported, false otherwise
     */
    public static boolean isModeSupported(int modeValue) {
        return modeValue >= 0 && modeValue <= 3;
    }
}