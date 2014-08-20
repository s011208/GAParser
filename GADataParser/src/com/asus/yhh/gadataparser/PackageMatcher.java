
package com.asus.yhh.gadataparser;

import java.io.File;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.TextView;

public class PackageMatcher extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_NAME = "pkg_matcher.db";

    private static final String TAG = "MusicDatabaseHelper";

    private SQLiteDatabase mDb;

    private Context mContext;

    private static PackageMatcher sInstance;

    public synchronized static PackageMatcher getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new PackageMatcher(context);
        }
        return sInstance;
    }

    private SQLiteDatabase getDatabase() {
        if ((mDb == null) || (mDb != null && mDb.isOpen() == false)) {
            try {
                mDb = getWritableDatabase();
            } catch (SQLiteFullException e) {
                Log.w(TAG, "SQLiteFullException", e);
            } catch (SQLiteException e) {
                Log.w(TAG, "SQLiteException", e);
            } catch (Exception e) {
                Log.w(TAG, "Exception", e);
            }
        }
        return mDb;
    }

    private PackageMatcher(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context.getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            getDatabase().execSQL("PRAGMA synchronous = 1");
            setWriteAheadLoggingEnabled(true);
        }
        getDatabase().execSQL(
                "CREATE TABLE IF NOT EXISTS " + "pkg_matcher" + " ( " + "package"
                        + " TEXT PRIMARY KEY, " + "title " + " TEXT)");
    }

    private void addTitle(String pkg, String title) {
        ContentValues cv = new ContentValues();
        cv.put("package", pkg);
        cv.put("title", title);
        getDatabase().insert("pkg_matcher", null, cv);
    }

    private String getTitle(String pkg) {
        SQLiteStatement state = getDatabase().compileStatement(
                "select title from pkg_matcher where package='" + pkg + "'");
        String title = null;
        if (state != null) {
            try {
                title = state.simpleQueryForString();
            } catch (Exception e) {
                Log.w("QQQQ", "failed", e);
            } finally {
                state.close();
            }
        }
        return title;
    }

    public void setTitle(TextView tv, int count, String pkg) {
        String title = getTitle(pkg);
        if (title == null) {
            tv.setText(pkg + ", " + count);
            new TitleParser(tv, count, pkg).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            tv.setText(title + ", " + count);
        }
    }

    public static class TitleParser extends AsyncTask<Void, Void, String> {
        private String mPkg;

        private int mCount;

        private TextView mTxt;

        public TitleParser(TextView tv, int count, String pkg) {
            mPkg = pkg;
            mCount = count;
            mTxt = tv;
        }

        @Override
        protected String doInBackground(Void... params) {
            Log.e("QQQQ", "doInBackground: " + mPkg);
            Document doc = Jsoup.parse("https://play.google.com/store/apps/details?id=" + mPkg);
            Elements eles = doc.select("div[class=document-title]").select("div");
            String title = null;
            for (Element ele : eles) {
                Log.e("QQQQ", "ele: " + ele.text());
                title = ele.text();
            }
            return title;
        }

        @Override
        protected void onPostExecute(String params) {
            if (params != null) {
                Log.e("QQQQ", params + ", " + mCount);
                mTxt.setText(params + ", " + mCount);
            } else {
                Log.e("QQQQ", "title is null");
            }
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub

    }
}
