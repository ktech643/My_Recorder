package com.checkmate.android.service.SharedEGL;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Time Overlay Renderer for displaying time information during blank frames.
 * This ensures visual feedback is maintained during service transitions and configuration updates.
 */
public class TimeOverlayRenderer {
    private static final String TAG = "TimeOverlayRenderer";
    
    // Overlay configuration
    private static final int DEFAULT_TEXT_SIZE_DP = 32;
    private static final int DEFAULT_PADDING_DP = 16;
    private static final int DEFAULT_MARGIN_DP = 20;
    private static final float DEFAULT_BACKGROUND_ALPHA = 0.7f;
    
    // Update interval
    private static final long UPDATE_INTERVAL_MS = 1000; // 1 second
    
    // OpenGL texture management
    private int mOverlayTextureId = -1;
    private int mOverlayWidth = 0;
    private int mOverlayHeight = 0;
    
    // Drawing components
    private Paint mTextPaint;
    private Paint mBackgroundPaint;
    private Rect mTextBounds;
    private Bitmap mOverlayBitmap;
    private Canvas mOverlayCanvas;
    
    // Configuration
    private int mTextSize;
    private int mPadding;
    private int mMarginX;
    private int mMarginY;
    private float mDensity;
    
    // State management
    private final AtomicBoolean mIsActive = new AtomicBoolean(false);
    private final AtomicBoolean mIsInitialized = new AtomicBoolean(false);
    
    // Time formatting
    private SimpleDateFormat mTimeFormat;
    private TimeZone mTimeZone;
    private String mLastTimeText = "";
    
    // Update handler
    private Handler mUpdateHandler;
    private Runnable mUpdateRunnable;
    
    // Position configuration
    public enum Position {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER
    }
    
    private Position mPosition = Position.TOP_RIGHT;
    
    /**
     * Initialize the time overlay renderer
     */
    public void initialize(float density, TimeZone timeZone) {
        if (mIsInitialized.compareAndSet(false, true)) {
            mDensity = density;
            mTimeZone = timeZone != null ? timeZone : TimeZone.getDefault();
            
            // Calculate sizes based on density
            mTextSize = (int) (DEFAULT_TEXT_SIZE_DP * density);
            mPadding = (int) (DEFAULT_PADDING_DP * density);
            mMarginX = (int) (DEFAULT_MARGIN_DP * density);
            mMarginY = (int) (DEFAULT_MARGIN_DP * density);
            
            initializePaints();
            initializeTimeFormat();
            initializeUpdateHandler();
            
            Log.d(TAG, "TimeOverlayRenderer initialized");
        }
    }
    
    /**
     * Initialize paint objects for text and background
     */
    private void initializePaints() {
        // Text paint
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setShadowLayer(2f, 1f, 1f, Color.BLACK);
        
        // Background paint
        mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBackgroundPaint.setColor(Color.BLACK);
        mBackgroundPaint.setAlpha((int) (255 * DEFAULT_BACKGROUND_ALPHA));
        
        // Text bounds rect
        mTextBounds = new Rect();
    }
    
    /**
     * Initialize time format
     */
    private void initializeTimeFormat() {
        mTimeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        mTimeFormat.setTimeZone(mTimeZone);
    }
    
