package com.hilton.todo;

import android.app.Activity;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.hilton.todo.TaskStore.TaskColumns;

public class PomodoroClockActivity extends Activity {
    private int mInterruptsCount;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.pomodoro_clock);
	
	final TextView taskDescription = (TextView) findViewById(R.id.status_panel);
	taskDescription.setText(getIntent().getStringExtra(TaskDetailsActivity.EXTRA_TASK_CONTENT));
	
	final Uri uri = getIntent().getData();
	
	mInterruptsCount = getIntent().getIntExtra(TaskDetailsActivity.EXTRA_INTERRUPTS_COUNT, 0);
	final Button interrupt = (Button) findViewById(R.id.interrupt);
	interrupt.setOnClickListener(new View.OnClickListener() {
	    @Override
	    public void onClick(View v) {
		mInterruptsCount++;
		final ContentValues values = new ContentValues(1);
		values.put(TaskColumns.INTERRUPTS, mInterruptsCount);
		getContentResolver().update(uri, values, null, null);
	    }
	});
    }
}