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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.udesign.cashlens.CashLensStorage.Account;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera.Parameters;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class AddExpenseActivity extends Activity 
	implements SurfaceHolder.Callback, Camera.AutoFocusCallback, Camera.PictureCallback, Camera.PreviewCallback
{
	private RelativeLayout mLayout;
	private SurfaceView mCameraPreview;
	private SurfaceHolder mHolder;
	private boolean mCameraSurfaceValid;
	private Camera mCamera;
	private boolean mInPreview = false;
	private Button mNumButtons[];
	private Button mDelButton;
	private Button mCurrConvButton;
	private Button mDotButton;
	private TextView mExpenseText;
	private EditText mDescriptionText;
	private Spinner mAccountSpinner;
	private ArrayAdapterIDAndName<Account> mAccountsAdapter;
	private ImageView mPhotoThumbnail;
	private ImageButton mSnapshotButton;
	private ImageButton mSaveButton;
	private boolean mShouldTakePicture = false;
	private boolean mInFocus = false;
	private Handler mAutoFocusStarter;
	private Runnable mAutoFocusTask;
	private byte[] mJPEGData;
	private CashLensStorage mStorage;
	private SensorEventListener mOrientationListener;
	private int mPictureRotation = 0;
	
	// Preview thumbnail
	private Size mPreviewSize;
	private byte[] mRGBPreviewData;
	private Bitmap mRGBPreviewBitmap;
	private Bitmap mRGBPreviewRotBitmap;
	private Canvas mRGBPreviewRotCanvas;
	
	public String mExpenseInt;
	public String mExpenseFrac;
	public boolean mExpenseDot = false;
	
	class NumericButtonClickListener implements OnClickListener
	{
		private int mFigure;
		
		NumericButtonClickListener(int figure)
		{
			mFigure = figure;
		}
		
		public void onClick(View v) 
		{
			AddExpenseActivity parent = AddExpenseActivity.this;
			
			if (parent.mExpenseDot)
			{
				if (parent.mExpenseFrac.length() < 2)
					parent.mExpenseFrac += Integer.toString(mFigure);
			}
			else
				parent.mExpenseInt +=  Integer.toString(mFigure);

			parent.updateExpenseText();
		}
	}

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
			mStorage = CashLensStorage.instance(this);
		} catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		mExpenseInt = "";
		mExpenseFrac = "";
		
		mLayout = (RelativeLayout)findViewById(R.id.addExpense);
		mLayout.setBackgroundColor(Color.argb(0, 0, 0, 0));    	

		mCameraPreview = (SurfaceView)findViewById(R.id.cameraPreview);

		mSaveButton = (ImageButton)findViewById(R.id.btnSave);

		mDescriptionText = (EditText)findViewById(R.id.txtDesc);
		
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
		mCurrConvButton = (Button)findViewById(R.id.btnCurrencyConvert);
		mDotButton = (Button)findViewById(R.id.btnDot);
		
		mPhotoThumbnail = (ImageView)findViewById(R.id.photoView);
		mSnapshotButton = (ImageButton)findViewById(R.id.btnShoot);
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
	    
	    mHolder = mCameraPreview.getHolder();
	    mHolder.addCallback(this);
	    mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		// Click listeners

		for (int i=0; i<10; ++i)
			mNumButtons[i].setOnClickListener(new NumericButtonClickListener(i));
		
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
		
		mCurrConvButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				Intent currConv = new Intent(AddExpenseActivity.this,
						CurrencyConversionActivity.class);
				Account account = (Account)mAccountSpinner.getSelectedItem();

				currConv.putExtra("to_currency", account.getCurrency().id);
				currConv.putExtra("amount", getExpenseFixedPoint());
				startActivityForResult(currConv, 0);
			}
		});
		
		mSnapshotButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) 
			{
				// picture is taken when autofocus is complete
				AddExpenseActivity parent = AddExpenseActivity.this;
				
				parent.mShouldTakePicture = true;
				
				// will also take the picture once in focus
				parent.startAutoFocusIfPossible();
			}
		});
		
		mSaveButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				// make sure we can't press Save again
				mSaveButton.setEnabled(false);

				// save last used account
		        AppSettings settings = AppSettings.instance(AddExpenseActivity.this);
		        long id;
		        
		        id = mAccountSpinner.getSelectedItemId();
		        if (id != AdapterView.INVALID_ROW_ID)
		        	settings.setLastUsedAccount((int)id);

				saveExpense();
			}
		});
		
		mDescriptionText.addTextChangedListener(new TextWatcher()
		{
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
			}
			
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after)
			{
			}
			
			public void afterTextChanged(Editable s)
			{
				updateSaveButtonState();
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
		
		updateSaveButtonState();
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
	
	protected void setExpenseFixedPoint(int amount)
	{
		mExpenseDot = false;
		mExpenseInt = "";
		mExpenseFrac = "";
		if (amount != 0)
		{
			mExpenseInt = Integer.toString(amount / 100);
			if (amount % 100 != 0)
			{
				mExpenseDot = true;
				mExpenseFrac = Integer.toString(amount % 100);
			}
		}
	}
	
	public void updateExpenseText()
	{
		mExpenseText.setText(getExpenseText());
		
		updateSaveButtonState();
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

		releaseCamera();
		
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
		final class MyComparator<T extends Size> implements Comparator<T>
		{
        	private int width;
        	private int height;
        	
            public MyComparator(int width, int height)
			{
				super();
				this.width = width;
				this.height = height;
			}

			public int compare(Camera.Size o1, Camera.Size o2) 
			{
            	int diff1 = Math.abs(o1.width - height) + Math.abs(o1.height - width);
            	int diff2 = Math.abs(o2.width - height) + Math.abs(o2.height - width);
                return new Integer(diff1).compareTo(diff2);
            }
		}
		
		if (mCamera == null)
			return;		// camera not working; bail
		
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = mCamera.getParameters();

        // Find optimum preview size by selecting the supported preview size
        // that is the closest to width x height
        final List<Size> previewSizes = parameters.getSupportedPreviewSizes();
        Comparator<Size> comp = new MyComparator<Size>(width, height);
        Collections.sort(previewSizes, comp);

        // for 2.2+, use standard (working) method to put camera in portrait mode
        if (Integer.parseInt(Build.VERSION.SDK) >= 8)
            setCameraDisplayOrientation_2_2(90);

    	for (Size sz : previewSizes)
    	{
    		// Use the closest preview size that is actually supported
	        try
	        {
	            parameters.setPreviewSize(sz.width, sz.height);
	        	mCamera.setParameters(parameters);
	        	
	        	mCamera.setPreviewCallback(this);
	            mCamera.startPreview();
	            mInPreview = true;
	            
	            // Cache these value so we can generate a thumbnail very quickly after
	            // we take a picture
	            mPreviewSize = sz;
	            setupPreviewThumbnail();
	            
	            break;
	        }
	        catch (Exception e)
	        {
	        	e.printStackTrace();
	        }
    	}
    	
        // Enable this if you want continuous autofocus (the camera will search for focus
        // all the time, while the user is typing)
		Log.w("onAutoFocus", "enable the code below if you want continuous focus");
        //startAutoFocusIfPossible();
	}

	private void openCamera()
	{
		if (mCamera != null)
			return;		// already open
		
		if (!mCameraSurfaceValid)
			return;		// nothing to render a preview into
		
        try 
        {
            mCamera = Camera.open();
        	mCamera.setPreviewDisplay(mHolder);

        	mSnapshotButton.setEnabled(true);
        } 
        catch (Exception e) 
        {
        	if (mCamera != null)
        	{
        		mCamera.release();
        		mCamera = null;
        	}
        	
            // Can no longer take pictures
            mSnapshotButton.setEnabled(false);
            
            Toast.makeText(this, e.getMessage(),Toast.LENGTH_SHORT);
        }
	}
	
	private void releaseCamera()
	{
		if (mCamera == null)
			return;
		
		if (mInPreview)
		{
			mCamera.stopPreview();
			mInPreview = false;
		}
			
		mCamera.setPreviewCallback(null);

		mCamera.release();
        mCamera = null;
	}
	
	public void surfaceCreated(SurfaceHolder holder) 
	{
		// From now on, we can open the camera - we have a surface
		mCameraSurfaceValid = true;
		
        // The Surface has been created, acquire the camera and tell it where
        // to draw
		openCamera();
	}

	public void surfaceDestroyed(SurfaceHolder holder) 
	{
        // Surface will be destroyed when we return, so stop the preview.
        // Because the CameraDevice object is not a shared resource, it's very
        // important to release it when the activity is paused
		releaseCamera();
		
		mCameraSurfaceValid = false;	// We don't have a surface anymore
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
			AppSettings settings = AppSettings.instance(this);
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
	        
	        // First, stop preview, otherwise the camera can crash
	        mCamera.stopPreview();
	        mInPreview = false;
	        
			mCamera.takePicture(null, null, this);
			return true;
		}
		
		return false;	// didn't need to take picture
	}

	private void saveExpense()
	{
		if (mJPEGData == null && mDescriptionText.getText().length() == 0)
			return;
		
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
					
					String description = mDescriptionText.getText().toString();
					if (description.length() == 0)
						description = null;
					
					mStorage.saveExpense(account, getExpenseFixedPoint(), new Date(), data, description);
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
					CashLensStorage storage = CashLensStorage.instance(AddExpenseActivity.this);

					// Show success or error message and notify listeners that a new expense may be available
					Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
					storage.notifyExpensesChanged();
				} catch (Exception e)
				{
					e.printStackTrace();
					Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
				}
			}
		}.execute(mJPEGData);
		
		// End activity; expense will be saved asynchronously
		finish();
	}
	
	public void onPictureTaken(byte[] data, Camera camera) 
	{
		mJPEGData = data;
		
		updateSaveButtonState();
		animatePictureThumbnail();

		// User can take another picture, so restart preview
        mCamera.startPreview();
        mInPreview = true;
	}

	private void setupPreviewThumbnail()
	{
        mRGBPreviewData = new byte[2 * mPreviewSize.width * mPreviewSize.height];
		mRGBPreviewBitmap = Bitmap.createBitmap(mPreviewSize.width, mPreviewSize.height, 
				Config.RGB_565);
		mRGBPreviewRotBitmap = Bitmap.createBitmap(mPreviewSize.height, mPreviewSize.width, 
				Config.RGB_565);
		
		mRGBPreviewRotCanvas = new Canvas(mRGBPreviewRotBitmap);
		mRGBPreviewRotCanvas.translate(mPreviewSize.height, 0);
		mRGBPreviewRotCanvas.rotate(90);
	}
	
	private void animatePictureThumbnail()
	{
		// Copy RGB565 preview data into bitmap
		mRGBPreviewBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(mRGBPreviewData));
		
		// Rotate bitmap
		mRGBPreviewRotCanvas.drawBitmap(mRGBPreviewBitmap, 0f, 0f, null);

		mPhotoThumbnail.setImageBitmap(mRGBPreviewRotBitmap);
		mPhotoThumbnail.setVisibility(View.VISIBLE);

		// The thumbnail will be scaled from full screen to the size of the
		// snapshot button
		float snapWidth = mSnapshotButton.getWidth();
		float snapHeight = mSnapshotButton.getHeight();
		
		// Can't use mPhotoThumbnail because it hasn't been laid out yet (width=height=0)
		float scaleX = snapWidth / mLayout.getWidth();
		float scaleY = snapHeight / mLayout.getHeight();
		
		ScaleAnimation scaleAnim = new ScaleAnimation(1.0f, scaleX, 1.0f, scaleY, 
				Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 1.0f);

		scaleAnim.setInterpolator(new LinearInterpolator());
		scaleAnim.setDuration(100);
		scaleAnim.setFillAfter(true);
		
		mPhotoThumbnail.startAnimation(scaleAnim);
	}
	
	private void updateSaveButtonState()
	{
		boolean enabled = 
				(mJPEGData != null || mDescriptionText.getText().length() != 0) 
				&& getExpenseFixedPoint() != 0 
				&& mAccountSpinner.getSelectedItemId() != AdapterView.INVALID_ROW_ID;

		mSaveButton.setEnabled(enabled);
		mSaveButton.setImageResource(enabled ? R.drawable.save : R.drawable.save_disabled);
	}

	public void onPreviewFrame(byte[] data, Camera camera)
	{
		// Preview data is in NV21 format (YUV 420); convert to RGB565
		CashLensUtils.nv21ToRGB565(data, mRGBPreviewData, 
				mPreviewSize.width, mPreviewSize.height);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume()
	{
		super.onResume();
		
		openCamera();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause()
	{
		super.onPause();
		
		releaseCamera();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		
		// resultCode is actually converted amount (fixed point)
		setExpenseFixedPoint(resultCode);
		updateExpenseText();
	}
}
