package com.hilton.todo;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.hilton.todo.Task.TaskColumns;

public class Utility {
    public static void deleteObseleteTasks(Context context) {
	final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
	final int period = pref.getInt(TaskHistoryActivity.HISTORY_PERIOD, TaskHistoryActivity.HISTORY_PERIOD_DEFAULT);
	final Calendar today = new GregorianCalendar();
	int day = today.get(Calendar.DAY_OF_YEAR);
	int firstDay = day - period;
	context.getContentResolver().delete(Task.CONTENT_URI, 
		TaskColumns.TYPE + " = " + Task.TYPE_HISTORY + " AND " + TaskColumns.DAY + " < " + firstDay, 
		null);
    }

    public static void recycleTasks(Context context) {
	final Calendar today = new GregorianCalendar();
	int day = today.get(Calendar.DAY_OF_YEAR);
	final ContentValues values = new ContentValues(1);
	values.put(TaskColumns.TYPE, Task.TYPE_HISTORY);
	context.getContentResolver().update(Task.CONTENT_URI, values, TaskColumns.DAY + " < " + day, null);
	values.put(TaskColumns.TYPE, Task.TYPE_TODAY);
	context.getContentResolver().update(Task.CONTENT_URI, values, TaskColumns.DAY + " = " + day, null);
	
	deleteObseleteTasks(context);
    }
}
