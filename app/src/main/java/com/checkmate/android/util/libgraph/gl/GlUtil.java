package com.checkmate.android.util.libgraph.gl;

import android.graphics.Bitmap;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.checkmate.android.BuildConfig;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Small grab-bag of static helpers for OpenGL ES 2/3.
 *
 *  ▸ No hidden-API reflection
 *  ▸ Debug-only {@link #check(String)} (stripped in release by R8)
 *  ▸ Convenience texture/shader/program builders
 */
public final class GlUtil {

    // --------------------------------------------------------------------- //
    // Public constants
    // --------------------------------------------------------------------- //
    public static final String TAG = "GlUtil";

    /** Identity matrix (do not modify). */
    public static final float[] IDENTITY;
    static {
        IDENTITY = new float[16];
        Matrix.setIdentityM(IDENTITY, 0);
    }

    // --------------------------------------------------------------------- //
    private static final int BYTES_PER_FLOAT = 4;
    private GlUtil() {}                    // utility class

    // --------------------------------------------------------------------- //
    // Shader / program
    // --------------------------------------------------------------------- //
    public static int loadShader(int type, String src) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, src);
        GLES20.glCompileShader(s);
        int[] ok = new int[1];
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) {
            Log.e(TAG, "Shader err: " + GLES20.glGetShaderInfoLog(s));
            GLES20.glDeleteShader(s);
            return 0;
        }
        return s;
    }
    public static int createProgram(String vs, String fs) {
        int v = loadShader(GLES20.GL_VERTEX_SHADER,   vs);
        int f = loadShader(GLES20.GL_FRAGMENT_SHADER, fs);
        if (v == 0 || f == 0) return 0;

        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, v);
        GLES20.glAttachShader(p, f);
        GLES20.glLinkProgram(p);

        int[] ok = new int[1];
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0);
        if (ok[0] == 0) {
            Log.e(TAG, "Program link err: " + GLES20.glGetProgramInfoLog(p));
            GLES20.glDeleteProgram(p);
            if (BuildConfig.DEBUG) {
                throw new RuntimeException("Program link err: " + GLES20.glGetProgramInfoLog(p));
            } else {
                return 0;
            }

        }
        // shaders can be deleted after linking
        GLES20.glDeleteShader(v);
        GLES20.glDeleteShader(f);
        return p;
    }

    // --------------------------------------------------------------------- //
    // Textures
    // --------------------------------------------------------------------- //
    public static int createTexture2D() {
        int[] ids = new int[1];
        GLES20.glGenTextures(1, ids, 0);
        int tex = ids[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return tex;
    }

    /** Convenience: load a Bitmap into a freshly-generated GL_TEXTURE_2D. */
    public static int loadTexture(Bitmap bmp) {
        int tex = createTexture2D();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex);
//        GLES20.glEnable(GLES20.GL_BLEND);
//        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return tex;
    }

    // --------------------------------------------------------------------- //
    // Buffers
    // --------------------------------------------------------------------- //
    public static FloatBuffer toFloatBuffer(float[] src) {
        ByteBuffer bb = ByteBuffer
                .allocateDirect(src.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder());
        bb.asFloatBuffer().put(src).position(0);
        return bb.asFloatBuffer();
    }

    // --------------------------------------------------------------------- //
    // Debug helpers
    // --------------------------------------------------------------------- //
    public static void check(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            String msg = op + " → GL error 0x" + Integer.toHexString(error);
            if (BuildConfig.DEBUG) {
                throw new RuntimeException(msg);
            } else {
                Log.e(TAG, msg);
            }
        }
    }

    public static boolean isContextCurrent() {
        return EGL14.eglGetCurrentContext() != EGL14.EGL_NO_CONTEXT;
    }
    /** Log vendor/renderer/version once for diagnostics. */
    public static void logVersionInfo() {
        Log.i(TAG, "vendor  : " + GLES20.glGetString(GLES20.GL_VENDOR));
        Log.i(TAG, "renderer: " + GLES20.glGetString(GLES20.GL_RENDERER));
        Log.i(TAG, "version : " + GLES20.glGetString(GLES20.GL_VERSION));
        try {
            int[] v = new int[1];
            GLES30.glGetIntegerv(GLES30.GL_MAJOR_VERSION, v, 0);
            int major = v[0];
            GLES30.glGetIntegerv(GLES30.GL_MINOR_VERSION, v, 0);
            int minor = v[0];
            Log.i(TAG, "GLES3 version: " + major + "." + minor);
        } catch (Throwable ignore) { /* GLES30 not present */ }
    }


}
