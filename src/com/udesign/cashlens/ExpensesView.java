/**
 * 
 */
package com.udesign.cashlens;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import com.udesign.cashlens.CashLensStorage.Expense;

import android.content.Context;
import android.util.AttributeSet;
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
	private ArrayList<Expense> mExpenses = null;
	private CashLensStorage mStorage = null;
	
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
		
		ArrayAdapterExpense adapter = new ArrayAdapterExpense(getContext(), mExpenses);
		setAdapter(adapter);
	}
}
