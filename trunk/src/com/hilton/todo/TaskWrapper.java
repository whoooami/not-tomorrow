package com.hilton.todo;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;

import com.google.api.client.util.DateTime;
import com.google.api.services.tasks.model.Task;
import com.hilton.todo.TaskStore.ProjectionIndex;

public class TaskWrapper {
    private final Uri mUri;
    private final Task mTask;
    
    public TaskWrapper(final Cursor c) {
	mUri = ContentUris.withAppendedId(TaskStore.CONTENT_URI, c.getLong(ProjectionIndex.ID));
	mTask = new Task();
	mTask.setTitle(c.getString(ProjectionIndex.TASK));
	boolean done = c.getInt(ProjectionIndex.DONE) == 1;
	DateTime d = new DateTime(c.getLong(ProjectionIndex.MODIFIED));
	mTask.setCompleted(done ? d : null);
	mTask.setUpdated(d);
	mTask.setId(c.getString(ProjectionIndex.GOOGLE_TASK_ID));
	mTask.setDeleted(c.getInt(ProjectionIndex.DELETED) == 1);
    }

    @Override
    public String toString() {
	return "Task {\n" + mUri.toString() + "\n{title " + mTask.getTitle() +
		", \nid: " + mTask.getId() + ", \n " + mTask.getCompleted() +
		", \nupdated " + mTask.getUpdated() + ", \ndeleted " + mTask.getDeleted() + "}}";
    }
}
