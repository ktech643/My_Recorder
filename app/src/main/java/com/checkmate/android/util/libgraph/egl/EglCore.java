package com.checkmate.android.util.libgraph.egl;

import android.opengl.*;
import android.util.Log;
import android.view.Surface;
import androidx.annotation.NonNull;
import com.checkmate.android.util.libgraph.gl.SecureSurfaceAccess;
import com.checkmate.android.util.libgraph.libutils.CloseGuard;
import com.checkmate.android.util.libgraph.libutils.GlCrashReporter;
import com.checkmate.android.util.libgraph.libutils.ThreadGuard;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EglCore implements AutoCloseable {
    private static final String TAG = "EglCore";
    // Flags
    public static final int FLAG_RECORDABLE = 0x1;
    public static final int FLAG_TRY_GLES3  = 0x2;
    private static final int EGL_RECORDABLE_ANDROID = 0x3142;
    /* state */
    private EGLDisplay display   = EGL14.EGL_NO_DISPLAY;
    private EGLContext context   = EGL14.EGL_NO_CONTEXT;
    private EGLConfig  config;
    private int glVersion = 2;
    /* helpers */
    private final ThreadGuard thread = new ThreadGuard();
    private final CloseGuard  guard  = new CloseGuard("EglCore");
    private final Set<EGLSurface> surfaces = Collections.newSetFromMap(new ConcurrentHashMap<>());
    // In EglCore:
    private final List<ContextListener> listeners = new CopyOnWriteArrayList<>();
    // Add these new fields
    private final EGLContext sharedContext;
    private final int flags;
    private boolean contextDirty;
    public void addContextListener(ContextListener l) {
        listeners.add(l);
    }


    private void initializeDisplay() throws RuntimeException {
        int[] vers = new int[2];
        if (!EGL14.eglInitialize(display, vers, 0, vers, 1)) {
            throw new RuntimeException("eglInitialize failed");
        }
    }

    private void createContext() {
        // Same initialization as original constructor
        // ...
    }

    public void handleContextLoss() {
        thread.check();
        destroySurfaces();
        notifyContextLost();
    }


    private void notifyContextLost() {
        for (ContextListener l : listeners) l.onContextLost();
    }
    // ───────────────────────────────── ctor ─────────────────────────────────

    public EglCore(EGLContext shared, int flags) {
        this.sharedContext = shared;
        this.flags = flags;
        guard.close();

        initializeEgl();
    }

    // New initialization helper
    private void initializeEgl() {
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (display == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed");
        }

        int[] vers = new int[2];
        if (!EGL14.eglInitialize(display, vers, 0, vers, 1)) {
            throw new RuntimeException("eglInitialize failed");
        }

        // Try creating context with stored parameters
        if ((flags & FLAG_TRY_GLES3) != 0) tryCreateContext(sharedContext, flags, 3);
        if (context == EGL14.EGL_NO_CONTEXT) tryCreateContext(sharedContext, flags, 2);
        if (context == EGL14.EGL_NO_CONTEXT) {
            throw new RuntimeException("Unable to create GLES context");
        }
    }

    // Enhanced context recreation
    public void recreate() {
        thread.check();
        contextDirty = true;

        // Cleanup existing resources
        closeInternal();

        try {
            // Re-initialize EGL with original parameters
            initializeEgl();

            // Notify listeners about successful recreation
            for (ContextListener l : listeners) {
                l.onContextRecreated();
            }
        } catch (Exception e) {
            // Notify listeners about recreation failure
            for (ContextListener l : listeners) {
                l.onContextLost();
            }
            throw new RuntimeException("Context recreation failed", e);
        }
    }

    private void tryCreateContext(EGLContext shared, int flags, int version) {
        int renderable = EGL14.EGL_OPENGL_ES2_BIT;
        if (version >= 3) renderable |= EGLExt.EGL_OPENGL_ES3_BIT_KHR;

        int[] attrib = {
                EGL14.EGL_RED_SIZE,   8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE,  8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, renderable,
                /* recordable */ EGL14.EGL_NONE, 0,
                EGL14.EGL_NONE
        };
        if ((flags & FLAG_RECORDABLE) != 0) {
            attrib[attrib.length - 3] = EGL_RECORDABLE_ANDROID;
            attrib[attrib.length - 2] = 1;
        }
        EGLConfig[] cfg = new EGLConfig[1];
        int[] num = new int[1];
        if (!EGL14.eglChooseConfig(display, attrib, 0, cfg, 0, 1, num, 0) || num[0] == 0)
            return;

        int[] ctxAttr = { EGL14.EGL_CONTEXT_CLIENT_VERSION, version, EGL14.EGL_NONE };
        context = EGL14.eglCreateContext(display, cfg[0],
                shared != null ? shared : EGL14.EGL_NO_CONTEXT, ctxAttr, 0);
        if (context != EGL14.EGL_NO_CONTEXT && EGL14.eglGetError() == EGL14.EGL_SUCCESS) {
            config = cfg[0];
            glVersion = version;
            Log.d(TAG, "Created GLES" + version + " context");
        } else {
            context = EGL14.EGL_NO_CONTEXT;
        }
    }

    // ───────────────────────────── surfaces ───────────────────────────────
    public EGLSurface createWindowSurface(@NonNull Object nativeSurface) {
        if (nativeSurface instanceof Surface) {
            SecureSurfaceAccess.validateAccess(((Surface)nativeSurface).toString());
        }
        thread.check();
        int[] attr = { EGL14.EGL_NONE };
        EGLSurface s;
        try {
            s = EGL14.eglCreateWindowSurface(display, config, nativeSurface, attr, 0);
            surfaces.add(s);
            check("eglCreateWindowSurface");
        } catch (Throwable t) {
            GlCrashReporter.log(t);
            throw t;
        }
        return s;
    }

    public EGLSurface createOffscreenSurface(int w, int h) {
        thread.check();
        int[] attr = { EGL14.EGL_WIDTH, w, EGL14.EGL_HEIGHT, h, EGL14.EGL_NONE };
        EGLSurface s = EGL14.eglCreatePbufferSurface(display, config, attr, 0);
        check("eglCreatePbufferSurface");
        return s;
    }

    public void makeCurrent(EGLSurface s) {
        thread.check();
        if (!EGL14.eglMakeCurrent(display, s, s, context)) {
            notifyContextLost(); // Trigger recovery
            throw new RuntimeException("eglMakeCurrent failed");
        }
    }

    public void makeCurrent(EGLSurface draw, EGLSurface read) {
        thread.check();
        if (!EGL14.eglMakeCurrent(display, draw, read, context))
            throw new RuntimeException("eglMakeCurrent(draw,read) fail 0x" + Integer.toHexString(EGL14.eglGetError()));
    }

    public boolean makeCurrentSafe(EGLSurface s) {
        thread.check();              // ThreadGuard
        if (!isValid()) return false;
        if (!EGL14.eglMakeCurrent(display, s, s, context)) {
            if (EGL14.eglGetError() == EGL14.EGL_CONTEXT_LOST) {
                notifyContextLost();
                return false;        // caller must bail out
            }
            throw new RuntimeException("eglMakeCurrent failed");
        }
        return true;
    }

    public void makeNothingCurrent() {
        thread.check();
        EGL14.eglMakeCurrent(display,
                EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
    }

    public boolean swap(EGLSurface s) { thread.check(); return EGL14.eglSwapBuffers(display, s); }

    public void setPresentationTime(EGLSurface s, long ns) {
        thread.check();
        EGLExt.eglPresentationTimeANDROID(display, s, ns);
    }

    public int query(EGLSurface s, int what) {
        int[] v = new int[1];
        EGL14.eglQuerySurface(display, s, what, v, 0);
        return v[0];
    }

    public void releaseSurface(EGLSurface s) {
        thread.check();
        if (s == EGL14.EGL_NO_SURFACE) return;

        if (surfaces.remove(s)) {
            EGL14.eglDestroySurface(display, s);
        }
    }

    private void destroySurfaces() {
        for (EGLSurface s : surfaces) {
            if (s != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(display, s);
            }
        }
        surfaces.clear();
    }
    public int getGlVersion() { return glVersion; }

    // ───────────────────────────── cleanup ────────────────────────────────

    // Modified close method
    public synchronized void close() {
        closeInternal();
        guard.close();
    }

    private void closeInternal() {
        if (display == EGL14.EGL_NO_DISPLAY) return;

        thread.check();
        try {
            makeNothingCurrent();
            destroySurfaces();
            if (context != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(display, context);
            }
            EGL14.eglReleaseThread();
            EGL14.eglTerminate(display);
        } finally {
            display = EGL14.EGL_NO_DISPLAY;
            context = EGL14.EGL_NO_CONTEXT;
            config = null;
        }
    }

    // Add context validity check
    public boolean isValid() {
        return context != EGL14.EGL_NO_CONTEXT &&
                display != EGL14.EGL_NO_DISPLAY;
    }

    private static void check(String op) {
        int err = EGL14.eglGetError();
        if (err != EGL14.EGL_SUCCESS)
            throw new RuntimeException(op + " error 0x" + Integer.toHexString(err));
    }
}
