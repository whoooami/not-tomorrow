package com.hilton.todo;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.hilton.todo.TaskStore.TaskColumns;

public class TaskProvider extends ContentProvider {
    public static final String AUTHORITY = "com.hilton.todo.TaskProvider";
    public static final String TABLE_NAME = "tasks";
    public static final String DEFAULT_SORT_ORDER = TaskColumns.CREATED + " DESC";
    
    private static final int URI_MATCH_TASK = 1;
    private static final int URI_MATCH_TASK_ID = 11;
    
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    private static final String DATABASE_NAME = "todo.db";
    private static final int DATABASE_VERSION = 3;
    private static final String TAG = "TaskProvider";
    static {
        URI_MATCHER.addURI(AUTHORITY, TABLE_NAME, URI_MATCH_TASK);
        URI_MATCHER.addURI(AUTHORITY, TABLE_NAME + "/#", URI_MATCH_TASK_ID);
    }

    private TaskDatabaseHelper mDatabaseHelper;
    
    @Override
    public int delete(Uri uri, String selection, String[] args) {
        final int uriMatch = URI_MATCHER.match(uri);
        if (uriMatch == -1) {
            throw new IllegalArgumentException("Delete bad uri " + uri);
        }
        final StringBuilder where = new StringBuilder();
        if (uriMatch == URI_MATCH_TASK_ID) {
            // query for id, we must extract id from uri and make it as a where clause
            where.append(" (" + TaskColumns._ID + "=" + uri.getPathSegments().get(1) + ") ");
        }
        if (!TextUtils.isEmpty(selection)) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append(" (");
            where.append(selection);
            where.append(") ");
        }
        final int count = mDatabaseHelper.getWritableDatabase().delete(TABLE_NAME, where.toString(), args); 
        getContext().getContentResolver().notifyChange(TaskStore.CONTENT_URI, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        final int match = URI_MATCHER.match(uri);
        switch (match) {
        case URI_MATCH_TASK:
            return "vnd.android.cursor.dir/tasks";
        case URI_MATCH_TASK_ID:
            return "vnd.android.cursor.item/tasks";
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final int uriMatch = URI_MATCHER.match(uri);
        if (uriMatch == -1) {
            throw new IllegalArgumentException("Query bad uri " + uri);
        }
        if (values.getAsLong(TaskColumns.CREATED) == null) {
            final Calendar today = new GregorianCalendar();
            values.put(TaskColumns.CREATED, today.getTimeInMillis());
            values.put(TaskColumns.DAY, today.get(Calendar.DAY_OF_YEAR));
        }
        final long id = mDatabaseHelper.getWritableDatabase().insert(TABLE_NAME, TaskColumns.TASK, values);
        getContext().getContentResolver().notifyChange(TaskStore.CONTENT_URI, null);
        return ContentUris.withAppendedId(TaskStore.CONTENT_URI, id);
    }

    @Override
    public boolean onCreate() {
        mDatabaseHelper =  new TaskDatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION);
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sortby) {
        final int uriMatch = URI_MATCHER.match(uri);
        if (uriMatch == -1) {
            throw new IllegalArgumentException("Query bad uri " + uri);
        }
        if (TextUtils.isEmpty(sortby)) {
            sortby = DEFAULT_SORT_ORDER;
        }
        final SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(TABLE_NAME);
        if (uriMatch == URI_MATCH_TASK_ID) {
            // query for id, we must extract id from uri and make it as a where clause
            queryBuilder.appendWhere(TaskColumns._ID + "=" + uri.getPathSegments().get(1));
        }
        final Cursor cursor = queryBuilder.query(mDatabaseHelper.getReadableDatabase(), 
                projection, selection, args, null, null, sortby);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] args) {
        final int uriMatch = URI_MATCHER.match(uri);
        if (uriMatch == -1) {
            throw new IllegalArgumentException("Insert bad uri " + uri);
        }
        final StringBuilder where = new StringBuilder();
        if (uriMatch == URI_MATCH_TASK_ID) {
            // query for id, we must extract id from uri and make it as a where clause
            where.append(" (" + TaskColumns._ID + "=" + uri.getPathSegments().get(1) + ") ");
        }
        if (!TextUtils.isEmpty(selection)) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append(" (");
            where.append(selection);
            where.append(") ");
        }
//        values.put(TaskColumns.MODIFIED, new Date().getTime());
        final int count = mDatabaseHelper.getWritableDatabase().update(TABLE_NAME, values, where.toString(), args);
        getContext().getContentResolver().notifyChange(TaskStore.CONTENT_URI, null);
        return count;
    }
    
    private class TaskDatabaseHelper extends SQLiteOpenHelper {
        public TaskDatabaseHelper(Context context, String name, CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            createTaskTable(db);
        }

        private void createTaskTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + 
                    TaskColumns._ID + " INTEGER PRIMARY KEY, " +
                    TaskColumns.DONE + " SHORT DEFAULT 0, " +
                    TaskColumns.TASK + " TEXT, " +
                    TaskColumns.TYPE + " INTEGER DEFAULT 1, " +
                    TaskColumns.CREATED + " DATE, " +
                    TaskColumns.DAY + " INTEGER);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }
}