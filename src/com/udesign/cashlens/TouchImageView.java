/*
 * TouchImageView.java
 * 
 * Based on https://github.com/MikeOrtiz/TouchImageView by Michael Ortiz
 * 
 * Modified by sorinescu
 * -------------------
 * Extends Android ImageView to include pinch zooming and panning.
 */

package com.udesign.cashlens;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

public class TouchImageView extends ImageView {

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
    
    Context context;

    public TouchImageView(Context context) {
        super(context);
        initialize(context);
    }

    public TouchImageView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
        initialize(context);
	}

	public TouchImageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
        initialize(context);
	}

	protected void initialize(Context context)
	{
        super.setClickable(true);
        this.context = context;
        mScaleDetector = VersionedGestureDetector.newInstance(context, new ScaleListener());
        matrix.setTranslate(1f, 1f);
        m = new float[9];
        setImageMatrix(matrix);
        setScaleType(ScaleType.MATRIX);
	}
	
    @Override
    public void setImageBitmap(Bitmap bm) { 
        super.setImageBitmap(bm);
        bmWidth = bm.getWidth();
        bmHeight = bm.getHeight();
    }
    
    private class ScaleListener implements VersionedGestureDetector.OnGestureListener {

		public void onScale(float scaleFactor)
		{
			//Log.d("ScaleListener", "onScale(" + Float.toString(scaleFactor) + "); center=" + 
			//		Float.toString(scaleCenter.x) + "," + Float.toString(scaleCenter.y));
			
		 	float origScale = saveScale;
	        saveScale *= scaleFactor;
	        if (saveScale > maxScale) {
	        	saveScale = maxScale;
	        	scaleFactor = maxScale / origScale;
	        } else if (saveScale < minScale) {
	        	saveScale = minScale;
	        	scaleFactor = minScale / origScale;
	        }
	        
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
		
		return mScaleDetector.onTouchEvent(event);
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