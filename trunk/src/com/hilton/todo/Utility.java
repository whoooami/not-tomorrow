package com.hilton.todo;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

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

    public static String getTaskContent(final ContentResolver cr, final Uri uri) {
	final Cursor c = cr.query(uri, new String[]{TaskColumns.TASK}, null, null, null);
	if (c == null || !c.moveToFirst() || c.getCount() != 1) {
	    if (c == null) {
		return "";
	    }
	    c.close();
	    return "";
	}
	return c.getString(0);
    }
    

    public static void showEditDialog(final Uri uri, final Activity a) {
	final View textEntryView = LayoutInflater.from(a).inflate(R.layout.dialog_edit_task, null);
	final String content = Utility.getTaskContent(a.getContentResolver(), uri);
	final AlertDialog dialogEditTask = new AlertDialog.Builder(a)
	.setIcon(android.R.drawable.ic_dialog_alert)
	.setTitle(R.string.dialog_edit_title)
	.setView(textEntryView)
	.create();
	
	dialogEditTask.setButton(AlertDialog.BUTTON_POSITIVE, a.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
	    public void onClick(DialogInterface dialog, int whichButton) {
		final EditText box = (EditText) dialogEditTask.findViewById(R.id.edit_box);
		final String newContent = box.getText().toString();
		if (content.equals(newContent)) {
		    return;
		}
		final ContentValues cv = new ContentValues();
		cv.put(TaskColumns.TASK, box.getText().toString());
		cv.put(TaskColumns.MODIFIED, new GregorianCalendar().getTimeInMillis());
		a.getContentResolver().update(uri, cv, null, null);
	    }
	});
	dialogEditTask.setButton(AlertDialog.BUTTON_NEGATIVE, a.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
	    }
	});
	
	dialogEditTask.show();
	
	final EditText box = (EditText) dialogEditTask.findViewById(R.id.edit_box);
	box.setText(content);
	box.addTextChangedListener(new TextWatcher() {
	    @Override
	    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	    }
	    
	    @Override
	    public void onTextChanged(CharSequence s, int start, int before, int count) {
		dialogEditTask.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(s.length() > 0);
	    }
	    
	    @Override
	    public void afterTextChanged(Editable s) {
	    }
	});
	
    }
}
