package com.applang.provider;

import java.util.LinkedHashMap;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Convenience definitions for TransactionProvider
 */
public final class Transaction
{
    // This class cannot be instantiated
    private Transaction() {}

    public static final String AUTHORITY = "com.applang.provider.Transaction";

	public static String[] TRANSACTIONS = new String[] {
		"Expense", 
		"ComplexExpense", 
		"Transfer", 
		"Submission", 
		"Multiple", 
	};

	public static String kitty = "KITTY";

	public static LinkedHashMap<String, String> tableDefs = new LinkedHashMap<String, String>();
    static {
    	tableDefs.put(kitty, 
    		Records.ENTRY + " integer not null," +
    		Records.NAME + " text not null," +
    		Records.AMOUNT + " real not null," +
    		Records.CURRENCY + " text," +
    		Records.TIMESTAMP + " text," +
    		Records.COMMENT + " text," +
    		Records.FLAGS + " integer not null"
    	);
    	tableDefs.put("purposes", 
    		Records.ENTRY + " integer not null," + 
    		Records.CATEG_ID + " integer not null," + 
			"foreign key(categoryId) references Categories(categoryId)"
    	);
    	tableDefs.put("categories", 
    		Records.CATEG_ID + " integer primary key," + 
    		Records.CATEGORY + " text"
    	);
    	tableDefs.put("policies", 
			Records.POLICY_ID + " integer primary key," + 
			Records.POLICY + " text," +
			Records.COMMENT + " text"
    	);
    }
    
    /**
     * Records table
     */
    public static final class Records implements BaseColumns {
		// This class cannot be instantiated
        private Records() {}

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/*");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of records.
         */
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.applang.record";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single record.
         */
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.applang.record";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "entry ASC";

        /**
         * unique for the entries, reference for the portions
         * <P>Type: INTEGER not null</P>
         */
        public static final String ENTRY = "entry";

        /**
         * the person involved
         * <P>Type: TEXT not null</P>
         */
        public static final String NAME = "name";

        /**
         * the amount of money, negative for portions
         * <P>Type: REAL not null</P>
         */
        public static final String AMOUNT = "amount";

        /**
         * if null it's the default currency (Euro or Dollar or ...)
         * <P>Type: TEXT</P>
         */
        public static final String CURRENCY = "currency";

        /**
         * The timestamp for when the entry was created (if null it's a portion)
         * <P>Type: TEXT</P>
         */
        public static final String TIMESTAMP = "timestamp";

        /**
         * optional, for recognition
         * <P>Type: TEXT</P>
         */
        public static final String COMMENT = "comment";

        /**
         * reflects the transaction type and a flag marking expenses
         * <P>Type: INTEGER not null</P>
         */
        public static final String FLAGS = "flags";
        
        /**
         * The unique ID for a row.
         * <P>Type: INTEGER (long)</P>
         */
        public static final String _ID = "ROWID";

        public static String[] FULL_PROJECTION = new String[] {
			_ID, // 0
			ENTRY, // 1
			NAME, // 2
			AMOUNT, // 3
			CURRENCY, // 4
			TIMESTAMP, // 5
			COMMENT, // 6
			FLAGS, // 7
		};
        
        /**
         * The unique ID for a row.
         * <P>Type: INTEGER (long)</P>
         */
        public static final String CATEG_ID = "categoryId";

        /**
         * category of purpose
         * <P>Type: TEXT</P>
         */
        public static final String CATEGORY = "category";
        
        /**
         * The unique ID for a row.
         * <P>Type: INTEGER (long)</P>
         */
        public static final String POLICY_ID = "policyId";

        /**
         * category of purpose
         * <P>Type: TEXT</P>
         */
        public static final String POLICY = "policy";
    }
}
