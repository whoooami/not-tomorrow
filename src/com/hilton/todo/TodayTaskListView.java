package com.hilton.todo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;

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
    private DropListener mDropListener;
    
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

	final View dragger = itemView.findViewById(R.id.drag_handle);
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
	    if (mDropListener != null) {
		mDropListener.drop(mDragSrcPosition - getFirstVisiblePosition(),
			mDragPosition - getFirstVisiblePosition());
	    }
	}
    }
    
    public void setDropListener(DropListener l) {
        mDropListener = l;
    }

    public interface DropListener {
        void drop(int from, int to);
    }
}