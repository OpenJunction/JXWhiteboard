
package edu.stanford.junction.sample.jxwhiteboard.util;


public class StringUtils {

    static final String HEXES = "0123456789ABCDEF";

    public static String getHex(byte[] raw) {
        if (raw == null) {
            return null;
        }
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }

    public static String getHex(byte b) {
        return "" + HEXES.charAt((b & 0xF0) >> 4) + HEXES.charAt(b & 0x0F);
    }

    public static String getHex(int raw) {
        final StringBuilder hex = new StringBuilder(8);
        byte b;

        b = (byte)(raw >> 24 & 0xFF); 
        hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));

        b = (byte)(raw >> 16 & 0xFF); 
        hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));

        b = (byte)(raw >> 8 & 0xFF); 
        hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));

        b = (byte)(raw & 0xFF); 
        hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));

        return hex.toString();
    }
}
