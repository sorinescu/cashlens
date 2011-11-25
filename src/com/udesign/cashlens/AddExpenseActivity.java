package com.udesign.cashlens;

import java.io.IOException;

import android.app.Activity;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AddExpenseActivity extends Activity implements SurfaceHolder.Callback 
{
	 
	private RelativeLayout mLayout;
	private SurfaceView mCameraPreview;
	private android.hardware.Camera mCamera;
	private boolean mInPreview;
	private Button mNumButtons[];
	private Button mDelButton;
	private Button mDotButton;
	private TextView mExpenseText;
	public String mExpenseInt;
	public String mExpenseFrac;
	public boolean mExpenseDot;
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_expense);
		
		mExpenseInt = "";
		mExpenseFrac = "";
		mExpenseDot = false;
		
		mLayout = (RelativeLayout)findViewById(R.id.addExpense);
		mLayout.setBackgroundColor(Color.argb(0, 0, 0, 0));    	

		mCameraPreview = (SurfaceView)findViewById(R.id.cameraPreview);
		mInPreview = false;

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
		
		SurfaceHolder holder = mCameraPreview.getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
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
	}

	public void updateExpenseText()
	{
		String txt;
		
		if (mExpenseInt.length() > 0)
			txt = mExpenseInt;
		else
			txt = "0";
		
		if (mExpenseDot && mExpenseFrac.length() > 0)
			txt += "." + mExpenseFrac;
		
		mExpenseText.setText(txt);
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() 
	{
		if (mCamera != null)
		{
			if (mInPreview)
				mCamera.stopPreview();
	        mCamera.release();
	        mCamera = null;
		}

		super.onDestroy();
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) 
	{
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(width, height);
        mCamera.setParameters(parameters);
        mCamera.startPreview();
        mInPreview = true;
	}

	public void surfaceCreated(SurfaceHolder holder) 
	{
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
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
        // important to release it when the activity is paused.
        if (mCamera != null)
        {
			if (mInPreview)
	        	mCamera.stopPreview();
	        mCamera.release();
	        mCamera = null;
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
			mParent.mExpenseFrac += Integer.toString(mFigure);
		else
			mParent.mExpenseInt +=  Integer.toString(mFigure);

		mParent.updateExpenseText();
	}
}
