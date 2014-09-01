
package com.asus.yhh.gadataparser;

import java.io.IOException;
import java.lang.ref.WeakReference;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.TextView;

public class PackageMatcher extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_NAME = "pkg_matcher.db";

    private static final String TAG = "PackageMatcher";

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

    public String getTitle(String pkg) {
        Cursor titleCursor = getDatabase().rawQuery(
                "select title from pkg_matcher where package='" + pkg + "'", null);

        String title = null;
        if (titleCursor != null) {
            try {
                if (titleCursor.getCount() > 0) {
                    titleCursor.moveToNext();
                    title = titleCursor.getString(0);
                } else {
                    // ignore
                }
            } catch (Exception e) {
                Log.w(TAG, "failed", e);
            } finally {
                titleCursor.close();
            }
        }
        return title;
    }

    public void setTitle(TextView tv, int count, String pkg) {
        String title = getTitle(pkg);
        if (title == null) {
            tv.setText(pkg + ", " + count);
            new TitleParser(tv, count, pkg, mContext)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            tv.setText(title + ", " + count);
        }
    }

    public static class TitleParser extends AsyncTask<Void, Void, String> {
        private String mPkg;

        private int mCount;

        private TextView mTxt;

        private WeakReference<Context> mContext;

        public TitleParser(TextView tv, int count, String pkg, Context context) {
            mPkg = pkg;
            mCount = count;
            mTxt = tv;
            mContext = new WeakReference<Context>(context);
        }

        @Override
        protected String doInBackground(Void... params) {
            Log.d(TAG, "doInBackground: " + mPkg);
            Document doc;
            String title = null;
            try {
                doc = Jsoup.connect("https://play.google.com/store/apps/details?id=" + mPkg).get();
                Elements eles = doc.select("div[class=document-title]").select("div");
                for (Element ele : eles) {
                    title = ele.text();
                }
            } catch (IOException e) {
                Log.w(TAG, "failed", e);
            }
            return title;
        }

        @Override
        protected void onPostExecute(String params) {
            if (params != null) {
                Log.i(TAG, params + ", " + mCount);
                Context context = mContext.get();
                if (context != null) {
                    getInstance(context).addTitle(mPkg, params);
                }
                mTxt.setText(params + ", " + mCount);
            } else {
                Log.w(TAG, "title is null");
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
