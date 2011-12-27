/**
 * 
 */
package com.udesign.cashlens;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

/**
 * @author sorin
 *
 */
public class ExpenseThumbnail
{
	protected Context mContext;
	protected Bitmap mBitmap = null;
	protected ArrayList<OnLoadedListenerCallback> mOnLoadedListeners = new ArrayList<OnLoadedListenerCallback>();
	
	public int id;
	
	public ExpenseThumbnail(Context context, int id)
	{
		mContext = context;
		this.id = id;
	}

	public static interface OnExpenseThumbnailLoadedListener {
		public void onExpenseThumbnailLoaded(ExpenseThumbnail thumb, Object context);
	}
	
	protected static class OnLoadedListenerCallback {
		public OnLoadedListenerCallback(OnExpenseThumbnailLoadedListener listener2,
				Object context2)
		{
			listener = listener2;
			context = context2;
		}
		
		OnExpenseThumbnailLoadedListener listener;
		Object context;
	}
	
	public void decodeFromByteArray(byte[] data) throws IOException
	{
		Bitmap thumb = BitmapFactory.decodeByteArray(data, 0, data.length);
		if (thumb == null)
			throw new IOException("Couldn't decode thumbnail");
		
		mBitmap = thumb;
	}
	
	public synchronized void registerOnLoadedListener(OnExpenseThumbnailLoadedListener listener, Object context)
	{
		// make sure we don't have duplicate entries
		unregisterOnLoadedListener(listener, context);
		
		mOnLoadedListeners.add(new OnLoadedListenerCallback(listener, context));
	}
	
	public synchronized void unregisterOnLoadedListener(OnExpenseThumbnailLoadedListener listener, Object context)
	{
		for (Iterator<OnLoadedListenerCallback> iter = mOnLoadedListeners.iterator(); iter.hasNext();)
		{
			OnLoadedListenerCallback callback = iter.next();
			if (callback.listener == listener && callback.context == context)
				iter.remove();
		}
	}
	
	public synchronized void notifyOnLoadedListeners()
	{
		for (OnLoadedListenerCallback callback : mOnLoadedListeners)
		{
			Log.d("Thumbnail", "thumb id " + Integer.toString(id) + " notifying load listener " + callback.listener.toString());
			callback.listener.onExpenseThumbnailLoaded(this, callback.context);
		}
	}

	public Bitmap asBitmap() throws IOException
	{
		createBitmapIfNeeded();
		return mBitmap;
	}
	
	public static byte[] createFromJPEG(Context context, byte[] jpegData) throws IOException
	{
		Bitmap jpeg = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
		if (jpeg == null)
			throw new IOException("Couldn't decode JPEG");
		
		DisplayMetrics metrics = new DisplayMetrics();
		((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);

		int width = Math.max(metrics.widthPixels, metrics.heightPixels);
		int height = Math.min(metrics.widthPixels, metrics.heightPixels) / 10;
		
		// scale thumbnail
		Bitmap thumb = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(thumb);
		
		float scale = Math.min(jpeg.getWidth() / width, jpeg.getHeight() / height);
		int realWidth = (int)(jpeg.getWidth() - scale * width);
		int realHeight = (int)(jpeg.getHeight() - scale * height);

		Rect srcRect = new Rect();
		srcRect.top = realHeight / 2; 
		srcRect.bottom = srcRect.top + realHeight;
		srcRect.left = realWidth / 2;
		srcRect.right = srcRect.left + realWidth;
		
		// copy the central part of the JPEG; that's where the focus should be
		canvas.drawBitmap(jpeg, srcRect, new Rect(0, 0, width, height), null);
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		thumb.compress(Bitmap.CompressFormat.JPEG, 50, out);
		
		return out.toByteArray();
	}

	protected void createBitmapIfNeeded() throws IOException
	{
		if (mBitmap != null)
			return;
		
		mBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.camera);
		if (mBitmap == null)
			throw new IOException("Couldn't load default thumbnail");
	}
}
