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

import java.security.InvalidParameterException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Pair;

import com.udesign.cashlens.CashLensStorage.Currency;

abstract class CurrencyConverter
{
	public interface OnExchangeRatesAvailableListener
	{
		public void onExchangeRatesAvailable(CurrencyConverter converter, Date gmtDate, 
				Currency baseCurrency, boolean error);
	}
	
	protected static class ExchangeRates
	{
		Date gmtDate;
		Currency baseCurrency;
		HashMap<Integer,Float> currencyIdToRate;
		String errorMsg;
	}
	
	protected Context mAppContext;

	private String mLastError = "";
	
	// Unfortunately there's no "typedef" in Java, so this must be explained.
	// Maps Date to a map of base currency IDs to a map of derived currency IDs to 
	// their rates when converted from the base currency. 
	private HashMap<Date, HashMap<Integer, HashMap<Integer,Float> > > mDatesToRates = 
			new HashMap<Date, HashMap<Integer,HashMap<Integer,Float>>>();
	
	// Contains the pairs of Date and base currency ID for which an async load
	// has been triggered, to avoid loading them again
	private HashSet<Pair<Date, Integer> > mPendingLoads = new HashSet<Pair<Date,Integer>>();
	
	private OnExchangeRatesAvailableListener mListener;
	
	public CurrencyConverter(Context context)
	{
		mAppContext = context.getApplicationContext();
	}
	
	/**
	 * Called by the async task to retrieve the exchange rates.
	 * The gmtDate and baseCurrency parameters are inputs.
	 * The callee must fill in the currencyIdToRate map.
	 * @param rates exchange rates
	 */
	protected abstract void loadExchangeRates(ExchangeRates rates);

	private synchronized HashMap<Integer,Float> getExchangeRates(Date gmtDate, Currency baseCurrency)
	{
		HashMap<Integer, HashMap<Integer,Float>> baseCurrencyToRates = 
				mDatesToRates.get(CashLensUtils.startOfDay(gmtDate));
		if (baseCurrencyToRates == null)
			return null;
		
		return baseCurrencyToRates.get(baseCurrency.id);
	}
	
	private synchronized void addExchangeRates(ExchangeRates rates)
	{
		// rates.gmtDate must be at the beginning of the day !
		
		// first clear "loading" status
		Pair<Date,Integer> pending = new Pair<Date, Integer>(
				rates.gmtDate, rates.baseCurrency.id);
		mPendingLoads.remove(pending);
		
		HashMap<Integer, HashMap<Integer,Float>> baseCurrencyToRates = 
				mDatesToRates.get(rates.gmtDate);
		if (baseCurrencyToRates == null)
		{
			baseCurrencyToRates = new HashMap<Integer, HashMap<Integer,Float>>();
			baseCurrencyToRates.put(rates.baseCurrency.id, rates.currencyIdToRate);
			mDatesToRates.put(rates.gmtDate, baseCurrencyToRates);
		}
		else
			baseCurrencyToRates.put(rates.baseCurrency.id, rates.currencyIdToRate);
	}
	
	public synchronized void setOnExchangeRatesAvailableListener(
			OnExchangeRatesAvailableListener listener)
	{
		mListener = listener;
	}
	
	private synchronized void notifyExchangeRatesAvailable(Date gmtDate, 
			Currency baseCurrency, boolean error)
	{
		if (mListener != null)
			mListener.onExchangeRatesAvailable(this, gmtDate, baseCurrency, error);
	}
	
