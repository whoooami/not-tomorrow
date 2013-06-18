package com.hilton.todo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.hilton.todo.TaskStore.TaskColumns;

public class PomodoroClockService extends Service {
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
		    quitService();
		    return;
		}
		mRemainingTimeInSeconds--;
		if (mRemainingTimeInSeconds == 300) {
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
	    startClock(mTaskUri);
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
	updateNotification();
    }

    private void updateNotification() {
	final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	final RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification);
	final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
	builder.setSmallIcon(R.drawable.ic_launcher);
	if (mRemainingTimeInSeconds <= 0) {
	    builder.setTicker("Pomodoro clock is finished.");
	    builder.setOngoing(false);
	} else {
	    if (mRemainingTimeInSeconds >= 300) {
		views.setTextViewText(R.id.description, "Now is working time!");
	    } else {
		views.setTextViewText(R.id.description, "Take a good rest!");
	    }
	    builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_checked_normal));
	    builder.setOngoing(true);
	}
	Intent intent = createIntent();
	builder.setContentIntent(PendingIntent.getActivity(getApplication(), 0, intent, 0));
	builder.setContent(views);
	if (mRemainingTimeInSeconds == 1800) {
	    builder.setTicker("Starting a Pomodoro clock.");
	} else if (mRemainingTimeInSeconds == 300) {
	    // TODO: When clock activity is in foreground, use another way to notify
	    if (!mHasClient) {
		builder.setVibrate(new long[] {100, 100, 100, 100});
	    }
	    builder.setTicker("Time to take a good rest!");
	}
	Notification notification = builder.build();
	
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
	mSpentPomodoros--;
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