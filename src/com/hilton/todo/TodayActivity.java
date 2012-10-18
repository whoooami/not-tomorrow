package com.hilton.todo;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.hilton.todo.Task.ProjectionIndex;
import com.hilton.todo.Task.TaskColumns;

public class TodayActivity extends Activity {
    protected static final String TAG = "TodayActivity";
    private static final int START_TOMORROW = 10;
    private static final int VIEW_HISTORY = 11;
    private ListView mTaskList;
    private EditText mAddTaskEditor;
    private LayoutInflater mFactory;
    private GestureDetector mGestureDetector;
    private SwitchGestureListener mSwitchGestureListener;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.today_activity);
        final TextView header = (TextView) findViewById(R.id.header);
        final Calendar date = new GregorianCalendar();
        header.setText(getString(R.string.today).replace("#", new SimpleDateFormat(getString(R.string.date_format)).format(date.getTime())));
        
        mFactory = LayoutInflater.from(getApplication());
        mTaskList = (ListView) findViewById(R.id.task_list);
        final View headerView = mFactory.inflate(R.layout.header_view, null);
        mTaskList.addHeaderView(headerView);
        mAddTaskEditor = (EditText) headerView.findViewById(R.id.task_editor);
        mAddTaskEditor.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View view, int keycode, KeyEvent event) {
        	if (keycode == KeyEvent.KEYCODE_DPAD_CENTER || keycode == KeyEvent.KEYCODE_ENTER) {
        	    // finish editing
        	    final InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        	    inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        	    final String text = mAddTaskEditor.getText().toString();
        	    if (!TextUtils.isEmpty(text)) {
        		final ContentValues values = new ContentValues(1);
        		values.put(TaskColumns.TASK, text);
        		values.put(TaskColumns.TYPE, Task.TYPE_TODAY);
        		getContentResolver().insert(Task.CONTENT_URI, values);
        	    }
        	    mAddTaskEditor.setText("");
        	}
        	return false;
            }
        });
        final Cursor cursor = getContentResolver().query(Task.CONTENT_URI, Task.PROJECTION, TaskColumns.TYPE + " = " + Task.TYPE_TODAY, null, null);
        final TaskAdapter adapter = new TaskAdapter(getApplication(), cursor);
        mTaskList.setAdapter(adapter);
        mSwitchGestureListener = new SwitchGestureListener();
        mGestureDetector = new GestureDetector(getApplication(), mSwitchGestureListener);
        mTaskList.setOnTouchListener(new OnTouchListener() {
	    @Override
	    public boolean onTouch(View v, MotionEvent event) {
		return mGestureDetector.onTouchEvent(event);
	    }
	});
    }
    
    @Override
    public void onResume() {
	super.onRestart();
	mSwitchGestureListener.reset();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	menu.add(0, START_TOMORROW, 0, R.string.goto_tomorrow);
	menu.add(0, VIEW_HISTORY, 0, R.string.view_history);
	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	switch (item.getItemId()) {
	case START_TOMORROW:
	    gotoTomorrow();
	    break;
	case VIEW_HISTORY:
	    final Intent i = new Intent();
	    i.setClass(getApplication(), TaskHistoryActivity.class);
	    startActivity(i);
	    overridePendingTransition(R.anim.activity_enter_in, R.anim.activity_enter_out);
	default:
	    break;
	}
	return super.onOptionsItemSelected(item);
    }

    private void gotoTomorrow() {
	final Intent i = new Intent();
	i.setClass(getApplication(), TomorrowActivity.class);
	startActivity(i);
	overridePendingTransition(R.anim.activity_enter_in, R.anim.activity_enter_out);
    }
    
    private class TaskAdapter extends CursorAdapter {
        private Cursor mCursor;
        
        public TaskAdapter(Context context, Cursor c) {
            super(context, c);
            mCursor = c;
        }
        
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            if (view  == null) {
                view = mFactory.inflate(R.layout.today_task_item, null);
            }
            /*
             * Everytime bindView is called from a ListView, the views are not coming in the same order before bindView()
             * is called. For instance, you click a checkbox which causes data changed event, ListView will refresh to
             * respond data change event. Views are reused, but not coming in the same order before clicking. The views 
             * are not coming in the same order as before, so bindView need to refill views with data from reliable database.
             * As a result, anything relying on the view objects becomes unreliable. Your code here, checked or not, show
             * image or checkbox, are relying view object and the switcher object. The switchers do remember their showing
             * status(which child to show), but they are not showed in the same order as before, so in your perspective,
             * some switchers show checkbox but others show image which they are supposed to show checkboxes. They do show
             * checkboxes, only they are arranged in different positions.
             * That's why "Do it Tomorrow" app use a separate mode to delete rather than in your way, becuase it is almost
             * impossible to trust view.
             */
            final ViewSwitcher switcher = (ViewSwitcher) view.findViewById(R.id.action_switcher);
            if (switcher.getDisplayedChild() == 1) {
        	switcher.clearAnimation();
        	switcher.showPrevious();
        	switcher.clearAnimation();
            }
            final CheckBox toggle = (CheckBox) view.findViewById(R.id.action_toggle_done);
            final short done = cursor.getShort(ProjectionIndex.DONE);
            final int id = cursor.getInt(ProjectionIndex.ID);
            /*
             * 1. bindView will be called every time refresh the UI, so onCheckedChangedListener will be changed to the last one set.
             * 2. setChecked would invoke onCheckedChanged.
             * 3. need to remove original listener and set again, to keep listener correct and up-to-date.
             * We must remove old one before setting new one, otherwise, this toggle.setChecked would invoke current listener 
             * which might not be correct, resulting in change other button's state.
             */
            toggle.setOnCheckedChangeListener(null);
            toggle.setChecked(done != 0);
            toggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
        	@Override
        	public void onCheckedChanged(CompoundButton view, boolean checked) {
        	    final Uri uri = ContentUris.withAppendedId(Task.CONTENT_URI, id);
        	    final ContentValues values = new ContentValues(1);
        	    values.put(TaskColumns.DONE, checked ? 1 : 0);
        	    getContentResolver().update(uri, values, null, null);
        	}

            });
            view.setOnClickListener(new OnClickListener() {
	        @Override
	        public void onClick(View v) {
	            switcher.showNext();
	            if (switcher.getDisplayedChild() == 0) {
	        	switcher.getInAnimation().setAnimationListener(null);
	        	return;
	            }
	            final ImageView delete = (ImageView) v.findViewById(R.id.action_delete_task);
	            delete.setOnClickListener(new OnClickListener() {
		        @Override
		        public void onClick(View v) {
		            switcher.getInAnimation().setAnimationListener(new AnimationListener() {
		        	@Override
		        	public void onAnimationEnd(Animation animation) {
		        	    switcher.getInAnimation().setAnimationListener(null);
		        	    final Uri uri = ContentUris.withAppendedId(Task.CONTENT_URI, id);
		        	    getContentResolver().delete(uri, null, null);
		        	}
		        	
		        	@Override
		        	public void onAnimationRepeat(Animation animation) {
		        	}
		        	
		        	@Override
		        	public void onAnimationStart(Animation animation) {
		        	}
		            });
		            switcher.showPrevious();
		        }
		    });
	        }
	    });
            view.setOnTouchListener(new OnTouchListener() {
	        @Override
	        public boolean onTouch(View v, MotionEvent event) {
	            return mGestureDetector.onTouchEvent(event);
	        }
	    });
            TextView task = (TextView) view.findViewById(R.id.task);
            final String taskContent = cursor.getString(ProjectionIndex.TASK);
            if (done != 0) {
        	final Spannable style = new SpannableString(taskContent);
        	style.setSpan(new StrikethroughSpan(), 0, taskContent.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        	style.setSpan(new StyleSpan(Typeface.ITALIC) , 0, taskContent.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        	task.setText(style);
        	task.setTextAppearance(getApplication(), R.style.done_task_item_text);
            } else {
        	task.setText(taskContent);
        	task.setTextAppearance(getApplication(), R.style.task_item_text);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = mFactory.inflate(R.layout.today_task_item, null);
            return view;
        }
        
        @Override
        public void onContentChanged() {
            mCursor.requery();
        }
    }
    
    private class SwitchGestureListener extends SimpleOnGestureListener {
	private boolean mGestureDetected;
	
	public SwitchGestureListener() {
	    mGestureDetected = false;
	}
	
	public void reset() {
	    mGestureDetected = false;
	}
	
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
	    if (mGestureDetected) {
		return false;
	    }
            if (distanceY*distanceY > distanceX*distanceX) {
                return false;
            }
	    if ((distanceY > -2 || distanceY < 2) && distanceX > 10) {
		mGestureDetected = true;
		gotoTomorrow();
	    } else {
		mGestureDetected = false;
	    }
	    return mGestureDetected;
	}
    }
}