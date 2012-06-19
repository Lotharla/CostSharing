package com.applang.test;

import java.util.*;

import android.database.Cursor;

import com.applang.db.*;
import com.applang.shared.*;

public class CostSharingTest extends ActivityTest 
{
	public CostSharingTest() {
		super();
	}

	public CostSharingTest(String method) {
		super(method);
	}

    private Transactor transactor;

    @Override
    protected void setUp() throws Exception {
        transactor = new Transactor(this.getActivity());
        
        super.setUp();	//	database file exists ?
    	
    	transactor.clear();
    }

	@Override
    protected void tearDown() throws Exception {
		transactor.close();
        super.tearDown();
	}
    
    void assertAmountZero(String message, Double actual) {
    	assertEquals(message, 0, actual, Util.delta);
    }
    
    void assertAmountEquals(Double expected, Double actual) {
    	assertEquals(expected, actual, Util.delta);
    }
    
    void assertAmountEquals(String message, Double expected, Double actual) {
    	assertEquals(message, expected, actual, Util.delta);
//    	String exp = Util.formatAmount(expected);
//    	String act = Util.formatAmount(actual);
//    	assertEquals(message, exp, act);
    }
   
    <T> void assertArrayEquals(String message, T[] expected, T[] actual) {
    	assertEquals(message, expected.length, actual.length);
    	for (int i = 0; i < actual.length; i++) 
        	assertEquals(message, expected[i], actual[i]);
    }

