package com.applang;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.TableRow.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import com.applang.db.*;
import com.applang.share.ShareMap;

public class PaymentEditView extends Activity {
	private Transactor transactor;
			ShareMap sharemap;
	        EditText mAmountText;
	        EditText mSubmitterText;
	        EditText mPurposeText;
	        EditText mFirstParticipantText;
	        EditText mFirstShareText;
	        EditText mFirstTypeText;
	        EditText mFirstAsText;
	        EditText etName;
	        EditText etShare;
	        EditText etType;
	        EditText etAs;
	        String firstShareString;
	        String firstParticipantString;
	        String firstTypeString;
	        String firstAsString;
	        String shareString;
			String amountString;
			String nameString;
			String typeString;
			String asString;
			String submitter;
			String comment;
			ArrayList<String> names;
			ArrayList<Double> shares;
			ArrayList<String> currencies;
			ArrayList<String> stakeholderTypes;
			Double firstShare;
			Double share;
			Double amount;
			int participantNum;
	        
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.payment_edit);
        
        transactor = new Transactor(this);
        mAmountText = (EditText) findViewById(R.id.amount);
        mSubmitterText = (EditText) findViewById(R.id.submitter);
        mPurposeText = (EditText) findViewById(R.id.purpose);
        mFirstParticipantText = (EditText) findViewById(R.id.first_participant);
        mFirstShareText = (EditText) findViewById(R.id.first_share);
        mFirstTypeText = (EditText) findViewById(R.id.first_type);
        mFirstAsText = (EditText) findViewById(R.id.first_as);
        Button confirmButton = (Button) findViewById(R.id.confirm);
        Button plus = (Button) findViewById(R.id.plus);
        Button minus = (Button) findViewById(R.id.minus);
        
        if (mFirstParticipantText != null) {
        	participantNum = 1;  	
        }
        
        plus.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View view) {
        		createTableRow(view);  	
        	}

        });    
        
        minus.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View view) {
        		removeTableRow(view);  	
        	}

        });   
        
        confirmButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View view) {
        		getEntryData();	
        		saveData();
        	}

        });    
        
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
	
	
	private void getEntryData() {
		
		try{
		amountString = mAmountText.getText().toString();
		amount = Double.parseDouble(amountString);
	    submitter = mSubmitterText.getText().toString();
	    comment = mPurposeText.getText().toString();
		}catch(Exception e ){
			amount=0.0;
			submitter = "niemand";
			comment = "nix";
		}
	    	getEntryParticipants();		
	}


	private void getEntryParticipants() {
		
		if (participantNum < 2) {
			names = new ArrayList<String>();
			shares = new ArrayList<Double>();
			currencies = new ArrayList<String>();
			stakeholderTypes = new ArrayList<String>();
			
			firstParticipantString = mFirstParticipantText.getText().toString();
			firstTypeString = mFirstTypeText.getText().toString();
			firstAsString = mFirstAsText.getText().toString();
			
			try{
				firstShareString = mFirstShareText.getText().toString();
				firstShare= Double.parseDouble(firstShareString);
			}catch(Exception e ){
				firstShare=0.0;
			}
			names.add(firstParticipantString);
			shares.add(firstShare);
			currencies.add(firstTypeString);
			stakeholderTypes.add(firstAsString);
			
		}else if (participantNum > 1) {
			names = new ArrayList<String>();
			shares = new ArrayList<Double>();
			currencies = new ArrayList<String>();
			stakeholderTypes = new ArrayList<String>();
			
			if (mFirstParticipantText.length() > 0){
				firstParticipantString = mFirstParticipantText.getText().toString();
			}else {
				firstParticipantString = "Noname1";
			}
			try{
				firstShareString = mFirstShareText.getText().toString();
				firstShare= Double.parseDouble(firstShareString);
			}catch(Exception e ){
				firstShare=0.0;
			}
			try{
				firstTypeString = mFirstTypeText.getText().toString();
			}catch(Exception e ){
				firstTypeString="€";
			}
			try{
				firstAsString = mFirstAsText.getText().toString();
			}catch(Exception e ){
				firstAsString="N";	
			}
			names.add(firstParticipantString);
			shares.add(firstShare);
			currencies.add(firstTypeString);
			stakeholderTypes.add(firstAsString);
			
			for (int i = 2; i < participantNum +1; i++){
				
				EditText etName = (EditText) findViewById(100 + i);
				EditText etShare = (EditText) findViewById(200 + i);
				EditText etType = (EditText) findViewById(300 + i);
				EditText etAs = (EditText) findViewById(400 + i);
			
				if(etName.length() > 0){
					nameString = etName.getText().toString();
				}else{
					nameString = "Noname" + i;
				}
				try{
					shareString = etShare.getText().toString();
					share= Double.parseDouble(shareString);
				}catch(Exception e ){
					share=0.0;
				}
				try{
					typeString = etType.getText().toString();
				}catch(Exception e ){
					typeString="€";
				}
				try{
					asString = etAs.getText().toString();
				}catch(Exception e ){
					asString="N";	
				}
				names.add(nameString);
				shares.add(share);
				currencies.add(typeString);
				stakeholderTypes.add(asString);
				
			}					
		} 	
	}
		
	private void saveData() {
		
			if(participantNum < 2 && firstParticipantString.length() < 1){
				transactor.performSubmission(submitter, amount, comment);
			}else {
				if (amount != 0.0){
					sharemap = new ShareMap(names.toArray(new String[0]), amount, shares.toArray(new Double[0]));
				} else {
					sharemap = new ShareMap(names.toArray(new String[0]), shares.toArray(new Double[0]));
				}
				transactor.performExpense(submitter, comment, sharemap);
			}
		
			showToast();   	
			
		}
			 
	private void showToast() {
		String mPaymentMessageTemplate =
	        	getString(R.string.payment_message_template);
		String message = 
				String.format(mPaymentMessageTemplate,submitter, amount.toString(), comment, names.toString(), stakeholderTypes.toString(), shares.toString(), currencies.toString());
		
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();	 
		}
	
}
