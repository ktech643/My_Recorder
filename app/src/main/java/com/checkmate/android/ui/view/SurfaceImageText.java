package com.checkmate.android.ui.view;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import com.checkmate.android.util.libgraph.SurfaceImageNew;


public class SurfaceImageText extends SurfaceImageNew {

    private int mFontSize;
    private int mColor;
    private Bitmap bitmap;

    public SurfaceImageText() {
        super();
        mFontSize = 9;
        mColor = Color.BLACK;
        // Initialize scheduler for delayed cache clearing

    }

    public void setFontSize(int size) {
        mFontSize = size;
    }

    public void setColor(int color) {
//        mColor = color;
        mColor = Color.BLACK;
    }

    public void setText(String text) {
        float scale = 3.0F;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize((int) (mFontSize * scale));

        // Reuse bitmap if size matches
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        int w = (bounds.width() + 36) & ~0x0f;
        int h = (bounds.height() + 15) & ~0x0f;
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }

        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE); // Set background to white
        paint.setColor(Color.BLACK); // Set text color to black
        canvas.drawText(text, 0, bounds.height(), paint);
        int bitmapSize = bitmap.getByteCount();
        Log.d("BitmapSize", "Bitmap size: " + bitmapSize + " bytes");
        setImage(bitmap);
//        new Handler(Looper.getMainLooper()).postDelayed(this::scheduleCacheClearing, 2000);
    }

    public void scheduleCacheClearing() {
        bitmap.recycle();
        bitmap=null;
       // super.release();

    }

}