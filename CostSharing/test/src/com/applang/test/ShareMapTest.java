package com.applang.test;

import java.util.*;

import junit.framework.TestCase;

import com.applang.share.*;
import com.applang.share.ShareMap.PolicyEvaluator;
import com.applang.share.ShareMap.PolicyException;

import static com.applang.test.AssertHelper.*;
import static com.applang.share.ShareUtil.*;

public class ShareMapTest extends TestCase
{
    public ShareMapTest() {
		super();
	}

	public ShareMapTest(String method) {
		super(method);
	}
    
    public void testBasics() {
		assertEquals("xxx", param("", 1, new String[] {"x", "xxx", "zzz"}));
		assertEquals(new Integer(42), param(0, 2, new Integer[] {-24, 5, 42}));
		assertEquals(new Integer(-24), param("", 0, new Object[] {-24, 5, 42}));
		assertEquals("x", param("x", 4, new Object[] {-24, 5, 42}));
		
    	assertEquals("{param0==}", namedParams("=").toString());
    	assertEquals("{Bob=}", namedParams("Bob=").toString());
    	assertEquals("{Sue=100.}", namedParams("Sue=100.").toString());
    	Map<String, Object> map = namedParams("Sue=true", "Tom=false", "Bob", -100.);
		assertTrue(map.toString().contains("Sue=true"));
		assertTrue(map.toString().contains("Tom=false"));
		assertTrue(map.toString().contains("param2=Bob"));
		assertTrue(map.toString().contains("param3=-100.0"));
		assertTrue(Boolean.parseBoolean(namedValue("false", "Sue", map)));
		assertFalse(Boolean.parseBoolean(namedValue("true", "Tom", map)));
		assertEquals("Bob", namedValue("", "param2", map));
		assertEquals(-100.0, namedValue(0., "param3", map), delta);
		
    	map = namedParams(42, new ShareMap(), "xxx", false, "type=333");
    	assertEquals(new Integer(42), namedInteger(-1, "param0", map));
    	assertEquals("xxx", namedValue(null, "param2", map));
    	assertEquals(new Boolean(false), namedBoolean(null, "param3", map));
    	assertEquals(new Integer(333), namedInteger(null, "type", map));
    	assertEquals(new ShareMap().toString(), namedValue(null, "param1", map).toString());
    	
    	assert_ArrayEquals(new String[] {"xxx", "yy", "z"}, (String[]) parseStringArray("[xxx,yy, z]"));
    	assert_ArrayEquals(new Double[] {42.34, -3.1415, null}, (Double[]) parseDoubleArray("[42.34,-3.1415, z]"));
	}
 
	String[] participants = new String[] {"Sue", "Bob", "Tom"};
    
    @SuppressWarnings("rawtypes")
	public void testShareMap() {
    	ShareMap sm = new ShareMap(participants, new Double[] {200.,null,300.});
    	assertShareMap(sm, 200., 300.);
    	
    	sm = new ShareMap(participants, 600.);
    	assertShareMap(sm, -200., -200., -200.);
    	
    	sm = new ShareMap(participants, 600., null, 300.);
    	assertShareMap(sm, -300., -100., -200.);
    	assertEquals("Bob", ((TreeMap)sm.rawMap).firstKey());
    	
    	sm = new ShareMap(new String[] {participants[0], "", participants[2]}, 600.);
    	assertEquals("", ((TreeMap)sm.rawMap).firstKey());
    	assertShareMap(sm, -200., -200., -200.);
    	
    	assertAmountEquals(-600., sm.sum());
    	assertAmountEquals(sm.sum(), -sm.negated().sum());
    	
    	assertEquals(789, parseInteger(null, "\t789 ").intValue());
    	assertNull(parseInteger(null, "\t789. "));
    	assertNull(parseInteger(null, "\t7 89 "));
    	
    	assertEquals(789., parseDouble(null, "\t789 ").doubleValue(), delta);
    	assertNotNull(parseDouble(null, "\t789. "));
    	assertNull(parseDouble(null, "\t7 89 "));
    }	
   
