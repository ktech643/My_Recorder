package com.checkmate.android.service.SharedEGL;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.checkmate.android.util.libgraph.EglCoreNew;
import com.checkmate.android.util.libgraph.WindowSurfaceNew;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages blank frames with time overlay during service transitions.
 * Ensures the stream remains active even when switching between services.
 */
public class BlankFrameManager {
    private static final String TAG = "BlankFrameManager";
    private static final int FRAME_RATE_MS = 33; // ~30 FPS
    private static final int OVERLAY_COLOR = Color.BLACK;
    private static final int TEXT_COLOR = Color.WHITE;
    private static final int TEXT_SIZE_DP = 48;
    private static final int PADDING_DP = 20;
    
    private final Context mContext;
    private final AtomicBoolean mIsRunning = new AtomicBoolean(false);
    private HandlerThread mRenderThread;
    private Handler mRenderHandler;
    
    // OpenGL components
    private EglCoreNew mEglCore;
    private WindowSurfaceNew mWindowSurface;
    private int mTextureId = -1;
    
    // Drawing components
    private Bitmap mOverlayBitmap;
    private Canvas mOverlayCanvas;
    private Paint mTextPaint;
    private Paint mBackgroundPaint;
    private SimpleDateFormat mTimeFormat;
    
    // Surface info
    private SurfaceTexture mSurfaceTexture;
    private int mWidth;
    private int mHeight;
    
    // Render runnable
    private final Runnable mRenderRunnable = new Runnable() {
        @Override
        public void run() {
            if (mIsRunning.get()) {
                renderFrame();
                mRenderHandler.postDelayed(this, FRAME_RATE_MS);
            }
        }
    };
    
    public BlankFrameManager(Context context) {
        mContext = context.getApplicationContext();
        initDrawingComponents();
    }
    
    private void initDrawingComponents() {
        // Initialize time format
        mTimeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
        
        // Initialize text paint
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(TEXT_COLOR);
        mTextPaint.setTextSize(dpToPx(TEXT_SIZE_DP));
        mTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        
        // Initialize background paint
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(OVERLAY_COLOR);
        mBackgroundPaint.setStyle(Paint.Style.FILL);
    }
    
    /**
     * Start rendering blank frames with time overlay.
     */
    public void startBlankFrames(SurfaceTexture surfaceTexture, int width, int height) {
        if (mIsRunning.getAndSet(true)) {
            Log.w(TAG, "Blank frames already running");
            return;
        }
        
        Log.d(TAG, "Starting blank frames: " + width + "x" + height);
        
        mSurfaceTexture = surfaceTexture;
        mWidth = width;
        mHeight = height;
        
        // Create render thread
        mRenderThread = new HandlerThread("BlankFrameRenderer");
        mRenderThread.start();
        mRenderHandler = new Handler(mRenderThread.getLooper());
        
        // Initialize OpenGL on render thread
        mRenderHandler.post(() -> {
            initializeOpenGL();
            mRenderHandler.post(mRenderRunnable);
        });
    }
    
    /**
     * Stop rendering blank frames.
     */
    public void stopBlankFrames() {
        if (!mIsRunning.getAndSet(false)) {
            Log.w(TAG, "Blank frames not running");
            return;
        }
        
        Log.d(TAG, "Stopping blank frames");
        
        if (mRenderHandler != null) {
            mRenderHandler.removeCallbacks(mRenderRunnable);
            mRenderHandler.post(this::releaseOpenGL);
        }
        
        if (mRenderThread != null) {
            mRenderThread.quitSafely();
            try {
                mRenderThread.join(1000);
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while stopping render thread", e);
            }
            mRenderThread = null;
        }
        
        mRenderHandler = null;
    }
    
