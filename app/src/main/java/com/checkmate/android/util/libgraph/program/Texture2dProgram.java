package com.checkmate.android.util.libgraph.program;

import android.annotation.TargetApi;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.checkmate.android.BuildConfig;
import com.checkmate.android.util.libgraph.gl.GlUtil;

/**
 * GLES 2 (with OES-external support) texture shader.
 * Supports:
 *   • 2-D sampler (GL_TEXTURE_2D)
 *   • OES external sampler (camera) – normal / black-white
 */
public final class Texture2dProgram implements AutoCloseable {

    private static final String TAG = "Texture2dProgram";
    private static int currentProgram = -1;
    private static int currentTexture = -1;
    // Static variables to track GL state
    private static int currentTexTarget = -1;
    // ----------------------------------------------------------------- //
    public enum Type { TEXTURE_2D, TEXTURE_EXT, TEXTURE_EXT_BW }

    private static final String VERT =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uTexMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "void main(){\n" +
                    "  gl_Position = uMVPMatrix * aPosition;\n" +
                    "  vTexCoord   = (uTexMatrix * aTextureCoord).xy;\n" +
                    "}";

    private static final String FRAG_2D =
            "precision mediump float;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "uniform sampler2D sTex;\n" +
                    "void main(){ gl_FragColor = texture2D(sTex, vTexCoord); }";

    private static final String FRAG_EXT =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "uniform samplerExternalOES sTex;\n" +
                    "void main(){ gl_FragColor = texture2D(sTex, vTexCoord); }";

    private static final String FRAG_EXT_BW =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "uniform samplerExternalOES sTex;\n" +
                    "void main(){\n" +
                    "  vec4 c = texture2D(sTex, vTexCoord);\n" +
                    "  float g = dot(c.rgb, vec3(0.299,0.587,0.114));\n" +
                    "  gl_FragColor = vec4(g,g,g,1.0);\n" +
                    "}";

    // ----------------------------------------------------------------- //
    private final int texTarget;
    private final int program;
    private final int maPos, maUv;
    private final int muMVP, muTex;

    public Texture2dProgram(@NonNull Type type) {
        this.program = GlUtil.createProgram(VERT,
                type == Type.TEXTURE_2D ? FRAG_2D :
                        type == Type.TEXTURE_EXT ? FRAG_EXT : FRAG_EXT_BW);

        this.texTarget = (type == Type.TEXTURE_2D)
                ? GLES20.GL_TEXTURE_2D
                : GLES11Ext.GL_TEXTURE_EXTERNAL_OES;

        maPos  = GLES20.glGetAttribLocation (program, "aPosition");
        maUv   = GLES20.glGetAttribLocation (program, "aTextureCoord");
        muMVP  = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        muTex  = GLES20.glGetUniformLocation(program, "uTexMatrix");
    }

    public int createTextureObject() {
        int[] id = new int[1];
        GLES20.glGenTextures(1, id, 0);
        GLES20.glBindTexture(texTarget, id[0]);
        GLES20.glTexParameteri(texTarget, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(texTarget, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(texTarget, GLES20.GL_TEXTURE_WRAP_S,  GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(texTarget, GLES20.GL_TEXTURE_WRAP_T,  GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(texTarget, 0);
        return id[0];
    }

    /** One-shot textured draw. */
    public void draw(@NonNull float[] mvp,
                     @NonNull java.nio.FloatBuffer vtx,
                     int first,
                     int count,
                     int coordsPerVtx,
                     int vtxStride,
                     @NonNull float[] texMat,
                     @NonNull java.nio.FloatBuffer uvBuf,
                     int texId,
                     int uvStride) {

        if (texId <= 0) {
            Log.e(TAG, "draw: invalid tex id");
            return;
        }
        try {
            // Program state management
            if (currentProgram != program) {
                currentProgram = program;
            }
            GLES20.glUseProgram(program); // No static tracking
            // Uniform updates
            GLES20.glUniformMatrix4fv(muMVP, 1, false, mvp, 0);
            GLES20.glUniformMatrix4fv(muTex, 1, false, texMat, 0);
            // Texture state management
            if (currentTexture != texId || currentTexTarget != texTarget) {
                currentTexture = texId;
                currentTexTarget = texTarget;
            }
            GLES20.glBindTexture(texTarget, texId);
            // Vertex attribute management
            GLES20.glEnableVertexAttribArray(maPos);
            GLES20.glVertexAttribPointer(maPos, coordsPerVtx,
                    GLES20.GL_FLOAT, false, vtxStride, vtx);
            GLES20.glEnableVertexAttribArray(maUv);
            GLES20.glVertexAttribPointer(maUv, 2,
                    GLES20.GL_FLOAT, false, uvStride, uvBuf);
            // Draw call
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, first, count);
        } finally {
            // Cleanup while preserving external state
            GLES20.glDisableVertexAttribArray(maPos);
            GLES20.glDisableVertexAttribArray(maUv);

            // Only reset state if we changed it
            if (currentProgram != 0) {
                GLES20.glUseProgram(0);
                currentProgram = 0;
            }
            if (currentTexture != 0) {
                GLES20.glBindTexture(currentTexTarget, 0);
                currentTexture = 0;
                currentTexTarget = -1;
            }
        }

    }

    @Override public void close() {
        GLES20.glDeleteProgram(program);
    }
}
