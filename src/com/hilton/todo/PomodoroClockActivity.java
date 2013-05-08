package com.hilton.todo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.hilton.todo.TaskStore.TaskColumns;

public class PomodoroClockActivity extends Activity {
    private static final int MSG_TERMINATE = 10;
    private static final int MSG_REFRESH_CLOCK = 0;
    private static final String TAG = "PomodoroClockActivity";
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
		mRemainingTimeInSeconds = getRemainingFromService();
		updateClockStatus();
		break;
	    default:
		break;
	    }
	}
    };
    private IPomodoroClock mTheService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.pomodoro_clock);
	
	bindToService();
	final Uri uri = getIntent().getData();
	
	mSpentPomodoros = getIntent().getIntExtra(TaskDetailsActivity.EXTRA_SPENT_POMODOROS, 1);
	mInterruptsCount = getIntent().getIntExtra(TaskDetailsActivity.EXTRA_INTERRUPTS_COUNT, 0);

	final TextView taskDescription = (TextView) findViewById(R.id.status_panel);
	final String status = pomodoroOrder() + " of task \"" + getIntent().getStringExtra(TaskDetailsActivity.EXTRA_TASK_CONTENT) + "\"";
	taskDescription.setText(status);
	
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
	
	final Button cancel = (Button) findViewById(R.id.cancel);
	cancel.setOnClickListener(new View.OnClickListener() {
	    @Override
	    public void onClick(View v) {
		cancelServiceClock();
		finish();
	    }
	});
	
	mRemainingTimeInSeconds = getRemainingFromService();
	updateClockStatus();
    }

    protected void cancelServiceClock() {
	if (mTheService == null) {
	    return;
	}
	try {
	    mTheService.cancelClock();
	} catch (RemoteException e) {
	    Log.e(TAG, "exceiton ", e);
	}
    }

    private int getRemainingFromService() {
	if (mTheService == null) {
	    return 1800;
	}
	int remaining = 1800;
	try {
	    remaining = mTheService.getRemainingTimeInSeconds();
	} catch (RemoteException e) {
	    Log.e(TAG, "remote excetion caught, ", e);
	}
	return remaining;
    }

    private String pomodoroOrder() {
	return "Pomodoro #" + mSpentPomodoros;
    }

    private void updateClockStatus() {
	Log.e(TAG, "updating clock status, remianing time " + mRemainingTimeInSeconds);
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

    @Override
    protected void onDestroy() {
	unbindFromService();
	mHandler.removeMessages(MSG_REFRESH_CLOCK);
	mHandler.removeMessages(MSG_TERMINATE);
	super.onDestroy();
    }

    @Override
    protected void onResume() {
	final PomodoroClockView clock = (PomodoroClockView) findViewById(R.id.clock_view);
	clock.setSweepAngle((1800.0f - mRemainingTimeInSeconds) / 5.0f);
	super.onResume();
    }
    
    private void bindToService() {
	final Intent i = new Intent();
	i.setClass(getApplication(), PomodoroClockService.class);
	bindService(i, mServiceConnection, Context.BIND_AUTO_CREATE);
    }
    
    private void unbindFromService() {
	if (mServiceConnection != null) {
	    unbindService(mServiceConnection);
	}
    }
    
    private ServiceConnection mServiceConnection = new ServiceConnection() {

	public void onServiceConnected(ComponentName className, IBinder obj) {
	    Log.e(TAG, "service " + className + " connected as " + obj);
	    mTheService = IPomodoroClock.Stub.asInterface(obj);
	    try {
		mRemainingTimeInSeconds = mTheService.getRemainingTimeInSeconds();
	    } catch (RemoteException e) {
		Log.e(TAG, "excepiton caught : ", e);
	    }
	}
	
	public void onServiceDisconnected(ComponentName className) {
	    Log.e(TAG, "service " + className + " is disconnected");
	}
    };
}