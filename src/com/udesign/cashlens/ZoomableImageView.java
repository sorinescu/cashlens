/*******************************************************************************
 * Copyright 2012 Sorin Otescu <sorin.otescu@gmail.com>
 * 
 * Based on https://github.com/MikeOrtiz/TouchImageView by Michael Ortiz
 * 
 * Extends Android ImageView to include pinch zooming and panning
 * and adds zoom controls.
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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ZoomButtonsController;

public class ZoomableImageView extends ImageView {

	Matrix matrix = new Matrix();

    // Remember some things for zooming
    PointF last = new PointF();
    PointF scaleCenter = new PointF();
    float minScale = 1f;
    float maxScale = 3f;
    float[] m;
    
    float redundantXSpace, redundantYSpace;
    
    float viewWidth, viewHeight;
    float saveScale = 1f;
    float right, bottom, origWidth, origHeight, bmWidth, bmHeight;
    
    VersionedGestureDetector mScaleDetector;
    Context mContext;

    private final static int ZOOM_BUTTONS_DISMISS_TIME = 2000;
    private final static int ZOOM_BUTTONS_ANIMATION_TIME = 300;
    
    ZoomButtonsController mZoomButtons;
    private static Animation mFadeIn;
    private static Animation mFadeOut;
    Handler mZoomButtonsAnimationHandler = new Handler();
    Runnable mHideZoomButtonsRunnable;
    
    public ZoomableImageView(Context context) {
        super(context);
        initialize(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
        initialize(context);
	}

	public ZoomableImageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
        initialize(context);
	}

	protected void initializeZoomButtonsIfNecessary()
	{
		if (mZoomButtons == null)
		{
			ViewGroup parent = (ViewGroup)this.getParent();
			
	    	mZoomButtons = new ZoomButtonsController(parent);
	    	mZoomButtons.setAutoDismissed(false);
	    	mZoomButtons.setZoomSpeed(30);
	    	mZoomButtons.setOnZoomListener(new ZoomButtonsController.OnZoomListener()
			{
				public void onZoom(boolean zoomIn)
				{
					float scale;
					
					Log.d("ZoomableImageView", "onZoom" + (zoomIn ? "In" : "Out"));
					
					if (zoomIn)
						scale = 1.1f;
					else
						scale = 0.9f;
					
					resetZoomButtonsHideTimeout();
					
					zoom(scale);
				}
				
				public void onVisibilityChanged(boolean visible)
				{
					if (visible)
						updateZoomButtonsEnabled();
				}
			});
	    	
	    	// Make sure zoom in/out are disabled if the current scale is outside zoom bounds 
	    	updateZoomButtonsEnabled();
	    	
	    	ViewGroup zoomContainer = mZoomButtons.getContainer();

			Log.d("ZoomableImageView", "parent class is " + parent.getClass().toString());

			// Move zoom controls to bottom right, if possible
	    	if (parent instanceof LinearLayout)
	    	{
	    		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
	    				LayoutParams.WRAP_CONTENT);
	    		params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
	    		
	    		zoomContainer.setLayoutParams(params);
	    	}
	    	else if (parent instanceof RelativeLayout)
	    	{
	    		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
	    				LayoutParams.WRAP_CONTENT);

	    		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
	    		params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
	    		
	    		zoomContainer.setLayoutParams(params);
	    	}
	    	
	    	// Don't add zoom buttons directly; use their container instead, otherwise they won't be visible
	    	parent.addView(zoomContainer);
		}
		
		if (mHideZoomButtonsRunnable == null)
		{
	        mHideZoomButtonsRunnable = new Runnable()
	    	{
	    		public void run()
	    		{
	                mZoomButtons.getZoomControls().startAnimation(mFadeOut);
	                mZoomButtons.getZoomControls().setVisibility(View.GONE);
	                mZoomButtonsAnimationHandler.removeCallbacks(this);
	    		}
	    	};
		}
   	}
	
	protected void initialize(Context context)
	{
        super.setClickable(true);
        this.mContext = context;
        mScaleDetector = VersionedGestureDetector.newInstance(context, new ScaleListener());
        matrix.setTranslate(1f, 1f);
        m = new float[9];
        setImageMatrix(matrix);
        setScaleType(ScaleType.MATRIX);
        
        // initialize animations
        if (mFadeIn == null)
        {
        	mFadeIn = new AlphaAnimation(0, 1);
        	mFadeIn.setDuration(ZOOM_BUTTONS_ANIMATION_TIME);
        }
        
        if (mFadeOut == null)
        {
        	mFadeOut = new AlphaAnimation(1, 0);
        	mFadeOut.setDuration(ZOOM_BUTTONS_ANIMATION_TIME);
        }
	}
	
    @Override
    public void setImageBitmap(Bitmap bm) { 
        super.setImageBitmap(bm);
        bmWidth = bm.getWidth();
        bmHeight = bm.getHeight();
    }
    
    protected void updateZoomButtonsEnabled()
    {
    	initializeZoomButtonsIfNecessary();
    	
        if (saveScale >= maxScale)
        	mZoomButtons.setZoomInEnabled(false);
        else if (saveScale <= minScale)
        	mZoomButtons.setZoomOutEnabled(false);
        else
        {
        	mZoomButtons.setZoomInEnabled(true);
        	mZoomButtons.setZoomOutEnabled(true);
        }
    }
    
    protected void zoom(float scaleFactor)
    {
		Log.d("ZoomableImageView", "zoom(" + Float.toString(scaleFactor) + "); center=" + 
				Float.toString(scaleCenter.x) + "," + Float.toString(scaleCenter.y));
		
	 	float origScale = saveScale;
        saveScale *= scaleFactor;
        if (saveScale > maxScale) {
        	saveScale = maxScale;
        	scaleFactor = maxScale / origScale;
        } else if (saveScale < minScale) {
        	saveScale = minScale;
        	scaleFactor = minScale / origScale;
        }
        
        updateZoomButtonsEnabled();
        
    	right = viewWidth * saveScale - viewWidth - (2 * redundantXSpace * saveScale);
        bottom = viewHeight * saveScale - viewHeight - (2 * redundantYSpace * saveScale);

        matrix.postScale(scaleFactor, scaleFactor, scaleCenter.x, scaleCenter.y);

    	if (origWidth * saveScale <= viewWidth || origHeight * saveScale <= viewHeight) {
        	if (scaleFactor < 1) {
        		matrix.getValues(m);
        		float x = m[Matrix.MTRANS_X];
            	float y = m[Matrix.MTRANS_Y];

            	if (Math.round(origWidth * saveScale) < viewWidth) {
	        		if (y < -bottom)
    	        		matrix.postTranslate(0, -(y + bottom));
	        		else if (y > 0)
    	        		matrix.postTranslate(0, -y);
	        	} else {
            		if (x < -right) 
    	        		matrix.postTranslate(-(x + right), 0);
            		else if (x > 0) 
    	        		matrix.postTranslate(-x, 0);
	        	}
        	}
    	}
    	
        setImageMatrix(matrix);
        invalidate();
    }
    
    private class ScaleListener implements VersionedGestureDetector.OnGestureListener {

		public void onScale(float scaleFactor)
		{
			zoom(scaleFactor);
	    }

		public void onDrag(float deltaX, float deltaY)
		{
			//Log.d("ScaleListener", "onDrag(" + Float.toString(deltaX) + "," + Float.toString(deltaY) + ")");

			matrix.getValues(m);
        	float x = m[Matrix.MTRANS_X];
        	float y = m[Matrix.MTRANS_Y];
			PointF curr = new PointF(last.x + deltaX, last.y + deltaY);
			
			float scaleWidth = Math.round(origWidth * saveScale);
			float scaleHeight = Math.round(origHeight * saveScale);
			if (scaleWidth < viewWidth) {
				deltaX = 0;
				if (y + deltaY > 0)
    				deltaY = -y;
				else if (y + deltaY < -bottom)
    				deltaY = -(y + bottom); 
			} else if (scaleHeight < viewHeight) {
				deltaY = 0;
				if (x + deltaX > 0)
    				deltaX = -x;
    			else if (x + deltaX < -right)
    				deltaX = -(x + right);
			} else {
				if (x + deltaX > 0)
    				deltaX = -x;
    			else if (x + deltaX < -right)
    				deltaX = -(x + right);
    			
				if (y + deltaY > 0)
    				deltaY = -y;
    			else if (y + deltaY < -bottom)
    				deltaY = -(y + bottom);
			}
        	matrix.postTranslate(deltaX, deltaY);
        	last.set(curr.x, curr.y);

            setImageMatrix(matrix);
            invalidate();
		}
	}

	/* (non-Javadoc)
	 * @see android.view.View#onTouchEvent(android.view.MotionEvent)
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		// Save center point for zooming
		scaleCenter.set(event.getX(), event.getY());
		
		// Show zoom controls if not already visible
		initializeZoomButtonsIfNecessary();
		if (mZoomButtons.getZoomControls().getVisibility() != View.VISIBLE)
		{
			mZoomButtons.getZoomControls().startAnimation(mFadeIn);
			mZoomButtons.getZoomControls().setVisibility(View.VISIBLE);
		}
		
		resetZoomButtonsHideTimeout();
		
		return mScaleDetector.onTouchEvent(event);
	}
	
	protected void resetZoomButtonsHideTimeout()
	{
		// Remove the zoom buttons hide event and repost it for ZOOM_BUTTONS_DISMISS_TIME 
		// milliseconds from now
		mZoomButtonsAnimationHandler.removeCallbacks(mHideZoomButtonsRunnable);
		mZoomButtonsAnimationHandler.postDelayed(mHideZoomButtonsRunnable, 
				ZOOM_BUTTONS_DISMISS_TIME);
	}
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        viewHeight = MeasureSpec.getSize(heightMeasureSpec);
        
        // Maximum zoom is 1:1
		maxScale = Math.max(bmWidth/viewWidth, bmHeight/viewHeight);
		//Log.d("TouchImageView", "max scale is " + Float.toString(maxScale));

		// Fit to screen.
        float scale;
        float scaleX = (float)viewWidth / (float)bmWidth;
        float scaleY = (float)viewHeight / (float)bmHeight;
        scale = Math.min(scaleX, scaleY);
        matrix.setScale(scale, scale);
        saveScale = 1f;

        // Center the image
        redundantYSpace = (float)viewHeight - (scale * (float)bmHeight) ;
        redundantXSpace = (float)viewWidth - (scale * (float)bmWidth);
        redundantYSpace /= (float)2;
        redundantXSpace /= (float)2;

        matrix.postTranslate(redundantXSpace, redundantYSpace);
        setImageMatrix(matrix);
        
        origWidth = viewWidth - 2 * redundantXSpace;
        origHeight = viewHeight - 2 * redundantYSpace;
        
        right = viewWidth * saveScale - viewWidth - (2 * redundantXSpace * saveScale);
        bottom = viewHeight * saveScale - viewHeight - (2 * redundantYSpace * saveScale);
    }
}
