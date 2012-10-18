package com.hilton.todo;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public class TaskRecyclerService extends Service {
    private int IDLE_TIMEOUT = 30 * 1000; // 30 seconds
    private static final int RECYCLE_TASKS = 0;
    private static final int STOP_SELF = 1;
    private static final String TAG = "TaskRecyclerService";
    private WakeLock mWakeLock;
    
    private Handler mServiceHandler = new Handler() {
	@Override
	public void handleMessage(Message msg) {
	    switch (msg.what) {
	    case RECYCLE_TASKS:
		recycleOldTasks();
		break;
	    case STOP_SELF:
		stopSelf();
		break;
	    }
	    super.handleMessage(msg);
	}
	
    };
    
    private void recycleOldTasks() {
	Utility.recycleTasks(getApplication());
	mServiceHandler.sendEmptyMessageDelayed(STOP_SELF, IDLE_TIMEOUT);
    }

    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
	if (mWakeLock == null) {
	    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
	    mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "TaskRecyclerService");
	}
	if (!mWakeLock.isHeld()) {
	    mWakeLock.acquire();
	}
	mServiceHandler.sendEmptyMessage(RECYCLE_TASKS);
	return 0;
    }
    
    @Override
    public void onDestroy() {
	if (mWakeLock != null && mWakeLock.isHeld()) {
	    mWakeLock.release();
	}
    }
    
    @Override
    public IBinder onBind(Intent intent) {
	return null;
    }
}