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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import com.udesign.cashlens.CashLensStorage.Currency;

abstract class CurrencyConverter
{
	public interface OnExchangeRatesAvailableListener
	{
		public void onExchangeRatesAvailable(CurrencyConverter converter, Date gmtDate, 
				Currency baseCurrency);
	}
	
	protected static class ExchangeRates
	{
		Date gmtDate;
		Currency baseCurrency;
		HashMap<Integer,Float> currencyIdToRate;
	}
	
	// Unfortunately there's no "typedef" in Java, so this must be explained.
	// Maps Date to a map of base currency IDs to a map of derived currency IDs to 
	// their rates when converted from the base currency. 
	HashMap<Date, HashMap<Integer, HashMap<Integer,Float> > > mDatesToRates = 
			new HashMap<Date, HashMap<Integer,HashMap<Integer,Float>>>();
	
	OnExchangeRatesAvailableListener mListener;
	
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
				mDatesToRates.get(gmtDate);
		if (baseCurrencyToRates == null)
			return null;
		
		return baseCurrencyToRates.get(baseCurrency.id);
	}
	
	private synchronized void addExchangeRates(ExchangeRates rates)
	{
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
		
		notifyExchangeRatesAvailable(rates.gmtDate, rates.baseCurrency);
	}
	
	public synchronized void setOnExchangeRatesAvailableListener(
			OnExchangeRatesAvailableListener listener)
	{
		mListener = listener;
	}
	
	private synchronized void notifyExchangeRatesAvailable(Date gmtDate, 
			Currency baseCurrency)
	{
		if (mListener != null)
			mListener.onExchangeRatesAvailable(this, gmtDate, baseCurrency);
	}
	
	/**
	 * 
	 * @param gmtDate
	 * @param baseCurrency
	 */
	public void getExchangeRatesAsync(Date gmtDate, Currency baseCurrency)
	{
		if (getExchangeRates(gmtDate, baseCurrency) != null)
		{
			// already cached; notify listener
			notifyExchangeRatesAvailable(gmtDate, baseCurrency);
			return;
		}
		
		ExchangeRates rates = new ExchangeRates();
		
		rates.baseCurrency = baseCurrency;
		rates.gmtDate = gmtDate;
		
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
				addExchangeRates(rates);
			}
		}.execute(rates);	// will cache the result on finish
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
		return convert(gmtDate, fromCurrency, toCurrency, amount * (100 + feePercent) / 100);
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
		// Assumes the rates exist; getExchangeRatesAsync must be called some time before this.
		HashMap<Integer,Float> rates = getExchangeRates(gmtDate, toCurrency);
		
		if (!rates.containsKey(fromCurrency.id))
			throw new InvalidParameterException("There is no conversion from " + fromCurrency.code
					+ " to " + toCurrency.code);
		
		return (int)(amount / rates.get(fromCurrency.id).floatValue());
	}
	
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
