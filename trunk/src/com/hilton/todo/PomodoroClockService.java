package com.hilton.todo;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.hilton.todo.TaskStore.TaskColumns;

public class PomodoroClockService extends Service {
    private static final int MSG_COUNTING_DOWN = 10;
    private static final int MSG_STOP_SELF = 11;
    protected static final String TAG = "PomodoroClockService";
    private int mSpentPomodoros;
    private int mRemainingTimeInSeconds;
    
    private final Handler mServiceHandler = new Handler() {
	@Override
	public void handleMessage(Message msg) {
	    switch (msg.what) {
	    case MSG_COUNTING_DOWN: {
		Log.e(TAG, "couting down remaining: " + mRemainingTimeInSeconds);
		if (mRemainingTimeInSeconds <= 0) {
		    removeMessages(MSG_COUNTING_DOWN);
		    removeMessages(MSG_STOP_SELF);
		    sendEmptyMessageDelayed(MSG_STOP_SELF, 30 * 1000);
		    return;
		}
		mRemainingTimeInSeconds--;
		removeMessages(MSG_COUNTING_DOWN);
		sendEmptyMessageDelayed(MSG_COUNTING_DOWN, 1000);
		break;
	    }
	    case MSG_STOP_SELF:
		Log.e(TAG, "stop myself");
		stopSelf();
		break;
	    default:
		break;
	    }
	    super.handleMessage(msg);
	}
    };
    
    @Override
    public void onCreate() {
	super.onCreate();
	Log.e(TAG, "on create, here i come");
	mSpentPomodoros = 0;
	mRemainingTimeInSeconds = 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
	Log.e(TAG, "on start command, got command " + intent);
	mSpentPomodoros = intent.getIntExtra(TaskDetailsActivity.EXTRA_SPENT_POMODOROS, 0);
	final Uri uri = intent.getData();
	if (mRemainingTimeInSeconds <= 0) {
	    startClock(uri);
	} else {
	    Log.e(TAG, "an existing clock is on the go, do nothing");
	}
	return super.onStartCommand(intent, flags, startId);
    }

    private void startClock(final Uri uri) {
	mRemainingTimeInSeconds = 1800;
	mSpentPomodoros++;
	Log.e(TAG, "start clock: start a pomodoro clock. spent " + mSpentPomodoros);
	final ContentValues values = new ContentValues(1);
	values.put(TaskColumns.SPENT, mSpentPomodoros);
	getContentResolver().update(uri, values, null, null);
	mServiceHandler.removeMessages(MSG_COUNTING_DOWN);
	mServiceHandler.sendEmptyMessageDelayed(MSG_COUNTING_DOWN, 1000);
    }

    @Override
    public void onDestroy() {
	Log.e(TAG, "on destory, good bye crule worold.");
	super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
	return null;
    }
}