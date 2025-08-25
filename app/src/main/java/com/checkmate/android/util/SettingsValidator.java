package com.checkmate.android.util;

import android.util.Log;
import java.util.HashMap;
import java.util.Map;

/**
 * Validates and constrains settings to ensure they are within acceptable ranges
 * Prevents invalid configurations that could cause crashes or poor performance
 */
public class SettingsValidator {
    private static final String TAG = "SettingsValidator";
    
    // Bitrate constraints (in bps)
    private static final int MIN_VIDEO_BITRATE = 100_000;      // 100 kbps
    private static final int MAX_VIDEO_BITRATE = 50_000_000;   // 50 Mbps
    private static final int MIN_AUDIO_BITRATE = 16_000;       // 16 kbps
    private static final int MAX_AUDIO_BITRATE = 512_000;      // 512 kbps
    
    // Resolution constraints
    private static final int MIN_RESOLUTION_WIDTH = 176;
    private static final int MAX_RESOLUTION_WIDTH = 3840;      // 4K
    private static final int MIN_RESOLUTION_HEIGHT = 144;
    private static final int MAX_RESOLUTION_HEIGHT = 2160;     // 4K
    
    // Frame rate constraints
    private static final int MIN_FPS = 10;
    private static final int MAX_FPS = 120;
    
    // File split time constraints (in minutes)
    private static final int MIN_SPLIT_TIME = 1;
    private static final int MAX_SPLIT_TIME = 60;
    
    // Sample rate constraints
    private static final int[] VALID_SAMPLE_RATES = {8000, 16000, 22050, 44100, 48000};
    
    private static SettingsValidator instance;
    private final Map<String, ValidationRule> validationRules = new HashMap<>();
    
    public interface ValidationRule {
        ValidationResult validate(Object value);
    }
    
    public static class ValidationResult {
        public final boolean isValid;
        public final Object correctedValue;
        public final String message;
        
        public ValidationResult(boolean isValid, Object correctedValue, String message) {
            this.isValid = isValid;
            this.correctedValue = correctedValue;
            this.message = message;
        }
    }
    
    private SettingsValidator() {
        initializeValidationRules();
    }
    
    public static synchronized SettingsValidator getInstance() {
        if (instance == null) {
            instance = new SettingsValidator();
        }
        return instance;
    }
    
