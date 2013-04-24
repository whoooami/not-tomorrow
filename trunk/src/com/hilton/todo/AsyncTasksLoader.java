package com.hilton.todo;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.Task;

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
	    final List<Task> tasks = mTaskService.tasks().list("@default").setFields("items/title").execute().getItems();
	    if (tasks != null) {
		for (Task t : tasks) {
		    Log.e(TAG, "\t\tgot task: '" + t.getTitle());
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
