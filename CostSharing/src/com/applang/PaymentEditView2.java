package com.applang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TableRow.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.applang.db.*;
import com.applang.share.ShareMap;

public class PaymentEditView2 extends Activity {
	private ArrayList<String> names;
			ArrayList<Double> shares;
			ArrayList<String> currencies;
			LinkedHashMap<String,Double> submitters;
			Double share2;
			Double amount;
			Double purposeAmount2;
			Double listDouble;
			
			EditText mDateText;
			EditText mAmountText;
	        EditText mCurrencyText;
	        EditText mSubmitterText;
	        EditText mPurposeText;
	        EditText mParticipant2Text;
	        EditText mShare2Text;
	        EditText mType2Text;
	        EditText mPurposeAmount2Text;
	        EditText etName;
	        EditText etShare;
	        EditText etType;
	        EditText etAs;
	        int participantNum;
	        int dateNum;
	        int subNum;
	        int partNum;
	        int fillNum;
	        int listId;
	        List<HashMap<String, String>> fillMaps;
	        List<HashMap<String, String>> submitterMaps;
	        List<HashMap<String, String>> participantMaps;
	        ShareMap sharemap;
	        String dateString;
	        String singleDateString;
	        String startDateString;
	        String endDateString;
	        String participant2;
	        String type2;
			String nameString;
			String currency;
			String submitter;
			String listString1;
			String listString2;
			String listInterimText;
//			String[] submitters;
			String comment;
			Transactor transactor;
			TextView mDatesText;
			View mUpperWindowView;
			View currentView;
			
			
	        
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.payment_edit2);
//      Versuch den ViewManager zu instanzieren, um removeView() in postDate/case2 zu nutzen 
//      Context.getSystemService();
        
        transactor = new Transactor(this);
        mUpperWindowView = (View) findViewById(R.id.upperWindow);
        mDateText = (EditText) findViewById(R.id.date);
        mSubmitterText = (EditText) findViewById(R.id.submitter);
        mAmountText = (EditText) findViewById(R.id.amount);
        mCurrencyText = (EditText) findViewById(R.id.currency);
        mPurposeAmount2Text = (EditText) findViewById(R.id.purposeAmount2);
        mPurposeText = (EditText) findViewById(R.id.purpose);
        mParticipant2Text = (EditText) findViewById(R.id.participant2);
        mShare2Text = (EditText) findViewById(R.id.share2);
        View vw = (View) findViewById(R.id.type2);
        mType2Text = (EditText) findViewById(R.id.type2);
        mDatesText = (TextView) findViewById(R.id.dates);
        
        Button confirmButton = (Button) findViewById(R.id.confirm);
        Button plus = (Button) findViewById(R.id.plus2);
        Button minus = (Button) findViewById(R.id.minus2);
        
        dateNum = 0;
        subNum = 0;
        partNum = 0;
