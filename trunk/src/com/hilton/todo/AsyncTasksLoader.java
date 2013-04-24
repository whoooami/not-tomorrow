package com.hilton.todo;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
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
import com.hilton.todo.Task.TaskColumns;

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
	    TasksOperations.List operatorList = mTaskService.tasks().list("@default");
	    final List<Task> tasks = operatorList.execute().getItems();
	    final ContentValues cv = new ContentValues();
	    if (tasks != null) {
		for (Task t : tasks) {
		    Log.e(TAG, "\t\tgot task: '" + t.getTitle() + ", updated " + 
			    t.getUpdated().getValue() + ", complted " + t.getCompleted() + ", deleted " + t.getDeleted());
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
		    cv.put(TaskColumns.MODIFIED, t.getUpdated().getValue());
		    cv.put(TaskColumns.DAY, date.get(Calendar.DAY_OF_YEAR));
		    cv.put(TaskColumns.TYPE, com.hilton.todo.Task.TYPE_TODAY);
		    mActivity.getContentResolver().insert(com.hilton.todo.Task.CONTENT_URI, cv);
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
