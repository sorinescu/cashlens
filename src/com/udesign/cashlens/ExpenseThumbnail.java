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

/**
 * @author sorin
 *
 */
public class ExpenseThumbnail
{
	protected Context mContext;
	protected Bitmap mBitmap = null;
	protected ArrayList<OnLoadedListenerCallback> mOnLoadedListeners = new ArrayList<OnLoadedListenerCallback>();
	
	public static final int WIDTH = 128;
	public static final int HEIGHT = 48;
	
	public int id;
	
	public ExpenseThumbnail(Context context, int id)
	{
		mContext = context;
		this.id = id;
	}

	public static interface OnLoadedListener {
		public void OnLoaded(ExpenseThumbnail thumb, Object context);
	}
	
	protected static class OnLoadedListenerCallback {
		public OnLoadedListenerCallback(OnLoadedListener listener2,
				Object context2)
		{
			listener = listener2;
			context = context2;
		}
		
		OnLoadedListener listener;
		Object context;
	}
	
	public void createFromByteArray(byte[] data) throws IOException
	{
		Bitmap thumb = BitmapFactory.decodeByteArray(data, 0, data.length);
		if (thumb == null)
			throw new IOException("Couldn't decode thumbnail");
		
		mBitmap = thumb;
		notifyOnLoadedListeners();	// notify listeners that a new bitmap is available
	}
	
	public synchronized void registerOnLoadedListener(OnLoadedListener listener, Object context)
	{
		// make sure we don't have duplicate entries
		unregisterOnLoadedListener(listener, context);
		
		mOnLoadedListeners.add(new OnLoadedListenerCallback(listener, context));
	}
	
	public synchronized void unregisterOnLoadedListener(OnLoadedListener listener, Object context)
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
			callback.listener.OnLoaded(this, callback.context);
	}

	public Bitmap asBitmap() throws IOException
	{
		createBitmapIfNeeded();
		return mBitmap;
	}
	
	public static byte[] createFromJPEG(byte[] jpegData) throws IOException
	{
		Bitmap jpeg = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
		if (jpeg == null)
			throw new IOException("Couldn't decode JPEG");
		
		// scale thumbnail
		Bitmap thumb = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(thumb);
		
		float scale = Math.min(jpeg.getWidth() / WIDTH, jpeg.getHeight() / HEIGHT);
		int realWidth = (int)(jpeg.getWidth() - scale * WIDTH);
		int realHeight = (int)(jpeg.getHeight() - scale * HEIGHT);

		Rect srcRect = new Rect();
		srcRect.top = realHeight / 2; 
		srcRect.bottom = srcRect.top + realHeight;
		srcRect.left = realWidth / 2;
		srcRect.right = srcRect.left + realWidth;
		
		// copy the central part of the JPEG; that's where the focus should be
		canvas.drawBitmap(jpeg, srcRect, new Rect(0, 0, WIDTH, HEIGHT), null);
		
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
