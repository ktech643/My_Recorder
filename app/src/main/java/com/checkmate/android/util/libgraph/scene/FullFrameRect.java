package com.checkmate.android.util.libgraph.scene;

import androidx.annotation.NonNull;

import com.checkmate.android.util.libgraph.drawable.Drawable2d;
import com.checkmate.android.util.libgraph.gl.GlUtil;
import com.checkmate.android.util.libgraph.program.Texture2dProgram;

/**
 * Draws a fullscreen quad textured with the supplied sampler.
 *
 * Ownership of the {@link Texture2dProgram} is transferred to this
 * object. Call {@link #close()} when finished.
 */
public class FullFrameRect implements AutoCloseable {

    /** Immutable full-viewport quad. */
    protected final Drawable2d quad =
            new Drawable2d(Drawable2d.Prefab.FULL_RECTANGLE);

    protected Texture2dProgram program;

    public FullFrameRect(@NonNull Texture2dProgram prog) { program = prog; }

    /** Replace the shader (old one is deleted). EGL context must be current. */
    public void changeProgram(@NonNull Texture2dProgram newProg) {
        program.close();
        program = newProg;
    }

    /** Create a GL texture compatible with this program’s sampler type. */
    public int createTextureObject() { return program.createTextureObject(); }

    /** Draw the quad covering the viewport. */
    public void drawFrame(int textureId, float[] texMatrix) {
        program.draw(GlUtil.IDENTITY,
                quad.getVertexArray(), 0, quad.getVertexCount(),
                quad.getCoordsPerVertex(), quad.getVertexStride(),
                texMatrix,
                quad.getTexCoordArray(), textureId, quad.getTexCoordStride());
    }

    @Override public void close() { program.close(); }
}
