package edu.stanford.junction.sample.jxwhiteboard;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.obj.MemObj;
import mobisocial.socialkit.musubi.Musubi;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.net.Uri;
import android.content.Intent;

import java.util.UUID;

/**
 * An activity invoked when the Whiteboard was instantiated from a
 * Musubi social context.
 */
public class SessionKickoffActivity extends Activity {
	public static final String TYPE = "wepaint";

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		// Create a session for others to join
		Musubi musubi = Musubi.forIntent(this, getIntent());
		Obj sharedSession = createAppSessionObj();
		Uri objUri = musubi.getFeed().insert(sharedSession);

		Intent view = getMusubiIntent(objUri);
		startActivity(view);
		finish();
	}

	Obj createAppSessionObj() {
		try {
			String sessionId = UUID.randomUUID().toString();
			JSONObject json = new JSONObject();
			json.put("session-uri", "junction://prpl.stanford.edu/" + sessionId); // sb.openjunction.org
			json.put("__html", "<span style='font-size:12dp;width:300px;'>Click to join me in a Whiteboard!</span>");
			return new MemObj(TYPE, json);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	protected Intent getMusubiIntent(Uri objUri) {
		Intent view = new Intent(Intent.ACTION_VIEW);
		view.setDataAndType(objUri, Musubi.mimeTypeFor(TYPE));
		view.putExtra("feedUri", getIntent().getParcelableExtra("feedUri"));
		return view;
	}
}
