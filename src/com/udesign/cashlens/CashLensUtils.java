/**
 * 
 */
package com.udesign.cashlens;

import java.io.IOException;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

/**
 * @author sorin
 *
 */
public final class CashLensUtils
{
	public static Display getDefaultDisplay(Context context)
	{
		return ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
	}
	
	public static Bitmap createCorrectlyRotatedBitmapIfNeeded(Bitmap bitmap, String jpegPath, float scale)
	{
		// Rotate the image if necessary; all images are shot in LANDSCAPE mode
		try
		{
			ExifInterface exif = new ExifInterface(jpegPath);
			
			int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
			Log.d("CashLensUtils", "image has orientation " + Integer.toString(orientation) + ", width " + 
					Integer.toString(bitmap.getWidth()) + ", height " + Integer.toString(bitmap.getHeight()));
			
			Matrix matrix = new Matrix();
			
			if (scale != 1.0f)
				matrix.preScale(scale, scale);

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
			
			if (orientation != 1 || scale != 1.0f)
			{
				// Create a new image with the correct (maybe rotated) width/height
				Bitmap newImage = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
				
				Log.d("CashLensUtils", "created a new image with width " + Integer.toString(newImage.getWidth()) + 
						", height " + Integer.toString(newImage.getHeight()));
	
				// Replace original image
				return newImage;
			}
		} catch (IOException e)
		{
			e.printStackTrace();
		}

		return bitmap;
	}
	
	public static int getScreenOrientation(Context context)
	{
	    Display display = getDefaultDisplay(context);
	    int orientation = Configuration.ORIENTATION_UNDEFINED;
	    if (display.getWidth() == display.getHeight())
	        orientation = Configuration.ORIENTATION_SQUARE;
	    else
	    { 
	        if(display.getWidth() < display.getHeight())
	            orientation = Configuration.ORIENTATION_PORTRAIT;
	        else 
	            orientation = Configuration.ORIENTATION_LANDSCAPE;
	    }

	    return orientation;
	}
}
