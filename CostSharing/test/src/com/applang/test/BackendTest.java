package com.applang.test;

import java.util.*;

import android.database.Cursor;

import com.applang.CostSharingActivity;
import com.applang.db.*;
import com.applang.provider.Transaction;
import com.applang.share.*;

import static com.applang.test.AssertHelper.*;
import static com.applang.share.ShareUtil.*;

public class BackendTest extends ActivityTest<CostSharingActivity> 
{
	public BackendTest() {
		super("com.applang", CostSharingActivity.class);
	}

/*	public BackendTest(String method) {
		super(method);
	}
*/
	public Transactor transactor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();	//	database file exists ?
    	
        transactor = new Transactor(this.getActivity());
    	transactor.clear();
    }

	@Override
    protected void tearDown() throws Exception {
        super.tearDown();
	}

    public void testConnect() {
        Cursor cursor = null;
		try {
			cursor = transactor.rawQuery("select * from sqlite_master;", new String[] {});
		    assertTrue(cursor.getCount() > 0);
			cursor.close();
			
		    cursor = transactor.rawQuery("pragma table_info(" + kitty + ")", null);
		    assertEquals(7, cursor.getCount());
		    String[] columnDefs = Transaction.tableDefs.get(kitty).split(",");
		    assertTrue(cursor.moveToFirst());
		    int i = 0;
            do {
                String columnName = cursor.getString(1);
            	assertTrue(columnDefs[i].startsWith(columnName));
            	i++;
        	} while (cursor.moveToNext());
		}
		catch (Exception e) {
		}
		finally {
			if (cursor != null)
				cursor.close();
		}
	}
    
    String[] participants = new String[] {"Sue", "Bob", "Tom"};
    
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
    	int entryId = transactor.addEntry("Bob", 100.5, null, "gas", transactor.composeFlags(0, true));
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
    
    void zeroSumTest(int entry, Object... params) {
		Double sum = transactor.getSum("entry=" + entry + param("", 0, params));
		assertAmountZero("Transaction leaking.", sum);
    }
    
    int allocationTest(final boolean expense) {
    	final int sign = expense ? 1 : -1;
    	final double allocation = 120 * sign;
    	
    	int entryId = transactor.addEntry(expense ? "Bob" : "", allocation, null, "test", transactor.composeFlags(0, expense));
    	
    	ShareMap portions = new ShareMap(
    			new String[] {"Bob","Sue","Tom"}, 
    			allocation, 
    			null, allocation / 4, allocation / 3);
    	
    	assertTrue(transactor.allocate(transactor.composeFlags(0, expense), entryId, portions.rawMap));
    	
    	entrySizeTest(4, entryId);
    	zeroSumTest(entryId);
		
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
    	assertEquals(-2, transactor.performExpense(kitty, "test1", new ShareMap(participants, -100.)));
    	
    	int id = transactor.performExpense(kitty, "test1", new ShareMap(participants, -99.));
    	entrySizeTest(4, id);
    	zeroSumTest(id);
    	
    	totalTest(99.);
    	
    	id = transactor.performExpense(participants[2], "test2", new ShareMap(participants, 70., "3:0:4"));
    	entrySizeTest(4, id);
    	zeroSumTest(id, " and flags % 10 > 0");
    }
    
    public void testSaveAndLoad() {
    	for (String participant : participants)
    		submissionTest(participant, 50, "stake");
    	
    	Integer count = transactor.getCount();
		assertEquals(new Integer(participants.length), count);
    	Double sum = transactor.getSum();
    	
    	saveTest("test");
    	
    	transactor.clear();
		assertEquals(new Integer(0), transactor.getCount());
		
		loadTest("test");
		
		assertEquals(count, transactor.getCount());
		assertEquals(sum, transactor.getSum());
 	}
    
    void saveTest(String suffix) {
    	if (suffix.length() < 1)
    		return;
    	
    	Set<String> tableSet = transactor.tableSet(suffix);
    	for (String table : tableSet) 
        	transactor.drop(table);

    	assertTrue(transactor.saveAs(suffix));
    	
    	Set<String> tables = transactor.savedTables();
    	System.out.println(tables.toString());
		assertTrue(tables.contains(tableName(suffix)));
    }
    
    void loadTest(String suffix) {
    	assertTrue(transactor.savedTables().contains(tableName(suffix)));
    	
    	Set<String> tableSet = transactor.tableSet();
    	for (String table : tableSet) 
        	transactor.drop(table);

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
    	
    	assertEquals("discard failed.", new Integer(0), transactor.getCount());
    }
    
    int transferTest(String submitter, double amount, String purpose, String recipient) {
    	int entryId = transactor.performTransfer(submitter, amount, purpose, recipient);
    	entrySizeTest(2, entryId);
    	zeroSumTest(entryId);
    	return entryId;
 	}
    
    int discardTest(int entryId) {
    	int discarded = transactor.performDiscard(entryId);
    	assertTrue("Nothing deleted.", discarded > 0);
    	entrySizeTest(0, entryId);
    	return discarded;
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
    		zeroSumTest(entry, " and flags % 10 > 0");
    	
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
    
    int expenseTest(String[] names, Double[] amounts, String purpose, String policy) {
    	if (names.length > 1)
    		return complexExpenseTest(names, amounts, purpose, policy);
    	
		ShareMap shares = new ShareMap(participants, amounts[0], policy);
    	int entryId = transactor.performExpense(names[0], purpose, shares);
    	return entryTest(entryId, 1, shares);
    }
    
    int complexExpenseTest(String[] names, Double[] amounts, String purpose, String policy) {
		ShareMap deals = new ShareMap(names, amounts);
		ShareMap shares = new ShareMap(participants, deals.sum(), policy);
    	int entry = transactor.performComplexExpense(deals, purpose, shares);
    	return entryTest(entry, deals.rawMap.size() + (deals.rawMap.containsKey(kitty) ? 1 : 0), shares);
    }
    
    void sharingTest(double expectedExpenses, String policy) {
    	double costs = transactor.expenses();
		assertEquals("costs are wrong.", expectedExpenses, costs, minAmount);
		ShareMap shares = new ShareMap(participants, costs, policy);
		assertAmountEquals("Sharing sucks.", costs, shares.negated().sum());
    }
    
    int compensationTest(String sharingPolicy, Object... expectedValues) {
		Transactor.setSharingPolicy(sharingPolicy);
    	ShareMap compensations = transactor.compensations(sharingPolicy);
    	assertShareMap(compensations, expectedValues);
    	
    	int entry = transactor.performMultiple(compensations, Transactor.getSharingPolicy() + " sharing compensation");
    	entryTest(entry, 0, compensations);
    	totalTest();
    	return entry;
    }
    
    void totalTest(double... expected) {
    	double total = transactor.total();
    	if (expected.length > 0)
    		assertEquals("unexpected total : " + Helper.formatAmount(total), expected[0], total, minAmount);
    	else
    		assertAmountZero("Total is wrong.", total);
    }
    
    void scenarioTest(String suffix, int count) {
    	String[] entries = transactor.getEntryStrings("", tableName());
    	assertEquals(count, entries.length);
    	String[] comments = transactor.sortedComments();
    	assertEquals(count, comments.length);
		if (entries.length > 0)
			saveTest(suffix);
    }
    
    public void test_Scenario0() {
    	transactor.clearAll();
		Set<String> tables = transactor.savedTables();
		assertEquals(0, tables.size());
    	
    	int entryId = compensationTest(null);
    	assertTrue(entryId > 0);
    	assertEquals("", transactor.getEntryString(entryId, tableName()));
    	
    	scenarioTest("Scenario0", 0);
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
    	complexExpenseTest(new String[] {kitty,"Bob"}, new Double[] {50.,50.}, "gas", "");
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
    	expenseTest(new String[] {participants[0]}, new Double[] {70.}, "groceries", "3:0:4");
    	expenseTest(new String[] {participants[0]}, new Double[] {100.}, "gas", "3:7:0");
    	
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