    public void testPolicies() throws ShareMap.PolicyException {
    	ShareMap sm = new ShareMap(participants, 120., "1:1:1");
    	assertShareMap(sm, -40., -40., -40.);
    	
    	sm = new ShareMap(participants, 120., "0:1:2");
    	assertShareMap(sm, -40., 0., -80.);
    	
    	sm = new ShareMap(participants, 150., "");
    	assertShareMap(sm, -50., -50., -50.);
    	
    	sm = new ShareMap(participants, -1., "");
    	assertShareMap(sm, "Bob", .33, "Sue", .33, "Tom", .33);
    	
    	sm = new ShareMap(null, 150., "1:2:3");
    	assertShareMap(sm, -25., -50., -75.);
    	
    	sm = new ShareMap();
    	sm.updateWith(120., "");
    	assertShareMap(sm, placeholder(), -120.);
    	sm.updateWith(120., "3:1:2");
    	assertShareMap(sm, placeholder(1), -60., placeholder(2), -20., placeholder(3), -40.);
    	sm.updateWith(120., "1*3");
    	assertShareMap(sm, placeholder(1, 1), -40., placeholder(1, 2), -40., placeholder(1, 3), -40.);
    	sm.updateWith(120., "1*1");
    	assertShareMap(sm, placeholder(1, 1), -120.);
    	sm.updateWith(120., "0:1*2:4");
    	assertShareMap(sm, placeholder(1), 0., placeholder(2, 1), -20., placeholder(2, 2), -20., placeholder(3), -80.);
    	sm.renameWith(participants);
    	assertShareMap(sm, "Bob", -20., "Sue", 0., "Tom", -20., placeholder(3), -80.);
    	sm.updateWith(120., "Tom=2\nSue=3\nBob=1");
    	assertShareMap(sm, "Bob", -20., "Sue", -60., "Tom", -40.);
    	sm.updateWith(0., "Tom=20.\nSue=-60.\nBob=40.");
    	assertShareMap(sm, "Bob", -40., "Sue", 60., "Tom", -20.);
    	
    	try {
    		assert_ArrayEquals(new String[] {"Tom","Sue","Bob"}, sm.getNames());
			sm.updateWith(120., "3.:1:2");
	    	assertShareMap(sm, "Bob", -78., "Sue", -39., "Tom", -3.);
		} catch (ShareMap.PolicyException e) {
			fail(e.getMessage());
		}
    	try {
			sm.updateWith(120., "Tom=2\n30.\nBob=1");
			fail(ShareMap.PolicyException.INVALID);
		} catch (ShareMap.PolicyException e) {
			assertEquals(2, e.loc);
		}
    	try {
			sm.updateWith(120., "3:1:");
			fail(ShareMap.PolicyException.INVALID);
		} catch (ShareMap.PolicyException e) {
			assertEquals(3, e.loc);
		}
    	try {
			sm.updateWith(120., "3%:1:2");
			fail(ShareMap.PolicyException.INVALID);
		} catch (ShareMap.PolicyException e) {
			assertEquals(3, e.loc);
		}
    	try {
			sm.updateWith(120., "0:1*0:4");
			fail(ShareMap.PolicyException.INVALID);
		} catch (ShareMap.PolicyException e) {
			assertEquals(1, e.loc);
		}
    	try {
			sm.updateWith(120., "0:1*:4");
			fail(ShareMap.PolicyException.INVALID);
		} catch (ShareMap.PolicyException e) {
			assertEquals(1, e.loc);
		}
    	try {
    		ShareMap.checkNames(true, new String[] {null});
			fail("checkNames should have failed");
		} catch (ShareMap.PolicyException e) {
			assertEquals(5, e.loc);
		}
    	try {
    		ShareMap.checkNames(true, new String[] {""});
			fail("checkNames should have failed");
		} catch (ShareMap.PolicyException e) {
			assertEquals(5, e.loc);
		}
    	try {
    		ShareMap.checkNames(true, new String[] {"Tom","Tom"});
			fail("checkNames should have failed");
		} catch (ShareMap.PolicyException e) {
			assertEquals(4, e.loc);
		}
    	try {
    		ShareMap.checkNames(true, new String[] {kitty});
			fail("checkNames should have failed");
		} catch (ShareMap.PolicyException e) {
			assertEquals(6, e.loc);
		}
    	try {
    		ShareMap.checkNames(true, new String[] {placeholder()});
			fail("checkNames should have failed");
		} catch (ShareMap.PolicyException e) {
			assertEquals(5, e.loc);
    		try {
				ShareMap.checkNames(false, new String[] {placeholder()});
			} catch (ShareMap.PolicyException e1) {
				fail("checkNames failed");;
			}
		}
    }	
        
