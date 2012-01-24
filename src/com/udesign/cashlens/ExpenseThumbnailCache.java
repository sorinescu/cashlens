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

import java.util.HashMap;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public final class ExpenseThumbnailCache
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
			} catch (Exception e)
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
			} catch (Exception e)
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
			Log.d("ThumbCache", "thumb id " + Integer.toString(thumb.id) + " has new load listener " 
					+ onLoadedListener.toString() + " with context " 
					+ (onLoadedContext == null ? "<null>" : onLoadedContext.toString()));
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
