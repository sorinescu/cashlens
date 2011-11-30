/**
 * 
 */
package com.udesign.cashlens;

import java.io.IOException;

import com.udesign.cashlens.CashLensStorage.Account;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupClickListener;

/**
 * @author sorin
 *
 */
public class AccountsActivity extends Activity 
{
	private CashLensStorage mStorage;
	private ExpandableListView mAccountsList;
	private Account mSelectedAccount = null;
	
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
		
		mAccountsList = (ExpandableListView)findViewById(R.id.lstAccounts);

		AccountsExpandableListAdapter adapter = new AccountsExpandableListAdapter(this);
		mAccountsList.setAdapter(adapter);
		
		mAccountsList.setOnGroupClickListener(new OnGroupClickListener()
		{
			public boolean onGroupClick(ExpandableListView parent, View v,
					int groupPosition, long id)
			{
				mSelectedAccount = (Account)mAccountsList.getItemAtPosition(groupPosition);
				
				Log.w("GroupClicked", "Selected group: " + mSelectedAccount.name);
				return false;
			}
		});
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
	    	newAccountFromUser();
	        return true;
	        
	    case R.id.delAccount:
	    	delAccountWithConfirm();
	        return true;
	        
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	protected void newAccountFromUser()
	{
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle(getString(R.string.new_account));
		alert.setMessage(getString(R.string.enter_account_name));

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				onNewAccountOK(input.getText().toString());
			}
		});

		alert.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// do nothing
			}
		});

		alert.show();
	}
	
	protected void delAccountWithConfirm()
	{
		if (mSelectedAccount == null)
			return;
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		
		alert.setTitle(getString(R.string.delete_account) + ": " + mSelectedAccount.name);
		alert.setMessage(getString(R.string.are_you_sure));

		alert.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				onDelAccountOK();
			}
		});

		alert.setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// do nothing
			}
		});

		alert.show();
	}
	
	private void onNewAccountOK(String accountName)
	{
		if (accountName == null)
			return;
		
		Account account = new Account();
		
		account.name = accountName.toString();
		try
		{
			mStorage.addAccount(account);
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void onDelAccountOK()
	{
		if (mSelectedAccount == null)
			return;
		
		try
		{
			mStorage.deleteAccount(mSelectedAccount);
			mSelectedAccount = null;
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}