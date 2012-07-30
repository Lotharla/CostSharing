package com.applang.test;

import java.util.*;

import android.database.Cursor;

import com.applang.db.*;
import com.applang.share.*;

import static com.applang.test.AssertHelper.*;

public class BackendTest extends ActivityTest 
{
	public BackendTest() {
		super();
	}

	public BackendTest(String method) {
		super(method);
	}

	public Transactor transactor;

    @Override
    protected void setUp() throws Exception {
        transactor = new Transactor(this.getActivity());
        
        super.setUp();	//	database file exists ?
    	
    	transactor.clear();
    }
    
    protected String suffix = "";

	@Override
    protected void tearDown() throws Exception {
		saveTest(suffix);
		transactor.close();
        super.tearDown();
	}
    
    String[] participants = new String[] {"Sue", "Bob", "Tom"};
    
    public void testShareMap() throws ShareMap.PolicyException {
    	ShareMap sm = new ShareMap(participants, new Double[] {200.,null,300.});
    	assertShareMap(sm, 200., 300.);
    	
    	sm = new ShareMap(participants, 600.);
    	assertShareMap(sm, -200., -200., -200.);
    	
    	sm = new ShareMap(participants, 600., null, 300.);
    	assertShareMap(sm, -300., -100., -200.);
    	assertEquals("Bob", sm.rawMap.firstKey());
    	
    	sm = new ShareMap(new String[] {participants[0], "", participants[2]}, 600.);
    	assertEquals("", sm.rawMap.firstKey());
    	assertShareMap(sm, -200., -200., -200.);
    	
    	assertAmountEquals(-600., sm.sum());
    	assertAmountEquals(sm.sum(), -sm.negated().sum());
    	
    	assertEquals(789, Util.parseInt(null, "\t789 ").intValue());
    	assertNull(Util.parseInt(null, "\t789. "));
    	assertNull(Util.parseInt(null, "\t7 89 "));
    	
    	assertEquals(789., Util.parseDouble(null, "\t789 ").doubleValue());
    	assertNotNull(Util.parseDouble(null, "\t789. "));
    	assertNull(Util.parseDouble(null, "\t7 89 "));
    	
    	sm = new ShareMap(participants, 120., "1:1:1");
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
    	assertShareMap(sm, Util.placeholder(), -120.);
    	sm.updateWith(120., "3:1:2");
    	assertShareMap(sm, Util.placeholder(1), -60., Util.placeholder(2), -20., Util.placeholder(3), -40.);
    	sm.updateWith(120., "1*3");
    	assertShareMap(sm, Util.placeholder(1, 1), -40., Util.placeholder(1, 2), -40., Util.placeholder(1, 3), -40.);
    	sm.updateWith(120., "1*1");
    	assertShareMap(sm, Util.placeholder(1, 1), -120.);
    	sm.updateWith(120., "0:1*2:4");
    	assertShareMap(sm, Util.placeholder(1), 0., Util.placeholder(2, 1), -20., Util.placeholder(2, 2), -20., Util.placeholder(3), -80.);
    	sm.renameWith(participants);
    	assertShareMap(sm, "Bob", -20., "Sue", 0., "Tom", -20., Util.placeholder(3), -80.);
    	sm.updateWith(120., "Tom=2\nSue=3\nBob=1");
    	assertShareMap(sm, "Bob", -20., "Sue", -60., "Tom", -40.);
    	sm.updateWith(0., "Tom=20.\nSue=-60.\nBob=40.");
    	assertShareMap(sm, "Bob", -40., "Sue", 60., "Tom", -20.);
    	
    	try {
    		assertArrayEquals(new String[] {"Tom","Sue","Bob"}, sm.getNames());
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
			sm.checkNames(new String[] {null});
			fail("checkNames should have failed");
		} catch (ShareMap.PolicyException e) {
			assertEquals(4, e.loc);
		}
    	try {
			sm.checkNames(new String[] {"Tom","Tom"});
			fail("checkNames should have failed");
		} catch (ShareMap.PolicyException e) {
			assertEquals(4, e.loc);
		}
    	
    	sm = new ShareMap();
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
    	
    	policy = ShareMap.makePolicy(true, participants, 1, 2., null);
    	assertTrue(ShareMap.isPolicy(policy));
    	assertEquals("Sue=1\nBob=2.0\nTom=0", policy);
    	
		List<String> names = new ArrayList<String>();
		List<Integer> proportions = new ArrayList<Integer>();
		List<Double> values = new ArrayList<Double>();
		assertEquals(3, ShareMap.parsePolicy(policy, "2", names, proportions, values));
		assertArrayEquals(participants, names.toArray(new String[0]));
		assertArrayEquals(new Integer[]{1,null,0}, proportions.toArray(new Integer[0]));
		assertArrayEquals(new Double[]{1.,2.,0.}, values.toArray(new Double[0]));
		
    	String policy1 = "1:2.:0";
    	String policy2 = ShareMap.translatePolicy(policy1, (String[])null);
    	names.clear();
		assertEquals(3, ShareMap.parsePolicy(policy2, "", names));
		for (int i = 0; i < 3; i++)
			assertEquals(Util.placeholder(1 + i), names.get(i));
		policy2 = ShareMap.translatePolicy(policy1, participants);
    	assertEquals(policy, policy2);
    	
    	sm = new ShareMap(participants, 120., "1:2:3");
    	assertShareMap(sm, "Bob", -40., "Sue", -20., "Tom", -60.);
    	sm.setSpender(participants[0]);
		sm.checkNames(sm.getKeys().toArray(new String[0]));
    	assertShareMap(sm, "Sue", -20., "Bob", -40., "Tom", -60.);
    	sm.setSpender(null);
    	assertShareMap(sm, "Bob", -40., "Sue", -20., "Tom", -60.);
    }

