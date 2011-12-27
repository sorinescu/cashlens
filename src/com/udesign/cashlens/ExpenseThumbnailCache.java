/**
 * 
 */
package com.udesign.cashlens;

import java.io.IOException;
import java.util.HashMap;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

/**
 * @author sorin
 *
 */
public class ExpenseThumbnailCache
{
	protected Context mContext;
	protected HashMap<Integer,ExpenseThumbnail> mThumbs = new HashMap<Integer, ExpenseThumbnail>();
	
	protected static ExpenseThumbnailCache mCache = null;

	protected class LoadThumbnailTask extends AsyncTask<ExpenseThumbnail,Void,ExpenseThumbnail>
	{
		@Override
		protected ExpenseThumbnail doInBackground(ExpenseThumbnail... thumbs)
		{
			CashLensStorage storage;
			
			try
			{
				storage = CashLensStorage.instance(mContext.getApplicationContext());
			} catch (IOException e)
			{
				e.printStackTrace();
				return null;
			}
			
			Log.d("LoadThumbnailTask", "running");
		
			ExpenseThumbnail thumb = thumbs[0];
			try
			{
				storage.loadExpenseThumbnail(thumb);
				Log.w("LoadThumbnailTask", "Loaded thumbnail with id " + Integer.toString(thumb.id));
			} catch (IOException e)
			{
				e.printStackTrace();
			}
			
			Log.d("LoadThumbnailTask", "exiting");
			return thumb;
		}

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(ExpenseThumbnail thumb)
		{
			// this will be executed on the UI thread
			thumb.notifyOnLoadedListeners();
		}
	}
	
	protected ExpenseThumbnailCache(Context context)
	{
		mContext = context;
	}
	
	public static ExpenseThumbnailCache instance(Context context)
	{
		if (mCache == null)
			mCache = new ExpenseThumbnailCache(context);

		return mCache;
	}
	
	public synchronized ExpenseThumbnail getThumbnail(int id, ExpenseThumbnail.OnExpenseThumbnailLoadedListener onLoadedListener,
			Object onLoadedContext)
	{
		ExpenseThumbnail thumb = mThumbs.get(new Integer(id));
		if (thumb != null)
		{
			if (onLoadedListener != null)
				thumb.registerOnLoadedListener(onLoadedListener, onLoadedContext);
			
			return thumb;
		}
		
		thumb = new ExpenseThumbnail(mContext, id);
	
		if (onLoadedListener != null)
		{
			thumb.registerOnLoadedListener(onLoadedListener, onLoadedContext);
			Log.d("ThumbCache", "thumb id " + Integer.toString(thumb.id) + " has new load listener " + onLoadedListener.toString());
		}

		LoadThumbnailTask task = new LoadThumbnailTask();
		task.execute(thumb);
		Log.d("ThumbCache", "added thumb id " + Integer.toString(thumb.id) + " to load list");

		// cache the thumbnail
		mThumbs.put(new Integer(id), thumb);
		Log.d("ThumbCache", "cached thumb id " + Integer.toString(thumb.id));
		
		return thumb;
	}
	
	public synchronized void releaseThumbnail(ExpenseThumbnail thumb)
	{
		// TODO should mark thumb as "not used", so it can be freed on OOM
	}
}
