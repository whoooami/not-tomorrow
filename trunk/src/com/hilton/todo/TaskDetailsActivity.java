package com.hilton.todo;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;

import com.hilton.todo.TaskStore.PomodoroIndex;
import com.hilton.todo.TaskStore.TaskColumns;

public class TaskDetailsActivity extends Activity {

    public static final String ACTION_VIEW_DETAILS = "com.hilton.todo.VIEW_TASK_DETAILS";
    private static final String TAG = "TaskDetailsActivity";
    private RatingBar mExpected_1;
    private RatingBar mExpected_2;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.task_details);
	final Uri uri = getIntent().getData();
	Log.e(TAG, "uri " + uri);
	
	instantiateExpected(uri);
	
	final Cursor cursor = TaskStore.getTaskDetails(uri, getContentResolver());
	int expected = cursor.getInt(PomodoroIndex.EXPECTED);
	if (expected >= 6) {
	    mExpected_1.setRating(6);
	    mExpected_2.setRating(expected - 6);
	} else {
	    mExpected_1.setRating(expected);
	}
	cursor.close();
    }

    private void instantiateExpected(final Uri uri) {
	mExpected_1 = (RatingBar) findViewById(R.id.expected_1);
	mExpected_1.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
	    @Override
	    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
		if (!fromUser) {
		    return;
		}
		final int expected = (int) rating + (int) mExpected_2.getRating();
		final ContentValues values = new ContentValues(1);
		values.put(TaskColumns.EXPECTED, expected);
		getContentResolver().update(uri, values, null, null);
	    }
	});
	mExpected_2 = (RatingBar) findViewById(R.id.expected_2);
	mExpected_2.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
	    @Override
	    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
		if (!fromUser) {
		    return;
		}
		final int expected = (int) rating + (int) mExpected_1.getRating();
		final ContentValues values = new ContentValues(1);
		values.put(TaskColumns.EXPECTED, expected);
		getContentResolver().update(uri, values, null, null);
	    }
	});
    }
}