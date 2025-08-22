package com.checkmate.android.util.libgraph;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.util.Log;
import android.view.Surface;

/**
 * An EGL helper class for creating an EGL context, surface, etc.
 *
 * We have added safer checks and logging around makeNothingCurrent()
 */
public final class EglCoreNew /* implements AutoCloseable */ {
    private static final String TAG = "EglCoreNewOptimized";

    // Flags for constructor options
    public static final int FLAG_RECORDABLE = 0x01;
    public static final int FLAG_TRY_GLES3  = 0x02;

    // Android-specific extension for recordable surfaces
    private static final int EGL_RECORDABLE_ANDROID = 0x3142;

    private EGLDisplay mEGLDisplay    = EGL14.EGL_NO_DISPLAY;
    private EGLContext mEGLContext    = EGL14.EGL_NO_CONTEXT;
    private EGLConfig  mEGLConfig     = null;
    private int        mGlVersion     = -1;

    private final long mOwnerThreadId;    // The thread ID that created EglCoreNew
    private boolean    mReleased = false; // Whether release() has been called

    /**
     * Default constructor. Equivalent to EglCoreNew(null, 0).
     */
    public EglCoreNew() {
        this(null, 0);
    }

    /**
     * Prepares the EGL display and context.
     *
     * @param sharedContext  The context to share, or null if not sharing.
     * @param flags          Configuration flags (e.g. FLAG_RECORDABLE, FLAG_TRY_GLES3).
     */
    public EglCoreNew(final EGLContext sharedContext, final int flags) {
        mOwnerThreadId = Thread.currentThread().getId();  // track creation thread
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            throw new IllegalStateException("EGL already set up");
        }
        EGLContext shareCtx = (sharedContext != null) ? sharedContext : EGL14.EGL_NO_CONTEXT;

