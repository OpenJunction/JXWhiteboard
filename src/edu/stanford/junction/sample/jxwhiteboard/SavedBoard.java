package edu.stanford.junction.sample.jxwhiteboard;

import org.json.*;

import android.util.Log;

import java.net.URI;
import java.util.*;


public class SavedBoard{
	public final String data;
	public final String name;
	public final long seqNum;

	public SavedBoard(final String name, final String data, final long seqNum){
		this.name = name;
		this.data = data;
		this.seqNum = seqNum;
	}

	public JSONObject obj(){
		try{
			return new JSONObject(data);
		}
		catch(JSONException e){
		    Log.e("loadboard", "Error retrieving json ", e);
			return new JSONObject();
		}
	}

	public String toString(){
		return name + " @ " + seqNum;
	}
}
