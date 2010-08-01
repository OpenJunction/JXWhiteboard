package edu.stanford.junction.sample.jxwhiteboard.widget;

import android.view.View;

/**
 * Interface for notifications of position change of slider.
 * 
 * @author Peli
 */
public interface OnLineWidthChangedListener {

	/**
	 * This method is called when the user selects a new width
	 * 
	 */
	void onLineWidthPicked(View view, int newWidth);
}
