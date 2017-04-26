package com.nordicsemi.IntensityLightControl.gui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.nordicsemi.IntensityLightControl.R;

/**
 * Created by too1 on 02.10.2015.
 */
public class AccDisplay extends View {
    private boolean mEnabled;
    private String mText;
    private float mTextHeight, mTextOffsetLeft;
    private int mTextColor;
    private Paint mTextPaint;
    private int mAccDataLength = 30, mAccDataPos = 0;
    private float [][]mAccData;
    private float mAccDataMax = 32.0f, mAccDataMin = -32.0f;

    private float mGraphPaddingLeft, mGraphPaddingRight, mGraphPaddingTopBottom, mGraphLineWidth;
    private int []mGraphLineColor = new int[3];
    private Drawable mBackgroundDrawable, mGraphBckDrawable;
    private Paint mGraphPaint;

    private RectF mRegion, mRegionPadded, mGraphRegion;

    public AccDisplay(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AccDisplay, 0, 0);

        try {
            mEnabled            = a.getBoolean(R.styleable.AccDisplay_enabled, true);
            mText               = a.getString(R.styleable.RgbLedButton_text);
            mTextHeight         = a.getDimension(R.styleable.RgbLedButton_textHeight, 0.0f);
            mTextOffsetLeft     = a.getDimension(R.styleable.RgbLedButton_textOffsetLeft, 20.0f);
            mTextColor          = a.getColor(R.styleable.RgbLedButton_textColor, Color.WHITE);
            mBackgroundDrawable = a.getDrawable(R.styleable.AccDisplay_backgroundDrawable);
            mGraphBckDrawable   = a.getDrawable(R.styleable.AccDisplay_graphBckDrawable);
            mGraphPaddingLeft   = a.getDimension(R.styleable.AccDisplay_graphPaddingLeft, 10.0f);
            mGraphPaddingRight  = a.getDimension(R.styleable.AccDisplay_graphPaddingRight, 10.0f);
            mGraphPaddingTopBottom = a.getDimension(R.styleable.AccDisplay_graphPaddingTopBottom, 10.0f);
            mGraphLineWidth     = a.getDimension(R.styleable.AccDisplay_graphLineWidth, 5.0f);
            mGraphLineColor[0]  = a.getColor(R.styleable.AccDisplay_graphLineColor1, 0xFF000000);
            mGraphLineColor[1]  = a.getColor(R.styleable.AccDisplay_graphLineColor2, 0xFF000000);
            mGraphLineColor[2]  = a.getColor(R.styleable.AccDisplay_graphLineColor3, 0xFF000000);
        }finally{
            a.recycle();
        }

        init();

