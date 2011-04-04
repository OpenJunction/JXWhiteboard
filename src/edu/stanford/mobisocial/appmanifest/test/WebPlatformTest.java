package edu.stanford.mobisocial.appmanifest.test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;

import edu.stanford.mobisocial.appmanifest.ApplicationManifest;
import edu.stanford.mobisocial.appmanifest.platforms.PlatformReference;
import edu.stanford.mobisocial.appmanifest.platforms.WebPlatformReference;

public class WebPlatformTest {

	public static void main(String... args) {
		PlatformReference webReference =
			new WebPlatformReference("http://openjunction.org/demo/whiteboard?jx=junction://sb.openjunction.org/myt3sts3s5i0n");
		ApplicationManifest manifest =
			new ApplicationManifest.Builder()
				.setName("weScribble")
				.addPlatformReference(webReference)
				.create();
		byte[] bytes = manifest.toByteArray();
		
		// Write to file
		try {
			FileOutputStream fout = new FileOutputStream(new File("/home/bjdodson/Desktop/manifest.apm"));
			fout.write(bytes);
			fout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		ApplicationManifest parsedManifest = new ApplicationManifest(bytes);
		
		System.out.println("Created application manifest of size " + bytes.length + " bytes.");
		
		if (!parsedManifest.getName().equals(manifest.getName())) {
			System.err.println("Mismatching names");
		}
		
		int platformCount = manifest.getPlatformReferences().size();
		if (platformCount != parsedManifest.getPlatformReferences().size()) {
			System.err.println("Mismatch platform reference length.");
			return;
		}
		
		System.out.println("Comparing " + platformCount + " references.");
		for (int i = 0; i < platformCount; i++) {
			PlatformReference original = manifest.getPlatformReferences().get(i);
			PlatformReference parsed = manifest.getPlatformReferences().get(i);
			
			if (original.getPlatformIdentifier() != parsed.getPlatformIdentifier()) {
				System.err.println("Mismatched platform identifier");
			}
			
			if (original.getPlatformVersion() != parsed.getPlatformVersion()) {
				System.err.println("Mismatched platform version");
			}
			
			if (original.getDeviceModality() != parsed.getDeviceModality()) {
				System.err.println("Mismatched device modality");
			}
			
			if (!Arrays.equals(original.getAppReference(), parsed.getAppReference())) {
				System.err.println("Mismatched platform arguments");
			}
			
			String arg = new String(parsed.getAppReference());
			System.out.println("Result: " + arg);
		}
		
		
	}
}
