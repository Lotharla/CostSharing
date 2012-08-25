package com.applang.db;

import java.util.*;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.applang.share.*;

import static com.applang.share.Util.*;

/**
 *	A class containing transactions and evaluation tools for a cost sharing system
 * 
 * @author lotharla
 */
public class Transactor extends DbAdapter implements java.io.Serializable
{
	private static final long serialVersionUID = 1619400649251233944L;

	public Transactor(Context context, Object... params) {
		super(context, params);
		
		if (!tableExists(table1))
			super.recreateTables(getDb());
	}

	public Transactor(Object... params) {
		this(null, params);
	}
    
	private static final String TAG = Transactor.class.getSimpleName();
	
	private double debtLimit = minAmount;
	
	private boolean breaksDebtLimit(double amount) {
    	double total = total();
		if (total - amount < -debtLimit) {
			Log.w(TAG, String.format("retrieval of %f is more than allowed : %f", amount, total));
			return true;
		}
		else
			return false;
    }
    
    /** @hide */ public double getDebtLimit() {
		return debtLimit;
	}

    /** @hide */ public void setDebtLimit(double debtLimit) {
		this.debtLimit = debtLimit;
	}

	private boolean isInternal(String name) {
    	return isKitty.apply(name);
    }
    
    private String notInternalClause() {
    	return "name != " + enclose("'", kitty);
    }
	/**
	 * The transaction registers the negated sum of the shares with the submitter.
	 * Additionally all the shares in the map are registered with the same entry id.
	 * This transaction does not change the total. It registers an allocation of a certain amount.
	 * If submitter is an empty <code>String</code> it means there is an internal reallocation which is not an expense.
	 * Such an internal reallocation is not allowed if the transaction causes a negative total.
	 * @param submitter	the name of the participant who expended an amount matching the negated sum of the shares
	 * @param comment	a <code>String</code> to make the transaction recognizable
	 * @param shares	a <code>ShareMap</code> containing the shares of participants involved
	 * @return	the entry id of the transaction or -1 in case the transaction failed or -2 if the transaction violates the 'no negative total' rule
	 */
	public int performExpense(String submitter, String comment, ShareMap shares) {
		double amount = -shares.sum();
		
		boolean internal = isInternal(submitter);
		if (internal) {
			if (breaksDebtLimit(-amount)) 
				return -2;
		}
		
		int entryId = addEntry(submitter, amount, currency, comment, !internal);
		if (entryId < 0)
			return -1;
		
    	if (allocate(!internal, entryId, shares.rawMap)) {
			String action = internal ? "reallocation of" : submitter + " expended";
    		Log.i(TAG, String.format("entry %d: %s %f %s for '%s' shared by %s", 
    				entryId, action, Math.abs(amount), currency, comment, shares.toString()));
    	}
    	else {
    		removeEntry(entryId);
    		entryId = -1;
    	}
    	
		return entryId;
	}
    /**
	 * The transaction registers the negated sum of the shares as an expense by a group of contributors.
	 * Additionally all the shares in the map are registered with the same entry id.
	 * Those names in the deals map that are empty <code>String</code> trigger transfers of internal amounts.
	 * Such an internal transfer is not allowed if the transaction causes a negative total.
     * @param deals	the names of the contributors and their contributions who expended an amount matching the negated sum of the deals
	 * @param comment	a <code>String</code> to make the transaction recognizable
	 * @param shares	a <code>ShareMap</code> containing the shares of participants involved
	 * @return	the entry id of the transaction or a negative value in case the transaction failed
     */
	public int performComplexExpense(ShareMap deals, String comment, ShareMap shares) {
    	int entryId = -1;
    	
    	double amount = deals.sum();
    	if (Math.abs(amount + shares.sum()) < delta) {
    		if (deals.option != null)
    			switch (deals.option) {
				default:
					setDebtLimit(1000. * deals.option);
					break;
				}
    		
    		if (deals.rawMap.containsKey(kitty))
				entryId = performTransfer(kitty, -deals.rawMap.get(kitty), comment, kitty);
    		else
				entryId = getNewEntryId();
    		
    		Collection<String> dealers = filter(deals.rawMap.keySet(), true, isKitty);
    		
	    	for (String name : dealers) {
				long rowId = addRecord(entryId, 
						name, deals.rawMap.get(name), currency, Helper.timestampNow(), comment, true);
				if (rowId < 0) {
		    		entryId = -1;
		    		break;
				}
			}
    	}
    	else
    		Log.w(TAG, String.format("the sum of the deals (%f) for '%s' doesn't match the sum of the shares (%f)", 
    				amount, comment, shares.sum()));
    	
    	if (entryId > -1) {
        	if (allocate(true, entryId, shares.rawMap)) {
        		Log.i(TAG, String.format("entry %d: %s expended %f %s for '%s' shared by %s", 
        				entryId, deals.toString(), Math.abs(amount), currency, comment, shares.toString()));
        	}
        	else {
        		removeEntry(entryId);
        		entryId = -1;
        	}
    	}
  	
    	if (entryId < 0)
    		removeEntry(entryId);
    	
		return entryId;
    }
	/**
	 * The transaction performs a transfer of the amount from a submitter to a recipient.
	 * This is the same as performing a contribution of the submitter and then a payout to the recipient.
	 * @param sender	the name of the participant who lost the amount
	 * @param amount	the amount of the transfer
	 * @param comment	a <code>String</code> to make the transaction recognizable
	 * @param recipient	the name of the participant who got the amount
	 * @return	the entry id of the transaction or -1 in case the transaction failed or -2 if the transaction violates the 'no negative total' rule
	 */
	public int performTransfer(String sender, double amount, String comment, String recipient) {
		boolean internal = isInternal(sender);
		if (internal) {
			if (breaksDebtLimit(amount)) 
				return -2;
		}
		
		int entryId = addEntry(sender, amount, currency, comment, false);
		if (entryId < 0)
			return -1;
		
		boolean expense = sender.equals(recipient);
		long rowId = addRecord(entryId, recipient, -amount, currency, Helper.timestampNow(), comment, expense);
		if (rowId < 0){
    		removeEntry(entryId);
			return -1;
		}

		Log.i(TAG, String.format("entry %d: %s transfer %f %s for '%s' to %s", entryId, 
				sender, 
				amount, currency, comment, 
				recipient));
		
		return entryId;
	}
	/**
	 * The transaction registers the amount as a contribution (positive) or a retrieval (negative).
	 * A retrieval is not allowed if it surmounts the current total.
	 * @param submitter	the name of the participant who did the submission
	 * @param amount	the amount of the submission
	 * @param comment	a <code>String</code> to make the transaction recognizable
	 * @return	the entry id of the transaction or -1 in case the transaction failed or -2 if the transaction violates the 'no negative total' rule
	 */
	public int performSubmission(String submitter, double amount, String comment) {
		int entryId = addEntry(submitter, amount, currency, comment, false);
		
		if (entryId > -1) {
			if (breaksDebtLimit(-amount)) {
	    		removeEntry(entryId);
				return -2;
			}
			
			String action = amount > 0 ? "contributed" : "retrieved";
			Log.i(TAG, String.format("entry %d: %s %s %f %s as '%s'", 
					entryId, submitter, action, Math.abs(amount), currency, comment));
		}
		
		return entryId;
	}
	/**
	 * The transaction registers multiple submissions.
	 * This transaction as a whole is not allowed if it causes a negative total.
	 * @param deals	a <code>ShareMap</code> containing the deals of participants involved
	 * @param comment	a <code>String</code> to make the transactions recognizable
	 * @return	the entry id of the transaction or -1 in case the transaction failed or -2 if the transactions in the sum violate the 'no negative total' rule
	 */
	public int performMultiple(ShareMap deals, String comment) {
		if (breaksDebtLimit(-deals.sum())) 
			return -2;
		
    	int entryId = getNewEntryId();
    	
		if (entryId > -1) {
			for (Map.Entry<String, Double> share : deals.rawMap.entrySet())
				if (addRecord(entryId, share.getKey(), share.getValue(), currency, Helper.timestampNow(), comment, false) < 0) {
		    		removeEntry(entryId);
					return -1;
				}
			
			Log.i(TAG, String.format("entry %d: '%s' submissions : %s",
					entryId, comment, deals.toString()));
		}
		
		return entryId;
	}
	/**
	 * The transaction discards any record with that entry id.
	 * @param entryId	the entry to be discarded
	 * @return	the number of affected records
	 */
    public int performDiscard(int entryId) {
		int affected = removeEntry(entryId);
		Log.i(TAG, String.format("entry %d: discarded, %d records deleted", entryId, affected));
		return affected;
    }
    /**
     * summarizes the cash flows caused by the participants
	 * @return	a sorted map containing the names as key and the cash flow of that participant as value
     */
    public ShareMap cashFlow() {
		return rawQuery("select name, sum(amount) as balance from " + table1 + 
				" where " + notInternalClause() + " and timestamp not null group by name order by name", null, 
			new QueryEvaluator<ShareMap>() {
				public ShareMap evaluate(Cursor cursor, ShareMap defaultResult, Object... params) {
					ShareMap sm = new ShareMap();
					
		    		if (cursor.moveToFirst()) do {
		    			sm.rawMap.put(cursor.getString(0), cursor.getDouble(1));
		    		} while (cursor.moveToNext());
		    		
		        	Log.i(TAG, String.format("cash flow : %s", sm.toString()));
					return sm;
				}
			}, null);
    }
    /**
     * calculates the balances of all participants identified by their names
	 * @return	a sorted map containing the names as keys and the balances as values
     */
    public ShareMap balances() {
		return rawQuery("select name, sum(amount) as balance from " + table1 + 
				" where " + notInternalClause() + " group by name order by name", null, 
			new QueryEvaluator<ShareMap>() {
				public ShareMap evaluate(Cursor cursor, ShareMap defaultResult, Object... params) {
					ShareMap sm = new ShareMap();
					
					if (cursor.moveToFirst()) do {
		    			sm.rawMap.put(cursor.getString(0), cursor.getDouble(1));
		    		} while (cursor.moveToNext());
		    		
		        	Log.i(TAG, String.format("balances : %s", sm.toString()));
					return sm;
				}
			}, null);
    }
    /**
     * calculates the current over-all amount
     * @return	the sum over the amounts of all records in the table
     */
    public double total() {
    	return getSum(null);
    }
    /**
     * calculates the accumulated costs
     * @return	the sum over all entries marked as 'expense'
     */
    public double expenses() {
    	return getSum("expense > 0 and timestamp not null");
    }