    void entrySizeTest(final int expected, int entry) {
    	assertTrue("invalid entry", entry > -1);
    	
    	transactor.fetchEntry(entry, 
    		new Transactor.QueryEvaluator<Void>() {
				public Void evaluate(Cursor cursor, Void defaultResult, Object... params) {
			    	assertNotNull(cursor);
			    	assertEquals(expected, cursor.getCount());
					return defaultResult;
				}
			}, null);
    }
    
    public void testEntry() {
    	int entryId = transactor.addEntry("Bob", 100.5, null, "gas", true);
    	assertEquals(transactor.getNewEntryId() - 1, entryId);
    	
    	transactor.fetchEntry(entryId, 
    		new Transactor.QueryEvaluator<Void>() {
				public Void evaluate(Cursor cursor, Void defaultResult, Object... params) {
			    	assertNotNull(cursor);
			    	assertTrue(cursor.moveToFirst());
					assertEquals("Bob", cursor.getString(1));
					assertEquals(100.5, cursor.getDouble(2));
					assertEquals(null, cursor.getString(3));
					String now = Helper.timestampNow();
					assertEquals(now.substring(0, 16), 
							cursor.getString(4).substring(0, 16));
					assertEquals("gas", cursor.getString(5));
			    	assertTrue(cursor.getInt(6) > 0);
					return defaultResult;
				}
			}, null);    	
		
		assertEquals(1, transactor.removeEntry(entryId));
		entrySizeTest(0, entryId);
	}
    
    void zeroSumTest(int entry, String andCondition) {
		double sum = transactor.getSum("entry=" + entry + andCondition);
		assertAmountZero("Transaction leaking.", sum);
    }
    