    private void initializeValidationRules() {
        // Video bitrate validation
        validationRules.put(AppPreference.KEY.VIDEO_BITRATE, value -> {
            if (value instanceof Integer) {
                int bitrate = (Integer) value;
                if (bitrate < MIN_VIDEO_BITRATE) {
                    return new ValidationResult(false, MIN_VIDEO_BITRATE, 
                        "Video bitrate too low, adjusted to minimum");
                } else if (bitrate > MAX_VIDEO_BITRATE) {
                    return new ValidationResult(false, MAX_VIDEO_BITRATE, 
                        "Video bitrate too high, adjusted to maximum");
                }
                return new ValidationResult(true, bitrate, "Valid");
            }
            return new ValidationResult(false, 2_000_000, "Invalid type, using default");
        });
        
        // Audio bitrate validation
        validationRules.put(AppPreference.KEY.AUDIO_OPTION_BITRATE, value -> {
            if (value instanceof Integer) {
                int bitrate = (Integer) value;
                if (bitrate < MIN_AUDIO_BITRATE) {
                    return new ValidationResult(false, MIN_AUDIO_BITRATE, 
                        "Audio bitrate too low, adjusted to minimum");
                } else if (bitrate > MAX_AUDIO_BITRATE) {
                    return new ValidationResult(false, MAX_AUDIO_BITRATE, 
                        "Audio bitrate too high, adjusted to maximum");
                }
                return new ValidationResult(true, bitrate, "Valid");
            }
            return new ValidationResult(false, 128_000, "Invalid type, using default");
        });
        
        // Frame rate validation
        validationRules.put(AppPreference.KEY.VIDEO_FRAME, value -> {
            if (value instanceof Integer) {
                int fps = (Integer) value;
                if (fps < MIN_FPS) {
                    return new ValidationResult(false, MIN_FPS, 
                        "Frame rate too low, adjusted to minimum");
                } else if (fps > MAX_FPS) {
                    return new ValidationResult(false, MAX_FPS, 
                        "Frame rate too high, adjusted to maximum");
                }
                return new ValidationResult(true, fps, "Valid");
            }
            return new ValidationResult(false, 30, "Invalid type, using default");
        });
        
        // Split time validation
        validationRules.put(AppPreference.KEY.SPLIT_TIME, value -> {
            if (value instanceof Integer) {
                int minutes = (Integer) value;
                if (minutes < MIN_SPLIT_TIME) {
                    return new ValidationResult(false, MIN_SPLIT_TIME, 
                        "Split time too short, adjusted to minimum");
                } else if (minutes > MAX_SPLIT_TIME) {
                    return new ValidationResult(false, MAX_SPLIT_TIME, 
                        "Split time too long, adjusted to maximum");
                }
                return new ValidationResult(true, minutes, "Valid");
            }
            return new ValidationResult(false, 10, "Invalid type, using default");
        });
        
        // Sample rate validation
        validationRules.put(AppPreference.KEY.AUDIO_OPTION_SAMPLE_RATE, value -> {
            if (value instanceof Integer) {
                int sampleRate = (Integer) value;
                for (int validRate : VALID_SAMPLE_RATES) {
                    if (sampleRate == validRate) {
                        return new ValidationResult(true, sampleRate, "Valid");
                    }
                }
                // Find closest valid sample rate
                int closest = VALID_SAMPLE_RATES[0];
                int minDiff = Math.abs(sampleRate - closest);
                for (int validRate : VALID_SAMPLE_RATES) {
                    int diff = Math.abs(sampleRate - validRate);
                    if (diff < minDiff) {
                        minDiff = diff;
                        closest = validRate;
                    }
                }
                return new ValidationResult(false, closest, 
                    "Invalid sample rate, adjusted to closest valid rate");
            }
            return new ValidationResult(false, 44100, "Invalid type, using default");
        });
    }
    
    /**
     * Validate a setting value
     */
    public ValidationResult validate(String key, Object value) {
        ValidationRule rule = validationRules.get(key);
        if (rule != null) {
            ValidationResult result = rule.validate(value);
            if (!result.isValid) {
                Log.w(TAG, "Validation failed for " + key + ": " + result.message);
                if (CrashLogger.getInstance() != null) {
                    CrashLogger.getInstance().logWarning(TAG, 
                        "Setting validation: " + key + " - " + result.message);
                }
            }
            return result;
        }
        return new ValidationResult(true, value, "No validation rule");
    }
    
