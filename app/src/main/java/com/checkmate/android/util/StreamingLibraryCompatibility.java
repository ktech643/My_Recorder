package com.checkmate.android.util;

import android.util.Log;
import com.wmspanel.libstream.Streamer;
import com.wmspanel.libstream.StreamerSurface;
import java.lang.reflect.Method;

/**
 * StreamingLibraryCompatibility - Safe wrapper for streaming library methods
 * 
 * This class provides safe access to streaming library methods with reflection
 * to ensure compatibility even if methods don't exist in the current version.
 */
public class StreamingLibraryCompatibility {
    private static final String TAG = "StreamCompatibility";
    
    /**
     * Safely set bitrate on Streamer class
     */
    public static boolean safelySetBitrateOnStreamer(Streamer streamer, int bitrate) {
        if (streamer == null) {
            return false;
        }
        
        try {
            // Try direct method call first
            Method setBitrateMethod = streamer.getClass().getMethod("setBitrate", int.class);
            setBitrateMethod.invoke(streamer, bitrate);
            Log.d(TAG, "✅ Bitrate set successfully on Streamer: " + bitrate);
            return true;
            
        } catch (NoSuchMethodException e) {
            Log.w(TAG, "setBitrate method not available on Streamer, trying alternatives");
            return tryAlternativeBitrateMethodOnStreamer(streamer, bitrate);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to set bitrate on Streamer: " + bitrate, e);
            return false;
        }
    }

    /**
     * Safely set frame rate on Streamer class  
     */
    public static boolean safelySetFramerateOnStreamer(Streamer streamer, int fps) {
        if (streamer == null) {
            return false;
        }
        
        try {
            // Try direct method call first
            Method setFramerateMethod = streamer.getClass().getMethod("setFramerate", int.class);
            setFramerateMethod.invoke(streamer, fps);
            Log.d(TAG, "✅ Frame rate set successfully on Streamer: " + fps);
            return true;
            
        } catch (NoSuchMethodException e) {
            Log.w(TAG, "setFramerate method not available on Streamer, trying alternatives");
            return tryAlternativeFramerateMethodOnStreamer(streamer, fps);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to set frame rate on Streamer: " + fps, e);
            return false;
        }
    }

    /**
     * Safely set bitrate on streamer/recorder
     */
    public static boolean saflySetBitrate(StreamerSurface streamer, int bitrate) {
        if (streamer == null) {
            return false;
        }
        
        try {
            // Try direct method call first
            Method setBitrateMethod = streamer.getClass().getMethod("setBitrate", int.class);
            setBitrateMethod.invoke(streamer, bitrate);
            Log.d(TAG, "✅ Bitrate set successfully: " + bitrate);
            return true;
            
        } catch (NoSuchMethodException e) {
            Log.w(TAG, "setBitrate method not available, trying alternatives");
            return tryAlternativeBitrateMethod(streamer, bitrate);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to set bitrate: " + bitrate, e);
            return false;
        }
    }
    
    /**
     * Safely set frame rate on streamer/recorder
     */
    public static boolean safelySetFramerate(StreamerSurface streamer, int fps) {
        if (streamer == null) {
            return false;
        }
        
        try {
            // Try direct method call first
            Method setFramerateMethod = streamer.getClass().getMethod("setFramerate", int.class);
            setFramerateMethod.invoke(streamer, fps);
            Log.d(TAG, "✅ Frame rate set successfully: " + fps);
            return true;
            
        } catch (NoSuchMethodException e) {
            Log.w(TAG, "setFramerate method not available, trying alternatives");
            return tryAlternativeFramerateMethod(streamer, fps);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to set frame rate: " + fps, e);
            return false;
        }
    }
    
    /**
     * Try alternative method names for bitrate on Streamer class
     */
    private static boolean tryAlternativeBitrateMethodOnStreamer(Streamer streamer, int bitrate) {
        String[] alternativeNames = {
            "setBitRate", "setVideoBitrate", "setEncodingBitrate", 
            "updateBitrate", "changeBitrate", "setTargetBitrate"
        };
        
        for (String methodName : alternativeNames) {
            try {
                Method method = streamer.getClass().getMethod(methodName, int.class);
                method.invoke(streamer, bitrate);
                Log.d(TAG, "✅ Bitrate set on Streamer using alternative method: " + methodName);
                return true;
            } catch (Exception e) {
                // Continue trying other methods
            }
        }
        
        Log.w(TAG, "⚠️ No bitrate method available on Streamer - feature not supported");
        return false; // Not necessarily an error, just not supported
    }

