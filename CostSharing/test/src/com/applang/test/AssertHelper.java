package com.applang.test;

import java.util.*;

import static junit.framework.Assert.*;

import com.applang.share.*;

public class AssertHelper
{
   
    public static <T> void assertArrayEquals(T[] expected, T[] actual) {
    	assertEquals(expected.length, actual.length);
    	for (int i = 0; i < actual.length; i++) 
        	assertEquals(expected[i], actual[i]);
    }
    
    public static void assertAmountZero(String message, Double actual) {
    	assertEquals(message, 0, actual, Util.delta);
    }
    
    public static void assertAmountEquals(Double expected, Double actual) {
    	assertEquals(expected, actual, Util.delta);
    }
    
    public static void assertAmountEquals(String message, Double expected, Double actual) {
    	assertEquals(message, expected, actual, Util.delta);
//    	String exp = Util.formatAmount(expected);
//    	String act = Util.formatAmount(actual);
//    	assertEquals(message, exp, act);
    }
    
    public static void assertShareMap(ShareMap actual, Object... expected) {
//    	System.out.println(actual.toString()); 			/* ascending order of keys by default */
    	
    	int len = expected.length;
    	boolean mapEntry = len > 1 && expected[0] instanceof String && expected[1] instanceof Double;
    	assertEquals("unexpected map size.", len, (mapEntry ? 2 : 1) * actual.rawMap.size());
    	if (len < 1) 
    		return;
    	
    	if (mapEntry) {
	    	Iterator<Map.Entry<String, Double>> it = actual.getMap().entrySet().iterator();
	    	for (int i = 0; i < len; i+=2) {
	    		Map.Entry<String, Double> entry = it.next();
	    		assertEquals(expected[i].toString(), entry.getKey());
	    		assertEquals((Double)expected[i + 1], entry.getValue(), Util.minAmount);
	    	}
    	}
    	else if (expected[0] instanceof Double) {
	    	Iterator<Double> it = actual.getMap().values().iterator();
	    	for (int i = 0; i < len; i++) 
	    		assertEquals((Double)expected[i], it.next(), Util.minAmount);
    	}
    	else if (expected[0] instanceof String) {
	    	Iterator<String> it = actual.getMap().keySet().iterator();
	    	for (int i = 0; i < len; i++) 
	    		assertEquals(expected[i].toString(), it.next());
    	}
    	else
    		throw new IllegalArgumentException();
    	
    	if (actual.getAmount() != null)
    		assertTrue("missing completeness", actual.isComplete());
    }

}
