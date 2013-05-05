package com.applang.test;

import android.app.Activity;
import android.test.*;

import com.jayway.android.robotium.solo.Solo;

public class ActivityTest<T extends Activity> extends ActivityInstrumentationTestCase2<T> 
{
/*	public static void main(String[] args) {
		TestSuite suite = new TestSuite(CashPointTest.class);
		InstrumentationTestRunner.run(suite);
	}
	
	public static Test suite() {
	    return new android.test.suitebuilder.TestSuiteBuilder(BackendTest.class).
	    		includePackages("com.applang.test.BackendTest").build();
	}
*/
	protected Solo solo;

	public ActivityTest(String pkg, Class<T> activityClass) {
		super(pkg, activityClass);
	}
	
/*	public ActivityTest(String method) {
		this();
	}
*/
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
		solo = new Solo(getInstrumentation(), getActivity());
    }

    @Override
    protected void tearDown() throws Exception {
		solo.finishOpenedActivities();
		
        super.tearDown();
	}
}
