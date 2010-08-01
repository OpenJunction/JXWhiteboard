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

package edu.stanford.junction.sample.jxwhiteboard.intents;


/**
 * @version 2009-02-08
 * 
 * @author Peli
 */
public final class WhiteboardIntents {


	/**
	 * Activity Action: Pick color.
	 * 
	 * <p>Displays a color picker. The color is returned in EXTRA_COLOR.</p>
	 * 
	 * <p>Constant Value: "org.openintents.action.SET_FLASHLIGHT_COLOR"</p>
	 */
	public final static String ACTION_PICK_COLOR = "edu.stanford.junction.sample.jxwhiteboard.action.PICK_COLOR";
	

	/**
	 * Color.
	 * 
	 * <p>Color as integer value, as used in setColor() and related.</p>
	 * 
	 * <p>Constant Value: "org.openintents.extra.COLOR"</p>
	 */
	public final static String EXTRA_COLOR = "edu.stanford.junction.sample.jxwhiteboard.extra.COLOR";





	/**
	 * Activity Action: Pick line_width.
	 * 
	 * <p>Displays a line_width picker. The line_width is returned in EXTRA_LINE_WIDTH.</p>
	 * 
	 * <p>Constant Value: "org.openintents.action.SET_FLASHLIGHT_LINE_WIDTH"</p>
	 */
	public final static String ACTION_PICK_LINE_WIDTH = "edu.stanford.junction.sample.jxwhiteboard.action.PICK_LINE_WIDTH";
	

	/**
	 * Line_Width.
	 * 
	 * <p>Line_Width as integer value, as used in setLine_Width() and related.</p>
	 * 
	 * <p>Constant Value: "org.openintents.extra.LINE_WIDTH"</p>
	 */
	public final static String EXTRA_LINE_WIDTH = "edu.stanford.junction.sample.jxwhiteboard.extra.LINE_WIDTH";





	/**
	 * Activity Action: Find a nearby (geographically) activity
	 */
	public final static String ACTION_FIND_WHITEBOARDS = "edu.stanford.junction.sample.jxwhiteboard.action.FIND_WHITEBOARDS";
	
	/**
	 * Session Url
	 * 
	 */
	public final static String EXTRA_SESSION_URL = "edu.stanford.junction.sample.jxwhiteboard.extra.SESSION_URL";


	/**
	 * Activity Action: Make my whiteboard known to the outside world.
	 */
	public final static String ACTION_BROADCAST_WHITEBOARD = "edu.stanford.junction.sample.jxwhiteboard.action.BROADCAST_WHITEBOARD";


	public final static String EXTRA_TWITTER_USERNAME = "edu.stanford.junction.sample.jxwhiteboard.extra.TWITTER_USERNAME";

	public final static String EXTRA_TWITTER_PASSWORD = "edu.stanford.junction.sample.jxwhiteboard.extra.TWITTER_PASSWORD";
	

}
