package com.checkmate.android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;

import com.checkmate.android.service.SharedEGL.SharedEglManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Transition overlay that provides time-stamped frames during service transitions
 * This prevents blank screens while maintaining active streams
 */
public class TransitionOverlay {
    private static final String TAG = "TransitionOverlay";
    private static TransitionOverlay sInstance;
    
    private final AtomicBoolean mIsActive = new AtomicBoolean(false);
    private SimpleDateFormat mTimeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
    private Paint mTimePaint;
    private Paint mStatusPaint;
    private Paint mBackgroundPaint;
    
    private String mStatusText = "Switching source...";
    private long mStartTime;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mFrameGenerator;
    
    private int mWidth = 1280;
    private int mHeight = 720;
    private Bitmap mOverlayBitmap;
    private Canvas mOverlayCanvas;
    
    private TransitionOverlay() {
        initializePaints();
    }
    
    /**
     * Show the transition overlay
     */
    public static void show(Context context) {
        if (sInstance == null) {
            sInstance = new TransitionOverlay();
        }
        sInstance.startOverlay(context);
    }
    
    /**
     * Hide the transition overlay
     */
    public static void hide() {
        if (sInstance != null) {
            sInstance.stopOverlay();
        }
    }
    
    /**
     * Update status text
     */
    public static void setStatus(String status) {
        if (sInstance != null) {
            sInstance.mStatusText = status;
        }
    }
    
    /**
     * Set overlay dimensions
     */
    public static void setDimensions(int width, int height) {
        if (sInstance != null) {
            sInstance.updateDimensions(width, height);
        }
    }
    
    private void startOverlay(Context context) {
        if (mIsActive.getAndSet(true)) {
            return;
        }
        
        mStartTime = System.currentTimeMillis();
        
        // Get current video dimensions from SharedEglManager
        SharedEglManager eglManager = SharedEglManager.getInstance();
        updateDimensions(eglManager.getSurfaceWidth(), eglManager.getSurfaceHeight());
        
        // Start generating overlay frames
        mFrameGenerator = new Runnable() {
            @Override
            public void run() {
                if (mIsActive.get()) {
                    generateOverlayFrame();
                    mHandler.postDelayed(this, 33); // ~30 FPS
                }
            }
        };
        mHandler.post(mFrameGenerator);
        
        Log.d(TAG, "Transition overlay started");
    }
    
    private void stopOverlay() {
        if (!mIsActive.getAndSet(false)) {
            return;
        }
        
        mHandler.removeCallbacks(mFrameGenerator);
        
        // Clean up bitmap
        if (mOverlayBitmap != null) {
            mOverlayBitmap.recycle();
            mOverlayBitmap = null;
            mOverlayCanvas = null;
        }
        
        Log.d(TAG, "Transition overlay stopped");
    }
    
    private void updateDimensions(int width, int height) {
        if (width == mWidth && height == mHeight && mOverlayBitmap != null) {
            return;
        }
        
        mWidth = width;
        mHeight = height;
        
        // Recreate bitmap with new dimensions
        if (mOverlayBitmap != null) {
            mOverlayBitmap.recycle();
        }
        
        try {
            mOverlayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mOverlayCanvas = new Canvas(mOverlayBitmap);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Failed to create overlay bitmap", e);
            mOverlayBitmap = null;
            mOverlayCanvas = null;
        }
    }
    
    private void initializePaints() {
        // Background paint (semi-transparent)
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(Color.argb(180, 0, 0, 0));
        mBackgroundPaint.setStyle(Paint.Style.FILL);
        
        // Time paint
        mTimePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTimePaint.setColor(Color.WHITE);
        mTimePaint.setTextSize(48);
        mTimePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        mTimePaint.setTextAlign(Paint.Align.CENTER);
        mTimePaint.setShadowLayer(4, 2, 2, Color.BLACK);
        
        // Status paint
        mStatusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mStatusPaint.setColor(Color.argb(220, 255, 255, 255));
        mStatusPaint.setTextSize(24);
        mStatusPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        mStatusPaint.setTextAlign(Paint.Align.CENTER);
        mStatusPaint.setShadowLayer(3, 1, 1, Color.BLACK);
    }
    
    private void generateOverlayFrame() {
        if (mOverlayCanvas == null || !mIsActive.get()) {
            return;
        }
        
        try {
            // Clear canvas
            mOverlayCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            
            // Draw semi-transparent background
            mOverlayCanvas.drawRect(0, 0, mWidth, mHeight, mBackgroundPaint);
            
            // Get current time
            String currentTime = mTimeFormat.format(new Date());
            
            // Calculate positions
            float centerX = mWidth / 2f;
            float centerY = mHeight / 2f;
            
            // Adjust text sizes based on resolution
            float scaleFactor = Math.min(mWidth / 1280f, mHeight / 720f);
            mTimePaint.setTextSize(48 * scaleFactor);
            mStatusPaint.setTextSize(24 * scaleFactor);
            
            // Draw time
            mOverlayCanvas.drawText(currentTime, centerX, centerY - 30 * scaleFactor, mTimePaint);
            
            // Draw status
            mOverlayCanvas.drawText(mStatusText, centerX, centerY + 50 * scaleFactor, mStatusPaint);
            
            // Draw transition duration
            long duration = System.currentTimeMillis() - mStartTime;
            String durationText = String.format(Locale.US, "Transition time: %.1fs", duration / 1000f);
            mOverlayCanvas.drawText(durationText, centerX, centerY + 100 * scaleFactor, mStatusPaint);
            
            // Send frame to SharedEglManager
            SharedEglManager eglManager = SharedEglManager.getInstance();
            if (eglManager != null && mOverlayBitmap != null) {
                eglManager.renderTransitionFrame(mOverlayBitmap);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating overlay frame", e);
        }
    }
    
    /**
     * Get the current overlay bitmap
     */
    public static Bitmap getCurrentFrame() {
        if (sInstance != null && sInstance.mOverlayBitmap != null) {
            return sInstance.mOverlayBitmap;
        }
        return null;
    }
    
    /**
     * Check if overlay is active
     */
    public static boolean isActive() {
        return sInstance != null && sInstance.mIsActive.get();
    }
}