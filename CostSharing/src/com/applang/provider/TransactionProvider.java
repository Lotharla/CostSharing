package com.applang.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.applang.provider.Transaction.Records;

import static com.applang.Util.*;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class TransactionProvider extends ContentProvider
{
	public static final int VERSION = 1;

	private static final String TAG = TransactionProvider.class.getSimpleName();

    private static List<String> tableList = new ArrayList<String>(Transaction.tableDefs.keySet());
    
    public static Uri contentUri(int index) {
    	if (index > -1 && index < tableList.size())
    		return Uri.parse("content://" + Transaction.AUTHORITY + "/" + tableList.get(index));
    	else
    		return null;
    }
    
    public static Uri contentUri(String name) {
    	return Uri.parse("content://" + Transaction.AUTHORITY + "/" + name);
    }

    protected static void createTables(SQLiteDatabase db) {
    	Cursor cursor = null;
    	try {
			cursor = db.rawQuery("pragma foreign_keys = ON", null);
		} 
    	catch (SQLiteException e) {}
    	finally {
			if (cursor != null)
				cursor.close();
		}
    	
		for (int i = 0; i < tableList.size(); i++) 
			createTable(i, db);
	}
    
    protected static void createTable(int index, SQLiteDatabase db, Object... params) {
    	if (index > -1 && index < tableList.size()) {
			String table = tableList.get(index);
			String sql = String.format("create table %s %s (%s);", 
					param("", 1, params), 
					table, 
					Transaction.tableDefs.get(table));
			db.execSQL(sql);
    	}
	}
    
    protected static void recreateTables(SQLiteDatabase db) {
		for (int i = tableList.size() - 1; i > -1; i--) 
			db.execSQL("DROP TABLE IF EXISTS " + tableList.get(i));
		
    	createTables(db);
	}

	public static String databaseName() {
		return "data";
	}
	
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, databaseName(), null, VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
			createTables(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion
					+ " to " + newVersion
					+ ", which will destroy all old data");

			recreateTables(db);
        }
    }

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }
    
    public void clear() {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
    	recreateTables(db);
    }
    
    public void drop(String tableName) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + tableName);
	}
    
    public void create(String tableName) {
    	int index = tableList.indexOf(tableName);
    	if (index > -1) {
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            createTable(index, db);
    	}
	}
    
    public void rename(String oldTableName, String newTableName) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		db.execSQL("ALTER TABLE " + oldTableName + " RENAME TO " + newTableName);
	}

    private static final UriMatcher sUriMatcher;

	private static final int DIRECTORY = 0;
	private static final int ITEM = 1;
	private static final int RAW = 2;
	private static final int DROP = 3;
	private static final int CREATE = 4;
	private static final int RENAME = 5;
	private static final int CLEAR = 6;

	private static HashMap<String, String> sProjectionMap;

	static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        for (String table : tableList) {
        	sUriMatcher.addURI(Transaction.AUTHORITY, table, DIRECTORY);
        	sUriMatcher.addURI(Transaction.AUTHORITY, table + "/#", ITEM);
		}
        sUriMatcher.addURI(Transaction.AUTHORITY, "raw", RAW);
        sUriMatcher.addURI(Transaction.AUTHORITY, "drop/*", DROP);
        sUriMatcher.addURI(Transaction.AUTHORITY, "create/*", CREATE);
        sUriMatcher.addURI(Transaction.AUTHORITY, "rename/*/*", RENAME);
        sUriMatcher.addURI(Transaction.AUTHORITY, "clear", CLEAR);

        sProjectionMap = new HashMap<String, String>();
        sProjectionMap.put(Records._ID, Records._ID);
        sProjectionMap.put(Records.ENTRY, Records.ENTRY);
        sProjectionMap.put(Records.NAME, Records.NAME);
        sProjectionMap.put(Records.AMOUNT, Records.AMOUNT);
        sProjectionMap.put(Records.CURRENCY, Records.CURRENCY);
        sProjectionMap.put(Records.TIMESTAMP, Records.TIMESTAMP);
        sProjectionMap.put(Records.COMMENT, Records.COMMENT);
        sProjectionMap.put(Records.FLAGS, Records.FLAGS);
        sProjectionMap.put("count", "count(*)");
    }

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        String tableName = uri.getPathSegments().get(0);
        int type = sUriMatcher.match(uri);
        switch (type) {
		case ITEM:
        	qb.appendWhere(Records._ID + "=" + uri.getPathSegments().get(1));

		case DIRECTORY:
            qb.setTables(tableName);
			break;

		case RENAME:
			rename(uri.getPathSegments().get(1), uri.getPathSegments().get(2));
			return null;
			
        case DROP:
			drop(uri.getPathSegments().get(1));
			return null;
			
        case CREATE:
			create(uri.getPathSegments().get(1));
			return null;
			
        case CLEAR:
			clear();
			return null;
			
        case RAW:
			break;
			
		default:
            throw new IllegalArgumentException("Unknown URI " + uri);
		}
        
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        
        if (type == RAW)
        	return db.rawQuery(selection, selectionArgs);
        
        String orderBy;
        if (notNullOrEmpty(sortOrder)) 
            orderBy = sortOrder;
        else
        	orderBy = Records.DEFAULT_SORT_ORDER;

        String groupBy = null, having = null;
        int index = selection.indexOf("group by");
        if (index > -1) {
        	groupBy = selection.substring(index + 8).trim();
        	selection = selection.substring(0, index).trim();
        	having = "";
        	index = groupBy.indexOf("having");
        	if (index > -1) {
        		having = groupBy.substring(index + 6).trim();
        		groupBy = groupBy.substring(0, index).trim();
        	}
        }
        
		Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy, having, orderBy);

        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
	}

	@Override
	public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
        case RAW:
        case DROP:
        case CREATE:
        case RENAME:
        case CLEAR:
        case DIRECTORY:
            return Records.CONTENT_TYPE;

        case ITEM:
      		return Records.CONTENT_ITEM_TYPE;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
        if (sUriMatcher.match(uri) != DIRECTORY) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) 
            values = new ContentValues(initialValues);
        else 
            values = new ContentValues();

        if (values.containsKey(Records.ENTRY) == false) {
            values.put(Records.ENTRY, -1);
        }
        if (values.containsKey(Records.AMOUNT) == false) {
            values.put(Records.AMOUNT, 0.0);
        }
        if (values.containsKey(Records.NAME) == false) {
            values.put(Records.NAME, "");
        }
        if (values.containsKey(Records.FLAGS) == false) {
            values.put(Records.FLAGS, 0);
        }
        
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        
        String tableName = uri.getPathSegments().get(0);
        long rowId = db.insert(tableName, Records.TIMESTAMP, values);
        if (rowId > 0) {
            Uri recUri = ContentUris.withAppendedId(contentUri(tableName), rowId);
            getContext().getContentResolver().notifyChange(contentUri(tableName), null);
            return recUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String tableName = uri.getPathSegments().get(0);
        
        int count;
        switch (sUriMatcher.match(uri)) {
        case DIRECTORY:
            count = db.delete(tableName, where, whereArgs);
            break;

        case ITEM:
            String recId = uri.getPathSegments().get(1);
            count = db.delete(tableName, 
            		Records._ID + "=" + recId + 
                    (notNullOrEmpty(where) ? " AND (" + where + ')' : ""), 
                    whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(contentUri(tableName), null);
        return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String tableName = uri.getPathSegments().get(0);
        
        int count;
        switch (sUriMatcher.match(uri)) {
        case DIRECTORY:
            count = db.update(tableName, values, where, whereArgs);
            break;

        case ITEM:
            String recId = uri.getPathSegments().get(1);
            count = db.update(tableName, 
            		values, 
            		Records._ID + "=" + recId + 
            		(notNullOrEmpty(where) ? " AND (" + where + ')' : ""), 
            		whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(contentUri(tableName), null);
        return count;
	}

}
