package com.applang;

import android.app.Activity;
import android.os.Bundle;

import com.applang.db.*;

public class CostSharingActivity extends Activity {
    private Transactor transactor;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
  
        setContentView(R.layout.main);
        
        transactor = new Transactor(this);
        
	}
}
