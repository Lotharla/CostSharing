package com.applang.test;

import java.util.*;

import android.database.Cursor;
import android.util.Log;

import com.applang.db.*;
import com.applang.db.Transactor.QueryEvaluator;
import com.applang.share.*;

public class FrontendTest extends ActivityTest 
{
	public FrontendTest() {
		super();
	}

	public FrontendTest(String method) {
		super(method);
	}

    private Transactor transactor;

    @Override
    protected void setUp() throws Exception {
        transactor = new Transactor(this.getActivity());
        
        super.setUp();	//	database file exists ?
    }

	@Override
    protected void tearDown() throws Exception {
		transactor.close();
        super.tearDown();
	}
    
    public void testClear (){
    	transactor.clear();
	}
    
    public void testEntries() {
    	String[] entries = transactor.getEntryStrings("", transactor.tableName(null));;
    	for (String string : entries) {
        	System.out.println(string);
		}
    	assertTrue(entries.length > 0);
    }
    
}
