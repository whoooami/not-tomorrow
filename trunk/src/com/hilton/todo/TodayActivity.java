package com.hilton.todo;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.hilton.todo.Task.ProjectionIndex;
import com.hilton.todo.Task.TaskColumns;
import com.hilton.todo.TodayTaskListView.DropListener;

public class TodayActivity extends Activity {
    protected static final String TAG = "TodayActivity";
    private static final int START_TOMORROW = 10;
    private static final int VIEW_HISTORY = 11;
    private static final int REORDER = 12;
    private static final int SYNC_GOOGLE_TASK = 13;
    private static final int REQUEST_CODE_GOOGLE_PLAY_SERVICES = 100;
    private TodayTaskListView mTaskList;
    private EditText mAddTaskEditor;
    private LayoutInflater mFactory;
    private GestureDetector mGestureDetector;
    private SwitchGestureListener mSwitchGestureListener;
    private Dialog mDialogEditTask;
    private ConnectivityManager mConnectivityManager;
    private Dialog mNoNetworkNotify;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.today_activity);
        final TextView header = (TextView) findViewById(R.id.header);
        final Calendar date = new GregorianCalendar();
        header.setText(getString(R.string.today).replace("#", new SimpleDateFormat(getString(R.string.date_format)).format(date.getTime())));
        
        mFactory = LayoutInflater.from(getApplication());
        mTaskList = (TodayTaskListView) findViewById(R.id.task_list);
        mTaskList.exitDraggingMode();
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
        mGestureDetector = new GestureDetector(mSwitchGestureListener);
        mTaskList.setOnTouchListener(new OnTouchListener() {
	    @Override
	    public boolean onTouch(View v, MotionEvent event) {
		return mGestureDetector.onTouchEvent(event);
	    }
	});
        registerForContextMenu(mTaskList);
        mTaskList.setDropListener(new DropListener() {
	    @Override
	    public void drop(int from, int to) {
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
		    final Cursor src = (Cursor) mTaskList.getItemAtPosition(from);
		    Log.e(TAG, "----------------src cursor and current row of source cursor");
		    android.database.DatabaseUtils.dumpCurrentRow(src);
		    long srcModified = src.getLong(ProjectionIndex.MODIFIED);
		    final Uri srcUri = ContentUris.withAppendedId(Task.CONTENT_URI, src.getLong(ProjectionIndex.ID));
		    Log.e(TAG, "src uri " + srcUri);

		    Log.e(TAG, "----------------dst cursor and current row of dst currsor");
		    final Cursor dst = (Cursor) mTaskList.getItemAtPosition(to);
		    android.database.DatabaseUtils.dumpCurrentRow(dst);
		    long dstModified = dst.getLong(ProjectionIndex.MODIFIED);
		    final Uri dstUri = ContentUris.withAppendedId(Task.CONTENT_URI, dst.getLong(ProjectionIndex.ID));
		    Log.e(TAG, "\t\t, dst uri " + dstUri);
		    Log.e(TAG, "srcm " + srcModified + ", dstM " + dstModified);
		    
		    final ContentValues values = new ContentValues(1);
		    values.put(TaskColumns.MODIFIED, dstModified);
		    getContentResolver().update(srcUri, values, null, null);
		    values.clear();
		    values.put(TaskColumns.MODIFIED, srcModified);
		    getContentResolver().update(dstUri, values, null, null);
		    Log.e(TAG, "data swapped, are you aware of that");
	    }
        });
        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public void onDestroy() {
	super.onDestroy();
	unregisterForContextMenu(mTaskList);
	mTaskList.setDropListener(null);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	final Uri uri = ContentUris.withAppendedId(Task.CONTENT_URI, info.id);
	switch (item.getItemId()) {
	case R.id.today_list_contextmenu_delete:
	    getContentResolver().delete(uri, null, null);
	    return true;
	case R.id.today_list_contextmenu_edit: {
	    final View textEntryView = mFactory.inflate(R.layout.dialog_edit_task, null);
	    mDialogEditTask = new AlertDialog.Builder(TodayActivity.this)
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
	    box.setText(getTaskContent(uri));
	    return true;
	}
	case R.id.today_list_contextmenu_push: {
    	    final ContentValues values = new ContentValues(2);
    	    values.put(TaskColumns.TYPE, Task.TYPE_TOMORROW);
    	    final Calendar today = new GregorianCalendar();
    	    today.add(Calendar.DAY_OF_YEAR, 1);
    	    values.put(TaskColumns.MODIFIED, today.getTimeInMillis());
    	    values.put(TaskColumns.DAY, today.get(Calendar.DAY_OF_YEAR));
    	    getContentResolver().update(uri, values, null, null);
    	    Toast.makeText(getApplication(), getString(R.string.move_to_tomorrow_tip).replace("#", getTaskContent(uri)), 
    		    Toast.LENGTH_SHORT).show();
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
	if (mTaskList.inDraggingMode()) {
	    return;
	}
	final long id = ((AdapterContextMenuInfo) menuInfo).id;
	if (id <= 0) {
	    return;
	}
	getMenuInflater().inflate(R.menu.today_contextmenu, menu);
	final Uri uri = ContentUris.withAppendedId(Task.CONTENT_URI, id);
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
    public void onResume() {
	super.onResume();
	mSwitchGestureListener.reset();
	if (mTaskList.inDraggingMode()) {
	    mTaskList.exitDraggingMode();
	}
    }
    
    @Override
    public void onPause() {
	super.onPause();
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
	switch (keyCode) {
	case KeyEvent.KEYCODE_BACK:
	case KeyEvent.KEYCODE_MENU:
	    if (mTaskList.inDraggingMode()) {
		mTaskList.exitDraggingMode();
		return true;
	    }
	}
	return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	menu.add(0, START_TOMORROW, 0, R.string.goto_tomorrow);
	menu.add(0, VIEW_HISTORY, 0, R.string.view_history);
	menu.add(0, REORDER, 0, R.string.reorder);
	menu.add(0, SYNC_GOOGLE_TASK, 0, "Sync");
	return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
	menu.findItem(REORDER).setVisible(mTaskList.getChildCount()> 1);
	return super.onPrepareOptionsMenu(menu);
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
	    break;
	case REORDER:
	    mTaskList.enterDragingMode();
	    Log.e(TAG, "reorder things, are you aware of that");
	    break;
	case SYNC_GOOGLE_TASK: {
	    // first check the network
	    NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
	    if (info == null || !info.isConnected()) {
		if (mNoNetworkNotify == null) {
		    mNoNetworkNotify = Utility.createNoNetworkDialog(TodayActivity.this);
		}
		mNoNetworkNotify.show();
	    } else {
		if (checkGooglePlayServicesAvailability()) {
		    // do authorization
		}
	    }
	    break;
	}
	default:
	    break;
	}
	return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	super.onActivityResult(requestCode, resultCode, data);
	switch (requestCode) {
	case REQUEST_CODE_GOOGLE_PLAY_SERVICES:
	    if (resultCode == Activity.RESULT_OK) {
//		do authorization
	    } else {
		checkGooglePlayServicesAvailability();
	    }
	    break;
	}
    }

    private boolean checkGooglePlayServicesAvailability() {
	final int connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
	if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
	    final Dialog dialog = GooglePlayServicesUtil.getErrorDialog(connectionStatusCode,
		    		TodayActivity.this, REQUEST_CODE_GOOGLE_PLAY_SERVICES);
	    dialog.show();
	    return false;
	}
	return true;
    }
    
    private void gotoTomorrow() {
	final Intent i = new Intent();
	i.setClass(getApplication(), TomorrowActivity.class);
	startActivity(i);
	overridePendingTransition(R.anim.activity_enter_in, R.anim.activity_enter_out);
    }
    
    private class TaskAdapter extends CursorAdapter {
        public TaskAdapter(Context context, Cursor c) {
            super(context, c);
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
            final CheckBox toggle = (CheckBox) view.findViewById(R.id.action_toggle_done);
            final short done = cursor.getShort(ProjectionIndex.DONE);
            final int id = cursor.getInt(ProjectionIndex.ID);
            final Uri uri = ContentUris.withAppendedId(Task.CONTENT_URI, id);
            final String taskContent = cursor.getString(ProjectionIndex.TASK);
            /*
             * 1. bindView will be called every time refresh the UI, so onCheckedChangedListener will be changed to the last one set.
             * 2. setChecked would invoke onCheckedChanged.
             * 3. need to remove original listener and set again, to keep listener correct and up-to-date.
             * We must remove old one before setting new one, otherwise, this toggle.setChecked would invoke current listener 
             * which might not be correct, resulting in change other button's state.
             */
            toggle.setOnCheckedChangeListener(null);
            toggle.setChecked(done != 0);
            toggle.setOnCheckedChangeListener(mTaskList.inDraggingMode() ? null : new OnCheckedChangeListener() {
        	@Override
        	public void onCheckedChanged(CompoundButton view, boolean checked) {
        	    final ContentValues values = new ContentValues(1);
        	    values.put(TaskColumns.DONE, checked ? 1 : 0);
        	    getContentResolver().update(uri, values, null, null);
        	}

            });
            toggle.setVisibility(mTaskList.inDraggingMode() ? View.GONE : View.VISIBLE);
            view.setOnTouchListener(mTaskList.inDraggingMode() ? null : new OnTouchListener() {
	        @Override
	        public boolean onTouch(View v, MotionEvent event) {
	            return mGestureDetector.onTouchEvent(event);
	        }
	    });
            TextView task = (TextView) view.findViewById(R.id.task);

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
            
            ImageView dragger = (ImageView) view.findViewById(R.id.dragger);
            dragger.setVisibility(mTaskList.inDraggingMode() ? View.VISIBLE : View.GONE);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = mFactory.inflate(R.layout.today_task_item, null);
            return view;
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