/*
 * Copyright (C) 2010-2011 Mike Novak <michael.novakjr@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.quietlycoding.android.picker;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;

public class NumberPickerPreference extends DialogPreference {
    private NumberPicker mPicker;
    private int mStartRange;
    private int mEndRange;
    private int mDefault;
    private int mValue;
    
    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        if (attrs == null) {
            return;
        }
        
        TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.numberpicker);
        mStartRange = arr.getInteger(R.styleable.numberpicker_startRange, 0);
        mEndRange = arr.getInteger(R.styleable.numberpicker_endRange, 200);
        mDefault = arr.getInteger(R.styleable.numberpicker_defaultValue, 0);
        mValue = mDefault;
        
        arr.recycle();
                        
        setDialogLayoutResource(R.layout.number_picker_pref);                
    }
    
    public NumberPickerPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.dialogPreferenceStyle);
    }
    
    public NumberPickerPreference(Context context) {
        this(context, null);
    }
    
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mPicker = (NumberPicker) view.findViewById(R.id.pref_num_picker);
        mPicker.setRange(mStartRange, mEndRange);
        mPicker.setCurrent(mValue);
    }
    
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        
        if (positiveResult) {
            Integer value = new Integer(mPicker.getCurrent());
            if (callChangeListener(value)) {
            	setValue(value.intValue());
            }
        }
    }
    
    public void setRange(int start, int end) {
        mPicker.setRange(start, end);
    }
    
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, mDefault);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedInt(mDefault) : ((Integer)defaultValue).intValue());
    }

    public void setValue(int val)
    {
    	mValue = val;
    	persistInt(val);
    	
    	if (mPicker != null)
    		mPicker.setCurrent(val);
    }
}

