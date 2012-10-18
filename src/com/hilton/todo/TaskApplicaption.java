package com.hilton.todo;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class TaskApplicaption extends Application {
    private static final String TAG = "TaskApplicaption";

    @Override
    public void onCreate() {
	super.onCreate();
	Utility.recycleTasks(this);
        setOffRecyclerAlarm();
    }
    
    private void setOffRecyclerAlarm() {
	final AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        final Intent intent = new Intent(this, TaskRecyclerService.class);
        PendingIntent startService = PendingIntent.getService(this, 0, intent, 0);
        final Calendar today = new GregorianCalendar();
        final Calendar cal = new GregorianCalendar(today.get(Calendar.YEAR), today.get(Calendar.MONTH), 
        	today.get(Calendar.DAY_OF_MONTH), 00, 00, 00);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        alarmManager.setRepeating(AlarmManager.RTC, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, startService);
    }
}