package com.checkmate.android.util.libgraph;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.opengl.EGL14;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Common base class for EGL surfaces.
 * <p>
 * There can be multiple surfaces associated with a single context.
 */
public class EglSurfaceBaseNew implements AutoCloseable {
    private static final String TAG = GlUtilNew.TAG;

    // EglCore object we're associated with.
    public EglCoreNew mEglCore;

    private EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;
    private int mWidth = -1;
    private int mHeight = -1;

    public EglSurfaceBaseNew(EglCoreNew eglCore) {
        if (eglCore == null) {
            throw new IllegalArgumentException("EglCoreNew cannot be null");
        }
        mEglCore = eglCore;
    }

    /**
     * Creates a window surface.
     *
     * @param surface May be a Surface or SurfaceTexture.
     */
    public void createWindowSurface(final Object surface) {
        if (mEGLSurface != EGL14.EGL_NO_SURFACE) {
            throw new IllegalStateException("Surface already created");
        }
        mEGLSurface = mEglCore.createWindowSurface(surface);
    }

    /**
     * Creates an off-screen surface.
     *
     * @param width  Width in pixels.
     * @param height Height in pixels.
     */
    public void createOffscreenSurface(final int width, final int height) {
        if (mEGLSurface != EGL14.EGL_NO_SURFACE) {
            throw new IllegalStateException("Surface already created");
        }
        mEGLSurface = mEglCore.createOffscreenSurface(width, height);
        mWidth = width;
        mHeight = height;
    }

    /**
     * Returns the surface's width in pixels.
     */
    public int getWidth() {
        if (mEGLSurface == EGL14.EGL_NO_SURFACE) {
            throw new IllegalStateException("Surface not created");
        }
        return mWidth < 0 ? mEglCore.querySurface(mEGLSurface, EGL14.EGL_WIDTH) : mWidth;
    }

    /**
     * Returns the surface's height in pixels.
     */
    public int getHeight() {
        if (mEGLSurface == EGL14.EGL_NO_SURFACE) {
            throw new IllegalStateException("Surface not created");
        }
        return mHeight < 0 ? mEglCore.querySurface(mEGLSurface, EGL14.EGL_HEIGHT) : mHeight;
    }

    /**
     * Releases the EGL surface and resets state.
     */
    public void releaseEglSurface() {
        if (mEGLSurface != EGL14.EGL_NO_SURFACE) {
            mEglCore.releaseSurface(mEGLSurface);
            mEGLSurface = EGL14.EGL_NO_SURFACE;
        }
        mWidth = mHeight = -1;
    }

    /**
     * Makes our EGL context and surface current.
     */
    public void makeCurrent() {
        if (mEGLSurface == EGL14.EGL_NO_SURFACE) {
            throw new IllegalStateException("Surface not created");
        }
        mEglCore.makeCurrent(mEGLSurface);
    }

    /**
     * Makes our EGL context and surface current for drawing, using the supplied surface for reading.
     *
     * @param readSurface The surface to use for reading.
     */
    public void makeCurrentReadFrom(final EglSurfaceBaseNew readSurface) {
        if (mEGLSurface == EGL14.EGL_NO_SURFACE || readSurface == null || readSurface.mEGLSurface == EGL14.EGL_NO_SURFACE) {
            throw new IllegalStateException("Invalid surface for reading");
        }
        mEglCore.makeCurrent(mEGLSurface, readSurface.mEGLSurface);
    }

    /**
     * Swaps the buffers of the EGL surface.
     *
     * @return true if successful, false otherwise.
     */
    public boolean swapBuffers() {
        if (mEGLSurface == EGL14.EGL_NO_SURFACE) {
            throw new IllegalStateException("Surface not created");
        }
        boolean result = mEglCore.swapBuffers(mEGLSurface);
        if (!result) {
            Log.w(TAG, "swapBuffers() failed");
        }
        return result;
    }

    /**
     * Sets the presentation timestamp for the EGL surface.
     *
     * @param nsecs Timestamp in nanoseconds.
     */
    public void setPresentationTime(final long nsecs) {
        if (mEGLSurface == EGL14.EGL_NO_SURFACE) {
            throw new IllegalStateException("Surface not created");
        }
        mEglCore.setPresentationTime(mEGLSurface, nsecs);
    }

    /**
     * Saves the current frame of the EGL surface to the specified file.
     * <p>
     * Expects that the EGL context and surface are current.
     *
     * @param file The destination file.
     * @throws IOException if an I/O error occurs.
     */
    public void saveFrame(final File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        if (!mEglCore.isCurrent(mEGLSurface)) {
            throw new IllegalStateException("Expected EGL context/surface is not current");
        }

        final int width = getWidth();
        final int height = getHeight();

        // Allocate a direct ByteBuffer for RGBA pixel data.
        final ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4)
                .order(ByteOrder.LITTLE_ENDIAN);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        GlUtilNew.checkGlError("glReadPixels");
        buffer.rewind();

        // Create a bitmap from the pixel buffer.
        final Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.copyPixelsFromBuffer(buffer);

        // Flip the bitmap vertically.
        final Matrix matrix = new Matrix();
        matrix.postScale(1.0f, -1.0f);
        final Bitmap rotatedBitmap = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true);
        // Recycle the original bitmap as it is no longer needed.
        bmp.recycle();

        // Save the flipped bitmap to file using try-with-resources.
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
            if (!rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 90, bos)) {
                throw new IOException("Failed to compress bitmap.");
            }
            bos.flush();
        } finally {
            rotatedBitmap.recycle();
        }
        Log.d(TAG, "Saved " + width + "x" + height + " frame as '" + file.getAbsolutePath() + "'");
    }

    /**
     * Reads pixels from the current EGL surface.
     *
     * @return A ByteBuffer containing the pixel data.
     */
    public ByteBuffer readPixels() {
        if (!mEglCore.isCurrent(mEGLSurface)) {
            throw new IllegalStateException("Expected EGL context/surface is not current");
        }
        final int width = getWidth();
        final int height = getHeight();
        final ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4)
                .order(ByteOrder.LITTLE_ENDIAN);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        GlUtilNew.checkGlError("glReadPixels");
        buffer.rewind();
        return buffer;
    }

    @Override
    public void close() {
        releaseEglSurface();
    }
}
