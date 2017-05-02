package com.nordicsemi.ImageTransferDemo.gui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.nordicsemi.ImageTransferDemo.R;

/**
 * Created by too1 on 02.10.2015.
 */
public class RgbLedButton extends View {
    private boolean mEnabled;
    private String mText;
    private float mTextHeight, mTextOffsetLeft;
    private int mTextColor, mSelectedColor;
    private Paint mTextPaint;
    private float mIconCenterY;
    private float mRgbSelectorSize;
    private float mRgbSelectorX = 0.5f, mRgbSelectorY = 0.5f;
    private float mColorHue = 0.0f, mColorIntensity = 1.0f, mColorSaturation = 1.0f;
    private Drawable mBackgroundDrawable, mRgbIconDrawable, mRgbSelectorDrawable;

    private RectF mRegion, mRegionPadded, mRgbRegion, mRgbSelectorRegion;

    private OnRgbChangedListener mRgbChangedListener = null;

    public RgbLedButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RgbLedButton, 0, 0);

        try{
            mEnabled             = a.getBoolean(R.styleable.RgbLedButton_enabled, true);
            mText                = a.getString(R.styleable.RgbLedButton_text);
            mTextHeight          = a.getDimension(R.styleable.RgbLedButton_textHeight, 0.0f);
            mTextOffsetLeft      = a.getDimension(R.styleable.RgbLedButton_textOffsetLeft, 20.0f);
            mTextColor           = a.getColor(R.styleable.RgbLedButton_textColor, Color.WHITE);
            mBackgroundDrawable  = a.getDrawable(R.styleable.RgbLedButton_backgroundDrawable);
            mIconCenterY         = a.getFloat(R.styleable.RgbLedButton_rgbiconYPos, 0.5f);
            mRgbIconDrawable     = a.getDrawable(R.styleable.RgbLedButton_rgbicon_drawable);
            mRgbSelectorDrawable = a.getDrawable(R.styleable.RgbLedButton_rgbselector_drawable);
            mRgbSelectorSize     = a.getDimension(R.styleable.RgbLedButton_rbgselector_size, 10.0f);
        }finally {
            a.recycle();
        }
        if(mText == null) mText = "";
        init();

    }

    public void setRgbChangedListener(OnRgbChangedListener listener){
        mRgbChangedListener = listener;
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
        mRgbRegion = new RectF();
        mRgbSelectorRegion = new RectF();
    }

    public void setEnabled(boolean enabled){
        mEnabled = enabled;
        invalidate();
    }

    public void resetSelector(){
        mRgbSelectorX = mRgbSelectorY = 0.5f;
        mColorIntensity = 0.0f;
        setRgbSelectorPosition();
        invalidate();
    }

    private void setRgbSelectorPosition(){
        mRgbSelectorDrawable.setBounds((int)(mRgbRegion.left + mRgbRegion.width() * mRgbSelectorX - mRgbSelectorSize),
            (int)(mRgbRegion.top + mRgbRegion.height() * mRgbSelectorY - mRgbSelectorSize),
            (int)(mRgbRegion.right - mRgbRegion.width() * (1.0f - mRgbSelectorX) + mRgbSelectorSize),
            (int)(mRgbRegion.bottom - mRgbRegion.height() * (1.0f - mRgbSelectorY) + mRgbSelectorSize));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float xpad = (float)(getPaddingLeft() + getPaddingRight());
        float ypad = (float)(getPaddingTop() + getPaddingBottom());
        mRegion.set(getLeft(), getTop(), (float) w, (float) h);
        mRegionPadded.set(getPaddingLeft(), getPaddingTop(), (float) w - getPaddingRight(), (float) h - getPaddingBottom());
        mBackgroundDrawable.setBounds((int) mRegionPadded.left, (int) mRegionPadded.top, (int) mRegionPadded.right, (int) mRegionPadded.bottom);

        // Set RGB Icon region
        float mIconWidth = Math.min(mRegionPadded.width(), mRegionPadded.height() - mTextHeight);
        float mIconHeight = Math.min(mRegionPadded.width(), mRegionPadded.height() - mTextHeight);
        mRgbRegion.set((getWidth() - mIconWidth) * 0.5f,
                getHeight() * mIconCenterY - mIconHeight * 0.5f + mTextHeight,
                (getWidth() + mIconWidth) * 0.5f,
                getHeight() * mIconCenterY + mIconHeight * 0.5f + mTextHeight);
        mRgbIconDrawable.setBounds((int) mRgbRegion.left, (int) mRgbRegion.top, (int) mRgbRegion.right, (int) mRgbRegion.bottom);

        setRgbSelectorPosition();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mBackgroundDrawable.draw(canvas);
        mRgbIconDrawable.setAlpha(mEnabled ? 255 : 40);
        mRgbIconDrawable.draw(canvas);
        mTextPaint.setAlpha(mEnabled ? 255 : 40);
        canvas.drawText(mText, mRegionPadded.left + mTextOffsetLeft, mRegionPadded.top + mTextHeight, mTextPaint);
        if(mEnabled){
            mRgbSelectorDrawable.draw(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            float normalizedX, normalizedY, rgbSelX, rgbSelY;
            float rawX = event.getX();
            float rawY = event.getY();
            if(mEnabled) {
                if (rawX > mRgbRegion.left && rawX < mRgbRegion.right && rawY > mRgbRegion.top && rawY < mRgbRegion.bottom) {
                    normalizedX = (rawX - mRgbRegion.left) / mRgbRegion.width();
                    normalizedY = (rawY - mRgbRegion.top) / mRgbRegion.height();
                    rgbSelX = normalizedX;
                    rgbSelY = normalizedY;
                    normalizedX = (normalizedX - 0.5f) * 2.0f;
                    normalizedY = (normalizedY - 0.5f) * 2.0f;
                    mColorHue = (float)Math.atan2((double)normalizedY, (double)normalizedX);
                    mColorHue = (mColorHue + (float)Math.PI) / (2.0f * (float)Math.PI);
                    mColorIntensity = (float)Math.sqrt(Math.pow((double) normalizedX, 2.0) + Math.pow((double) normalizedY, 2.0));
                    mColorIntensity *= (1 / 0.89f);
                    if(mColorIntensity <= 1.0f) {
                        mColorIntensity = (mColorIntensity < 0.2f ? 0.0f : (mColorIntensity - 0.2f) * 1.25f);
                        mRgbSelectorX = rgbSelX;
                        mRgbSelectorY = rgbSelY;
                        if(mRgbChangedListener != null){
                            float []hsv = new float[3];
                            hsv[0] = 360.0f - mColorHue * 360.0f + 90.0f;
                            if(hsv[0] > 360.0f) hsv[0] -= 360.0f;
                            hsv[1] = 1.0f;
                            hsv[2] = mColorIntensity;
                            mSelectedColor = Color.HSVToColor(hsv);
                            mRgbChangedListener.onRgbChanged(this, (float)((mSelectedColor >> 16) & 0xFF)/ 255.0f, (float)((mSelectedColor >> 8) & 0xFF)/ 255.0f, (float)((mSelectedColor >> 0) & 0xFF)/ 255.0f);
                        }
                        setRgbSelectorPosition();
                        invalidate();
                    }
                    Log.d("RgbDemo", String.valueOf(mColorHue) + ", " + String.valueOf(mColorIntensity));
                    return true;
                }
            }
        }

        return false;
    }

    public interface OnRgbChangedListener {
        void onRgbChanged(RgbLedButton sender, float r, float g, float b);
    }
}
