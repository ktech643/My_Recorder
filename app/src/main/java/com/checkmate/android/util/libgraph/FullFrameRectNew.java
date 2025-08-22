package com.checkmate.android.util.libgraph;

import android.util.Log;

/**
 * This class essentially represents a viewport-sized sprite rendered with a texture,
 * typically from an external source (e.g. camera or video decoder).
 */
public class FullFrameRectNew implements AutoCloseable {
    private static final String TAG = "FullFrameRectNew";

    // A viewport-sized drawable (the full rectangle) that is immutable.
    protected final Drawable2dNew mRectDrawable = new Drawable2dNew(Drawable2dNew.Prefab.FULL_RECTANGLE);

    // The program used for rendering. FullFrameRect takes ownership and is responsible for releasing it.
    protected Texture2dProgramNew mProgram;

    /**
     * Constructs a FullFrameRectNew with the given program.
     *
     * @param program the texture program to use; must not be null.
     * @throws IllegalArgumentException if program is null.
     */
    public FullFrameRectNew(final Texture2dProgramNew program) {
        if (program == null) {
            throw new IllegalArgumentException("Texture2dProgramNew cannot be null");
        }
        mProgram = program;
    }

    /**
     * Releases resources. This must be called with the appropriate EGL context current
     * if EGL-specific cleanup is desired.
     *
     * @param doEglCleanup if true, performs EGL-context-specific cleanup.
     */
    public synchronized void release(final boolean doEglCleanup) {
        if (mProgram != null) {
            try {
                if (doEglCleanup) {
                    mProgram.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error releasing Texture2dProgramNew", e);
            } finally {
                mProgram = null;
            }
        }
    }

    /**
     * Closes this resource. By default, this performs a safe release that skips EGL-specific cleanup.
     * Clients needing EGL cleanup should call {@code release(true)} explicitly.
     */
    @Override
    public void close() {
        release(false);
    }

    /**
     * Returns the current texture program.
     *
     * @return the current Texture2dProgramNew.
     */
    public Texture2dProgramNew getProgram() {
        return mProgram;
    }

    /**
     * Changes the program. The previous program is released.
     * The appropriate EGL context must be current.
     *
     * @param newProgram the new program to use; must not be null.
     * @throws IllegalArgumentException if newProgram is null.
     */
    public synchronized void changeProgram(final Texture2dProgramNew newProgram) {
        if (newProgram == null) {
            throw new IllegalArgumentException("New program cannot be null");
        }
        if (mProgram != null) {
            try {
                mProgram.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing previous Texture2dProgramNew", e);
            }
        }
        mProgram = newProgram;
    }

    /**
     * Creates a texture object suitable for use with drawFrame().
     *
     * @return a new texture object.
     * @throws IllegalStateException if the program has been released.
     */
    public int createTextureObject() {
        if (mProgram == null) {
            throw new IllegalStateException("Texture2dProgramNew has been released");
        }
        return mProgram.createTextureObject();
    }

    /**
     * Draws a viewport-filling rectangle textured with the specified texture object.
     *
     * @param textureId the texture object ID.
     * @param texMatrix the texture coordinate transformation matrix.
     * @throws IllegalStateException if the program has been released.
     */
    public void drawFrame(final int textureId, final float[] texMatrix) {
        if (mProgram == null) {
            throw new IllegalStateException("Texture2dProgramNew has been released");
        }
        // Use the identity matrix for the model/view/projection transformation so that the
        // 2x2 FULL_RECTANGLE covers the entire viewport.
        mProgram.draw(GlUtilNew.IDENTITY_MATRIX,
                mRectDrawable.getVertexArray(),
                0,
                mRectDrawable.getVertexCount(),
                mRectDrawable.getCoordsPerVertex(),
                mRectDrawable.getVertexStride(),
                texMatrix,
                mRectDrawable.getTexCoordArray(),
                textureId,
                mRectDrawable.getTexCoordStride());
    }
}
