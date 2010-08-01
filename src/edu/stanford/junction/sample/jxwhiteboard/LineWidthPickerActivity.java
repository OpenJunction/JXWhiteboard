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
import edu.stanford.junction.sample.jxwhiteboard.widget.LineWidthChooser;
import edu.stanford.junction.sample.jxwhiteboard.widget.OnLineWidthChangedListener;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

public class LineWidthPickerActivity extends Activity 
	implements OnLineWidthChangedListener {
	Intent mIntent;
	LineWidthChooser mChooser;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
        setContentView(R.layout.linewidthpicker);

        mIntent = getIntent();
        if (mIntent == null) {
        	mIntent = new Intent();
        }
		int width = mIntent.getIntExtra(WhiteboardIntents.EXTRA_LINE_WIDTH, 2);
		
        mChooser = (LineWidthChooser)findViewById(R.id.linewidthchooser);
        mChooser.setOnLineWidthChangedListener(this);
        mChooser.setLineWidth(width);
	}

	public void onLineWidthPicked(View view, int newWidth){
		mIntent.putExtra(WhiteboardIntents.EXTRA_LINE_WIDTH, newWidth);
		setResult(RESULT_OK, mIntent);
		finish();
	}

}