	private static String sharingPolicy = "";    //	uniform sharing among all participants
	/**
	 * A sharing policy depicts the way the volume of all the cash flows is to be shared among the participants 
	 * when it comes to calculating compensations (x:y:z ...). The given integers are proportional weights for those participants 
	 * who are involved in the compensation procedure. They are assigned according to the sorted list of names.
	 * @return	an array containing integer values or empty meaning the default sharing policy (uniform sharing among all participants)
	 */
	public static String getSharingPolicy() {
		return Transactor.sharingPolicy;
	}
	/**
	 * sets the sharing policy.
	 * Note that a call of <code>clear</code> or <code>clearAll</code> 
	 * resets the sharing policy to its default (uniform sharing among all participants).
	 * @param sharingPolicy
	 */
	public static void setSharingPolicy(String sharingPolicy) {
		Transactor.sharingPolicy = sharingPolicy;
	}
	/**
	 * calculates a compensation for each participant from the balance and the all-over cash flow at this point.
	 * The sharing policy is taken into account for this calculation.
	 * An example: A sharing policy 1:2:3 splits the volume of all the cash flows minus the current total
	 * which is the remaining amount among the first, second and third participants out of the sorted list of names.
	 * The first portion is 1/6, the second 2/6 and the third is 3/6 of the volume.
	 * @return	a sorted map containing the names as keys and the compensations as values
	 */
	public ShareMap compensations(String sharingPolicy) {
    	String[] sortedNames = sortedNames();
    	ShareMap cashFlow = cashFlow();
    	double volume = cashFlow.sum() - total();
    	
    	ShareMap differences = new ShareMap(sortedNames, volume);
    	ShareMap sharedCosts = new ShareMap(sortedNames, volume, sharingPolicy);
    	differences.minus(sharedCosts.rawMap.values());
    	
    	ShareMap compensations = balances().negated();
    	compensations.minus(differences.negated().rawMap.values());
		return compensations;
	}
    /**
     * @return	a sorted array of all names of the participants
     */
    public String[] sortedNames() {
		return rawQuery("select distinct name from " + table1 + " where " + notInternalClause(), null, 
			new QueryEvaluator<String[]>() {
				public String[] evaluate(Cursor cursor, String[] defaultResult, Object... params) {
			    	TreeSet<String> names = new TreeSet<String>();
			    	if (cursor.moveToFirst()) do {
		    			names.add(cursor.getString(0));
		    		} while (cursor.moveToNext());
					return names.toArray(defaultResult);
				}
			}, new String[0]);
    }
    /**
     * @return	a sorted array of distinct comments
     */
    public String[] sortedComments() {
		return rawQuery("select distinct comment from " + table1 + " where length(comment) > 0", null, 
			new QueryEvaluator<String[]>() {
				public String[] evaluate(Cursor cursor, String[] defaultResult, Object... params) {
			    	TreeSet<String> comments = new TreeSet<String>();
			    	if (cursor.moveToFirst()) do {
		    			comments.add(cursor.getString(0));
		    		} while (cursor.moveToNext());
					return comments.toArray(defaultResult);
				}
			}, new String[0]);
    }
    /**
     * @param clause	the condition constraining the query through a where clause
     * @return	a sorted array of entries identified by their id
     */
    public Integer[] getEntryIds(String clause) {
		return rawQuery("select entry from " + table1 + 
				(notNullOrEmpty(clause) ? " where " + clause : ""), null, 
			new QueryEvaluator<Integer[]>() {
				public Integer[] evaluate(Cursor cursor, Integer[] defaultResult, Object... params) {
			    	TreeSet<Integer> ids = new TreeSet<Integer>();
			    	if (cursor.moveToFirst()) do {
		       			ids.add(cursor.getInt(0));
		       		} while (cursor.moveToNext());
					return ids.toArray(defaultResult);
				}
			}, new Integer[0]);
    }
    /**
     * @param clause	the condition constraining the query through a where clause
     * @return	the textual representations of all the entries to the active database table
     */
    public String[] getEntryStrings(String clause, final String tableName) {
		return rawQuery("select distinct entry from " + tableName + 
				(notNullOrEmpty(clause) ? " where " + clause : ""), null, 
			new QueryEvaluator<String[]>() {
				public String[] evaluate(Cursor cursor, String[] defaultResult, Object... params) {
			    	ArrayList<String> strings = new ArrayList<String>();
			    	if (cursor.moveToFirst()) do {
		       			strings.add(getEntryString(cursor.getInt(0), tableName));
		       		} while (cursor.moveToNext());
					return strings.toArray(defaultResult);
				}
			}, new String[0]);
    }
    /**
     * @param entryId	the integer value identifying the entry in question
     * @return	textual representation of the entry to the active database table
     */
    public String getEntryString(int entryId, String tableName) {
		return rawQuery("select * from " + tableName + " where entry=" + entryId, null, 
			new QueryEvaluator<String>() {
				public String evaluate(Cursor cursor, String defaultResult, Object... params) {
					String s = "";
			    	if (cursor.moveToFirst()) {
			    		s += String.format("Entry %d on %s comment '%s'", cursor.getInt(0), cursor.getString(4), cursor.getString(5));
			    		do {
			    			String assoc = ShareMap.association(
			    					cursor.getString(1), 
			    					Helper.formatAmount(cursor.getDouble(2)));
			    			s += " : " + assoc;
			       		} while (cursor.moveToNext());
			    	}
					return s;
				}
			}, "");
    }
    /**
     * @param clause	a condition constraining the query through a where clause
     * @return	a sorted array of records identified by their row id
     */
    public Long[] getRowIds(String clause) {
		return rawQuery("select rowid from " + table1 + 
				(notNullOrEmpty(clause) ? " where " + clause : ""), null, 
			new QueryEvaluator<Long[]>() {
				public Long[] evaluate(Cursor cursor, Long[] defaultResult, Object... params) {
			    	TreeSet<Long> ids = new TreeSet<Long>();
			    	if (cursor.moveToFirst()) do {
		       			ids.add(cursor.getLong(0));
		       		} while (cursor.moveToNext());
					return ids.toArray(defaultResult);
				}
			}, new Long[0]);
    }
    /**
     * @param clause	a condition constraining the query through a where clause
     * @return	a aggregated sum of the amount values in the queried records
     */
    public double getSum(String clause) {
		return rawQuery("select sum(amount) from " + table1 + 
				(notNullOrEmpty(clause) ? " where " + clause : ""), null, 
			new QueryEvaluator<Double>() {
				public Double evaluate(Cursor cursor, Double defaultResult, Object... params) {
					if (cursor.moveToFirst()) 
						return cursor.getDouble(0);
					else
						return defaultResult;
				}
			}, 0.);
	}
    /**
     * @param clause	a condition constraining the query through a where clause
     * @return	the count of the queried records
     */
    public int getCount(String clause) {
		return rawQuery("select count(*) from " + table1 + 
				(notNullOrEmpty(clause) ? " where " + clause : ""), null, 
			new QueryEvaluator<Integer>() {
				public Integer evaluate(Cursor cursor, Integer defaultResult, Object... params) {
					if (cursor.moveToFirst()) 
						return cursor.getInt(0);
					else
						return defaultResult;
				}
			}, 0);
    }
    /**
     * determines whether the table with the name exists in the database
     * @param name	name of the table
     * @return	the table does not exist if false
     */
    public Boolean tableExists(String name) {
		return rawQuery("select name from sqlite_master where type = 'table'", null, 
			new QueryEvaluator<Boolean>() {
				public Boolean evaluate(Cursor cursor, Boolean defaultResult, Object... params) {
			    	String name = (String)params[0];
			    	Boolean result = defaultResult;
			    	
			    	if (cursor.moveToFirst()) do {
		        		if (name.compareTo(cursor.getString(0)) == 0) {
		        			result = true;
		        			break;
		        		}
		    		} while (cursor.moveToNext());
					
			    	return result;
				}
			}, false, name);
    }
    /**
     * retrieves the names of tables that had been 'saved' in the past
     * @return	the <code>Set</code> of saved table names
     */
    public Set<String> savedTables() {
		return rawQuery("select name from sqlite_master where type = 'table'", null, 
			new QueryEvaluator<Set<String>>() {
				public Set<String> evaluate(Cursor cursor, Set<String> defaultResult, Object... params) {
			    	TreeSet<String> names = new TreeSet<String>();
		    		
			    	if (cursor.moveToFirst()) do {
		        		String name = cursor.getString(0);
		        		if (name.startsWith(tableName("")))
		        			names.add(name);
		    		} while (cursor.moveToNext());
					
			    	Log.i(TAG, String.format("saved tables : %s", names.toString()));
			    	return names;
				}
			}, null);
    }
    /**
     * 
     * @param suffix	an arbitrary <code>String</code> which is legal as an SQLite table name
     * @return	the complete SQLite table name
     */
    public String tableName(String suffix) {
    	return tableName(table1, suffix);
    }
    
