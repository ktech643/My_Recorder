package com.checkmate.android.util.libgraph;

import android.opengl.GLES20;
import java.nio.FloatBuffer;

/**
 * GL program and supporting functions for flat-shaded rendering.
 */
public final class FlatShadedProgramNew implements AutoCloseable {
    private static final String TAG = GlUtilNew.TAG;

    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 aPosition;" +
                    "void main() {" +
                    "    gl_Position = uMVPMatrix * aPosition;" +
                    "}";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;" +
                    "uniform vec4 uColor;" +
                    "void main() {" +
                    "    gl_FragColor = uColor;" +
                    "}";

    // Handle to the GL program and various components.
    private int mProgramHandle = 0;
    private int muColorLoc = -1;
    private int muMVPMatrixLoc = -1;
    private int maPositionLoc = -1;

    /**
     * Prepares the program in the current EGL context.
     */
    public FlatShadedProgramNew() {
        mProgramHandle = GlUtilNew.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        if (mProgramHandle == 0) {
            throw new RuntimeException("Unable to create program");
        }

        // Get locations of attributes and uniforms.
        maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        GlUtilNew.checkLocation(maPositionLoc, "aPosition");
        muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        GlUtilNew.checkLocation(muMVPMatrixLoc, "uMVPMatrix");
        muColorLoc = GLES20.glGetUniformLocation(mProgramHandle, "uColor");
        GlUtilNew.checkLocation(muColorLoc, "uColor");
    }

    /**
     * Releases the program and any associated resources.
     */
    public void release() {
        if (mProgramHandle != 0) {
            GLES20.glDeleteProgram(mProgramHandle);
            mProgramHandle = 0;
        }
    }

    @Override
    public void close() {
        release();
    }

    /**
     * Issues the draw call. Performs full setup on every call.
     *
     * @param mvpMatrix       The model/view/projection matrix (must not be null).
     * @param color           The color vector (must not be null).
     * @param vertexBuffer    The vertex buffer (must not be null).
     * @param firstVertex     The first vertex index.
     * @param vertexCount     The number of vertices to draw.
     * @param coordsPerVertex The number of coordinates per vertex.
     * @param vertexStride    The stride (in bytes) between vertices.
     */
    public void draw(final float[] mvpMatrix, final float[] color, final FloatBuffer vertexBuffer,
                     final int firstVertex, final int vertexCount,
                     final int coordsPerVertex, final int vertexStride) {
        if (mvpMatrix == null) {
            throw new IllegalArgumentException("mvpMatrix cannot be null");
        }
        if (color == null) {
            throw new IllegalArgumentException("color cannot be null");
        }
        if (vertexBuffer == null) {
            throw new IllegalArgumentException("vertexBuffer cannot be null");
        }

        GlUtilNew.checkGlError("draw start");

        // Use the program.
        GLES20.glUseProgram(mProgramHandle);
        GlUtilNew.checkGlError("glUseProgram");

        // Set the MVP matrix.
        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0);
        GlUtilNew.checkGlError("glUniformMatrix4fv");

        // Set the color.
        GLES20.glUniform4fv(muColorLoc, 1, color, 0);
        GlUtilNew.checkGlError("glUniform4fv");

        try {
            // Enable and set up the vertex attribute.
            GLES20.glEnableVertexAttribArray(maPositionLoc);
            GlUtilNew.checkGlError("glEnableVertexAttribArray");
            GLES20.glVertexAttribPointer(maPositionLoc, coordsPerVertex,
                    GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
            GlUtilNew.checkGlError("glVertexAttribPointer");

            // Draw the shape.
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount);
            GlUtilNew.checkGlError("glDrawArrays");
        } finally {
            // Ensure cleanup regardless of errors.
            GLES20.glDisableVertexAttribArray(maPositionLoc);
            GLES20.glUseProgram(0);
        }
    }
}
