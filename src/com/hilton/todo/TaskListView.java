package com.hilton.todo;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import com.hilton.todo.Task.ProjectionIndex;
import com.hilton.todo.Task.TaskColumns;

public class TaskListView extends ListView {
    public static final String TAG = TaskListView.class.getSimpleName();
    private ImageView mDragView;
    private int mDragSrcPosition;
    private int mDragPosition;
    private int mDragPoint;
    private int mDragOffset;
    private int mScrollUpBound;
    private int mScrollDownBound;
    private final int mTouchSlop;
    
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowParams;
    private DragListener mDragListener;
    private DropListener mDropListener;
    private RemoveListener mRemoveListener;
    
    public TaskListView(Context context, AttributeSet attrs) {
	this(context, attrs, 0);
    }

    public TaskListView(Context paramContext, AttributeSet paramAttributeSet, int paramInt) {
	super(paramContext, paramAttributeSet, paramInt);
        mTouchSlop = ViewConfiguration.get(paramContext).getScaledTouchSlop();
    }
    
    @Override
    public void onDraw(Canvas paramCanvas) {
	Paint divider = new Paint();
	divider.setColor(getContext().getResources().getColor(R.color.divider));
	divider.setStyle(Paint.Style.STROKE);
	paramCanvas.drawLine(0.0F, 0.0F, getWidth(), 0.0F, divider);
	final int itemHeight = (int) (50.0F * getContext().getResources().getDisplayMetrics().density);
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

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
	if (ev.getAction() != MotionEvent.ACTION_DOWN) {
	    return super.onInterceptTouchEvent(ev);
	}
	final int x = (int) ev.getX();
	final int y = (int) ev.getY();
	mDragSrcPosition = pointToPosition(x, y);
	mDragPosition = mDragSrcPosition;
	if (mDragPosition == AdapterView.INVALID_POSITION) {
	    return super.onInterceptTouchEvent(ev);
	}
	final ViewGroup itemView = (ViewGroup) getChildAt(mDragPosition - getFirstVisiblePosition());
	mDragPoint = y - itemView.getTop();
	mDragOffset = (int) (ev.getRawY() - y);

	final View dragger = itemView.findViewById(R.id.dragging);
	if (dragger != null && x > dragger.getLeft() - 20) {
	    mScrollUpBound = Math.min(y-mTouchSlop, getHeight()/3);
	    mScrollDownBound = Math.max(y+mTouchSlop, getHeight()*2/3);
	    itemView.setDrawingCacheEnabled(true);
	    final Bitmap bm = Bitmap.createBitmap(itemView.getDrawingCache());
	    startDrag(bm, y);
	}
	return false;
    }
    
    private void startDrag(Bitmap bm, int y) {
	stopDrag();
	
	mWindowParams = new WindowManager.LayoutParams();
	mWindowParams.gravity = Gravity.TOP;
	mWindowParams.x = 0;
	mWindowParams.y = y - mDragPoint + mDragOffset;
	mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
	mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
	mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
		WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
		WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
		WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
	mWindowParams.format = PixelFormat.TRANSLUCENT;
	mWindowParams.windowAnimations = 0;
	
	final ImageView imageView = new ImageView(getContext());
	imageView.setImageBitmap(bm);
	mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
	mWindowManager.addView(imageView, mWindowParams);
	mDragView = imageView;
    }
    
    private void stopDrag() {
	if (mDragView != null) {
	    mWindowManager.removeView(mDragView);
	    mDragView = null;
	}
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
	if (mDragView == null || mDragPosition == AdapterView.INVALID_POSITION) {
	    return super.onTouchEvent(ev);
	}
	switch (ev.getAction()) {
	case MotionEvent.ACTION_UP:
	    final int upY = (int) ev.getY();
	    stopDrag();
	    onDrop(upY);
	    break;
	case MotionEvent.ACTION_MOVE:
	    final int moveY = (int) ev.getY();
	    onDrag(moveY);
	    break;
	default:
	    break;
	}
	return true;
    }
    
