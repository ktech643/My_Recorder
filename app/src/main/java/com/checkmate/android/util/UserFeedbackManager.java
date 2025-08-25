package com.checkmate.android.util;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.checkmate.android.R;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Provides user feedback for dynamic settings changes
 * Shows non-intrusive notifications when settings are applied
 */
public class UserFeedbackManager {
    private static final String TAG = "UserFeedbackManager";
    private static UserFeedbackManager instance;
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Queue<FeedbackItem> feedbackQueue = new LinkedList<>();
    private boolean isShowingFeedback = false;
    
    public enum FeedbackType {
        SUCCESS(Color.parseColor("#4CAF50"), "✓"),
        WARNING(Color.parseColor("#FF9800"), "⚠"),
        ERROR(Color.parseColor("#F44336"), "✗"),
        INFO(Color.parseColor("#2196F3"), "ⓘ");
        
        public final int color;
        public final String icon;
        
        FeedbackType(int color, String icon) {
            this.color = color;
            this.icon = icon;
        }
    }
    
    private static class FeedbackItem {
        final String message;
        final FeedbackType type;
        final long duration;
        final boolean vibrate;
        
        FeedbackItem(String message, FeedbackType type, long duration, boolean vibrate) {
            this.message = message;
            this.type = type;
            this.duration = duration;
            this.vibrate = vibrate;
        }
    }
    
    private UserFeedbackManager(Context context) {
        this.context = context.getApplicationContext();
    }
    
    public static synchronized UserFeedbackManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserFeedbackManager(context);
        }
        return instance;
    }
    
    /**
     * Show feedback for a settings change
     */
    public void showSettingChangeFeedback(String settingName, Object oldValue, Object newValue) {
        String message = String.format("%s changed: %s → %s", 
            settingName, 
            formatValue(oldValue), 
            formatValue(newValue));
        
        showFeedback(message, FeedbackType.SUCCESS, 2000, true);
    }
    
    /**
     * Show feedback with custom message
     */
    public void showFeedback(String message, FeedbackType type, long duration, boolean vibrate) {
        FeedbackItem item = new FeedbackItem(message, type, duration, vibrate);
        
        mainHandler.post(() -> {
            feedbackQueue.offer(item);
            if (!isShowingFeedback) {
                showNextFeedback();
            }
        });
    }
    
    /**
     * Show quick success feedback
     */
    public void showSuccess(String message) {
        showFeedback(message, FeedbackType.SUCCESS, 1500, false);
    }
    
    /**
     * Show warning feedback
     */
    public void showWarning(String message) {
        showFeedback(message, FeedbackType.WARNING, 2500, true);
    }
    
    /**
     * Show error feedback
     */
    public void showError(String message) {
        showFeedback(message, FeedbackType.ERROR, 3000, true);
    }
    
    /**
     * Show info feedback
     */
    public void showInfo(String message) {
        showFeedback(message, FeedbackType.INFO, 2000, false);
    }
    
    /**
     * Process feedback queue
     */
    private void showNextFeedback() {
        FeedbackItem item = feedbackQueue.poll();
        if (item == null) {
            isShowingFeedback = false;
            return;
        }
        
        isShowingFeedback = true;
        
        try {
            // Create custom toast
            Toast toast = createCustomToast(item);
            toast.show();
            
            // Vibrate if requested
            if (item.vibrate) {
                vibrate(item.type);
            }
            
            // Log feedback
            Log.d(TAG, String.format("[%s] %s", item.type.name(), item.message));
            
            // Schedule next feedback
            mainHandler.postDelayed(this::showNextFeedback, item.duration);
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing feedback", e);
            isShowingFeedback = false;
        }
    }
    
    /**
     * Create custom styled toast
     */
    private Toast createCustomToast(FeedbackItem item) {
        Toast toast = new Toast(context);
        
        // Create custom view
        TextView textView = new TextView(context);
        textView.setText(item.type.icon + " " + item.message);
        textView.setTextColor(Color.WHITE);
        textView.setBackgroundColor(item.type.color);
        textView.setPadding(32, 16, 32, 16);
        textView.setTextSize(14);
        textView.setGravity(Gravity.CENTER);
        
        // Add rounded corners
        textView.setBackgroundDrawable(createRoundedBackground(item.type.color));
        
        toast.setView(textView);
        toast.setDuration(item.duration > 2000 ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 100);
        
        return toast;
    }
    
    /**
     * Create rounded background (simplified version)
     */
    private android.graphics.drawable.Drawable createRoundedBackground(int color) {
        android.graphics.drawable.GradientDrawable drawable = 
            new android.graphics.drawable.GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(24);
        drawable.setAlpha(230); // Slight transparency
        return drawable;
    }
    
    /**
     * Vibrate based on feedback type
     */
    private void vibrate(FeedbackType type) {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    VibrationEffect effect;
                    switch (type) {
                        case SUCCESS:
                            effect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE);
                            break;
                        case WARNING:
                            effect = VibrationEffect.createWaveform(new long[]{0, 100, 50, 100}, -1);
                            break;
                        case ERROR:
                            effect = VibrationEffect.createWaveform(new long[]{0, 200, 100, 200}, -1);
                            break;
                        default:
                            effect = VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE);
                    }
                    vibrator.vibrate(effect);
                } else {
                    // Legacy vibration
                    vibrator.vibrate(type == FeedbackType.ERROR ? 400 : 100);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error vibrating", e);
        }
    }
    
    /**
     * Format value for display
     */
    private String formatValue(Object value) {
        if (value == null) return "null";
        
        if (value instanceof Integer) {
            int intValue = (Integer) value;
            // Format bitrates
            if (intValue > 1_000_000) {
                return String.format("%.1f Mbps", intValue / 1_000_000.0);
            } else if (intValue > 1000) {
                return String.format("%d kbps", intValue / 1000);
            }
        } else if (value instanceof Boolean) {
            return (Boolean) value ? "ON" : "OFF";
        }
        
        return value.toString();
    }
    
    /**
     * Show real-time performance feedback
     */
    public void showPerformanceFeedback(String metric, float value, float threshold) {
        if (value > threshold) {
            showWarning(String.format("%s: %.1f (above %.1f threshold)", metric, value, threshold));
        }
    }
    
    /**
     * Show batch settings change summary
     */
    public void showBatchChangesSummary(int changedCount, int failedCount) {
        if (failedCount > 0) {
            showWarning(String.format("%d settings applied, %d failed", changedCount, failedCount));
        } else {
            showSuccess(String.format("%d settings applied successfully", changedCount));
        }
    }
}