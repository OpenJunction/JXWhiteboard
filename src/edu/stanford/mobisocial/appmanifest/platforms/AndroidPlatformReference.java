package edu.stanford.mobisocial.appmanifest.platforms;

import edu.stanford.mobisocial.appmanifest.ApplicationManifest;

public class AndroidPlatformReference extends PlatformReference {
	public static final int VERSION_GINGERBREAD = 0x10;
	public static final int VERSION_FROYO = 0x08;
	public static final int VERSION_ECLAIRE = 0x07;
	public static final int VERSION_DONUT = 0x04;
	public static final int VERSION_CUPCAKE = 0x03;
	public static final int VERSION_ANY = 0x0;
	
	private int platformVersion;
	private int deviceModality = ApplicationManifest.MODALITY_UNSPECIFIED;
	private byte[] appReference;
	
	public AndroidPlatformReference(int version, String pkg, String argument) {
		platformVersion = version;
		appReference = (pkg + ":" + argument).getBytes();
	}

	@Override
	public int getPlatformIdentifier() {
		return ApplicationManifest.PLATFORM_ANDROID_PACKAGE;
	}

	@Override
	public int getPlatformVersion() {
		return platformVersion;
	}

	@Override
	public int getDeviceModality() {
		return deviceModality;
	}

	@Override
	public byte[] getAppReference() {
		return appReference;
	}
}