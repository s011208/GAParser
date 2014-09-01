
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
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
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
        HashMap<ComponentName, Integer> related = sortByValues(data.relatedData);
        Iterator<ComponentName> pkgs = related.keySet().iterator();
        float totalCount = 0;
        PackageMatcher pkgM = PackageMatcher.getInstance(getApplicationContext());
        while (pkgs.hasNext()) {
            ComponentName pkg = pkgs.next();
            totalCount += related.get(pkg);
        }
        boolean lessThan4Percent = false;
        int ignoreCount = 0;
        if (totalCount != 0) {
            pkgs = related.keySet().iterator();
            while (pkgs.hasNext()) {
                ComponentName com = pkgs.next();
                String pkg = com.getPackageName();
                String title = pkgM.getTitle(pkg);
                int count = related.get(com);
                pkg = title == null ? pkg : title;
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
            if (lessThan4Percent) {
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
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
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
                mReportListAdapter.setParsedData(data, mPackageListAdapter.getMapData());
                mReportListAdapter.notifyDataSetChanged();
                mPackageListAdapter.setSelectedPosition(position);
                mPackageListAdapter.notifyDataSetChanged();
                mReportList.smoothScrollToPosition(0);
                rePainChart(data);
            }
        });
        mSwipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(new OnRefreshListener() {

            @Override
            public void onRefresh() {
                mReportListAdapter.setParsedData(null, null);
                mReportListAdapter.notifyDataSetChanged();
                mPackageListAdapter.setSelectedPosition(-1);
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

    private static HashMap<ComponentName, Integer> sortByValues(HashMap<ComponentName, Integer> map) {
        List<ComponentName> list = new LinkedList(map.entrySet());
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

        private final ArrayList<ComponentName> mPkgs = new ArrayList<ComponentName>();

        private final HashMap<ComponentName, ParsedData> mRelatedData = new HashMap<ComponentName, ParsedData>();

        private final ArrayList<Integer> mCounts = new ArrayList<Integer>();

        public ReportListAdapter(Context context) {
            mContext = context.getApplicationContext();
            mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setParsedData(ParsedData p, HashMap<ComponentName, ParsedData> relatedData) {
            mRelatedData.clear();
            if (relatedData != null) {
                mRelatedData.putAll(relatedData);
            }
            mPkgs.clear();
            mCounts.clear();
            if (p == null) {
                return;
            }
            HashMap<ComponentName, Integer> related = sortByValues(p.relatedData);
            Iterator<ComponentName> coms = related.keySet().iterator();
            while (coms.hasNext()) {
                ComponentName com = coms.next();
                mPkgs.add(com);
                mCounts.add(related.get(com));
            }
            Collections.reverse(mPkgs);
            Collections.reverse(mCounts);
        }

        @Override
        public int getCount() {
            return mPkgs.size();
        }

        @Override
        public ComponentName getItem(int position) {
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
                convertView = mInflater.inflate(R.layout.pkg_list_view, null);
                holder = new ViewHolder();
                holder.mtxt = (TextView)convertView.findViewById(R.id.package_title);
                holder.mDetailed = (TextView)convertView.findViewById(R.id.detailed);
                holder.mImg = (ImageView)convertView.findViewById(R.id.type_img);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }
            ComponentName item = getItem(position);
            int count = mCounts.get(position);
            PackageMatcher.getInstance(mContext)
                    .setTitle(holder.mtxt, count, item.getPackageName());
            ParsedData pData = mRelatedData.get(item);
            if (pData != null) {
                if (pData.mType == ParsedData.TYPE_SHORTCUT) {
                    holder.mImg.setImageResource(R.drawable.shortcut);
                } else {
                    holder.mImg.setImageResource(R.drawable.widget);
                }
            }
            holder.mDetailed.setText(item.getPackageName() + "\n" + item.getClassName());
            return convertView;
        }

        private static class ViewHolder {
            TextView mtxt;

            TextView mDetailed;

            ImageView mImg;
        }
    }

    private static class PackageListAdapter extends BaseAdapter {

        private final ArrayList<ComponentName> mData = new ArrayList<ComponentName>();

        private final HashMap<ComponentName, ParsedData> mRelatedData = new HashMap<ComponentName, ParsedData>();

        private LayoutInflater mInflater;

        private int mSelectedPosition = -1;

        private Context mContext;

        public PackageListAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setSelectedPosition(int position) {
            mSelectedPosition = position;
        }

        public void setData(HashMap<ComponentName, ParsedData> relatedData) {
            mRelatedData.clear();
            mRelatedData.putAll(relatedData);
            mData.clear();
            mData.addAll(sortPkgs(mRelatedData));
        }

        public HashMap<ComponentName, ParsedData> getMapData() {
            return mRelatedData;
        }

        private static ArrayList<ComponentName> sortPkgs(
                final HashMap<ComponentName, ParsedData> relatedData) {
            ArrayList<ParsedData> rData = new ArrayList<ParsedData>(relatedData.values());
            Comparator<ParsedData> comparator = new Comparator<ParsedData>() {

                @Override
                public int compare(ParsedData lhs, ParsedData rhs) {
                    return Integer.compare(lhs.meetCount, rhs.meetCount);
                }
            };
            Collections.sort(rData, comparator);
            ArrayList<ComponentName> rtn = new ArrayList<ComponentName>();
            for (ParsedData data : rData) {
                rtn.add(new ComponentName(data.packageName, data.mClassName));
            }
            Collections.reverse(rtn);
            return rtn;
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        public ParsedData getRawData(ComponentName key) {
            return mRelatedData.get(key);
        }

        @Override
        public ComponentName getItem(int position) {
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
                convertView = mInflater.inflate(R.layout.pkg_list_view, null);
                holder = new ViewHolder();
                holder.mTitle = (TextView)convertView.findViewById(R.id.package_title);
                holder.mDetailed = (TextView)convertView.findViewById(R.id.detailed);
                holder.mImg = (ImageView)convertView.findViewById(R.id.type_img);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }
            ComponentName item = getItem(position);
            ParsedData pData = mRelatedData.get(item);
            PackageMatcher.getInstance(mContext).setTitle(holder.mTitle, pData.meetCount,
                    item.getPackageName());
            if (mSelectedPosition == position) {
                if (pData.mType == ParsedData.TYPE_SHORTCUT) {
                    holder.mTitle.setTextColor(Color.argb(150, 52, 139, 254));
                    holder.mDetailed.setTextColor(Color.argb(150, 52, 139, 254));
                } else {
                    holder.mTitle.setTextColor(Color.argb(150, 254, 219, 61));
                    holder.mDetailed.setTextColor(Color.argb(150, 254, 219, 61));
                }
            } else {
                holder.mDetailed.setTextColor(Color.WHITE);
                holder.mTitle.setTextColor(Color.WHITE);
            }
            holder.mDetailed.setText(item.getPackageName() + "\n" + item.getClassName());
            if (pData.mType == ParsedData.TYPE_SHORTCUT) {
                holder.mImg.setImageResource(R.drawable.shortcut);
            } else {
                holder.mImg.setImageResource(R.drawable.widget);
            }
            return convertView;
        }

        private static class ViewHolder {
            TextView mTitle;

            TextView mDetailed;

            ImageView mImg;
        }
    }

    public static class GaParser extends AsyncTask<String, Void, Void> {
        private PackageListAdapter mAdapter;

        private SwipeRefreshLayout mSwipeRefreshLayout;

        private final HashMap<ComponentName, ParsedData> mData = new HashMap<ComponentName, ParsedData>();

        public GaParser(PackageListAdapter adapter, SwipeRefreshLayout swipeRefreshLayout) {
            mAdapter = adapter;
            mSwipeRefreshLayout = swipeRefreshLayout;
            mAdapter.setData(new HashMap<ComponentName, ParsedData>());
            mAdapter.notifyDataSetChanged();
        }

        @Override
        protected Void doInBackground(String... params) {
            for (String path : params) {
                processData(gaRawHtmParser(path), mData);
            }
            return null;
        }

        private static void processData(String rawData, HashMap<ComponentName, ParsedData> wholeData) {
            String[] rawItemList = rawData.split(System.lineSeparator());
            for (String rawItem : rawItemList) {
                try {
                    // record all pkgs in a page
                    ArrayList<ComponentName> itemsInThisPage = new ArrayList<ComponentName>();
                    JSONArray pageItemArray = new JSONArray(rawItem);
                    boolean isShortCut = true;
                    // first step: parse all items in a page
                    for (int i = 0; i < pageItemArray.length(); i++) {
                        JSONObject item = pageItemArray.getJSONObject(i);
                        String intent = item.getString("intent");
                        String widget = item.getString("appWidgetProvider");
                        String title = item.getString("title");
                        String pkg = null;
                        String clz = null;
                        if (widget.isEmpty() == false) {
                            // widget
                            int indexOfSlash = widget.lastIndexOf("/");
                            if (indexOfSlash == -1) {
                                continue;
                            }
                            pkg = widget.substring(0, indexOfSlash);
                            clz = widget.substring(indexOfSlash + 1);
                            isShortCut = false;
                        } else if (intent.isEmpty() == false) {
                            // shortcut
                            int indexOfSlash = intent.lastIndexOf("/");
                            if (indexOfSlash == -1) {
                                continue;
                            }
                            pkg = intent.substring(0, indexOfSlash);
                            clz = intent.substring(indexOfSlash + 1);
                            isShortCut = true;
                        } else {
                            // folder container
                            continue;
                        }
                        ComponentName tempKey = new ComponentName(pkg, clz);
                        if (!wholeData.containsKey(tempKey)) {
                            wholeData
                                    .put(tempKey, new ParsedData(pkg, clz, title,
                                            isShortCut ? ParsedData.TYPE_SHORTCUT
                                                    : ParsedData.TYPE_WIDGET));
                        }
                        itemsInThisPage.add(new ComponentName(pkg, clz));
                    }
                    for (int i = 0; i < itemsInThisPage.size(); i++) {
                        ComponentName itemInThisPage = itemsInThisPage.get(i);
                        ParsedData data = wholeData.get(itemInThisPage);
                        for (int j = 0; j < itemsInThisPage.size(); j++) {
                            if (j == i) {
                                continue;
                            }
                            data.addData(itemsInThisPage.get(j));
                        }
                        data.addCount();
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
