package com.checkmate.android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class TextUtils {
    private Context mContext;
    private int mFontSize;
    private int mColor;

    public TextUtils(Context ctx) {
        mContext = ctx;
        mFontSize = 5;
        mColor = Color.WHITE;
    }

    public TextUtils(Context ctx, int font_size) {
        mContext = ctx;
        mFontSize = font_size;
        mColor = Color.WHITE;
    }

    void setFontSize(int size) {
        mFontSize = size;
    }

    public void setColor(int color) {
        mColor = color;
    }

    public Bitmap createImageFromText(String text) {
        Rect bounds = getTextBounds(text, mFontSize);
        int w = (bounds.width() + 36) & ~0x0f;
        int h = (bounds.height() + 4) & ~0x04;
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawTextOnImage(text, b);
        return b;
    }

    public Rect getTextBounds(String text, int fontSize) {
        float scale = mContext.getResources().getDisplayMetrics().density;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setTextSize((int) (fontSize * scale));

        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        return bounds;
    }

    public void drawTextOnImage(String text, Bitmap b) {
        if (b == null) {
            return;
        }
        float scale = mContext.getResources().getDisplayMetrics().density;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setTextSize((int) (mFontSize * scale));

        Rect bounds = getTextBounds(text, mFontSize);
        b.eraseColor(0);
        Canvas cnv = new Canvas(b);

        paint.setColor(mColor);
        cnv.drawText(text, 0, bounds.height(), paint);
    }

}
