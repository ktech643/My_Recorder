package com.checkmate.android.util.libgraph.scene;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import androidx.annotation.NonNull;

import com.checkmate.android.util.libgraph.drawable.Drawable2d;
import com.checkmate.android.util.libgraph.gl.GlUtil;
import com.checkmate.android.util.libgraph.program.Texture2dProgram;

/**
 * Transformable textured sprite (position, scale, rotation).
 */
public final class Sprite2d {

    private static final String TAG = "Sprite2d";

    private final Drawable2d shape;
    private int texture = -1;

    private float x, y;
    private float sx = 1, sy = 1;
    private float angleDeg;

    private final float[] model = new float[16];
    private final float[] mvp   = new float[16];

    public Sprite2d(@NonNull Drawable2d shape) { this.shape = shape; }

    // — setters —
    public void setPosition(float px, float py) { x = px; y = py; }
    public void setScale(float sX, float sY)    { sx = sX; sy = sY; }
    public void setRotation(float deg)          { angleDeg = deg; }
    public void setTexture(int tex)             { texture = tex; }

    // — draw —
    public void draw(@NonNull Texture2dProgram prog, float[] projection) {
        if (texture <= 0) {
            Log.w(TAG, "draw called with invalid texture");
            return;
        }
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, x, y, 0);
        Matrix.rotateM  (model, 0, angleDeg, 0, 0, 1);
        Matrix.scaleM   (model, 0, sx, sy, 1);
        Matrix.multiplyMM(mvp, 0, projection, 0, model, 0);

        prog.draw(mvp,
                shape.getVertexArray(), 0, shape.getVertexCount(),
                shape.getCoordsPerVertex(), shape.getVertexStride(),
                GlUtil.IDENTITY,
                shape.getTexCoordArray(), texture, shape.getTexCoordStride());
    }

    /** Free the GL texture (call from GL thread). */
    public void releaseTexture() {
        if (texture > 0) {
            GLES20.glDeleteTextures(1, new int[]{texture}, 0);
            texture = -1;
        }
    }
}
