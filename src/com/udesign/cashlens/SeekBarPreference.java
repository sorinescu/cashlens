/* The following code was written by Matthew Wiggins 
 * and is released under the APACHE 2.0 license 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Modified by Sorin Otescu <sorin.otescu@gmail.com>
 */
package com.udesign.cashlens;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.preference.DialogPreference;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.LinearLayout;

public class SeekBarPreference extends DialogPreference implements
		SeekBar.OnSeekBarChangeListener
{
	private static final String androidns = "http://schemas.android.com/apk/res/android";
	private static final String udesignns = "http://schemas.android.com/apk/res/com.udesign.cashlens";

	private SeekBar mSeekBar;
	private TextView mValueText;
	private Context mContext;

	private String mDialogMessage, mSuffix;
	private int mDefault, mMax, mMin, mValue = 0;

	public SeekBarPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		mContext = context;

		mDialogMessage = attrs.getAttributeValue(androidns, "dialogMessage");
		mSuffix = attrs.getAttributeValue(udesignns, "units");
		mDefault = attrs.getAttributeIntValue(androidns, "defaultValue", 0);
		mMin = attrs.getAttributeIntValue(udesignns, "min", 0);
		mMax = attrs.getAttributeIntValue(androidns, "max", 100);
	}

	@Override
	protected View onCreateDialogView()
	{
		LinearLayout.LayoutParams params;
		LinearLayout layout = new LinearLayout(mContext);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setPadding(6, 6, 6, 6);

		if (mDialogMessage != null)
		{
			TextView splashText = new TextView(mContext);
			splashText.setText(mDialogMessage);
			layout.addView(splashText);
		}

		mValueText = new TextView(mContext);
		mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
		mValueText.setTextAppearance(mContext, android.R.attr.textAppearanceLarge);
		params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		layout.addView(mValueText, params);

		mSeekBar = new SeekBar(mContext);
		mSeekBar.setOnSeekBarChangeListener(this);
		layout.addView(mSeekBar, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

		if (shouldPersist())
			mValue = getPersistedInt(mDefault);

		enforceProgressBoundsAndUpdate();
		
		return layout;
	}

	@Override
	protected void onBindDialogView(View v)
	{
		super.onBindDialogView(v);
		enforceProgressBoundsAndUpdate();
	}

	@Override
	protected void onSetInitialValue(boolean restore, Object defaultValue)
	{
		super.onSetInitialValue(restore, defaultValue);
		if (restore)
			mValue = shouldPersist() ? getPersistedInt(mDefault) : mDefault;
		else
			mValue = (Integer)defaultValue;
		
		enforceProgressBoundsAndUpdate();
	}

	protected void enforceProgressBoundsAndUpdate()
	{
		if (mMin > mMax)
			mMin = mMax;
		
		if (mValue < mMin)
			mValue = mMin;
		if (mValue > mMax)
			mValue = mMax;

		if (mSeekBar != null)
		{
			mSeekBar.setMax(mMax);
			mSeekBar.setProgress(mValue);
		}
		
		if (mValueText != null)
		{
			String t = String.valueOf(mValue);
			mValueText.setText(mSuffix == null ? t : t + " " + mSuffix);
		}
	}
	
	public void onProgressChanged(SeekBar seek, int value, boolean fromUser)
	{
		if (!fromUser)
			return;

		setProgress(value);
	}

	public void onStartTrackingTouch(SeekBar seek)
	{
	}

	public void onStopTrackingTouch(SeekBar seek)
	{
	}

	public void setMin(int min)
	{
		mMin = min;
	}

	public int getMin()
	{
		return mMin;
	}

	public void setMax(int max)
	{
		mMax = max;
	}

	public int getMax()
	{
		return mMax;
	}

	public void setProgress(int value)
	{
		mValue = value;
		enforceProgressBoundsAndUpdate();

		if (shouldPersist())
			persistInt(mValue);
		callChangeListener(new Integer(mValue));
	}

	public int getProgress()
	{
		return mValue;
	}
}
