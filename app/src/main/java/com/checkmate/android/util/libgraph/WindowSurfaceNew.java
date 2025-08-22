package com.checkmate.android.util.libgraph;

import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;

/**
 * Associates an EGL surface with a native window or a SurfaceTexture.
 */
public final class WindowSurfaceNew extends EglSurfaceBaseNew {
    private static final String TAG = "WindowSurfaceNew";

    private Surface mSurface; // May be null when using SurfaceTexture.
    private final boolean mReleaseSurface; // Indicates if mSurface should be released.

    /**
     * Constructs a WindowSurfaceNew for a native window surface.
     *
     * @param eglCore        The EGL core to use; must not be null.
     * @param surface        The native Surface; must not be null.
     * @param releaseSurface If true, mSurface will be released when release() is called.
     * @throws IllegalArgumentException if eglCore or surface is null.
     */
    public WindowSurfaceNew(final EglCoreNew eglCore, final Surface surface, final boolean releaseSurface) {
        super(validateEglCore(eglCore));
        if (surface == null) {
            throw new IllegalArgumentException("Surface cannot be null");
        }
        createWindowSurface(surface);
        mSurface = surface;
        mReleaseSurface = releaseSurface;
    }

    /**
     * Constructs a WindowSurfaceNew for a SurfaceTexture.
     *
     * @param eglCore        The EGL core to use; must not be null.
     * @param surfaceTexture The SurfaceTexture; must not be null.
     * @throws IllegalArgumentException if eglCore or surfaceTexture is null.
     */
    public WindowSurfaceNew(final EglCoreNew eglCore, final SurfaceTexture surfaceTexture) {
        super(validateEglCore(eglCore));
        if (surfaceTexture == null) {
            throw new IllegalArgumentException("SurfaceTexture cannot be null");
        }
        createWindowSurface(surfaceTexture);
        mReleaseSurface = false; // Not applicable for SurfaceTexture.
    }

    /**
     * Releases EGL resources (via releaseEglSurface()) and, if configured, releases the native Surface.
     * This method does not require the EGL context to be current.
     */
    public void release() {
        // Release EGL surface resources first.
        releaseEglSurface();
        // If using a native Surface and flagged to release it, do so.
        if (mSurface != null) {
            if (mReleaseSurface) {
                try {
                    mSurface.release();
                    Log.d(TAG, "Surface released.");
                } catch (Exception e) {
                    Log.e(TAG, "Error releasing Surface", e);
                }
            }
            mSurface = null;
        }
    }

    /**
     * Recreates the EGL surface using the new EGL core.
     * <p>
     * This method is only supported for native Surfaces (not SurfaceTextures).
     * The caller must have already freed the old EGL surface using releaseEglSurface().
     *
     * @param newEglCore The new EGL core to use; must not be null.
     * @throws IllegalArgumentException if newEglCore is null.
     * @throws RuntimeException         if this method is called for a SurfaceTexture.
     */
    public void recreate(final EglCoreNew newEglCore) {
        if (newEglCore == null) {
            throw new IllegalArgumentException("newEglCore cannot be null");
        }
        if (mSurface == null) {
            throw new RuntimeException("Recreation for SurfaceTexture is not implemented");
        }
        // Switch to the new EGL core and create a new EGL window surface.
        // (The old EGL surface should have already been released.)
        mEglCore = newEglCore;
        createWindowSurface(mSurface);
        Log.d(TAG, "EGL surface recreated using new EGL core.");
    }

    /**
     * Helper method to validate the EGL core.
     */
    private static EglCoreNew validateEglCore(final EglCoreNew eglCore) {
        if (eglCore == null) {
            throw new IllegalArgumentException("EglCoreNew cannot be null");
        }
        return eglCore;
    }
}
