package edu.stanford.junction.sample.jxwhiteboard;

import edu.stanford.junction.addon.JSONObjWrapper;
import edu.stanford.junction.sample.jxwhiteboard.intents.WhiteboardIntents;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.os.IBinder;
import android.os.Handler;
import android.os.Message;
import android.content.ServiceConnection;
import android.app.ListActivity;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.content.Intent;
import android.content.ComponentName;

import org.json.JSONObject;
import java.util.*;
import java.text.DateFormat;

public class FindWhiteboardsActivity extends ListActivity implements OnItemClickListener{

    private Handler mMainHandler;
    private ArrayAdapter<JSONObject> mAdverts;

	private final DateFormat dateFormat = DateFormat.getDateTimeInstance();

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAdverts = new ArrayAdapter<JSONObject>(this, 
												android.R.layout.simple_list_item_1,
												new ArrayList<JSONObject>());
		setListAdapter(mAdverts);
		getListView().setTextFilterEnabled(true);
		getListView().setOnItemClickListener(this); 
		bindLiaisonService();
	}

    public void onItemClick(AdapterView parent, View v, int position, long id){
		Intent intent = new Intent();
		JSONObject advert = (JSONObject)mAdverts.getItem(position);
		String url = advert.optString("url");
		intent.putExtra(WhiteboardIntents.EXTRA_SESSION_URL, url);
		setResult(RESULT_OK, intent);
		Toast.makeText(this, "Connecting to '" + advert.optString("name") + 
					   "', please wait...", Toast.LENGTH_SHORT).show();
		finish();
    }

	private LiaisonService mBoundService;
	private IBinder mServiceBinder;

	private void refreshAdverts(Set<JSONObject> adverts){
		mAdverts.setNotifyOnChange(false);
		mAdverts.clear();
		for(JSONObject a : adverts){
			JSONObject advert = new JSONObjWrapper(a){
					public String toString(){ 
						String name = optString("name");
						Date d = new Date(optLong("time"));
						return  name + " - " + dateFormat.format(d); 
					}
				};
			mAdverts.add(advert);
		}
		mAdverts.setNotifyOnChange(true);
		mAdverts.notifyDataSetChanged();
	}

	private ServiceConnection mConnection = new ServiceConnection() {
			public void onServiceConnected(ComponentName className, IBinder service) {
				// This is called when the connection with the service has been
				// established, giving us the service object we can use to
				// interact with the service.  Because we have bound to a explicit
				// service that we know is running in our own process, we can
				// cast its IBinder to a concrete class and directly access it.
				mBoundService = ((LiaisonService.LiaisonBinder)service).getService();
				mBoundService.addHandler(new Handler() {
						public void handleMessage(Message m) {
							Set<JSONObject> adverts = (Set<JSONObject>) m.obj;
							refreshAdverts(adverts);
						}
					});
				Set<JSONObject> adverts = mBoundService.getAdverts();
				refreshAdverts(adverts);
				Toast.makeText(
					FindWhiteboardsActivity.this, 
					"Found " + adverts.size() + 
					" public Whiteboards. Scanning for new advertisements...",
					Toast.LENGTH_SHORT).show();
			}

			public void onServiceDisconnected(ComponentName className){
				// This is called when the connection with the service has been
				// unexpectedly disconnected -- that is, its process crashed.
				// Because it is running in our same process, we should never
				// see this happen.
				mBoundService = null;
				Toast.makeText(FindWhiteboardsActivity.this, 
							   R.string.liaison_service_disconnected,
							   Toast.LENGTH_SHORT).show();
			}
		};



	private void bindLiaisonService(){
		// Bind the service in main activity - to make sure it's started and working.
		bindService(new Intent(this, LiaisonService.class), 
					mConnection, Context.BIND_AUTO_CREATE);
	}


	public void onDestroy(){
		super.onDestroy();
		unbindService(mConnection);
	}

}




