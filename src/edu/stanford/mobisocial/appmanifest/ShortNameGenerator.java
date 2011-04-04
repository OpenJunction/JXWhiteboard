package edu.stanford.mobisocial.appmanifest;

import java.nio.ByteBuffer;

public class ShortNameGenerator {

	public static void main(String[] args) {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.put((byte)'A');
		buffer.put((byte)'N');
		buffer.put((byte)'D');
		buffer.put((byte)'P');
		
		buffer.position(0);
		System.out.println("0x" + Integer.toHexString(buffer.getInt()));
	}
}