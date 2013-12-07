package com.hilton.todo;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class DragAndSortTester extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drag_and_sort_tester);
        ListView list = (ListView) findViewById(R.id.task_list);
        list.setAdapter(new TesterAdapter());
/*        list.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mVendors));
*/    }
    
    private String[] mVendors = {
            "Apple",
            "Microsoft",
            "google",
            "Amazon",
            "Baidu",
    };
    private class TesterAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return mVendors.length;
        }

        @Override
        public String getItem(int position) {
            // TODO Auto-generated method stub
            return mVendors[position];
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getApplication()).inflate(R.layout.today_task_item, null);
            }
            TextView text = (TextView) convertView.findViewById(R.id.task);
            text.setText(getItem(position));
            return convertView;
        }
        
    }
}