    void assertEntrySize(final int expected, int entry) {
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
					assertEquals("Bob", cursor.getString(1));
					assertEquals(100.5, cursor.getDouble(2));
					assertEquals(null, cursor.getString(3));
					String now = Util.timestampNow();
					assertEquals(now.substring(0, 16), 
							cursor.getString(4).substring(0, 16));
					assertEquals("gas", cursor.getString(5));
			    	assertTrue(cursor.getInt(6) > 0);
					return defaultResult;
				}
			}, null);    	
		
		assertEquals(1, transactor.removeEntry(entryId));
		assertEntrySize(0, entryId);
	}
    
    String[] participants = new String[] {"Sue", "Bob", "Tom"};
    
    public void testShareMap() {
    	ShareMap map = new ShareMap(participants, new Double[] {200.,null,300.});
    	assertShareMap(map, 200., 300.);
    	
    	map = new ShareMap(participants, 600.);
    	assertShareMap(map, -200., -200., -200.);
    	
    	map = new ShareMap(participants, 600., null, 300.);
    	assertShareMap(map, -300., -100., -200.);
    	assertEquals("Bob", map.firstKey());
    	
    	assertAmountEquals(-600., map.sum());
    	assertAmountEquals(map.sum(), -map.negated().sum());
    	
    	map = new ShareMap(new String[] {participants[0], "", participants[2]}, 600.);
    	assertEquals("", map.firstKey());
    	assertShareMap(map, -200., -200., -200.);
    	
    	map = new ShareMap(participants, 120., "1:1:1");
    	assertShareMap(map, -40., -40., -40.);
    	
    	map = new ShareMap(participants, 120., "0:1:2");
    	assertShareMap(map, -40., 0., -80.);
    	
    	map = new ShareMap(participants, 150., "");
    	assertShareMap(map, -50., -50., -50.);
    	
    	map = new ShareMap(null, 150., "1:2:3");
    	assertShareMap(map, -25., -50., -75.);
    	
    	map = new ShareMap();
    	map.reorganize(120., "");
    	assertShareMap(map, map.placeholder(), -120.);
    	map.reorganize(120., "3:1:2");
    	assertShareMap(map, map.placeholder(1), -60., map.placeholder(2), -20., map.placeholder(3), -40.);
    	map.reorganize(120., "1*3");
    	assertShareMap(map, map.placeholder(1, 1), -40., map.placeholder(1, 2), -40., map.placeholder(1, 3), -40.);
    	map.reorganize(120., "0:1*2:4");
    	assertShareMap(map, map.placeholder(1), 0., map.placeholder(2, 1), -20., map.placeholder(2, 2), -20., map.placeholder(3), -80.);
    	map.renameWith(participants);
    	assertShareMap(map, "Bob", -20., "Sue", 0., "Tom", -20., map.placeholder(3), -80.);
    }
    
    void assertShareMap(ShareMap actual, Object... expected) {
    	System.out.println(actual.toString()); 			/* ascending order of keys by default */
    	
    	int len = expected.length;
    	boolean mapEntry = len > 1 && expected[0] instanceof String && expected[1] instanceof Double;
    	assertEquals("unexpected map size.", len, (mapEntry ? 2 : 1) * actual.size());
    	if (len < 1) 
    		return;
    	
    	if (mapEntry) {
	    	Iterator<Map.Entry<String, Double>> it = actual.entrySet().iterator();
	    	for (int i = 0; i < len; i+=2) {
	    		Map.Entry<String, Double> entry = it.next();
	    		assertEquals(expected[i].toString(), entry.getKey());
	    		assertEquals((Double)expected[i + 1], entry.getValue(), Transactor.minAmount);
	    	}
    	}
    	else if (expected[0] instanceof Double) {
	    	Iterator<Double> it = actual.values().iterator();
	    	for (int i = 0; i < len; i++) 
	    		assertEquals((Double)expected[i], it.next(), Transactor.minAmount);
    	}
    	else if (expected[0] instanceof String) {
	    	Iterator<String> it = actual.keySet().iterator();
	    	for (int i = 0; i < len; i++) 
	    		assertEquals(expected[i].toString(), it.next());
    	}
    	else
    		throw new IllegalArgumentException();
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
    	
    	assertTrue(transactor.allocate(expense, entryId, portions));
    	
    	assertEntrySize(4, entryId);
    	zeroSumTest(entryId, "");
		
		assertNotNull("timestamp missing", transactor.fetchField(entryId, "timestamp"));
		
		transactor.fetchEntry(entryId, 
    		new Transactor.QueryEvaluator<Void>() {
				public Void evaluate(Cursor cursor, Void defaultResult, Object... params) {
			    	assertNotNull(cursor);
			    	do {
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
				assertEntrySize(0, id);
	    	}
    	
    	assertTrue(-1 < transactor.performSubmission(participants[0], 99., "test1"));
    	assertEquals(-2, transactor.performExpense("", "test1", new ShareMap(participants, -100.)));
    	
    	int id = transactor.performExpense("", "test1", new ShareMap(participants, -99.));
    	assertEntrySize(4, id);
    	zeroSumTest(id, "");
    	
    	totalTest(99.);
    	
    	id = transactor.performExpense(participants[2], "test2", new ShareMap(participants, 70., "3:0:4"));
    	assertEntrySize(4, id);
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
    	String tableName = transactor.tableName(suffix);
    	transactor.drop(tableName);
    	assertTrue(transactor.saveAs(suffix));
    	assertTrue(transactor.savedTables().contains(tableName));
    }
    
    void loadTest(String suffix) {
    	String tableName = transactor.tableName(suffix);
    	assertTrue(transactor.savedTables().contains(tableName));
    	transactor.drop(transactor.table1);
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
    	assertEntrySize(2, entryId);
    	zeroSumTest(entryId, "");
 	}
    
    void discardTest(int entryId) {
    	int discarded = transactor.performDiscard(entryId);
    	assertTrue("Nothing deleted.", discarded > 0);
    	assertEntrySize(0, entryId);
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
						assertEquals(size + shares.size(), cursor.getCount());
						for (String name : shares.keySet()) {
							double share = shares.get(name);

							do {
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
    	return entryTest(entry, deals.size() + (deals.containsKey("") ? 1 : 0), shares);
    }
    
    void sharingTest(double expectedExpenses, String policy) {
    	double costs = transactor.expenses();
		assertEquals("costs are wrong.", expectedExpenses, costs, Transactor.minAmount);
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
    		assertEquals("unexpected total : " + Util.formatAmount(total), expected[0], total, Transactor.minAmount);
    	else
    		assertAmountZero("Total is wrong.", total);
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
    	
    	saveTest("Scenario1");
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
    	
    	saveTest("Scenario2");
 	}
    
    public void test_Scenario3() {
    	//	non-uniform expenses
    	submissionTest(participants[2], 50, "stake");
    	expenseTest(participants[0], 70, "groceries", "3:0:4");
    	expenseTest(participants[0], 100, "gas", "3:7:0");
    	
    	totalTest(50);
		sharingTest(170, "");
   	
    	compensationTest(null, 70., -110., -10.);
    	
    	saveTest("Scenario3");
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
    	
    	saveTest("Scenario4");
 	}
    
}