    /**
     * Initialize the update handler and runnable
     */
    private void initializeUpdateHandler() {
        mUpdateHandler = new Handler(Looper.getMainLooper());
        mUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (mIsActive.get()) {
                    updateTimeText();
                    mUpdateHandler.postDelayed(this, UPDATE_INTERVAL_MS);
                }
            }
        };
    }
    
    /**
     * Start the time overlay
     */
    public void start() {
        if (!mIsInitialized.get()) {
            Log.w(TAG, "TimeOverlayRenderer not initialized");
            return;
        }
        
        if (mIsActive.compareAndSet(false, true)) {
            Log.d(TAG, "Starting time overlay");
            updateTimeText();
            mUpdateHandler.post(mUpdateRunnable);
        }
    }
    
    /**
     * Stop the time overlay
     */
    public void stop() {
        if (mIsActive.compareAndSet(true, false)) {
            Log.d(TAG, "Stopping time overlay");
            mUpdateHandler.removeCallbacks(mUpdateRunnable);
        }
    }
    
    /**
     * Set the overlay position
     */
    public void setPosition(Position position) {
        mPosition = position;
    }
    
    /**
     * Set custom time format
     */
    public void setTimeFormat(String format) {
        if (format != null && !format.isEmpty()) {
            mTimeFormat = new SimpleDateFormat(format, Locale.getDefault());
            mTimeFormat.setTimeZone(mTimeZone);
        }
    }
    
    /**
     * Update the time text and overlay bitmap
     */
    private void updateTimeText() {
        String currentTime = mTimeFormat.format(new Date());
        
        if (!currentTime.equals(mLastTimeText)) {
            mLastTimeText = currentTime;
            createOverlayBitmap(currentTime);
        }
    }
    
    /**
     * Create the overlay bitmap with the current time
     */
    private void createOverlayBitmap(String timeText) {
        // Measure text
        mTextPaint.getTextBounds(timeText, 0, timeText.length(), mTextBounds);
        
        int textWidth = mTextBounds.width();
        int textHeight = mTextBounds.height();
        
        // Calculate bitmap size
        int bitmapWidth = textWidth + (mPadding * 2);
        int bitmapHeight = textHeight + (mPadding * 2);
        
        // Create or recreate bitmap if size changed
        if (mOverlayBitmap == null || mOverlayBitmap.getWidth() != bitmapWidth || mOverlayBitmap.getHeight() != bitmapHeight) {
            if (mOverlayBitmap != null && !mOverlayBitmap.isRecycled()) {
                mOverlayBitmap.recycle();
            }
            
            mOverlayBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
            mOverlayCanvas = new Canvas(mOverlayBitmap);
            mOverlayWidth = bitmapWidth;
            mOverlayHeight = bitmapHeight;
        }
        
        // Clear the canvas
        mOverlayCanvas.drawColor(Color.TRANSPARENT);
        
        // Draw background
        mOverlayCanvas.drawRoundRect(0, 0, bitmapWidth, bitmapHeight, 8f, 8f, mBackgroundPaint);
        
        // Draw text
        float textX = mPadding;
        float textY = mPadding + textHeight;
        mOverlayCanvas.drawText(timeText, textX, textY, mTextPaint);
        
        // Update OpenGL texture
        updateGLTexture();
    }
    
    /**
     * Update the OpenGL texture with the overlay bitmap
     */
    private void updateGLTexture() {
        if (mOverlayBitmap == null || mOverlayBitmap.isRecycled()) {
            return;
        }
        
        // Generate texture if needed
        if (mOverlayTextureId == -1) {
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            mOverlayTextureId = textures[0];
            
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOverlayTextureId);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOverlayTextureId);
        }
        
        // Upload bitmap to texture
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mOverlayBitmap, 0);
        
        // Check for GL errors
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "OpenGL error updating overlay texture: " + error);
        }
    }
    
    /**
     * Render the time overlay to the current OpenGL context
     */
    public void render(int surfaceWidth, int surfaceHeight) {
        if (!mIsActive.get() || mOverlayTextureId == -1 || mOverlayBitmap == null) {
            return;
        }
        
        // Calculate overlay position
        float[] position = calculateOverlayPosition(surfaceWidth, surfaceHeight);
        float overlayX = position[0];
        float overlayY = position[1];
        
        // Enable blending for transparency
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        
        // Bind overlay texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOverlayTextureId);
        
        // Here you would typically use your shader program to render the texture
        // as a quad at the calculated position. This would integrate with your
        // existing OpenGL rendering pipeline.
        
        // For now, we'll just log the render call
        Log.v(TAG, "Rendering time overlay at (" + overlayX + ", " + overlayY + ")");
        
        // Disable blending
        GLES20.glDisable(GLES20.GL_BLEND);
    }
    
    /**
     * Calculate the overlay position based on the configured position
     */
    private float[] calculateOverlayPosition(int surfaceWidth, int surfaceHeight) {
        float x, y;
        
        switch (mPosition) {
            case TOP_LEFT:
                x = mMarginX;
                y = mMarginY;
                break;
            case TOP_RIGHT:
                x = surfaceWidth - mOverlayWidth - mMarginX;
                y = mMarginY;
                break;
            case BOTTOM_LEFT:
                x = mMarginX;
                y = surfaceHeight - mOverlayHeight - mMarginY;
                break;
            case BOTTOM_RIGHT:
                x = surfaceWidth - mOverlayWidth - mMarginX;
                y = surfaceHeight - mOverlayHeight - mMarginY;
                break;
            case CENTER:
                x = (surfaceWidth - mOverlayWidth) / 2f;
                y = (surfaceHeight - mOverlayHeight) / 2f;
                break;
            default:
                x = mMarginX;
                y = mMarginY;
                break;
        }
        
        return new float[]{x, y};
    }
    
    /**
     * Get the overlay texture ID
     */
    public int getOverlayTextureId() {
        return mOverlayTextureId;
    }
    
    /**
     * Get the overlay dimensions
     */
    public int[] getOverlayDimensions() {
        return new int[]{mOverlayWidth, mOverlayHeight};
    }
    
    /**
     * Check if the overlay is active
     */
    public boolean isActive() {
        return mIsActive.get();
    }
    
    /**
     * Destroy the overlay renderer and clean up resources
     */
    public void destroy() {
        Log.d(TAG, "Destroying TimeOverlayRenderer");
        
        stop();
        
        // Clean up OpenGL resources
        if (mOverlayTextureId != -1) {
            GLES20.glDeleteTextures(1, new int[]{mOverlayTextureId}, 0);
            mOverlayTextureId = -1;
        }
        
        // Clean up bitmap
        if (mOverlayBitmap != null && !mOverlayBitmap.isRecycled()) {
            mOverlayBitmap.recycle();
            mOverlayBitmap = null;
        }
        
        mOverlayCanvas = null;
        mTextPaint = null;
        mBackgroundPaint = null;
        mTextBounds = null;
        mUpdateHandler = null;
        mUpdateRunnable = null;
        
        mIsInitialized.set(false);
    }
}