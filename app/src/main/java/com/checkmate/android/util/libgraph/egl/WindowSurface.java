package com.checkmate.android.util.libgraph.egl;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.view.Surface;
import androidx.annotation.NonNull;

/**
 * EglSurfaceBase bound to a {@link Surface} or {@link SurfaceTexture}.
 */
public final class WindowSurface extends EglSurfaceBase {

    private Surface  nativeSurface;   // null if using SurfaceTexture
    private final boolean releaseNative;

    public WindowSurface(@NonNull EglCore core,
                         @NonNull Surface surface,
                         boolean releaseWithEglCore) {
        super(core);
        createWindowSurface(surface);
        this.nativeSurface  = surface;
        this.releaseNative  = releaseWithEglCore;
    }

    public WindowSurface(@NonNull EglCore core,
                         @NonNull SurfaceTexture st) {
        super(core);
        createWindowSurface(st);
        this.nativeSurface = null;
        this.releaseNative = false;
    }

    /** Free the EGL surface and, if requested, the underlying {@link Surface}. */
    public void release() {
        releaseEglSurface();
        if (releaseNative && nativeSurface != null) {
            nativeSurface.release();
        }
        nativeSurface = null;
    }
}

