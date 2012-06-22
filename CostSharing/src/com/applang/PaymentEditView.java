package com.applang;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
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
	      
		  TextView tvLeft = new TextView(this);
		  tvLeft.setLayoutParams(lp);
		  tvLeft.setText(R.string.next_participant);
		  etLeft = new EditText(this);
		  etLeft.setLayoutParams(lp);
		  // etLeft.setId(R.id.participantNum);
		 
		  TextView tvRight = new TextView(this);
		  tvRight.setLayoutParams(lp);
		  tvRight.setText(R.string.next_participant);
		  etRight = new EditText(this);
		  etRight.setLayoutParams(lp);
		  etRight.getInputType();
		  etRight.setInputType(TYPE_NUMBER_FLAG_DECIMAL);
		  //etRight.setId(shareNum);
		  
		  RadioButton rbRight = new RadioButton(this);
		  rbRight.setLayoutParams(lp);

		  tr.addView(tvLeft);
		  tr.addView(etLeft);
		  tr.addView(tvRight);
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
		amountString = mAmountText.getText().toString();
		amount = Double.parseDouble(amountString);
	    submitter = mSubmitterText.getText().toString();
	    comment = mPurposeText.getText().toString();	 
		}


	private void getEntryParticipants() {
		shareString = etRight.getText().toString();
		share = Double.parseDouble(shareString);
		
		if (participantNum < 2 && participantNum > 0) {
			names = new ArrayList<String>();
			firstShareString = mFirstShareText.getText().toString();
			firstShare = Double.parseDouble(firstShareString);
			
			names.add(mFirstParticipantText.getText().toString());
			names.add(etLeft.getText().toString());
			shares.add(firstShare);
			shares.add(share);
			
		} else if (participantNum > 2){
			names.add(etLeft.getText().toString());
			shares.add(share);
		}
		
		
		}
	
	private void showToast(String text) {
		Toast.makeText(this, text, Toast.LENGTH_LONG).show();	 
		}
}
