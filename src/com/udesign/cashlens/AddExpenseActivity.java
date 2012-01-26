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

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;

import com.udesign.cashlens.CashLensStorage.Account;

import android.app.Activity;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class AddExpenseActivity extends Activity implements SurfaceHolder.Callback, AutoFocusCallback, PictureCallback
{
	private RelativeLayout mLayout;
	private SurfaceView mCameraPreview;
	private android.hardware.Camera mCamera;
	private boolean mInPreview = false;
	private Button mNumButtons[];
	private Button mDelButton;
	private Button mDotButton;
	private TextView mExpenseText;
	private Spinner mAccountSpinner;
	private ArrayAdapterIDAndName<Account> mAccountsAdapter;
	private ImageButton mSnapshotButton;
	private boolean mShouldTakePicture = false;
	private boolean mInFocus = false;
	private Handler mAutoFocusStarter;
	private Runnable mAutoFocusTask;
	//private ImageButton mRecordButton;
	private CashLensStorage mStorage;
	private SensorEventListener mOrientationListener;
	private int mPictureRotation = 0;
	public String mExpenseInt;
	public String mExpenseFrac;
	public boolean mExpenseDot = false;
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_expense);
		
		try 
		{
			mStorage = CashLensStorage.instance(getApplicationContext());
		} catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		mExpenseInt = "";
		mExpenseFrac = "";
		
		mLayout = (RelativeLayout)findViewById(R.id.addExpense);
		mLayout.setBackgroundColor(Color.argb(0, 0, 0, 0));    	

		mCameraPreview = (SurfaceView)findViewById(R.id.cameraPreview);

		mExpenseText = (TextView)findViewById(R.id.txtSum);
		updateExpenseText();
		
		mNumButtons = new Button[10];
		mNumButtons[0] = (Button)findViewById(R.id.btn0);
		mNumButtons[1] = (Button)findViewById(R.id.btn1);
		mNumButtons[2] = (Button)findViewById(R.id.btn2);
		mNumButtons[3] = (Button)findViewById(R.id.btn3);
		mNumButtons[4] = (Button)findViewById(R.id.btn4);
		mNumButtons[5] = (Button)findViewById(R.id.btn5);
		mNumButtons[6] = (Button)findViewById(R.id.btn6);
		mNumButtons[7] = (Button)findViewById(R.id.btn7);
		mNumButtons[8] = (Button)findViewById(R.id.btn8);
		mNumButtons[9] = (Button)findViewById(R.id.btn9);
		
		mDelButton = (Button)findViewById(R.id.btnDel);
		mDotButton = (Button)findViewById(R.id.btnDot);
		
		mSnapshotButton = (ImageButton)findViewById(R.id.btnShoot);
		//mRecordButton = (ImageButton)findViewById(R.id.btnRec);

		mAccountSpinner = (Spinner)findViewById(R.id.spinAccount);
		
		AppSettings settings = AppSettings.instance(this);
		int position;
		
		mAccountsAdapter = mStorage.accountsAdapter(this);
		if (mAccountsAdapter.isEmpty())
		{
			Toast.makeText(this, R.string.no_accounts_defined, Toast.LENGTH_LONG).show();
			finish();
		}
		
		mAccountsAdapter.setTextAppearance(R.style.large_textview_text);
	    mAccountSpinner.setAdapter(mAccountsAdapter);
	    
	    // automatically select last used account
	    position = mAccountsAdapter.getItemPositionById(settings.getLastUsedAccount());
	    mAccountSpinner.setSelection(position);
	    
	    SurfaceHolder holder = mCameraPreview.getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		// Click listeners

		for (int i=0; i<10; ++i)
			mNumButtons[i].setOnClickListener(new NumericButtonClickListener(this, i));
		
		mDelButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) 
			{
				AddExpenseActivity parent = AddExpenseActivity.this;
				
				if (parent.mExpenseFrac.length() > 0)
					parent.mExpenseFrac = parent.mExpenseFrac.substring(0, parent.mExpenseFrac.length() - 1); 
				else
				{
					parent.mExpenseDot = false;
					
					if (parent.mExpenseInt.length() > 0)
						parent.mExpenseInt = parent.mExpenseInt.substring(0, parent.mExpenseInt.length() - 1); 
				}
				
				parent.updateExpenseText();
			}
		});
		
		mDotButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) 
			{
				AddExpenseActivity parent = AddExpenseActivity.this;
				
				parent.mExpenseDot = true;
				parent.updateExpenseText();
			}
		});
		
		mSnapshotButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) 
			{
				// picture is taken when autofocus is complete
				AddExpenseActivity parent = AddExpenseActivity.this;
				
				if (parent.dataValid())
				{
					parent.mShouldTakePicture = true;
					
					// will also take the picture once in focus
					parent.startAutoFocusIfPossible();
				}
				else
					Toast.makeText(parent, R.string.fill_amount_before_snapshot, Toast.LENGTH_LONG).show();
			}
		});
		
		// register a screen orientation listener through the sensor manager
		// because the orientation listener gives bogus values
		
		SensorManager sensors = (SensorManager)getSystemService(SENSOR_SERVICE);
		
		mOrientationListener = new SensorEventListener()
		{
			public void onSensorChanged(SensorEvent event)
			{
				/*
				Log.d("OrientationChanged", "orientation is now [" 
						+ Float.toString(event.values[0]) + ","
						+ Float.toString(event.values[1]) + ","
						+ Float.toString(event.values[2]) + "]");
				*/
				
				float roll = event.values[2];
				if (roll <= 45 && roll >= -45) {
					//Log.d("OrientationChanged", "PORTRAIT");
					mPictureRotation = 90;
				} else if (roll > 45) {
					//Log.d("OrientationChanged", "LANDSCAPE");
					mPictureRotation = 0;
				} else if (roll < -45) {
					//Log.d("OrientationChanged", "REVERSE_LANDSCAPE");
					mPictureRotation = 180;
				}
			}
			
			public void onAccuracyChanged(Sensor sensor, int accuracy)
			{
			}
		};
		
		sensors.registerListener(mOrientationListener, sensors.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	public boolean dataValid()
	{
		if (getExpenseFixedPoint() == 0)
			return false;
		
		if (mAccountSpinner.getSelectedItemId() == AdapterView.INVALID_ROW_ID)
			return false;
		
		return true;
	}
	
	protected String getExpenseText()
	{
		String txt;
		
		if (mExpenseInt.length() > 0)
			txt = mExpenseInt;
		else
			txt = "0";
		
		if (mExpenseDot && mExpenseFrac.length() > 0)
			txt += "." + mExpenseFrac;
		
		return txt;
	}
	
	protected int getExpenseFixedPoint()
	{
		int val = 0;
		
		if (mExpenseInt.length() > 0)
			val = Integer.parseInt(mExpenseInt) * 100;
		
		if (mExpenseDot && mExpenseFrac.length() > 0)
			val += Integer.parseInt(mExpenseFrac) % 100;	// to be safe
		
		return val;
	}
	
	public void updateExpenseText()
	{
		mExpenseText.setText(getExpenseText());
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() 
	{
		if (mAutoFocusStarter != null)
		{
			mAutoFocusStarter.removeCallbacks(mAutoFocusTask);
			mAutoFocusStarter = null;
		}
		
		if (mCamera != null)
		{
			if (mInPreview)
				mCamera.stopPreview();
	        mCamera.release();
	        mCamera = null;
		}
		
		// Make sure the data changed listeners are unregistered
		mAccountsAdapter.releaseByActivity();

		super.onDestroy();
	}

	protected void setCameraDisplayOrientation_2_2(int angle)
	{
		// find Android 2.2 setDisplayOrientation() on camera and use it to set 90 deg orientation
		try
	    {
			Method downPolymorphic = mCamera.getClass().getMethod("setDisplayOrientation", new Class[] { int.class });
	        if (downPolymorphic != null)
	        {
	        	Log.w("CameraOrientation", "setting angle " + Integer.toString(angle) + " via Android 2.2 interface");
	            downPolymorphic.invoke(mCamera, new Object[] { angle });
	        }
	    }
	    catch (Exception e1)
	    {
	    	// ignore
	    }	
	}
	
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) 
	{
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(width, height);
    
        // for 2.2+, use standard (working) method to put camera in portrait mode
        if (Integer.parseInt(Build.VERSION.SDK) >= 8)
            setCameraDisplayOrientation_2_2(90);

        try
        {
        	mCamera.setParameters(parameters);
        }
        catch (Exception e)
        {
        	e.printStackTrace();
        }
        
        mCamera.startPreview();
        mInPreview = true;

        // Enable this if you want continuous autofocus (the camera will search for focus
        // all the time, while the user is typing)
		Log.w("onAutoFocus", "enable the code below if you want continuous focus");
        //startAutoFocusIfPossible();
	}

	public void surfaceCreated(SurfaceHolder holder) 
	{
        // The Surface has been created, acquire the camera and tell it where
        // to draw
        mCamera = Camera.open();
        try 
        {
           mCamera.setPreviewDisplay(holder);
        } 
        catch (IOException exception) 
        {
            mCamera.release();
            mCamera = null;
        }
	}

	public void surfaceDestroyed(SurfaceHolder holder) 
	{
        // Surface will be destroyed when we return, so stop the preview.
        // Because the CameraDevice object is not a shared resource, it's very
        // important to release it when the activity is paused
        if (mCamera != null)
        {
			if (mInPreview)
	        	mCamera.stopPreview();
	        mCamera.release();
	        mCamera = null;
        }
        
        // save last used currency and account
        AppSettings settings = AppSettings.instance(this);
        long id;
        
        id = mAccountSpinner.getSelectedItemId();
        if (id != AdapterView.INVALID_ROW_ID)
        	settings.setLastUsedAccount((int)id);
	}

	public void onAutoFocus(boolean success, Camera camera) 
	{
		if (mCamera == null)
			return;
		
		if (success)
		{
			mInFocus = true;
			success = takePictureIfNecessary();	// did we take the picture ?
		}

		if (!success)	// didn't need to take the picture, or we couldn't focus; retry
			startAutoFocusIfPossible();
	}
	
	public void startAutoFocusIfPossible()
	{
		// first, stop auto focus if active
		mInFocus = false;
		mCamera.cancelAutoFocus();
		
		// try to set a continuous focus mode (preferably picture, but video is also ok)
        try
        {      	
        	Camera.Parameters params = mCamera.getParameters();
        	String focusMode;
        
        	try
        	{
        		Field continuousPicFocus = params.getClass().getField("FOCUS_MODE_CONTINUOUS_PICTURE");
        		focusMode = (String)continuousPicFocus.get(params);
        	}
        	catch (Exception e1)
        	{
        		// continuous picture focus is not present (API level 14); use default
    			focusMode = Parameters.FOCUS_MODE_AUTO;
        	}
        	
        	params.setFocusMode(focusMode);
        	mCamera.setParameters(params);
        	
        	Log.w("startAutofocus", "using focus mode " + focusMode);
        	
        	if (focusMode != Parameters.FOCUS_MODE_AUTO)
        		mCamera.autoFocus(this);
        	else
        	{
        		// start autofocus some time in the future, to avoid an infinite loop in onAutoFocus 
        		if (mAutoFocusStarter == null)
        			mAutoFocusStarter = new Handler();
        		
        		if (mAutoFocusTask == null)
        			mAutoFocusTask = new Runnable() {
						public void run() 
						{
							if (AddExpenseActivity.this != null && mCamera != null)
								mCamera.autoFocus(AddExpenseActivity.this);
						}
					};
					
        		mAutoFocusStarter.removeCallbacks(mAutoFocusTask);
        		mAutoFocusStarter.post(mAutoFocusTask);
        	}
        }
        catch (Exception e)
        {
        	// autofocus isn't present on all cameras; assume in focus
        	mInFocus = true;
        }
	}
	
	private boolean takePictureIfNecessary()
	{
		if (mInFocus && mShouldTakePicture)
		{
			mShouldTakePicture = false;

			Camera.Parameters parameters = mCamera.getParameters();
			
	        // set EXIF picture orientation based on device orientation
	    	Log.w("takePicture", "setting rotation " + Integer.toString(mPictureRotation) + 
	    			" in portrait mode via Android 2.1 interface (buggy)");
	        parameters.setRotation(mPictureRotation);
	        
	        // Set JPEG quality and size from app settings
			AppSettings settings = AppSettings.instance(getApplicationContext());
	        AppSettings.PictureSize picSize = settings.getJpegPictureSize();

	        parameters.setJpegQuality(settings.getJpegQuality());
	        parameters.setPictureSize(picSize.width(), picSize.height());

	        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
	        
	    	Log.w("takePicture", "setting picture quality " + Integer.toString(parameters.getJpegQuality()) + 
				", size " + picSize.toString());
	    	
	        try
	        {
	        	mCamera.setParameters(parameters);
	        }
	        catch (Exception e)
	        {
	        	e.printStackTrace();
	        }
	        
			mCamera.takePicture(null, null, this);
			return true;
		}
		
		return false;	// didn't need to take picture
	}

	public void onPictureTaken(byte[] data, Camera camera) 
	{
		new AsyncTask<byte[], Void, String>() 
		{
			@Override
			protected String doInBackground(byte[]... params)
			{
				byte[] data = params[0];
				
				try 
				{
					Account account = (Account)mAccountSpinner.getSelectedItem();
					if (account == null)
						return null;
					Log.w("onPictureTaken", "Selected account is " + account.name + ", id " + Integer.toString(account.id));
					
					mStorage.saveExpense(account, getExpenseFixedPoint(), new Date(), data);
				} 
				catch (Exception e) 
				{
					e.printStackTrace();
					return e.getLocalizedMessage();
				}
				
				// a Toast will be shown, containing this string
				return getApplicationContext().getString(R.string.expense_added);
			}

			/* (non-Javadoc)
			 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
			 */
			@Override
			protected void onPostExecute(String message)
			{
				// This will be executed on the UI thread
				if (message == null)	// expense hasn't been added due to incomplete parameters
					return;

				// We can't use the parent's variables because the activity doesn't exist anymore
				try
				{
					CashLensStorage storage = CashLensStorage.instance(getApplicationContext());

					// Show success or error message and notify listeners that a new expense may be available
					Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
					storage.notifyExpensesChanged();
				} catch (Exception e)
				{
					e.printStackTrace();
					Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
				}
			}
		}.execute(data);
		
		// End activity; expense will be saved asynchronously
		finish();
	}
}

class NumericButtonClickListener implements OnClickListener
{
	private int mFigure;
	private AddExpenseActivity mParent;
	
	NumericButtonClickListener(AddExpenseActivity parent, int figure)
	{
		mParent = parent;
		mFigure = figure;
	}
	
	public void onClick(View v) 
	{
		if (mParent.mExpenseDot)
		{
			if (mParent.mExpenseFrac.length() < 2)
				mParent.mExpenseFrac += Integer.toString(mFigure);
		}
		else
			mParent.mExpenseInt +=  Integer.toString(mFigure);

		mParent.updateExpenseText();
	}
}
