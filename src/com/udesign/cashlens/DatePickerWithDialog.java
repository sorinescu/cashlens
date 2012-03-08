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

import java.text.DateFormat;
import java.util.Calendar;

import android.app.DatePickerDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DatePickerWithDialog extends LinearLayout
{
	private TextView mTxtDate;
	private ImageButton mBtnSet;
	private int mYear;
	private int mMonth;
	private int mDay;
	private Calendar mCal;
	private DatePicker.OnDateChangedListener mOnDateChangedListener;
	private DateFormat mDateFmt;
	
	public DatePickerWithDialog(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initialize();
	}

	public DatePickerWithDialog(Context context)
	{
		super(context);
		initialize();
	}
	
	public void init(int year, int monthOfYear, int dayOfMonth, DatePicker.OnDateChangedListener onDateChangedListener)
	{
		mOnDateChangedListener = onDateChangedListener;
		updateDate(year, monthOfYear, dayOfMonth);
	}
	
	public void updateDate(int year, int month, int dayOfMonth)
	{
		mYear = year;
		mMonth = month;
		mDay = dayOfMonth;
		
		mCal.set(mYear, mMonth, mDay);
		
		mTxtDate.setText(mDateFmt.format(mCal.getTime()));
	}
	
	public int getDayOfMonth()
	{
		return mDay;
	}
	
	public int getMonth()
	{
		return mMonth;
	}
	
	public int getYear()
	{
		return mYear;
	}

	private void initialize()
	{
		mCal = Calendar.getInstance();
		mCal.set(mYear, mMonth, mDay);
		mDateFmt = DateFormat.getDateInstance(DateFormat.LONG);
		
		setOrientation(HORIZONTAL);
		//setBackgroundResource(android.R.drawable.btn_default);
		
		mTxtDate = new TextView(getContext());
		mBtnSet = new ImageButton(getContext());
		
		LayoutParams dateParams = new LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		dateParams.gravity = Gravity.CENTER_VERTICAL;
		dateParams.weight = 2.0f;
		mTxtDate.setLayoutParams(dateParams);
		if (!isInEditMode())
		{
			TypedValue textAppear = new TypedValue();
			if (getContext().getTheme().resolveAttribute(android.R.attr.textAppearanceLarge, textAppear, true))
				mTxtDate.setTextAppearance(getContext(), textAppear.data);
		}
		
		// Sets date text
		updateDate(mYear, mMonth, mDay);
		
		LayoutParams btnParams = new LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		mBtnSet.setLayoutParams(btnParams);
		mBtnSet.setImageDrawable(getResources().getDrawable(R.drawable.ic_dialog_time));
		
		addView(mTxtDate);
		addView(mBtnSet);
		
		if (!isInEditMode())
			mBtnSet.setOnClickListener(new OnClickListener()
			{
				public void onClick(View v)
				{
					DatePickerDialog dlg = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener()
					{
						public void onDateSet(DatePicker view, int year, int monthOfYear,
								int dayOfMonth)
						{
							updateDate(year, monthOfYear, dayOfMonth);
							
							if (mOnDateChangedListener != null)
								mOnDateChangedListener.onDateChanged(view, year, monthOfYear, dayOfMonth);
	
						}
					}, mYear, mMonth, mDay);
					
					dlg.show();
				}
			});
	}
}
