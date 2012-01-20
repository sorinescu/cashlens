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

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ArrayAdapterIDAndName<T extends ArrayAdapterIDAndName.IDAndName> extends BaseAdapter
	implements ArrayListWithNotify.OnDataChangedListener
{
	protected ArrayListWithNotify<T> mItems;
	protected Context mContext;
	protected int mDropDownResource;
	protected LayoutInflater mInflater;
	protected int mForcedTextAppearanceResource = -1;

	public static class IDAndName
	{
		int id;
		String name;
	}

	public ArrayAdapterIDAndName(Context context, ArrayListWithNotify<T> items) 
	{
		mItems = items;
		mContext = context;
		mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		mItems.addOnDataChangedListener(this);
	}
	
	public void setTextAppearance(int resID)
	{
		mForcedTextAppearanceResource = resID;
	}
	
	public void release()
	{
		mItems.removeOnDataChangedListener(this);
	}

	public int getCount() 
	{
		return mItems.size();
	}

	public int getItemPositionById(int id)
	{
		int pos = 0;
		for (T item : mItems)
		{
			if (item.id == id)
				return pos;
			++pos;
		}
		
		return -1;	// not found
	}
	
	public Object getItem(int position) 
	{
		return mItems.get(position);
	}

	public long getItemId(int position) 
	{
		return mItems.get(position).id;
	}

	public boolean areAllItemsSelectable() 
	{
		return false;
	}

	public View getView(int position, View convertView, ViewGroup parent) 
	{
		TextView view = (TextView)createResource(position, convertView, parent,android.R.layout.simple_spinner_item);
		if (mForcedTextAppearanceResource >= 0)
			view.setTextAppearance(mContext, mForcedTextAppearanceResource);
		
		return view;
	}

	public View getDropDownView(int position, View convertView, ViewGroup parent) 
	{
		return createResource(position, convertView, parent,android.R.layout.simple_spinner_dropdown_item);
	}

	public View createResource(int position, View convertView,
			ViewGroup parent, int resource) 
	{
		View view = convertView;
		TextView textView;

		if (view == null) 
		{
			Log.w("Inside Inflate:","Creating Inflater");
			view = mInflater.inflate(resource, parent,false);
			textView = (TextView)view.findViewById(android.R.id.text1);
			view.setTag(textView);
		} 
		else
			textView = (TextView)view.getTag();
		
		textView.setText(mItems.get(position).name);
		return view;
	}

	public void onDataChanged() 
	{
		// let view know that the data has changed 
		notifyDataSetChanged();
	}
}
