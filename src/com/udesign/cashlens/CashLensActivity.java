package com.udesign.cashlens;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class CashLensActivity extends Activity {
	private static String msgTitle1 = "No accounts found";
	private static String msgMsg1 = "Tap screen to create a new account";

	private LinearLayout ln;
	private Button createAccountBtn;
	private Button addExpenseBtn;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		ln = (LinearLayout)findViewById(R.id.mainwindow);
		ln.setBackgroundColor(Color.argb(0xf0, 0xf0, 0xf0, 0xf0));    	

		createAccountBtn = new Button(this);
		createAccountBtn.setText("Open Account");
		
		addExpenseBtn = new Button(this);
		addExpenseBtn.setText("Add expense");
		
		ln.addView(createAccountBtn);
		ln.addView(addExpenseBtn);
		
		createAccountBtn.setOnClickListener(new Button.OnClickListener() { public void onClick (View v){ msg(msgTitle1, msgMsg1); }});    	
		addExpenseBtn.setOnClickListener(new Button.OnClickListener() 
			{ 
				public void onClick(View v)
				{ 
					Intent myIntent = new Intent(CashLensActivity.this,AddExpenseActivity.class); 
					startActivity(myIntent); 				
				}
			}
		);    	
	}

	private void msg(String title, String msg)
	{
		AlertDialog alertDialog;
		alertDialog = new AlertDialog.Builder(this).create();
		alertDialog.setTitle(title);
		alertDialog.setMessage(msg);
		alertDialog.show();
	}
}