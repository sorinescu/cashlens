/**
 * 
 */
package com.udesign.cashlens;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.util.Log;

/**
 * @author sorin
 *
 */
public class ExpenseThumbnailCache implements Runnable
{
	protected Context mContext;
	protected Thread mRunner;
	protected HashMap<Integer,ExpenseThumbnail> mThumbs = new HashMap<Integer, ExpenseThumbnail>();
	protected LinkedList<ExpenseThumbnail> mThumbsToLoad = new LinkedList<ExpenseThumbnail>();
	protected ReentrantLock mLoadMutex = new ReentrantLock();
	protected Condition mNewThumbsToLoad = mLoadMutex.newCondition();
	
	protected static ExpenseThumbnailCache mCache = null;
	
	protected ExpenseThumbnailCache(Context context)
	{
		mContext = context;
		mRunner = new Thread(this);	// start loading thumbnails
	}
	
	public static ExpenseThumbnailCache instance(Context context)
	{
		if (mCache == null)
			mCache = new ExpenseThumbnailCache(context);

		return mCache;
	}
	
	public synchronized ExpenseThumbnail getThumbnail(int id, ExpenseThumbnail.OnLoadedListener onLoadedListener,
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
			thumb.registerOnLoadedListener(onLoadedListener, onLoadedContext);
		
		// add thumb to load list
		mLoadMutex.lock();
		mThumbsToLoad.add(thumb);
		mLoadMutex.unlock();
		
		// cache the thumbnail
		mThumbs.put(new Integer(id), thumb);
		
		return thumb;
	}
	
	public synchronized void releaseThumbnail(ExpenseThumbnail thumb)
	{
		// TODO should mark thumb as "not used", so it can be freed on OOM
	}

	public void run()
	{
		CashLensStorage storage;
		
		try
		{
			storage = CashLensStorage.instance(mContext);
		} catch (IOException e)
		{
			e.printStackTrace();
			return;
		}
		
		// extract thumbnails to load from list, one by one, load them
		// and notify listeners
		mLoadMutex.lock();
		while (mThumbsToLoad.isEmpty())
			try
			{
				mNewThumbsToLoad.await();
			} catch (InterruptedException e)
			{
				e.printStackTrace();
				return;
			}
		
		ExpenseThumbnail thumb = mThumbsToLoad.remove();
		try
		{
			storage.loadExpenseThumbnail(thumb);
			Log.w("ThumbCache.run", "Loaded thumbnail with id " + Integer.toString(thumb.id));
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		
		mLoadMutex.unlock();
	}
}
