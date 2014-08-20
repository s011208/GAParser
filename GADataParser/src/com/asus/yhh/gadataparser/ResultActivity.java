
package com.asus.yhh.gadataparser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.CategorySeries;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ViewSwitcher;

public class ResultActivity extends Activity {
    private static final float PIE_CHART_IGNORE_THRESHOLD = 0.03f;
    
    private String[] mParsingPathes;

    private ListView mPackageList;

    private PackageListAdapter mPackageListAdapter;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    // report
    private ViewSwitcher mReportSwitcher;

    private ListView mReportList;

    private ReportListAdapter mReportListAdapter;

    private TextView mList, mPie;

    private LinearLayout mChartContainer;

    private static int[] COLORS = new int[] {
            Color.GREEN, Color.BLUE, Color.MAGENTA, Color.CYAN, Color.LTGRAY, Color.RED,
            Color.YELLOW
    };

    private CategorySeries mSeries = new CategorySeries("");

    private DefaultRenderer mRenderer = new DefaultRenderer();

    private GraphicalView mChartView;

    @Override
    protected void onRestoreInstanceState(Bundle savedState) {
        super.onRestoreInstanceState(savedState);
        mSeries = (CategorySeries)savedState.getSerializable("current_series");
        mRenderer = (DefaultRenderer)savedState.getSerializable("current_renderer");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("current_series", mSeries);
        outState.putSerializable("current_renderer", mRenderer);
    }

    private void clearChart() {
        mSeries.clear();
        mRenderer.removeAllRenderers();
        mChartView.repaint();
    }