//		if (mFirstParticipantText != null) {
//			participantNum = 1;
//		}
     // prepare the list of all records
        submitterMaps = new ArrayList<HashMap<String, String>>();
        participantMaps = new ArrayList<HashMap<String, String>>();
        
        plus.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View view) {
        		if (mSubmitterText.length() < 1 && mPurposeText.length() < 1 && mParticipant2Text.length() < 1) {
        			String message = "Keine Angaben zum Hinzufügen";
    				showToast(message);
    			} else {		
	        		if (mDateText.length() > 0) {
	        			postDate();
	        		} else {
	        			mDatesText.setText("Heute");
	        		}
	        		if (mSubmitterText.length() < 1) {
	    				if (mAmountText.length() > 0){
		    				String message = "Bitte eingeben wer zahlt";
		    				showToast(message);
	    				} 
	    			}else if (mAmountText.length() < 1) {
	    				String message = "Bitte eingeben wieviel ausgegeben wurde";
	    				showToast(message);
	    			}else {
	    				subNum++;
	    				listString1 = mSubmitterText.getText().toString();
	    				listInterimText = getString(R.string.list_interim_text_submitter);
	    				listDouble = Double.parseDouble(mAmountText.getText().toString());
	    				if (mCurrencyText.length() > 0){
	    					listString2 = mCurrencyText.getText().toString();
	    				} else {
	    					listString2 = "€";
	    				}
	    				listId = R.id.submitters_amounts;
	    				fillMaps = submitterMaps;
	    				fillNum = subNum;
	    				
	    				mSubmitterText.setText("");
	    				mSubmitterText.setHint("Wer noch?");
	    				mAmountText.setText("");
	    				postList();
	    			}			
	    			if (mPurposeText.length() > 0){
	//					postPurposes();
					} else if (mPurposeAmount2Text.length() > 0){
						String message = "Bitte eingeben wofür gezahlt wird";
	    				showToast(message);
					}
					if (mParticipant2Text.length() > 0 || mShare2Text.length() > 0){
						if (mParticipant2Text.length() < 1){
							String message = "Bitte eingeben wer sich beteiligt";
		    				showToast(message);
						}else if (mShare2Text.length() < 1) {
	        				String message = "Bitte eingeben mit wieviel sich beteiligt wird";
	        				showToast(message);
	        			} else {
	        				partNum++;
	        				listString1 = mParticipant2Text.getText().toString();
	        				listInterimText = getString(R.string.list_interim_text_participant);
	        				listDouble = Double.parseDouble(mShare2Text.getText().toString());
	        				if (mType2Text.length() > 0){
	        					listString2 = mType2Text.getText().toString();
	        				} else if (mCurrencyText.length() > 0){
	        					listString2 = mCurrencyText.getText().toString();	
	        				} else {
	        					listString2 = "€";
	        				}
	        				listId = R.id.participants_shares;
	        				fillMaps = participantMaps;
	        				fillNum = partNum;
	        				
	        				mParticipant2Text.setText("");
		    				mParticipant2Text.setHint("Wer noch?");
		    				mShare2Text.setText("");
	        				postList();
	        			}	
					} 	
    			}
    		}
        });    
        
        minus.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View view) {
        		removeTableRow(view);  	
        	}

        });   
        
        confirmButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View view) {
//        		getEntryData();	
//        		saveData();
        	}

        });    
        
	}
	
	private void postDate(){	
			dateNum++;
			switch (dateNum){
			case 1:
				String mSingleDatePrefix = (String) getString(R.string.single_date_prefix); 
				singleDateString = mDateText.getText().toString();
				dateString = mSingleDatePrefix + singleDateString;
				mDatesText.setText(dateString);
				mDateText.setText("");
				mDateText.setHint(R.string.end_date);
			break;
			case 2:
				// delete case1-post ????
				String mStartDatePrefix = (String) getString(R.string.start_date_prefix);
				String mEndDatePrefix = (String) getString(R.string.end_date_prefix);
				startDateString = singleDateString;
				endDateString = mDateText.getText().toString();
				dateString = mStartDatePrefix + startDateString + mEndDatePrefix + endDateString;
				mDatesText.setText(dateString);
				mDateText.setText("");
				mDateText.setHint("STOP");
			}		
	}
	
	private void postList(){	
        ListView lv= (ListView)findViewById(listId);

        // create the grid item mapping
        String[] from = new String[] {"rowid", "col_1", "text", "col_2", "col_3"};
        int[] to = new int[] {R.id.rowId, R.id.item1, R.id.text1, R.id.item2, R.id.item3};

    	HashMap<String, String> map = new HashMap<String, String>();
    	map.put("rowid", "" + fillNum);
    	map.put("col_1", listString1);
    	map.put("text", listInterimText);
    	map.put("col_2", listDouble.toString());
    	map.put("col_3", listString2);
    	fillMaps.add(map);
        

        // fill in the grid_item layout
        SimpleAdapter adapter = new SimpleAdapter(this, fillMaps, R.layout.paylist_row, from, to);
        lv.setAdapter(adapter);
    }
	
	private void createTableRow(View view) {  		  
		  TableLayout tl = (TableLayout) findViewById(R.id.recip_table);
		  TableRow tr = new TableRow(this);
		  LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		  
	      participantNum++;
//	      tr.setId(participantNum);
	      
		  etName = new EditText(this);
		  etName.setLayoutParams(lp);
		  etName.setId(100 + participantNum);	
		  
		  etShare = new EditText(this);
		  etShare.setLayoutParams(lp);
		  etShare.setInputType(InputType.TYPE_CLASS_NUMBER);
		  etShare.setId(200 + participantNum);
		  
		  etType = new EditText(this);
		  etType.setLayoutParams(lp);
		  etType.setId(300 + participantNum);
		  
		  etAs = new EditText(this);
		  etAs.setLayoutParams(lp);
		  etAs.setId(400 + participantNum);	
		  

		  tr.addView(etName);
		  tr.addView(etShare);
		  tr.addView(etType);
		  tr.addView(etAs);

		  tl.addView(tr, new TableLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));	  	  
	}
	
	private void removeTableRow(View view) {  		  
		  TableLayout tl = (TableLayout) findViewById(R.id.recip_table);
		  TableRow tr;
		  try{
			  tr = (TableRow) tl.findFocus().getParent();
			}catch(Exception e ){
				tr = null;		
			}
		  TableRow firstRow = (TableRow) findViewById(R.id.first_row);
		  
		  if (tr != firstRow && tr != null){
			  tl.removeView(tr);	
		  }else{
			  Toast.makeText(this, "No need for delete!", Toast.LENGTH_LONG).show();
		  }	 	 
	}
	
	
