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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import com.udesign.cashlens.CashLensStorage.Account;
import com.udesign.cashlens.CashLensStorage.Expense;
import com.udesign.cashlens.CashLensStorage.ExpenseFilter;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

public final class ExpensesView extends ListView
{
	private ExpenseFilter[] mFilters;
	private FilterType mFilterType = FilterType.NONE;
	private ArrayListWithNotify<Expense> mExpenses;
	
	private CashLensStorage mStorage = CashLensStorage.instance(getContext().getApplicationContext());
	
	public static enum FilterType
	{
		NONE, DAY, MONTH, CUSTOM		
	}

	/**
	 * @param context
	 * @throws IOException 
	 */
	public ExpensesView(Context context) throws IOException
	{
		super(context);
	}

	/**
	 * @param context
	 * @param attrs
	 * @throws IOException 
	 */
	public ExpensesView(Context context, AttributeSet attrs) throws IOException
	{
		super(context, attrs);
	}

	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 * @throws IOException 
	 */
	public ExpensesView(Context context, AttributeSet attrs, int defStyle) throws IOException
	{
		super(context, attrs, defStyle);
	}

	public FilterType getFilterType() 
	{
		return mFilterType;
	}
	
	public void setFilterType(FilterType filterType)
	{
		mFilterType = filterType;
		
		if (filterType == FilterType.CUSTOM)
			mFilters = null;	// should be followed by a call to setCustomFilter()
		else
			recomputeFilters();
	}
	
	protected void recomputeFilters()
	{
		Calendar cal = Calendar.getInstance();
		ArrayList<Account> accounts = mStorage.getAccounts();
		Date now = cal.getTime();
		
		switch (mFilterType)
		{
		case NONE:
			mFilters = null;
			break;
		case CUSTOM:	// applied in setCustomFilter
			break;
		case DAY:
			mFilters = new ExpenseFilter[1];
			mFilters[0] = new ExpenseFilter();
			mFilters[0].startDate = CashLensUtils.startOfDay(now);
			mFilters[0].endDate = CashLensUtils.startOfNextDay(now);
			break;
		case MONTH:
			mFilters = new ExpenseFilter[accounts.size()];
			for (int i=0; i<mFilters.length; ++i)
			{
				ExpenseFilter filter = new ExpenseFilter();
				Account account = accounts.get(i);
				
				filter.accountId = account.id;
				filter.startDate = CashLensUtils.startOfThisMonth(account.monthStartDay); 
				filter.endDate = CashLensUtils.endOfThisMonth(account.monthStartDay);
				
				mFilters[i] = filter;
			}
			break;
		}
	}

	public void setCustomFilter(ExpenseFilter filter)
	{
		mFilterType = FilterType.CUSTOM;
		mFilters = new ExpenseFilter[1];
		mFilters[0] = filter;
	}
	
	protected String[] expensesToStrings()
	{
		if (mExpenses == null)
			return null;
		
		String[] list = new String[mExpenses.size()];
		int i = 0;
		for (Expense expense : mExpenses)
			list[i++] = expense.date.toLocaleString() + " " + expense.amountToString() 
				+ " " + expense.imagePath;
		
		return list;
	}
	
	public void detachExpenses()
	{
		setAdapter(null);
		mExpenses = null;
	}
	
	public void updateExpenses()
	{
		mExpenses = mStorage.readExpenses(mFilters);
		
		ArrayAdapterExpense adapter = new ArrayAdapterExpense(getContext(), mExpenses);
		setAdapter(adapter);
	}
}