    protected String tableName(String prefix, String suffix) {
    	if (suffix == null)
    		return prefix;
    	else
    		return prefix + "_" + suffix;
    }
    /**
     * changes the name of the table that has been worked on with transactions (current table). 
     * Thus this table is 'saved' in the same database. Note that the current table is non-existing after this operation. 
     * In order to continue the current table has to be restored (loadFrom) or recreated (clear).
     * @param newSuffix	the <code>String</code> to append to the name of the current table in order to form the new table name 
     * @return	success if true
     */
    public boolean saveAs(String newSuffix) {
    	if (newSuffix == null || newSuffix.length() < 1)
    		return false;
    	
    	String newTableName = tableName(newSuffix);
    	if (savedTables().contains(newTableName))
    		return false;
    		
    	rename(table1, newTableName);
    	
    	String oldTableName = tableList.get(1);
    	if (tableExists(oldTableName)) {
    		newTableName = tableName(oldTableName, newSuffix);
        	rename(oldTableName, newTableName);
    	}
    	
		Log.i(TAG, String.format("table saved as '%s'", newTableName));
    	return true;
    }
    /**
     * restores one of the saved tables as the current table. Note that the table that was current up to this point will be dropped.
     * Also there will be one less 'saved' table after this operation.
     * @param oldSuffix	the <code>String</code> to append to the name of the current table in order to form the old table name 
     * @return	success if true
     */
    public boolean loadFrom(String oldSuffix) {
    	if (oldSuffix == null || oldSuffix.length() < 1)
    		return false;
    	
    	String oldTableName = tableName(oldSuffix);
    	if (!savedTables().contains(oldTableName))
    		return false;
    		
    	rename(oldTableName, table1);
    	
    	String newTableName = tableList.get(1);
    	oldTableName = tableName(newTableName, oldSuffix);
    	if (tableExists(oldTableName))
    		rename(oldTableName, newTableName);
    	
		Log.i(TAG, String.format("table loaded from '%s'", oldTableName));
    	return true;
    }
    /**
     * deletes all records from the current table and recreates it
     */
    @Override
    public void clear() {
    	int count = tableExists(table1) ? getCount(null) : 0;
    	super.clear();
		Log.i(TAG, String.format("table '%s' cleared, %d records deleted", table1, count));
		setSharingPolicy(null);
    }
    /**
     * clears the current table and drops all saved tables
     */
    public void clearAll() {
    	for (String table : savedTables())
    		drop(table);
    	
    	super.clear();
		Log.i(TAG, String.format("table '%s' cleared, all saved tables dropped", table1));
		setSharingPolicy(null);
    }
	