    private void onDrag(int y) {
	if (mDragView != null) {
	    mWindowParams.alpha =  0.8f;
	    mWindowParams.y = y - mDragPoint + mDragOffset;
	    mWindowManager.updateViewLayout(mDragView, mWindowParams);
	}
	
	final int tmpPostion = pointToPosition(0, y);
	if (tmpPostion != AdapterView.INVALID_POSITION) {
	    mDragPosition = tmpPostion;
	}
	
	int scrollHeight = 0;
	if (y < mScrollUpBound) { 
	    scrollHeight = 8;
	} else if (y > mScrollDownBound) {
	    scrollHeight = -8;
	}
	if (scrollHeight != 0) {
	    setSelectionFromTop(mDragPosition, getChildAt(mDragPosition - getFirstVisiblePosition()).getTop() + scrollHeight);
	}
    }
    
    private void onDrop(int y) {
	int tmpPostion = pointToPosition(0, y);
	if (tmpPostion != AdapterView.INVALID_POSITION) {
	    mDragPosition = tmpPostion;
	}
	if (y < getChildAt(0).getTop()) {
	    mDragPosition = 0;
	} else if (y > getChildAt(getChildCount() - 1).getBottom()) {
	    mDragPosition = getAdapter().getCount() - 1;
	}
	
	if (mDragPosition > 0 && mDragPosition < getAdapter().getCount()) {
	    // swap modified time of mDragPosition and mDragSrcPosition
	    Log.e(TAG, "src " + mDragSrcPosition + ", dst " + mDragPosition + ", first visible " + getFirstVisiblePosition());
	    Log.e(TAG, "child count " + getChildCount());
	    Log.e(TAG, "adapter count " + getAdapter().getCount());
	    final Cursor src = (Cursor) getItemAtPosition(mDragSrcPosition - getFirstVisiblePosition());
	    android.database.DatabaseUtils.dumpCursor(src);
	    Log.e(TAG, " src move to " + (mDragSrcPosition - 1) + ", " + src.moveToPosition(mDragSrcPosition - 1));
	    final Cursor dst = (Cursor) getItemAtPosition(mDragPosition - getFirstVisiblePosition());
	    Log.e(TAG, " dst move to " + (mDragPosition - 1) + ", " + dst.moveToPosition(mDragPosition - 1));
	    android.database.DatabaseUtils.dumpCursor(dst);
	    Log.e(TAG, "src " + src + ", dst " + dst);
	    android.database.DatabaseUtils.dumpCurrentRow(src);
	    android.database.DatabaseUtils.dumpCurrentRow(dst);
	    long srcModified = src.getLong(ProjectionIndex.MODIFIED);
	    long dstModified = dst.getLong(ProjectionIndex.MODIFIED);
	    Log.e(TAG, "srcm " + srcModified + ", dstM " + dstModified);
	    final ContentValues values = new ContentValues(1);
	    final Uri srcUri = ContentUris.withAppendedId(Task.CONTENT_URI, src.getLong(ProjectionIndex.ID));
	    final Uri dstUri = ContentUris.withAppendedId(Task.CONTENT_URI, dst.getLong(ProjectionIndex.ID));
	    values.put(TaskColumns.MODIFIED, dstModified);
	    getContext().getContentResolver().update(srcUri, values, null, null);
	    values.clear();
	    values.put(TaskColumns.MODIFIED, srcModified);
	    getContext().getContentResolver().update(dstUri, values, null, null);
	    Log.e(TAG, "data swapped, are you aware of that");
	}
    }
    public void setDragListener(DragListener l) {
        mDragListener = l;
    }
    
    public void setDropListener(DropListener l) {
        mDropListener = l;
    }
    
    public void setRemoveListener(RemoveListener l) {
        mRemoveListener = l;
    }

    public interface DragListener {
        void drag(int from, int to);
    }
    public interface DropListener {
        void drop(int from, int to);
    }
    public interface RemoveListener {
        void remove(int which);
    }
}