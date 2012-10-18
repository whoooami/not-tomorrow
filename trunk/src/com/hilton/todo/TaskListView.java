package com.hilton.todo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ListView;

public class TaskListView extends ListView {
    public static final String TAG = TaskListView.class.getSimpleName();
    private Context mContext;

    public TaskListView(Context paramContext) {
	super(paramContext);
	mContext = paramContext;
    }

    public TaskListView(Context paramContext, AttributeSet paramAttributeSet) {
	super(paramContext, paramAttributeSet, 0);
	mContext = paramContext;
    }

    public TaskListView(Context paramContext, AttributeSet paramAttributeSet,
	    int paramInt) {
	super(paramContext, paramAttributeSet, paramInt);
	mContext = paramContext;
    }
    
    @Override
    public void onDraw(Canvas paramCanvas) {
	Paint divider = new Paint();
	divider.setColor(mContext.getResources().getColor(R.color.divider));
	divider.setStyle(Paint.Style.STROKE);
	paramCanvas.drawLine(0.0F, 0.0F, getWidth(), 0.0F, divider);
	final int itemHeight = (int) (50.0F * mContext.getResources().getDisplayMetrics().density);
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
	    divider.setColor(mContext.getResources().getColor(R.color.divider));
	    divider.setStyle(Paint.Style.STROKE);
	    float depth = bottom + itemHeight;
	    paramCanvas.drawLine(0.0F, depth, getWidth(), depth, divider);
	    bottom += itemHeight;
	}
    }
}