/**
 * 
 */
package com.udesign.cashlens;

import java.io.IOException;

import com.udesign.cashlens.CashLensStorage.Account;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

/**
 * @author sorin
 *
 */
public class AccountsExpandableListAdapter extends BaseExpandableListAdapter
{
	Context mContext;
	CashLensStorage mStorage;
	ArrayAdapterIDAndName<Account> mAccountsAdapter;
	
	private static final int ID_POS = 0;
	private static final int NAME_POS = 1;
	private static final int FIELDS_COUNT = 2;

	/**
	 * 
	 */
	public AccountsExpandableListAdapter(Context context)
	{
		mContext = context;
		
		try
		{
			mStorage = CashLensStorage.instance(context);
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		mAccountsAdapter = mStorage.accountsAdapter(context);
		mAccountsAdapter.registerDataSetObserver(new DataSetObserver()
		{
			@Override
			public void onChanged()
			{
				// let listeners know the data has changed
				notifyDataSetChanged();
			}
		});
	}

	/* (non-Javadoc)
	 * @see android.widget.ExpandableListAdapter#getChild(int, int)
	 */
	public Object getChild(int groupPosition, int childPosition)
	{
		Account account = (Account)mAccountsAdapter.getItem(groupPosition);
		
		if (account == null)
			return null;
		
		switch (childPosition)
		{
		case ID_POS:
			return account.id;
		case NAME_POS:
			return account.name;
		default:
			return "???";
		}
	}

	public long getChildId(int groupPosition, int childPosition)
	{
		return childPosition;
	}

	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent)
	{
        Account account = (Account)mAccountsAdapter.getItem(groupPosition);
        
        if (convertView == null) 
        {
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(android.R.layout.simple_list_item_1, null);
        }
        
        TextView tv = (TextView)convertView.findViewById(android.R.id.text1);
        tv.setMinimumHeight(1);	// both of these are necessary (?!)
        tv.setMinHeight(1);
        tv.setTextAppearance(mContext, android.R.style.TextAppearance_Medium);
        
        switch (childPosition)
        {
        case ID_POS:
            tv.setText("ID: " + Integer.toString(account.id));
        	break;
        case NAME_POS:
            tv.setText("Name: " + account.name);
        	break;
        default:
            tv.setText("???");
        }

        return convertView;
	}

	public int getChildrenCount(int groupPosition)
	{
		return FIELDS_COUNT; 
	}

	public Object getGroup(int groupPosition)
	{
        return mAccountsAdapter.getItem(groupPosition);
	}

	public int getGroupCount()
	{
        return mAccountsAdapter.getCount();
	}

	public long getGroupId(int groupPosition)
	{
        Account account = (Account)mAccountsAdapter.getItem(groupPosition);
        
        return account.id;
	}

	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent)
	{
        Account account = (Account)mAccountsAdapter.getItem(groupPosition);
        
        if (convertView == null) 
        {
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(android.R.layout.simple_expandable_list_item_1, null);
        }
        
        TextView tv = (TextView)convertView.findViewById(android.R.id.text1);
        tv.setText(account.name);
        
        return convertView;
	}

	public boolean hasStableIds()
	{
		return true;
	}

	public boolean isChildSelectable(int groupPosition, int childPosition)
	{
		return false;
	}
}
