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
    
    public void testPaymentEditView (){
    	ShareMap sm = transactor.rawQuery("select name, amount from " + transactor.table1, null, 
			new QueryEvaluator<ShareMap>() {
				public ShareMap evaluate(Cursor cursor, ShareMap defaultResult, Object... params) {
					ShareMap map = new ShareMap();
					
					if (cursor.moveToFirst()) 
		    			do {
		    				String name = cursor.getString(0);
		    				if (map.containsKey(name))
		    					name=name+"_";
		    				map.put(name, cursor.getDouble(1));
		    			} while (cursor.moveToNext());
		    		
					return map;
				}
			}, null);
    	
    	System.out.println(sm.toString());
    	assertTrue(sm.size() > 0);
    }
    
}
