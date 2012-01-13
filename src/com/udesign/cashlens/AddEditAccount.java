/**
 * 
 */
package com.udesign.cashlens;

import java.io.IOException;

import com.udesign.cashlens.CashLensStorage.Account;
import com.udesign.cashlens.CashLensStorage.Currency;

import android.app.Activity;
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

/**
 * @author sorin
 *
 */
public final class AddEditAccount extends Activity
{
	private CashLensStorage mStorage;
	private Account mAccount;
	private Button mSaveButton;
	private Button mDiscardButton;
	private EditText mAccountTxt;
	private Spinner mCurrencySpinner;
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
			mAccountTxt.getText().toString().length() > 0;

		mSaveButton.setEnabled(enabled);
	}
}
