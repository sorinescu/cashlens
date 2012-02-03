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

import com.udesign.cashlens.CashLensStorage.Expense;
import com.udesign.cashlens.CashLensStorage.ExpenseFilter;
import com.udesign.cashlens.CashLensStorage.ExpenseFilterType;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

public final class ExpensesView extends ListView
{
	private ExpenseFilter mCustomFilter;
	private ExpenseFilterType mFilterType = ExpenseFilterType.NONE;
	private ArrayListWithNotify<Expense> mExpenses;
	private ArrayAdapterExpense mExpensesAdapter;
	
	private CashLensStorage mStorage = CashLensStorage.instance(getContext());
	
	/**
	 * @param context
	 * @throws IOException 
	 */
	public ExpensesView(Context context) throws IOException, IllegalAccessException
	{
		super(context);
	}

	/**
	 * @param context
	 * @param attrs
	 * @throws IOException 
	 */
	public ExpensesView(Context context, AttributeSet attrs) throws IOException, IllegalAccessException
	{
		super(context, attrs);
	}

	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 * @throws IOException 
	 */
	public ExpensesView(Context context, AttributeSet attrs, int defStyle) throws IOException, IllegalAccessException
	{
		super(context, attrs, defStyle);
	}

	public ExpenseFilterType getFilterType() 
	{
		return mFilterType;
	}
	
	public void setFilterType(ExpenseFilterType filterType, ExpenseFilter customFilter)
	{
		mFilterType = filterType;
		if (filterType == ExpenseFilterType.CUSTOM)
			mCustomFilter = customFilter;
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
		if (mExpensesAdapter != null)
		{
			mExpensesAdapter.recycle();
			mExpensesAdapter = null;
		}
		
		setAdapter(null);
		mExpenses = null;
	}
	
	public void updateExpenses() throws IllegalAccessException
	{
		mStorage.setExpenseFilter(mFilterType, mCustomFilter);

		mExpenses = mStorage.readExpenses(false);
		
		if (mExpensesAdapter != null)
			mExpensesAdapter.recycle();
		
		mExpensesAdapter = new ArrayAdapterExpense(getContext(), mExpenses);
		setAdapter(mExpensesAdapter);
	}
}
