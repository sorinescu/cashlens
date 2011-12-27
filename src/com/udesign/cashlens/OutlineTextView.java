package com.udesign.cashlens;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.PorterDuff.Mode;
import android.util.AttributeSet;
import android.widget.TextView;

public class OutlineTextView extends TextView
{
	private static final Paint BLACK_BORDER_PAINT = new Paint();
    private static final int BORDER_WIDTH = 1;

    static {
            BLACK_BORDER_PAINT.setXfermode(new PorterDuffXfermode(Mode.DST_OUT));
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

	/* (non-Javadoc)
	 * @see android.widget.TextView#onDraw(android.graphics.Canvas)
	 */
	@Override
	protected void onDraw(Canvas canvas)
	{
		if (true)
		{
			canvas.saveLayer(null, BLACK_BORDER_PAINT, Canvas.HAS_ALPHA_LAYER_SAVE_FLAG
	                | Canvas.FULL_COLOR_LAYER_SAVE_FLAG | Canvas.MATRIX_SAVE_FLAG);
			drawBackground(canvas, -BORDER_WIDTH, -BORDER_WIDTH);
			drawBackground(canvas, BORDER_WIDTH + BORDER_WIDTH, 0);
			drawBackground(canvas, 0, BORDER_WIDTH + BORDER_WIDTH);
			drawBackground(canvas, -BORDER_WIDTH - BORDER_WIDTH, 0);
			canvas.restore();
		}
		super.onDraw(canvas);
	}

	private void drawBackground(Canvas canvas, int dx, int dy) 
	{
		canvas.translate(dx, dy);
        super.onDraw(canvas);
	}
}
