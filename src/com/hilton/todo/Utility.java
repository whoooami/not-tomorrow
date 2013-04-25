package com.hilton.todo;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;

import com.hilton.todo.TaskStore.TaskColumns;

public class Utility {
    public static void deleteObseleteTasks(Context context) {
	final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
	final int period = pref.getInt(TaskHistoryActivity.HISTORY_PERIOD, TaskHistoryActivity.HISTORY_PERIOD_DEFAULT);
	final Calendar today = new GregorianCalendar();
	int day = today.get(Calendar.DAY_OF_YEAR);
	int firstDay = day - period;
	context.getContentResolver().delete(TaskStore.CONTENT_URI, 
		TaskColumns.TYPE + " = " + TaskStore.TYPE_HISTORY + " AND " + TaskColumns.DAY + " < " + firstDay, 
		null);
	
	context.getContentResolver().delete(TaskStore.CONTENT_URI, 
		TaskColumns.TYPE + "=" + TaskStore.TYPE_HISTORY + " AND " + TaskColumns.DELETED + "=1", null);
    }

    public static void recycleTasks(Context context) {
	final Calendar today = new GregorianCalendar();
	int day = today.get(Calendar.DAY_OF_YEAR);
	final ContentValues values = new ContentValues(1);
	values.put(TaskColumns.TYPE, TaskStore.TYPE_HISTORY);
	context.getContentResolver().update(TaskStore.CONTENT_URI, values, TaskColumns.DAY + " < " + day, null);
	values.put(TaskColumns.TYPE, TaskStore.TYPE_TODAY);
	context.getContentResolver().update(TaskStore.CONTENT_URI, values, TaskColumns.DAY + " = " + day, null);
	
	deleteObseleteTasks(context);
    }
    
    public static Dialog createNoNetworkDialog(final Activity a) {
	AlertDialog dialog = new AlertDialog.Builder(a)
	.setIcon(android.R.drawable.ic_dialog_alert)
	.setTitle("Network unavailable")
	.setMessage("Network is currently not available. Please enable your network connectivity.")
	.setPositiveButton("Enable", new OnClickListener() {
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
		a.startActivity(new Intent(Settings.ACTION_SETTINGS));
	    }
	})
	.setNegativeButton("Cancel", null)
	.create();
	return dialog;
    }
}
