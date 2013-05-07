package com.hilton.todo;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.Toast;

import com.hilton.todo.TaskStore.PomodoroIndex;
import com.hilton.todo.TaskStore.TaskColumns;

public class TaskDetailsActivity extends Activity {

    public static final String ACTION_VIEW_DETAILS = "com.hilton.todo.VIEW_TASK_DETAILS";
    private static final String TAG = "TaskDetailsActivity";
    private RatingBar mExpected_1;
    private RatingBar mExpected_2;
    private RatingBar mSpent_1;
    private RatingBar mSpent_2;
    private int mSpentPomodoros;
    private Toast mTaskTooBigNoti;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.task_details);
	final Uri uri = getIntent().getData();
	Log.e(TAG, "uri " + uri);
	
	instantiateExpected(uri);
	
	mSpent_1 = (RatingBar) findViewById(R.id.spent_1);
	mSpent_2 = (RatingBar) findViewById(R.id.spent_2);
	
	final Cursor cursor = TaskStore.getTaskDetails(uri, getContentResolver());
	int expected = cursor.getInt(PomodoroIndex.EXPECTED);
	setExpectedRating(expected);
	
	mSpentPomodoros = cursor.getInt(PomodoroIndex.SPENT);
	setSpentRating();
	cursor.close();
	
	final Button startPomodoro = (Button) findViewById(R.id.start_pomodoro);
	startPomodoro.setOnClickListener(new View.OnClickListener() {
	    private Toast mOverflowNoti;

	    @Override
	    public void onClick(View v) {
		if (mSpentPomodoros >= 12) {
		    if (mOverflowNoti == null) {
			mOverflowNoti = Toast.makeText(getApplication(), 
				"You have spent too much time on this task. You should concenrate or divide it into smaller tasks.", 
				Toast.LENGTH_SHORT);
		    }
		    mOverflowNoti.show();
		}
		mSpentPomodoros++;
		setSpentRating();
		final ContentValues values = new ContentValues(1);
		values.put(TaskColumns.SPENT, mSpentPomodoros);
		getContentResolver().update(uri, values, null, null);
	    }
	});
    }

    private void setExpectedRating(int expected) {
	if (expected >= 6) {
	    mExpected_1.setRating(6);
	    mExpected_2.setRating(expected - 6);
	} else {
	    mExpected_1.setRating(expected);
	    mExpected_2.setRating(0);
	}
    }

    private void setSpentRating() {
	if (mSpentPomodoros >= 6) {
	    mSpent_1.setRating(6);
	    mSpent_2.setRating(mSpentPomodoros - 6);
	} else {
	    mSpent_1.setRating(mSpentPomodoros);
	    mSpent_2.setRating(0);
	}
    }

    private void instantiateExpected(final Uri uri) {
	mExpected_1 = (RatingBar) findViewById(R.id.expected_1);
	mExpected_1.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
	    @Override
	    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
		if (!fromUser) {
		    return;
		}
		updateExpected(uri, rating, mExpected_2);
	    }

	});
	mExpected_2 = (RatingBar) findViewById(R.id.expected_2);
	mExpected_2.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
	    @Override
	    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
		if (!fromUser) {
		    return;
		}
		updateExpected(uri, rating, mExpected_1);
	    }
	});
    }
    
    private void updateExpected(final Uri uri, float rating, RatingBar another) {
	final int expected = (int) rating + (int) another.getRating();
	if (expected > 6) {
	    warnTaskTooBig();
	}
	final ContentValues values = new ContentValues(1);
	values.put(TaskColumns.EXPECTED, expected);
	getContentResolver().update(uri, values, null, null);
    }

    private void warnTaskTooBig() {
	if (mTaskTooBigNoti == null) {
	    mTaskTooBigNoti = Toast.makeText(getApplication(), "Task is too complex, divid it.", Toast.LENGTH_SHORT);
	}
	mTaskTooBigNoti.show();
    }
}