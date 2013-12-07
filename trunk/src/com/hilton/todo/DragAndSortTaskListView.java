package com.hilton.todo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.mobeta.android.dslv.DragSortListView;

public class DragAndSortTaskListView extends DragSortListView {
    private GestureDetector mGestureDetector;
    
    public DragAndSortTaskListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setGestureDetector(GestureDetector detector) {
        mGestureDetector = detector;
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mGestureDetector != null) {
            mGestureDetector.onTouchEvent(ev);
        }
        return super.onTouchEvent(ev);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawEmptyDividers(canvas);
    }

    private void drawEmptyDividers(Canvas canvas) {
        final int itemHeight = (int) getContext().getResources().getDimension(R.dimen.list_view_empty_item_height);
        int bottom = 0;
        if (getChildCount() > 0) {
            bottom = getChildAt(getChildCount() - 1).getBottom();
        }
        final int height = getHeight();
        while (true) {
            if (bottom > height) {
                return;
            }
            Paint divider = new Paint();
            divider.setColor(getContext().getResources().getColor(R.color.divider));
            divider.setStyle(Paint.Style.STROKE);
            float depth = bottom + itemHeight;
            canvas.drawLine(0.0F, depth, getWidth(), depth, divider);
            bottom += itemHeight;
        }
    }
}