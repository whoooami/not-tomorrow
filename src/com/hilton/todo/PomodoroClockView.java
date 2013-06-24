package com.hilton.todo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class PomodoroClockView extends View {
    private static final String TAG = "PomodoroClockView";
    private float mClockRadius;
    private RectF mWorkingArea;
    private RectF mRestArea;
    private Paint mRestPaint;
    private Paint mWorkingPaint;
    private Paint mCountDownPaint;
    private float mSweepAngle;
    private Paint mMarkerPaint;
    private int mMarkerX;
    private int mMarkerY;
    private int mMarkerHeight;
    private Paint mMarkerTextPaint;
    private float mCenterX;
    private float mCenterY;
    
    public PomodoroClockView(Context context) {
	this(context, null);
    }

    public PomodoroClockView(Context context, AttributeSet attrs) {
	this(context, attrs, 0);
    }
    
    public PomodoroClockView(Context context, AttributeSet attrs, int defStyle) {
	super(context, attrs, defStyle);
	mRestArea = new RectF();
	mWorkingArea = new RectF();
	
	mRestPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	mRestPaint.setColor(Color.GREEN);
	mRestPaint.setStyle(Style.FILL_AND_STROKE);
	
	mWorkingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	mWorkingPaint.setColor(Color.RED);
	mWorkingPaint.setStyle(Style.FILL_AND_STROKE);
	
	mCountDownPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	mCountDownPaint.setColor(Color.GRAY);
	mCountDownPaint.setAlpha(200);
	mCountDownPaint.setStyle(Style.FILL);
	mSweepAngle = 0.0f;
	
	mMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	mMarkerPaint.setColor(Color.BLUE);
	mMarkerPaint.setStyle(Style.STROKE);
	mMarkerPaint.setStrokeWidth(2.0f);
	
	mMarkerTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	mMarkerTextPaint.setColor(Color.BLUE);
	mMarkerHeight = (int) context.getResources().getDimension(R.dimen.pomodoro_clock_marker_height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
	super.onSizeChanged(w, h, oldw, oldh);
	mClockRadius = Math.min(w, h) / 3.0f;
	final float top = h/2.0f - mClockRadius;
	final float left = w/2.0f - mClockRadius;
	mRestArea.top = top;
	mRestArea.left = left;
	mRestArea.right = left + mClockRadius;
	mRestArea.bottom = top + mClockRadius;
	
	mWorkingArea.top = top;
	mWorkingArea.left = left;
	mWorkingArea.bottom = top + 2 * mClockRadius;
	mWorkingArea.right = left + 2 * mClockRadius;
	mMarkerX = w / 2;
	mMarkerY = (int) top;
	mCenterX = mMarkerX;
	mCenterY = h / 2;
	Log.e(TAG, "onsize changed ");
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
	Log.e(TAG, "on draw sweep angle " + mSweepAngle);
	canvas.drawArc(mWorkingArea, 210, 60, true, mRestPaint);
	canvas.drawArc(mWorkingArea, 270, 300, true, mWorkingPaint);
	canvas.drawArc(mWorkingArea, 270, mSweepAngle, true, mCountDownPaint);
	canvas.drawLine(mMarkerX, mMarkerY, mMarkerX, mMarkerHeight + mMarkerY, mMarkerPaint);
	canvas.drawText("0", mMarkerX - 3, mMarkerY - 5, mMarkerTextPaint);
	canvas.save();
	canvas.rotate(60, mCenterX, mCenterY);
	canvas.drawLine(mMarkerX, mMarkerY, mMarkerX, mMarkerHeight + mMarkerY, mMarkerPaint);
	canvas.drawText("5", mMarkerX - 3, mMarkerY - 5, mMarkerTextPaint);
	canvas.save();
	canvas.rotate(60, mCenterX, mCenterY);
	canvas.drawLine(mMarkerX, mMarkerY, mMarkerX, mMarkerHeight + mMarkerY, mMarkerPaint);
	canvas.drawText("10", mMarkerX - 5, mMarkerY - 5, mMarkerTextPaint);
	canvas.save();
	canvas.rotate(60, mCenterX, mCenterY);
	canvas.drawLine(mMarkerX, mMarkerY, mMarkerX, mMarkerHeight + mMarkerY, mMarkerPaint);
	canvas.drawText("15", mMarkerX - 7, mMarkerY - 5, mMarkerTextPaint);
	canvas.save();
	canvas.rotate(60, mCenterX, mCenterY);
	canvas.drawLine(mMarkerX, mMarkerY, mMarkerX, mMarkerHeight + mMarkerY, mMarkerPaint);
	canvas.drawText("20", mMarkerX - 5, mMarkerY - 5, mMarkerTextPaint);
	canvas.save();
	canvas.rotate(60, mCenterX, mCenterY);
	canvas.drawLine(mMarkerX, mMarkerY, mMarkerX, mMarkerHeight + mMarkerY, mMarkerPaint);
	canvas.drawText("25", mMarkerX - 5, mMarkerY - 5, mMarkerTextPaint);
	canvas.restore();
	canvas.restore();
	canvas.restore();
	canvas.restore();
	canvas.restore();
	super.onDraw(canvas);
	mSweepAngle += 1.0f;
	postInvalidateDelayed(5000, (int) mWorkingArea.left, (int) mWorkingArea.top, (int) mWorkingArea.right, (int) mWorkingArea.bottom);
    }
    
    public void setSweepAngle(final float s) {
	mSweepAngle = s;
    }
}