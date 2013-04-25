package com.hilton.todo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.util.DateTime;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.Tasks.TasksOperations;
import com.google.api.services.tasks.model.Task;
import com.hilton.todo.TaskStore.ProjectionIndex;
import com.hilton.todo.TaskStore.TaskColumns;

public class AsyncTasksLoader extends AsyncTask<Void, Void, Boolean> {
    private static final String TAG = "AsyncTasksLoader";
    private Activity mActivity;
    private Dialog mProgress;
    private Tasks mTaskService;
    
    public AsyncTasksLoader(Activity a, Tasks service) {
	mActivity = a;
	mTaskService = service;
    }
    
    @Override
    protected Boolean doInBackground(Void... params) {
	try {
	    // step 1: read data of local
	    final List<Task> localTasks = getLocalTasks();
	    for (Task t : localTasks) {
		Log.e(TAG, "look at local tasks: " + t.getTitle());
	    }
	    TasksOperations.List operatorList = mTaskService.tasks().list("@default");
	    final List<Task> tasks = operatorList.execute().getItems();
	    final ContentValues cv = new ContentValues();
	    if (tasks != null) {
		for (Task t : tasks) {
		    Log.e(TAG, "\t\tgot task: '" + t.getTitle() + ", updated " + t.getUpdated().getValue() + 
			    ", complted " + t.getCompleted() + ", deleted " + t.getDeleted() + " id " + t.getId() +
			    ", due " + t.getDue() + ", status " + t.getStatus());
		    if (t.getDeleted() != null && t.getDeleted()) {
			continue;
		    }
		    if (TextUtils.isEmpty(t.getTitle())) {
			continue;
		    }
		    final long updated = t.getUpdated().getValue();
		    final Calendar date = new GregorianCalendar();
		    date.setTimeInMillis(updated);
		    cv.clear();
		    cv.put(TaskColumns.TASK, t.getTitle());
		    cv.put(TaskColumns.DONE, (t.getCompleted() == null ? 0 : 1));
		    cv.put(TaskColumns.CREATED, t.getUpdated().getValue());
		    cv.put(TaskColumns.DAY, date.get(Calendar.DAY_OF_YEAR));
		    cv.put(TaskColumns.TYPE, TaskStore.TYPE_TODAY);
		    mActivity.getContentResolver().insert(TaskStore.CONTENT_URI, cv);
		}
	    }
	    return true;
	} catch (final GooglePlayServicesAvailabilityIOException availabilityException) {
	    final Dialog dialog = GooglePlayServicesUtil.getErrorDialog(availabilityException.getConnectionStatusCode(),
	    		mActivity, TodayActivity.REQUEST_CODE_GOOGLE_PLAY_SERVICES);
	    dialog.show();
	} catch (UserRecoverableAuthIOException userRecoverableException) {
	    mActivity.startActivityForResult(userRecoverableException.getIntent(), TodayActivity.REQUEST_CODE_AUTHORIZATION);
	} catch (IOException e) {
	    Log.e(TAG, "exception caught, ", e);
	}
	return false;
    }

    private List<Task> getLocalTasks() {
	final Cursor c = mActivity.getContentResolver().query(TaskStore.CONTENT_URI, 
	    TaskStore.PROJECTION, TaskColumns.TYPE + "=?", 
	    new String[]{String.valueOf(TaskStore.TYPE_TODAY)}, null);
	List<Task> tasks = new ArrayList<Task>();
	if (c == null) {
	    return tasks;
	}
	if (!c.moveToFirst()) {
	    c.close();
	    return tasks;
	}
	do {
	    Task t = new Task();
	    t.setTitle(c.getString(ProjectionIndex.TASK));
	    boolean done = c.getInt(ProjectionIndex.DONE) == 1;
	    DateTime d = new DateTime(c.getLong(ProjectionIndex.CREATED));
	    t.setCompleted(done ? d : null);
	    t.setUpdated(d);
	    tasks.add(t);
	} while (c.moveToNext());
	c.close();
	return tasks;
    }

    @Override
    protected void onPreExecute() {
	if (mProgress == null) {
	    mProgress = ProgressDialog.show(mActivity, "Syncrhonization", 
		    "Synchronizing with Google Tasks. This might take a while.", true, true, null);
	} else {
	    mProgress.show();
	}
	super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Boolean result) {
	if (mProgress.isShowing()) {
	    mProgress.dismiss();
	}
	if (result) {
	    Toast.makeText(mActivity, "Synchronized with Google Tasks successfully.", Toast.LENGTH_SHORT).show();
	} else {
	    Toast.makeText(mActivity, "Failed to synchronize with Google Tasks.", Toast.LENGTH_SHORT).show();
	}
	super.onPostExecute(result);
    }
}
