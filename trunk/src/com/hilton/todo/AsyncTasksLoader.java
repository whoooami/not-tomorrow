package com.hilton.todo;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.Toast;

public class AsyncTasksLoader extends AsyncTask<Void, Void, Boolean> {
    private Activity mActivity;
    private Dialog mProgress;
    
    public AsyncTasksLoader(Activity a) {
	mActivity = a;
    }
    
    @Override
    protected Boolean doInBackground(Void... params) {
	try {
	    Thread.sleep(5000);
	} catch (InterruptedException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return false;
	}
	return true;
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
	if (result) {
	    if (mProgress.isShowing()) {
		mProgress.dismiss();
	    }
	    Toast.makeText(mActivity, "Synchronized with Google Tasks successfully.", Toast.LENGTH_SHORT).show();
	} else {
	    Toast.makeText(mActivity, "Failed to synchronize with Google Tasks.", Toast.LENGTH_SHORT).show();
	}
	super.onPostExecute(result);
    }
}
