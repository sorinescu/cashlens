/**
 * 
 */
package com.udesign.cashlens;

import java.io.IOException;

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

/**
 * @author sorin
 *
 */
public class ArrayAdapterExpense extends BaseAdapter implements ExpenseThumbnail.OnExpenseThumbnailLoadedListener,
	ArrayListWithNotify.OnDataChangedListener
{
	protected ExpenseThumbnailCache mThumbCache;
	protected ArrayListWithNotify<Expense> mItems;
	protected Context mContext;
	protected LayoutInflater mInflater;
	protected int mOrientation;
	
	public ArrayAdapterExpense(Context context, ArrayListWithNotify<Expense> items)
	{
		mOrientation = CashLensUtils.getScreenOrientation(context);
		
		mItems = items;
		mContext = context;
		mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mThumbCache = ExpenseThumbnailCache.instance(context.getApplicationContext());
		
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
		
		TextView textView = (TextView)rowLayout.findViewById(android.R.id.text1);
		
		String html = "<b>" + expense.date.toLocaleString() + "</b>" +  "<br />";
		if (expense.description != null)
			html += expense.description + "<br />";
		
        html += expense.amountToString() + " " + expense.currencyName();
        
        // formatted text
		textView.setText(Html.fromHtml(html));
		
		// if there's a thumbnail, override default image once thumbnail is loaded
		if (expense.thumbnailId != 0)
		{
			ImageView image = (ImageView)rowLayout.findViewById(android.R.id.icon1);
			
			ExpenseThumbnail thumb = mThumbCache.getThumbnail(expense.thumbnailId, this, image);
			// TODO remove self from onLoad listener of thumbnail when activity is destroyed
			
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
		notifyDataSetChanged();
	}
}
