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
import com.udesign.cashlens.CashLensStorage.ExpenseFilterType;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
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
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
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
			mInstance = new AppSettings(context.getApplicationContext());
		
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
	public ExpenseFilterType getExpenseFilterType()
	{
		int ordinal = mSharedPrefs.getInt("expenseFilterType", ExpenseFilterType.MONTH.ordinal());
		return ExpenseFilterType.values()[ordinal];
	}
	/**
	 * @param filterType the expenseFilterType to set
	 */
	public void setExpenseFilterType(ExpenseFilterType filterType)
	{
		mSharedPrefsEditor.putInt("expenseFilterType", filterType.ordinal());
		mSharedPrefsEditor.commit();
	}
	/**
	 * @return the customExpenseFilter
	 */
	public ExpenseFilter getCustomExpenseFilter()
	{
		ExpenseFilter filter = new ExpenseFilter();
		
		if (mSharedPrefs.getBoolean("customExpenseFilterStartEnabled", false))
			filter.startDate = getDate("customExpenseFilterStart");
		if (mSharedPrefs.getBoolean("customExpenseFilterEndEnabled", false))
			filter.endDate = getDate("customExpenseFilterEnd");
		
		String[] accounts = mSharedPrefs.getString("customExpenseFilterAccounts", "").split(",");
		if (accounts.length != 0 && !accounts[0].equals(""))
		{
			filter.accountIds = new int[accounts.length];
			for (int i=0; i<filter.accountIds.length; ++i)
				filter.accountIds[i] = Integer.parseInt(accounts[i]);
		}
		
		return filter;
	}
	/**
	 * @param filter the customExpenseFilter to set
	 */
	public void setCustomExpenseFilter(ExpenseFilter filter)
	{
		mSharedPrefsEditor.putBoolean("customExpenseFilterStartEnabled", 
				filter.startDate != null);
		mSharedPrefsEditor.putBoolean("customExpenseFilterEndEnabled", 
				filter.endDate != null);
		
		putDate("customExpenseFilterStart", filter.startDate);
		putDate("customExpenseFilterEnd", filter.endDate);
		
		if (filter.accountIds != null)
		{
			StringBuilder builder = new StringBuilder();
			
			builder.append(filter.accountIds[0]);
			for (int i=1; i<filter.accountIds.length; ++i)
				builder.append(",").append(filter.accountIds[i]);
			
			mSharedPrefsEditor.putString("customExpenseFilterAccounts", builder.toString());
		}
		else
			mSharedPrefsEditor.putString("customExpenseFilterAccounts", "");
		
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
	public boolean getExpenseFilterViewEnabled(ExpenseFilterType filterType) 
	{
		switch (filterType)
		{
		case MONTH:
			return mSharedPrefs.getBoolean("expenseFilterMonthEnabled", true);
		case WEEK:
			return mSharedPrefs.getBoolean("expenseFilterWeekEnabled", true);
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
	public void setExpenseFilterViewEnabled(ExpenseFilterType filterType, boolean enabled) 
	{
		switch (filterType)
		{
		case MONTH:
			mSharedPrefsEditor.putBoolean("expenseFilterMonthEnabled", enabled);
			break;
		case WEEK:
			mSharedPrefsEditor.putBoolean("expenseFilterWeekEnabled", enabled);
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
	
	/**
	 * @return the expenseTextColor
	 */
	public int getExpenseTextColor()
	{
		return mSharedPrefs.getInt("expenseTextColor", Color.WHITE);
	}

	/**
	 * @param expenseTextColor the expenseTextColor to set
	 */
	public void setExpenseTextColor(int expenseTextColor)
	{
		mSharedPrefsEditor.putInt("expenseTextColor", expenseTextColor);
		mSharedPrefsEditor.commit();
	}
	
	/**
	 * @return the expenseOutlineColor
	 */
	public int getExpenseOutlineColor()
	{
		return mSharedPrefs.getInt("expenseOutlineColor", Color.BLACK);
	}

	/**
	 * @param expenseOutlineColor the expenseOutlineColor to set
	 */
	public void setExpenseOutlineColor(int expenseOutlineColor)
	{
		mSharedPrefsEditor.putInt("expenseOutlineColor", expenseOutlineColor);
		mSharedPrefsEditor.commit();
	}
}
