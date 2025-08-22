package com.checkmate.android.util;

import android.util.Log;
import android.view.Choreographer;

/**
 * Performance monitoring utility for ViewPager2 transitions
 */
public class PerformanceMonitor {
    private static final String TAG = "PerformanceMonitor";
    private long lastFrameTime = 0;
    private int frameCount = 0;
    
    public void startMonitoring() {
        Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                if (lastFrameTime > 0) {
                    long frameDuration = frameTimeNanos - lastFrameTime;
                    float fps = 1000000000f / frameDuration;
                    
                    frameCount++;
                    if (frameCount % 60 == 0) { // Log every 60 frames
                        Log.d(TAG, "FPS: " + String.format("%.1f", fps));
                    }
                    
                    if (fps < 50) { // Warn if dropping below 50 FPS
                        Log.w(TAG, "Frame drop detected: " + String.format("%.1f", fps) + " FPS");
                    }
                }
                
                lastFrameTime = frameTimeNanos;
                Choreographer.getInstance().postFrameCallback(this);
            }
        });
    }
    
    public void stopMonitoring() {
        lastFrameTime = 0;
        frameCount = 0;
    }
} 