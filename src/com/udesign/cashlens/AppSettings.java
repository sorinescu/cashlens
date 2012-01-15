/**
 * 
 */
package com.udesign.cashlens;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

import com.udesign.cashlens.CashLensStorage.ExpenseFilter;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * @author sorin
 *
 */
final class AppSettings
{
	private static AppSettings mInstance = null;
	
	private int lastUsedCurrency;
	private int lastUsedAccount;
	private ExpenseFilter lastUsedCustomExpenseFilter;
	private ExpensesView.FilterType expenseFilterType;
	
	private SharedPreferences mSharedPrefs;
	
	private AppSettings(Context context)
	{
		mSharedPrefs = context.getSharedPreferences("CashLensSharedPrefs", 0);
		
		lastUsedCurrency = mSharedPrefs.getInt("lastUsedCurrency", -1);
		lastUsedAccount = mSharedPrefs.getInt("lastUsedAccount", -1);
		
		lastUsedCustomExpenseFilter = new ExpenseFilter();
		lastUsedCustomExpenseFilter.startDate = getDate("lastUsedCustomExpenseFilterStart");
		lastUsedCustomExpenseFilter.endDate = getDate("lastUsedCustomExpenseFilterEnd");
		lastUsedCustomExpenseFilter.accountId = mSharedPrefs.getInt("lastUsedCustomExpenseFilterAccount", 0);
		
		int ordinal = mSharedPrefs.getInt("expenseFilterType", ExpensesView.FilterType.MONTH.ordinal());
		expenseFilterType = ExpensesView.FilterType.values()[ordinal];
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
		
		mSharedPrefs.edit().putString(fieldName, val);
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
		return lastUsedCurrency;
	}
	/**
	 * @param lastUsedCurrency the lastUsedCurrency to set
	 */
	public void setLastUsedCurrency(int aLastUsedCurrency)
	{
		lastUsedCurrency = aLastUsedCurrency;
		mSharedPrefs.edit().putInt("lastUsedCurrency", lastUsedCurrency);
		mSharedPrefs.edit().commit();
	}
	/**
	 * @return the lastUsedAccount
	 */
	public int getLastUsedAccount()
	{
		return lastUsedAccount;
	}
	/**
	 * @param lastUsedAccount the lastUsedAccount to set
	 */
	public void setLastUsedAccount(int aLastUsedAccount)
	{
		lastUsedAccount = aLastUsedAccount;
		mSharedPrefs.edit().putInt("lastUsedAccount", lastUsedAccount);
		mSharedPrefs.edit().commit();
	}
	/**
	 * @return the expenseFilterType
	 */
	public ExpensesView.FilterType getExpenseFilterType()
	{
		return expenseFilterType;
	}
	/**
	 * @param filterType the expenseFilterType to set
	 */
	public void setExpenseFilterType(ExpensesView.FilterType filterType)
	{
		expenseFilterType = filterType;
		mSharedPrefs.edit().putInt("expenseFilterType", filterType.ordinal());
		mSharedPrefs.edit().commit();
	}
	/**
	 * @return the lastUsedCustomExpenseFilter
	 */
	public ExpenseFilter getLastUsedCustomExpenseFilter()
	{
		return lastUsedCustomExpenseFilter;
	}
	/**
	 * @param filter the lastUsedCustomExpenseFilter to set
	 */
	public void setLastUsedCustomExpenseFilter(ExpenseFilter filter)
	{
		lastUsedCustomExpenseFilter = filter;
		putDate("lastUsedCustomExpenseFilterStart", filter.startDate);
		putDate("lastUsedCustomExpenseFilterEnd", filter.endDate);
		mSharedPrefs.edit().putInt("lastUsedCustomExpenseFilterAccount", filter.accountId);
		mSharedPrefs.edit().commit();
	}
}
