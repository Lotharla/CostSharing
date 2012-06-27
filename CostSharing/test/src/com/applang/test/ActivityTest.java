package com.applang.test;

import java.io.File;

import junit.framework.Test;

import android.os.Environment;
import android.test.*;
import android.test.suitebuilder.TestSuiteBuilder;

import com.applang.*;
import com.applang.db.*;

public class ActivityTest extends ActivityInstrumentationTestCase2<CostSharingActivity> 
{
	public static void main(String[] args) {
//		TestSuite suite = new TestSuite(CashPointTest.class);
//		InstrumentationTestRunner.run(suite);
	}
	
	public static Test suite() {
	    return new TestSuiteBuilder(BackendTest.class).
	    		includePackages("com.applang.test.BackendTest").build();
	}

	public ActivityTest() {
		super("com.applang", CostSharingActivity.class);
	}
	
	public ActivityTest(String method) {
		this();
	}

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    	
    	File dbDir = new File(Environment.getDataDirectory(), Helper.pathToDatabases());
    	assertTrue(new File(dbDir, Helper.databaseName()).exists());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
	}
}
