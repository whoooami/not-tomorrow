package com.hilton.todo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;

public class PomodoroClockView extends View {
    public PomodoroClockView(Context context) {
	this(context, null);
    }

    public PomodoroClockView(Context context, AttributeSet attrs, int defStyle) {
	super(context, attrs, defStyle);
    }

    public PomodoroClockView(Context context, AttributeSet attrs) {
	this(context, attrs, 0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
	canvas.drawColor(Color.GRAY);
	super.onDraw(canvas);
    }
}