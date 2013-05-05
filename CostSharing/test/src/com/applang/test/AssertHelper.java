package com.applang.test;

import java.util.*;

import static junit.framework.Assert.*;

import com.applang.share.*;

public class AssertHelper
{
   
    public static <T> void assert_ArrayEquals(T[] expected, T[] actual) {
    	assert_ArrayEquals("", expected, actual);
    }
    
    public static <T> void assert_ArrayEquals(String message, T[] expected, T[] actual) {
    	assertEquals(message, expected.length, actual.length);
    	for (int i = 0; i < expected.length; i++) 
        	assertEquals(message, expected[i], actual[i]);
    }
    
    public static <T> void assert_ArrayEquals(String message, Object[] expected, Object[] actual, double delta) {
    	assertEquals(message, expected.length, actual.length);
    	for (int i = 0; i < expected.length; i++) 
        	assertEquals(message, (Double) expected[i], (Double) actual[i], delta);
    }
    
    public static void assertMapEquals(Map<String, Double> actual, Object... expected) {
    	assertMapEquals("", actual, expected);
    }
    
    public static void assertMapEquals(String message, Map<String, Double> actual, Object... expected) {
    	assertEquals(message, expected.length, 2 * actual.size());
		Iterator<Map.Entry<String, Double>> it = actual.entrySet().iterator();
    	for (int i = 0; i < expected.length; i+=2) {
    		Map.Entry<String, Double> entry = it.next();
    		assertEquals(message, expected[i].toString(), entry.getKey());
    		assertEquals(message, (Double) expected[i + 1], entry.getValue(), ShareUtil.minAmount);
    	}
    }
    
    public static void assertAmountZero(String message, Double amount) {
    	assertNotNull("amount is null instead of zero", amount);
    	assertEquals(message, 0., amount, ShareUtil.delta);
    }
    
    public static void assertAmountEquals(Double expected, Double actual) {
    	assertEquals(expected, actual, ShareUtil.delta);
    }
    
    public static void assertAmountEquals(String message, Double expected, Double actual) {
    	assertEquals(message, expected, actual, ShareUtil.delta);
//    	String exp = Util.formatAmount(expected);
//    	String act = Util.formatAmount(actual);
//    	assertEquals(message, exp, act);
    }
    
    public static void assertShareMap(ShareMap actual, Object... expected) {
    	assertShareMap("", actual, expected);
    }
    
    public static void assertShareMap(String message, ShareMap actual, Object... expected) {
//    	System.out.println(actual.toString()); 			/* ascending order of keys by default */
    	
    	int len = expected.length;
    	boolean mapEntry = len > 1 && expected[0] instanceof String && expected[1] instanceof Double;
    	assertEquals("unexpected map size.", len, (mapEntry ? 2 : 1) * actual.rawMap.size());
    	if (len < 1) 
    		return;
    	
    	TreeMap<String, Double> map = actual.getSpenderMap();
    	
    	if (mapEntry) 
    		assertMapEquals(message, map, expected);
    	else if (expected[0] instanceof Double) 
    		assert_ArrayEquals(message, expected, map.values().toArray(), ShareUtil.minAmount);
    	else if (expected[0] instanceof String) 
    		assert_ArrayEquals(message, expected, map.keySet().toArray());
    	else
    		throw new IllegalArgumentException();
    	
    	if (actual.getAmount() != null)
    		assertTrue("missing completeness", actual.isComplete());
    }

}
