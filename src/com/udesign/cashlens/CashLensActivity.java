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

import com.udesign.cashlens.CashLensStorage.ExpenseFilter;
import com.udesign.cashlens.CashLensStorage.ExpenseFilterType;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public final class CashLensActivity extends Activity
{
	private Button mAddExpenseBtn;
	private ViewFlipper mFlipExpenses;
	private GestureDetector mGestureDetector;
	private Animation mSlideLeftIn;
	private Animation mSlideLeftOut;
	private Animation mSlideRightIn;
    private Animation mSlideRightOut;
    private View.OnTouchListener mOnTouchListener;

    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;
	
	private static final int RESULT_CODE_SETTINGS = 1;
	private static final int RESULT_CODE_ACCOUNTS = 2;
	private static final int RESULT_CODE_CUSTOM_FILTER = 3;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mAddExpenseBtn = (Button)findViewById(R.id.addExpense);
		mFlipExpenses = (ViewFlipper)findViewById(R.id.expensesFlip);
		
        mSlideLeftIn = AnimationUtils.loadAnimation(this, R.anim.slide_left_in);
        mSlideLeftOut = AnimationUtils.loadAnimation(this, R.anim.slide_left_out);
        mSlideRightIn = AnimationUtils.loadAnimation(this, R.anim.slide_right_in);
        mSlideRightOut = AnimationUtils.loadAnimation(this, R.anim.slide_right_out);
		
		mGestureDetector = new GestureDetector(new SimpleOnGestureListener() {
			/* (non-Javadoc)
			 * @see android.view.GestureDetector.SimpleOnGestureListener#onFling(android.view.MotionEvent, android.view.MotionEvent, float, float)
			 */
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2,
					float velocityX, float velocityY) 
			{
	            try 
	            {
	                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
	                    return false;
	                
	                // right to left swipe
	                if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
	                	mFlipExpenses.setInAnimation(mSlideLeftIn);
	                	mFlipExpenses.setOutAnimation(mSlideLeftOut);
	                	mFlipExpenses.showNext();
	                }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
	                	mFlipExpenses.setInAnimation(mSlideRightIn);
	                	mFlipExpenses.setOutAnimation(mSlideRightOut);
	                	mFlipExpenses.showPrevious();
	                }
	                
	                updateCurrentExpensesView();
	            } 
	            catch (Exception e) 
	            {
	                // nothing
	            }

	            return true;
			}
		});
		
		mAddExpenseBtn.setOnClickListener(new Button.OnClickListener()
		{
			public void onClick(View v)
			{
				Intent myIntent = new Intent(CashLensActivity.this,
						AddExpenseActivity.class);
				startActivity(myIntent);
			}
		});
		
		// This will be used for all expense views and "No expenses" texts
		mOnTouchListener = new View.OnTouchListener() 
		{
			public boolean onTouch(View v, MotionEvent event) 
			{
				// let the activity handle the event
				return onTouchEvent(event);
			}
		};
		
		try {
			initializeExpenses();
			updateCurrentExpensesView();
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
			finish();	// exit app
			return;
		}
	}
	
	private void initializeExpenses() throws IOException, IllegalAccessException
	{
		AppSettings settings = AppSettings.instance(this);
		
		// First make sure old views are detached (remove dangling listeners)
		detachAllExpensesViews();
		
		// These listeners will be applied to all expense views
		OnItemClickListener onItemClickListener = new OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				if (id == 0)
					return;
				
				Log.d("ExpenseClicked", "selected expense with id " + Long.toString(id));
				
				Intent myIntent = new Intent(CashLensActivity.this,	ViewExpenseActivity.class);
				myIntent.putExtra("expense_id", (int)id);
				
				startActivity(myIntent);
			}
		};
		
		ExpenseFilterType currentFilter = settings.getExpenseFilterType();

		mFlipExpenses.removeAllViews();
		
		for (int i=0; i<ExpenseFilterType.values().length; ++i)
		{
			ExpenseFilterType filterType = ExpenseFilterType.values()[i];
			
			if (filterType == ExpenseFilterType.NONE)
				continue;	// ignore empty filter
			
			if (!settings.getExpenseFilterViewEnabled(filterType))
				continue;	// don't show disabled views
			
			ExpensesView expenses = new ExpensesView(this);
			
			// When there are no expenses to display, show "No expenses" instead of an empty list
			RelativeLayout layout = new RelativeLayout(this);
			layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			
			TextView noExpensesText = new TextView(this);
			noExpensesText.setText(R.string.no_expenses);
			noExpensesText.setGravity(Gravity.CENTER);
			
			// This is necessary to properly handle onFling
			expenses.setOnTouchListener(mOnTouchListener);
			noExpensesText.setOnTouchListener(mOnTouchListener);
			
			expenses.setEmptyView(noExpensesText);
			
			RelativeLayout.LayoutParams fillParent = new RelativeLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			fillParent.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
			fillParent.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
			fillParent.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			fillParent.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

			layout.addView(expenses, fillParent);
			layout.addView(noExpensesText, fillParent);
			
			expenses.setOnItemClickListener(onItemClickListener);

			// Initialize expense views in the order specified in settings
			ExpenseFilter customFilter = null;
			if (filterType == ExpenseFilterType.CUSTOM)
				customFilter = settings.getCustomExpenseFilter();
			
			expenses.setFilterType(filterType, customFilter);
			
			// Add view to view flipper, in the correct order
			mFlipExpenses.addView(layout);
			
			// Make the saved filter type as the visible child
			if (filterType.equals(currentFilter))
				mFlipExpenses.setDisplayedChild(mFlipExpenses.indexOfChild(layout));
		}
	}
	
	private ExpensesView getCurrentExpensesView()
	{
		RelativeLayout layout = (RelativeLayout)mFlipExpenses.getCurrentView();
		return (ExpensesView)layout.getChildAt(0);
	}
	
	private void setCurrentExpensesView(ExpenseFilterType filterType) throws IllegalAccessException
	{
		for (int i=0; i<mFlipExpenses.getChildCount(); ++i)
		{
			RelativeLayout layout = (RelativeLayout)mFlipExpenses.getChildAt(i);
			ExpensesView expensesView = (ExpensesView)layout.getChildAt(0);
			
			if (expensesView.getFilterType() == filterType)
			{
				mFlipExpenses.setDisplayedChild(i);
				updateCurrentExpensesView();
			}
		}
	}
	
	private void updateCurrentExpensesView() throws IllegalAccessException
	{
		ExpensesView expenses = getCurrentExpensesView();
		
		// Detach expenses list from all the other ExpensesViews 
		for (int i=0; i<mFlipExpenses.getChildCount(); ++i)
		{
			RelativeLayout layout = (RelativeLayout)mFlipExpenses.getChildAt(i);
			ExpensesView otherExpenses = (ExpensesView)layout.getChildAt(0);
			
			if (otherExpenses != expenses)
				otherExpenses.detachExpenses();
		}
		
		// Read expense data from db
		expenses.updateExpenses();
		
		// Save the current view filter
		AppSettings settings = AppSettings.instance(this);
		settings.setExpenseFilterType(expenses.getFilterType());

		// Show filter type in activity title
		updateTitle();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.cash_lens_menu, menu);
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
	    case R.id.manage_accounts:
			Intent manageAccounts = new Intent(CashLensActivity.this,
					AccountsActivity.class);
			startActivityForResult(manageAccounts, RESULT_CODE_ACCOUNTS);
	        return true;
	        
	    case R.id.custom_filter:
			Intent customFilter = new Intent(CashLensActivity.this,
					CustomExpenseFilterActivity.class);
			startActivityForResult(customFilter, RESULT_CODE_CUSTOM_FILTER);
	        return true;
	        
	    case R.id.settings:
			Intent settings = new Intent(CashLensActivity.this,
					SettingsActivity.class);
			startActivityForResult(settings, RESULT_CODE_SETTINGS);
	        return true;
	        
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
		
	private void updateTitle()
	{
		AppSettings settings = AppSettings.instance(this);
		String filterType = "";
		
		switch (settings.getExpenseFilterType())
		{
		case NONE:
			filterType = getResources().getString(R.string.no_filter);
			break;
		case CUSTOM:
			filterType = getResources().getString(R.string.custom_interval);
			break;
		case DAY:
			filterType = getResources().getString(R.string.current_day);
			break;
		case MONTH:
			filterType = getResources().getString(R.string.current_month);
			break;
		case WEEK:
			filterType = getResources().getString(R.string.current_week);
			break;
		}
		
		setTitle(getResources().getString(R.string.app_name) + 
				" (" + filterType + ")");
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onTouchEvent(android.view.MotionEvent)
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) 
	{
        if (mGestureDetector.onTouchEvent(event))
	        return true;

        return false;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		
		// This method is called after the Accounts or Settings activity returns.
		
		if (requestCode == RESULT_CODE_ACCOUNTS)
		{
			// If an account was modified/added/deleted, we need to recompute the
			// expense filters
			ExpensesView expenses = getCurrentExpensesView();
			try
			{
				expenses.updateExpenses();
			} catch (Exception e) {
				Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
				finish();	// exit app
				return;
			}
		}
		else if (requestCode == RESULT_CODE_SETTINGS || requestCode == RESULT_CODE_CUSTOM_FILTER)
		{
			// The user may have added/removed displayable expense views; reinitialize
			try {
				initializeExpenses();
				
				// If the custom filter has been enabled, make it the current view
				if (requestCode == RESULT_CODE_CUSTOM_FILTER)
				{
					AppSettings settings = AppSettings.instance(this);
					
					if (settings.getExpenseFilterViewEnabled(ExpenseFilterType.CUSTOM))
						setCurrentExpensesView(ExpenseFilterType.CUSTOM);	// also updates view
				}
				else
					updateCurrentExpensesView();
			} catch (Exception e) {
				Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
				finish();	// exit app
				return;
			}
		}
	}

	protected void detachAllExpensesViews()
	{
		// Detach expenses list from all ExpensesViews 
		for (int i=0; i<mFlipExpenses.getChildCount(); ++i)
		{
			RelativeLayout layout = (RelativeLayout)mFlipExpenses.getChildAt(i);
			ExpensesView expenses = (ExpensesView)layout.getChildAt(0);

			expenses.detachExpenses();
		}
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy()
	{
		detachAllExpensesViews();
		
		super.onDestroy();
	}
}
