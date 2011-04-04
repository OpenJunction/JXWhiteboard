package edu.stanford.mobisocial.appmanifest.test;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import edu.stanford.mobisocial.appmanifest.ApplicationManifest;
import edu.stanford.mobisocial.appmanifest.platforms.AndroidPlatformReference;
import edu.stanford.mobisocial.appmanifest.platforms.PlatformReference;
import edu.stanford.mobisocial.appmanifest.platforms.WebPlatformReference;


public class WeHoldEmTest {

	public static void main(String[] args) {
		String junctionUri = "junction://my.switchboard/sessionUri";
		String webUrl = null;
		try {
			webUrl = "http://prpl.stanford.edu/junction/poker?jxinvite=" + URLEncoder.encode(junctionUri, "UTF-8");
		} catch (UnsupportedEncodingException e) {}
		WebPlatformReference webReference = new WebPlatformReference(webUrl);
		webReference.setDeviceModality(ApplicationManifest.MODALITY_TELEVISION);
		
		PlatformReference androidReference = new AndroidPlatformReference(AndroidPlatformReference.VERSION_FROYO, "edu.stanford.prpl.junction.poker", junctionUri);
   		ApplicationManifest appManifest = new ApplicationManifest.Builder()
   			.addPlatformReference(webReference)
   			.addPlatformReference(androidReference)
   			.setName("weHold'Em")
   			.create();
	}
}
