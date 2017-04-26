package com.nordicsemi.IntensityLightControl.gui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.nordicsemi.IntensityLightControl.R;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by too1 on 2016-01-25.
 */
public class IntensityLedButton  extends View {
    private boolean mEnabled;
    private String mText;
    private float mTextHeight, mTextOffsetLeft;
    private float mIntensitySelectorWidth;
    private int mTextColor, mSelectedColor;
    private Paint mTextPaint;
    private boolean mLedOn = true;
    private float mLedIntensity = 1.0f;
    private float mIntSelectorX, mIntSelectorY;
    private float mTime = 0.0f;
    private Drawable mBackgroundDrawable, mButtonOnDrawable, mButtonOffDrawable, mIntensitySelectorDrawable;

    private boolean mInIntensityDrag = false;

    private RectF mRegion, mRegionPadded, mButtonRegion, mIntensitySelectorRegion;

    private OnLedChangeListener mLedChangeListener = null;

    public IntensityLedButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.IntensityLedButton, 0, 0);

        try{
            mEnabled             = a.getBoolean(R.styleable.IntensityLedButton_enabled, true);
            mText                = a.getString(R.styleable.IntensityLedButton_text);
            mTextHeight          = a.getDimension(R.styleable.IntensityLedButton_textHeight, 0.0f);
            mTextOffsetLeft      = a.getDimension(R.styleable.IntensityLedButton_textOffsetLeft, 20.0f);
            mTextColor           = a.getColor(R.styleable.IntensityLedButton_textColor, Color.WHITE);
            mBackgroundDrawable  = a.getDrawable(R.styleable.IntensityLedButton_backgroundDrawable);
            mButtonOnDrawable = a.getDrawable(R.styleable.IntensityLedButton_buttonOnDrawable);
            mButtonOffDrawable = a.getDrawable(R.styleable.IntensityLedButton_buttonOffDrawable);
            mIntensitySelectorWidth = a.getFloat(R.styleable.IntensityLedButton_intensitySelectorWidth, 0.03f);
            mIntensitySelectorDrawable = a.getDrawable(R.styleable.IntensityLedButton_intensitySelectorDrawable);
        }finally {
            a.recycle();
        }
        if(mText == null) mText = "";
        init();

    }

    public void setLedChangedListener(OnLedChangeListener listener){
        mLedChangeListener = listener;
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
        mButtonRegion = new RectF();
        mIntensitySelectorRegion = new RectF();
        setIntensitySelectorPosition(0.5f);

        Timer timeTimer = new Timer();
        timeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateTime();
            }
        }, 0, 100);
    }

    public void setEnabled(boolean enabled){
        mEnabled = enabled;
        invalidate();
    }

    final Runnable timeRunnable = new Runnable(){
        public void run(){
            mTime += 0.1f;
            invalidate();
        }
    };

    final Handler timeHandle = new Handler();

    private void updateTime(){
        timeHandle.post(timeRunnable);
    }

    public void resetSelector(){
        mLedIntensity = 0.0f;
        setIntensitySelectorPosition(0.5f);
        invalidate();
    }

    private void setIntensitySelectorPosition(float angle){
        float selectorWidth = mIntensitySelectorWidth * mRegionPadded.width();
        mIntSelectorX = (mRegionPadded.right - mRegionPadded.left) * 0.5f + (float)Math.cos((double) angle * Math.PI * 2.0 + Math.PI * 0.5) * mRegionPadded.width() * 0.34f;
        mIntSelectorY = (mRegionPadded.bottom - mRegionPadded.top) * 0.5f + (float) Math.sin((double) angle * Math.PI * 2.0 + Math.PI * 0.5) * mRegionPadded.height() * 0.34f;

        mIntensitySelectorDrawable.setBounds((int)(mIntSelectorX - selectorWidth), (int)(mIntSelectorY - selectorWidth), (int)(mIntSelectorX + selectorWidth), (int)(mIntSelectorY + selectorWidth));
        /*mIntensitySelectorDrawable.setBounds((int) (mRgbRegion.left + mRgbRegion.width() * mRgbSelectorX - mRgbSelectorSize),
                (int) (mRgbRegion.top + mRgbRegion.height() * mRgbSelectorY - mRgbSelectorSize),
                (int) (mRgbRegion.right - mRgbRegion.width() * (1.0f - mRgbSelectorX) + mRgbSelectorSize),
                (int) (mRgbRegion.bottom - mRgbRegion.height() * (1.0f - mRgbSelectorY) + mRgbSelectorSize));*/
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float xpad = (float)(getPaddingLeft() + getPaddingRight());
        float ypad = (float)(getPaddingTop() + getPaddingBottom());
        mRegion.set(getLeft(), getTop(), (float) w, (float) h);
        mRegionPadded.set(getPaddingLeft(), getPaddingTop(), (float) w - getPaddingRight(), (float) h - getPaddingBottom());
        mBackgroundDrawable.setBounds((int) mRegionPadded.left, (int) mRegionPadded.top, (int) mRegionPadded.right, (int) mRegionPadded.bottom);

        // Set RGB Icon region
        float mIconWidth = mRegionPadded.width()*0.57f;
        float mIconHeight = mRegionPadded.height()*0.57f;
        mButtonRegion.set((getWidth() - mIconWidth) * 0.5f,
                getHeight() * 0.5f - mIconHeight * 0.5f,
                (getWidth() + mIconWidth) * 0.5f,
                getHeight() * 0.5f + mIconHeight * 0.5f);
        mButtonOnDrawable.setBounds((int) mButtonRegion.left, (int) mButtonRegion.top, (int) mButtonRegion.right, (int) mButtonRegion.bottom);
        mButtonOffDrawable.setBounds((int) mButtonRegion.left, (int) mButtonRegion.top, (int) mButtonRegion.right, (int) mButtonRegion.bottom);

        setIntensitySelectorPosition(0.5f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mBackgroundDrawable.setAlpha(mEnabled ? 255 : 84);
        mBackgroundDrawable.draw(canvas);
        if(mLedOn) {
            mButtonOnDrawable.setAlpha(mEnabled ? 255 : 40);
            mButtonOnDrawable.draw(canvas);
        }
        else {
            mButtonOffDrawable.setAlpha(mEnabled ? 255 : 40);
            mButtonOffDrawable.draw(canvas);
        }
        mTextPaint.setAlpha(mEnabled ? 255 : 40);
        canvas.drawText(mText, mRegionPadded.left + (mRegionPadded.width()-mTextPaint.measureText(mText))/2.0f, mRegionPadded.top + mRegionPadded.height()*0.78f + mTextHeight, mTextPaint);
        if(mEnabled){
            mIntensitySelectorDrawable.setAlpha(mLedOn ? 255 : 100);
            mIntensitySelectorDrawable.draw(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float intAngOffset = 0.09f;
        float normalizedX, normalizedY;
        float rawX = event.getX();
        float rawY = event.getY();
        float polarAngle, polarRadius;
        normalizedX = (rawX - mRegionPadded.left) / mRegionPadded.width();
        normalizedY = (rawY - mRegionPadded.top) / mRegionPadded.height();
        normalizedX = (normalizedX - 0.5f) * 2.0f;
        normalizedY = (normalizedY - 0.5f) * 2.0f;
        polarAngle = (float)Math.atan2((double) normalizedX, -(double) normalizedY);
        polarAngle = (polarAngle + (float)Math.PI) / (2.0f * (float)Math.PI);
        polarRadius = (float)Math.sqrt(Math.pow((double) normalizedX, 2.0) + Math.pow((double) normalizedY, 2.0));

        if(event.getAction() == MotionEvent.ACTION_DOWN){
            if(mEnabled) {
                if(normalizedX >= -1.0f && normalizedX <= 1.0f && normalizedY >= -1.0f && normalizedY < 1.0f){
                    //Log.d("App", "angle: " + String.valueOf(polarAngle) + ", radius: " + String.valueOf(polarRadius));
                    // Check if middle button is pressed
                    if(polarRadius < 0.5f)
                    {
                        mLedOn = !mLedOn;
                        if(mLedChangeListener != null){
                            mLedChangeListener.onIntensityChanged(this, mLedOn, mLedIntensity);
                        }
                    }
                    // Check if intensity ring is pressed
                    else if(mLedOn && polarRadius < 0.85f && (polarAngle > intAngOffset && polarAngle < (1.0f - intAngOffset))) {
                        mLedIntensity = (polarAngle - intAngOffset) / (1.0f - 2.0f * intAngOffset);
                        setIntensitySelectorPosition(polarAngle);
                        mInIntensityDrag = true;
                        if(mLedChangeListener != null){
                            mLedChangeListener.onIntensityChanged(this, mLedOn, mLedIntensity);
                        }
                    }
                    invalidate();
                    return true;
                }
            }
        }
        else if(event.getAction() == MotionEvent.ACTION_MOVE){
            if(mInIntensityDrag){
                if(polarRadius > 0.3f) {
                    Log.d("App", "angle: " + String.valueOf(polarAngle) + ", radius: " + String.valueOf(polarRadius));
                    if (polarAngle < intAngOffset) {
                        mLedIntensity = 0.0f;
                        setIntensitySelectorPosition(intAngOffset);
                    } else if (polarAngle > (1.0f - intAngOffset)) {
                        mLedIntensity = 1.0f;
                        setIntensitySelectorPosition(1.0f - intAngOffset);
                    } else {
                        mLedIntensity = (polarAngle - intAngOffset) / (1.0f - 2.0f * intAngOffset);
                        setIntensitySelectorPosition(polarAngle);
                    }
                    invalidate();
                    return true;
                }
                else mInIntensityDrag = false;
            }
        }
        else if(event.getAction() == MotionEvent.ACTION_UP){
            mInIntensityDrag = false;
        }

        return false;
    }

    public interface OnLedChangeListener {
        void onIntensityChanged(IntensityLedButton sender, boolean ledOn, float ledIntensity);
    }
}