        // 1) Get display
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("Unable to get EGL14 display");
        }
        // 2) Initialize
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            mEGLDisplay = EGL14.EGL_NO_DISPLAY;
            throw new RuntimeException("Unable to initialize EGL14");
        }

        // 3) Attempt GLES3 if requested
        if ((flags & FLAG_TRY_GLES3) != 0) {
            EGLConfig config = getConfig(flags, 3);
            if (config != null) {
                final int[] attrib3_list = {
                        EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                        EGL14.EGL_NONE
                };
                EGLContext context = EGL14.eglCreateContext(
                        mEGLDisplay, config, shareCtx, attrib3_list, 0
                );
                if (EGL14.eglGetError() == EGL14.EGL_SUCCESS && context != null
                        && context != EGL14.EGL_NO_CONTEXT) {
                    mEGLConfig = config;
                    mEGLContext = context;
                    mGlVersion = 3;
                    Log.d(TAG, "GLES 3 context created.");
                } else {
                    Log.w(TAG, "Failed to create GLES 3 context; fallback to GLES 2.");
                }
            }
        }

        // 4) Fallback to GLES2 if needed
        if (mEGLContext == EGL14.EGL_NO_CONTEXT) {
            EGLConfig config = getConfig(flags, 2);
            if (config == null) {
                throw new RuntimeException("Unable to find a suitable EGLConfig for GLES2");
            }
            final int[] attrib2_list = {
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
            };
            EGLContext context = EGL14.eglCreateContext(
                    mEGLDisplay, config, shareCtx, attrib2_list, 0
            );
            checkEglError("eglCreateContext (GLES2)");
            mEGLConfig  = config;
            mEGLContext = context;
            mGlVersion  = 2;
            Log.d(TAG, "GLES 2 context created.");
        }

        // 5) Confirm
        int[] values = new int[1];
        EGL14.eglQueryContext(
                mEGLDisplay, mEGLContext,
                EGL14.EGL_CONTEXT_CLIENT_VERSION, values, 0
        );
        Log.d(TAG, "EGLContext created, client version " + values[0]);
    }

    /**
     * Finds a suitable EGLConfig.
     */
    private EGLConfig getConfig(final int flags, final int version) {
        int renderableType = EGL14.EGL_OPENGL_ES2_BIT;
        if (version >= 3) {
            renderableType |= EGLExt.EGL_OPENGL_ES3_BIT_KHR;
        }
        final int[] attribList = {
                EGL14.EGL_RED_SIZE,        8,
                EGL14.EGL_GREEN_SIZE,      8,
                EGL14.EGL_BLUE_SIZE,       8,
                EGL14.EGL_ALPHA_SIZE,      8,
                EGL14.EGL_RENDERABLE_TYPE, renderableType,
                EGL14.EGL_NONE,            0,    // for recordable
                EGL14.EGL_NONE
        };
        if ((flags & FLAG_RECORDABLE) != 0) {
            attribList[attribList.length - 3] = EGL_RECORDABLE_ANDROID;
            attribList[attribList.length - 2] = 1;
        }

        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        boolean success = EGL14.eglChooseConfig(
                mEGLDisplay, attribList, 0, configs, 0, configs.length,
                numConfigs, 0
        );
        if (!success || numConfigs[0] <= 0 || configs[0] == null) {
            Log.w(TAG, "Unable to find RGB8888 / GLES" + version + " EGLConfig");
            return null;
        }
        return configs[0];
    }

    /**
     * Release all EGL resources. Must be called from the same thread that created EglCoreNew.
     */
    public void release() {
        if (mReleased) {
            // Already released
            Log.w(TAG, "release() called after EglCoreNew was already released.");
            return;
        }
        if (Thread.currentThread().getId() != mOwnerThreadId) {
            Log.e(TAG, "release() called from a different thread than the one that created EglCoreNew!");
            // Just log or throw an exception. For safety, let's just log:
        }
        mReleased = true;
        Log.d(TAG, "Releasing EglCoreNew resources.");

        try {
            if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
                // Attempt to make nothing current
                makeNothingCurrentInternal();

                // Destroy context
                if (mEGLContext != EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
                }
                EGL14.eglReleaseThread();
                EGL14.eglTerminate(mEGLDisplay);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception during EglCoreNew.release()", e);
        } finally {
            mEGLDisplay = EGL14.EGL_NO_DISPLAY;
            mEGLContext = EGL14.EGL_NO_CONTEXT;
            mEGLConfig  = null;
            mGlVersion  = -1;
            Log.d(TAG, "EglCoreNew release() complete.");
        }
    }

    /**
     * Creates an EGL window surface.
     */
    public EGLSurface createWindowSurface(final Object surface) {
        if (mReleased) {
            Log.e(TAG, "createWindowSurface() after release(). returning EGL_NO_SURFACE.");
            return EGL14.EGL_NO_SURFACE;
        }
        if (!(surface instanceof Surface || surface instanceof SurfaceTexture)) {
            throw new IllegalArgumentException("Invalid surface: " + surface);
        }
        int[] surfaceAttribs = {EGL14.EGL_NONE};
        EGLSurface eglSurface = null;
        try {
            eglSurface = EGL14.eglCreateWindowSurface(
                    mEGLDisplay, mEGLConfig, surface, surfaceAttribs, 0
            );
            checkEglError("eglCreateWindowSurface");
            if (eglSurface == null || eglSurface == EGL14.EGL_NO_SURFACE) {
                throw new RuntimeException("Surface creation failed: " + EGL14.eglGetError());
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in createWindowSurface", e);
            throw new RuntimeException("Failed to create EGL window surface", e);
        }
        return eglSurface;
    }

    /**
     * Creates an offscreen surface.
     */
    public EGLSurface createOffscreenSurface(final int width, final int height) {
        if (mReleased) {
            Log.e(TAG, "createOffscreenSurface() after release(). returning EGL_NO_SURFACE.");
            return EGL14.EGL_NO_SURFACE;
        }
        int[] surfaceAttribs = {
                EGL14.EGL_WIDTH,  width,
                EGL14.EGL_HEIGHT, height,
                EGL14.EGL_NONE
        };
        EGLSurface eglSurface = null;
        try {
            eglSurface = EGL14.eglCreatePbufferSurface(mEGLDisplay, mEGLConfig, surfaceAttribs, 0);
            checkEglError("eglCreatePbufferSurface");
            if (eglSurface == null || eglSurface == EGL14.EGL_NO_SURFACE) {
                throw new RuntimeException("Offscreen surface creation failed: " + EGL14.eglGetError());
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in createOffscreenSurface", e);
            throw new RuntimeException("Failed to create offscreen surface", e);
        }
        return eglSurface;
    }

    /**
     * Release a surface.
     */
    public void releaseSurface(final EGLSurface eglSurface) {
        if (mReleased) {
            Log.w(TAG, "releaseSurface() after EglCoreNew was released. Doing nothing.");
            return;
        }
        if (eglSurface != null && eglSurface != EGL14.EGL_NO_SURFACE) {
            try {
                EGL14.eglDestroySurface(mEGLDisplay, eglSurface);
                checkEglError("eglDestroySurface");
            } catch (Exception e) {
                Log.e(TAG, "Exception releasing EGLSurface", e);
            }
        }
    }

    /**
     * Makes the specified surface current.
     */
    public void makeCurrent(final EGLSurface eglSurface) {
        if (mReleased) {
            Log.w(TAG, "makeCurrent() after release(). ignoring...");
            return;
        }
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY || mEGLContext == EGL14.EGL_NO_CONTEXT) {
            Log.e(TAG, "makeCurrent() invalid display or context (already released?)");
            return;
        }
        try {
            if (!EGL14.eglMakeCurrent(mEGLDisplay, eglSurface, eglSurface, mEGLContext)) {
                throw new RuntimeException("eglMakeCurrent failed: 0x"
                        + Integer.toHexString(EGL14.eglGetError()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in makeCurrent", e);
            throw new RuntimeException("Failed to make EGLSurface current", e);
        }
    }

    /**
     * Makes draw/read surfaces current.
     */
    public void makeCurrent(final EGLSurface drawSurface, final EGLSurface readSurface) {
        if (mReleased) {
            Log.w(TAG, "makeCurrent(draw, read) after release(). ignoring...");
            return;
        }
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY || mEGLContext == EGL14.EGL_NO_CONTEXT) {
            Log.e(TAG, "makeCurrent(draw, read) invalid display or context");
            return;
        }
        try {
            if (!EGL14.eglMakeCurrent(mEGLDisplay, drawSurface, readSurface, mEGLContext)) {
                throw new RuntimeException("eglMakeCurrent(draw, read) failed: 0x"
                        + Integer.toHexString(EGL14.eglGetError()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in makeCurrent(draw, read)", e);
            throw new RuntimeException("Failed to make draw/read surfaces current", e);
        }
    }

    /**
     * Clears the current EGL context (make nothing current).
     */
    public void makeNothingCurrent() {
        // We'll only do this if it's the same thread, or else just log a warning:
        if (Thread.currentThread().getId() != mOwnerThreadId) {
            Log.e(TAG, "makeNothingCurrent() from wrong thread. Skipping for safety...");
            return; // prevent crash
        }
        makeNothingCurrentInternal();
    }

    private void makeNothingCurrentInternal() {
        if (mReleased) {
            Log.w(TAG, "makeNothingCurrentInternal() after release(), ignoring...");
            return;
        }
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            Log.w(TAG, "makeNothingCurrentInternal() with EGL_NO_DISPLAY, skipping...");
            return;
        }
        if (mEGLContext == EGL14.EGL_NO_CONTEXT) {
            Log.w(TAG, "makeNothingCurrentInternal() with EGL_NO_CONTEXT, skipping...");
            return;
        }
        try {
            if (!EGL14.eglMakeCurrent(
                    mEGLDisplay,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT))
            {
                int err = EGL14.eglGetError();
                Log.e(TAG, "eglMakeCurrent (release) failed: 0x" + Integer.toHexString(err));
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in makeNothingCurrentInternal", e);
        }
    }

    /**
     * Swaps the buffers.
     */
    public boolean swapBuffers(final EGLSurface eglSurface) {
        if (mReleased) {
            Log.w(TAG, "swapBuffers() after release(), ignoring...");
            return false;
        }
        try {
            return EGL14.eglSwapBuffers(mEGLDisplay, eglSurface);
        } catch (Exception e) {
            Log.e(TAG, "Exception in swapBuffers", e);
            return false;
        }
    }

    /**
     * Sets presentation timestamp.
     */
    public void setPresentationTime(final EGLSurface eglSurface, final long nsecs) {
        if (mReleased) {
            Log.w(TAG, "setPresentationTime() after release(), ignoring...");
            return;
        }
        try {
            EGLExt.eglPresentationTimeANDROID(mEGLDisplay, eglSurface, nsecs);
        } catch (Exception e) {
            Log.e(TAG, "Exception in setPresentationTime", e);
        }
    }

    /**
     * Checks if the given surface is current.
     */
    public boolean isCurrent(final EGLSurface eglSurface) {
        if (mReleased) {
            return false;
        }
        return (mEGLContext.equals(EGL14.eglGetCurrentContext())
                && eglSurface.equals(EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW)));
    }

    /**
     * Queries integer value from surface.
     */
    public int querySurface(final EGLSurface eglSurface, final int what) {
        int[] value = new int[1];
        EGL14.eglQuerySurface(mEGLDisplay, eglSurface, what, value, 0);
        return value[0];
    }

    public int getGlVersion() {
        return mGlVersion;
    }

    /**
     * Logs current EGL info.
     */
    public static void logCurrent(final String msg) {
        EGLDisplay display = EGL14.eglGetCurrentDisplay();
        EGLContext context = EGL14.eglGetCurrentContext();
        EGLSurface surface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW);
        Log.i(TAG, "Current EGL (" + msg + "): display=" + display +
                ", context=" + context + ", surface=" + surface);
    }

    /**
     * Check EGL error helper.
     */
    private void checkEglError(final String msg) {
        int error = EGL14.eglGetError();
        if (error != EGL14.EGL_SUCCESS) {
            throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }

    /**
     * Check if the EGL context is valid
     */
    public boolean isEglContextValid() {
        return !mReleased &&
                mEGLDisplay != null && mEGLDisplay != EGL14.EGL_NO_DISPLAY &&
                mEGLContext != null && mEGLContext != EGL14.EGL_NO_CONTEXT &&
                mEGLConfig != null;
    }
}
