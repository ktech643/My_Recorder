//package com.checkmate.android.util.libgraph;
//
//import android.graphics.Bitmap;
//import android.opengl.GLES20;
//import android.opengl.Matrix;
//import android.util.Log;
//
//
//import com.checkmate.android.AppPreference;
//
//public class SurfaceImageNew {
//    private Bitmap mBitmap;
//    private Drawable2dNew mRectDrawable;
//    private Sprite2dNew mSprite;
//    private Texture2dProgramNew mImageTp;
//    private int mTextureId;
//    private float mScale;
//    private float mPosX;
//    private float mPosY;
//    private boolean mBitmapUpdated = false;
//    private final float[] mOrthoMatrix = new float[16];
//
//    public SurfaceImageNew() {
//        mScale = 0.1f;
//        mPosX = -0.05f;
//        mPosY = -0.05f;
//        mTextureId = -1;
//    }
//
//    public void setImage(Bitmap image) {
//        if (mTextureId >= 0) {
//            int[] textures = new int[]{mTextureId};
//            GLES20.glDeleteTextures(1, textures, 0);
//            if (mSprite!=null) {
//                mSprite.setTexture(-1);
//            }
//            mTextureId = -1;
//            mImageTp = null;
//            mSprite = null;
//            mRectDrawable = null;
//        }
//        mBitmap = image;
//        mBitmapUpdated = true;
//    }
//
//    // Set relative size in fraction of screen
//    public void setSize(float scale) {
//        mScale = scale;
//    }
//
//    public void setPos(float posX, float posY) {
//        mPosX = posX;
//        mPosY = posY;
//    }
//
//    public void draw(int w, int h) {
//        draw(w, h, 1.0f, 1.0f);
//    }
//
//    public void draw(int w, int h, float aspectX, float aspectY) {
//        try {
//            if (mBitmap == null) return;
//            if (mSprite == null) {
//                mRectDrawable = new Drawable2dNew(Drawable2dNew.Prefab.RECTANGLE);
//                mSprite = new Sprite2dNew(mRectDrawable);
//                mImageTp = new Texture2dProgramNew(Texture2dProgramNew.ProgramType.TEXTURE_2D);
//                mTextureId = GlUtilNew.createImageTexture(mBitmap);
//                if (mSprite != null) {
//                    mSprite.setTexture(mTextureId);
//                } else {
//                    Log.e("SurfaceImageNew", "mSprite is null");
//                }
//            }
//
//            if (mBitmap != null && !mBitmap.isRecycled()) {
//                int w0 = Math.round(w * aspectX);
//                int h0 = Math.round(h * aspectY);
//                float aspect = (float) mBitmap.getWidth() / mBitmap.getHeight();
//                float size = Math.min(w0 / aspect, h0) * mScale;
//                float left = (w - (size * aspect)) / 2.0f; // Center horizontally
//                float top = h - size;                      // Align with the top edge
//                if (mSprite != null) {
//                    mSprite.setPosition(left + 30, top);            // Set position at top-center
//                    mSprite.setScale(size * aspect, size);
//                    float[] m = new float[16];
//                    Matrix.orthoM(m, 0, 0, w, 0, h, -1, 1);
//                    if (mSprite != null) {
//                        if (mImageTp != null) {
//                            mSprite.draw(mImageTp, m);
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void drawScreenCast(int w, int h) {
//        try {
//            float aspectX = 1.0f;
//            float aspectY = 1.0f;
//
//            if (mBitmap == null) return;
//            if (mSprite == null) {
//                mRectDrawable = new Drawable2dNew(Drawable2dNew.Prefab.RECTANGLE);
//                mSprite = new Sprite2dNew(mRectDrawable);
//                mImageTp = new Texture2dProgramNew(Texture2dProgramNew.ProgramType.TEXTURE_2D);
//                mTextureId = GlUtilNew.createImageTexture(mBitmap);
//                if (mSprite != null) {
//                    mSprite.setTexture(mTextureId);
//                } else {
//                    Log.e("SurfaceImageNew", "mSprite is null");
//                }
//            }
//
//            if (mBitmap != null && !mBitmap.isRecycled()) {
//                int w0 = Math.round(w * aspectX);
//                int h0 = Math.round(h * aspectY);
//                float aspect = (float) mBitmap.getWidth() / mBitmap.getHeight();
//                float size = Math.min(w0 / aspect, h0) * mScale;
//                float left = (w - (size * aspect)) / 2.0f; // Center horizontally
//                float top = h - size;                      // Align with the top edge
//                if (mSprite != null) {
//                    mSprite.setPosition(left - 50, top - 20);            // Set position at top-center
//                    mSprite.setScale(size * aspect, size);
//                    float[] m = new float[16];
//                    Matrix.orthoM(m, 0, 0, w, 0, h, -1, 1);
//                    if (mSprite != null) {
//                        if (mImageTp != null) {
//                            mSprite.draw(mImageTp, m);
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void drawUSB(int w, int h) {
//        try {
//            boolean isUsbCam= AppPreference.getBool(AppPreference.KEY.IS_USB_OPENED,false);
//            if (mBitmap == null) return;
//            if (mSprite == null) {
//                mRectDrawable = new Drawable2dNew(Drawable2dNew.Prefab.RECTANGLE);
//                mSprite = new Sprite2dNew(mRectDrawable);
//                mImageTp = new Texture2dProgramNew(Texture2dProgramNew.ProgramType.TEXTURE_2D);
//                mTextureId = GlUtilNew.createImageTexture(mBitmap);
//                if (mSprite != null) {
//                    mSprite.setTexture(mTextureId);
//                }else {
//                    Log.e("SurfaceImageNew", "mSprite: mSprite == null " );
//                }
//            }
//            // 1) If you’re drawing text as a Bitmap, create a Sprite2D, etc.
//            float[] ortho = new float[16];
//            android.opengl.Matrix.orthoM(ortho, 0, 0, w, 0, h, -1, 1);
//
//            // 2) Create an orthographic projection: left=0, right=w, bottom=0, top=h
//            if (mBitmap == null || mBitmap.isRecycled()) {
//                return;
//            }
//            if (mSprite == null) {
//                mRectDrawable = new Drawable2dNew(Drawable2dNew.Prefab.RECTANGLE);
//                mSprite = new Sprite2dNew(mRectDrawable);
//
//                mImageTp = new Texture2dProgramNew(Texture2dProgramNew.ProgramType.TEXTURE_2D);
//                mTextureId = GlUtilNew.createImageTexture(mBitmap);
//
//                mSprite.setTexture(mTextureId);
//            }
//
//            // 4) Decide how large your text overlay is
//            //    Let’s make it a fixed “pixel size” or a fraction of containerWidth.
//            float spriteWidth = w * 0.3f; // 30% of screen width
//            float originalAspect = (float) mBitmap.getWidth() / (float) mBitmap.getHeight();
//            float spriteHeight = spriteWidth / originalAspect;
//
//            // 5) Position at top-left. If setPosition(cx, cy) means "center" of sprite,
//            //    we shift by half the width/height to get top-left.
//            float marginLeft = 20f;
//            float marginTop  = 30f;
//            float centerX    = marginLeft + spriteWidth / 2f;
//            float centerY    = (h - marginTop) - spriteHeight / 2f;
//            if (mSprite != null) {
//                float extraPadding = 1f;
//                mSprite.setPosition(centerX + extraPadding, centerY + extraPadding);
//                mSprite.setScale(spriteWidth + extraPadding, spriteHeight + extraPadding);
//                // 7) Draw
//                mSprite.draw(mImageTp, ortho);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    /**
//     * Draw the timestamp (mBitmap) in the top-right corner of the *camera feed area*
//     * within the final frame. This prevents it from ending up in black bars.
//     *If Still Too Small?
//     * Increase maxWidthFraction (e.g., 0.35f or even 0.4f)
//     * Increase paint.setTextSize(...) (e.g., 32 * density)
//     * Increase margins if needed to keep it away from the very edge.
//     *
//     * @param frameWidth   The total frame width (e.g. videoSize.width).
//     * @param frameHeight  The total frame height (e.g. videoSize.height).
//     * @param cameraWidth  The camera feed width (before scaling).
//     * @param cameraHeight The camera feed height (before scaling).
//     */
//    /**
//     * Draw the timestamp (mBitmap) in the top-right corner of the *camera feed area*
//     * within the final frame. This prevents it from being in any black letterbox region.
//     *
//     * @param frameWidth   The total frame width (e.g., videoSize.width).
//     * @param frameHeight  The total frame height (e.g., videoSize.height).
//     * @param cameraWidth  The camera feed width (before scaling).
//     * @param cameraHeight The camera feed height (before scaling).
//     */
//    private int mLastFrameWidth = -1;
//    private int mLastFrameHeight = -1;
//    private int mLastCameraWidth = -1;
//    private int mLastCameraHeight = -1;
//    public void drawTimeStampOnImage(int frameWidth, int frameHeight,
//                                     int cameraWidth, int cameraHeight) {
//        try {
//            // Basic validation
//            if (frameWidth <= 0 || frameHeight <= 0 ||
//                    cameraWidth <= 0 || cameraHeight <= 0) {
//                return;
//            }
//            if (mBitmap == null || mBitmap.isRecycled()) {
//                return;
//            }
//            // (1) Create or re-create OpenGL objects if needed
//            if (mSprite == null) {
//                mRectDrawable = new Drawable2dNew(Drawable2dNew.Prefab.RECTANGLE);
//                mSprite = new Sprite2dNew(mRectDrawable);
//            }
//            if (mImageTp == null) {
//                mImageTp = new Texture2dProgramNew(Texture2dProgramNew.ProgramType.TEXTURE_2D);
//            }
//            // If the texture was never created or if the bitmap has changed, create a new texture.
//            if (mTextureId <= 0 || mBitmapUpdated) {
//                mTextureId = GlUtilNew.createImageTexture(mBitmap);
//                mSprite.setTexture(mTextureId);
//                mBitmapUpdated = false;
//            }
//            // (2) Compute or cache the projection matrix if sizes have changed
//            boolean sizeChanged = (frameWidth != mLastFrameWidth ||
//                    frameHeight != mLastFrameHeight);
//            if (sizeChanged) {
//                Matrix.orthoM(mOrthoMatrix, 0,
//                        0, frameWidth,
//                        0, frameHeight,
//                        -1, 1);
//                mLastFrameWidth = frameWidth;
//                mLastFrameHeight = frameHeight;
//            }
//            // (3) Compute how the camera feed is letter/pillar-boxed within final frame
//            //     and compute how to position the timestamp only if camera or frame changed.
//            boolean cameraChanged = (cameraWidth != mLastCameraWidth ||
//                    cameraHeight != mLastCameraHeight ||
//                    sizeChanged);
//            float centerX = 0f;
//            float centerY = 0f;
//            float scaledW = 0f;
//            float scaledH = 0f;
//            float extraScale = 1.20f;  // 20% bigger
//            if (cameraChanged) {
//                float frameRatio = (float) frameWidth / frameHeight;
//                float cameraRatio = (float) cameraWidth / cameraHeight;
//                float displayedWidth, displayedHeight;
//                float offsetX = 0f, offsetY = 0f;
//                if (cameraRatio > frameRatio) {
//                    // Wider than the frame
//                    displayedWidth = frameWidth;
//                    displayedHeight = displayedWidth / cameraRatio;
//                    offsetX = 0f;
//                    offsetY = (frameHeight - displayedHeight) / 2f;
//                } else {
//                    // Taller or equal ratio
//                    displayedHeight = frameHeight;
//                    displayedWidth = displayedHeight * cameraRatio;
//                    offsetX = (frameWidth - displayedWidth) / 2f;
//                    offsetY = 0f;
//                }
//                // (4) Decide how large the timestamp can be relative to the visible image
//                float maxWidthFraction = 0.35f;  // 35% of displayed camera feed's width
//                float margin = 10f;             // margin from edges
//                float bitmapW = mBitmap.getWidth();
//                float bitmapH = mBitmap.getHeight();
//                float desiredW = displayedWidth * maxWidthFraction;
//                float scaleFactor = (desiredW / bitmapW) * extraScale;
//                // If scaled height exceeds displayedHeight, clamp further
//                scaledW = bitmapW * scaleFactor;
//                scaledH = bitmapH * scaleFactor;
//                if (scaledH > displayedHeight) {
//                    scaleFactor = displayedHeight / bitmapH;
//                    scaledW = bitmapW * scaleFactor;
//                    scaledH = bitmapH * scaleFactor;
//                }
//                // (5) Position at top-left or top-right (your choice) of camera feed region
//                // Here we place it top-left, as an example
//                centerX = offsetX + margin + (scaledW / 2f);
//                centerY = offsetY + displayedHeight - margin - (scaledH / 2f);
//                // Cache these last camera sizes
//                mLastCameraWidth = cameraWidth;
//                mLastCameraHeight = cameraHeight;
//            }
//            // If for some reason the scaled sizes are invalid or
//            // we never recalculated them, skip drawing
//            if (scaledW <= 0f || scaledH <= 0f) {
//                return;
//            }
//            // (6) Update sprite position/scale
//            if (mSprite != null) {
//                float extraPadding = 1f;
//                mSprite.setPosition(centerX + extraPadding, centerY + extraPadding);
//                mSprite.setScale(scaledW + extraPadding, scaledH + extraPadding);
//                // (7) Draw
//                mSprite.draw(mImageTp, mOrthoMatrix);
//            }
//
//        } catch (IllegalArgumentException e) {
//            Log.e("SurfaceImageNew", "Invalid capacity: ", e);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Releases all resources used by this class.
//     * Call this method when the object is no longer needed.
//     */
//    public void release() {
//
//        // Release bitmap
//        if (mBitmap != null) {
//            mBitmap.recycle();
//            mBitmap = null;
//        }
//
//        // Release texture
//        if (mTextureId >= 0) {
//            GLES20.glDeleteTextures(1, new int[]{mTextureId}, 0);
//            mTextureId = -1;
//        }
//
//        // Release texture program
//        if (mImageTp != null) {
//            mImageTp.release();
//            mImageTp = null;
//        }
//
//        // Release sprite
//        if (mSprite != null) {
//            mSprite.setTexture(-1);
//            mSprite.clearResources();
//            mSprite = null;
//        }
//
//        // Release drawable
//        if (mRectDrawable != null) {
//            mRectDrawable = null;
//        }
//
//        System.gc();
//    }
//
//}

package com.checkmate.android.util.libgraph;

import android.graphics.Bitmap;
import android.opengl.GLES20;
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

    public void drawCenteredOnFrame(int frameW, int frameH,float fraction) {
        if (mBitmap == null || mBitmap.isRecycled()) {
            Log.w("SurfaceImageNew", "Cannot draw centered frame: Bitmap is null or recycled");
            return;
        }
        ensureGlObjects();

        // Get bitmap dimensions
        final float bmpWidth = mBitmap.getWidth();
        final float bmpHeight = mBitmap.getHeight();

        // Calculate scale to make the overlay reasonably sized (15% of frame height)
        final float HEIGHT_FRACTION = 0.15f;
        float scale = (frameH * HEIGHT_FRACTION) / bmpHeight;

        // Also check width constraint (max 50% of frame width)
        float maxWidthScale = (frameW * fraction) / bmpWidth;
        scale = Math.min(scale, maxWidthScale);

        // Apply a maximum scale to prevent the overlay from being too large
        float maxScale = 2.0f; // Never scale up more than 2x
        scale = Math.min(scale, maxScale);

        // Calculate scaled dimensions
        float scaledWidth = bmpWidth * scale;
        float scaledHeight = bmpHeight * scale;

        // Calculate centered position
        // Note: In OpenGL, Y=0 is at the bottom, so we need to adjust
        float centerX = frameW / 2f;
        float centerY = frameH / 2f;  // This is correct for center in OpenGL coords
        // Reset transformations
        mSprite.setRotation(0);
        mSprite.setPosition(centerX, centerY);
        mSprite.setScale(scaledWidth, scaledHeight);

        // Set up orthographic projection
        Matrix.orthoM(mOrtho, 0, 0, frameW, 0, frameH, -1, 1);

        // Draw the sprite
        mSprite.draw(mImageTp, mOrtho);
    }


    /* ---------- helpers ---------- */

    private void ensureGlObjects() {
        if (mSprite == null) {
            mRectDrawable = new Drawable2dNew(Drawable2dNew.Prefab.RECTANGLE);
            mSprite       = new Sprite2dNew(mRectDrawable);
            mImageTp      = new Texture2dProgramNew(
                    Texture2dProgramNew.ProgramType.TEXTURE_2D);
        }
        if (mTextureId <= 0 || mBitmapUpdated) {
            deleteTexture();
            mTextureId = GlUtilNew.createImageTexture(mBitmap);
            mSprite.setTexture(mTextureId);
            mBitmapUpdated = false;
        }
    }

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

    /**
     * Draw timestamp at top-left corner with 8px padding
     * Generic method for all service types
     */
    public void drawTopLeft(int frameW, int frameH) {
        if (mBitmap == null || mBitmap.isRecycled()) return;
        ensureGlObjects();

        // Use actual bitmap size for crisp rendering
        float spriteW = mBitmap.getWidth();
        float spriteH = mBitmap.getHeight();

        // Position at top-left with 8px padding
        final int PADDING = 8;
        float cx = spriteW * 0.5f + PADDING;            // center-x
        float cy = frameH - spriteH * 0.5f - PADDING;   // center-y (OpenGL coords)

        mSprite.setPosition(cx, cy);
        mSprite.setScale(spriteW, spriteH);

        // Full-frame ortho projection
        Matrix.orthoM(mOrtho, 0, 0, frameW, 0, frameH, -1, 1);
        mSprite.draw(mImageTp, mOrtho);
    }
}