//	private void getEntryData() {
//		
//		try{
//		amountString = mAmountText.getText().toString();
//		amount = Double.parseDouble(amountString);
//		currency = mCurrencyText.getText().toString();
//		submitter = mSubmitterText.getText().toString();
//	    comment = mPurposeText.getText().toString();
//		}catch(Exception e ){
//			amount=0.0;
//			submitter = "niemand";
//			comment = "nix";
//		}
//	    	getEntryParticipants();		
//	}
//
//
//	private void getEntryParticipants() {
//		
//		if (participantNum < 2) {
//			names = new ArrayList<String>();
//			shares = new ArrayList<Double>();
//			currencies = new ArrayList<String>();
//			stakeholderTypes = new ArrayList<String>();
//			
//			firstParticipantString = mFirstParticipantText.getText().toString();
//			firstTypeString = mFirstTypeText.getText().toString();
//			firstAsString = mFirstAsText.getText().toString();
//			
//			try{
//				firstShareString = mFirstShareText.getText().toString();
//				firstShare= Double.parseDouble(firstShareString);
//			}catch(Exception e ){
//				firstShare=0.0;
//			}
//			names.add(firstParticipantString);
//			shares.add(firstShare);
//			currencies.add(firstTypeString);
//			stakeholderTypes.add(firstAsString);
//			
//		}else if (participantNum > 1) {
//			names = new ArrayList<String>();
//			shares = new ArrayList<Double>();
//			currencies = new ArrayList<String>();
//			stakeholderTypes = new ArrayList<String>();
//			
//			if (mFirstParticipantText.length() > 0){
//				firstParticipantString = mFirstParticipantText.getText().toString();
//			}else {
//				firstParticipantString = "Noname1";
//			}
//			try{
//				firstShareString = mFirstShareText.getText().toString();
//				firstShare= Double.parseDouble(firstShareString);
//			}catch(Exception e ){
//				firstShare=0.0;
//			}
//			try{
//				firstTypeString = mFirstTypeText.getText().toString();
//			}catch(Exception e ){
//				firstTypeString="€";
//			}
//			try{
//				firstAsString = mFirstAsText.getText().toString();
//			}catch(Exception e ){
//				firstAsString="N";	
//			}
//			names.add(firstParticipantString);
//			shares.add(firstShare);
//			currencies.add(firstTypeString);
//			stakeholderTypes.add(firstAsString);
//			
//			for (int i = 2; i < participantNum +1; i++){
//				
//				EditText etName = (EditText) findViewById(100 + i);
//				EditText etShare = (EditText) findViewById(200 + i);
//				EditText etType = (EditText) findViewById(300 + i);
//				EditText etAs = (EditText) findViewById(400 + i);
//			
//				if(etName.length() > 0){
//					nameString = etName.getText().toString();
//				}else{
//					nameString = "Noname" + i;
//				}
//				try{
//					shareString = etShare.getText().toString();
//					share= Double.parseDouble(shareString);
//				}catch(Exception e ){
//					share=0.0;
//				}
//				try{
//					typeString = etType.getText().toString();
//				}catch(Exception e ){
//					typeString="€";
//				}
//				try{
//					asString = etAs.getText().toString();
//				}catch(Exception e ){
//					asString="N";	
//				}
//				names.add(nameString);
//				shares.add(share);
//				currencies.add(typeString);
//				stakeholderTypes.add(asString);
//				
//			}					
//		} 	
//	}
//		
//	private void saveData() {
//			// currency, currencies and stakeholderTypes aren't included in saveData yet !
//			if(participantNum < 2 && firstParticipantString.length() < 1){
//				transactor.performSubmission(submitter, amount, comment);
//			}else {
//				if (amount != 0.0){
//					sharemap = new ShareMap(names.toArray(new String[0]), amount, shares.toArray(new Double[0]));
//				} else {
//					sharemap = new ShareMap(names.toArray(new String[0]), shares.toArray(new Double[0]));
//				}
//				transactor.performExpense(submitter, comment, sharemap);
//			}
//		
//			showToast();   	
//			
//		}
			 
	private void showToast(String message) {
		
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();	 
		}
	
}
