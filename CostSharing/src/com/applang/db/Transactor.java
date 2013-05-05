package com.applang.db;

import java.util.*;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.util.Log;

import static com.applang.Util.*;
import static com.applang.provider.Transaction.*;

import com.applang.provider.Transaction;
import com.applang.provider.TransactionProvider;
import com.applang.share.*;

/**
 *	A class containing transactions and evaluation tools for a cost sharing system
 * 
 * @author lotharla
 */
public class Transactor implements java.io.Serializable
{
	private static final long serialVersionUID = 1619400649251233944L;

	public Transactor(Context context, Object... params) {
		contentResolver = context.getContentResolver();
		
		if (!tableExists(kitty))
			_clear();
	}

	public Transactor(Object... params) {
		this(null, params);
	}
    
	private static final String TAG = Transactor.class.getSimpleName();
	
	private double debtLimit = ShareUtil.minAmount;
	
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
    	return kitty.equals(name);
    }
    
    private String notInternalCondition() {
    	return "name != " + enclose("'", kitty);
    }
    
    private String whereClause(String... clause) {
    	return nullOrEmpty(clause) || !notNullOrEmpty(clause[0]) ? "" : " where " + clause[0];
    }
   
    public int composeFlags(int transactionCode, boolean expenseFlag) {
    	return transactionCode * 100 + (expenseFlag ? 1 : 0);
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
		
		int transactionCode = 3;
		int flags = composeFlags(transactionCode, !internal);
		
		int entryId = addEntry(submitter, amount, currency, comment, flags);
		if (entryId < 0)
			return -1;
		
    	if (allocate(flags, entryId, shares.rawMap)) {
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
		
		int transactionCode = 3;
		int flags = composeFlags(transactionCode, true);
    	
    	double amount = deals.sum();
    	if (Math.abs(amount + shares.sum()) < ShareUtil.delta) {
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
    		
    		Collection<String> dealers = ShareUtil.filter(deals.rawMap.keySet(), true, ShareUtil.isKitty);
    		
	    	for (String name : dealers) {
				long rowId = addRecord(entryId, 
						name, deals.rawMap.get(name), currency, Helper.timestampNow(), comment, flags);
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
        	if (allocate(flags, entryId, shares.rawMap)) {
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
		
		int transactionCode = 2;
		int flags = composeFlags(transactionCode, false);
		
		int entryId = addEntry(sender, amount, currency, comment, flags);
		if (entryId < 0)
			return -1;
		
		flags = composeFlags(transactionCode, sender.equals(recipient));
		long rowId = addRecord(entryId, recipient, -amount, currency, Helper.timestampNow(), comment, flags);
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
		int transactionCode = 1;
		int flags = composeFlags(transactionCode, false);
		
		int entryId = addEntry(submitter, amount, currency, comment, flags);
		
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
		
		int transactionCode = 4;
		int flags = composeFlags(transactionCode, false);
		
    	int entryId = getNewEntryId();
    	
		if (entryId > -1) {
			for (Map.Entry<String, Double> share : deals.rawMap.entrySet())
				if (addRecord(entryId, share.getKey(), share.getValue(), currency, Helper.timestampNow(), comment, flags) < 0) {
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
		return rawQuery("select name, sum(amount) as balance from " + kitty + 
				whereClause(notInternalCondition()) + " and timestamp not null group by name order by name", null, 
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
		return rawQuery("select name, sum(amount) as balance from " + kitty + 
				whereClause(notInternalCondition()) + " group by name order by name", null, 
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
    	return getSum();
    }
    /**
     * calculates the accumulated costs
     * @return	the sum over all entries marked as 'expense'
     */
    public double expenses() {
    	return getSum("flags % 10 > 0 and timestamp not null");
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
		return rawQuery("select distinct name from " + kitty + whereClause(notInternalCondition()), null, 
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
		return rawQuery("select distinct comment from " + kitty + whereClause("length(comment) > 0"), null, 
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
    public Integer[] getEntryIds(String... clause) {
		return rawQuery("select entry from " + kitty + whereClause(clause), null, 
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
		return rawQuery("select distinct entry from " + tableName + whereClause(clause), null, 
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
		return rawQuery("select * from " + tableName + whereClause("entry=" + entryId), null, 
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
    public Long[] getRowIds(String... clause) {
		return rawQuery("select rowid from " + kitty + whereClause(clause), null, 
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
    public Double getSum(String... clause) {
		return rawQuery("select sum(amount) from " + kitty + whereClause(clause), null, 
			new QueryEvaluator<Double>() {
				public Double evaluate(Cursor cursor, Double defaultResult, Object... params) {
					if (cursor.moveToFirst()) 
						return cursor.getDouble(0);
					else
						return defaultResult;
				}
			}, null);
	}
    /**
     * @param params	an array of parameters : the first is the name of the table to count the records in (default is the main table) and the second is a condition constraining the query in a where clause
     * @return	the count of the queried records
     */
    public Integer getCount(Object... params) {
    	String tableName = param(kitty, 0, params);
    	String clause = param(null, 1, params);
		return rawQuery("select count(*) from " + tableName + whereClause(clause), null, 
			new QueryEvaluator<Integer>() {
				public Integer evaluate(Cursor cursor, Integer defaultResult, Object... params) {
					if (cursor.moveToFirst()) 
						return cursor.getInt(0);
					else
						return defaultResult;
				}
			}, null);
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
			    	Boolean result = defaultResult;
			    	String name = param("", 0, params);
			    	
			    	if (cursor.moveToFirst()) 
			    		do {
			        		if (name.compareToIgnoreCase(cursor.getString(0)) == 0) {
			        			result = true;
			        			break;
			        		}
			    		} while (cursor.moveToNext());
					
			    	return result;
				}
			}, false, name);
    }
    /**
     * retrieves the names of tables that exist under a given name or - if no name is given - are current
     * @param suffix	the name of a saved table if given
     * @return	the <code>Set</code> of table names
     */
    public Set<String> tableSet(Object... suffix) {
		return rawQuery("select name from sqlite_master where type = 'table'", null, 
			new QueryEvaluator<Set<String>>() {
				public Set<String> evaluate(Cursor cursor, Set<String> defaultResult, Object... params) {
			    	TreeSet<String> set = new TreeSet<String>();
			    	String suffix = param(null, 0, params);
			    	
			    	if (cursor.moveToFirst()) 
			    		do {
			        		String name = cursor.getString(0);
							if (suffix == null) {
								if (tableList.contains(name))
									set.add(name);
							}
							else if (name.endsWith("_" + suffix)) 
								set.add(name);
			    		} while (cursor.moveToNext());
					
			    	return set;
				}
			}, null, suffix);
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
	        		String prefix = ShareUtil.tableName("");
		    		
			    	if (cursor.moveToFirst()) 
			    		do {
			        		String name = cursor.getString(0);
							if (name.startsWith(prefix))
			        			names.add(name);
			    		} while (cursor.moveToNext());
					
			    	Log.i(TAG, String.format("saved tables : %s", names.toString()));
			    	return names;
				}
			}, null);
    }
    /**
     * changes the name of the table that has been worked on (current table). 
     * Thus this table is 'saved' in the same database. Note that the current table is non-existing after this operation. 
     * In order to continue the current table has to be restored (loadFrom) or recreated (clear).
     * @param suffix	the <code>String</code> to append to the name of the current table in order to form the new table name 
     * @return	success if true
     */
    public boolean saveAs(String suffix) {
    	if (!notNullOrEmpty(suffix))
    		return false;
    	
    	String newTableName = ShareUtil.tableName(suffix);
    	if (savedTables().contains(newTableName))
    		return false;
    	
    	for (int index : savedTableIndices()) {
    		String oldTableName = tableList.get(index);
        	if (tableExists(oldTableName) && getCount(oldTableName) > 0) {
        		newTableName = ShareUtil.tableName(oldTableName, suffix);
            	rename(oldTableName, newTableName);
        	}
        	else
        		drop(oldTableName);
		}
    	
		Log.i(TAG, String.format("table saved as '%s'", newTableName));
    	return true;
    }
    
    protected int[] savedTableIndices() {
    	return new int[] {0,1};
    }
    
    /**
     * restores one of the saved tables as the current table. Note that the table that was current up to this point will be dropped.
     * Also there will be one less 'saved' table after this operation.
     * @param suffix	the <code>String</code> to append to the name of the current table in order to form the old table name 
     * @return	success if true
     */
    public boolean loadFrom(String suffix) {
    	if (!notNullOrEmpty(suffix))
    		return false;
    	
    	String oldTableName = ShareUtil.tableName(suffix);
    	if (!savedTables().contains(oldTableName))
    		return false;
    		
    	for (int index : savedTableIndices()) {
    		String newTableName = tableList.get(index);
	    	oldTableName = ShareUtil.tableName(newTableName, suffix);
	    	if (tableExists(oldTableName))
	    		rename(oldTableName, newTableName);
	    	else
	    		create(newTableName);
    	}
    	
		Log.i(TAG, String.format("table loaded from '%s'", oldTableName));
    	return true;
    }
    /**
     * deletes all records from the current table and recreates it
     */
    public void clear() {
    	Integer count = tableExists(kitty) ? getCount() : 0;
    	_clear();
		Log.i(TAG, String.format("table '%s' cleared, %d records deleted", kitty, count));
		setSharingPolicy(null);
    }
    /**
     * clears the current table and drops all saved tables
     */
    public void clearAll() {
    	for (String table : savedTables())
    		drop(table);
    	
    	_clear();
		Log.i(TAG, String.format("table '%s' cleared, all saved tables dropped", kitty));
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
		return rawQuery("select flags from " + kitty + " where ROWID=" + rowId, null, 
			new QueryEvaluator<Boolean>() {
				public Boolean evaluate(Cursor cursor, Boolean defaultResult, Object... params) {
					return cursor.moveToFirst() && cursor.getInt(0) % 10 > 0;
				}
			}, false);
	}
	
	/** @hide */ public int getNewEntryId() {
		return 1 + rawQuery("select max(entry) from " + kitty, null, 
			new QueryEvaluator<Integer>() {
				public Integer evaluate(Cursor cursor, Integer defaultResult, Object... params) {
					if (cursor.moveToFirst()) 
						return cursor.getInt(0);
					else
						return defaultResult;
				}
			}, -1);
    }
   
    /** @hide */ public int addEntry(String name, double amount, String currency, String comment, int flags) {
    	int entryId = getNewEntryId();
        
        long rowId = addRecord(entryId, name, amount, currency, Helper.timestampNow(), comment, flags);
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
    
    /** @hide */ public boolean allocate(int flags, int entryId, Map<String, Double> portions) {
		for (String name : portions.keySet()) {
			double portion = portions.get(name);

			if (addRecord(entryId, name, portion, null, null, null, flags) < 0)
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
    
    protected ContentValues putValues(Integer entry, String name, Double amount, String currency, String timestamp, String comment, Integer flags) {
        ContentValues values = new ContentValues();
        
    	String[] fields = getFieldNames();
        if (entry != null) values.put(fields[0], entry);
        if (name != null) values.put(fields[1], name);
        if (amount != null) values.put(fields[2], amount);
        if (currency != null) values.put(fields[3], currency);
        if (timestamp != null) values.put(fields[4], timestamp);
        if (comment != null) values.put(fields[5], comment);
        if (flags != null) values.put(fields[6], flags);
        
        return values;
    }
	
    protected long addRecord(int entryId, String name, double amount, String currency, String timestamp, String comment, int flags) {
        ContentValues values = putValues(entryId, name, amount, currency, timestamp, comment, flags);
    	long rowId = insert(values);
    	if (rowId < 0)
    		Log.e(TAG, String.format(".addRecord failed with : %s", values.toString()));
    	return rowId;
	}

	protected int updateExpenseFlag(long rowId, int flags) {
		return update(rowId, putValues(null, null, null, null, null, null, flags));
	}

    protected String[] getFieldNames() {
    	return new String[] {"entry", "name", "amount", "currency", "timestamp", "comment", "flags", "ROWID"};
    }

    private static List<String> tableList = new ArrayList<String>(Transaction.tableDefs.keySet());
	
    private ContentResolver contentResolver;
    
    protected void _clear() {
    	contentResolver.query(TransactionProvider.contentUri("clear"), null, null, null, null);
    }
    
    public void drop(String tableName) {
    	Uri uri = TransactionProvider.contentUri("drop");
    	uri = Uri.withAppendedPath(uri, tableName);
		contentResolver.query(uri, null, null, null, null);
	}
    
    protected void create(String tableName) {
    	Uri uri = TransactionProvider.contentUri("create");
    	uri = Uri.withAppendedPath(uri, tableName);
		contentResolver.query(uri, null, null, null, null);
	}
    
	protected void rename(String oldTableName, String newTableName) {
    	Uri uri = TransactionProvider.contentUri("rename");
    	uri = Uri.withAppendedPath(uri, oldTableName);
    	uri = Uri.withAppendedPath(uri, newTableName);
		contentResolver.query(uri, null, null, null, null);
	}

	private Uri KITTY_URI = TransactionProvider.contentUri(0);
	
    protected long insert(ContentValues values) {
    	Uri uri = contentResolver.insert(KITTY_URI, values);
    	return toLong(-1L, uri.getPathSegments().get(1));
	}

	protected int update(long rowId, ContentValues values) {
		return contentResolver.update(ContentUris.withAppendedId(KITTY_URI, rowId), values, "", null);
	}

	protected int delete(String clause) {
		return contentResolver.delete(KITTY_URI, clause, null);
	}

	protected Cursor query(String[] columns, String clause) {
		return contentResolver.query(KITTY_URI, columns, clause, null, null);
	}
	
	public Cursor rawQuery(String sql, String[] selectionArgs) {
		return contentResolver.query(TransactionProvider.contentUri("raw"), null, sql, selectionArgs, null);
	}
}
