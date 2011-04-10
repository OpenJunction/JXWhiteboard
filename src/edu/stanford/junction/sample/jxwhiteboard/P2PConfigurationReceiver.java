package edu.stanford.junction.sample.jxwhiteboard;

import edu.stanford.junction.android.AndroidJunctionMaker;
import edu.stanford.junction.provider.xmpp.XMPPSwitchboardConfig;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * A broadcast receiver for configuring an argument that
 * can be used to launch a P2P session.
 *
 */
public class P2PConfigurationReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		XMPPSwitchboardConfig xmppConfig = new XMPPSwitchboardConfig("prpl.stanford.edu");
		String config = AndroidJunctionMaker.getInstance(xmppConfig).generateSessionUri().toString();
        Log.i("P2PConfigurationReceiver", "Generated session: " + config);
		setResultData(config);
	}
}
