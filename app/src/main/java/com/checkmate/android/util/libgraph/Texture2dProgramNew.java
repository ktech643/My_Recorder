package com.checkmate.android.util.libgraph;

import android.annotation.TargetApi;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Log;

import java.nio.FloatBuffer;

/**
 * GL program and supporting functions for textured 2D shapes with optimizations.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public final class Texture2dProgramNew {
    private static final String TAG = "Texture2dProgramNew";

    public enum ProgramType {
        TEXTURE_2D, TEXTURE_EXT, TEXTURE_EXT_BW, TEXTURE_EXT_FILT
    }

    // Shaders
    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uTexMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * aPosition;\n" +
                    "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
                    "}\n";

    private static final String FRAGMENT_SHADER_2D =
            "precision lowp float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform sampler2D sTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";

    private static final String FRAGMENT_SHADER_EXT =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision lowp float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";

    private static final String FRAGMENT_SHADER_EXT_BW =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision lowp float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
                    "    float color = tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11;\n" +
                    "    gl_FragColor = vec4(color, color, color, 1.0);\n" +
                    "}\n";

    private static final int KERNEL_SIZE = 9;

    private static final String FRAGMENT_SHADER_EXT_FILT =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "#define KERNEL_SIZE " + KERNEL_SIZE + "\n" +
                    "precision highp float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "uniform float uKernel[KERNEL_SIZE];\n" +
                    "uniform vec2 uTexOffset[KERNEL_SIZE];\n" +
                    "uniform float uColorAdjust;\n" +
                    "void main() {\n" +
                    "    vec4 sum = vec4(0.0);\n" +
                    "    for (int i = 0; i < KERNEL_SIZE; i++) {\n" +
                    "        vec4 texc = texture2D(sTexture, vTextureCoord + uTexOffset[i]);\n" +
                    "        sum += texc * uKernel[i];\n" +
                    "    }\n" +
                    "    gl_FragColor = sum + uColorAdjust;\n" +
                    "}\n";

    private final ProgramType mProgramType;
    private final int mProgramHandle;
    private final int mTextureTarget;

    // Uniform and attribute locations
    private int muMVPMatrixLoc;
    private int muTexMatrixLoc;
    private int muKernelLoc;
    private int muTexOffsetLoc;
    private int muColorAdjustLoc;
    private int maPositionLoc;
    private int maTextureCoordLoc;

    private final float[] mKernel = new float[KERNEL_SIZE];
    private final float[] mTexOffset = new float[2 * KERNEL_SIZE];
    private float mColorAdjust = 0f;

    public Texture2dProgramNew(final ProgramType programType) {
        mProgramType = programType;
        switch (programType) {
            case TEXTURE_2D:
                mTextureTarget = GLES20.GL_TEXTURE_2D;
                mProgramHandle = GlUtilNew.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_2D);
                break;
            case TEXTURE_EXT:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle = GlUtilNew.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT);
                break;
            case TEXTURE_EXT_BW:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle = GlUtilNew.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_BW);
                break;
            case TEXTURE_EXT_FILT:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle = GlUtilNew.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_FILT);
                break;
            default:
                throw new IllegalArgumentException("Unsupported program type: " + programType);
        }
        if (mProgramHandle == 0) {
            throw new RuntimeException("Failed to create GL program");
        }
        initializeLocations();
    }

    private void initializeLocations() {
        maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
        muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
        muKernelLoc = GLES20.glGetUniformLocation(mProgramHandle, "uKernel");
        muTexOffsetLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexOffset");
        muColorAdjustLoc = GLES20.glGetUniformLocation(mProgramHandle, "uColorAdjust");
    }

    /**
     * Releases the GL program.
     */
    public void release() {
        Log.d(TAG, "Releasing program " + mProgramHandle);
        GLES20.glDeleteProgram(mProgramHandle);
    }

    /**
     * Creates and configures a texture object.
     *
     * @return The texture object ID, or 0 if creation failed.
     */
    public int createTextureObject() {
        final int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GlUtilNew.checkGlError("glGenTextures");
        final int texId = textures[0];
        if (texId == 0) {
            Log.e(TAG, "Failed to generate a texture object");
            return 0;
        }
        GLES20.glBindTexture(mTextureTarget, texId);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GlUtilNew.checkGlError("setTextureParameters");
        GLES20.glBindTexture(mTextureTarget, 0);
        return texId;
    }

    /**
     * Draws textured geometry using the supplied matrices, buffers, and texture.
     *
     * @param mvpMatrix      The model-view-projection matrix.
     * @param vertexBuffer   Buffer containing vertex coordinates.
     * @param firstVertex    First vertex index.
     * @param vertexCount    Number of vertices to draw.
     * @param coordsPerVertex Number of coordinates per vertex.
     * @param vertexStride   Stride in bytes between vertices.
     * @param texMatrix      Texture transformation matrix.
     * @param texBuffer      Buffer containing texture coordinates.
     * @param textureId      Texture object ID to bind.
     * @param texStride      Stride in bytes between texture coordinates.
     */
    public void draw(final float[] mvpMatrix, final FloatBuffer vertexBuffer, final int firstVertex,
                     final int vertexCount, final int coordsPerVertex, final int vertexStride,
                     final float[] texMatrix, final FloatBuffer texBuffer, final int textureId, final int texStride) {

        if (textureId <= 0) {
            Log.e(TAG, "draw: Invalid texture ID: " + textureId);
            return;
        }
        if (muMVPMatrixLoc < 0) {
            Log.e(TAG, "draw: Invalid MVP matrix location");
            return;
        }

        GLES20.glUseProgram(mProgramHandle);
        GLES20.glBindTexture(mTextureTarget, textureId);

        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0);
        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, 0);

        GLES20.glVertexAttribPointer(maPositionLoc, coordsPerVertex, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
        GLES20.glEnableVertexAttribArray(maPositionLoc);

        GLES20.glVertexAttribPointer(maTextureCoordLoc, 2, GLES20.GL_FLOAT, false, texStride, texBuffer);
        GLES20.glEnableVertexAttribArray(maTextureCoordLoc);

        if (muKernelLoc >= 0) {
            GLES20.glUniform1fv(muKernelLoc, KERNEL_SIZE, mKernel, 0);
            GLES20.glUniform2fv(muTexOffsetLoc, KERNEL_SIZE, mTexOffset, 0);
            GLES20.glUniform1f(muColorAdjustLoc, mColorAdjust);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount);
        GlUtilNew.checkGlError("glDrawArrays");

        GLES20.glDisableVertexAttribArray(maPositionLoc);
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc);

        GLES20.glBindTexture(mTextureTarget, 0);
        GLES20.glUseProgram(0);
    }
}