        mAccData = new float[3][mAccDataLength];
        for(int i = 0; i < mAccDataLength; i++) {
            mAccData[0][i] = 0;
            mAccData[1][i] = (float) i * 0.2f;
            mAccData[2][i] = (float)Math.sin((double)i * 0.2) * 6.0f;
        }
    }

    private void init(){
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setColor(mTextColor);
        if(mTextHeight == 0.0f){
            mTextHeight = mTextPaint.getTextSize();
        }else{
            mTextPaint.setTextSize(mTextHeight);
        }

        mRegion = new RectF();
        mRegionPadded = new RectF();
        mGraphRegion = new RectF();
        mGraphPaint = new Paint();
        mGraphPaint.setAntiAlias(true);
        mGraphPaint.setDither(true);
        mGraphPaint.setStyle(Paint.Style.STROKE);
        mGraphPaint.setStrokeJoin(Paint.Join.ROUND);
        mGraphPaint.setStrokeWidth(mGraphLineWidth);
    }

    public void setEnabled(boolean enabled){
        mEnabled = enabled;
        invalidate();
    }

    public void dataClear(){
        for(int axis = 0; axis < 3; axis++){
            for(int sample = 0; sample < mAccDataLength; sample++){
                mAccData[axis][sample] = 0.0f;
            }
        }
        mAccDataPos = 0;
    }

    public void dataPush(float x, float y, float z){
        if(mAccDataPos >= mAccDataLength) {
            for (int axis = 0; axis < 3; axis++) {
                for (int sample = 0; sample < (mAccDataLength - 1); sample++) {
                    mAccData[axis][sample] = mAccData[axis][sample + 1];
                }
            }
            mAccData[0][mAccDataLength-1] = x;
            mAccData[1][mAccDataLength-1] = y;
            mAccData[2][mAccDataLength-1] = z;
        }
        else {
            mAccData[0][mAccDataPos] = x;
            mAccData[1][mAccDataPos] = y;
            mAccData[2][mAccDataPos] = z;
            mAccDataPos++;
        }

        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float xpad = (float)(getPaddingLeft() + getPaddingRight());
        float ypad = (float)(getPaddingTop() + getPaddingBottom());
        mRegion.set(getLeft(), getTop(), (float) w, (float) h);
        mRegionPadded.set(getPaddingLeft(), getPaddingTop(), (float) w - getPaddingRight(), (float) h - getPaddingBottom());
        mBackgroundDrawable.setBounds((int) mRegionPadded.left, (int) mRegionPadded.top, (int) mRegionPadded.right, (int) mRegionPadded.bottom);

        // Set RGB Icon region
        float mIconWidth = mRegionPadded.width() - mGraphPaddingLeft - mGraphPaddingRight;
        float mIconHeight = mRegionPadded.height() - mGraphPaddingTopBottom * 2 - mTextHeight;
        float mIconCenterY = 0.5f;
        mGraphRegion.set(mRegionPadded.left + mGraphPaddingLeft,
                mRegionPadded.top + mGraphPaddingTopBottom + mTextHeight,
                mRegionPadded.right - mGraphPaddingRight,
                mRegionPadded.bottom - mGraphPaddingTopBottom);
        mGraphBckDrawable.setBounds((int) mGraphRegion.left, (int) mGraphRegion.top, (int) mGraphRegion.right, (int) mGraphRegion.bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mBackgroundDrawable.draw(canvas);
        mGraphBckDrawable.setAlpha(mEnabled ? 255 : 40);
        mGraphBckDrawable.draw(canvas);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setAlpha(mEnabled ? 255 : 40);
        canvas.drawText(mText, mRegionPadded.left + mTextOffsetLeft, mRegionPadded.top + mTextHeight, mTextPaint);

        mTextPaint.setColor(mGraphLineColor[0]);
        mTextPaint.setAlpha(mEnabled ? 255 : 40);
        canvas.drawText(" X", mGraphRegion.right, mGraphRegion.top + mTextHeight * 2.0f, mTextPaint);
        mTextPaint.setColor(mGraphLineColor[1]);
        mTextPaint.setAlpha(mEnabled ? 255 : 40);
        canvas.drawText(" Y", mGraphRegion.right, mGraphRegion.top + mTextHeight * 3.5f, mTextPaint);
        mTextPaint.setColor(mGraphLineColor[2]);
        mTextPaint.setAlpha(mEnabled ? 255 : 40);
        canvas.drawText(" Z", mGraphRegion.right, mGraphRegion.top + mTextHeight * 5.0f, mTextPaint);

        if(mEnabled) {
            float graphPaddingX = 6.0f;
            float graphPosX = mGraphRegion.left + graphPaddingX;
            float graphScaleX = (mGraphRegion.width() - graphPaddingX * 2.0f) / (float) (mAccDataLength - 1);
            float graphPosY = mGraphRegion.bottom - mGraphRegion.height() * ((0 - mAccDataMin) / (mAccDataMax - mAccDataMin));
            float graphScaleY = mGraphRegion.height() / (mAccDataMax - mAccDataMin);
            for (int axis = 0; axis < 3; axis++) {
                mGraphPaint.setColor(mGraphLineColor[axis]);
                for (int sample = 0; sample < (mAccDataPos - 1); sample++) {
                    canvas.drawLine(graphPosX + (float) sample * graphScaleX, graphPosY - mAccData[axis][sample] * graphScaleY,
                            graphPosX + (float) (sample + 1) * graphScaleX, graphPosY - mAccData[axis][sample + 1] * graphScaleY, mGraphPaint);
                }
            }
        }
    }
}
