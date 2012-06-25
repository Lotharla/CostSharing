package com.applang;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TableRow.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.applang.db.*;

public class PaymentEditView extends Activity {
	private static final int TYPE_NUMBER_FLAG_DECIMAL = 0;
	private Transactor transactor;
	        EditText mAmountText;
	        EditText mSubmitterText;
	        EditText mPurposeText;
	        EditText mFirstParticipantText;
	        EditText mFirstShareText;
	        EditText etLeft;
	        EditText etRight;
	        String firstShareString;
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
        		
        		String mPaymentMessageTemplate =
        	        	getString(R.string.payment_message_template);
        		String message = 
        				String.format(mPaymentMessageTemplate,submitter, amount, comment, names, shares);
        		showToast(message);    	
        	}

        });    
        
	}
	
	private void createTableRow(View view) {  		  
		  TableLayout tl = (TableLayout) findViewById(R.id.recip_table);
		  TableRow tr = new TableRow(this);
		  LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		  //tr.setLayoutParams(lp);
		  
	      participantNum = participantNum +1;
	      
		  etLeft = new EditText(this);
		  etLeft.setLayoutParams(lp);
		  // etLeft.setId(R.id.participantNum);		 
		  etRight = new EditText(this);
		  etRight.setLayoutParams(lp);
		  //etRight.getInputType();
		  etRight.setInputType(TYPE_NUMBER_FLAG_DECIMAL);
		  //etRight.setId(shareNum);
		  
		  Button rbRight = new Button(this);
		  rbRight.setLayoutParams(lp);
		  rbRight.setText(R.string.plus);

		  tr.addView(etLeft);
		  tr.addView(etRight);
		  tr.addView(rbRight);

		  tl.addView(tr, new TableLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		  
		  rbRight.setOnClickListener(new View.OnClickListener() {
	        	public void onClick(View view) {
	        		getEntryParticipants();
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
	}


	private void getEntryParticipants() {
		
		try{
			nameString = etLeft.getText().toString();
			shareString = etRight.getText().toString();
			share= Double.parseDouble(shareString);
			}catch(Exception e ){
				share=0.0;
				nameString = "name" + participantNum;
			}

		
		if (participantNum < 3 && participantNum > 0) {
			names = new ArrayList<String>();
			shares = new ArrayList<Double>();
			try{
			firstShareString = mFirstShareText.getText().toString();
			firstShare= Double.parseDouble(firstShareString);
			}catch(Exception e ){
				share=0.0;
			}
			names.add(mFirstParticipantText.getText().toString());
			names.add(nameString);
			shares.add(firstShare);
			shares.add(share);
			
		} else if (participantNum > 2){
			names.add(nameString);
			shares.add(share);
		} 
		
		
	}
	
	private void showToast(String text) {
		Toast.makeText(this, text, Toast.LENGTH_LONG).show();	 
		}
}
