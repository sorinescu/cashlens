package com.udesign.cashlens;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

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
		
		// this can generate an IOException during the initialization of ExpensesView
		// which is typically caused by a missing SD card
		try
		{
			mExpenses.initialize();
		}
		catch (Exception e)
		{
			Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
			finish();	// exit app
			return;
		}

		mAddExpenseBtn.setOnClickListener(new Button.OnClickListener()
		{
			public void onClick(View v)
			{
				Intent myIntent = new Intent(CashLensActivity.this,
						AddExpenseActivity.class);
				startActivity(myIntent);
			}
		});
		
		mExpenses.setOnItemClickListener(new OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				if (id == 0)
					return;
				
				Log.d("ExpenseClicked", "selected expense with id " + Long.toString(id));
				
				Intent myIntent = new Intent(CashLensActivity.this,	ViewExpenseActivity.class);
				myIntent.putExtra("expense_id", (int)id);
				
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