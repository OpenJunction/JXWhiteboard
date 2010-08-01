package edu.stanford.junction.sample.jxwhiteboard;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Date;
import org.json.*;
import android.util.Log;
import edu.stanford.junction.props2.IPropChangeListener;
import edu.stanford.junction.props2.sample.ListProp;

public class WhiteboardProp extends ListProp {

	public WhiteboardProp(String propName){
		super(propName, propName + (new Random()).nextInt());
	}

	public JSONObject newStroke(int color, int width, List<Integer> points){
		JSONObject obj = new JSONObject();
		try{
			obj.put("id", (new Random()).nextInt());
			obj.put("color", "#" + Integer.toHexString(color));
			obj.put("width", width);
			JSONArray a = new JSONArray();
			for(Integer i : points){
				a.put(i.intValue());
			}
			obj.put("points", a);
		}
		catch(JSONException e){}
		return obj;
	}

}