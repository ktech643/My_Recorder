package com.checkmate.android.util.libgraph.egl;

import android.opengl.EGL14;
import android.opengl.EGLSurface;
import android.util.Log;

import androidx.annotation.NonNull;

import com.checkmate.android.util.libgraph.libutils.CloseGuard;

/**
 * Convenience wrapper for an {@link EGLSurface} + size cache.
 */
public class EglSurfaceBase implements AutoCloseable {

    protected EglCore core;
    protected EGLSurface surface = EGL14.EGL_NO_SURFACE;
    private int cachedW = -1, cachedH = -1;
    private final CloseGuard guard = new CloseGuard("EglSurface");

    protected EglSurfaceBase(@NonNull EglCore core) { this.core = core; }

    public void createWindowSurface(Object nativeSurf) {
        if (surface != EGL14.EGL_NO_SURFACE) throw new IllegalStateException("surface exists");
        surface = core.createWindowSurface(nativeSurf);
    }

    public void createOffscreenSurface(int w, int h) {
        if (surface != EGL14.EGL_NO_SURFACE) throw new IllegalStateException("surface exists");
        surface = core.createOffscreenSurface(w, h);
        cachedW = w; cachedH = h;
    }

    public int  getWidth()  { return cachedW >= 0 ? cachedW : core.query(surface, EGL14.EGL_WIDTH); }
    public int  getHeight() { return cachedH >= 0 ? cachedH : core.query(surface, EGL14.EGL_HEIGHT); }

    /** Old behaviour (no context-loss handling). */
    public void makeCurrent() { core.makeCurrent(surface); }

    /** Delegates to {@link EglCore#makeCurrentSafe(EGLSurface)}. */
    public boolean makeCurrentSafe() { return core.makeCurrentSafe(surface); }

    public void makeCurrentReadFrom(EglSurfaceBase read) { core.makeCurrent(surface, read.surface); }
    public boolean swap()                { return core.swap(surface); }
    public void   setPresentation(long ns){ core.setPresentationTime(surface, ns); }

    public void releaseEglSurface() {
        guard.close();
        core.releaseSurface(surface);
        surface = EGL14.EGL_NO_SURFACE;
        cachedW = cachedH = -1;
    }

    @Override public void close() {
        releaseEglSurface();
        core = null;
    }

    @Override protected void finalize() throws Throwable {
        try {
            if (surface != EGL14.EGL_NO_SURFACE) {
                Log.w("TAG", "Surface was not properly released");
                core.releaseSurface(surface);
                guard.close();
            }
        } finally {
            super.finalize();
        }
    }
}
