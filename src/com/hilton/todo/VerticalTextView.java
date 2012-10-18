package com.hilton.todo;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Path;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.TextView;

/*
 * for the attributes of TextView, some works some not.
 * 1. setTextsize works
 * 2. setBackgroundColor works
 * 3. setTextColor also works
 * You can adjust the size of TextView by margins and the drawing area by paddings(only paddingTop and paddingBottom works).
 * For other attributes, like lines or maxLines, or ellipsize are not supported currently. To support them, you should get 
 * the attributes value before drawText and apply them.
 */
public class VerticalTextView extends TextView {
    private static final String TAG = "VerticalTextView";

    public VerticalTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public VerticalTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VerticalTextView(Context context) {
        super(context);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        final ColorStateList csl = getTextColors();
        final int color = csl.getDefaultColor();
        final int paddingBottom = getPaddingBottom();
        final int paddingTop = getPaddingTop();
        final int viewWidth = getWidth();
        final int viewHeight = getHeight();
        final TextPaint paint = getPaint();
        paint.setColor(color);
        final float bottom = viewWidth * 9.0f / 11.0f;
        Path p = new Path();
        p.moveTo(bottom, viewHeight - paddingBottom - paddingTop);
        p.lineTo(bottom, paddingTop);
        canvas.drawTextOnPath(getText().toString(), p, 0, 0, paint);
    }
}