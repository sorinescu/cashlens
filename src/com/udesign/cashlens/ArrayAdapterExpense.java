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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import com.udesign.cashlens.CashLensStorage.Expense;

import android.content.Context;
import android.content.res.Configuration;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ArrayAdapterExpense extends BaseAdapter implements ExpenseThumbnail.OnExpenseThumbnailLoadedListener,
	ArrayListWithNotify.OnDataChangedListener
{
	protected ExpenseThumbnailCache mThumbCache;
	protected ArrayList<ExpenseThumbnail> mThumbs;
	protected ArrayListWithNotify<Expense> mItems;
	protected Context mContext;
	protected LayoutInflater mInflater;
	protected int mOrientation;
	protected HashMap<Integer,Integer> mTotalPerCurrency;
	protected HashMap<Integer,Integer> mTotalPerAccount;
	protected int mTotalPosition;	// the position up to which the total is correct
	
	public ArrayAdapterExpense(Context context, ArrayListWithNotify<Expense> items)
	{
		mOrientation = CashLensUtils.getScreenOrientation(context);
		
		mItems = items;
		mContext = context;
		mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		mThumbCache = ExpenseThumbnailCache.instance(context);
		mThumbs = new ArrayList<ExpenseThumbnail>();
		
		mTotalPerCurrency = new HashMap<Integer,Integer>();
		mTotalPerAccount = new HashMap<Integer,Integer>();
		invalidateSums();
		
		mItems.addOnDataChangedListener(this);
	}

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getCount()
	 */
	public int getCount()
	{
		return mItems.size();
	}

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getItem(int)
	 */
	public Object getItem(int position)
	{
		return mItems.get(position);
	}

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getItemId(int)
	 */
	public long getItemId(int position)
	{
		return mItems.get(position).id;
	}

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
	 */
	public View getView(int position, View convertView, ViewGroup parent)
	{
		RelativeLayout rowLayout;

		if (convertView == null) 
		{
			Log.d("ExpenseAdapter","Creating Inflater");
			rowLayout = (RelativeLayout)mInflater.inflate(R.layout.expense_row, parent, false);
		} 
		else
			rowLayout = (RelativeLayout)convertView;
		
		Expense expense = mItems.get(position);
		
		TextView expenseText = (TextView)rowLayout.findViewById(android.R.id.text1);
		TextView totalText = (TextView)rowLayout.findViewById(android.R.id.text2);
		
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
		
		String html = "<b>" + df.format(expense.date) + "</b>" +  "<br />";
		if (expense.description != null)
			html += expense.description + "<br />";
		
        html += expense.amountToString() + " " + expense.currencyCode() + ", " 
        	+ expense.accountName();
        
        // formatted text
		expenseText.setText(Html.fromHtml(html));
		
		String currencyName = expense.currencyCode();
		
		String[] amounts = computeSum(expense, position);
		html = expense.accountName() + ": " + amounts[1] + " " + currencyName + "<br />" +
			parent.getResources().getString(R.string.total) + " (" + 
			currencyName + "): " + amounts[0] + " " + currencyName;
		
		totalText.setText(Html.fromHtml(html));
		
		// if there's a thumbnail, override default image once thumbnail is loaded
		if (expense.thumbnailId != 0)
		{
			ImageView image = (ImageView)rowLayout.findViewById(android.R.id.icon1);
			
			ExpenseThumbnail thumb = mThumbCache.getThumbnail(expense.thumbnailId, this, image);
			mThumbs.add(thumb);
			
			try
			{
				image.setImageBitmap(thumb.asBitmap(mOrientation != Configuration.ORIENTATION_LANDSCAPE));
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		return rowLayout;
	}

	public void onExpenseThumbnailLoaded(ExpenseThumbnail thumb, Object context)
	{
		ImageView image = (ImageView)context;
	
		Log.d("ExpenseAdapter", "thumb " + Integer.toString(thumb.id) + " loaded");
		
		try
		{
			image.setImageBitmap(thumb.asBitmap(mOrientation != Configuration.ORIENTATION_LANDSCAPE));
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void onDataChanged()
	{
		Log.d("onDataChanged", "invalidating sums and releasing thumbnails");
		
		invalidateSums();
		releaseThumbnails();
		
		notifyDataSetChanged();
	}
	
	protected Integer getTotalPerAccount(int accountId)
	{
		Integer key = new Integer(accountId);
		Integer val = mTotalPerAccount.get(key);
		if (val == null)
		{
			val = new Integer(0);
			mTotalPerAccount.put(key, val);
		}
		
		return val;
	}
	
	protected Integer getTotalPerCurrency(int currencyId)
	{
		Integer key = new Integer(currencyId);
		Integer val = mTotalPerCurrency.get(key);
		if (val == null)
		{
			val = new Integer(0);
			mTotalPerCurrency.put(key, val);
		}
		
		return val;
	}
	
	// Returns a string representation of the sum
	protected String[] computeSum(Expense expenseToAdd, int position)
	{
		String[] res = new String[2];

		// We need to recompute the sums ins this case; we can only compute forward
		if (position < mTotalPosition)
			invalidateSums();
		
		Log.d("computeSum", "computing from position " + Integer.toString(mTotalPosition + 1) + 
				" to position " + Integer.toString(position));
		
		for (int i = mTotalPosition + 1; i < position; ++i)
		{
			Expense expense = mItems.get(i);
			int currencyId = expense.currencyId();
			Integer totalPerAccount = getTotalPerAccount(expense.accountId);
			Integer totalPerCurrency = getTotalPerCurrency(currencyId);
			
			totalPerCurrency += expense.amount;
			mTotalPerCurrency.put(new Integer(currencyId), totalPerCurrency);
			
			totalPerAccount += expense.amount;
			mTotalPerAccount.put(new Integer(expense.accountId), totalPerAccount);
		}
		
		int currencyId = expenseToAdd.currencyId();
		Integer totalPerAccount = getTotalPerAccount(expenseToAdd.accountId);
		Integer totalPerCurrency = getTotalPerCurrency(currencyId);
		
		if (position > mTotalPosition)	// can also be == mTotalPosition, in which case it's already added
		{
			totalPerCurrency += expenseToAdd.amount;
			mTotalPerCurrency.put(new Integer(currencyId), totalPerCurrency);
	
			totalPerAccount += expenseToAdd.amount;		// should also update the stored value in the map
			mTotalPerAccount.put(new Integer(expenseToAdd.accountId), totalPerAccount);
			
			mTotalPosition = position;
		}
		
		res[0] = Expense.amountToString(totalPerCurrency);
		res[1] = Expense.amountToString(totalPerAccount);
		
		return res;
	}
	
	protected void invalidateSums()
	{
		mTotalPerCurrency.clear();
		mTotalPerAccount.clear();
		mTotalPosition = -1;
	}
	
	public void recycle()
	{
		mItems.removeOnDataChangedListener(this);
		
		releaseThumbnails();
	}
	
	protected void releaseThumbnails()
	{
		// We were automatically registered when we got the thumbnail from the cache
		for (ExpenseThumbnail thumb : mThumbs)
			thumb.unregisterOnLoadedListener(this);
		
		mThumbs.clear();
	}
}
