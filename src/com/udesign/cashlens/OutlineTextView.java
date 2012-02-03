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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.PorterDuff.Mode;
import android.util.AttributeSet;
import android.widget.TextView;

public class OutlineTextView extends TextView
{
	private static final Paint BORDER_PAINT = new Paint();
    private static final int BORDER_WIDTH = 1;
    
    protected int mOutlineColor = Color.BLACK;

    static {
        BORDER_PAINT.setXfermode(new PorterDuffXfermode(Mode.DST_OUT));
    }
    
	public OutlineTextView(Context context)
	{
		super(context);
	}

	public OutlineTextView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public OutlineTextView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}
	
	/**
	 * Set the color of the text outline.
	 * 
	 * @param color color to set
	 */
	public void setOutlineColor(int color)
	{
		// TODO this is ignored; find an efficient way of drawing it
		mOutlineColor = color;
	}

	/* (non-Javadoc)
	 * @see android.widget.TextView#onDraw(android.graphics.Canvas)
	 */
	@Override
	protected void onDraw(Canvas canvas)
	{
		//ColorStateList colors = getTextColors();
		
		canvas.saveLayer(null, BORDER_PAINT, Canvas.HAS_ALPHA_LAYER_SAVE_FLAG
                | Canvas.FULL_COLOR_LAYER_SAVE_FLAG | Canvas.MATRIX_SAVE_FLAG);
		//setTextColor(mOutlineColor);
		drawBackground(canvas, -BORDER_WIDTH, -BORDER_WIDTH);
		drawBackground(canvas, BORDER_WIDTH + BORDER_WIDTH, 0);
		drawBackground(canvas, 0, BORDER_WIDTH + BORDER_WIDTH);
		drawBackground(canvas, -BORDER_WIDTH - BORDER_WIDTH, 0);
		//setTextColor(colors);
		canvas.restore();
		
		super.onDraw(canvas);
	}

	private void drawBackground(Canvas canvas, int dx, int dy) 
	{
		canvas.translate(dx, dy);
        super.onDraw(canvas);
	}
}
