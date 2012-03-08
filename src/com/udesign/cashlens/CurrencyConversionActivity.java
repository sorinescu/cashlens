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

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.udesign.cashlens.CashLensStorage.Currency;
import com.udesign.cashlens.CashLensStorage.Expense;
import com.udesign.cashlens.CurrencyConverter.OnExchangeRatesAvailableListener;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

public final class CurrencyConversionActivity extends Activity implements 
	OnDateChangedListener, OnExchangeRatesAvailableListener
{
	Spinner mFromCurrency;
	Spinner mToCurrency;
	DatePickerWithDialog mDate;
	Spinner mService;
	EditText mAmount;
	EditText mExtraFee;
	EditText mResult;
	ViewGroup mResultLayout;
	int mResultFixedPoint;
	ProgressBar mLoadingRatesBar;
	TextView mErrorTxt;
	Button mUseBtn;
	CashLensStorage mStorage;
	CurrencyConverterCache mConvertCache;

	private static class SpinnerAdapterConverters extends ArrayAdapter<CurrencyConverter>
	{
		private LayoutInflater mInflater;
		
		public SpinnerAdapterConverters(Context context, List<CurrencyConverter> objects)
		{
			super(context, R.id.textView1, objects);
			//setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		/* (non-Javadoc)
		 * @see android.widget.ArrayAdapter#getView(int, android.view.View, android.view.ViewGroup)
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			return createResource(position, convertView, parent, R.layout.currency_converter_item);
		}

		/* (non-Javadoc)
		 * @see android.widget.ArrayAdapter#getDropDownView(int, android.view.View, android.view.ViewGroup)
		 */
		@Override
		public View getDropDownView(int position, View convertView,
				ViewGroup parent)
		{
			return createResource(position, convertView, parent, R.layout.currency_converter_dropdown_item);
		}
		
		private View createResource(int position, View convertView, 
				ViewGroup parent, int resource) 
		{
			TextView textView;

			if (convertView == null) 
				textView = (TextView)mInflater.inflate(resource, parent, false);
			else
				textView = (TextView)convertView;
			
			CurrencyConverter converter = getItem(position);
			
			textView.setCompoundDrawablesWithIntrinsicBounds(converter.icon(getContext()), null, null, null);
			textView.setText(converter.name());
			
			return textView;
		}
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.currency_conversion);
		
		try
		{
			mStorage = CashLensStorage.instance(this);
		} 
		catch (Exception e)
		{
			e.printStackTrace();
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
			finish();
		}
		
		mConvertCache = CurrencyConverterCache.instance(this);
		mConvertCache.addOnExchangeRatesAvailableListener(this);

		AppSettings settings = AppSettings.instance(this);
		
		int toCurrencyId = getIntent().getIntExtra("to_currency", 0);
		int utcDate = getIntent().getIntExtra("date", 0);
		int amount = getIntent().getIntExtra("amount", 100);
		
		if (toCurrencyId == 0)
			toCurrencyId = settings.getLastToCurrency();

		int fromCurrencyId = settings.getLastFromCurrency(); 
		String lastService = settings.getLastUsedExchangeService();
		
		Calendar cal = Calendar.getInstance();
		
		// Set it to the date from the calling activity, if specified
		if (utcDate > 0)
			cal.setTime(CashLensStorage.dateFromUTCInt(utcDate));

		mFromCurrency = (Spinner)findViewById(R.id.spinFromCurr);
		mToCurrency = (Spinner)findViewById(R.id.spinToCurr);
		mDate = (DatePickerWithDialog)findViewById(R.id.date);
		mService = (Spinner)findViewById(R.id.spinExchgRateService);
		mAmount = (EditText)findViewById(R.id.txtAmount);
		mExtraFee = (EditText)findViewById(R.id.txtFee);
		mResult = (EditText)findViewById(R.id.txtResult);
		mResultLayout = (ViewGroup)findViewById(R.id.resultLayout);
		mLoadingRatesBar = (ProgressBar)findViewById(R.id.loadingRates);
		mUseBtn = (Button)findViewById(R.id.btnUse);
		mErrorTxt = (TextView)findViewById(R.id.txtError);
		
		mDate.init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE), this);
		
		// Get list of exchange services with icons
		SpinnerAdapterConverters converters = new SpinnerAdapterConverters(this, 
				mConvertCache.getConverters());
		mService.setAdapter(converters);
		
		// Select last used exchange service, if any
		if (lastService.length() > 0)
		{
			for (int pos=0; pos < converters.getCount(); ++pos)
				if (converters.getItem(pos).name().equals(lastService))
				{
					mService.setSelection(pos);
					break;
				}
		}
		
		// Populate currencies.
		// If the destination/source currency is set by the calling activity,
		// select it, otherwise use the last used one from preferences (if any)
		populateCurrency(mToCurrency, toCurrencyId);
		populateCurrency(mFromCurrency, fromCurrencyId);
		
		// When from/to currency, service or date change, make the current exchange service
		// asynchronously load the exchange rate
		
		OnItemSelectedListener spinnerListener = new OnItemSelectedListener()
		{
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id)
			{
				if (parent == mService)
				{
					Currency toCurrency = (Currency) mToCurrency.getSelectedItem();
					Currency fromCurrency = (Currency) mFromCurrency.getSelectedItem();
					
					// repopulate currencies with the supported currencies of the service
					populateCurrency(mToCurrency, toCurrency.id);
					populateCurrency(mFromCurrency, fromCurrency.id);
				}
				
				getExchangeRatesAsync();
			}

			public void onNothingSelected(AdapterView<?> parent)
			{ }
		};
		
		mFromCurrency.setOnItemSelectedListener(spinnerListener);
		mToCurrency.setOnItemSelectedListener(spinnerListener);
		mService.setOnItemSelectedListener(spinnerListener);
		
		mUseBtn.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				AppSettings settings = AppSettings.instance(CurrencyConversionActivity.this);

				CurrencyConverter converter = (CurrencyConverter) mService.getSelectedItem();
				Currency from = (Currency)mFromCurrency.getSelectedItem();
				Currency to = (Currency)mToCurrency.getSelectedItem();
				
				settings.setLastUsedExchangeService(converter.name());
				settings.setLastFromCurrency(from.id);
				settings.setLastToCurrency(to.id);
				setResult(mResultFixedPoint);
				finish();
			}
		});
		
		TextWatcher textChanged = new TextWatcher()
		{
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{ }
			
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after)
			{ }
			
			public void afterTextChanged(Editable s)
			{
				updateConversion();
			}
		};
		
		mAmount.setText(Expense.amountToString(amount));
		
		mAmount.addTextChangedListener(textChanged);
		mExtraFee.addTextChangedListener(textChanged);
	}
	
	final static int SHOW_PROGRESS = 1;
	final static int SHOW_RESULT = 2;
	final static int SHOW_ERROR = 3;
	
	private void showProgressOrResultOrError(int which)
	{
		int visiProgress = (which == SHOW_PROGRESS) ? View.VISIBLE : View.GONE;
		int visiResult = (which == SHOW_RESULT) ? View.VISIBLE : View.GONE;
		int visiError = (which == SHOW_ERROR) ? View.VISIBLE : View.GONE;
		
		mResultLayout.setVisibility(visiResult);
		mLoadingRatesBar.setVisibility(visiProgress);
		mErrorTxt.setVisibility(visiError);
	}
	
	private void getExchangeRatesAsync()
	{
		CurrencyConverter converter = (CurrencyConverter) mService.getSelectedItem();
		
		Calendar cal = Calendar.getInstance();
		cal.set(mDate.getYear(), mDate.getMonth(), mDate.getDayOfMonth());

		Date date = cal.getTime();
		Currency from = (Currency)mFromCurrency.getSelectedItem();
		Currency to = (Currency)mToCurrency.getSelectedItem();
		
		// Fetch exchange rates if not already cached
		if (!converter.canConvert(date, from, to))
		{
			// failure means they are already queued for download,
			// so the progress bar is already active
			if (converter.getExchangeRatesAsync(date, to))
			{
				// Show user that result is unavailable
				mLoadingRatesBar.setProgress(0);
				showProgressOrResultOrError(SHOW_PROGRESS);
			}
		}
		else
			updateConversion();
	}
	
	private void setFixedPointText(EditText control, int value)
	{
		control.setText(Expense.amountToString(value));
	}

	private void populateCurrency(Spinner spinner, int selectedCurrId)
	{
		CurrencyConverter converter = (CurrencyConverter) mService.getSelectedItem();

		ArrayListWithNotify<Currency> currencies = 
				new ArrayListWithNotify<CashLensStorage.Currency>(converter.supportedCurrencies());

		Collections.sort(currencies);
		
		SpinnerAdapter adapter = new ArrayAdapterIDAndName<CashLensStorage.Currency>(this, currencies);
		
		spinner.setAdapter(adapter);

		// select currency
		if (selectedCurrId != 0)
		{
			int pos;
			for (pos=0; pos < adapter.getCount(); ++pos)
				if (adapter.getItemId(pos) == selectedCurrId)
				{
					spinner.setSelection(pos);
					break;
				}
			
			if (pos == adapter.getCount())	// not found
			{
				StringBuilder txt = new StringBuilder(getResources().getString(R.string.currency_not_supported));
				txt.append(mStorage.getCurrency(selectedCurrId).displayName());
				
				Toast.makeText(this, txt, Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	public void onDateChanged(DatePicker view, int year, int monthOfYear,
			int dayOfMonth)
	{
		getExchangeRatesAsync();
	}

	public void onExchangeRatesAvailable(CurrencyConverter converter,
			Date gmtDate, Currency baseCurrency, boolean error)
	{
		// Show user that result is available
		mLoadingRatesBar.setProgress(100);
		
		if (error)
		{
			mErrorTxt.setText(converter.getLastError());
			showProgressOrResultOrError(SHOW_ERROR);
		}
		else
		{
			showProgressOrResultOrError(SHOW_RESULT);
			updateConversion();
		}
	}
	
	private static float floatFromString(String s)
	{
		if (s.length() == 0)
			return 0.0f;
		
		if (s.matches("[.,].*"))
			return Float.parseFloat("0" + s);
		
		return Float.parseFloat(s);
	}
	
	private void updateConversion()
	{
		mResultFixedPoint = 0;
		
		CurrencyConverter converter = (CurrencyConverter) mService.getSelectedItem();
		
		Calendar cal = Calendar.getInstance();
		cal.set(mDate.getYear(), mDate.getMonth(), mDate.getDayOfMonth());

		Date date = cal.getTime();
		Currency from = (Currency)mFromCurrency.getSelectedItem();
		Currency to = (Currency)mToCurrency.getSelectedItem();
		
		// Fetch exchange rates if not already cached
		if (converter.canConvert(date, from, to))
		{
			String amountTxt = mAmount.getText().toString(); 
			String feeTxt = mExtraFee.getText().toString(); 
			int amount = (int)(floatFromString(amountTxt) * 100);
			int fee = (int)(floatFromString(feeTxt) * 100);
			
			mResultFixedPoint = converter.convert(date, from, to, fee, amount);
		}
		
		setFixedPointText(mResult, mResultFixedPoint);
		mUseBtn.setEnabled(mResultFixedPoint > 0);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		
		mConvertCache.removeOnExchangeRatesAvailableListener(this);
	}
}
