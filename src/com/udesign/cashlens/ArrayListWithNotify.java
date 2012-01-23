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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import android.util.Log;

public class ArrayListWithNotify<T> extends ArrayList<T> 
{
	private static final long serialVersionUID = 1L;
	private boolean mAutoNotify = true;
	private HashSet<OnDataChangedListener> mDataChangedListeners = new HashSet<OnDataChangedListener>();
	
	public static interface OnDataChangedListener {
		public void onDataChanged();
	}
	
	public void setAutoNotify(boolean autoNotify) {
		mAutoNotify = autoNotify;
	}
	
	public synchronized void notifyDataChanged() {
		for (OnDataChangedListener listener : mDataChangedListeners)
		{
			Log.d(this.getClass().toString(), "notifying data changed - listener " + listener.toString());
			listener.onDataChanged();
		}
	}
	
	public synchronized void addOnDataChangedListener(OnDataChangedListener listener) {
		mDataChangedListeners.add(listener);
		Log.d(this.getClass().getSimpleName(), "added on data changed listener " + listener.toString());
	}
	
	public synchronized void removeOnDataChangedListener(OnDataChangedListener listener) {
		if (mDataChangedListeners.remove(listener))
			Log.d(this.getClass().getSimpleName(), "removed on data changed listener " + listener.toString());
	}
	
	private void autoNotifyIfNeeded() {
		if (mAutoNotify)
			notifyDataChanged();
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#add(int, java.lang.Object)
	 */
	@Override
	public void add(int index, T object) {
		super.add(index, object);
		autoNotifyIfNeeded();
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#add(java.lang.Object)
	 */
	@Override
	public boolean add(T object) {
		boolean ret = super.add(object);
		autoNotifyIfNeeded();
		return ret;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#addAll(java.util.Collection)
	 */
	@Override
	public boolean addAll(Collection<? extends T> collection) {
		boolean ret = super.addAll(collection);
		autoNotifyIfNeeded();
		return ret;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#addAll(int, java.util.Collection)
	 */
	@Override
	public boolean addAll(int location, Collection<? extends T> collection) {
		boolean ret = super.addAll(location, collection);
		autoNotifyIfNeeded();
		return ret;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#clear()
	 */
	@Override
	public void clear() {
		super.clear();
		autoNotifyIfNeeded();
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#clone()
	 */
	@Override
	public Object clone() {
		Object ret = super.clone();
		autoNotifyIfNeeded();
		return ret;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#remove(int)
	 */
	@Override
	public T remove(int index) {
		T ret = super.remove(index);
		autoNotifyIfNeeded();
		return ret;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#remove(java.lang.Object)
	 */
	@Override
	public boolean remove(Object object) {
		boolean ret = super.remove(object);
		autoNotifyIfNeeded();
		return ret;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#removeRange(int, int)
	 */
	@Override
	protected void removeRange(int fromIndex, int toIndex) {
		super.removeRange(fromIndex, toIndex);
		autoNotifyIfNeeded();
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#set(int, java.lang.Object)
	 */
	@Override
	public T set(int index, T object) {
		T ret = super.set(index, object);
		autoNotifyIfNeeded();
		return ret;
	}

	/* (non-Javadoc)
	 * @see java.util.AbstractCollection#removeAll(java.util.Collection)
	 */
	@Override
	public boolean removeAll(Collection<?> collection) {
		boolean ret = super.removeAll(collection);
		autoNotifyIfNeeded();
		return ret;
	}

	public ArrayListWithNotify() {
		super();
	}

	public ArrayListWithNotify(Collection<? extends T> collection) {
		super(collection);
	}

	public ArrayListWithNotify(int capacity) {
		super(capacity);
	}
}
