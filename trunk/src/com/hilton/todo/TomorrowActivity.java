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
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hilton.todo.TaskStore.ProjectionIndex;
import com.hilton.todo.TaskStore.TaskColumns;

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
        		values.put(TaskColumns.TYPE, TaskStore.TYPE_TOMORROW);
        		final Calendar tomorrow = new GregorianCalendar();
        		tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        		tomorrow.set(Calendar.HOUR_OF_DAY, 00);
        		tomorrow.set(Calendar.MINUTE, 00);
        		tomorrow.set(Calendar.SECOND, ++sTaskOrder);
        		values.put(TaskColumns.CREATED, tomorrow.getTimeInMillis());
        		values.put(TaskColumns.DAY, tomorrow.get(Calendar.DAY_OF_YEAR));
        		getContentResolver().insert(TaskStore.CONTENT_URI, values);
        	    }
        	    mAddTaskEditor.setText("");
        	}
        	return false;
            }
        });
        final Cursor cursor = getContentResolver().query(TaskStore.CONTENT_URI, TaskStore.PROJECTION, TaskColumns.TYPE + " = " + TaskStore.TYPE_TOMORROW, null, null);
        final TaskAdapter adapter = new TaskAdapter(getApplication(), cursor);
        mTaskList.setAdapter(adapter);
        mGestureDetector = new GestureDetector(new SwitchGestureListener());
        mTaskList.setOnTouchListener(new OnTouchListener() {
	    @Override
	    public boolean onTouch(View v, MotionEvent event) {
		return mGestureDetector.onTouchEvent(event);
	    }
	});
        registerForContextMenu(mTaskList);
    }

    @Override
    public void onDestroy() {
	super.onDestroy();
	unregisterForContextMenu(mTaskList);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	final Uri uri = ContentUris.withAppendedId(TaskStore.CONTENT_URI, info.id);
	switch (item.getItemId()) {
	case R.id.tomorrow_list_contextmenu_delete:
	    getContentResolver().delete(uri, null, null);
	    return true;
	case R.id.tomorrow_list_contextmenu_edit: {
	    final View textEntryView = mFactory.inflate(R.layout.dialog_edit_task, null);
	    final String content = getTaskContent(uri);
	    mDialogEditTask = new AlertDialog.Builder(TomorrowActivity.this)
	    .setIcon(android.R.drawable.ic_dialog_alert)
	    .setTitle(R.string.dialog_edit_title)
	    .setView(textEntryView)
	    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
		    final EditText box = (EditText) mDialogEditTask.findViewById(R.id.edit_box);
		    final String newContent = box.getText().toString();
		    if (content.equals(newContent)) {
			return;
		    }
		    final ContentValues cv = new ContentValues();
		    cv.put(TaskColumns.TASK, newContent);
		    cv.put(TaskColumns.MODIFIED, new GregorianCalendar().getTimeInMillis());
		    getContentResolver().update(uri, cv, null, null);
		}
	    })
	    .setNegativeButton(android.R.string.cancel, null)
	    .create();
	    mDialogEditTask.show();
	    EditText box = (EditText) mDialogEditTask.findViewById(R.id.edit_box);
	    box.setText(content);
	    return true;
	}
	default:
	    Log.e(TAG, "bad context menu id " + item.getItemId());
	    break;
	}
	return super.onContextItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
	final long id = ((AdapterContextMenuInfo) menuInfo).id;
	if (id <= 0) {
	    return;
	}
	getMenuInflater().inflate(R.menu.tomorrow_contextmenu, menu);
	final Uri uri = ContentUris.withAppendedId(TaskStore.CONTENT_URI, id);
	final String task = getTaskContent(uri);
        menu.setHeaderTitle(task);
	super.onCreateContextMenu(menu, v, menuInfo);
    }

    private String getTaskContent(final Uri uri) {
	final Cursor c = getContentResolver().query(uri, new String[]{TaskColumns.TASK}, null, null, null);
	if (c == null || !c.moveToFirst() || c.getCount() != 1) {
	    Log.e(TAG, "shit, something is wrong, duplicated uri");
	    if (c == null) {
		return "";
	    }
	    c.close();
	    return "";
	}
	return c.getString(0);
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
            final Uri uri = ContentUris.withAppendedId(TaskStore.CONTENT_URI, id);
            final ImageView move = (ImageView) view.findViewById(R.id.action_move_to_today);
            move.setOnClickListener(new OnClickListener() {
	        @Override
	        public void onClick(View v) {
	    	    final ContentValues values = new ContentValues(2);
	    	    values.put(TaskColumns.TYPE, TaskStore.TYPE_TODAY);
	    	    final Calendar today = new GregorianCalendar();
	    	    values.put(TaskColumns.CREATED, today.getTimeInMillis());
	    	    values.put(TaskColumns.DAY, today.get(Calendar.DAY_OF_YEAR));
	    	    getContentResolver().update(uri, values, null, null);
	    	    Toast.makeText(getApplication(), getString(R.string.move_to_today_tip).replace("#", taskContent), Toast.LENGTH_SHORT).show();
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