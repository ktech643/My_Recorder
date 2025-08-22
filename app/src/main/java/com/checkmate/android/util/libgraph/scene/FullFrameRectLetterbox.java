package com.checkmate.android.util.libgraph.scene;

import android.opengl.Matrix;

import com.checkmate.android.util.libgraph.program.Texture2dProgram;

/**
 * Adds rotation / mirroring / axis-scaling helpers to {@link FullFrameRect}.
 */
public final class FullFrameRectLetterbox extends FullFrameRect {

    private final float[] scratch = new float[16];

    public FullFrameRectLetterbox(Texture2dProgram prog) { super(prog); }

    /** Y-axis letterbox (scale Y, optional rotation). */
    public void drawLetterboxY(int texId, float[] texMat, int rotationDeg, float scaleY) {
        Matrix.setIdentityM(scratch, 0);
        if (rotationDeg != 0) Matrix.rotateM(scratch, 0, rotationDeg, 0, 0, 1);
        if (scaleY != 1f)     Matrix.scaleM (scratch, 0, 1f, scaleY, 1f);
        program.draw(scratch,
                quad.getVertexArray(), 0, quad.getVertexCount(),
                quad.getCoordsPerVertex(), quad.getVertexStride(),
                texMat,
                quad.getTexCoordArray(), texId, quad.getTexCoordStride());
    }

    /** Mirror + rotation (common for selfie camera). */
    public void drawMirror(int texId, float[] texMat, int rotationDeg) {
        Matrix.setIdentityM(scratch, 0);
        Matrix.rotateM(scratch, 0, rotationDeg, 0, 0, 1);
        Matrix.scaleM (scratch, 0, -1f, 1f, 1f);
        program.draw(scratch,
                quad.getVertexArray(), 0, quad.getVertexCount(),
                quad.getCoordsPerVertex(), quad.getVertexStride(),
                texMat,
                quad.getTexCoordArray(), texId, quad.getTexCoordStride());
    }
}