    /**
     * Try alternative method names for frame rate on Streamer class
     */
    private static boolean tryAlternativeFramerateMethodOnStreamer(Streamer streamer, int fps) {
        String[] alternativeNames = {
            "setFrameRate", "setVideoFramerate", "setEncodingFramerate",
            "updateFramerate", "changeFramerate", "setTargetFramerate", "setFps"
        };
        
        for (String methodName : alternativeNames) {
            try {
                Method method = streamer.getClass().getMethod(methodName, int.class);
                method.invoke(streamer, fps);
                Log.d(TAG, "✅ Frame rate set on Streamer using alternative method: " + methodName);
                return true;
            } catch (Exception e) {
                // Continue trying other methods
            }
        }
        
        Log.w(TAG, "⚠️ No framerate method available on Streamer - feature not supported");
        return false; // Not necessarily an error, just not supported
    }

    /**
     * Try alternative method names for bitrate
     */
    private static boolean tryAlternativeBitrateMethod(StreamerSurface streamer, int bitrate) {
        String[] alternativeNames = {
            "setBitRate", "setVideoBitrate", "setEncodingBitrate", 
            "updateBitrate", "changeBitrate", "setTargetBitrate"
        };
        
        for (String methodName : alternativeNames) {
            try {
                Method method = streamer.getClass().getMethod(methodName, int.class);
                method.invoke(streamer, bitrate);
                Log.d(TAG, "✅ Bitrate set using alternative method: " + methodName);
                return true;
            } catch (Exception e) {
                // Continue trying other methods
            }
        }
        
        Log.w(TAG, "⚠️ No bitrate method available - feature not supported");
        return false; // Not necessarily an error, just not supported
    }
    
    /**
     * Try alternative method names for frame rate
     */
    private static boolean tryAlternativeFramerateMethod(StreamerSurface streamer, int fps) {
        String[] alternativeNames = {
            "setFrameRate", "setVideoFramerate", "setEncodingFramerate",
            "updateFramerate", "changeFramerate", "setTargetFramerate", "setFps"
        };
        
        for (String methodName : alternativeNames) {
            try {
                Method method = streamer.getClass().getMethod(methodName, int.class);
                method.invoke(streamer, fps);
                Log.d(TAG, "✅ Frame rate set using alternative method: " + methodName);
                return true;
            } catch (Exception e) {
                // Continue trying other methods
            }
        }
        
        Log.w(TAG, "⚠️ No framerate method available - feature not supported");
        return false; // Not necessarily an error, just not supported
    }
    
    /**
     * Check if streaming library supports dynamic bitrate changes
     */
    public static boolean supportsDynamicBitrate(StreamerSurface streamer) {
        if (streamer == null) {
            return false;
        }
        
        try {
            Method method = streamer.getClass().getMethod("setBitrate", int.class);
            return true;
        } catch (NoSuchMethodException e) {
            return tryAlternativeBitrateMethod(streamer, 0); // Test with dummy value
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if streaming library supports dynamic frame rate changes
     */
    public static boolean supportsDynamicFramerate(StreamerSurface streamer) {
        if (streamer == null) {
            return false;
        }
        
        try {
            Method method = streamer.getClass().getMethod("setFramerate", int.class);
            return true;
        } catch (NoSuchMethodException e) {
            return tryAlternativeFramerateMethod(streamer, 0); // Test with dummy value
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get available methods from streaming library for debugging
     */
    public static String getAvailableMethods(StreamerSurface streamer) {
        if (streamer == null) {
            return "Streamer is null";
        }
        
        StringBuilder methods = new StringBuilder();
        Method[] allMethods = streamer.getClass().getMethods();
        
        for (Method method : allMethods) {
            if (method.getName().toLowerCase().contains("bitrate") || 
                method.getName().toLowerCase().contains("framerate") ||
                method.getName().toLowerCase().contains("fps")) {
                methods.append(method.getName()).append("(");
                
                Class<?>[] params = method.getParameterTypes();
                for (int i = 0; i < params.length; i++) {
                    if (i > 0) methods.append(", ");
                    methods.append(params[i].getSimpleName());
                }
                methods.append("), ");
            }
        }
        
        return methods.toString();
    }
}