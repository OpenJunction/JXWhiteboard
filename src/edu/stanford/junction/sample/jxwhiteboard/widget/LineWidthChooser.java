/* 
 * Copyright (C) 2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.stanford.junction.sample.jxwhiteboard.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;


public class LineWidthChooser extends View {

	/** Default widget width */
	public int defaultWidth = 200;

	/** Default widget height */
	public int defaultHeight = 300;
	
    private Paint mPaint;

	private int mWidth;

    private OnLineWidthChangedListener mListener;

    private Integer[] WIDTHS = new Integer[]{
		1,3,7,13,20,30
	};

	/**
	 * Constructor. This version is only needed for instantiating the object
	 * manually (not from a layout XML file).
	 * 
	 * @param context
	 */
	public LineWidthChooser(Context context) {
		super(context);
		init();
	}

	/**
	 * Construct object, initializing with any attributes we understand from a
	 * layout file.
	 * 
	 * These attributes are defined in res/values/attrs.xml .
	 * 
	 * @see android.view.View#View(android.content.Context,
	 *      android.util.AttributeSet, java.util.Map)
	 */
	public LineWidthChooser(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO what happens with inflateParams
		init();
	}

	/**
	 * Initializes variables.
	 */
	void init() {
		mWidth = 3;
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaint.setDither(true);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
	}

    @Override 
		protected void onDraw(Canvas canvas) {

		int step = getHeight() / WIDTHS.length;
		for(int i = 0; i < WIDTHS.length; i++){
			if(mWidth == WIDTHS[i]) mPaint.setColor(0xFFAAAAAA);
			else mPaint.setColor(0xFFFFFFFF);
			mPaint.setStrokeWidth(WIDTHS[i]);
			canvas.drawLine(0,step*i,getWidth(),step*i, mPaint);
		}
    }
    

	/**
	 * @see android.view.View#measure(int, int)
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(measureWidth(widthMeasureSpec),
							 measureHeight(heightMeasureSpec));
	}

	/**
	 * Determines the width of this view
	 * 
	 * @param measureSpec
	 *            A measureSpec packed into an int
	 * @return The width of the view, honoring constraints from measureSpec
	 */
	private int measureWidth(int measureSpec) {
		int result = 0;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);

		if (specMode == MeasureSpec.EXACTLY) {
			// We were told how big to be
			result = specSize;
		} else {
			// Default width:
			result = defaultWidth;
			if (specMode == MeasureSpec.AT_MOST) {
				// Respect AT_MOST value if that was what is called for by
				// measureSpec
				result = Math.min(result, specSize);
			}
		}
		return result;
	}

	/**
	 * Determines the height of this view
	 * 
	 * @param measureSpec
	 *            A measureSpec packed into an int
	 * @return The height of the view, honoring constraints from measureSpec
	 */
	private int measureHeight(int measureSpec) {
		int result = 0;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);

		if (specMode == MeasureSpec.EXACTLY) {
			// We were told how big to be
			result = specSize;
		} else {
			// Default height
			result = defaultHeight;
			if (specMode == MeasureSpec.AT_MOST) {
				// Respect AT_MOST value if that was what is called for by
				// measureSpec
				result = Math.min(result, specSize);
			}
		}
		return result;
	}
    
	public void setLineWidth(int width) {
		mWidth = width;
        invalidate();
	}

	public void setOnLineWidthChangedListener(OnLineWidthChangedListener l) {
		mListener = l;
	}
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			float unit = (float) y / ((float) getHeight()); 
			int index = Math.min((int)Math.floor(unit * (float)WIDTHS.length), 
								 WIDTHS.length - 1);
			mWidth = WIDTHS[index];
			invalidate();
			break;
		case MotionEvent.ACTION_MOVE:
			break;
		case MotionEvent.ACTION_UP:
			if (mListener != null) {
				mListener.onLineWidthPicked(this, mWidth);
			}
			invalidate();
			break;
        }
        return true;
    }
}
