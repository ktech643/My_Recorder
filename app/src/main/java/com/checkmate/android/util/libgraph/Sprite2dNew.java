package com.checkmate.android.util.libgraph;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

/**
 * Base class for a 2D object. Includes position, scale, rotation, and flat-shaded color.
 */
public final class Sprite2dNew {
    private static final String TAG = GlUtilNew.TAG;

    private Drawable2dNew mDrawable;
    private final float[] mColor;
    private int mTextureId;
    private float mAngle;
    private float mScaleX, mScaleY;
    private float mPosX, mPosY;

    // Model-view matrix and flag for lazy recomputation.
    private final float[] mModelViewMatrix;
    private boolean mMatrixReady;

    // Scratch matrix used to compute the MVP matrix.
    private final float[] mScratchMatrix;

    /**
     * Constructs a sprite with the given drawable.
     *
     * @param drawable The drawable object to use. Must not be null.
     * @throws IllegalArgumentException if drawable is null.
     */
    public Sprite2dNew(final Drawable2dNew drawable) {
        if (drawable == null) {
            throw new IllegalArgumentException("Drawable2dNew cannot be null");
        }
        mDrawable = drawable;
        // Default color: opaque white.
        mColor = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
        mTextureId = -1;
        mScaleX = 1.0f;
        mScaleY = 1.0f;
        mPosX = 0.0f;
        mPosY = 0.0f;
        mAngle = 0.0f;
        mModelViewMatrix = new float[16];
        mScratchMatrix = new float[16];
        mMatrixReady = false;
    }

    /**
     * Recomputes the model-view matrix based on the current translation, rotation, and scale.
     */
    private void recomputeMatrix() {
        Matrix.setIdentityM(mModelViewMatrix, 0);
        Matrix.translateM(mModelViewMatrix, 0, mPosX, mPosY, 0.0f);
        if (mAngle != 0.0f) {
            Matrix.rotateM(mModelViewMatrix, 0, mAngle, 0.0f, 0.0f, 1.0f);
        }
        Matrix.scaleM(mModelViewMatrix, 0, mScaleX, mScaleY, 1.0f);
        mMatrixReady = true;
    }

    /**
     * Returns the sprite scale along the X axis.
     */
    public float getScaleX() {
        return mScaleX;
    }

    /**
     * Returns the sprite scale along the Y axis.
     */
    public float getScaleY() {
        return mScaleY;
    }

    /**
     * Sets the sprite scale.
     *
     * @param scaleX Scale along X.
     * @param scaleY Scale along Y.
     */
    public void setScale(final float scaleX, final float scaleY) {
        mScaleX = scaleX;
        mScaleY = scaleY;
        mMatrixReady = false;
    }

    /**
     * Returns the sprite rotation angle in degrees.
     */
    public float getRotation() {
        return mAngle;
    }

    /**
     * Sets the sprite rotation angle in degrees. Rotation is normalized to [0, 360).
     *
     * @param angle The desired rotation angle.
     */
    public void setRotation(final float angle) {
        // Normalize the angle to [0, 360).
        mAngle = ((angle % 360) + 360) % 360;
        mMatrixReady = false;
    }

    /**
     * Returns the sprite position along the X axis.
     */
    public float getPositionX() {
        return mPosX;
    }

    /**
     * Returns the sprite position along the Y axis.
     */
    public float getPositionY() {
        return mPosY;
    }

    /**
     * Sets the sprite position.
     *
     * @param posX X position.
     * @param posY Y position.
     */
    public void setPosition(final float posX, final float posY) {
        mPosX = posX;
        mPosY = posY;
        mMatrixReady = false;
    }

    /**
     * Returns the model-view matrix.
     * <p>
     * To avoid allocations, this returns internal state (do not modify).
     */
    public float[] getModelViewMatrix() {
        if (!mMatrixReady) {
            recomputeMatrix();
        }
        return mModelViewMatrix;
    }

    /**
     * Sets the color for flat-shaded rendering. (No effect on textured rendering.)
     *
     * @param red   Red component.
     * @param green Green component.
     * @param blue  Blue component.
     */
    public void setColor(final float red, final float green, final float blue) {
        mColor[0] = red;
        mColor[1] = green;
        mColor[2] = blue;
    }

    /**
     * Sets the texture to use for textured rendering.
     *
     * @param textureId The texture object ID.
     */
    public void setTexture(final int textureId) {
        mTextureId = textureId;
    }

    /**
     * Returns the color array. To avoid allocations, this returns internal state (do not modify).
     */
    public float[] getColor() {
        return mColor;
    }

    /**
     * Draws the sprite with the supplied texture program and projection matrix.
     *
     * @param program          The texture program to use. If null, an error is logged.
     * @param projectionMatrix The projection matrix (must be a 16-element array).
     */
    public void draw(final Texture2dProgramNew program, final float[] projectionMatrix) {

        if (program == null) {
            Log.e(TAG, "draw: Program is null");
            return;
        }
        if (projectionMatrix == null || projectionMatrix.length != 16) {
            Log.e(TAG, "draw: Invalid projection matrix");
            return;
        }
        if (mDrawable == null) {
            Log.e(TAG, "draw: Drawable is null");
            return;
        }
        // Compute the model-view-projection matrix: projectionMatrix * modelViewMatrix.
        Matrix.multiplyMM(mScratchMatrix, 0, projectionMatrix, 0, getModelViewMatrix(), 0);
        program.draw(mScratchMatrix, mDrawable.getVertexArray(), 0,
                mDrawable.getVertexCount(), mDrawable.getCoordsPerVertex(),
                mDrawable.getVertexStride(), GlUtilNew.IDENTITY_MATRIX,
                mDrawable.getTexCoordArray(), mTextureId, mDrawable.getTexCoordStride());
    }

    @Override
    public String toString() {
        return "[Sprite2d pos=" + mPosX + "," + mPosY +
                " scale=" + mScaleX + "," + mScaleY +
                " angle=" + mAngle +
                " color={" + mColor[0] + "," + mColor[1] + "," + mColor[2] +
                "} drawable=" + mDrawable + "]";
    }

    /**
     * Clears and releases resources used by this sprite.
     * <p>
     * This method deletes the texture (if valid) and resets internal state.
     */
    public void clearResources() {
        // Release texture if valid.
        if (mTextureId != -1) {
            final int[] textures = { mTextureId };
            GLES20.glDeleteTextures(1, textures, 0);
            mTextureId = -1;
        }
        // Reset transformation parameters.
        mScaleX = 1.0f;
        mScaleY = 1.0f;
        mPosX = 0.0f;
        mPosY = 0.0f;
        mAngle = 0.0f;
        mMatrixReady = false;
        // Reset color to default opaque white.
        mColor[0] = 1.0f;
        mColor[1] = 1.0f;
        mColor[2] = 1.0f;
        mColor[3] = 1.0f;
        // Release drawable reference.
        mDrawable = null;
    }
}