    int allocationTest(final boolean expense) {
    	final int sign = expense ? 1 : -1;
    	final double allocation = 120 * sign;
    	
    	int entryId = transactor.addEntry(expense ? "Bob" : "", allocation, null, "test", expense);
    	
    	ShareMap portions = new ShareMap(
    			new String[] {"Bob","Sue","Tom"}, 
    			allocation, 
    			null, allocation / 4, allocation / 3);
    	
    	assertTrue(transactor.allocate(expense, entryId, portions.rawMap));
    	
    	entrySizeTest(4, entryId);
    	zeroSumTest(entryId, "");
		
		assertNotNull("timestamp missing", transactor.fetchField(entryId, "timestamp"));
		
		transactor.fetchEntry(entryId, 
    		new Transactor.QueryEvaluator<Void>() {
				public Void evaluate(Cursor cursor, Void defaultResult, Object... params) {
			    	assertNotNull(cursor);
			    	if (cursor.moveToFirst()) do {
			        	assertEquals(expense, transactor.isExpense(cursor.getLong(transactor.columnIndex("ROWID"))));
			        	double amount = cursor.getDouble(transactor.columnIndex("amount"));
			        	if (cursor.getString(transactor.columnIndex("timestamp")) != null)
			        		assertAmountEquals("allocated amount wrong", allocation, amount);
			        	else {
				        	String name = cursor.getString(transactor.columnIndex("name"));
							if (name.equals("Sue")) assertAmountEquals("Sue 's portion wrong.", -30. * sign, amount);
							if (name.equals("Tom")) assertAmountEquals("Tom 's portion wrong.", -40. * sign, amount);
							if (name.equals("Bob")) assertAmountEquals("Bob 's portion wrong.", -50. * sign, amount);
						}
					} while (cursor.moveToNext());
					return defaultResult;
				}
			}, null);
		
    	return entryId;
    }
    
    public void testAllocation() {
    	Set<Integer> ids = new HashSet<Integer>();
    	
    	ids.add(allocationTest(true));
    	ids.add(allocationTest(false));
    	
    	for (Integer id : ids) 
    		if (id > -1) {
    			transactor.removeEntry(id);
				entrySizeTest(0, id);
	    	}
    	
    	assertTrue(-1 < transactor.performSubmission(participants[0], 99., "test1"));
    	assertEquals(-2, transactor.performExpense("", "test1", new ShareMap(participants, -100.)));
    	
    	int id = transactor.performExpense("", "test1", new ShareMap(participants, -99.));
    	entrySizeTest(4, id);
    	zeroSumTest(id, "");
    	
    	totalTest(99.);
    	
    	id = transactor.performExpense(participants[2], "test2", new ShareMap(participants, 70., "3:0:4"));
    	entrySizeTest(4, id);
    	zeroSumTest(id, " and expense > 0");
    }
    
    public void testSaveLoad() {
    	for (String participant : participants)
    		submissionTest(participant, 50, "stake");
    	
    	int count = transactor.getCount(null);
    	double sum = transactor.getSum(null);
		assertEquals(participants.length, count);
    	
    	saveTest("test");
    	
    	transactor.clear();
		assertEquals(0, transactor.getCount(null));
		
		loadTest("test");
		
		assertEquals(count, transactor.getCount(null));
		assertEquals(sum, transactor.getSum(null));
 	}
    
    void saveTest(String suffix) {
    	if (suffix.length() < 1)
    		return;
    	
    	String tableName = transactor.tableName(suffix);
    	transactor.drop(tableName);
    	assertTrue(transactor.saveAs(suffix));
    	assertTrue(transactor.savedTables().contains(tableName));
    }
    
    void loadTest(String suffix) {
    	String tableName = transactor.tableName(suffix);
    	assertTrue(transactor.savedTables().contains(tableName));
    	transactor.drop(transactor.tableName(null));
		assertTrue(transactor.loadFrom(suffix));
    }
    
