package com.hilton.todo;

import android.app.Activity;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.hilton.todo.TaskStore.TaskColumns;

public class PomodoroClockActivity extends Activity {
    private static final int MSG_TERMINATE = 10;
    private static final int MSG_REFRESH_CLOCK = 0;
    private int mInterruptsCount;
    private int mSpentPomodoros;
    private int mRemainingTimeInSeconds;
    private Handler mHandler = new Handler() {
	@Override
	public void handleMessage(Message msg) {
	    switch (msg.what) {
	    case MSG_TERMINATE:
		finish();
		break;
	    case MSG_REFRESH_CLOCK:
		mRemainingTimeInSeconds--;
		updateClockStatus();
		break;
	    default:
		break;
	    }
	}
    };

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
	
	mSpentPomodoros = getIntent().getIntExtra(TaskDetailsActivity.EXTRA_SPENT_POMODOROS, 1);
	final Button cancel = (Button) findViewById(R.id.cancel);
	cancel.setOnClickListener(new View.OnClickListener() {
	    @Override
	    public void onClick(View v) {
		mSpentPomodoros--;
		final ContentValues values = new ContentValues(1);
		values.put(TaskColumns.SPENT, mSpentPomodoros);
		getContentResolver().update(uri, values, null, null);
		finish();
	    }
	});
	
	mRemainingTimeInSeconds = 180; // 30 mins
	updateClockStatus();
    }

    private void updateClockStatus() {
	final TextView clockStatus = (TextView) findViewById(R.id.clock_status);
	String status = "";
	if (mRemainingTimeInSeconds <= 0) {
	    status = "Time is up!";
	    clockStatus.setText(status);
	    mHandler.removeMessages(MSG_REFRESH_CLOCK);
	    mHandler.removeMessages(MSG_TERMINATE);
	    mHandler.sendEmptyMessageDelayed(MSG_TERMINATE, 1000);
	    return;
	}
	int min = mRemainingTimeInSeconds / 60;
	int sec = mRemainingTimeInSeconds % 60;
	if (mRemainingTimeInSeconds >= 300) {
	    status = "Work time:   -";
	    min = (mRemainingTimeInSeconds - 300) / 60;
	    sec = (mRemainingTimeInSeconds - 300) % 60;
	} else {
	    status = "Rest time:  -";
	}
	status += min;
	status += ":";
	if (sec < 10) {
	    status += "0";
	}
	status += sec;
	clockStatus.setText(status);
	mHandler.removeMessages(MSG_REFRESH_CLOCK);
	mHandler.sendEmptyMessageDelayed(MSG_REFRESH_CLOCK, 1000);
    }
}