    private void rePainChart(ParsedData data) {
        clearChart();
        HashMap<String, Integer> related = sortByValues(data.relatedData);
        Iterator<String> pkgs = related.keySet().iterator();
        float totalCount = 0;
        while (pkgs.hasNext()) {
            String pkg = pkgs.next();
            totalCount += related.get(pkg);
        }
        boolean lessThan4Percent = false;
        int ignoreCount = 0;
        if (totalCount != 0) {
            pkgs = related.keySet().iterator();
            while (pkgs.hasNext()) {
                String pkg = pkgs.next();
                int count = related.get(pkg);
                if (count / totalCount >= PIE_CHART_IGNORE_THRESHOLD) {
                    mSeries.add(pkg, count);
                    SimpleSeriesRenderer renderer = new SimpleSeriesRenderer();
                    renderer.setColor(COLORS[(mSeries.getItemCount() - 1) % COLORS.length]);
                    mRenderer.addSeriesRenderer(renderer);
                } else {
                    lessThan4Percent = true;
                    ignoreCount += count;
                }
            }
            if(lessThan4Percent){
                mSeries.add("else", ignoreCount);
                SimpleSeriesRenderer renderer = new SimpleSeriesRenderer();
                renderer.setColor(COLORS[(mSeries.getItemCount() - 1) % COLORS.length]);
                mRenderer.addSeriesRenderer(renderer);
            }
        }
        mChartView.repaint();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result_activity);
        mParsingPathes = getIntent().getStringArrayExtra("path");
        mPackageList = (ListView)findViewById(R.id.file_list);
        mPackageListAdapter = new PackageListAdapter(this);
        mPackageList.setAdapter(mPackageListAdapter);
        mPackageList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ParsedData data = mPackageListAdapter.getRawData(mPackageListAdapter
                        .getItem(position));
                mReportListAdapter.setParsedData(data);
                mReportListAdapter.notifyDataSetChanged();
                rePainChart(data);
            }
        });
        mSwipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(new OnRefreshListener() {

            @Override
            public void onRefresh() {
                mReportListAdapter.setParsedData(null);
                mReportListAdapter.notifyDataSetChanged();
                clearChart();
                new GaParser(mPackageListAdapter, mSwipeRefreshLayout).execute(mParsingPathes);
            }
        });
        mSwipeRefreshLayout.setColorScheme(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light, android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        mSwipeRefreshLayout.setRefreshing(true);
        // report
        mList = (TextView)findViewById(R.id.report_list);
        mList.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mReportSwitcher.setDisplayedChild(0);
            }
        });
        mPie = (TextView)findViewById(R.id.report_pie);
        mPie.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mReportSwitcher.setDisplayedChild(1);
            }
        });
        mChartContainer = (LinearLayout)findViewById(R.id.report_chart);
        mRenderer.setZoomButtonsVisible(true);
        mRenderer.setStartAngle(90);
        mRenderer.setDisplayValues(true);
        mReportList = (ListView)findViewById(R.id.report_list_view);
        mReportListAdapter = new ReportListAdapter(this);
        mReportList.setAdapter(mReportListAdapter);
        mReportSwitcher = (ViewSwitcher)findViewById(R.id.report_switcher);
        mReportSwitcher.setDisplayedChild(0);
        new GaParser(mPackageListAdapter, mSwipeRefreshLayout).execute(mParsingPathes);
    }

    public void onResume() {
        super.onResume();
        mChartView = ChartFactory.getPieChartView(this, mSeries, mRenderer);
        mRenderer.setClickEnabled(true);
        mChartContainer.addView(mChartView);
    }

    private static HashMap sortByValues(HashMap map) {
        List list = new LinkedList(map.entrySet());
        // Defined Custom Comparator here
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable)((Map.Entry)(o1)).getValue()).compareTo(((Map.Entry)(o2))
                        .getValue());
            }
        });

        // Here I am copying the sorted list in HashMap
        // using LinkedHashMap to preserve the insertion order
        HashMap sortedHashMap = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry)it.next();
            sortedHashMap.put(entry.getKey(), entry.getValue());
        }
        return sortedHashMap;
    }

    private static class ReportListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private Context mContext;
        private final ArrayList<String> mPkgs = new ArrayList<String>();

        private final ArrayList<Integer> mCounts = new ArrayList<Integer>();

        public ReportListAdapter(Context context) {
            mContext = context.getApplicationContext();
            mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setParsedData(ParsedData p) {
            mPkgs.clear();
            mCounts.clear();
            if (p == null) {
                return;
            }
            HashMap<String, Integer> related = sortByValues(p.relatedData);
            Iterator<String> pkgs = related.keySet().iterator();
            while (pkgs.hasNext()) {
                String pkg = pkgs.next();
                mPkgs.add(pkg);
                mCounts.add(related.get(pkg));
            }
            Collections.reverse(mPkgs);
            Collections.reverse(mCounts);
        }

        @Override
        public int getCount() {
            return mPkgs.size();
        }

        @Override
        public String getItem(int position) {
            return mPkgs.get(position);
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
            String item = getItem(position);
            int count = mCounts.get(position);
            PackageMatcher.getInstance(mContext).setTitle(holder.mtxt, count, item);
            return convertView;
        }

        private static class ViewHolder {
            TextView mtxt;
        }
    }

    private static class ParsedData {
        private String packageName;

        private String title;

        int meetCount;

        HashMap<String, Integer> relatedData = new HashMap<String, Integer>();

        public ParsedData(String pkg, String title) {
            packageName = pkg;
            this.title = title;
        }

        public void addCount() {
            ++meetCount;
        }

        public void addData(String pkg) {
            Integer times = relatedData.get(pkg);
            if (times == null) {
                relatedData.put(pkg, 1);
            } else {
                relatedData.put(pkg, times + 1);
            }
        }
    }

    private static class PackageListAdapter extends BaseAdapter {

        private final ArrayList<String> mData = new ArrayList<String>();

        private final HashMap<String, ParsedData> mRelatedData = new HashMap<String, ParsedData>();

        private LayoutInflater mInflater;

        public PackageListAdapter(Context context) {
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setData(HashMap<String, ParsedData> relatedData) {
            mRelatedData.clear();
            mRelatedData.putAll(relatedData);
            mData.clear();
            mData.addAll(sortPkgs(mRelatedData));
        }

        private static ArrayList<String> sortPkgs(final HashMap<String, ParsedData> relatedData) {
            ArrayList<ParsedData> rData = new ArrayList<ParsedData>(relatedData.values());
            Comparator<ParsedData> comparator = new Comparator<ParsedData>() {

                @Override
                public int compare(ParsedData lhs, ParsedData rhs) {
                    return Integer.compare(lhs.meetCount, rhs.meetCount);
                }
            };
            Collections.sort(rData, comparator);
            ArrayList<String> rtn = new ArrayList<String>();
            for (ParsedData data : rData) {
                rtn.add(data.packageName);
            }
            Collections.reverse(rtn);
            return rtn;
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        public ParsedData getRawData(String key) {
            return mRelatedData.get(key);
        }

        @Override
        public String getItem(int position) {
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
            String item = getItem(position);
            ParsedData pData = mRelatedData.get(item);
            holder.mtxt.setText(item + ", " + pData.meetCount);
            return convertView;
        }

        private static class ViewHolder {
            TextView mtxt;
        }
    }

    public static class GaParser extends AsyncTask<String, Void, Void> {
        private PackageListAdapter mAdapter;

        private SwipeRefreshLayout mSwipeRefreshLayout;

        private final HashMap<String, ParsedData> mData = new HashMap<String, ParsedData>();

        public GaParser(PackageListAdapter adapter, SwipeRefreshLayout swipeRefreshLayout) {
            mAdapter = adapter;
            mSwipeRefreshLayout = swipeRefreshLayout;
            mAdapter.setData(new HashMap<String, ParsedData>());
            mAdapter.notifyDataSetChanged();
        }

        @Override
        protected Void doInBackground(String... params) {
            for (String path : params) {
                processData(gaRawHtmParser(path), mData);
            }
            return null;
        }

        private static void processData(String rawData, HashMap<String, ParsedData> rtn) {
            String[] dataList = rawData.split("\n");
            for (String data : dataList) {
                try {
                    // record all pkgs in this data
                    ArrayList<String> r = new ArrayList<String>();
                    JSONArray array = new JSONArray(data);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject ob = array.getJSONObject(i);
                        String intent = ob.getString("intent");
                        String widget = ob.getString("appWidgetProvider");
                        String pkg = null;
                        if (widget.isEmpty() == false) {
                            pkg = widget.substring(0, widget.lastIndexOf("/"));
                        } else if (intent.isEmpty() == false) {
                            pkg = intent.substring(0, intent.lastIndexOf("/"));
                        } else {
                            continue;
                        }
                        if (pkg != null && r.contains(pkg) == false)
                            r.add(pkg);
                    }
                    for (int i = 0; i < r.size(); i++) {
                        String mainPkg = r.get(i);
                        ParsedData pData = rtn.get(mainPkg);
                        if (pData == null) {
                            pData = new ParsedData(mainPkg, null);
                        }
                        for (int j = 0; j < r.size(); j++) {
                            if (j == i)
                                continue;
                            String relatedPkg = r.get(j);
                            pData.addData(relatedPkg);
                            ParsedData relatedData = rtn.get(relatedPkg);
                            if (relatedData == null) {
                                relatedData = new ParsedData(relatedPkg, null);
                            }
                            relatedData.addCount();
                        }
                        rtn.put(mainPkg, pData);
                    }
                } catch (JSONException e) {
                }
            }
        }

        private static String gaRawHtmParser(String filePath) {
            StringBuilder sb = new StringBuilder();
            try {
                Document doc = Jsoup.parse(new File(filePath), "UTF-8");
                Elements eles = doc.select("td[class=google-visualization-table-td]");
                for (Element ele : eles) {
                    sb.append(ele.text() + System.lineSeparator());
                }
            } catch (IOException e) {
            }
            return sb.toString();
        }

        @Override
        protected void onPostExecute(Void params) {
            mAdapter.setData(mData);
            mAdapter.notifyDataSetChanged();
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }
}
