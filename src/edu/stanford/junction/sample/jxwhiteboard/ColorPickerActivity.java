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

package edu.stanford.junction.sample.jxwhiteboard;

import edu.stanford.junction.sample.jxwhiteboard.intents.WhiteboardIntents;
import edu.stanford.junction.sample.jxwhiteboard.widget.ColorCircle;
import edu.stanford.junction.sample.jxwhiteboard.widget.ColorSlider;
import edu.stanford.junction.sample.jxwhiteboard.widget.OnColorChangedListener;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

public class ColorPickerActivity extends Activity 
	implements OnColorChangedListener {
	
	ColorCircle mColorCircle;
	ColorSlider mSaturation;
	ColorSlider mValue;
	
	Intent mIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
        setContentView(R.layout.colorpicker);

        // Get original color
        mIntent = getIntent();
        if (mIntent == null) {
        	mIntent = new Intent();
        }
        

        int color;
        final ColorPickerState state = (ColorPickerState) getLastNonConfigurationInstance();
        if (state != null) {
        	color = state.mColor;
        } else {
        	color = mIntent.getIntExtra(WhiteboardIntents.EXTRA_COLOR, Color.BLACK);
        }

        mColorCircle = (ColorCircle) findViewById(R.id.colorcircle);
        mColorCircle.setOnColorChangedListener(this);
        mColorCircle.setColor(color);

        mSaturation = (ColorSlider) findViewById(R.id.saturation);
        mSaturation.setOnColorChangedListener(this);
        mSaturation.setColors(color, Color.BLACK);

        mValue = (ColorSlider) findViewById(R.id.value);
        mValue.setOnColorChangedListener(this);
        mValue.setColors(Color.WHITE, color);
	}
	
	
    class ColorPickerState {
    	int mColor;
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
    	ColorPickerState state = new ColorPickerState();
    	state.mColor = this.mColorCircle.getColor();
        return state;
    }
	
	

	public int toGray(int color) {
		int a = Color.alpha(color);
		int r = Color.red(color);
		int g = Color.green(color);
		int b = Color.blue(color);
		int gray = (r + g + b) / 3;
		return Color.argb(a, gray, gray, gray);
	}
	
	
	public void onColorChanged(View view, int newColor) {
		if (view == mColorCircle) {
			mValue.setColors(0xFFFFFFFF, newColor);
	        mSaturation.setColors(newColor, 0xff000000);
		} else if (view == mSaturation) {
			mColorCircle.setColor(newColor);
			mValue.setColors(0xFFFFFFFF, newColor);
		} else if (view == mValue) {
			mColorCircle.setColor(newColor);
		}
		
	}

	
	public void onColorPicked(View view, int newColor) {
		// We can return result
		mIntent.putExtra(WhiteboardIntents.EXTRA_COLOR, newColor);
		setResult(RESULT_OK, mIntent);
		finish();
	}
}
