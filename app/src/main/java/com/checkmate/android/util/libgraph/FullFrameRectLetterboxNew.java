package com.checkmate.android.util.libgraph;

import android.opengl.Matrix;
import android.util.Log;

import com.checkmate.android.util.libgraph.gl.GlResourceManager;

import java.io.Closeable;

public final class FullFrameRectLetterboxNew extends FullFrameRectNew implements Closeable {
    private static final String TAG = "FullFrameRectLetterboxNew";

    // Reusable transformation matrix.
    private final float[] mMatrix = new float[16];
    private boolean isReleased = false;

    public FullFrameRectLetterboxNew(final Texture2dProgramNew program) {
        super(program);
    }

    /**
     * Resets the transformation matrix to identity.
     */
    private void resetMatrix() {
        Matrix.setIdentityM(mMatrix, 0);
    }

    /**
     * Common draw frame logic.
     *
     * @param textureId   Texture ID.
     * @param texMatrix   Texture coordinate matrix.
     * @param transform   Transformation matrix.
     */
    private void drawFrame(final int textureId, final float[] texMatrix, final float[] transform) {
        if (isReleased) {
            Log.w(TAG, "Attempted to draw on a released object.");
            return;
        }
        mProgram.draw(transform,
                mRectDrawable.getVertexArray(),
                0,
                mRectDrawable.getVertexCount(),
                mRectDrawable.getCoordsPerVertex(),
                mRectDrawable.getVertexStride(),
                texMatrix,
                mRectDrawable.getTexCoordArray(),
                textureId,
                mRectDrawable.getTexCoordStride());
        GlResourceManager.performCleanup();   // GL context is current here

    }

    /**
     * Releases resources associated with this object.
     */
    public void release() {
        if (!isReleased) {
            if (mProgram != null) {
                mProgram.release(); // Clean up the OpenGL program.
            }
            isReleased = true;
            Log.d(TAG, "Resources released.");
        }
    }

    @Override
    public void close() {
        release();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            release(); // Backup release in case close() was not called.
        } finally {
            super.finalize();
        }
    }

    /**
     * Draws the frame with Y-axis letterboxing.
     *
     * @param textureId Texture ID.
     * @param texMatrix Texture coordinate matrix.
     * @param rotate    Rotation angle (in degrees).
     * @param scale     Scaling factor along Y.
     */
    public void drawFrameY(final int textureId, final float[] texMatrix, final int rotate, final float scale) {
        resetMatrix();
        if (rotate != 0) {
            Matrix.rotateM(mMatrix, 0, rotate, 0.0f, 0.0f, 1.0f);
        }
        if (scale != 1.0f) {
            Matrix.scaleM(mMatrix, 0, 1.0f, scale, 1.0f);
        }
        drawFrame(textureId, texMatrix, mMatrix);
    }

    /**
     * Draws a flipped and mirrored frame.
     *
     * @param textureId Texture ID.
     * @param texMatrix Texture coordinate matrix.
     * @param rotation  Input rotation angle (in degrees).
     */
    public void drawFlipMirror(final int textureId, final float[] texMatrix, final int rotation) {
        resetMatrix();
        int rotate;
        if (rotation == 0) {
            rotate = 180;
        } else if (rotation == 90) {
            rotate = 270;
        } else if (rotation == 270) {
            rotate = 90;
        } else {
            rotate = rotation; // Default to input value if not one of the above.
        }
        Matrix.rotateM(mMatrix, 0, rotate, 0.0f, 0.0f, 1.0f);
        Matrix.scaleM(mMatrix, 0, -1.0f, 1.0f, 1.0f);
        drawFrame(textureId, texMatrix, mMatrix);
    }

    /**
     * Draws a mirrored frame along the Y-axis.
     *
     * @param textureId Texture ID.
     * @param texMatrix Texture coordinate matrix.
     * @param rotate    Rotation angle (in degrees).
     * @param scale     Scaling factor.
     */
    public void drawFrameMirrorY(final int textureId, final float[] texMatrix, final int rotate, final float scale) {
        resetMatrix();
        if (rotate != 0) {
            Matrix.rotateM(mMatrix, 0, rotate, 0.0f, 0.0f, 1.0f);
        }
        float effectiveScale = (rotate == 90 || rotate == 270) ? -scale : scale;
        if (effectiveScale != 1.0f) {
            Matrix.scaleM(mMatrix, 0, 1.0f, effectiveScale, 1.0f);
        }
        if (rotate == 0 || rotate == 180) {
            Matrix.scaleM(mMatrix, 0, -1.0f, 1.0f, 1.0f);
        }
        drawFrame(textureId, texMatrix, mMatrix);
    }

    /**
     * Draws the frame with X-axis letterboxing.
     *
     * @param textureId Texture ID.
     * @param texMatrix Texture coordinate matrix.
     * @param rotate    Rotation angle (in degrees).
     * @param scale     Scaling factor along X.
     */
    public void drawFrameX(final int textureId, final float[] texMatrix, final int rotate, final float scale) {
        resetMatrix();
        if (rotate != 0) {
            Matrix.rotateM(mMatrix, 0, rotate, 0.0f, 0.0f, 1.0f);
        }
        if (scale != 1.0f) {
            Matrix.scaleM(mMatrix, 0, scale, 1.0f, 1.0f);
        }
        drawFrame(textureId, texMatrix, mMatrix);
    }

    /**
     * Draws a mirrored frame along the X-axis.
     *
     * @param textureId Texture ID.
     * @param texMatrix Texture coordinate matrix.
     * @param rotate    Rotation angle (in degrees).
     * @param scale     Scaling factor.
     */
    public void drawFrameMirrorX(final int textureId, final float[] texMatrix, final int rotate, final float scale) {
        resetMatrix();
        if (rotate != 0) {
            Matrix.rotateM(mMatrix, 0, rotate, 0.0f, 0.0f, 1.0f);
        }
        float effectiveScale = (rotate == 0 || rotate == 180) ? -scale : scale;
        if (effectiveScale != 1.0f) {
            Matrix.scaleM(mMatrix, 0, effectiveScale, 1.0f, 1.0f);
        }
        if (rotate == 90 || rotate == 270) {
            Matrix.scaleM(mMatrix, 0, 1.0f, -1.0f, 1.0f);
        }
        drawFrame(textureId, texMatrix, mMatrix);
    }
}