	private String currency = "";

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}
    
    /** @hide */ public int columnIndex(String key) {
       return Arrays.asList(getFieldNames()).indexOf(key);
    }

	/** @hide */ public boolean isExpense(long rowId) {
		return rawQuery("select expense from " + table1 + " where ROWID=" + rowId, null, 
			new QueryEvaluator<Boolean>() {
				public Boolean evaluate(Cursor cursor, Boolean defaultResult, Object... params) {
					return cursor.moveToFirst() && cursor.getInt(0) > 0;
				}
			}, false);
	}
	
	/** @hide */ public int getNewEntryId() {
		return 1 + rawQuery("select max(entry) from " + table1, null, 
			new QueryEvaluator<Integer>() {
				public Integer evaluate(Cursor cursor, Integer defaultResult, Object... params) {
					if (cursor.moveToFirst()) 
						return cursor.getInt(0);
					else
						return defaultResult;
				}
			}, -1);
    }
   
    /** @hide */ public int addEntry(String name, double amount, String currency, String comment, boolean expense) {
    	int entryId = getNewEntryId();
        
        long rowId = addRecord(entryId, name, amount, currency, Helper.timestampNow(), comment, expense);
        if (rowId < 0) {
    		removeEntry(entryId);
    		entryId = -1;
        }

    	return entryId;
    }
    
	/** @hide */ public String fetchField(int entryId, final String name) {
    	return fetchEntry(entryId, 
			new QueryEvaluator<String>() {
				public String evaluate(Cursor cursor, String defaultResult, Object... params) {
					int timestampIndex = columnIndex("timestamp");
					if (cursor.moveToFirst()) do {
						if (cursor.getString(timestampIndex) != null)
							return cursor.getString(columnIndex(name));
					} while (cursor.moveToNext());
					return defaultResult;
				}
			}, null);
    }
    
    /** @hide */ public boolean allocate(boolean expense, int entryId, Map<String, Double> portions) {
		for (String name : portions.keySet()) {
			double portion = portions.get(name);

			if (addRecord(entryId, name, portion, null, null, null, expense) < 0)
				return false;
		}
		
		return portions.size() > 0;
    }
	
    /** @hide */ public interface QueryEvaluator<T> 
	{
		public T evaluate(Cursor cursor, T defaultResult, Object... params);
	}
	
    /** @hide */ public <T> T rawQuery(String sql, String[] selectionArgs, QueryEvaluator<T> qe, T defaultResult, Object... params) {
        Cursor cursor = null;
        
		try {
			cursor = rawQuery(sql, selectionArgs);
	        if (cursor != null) 
	        	return qe.evaluate(cursor, defaultResult, params);
		} 
		catch (SQLiteException ex) {
		}
		finally {
			if (cursor != null)
				cursor.close();
		}

		return defaultResult;
	}
    
    /** @hide */ public <T> T fetchEntry(int entryId, QueryEvaluator<T> qe, T defaultResult, Object... params) {
        Cursor cursor = null;
        
		try {
	        cursor = query(getFieldNames(), "entry=" + entryId);
	        if (cursor != null) 
	        	return qe.evaluate(cursor, defaultResult, params);
		} 
		catch (SQLiteException ex) {
		}
		finally {
			if (cursor != null)
				cursor.close();
		}

		return defaultResult;
    }
    
    /** @hide */ public int removeEntry(int entryId) {
    	return delete("entry=" + entryId);
    }
    
    protected ContentValues putValues(Integer entry, String name, Double amount, String currency, String timestamp, String comment, Boolean expense) {
        ContentValues values = new ContentValues();
        
    	String[] fields = getFieldNames();
        if (entry != null) values.put(fields[0], entry);
        if (name != null) values.put(fields[1], name);
        if (amount != null) values.put(fields[2], amount);
        if (currency != null) values.put(fields[3], currency);
        if (timestamp != null) values.put(fields[4], timestamp);
        if (comment != null) values.put(fields[5], comment);
        if (expense != null) values.put(fields[6], expense ? 1 : 0);
        
        return values;
    }
	
    protected long addRecord(int entryId, String name, double amount, String currency, String timestamp, String comment, boolean expense) {
        ContentValues values = putValues(entryId, name, amount, currency, timestamp, comment, expense);
    	long rowId = insert(values);
    	if (rowId < 0)
    		Log.e(TAG, String.format(".addRecord failed with : %s", values.toString()));
    	return rowId;
	}

	protected int updateExpenseFlag(long rowId, boolean expense) {
		return update(rowId, putValues(null, null, null, null, null, null, expense));
	}

    protected String[] getFieldNames() {
    	return new String[] {"entry", "name", "amount", "currency", "timestamp", "comment", "expense", "ROWID"};
    }

    static {
    	tableDefs.put(kitty, 
			"entry integer not null," +		//	unique for the entries, reference for the portions
			"name text not null," +			//	the person involved
			"amount real not null," +		//	the amount of money, negative for portions
			"currency text," +				//	if null it's the default currency (Euro or Dollar or ...)
			"timestamp text," +				//	if null it's a portion
			"comment text," +				//	optional, for recognition
			"expense integer not null"		//	boolean, if true then the amount has been expended and likely shared among others
    	);
    	tableDefs.put("Purposes", 
			"entry integer not null," + 
			"categoryId integer not null," + 
			"foreign key(categoryId) references Categories(categoryId)"
    	);
    	tableDefs.put("Categories", 
			"categoryId integer primary key," + 
			"category text"
    	);
    }
}
