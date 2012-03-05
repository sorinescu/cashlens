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

import java.util.ArrayList;
import java.util.Date;

import com.udesign.cashlens.CashLensStorage.Currency;
import com.udesign.cashlens.CurrencyConverter.OnExchangeRatesAvailableListener;

final class CurrencyConverterCache implements OnExchangeRatesAvailableListener
{
	private ArrayList<CurrencyConverter> mConverters = new ArrayList<CurrencyConverter>();
	private static CurrencyConverterCache mInstance;
	private ArrayList<OnExchangeRatesAvailableListener> mListeners = 
			new ArrayList<CurrencyConverter.OnExchangeRatesAvailableListener>();
	
	private CurrencyConverterCache()
	{
		// TODO add other converters (XE, Visa)
		MastercardCurrencyConverter mc = new MastercardCurrencyConverter();
		mc.setOnExchangeRatesAvailableListener(this);
		mConverters.add(mc);
	}
	
	public static CurrencyConverterCache instance()
	{
		if (mInstance == null)
			mInstance = new CurrencyConverterCache();
		
		return mInstance;
	}

	public ArrayList<CurrencyConverter> getConverters()
	{
		return mConverters;
	}
	
	public synchronized void onExchangeRatesAvailable(CurrencyConverter converter,
			Date gmtDate, Currency baseCurrency)
	{
		for (OnExchangeRatesAvailableListener listener : mListeners)
			listener.onExchangeRatesAvailable(converter, gmtDate, baseCurrency);
	}
	
	public synchronized void addOnExchangeRatesAvailableListener(
			OnExchangeRatesAvailableListener listener)
	{
		mListeners.add(listener);
	}
	
	public synchronized void removeOnExchangeRatesAvailableListener(
			OnExchangeRatesAvailableListener listener)
	{
		mListeners.remove(listener);
	}
}
