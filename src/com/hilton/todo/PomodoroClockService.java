package com.hilton.todo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.hilton.todo.TaskStore.TaskColumns;

public class PomodoroClockService extends Service {
    static final int POMODORO_CLOCK_REST_DURATION = 300;
    static final int POMODORO_CLOCK_DURATION = 1800;
    private static final int MSG_COUNTING_DOWN = 10;
    private static final int MSG_STOP_SELF = 11;
    protected static final String TAG = "PomodoroClockService";
    private int mSpentPomodoros;
    private int mRemainingTimeInSeconds;
    private IBinder mBinder;
    private static final int NOTIFICATION_ID = 10001;
    private String mTaskDescription;
    private int mTaskInterrupts;
    private boolean mHasClient;
    
    private final Handler mServiceHandler = new Handler() {
	@Override
	public void handleMessage(Message msg) {
	    switch (msg.what) {
	    case MSG_COUNTING_DOWN: {
		Log.e(TAG, "couting down remaining: " + mRemainingTimeInSeconds);
		if (mRemainingTimeInSeconds <= 0) {
		    incrementSpent();
		    quitService();
		    return;
		}
		mRemainingTimeInSeconds--;
		if (mRemainingTimeInSeconds == POMODORO_CLOCK_REST_DURATION) {
		    updateNotification();
		}
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
    private Uri mTaskUri;
    
    @Override
    public void onCreate() {
	super.onCreate();
	Log.e(TAG, "on create, here i come");
	mSpentPomodoros = 0;
	mRemainingTimeInSeconds = 0;
	mBinder = new ServiceStub(this);
	mHasClient = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
	Log.e(TAG, "on start command, got command " + intent);
	mSpentPomodoros = intent.getIntExtra(TaskDetailsActivity.EXTRA_SPENT_POMODOROS, 0);
	mTaskDescription = intent.getStringExtra(TaskDetailsActivity.EXTRA_TASK_CONTENT);
	mTaskInterrupts = intent.getIntExtra(TaskDetailsActivity.EXTRA_INTERRUPTS_COUNT, 0);
	mTaskUri = intent.getData();
	if (mRemainingTimeInSeconds <= 0) {
	    startClock();
	} else {
	    Log.e(TAG, "an existing clock is on the go, do nothing");
	}
	return super.onStartCommand(intent, flags, startId);
    }

    private void startClock() {
	mRemainingTimeInSeconds = POMODORO_CLOCK_DURATION;
	mServiceHandler.removeMessages(MSG_COUNTING_DOWN);
	mServiceHandler.sendEmptyMessageDelayed(MSG_COUNTING_DOWN, 1000);
	updateNotification();
    }

    private void incrementSpent() {
	mSpentPomodoros++;
	Log.e(TAG, "start clock: start a pomodoro clock. spent " + mSpentPomodoros);
	final ContentValues values = new ContentValues(1);
	values.put(TaskColumns.SPENT, mSpentPomodoros);
	getContentResolver().update(mTaskUri, values, null, null);
    }
    private void updateNotification() {
	final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	final Notification notification = new Notification();
	final String title = getString(R.string.pomodoro_clock);
	String message = getString(R.string.noti_work_time);
	notification.icon = R.drawable.ic_launcher;
	notification.defaults = 0;
	notification.when = System.currentTimeMillis();
	if (mRemainingTimeInSeconds <= 0) {
	    notification.tickerText = getString(R.string.pomodoro_finished);
	    notification.flags = 0;
	} else {
	    if (mRemainingTimeInSeconds >= POMODORO_CLOCK_REST_DURATION) {
		message = getString(R.string.noti_work_time);
	    } else {
		message = getString(R.string.noti_rest_time);
	    }
	    notification.flags = Notification.FLAG_ONGOING_EVENT;
	}
	Intent intent = createIntent();
	PendingIntent pi = PendingIntent.getActivity(getApplication(), 0, intent, 0);
	if (mRemainingTimeInSeconds == POMODORO_CLOCK_DURATION) {
	    notification.tickerText = getString(R.string.pomodoro_start);
	} else if (mRemainingTimeInSeconds == POMODORO_CLOCK_REST_DURATION) {
	    // TODO: When clock activity is in foreground, use another way to notify
	    if (!mHasClient) {
		notification.vibrate = new long[] {100, 100, 100, 100};
	    }
	    notification.tickerText = getString(R.string.noti_rest_time);
	}
	notification.setLatestEventInfo(getApplication(), title, message, pi);
	
	manager.notify(NOTIFICATION_ID, notification);
    }

    private Intent createIntent() {
	Intent intent = new Intent(getApplication(), PomodoroClockActivity.class);
	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	intent.setData(mTaskUri);
	intent.putExtra(TaskDetailsActivity.EXTRA_TASK_CONTENT, mTaskDescription);
	intent.putExtra(TaskDetailsActivity.EXTRA_INTERRUPTS_COUNT, mTaskInterrupts);
	intent.putExtra(TaskDetailsActivity.EXTRA_SPENT_POMODOROS, mSpentPomodoros);
	return intent;
    }

    private void cancelNotification() {
	final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	manager.cancel(NOTIFICATION_ID);
    }
    
    @Override
    public void onDestroy() {
	Log.e(TAG, "on destory, good bye crule worold.");
	mHasClient = false;
	super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
	Log.e(TAG, "on bind; new binder " + intent);
	mHasClient = true;
	return mBinder;
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
	Log.e(TAG, "on unbind : " + intent);
	mHasClient = false;
	return super.onUnbind(intent);
    }

    public int getRemainingTimeInSeconds() {
	return mRemainingTimeInSeconds;
    }
    
    public void cancelClock() {
	cancelNotification();
	final ContentValues values = new ContentValues(1);
	values.put(TaskColumns.SPENT, mSpentPomodoros);
	getContentResolver().update(mTaskUri, values, null, null);
	mRemainingTimeInSeconds = 0;
	quitService();
    }
    
    private void quitService() {
	updateNotification();
	cancelNotification();
	mServiceHandler.removeMessages(MSG_COUNTING_DOWN);
	mServiceHandler.removeMessages(MSG_STOP_SELF);
	mServiceHandler.sendEmptyMessageDelayed(MSG_STOP_SELF, 30 * 1000);
    }

    private static class ServiceStub extends IPomodoroClock.Stub {
	PomodoroClockService mService;
	
	ServiceStub(PomodoroClockService s) {
	    mService = s;
	}

	@Override
	public int getRemainingTimeInSeconds() throws RemoteException {
	    return mService.getRemainingTimeInSeconds();
	}

	@Override
	public void cancelClock() throws RemoteException {
	    mService.cancelClock();
	}
    }
}