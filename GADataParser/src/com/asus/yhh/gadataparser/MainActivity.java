
package com.asus.yhh.gadataparser;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private ListView mFileList;

    private FileListAdapter mFileListAdapter;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private TextView mCancel, mParse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFileList = (ListView)findViewById(R.id.file_list);
        mFileListAdapter = new FileListAdapter(this);
        mFileList.setAdapter(mFileListAdapter);
        mFileList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mFileListAdapter.switchState(position);
                mFileListAdapter.notifyDataSetChanged();
            }
        });
        mSwipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(new OnRefreshListener() {

            @Override
            public void onRefresh() {
                new GaParser(mFileListAdapter, mSwipeRefreshLayout).execute();
            }
        });
        mSwipeRefreshLayout.setColorScheme(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light, android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        mSwipeRefreshLayout.setRefreshing(true);
        mCancel = (TextView)findViewById(R.id.cancel);
        mCancel.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mFileListAdapter.resetState();
                mFileListAdapter.notifyDataSetChanged();
            }
        });
        mParse = (TextView)findViewById(R.id.parse);
        mParse.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ArrayList<Integer> selectedPosition = mFileListAdapter.getSeletedPosition();
                ArrayList<String> fPath = new ArrayList<String>();
                for (Integer position : selectedPosition) {
                    File target = mFileListAdapter.getItem(position);
                    fPath.add(target.getAbsolutePath());
                }
                String[] filePathes = new String[fPath.size()];
                for (int i = 0; i < fPath.size(); i++) {
                    filePathes[i] = fPath.get(i);
                }
                Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                intent.putExtra("path", filePathes);
                startActivity(intent);
            }
        });
        new GaParser(mFileListAdapter, mSwipeRefreshLayout).execute();
    }

    private static class FileListAdapter extends BaseAdapter {

        private final ArrayList<File> mData = new ArrayList<File>();

        private final ArrayList<Boolean> mSelectedState = new ArrayList<Boolean>();

        private LayoutInflater mInflater;

        public FileListAdapter(Context context) {
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setData(ArrayList<File> data) {
            mData.clear();
            mData.addAll(data);
            resetState();
        }

        public ArrayList<Integer> getSeletedPosition() {
            ArrayList<Integer> rtn = new ArrayList<Integer>();
            for (int i = 0; i < mSelectedState.size(); i++) {
                if (mSelectedState.get(i)) {
                    rtn.add(i);
                }
            }
            return rtn;
        }

        public void resetState() {
            mSelectedState.clear();
            for (int i = 0; i < mData.size(); i++) {
                mSelectedState.add(false);
            }
        }

        public void switchState(int position) {
            boolean state = !mSelectedState.remove(position);
            mSelectedState.add(position, state);
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public File getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.file_list_view, null);
                holder = new ViewHolder();
                holder.mtxt = (TextView)convertView.findViewById(R.id.file_name);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }
            boolean isSelected = mSelectedState.get(position);
            File item = getItem(position);
            holder.mtxt.setText(item.getName());
            if (isSelected) {
                holder.mtxt.setTextColor(Color.rgb(0, 255, 255));
            } else {
                holder.mtxt.setTextColor(Color.WHITE);
            }
            return convertView;
        }

        private static class ViewHolder {
            TextView mtxt;
        }
    }

    public static class GaParser extends AsyncTask<Void, Void, Void> {
        private FileListAdapter mAdapter;

        private SwipeRefreshLayout mSwipeRefreshLayout;

        private final ArrayList<File> mData = new ArrayList<File>();

        public GaParser(FileListAdapter adapter, SwipeRefreshLayout swipeRefreshLayout) {
            mAdapter = adapter;
            mSwipeRefreshLayout = swipeRefreshLayout;
            mAdapter.setData(new ArrayList<File>());
            mAdapter.notifyDataSetChanged();
        }

        @Override
        protected Void doInBackground(Void... params) {
            parseChildren(Environment.getExternalStorageDirectory());
            return null;
        }

        private void parseChildren(File root) {
            if (root == null)
                return;
            File[] files = root.listFiles();
            if (files == null)
                return;
            for (File file : files) {
                if (file.isDirectory()) {
                    parseChildren(file);
                } else {
                    String filePath = file.getPath();
                    if (filePath.endsWith(".htm") || filePath.endsWith(".html"))
                        mData.add(file);
                }
            }
        }

        @Override
        protected void onPostExecute(Void params) {
            mAdapter.setData(mData);
            mAdapter.notifyDataSetChanged();
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }
}
