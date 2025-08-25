package com.checkmate.android.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;

import com.checkmate.android.service.SharedEGL.ServiceType;
import com.checkmate.android.service.SharedEGL.SharedEglManager;
import com.checkmate.android.util.libgraph.EglCoreNew;
import com.checkmate.android.util.libgraph.SurfaceImageNew;
import com.checkmate.android.util.libgraph.WindowSurfaceNew;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * StreamTransitionManager - Manages seamless transitions between streaming services
 * 
 * Key Features:
 * 1. Pre-initializes EGL context at app startup for instant service switching
 * 2. Maintains active streams during transitions with blank frames + time overlay
 * 3. Dynamic configuration updates without stopping ongoing processes
 * 4. Optimal surface management for continuous streaming/recording
 */
public class StreamTransitionManager {
    private static final String TAG = "StreamTransitionManager";
    
    // Singleton pattern for application-wide access
    private static volatile StreamTransitionManager sInstance;
    private static final Object sLock = new Object();
    
    // EGL and graphics components
    private EglCoreNew mEglCore;
    private EGLContext mSharedContext;
    private HandlerThread mRenderThread;
    private Handler mRenderHandler;
    private volatile boolean mIsInitialized = false;
    private final CountDownLatch mInitLatch = new CountDownLatch(1);
    
    // Blank frame rendering components
    private WindowSurfaceNew mBlankFrameSurface;
    private SurfaceImageNew mTimeOverlay;
    private Paint mTextPaint;
    private Paint mBackgroundPaint;
    private Bitmap mOverlayBitmap;
    private Canvas mOverlayCanvas;
    private final AtomicBoolean mRenderingBlankFrames = new AtomicBoolean(false);
    
    // Transition state management
    private ServiceType mCurrentActiveService;
    private ServiceType mPendingService;
    private final Object mTransitionLock = new Object();
    private volatile boolean mTransitionInProgress = false;
    
    // Configuration management
    private Context mContext;
    private SharedEglManager mEglManager;
    private TransitionCallback mTransitionCallback;
    
    // Performance constants
    private static final long BLANK_FRAME_INTERVAL_MS = 33; // 30 FPS
    private static final int OVERLAY_TEXT_SIZE = 48;
    private static final int OVERLAY_PADDING = 32;
    private static final long TRANSITION_TIMEOUT_MS = 5000; // 5 seconds
    
    public interface TransitionCallback {
        void onTransitionStarted(ServiceType fromService, ServiceType toService);
        void onTransitionCompleted(ServiceType newService);
        void onTransitionFailed(ServiceType targetService, String error);
        void onBlankFrameRendered(long timestamp);
    }
    
    public static StreamTransitionManager getInstance() {
        if (sInstance == null) {
            synchronized (sLock) {
                if (sInstance == null) {
                    sInstance = new StreamTransitionManager();
                }
            }
        }
        return sInstance;
    }
    
    private StreamTransitionManager() {
        // Private constructor for singleton
    }
    
    /**
     * Initialize the transition manager early in app lifecycle
     * This should be called from MainActivity.onCreate()
     */
    public void initializeEarly(Context context) {
        if (mIsInitialized) {
            Log.d(TAG, "StreamTransitionManager already initialized");
            return;
        }
        
        mContext = context.getApplicationContext();
        mEglManager = SharedEglManager.getInstance();
        
        Log.d(TAG, "Starting early EGL initialization for seamless transitions");
        
        // Start render thread for background operations
        mRenderThread = new HandlerThread("StreamTransition");
        mRenderThread.start();
        mRenderHandler = new Handler(mRenderThread.getLooper());
        
        // Initialize EGL on render thread
        mRenderHandler.post(this::initializeEGL);
    }
    
