package com.checkmate.android.util.libgraph;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

public class SurfaceImageNew {

    /* ---------- state ---------- */
    private Bitmap              mBitmap;
    private Drawable2dNew       mRectDrawable;
    private Sprite2dNew         mSprite;
    private Texture2dProgramNew mImageTp;
    private int                 mTextureId = -1;

    /* user-controlled parameters */
    private float mScale  = 0.10f;      // fraction of shorter screen dimension
    private int   mOffPxX = 0;          // pixel margins from left / top
    private int   mOffPxY = 0;

    /* cached data */
    private final float[] mOrtho = new float[16];
    private boolean       mBitmapUpdated = false;
    private static final int MIN_SPRITE_PX = 11;   // tweak if you like
    /* ---------- public API ---------- */
    public SurfaceImageNew() {}

    /** Replace the bitmap (FROM GL THREAD) */
    public void setImage(Bitmap bmp) {
        deleteTexture();
        mBitmap         = bmp;
        mBitmapUpdated  = true;
    }

    /** Scale as fraction of frame (0 … 1). */
    public void setSize(float frac)   { mScale = frac; }

    /** Absolute pixel margins from top-left. */
    public void setPosition(int px, int py) {
        mOffPxX = Math.max(0, px);
        mOffPxY = Math.max(0, py);
    }

    /** Deprecated: kept for source-compatibility (fractional offsets) */
    public void setPos(float fx, float fy) {
        // preserve old semantics: -0.05 meant “5 % inside”; convert to px later
        mOffPxX = (int) fx;
        mOffPxY = (int) fy;
    }

    /* ---------- drawing entry-points ---------- */

    public void draw(int w, int h) {
        drawInternal(w, h, 1f, 1f);
    }

    /** Main path used by Encoder / Recorder surfaces (letter-box aware). */
    public void draw(int w, int h, float aspectX, float aspectY) {
        drawInternal(w, h, aspectX, aspectY);
    }

    /** USB-cam & screencast now forward to the common code. */
    public void drawUSB   (int w,int h) { drawInternal(w,h,1f,1f); }
    public void drawScreenCast(int w,int h){ drawInternal(w,h,1f,1f); }

    /* ---------- core renderer ---------- */

    private static final float MIN_SCALE = 1f;   // never shrink below 1 : 1

    private void drawInternal(int frameW, int frameH,
                              float aspectX, float aspectY) {
        if (mBitmap == null) return;
        ensureGlObjects();

        /* ---------- sprite size ------------------------------------ */
        int availW = Math.round(frameW * aspectX);
        int availH = Math.round(frameH * aspectY);
        float bmpAspect = (float) mBitmap.getWidth() / mBitmap.getHeight();

        float spriteH = Math.min(availH * mScale,
                availW / bmpAspect * mScale);
        float spriteW = spriteH * bmpAspect;

        /* ---------- NEW: never let GPU down-scale the bitmap -------- */
        float scaleClamp = Math.max(MIN_SCALE,
                (float) mBitmap.getHeight() / spriteH);
        spriteW *= scaleClamp;
        spriteH *= scaleClamp;

        /* ---------- position & draw -------------------------------- */
        float left = mOffPxX;
        float top  = frameH - spriteH - mOffPxY;

        mSprite.setPosition(left + spriteW * .5f,
                top  + spriteH * .5f);
        mSprite.setScale(spriteW, spriteH);

        Matrix.orthoM(mOrtho, 0, 0, frameW, 0, frameH, -1, 1);
        mSprite.draw(mImageTp, mOrtho);
    }


    /* ---------- helpers ---------- */

//    private void ensureGlObjects() {
//        if (mSprite == null) {
//            mRectDrawable = new Drawable2dNew(Drawable2dNew.Prefab.RECTANGLE);
//            mSprite       = new Sprite2dNew(mRectDrawable);
//            mImageTp      = new Texture2dProgramNew(
//                    Texture2dProgramNew.ProgramType.TEXTURE_2D);
//        }
//        if (mTextureId <= 0 || mBitmapUpdated) {
//            deleteTexture();
//            mTextureId = GlUtilNew.createImageTexture(mBitmap);
//            mSprite.setTexture(mTextureId);
//            mBitmapUpdated = false;
//        }
//    }