	/**
	 * 
	 * @param gmtDate GMT date of conversion
	 * @param baseCurrency base (destination of conversion) currency
	 * @return false if the same rates are already being retrieved, true if they have
	 *    been successfully queued for retrieval or they are already available
	 */
	public synchronized boolean getExchangeRatesAsync(Date gmtDate, Currency baseCurrency)
	{
		Date date = CashLensUtils.startOfDay(gmtDate);
		
		Pair<Date,Integer> pending = new Pair<Date,Integer>(date, baseCurrency.id);
		if (mPendingLoads.contains(pending))
			return false;	// already waiting for load
		
		if (getExchangeRates(gmtDate, baseCurrency) != null)
		{
			// already cached; notify listener
			notifyExchangeRatesAvailable(date, baseCurrency, false);
			return true;
		}
		
		mPendingLoads.add(pending);
		
		ExchangeRates rates = new ExchangeRates();
		
		rates.baseCurrency = baseCurrency;
		rates.gmtDate = date;
		
		new AsyncTask<ExchangeRates,Void,ExchangeRates>()
		{
			@Override
			protected ExchangeRates doInBackground(ExchangeRates... rates)
			{
				ExchangeRates rate = rates[0];
				
				loadExchangeRates(rate);	// synchronous
				
				return rate;
			}

			/* (non-Javadoc)
			 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
			 */
			@Override
			protected void onPostExecute(ExchangeRates rates)
			{
				// this will be executed on the UI thread
				if (rates.errorMsg == null)	// no error
					addExchangeRates(rates);
				else
					mLastError = rates.errorMsg;

				// Whether there was an error or not, notify caller 
				notifyExchangeRatesAvailable(rates.gmtDate, rates.baseCurrency, 
						rates.errorMsg != null);
			}
		}.execute(rates);	// will cache the result on finish
		
		return true;
	}
	
	public String getLastError()
	{
		return mLastError;
	}
	
	/**
	 * Verifies if the conversion is possible for the supplied parameters.
	 * If the return value is false, getExchangeRatesAsync() must be called
	 * before attempting a conversion.
	 * @param gmtDate GMT date of conversion
	 * @param fromCurrency source currency
	 * @param toCurrency destination currency
	 * @return true if the conversion is possible at this time
	 */
	public boolean canConvert(Date gmtDate, Currency fromCurrency, Currency toCurrency)
	{
		HashMap<Integer,Float> rates = getExchangeRates(gmtDate, toCurrency);
		
		return rates != null && rates.containsKey(fromCurrency.id);
	}
	
	/**
	 * Converts an amount from one currency to another, with an optional fee
	 * @param gmtDate GMT date of conversion 
	 * @param fromCurrencyId source currency
	 * @param toCurrencyId destination currency
	 * @param feePercent a fixed point percent (actual percent x100)
	 * @param amount a fixed point amount (amount x100)
	 * @return a fixed point converted amount (conversion x100)
	 */
	public int convert(Date gmtDate, Currency fromCurrency, Currency toCurrency, int feePercent, int amount)
	{
		// Assumes the rates exist; getExchangeRatesAsync must be called some time before this.
		HashMap<Integer,Float> rates = getExchangeRates(gmtDate, toCurrency);
		
		if (rates == null || !rates.containsKey(fromCurrency.id))
			throw new InvalidParameterException("There is no conversion from " + fromCurrency.code
					+ " to " + toCurrency.code);
		
		return (int)((double)amount * (10000 + feePercent) / (10000 * rates.get(fromCurrency.id).floatValue()));
	}

	/**
	 * Converts an amount from one currency to another.
	 * getExchangeRatesAsync() must be called some time before this function, to ensure that
	 * the exchange rates are available. It is advised to call this function after 
	 * receiving the onExchangeRatesAvailable() notification.
	 * @param gmtDate GMT date of conversion 
	 * @param fromCurrencyId source currency
	 * @param toCurrencyId destination currency
	 * @param amount a fixed point amount (amount x100)
	 * @return a fixed point converted amount (conversion x100)
	 */
	public int convert(Date gmtDate, Currency fromCurrency, Currency toCurrency, int amount)
	{
		return convert(gmtDate, fromCurrency, toCurrency, 0, amount);
	}
	
	public abstract Set<Currency> supportedCurrencies();
	
	/**
	 * Returns the human-friendly name of the currency converter.
	 * @return converter name
	 */
	public abstract String name();
	
	/**
	 * Returns an optional detailed description of the converter.
	 * @return converter description
	 */
	public abstract String description();
	
	/**
	 * Returns the icon of the converter. By default it returns an empty image.
	 * @return converter icon
	 */
	public Drawable icon(Context context)
	{
		return context.getResources().getDrawable(android.R.drawable.screen_background_light);
	}
}
