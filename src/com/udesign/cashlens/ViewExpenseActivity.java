/**
 * 
 */
package com.udesign.cashlens;

import java.io.IOException;

import com.udesign.cashlens.CashLensStorage.Expense;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author sorin
 *
 */
public class ViewExpenseActivity extends Activity
{
	protected CashLensStorage mStorage = null;
	protected Expense mExpense = null;
	protected TouchImageView mImageView;
	protected TextView mText;
	protected Bitmap mImage = null;
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.view_expense);

		int expenseId = getIntent().getIntExtra("expense_id", 0);
		if (expenseId == 0)
		{
			finish();
			return;
		}
		
		try
		{
			mStorage = CashLensStorage.instance(getApplicationContext());
		} catch (IOException e)
		{
			e.printStackTrace();
			Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		
		mExpense = mStorage.getExpense(expenseId);
		Log.d("ViewExpense.onCreate", "viewing expense with id " + Integer.toString(expenseId) + ", resolved to " + mExpense.toString());
		
		mImageView = (TouchImageView)findViewById(android.R.id.background);
		mText = (TextView)findViewById(android.R.id.text1);
		
		if (mExpense.imagePath != null)
		{
			// Reuse the already decoded image (onCreate is called again after a configuration change) 
			Object data = getLastNonConfigurationInstance(); 

			if (data != null)
			{
				mImage = (Bitmap)data;
				Log.d("ExpenseImage", "reusing saved image: width " + Integer.toString(mImage.getWidth()) + 
						", height " + Integer.toString(mImage.getHeight()));
			}
			else
				mImage = CashLensUtils.createCorrectlyRotatedBitmapIfNeeded(BitmapFactory.decodeFile(mExpense.imagePath),
								mExpense.imagePath, 1.0f);
			
			mImageView.setImageBitmap(mImage);
		}
		
		String html = "<b>" + mExpense.date.toLocaleString() + "</b>" +  "<br />";
		if (mExpense.description != null)
			html += mExpense.description + "<br />";
		
        html += mExpense.amountToString() + " " + mExpense.currencyName();
        
        // formatted text
		mText.setText(Html.fromHtml(html));
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy()
	{
		// Try to free memory by releasing objects manually
		mImageView.getDrawable().setCallback(null);
		mImageView.setImageDrawable(null);
		
		super.onDestroy();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onRetainNonConfigurationInstance()
	 * @see http://android-developers.blogspot.com/2009/02/faster-screen-orientation-change.html
	 */
	@Override
	public Object onRetainNonConfigurationInstance()
	{
		return mImage;
	}

}
