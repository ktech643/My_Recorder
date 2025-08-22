package com.checkmate.android.util.libgraph;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Some OpenGL utility functions.
 */
public final class GlUtilNew {
    public static final String TAG = "GLUtilNew";

    /** Identity matrix for general use. Don't modify or life will get weird. */
    public static final float[] IDENTITY_MATRIX;
    static {
        IDENTITY_MATRIX = new float[16];
        Matrix.setIdentityM(IDENTITY_MATRIX, 0);
    }

    private static final int SIZEOF_FLOAT = 4;

    // Private constructor to prevent instantiation.
    private GlUtilNew() {}

    /**
     * Creates a new OpenGL program from vertex and fragment shader source code.
     *
     * @param vertexSource   The source code for the vertex shader.
     * @param fragmentSource The source code for the fragment shader.
     * @return A handle to the program, or 0 on failure.
     */
    public static int createProgram(final String vertexSource, final String fragmentSource) {
        final int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        final int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (fragmentShader == 0) {
            GLES20.glDeleteShader(vertexShader);
            return 0;
        }

        int program = GLES20.glCreateProgram();
        checkGlError("glCreateProgram");
        if (program == 0) {
            Log.e(TAG, "Could not create program");
            GLES20.glDeleteShader(vertexShader);
            GLES20.glDeleteShader(fragmentShader);
            return 0;
        }
        GLES20.glAttachShader(program, vertexShader);
        checkGlError("glAttachShader (vertex)");
        GLES20.glAttachShader(program, fragmentShader);
        checkGlError("glAttachShader (fragment)");
        GLES20.glLinkProgram(program);

        final int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Could not link program:");
            Log.e(TAG, GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        // Detach and delete shaders after successful link.
        GLES20.glDetachShader(program, vertexShader);
        GLES20.glDetachShader(program, fragmentShader);
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);
        return program;
    }

    /**
     * Compiles a shader from the given source code.
     *
     * @param shaderType The type of shader (GLES20.GL_VERTEX_SHADER or GLES20.GL_FRAGMENT_SHADER).
     * @param source     The shader source code.
     * @return A handle to the shader, or 0 on failure.
     */
    public static int loadShader(final int shaderType, final String source) {
        int shader = GLES20.glCreateShader(shaderType);
        checkGlError("glCreateShader type=" + shaderType);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        final int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader " + shaderType + ":");
            Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    /**
     * Checks for OpenGL errors and logs them.
     *
     * @param op The operation after which to check for errors.
     */
    public static void checkGlError(final String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            Log.e(TAG, msg);
            // Optionally, throw an exception here in debug builds.
            // throw new RuntimeException(msg);
        }
    }

    /**
     * Checks if the given attribute or uniform location is valid.
     *
     * @param location The location obtained from glGetAttribLocation or glGetUniformLocation.
     * @param label    The label of the attribute or uniform.
     * @throws RuntimeException if the location is invalid.
     */
    public static void checkLocation(final int location, final String label) {
        if (location < 0) {
            throw new RuntimeException("Unable to locate '" + label + "' in program");
        }
    }

    /**
     * Creates a texture from raw image data.
     *
     * @param data   The image data in a direct ByteBuffer.
     * @param width  The texture width in pixels.
     * @param height The texture height in pixels.
     * @param format The image data format (e.g., GLES20.GL_RGBA).
     * @return A handle to the texture.
     */
    public static int createImageTexture(final ByteBuffer data, final int width, final int height, final int format) {
        final int[] textureHandles = new int[1];
        GLES20.glGenTextures(1, textureHandles, 0);
        final int textureHandle = textureHandles[0];
        checkGlError("glGenTextures");

        // Bind the texture to the 2D target.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);

        // Set texture filtering parameters.
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        checkGlError("setTextureParameters");

        // Load the texture data.
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, width, height, 0, format, GLES20.GL_UNSIGNED_BYTE, data);
        checkGlError("glTexImage2D");
        return textureHandle;
    }

    /**
     * Creates a texture from a Bitmap.
     *
     * @param image The Bitmap image.
     * @return A handle to the texture.
     */
    public static int createImageTexture(final Bitmap image) {
        final int[] textureHandles = new int[1];
        GLES20.glGenTextures(1, textureHandles, 0);
        final int textureHandle = textureHandles[0];
        checkGlError("glGenTextures");

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);

        // Enable blending for transparent textures.
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Set texture filtering parameters.
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        checkGlError("setTextureParameters");

        // Load the Bitmap data into the texture.
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, image, 0);
        checkGlError("texImage2D");
        return textureHandle;
    }

    /**
     * Allocates a direct FloatBuffer and populates it with the provided float array data.
     *
     * @param coords The float array containing the data.
     * @return A FloatBuffer containing the data.
     */
    public static FloatBuffer createFloatBuffer(final float[] coords) {
        final ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * SIZEOF_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        final FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }

    /**
     * Logs OpenGL version and vendor information.
     */
    public static void logVersionInfo() {
        Log.i(TAG, "vendor  : " + GLES20.glGetString(GLES20.GL_VENDOR));
        Log.i(TAG, "renderer: " + GLES20.glGetString(GLES20.GL_RENDERER));
        Log.i(TAG, "version : " + GLES20.glGetString(GLES20.GL_VERSION));

        // Optionally log GLES30 version info if available.
        try {
            final int[] values = new int[1];
            GLES30.glGetIntegerv(GLES30.GL_MAJOR_VERSION, values, 0);
            final int majorVersion = values[0];
            GLES30.glGetIntegerv(GLES30.GL_MINOR_VERSION, values, 0);
            final int minorVersion = values[0];
            if (GLES30.glGetError() == GLES30.GL_NO_ERROR) {
                Log.i(TAG, "GLES30 version: " + majorVersion + "." + minorVersion);
            }
        } catch (Exception e) {
            Log.w(TAG, "GLES30 not available", e);
        }
    }
}
