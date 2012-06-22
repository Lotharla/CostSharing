package com.applang;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.applang.db.*;

public class CostSharingActivity extends Activity {
    private Transactor transactor;
    private String mButtonMessageTemplate;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        transactor = new Transactor(this);
        
        mButtonMessageTemplate =
        		getString(R.string.button_message_template);
        		
	}
	
	public void showPaymentView(View clickedButton) {
		Button button = (Button)clickedButton;
		Intent activityIntent =
				new Intent(this, PaymentEditView.class);
		startActivity(activityIntent);
		}

	public void showAccountView(View clickedButton) {
		Button button = (Button)clickedButton;
		CharSequence text = button.getText();
		String message =
				String.format(mButtonMessageTemplate, text);
		showToast(message);
		}
	
	public void showGroupView(View clickedButton) {
		Button button = (Button)clickedButton;
		CharSequence text = button.getText();
		String message =
				String.format(mButtonMessageTemplate, text);
		showToast(message);
		}
	
	public void showCalenderView(View clickedButton) {
		Button button = (Button)clickedButton;
		CharSequence text = button.getText();
		String message =
				String.format(mButtonMessageTemplate, text);
		showToast(message);
		}
	
	
	
	private void showToast(String text) {
		Toast.makeText(this, text, Toast.LENGTH_LONG).show();	 
		}

}
