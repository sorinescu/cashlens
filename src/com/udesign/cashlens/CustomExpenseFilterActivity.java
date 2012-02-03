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

import com.udesign.cashlens.CashLensStorage.Account;
import com.udesign.cashlens.CashLensStorage.ExpenseFilter;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * @author sorin
 *
 */
public final class CustomExpenseFilterActivity extends PreferenceActivity
{
	private CashLensStorage mStorage;
	private ListPreferenceMultiSelect mAccounts;
	private DatePreference mStartDate;
	private DatePreference mEndDate;
	private ArrayListWithNotify<Account> mAccountsList;
	
	/* (non-Javadoc)
	 * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.custom_expense_filter);
		
		try
		{
			mStorage = CashLensStorage.instance(this);
		} 
		catch (Exception e)
		{
			e.printStackTrace();
			finish();
		}
		
		mAccounts = (ListPreferenceMultiSelect)findPreference("customExpenseFilterAccounts");
		mStartDate = (DatePreference)findPreference("customExpenseFilterStart");
		mEndDate = (DatePreference)findPreference("customExpenseFilterEnd");
		
		mAccountsList = mStorage.getAccounts();

		String[] entries = new String[mAccountsList.size()];
		String[] entryValues = new String[mAccountsList.size()];
		int i=0;
		
		for (Account account : mAccountsList)
		{
			entries[i] = account.name;
			entryValues[i] = Integer.toString(account.id);
			++i;
		}
		
		mAccounts.setEntries(entries);
		mAccounts.setEntryValues(entryValues);
		
		updateSummaries();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onWindowFocusChanged(boolean)
	 */
	@Override
	public void onWindowFocusChanged(boolean hasFocus)
	{
		updateSummaries();
	
		super.onWindowFocusChanged(hasFocus);
	}

	private void updateSummaries()
	{
		AppSettings settings = AppSettings.instance(this);
		
		ExpenseFilter customFilter = settings.getCustomExpenseFilter();
		
		if (customFilter.accountIds != null)
			mAccounts.setSummary(Integer.toString(customFilter.accountIds.length) + 
					getResources().getString(R.string.accounts_included));
		else
			mAccounts.setSummary(getResources().getString(R.string.all_accounts_included));
		
		if (customFilter.startDate != null)
			mStartDate.setSummary(mStartDate.summaryFormatter().format(customFilter.startDate));
		else
			mStartDate.setSummary(R.string.not_set);
		
		if (customFilter.endDate != null)
			mEndDate.setSummary(mEndDate.summaryFormatter().format(customFilter.endDate));
		else
			mEndDate.setSummary(R.string.not_set);
	}
}
