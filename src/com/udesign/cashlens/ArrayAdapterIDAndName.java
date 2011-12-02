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
	ArrayListWithNotify<T> mItems;
	Context mContext;
	int mDropDownResource;
	LayoutInflater mInflater;

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
		return createResource(position, convertView, parent,android.R.layout.simple_spinner_item);
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
