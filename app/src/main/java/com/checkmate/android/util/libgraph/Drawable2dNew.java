package com.checkmate.android.util.libgraph;

import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Drawable2dNew implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(Drawable2dNew.class.getName());
    private static final int SIZEOF_FLOAT = 4;

    // Immutable coordinate arrays for shapes.
    private static final float[] TRIANGLE_COORDS = {
            0.0f,  0.577350269f,   // top
            -0.5f, -0.288675135f,  // bottom left
            0.5f, -0.288675135f    // bottom right
    };
    private static final float[] TRIANGLE_TEX_COORDS = {
            0.5f, 0.0f,   // top center
            0.0f, 1.0f,   // bottom left
            1.0f, 1.0f    // bottom right
    };

    private static final float[] RECTANGLE_COORDS = {
            -0.5f, -0.5f,  // bottom left
            0.5f, -0.5f,  // bottom right
            -0.5f,  0.5f,  // top left
            0.5f,  0.5f   // top right
    };
    private static final float[] RECTANGLE_TEX_COORDS = {
            0.0f, 1.0f,   // bottom left
            1.0f, 1.0f,   // bottom right
            0.0f, 0.0f,   // top left
            1.0f, 0.0f    // top right
    };

    private static final float[] FULL_RECTANGLE_COORDS = {
            -1.0f, -1.0f,  // bottom left
            1.0f, -1.0f,  // bottom right
            -1.0f,  1.0f,  // top left
            1.0f,  1.0f   // top right
    };
    private static final float[] FULL_RECTANGLE_TEX_COORDS = {
            0.0f, 0.0f,   // bottom left
            1.0f, 0.0f,   // bottom right
            0.0f, 1.0f,   // top left
            1.0f, 1.0f    // top right
    };

    // Create static FloatBuffers once; these buffers are immutable and shared.
    private static FloatBuffer TRIANGLE_BUF = GlUtilNew.createFloatBuffer(TRIANGLE_COORDS);
    private static FloatBuffer TRIANGLE_TEX_BUF = GlUtilNew.createFloatBuffer(TRIANGLE_TEX_COORDS);
    private static FloatBuffer RECTANGLE_BUF = GlUtilNew.createFloatBuffer(RECTANGLE_COORDS);
    private static FloatBuffer RECTANGLE_TEX_BUF = GlUtilNew.createFloatBuffer(RECTANGLE_TEX_COORDS);
    private static FloatBuffer FULL_RECTANGLE_BUF = GlUtilNew.createFloatBuffer(FULL_RECTANGLE_COORDS);
    private static FloatBuffer FULL_RECTANGLE_TEX_BUF = GlUtilNew.createFloatBuffer(FULL_RECTANGLE_TEX_COORDS);

    // Instance-level buffers and properties.
    private FloatBuffer mVertexArray;
    private FloatBuffer mTexCoordArray;
    private int mVertexCount;
    private int mCoordsPerVertex;
    private int mVertexStride;
    private int mTexCoordStride;
    private Prefab mPrefab;

    /**
     * Enum defining pre-fabricated shapes.
     */
    public enum Prefab {
        TRIANGLE, RECTANGLE, FULL_RECTANGLE
    }

    /**
     * Constructs a drawable for the specified shape.
     *
     * @param shape The pre-fabricated shape to use.
     * @throws IllegalArgumentException if shape is null or unknown.
     */
    public Drawable2dNew(final Prefab shape) {
        if (shape == null) {
            throw new IllegalArgumentException("Shape cannot be null");
        }
        mCoordsPerVertex = 2;
        mVertexStride = mCoordsPerVertex * SIZEOF_FLOAT;
        mTexCoordStride = 2 * SIZEOF_FLOAT;
        switch (shape) {
            case TRIANGLE:
                mVertexArray = TRIANGLE_BUF;
                mTexCoordArray = TRIANGLE_TEX_BUF;
                mVertexCount = TRIANGLE_COORDS.length / mCoordsPerVertex;
                break;
            case RECTANGLE:
                mVertexArray = RECTANGLE_BUF;
                mTexCoordArray = RECTANGLE_TEX_BUF;
                mVertexCount = RECTANGLE_COORDS.length / mCoordsPerVertex;
                break;
            case FULL_RECTANGLE:
                mVertexArray = FULL_RECTANGLE_BUF;
                mTexCoordArray = FULL_RECTANGLE_TEX_BUF;
                mVertexCount = FULL_RECTANGLE_COORDS.length / mCoordsPerVertex;
                break;
            default:
                throw new IllegalArgumentException("Unknown shape: " + shape);
        }
        mPrefab = shape;
    }

    /**
     * Returns the vertex data.
     * The returned FloatBuffer should not be modified.
     *
     * @return The vertex array.
     */
    public FloatBuffer getVertexArray() {
        return mVertexArray;
    }

    /**
     * Returns the texture coordinate data.
     * The returned FloatBuffer should not be modified.
     *
     * @return The texture coordinate array.
     */
    public FloatBuffer getTexCoordArray() {
        return mTexCoordArray;
    }

    /**
     * Returns the number of vertices.
     *
     * @return The vertex count.
     */
    public int getVertexCount() {
        return mVertexCount;
    }

    /**
     * Returns the vertex stride in bytes.
     *
     * @return The stride for each vertex.
     */
    public int getVertexStride() {
        return mVertexStride;
    }

    /**
     * Returns the texture coordinate stride in bytes.
     *
     * @return The stride for each texture coordinate.
     */
    public int getTexCoordStride() {
        return mTexCoordStride;
    }

    /**
     * Returns the number of coordinates per vertex.
     *
     * @return Typically 2 (x and y).
     */
    public int getCoordsPerVertex() {
        return mCoordsPerVertex;
    }

    @Override
    public String toString() {
        return "[Drawable2d: " + (mPrefab != null ? mPrefab : "undefined") + "]";
    }

    /**
     * Clears instance-level resources.
     * For static resources, use {@link #clearStaticResources()}.
     */
    public void clearInstanceResources() {
        mVertexArray = null;
        mTexCoordArray = null;
        mPrefab = null;
        mVertexCount = 0;
        mCoordsPerVertex = 0;
        mVertexStride = 0;
        mTexCoordStride = 0;
    }

    /**
     * Releases static buffers and associated resources.
     * <p>
     * Note: Call this only when you are sure that no instances are using these buffers.
     * </p>
     */
    public static void clearStaticResources() {
        releaseBuffer(TRIANGLE_BUF);
        releaseBuffer(TRIANGLE_TEX_BUF);
        releaseBuffer(RECTANGLE_BUF);
        releaseBuffer(RECTANGLE_TEX_BUF);
        releaseBuffer(FULL_RECTANGLE_BUF);
        releaseBuffer(FULL_RECTANGLE_TEX_BUF);
        TRIANGLE_BUF = null;
        TRIANGLE_TEX_BUF = null;
        RECTANGLE_BUF = null;
        RECTANGLE_TEX_BUF = null;
        FULL_RECTANGLE_BUF = null;
        FULL_RECTANGLE_TEX_BUF = null;
    }

    /**
     * Attempts to release a direct buffer's native memory using reflection.
     *
     * @param buffer The buffer to release.
     */
    private static void releaseBuffer(Buffer buffer) {
        if (buffer == null || !buffer.isDirect()) {
            return;
        }
        try {
            Method cleanerMethod = buffer.getClass().getMethod("cleaner");
            cleanerMethod.setAccessible(true);
            Object cleaner = cleanerMethod.invoke(buffer);
            if (cleaner != null) {
                Method cleanMethod = cleaner.getClass().getMethod("clean");
                cleanMethod.invoke(cleaner);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to release buffer", e);
        }
    }

    /**
     * Implements AutoCloseable. Closes instance-level resources.
     */
    @Override
    public void close() {
        clearInstanceResources();
    }
}
