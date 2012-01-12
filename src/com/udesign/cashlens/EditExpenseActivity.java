/**
 * 
 */
package com.udesign.cashlens;

import java.io.IOException;
import java.text.DecimalFormatSymbols;

import com.udesign.cashlens.CashLensStorage.Account;
import com.udesign.cashlens.CashLensStorage.Expense;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * @author sorin
 *
 */
public class EditExpenseActivity extends Activity
{
	protected CashLensStorage mStorage;
	protected Expense mExpense;
	protected Expense mExpenseMod;
	protected EditText mAmountTxt;
	private Spinner mAccountSpinner;
	private ArrayAdapterIDAndName<Account> mAccountsAdapter;
	protected DatePicker mDatePicker;
	protected TimePicker mTimePicker;
	protected Button mSaveButton;
	protected Button mDiscardButton;
	protected boolean mChanged;
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit_expense);
		
		int expenseId = getIntent().getIntExtra("expense_id", 0);
		if (expenseId == 0)
		{
			finish();
			return;
		}
		
		try
		{
			mStorage = CashLensStorage.instance(getApplicationContext());
		} catch (IOException e)
		{
			e.printStackTrace();
			Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		
		mExpense = mStorage.getExpense(expenseId);
		mExpenseMod = new Expense(mStorage);
		mExpenseMod.copyFrom(mExpense);
		
		Log.d("EditExpense.onCreate", "editing expense with id " + Integer.toString(expenseId) + ", resolved to " + mExpense.toString());

		mSaveButton = (Button)findViewById(R.id.btnSave);
		mDiscardButton = (Button)findViewById(R.id.btnDiscard);
		mAmountTxt = (EditText)findViewById(R.id.amountTxt);
		mAccountSpinner = (Spinner)findViewById(R.id.spinAccount);
		mDatePicker = (DatePicker)findViewById(R.id.datePicker1);
		mTimePicker = (TimePicker)findViewById(R.id.timePicker1);
		
		mAmountTxt.setText(mExpense.amountToString());
		mAmountTxt.setRawInputType(Configuration.KEYBOARD_12KEY);	// phone keypad
		mAmountTxt.addTextChangedListener(new TextWatcher()
		{
			private DecimalFormatSymbols mSymbols = new DecimalFormatSymbols();
			
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
			}
			
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after)
			{
			}
			
			public void afterTextChanged(Editable edt)
			{
		        String temp = edt.toString();
		        
		        // only allow 2 decimal places at most
		        int posDot = temp.indexOf(mSymbols.getDecimalSeparator());
		        if (posDot > 0) 
		        	if (temp.length() - posDot - 1 > 2)
		        		edt.delete(posDot + 3, posDot + 4);
		        
		        // Don't allow negative values
		        if (temp.startsWith("-"))
		        	edt.delete(0, 1);
		        
		        temp = edt.toString();
		        
		        float amount;
		        if (temp.length() > 0)
		        	amount = Float.parseFloat(temp);
		        else
		        	amount = 0;
		        
		        mExpenseMod.amount = (int)(Math.floor(amount * 100));
		        
		        updateSaveEnabled();
			}
		});
		
		mAccountsAdapter = mStorage.accountsAdapter(this);
	    mAccountSpinner.setAdapter(mAccountsAdapter);
	    mAccountSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
		{
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				mExpenseMod.accountId = (int)id;
				updateSaveEnabled();
			}

			public void onNothingSelected(AdapterView<?> parent)
			{
			}
		});
	    
	    // automatically select last used account
	    int position = mAccountsAdapter.getItemPositionById(mExpense.accountId);
	    mAccountSpinner.setSelection(position);
	    
		mDatePicker.init(mExpense.date.getYear() + 1900, mExpense.date.getMonth(), mExpense.date.getDate(),
				new DatePicker.OnDateChangedListener()
				{
					public void onDateChanged(DatePicker view, int year, int monthOfYear,
							int dayOfMonth)
					{
						mExpenseMod.date.setYear(year - 1900);
						mExpenseMod.date.setMonth(monthOfYear);
						mExpenseMod.date.setDate(dayOfMonth);
						
						updateSaveEnabled();
					}
				});
		
		mTimePicker.setCurrentHour(mExpense.date.getHours());
		mTimePicker.setCurrentMinute(mExpense.date.getMinutes());
		mTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener()
		{
			public void onTimeChanged(TimePicker view, int hourOfDay, int minute)
			{
				mExpenseMod.date.setHours(hourOfDay);
				mExpenseMod.date.setMinutes(minute);

				updateSaveEnabled();
			}
		});
		
		mSaveButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				try
				{
					mExpense.copyFrom(mExpenseMod);
					mStorage.updateExpense(mExpense);
					finish();
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		});
		
		mDiscardButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				finish();
			}
		});
		
		updateSaveEnabled();		
	}

	void updateSaveEnabled()
	{
		mSaveButton.setEnabled(!mExpense.equals(mExpenseMod));
	}
}
