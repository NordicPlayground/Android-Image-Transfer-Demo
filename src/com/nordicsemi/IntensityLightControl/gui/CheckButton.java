package com.nordicsemi.IntensityLightControl.gui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.nordicsemi.IntensityLightControl.R;

/**
 * Created by too1 on 06.02.14.
 */
public class CheckButton extends View {
    private String mText, mTextChecked, mTextUnchecked;
    private float mTextHeight;
    private int mTextColor;
    private Rect mTextBounds = new Rect(), mStatTextBounds = new Rect();
    private float mTextLeft, mTextTop, mStatTextLeft, mStatTextTop;
    private boolean mShowStatusText;
    private boolean mChecked, mEnabled;

    private float mIconWidth, mIconHeight, mIconCenterY;
    private RectF mIconRegion;

    private RectF mRegion, mRegionPadded;

    private Paint mTextPaint;//, mBackgroundPaint, mForegroundPaint;
    private Drawable mCheckedDrawable, mUncheckedDrawable, mBackgroundDrawable;

    GestureDetector mDetector;

    OnClickListener mOnClickListener = null;

    public CheckButton(Context context, AttributeSet attrs){
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.CheckButton,
                0, 0);

        try{
            mText               = a.getString(R.styleable.CheckButton_text);
            mTextChecked        = a.getString(R.styleable.CheckButton_textChecked);
            mTextUnchecked      = a.getString(R.styleable.CheckButton_textUnChecked);
            mTextHeight         = a.getDimension(R.styleable.CheckButton_textHeight, 0.0f);
            mTextColor          = a.getColor(R.styleable.CheckButton_textColor, Color.WHITE);
            mChecked            = a.getBoolean(R.styleable.CheckButton_checked, false);
            mEnabled            = a.getBoolean(R.styleable.CheckButton_enabled, true);
            mBackgroundDrawable = a.getDrawable(R.styleable.CheckButton_backgroundDrawable);
            mCheckedDrawable    = a.getDrawable(R.styleable.CheckButton_iconCheckedDrawable);
            mUncheckedDrawable  = a.getDrawable(R.styleable.CheckButton_iconUncheckedDrawable);
            mIconWidth          = a.getDimension(R.styleable.CheckButton_iconWidth, 10.0f);
            mIconHeight         = a.getDimension(R.styleable.CheckButton_iconHeight, 10.0f);
            mIconCenterY        = a.getFloat(R.styleable.CheckButton_iconCenterY, 0.5f);
        }finally {
            a.recycle();
        }
        mShowStatusText = (mTextChecked != "" || mTextUnchecked != "");
        init();

        // Create our gesturedetector, based on the private mListener class
        mDetector = new GestureDetector(CheckButton.this.getContext(), new mListener());
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
        mIconRegion = new RectF();
    }

    public boolean isChecked(){
        return mChecked;
    }

    public void setChecked(boolean checked) {
        if(mChecked != checked) {
            mChecked = checked;
            invalidate();
        }
    }

    public boolean isEnabled(){
        return mEnabled;
    }

    public void setEnabled(boolean enabled){
        mEnabled = enabled;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float xpad = (float)(getPaddingLeft() + getPaddingRight());
        float ypad = (float)(getPaddingTop() + getPaddingBottom());
        mRegion.set(getLeft(), getTop(), (float)w, (float)h);
        mRegionPadded.set(getPaddingLeft(), getPaddingTop(), (float) w - getPaddingRight(), (float) h - getPaddingBottom());
        mBackgroundDrawable.setBounds((int) mRegionPadded.left, (int) mRegionPadded.top, (int) mRegionPadded.right, (int) mRegionPadded.bottom);
        mTextPaint.getTextBounds(mText, 0, mText.length(), mTextBounds);
        mTextLeft = (getWidth() - mTextBounds.width()) / 2;
        mTextTop  = mTextHeight + getPaddingTop();
        mStatTextTop = mRegionPadded.bottom - getPaddingBottom();
        mIconRegion.set((getWidth() - mIconWidth) * 0.5f,
                        getHeight() * mIconCenterY - mIconHeight * 0.5f,
                        (getWidth() + mIconWidth) * 0.5f,
                        getHeight() * mIconCenterY + mIconHeight * 0.5f);
        mCheckedDrawable.setBounds((int)mIconRegion.left, (int)mIconRegion.top, (int)mIconRegion.right, (int)mIconRegion.bottom);
        mUncheckedDrawable.setBounds((int)mIconRegion.left, (int)mIconRegion.top, (int)mIconRegion.right, (int)mIconRegion.bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mBackgroundDrawable.draw(canvas);
        mTextPaint.getTextBounds(mText, 0, mText.length(), mTextBounds);
        canvas.drawText(mText, mTextLeft, mTextTop, mTextPaint);
        if(mShowStatusText){
            if(mChecked){
                mTextPaint.getTextBounds(mTextChecked, 0, mTextChecked.length(), mStatTextBounds);
                canvas.drawText(mTextChecked, (getWidth() - mStatTextBounds.width()) * 0.5f, mStatTextTop, mTextPaint);
            }
            else {
                mTextPaint.getTextBounds(mTextUnchecked, 0, mTextUnchecked.length(), mStatTextBounds);
                canvas.drawText(mTextUnchecked, (getWidth() - mStatTextBounds.width()) * 0.5f, mStatTextTop, mTextPaint);
            }
        }
        if(mChecked){
            mCheckedDrawable.draw(canvas);
        }
        else {
            mUncheckedDrawable.draw(canvas);
        }
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result;// = mDetector.onTouchEvent(event);
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            if(mEnabled) {
                mChecked = !mChecked;
                if(mOnClickListener != null) mOnClickListener.onClick(this);
                invalidate();
                return true;
            }
        }
        return false;
    }

    private class mListener extends GestureDetector.SimpleOnGestureListener{
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {

            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {

            return true;
        }
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        mOnClickListener = l;
    }
}
