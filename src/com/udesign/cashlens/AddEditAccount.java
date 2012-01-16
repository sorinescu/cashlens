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

import java.io.IOException;

import com.udesign.cashlens.CashLensStorage.Account;
import com.udesign.cashlens.CashLensStorage.Currency;

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
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public final class AddEditAccount extends Activity
{
	private CashLensStorage mStorage;
	private Account mAccount;
	private Button mSaveButton;
	private Button mDiscardButton;
	private EditText mAccountTxt;
	private Spinner mCurrencySpinner;
	private EditText mMonthStartTxt;
	private ArrayAdapterIDAndName<Currency> mCurrenciesAdapter;
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_edit_account);
		
		// TODO also implement Edit account (not only Add)
		mAccount = new Account();
		
		try
		{
			mStorage = CashLensStorage.instance(this);
		} 
		catch (IOException e)
		{
			e.printStackTrace();
			finish();
		}
		
		mSaveButton = (Button)findViewById(R.id.btnSave);
		mDiscardButton = (Button)findViewById(R.id.btnDiscard);
		mAccountTxt = (EditText)findViewById(R.id.accountTxt);
		mCurrencySpinner = (Spinner)findViewById(R.id.spinCurrency);
		mMonthStartTxt = (EditText)findViewById(R.id.monthStartTxt);
		
		mSaveButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				try
				{
					// TODO also implement Edit (not only Add)

					AppSettings settings = AppSettings.instance(AddEditAccount.this);
					
					long id = mCurrencySpinner.getSelectedItemId();
			        if (id != AdapterView.INVALID_ROW_ID)
			        	settings.setLastUsedCurrency((int)id);
			        
					Currency currency = (Currency)mCurrencySpinner.getSelectedItem();
					if (currency == null)
						return;
					Log.d("AddEditAccount", "selected currency is " + currency.name + ", id " + Integer.toString(currency.id));

					mAccount.currencyId = currency.id;
					mAccount.name = mAccountTxt.getText().toString();
					mAccount.monthStartDay = Integer.parseInt(mMonthStartTxt.getText().toString());
					Log.d("AddEditAccount", "account name is " + mAccount.name);

					mStorage.addAccount(mAccount);
					
					Toast.makeText(AddEditAccount.this, R.string.account_added, Toast.LENGTH_SHORT).show();
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

		mAccountTxt.addTextChangedListener(new TextWatcher()
		{
			
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
			}
			
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after)
			{
			}
			
			public void afterTextChanged(Editable s)
			{
				updateSaveEnabled();
			}
		});
		
		mMonthStartTxt.setRawInputType(Configuration.KEYBOARD_12KEY);	// phone keypad
		mMonthStartTxt.addTextChangedListener(new TextWatcher()
		{
			private String origTxt;
			private boolean recursive;
			
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
			}
			
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after)
			{
				if (!recursive)
					origTxt = mMonthStartTxt.getText().toString();
			}
			
			public void afterTextChanged(Editable s)
			{
				if (recursive)
					return;
				
		        String temp = s.toString();
		        
		        // must be between 1 and 31
		        if (temp.length() > 0)
		        {
		        	int day = Integer.parseInt(temp);
		        	if (day < 1 || day > 31)
		        	{
		        		recursive = true;
		        		s.replace(0, temp.length(), origTxt);
		        		recursive = false;
		        	}
		        }
		        
		        updateSaveEnabled();
			}
		});
		
		mCurrenciesAdapter = mStorage.currenciesAdapter(this);
		if (mCurrenciesAdapter.isEmpty())
		{
			Toast.makeText(this, R.string.no_currencies_defined, Toast.LENGTH_LONG).show();
			finish();
		}

		mCurrencySpinner.setAdapter(mCurrenciesAdapter);

	    // automatically select last used currency
		AppSettings settings = AppSettings.instance(this);
	    int position = mCurrenciesAdapter.getItemPositionById(settings.getLastUsedCurrency());
	    mCurrencySpinner.setSelection(position);
	    
	    updateSaveEnabled();
	}
	
	void updateSaveEnabled()
	{
		boolean enabled = 
			mCurrencySpinner.getSelectedItemId() != AdapterView.INVALID_ROW_ID &&
			mAccountTxt.getText().toString().length() > 0 &&
			mMonthStartTxt.getText().toString().length() > 0;

		mSaveButton.setEnabled(enabled);
	}
}