    private void initializeOpenGL() {
        try {
            // Get shared EGL context
            EGLLifecycleManager eglManager = EGLLifecycleManager.getInstance();
            if (!eglManager.isInitialized()) {
                Log.e(TAG, "EGL not initialized");
                return;
            }
            
            // Create EGL core with shared context
            mEglCore = new EglCoreNew(eglManager.getSharedContext(), 
                                      EglCoreNew.FLAG_RECORDABLE);
            
            // Create window surface
            mWindowSurface = new WindowSurfaceNew(mEglCore, mSurfaceTexture);
            mWindowSurface.makeCurrent();
            
            // Create texture for overlay
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            mTextureId = textures[0];
            
            // Set up texture parameters
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, 
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, 
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, 
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, 
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            
            // Create overlay bitmap
            mOverlayBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
            mOverlayCanvas = new Canvas(mOverlayBitmap);
            
            Log.d(TAG, "OpenGL initialized for blank frames");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize OpenGL", e);
        }
    }
    
    private void releaseOpenGL() {
        try {
            if (mTextureId != -1) {
                int[] textures = new int[] { mTextureId };
                GLES20.glDeleteTextures(1, textures, 0);
                mTextureId = -1;
            }
            
            if (mWindowSurface != null) {
                mWindowSurface.release();
                mWindowSurface = null;
            }
            
            if (mEglCore != null) {
                mEglCore.release();
                mEglCore = null;
            }
            
            if (mOverlayBitmap != null) {
                mOverlayBitmap.recycle();
                mOverlayBitmap = null;
            }
            
            mOverlayCanvas = null;
            
            Log.d(TAG, "OpenGL released");
            
        } catch (Exception e) {
            Log.e(TAG, "Error releasing OpenGL", e);
        }
    }
    
    private void renderFrame() {
        if (mWindowSurface == null || mOverlayBitmap == null) {
            return;
        }
        
        try {
            // Make surface current
            mWindowSurface.makeCurrent();
            
            // Clear with black
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            
            // Draw overlay with time
            drawTimeOverlay();
            
            // Upload bitmap to texture
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mOverlayBitmap, 0);
            
            // Draw texture (simplified - in production you'd use a proper shader)
            drawTexture();
            
            // Swap buffers
            mWindowSurface.swapBuffers();
            
        } catch (Exception e) {
            Log.e(TAG, "Error rendering frame", e);
        }
    }
    
    private void drawTimeOverlay() {
        // Clear canvas
        mOverlayCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        
        // Draw black background
        mOverlayCanvas.drawRect(0, 0, mWidth, mHeight, mBackgroundPaint);
        
        // Get current time
        String timeText = mTimeFormat.format(new Date());
        
        // Draw loading message
        String loadingText = "Switching source...";
        
        // Calculate text positions
        Rect timeBounds = new Rect();
        mTextPaint.getTextBounds(timeText, 0, timeText.length(), timeBounds);
        
        Rect loadingBounds = new Rect();
        mTextPaint.getTextBounds(loadingText, 0, loadingText.length(), loadingBounds);
        
        float centerX = mWidth / 2f;
        float centerY = mHeight / 2f;
        
        // Draw loading text
        mOverlayCanvas.drawText(loadingText, centerX, 
            centerY - timeBounds.height() / 2f - dpToPx(10), mTextPaint);
        
        // Draw time text
        mOverlayCanvas.drawText(timeText, centerX, 
            centerY + timeBounds.height() / 2f + dpToPx(10), mTextPaint);
        
        // Draw status indicator
        drawStatusIndicator();
    }
    
    private void drawStatusIndicator() {
        // Draw a pulsing circle to indicate activity
        long time = System.currentTimeMillis();
        float pulse = (float) Math.sin(time / 200.0) * 0.5f + 0.5f;
        
        Paint indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        indicatorPaint.setColor(Color.RED);
        indicatorPaint.setAlpha((int) (255 * pulse));
        
        float radius = dpToPx(10);
        float x = mWidth - dpToPx(30);
        float y = dpToPx(30);
        
        mOverlayCanvas.drawCircle(x, y, radius, indicatorPaint);
    }
    
    private void drawTexture() {
        // Simplified texture drawing
        // In production, you would use a proper shader program
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        
        // Draw fullscreen quad with texture
        // (Implementation depends on your shader setup)
    }
    
    private int dpToPx(int dp) {
        float density = mContext.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    
    /**
     * Check if blank frames are currently running.
     */
    public boolean isRunning() {
        return mIsRunning.get();
    }
    
    /**
     * Release all resources.
     */
    public void release() {
        stopBlankFrames();
    }
}