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
import java.util.List;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public final class SettingsActivity extends PreferenceActivity
	implements OnSharedPreferenceChangeListener
{
	private Preference mJpegParams;
	private Preference mJpegQuality;
	private ListPreference mJpegPictureSize;
	
	/* (non-Javadoc)
	 * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		mJpegParams = findPreference("jpegParams");
		mJpegQuality = findPreference("jpegQuality");
		mJpegPictureSize = (ListPreference)findPreference("jpegPictureSize");
		
		populateJpegPictureSize();
	}
	
	private void updateSummaries(boolean mainScreen)
	{
		AppSettings settings = AppSettings.instance(getApplicationContext());
		
		if (mainScreen)
		{
			mJpegParams.setSummary("Quality: " + settings.getJpegQuality() +
					"; " + "Size: " + settings.getJpegPictureSize().toString());
			
			// Android doesn't update its views after setSummary; must do it manually
			// See http://code.google.com/p/android/issues/detail?id=931
			onContentChanged();
		}
		else
		{
			mJpegQuality.setSummary(Integer.toString(settings.getJpegQuality()));
			mJpegPictureSize.setSummary(settings.getJpegPictureSize().toString());
		}
	}
	
	private void populateJpegPictureSize()
	{
		Camera camera = Camera.open();
		Camera.Parameters params = camera.getParameters();
		List<Size> sizes = params.getSupportedPictureSizes();
		
		// Emulator returns null sizes (Android bug)
		if (sizes == null)
		{
			sizes = new ArrayList<Size>(1);
			sizes.add(camera.new Size(1024, 768));
		}

		camera.release();
				
		String[] entries = new String[sizes.size()];
		for (int i=0; i<sizes.size(); ++i)
		{
			Size size = sizes.get(i);
			entries[i] = Integer.toString(size.width) +
				" x " + Integer.toString(size.height);
		}
		
		mJpegPictureSize.setEntries(entries);
		mJpegPictureSize.setEntryValues(entries);

		// Set a default picture size if none is stored
		AppSettings settings = AppSettings.instance(getApplicationContext());

		if (settings.getJpegPictureSize().width() == 0)	// not set
			mJpegPictureSize.setValueIndex(0);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onWindowFocusChanged(boolean)
	 */
	@Override
	public void onWindowFocusChanged(boolean hasFocus)
	{
		// If hasFocus, update main screen preferences, 
		// otherwise secondary screen preferences
		updateSummaries(hasFocus);
	
		super.onWindowFocusChanged(hasFocus);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext());
		
		prefs.unregisterOnSharedPreferenceChangeListener(this);

		super.onPause();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume()
	{
		super.onResume();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext());
		
		prefs.registerOnSharedPreferenceChangeListener(this);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key)
	{
		updateSummaries(false);
	}
}
