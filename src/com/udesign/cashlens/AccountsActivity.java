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

import com.udesign.cashlens.CashLensStorage.Account;
import com.udesign.cashlens.CashLensStorage.Currency;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

public final class AccountsActivity extends Activity 
{
	private CashLensStorage mStorage;
	private ListView mAccountsList;
	private TextView mNoAccountsText;
	
	private static class ArrayAdapterAccount extends ArrayAdapterIDAndName<Account>
	{
		public ArrayAdapterAccount(Context context, ArrayListWithNotify<Account> items)
		{
			super(context, items);
		}

		/* (non-Javadoc)
		 * @see com.udesign.cashlens.ArrayAdapterIDAndName#getView(int, android.view.View, android.view.ViewGroup)
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			TextView textView;

			if (convertView == null) 
			{
				Log.d("AccountAdapter","Creating Inflater");
				textView = (TextView)mInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
			} 
			else
				textView = (TextView)convertView;
			
			Account account = mItems.get(position);
			
			String html = "<big>" + account.name + "</big>" +  "<br/>";
			
			Currency currency = account.getCurrency();
	        html += "<small>" + currency.fullName() + "</small>" + "<br/>"; 
	        
	        html += "<small>" + parent.getContext().getString(R.string.month_start) + ": " 
	        		+ Integer.toString(account.monthStartDay) + "</small>";
	        
	        // formatted text
			textView.setText(Html.fromHtml(html));
			
			return textView;
		}
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.accounts);
		
		try
		{
			mStorage = CashLensStorage.instance(this);
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		mAccountsList = (ListView)findViewById(R.id.lstAccounts);
		mNoAccountsText = (TextView)findViewById(android.R.id.text1);

		ArrayAdapterAccount adapter = new ArrayAdapterAccount(this, mStorage.getAccounts());
		mAccountsList.setAdapter(adapter);
		
		// If there are no more accounts following a delete, show "No accounts" text
		// instead of list, and viceversa
		mAccountsList.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener()
		{
			public void onChildViewRemoved(View parent, View child)
			{
				updateViewIfNoAccounts();
			}
			
			public void onChildViewAdded(View parent, View child)
			{
				updateViewIfNoAccounts();
			}
		});

		mAccountsList.setOnItemClickListener(new OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id)
			{
				Account account = (Account)mAccountsList.getItemAtPosition(position);
				
				Intent myIntent = new Intent(AccountsActivity.this, AddEditAccount.class);
				myIntent.putExtra("account_id", account.id);
				startActivity(myIntent);
			}
		});
		
		updateViewIfNoAccounts();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.accounts_menu, menu);
		return true;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
	    // Handle item selection
	    switch (item.getItemId()) 
	    {
	    case R.id.addAccount:
			Intent myIntent = new Intent(this,
					AddEditAccount.class);
			startActivity(myIntent);
	        return true;
	        
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	private void updateViewIfNoAccounts()
	{
		if (mAccountsList.getChildCount() == 0)
		{
			mAccountsList.setVisibility(View.INVISIBLE);
			mNoAccountsText.setVisibility(View.VISIBLE);
		}
		else
		{
			mNoAccountsText.setVisibility(View.INVISIBLE);
			mAccountsList.setVisibility(View.VISIBLE);
		}
	}
}
