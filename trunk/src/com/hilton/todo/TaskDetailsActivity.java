package com.hilton.todo;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class TaskDetailsActivity extends Activity {

    public static final String ACTION_VIEW_DETAILS = "com.hilton.todo.VIEW_TASK_DETAILS";
    private static final String TAG = "TaskDetailsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	final Uri uri = getIntent().getData();
	Log.e(TAG, "uri " + uri);
    }
}