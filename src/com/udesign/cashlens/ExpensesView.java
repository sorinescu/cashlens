/**
 * 
 */
package com.udesign.cashlens;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import com.udesign.cashlens.CashLensStorage.Expense;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * @author sorin
 *
 */
public class ExpensesView extends ListView
{
	private Date mStartDate = null;
	private Date mEndDate = null;
	private int[] mAccountsFilter = null;
	private List<Expense> mExpenses = null;
	private CashLensStorage mStorage = null;
	
	/**
	 * @param context
	 */
	public ExpensesView(Context context)
		{
			super(context);
			initialize(context);
		}

	/**
	 * @param context
	 * @param attrs
	 */
	public ExpensesView(Context context, AttributeSet attrs)
		{
			super(context, attrs);
			initialize(context);
		}

	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public ExpensesView(Context context, AttributeSet attrs, int defStyle)
		{
			super(context, attrs, defStyle);
			initialize(context);
		}
	
	protected void initialize(Context context)
	{
		try
		{
			mStorage = CashLensStorage.instance(context);
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * @param startDate start date, or beginning if null
	 * @param endDate end date, or now if null
	 * @param accountIDs list of accounts to use, or all if null
	 */
	public void setFilter(Date startDate, Date endDate, int[] accountIDs)
	{
		mStartDate = startDate;
		mEndDate = endDate;
		mAccountsFilter = accountIDs;
		
		// Re-read expenses
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
		mExpenses = mStorage.readExpenses(mStartDate, mEndDate, mAccountsFilter);
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), 
				android.R.layout.simple_list_item_1, expensesToStrings());
		setAdapter(adapter);
	}
}
