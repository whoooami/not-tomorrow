package com.hilton.todo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ListView;

public class TaskListView extends ListView {
    public static final String TAG = TaskListView.class.getSimpleName();

    public TaskListView(Context context, AttributeSet attrs) {
	this(context, attrs, 0);
    }

    public TaskListView(Context paramContext, AttributeSet paramAttributeSet, int paramInt) {
	super(paramContext, paramAttributeSet, paramInt);

    }
    
    @Override
    public void onDraw(Canvas paramCanvas) {
	Paint divider = new Paint();
	divider.setColor(getContext().getResources().getColor(R.color.divider));
	divider.setStyle(Paint.Style.STROKE);
	paramCanvas.drawLine(0.0F, 0.0F, getWidth(), 0.0F, divider);
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
	    divider = new Paint();
	    divider.setColor(getContext().getResources().getColor(R.color.divider));
	    divider.setStyle(Paint.Style.STROKE);
	    float depth = bottom + itemHeight;
	    paramCanvas.drawLine(0.0F, depth, getWidth(), depth, divider);
	    bottom += itemHeight;
	}
    }
}