package com.hilton.todo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class PomodoroClockActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.pomodoro_clock);
	
	final TextView taskDescription = (TextView) findViewById(R.id.status_panel);
	taskDescription.setText(getIntent().getStringExtra(TaskDetailsActivity.EXTRA_TASK_CONTENT));
    }
}