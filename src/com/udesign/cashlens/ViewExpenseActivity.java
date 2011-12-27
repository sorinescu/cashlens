/**
 * 
 */
package com.udesign.cashlens;

import java.io.IOException;

import com.udesign.cashlens.CashLensStorage.Expense;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.widget.ImageView;
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
	protected ImageView mImage;
	protected TextView mText;
	
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
		
		mImage = (ImageView)findViewById(android.R.id.background);
		mText = (TextView)findViewById(android.R.id.text1);
		
		if (mExpense.imagePath != null)
		{
			Bitmap image = BitmapFactory.decodeFile(mExpense.imagePath);

			// rotate the image if necessary
			try
			{
				ExifInterface exif = new ExifInterface(mExpense.imagePath);
				
				int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
				Log.w("ExpenseImage", "image has orientation " + Integer.toString(orientation) + ", width " + 
						Integer.toString(image.getWidth()) + ", height " + Integer.toString(image.getHeight()));
				
				if (orientation != 1)
				{
					Matrix matrix = new Matrix();
					
					if (orientation == 3)
						matrix.postRotate(180);
					else if (orientation == 6)
						matrix.postRotate(90);
					else if (orientation == 8)
						matrix.postRotate(-90);
					image = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
				}
			} catch (IOException e)
			{
				e.printStackTrace();
			}
			
			mImage.setImageBitmap(image);
		}
		
		String html = "<b>" + mExpense.date.toLocaleString() + "</b>" +  "<br />";
		if (mExpense.description != null)
			html += mExpense.description + "<br />";
		
        html += mExpense.amountToString() + " " + mExpense.currencyName();
        
        // formatted text
		mText.setText(Html.fromHtml(html));
	}

}