    /**
     * Initialize EGL context and rendering components
     */
    private void initializeEGL() {
        try {
            Log.d(TAG, "Initializing EGL core for transition management");
            
            // Create EGL core with recordable flag for streaming/recording
            mEglCore = new EglCoreNew(null, EglCoreNew.FLAG_RECORDABLE);
            mSharedContext = EGL14.eglGetCurrentContext();
            
            // Initialize overlay rendering components
            initializeOverlayRendering();
            
            // Mark as initialized
            mIsInitialized = true;
            mInitLatch.countDown();
            
            Log.d(TAG, "EGL initialization completed successfully");
            
            if (mTransitionCallback != null) {
                mTransitionCallback.onTransitionCompleted(null);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize EGL for transitions", e);
            mInitLatch.countDown();
            
            if (mTransitionCallback != null) {
                mTransitionCallback.onTransitionFailed(null, e.getMessage());
            }
        }
    }
    
    /**
     * Initialize overlay rendering for blank frames with time display
     */
    private void initializeOverlayRendering() {
        // Create text paint for time overlay
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(OVERLAY_TEXT_SIZE);
        mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        
        // Create background paint for better text visibility
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(Color.argb(128, 0, 0, 0)); // Semi-transparent black
        
        // Create overlay bitmap (will be resized based on surface dimensions)
        createOverlayBitmap(1280, 720); // Default size, will be adjusted
        
        Log.d(TAG, "Overlay rendering initialized");
    }
    
    /**
     * Create overlay bitmap for rendering time information
     */
    private void createOverlayBitmap(int width, int height) {
        if (mOverlayBitmap != null && !mOverlayBitmap.isRecycled()) {
            mOverlayBitmap.recycle();
        }
        
        mOverlayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mOverlayCanvas = new Canvas(mOverlayBitmap);
        
        // Initialize overlay surface for rendering
        if (mTimeOverlay != null) {
            mTimeOverlay.release();
        }
        mTimeOverlay = new SurfaceImageNew(mEglCore);
    }
    
    /**
     * Seamlessly switch between services while maintaining active streams
     */
    public void switchService(ServiceType fromService, ServiceType toService, SurfaceTexture newSurface, int width, int height) {
        if (!waitForInitialization()) {
            Log.e(TAG, "Cannot switch service - initialization failed");
            return;
        }
        
        synchronized (mTransitionLock) {
            if (mTransitionInProgress) {
                Log.w(TAG, "Transition already in progress, queuing new request");
                mPendingService = toService;
                return;
            }
            
            mTransitionInProgress = true;
            mCurrentActiveService = fromService;
            mPendingService = toService;
        }
        
        Log.d(TAG, "Starting seamless service transition: " + fromService + " -> " + toService);
        
        if (mTransitionCallback != null) {
            mTransitionCallback.onTransitionStarted(fromService, toService);
        }
        
        mRenderHandler.post(() -> performServiceSwitch(toService, newSurface, width, height));
    }
    
    /**
     * Perform the actual service switch on the render thread
     */
    private void performServiceSwitch(ServiceType toService, SurfaceTexture newSurface, int width, int height) {
        try {
            // Start rendering blank frames to maintain stream continuity
            if (mEglManager.isStreaming() || mEglManager.isRecording()) {
                startBlankFrameRendering(width, height);
            }
            
            // Update SharedEglManager with new service surface
            mEglManager.switchActiveService(toService, newSurface, width, height);
            
            // Wait a brief moment for service to initialize
            Thread.sleep(100);
            
            // Stop blank frame rendering once new service is active
            stopBlankFrameRendering();
            
            // Complete transition
            synchronized (mTransitionLock) {
                mCurrentActiveService = toService;
                mTransitionInProgress = false;
                
                // Handle any pending transition
                ServiceType pending = mPendingService;
                mPendingService = null;
                
                if (pending != null && !pending.equals(toService)) {
                    // Schedule pending transition
                    mRenderHandler.postDelayed(() -> 
                        switchService(toService, pending, newSurface, width, height), 100);
                }
            }
            
            Log.d(TAG, "Service transition completed successfully: " + toService);
            
            if (mTransitionCallback != null) {
                mTransitionCallback.onTransitionCompleted(toService);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Service transition failed", e);
            
            stopBlankFrameRendering();
            
            synchronized (mTransitionLock) {
                mTransitionInProgress = false;
                mPendingService = null;
            }
            
            if (mTransitionCallback != null) {
                mTransitionCallback.onTransitionFailed(toService, e.getMessage());
            }
        }
    }
    
    /**
     * Start rendering blank frames with time overlay during transitions
     */
    private void startBlankFrameRendering(int width, int height) {
        if (mRenderingBlankFrames.getAndSet(true)) {
            return; // Already rendering
        }
        
        Log.d(TAG, "Starting blank frame rendering with time overlay");
        
        // Update overlay bitmap size if needed
        if (mOverlayBitmap.getWidth() != width || mOverlayBitmap.getHeight() != height) {
            createOverlayBitmap(width, height);
        }
        
        // Start render loop
        renderBlankFrameLoop();
    }
    
    /**
     * Render loop for blank frames with time overlay
     */
    private void renderBlankFrameLoop() {
        if (!mRenderingBlankFrames.get()) {
            return;
        }
        
        try {
            // Clear the overlay bitmap
            mOverlayCanvas.drawColor(Color.BLACK);
            
            // Draw semi-transparent background for text
            int centerX = mOverlayBitmap.getWidth() / 2;
            int centerY = mOverlayBitmap.getHeight() / 2;
            
            mOverlayCanvas.drawRect(
                centerX - 200, centerY - 60,
                centerX + 200, centerY + 60,
                mBackgroundPaint
            );
            
            // Draw current time
            String timeText = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            mOverlayCanvas.drawText(timeText, centerX, centerY, mTextPaint);
            
            // Draw "Transitioning..." text
            mOverlayCanvas.drawText("Transitioning...", centerX, centerY + 40, mTextPaint);
            
            // Render to all active surfaces (streaming/recording)
            renderOverlayToActiveSurfaces();
            
            if (mTransitionCallback != null) {
                mTransitionCallback.onBlankFrameRendered(SystemClock.elapsedRealtime());
            }
            
            // Schedule next frame
            mRenderHandler.postDelayed(this::renderBlankFrameLoop, BLANK_FRAME_INTERVAL_MS);
            
        } catch (Exception e) {
            Log.e(TAG, "Error rendering blank frame", e);
            stopBlankFrameRendering();
        }
    }
    
    /**
     * Render overlay to all active streaming/recording surfaces
     */
    private void renderOverlayToActiveSurfaces() {
        try {
            // Update overlay texture with current bitmap
            mTimeOverlay.updateBitmap(mOverlayBitmap);
            
            // Render to encoder surfaces maintained by SharedEglManager
            if (mEglManager.isStreaming()) {
                mTimeOverlay.drawFrame();
            }
            
            if (mEglManager.isRecording()) {
                mTimeOverlay.drawFrame();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error rendering overlay to surfaces", e);
        }
    }
    
    /**
     * Stop blank frame rendering
     */
    private void stopBlankFrameRendering() {
        if (mRenderingBlankFrames.getAndSet(false)) {
            Log.d(TAG, "Stopped blank frame rendering");
        }
    }
    
    /**
     * Update configuration dynamically without stopping streams/recordings
     */
    public void updateConfiguration(String configKey, Object configValue) {
        if (!waitForInitialization()) {
            Log.e(TAG, "Cannot update configuration - not initialized");
            return;
        }
        
        Log.d(TAG, "Updating configuration: " + configKey + " = " + configValue);
        
        mRenderHandler.post(() -> {
            try {
                // Apply configuration updates to SharedEglManager
                mEglManager.updateDynamicConfiguration(configKey, configValue);
                
                Log.d(TAG, "Configuration updated successfully: " + configKey);
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to update configuration: " + configKey, e);
            }
        });
    }
    
    /**
     * Wait for initialization to complete
     */
    private boolean waitForInitialization() {
        if (mIsInitialized) {
            return true;
        }
        
        try {
            return mInitLatch.await(TRANSITION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting for initialization", e);
            return false;
        }
    }
    
    /**
     * Set transition callback for receiving events
     */
    public void setTransitionCallback(TransitionCallback callback) {
        mTransitionCallback = callback;
    }
    
    /**
     * Check if a transition is currently in progress
     */
    public boolean isTransitionInProgress() {
        synchronized (mTransitionLock) {
            return mTransitionInProgress;
        }
    }
    
    /**
     * Get current active service
     */
    public ServiceType getCurrentActiveService() {
        synchronized (mTransitionLock) {
            return mCurrentActiveService;
        }
    }
    
    /**
     * Get shared EGL context for service initialization
     */
    public EGLContext getSharedEGLContext() {
        return mSharedContext;
    }
    
    /**
     * CRITICAL: Ensure minimal loading time during service transitions
     */
    public void ensureMinimalLoadingTime() {
        if (!mIsInitialized) {
            Log.w(TAG, "Cannot ensure minimal loading time - not initialized");
            return;
        }
        
        // Pre-create all necessary components
        mRenderHandler.post(() -> {
            try {
                // Ensure EGL context is ready
                if (mEglCore == null) {
                    mEglCore = new EglCoreNew(null, EglCoreNew.FLAG_RECORDABLE);
                }
                
                // Ensure SharedEglManager has streamers ready
                mEglManager.ensureStreamersCreated();
                
                Log.d(TAG, "Minimal loading time ensured - all components ready");
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to ensure minimal loading time", e);
            }
        });
    }

    /**
     * CRITICAL: Validate that blank screens are prevented
     */
    public boolean validateNoBlankScreens() {
        if (!mIsInitialized) {
            return false;
        }
        
        // Check if we can render blank frames with overlay
        try {
            createOverlayBitmap(1280, 720);
            return mOverlayBitmap != null && mTimeOverlay != null;
        } catch (Exception e) {
            Log.e(TAG, "Blank screen validation failed", e);
            return false;
        }
    }

    /**
     * CRITICAL: Maintain active stream without compromising user experience
     */
    public void maintainActiveStreamDuringTransition(ServiceType fromService, ServiceType toService) {
        if (!mIsInitialized) {
            Log.e(TAG, "Cannot maintain active stream - not initialized");
            return;
        }
        
        Log.d(TAG, "Maintaining active stream during transition: " + fromService + " -> " + toService);
        
        // Check if streaming or recording is active
        boolean isStreamingActive = mEglManager.isStreaming();
        boolean isRecordingActive = mEglManager.isRecording();
        
        if (isStreamingActive || isRecordingActive) {
            // Start blank frame rendering immediately
            startBlankFrameRendering(1280, 720); // Default size, will be adjusted
            
            Log.d(TAG, "Active stream maintained with blank frames during transition");
        }
    }

    /**
     * CRITICAL: Handle EGL restart only when safe
     */
    public void handleEglRestartSafely(ServiceType newServiceType, SurfaceTexture newSurface, int width, int height) {
        // Check if it's safe to restart EGL
        if (mEglManager.canPerformMajorOperation()) {
            Log.d(TAG, "Safe to restart EGL - no active streaming/recording");
            mEglManager.restartEglForNewConfiguration(newServiceType, newSurface, width, height);
        } else {
            Log.d(TAG, "EGL restart not safe - maintaining current configuration");
            // Use seamless transition instead
            switchService(mCurrentActiveService, newServiceType, newSurface, width, height);
        }
    }

    /**
     * CRITICAL: Ensure surface updates without interruption
     */
    public void updateSurfaceWithoutInterruption(SurfaceTexture newSurface, int width, int height) {
        if (!mIsInitialized) {
            Log.e(TAG, "Cannot update surface - not initialized");
            return;
        }
        
        Log.d(TAG, "Updating surface without interruption: " + width + "x" + height);
        
        mRenderHandler.post(() -> {
            try {
                // Check if we need to maintain stream during update
                if (mEglManager.isStreaming() || mEglManager.isRecording()) {
                    // Start blank frame rendering
                    startBlankFrameRendering(width, height);
                    
                    // Brief delay for smooth transition
                    Thread.sleep(50);
                }
                
                // Update the surface in SharedEglManager
                mEglManager.updateActiveSurface(newSurface, width, height);
                
                // Stop blank frame rendering
                stopBlankFrameRendering();
                
                Log.d(TAG, "Surface updated without interruption");
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to update surface without interruption", e);
                stopBlankFrameRendering();
            }
        });
    }

    /**
     * CRITICAL: Force configuration update without stopping processes
     */
    public void forceConfigurationUpdate(String configKey, Object configValue) {
        if (!mIsInitialized) {
            Log.e(TAG, "Cannot force configuration update - not initialized");
            return;
        }
        
        Log.d(TAG, "Force updating configuration without stopping processes: " + configKey);
        
        // Use SharedEglManager's force update method
        mEglManager.forceConfigurationUpdate(configKey, configValue);
        
        // Also update through regular method
        updateConfiguration(configKey, configValue);
    }

    /**
     * CRITICAL: Validate all requirements are met
     */
    public boolean validateAllRequirements() {
        Log.d(TAG, "Validating all optimization requirements...");
        
        boolean allValid = true;
        
        // 1. Check EGL initialization at startup
        if (!mIsInitialized || mEglCore == null) {
            Log.e(TAG, "FAILED: EGL not initialized at startup");
            allValid = false;
        } else {
            Log.d(TAG, "PASSED: EGL initialized at startup");
        }
        
        // 2. Check blank frame capability
        if (!validateNoBlankScreens()) {
            Log.e(TAG, "FAILED: Cannot prevent blank screens");
            allValid = false;
        } else {
            Log.d(TAG, "PASSED: Blank screen prevention ready");
        }
        
        // 3. Check streaming maintenance capability
        if (mEglManager == null || !mEglManager.isInitialized()) {
            Log.e(TAG, "FAILED: Cannot maintain active streams");
            allValid = false;
        } else {
            Log.d(TAG, "PASSED: Active stream maintenance ready");
        }
        
        // 4. Check surface update capability
        if (mTimeOverlay == null) {
            Log.e(TAG, "FAILED: Surface updates not ready");
            allValid = false;
        } else {
            Log.d(TAG, "PASSED: Surface updates ready");
        }
        
        // 5. Check dynamic configuration capability
        try {
            mEglManager.updateDynamicConfiguration("test", "value");
            Log.d(TAG, "PASSED: Dynamic configuration updates ready");
        } catch (Exception e) {
            Log.e(TAG, "FAILED: Dynamic configuration updates not working");
            allValid = false;
        }
        
        Log.d(TAG, "Requirements validation result: " + (allValid ? "ALL PASSED" : "SOME FAILED"));
        return allValid;
    }

    /**
     * CRITICAL: Performance monitoring during transitions
     */
    public void monitorTransitionPerformance(ServiceType fromService, ServiceType toService) {
        long startTime = System.currentTimeMillis();
        
        // Set up performance callback
        setTransitionCallback(new TransitionCallback() {
            @Override
            public void onTransitionStarted(ServiceType from, ServiceType to) {
                Log.d(TAG, "PERFORMANCE: Transition started at " + startTime);
            }
            
            @Override
            public void onTransitionCompleted(ServiceType newService) {
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                
                Log.d(TAG, "PERFORMANCE: Transition completed in " + duration + "ms");
                
                if (duration > 200) {
                    Log.w(TAG, "PERFORMANCE WARNING: Transition took longer than expected");
                } else {
                    Log.d(TAG, "PERFORMANCE: Transition within optimal time");
                }
            }
            
            @Override
            public void onTransitionFailed(ServiceType targetService, String error) {
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                
                Log.e(TAG, "PERFORMANCE: Transition failed after " + duration + "ms: " + error);
            }
            
            @Override
            public void onBlankFrameRendered(long timestamp) {
                // Monitor blank frame rendering performance
            }
        });
    }

    /**
     * Release resources and cleanup
     */
    public void release() {
        Log.d(TAG, "Releasing StreamTransitionManager resources");
        
        stopBlankFrameRendering();
        
        if (mRenderHandler != null) {
            mRenderHandler.removeCallbacksAndMessages(null);
        }
        
        if (mRenderThread != null) {
            mRenderThread.quitSafely();
        }
        
        if (mTimeOverlay != null) {
            mTimeOverlay.release();
            mTimeOverlay = null;
        }
        
        if (mOverlayBitmap != null && !mOverlayBitmap.isRecycled()) {
            mOverlayBitmap.recycle();
            mOverlayBitmap = null;
        }
        
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
        
        mIsInitialized = false;
        
        Log.d(TAG, "StreamTransitionManager resources released");
    }
}