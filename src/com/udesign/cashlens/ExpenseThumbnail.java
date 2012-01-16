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

public class ExpenseThumbnail
{
	protected Context mContext;
	protected Bitmap mBitmapPortrait = null;
	protected Bitmap mBitmapLandscape = null;
	protected ArrayList<OnLoadedListenerCallback> mOnLoadedListeners = new ArrayList<OnLoadedListenerCallback>();
	
	public int id;
	
	public static class Data
	{
		byte[] portraitData = null;
		byte[] landscapeData = null;
	}
	
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
	
	public void decodeFromByteArray(byte[] data, boolean forPortrait) throws IOException
	{
		Bitmap thumb = BitmapFactory.decodeByteArray(data, 0, data.length);
		if (thumb == null)
			throw new IOException("Couldn't decode thumbnail");
		
		if (forPortrait)
			mBitmapPortrait = thumb;
		else
			mBitmapLandscape = thumb;
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

	public Bitmap asBitmap(boolean forPortrait) throws IOException
	{
		createBitmapIfNeeded();
		return (forPortrait || mBitmapLandscape == null) ? mBitmapPortrait : mBitmapLandscape;
	}
	
	public static Data createFromJPEG(Context context, byte[] jpegData, String jpegPath) throws IOException
	{
		Bitmap origJpeg = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
		if (origJpeg == null)
			throw new IOException("Couldn't decode JPEG");
		
		DisplayMetrics metrics = new DisplayMetrics();
		CashLensUtils.getDefaultDisplay(context).getMetrics(metrics);
		
		int screenWidth = metrics.widthPixels;
		int screenHeight = metrics.heightPixels;
		
		// If we need to rotate the bitmap, do it at a smaller size to speed things up
		float scale = (float)Math.max(screenWidth, screenHeight) / Math.min(origJpeg.getWidth(), origJpeg.getHeight());
		Log.d("ExpenseThumbnail", "generating thumbnail from JPEG with scale " + Float.toString(scale));
		
		Bitmap jpeg = CashLensUtils.createCorrectlyRotatedBitmapIfNeeded(origJpeg, jpegPath, scale);
		
		Data thumbData = new Data();

		// First generate portrait thumbnail, then landscape thumbnail
		for (int i=0; i<2; ++i)
		{
			boolean forPortrait = (i == 0);
			
			int width;
			int height;
	
			if (forPortrait)
			{
				height = Math.max(screenWidth, screenHeight);
				width = Math.min(screenWidth, screenHeight);
			}
			else
			{
				width = Math.max(screenWidth, screenHeight);
				height = Math.min(screenWidth, screenHeight);
	
				// For devices with square screens, only generate portrait thumbnail 
				if (width == height)
					break;
			}
	
			// Fit 10 vertical thumbnails per screen
			height /= 10;
			
			// scale thumbnail
			Log.d("ExpenseThumbnail", "creating a thumbnail with width " + Integer.toString(width) + 
					", height " + Integer.toString(height));
	
			Bitmap thumb = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(thumb);
			
			scale = Math.min(jpeg.getWidth() / width, jpeg.getHeight() / height);
			float srcWidth = (int)(scale * width);
			float srcHeight = (int)(scale * height);
	
			Rect srcRect = new Rect();
			srcRect.top = (int)((jpeg.getHeight() - srcHeight) / 2); 
			srcRect.bottom = (int)(srcRect.top + srcHeight);
			srcRect.left = (int)((jpeg.getWidth() - srcWidth) / 2);
			srcRect.right = (int)(srcRect.left + srcWidth);
			
			// copy the central part of the JPEG; that's where the focus should be
			Log.d("ExpenseThumbnail", "copying thumbnail from " + Integer.toString(srcRect.left) + 
					"," + Integer.toString(srcRect.top) + " (" + Integer.toString(srcRect.width()) +
					"x" + Integer.toString(srcRect.height()) + ")");
			
			canvas.drawBitmap(jpeg, srcRect, new Rect(0, 0, width, height), null);
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			thumb.compress(Bitmap.CompressFormat.JPEG, 50, out);
			
			if (forPortrait)
				thumbData.portraitData = out.toByteArray();
			else
				thumbData.landscapeData = out.toByteArray();
		}
		
		return thumbData;
	}

	protected void createBitmapIfNeeded() throws IOException
	{
		if (mBitmapPortrait != null)
			return;
		
		mBitmapPortrait = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.camera);
		if (mBitmapPortrait == null)
			throw new IOException("Couldn't load default thumbnail");
	}
}
