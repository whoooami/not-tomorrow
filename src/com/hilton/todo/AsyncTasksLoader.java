package com.hilton.todo;

import java.io.IOException;
import java.util.ArrayList;
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
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.Tasks.TasksOperations;
import com.google.api.services.tasks.model.Task;
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
	    final List<TaskWrapper> localTasks = getLocalTasks();
	    // step 2: get data from server
	    TasksOperations.List operatorList = mTaskService.tasks().list("@default");
	    List<Task> serverTasks = operatorList.execute().getItems();
	    if (serverTasks == null) {
		serverTasks = new ArrayList<Task>();
	    }
	    // step 3: do synchronization by Id as primary key
	    // phase A: iterate localTasks and merge: add to server, update to server and update database and delete processed tasks
	    for (final TaskWrapper t : localTasks) {
		if (t.idIsNull()) {
		    Log.e(TAG, "orginal local task is " + t);
		    // push to server
		    final Task newTask = mTaskService.tasks().insert("@default", t.getTask()).execute();
		    printTask(newTask);
		    t.updateTask(newTask, mActivity.getContentResolver());
		} else {
		    final Task st = findTaskWithId(serverTasks, t.getId());
		    if (st == null) {
			continue;
		    }
		    if (t.isNewerThan(st)) {
			if (t.isDeleted()) {
			    mTaskService.tasks().delete("@default", st.getId()).execute();
			} else {
			    // TODO: update cannot work, bad request
			    t.mergeInto(st);
			    final Task ut = mTaskService.tasks().update("@default", st.getId(), st).execute();
			    // TOCHEKCK: ut should be the same to t.getTask and different from st
			    Log.e(TAG, "local task " + t + "\n server task " + st + "\n executed returns " + ut);
			}
		    } else {
			t.updateTask(st, mActivity.getContentResolver());
		    }
		}
	    }
	    // phase B: iterate serverTasks and merge into database
	    for (Task t : serverTasks) {
		if (localContains(localTasks, t)) {
		    continue;
		}
		if (TextUtils.isEmpty(t.getTitle())) {
		    continue;
		}
		addToLocal(t);
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

    private void addToLocal(Task t) {
	final ContentValues cv = TaskWrapper.extractValues(t);
	mActivity.getContentResolver().insert(TaskStore.CONTENT_URI, cv);
    }

    private boolean localContains(List<TaskWrapper> localTasks, Task t) {
	for (TaskWrapper lt : localTasks) {
	    if (t.getId().equals(lt.getId())) {
		return true;
	    }
	}
	return false;
    }

    private Task findTaskWithId(List<Task> serverTasks, String id) {
	for (Task t : serverTasks) {
	    if (id.equals(t.getId())) {
		return t;
	    }
	}
	return null;
    }

    private void printTask(Task t) {
	final StringBuilder sb = new StringBuilder();
	sb.append("Task {\n");
	sb.append("title : " + t.getTitle());
	sb.append("\n id: " + t.getId());
	sb.append("\n updated: " + t.getUpdated());
	sb.append("\n completed: " + t.getCompleted());
	sb.append("\n deleted: " + t.getDeleted());
	sb.append("\n due: " + t.getDue());
	sb.append("\n status: " + t.getStatus());
	sb.append("}\n");
	Log.e(TAG, "\t\tgot task: '" + sb.toString());
	Log.e(TAG, "original task " + t);
    }

    private List<TaskWrapper> getLocalTasks() {
	final Cursor c = mActivity.getContentResolver().query(TaskStore.CONTENT_URI, 
	    TaskStore.PROJECTION, TaskColumns.TYPE + "=?", 
	    new String[]{String.valueOf(TaskStore.TYPE_TODAY)}, null);
	List<TaskWrapper> tasks = new ArrayList<TaskWrapper>();
	if (c == null) {
	    return tasks;
	}
	if (!c.moveToFirst()) {
	    c.close();
	    return tasks;
	}
	do {
	    TaskWrapper t = new TaskWrapper(c);
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
