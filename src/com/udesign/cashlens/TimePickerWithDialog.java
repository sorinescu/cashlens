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

import android.app.TimePickerDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

public class TimePickerWithDialog extends LinearLayout
{
	private TextView mTxtTime;
	private ImageButton mBtnSet;
	private int mHour;
	private int mMinute;
	private boolean m24HourView;
	private Calendar mCal;
	private TimePicker.OnTimeChangedListener mOnTimeChangedListener;
	private DateFormat mTimeFmt;
	
	public TimePickerWithDialog(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initialize();
	}

	public TimePickerWithDialog(Context context)
	{
		super(context);
		initialize();
	}
	
	public void setCurrentHour(Integer currentHour)
	{
		mHour = currentHour;
		updateTime();
	}
	
	public void setCurrentMinute(Integer currentMinute)
	{
		mMinute = currentMinute;
		updateTime();
	}
	
	public void setIs24HourView(Boolean is24HourView)
	{
		m24HourView = is24HourView;
	}
	
	public void setOnTimeChangedListener(TimePicker.OnTimeChangedListener onTimeChangedListener)
	{
		mOnTimeChangedListener = onTimeChangedListener;
	}
	
	private void updateTime()
	{
		mCal.set(Calendar.HOUR_OF_DAY, mHour);
		mCal.set(Calendar.MINUTE, mMinute);
		
		mTxtTime.setText(mTimeFmt.format(mCal.getTime()));
	}
	
	public Integer getCurrentHour()
	{
		return mHour;
	}
	
	public Integer getCurrentMinute()
	{
		return mMinute;
	}
	
	public boolean is24HourView()
	{
		return m24HourView;
	}
	
	private void initialize()
	{
		mCal = Calendar.getInstance();
		mTimeFmt = DateFormat.getTimeInstance(DateFormat.SHORT);
		
		setOrientation(HORIZONTAL);
		
		mTxtTime = new TextView(getContext());
		mBtnSet = new ImageButton(getContext());
		
		LayoutParams dateParams = new LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		dateParams.gravity = Gravity.CENTER_VERTICAL;
		dateParams.weight = 2.0f;
		mTxtTime.setLayoutParams(dateParams);
		
		if (!isInEditMode())
		{
			TypedValue textAppear = new TypedValue();
			if (getContext().getTheme().resolveAttribute(android.R.attr.textAppearanceLarge, textAppear, true))
				mTxtTime.setTextAppearance(getContext(), textAppear.data);
		}
		
		// Sets time text
		updateTime();
		
		LayoutParams btnParams = new LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		mBtnSet.setLayoutParams(btnParams);
		mBtnSet.setImageDrawable(getResources().getDrawable(R.drawable.ic_dialog_time));
		
		addView(mTxtTime);
		addView(mBtnSet);
		
		if (!isInEditMode())
			mBtnSet.setOnClickListener(new OnClickListener()
			{
				public void onClick(View v)
				{
					TimePickerDialog dlg = new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener()
					{
						public void onTimeSet(TimePicker view, int hourOfDay, int minute)
						{
							mHour = hourOfDay;
							mMinute = minute;
							
							updateTime();
							
							if (mOnTimeChangedListener != null)
								mOnTimeChangedListener.onTimeChanged(view, hourOfDay, minute);
						}
					}, mHour, mMinute, m24HourView);
					
					dlg.show();
				}
			});
	}
}
