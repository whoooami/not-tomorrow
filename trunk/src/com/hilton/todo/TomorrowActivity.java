package com.hilton.todo;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hilton.todo.Task.ProjectionIndex;
import com.hilton.todo.Task.TaskColumns;

public class TomorrowActivity extends Activity {
    protected static final String TAG = "TomorrowActivity";
    private static int sTaskOrder = 0;
    private TaskListView mTaskList;
    private EditText mAddTaskEditor;
    private LayoutInflater mFactory;
    private GestureDetector mGestureDetector;
    protected AlertDialog mDialogEditTask;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.today_activity);
        final TextView header = (TextView) findViewById(R.id.header);
        final Calendar date = new GregorianCalendar();
        date.add(Calendar.DAY_OF_YEAR, 1);
        header.setText(getString(R.string.tomorrow).replace("#", new SimpleDateFormat(getString(R.string.date_format)).format(date.getTime())));
        
        mFactory = LayoutInflater.from(getApplication());
        mTaskList = (TaskListView) findViewById(R.id.task_list);
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
        		values.put(TaskColumns.TYPE, Task.TYPE_TOMORROW);
        		final Calendar tomorrow = new GregorianCalendar();
        		tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        		tomorrow.set(Calendar.HOUR_OF_DAY, 00);
        		tomorrow.set(Calendar.MINUTE, 00);
        		tomorrow.set(Calendar.SECOND, ++sTaskOrder);
        		values.put(TaskColumns.MODIFIED, tomorrow.getTimeInMillis());
        		values.put(TaskColumns.DAY, tomorrow.get(Calendar.DAY_OF_YEAR));
        		getContentResolver().insert(Task.CONTENT_URI, values);
        	    }
        	    mAddTaskEditor.setText("");
        	}
        	return false;
            }
        });
        final Cursor cursor = getContentResolver().query(Task.CONTENT_URI, Task.PROJECTION, TaskColumns.TYPE + " = " + Task.TYPE_TOMORROW, null, null);
        final TaskAdapter adapter = new TaskAdapter(getApplication(), cursor);
        mTaskList.setAdapter(adapter);
        mGestureDetector = new GestureDetector(new SwitchGestureListener());
        mTaskList.setOnTouchListener(new OnTouchListener() {
	    @Override
	    public boolean onTouch(View v, MotionEvent event) {
		return mGestureDetector.onTouchEvent(event);
	    }
	});
    }

    @Override
    public void onBackPressed() {
	super.onBackPressed();
	setTransitionAnimation();
    }

    private void setTransitionAnimation() {
	overridePendingTransition(R.anim.activity_leave_in, R.anim.activity_leave_out);
    }
    
    private class TaskAdapter extends CursorAdapter {
        public TaskAdapter(Context context, Cursor c) {
            super(context, c);
        }
        
	@Override
        public void bindView(View view, Context context, Cursor cursor) {
            if (view  == null) {
                view = mFactory.inflate(R.layout.tomorrow_task_item, null);
            }

            final String taskContent = cursor.getString(ProjectionIndex.TASK);
            final short done = cursor.getShort(ProjectionIndex.DONE);
            final int id = cursor.getInt(ProjectionIndex.ID);
            final Uri uri = ContentUris.withAppendedId(Task.CONTENT_URI, id);
            final ImageView move = (ImageView) view.findViewById(R.id.action_move_to_today);
            move.setOnClickListener(new OnClickListener() {
	        @Override
	        public void onClick(View v) {
	    	    final ContentValues values = new ContentValues(2);
	    	    values.put(TaskColumns.TYPE, Task.TYPE_TODAY);
	    	    final Calendar today = new GregorianCalendar();
	    	    values.put(TaskColumns.MODIFIED, today.getTimeInMillis());
	    	    values.put(TaskColumns.DAY, today.get(Calendar.DAY_OF_YEAR));
	    	    getContentResolver().update(uri, values, null, null);
	    	    Toast.makeText(getApplication(), getString(R.string.move_to_today_tip).replace("#", taskContent), Toast.LENGTH_SHORT).show();
	        }
	    });

            view.setOnLongClickListener(new OnLongClickListener() {
        	@Override
        	public boolean onLongClick(View v) {
        	    final View textEntryView = mFactory.inflate(R.layout.dialog_edit_task, null);
        	    mDialogEditTask = new AlertDialog.Builder(TomorrowActivity.this)
        	    .setIcon(android.R.drawable.ic_dialog_alert)
        	    .setTitle(R.string.dialog_edit_title)
        	    .setView(textEntryView)
        	    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dialog, int whichButton) {
        		    final EditText box = (EditText) mDialogEditTask.findViewById(R.id.edit_box);
        		    final ContentValues cv = new ContentValues();
        		    cv.put(TaskColumns.TASK, box.getText().toString());
        		    getContentResolver().update(uri, cv, null, null);
        		}
        	    })
        	    .setNegativeButton(android.R.string.cancel, null)
        	    .create();
        	    mDialogEditTask.show();
        	    EditText box = (EditText) mDialogEditTask.findViewById(R.id.edit_box);
        	    box.setText(taskContent);
        	    return true;
        	}
            });
            view.setOnTouchListener(new OnTouchListener() {
	        @Override
	        public boolean onTouch(View v, MotionEvent event) {
	            return mGestureDetector.onTouchEvent(event);
	        }
	    });
            TextView task = (TextView) view.findViewById(R.id.task);
            task.setText(taskContent);
            task.setTextAppearance(getApplication(), R.style.task_item_text);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = mFactory.inflate(R.layout.tomorrow_task_item, null);
            return view;
        }
    }
    
    private class SwitchGestureListener extends SimpleOnGestureListener {
	private boolean mGestureDetected;
	
	public SwitchGestureListener() {
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
	    if ((distanceY > -2 || distanceY < 2) && distanceX < -10) {
		mGestureDetected = true;
		finish();
		setTransitionAnimation();
	    } else {
		mGestureDetected = false;
	    }
	    return mGestureDetected;
	}
    }
}