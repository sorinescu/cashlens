/*******************************************************************************
 * Copyright 2012 Sorin Otescu <sorin.otescu@gmail.com>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.udesign.cashlens;

import java.text.DecimalFormatSymbols;

import com.udesign.cashlens.CashLensStorage.Account;
import com.udesign.cashlens.CashLensStorage.Expense;

import android.app.Activity;
import android.content.Intent;
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

public class EditExpenseActivity extends Activity
{
	protected CashLensStorage mStorage;
	protected Expense mExpense;
	protected Expense mExpenseMod;
	protected EditText mAmountTxt;
	private Spinner mAccountSpinner;
	private ArrayAdapterIDAndName<Account> mAccountsAdapter;
	protected DatePickerWithDialog mDatePicker;
	protected TimePickerWithDialog mTimePicker;
	protected Button mSaveButton;
	protected Button mDiscardButton;
	protected Button mCurrConvertButton;
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
			mStorage = CashLensStorage.instance(this);
		} catch (Exception e)
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
		mCurrConvertButton = (Button)findViewById(R.id.btnCurrencyConvert);
		mAmountTxt = (EditText)findViewById(R.id.amountTxt);
		mAccountSpinner = (Spinner)findViewById(R.id.spinAccount);
		mDatePicker = (DatePickerWithDialog)findViewById(R.id.datePicker1);
		mTimePicker = (TimePickerWithDialog)findViewById(R.id.timePicker1);
		
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
				} catch (Exception e)
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
		
		mCurrConvertButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				Intent currConv = new Intent(EditExpenseActivity.this,
						CurrencyConversionActivity.class);

				currConv.putExtra("to_currency", mExpenseMod.currencyId());
				currConv.putExtra("amount", mExpenseMod.amount);
				startActivityForResult(currConv, 0);
			}
		});
		
		updateSaveEnabled();		
	}

	void updateSaveEnabled()
	{
		mSaveButton.setEnabled(!mExpense.equals(mExpenseMod));
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy()
	{
		// Make sure the data changed listeners are unregistered
		mAccountsAdapter.releaseByActivity();
		
		super.onDestroy();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_CANCELED)
			return;
		
		// resultCode is actually converted amount (fixed point)
		mExpenseMod.amount = resultCode;
		mAmountTxt.setText(mExpenseMod.amountToString());
	}
}