    public void testRingTransfer() {
    	double someAmount = 312.54;
    	String purpose = "ring transfer";
    	
    	List<String> names = Arrays.asList(participants);
    	Iterator<String> it = names.iterator();
    	String submitter = it.next();
    	while (it.hasNext()) {
	    	String recipient = it.next();
	    	transferTest(submitter, someAmount, purpose, recipient);
	    	submitter = recipient;
    	}
    	transferTest(submitter, someAmount, purpose, names.iterator().next());
    	
    	String[] sortedNames = transactor.sortedNames();
		assertTrue("Some name missing.", Arrays.asList(sortedNames).containsAll(Arrays.asList(participants)));
    	
		Integer[] ids = transactor.getEntryIds(String.format("comment='%s'", purpose));
    	for (Integer id : ids) 
    		discardTest(id);
    	
    	assertEquals("discard failed.", 0, transactor.getCount(null));
    }
    
    void transferTest(String submitter, double amount, String purpose, String recipient) {
    	int entryId = transactor.performTransfer(submitter, amount, purpose, recipient);
    	entrySizeTest(2, entryId);
    	zeroSumTest(entryId, "");
 	}
    
    void discardTest(int entryId) {
    	int discarded = transactor.performDiscard(entryId);
    	assertTrue("Nothing deleted.", discarded > 0);
    	entrySizeTest(0, entryId);
 	}
    
    int entryTest(int entry, final int size, final ShareMap shares) {
    	assertFalse("Transaction blocked because of negative total.", entry == -2);
    	assertTrue("Invalid entry.", entry > -1);
    	
    	transactor.fetchEntry(entry, 
    		new Transactor.QueryEvaluator<Void>() {
				public Void evaluate(Cursor cursor, Void defaultResult, Object... params) {
			    	assertNotNull(cursor);
			    	if (shares == null) 
						assertEquals(size, cursor.getCount());
			    	else {
						assertEquals(size + shares.rawMap.size(), cursor.getCount());
						for (String name : shares.rawMap.keySet()) {
							double share = shares.rawMap.get(name);

							if (cursor.moveToFirst()) do {
								if (cursor.getString(transactor.columnIndex("timestamp")) == null
										&& name.equals(cursor.getString(transactor.columnIndex("name")))) {
									double amount = cursor.getDouble(transactor.columnIndex("amount"));
									assertAmountEquals("Allocation violated.", share, amount);
								}
							} while (cursor.moveToNext());

							cursor.moveToFirst();
						}
					}
					return defaultResult;
				}
			}, null);
    	
    	if (shares != null) 
    		zeroSumTest(entry, " and expense > 0");
    	
		return entry;
    }
    
    int submissionTest(String submitter, double amount, String purpose) {
    	double total = transactor.total();
    	int entry = transactor.performSubmission(submitter, amount, purpose);
    	
    	entryTest(entry, 1, null);
    	
    	assertAmountEquals("Wrong result after submission.", total + amount, transactor.total());
    	
    	return entry;
    }
    
    int payoutTest(String[] recipients, double amount, String purpose, Double... portions) {
    	double total = transactor.total();
		ShareMap shares = new ShareMap(recipients, amount, portions);
    	int entry = transactor.performMultiple(shares.negated(), purpose);
    	
    	entryTest(entry, 0, shares);
    	
    	assertAmountEquals("Wrong result after multiple submissions.", total + shares.sum(), transactor.total());
    	
    	return entry;
    }
    
    int simpleExpenseTest(String submitter, double amount, String purpose, Double... portions) {
		ShareMap shares = new ShareMap(participants, amount, portions);
    	int entry = transactor.performExpense(submitter, purpose, shares);
    	return entryTest(entry, 1, shares);
    }
    
    int expenseTest(String submitter, double amount, String purpose, String policy) {
		ShareMap shares = new ShareMap(participants, amount, policy);
    	int entryId = transactor.performExpense(submitter, purpose, shares);
    	return entryTest(entryId, 1, shares);
    }
    
    int complexExpenseTest(String[] names, Double[] amounts, String purpose, Double... portions) {
		ShareMap deals = new ShareMap(names, amounts);
		ShareMap shares = new ShareMap(participants, deals.sum(), portions);
    	int entry = transactor.performComplexExpense(deals, purpose, shares);
    	return entryTest(entry, deals.rawMap.size() + (deals.rawMap.containsKey("") ? 1 : 0), shares);
    }
    
