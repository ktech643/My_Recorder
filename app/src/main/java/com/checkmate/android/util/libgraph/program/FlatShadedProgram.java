package com.checkmate.android.util.libgraph.program;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.checkmate.android.util.libgraph.gl.GlUtil;

/**
 * Very small GLES 2 program that renders flat-shaded geometry
 * (no texture sampling, just a solid color).
 *
 * Hot-path allocations: 0
 */
public final class FlatShadedProgram implements AutoCloseable {

    private static final String VERT =
            "uniform mat4 uMVPMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "void main(){\n" +
                    "  gl_Position = uMVPMatrix * aPosition;\n" +
                    "}";

    private static final String FRAG =
            "precision mediump float;\n" +
                    "uniform vec4 uColor;\n" +
                    "void main(){\n" +
                    "  gl_FragColor = uColor;\n" +
                    "}";

    // handles
    private final int program;
    private final int muMVP;
    private final int muColor;
    private final int maPosition;

    public FlatShadedProgram() {
        program     = GlUtil.createProgram(VERT, FRAG);
        maPosition  = GLES20.glGetAttribLocation (program, "aPosition");
        muMVP       = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        muColor     = GLES20.glGetUniformLocation(program, "uColor");
    }

    /** One-shot draw (enable/disable attributes internally). */
    public void draw(@NonNull float[] mvp,
                     @NonNull java.nio.FloatBuffer vertices,
                     int firstVertex,
                     int vertexCount,
                     int coordsPerVtx,
                     int vertexStride,
                     @NonNull float[] rgba) {

        GLES20.glUseProgram(program);

        GLES20.glUniformMatrix4fv(muMVP, 1, false, mvp, 0);
        GLES20.glUniform4fv(muColor, 1, rgba, 0);

        GLES20.glEnableVertexAttribArray(maPosition);
        GLES20.glVertexAttribPointer(maPosition, coordsPerVtx,
                GLES20.GL_FLOAT, false, vertexStride, vertices);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount);

        GLES20.glDisableVertexAttribArray(maPosition);
        GLES20.glUseProgram(0);
    }

    @Override public void close() { GLES20.glDeleteProgram(program); }
}
