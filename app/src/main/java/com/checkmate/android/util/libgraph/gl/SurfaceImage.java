package com.checkmate.android.util.libgraph.gl;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import com.checkmate.android.util.libgraph.drawable.Drawable2d;
import com.checkmate.android.util.libgraph.egl.ContextListener;
import com.checkmate.android.util.libgraph.egl.EglCore;
import com.checkmate.android.util.libgraph.program.Texture2dProgram;
import com.checkmate.android.util.libgraph.program.TexturePool;
import com.checkmate.android.util.libgraph.scene.Sprite2d;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SurfaceImage implements ContextListener {
    private static final String TAG = "SurfaceImage";
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
            ThreadLocal.withInitial(() ->
                    new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.US)
            );

    // Configuration
    public float scale = 0.12f;
    public float marginX = 20f;
    public float marginY = 20f;

    // GL state
    private final GlThread glThread;
    private final TexturePool texturePool;
    private final float[] ortho = new float[16];
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private Drawable2d rect;
    private Sprite2d sprite;
    private Texture2dProgram program;

    // Resources
    private int texture = -1;
    private Bitmap reusableBitmap;
    private final Paint textPaint = new Paint();
    private ByteBuffer timestampBuffer;
    private final int textureWidth = 400;
    private final int textureHeight = 100;

    public SurfaceImage(EglCore core, GlThread glThread) {
        this.glThread = glThread;
        this.texturePool = new TexturePool(GLES20.GL_TEXTURE_2D, glThread, core);
        core.addContextListener(this);

        configurePaint();
        initializeGlObjects();
    }

    private void configurePaint() {
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(24f);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    private void renderTextToBuffer() {
        if (reusableBitmap == null || reusableBitmap.isRecycled()) {
            Log.w(TAG, "Bitmap not available for rendering");
            return;
        }

        Canvas canvas = new Canvas(reusableBitmap);
        canvas.drawColor(Color.WHITE);

        String timestamp = DATE_FORMAT.get().format(new Date());

        Rect bounds = new Rect();
        textPaint.getTextBounds(timestamp, 0, timestamp.length(), bounds);
        float yPos = textureHeight/2f + (bounds.height() - bounds.bottom)/2f;

        canvas.drawText(timestamp, 10, yPos, textPaint);

        if (timestampBuffer != null) {
            timestampBuffer.rewind();
            reusableBitmap.copyPixelsToBuffer(timestampBuffer);
            timestampBuffer.rewind();
        }
    }

    private void initializeBitmap() {
        glThread.post(() -> {
            try {
                if (reusableBitmap == null || reusableBitmap.isRecycled()) {
                    reusableBitmap = Bitmap.createBitmap(
                            textureWidth,
                            textureHeight,
                            Bitmap.Config.ARGB_8888
                    );
                }

                if (timestampBuffer == null) {
                    timestampBuffer = ByteBuffer.allocateDirect(
                            textureWidth * textureHeight * 4
                    ).order(ByteOrder.nativeOrder());
                }
            } catch (Exception e) {
                Log.e(TAG, "Bitmap initialization failed", e);
            }
        });
    }

    private void updateTexture() {
        glThread.post(() -> {
            if (texture == -1 || timestampBuffer == null) return;

            try {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
                GLES20.glTexSubImage2D(
                        GLES20.GL_TEXTURE_2D,
                        0,
                        0,
                        0,
                        textureWidth,
                        textureHeight,
                        GLES20.GL_RGBA,
                        GLES20.GL_UNSIGNED_BYTE,
                        timestampBuffer
                );
                GlUtil.check("glTexSubImage2D");
            } finally {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            }
        });
    }

    private void initializeGlObjects() {
        glThread.post(() -> {
            try {
                // Release existing resources if recreating
                releaseGlResources();

                rect = new Drawable2d(Drawable2d.Prefab.RECTANGLE);
                sprite = new Sprite2d(rect);
                program = new Texture2dProgram(Texture2dProgram.Type.TEXTURE_2D);

                texture = texturePool.acquire();
                initializeBitmap();

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
                GLES20.glTexImage2D(
                        GLES20.GL_TEXTURE_2D,
                        0,
                        GLES20.GL_RGBA,
                        textureWidth,
                        textureHeight,
                        0,
                        GLES20.GL_RGBA,
                        GLES20.GL_UNSIGNED_BYTE,
                        null
                );

                renderTextToBuffer();
                updateTexture();
                initialized.set(true);
            } catch (Exception e) {
                Log.e(TAG, "GL initialization failed", e);
                initialized.set(false);
            } finally {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            }
        });
    }

    @Override
    public void onContextLost() {
        glThread.post(() -> {
            initialized.set(false);
            if (texture != -1) {
                texturePool.release(texture);
                texture = -1;
            }
        });
    }

    @Override
    public void onContextRecreated() {
        initializeGlObjects();
    }

    public void draw(int vpWidth, int vpHeight) {
        if (!initialized.get() || texture == -1 || program == null) return;

        renderTextToBuffer();
        updateTexture();

        glThread.post(() -> {
            if (!initialized.get()) return;

            float aspect = (float) textureWidth / textureHeight;
            float w = vpWidth * scale;
            float h = w / aspect;

            sprite.setScale(w, h);
            sprite.setPosition(
                    marginX + w/2f,
                    vpHeight - marginY - h/2f
            );

            Matrix.orthoM(ortho, 0, 0, vpWidth, 0, vpHeight, -1, 1);
            sprite.draw(program, ortho);
        });
    }

    public void release() {
        glThread.post(() -> {
            releaseGlResources();
            if (reusableBitmap != null && !reusableBitmap.isRecycled()) {
                reusableBitmap.recycle();
            }
            reusableBitmap = null;
        });
    }

    private void releaseGlResources() {
        if (texture != -1) {
            texturePool.release(texture);
            texture = -1;
        }
        if (program != null) {
            program.close();
            program = null;
        }
        rect = null;
        sprite = null;
        initialized.set(false);
    }
}