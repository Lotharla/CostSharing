package com.applang.test;

import java.io.File;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.*;
import android.os.Environment;
import android.util.Log;

import com.applang.db.*;

public class ExperimentalTests extends ActivityTest
{
	public ExperimentalTests() {
		super();
	}

	public ExperimentalTests(String method) {
		super(method);
	}

	public void testRelatedTables() {
		Helper.setDataDirectory(Environment.getDataDirectory().getPath());
		SQLiteDatabase db = SQLiteDatabase.openDatabase(
			new File(Helper.databasesDir(), "test.db").getPath(), 
       		null, 
       		SQLiteDatabase.CREATE_IF_NECESSARY);
		
        db.setVersion(1);
        db.setLocale(Locale.getDefault());
        db.setLockingEnabled(true);
        
        db.execSQL("drop table if exists tbl_countries");
        db.execSQL("drop table if exists tbl_states");
        
        final String CREATE_TABLE_COUNTRIES =
        	"CREATE TABLE tbl_countries ("
        	+ "id INTEGER PRIMARY KEY AUTOINCREMENT,"
        	+ "country_name TEXT);";
        final String CREATE_TABLE_STATES = 
        	"CREATE TABLE tbl_states ("
        	+ "id INTEGER PRIMARY KEY AUTOINCREMENT,"
        	+ "state_name TEXT,"
        	+ "country_id INTEGER NOT NULL CONSTRAINT "
        	+ "country_id REFERENCES tbl_countries(id) "
        	+ "ON DELETE CASCADE);";
        db.execSQL(CREATE_TABLE_COUNTRIES);
        db.execSQL(CREATE_TABLE_STATES);
        final String CREATE_TRIGGER_STATES = 
        	"CREATE TRIGGER fk_insert_state BEFORE "
        	+ "INSERT on tbl_states "
        	+ "FOR EACH ROW "
        	+ "BEGIN "
        	+ "SELECT RAISE(ROLLBACK, 'insert on table "
        	+ "\"tbl_states\" violates foreign key constraint "
        	+ "\"fk_insert_state\"') WHERE (SELECT id FROM "
        	+ "tbl_countries WHERE id = NEW.country_id) IS NULL; "
        	+ "END;";
        db.execSQL(CREATE_TRIGGER_STATES);
        
        ContentValues values = new ContentValues();
        values.put("country_name", "US");
        long countryId = db.insert("tbl_countries", null, values);
        
        ContentValues stateValues = new ContentValues();
        stateValues.put("state_name", "Texas");
        stateValues.put("country_id", Long.toString(countryId));
        try {
            db.insertOrThrow("tbl_states", null, stateValues);
        } catch (Exception e) {
        	Log.e("Error insert", e.getMessage());
        }
        
        ContentValues updateCountry = new ContentValues();
        updateCountry.put("country_name", "United States");
        db.update("tbl_countries", updateCountry, "id=?", new String[] {Long.toString(countryId)});
        
        Cursor cur = db.query("tbl_countries", null, null, null, null, null, null);
		if (cur.moveToFirst()) do {
            System.out.println(cur.getString(1));
		} while (cur.moveToNext());
    	cur.moveToFirst();
    	String country_id = cur.getString(0);
        cur.close();

        db.beginTransaction();
        try {
        	values = new ContentValues();
        	values.put("state_name", "Georgia");
        	values.put("country_id", country_id);
        	long stateId = db.insert("tbl_states", null, values);
        	db.setTransactionSuccessful();
        	System.out.println(Long.toString(stateId));
        } catch (Exception e) {
        	Log.e("Error in transaction", e.getMessage());
        } finally {
        	db.endTransaction();
        }
        
        db.delete("tbl_states", "id=?", new String[] {Long.toString(countryId)});
        
		db.close();
	}

}
