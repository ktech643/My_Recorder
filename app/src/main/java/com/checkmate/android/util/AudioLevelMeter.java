package com.checkmate.android.util;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.RelativeLayout;

import com.checkmate.android.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class AudioLevelMeter extends View implements ValueAnimator.AnimatorUpdateListener {
    float measureInterval = 0.1f; //Interval for update volume meter in seconds
    private float[] rms;
    private double[] sum;
    private int count = 0;
    private float duration = 0.0f;
    private ValueAnimator[] mAnimators;

    private Paint paint;
    private int mWidth;
    private int mHeight;

    public int getChannels() {
        return mChannels;
    }

    public void setChannels(int mChannels) {
        this.mChannels = mChannels;
        if (mAnimators != null) {
            for (Animator a: mAnimators) {
                a.removeAllListeners();
            }
        }
        initChannels();
    }

    private int mChannels;
    private int mLedCount;
    private int mRedCount;
    private int mYellowCount;

    private RectF barRect;
    private Path mPath;
    private float[] mValue;
    private boolean needUpdateTiks = true;

    static private final double conversion16Base = Math.pow(2.0, 15);
    static private float dbRangeMin = -80.0f;
    static private float dbRangeMax = 0.0f;

    public AudioLevelMeter(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.AudioLevelMeter);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setColor(0xFFFF0000);
        paint.setARGB(255, 0, 255, 0);
        paint.setStyle(Paint.Style.STROKE);

        mChannels = attributes.getInt(R.styleable.AudioLevelMeter_chanels, 1);
        if (mChannels <= 0) {
            mChannels = 1;
        }

        mLedCount = attributes.getInt(R.styleable.AudioLevelMeter_ledCount, 30);
        mRedCount = attributes.getInt(R.styleable.AudioLevelMeter_redCount, 0);
        mYellowCount = attributes.getInt(R.styleable.AudioLevelMeter_redCount, 0);
        if (mRedCount <= 0 || mRedCount >= mLedCount) {
            mRedCount = (mLedCount + 9) / 10;
        }
        if (mYellowCount <= 0 || mYellowCount >= mLedCount) {
            mYellowCount = (mLedCount + 2) / 3 - mRedCount;
        }
        initChannels();
        mPath = new Path();

    }

    void initChannels() {
        mAnimators = new ValueAnimator[mChannels];

        for (int i = 0; i < mChannels; i++) {
            ValueAnimator animator = new ValueAnimator();
            animator.setDuration((long) (measureInterval * 1000.0f));
            animator.addUpdateListener(this);
            animator.setInterpolator(new LinearInterpolator());
            mAnimators[i] = animator;
        }

        rms = new float[mChannels];
        sum = new double[mChannels];
        mValue = new float[mChannels];
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float barH = (float) Math.max(mWidth, mHeight);
        final float barW = (float) Math.min(mWidth, mHeight) / (float) mChannels;
        final float strokeLen = barH / (float) mLedCount;
        if (needUpdateTiks) {
            paint.setStrokeWidth(barW*0.9f);

            final float strokeFill = strokeLen * 2.0f / 3.0f;
            final float strokeBlank = strokeLen * 1.0f / 3.0f;
            final float[] intervals = {strokeFill, strokeBlank};
            PathEffect dash = new DashPathEffect(intervals, strokeLen);
            paint.setPathEffect(dash);

            setGradient();
            needUpdateTiks = false;
        }
        for (int i = 0; i < mChannels; i++) {
            float numStrokes = Math.round(mLedCount * mValue[i]);
            float len = numStrokes * strokeLen;
            Path path = new Path();
            if (mWidth > mHeight) {
                path.moveTo(0, barW * (i +0.5f));
                path.lineTo(len, barW * (i +0.5f));
            } else {
                path.moveTo(barW * (i +0.5f), barH);
                path.lineTo(barW * (i +0.5f), barH - len);
            }
            canvas.drawPath(path, paint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int orientation = getResources().getConfiguration().orientation;
        RelativeLayout.LayoutParams barParams = (RelativeLayout.LayoutParams) getLayoutParams();
        int a = Math.min(barParams.width, barParams.height);
        int b = Math.max(barParams.width, barParams.height);
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            mWidth = a;
            mHeight = b;
        } else {
            mWidth = b;
            mHeight = a;
        }
        needUpdateTiks = true;
        setMeasuredDimension(mWidth, mHeight);
    }

    private void setGradient() {
        final int clrRed = 0xffff0000;
        final int clrYellow = 0xffffff00;
        final int clrGreen = 0xff00ff00;
        final int[] gradientColors = {clrGreen, clrGreen, clrYellow, clrYellow, clrRed};

        final float redPos = (float)(mLedCount - mRedCount) / (float)mLedCount;
        final float yellowPos = (float)(mLedCount - mRedCount - mYellowCount) / (float)mLedCount;
        final float[] locations = {0f, (yellowPos-0.01f), (yellowPos), (redPos-0.01f), (redPos)};

        LinearGradient gradient;
        if (mWidth > mHeight) {
            gradient = new LinearGradient(0f, 0f, (float)mWidth, 0f, gradientColors, locations, Shader.TileMode.CLAMP);
        } else {
            gradient = new LinearGradient(0f, (float)mHeight, 0f, 0f, gradientColors, locations, Shader.TileMode.CLAMP);

        }
        paint.setShader(gradient);
    }

    public void putBuffer(byte[] data, int channelCount, int sampleRate) {
        ShortBuffer shortBuffer = ByteBuffer.wrap(data).order(ByteOrder.nativeOrder()).asShortBuffer();
        double channelFactor = 1.0 / channelCount;
        int ch = 0;
        while (shortBuffer.hasRemaining()) {
            short val = shortBuffer.get();
            double f = val / conversion16Base;
            sum[ch] += f * f;
            if (mChannels > channelCount) {
                sum[ch + 1] += f * f;
            }
            if (ch == channelCount - 1) {
                count++;
            }
            ch = (ch + 1) % channelCount;
            duration += channelFactor / sampleRate;
            if (duration > measureInterval) {
                updateValue();
            }
        }
    }

    private void updateValue() {
        for (int i = 0; i < mChannels; i++) {
            double v = sum[i] == 0.0f ? -100.0f : 10.0f * Math.log(Math.sqrt(sum[i] / count));
            float newValue = rms[i] * 0.1f + (float) v * 0.9f;
            if (newValue < dbRangeMin) {
                newValue = dbRangeMin;
            } else if (newValue > dbRangeMax) {
                newValue = dbRangeMax;
            }
            mAnimators[i].setFloatValues(rms[i], newValue);
            rms[i] = newValue;
            sum[i] = 0;

        }
        new Handler(Looper.getMainLooper()).post(() -> {
            for (int i = 0; i < mChannels; i++) {
                mAnimators[i].cancel();
                mAnimators[i].start();
            }
        });
        duration -= measureInterval;
        count = 0;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        if (animation == mAnimators[mChannels-1]) {
            for (int i = 0; i < mChannels; i++) {
                float val = (Float) mAnimators[i].getAnimatedValue();
                mValue[i] = (val - dbRangeMin) / (dbRangeMax - dbRangeMin);
                invalidate();
            }
        }
    }
}
