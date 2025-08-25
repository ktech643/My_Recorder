package com.checkmate.android.util;

import android.app.Activity;
import java.lang.ref.WeakReference;

/**
 * Optimized version of MainActivity singleton to prevent memory leaks
 */
public class OptimizedMainActivity {
    private static WeakReference<MainActivity> instanceRef;
    
    /**
     * Get the MainActivity instance using WeakReference to prevent memory leaks
     * @return MainActivity instance or null if not available
     */
    public static MainActivity getInstance() {
        if (instanceRef != null) {
            return instanceRef.get();
        }
        return null;
    }
    
    /**
     * Set the MainActivity instance
     * @param activity The MainActivity instance
     */
    public static void setInstance(MainActivity activity) {
        if (activity != null) {
            instanceRef = new WeakReference<>(activity);
        } else {
            instanceRef = null;
        }
    }
    
    /**
     * Clear the instance reference
     */
    public static void clearInstance() {
        instanceRef = null;
    }
    
    /**
     * Check if instance is available and not finishing
     * @return true if instance is valid and usable
     */
    public static boolean isInstanceValid() {
        MainActivity activity = getInstance();
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }
}