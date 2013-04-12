package com.hilton.todo;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
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

import com.hilton.todo.Task.ProjectionIndex;
import com.hilton.todo.Task.TaskColumns;

public class TodayTaskListView extends TaskListView {
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
    
    private boolean mDraggingMode;
    
    public TodayTaskListView(Context context, AttributeSet attrs) {
	this(context, attrs, 0);
    }

    public TodayTaskListView(Context paramContext,
	    AttributeSet paramAttributeSet, int paramInt) {
	super(paramContext, paramAttributeSet, paramInt);
        mTouchSlop = ViewConfiguration.get(paramContext).getScaledTouchSlop();
        mDraggingMode = false;
    }
    
    public void enterDragingMode() {
	if (!mDraggingMode) {
	    mDraggingMode = true;
	    invalidateViews();
	}
    }
    
    public void exitDraggingMode() {
	if (mDraggingMode) {
	    mDraggingMode = false;
	    invalidateViews();
	}
    }
    
    public boolean inDraggingMode() {
	return mDraggingMode;
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
	if (!mDraggingMode) {
	    return super.onInterceptTouchEvent(ev);
	}
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

	final View dragger = itemView.findViewById(R.id.dragger);
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
	if (!mDraggingMode) {
	    return super.onTouchEvent(ev);
	}
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
	    /*
	     * Why data won't change before?
	     * You see cursors return by #getItemAtPosition are the same cursor with positioning on different rows.
	     * So, the cursors should be one the same row positioned by last call to #getItemAtPosition.
	     *    Cursor c1 = #getItemAtPosition(0); // cursor.moveToPosition(0);
	     *    // get data from c1
	     *    Cursor c2 = #getItemAtPosition(2); // cursor.moveToPosition(2);
	     *    // get data from c2
	     *    Cursor c3 = #getItemAtPosition(4); // cursor.moveToPosition(4);
	     *    // get data from c4
	     *    
	     *    now c1, c2 and c3 are all positioned at 4.
	     *    So if you want to retrieve data on different row, you should get data after each #getItemAtPosition.
	     */
	    // swap modified time of mDragPosition and mDragSrcPosition
	    Log.e(TAG, "src " + mDragSrcPosition + ", dst " + mDragPosition + ", first visible " + getFirstVisiblePosition());
	    Log.e(TAG, "child count " + getChildCount());
	    Log.e(TAG, "adapter count " + getAdapter().getCount());
	    final Cursor src = (Cursor) getItemAtPosition(mDragSrcPosition - getFirstVisiblePosition());
	    Log.e(TAG, "----------------src cursor and current row of source cursor");
	    android.database.DatabaseUtils.dumpCurrentRow(src);
	    long srcModified = src.getLong(ProjectionIndex.MODIFIED);
	    final Uri srcUri = ContentUris.withAppendedId(Task.CONTENT_URI, src.getLong(ProjectionIndex.ID));
	    Log.e(TAG, "src uri " + srcUri);

	    Log.e(TAG, "----------------dst cursor and current row of dst currsor");
	    final Cursor dst = (Cursor) getItemAtPosition(mDragPosition - getFirstVisiblePosition());
	    android.database.DatabaseUtils.dumpCurrentRow(dst);
	    long dstModified = dst.getLong(ProjectionIndex.MODIFIED);
	    final Uri dstUri = ContentUris.withAppendedId(Task.CONTENT_URI, dst.getLong(ProjectionIndex.ID));
	    Log.e(TAG, "\t\t, dst uri " + dstUri);
	    Log.e(TAG, "srcm " + srcModified + ", dstM " + dstModified);
	    
	    final ContentValues values = new ContentValues(1);
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