
package edu.stanford.junction.sample.jxwhiteboard;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

public class ImageToWhiteboardActivity extends FragmentActivity {
    private static final String TAG = "wbi";

    private static final int REQUEST_CONTENT = 7;

    // onCreate, startActivityForResult, getImageToWhiteboard

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);

        Intent gallery = new Intent(Intent.ACTION_GET_CONTENT);
        gallery.setType("image/*");
        startActivityForResult(Intent.createChooser(gallery, null), REQUEST_CONTENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CONTENT) {
            if (resultCode != RESULT_OK) {
                finish();
                return;
            }

            // Query gallery for camera picture via
            // Android ContentResolver interface
            Context context = this;
            Uri imageUri = data.getData();
            ContentResolver cr = context.getContentResolver();
            InputStream is;
            try {
                is = cr.openInputStream(imageUri);
            } catch (IOException e) {
                Log.e(TAG, "Bad file " + imageUri, e);
                return;
            }
            // Get binary bytes for encode
            byte[] imgData = getBytesFromFile(is);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 8;
            Bitmap sourceBitmap = BitmapFactory
                    .decodeByteArray(imgData, 0, imgData.length, options);

            // Bitmap sourceBitmap = Media.getBitmap(getContentResolver(),
            // Uri.fromFile(file) );
            int width = sourceBitmap.getWidth();
            int height = sourceBitmap.getHeight();
            int cropSize = Math.min(width, height);

            int targetSize = 50;
            float scaleSize = ((float) targetSize) / cropSize;

            Matrix matrix = new Matrix();
            matrix.postScale(scaleSize, scaleSize);
            float rotation = rotationForImage(context, imageUri);
            if (rotation != 0f) {
                matrix.preRotate(rotation);
            }

            Bitmap resizedBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, width, height, matrix,
                    true);
            width = resizedBitmap.getWidth();
            height = resizedBitmap.getHeight();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            long total = 0;
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            for (int x = 0; x < width; x++) {
                int rowTotal = 0;
                for (int y = 0; y < height; y++) {
                    rowTotal += colorValue(resizedBitmap.getPixel(x, y));
                    if (y == height - 1) {
                        total += (rowTotal / height);
                    }
                }
            }

            
            /*
            StringBuilder points = new StringBuilder("");
            long average = total / width;
            float factor = 740 / width;
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int pixel = resizedBitmap.getPixel(x, y);
                    if (colorValue(pixel) > average*1.2) {
                        points.append(","+(x*factor)+","+(y*factor));
                    } else {

                    }
                }
            }

            String json = "{\"items\":[{\"id\": 270663982,\"color\":\"#000000\", \"width\":4, \"points\": [" +
                    points.substring(1) + "]}]}";
          */

            // horribly wasteful! todo: add point-based parameters. eg, "p":[0,1,2,3]
            // the above demonstrates that approach.

            try {
                int id = 0;
                JSONObject drawing = new JSONObject();
                JSONArray items = new JSONArray();
    
                long average = total / width;
                float factor = 740 / width;
                for (int x = 1; x < width; x++) {
                    for (int y = 1; y < height; y++) {
                        int pixel = resizedBitmap.getPixel(x, y);
                        if (colorValue(pixel) > average) {

                            JSONObject obj = new JSONObject();
                            obj.put("id", id++);
                            obj.put("color", "#000000");

                            JSONArray dpx = new JSONArray();
                            dpx.put((x-1)*factor);
                            dpx.put((y-1)*factor);
                            dpx.put(x*factor);
                            dpx.put(y*factor);
                            obj.put("points", dpx);
                            items.put(obj);
                        } else {
    
                        }
                    }
                }
    
                drawing.put("items", items);
                Intent imprt = new Intent(getIntent());
                imprt.setClass(this, JXWhiteboardActivity.class);
                imprt.putExtra("boardString", drawing.toString());
                imprt.putExtra("boardSeq", 1);
                startActivity(imprt);
                finish();
            } catch (JSONException e) {
                Log.e(TAG, "piss off json", e);
            }
        }
    }

    public static float exifOrientationToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

    public static float rotationForImage(Context context, Uri uri) {
        if (uri.getScheme().equals("content")) {
            String[] projection = {
                Images.ImageColumns.ORIENTATION
            };
            Cursor c = context.getContentResolver().query(uri, projection, null, null, null);
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
        } else if (uri.getScheme().equals("file")) {
            try {
                ExifInterface exif = new ExifInterface(uri.getPath());
                int rotation = (int) exifOrientationToDegrees(exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
                return rotation;
            } catch (IOException e) {
                Log.e(TAG, "Error checking exif", e);
            }
        }
        return 0f;
    }

    private static byte[] getBytesFromFile(InputStream is) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
            return buffer.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "Error reading bytes from file", e);
            return null;
        }
    }

    private int colorValue(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        return Math.round((float)Math.sqrt(r*r + g*g + b*b));
    }
}