    void sharingTest(double expectedExpenses, String policy) {
    	double costs = transactor.expenses();
		assertEquals("costs are wrong.", expectedExpenses, costs, Util.minAmount);
		ShareMap shares = new ShareMap(participants, costs, policy);
		assertAmountEquals("Sharing sucks.", costs, shares.negated().sum());
    }
    
    int compensationTest(String sharingPolicy, Object... expectedValues) {
		transactor.setSharingPolicy(sharingPolicy);
    	ShareMap compensations = transactor.compensations();
    	assertShareMap(compensations, expectedValues);
    	
    	int entry = transactor.performMultiple(compensations, transactor.getSharingPolicy() + " sharing compensation");
    	entryTest(entry, 0, compensations);
    	totalTest();
    	return entry;
    }
    
    void totalTest(double... expected) {
    	double total = transactor.total();
    	if (expected.length > 0)
    		assertEquals("unexpected total : " + Helper.formatAmount(total), expected[0], total, Util.minAmount);
    	else
    		assertAmountZero("Total is wrong.", total);
    }
    
    public String[] entries = null;
    
    void scenarioTest(String suffix, int count) {
    	this.suffix = suffix;
    	this.entries = transactor.getEntryStrings("", transactor.tableName(null));
    	assertEquals(count, this.entries.length);
    	String[] comments = transactor.sortedComments();
    	assertEquals(count, comments.length);
    }
   
    public void test_Scenario1() {
    	//	cost sharing on a trip
    	submissionTest("Tom", 50, "stake");
    	simpleExpenseTest("Bob", 100, "gas");
    	simpleExpenseTest("Sue", 70, "groceries");
    	
    	//	Kassensturz !
    	totalTest(50);
		//	total of expenses
		sharingTest(170, "");
		
		assertShareMap(transactor.balances(), 43.33, 13.33, -6.67);
    	//	Tom transfers some money to Sue to improve his balance
    	transferTest("Tom", 10, "better balance", "Sue");
   	
    	compensationTest(null, -43.33, -3.33, -3.33);
    	
    	scenarioTest("Scenario1", 5);
 	}
    
    public void test_Scenario2() {
    	//	cost sharing on a trip ... more complex
    	submissionTest("Tom", 60, "stake");
    	//	Bob pays 100 for gas and uses 50 from the kitty
    	complexExpenseTest(new String[] {"","Bob"}, new Double[] {50.,50.}, "gas");
    	simpleExpenseTest("Sue", 70, "groceries");
    	
    	//	Kassensturz !
    	totalTest(10);
		//	total of expenses
		sharingTest(170, "");
   	
    	compensationTest(null, 6.67, -13.33, -3.33);
    	
    	scenarioTest("Scenario2", 4);
 	}
    
    public void test_Scenario3() {
    	//	non-uniform expenses
    	submissionTest(participants[2], 50, "stake");
    	expenseTest(participants[0], 70, "groceries", "3:0:4");
    	expenseTest(participants[0], 100, "gas", "3:7:0");
    	
    	totalTest(50);
		sharingTest(170, "");
   	
    	compensationTest(null, 70., -110., -10.);
    	
    	scenarioTest("Scenario3", 4);
 	}
    
    public void test_Scenario4() {
    	//	same as Scenario1 but with a sharing policy
    	submissionTest("Tom", 50, "stake");
    	simpleExpenseTest("Bob", 100, "gas");
    	simpleExpenseTest("Sue", 70, "groceries");
    	
    	totalTest(50);
		sharingTest(170, "");
    	transferTest("Tom", 10, "better balance", "Sue");
   	
    	int entry = compensationTest("1:2:2", -66., 8., 8.);
    	transactor.performDiscard(entry);
    	
    	entry = compensationTest("1:0:1", -15., -60., 25.);
    	
    	scenarioTest("Scenario4", 5);
 	}
    
}
