package com.checkmate.android.util;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Utility class for safe method invocations to prevent null pointer exceptions
 */
public class SafeUtils {
    private static final String TAG = "SafeUtils";
    
    /**
     * Safely set text on a TextView
     */
    public static void setText(@Nullable TextView textView, @Nullable String text) {
        if (textView != null) {
            textView.setText(text != null ? text : "");
        }
    }
    
    /**
     * Safely set text on a TextView with resource ID
     */
    public static void setText(@Nullable TextView textView, int resId) {
        if (textView != null) {
            try {
                textView.setText(resId);
            } catch (Exception e) {
                AppLogger.e(TAG, "Error setting text resource", e);
            }
        }
    }
    
    /**
     * Safely set visibility on a View
     */
    public static void setVisibility(@Nullable View view, int visibility) {
        if (view != null) {
            view.setVisibility(visibility);
        }
    }
    
    /**
     * Safely set image resource on an ImageView
     */
    public static void setImageResource(@Nullable ImageView imageView, int resId) {
        if (imageView != null) {
            try {
                imageView.setImageResource(resId);
            } catch (Exception e) {
                AppLogger.e(TAG, "Error setting image resource", e);
            }
        }
    }
    
    /**
     * Safely run on UI thread
     */
    public static void runOnUiThread(@Nullable Activity activity, @NonNull Runnable runnable) {
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
            activity.runOnUiThread(runnable);
        } else {
            // Fallback to main handler
            new Handler(Looper.getMainLooper()).post(runnable);
        }
    }
    
    /**
     * Safely post to handler
     */
    public static void postDelayed(@Nullable Handler handler, @NonNull Runnable runnable, long delayMillis) {
        if (handler != null) {
            handler.postDelayed(runnable, delayMillis);
        } else {
            // Fallback to main handler
            new Handler(Looper.getMainLooper()).postDelayed(runnable, delayMillis);
        }
    }
    
    /**
     * Safely get string from context
     */
    @NonNull
    public static String getString(@Nullable Context context, int resId, @NonNull String defaultValue) {
        if (context != null) {
            try {
                return context.getString(resId);
            } catch (Exception e) {
                AppLogger.e(TAG, "Error getting string resource", e);
            }
        }
        return defaultValue;
    }
    
    /**
     * Safely check if activity is valid for operations
     */
    public static boolean isActivityValid(@Nullable Activity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }
    
    /**
     * Safely show/hide view with null check
     */
    public static void show(@Nullable View view) {
        setVisibility(view, View.VISIBLE);
    }
    
    public static void hide(@Nullable View view) {
        setVisibility(view, View.GONE);
    }
    
    public static void invisible(@Nullable View view) {
        setVisibility(view, View.INVISIBLE);
    }
    
    /**
     * Safely enable/disable view
     */
    public static void setEnabled(@Nullable View view, boolean enabled) {
        if (view != null) {
            view.setEnabled(enabled);
        }
    }
    
    /**
     * Safely set click listener
     */
    public static void setOnClickListener(@Nullable View view, @Nullable View.OnClickListener listener) {
        if (view != null) {
            view.setOnClickListener(listener);
        }
    }
    
    /**
     * Safe string comparison
     */
    public static boolean equals(@Nullable String str1, @Nullable String str2) {
        if (str1 == null && str2 == null) return true;
        if (str1 == null || str2 == null) return false;
        return str1.equals(str2);
    }
    
    /**
     * Check if string is empty with null safety
     */
    public static boolean isEmpty(@Nullable String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * Get non-null string
     */
    @NonNull
    public static String nonNull(@Nullable String str) {
        return str != null ? str : "";
    }
    
    /**
     * Get non-null string with default
     */
    @NonNull
    public static String nonNull(@Nullable String str, @NonNull String defaultValue) {
        return str != null ? str : defaultValue;
    }
}