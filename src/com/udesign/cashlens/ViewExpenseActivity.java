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
			{
				mImage = BitmapFactory.decodeFile(mExpense.imagePath);

				// Rotate the image if necessary; all images are shot in LANDSCAPE mode
				try
				{
					ExifInterface exif = new ExifInterface(mExpense.imagePath);
					
					int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
					Log.d("ExpenseImage", "image has orientation " + Integer.toString(orientation) + ", width " + 
							Integer.toString(mImage.getWidth()) + ", height " + Integer.toString(mImage.getHeight()));
					
					Matrix matrix = new Matrix();
	
					// From http://sylvana.net/jpegcrop/exif_orientation.html
					// For convenience, here is what the letter F would look like if it were tagged correctly 
					// and displayed by a program that ignores the orientation tag (thus showing the stored image):
					//   (1)       2      (3)      4         5          (6)          7         (8)
					//
					//	888888  888888      88  88      8888888888  88                  88  8888888888
					//	88          88      88  88      88  88      88  88          88  88      88  88
					//	8888      8888    8888  8888    88          8888888888  8888888888          88
					//	88          88      88  88
					//	88          88  888888  888888
	
					if (orientation == 3)
						matrix.postRotate(180);
					else if (orientation == 6)
						matrix.postRotate(90);
					else if (orientation == 8)
						matrix.postRotate(-90);
					
					if (orientation != 1)
					{
						// Create a new image with the correct (maybe rotated) width/height
						Bitmap newImage = Bitmap.createBitmap(mImage, 0, 0, mImage.getWidth(), mImage.getHeight(), matrix, true);
						
						Log.d("ExpenseImage", "created a new image with width " + Integer.toString(newImage.getWidth()) + 
								", height " + Integer.toString(newImage.getHeight()));
			
						// Replace original image and release it
						mImage = newImage;
					}
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			
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
