/**
 * 
 */
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

/**
 * @author sorin
 *
 */
public final class ExpensesView extends ListView
{
	private ExpenseFilter[] mFilters;
	private FilterType mFilterType;
	private ArrayListWithNotify<Expense> mExpenses;
	private CashLensStorage mStorage;
	
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
	
	public void initialize() throws IOException
	{
		mStorage = CashLensStorage.instance(getContext().getApplicationContext());

		AppSettings settings = AppSettings.instance(getContext().getApplicationContext());
		
		FilterType filterType = settings.getExpenseFilterType();
		if (filterType == FilterType.CUSTOM)
			setCustomFilter(settings.getLastUsedCustomExpenseFilter());
		else
			setFilterType(settings.getExpenseFilterType());
	}

	public void setFilterType(FilterType filterType)
	{
		mFilterType = filterType;
		
		if (filterType == FilterType.CUSTOM)
			mFilters = null;	// should be followed by a call to setCustomFilter()
		else
		{
			recomputeFilters();
			updateExpenses();
		}
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
		mFilters = new ExpenseFilter[1];
		mFilters[0] = filter;
		
		updateExpenses();
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
	
	protected void updateExpenses()
	{
		mExpenses = mStorage.readExpenses(mFilters);
		
		ArrayAdapterExpense adapter = new ArrayAdapterExpense(getContext(), mExpenses);
		setAdapter(adapter);
	}
}
