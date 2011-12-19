package com.udesign.cashlens;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;

import com.udesign.cashlens.CashLensStorage.Account;
import com.udesign.cashlens.CashLensStorage.Currency;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
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
	private Spinner mCurrencySpinner;
	private ArrayAdapterIDAndName<Currency> mCurrenciesAdapter;
	private ImageButton mSnapshotButton;
	private boolean mShouldTakePicture = false;
	private boolean mInFocus = false;
	private Handler mAutoFocusStarter;
	private Runnable mAutoFocusTask;
	//private ImageButton mRecordButton;
	private CashLensStorage mStorage;
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
			mStorage = CashLensStorage.instance(this);
		} catch (IOException e) 
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
		mCurrencySpinner = (Spinner)findViewById(R.id.spinCurrency);
		
		AppSettings settings = AppSettings.instance(this);
		int position;
		
		mAccountsAdapter = mStorage.accountsAdapter(this);
	    mAccountSpinner.setAdapter(mAccountsAdapter);
	    
	    // automatically select last used account
	    position = mAccountsAdapter.getItemPositionById(settings.getLastUsedAccount());
	    mAccountSpinner.setSelection(position);
	    
		mCurrenciesAdapter = mStorage.currenciesAdapter(this);
	    mCurrencySpinner.setAdapter(mCurrenciesAdapter);

	    // automatically select last used currency
	    position = mCurrenciesAdapter.getItemPositionById(settings.getLastUsedCurrency());
	    mCurrencySpinner.setSelection(position);
	    
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
					parent.mShouldTakePicture = true;
			}
		});
	}

	public boolean dataValid()
	{
		if (getExpenseFixedPoint() != 0)
			return true;
		
		return false;
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
		
		if (mStorage != null)
		{
			mStorage.close();
			mStorage = null;
		}

		super.onDestroy();
	}

	protected void setCameraDisplayOrientation_2_2(int angle)
	{
		// find Android 2.2 setDisplayOrientation() on camera and use it to set 90 deg orientation
		try
	    {
			Method downPolymorphic = mCamera.getClass().getMethod("setDisplayOrientation", new Class[] { int.class });
	        if (downPolymorphic != null)
	            downPolymorphic.invoke(mCamera, new Object[] { angle });
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
        else
        {
        	// use 2.1 method (buggy, doesn't work on all devices)
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            {
                parameters.set("orientation", "portrait");
                parameters.set("rotation", 90);
            }
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            {
                parameters.set("orientation", "landscape");
                parameters.set("rotation", 90);
            }
        }        

        try
        {
        	mCamera.setParameters(parameters);
        }
        catch (Exception e)
        {
        	// ignore
        }
        
        mCamera.startPreview();
        mInPreview = true;

        startAutoFocusIfPossible();
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
        
        id = mCurrencySpinner.getSelectedItemId();
        if (id != AdapterView.INVALID_ROW_ID)
        	settings.setLastUsedCurrency((int)id);
	}

	public void onAutoFocus(boolean success, Camera camera) 
	{
		if (mCamera == null)
			return;
		
		if (success)
		{
			mInFocus = true;
			takePictureIfNecessary();
		}
		
		startAutoFocusIfPossible();
	}
	
	private void startAutoFocusIfPossible()
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
        		// continuous picture focus is not present (API level 14); try other mode
        		try
        		{
        			Field continuousVideoFocus = params.getClass().getField("FOCUS_MODE_CONTINUOUS_PICTURE");
            		focusMode = (String)continuousVideoFocus.get(params);
        		}
        		catch (Exception e2)
        		{
        			focusMode = Parameters.FOCUS_MODE_AUTO;	// default
        		}
        	}
        	
        	params.setFocusMode(focusMode);
        	mCamera.setParameters(params);
        	
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
							if (AddExpenseActivity.this != null &&  mCamera != null)
								mCamera.autoFocus(AddExpenseActivity.this);
						}
					};
					
        		mAutoFocusStarter.removeCallbacks(mAutoFocusTask);
        		mAutoFocusStarter.postDelayed(mAutoFocusTask, 500);
        	}
        }
        catch (Exception e)
        {
        	// autofocus isn't present on all cameras; assume in focus
        	mInFocus = true;
        }
	}
	
	private void takePictureIfNecessary()
	{
		if (mInFocus && mShouldTakePicture)
		{
			mShouldTakePicture = false;
			mCamera.takePicture(null, null, this);
		}
	}

	public void onPictureTaken(byte[] data, Camera camera) 
	{
		try 
		{
			Account account = (Account)mAccountSpinner.getSelectedItem();
			if (account == null)
				return;
			Log.w("onPictureTaken", "Selected account is " + account.name + ", id " + Integer.toString(account.id));
			
			Currency currency = (Currency)mCurrencySpinner.getSelectedItem();
			if (currency == null)
				return;
			Log.w("onPictureTaken", "Selected currency is " + currency.name + ", id " + Integer.toString(currency.id));
			
			mStorage.saveExpense(account, currency, getExpenseFixedPoint(), new Date(), data);
			Toast.makeText(this, R.string.expense_added, Toast.LENGTH_SHORT).show();
			
			// End activity.
			finish();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
		}
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