    /**
     * Validate resolution string (e.g., "1920x1080")
     */
    public ValidationResult validateResolution(String resolution) {
        try {
            String[] parts = resolution.split("x");
            if (parts.length != 2) {
                return new ValidationResult(false, "1920x1080", "Invalid format");
            }
            
            int width = Integer.parseInt(parts[0]);
            int height = Integer.parseInt(parts[1]);
            
            // Validate dimensions
            if (width < MIN_RESOLUTION_WIDTH || width > MAX_RESOLUTION_WIDTH ||
                height < MIN_RESOLUTION_HEIGHT || height > MAX_RESOLUTION_HEIGHT) {
                
                // Adjust to nearest valid resolution
                width = Math.max(MIN_RESOLUTION_WIDTH, Math.min(width, MAX_RESOLUTION_WIDTH));
                height = Math.max(MIN_RESOLUTION_HEIGHT, Math.min(height, MAX_RESOLUTION_HEIGHT));
                
                // Ensure 16:9 aspect ratio if possible
                if ((float)width / height > 1.9f) {
                    height = (int)(width / 1.77f); // 16:9 ratio
                } else if ((float)width / height < 1.5f) {
                    width = (int)(height * 1.77f); // 16:9 ratio
                }
                
                // Round to nearest even number for codec compatibility
                width = (width / 2) * 2;
                height = (height / 2) * 2;
                
                String corrected = width + "x" + height;
                return new ValidationResult(false, corrected, 
                    "Resolution adjusted for compatibility");
            }
            
            // Check if dimensions are even (required by many codecs)
            if (width % 2 != 0 || height % 2 != 0) {
                width = (width / 2) * 2;
                height = (height / 2) * 2;
                String corrected = width + "x" + height;
                return new ValidationResult(false, corrected, 
                    "Resolution adjusted to even dimensions");
            }
            
            return new ValidationResult(true, resolution, "Valid");
            
        } catch (Exception e) {
            Log.e(TAG, "Error validating resolution", e);
            return new ValidationResult(false, "1920x1080", "Parse error");
        }
    }
    
    /**
     * Check if settings combination is valid
     */
    public boolean isSettingsCombinationValid(int bitrate, String resolution, int fps) {
        try {
            // Parse resolution
            String[] parts = resolution.split("x");
            int width = Integer.parseInt(parts[0]);
            int height = Integer.parseInt(parts[1]);
            int pixels = width * height;
            
            // Calculate required bitrate for quality
            // Rule of thumb: 0.1 bits per pixel for acceptable quality
            int minRequiredBitrate = (int)(pixels * fps * 0.1);
            
            if (bitrate < minRequiredBitrate) {
                Log.w(TAG, String.format(
                    "Bitrate may be too low for %s@%dfps. Recommended minimum: %d bps",
                    resolution, fps, minRequiredBitrate));
                return false;
            }
            
            // Check if bitrate is too high (wasteful)
            int maxReasonableBitrate = (int)(pixels * fps * 0.5);
            if (bitrate > maxReasonableBitrate) {
                Log.w(TAG, String.format(
                    "Bitrate may be unnecessarily high for %s@%dfps. Recommended maximum: %d bps",
                    resolution, fps, maxReasonableBitrate));
            }
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error validating settings combination", e);
            return false;
        }
    }
    
    /**
     * Get recommended settings based on device capabilities
     */
    public Map<String, Object> getRecommendedSettings() {
        Map<String, Object> recommended = new HashMap<>();
        
        // Get available memory
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        
        // Recommend settings based on device capabilities
        if (maxMemory > 512 * 1024 * 1024) { // > 512MB heap
            // High-end device
            recommended.put(AppPreference.KEY.VIDEO_RESOLUTION, "1920x1080");
            recommended.put(AppPreference.KEY.VIDEO_BITRATE, 4_000_000);
            recommended.put(AppPreference.KEY.VIDEO_FRAME, 30);
        } else if (maxMemory > 256 * 1024 * 1024) { // > 256MB heap
            // Mid-range device
            recommended.put(AppPreference.KEY.VIDEO_RESOLUTION, "1280x720");
            recommended.put(AppPreference.KEY.VIDEO_BITRATE, 2_000_000);
            recommended.put(AppPreference.KEY.VIDEO_FRAME, 30);
        } else {
            // Low-end device
            recommended.put(AppPreference.KEY.VIDEO_RESOLUTION, "854x480");
            recommended.put(AppPreference.KEY.VIDEO_BITRATE, 1_000_000);
            recommended.put(AppPreference.KEY.VIDEO_FRAME, 25);
        }
        
        // Audio settings (consistent across devices)
        recommended.put(AppPreference.KEY.AUDIO_OPTION_BITRATE, 128_000);
        recommended.put(AppPreference.KEY.AUDIO_OPTION_SAMPLE_RATE, 44100);
        
        return recommended;
    }
}