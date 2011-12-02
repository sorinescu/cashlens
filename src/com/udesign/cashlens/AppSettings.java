/**
 * 
 */
package com.udesign.cashlens;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * @author sorin
 *
 */
class AppSettings
{
	private static AppSettings mInstance = null;
	
	private int lastUsedCurrency;
	private int lastUsedAccount;
	
	private SharedPreferences.Editor mSharedPrefsEditor;
	
	private AppSettings(Context context)
	{
		SharedPreferences sharedPrefs = context.getSharedPreferences("CashLensSharedPrefs", 0);
		mSharedPrefsEditor = sharedPrefs.edit();
		
		lastUsedCurrency = sharedPrefs.getInt("lastUsedCurrency", -1);
		lastUsedAccount = sharedPrefs.getInt("lastUsedAccount", -1);
	}
	
	public static AppSettings instance(Context context)
	{
		if (mInstance == null)
			mInstance = new AppSettings(context);
		
		return mInstance;
	}
	
	/**
	 * @return the lastUsedCurrency
	 */
	public int getLastUsedCurrency()
	{
		return lastUsedCurrency;
	}
	/**
	 * @param lastUsedCurrency the lastUsedCurrency to set
	 */
	public void setLastUsedCurrency(int aLastUsedCurrency)
	{
		lastUsedCurrency = aLastUsedCurrency;
		mSharedPrefsEditor.putInt("lastUsedCurrency", lastUsedCurrency);
		mSharedPrefsEditor.commit();
	}
	/**
	 * @return the lastUsedAccount
	 */
	public int getLastUsedAccount()
	{
		return lastUsedAccount;
	}
	/**
	 * @param lastUsedAccount the lastUsedAccount to set
	 */
	public void setLastUsedAccount(int aLastUsedAccount)
	{
		lastUsedAccount = aLastUsedAccount;
		mSharedPrefsEditor.putInt("lastUsedAccount", lastUsedAccount);
		mSharedPrefsEditor.commit();
	}
	
}
