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

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

import com.udesign.cashlens.CashLensStorage.ExpenseFilter;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

final class AppSettings
{
	private static AppSettings mInstance = null;
	
	private SharedPreferences mSharedPrefs;
	private SharedPreferences.Editor mSharedPrefsEditor;
	
	public static class PictureSize
	{
		private int width;
		private int height;
		
		PictureSize(int width, int height)
		{
			this.width = width;
			this.height = height;
		}
		
		int width()
		{
			return width;
		}
		
		int height()
		{
			return height;
		}
		
		public String toString()
		{
			return Integer.toString(width) + " x " + Integer.toString(height);
		}
		
		public static PictureSize parseString(String str)
		{
			int width, height;
			
			String[] res = str.split(" x ");
			width = Integer.parseInt(res[0]);
			height = Integer.parseInt(res[1]);
			
			return new PictureSize(width, height);
		}
	}
	
	private AppSettings(Context context)
	{
//		Log.d("AppSettings", "creating AppSettings");
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(
				context.getApplicationContext());
		mSharedPrefsEditor = mSharedPrefs.edit();
	}
	
	private Date getDate(String fieldName)
	{
		String val = mSharedPrefs.getString(fieldName, "");
		if (val != null)
		{
			DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, Locale.ENGLISH);
			try
			{
				Date date = df.parse(val);
				return date;
			} catch (ParseException e)
			{
				e.printStackTrace();
				return null;
			}
		}
		
		return null;
	}
	
	private void putDate(String fieldName, Date date)
	{
		String val = "";
		if (date != null)
		{
			DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, Locale.ENGLISH);
			val = df.format(date);
		}
		
		mSharedPrefsEditor.putString(fieldName, val);
	}
	
	public static AppSettings instance(Context context)
	{
		if (mInstance == null)
			mInstance = new AppSettings(context);
		
		return mInstance;
	}
	
	/**
	 * @return the lastUsedCurrency
	 */
	public int getLastUsedCurrency()
	{
		return mSharedPrefs.getInt("lastUsedCurrency", -1);
	}
	/**
	 * @param lastUsedCurrency the lastUsedCurrency to set
	 */
	public void setLastUsedCurrency(int lastUsedCurrency)
	{
		mSharedPrefsEditor.putInt("lastUsedCurrency", lastUsedCurrency);
		mSharedPrefsEditor.commit();
	}
	/**
	 * @return the lastUsedAccount
	 */
	public int getLastUsedAccount()
	{
		return mSharedPrefs.getInt("lastUsedAccount", -1);
	}
	/**
	 * @param lastUsedAccount the lastUsedAccount to set
	 */
	public void setLastUsedAccount(int lastUsedAccount)
	{
		mSharedPrefsEditor.putInt("lastUsedAccount", lastUsedAccount);
		mSharedPrefsEditor.commit();
	}
	/**
	 * @return the expenseFilterType
	 */
	public ExpensesView.FilterType getExpenseFilterType()
	{
		int ordinal = mSharedPrefs.getInt("expenseFilterType", ExpensesView.FilterType.MONTH.ordinal());
		return ExpensesView.FilterType.values()[ordinal];
	}
	/**
	 * @param filterType the expenseFilterType to set
	 */
	public void setExpenseFilterType(ExpensesView.FilterType filterType)
	{
		mSharedPrefsEditor.putInt("expenseFilterType", filterType.ordinal());
		mSharedPrefsEditor.commit();
	}
	/**
	 * @return the lastUsedCustomExpenseFilter
	 */
	public ExpenseFilter getLastUsedCustomExpenseFilter()
	{
		ExpenseFilter filter = new ExpenseFilter();
		filter.startDate = getDate("lastUsedCustomExpenseFilterStart");
		filter.endDate = getDate("lastUsedCustomExpenseFilterEnd");
		filter.accountId = mSharedPrefs.getInt("lastUsedCustomExpenseFilterAccount", 0);
		
		return filter;
	}
	/**
	 * @param filter the lastUsedCustomExpenseFilter to set
	 */
	public void setLastUsedCustomExpenseFilter(ExpenseFilter filter)
	{
		putDate("lastUsedCustomExpenseFilterStart", filter.startDate);
		putDate("lastUsedCustomExpenseFilterEnd", filter.endDate);
		mSharedPrefsEditor.putInt("lastUsedCustomExpenseFilterAccount", filter.accountId);
		mSharedPrefsEditor.commit();
	}

	/**
	 * @return the jpegQuality
	 */
	public int getJpegQuality()
	{
		return mSharedPrefs.getInt("jpegQuality", 75);
	}

	/**
	 * @param jpegQuality the jpegQuality to set
	 */
	public void setJpegQuality(int jpegQuality)
	{
		mSharedPrefsEditor.putInt("jpegQuality", jpegQuality);
		mSharedPrefsEditor.commit();
	}
	
	/**
	 * @return the jpegPictureSize
	 */
	public PictureSize getJpegPictureSize()
	{
		String str = mSharedPrefs.getString("jpegPictureSize", "0 x 0");
//		Log.d("getJpegPictureSize", "got from prefs: " + str);
		PictureSize picSize = PictureSize.parseString(str);
//		Log.d("getJpegPictureSize", "parsed: " + picSize.toString());
		
		return picSize;
	}

	/**
	 * @param jpegPictureSize the jpegPictureSize to set
	 */
	public void setJpegPictureSize(PictureSize jpegPictureSize)
	{
//		Log.d("setJpegPictureSize", jpegPictureSize.toString());
		mSharedPrefsEditor.putString("jpegPictureSize", jpegPictureSize.toString());
		mSharedPrefsEditor.commit();
	}
	
	/**
	 * @return the filterViewsOrder
	 */
	public boolean getExpenseFilterViewEnabled(ExpensesView.FilterType filterType) 
	{
		switch (filterType)
		{
		case MONTH:
			return mSharedPrefs.getBoolean("expenseFilterMonthEnabled", true);
		case DAY:
			return mSharedPrefs.getBoolean("expenseFilterDayEnabled", true);
		case CUSTOM:
			return mSharedPrefs.getBoolean("expenseFilterCustomEnabled", true);
		default:
			return false;
		}
	}

	/**
	 * @param filterViewsOrder the filterViewsOrder to set
	 */
	public void setExpenseFilterViewEnabled(ExpensesView.FilterType filterType, boolean enabled) 
	{
		switch (filterType)
		{
		case MONTH:
			mSharedPrefsEditor.putBoolean("expenseFilterMonthEnabled", enabled);
			break;
		case DAY:
			mSharedPrefsEditor.putBoolean("expenseFilterDayEnabled", enabled);
			break;
		case CUSTOM:
			mSharedPrefsEditor.putBoolean("expenseFilterCustomEnabled", enabled);
			break;
		default:
			return;
		}
		
		mSharedPrefsEditor.commit();
	}
}
