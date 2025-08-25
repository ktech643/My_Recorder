package com.checkmate.android.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * EnhancedCameraUI - Advanced UI system for ultra-smooth camera switching
 * 
 * This class provides professional-grade UI enhancements for camera switching,
 * including beautiful animations, progress indicators, and seamless transitions
 * that make switching feel instant and smooth.
 * 
 * Features:
 * - Sub-500ms visual feedback
 * - Professional loading animations  
 * - Smooth spinner transitions
 * - Real-time progress indication
 * - Beautiful fade effects
 * - Bounce animations for completion
 * - Emergency UI recovery
 */
public class EnhancedCameraUI {
    
    private static final String TAG = "EnhancedCameraUI";
    
    // Animation timings (optimized for speed and smoothness)
    private static final long ULTRA_FAST_ANIMATION = 150;
    private static final long SMOOTH_ANIMATION = 200;
    private static final long BOUNCE_ANIMATION = 300;
    private static final long SPINNER_ROTATION_TIME = 100;
    
    // UI state management
    private static EnhancedCameraUI instance;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // UI Components
    private FrameLayout overlayView;
    private ProgressBar progressSpinner;
    private TextView statusText;
    private ImageView cameraIcon;
    private FrameLayout transitionContainer;
    
    // Animation states
    private boolean isAnimating = false;
    private AnimatorSet currentAnimationSet;
    
    public static EnhancedCameraUI getInstance() {
        if (instance == null) {
            instance = new EnhancedCameraUI();
        }
        return instance;
    }
    
    private EnhancedCameraUI() {
        Log.d(TAG, "🎬 EnhancedCameraUI initialized for professional transitions");
    }
    
    /**
     * Show ultra-fast camera switch feedback with beautiful animations
     */
    public void showCameraSwitchFeedback(Context context, String cameraType, ViewGroup parentView) {
        if (context == null || parentView == null) return;
        
        try {
            Log.d(TAG, "🚀 Starting ultra-fast UI feedback for: " + cameraType);
            
            // Cancel any existing animations
            cancelCurrentAnimations();
            
            // Create or update UI components
            createTransitionUI(context, parentView);
            
            // Update content for current camera type
            updateUIContent(cameraType);
            
            // Start beautiful animation sequence
            startSwitchAnimationSequence();
            
            // Auto-hide after optimal duration
            scheduleAutoHide();
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing camera switch feedback", e);
            showFallbackToast(context, cameraType);
        }
    }
    
    /**
     * Create professional transition UI overlay
     */
    private void createTransitionUI(Context context, ViewGroup parentView) {
        try {
            // Remove existing overlay if present
            if (overlayView != null && overlayView.getParent() != null) {
                ((ViewGroup) overlayView.getParent()).removeView(overlayView);
            }
            
            // Create main overlay container
            overlayView = new FrameLayout(context);
            overlayView.setBackgroundColor(Color.parseColor("#80000000")); // Semi-transparent
            overlayView.setAlpha(0f);
            overlayView.setVisibility(View.VISIBLE);
            
            // Create transition container
            transitionContainer = new FrameLayout(context);
            
            // Create beautiful gradient background
            GradientDrawable gradient = new GradientDrawable();
            gradient.setColors(new int[]{
                Color.parseColor("#FF2196F3"), // Beautiful blue
                Color.parseColor("#FF1976D2")  // Darker blue
            });
            gradient.setGradientType(GradientDrawable.LINEAR_GRADIENT);
            gradient.setCornerRadius(24f);
            transitionContainer.setBackground(gradient);
            
            // Set container properties
            FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                dpToPx(context, 200), dpToPx(context, 80)
            );
            containerParams.gravity = Gravity.CENTER;
            transitionContainer.setLayoutParams(containerParams);
            transitionContainer.setElevation(dpToPx(context, 8));
            
            // Create inner content layout
            LinearLayout contentLayout = new LinearLayout(context);
            contentLayout.setOrientation(LinearLayout.HORIZONTAL);
            contentLayout.setGravity(Gravity.CENTER);
            contentLayout.setPadding(dpToPx(context, 16), dpToPx(context, 12), 
                                   dpToPx(context, 16), dpToPx(context, 12));
            
            // Create camera icon
            cameraIcon = new ImageView(context);
            cameraIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                dpToPx(context, 24), dpToPx(context, 24)
            );
            iconParams.setMargins(0, 0, dpToPx(context, 12), 0);
            cameraIcon.setLayoutParams(iconParams);
            cameraIcon.setColorFilter(Color.WHITE);
            