    /**
     * Draws the current bitmap (mBitmap) as a “timestamp” overlay strictly inside the camera‐feed region,
     * avoiding black letterbox areas. The caller must have already called setImage(...) to set mBitmap.
     *
     * @param frameWidth   total output width (e.g. encoder or recorder frame)
     * @param frameHeight  total output height
     * @param cameraWidth  raw camera texture width (srcW)
     * @param cameraHeight raw camera texture height (srcH)
     */
    public void drawTimeStampOnImage(int frameWidth, int frameHeight,
                                     int cameraWidth, int cameraHeight) {
        if (mBitmap == null || mBitmap.isRecycled()) return;
        ensureGlObjects();

        // (1) Compute orthographic matrix for full frame:
        float[] ortho = new float[16];
        android.opengl.Matrix.orthoM(ortho, 0,
                0, frameWidth,
                0, frameHeight,
                -1, 1);

        // (2) Figure out how the camera feed is letter-boxed (or pillar-boxed)
        //     inside the [frameWidth x frameHeight] output.
        float frameRatio = (float) frameWidth / frameHeight;
        float cameraRatio = (float) cameraWidth / cameraHeight;
        float displayedW, displayedH, offsetX = 0f, offsetY = 0f;

        if (cameraRatio > frameRatio) {
            // Camera is wider than output → pillar-box vertically
            displayedW = frameWidth;
            displayedH = displayedW / cameraRatio;
            offsetY = (frameHeight - displayedH) / 2f;
        } else {
            // Camera is taller (or equal) → letter-box horizontally
            displayedH = frameHeight;
            displayedW = displayedH * cameraRatio;
            offsetX = (frameWidth - displayedW) / 2f;
        }

        // (3) Determine how big the timestamp should be (relative to camera region).
        //     Here we allow the overlay to take up at most 30% of the camera feed’s width.
        float maxWidthFraction = 0.30f;  // tweak if you want smaller/larger text
        float marginPx        = 10f;    // 10px margin inside camera region
        float extraScale      = 1.10f;  // scale up ~10% if there’s room

        // Original bitmap dimensions:
        float bmpW = mBitmap.getWidth();
        float bmpH = mBitmap.getHeight();

        // Desired width (in pixels) inside the camera region:
        float desiredW = displayedW * maxWidthFraction * extraScale;
        // Compute a scale factor so that bmpW → desiredW
        float scaleFactor = desiredW / bmpW;

        // Compute the scaled height after that scale:
        float scaledH = bmpH * scaleFactor;
        float scaledW = bmpW * scaleFactor;

        // If the scaled height exceeds the camera region’s height, clamp it:
        if (scaledH > displayedH) {
            scaleFactor = displayedH / bmpH;
            scaledW = bmpW * scaleFactor;
            scaledH = bmpH * scaleFactor;
        }

        // (4) Find the “center” position so that the overlay sits marginPx down from the top‐left
        //     of the camera region. Because Sprite2d.setPosition(...) expects center‐of‐bitmap:
        float centerX = offsetX + marginPx + (scaledW  * 0.5f);
        float centerY = offsetY + displayedH - marginPx - (scaledH * 0.5f);

        // (5) Finally, draw the sprite at that size/position:
        mSprite.setPosition(centerX, centerY);
        mSprite.setScale(scaledW, scaledH);
        mSprite.draw(mImageTp, ortho);
    }

    /* … keep the existing fields … */

    /* ===== new PUBLIC method – always top-left of the full frame ===== */
    public void drawTimeStampOnFrame(int frameW, int frameH) {
        if (mBitmap == null || mBitmap.isRecycled()) return;
        ensureGlObjects();

        // ---------- how big?  —  8 % of frame-height  -----------------
        final float HEIGHT_FRAC = 0.08f;          // 8 %
        float bmpAspect = mBitmap.getWidth() / (float) mBitmap.getHeight();

        float spriteH = frameH * HEIGHT_FRAC;
        // never enlarge
        spriteH = Math.min(spriteH, mBitmap.getHeight());
        float spriteW = spriteH * bmpAspect;

        // ---------- where?  —  absolute top-left  ---------------------
        final int MARGIN_PX = 16;
        float cx = spriteW * .5f + MARGIN_PX;            // centre-x
        float cy = frameH - spriteH * .5f - MARGIN_PX;   // centre-y

        mSprite.setPosition(cx, cy);
        mSprite.setScale(spriteW, spriteH);

        // full-frame ortho
        Matrix.orthoM(mOrtho, 0, 0, frameW, 0, frameH, -1, 1);
        mSprite.draw(mImageTp, mOrtho);
    }

    /* -----------------------------------------------------------------
       Everything below is just the old code with TWO tiny modifications:
       - GL_LINEAR filtering (for crisp down-scaling)
       - bitmap creation: white bg + black text
       ----------------------------------------------------------------- */

    private void ensureGlObjects() {
        if (mSprite == null) {
            mRectDrawable = new Drawable2dNew(Drawable2dNew.Prefab.RECTANGLE);
            mSprite       = new Sprite2dNew(mRectDrawable);
            mImageTp      = new Texture2dProgramNew(
                    Texture2dProgramNew.ProgramType.TEXTURE_2D);
        }
        if (mTextureId <= 0 || mBitmapUpdated) {
            deleteTexture();
            mTextureId = GlUtilNew.createImageTexture(mBitmap);//createLinearTexture(mBitmap);   // <-- GL_LINEAR
            mSprite.setTexture(mTextureId);
            mBitmapUpdated = false;
        }
    }

    private int createLinearTexture(Bitmap bmp) {
        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        int id = tex[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
        return id;
    }

    private void deleteTexture() {
        if (mTextureId >= 0) {
            GLES20.glDeleteTextures(1, new int[]{mTextureId}, 0);
            mTextureId = -1;
        }
    }

    public void release() {
        deleteTexture();
        if (mBitmap != null) { mBitmap.recycle(); mBitmap = null; }
        if (mImageTp != null) { mImageTp.release(); mImageTp = null; }
        if (mSprite  != null) { mSprite.clearResources(); mSprite = null; }
        mRectDrawable = null;
    }

    public int getImageWidth()  { return mBitmap != null ? mBitmap.getWidth()  : 0; }
    public int getImageHeight() { return mBitmap != null ? mBitmap.getHeight() : 0; }

}
