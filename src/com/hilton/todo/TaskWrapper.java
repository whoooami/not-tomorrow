package com.hilton.todo;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.google.api.client.util.DateTime;
import com.google.api.services.tasks.model.Task;
import com.hilton.todo.TaskStore.ProjectionIndex;
import com.hilton.todo.TaskStore.TaskColumns;

public class TaskWrapper {
    private final Uri mUri;
    private final Task mTask;
    
    public TaskWrapper(final Cursor c) {
	mUri = ContentUris.withAppendedId(TaskStore.CONTENT_URI, c.getLong(ProjectionIndex.ID));
	mTask = new Task();
	mTask.setTitle(c.getString(ProjectionIndex.TASK));
	final boolean done = c.getInt(ProjectionIndex.DONE) == 1;
	mTask.setStatus(done ? "completed" : "needsAction");
	DateTime d = new DateTime(c.getLong(ProjectionIndex.MODIFIED));
	mTask.setCompleted(done ? d : null);
	mTask.setUpdated(d);
	mTask.setId(c.getString(ProjectionIndex.GOOGLE_TASK_ID));
	mTask.setDeleted(c.getInt(ProjectionIndex.DELETED) == 1);
	final int type = c.getInt(ProjectionIndex.TYPE);
	GregorianCalendar now = new GregorianCalendar();
	int year = now.get(Calendar.YEAR);
	int month = now.get(Calendar.MONTH);
	int dayOfMonth = now.get(Calendar.DAY_OF_MONTH);
	GregorianCalendar today = new GregorianCalendar(year, month, dayOfMonth, 23, 59);
	DateTime due = null;
	if (type == TaskStore.TYPE_TODAY) {
	    due = new DateTime(today.getTimeInMillis());
	} else if (type == TaskStore.TYPE_TOMORROW) {
	    today.roll(Calendar.DAY_OF_MONTH, true);
	    due = new DateTime(today.getTimeInMillis());
	}
	mTask.setDue(due);
    }
    
    public void updateTask(Task t, ContentResolver cr) {
	final ContentValues cv = extraValues(t);
	cr.update(mUri, cv, null, null);
    }

    public static ContentValues extraValues(Task t) {
	final ContentValues cv = new ContentValues();
	cv.put(TaskColumns.TASK, t.getTitle());
	cv.put(TaskColumns.GOOGLE_TASK_ID, t.getId());
	cv.put(TaskColumns.MODIFIED, t.getUpdated().getValue());
	cv.put(TaskColumns.DONE, isCompleted(t) ? 1 : 0);
	cv.put(TaskColumns.DELETED, t.getDeleted() != null && t.getDeleted() ? 1 : 0);
	int type = TaskStore.TYPE_TODAY;
	if (t.getDue() != null) {
	    GregorianCalendar today = new GregorianCalendar();
	    GregorianCalendar due = new GregorianCalendar();
	    due.setTimeInMillis(t.getDue().getValue());
	    type = today.get(Calendar.DAY_OF_YEAR) < due.get(Calendar.DAY_OF_YEAR) ? TaskStore.TYPE_TOMORROW : TaskStore.TYPE_TODAY;
	}
	cv.put(TaskColumns.TYPE, type);
	return cv;
    }
    
    private static boolean isCompleted(Task t) {
	return t.getStatus().equalsIgnoreCase("completed");
    }

    public void mergeInto(Task t) {
	t.setTitle(mTask.getTitle());
	t.setStatus(mTask.getStatus());
	t.setCompleted(mTask.getCompleted());
	t.setDeleted(mTask.getDeleted());
	t.setDue(mTask.getDue());
	t.setUpdated(mTask.getUpdated());
    }
    
    public boolean isDeleted() {
	return mTask.getDeleted();
    }
    
    public boolean isNewerThan(final Task t) {
	return mTask.getUpdated().getValue() > t.getUpdated().getValue();
    }
    
    public String getId() {
	return mTask.getId();
    }
    
    public Task getTask() {
	return mTask;
    }
    
    public boolean idIsNull() {
	return TextUtils.isEmpty(mTask.getId());
    }
    
    @Override
    public String toString() {
	return "Task {\n" + mUri.toString() + "\n{title " + mTask.getTitle() +
		", \nid: " + mTask.getId() + ", \n " + mTask.getCompleted() +
		", \nupdated " + mTask.getUpdated() + ", \ndeleted " + mTask.getDeleted() + "}}";
    }
}
