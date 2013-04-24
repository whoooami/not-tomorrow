package com.hilton.todo;

import android.app.Activity;
import android.os.AsyncTask;

public class AsyncTasksLoader extends AsyncTask<Void, Void, Boolean> {
    private Activity mActivity;
    
    public AsyncTasksLoader(Activity a) {
	mActivity = a;
    }
    
    @Override
    protected Boolean doInBackground(Void... params) {
	// TODO Auto-generated method stub
	return null;
    }

}
