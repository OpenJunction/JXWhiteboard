package edu.stanford.junction.sample.jxwhiteboard;

import android.app.Service;  
import android.content.Intent;    
import android.util.Log;   
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.IBinder;
import android.os.Binder;

import org.json.*;
import java.net.*;  
import java.util.*;  

import edu.stanford.junction.addon.JSONObjWrapper;
import edu.stanford.junction.android.AndroidJunctionMaker;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;
import edu.stanford.junction.provider.xmpp.XMPPSwitchboardConfig;

public class LiaisonService extends Service {

    private Intent mIntent;
    private Handler mMainHandler;
    private Set<JSONObject> mAdverts = new HashSet<JSONObject>();
    private Set<JSONObject> mAdvertised = new HashSet<JSONObject>();
    private List<Handler> mHandlers = new ArrayList<Handler>();

	private static String ROLE_LIAISON = "liaison";

	private static int EXTRA_LIAISON_ACTIVITY_URL = 0;
	private static String MSG_TYPE_ADVERT = "advert";
	private static String MSG_TYPE_UNADVERT = "unadvert";
	private static String MSG_TYPE_ADVERT_QUERY = "advertQuery";

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LiaisonBinder extends Binder {
        LiaisonService getService() {
            return LiaisonService.this;
        }
    }

    public interface LiaisonListener {
        void onActivityAdvert(JSONObject obj);
    }

	synchronized public void addHandler(Handler l){
		mHandlers.add(l);
	}

	synchronized public void removeHandler(Handler l){
		mHandlers.remove(l);
	}

	synchronized private void dispatchChanged(){
		for(Handler h : mHandlers){
			Message m = h.obtainMessage();
			m.obj = getAdverts();
			h.sendMessage(m);
			Log.i("LiaisonService", "Sent message to handler!");
		}
	}

	/**
	 * Return an unmodifiable set of all advertisements received
	 * thus far.
	 */
	public Set<JSONObject> getAdverts(){
		return Collections.unmodifiableSet(mAdverts);
	}

	private boolean connected(){
		return mActor != null && mActor.getJunction() != null;
	}

	public void advertiseActivity(String name, String uri){
		if(connected()){
			try {
				JSONObject msg = new JSONObject();
				msg.put("type", MSG_TYPE_ADVERT);
				msg.put("id", (name + uri).hashCode());
				msg.put("name", name);
				msg.put("time", (new Date()).getTime());
				msg.put("url", uri);
				mAdvertised.add(new JSONObjWrapper(msg));
				mActor.sendMessageToRole(ROLE_LIAISON, msg);
			} catch (Exception e) {
				Log.e("LiaisonService", "Oops: " + e);
			}
		}
	}

	public void unadvertiseActivity(String name, String uri){
		unadvertiseById((name + uri).hashCode());
	}

	public void unadvertiseById(int id){
		if(connected()){
			try {
				JSONObject msg = new JSONObject();
				msg.put("type", MSG_TYPE_UNADVERT);
				msg.put("id", id);
				mAdvertised.remove(new JSONObjWrapper(msg));
				mActor.sendMessageToRole(ROLE_LIAISON, msg);
			} catch (Exception e) {
				Log.e("LiaisonService", "Oops: " + e);
			}
		}
	}

	public void unadvertiseAll(){
		if(connected()){
			for(JSONObject o : mAdvertised){
				unadvertiseById(o.optInt("id"));
			}
		}
	}

	public void renewAdverts(){
		if(connected()){
			for(JSONObject o : mAdvertised){
				mActor.sendMessageToRole(ROLE_LIAISON, o);
			}
		}
	}

	public void renewAdverts(String toActor){
		if(connected()){
			for(JSONObject o : mAdvertised){
				mActor.sendMessageToActor(toActor, o);
			}
		}
	}

	public void sendAdvertQuery(){
		if(mActor != null){
			try {
				JSONObject msg = new JSONObject();
				msg.put("type", MSG_TYPE_ADVERT_QUERY);
				mActor.sendMessageToRole(ROLE_LIAISON, msg);
			} catch (Exception e) {
				Log.e("LiaisonService", "Oops: " + e);
			}
		}
	}

	private final JunctionActor mActor = new JunctionActor(ROLE_LIAISON){
			@Override
			public void onActivityJoin(){
				Log.i("LiaisonService", "Liaison joined the lobby.");
				sendAdvertQuery();
			}
			@Override
			public void onMessageReceived(MessageHeader header, JSONObject msg) {
				Log.i("LiaisonService", "Liaison got msg!");
				String type = msg.optString("type");

				if(type == null) return;

				if(type.equals(LiaisonService.MSG_TYPE_ADVERT)){
					JSONObject advert = new JSONObjWrapper(msg);
					// Note, we intentionally include adverts that we posted ourselves.
					mAdverts.add(new JSONObjWrapper(msg));
					dispatchChanged();
				}
				else if(type.equals(LiaisonService.MSG_TYPE_UNADVERT)){
					mAdverts.remove(new JSONObjWrapper(msg));
					dispatchChanged();
				}
				else if(type.equals(LiaisonService.MSG_TYPE_ADVERT_QUERY)){
					renewAdverts(header.getSender());
				}
				else{
					Log.d("LiaisonService", "Unrecognized message type: " + type);
				}
			}
		};

	public void init(final Uri uri){
		new Thread(){
			public void run(){
				try {
					URI url = new URI(uri.toString());
					final XMPPSwitchboardConfig sb = new XMPPSwitchboardConfig(uri.getAuthority());
					AndroidJunctionMaker.getInstance(sb).newJunction(url, mActor);
					Log.i("LiaisonService", "LiasonService connected.");
				}
				catch (Exception e) {
					Log.e("LiaisonService", "Ooops! " + e);
				}
			}
		}.start();
	}

	@Override
	public void onCreate() {}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		Log.i("LiaisonService", "Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}


	@Override
	public void onDestroy() {
		unadvertiseAll();
		super.onDestroy();
	}


	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}


	// This is the object that receives interactions from clients.  See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new LiaisonBinder();


}