            // Create progress spinner
            progressSpinner = new ProgressBar(context);
            progressSpinner.setIndeterminate(true);
            LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                dpToPx(context, 20), dpToPx(context, 20)
            );
            spinnerParams.setMargins(0, 0, dpToPx(context, 12), 0);
            progressSpinner.setLayoutParams(spinnerParams);
            
            // Create status text
            statusText = new TextView(context);
            statusText.setTextColor(Color.WHITE);
            statusText.setTextSize(14f);
            statusText.setGravity(Gravity.CENTER_VERTICAL);
            
            // Assemble UI
            contentLayout.addView(cameraIcon);
            contentLayout.addView(progressSpinner);
            contentLayout.addView(statusText);
            
            transitionContainer.addView(contentLayout);
            ((FrameLayout) overlayView).addView(transitionContainer);
            
            // Add to parent view
            FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            );
            parentView.addView(overlayView, overlayParams);
            
            Log.d(TAG, "✅ Professional transition UI created successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating transition UI", e);
        }
    }
    
    /**
     * Update UI content for specific camera type
     */
    private void updateUIContent(String cameraType) {
        try {
            if (statusText != null) {
                String message = "🔄 Switching to " + cameraType;
                statusText.setText(message);
            }
            
            // Set appropriate icon based on camera type
            if (cameraIcon != null) {
                // You can set different icons for different camera types
                // For now, using a default camera icon representation
                cameraIcon.setBackgroundColor(Color.WHITE);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI content", e);
        }
    }
    
    /**
     * Start beautiful animation sequence
     */
    private void startSwitchAnimationSequence() {
        try {
            if (overlayView == null || transitionContainer == null) return;
            
            isAnimating = true;
            
            // Create animation set
            currentAnimationSet = new AnimatorSet();
            
            // Phase 1: Fade in overlay
            ObjectAnimator overlayFadeIn = ObjectAnimator.ofFloat(overlayView, "alpha", 0f, 1f);
            overlayFadeIn.setDuration(ULTRA_FAST_ANIMATION);
            overlayFadeIn.setInterpolator(new AccelerateDecelerateInterpolator());
            
            // Phase 2: Scale in container with bounce
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(transitionContainer, "scaleX", 0.3f, 1.1f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(transitionContainer, "scaleY", 0.3f, 1.1f, 1f);
            scaleX.setDuration(BOUNCE_ANIMATION);
            scaleY.setDuration(BOUNCE_ANIMATION);
            scaleX.setInterpolator(new AnticipateOvershootInterpolator());
            scaleY.setInterpolator(new AnticipateOvershootInterpolator());
            
            // Phase 3: Rotate spinner
            if (progressSpinner != null) {
                ObjectAnimator spinnerRotation = ObjectAnimator.ofFloat(progressSpinner, "rotation", 0f, 360f);
                spinnerRotation.setDuration(SPINNER_ROTATION_TIME);
                spinnerRotation.setRepeatCount(2); // 3 total rotations
                spinnerRotation.setInterpolator(new AccelerateDecelerateInterpolator());
                currentAnimationSet.play(spinnerRotation).after(ULTRA_FAST_ANIMATION);
            }
            
            // Phase 4: Text fade in
            if (statusText != null) {
                ObjectAnimator textFade = ObjectAnimator.ofFloat(statusText, "alpha", 0f, 1f);
                textFade.setDuration(SMOOTH_ANIMATION);
                textFade.setInterpolator(new AccelerateDecelerateInterpolator());
                currentAnimationSet.play(textFade).after(ULTRA_FAST_ANIMATION);
            }
            
            // Play animations
            currentAnimationSet.play(overlayFadeIn);
            currentAnimationSet.play(scaleX).after(overlayFadeIn);
            currentAnimationSet.play(scaleY).after(overlayFadeIn);
            
            currentAnimationSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    Log.d(TAG, "✨ Switch animation sequence completed");
                }
            });
            
            currentAnimationSet.start();
            
            Log.d(TAG, "🎬 Beautiful animation sequence started");
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting animation sequence", e);
            isAnimating = false;
        }
    }
    
    /**
     * Show completion feedback with success animation
     */
    public void showCompletionFeedback(Context context, String cameraType, boolean success) {
        try {
            if (overlayView == null || !isAnimating) return;
            
            Log.d(TAG, "🎊 Showing completion feedback: " + (success ? "SUCCESS" : "FAILED"));
            
            // Update UI for completion
            if (statusText != null) {
                String message = success ? "✅ " + cameraType + " Ready!" : "❌ Switch Failed";
                statusText.setText(message);
            }
            
            // Change background color based on result
            if (transitionContainer != null) {
                GradientDrawable completionGradient = new GradientDrawable();
                if (success) {
                    completionGradient.setColors(new int[]{
                        Color.parseColor("#FF4CAF50"), // Success green
                        Color.parseColor("#FF388E3C")
                    });
                } else {
                    completionGradient.setColors(new int[]{
                        Color.parseColor("#FFF44336"), // Error red
                        Color.parseColor("#FFD32F2F")
                    });
                }
                completionGradient.setGradientType(GradientDrawable.LINEAR_GRADIENT);
                completionGradient.setCornerRadius(24f);
                transitionContainer.setBackground(completionGradient);
            }
            
            // Hide spinner
            if (progressSpinner != null) {
                progressSpinner.setVisibility(View.GONE);
            }
            
            // Bounce animation for completion
            if (transitionContainer != null) {
                ObjectAnimator bounceX = ObjectAnimator.ofFloat(transitionContainer, "scaleX", 1f, 1.2f, 1f);
                ObjectAnimator bounceY = ObjectAnimator.ofFloat(transitionContainer, "scaleY", 1f, 1.2f, 1f);
                bounceX.setDuration(SMOOTH_ANIMATION);
                bounceY.setDuration(SMOOTH_ANIMATION);
                bounceX.setInterpolator(new BounceInterpolator());
                bounceY.setInterpolator(new BounceInterpolator());
                
                AnimatorSet bounceSet = new AnimatorSet();
                bounceSet.playTogether(bounceX, bounceY);
                bounceSet.start();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing completion feedback", e);
        }
    }
    
    /**
     * Hide UI with smooth fade out
     */
    public void hideTransitionUI() {
        try {
            if (overlayView == null || !isAnimating) return;
            
            Log.d(TAG, "🎭 Hiding transition UI smoothly");
            
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(overlayView, "alpha", 1f, 0f);
            fadeOut.setDuration(SMOOTH_ANIMATION);
            fadeOut.setInterpolator(new AccelerateDecelerateInterpolator());
            
            fadeOut.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    try {
                        if (overlayView != null && overlayView.getParent() != null) {
                            ((ViewGroup) overlayView.getParent()).removeView(overlayView);
                        }
                        overlayView = null;
                        transitionContainer = null;
                        progressSpinner = null;
                        statusText = null;
                        cameraIcon = null;
                        isAnimating = false;
                        
                        Log.d(TAG, "✅ Transition UI cleaned up successfully");
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error cleaning up UI", e);
                    }
                }
            });
            
            fadeOut.start();
            
        } catch (Exception e) {
            Log.e(TAG, "Error hiding transition UI", e);
        }
    }
    
    /**
     * Schedule auto-hide after optimal duration
     */
    private void scheduleAutoHide() {
        mainHandler.postDelayed(() -> {
            if (isAnimating) {
                hideTransitionUI();
            }
        }, 800); // Hide after 800ms for optimal UX
    }
    
    /**
     * Cancel current animations
     */
    private void cancelCurrentAnimations() {
        try {
            if (currentAnimationSet != null && currentAnimationSet.isRunning()) {
                currentAnimationSet.cancel();
            }
            isAnimating = false;
        } catch (Exception e) {
            Log.e(TAG, "Error canceling animations", e);
        }
    }
    
    /**
     * Fallback toast for emergency cases
     */
    private void showFallbackToast(Context context, String cameraType) {
        try {
            String message = "🔄 Switching to " + cameraType + "...";
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing fallback toast", e);
        }
    }
    
    /**
     * Utility: Convert dp to pixels
     */
    private int dpToPx(Context context, int dp) {
        try {
            float density = context.getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        } catch (Exception e) {
            return dp; // Fallback
        }
    }
    
    /**
     * Quick camera switch indicator (for rapid successive switches)
     */
    public void showQuickSwitchIndicator(Context context, String cameraType, ViewGroup parentView) {
        try {
            Log.d(TAG, "⚡ Quick switch indicator for: " + cameraType);
            
            // Create minimal quick indicator
            TextView quickIndicator = new TextView(context);
            quickIndicator.setText("🔄 " + cameraType);
            quickIndicator.setTextColor(Color.WHITE);
            quickIndicator.setTextSize(12f);
            quickIndicator.setBackgroundColor(Color.parseColor("#CC2196F3"));
            quickIndicator.setPadding(dpToPx(context, 12), dpToPx(context, 6), 
                                    dpToPx(context, 12), dpToPx(context, 6));
            quickIndicator.setAlpha(0f);
            
            // Position at top of screen
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            params.topMargin = dpToPx(context, 50);
            
            parentView.addView(quickIndicator, params);
            
            // Quick fade in/out animation
            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(quickIndicator, "alpha", 0f, 1f);
            fadeIn.setDuration(100);
            
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(quickIndicator, "alpha", 1f, 0f);
            fadeOut.setDuration(100);
            fadeOut.setStartDelay(400);
            
            fadeOut.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    try {
                        parentView.removeView(quickIndicator);
                    } catch (Exception e) {
                        Log.e(TAG, "Error removing quick indicator", e);
                    }
                }
            });
            
            fadeIn.start();
            fadeOut.start();
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing quick switch indicator", e);
        }
    }
    
    /**
     * Emergency cleanup
     */
    public void emergencyCleanup() {
        try {
            Log.w(TAG, "🚨 Emergency UI cleanup initiated");
            
            cancelCurrentAnimations();
            
            if (overlayView != null && overlayView.getParent() != null) {
                ((ViewGroup) overlayView.getParent()).removeView(overlayView);
            }
            
            overlayView = null;
            transitionContainer = null;
            progressSpinner = null;
            statusText = null;
            cameraIcon = null;
            isAnimating = false;
            
            Log.w(TAG, "✅ Emergency cleanup completed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during emergency cleanup", e);
        }
    }
}