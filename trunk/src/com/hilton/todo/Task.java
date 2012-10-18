package com.hilton.todo;

import android.net.Uri;

public class Task {
    public static final Uri CONTENT_URI = Uri.parse("content://" + TaskProvider.AUTHORITY + "/" + TaskProvider.TABLE_NAME);
    public static final String PROJECTION[] = new String[] {
        TaskColumns._ID,
        TaskColumns.DONE,
        TaskColumns.TASK,
        TaskColumns.TYPE,
        TaskColumns.MODIFIED,
        TaskColumns.DAY,
    };
    
    public static final class ProjectionIndex {
        public static final int ID = 0;
        public static final int DONE = 1;
        public static final int TASK = 2;
        public static final int TYPE = 3;
        public static final int MODIFIED = 4;
        public static final int DAY = 5;
    }
    
    public static class TaskColumns {
        public static final String _ID = "_id";
        public static final String DONE = "done";
        public static final String TASK = "task";
        /** History task, today's task or tomorrow's task */
        public static final String TYPE = "type";
        public static final String MODIFIED = "modified";
        /** the day of the year on which task is created */
        // TODO: there is a bug, if we are at end of a year, day-of-year is not reliable any more
        public static final String DAY = "day";
    }
    
    public static final int TYPE_TODAY = 1;
    public static final int TYPE_TOMORROW = 2;
    public static final int TYPE_HISTORY = 3;
}