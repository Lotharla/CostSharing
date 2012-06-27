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
	private static final int TYPE_NUMBER_FLAG_DECIMAL = 0;
	private Transactor transactor;
			ShareMap sharemap;
	        EditText mAmountText;
	        EditText mSubmitterText;
	        EditText mPurposeText;
	        EditText mFirstParticipantText;
	        EditText mFirstShareText;
	        EditText etLeft;
	        EditText etRight;
	        String firstShareString;
	        String firstParticipantString;
	        String shareString;
			String amountString;
			String nameString;
			String submitter;
			String comment;
			ArrayList<String> names;
			ArrayList<Double> shares;
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
        Button confirmButton = (Button) findViewById(R.id.confirm);
        Button plus = (Button) findViewById(R.id.plus);
        
        if (mFirstParticipantText != null) {
        	participantNum = 1;  	
        }
        
        plus.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View view) {
        		createTableRow(view);  	
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
		  tr.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		  
	      participantNum++;
//	      tr.setId(participantNum);
	      
		  etLeft = new EditText(this);
		  etLeft.setLayoutParams(lp);
		  etLeft.setId(100 + participantNum);	
		  
		  etRight = new EditText(this);
		  etRight.setLayoutParams(lp);
		  etRight.setInputType(InputType.TYPE_CLASS_NUMBER);
		  etRight.setId(200 + participantNum);
		  
		  Button rbRight = new Button(this);
		  rbRight.setLayoutParams(lp);
		  rbRight.setText(R.string.plus);

		  tr.addView(etLeft);
		  tr.addView(etRight);
		  tr.addView(rbRight);

		  tl.addView(tr, new TableLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		  
		  rbRight.setOnClickListener(new View.OnClickListener() {
	        	public void onClick(View view) {
	        		createTableRow(view);  	
	        	}

	        });    
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
			
			firstParticipantString = mFirstParticipantText.getText().toString();
			
			try{
				firstShareString = mFirstShareText.getText().toString();
				firstShare= Double.parseDouble(firstShareString);
			}catch(Exception e ){
				firstShare=0.0;
			}
			names.add(firstParticipantString);
			shares.add(firstShare);
			
		}else if (participantNum > 1) {
			names = new ArrayList<String>();
			shares = new ArrayList<Double>();
			
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
			names.add(firstParticipantString);
			shares.add(firstShare);
			
			for (int i = 2; i < participantNum +1; i++){
				
				EditText etLeft = (EditText) findViewById(100 + i);
				EditText etRight = (EditText) findViewById(200 + i);
			
				if(etLeft.length() > 0){
					nameString = etLeft.getText().toString();
				}else{
					nameString = "Noname" + i;
				}
				try{
					shareString = etRight.getText().toString();
					share= Double.parseDouble(shareString);
				}catch(Exception e ){
					share=0.0;
				}
				names.add(nameString);
				shares.add(share);
				
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
				String.format(mPaymentMessageTemplate,submitter, amount.toString(), comment, names.toString(), shares.toString());
		
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();	 
		}
	
}
