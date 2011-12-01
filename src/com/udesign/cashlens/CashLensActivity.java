package com.udesign.cashlens;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class CashLensActivity extends Activity
{
	private Button mAddExpenseBtn;
	private ExpensesView mExpenses; 

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mAddExpenseBtn = (Button)findViewById(R.id.addExpense);
		mExpenses = (ExpensesView)findViewById(R.id.expensesLst);

		mAddExpenseBtn.setOnClickListener(new Button.OnClickListener()
		{
			public void onClick(View v)
			{
				Intent myIntent = new Intent(CashLensActivity.this,
						AddExpenseActivity.class);
				startActivity(myIntent);
			}
		});
		
		mExpenses.setFilter(null, null, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.cash_lens_menu, menu);
		return true;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
	    // Handle item selection
	    switch (item.getItemId()) 
	    {
	    case R.id.manage_accounts:
			Intent myIntent = new Intent(CashLensActivity.this,
					AccountsActivity.class);
			startActivity(myIntent);
	        return true;
	        
	    case R.id.manage_currencies:
	    	// TODO implement
	        return true;
	        
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
}