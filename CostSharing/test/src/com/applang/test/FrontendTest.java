package com.applang.test;

import com.applang.CostSharingActivity;
import com.applang.db.*;
import com.applang.share.*;

public class FrontendTest extends ActivityTest<CostSharingActivity>
{
	public FrontendTest() {
		super("com.applang", CostSharingActivity.class);
	}

/*	public FrontendTest(String method) {
		super(method);
	}
*/
    private Transactor transactor;

    @Override
    protected void setUp() throws Exception {
    	super.setUp();	//	database file exists ?
        
        transactor = new Transactor(this.getActivity());
    }

	@Override
    protected void tearDown() throws Exception {
        super.tearDown();
	}
    
    public void testClear (){
    	transactor.clear();
	}
    
    public void testEntries() {
    	String[] entries = transactor.getEntryStrings("", ShareUtil.tableName());;
    	for (String string : entries) {
        	System.out.println(string);
		}
    	assertTrue(entries.length > 0);
    }
    
}
