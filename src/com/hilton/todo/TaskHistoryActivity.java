package com.hilton.todo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.hilton.todo.Task.ProjectionIndex;
import com.hilton.todo.Task.TaskColumns;

public class TaskHistoryActivity extends ExpandableListActivity {
    public static final String HISTORY_PERIOD_CHOICE = "history_period_choice";
    public static final String HISTORY_PERIOD = "history_period";
    public static final int HISTORY_PERIOD_CHOICE_DEFAULT = 0;
    public static final int HISTORY_PERIOD_DEFAULT = 7;
    private static final String TAG = "TaskHistoryActivity";
    private static final int CLEAR_HISTORY = 10;
    private static final int SET_TIME_PERIOD = 11;
    private TaskHistoryExpandableListAdapter mAdapter;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.white)));
        final Cursor cursor = getContentResolver().query(Task.CONTENT_URI, Task.PROJECTION, TaskColumns.TYPE + " = ?", 
        	new String[]{String.valueOf(Task.TYPE_HISTORY)}, TaskColumns.DAY + " DESC");
        mAdapter = new TaskHistoryExpandableListAdapter(getApplication(), cursor);
        setListAdapter(mAdapter);
        final ExpandableListView list = getExpandableListView();
        final View v = LayoutInflater.from(getApplication()).inflate(R.layout.empty_history, null);
        addContentView(v, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        list.setEmptyView(v);
    }

    @Override
    public void onBackPressed() {
	super.onBackPressed();
	overridePendingTransition(R.anim.activity_leave_in, R.anim.activity_leave_out);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	menu.add(0, SET_TIME_PERIOD, 0, R.string.set_time_period);
	return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
	if (mAdapter.getGroupCount() > 0) {
	    if (menu.findItem(CLEAR_HISTORY) == null) {
		menu.add(0, CLEAR_HISTORY, 0, R.string.clear_history);
	    }
	} else {
	    menu.removeItem(CLEAR_HISTORY);
	}
	return true;
    }
    
    private int mHistoryPeriodChoice;
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	switch (item.getItemId()) {
	case CLEAR_HISTORY:
	    clearHistory();
	    break;
	case SET_TIME_PERIOD: {
	    final String periodName[] = getResources().getStringArray(R.array.time_period_name);
	    final int periodValue[] = getResources().getIntArray(R.array.time_period_value);
	    final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplication());
	    pref.getInt(HISTORY_PERIOD, periodValue[0]);
	    mHistoryPeriodChoice = pref.getInt(HISTORY_PERIOD_CHOICE, 0);
	    new AlertDialog.Builder(TaskHistoryActivity.this)
            .setTitle(R.string.select_time_period)
            .setSingleChoiceItems(periodName, mHistoryPeriodChoice, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    mHistoryPeriodChoice = whichButton;
                }
            })
            .setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    final Editor editor = pref.edit();
                    editor.putInt(HISTORY_PERIOD_CHOICE, mHistoryPeriodChoice);
                    editor.putInt(HISTORY_PERIOD, periodValue[mHistoryPeriodChoice]);
                    editor.commit();
                }
            })
            .setNegativeButton(R.string.cancel, null)
           .create()
           .show();
	    break;
	}
	default:
	    break;
	}
	return super.onOptionsItemSelected(item);
    }
    
    private void clearHistory() {
	getContentResolver().delete(Task.CONTENT_URI, TaskColumns.TYPE + " = " + Task.TYPE_HISTORY, null);
    }
    
    public class TaskHistoryExpandableListAdapter extends BaseExpandableListAdapter {
        private List<ArrayList<TaskItem>> mTasks;
        private Cursor mCursor;
        private LayoutInflater mFactory;
        private ContentObserver mContentObserver;
        private DataSetObserver mDataSetObserver;
        
        public TaskHistoryExpandableListAdapter(Context ctx, Cursor cursor) {
            mFactory = LayoutInflater.from(ctx);
            mCursor = cursor;
            // Attention: if you want to operate UI or Activity, you must provide main thread's Handler to
            // ContentObserver, otherwise you get the exception: "android.view.ViewRoot$CalledFromWrongThreadException: 
            // Only the original thread that created a view hierarchy can touch its views."
            // ExpandableListActivity#onContentChanged() will invalidate View hierarchy and refresh all views, so it
            // must be done in Main thread. As a result you must provide main Thread's handler.
            // You do not have to provide handler if you do not operate UI or Activity in onChange, of course.

            mContentObserver = new ContentObserver(new Handler()) {
		@Override
		public boolean deliverSelfNotifications() {
		    return true;
		}

		@Override
		public void onChange(boolean selfChange) {
		    mCursor.requery();
		    mTasks = createTasks(mCursor);
		    onContentChanged();
		}
	    };
	    mDataSetObserver = new DataSetObserver() {
		@Override
		public void onChanged() {
		    notifyDataSetChanged();
		}

		@Override
		public void onInvalidated() {
		    notifyDataSetInvalidated();
		}
	    };
	    // It is the Provider to know original exact changes, so we must pass the observers to them.
	    // otherwise you got nothing.
            mCursor.registerContentObserver(mContentObserver);
            mCursor.registerDataSetObserver(mDataSetObserver);
            mTasks = createTasks(mCursor);
//            dumpTasks(mTasks);
        }
        
        private void dumpTasks(List<ArrayList<TaskItem>> tasks) {
            if (tasks == null) {
        	Log.e(TAG, "tasks is null");
        	return;
            }
            for (ArrayList<TaskItem> children : tasks) {
        	Log.e(TAG, "Group #\n");
        	for (TaskItem child : children) {
        	    Log.e(TAG, "\t" + child);
        	}
            }
        }
        
        private List<ArrayList<TaskItem>> createTasks(Cursor cursor) {
            if (cursor == null || !cursor.moveToFirst()) {
        	return null;
            }
            List<ArrayList<TaskItem>> tasks = new ArrayList<ArrayList<TaskItem>>();
            ArrayList<TaskItem> children = new ArrayList<TaskItem>();
            tasks.add(children);
            children.add(new TaskItem(cursor));
            int day = cursor.getInt(ProjectionIndex.DAY);
            while (cursor.moveToNext()) {
        	int cday = cursor.getInt(ProjectionIndex.DAY);
        	if (day == cday) {
        	    children.add(new TaskItem(cursor));
        	} else {
        	    children = new ArrayList<TaskItem>();
        	    tasks.add(children);
        	    children.add(new TaskItem(cursor));
        	}
        	day = cday;
            }
	    return tasks;
	}

	public TaskItem getChild(int groupPosition, int childPosition) {
	    if (mTasks == null) {
		return null;
	    }
            return mTasks.get(groupPosition).get(childPosition);
        }

        public long getChildId(int groupPosition, int childPosition) {
	    if (mTasks == null) {
		return -1;
	    }
            return mTasks.get(groupPosition).get(childPosition).mId;
        }

        public int getChildrenCount(int groupPosition) {
	    if (mTasks == null) {
		return -1;
	    }
            return mTasks.get(groupPosition).size();
        }

        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            if (convertView == null) {
        	convertView = mFactory.inflate(R.layout.history_item, null);
            }
            TextView textView = (TextView) convertView;
            final TaskItem child = getChild(groupPosition, childPosition);
            final String taskContent = child.mTaskLabel;
            if (child.isFinished()) {
        	final Spannable style = new SpannableString(taskContent);
        	style.setSpan(new StrikethroughSpan(), 0, taskContent.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        	style.setSpan(new StyleSpan(Typeface.ITALIC) , 0, taskContent.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        	textView.setText(style);
        	textView.setTextAppearance(getApplication(), R.style.done_task_item_text);
            } else {
        	textView.setText(taskContent);
        	textView.setTextAppearance(getApplication(), R.style.task_item_text);
            }
            return textView;
        }

        public TaskItem getGroup(int groupPosition) {
	    if (mTasks == null) {
		return null;
	    }
            return mTasks.get(groupPosition).get(0);
        }

        public int getGroupCount() {
	    if (mTasks == null) {
		return 0;
	    }
            return mTasks.size();
        }

        public long getGroupId(int groupPosition) {
	    if (mTasks == null) {
		return -1;
	    }
            return groupPosition;
        }

        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            if (convertView == null) {
        	convertView = mFactory.inflate(R.layout.history_header, null);
            }
            TextView textView = (TextView) convertView;
            textView.setText(new SimpleDateFormat(getString(R.string.date_format)).format(getGroup(groupPosition).mModified));
            textView.setTextAppearance(getApplication(), R.style.history_header_text);
            return textView;
        }

        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        public boolean hasStableIds() {
            return true;
        }
        
	private class TaskItem {
            int mId;
            String mTaskLabel;
            int mFinished;
            long mModified;
            int mDay;
            
            public TaskItem(Cursor cursor) {
        	mId = cursor.getInt(ProjectionIndex.ID);
        	mTaskLabel = cursor.getString(ProjectionIndex.TASK);
        	mFinished = cursor.getInt(ProjectionIndex.DONE);
        	mModified = cursor.getLong(ProjectionIndex.MODIFIED);
        	mDay = cursor.getInt(ProjectionIndex.DAY);
            }
            
            public boolean isFinished() {
        	return mFinished != 0;
            }
            
            @Override
            public String toString() {
        	return "ID: " + mId + ", Task: " + mTaskLabel + ", Finished: " + mFinished + ", Modified: " + mModified + ", Day: " + mDay;
            }
        }
    }
}