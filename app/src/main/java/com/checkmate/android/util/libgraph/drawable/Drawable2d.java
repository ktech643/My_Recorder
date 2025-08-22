package com.checkmate.android.util.libgraph.drawable;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Immutable, shared vertex/UV buffers for common 2-D shapes.
 *
 *  • No reflection “cleaner” hacks (Android 14 blocks them)
 *  • No per-instance allocations in the hot path
 *  • Thread-safe: shared buffers are read-only
 */
public final class Drawable2d implements AutoCloseable {

    // ───── Public API ─────
    public enum Prefab { TRIANGLE, RECTANGLE, FULL_RECTANGLE }

    public FloatBuffer getVertexArray()   { return vertices;       }
    public FloatBuffer getTexCoordArray() { return uv;             }
    public int         getVertexCount()   { return vertexCount;    }
    public int         getVertexStride()  { return vertexStride;   }
    public int         getTexCoordStride(){ return uvStride;       }
    public int         getCoordsPerVertex(){ return COORDS_PER_VTX;}

    @Override public void close() { /* nothing to free */ }
    private static WeakReference<FloatBuffer> triangleBufRef;

    public static FloatBuffer getTriangleBuffer() {
        FloatBuffer buf = triangleBufRef != null ? triangleBufRef.get() : null;
        if (buf == null) {
            buf = asBuffer(TRIANGLE_COORDS);
            triangleBufRef = new WeakReference<>(buf);
        }
        return buf;
    }
    @NonNull
    @Override public String toString() {
        return "[Drawable2d " + prefab + "]";
    }

    // ───── Implementation ─────
    private static final int BYTES_PER_FLOAT = 4;
    private static final int COORDS_PER_VTX  = 2;

    private static FloatBuffer asBuffer(float[] src) {
        ByteBuffer bb = ByteBuffer
                .allocateDirect(src.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder());
        bb.asFloatBuffer().put(src).position(0);
        return bb.asFloatBuffer().asReadOnlyBuffer();
    }

    /* shape data (NDC) */
    private static final float[] TRIANGLE_COORDS = {
            0.0f,  0.577350269f,
            -0.5f, -0.288675135f,
            0.5f, -0.288675135f };

    private static final float[] TRIANGLE_UV = {
            0.5f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f };

    private static final float[] RECT_COORDS = {
            -0.5f, -0.5f,
            0.5f, -0.5f,
            -0.5f,  0.5f,
            0.5f,  0.5f };

    private static final float[] RECT_UV = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f };

    private static final float[] FULL_COORDS = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f,  1.0f,
            1.0f,  1.0f };

    private static final float[] FULL_UV = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f };

    // Shared, read-only buffers
    private static final FloatBuffer TRIANGLE_BUF   = asBuffer(TRIANGLE_COORDS);
    private static final FloatBuffer TRIANGLE_UVBUF = asBuffer(TRIANGLE_UV);
    private static final FloatBuffer RECT_BUF       = asBuffer(RECT_COORDS);
    private static final FloatBuffer RECT_UVBUF     = asBuffer(RECT_UV);
    private static final FloatBuffer FULL_BUF       = asBuffer(FULL_COORDS);
    private static final FloatBuffer FULL_UVBUF     = asBuffer(FULL_UV);

    // Instance fields
    private final FloatBuffer vertices;
    private final FloatBuffer uv;
    private final int vertexCount;
    private final int vertexStride;
    private final int uvStride;
    private final Prefab prefab;

    public Drawable2d(@NonNull Prefab prefab) {
        this.prefab = prefab;
        switch (prefab) {
            case TRIANGLE:
                vertices = TRIANGLE_BUF;
                uv       = TRIANGLE_UVBUF;
                break;
            case RECTANGLE:
                vertices = RECT_BUF;
                uv       = RECT_UVBUF;
                break;
            case FULL_RECTANGLE:
            default:
                vertices = FULL_BUF;
                uv       = FULL_UVBUF;
        }
        vertexCount  = vertices.capacity() / COORDS_PER_VTX;
        vertexStride = COORDS_PER_VTX * BYTES_PER_FLOAT;
        uvStride     = 2 * BYTES_PER_FLOAT;
    }
}