    public void testPolicyAnalyzing() throws ShareMap.PolicyException {
    	ShareMap sm = new ShareMap();
    	assertShareMap(sm);
    	
    	String policy = ShareMap.makePolicy(false, null);
    	assertEquals("", policy);
    	assertFalse(ShareMap.isPolicy(policy));
    	
    	policy = ShareMap.makePolicy(false, participants);
    	assertTrue(ShareMap.isPolicy(policy));
    	assertEquals("1:1:1", policy);
    	
    	policy = ShareMap.makePolicy(true, participants, 1, 2);
    	assertTrue(ShareMap.isPolicy(policy));
    	assertEquals("Sue=1\nBob=2\nTom=0", policy);
    	
    	final String policy0 = ShareMap.makePolicy(true, participants, 1, 2., null);
    	assertTrue(ShareMap.isPolicy(policy0));
    	assertEquals("Sue=1\nBob=2.0\nTom=0", policy0);
    	
    	ShareMap.analyzePolicy(policy0, "", new PolicyEvaluator<Void>() {
			public Void evaluate(List<String> names, List<Integer> proportions, List<Double> portions) throws PolicyException {
				int len = names.size();
				
				assertEquals(3, len);
				assert_ArrayEquals(participants, names.toArray(new String[0]));
				assert_ArrayEquals(new Integer[]{1,null,0}, proportions.toArray(new Integer[0]));
				assert_ArrayEquals(new Double[]{null,2.,null}, portions.toArray(new Double[0]));
				
		    	String policy1 = "1:2.:0";
		    	String policy2 = ShareMap.translatePolicy(true, policy1, (String[])null);
		    	names.clear();
				assertEquals(3, ShareMap.parsePolicy(policy2, "", names));
				for (int i = 0; i < 3; i++)
					assertEquals(placeholder(1 + i), names.get(i));
				policy2 = ShareMap.translatePolicy(true, policy1, participants);
		    	assertEquals(policy0, policy2);
				
				return null;
			}
		});
    }	
    
    public void testSpender() throws ShareMap.PolicyException {
    	ShareMap sm = new ShareMap(participants, 120., "1:2:3");
    	assertShareMap(sm, "Bob", -40., "Sue", -20., "Tom", -60.);
    	
    	String[] keys = sm.getSpenderKeys().toArray(new String[0]);
		ShareMap.checkNames(true, keys);
		
    	sm.setSpender(participants[2]);
    	assertShareMap(sm, "Tom", -60., "Bob", -40., "Sue", -20.);
    	
    	assert_ArrayEquals(new String[] {"Sue", "Bob", "Tom"}, participants);
    	
    	sm.setSpender(null);
    	assertShareMap(sm, "Bob", -40., "Sue", -20., "Tom", -60.);
    }
    
    public void testFilter() {
		ShareMap sm = new ShareMap();
		sm.rawMap.put(kitty, 60.);
		sm.rawMap.put("Bob", 20.);
		sm.rawMap.put("Tom", 40.);
		assert_ArrayEquals(filter(sm.rawMap.keySet(), false, isKitty).toArray(), new Object[] {kitty});
		assert_ArrayEquals(filter(sm.rawMap.keySet(), true, isKitty).toArray(), new Object[] {"Bob", "Tom"});
		sm.setNames(sm.rawMap.keySet().toArray(new String[0]));
		sm.setSpender("Tom");
		assert_ArrayEquals(filter(Arrays.asList(sm.getNames()), true, isKitty).toArray(), new Object[] {"Tom", "Bob"});
    }	

}
