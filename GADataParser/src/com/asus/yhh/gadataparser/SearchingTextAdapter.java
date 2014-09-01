
package com.asus.yhh.gadataparser;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;

public class SearchingTextAdapter extends ArrayAdapter<String> {
    private final ArrayList<String> mResultList = new ArrayList<String>();

    public SearchingTextAdapter(Context context, int resource) {
        super(context, resource);
    }

    public void addData(ArrayList<String> data) {
        mResultList.addAll(data);
    }

    public void addData(String data) {
        mResultList.add(data);
    }

    public void clearData() {
        mResultList.clear();
    }

    @Override
    public int getCount() {
        return mResultList.size();
    }

    @Override
    public String getItem(int index) {
        return mResultList.get(index);
    }
}
