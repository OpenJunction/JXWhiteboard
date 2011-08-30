package edu.stanford.junction.sample.jxwhiteboard;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mobisocial.nfc.Nfc;
import mobisocial.socialkit.musubi.AppState;
import mobisocial.socialkit.musubi.Musubi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import edu.stanford.junction.Junction;
import edu.stanford.junction.JunctionException;
import edu.stanford.junction.SwitchboardConfig;
import edu.stanford.junction.android.AndroidJunctionMaker;
import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.activity.JunctionExtra;
import edu.stanford.junction.api.messaging.MessageHeader;
import edu.stanford.junction.props2.IPropChangeListener;
import edu.stanford.junction.props2.IPropState;
import edu.stanford.junction.props2.IWithStateAction;
import edu.stanford.junction.props2.Prop;
import edu.stanford.junction.props2.sample.ListState;
import edu.stanford.junction.provider.xmpp.XMPPSwitchboardConfig;
import edu.stanford.junction.sample.jxwhiteboard.intents.WhiteboardIntents;
import edu.stanford.junction.sample.jxwhiteboard.util.Base64;
import edu.stanford.mobisocial.appmanifest.ApplicationManifest;
import edu.stanford.mobisocial.appmanifest.platforms.AndroidPlatformReference;
import edu.stanford.mobisocial.appmanifest.platforms.PlatformReference;
import edu.stanford.mobisocial.appmanifest.platforms.WebPlatformReference;

@SuppressWarnings("deprecation")
public class JXWhiteboardActivity extends Activity {

	private WhiteboardProp prop;

	public static final String TAG = "whiteboard";
	public static final boolean DBG = false;
	public static final String EXTRA_APP_ARGUMENT = "android.intent.extra.APPLICATION_ARGUMENT";
    private static final int REQUEST_CODE_PICK_COLOR = 1;
    private static final int REQUEST_CODE_PICK_LINE_WIDTH = 2;
    private static final int REQUEST_CODE_LOAD_WHITEBOARD = 4;


    private static final int ERASE_COLOR = 0xFFFFFF;
    private static final int ERASE_WIDTH = 30;
    private static final int UPDATE_FREQUENCY = 10; // 9999999; // 4 for realtime.

    private ActivityScript mScript = null;
    
	private int currentColor = 0x000000;
	private int currentWidth = 3;
	private boolean eraseMode = false;
	private Bitmap mBackgroundImage = null;
	private DrawingPanel panel = null;
	private PropUpdateThread mPropUpdateThread;

	public static final int SET_COLOR = 0;
	public static final int SET_LINE_WIDTH = 1;
	public static final int START_ERASER = 2;
	public static final int STOP_ERASER = 3;
	public static final int CLEAR = 4;
	public static final int EXIT = 5;
	public static final int SHARE_SNAPSHOT = 6;
	public static final int LOAD_BOARD = 10;
	public static final int SAVE_BOARD = 11;
	public static final String DEFAULT_HOST = "junction://prpl.stanford.edu";

    private static final int VIRTUAL_WIDTH = 768;
    private int localWidth = 0;

    private Nfc mNfc = null;
    private String mAppArgument = null;
    private boolean mPausingInternal = false;
    private boolean mResumingInternal = false;
    private boolean mConnectedToJunction = false;
    private Uri mJunctionUri;
    private boolean mIsDirty = false; // Updated since load?

    private Musubi mMusubi;

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		panel = new DrawingPanel(this);
		mPropUpdateThread = new PropUpdateThread();
		mPropUpdateThread.start();
		setContentView(panel);

		mNfc = new Nfc(this);
		Intent intent = getIntent();
		SavedBoard savedBoard = null;
		if (Musubi.isDungbeetleIntent(intent)) { // SocialKit.hasFeed(intent)
		    mMusubi = Musubi.getInstance(this, intent);
		    JSONObject state = mMusubi.getFeed().getLatestState();
		    if (state != null) {
                savedBoard = new SavedBoard("loaded", state.optString("data"), state.optLong("seq"));
                if (DBG) Log.d(TAG, "loading whiteboard state " + savedBoard.data + ", " + savedBoard.seqNum);
		    }
		}
		initBoard(savedBoard);

		mScript = new ActivityScript();
		mScript.setFriendlyName("JXWhiteboard");
		JSONObject androidPlatform = new JSONObject();
		try {
			androidPlatform.put("package", this.getPackageName());
			androidPlatform.put("url", "http://openjunction.org/demos/jxwhiteboard.apk");
		} catch (JSONException e) { }
		
		mScript.addRolePlatform("participant", "android", androidPlatform);
		
		if (getIntent() != null && getIntent().hasExtra(EXTRA_APP_ARGUMENT)) {
		    mAppArgument = getIntent().getStringExtra(EXTRA_APP_ARGUMENT);
            Log.i("JXWhiteboard", "Got app argument: " + mAppArgument);
		}
		
		Uri sessionUri;
		if (AndroidJunctionMaker.isJoinable(this)) {
			sessionUri = Uri.parse(getIntent().getStringExtra("invitationURI"));
		} else if (mAppArgument != null && mAppArgument.length() > 0) {
		    Log.i("JXWhiteboard", "Got app argument: " + mAppArgument);
			sessionUri = Uri.parse(mAppArgument);
		} else if (mMusubi != null) {
           sessionUri = Uri.parse(mMusubi.getFeed().getJunction().getInvitationURI().toString());
        } else {
            //sessionUri = fixedSessionUri("whiteboard");
			sessionUri = newRandomSessionUri();
		}
		mJunctionUri = sessionUri;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mNfc.onResume(this);
		if (!mResumingInternal) {
		    initJunction(mJunctionUri);
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mNfc.onPause(this);
		if (!mPausingInternal && mIsDirty && mMusubi != null) {
		    sendToDungbeetle();
		}
		if (mPausingInternal) {
		    mResumingInternal = true;
		} else if (mConnectedToJunction) {
		    closeJunction();
		}
		mPausingInternal = false;
		mIsDirty = false;
	}
	
	@Override
    protected void onNewIntent(Intent intent) {
    	if (mNfc.onNewIntent(this, intent)) return;
    }

    class DrawingPanel extends SurfaceView implements SurfaceHolder.Callback {
        private Map<Integer, List<Integer>> currentStrokes = new HashMap<Integer, List<Integer>>();
        private SurfaceHolder _surfaceHolder;

        public DrawingPanel(Context context) {
            super(context);
            getHolder().addCallback(this);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            repaint(true);
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            int action = ev.getAction();

            switch (action & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN: {
                    startStroke(0, 0, ev);
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    finishStroke(0, 0, ev);
                    mIsDirty = true;
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    updateStrokes(ev);
                    break;
                }
                case MotionEvent.ACTION_POINTER_UP: {
                    final int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK)
                            >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                    final int pointerId = ev.getPointerId(pointerIndex);
                    finishStroke(pointerId, pointerIndex, ev);
                    break;
                }
                case MotionEvent.ACTION_POINTER_DOWN: {
                    final int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK)
                            >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                    final int pointerId = ev.getPointerId(pointerIndex);
                    startStroke(pointerId, pointerIndex, ev);
                    break;
                }
                case MotionEvent.ACTION_CANCEL: {
                    Log.d(TAG, "Cancelled a gesture");
                    currentStrokes.clear();
                    break;
                }
            }
            return true;
        }

        void updateStrokes(MotionEvent ev) {
            final int historySize = ev.getHistorySize();
            final int pointerCount = ev.getPointerCount();
            for (int p = 0; p < pointerCount; p++) {
                int id = ev.getPointerId(p);
                List<Integer> currentPoints = currentStrokes.get(id);
                for (int h = 0; h < historySize; h++) {
                    currentPoints.add(localToVirt(ev.getHistoricalX(p, h)));
                    currentPoints.add(localToVirt(ev.getHistoricalY(p, h)));
                }
            }

            for (int p = 0; p < pointerCount; p++) {
                int id = ev.getPointerId(p);
                List<Integer> currentPoints = currentStrokes.get(id);
                currentPoints.add(localToVirt(ev.getX(p)));
                currentPoints.add(localToVirt(ev.getY(p)));
                if (currentPoints.size() >= UPDATE_FREQUENCY) {
                    sendStroke(currentPoints);
                    currentPoints.clear();
                    currentPoints.add(localToVirt(ev.getX(p)));
                    currentPoints.add(localToVirt(ev.getY(p)));
                }
            }
            repaint(false);
        }

        private void startStroke(int pointerId, int pointerIndex, MotionEvent ev) {
            if (!currentStrokes.containsKey(pointerId)) {
                currentStrokes.put(pointerId, new ArrayList<Integer>());
            } else {
                currentStrokes.get(pointerId).clear();
            }
            currentStrokes.get(pointerId).add(localToVirt(ev.getX(pointerIndex)));
            currentStrokes.get(pointerId).add(localToVirt(ev.getY(pointerIndex)));
        }

        private void finishStroke(int pointerId, int pointerIndex, MotionEvent ev) {
            List<Integer> stroke = currentStrokes.get(pointerId);
            if (stroke == null) {
                return;
            }
            if (stroke.size() <= 4) {
                // fun effect :)
                //stroke.add(0);
                //stroke.add(0);

                int x = localToVirt(ev.getX(pointerIndex));
                int y = localToVirt(ev.getY(pointerIndex));

                stroke.add(x - 1);
                stroke.add(y - 1);

                stroke.add(x + 1);
                stroke.add(y + 1);
            } else {
                stroke.add(localToVirt(ev.getX(pointerIndex)));
                stroke.add(localToVirt(ev.getY(pointerIndex)));
            }
            sendStroke(stroke);
            stroke.clear();
            repaint(false);
        }

        private void sendStroke(List<Integer> stroke) {
            Message msg = mPropUpdateThread.mHandler
                    .obtainMessage(PropUpdateThread.MSG_ADD_STROKE);
            List<Integer> out = new ArrayList<Integer>(stroke);
            msg.obj = out;

            if (eraseMode) {
                msg.arg1 = ERASE_COLOR;
                msg.arg2 = ERASE_WIDTH;
            } else {
                msg.arg1 = currentColor;
                msg.arg2 = localToVirt(currentWidth);
            }
            mPropUpdateThread.mHandler.handleMessage(msg);
        }

		protected void paintState(final Canvas canvas){
			final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			mPaint.setDither(true);
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeJoin(Paint.Join.ROUND);
			mPaint.setStrokeCap(Paint.Cap.ROUND);

			// clear canvas
			canvas.drawColor(0xFFFFFFFF);

			// paint prop state
            prop.withState(new IWithStateAction<Void>(){
                    public Void run(IPropState state){
                        for (JSONObject o : prop.items()) {
                            int color = Integer.parseInt(o.optString("color").substring(1), 16);
                            mPaint.setColor(0xFF000000 | color);
                            mPaint.setStrokeWidth(virtToLocal(o.optInt("width")));
                            JSONArray points = o.optJSONArray("points");
                            paintStroke(canvas, mPaint, points);
                        }
                        return null;
                    }
                });
			paintCurrentStrokes(canvas);
		}

        protected void paintCurrentStrokes(Canvas canvas) {
            Paint paint = new Paint();
            paint.setDither(true);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);

            // Draw stroke-in-progress
            if (eraseMode) {
                paint.setColor(0xFF000000 | ERASE_COLOR);
                paint.setStrokeWidth(ERASE_WIDTH);
            } else {
                paint.setColor(0xFF000000 | currentColor);
                paint.setStrokeWidth(virtToLocal(localToVirt(currentWidth)));
            }
            for (List<Integer> points : currentStrokes.values()) {
                paintStroke(canvas, paint, points);
            }
        }

		protected void paintStroke(Canvas canvas, Paint paint, List<Integer> points){
			if(points.size() >= 4){
				int x1, y1, x2, y2;
				x1 = virtToLocal(points.get(0));
				y1 = virtToLocal(points.get(1));
				for(int i = 2; i < points.size(); i += 2) {
					x2 = virtToLocal(points.get(i));
					y2 = virtToLocal(points.get(i+1));
					canvas.drawLine(x1,y1,x2,y2, paint);
					x1 = x2;
					y1 = y2;
				}
			}
		}

		protected void paintStroke(Canvas canvas, Paint paint, JSONArray points){
			if(points.length() >= 4){
				int x1, y1, x2, y2;
				x1 = virtToLocal(points.optInt(0));
				y1 = virtToLocal(points.optInt(1));
				for(int i = 2; i < points.length(); i += 2) {
					x2 = virtToLocal(points.optInt(i));
					y2 = virtToLocal(points.optInt(i+1));
					canvas.drawLine(x1,y1,x2,y2, paint);
					x1 = x2;
					y1 = y2;
				}
			}
		}
		
        public void repaint(boolean all) {
            if (_surfaceHolder != null && mBackgroundImage != null) {
                Canvas canvas = new Canvas(mBackgroundImage);
                if (all) {
                    paintState(canvas);
                } else {
                    paintCurrentStrokes(canvas);
                }
                try {
                    synchronized (_surfaceHolder) {
                        canvas = _surfaceHolder.lockCanvas(null);
                        canvas.drawBitmap(mBackgroundImage, 0, 0, null);
                    }
                } finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (canvas != null) {
                        _surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }

		public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {
			localWidth = width;
			mBackgroundImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			repaint(true);
		}
		
		public void surfaceCreated(SurfaceHolder holder) {
			_surfaceHolder = holder;
			repaint(true);
		}
		
		public void surfaceDestroyed(SurfaceHolder holder) {
			_surfaceHolder = null;
		}
	}


	private boolean externalStorageReadableAndWritable() {
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			return true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			return false;
		} else {
			// Something else is wrong. It may be one of many other states, 
			//  but all we need to know is we can neither read nor write
			return false;
		}
	}

	private void shareSnapshot() {
	    Snapshot snapshot = captureSnapshot();

	    final Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
	    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Whiteboard Snapshot");
        shareIntent.putExtra(Intent.EXTRA_STREAM, snapshot.uri);
        shareIntent.setType(snapshot.type);
        startActivity(Intent.createChooser(shareIntent, "Share Snapshot..."));
	}

	class Snapshot {
	    String type;
	    Uri uri;
	    
	    public Snapshot(String type, Uri uri) {
	        this.type = type;
	        this.uri = uri;
	    }
	}

	private String captureThumbnailBase64() {
	    Bitmap bitmap = mBackgroundImage;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int targetHeight= 240;
        float scale = ((float) targetHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.PNG, 80, byteStream);
        byte[] data = byteStream.toByteArray();
        return Base64.encodeToString(data, false);
	}

	private Snapshot captureSnapshot() {
		if(mBackgroundImage != null) {
			String filename = "jxwhiteboard_tmp_output.png";
			// If sd card is available, we'll write the full quality image there
			// before sending..
			if(externalStorageReadableAndWritable()){
				Bitmap bitmap = mBackgroundImage;
				File png = new File(Environment.getExternalStorageDirectory(), filename);
				if (png.exists()) {
				    try {
				        png.delete();
				        png = new File(Environment.getExternalStorageDirectory(), filename);
				    } catch (Exception e) {
				        Log.e(TAG, "error removing file", e);
				    }
				}
				FileOutputStream out = null;
				try {
					out = new FileOutputStream(png);
					bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
					out.flush();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try {
						if (out != null) out.close();
					}
					catch (IOException ignore) {}
				}
				return new Snapshot("image/png", Uri.fromFile(png));
				
			}
			// Otherwise, use mediastore, which unfortunately compresses the hell
			// out of the image.
			else{
				String url = MediaStore.Images.Media.insertImage(
					getContentResolver(), mBackgroundImage, filename, null);
				return new Snapshot("image/jpg", Uri.parse(url));
			}
		}
		return null;
	}

	public boolean onCreateOptionsMenu(Menu menu){
		return true;
	}

	public boolean onPreparePanel(int featureId, View view, Menu menu){
	    MenuItem item;
        menu.clear();
        if (eraseMode) {
            menu.add(0, STOP_ERASER, 0, "Stop Erasing");
            menu.add(0, CLEAR, 0, "Clear All");
            menu.add(0, SHARE_SNAPSHOT, 0, "Send Snapshot");
            menu.add(0, LOAD_BOARD, 0, "Load a Saved Board");
            menu.add(0, SAVE_BOARD, 0, "Save Board");
            menu.add(0, EXIT, 0, "Exit");
        } else {
            item = menu.add(0, SET_COLOR, 0, "Set Color");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
            item = menu.add(0, SET_LINE_WIDTH, 0, "Set Line Width");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
            item = menu.add(0, START_ERASER, 0, "Eraser");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
            item = menu.add(0, CLEAR, 0, "Clear All");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
            menu.add(0, SHARE_SNAPSHOT, 0, "Send Snapshot");
            menu.add(0, LOAD_BOARD, 0, "Load a Saved Board");
            menu.add(0, SAVE_BOARD, 0, "Save Board");
            menu.add(0, EXIT, 0, "Exit");
        }
        return true;
    }

	public boolean onOptionsItemSelected (MenuItem item){
		switch (item.getItemId()){
		case SET_COLOR:
		    mPausingInternal = true;
			pickColor();
			return true;
		case SET_LINE_WIDTH:
		    mPausingInternal = true;
			pickLineWidth();
			return true;
		case START_ERASER:
			eraseMode = true;
			return true;
		case STOP_ERASER:
			eraseMode = false;
			return true;
		case CLEAR:
			prop.clear();
			return true;
		case SHARE_SNAPSHOT:
		    mPausingInternal = true;
			shareSnapshot();
			return true;
		case LOAD_BOARD:
		    mPausingInternal = true;
			loadSavedBoard();
			return true;
		case SAVE_BOARD:
			saveBoard();
			return true;
		case EXIT:
			Process.killProcess(Process.myPid());
		}
		return false;
	}

	private void pickColor(){
		Intent i = new Intent();
		i.setAction(WhiteboardIntents.ACTION_PICK_COLOR);
		i.putExtra(WhiteboardIntents.EXTRA_COLOR, 0xFF000000 | currentColor );
		startActivityForResult(i, REQUEST_CODE_PICK_COLOR);
	}

	private void pickLineWidth(){
		Intent i = new Intent();
		i.setAction(WhiteboardIntents.ACTION_PICK_LINE_WIDTH);
		i.putExtra(WhiteboardIntents.EXTRA_LINE_WIDTH, currentWidth );
		startActivityForResult(i, REQUEST_CODE_PICK_LINE_WIDTH);
	}

	private void loadSavedBoard(){
		Intent i = new Intent();
		i.setAction(WhiteboardIntents.ACTION_LOAD_SAVED_BOARD);
		startActivityForResult(i, REQUEST_CODE_LOAD_WHITEBOARD);
	}

	private void saveBoard() {
		final Junction jx = mActor.getJunction();
		if(jx != null){
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle(R.string.advertise_dialog_title);  
			alert.setMessage("Name this Whiteboard");
			final EditText input = new EditText(this);  
			alert.setView(input);
			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {  
					public void onClick(DialogInterface dialog, int whichButton){
						String name = input.getText().toString();
						JSONObject state = prop.stateToJSON();
						long seq = prop.getSequenceNum();
						Log.d(TAG, "saving whiteboard state " + state);
						saveBoardToDB(name, state, seq);
						Toast.makeText(JXWhiteboardActivity.this, "Saved",
						        Toast.LENGTH_SHORT).show();
					}
				});  
			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {  
					public void onClick(DialogInterface dialog, int whichButton) {}  
				});  
			alert.show();  
			return;
		}
	}

	private void saveBoardToDB(String name, JSONObject data, long seqNum){
		SQLiteOpenHelper helper = new BoardsDBHelper(this);
		SQLiteDatabase db = helper.getWritableDatabase();
		ContentValues vals = new ContentValues();
		vals.put("name", name);
		vals.put("data", data.toString());
		vals.put("seqNum", seqNum);
		try{
			db.insertOrThrow("boards", null, vals);
		}
		catch(Exception e){
			Log.e("JXWhiteboardActivity", e.toString());
		}
		helper.close();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode) {
		case REQUEST_CODE_PICK_COLOR:
			if(resultCode == RESULT_OK){
				currentColor = data.getIntExtra(
					WhiteboardIntents.EXTRA_COLOR, currentColor);
				// Get rid of alpha information
				currentColor &= 0x00FFFFFF;
			}
			break;
		case REQUEST_CODE_PICK_LINE_WIDTH:
			if(resultCode == RESULT_OK){
				currentWidth = data.getIntExtra(
					WhiteboardIntents.EXTRA_LINE_WIDTH, currentWidth);
			}
			break;
		case REQUEST_CODE_LOAD_WHITEBOARD:
			if(resultCode == RESULT_OK){
				String name = data.getStringExtra(
					WhiteboardIntents.EXTRA_SAVED_BOARD_NAME);
				String d = data.getStringExtra(
					WhiteboardIntents.EXTRA_SAVED_BOARD_DATA);
				long seqNum = data.getLongExtra(
					WhiteboardIntents.EXTRA_SAVED_BOARD_SEQNUM, 0);
				Log.d(TAG, "loading: " + seqNum + ", " + d);
				SavedBoard b = new SavedBoard(name, d, seqNum);
				initBoard(b);
				initJunction(newRandomSessionUri());
			}
			break;
		}
	}

	private Uri newRandomSessionUri(){
		/*
          String randomSession = UUID.randomUUID().toString().substring(0,8);
          return Uri.parse(DEFAULT_HOST + "/" + randomSession  + "#xmpp");
		*/
		SwitchboardConfig config = new XMPPSwitchboardConfig("prpl.stanford.edu");
		URI uri = AndroidJunctionMaker.getInstance(config).generateSessionUri();
		return Uri.parse(uri.toString());
	}

	@SuppressWarnings("unused")
    private Uri fixedSessionUri(String sessId){
		return Uri.parse(DEFAULT_HOST + "/" + sessId );
	}

	private void initBoard(SavedBoard savedBoard) {
	    if (savedBoard != null) {
            JSONObject obj = savedBoard.obj();
            JSONArray items = obj.optJSONArray("items");
            ArrayList<JSONObject> strokes = new ArrayList<JSONObject>();
            if (items != null) {
                for (int i = 0; i < items.length(); i++) {
                    JSONObject stroke = items.optJSONObject(i);
                    strokes.add(stroke);
                }
            }
            ListState state = new ListState(strokes);
            long seqNum = savedBoard.seqNum;
            prop = new WhiteboardProp("whiteboard_model", state, seqNum);
        } else {
            prop = new WhiteboardProp("whiteboard_model");
        }
        prop.addChangeListener(new IPropChangeListener() {
            public String getType() {
                return Prop.EVT_ANY;
            }

            public void onChange(Object data) {
                JXWhiteboardActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        // panel.invalidate();
                        panel.repaint(true);
                    }
                });
            }
        });
	}

    private void initJunction(final Uri uri) {
        final URI url;
        try {
            url = new URI(uri.toString());
        } catch (URISyntaxException e) {
            Log.e("JXWhiteboardActivity", "Failed to parse uri: " + uri.toString());
            return;
        }

        final SwitchboardConfig sb = AndroidJunctionMaker.getDefaultSwitchboardConfig(url);
        if (sb instanceof XMPPSwitchboardConfig) {
            ((XMPPSwitchboardConfig) sb).setConnectionTimeout(10000);
        }

        new AsyncTask<Void, Void, Boolean>() {
            private ProgressDialog mmProgress = new ProgressDialog(JXWhiteboardActivity.this);
            private JunctionException mmException;
            private boolean mmCancelled;

            @Override
            protected Boolean doInBackground(Void... arg0) {
                try {
                    AndroidJunctionMaker.getInstance(sb).newJunction(url, mScript, mActor);
                    mConnectedToJunction = true;
                } catch (JunctionException e) {
                    mmException = e;
                    return false;
                }
                return true;
            }

            @Override
            protected void onPreExecute() {
                mmCancelled = false;
                mmProgress.setTitle("Connecting to session");
                mmProgress.setMessage("Connecting...");
                mmProgress.setIndeterminate(true);
                mmProgress.setCancelable(true);
                mmProgress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface arg0) {
                        mmCancelled = true;
                    }
                });
                mmProgress.show();
            }

            @Override
            protected void onPostExecute(Boolean result) {
                mmProgress.dismiss();
                if (!result && !mmCancelled) {
                    maybeRetryJunction(uri, mmException);
                }
            };
        }.execute();
    }

    private void closeJunction() {
        mActor.leave();
    }

	private void maybeRetryJunction(final Uri uri, final JunctionException e){
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Connection Failed");  
		alert.setMessage("Failed to connect to Whiteboard. " + 
						 e.getWrappedThrowable().getMessage() + 
						 ". Retry connection?");
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {  
				public void onClick(DialogInterface dialog, int whichButton){  
					initJunction(uri);
				}
			});  
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {  
				public void onClick(DialogInterface dialog, int whichButton) {}
			});  
		alert.show();  
		return;
	}

	public void onDestroy(){
		super.onDestroy();
		//unbindService(mConnection);
	}

	private int virtToLocal(float val){
		float ratio = (float)localWidth/(float)VIRTUAL_WIDTH;
		return (int)(val * ratio);
	}

	private int virtToLocal(int val){
		return virtToLocal((float)val);
	}

	private int localToVirt(float val){
		float ratio = (float)VIRTUAL_WIDTH/(float)localWidth;
		return (int)(val * ratio);
	}

	private int localToVirt(int val){
		return localToVirt((float)val);
	}

	class PropUpdateThread extends Thread {
	    static final int MSG_ADD_STROKE = 1;
	    private Handler mHandler;

	    @Override
	    public void run() {
            Looper.prepare();

            mHandler = new Handler() {
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MSG_ADD_STROKE:
                            int color = msg.arg1;
                            int width = msg.arg2;
                            @SuppressWarnings("unchecked")
                            List<Integer> points = (List<Integer>)msg.obj;

                            prop.add(prop.newStroke(color, width, points));
                            break;
                    }
                }
            };
            Looper.loop();
        }
	}

	private boolean doNFCBroadcast() {
		if (mActor == null || mActor.getJunction() == null) {
			return false;
		}

		// hack! old code all over the place, broken whiteboards, etc!
		String sessionId = mActor.getJunction().getSessionID();
        String switchboard = mActor.getJunction().getSwitchboard();
		String webUrl = "http://prpl.stanford.edu/junction/whiteboard2/?jxsessionid="+sessionId+"&jxswitchboard="+switchboard;
		PlatformReference webReference = new WebPlatformReference(webUrl);
		PlatformReference androidReference = new AndroidPlatformReference(0x09, getPackageName(), mActor.getJunction().getInvitationURI().toString());
   		ApplicationManifest appManifest = new ApplicationManifest.Builder()
   			.addPlatformReference(webReference)
   			.addPlatformReference(androidReference)
   			.setName("weScribble")
   			.create();
   		
   		try {
	   		byte[] appManifestBytes = appManifest.toByteArray();
	   		mNfc.share(ApplicationManifest.MIME_TYPE, appManifestBytes);
   		} catch (NoClassDefFoundError e) {}
   		
   		return true;
	}

	private void sendToDungbeetle() {
        // TODO: Whiteboard content provider via content corral?
        try {
            JSONObject state = new JSONObject();
            state.put("data", prop.stateToJSON().toString());
            state.put("seq", prop.getSequenceNum());
            AppState appState = new AppState.Builder()
                .setState(state)
                .setThumbnailB64Image(captureThumbnailBase64())
                .setArgument(mAppArgument).build();
            mMusubi.getFeed().postAppState(appState);
        } catch (JSONException e) {}
	}

	@SuppressWarnings("unused")
    private void toast(final String text) {
	    Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}

	JunctionActor mActor = new JunctionActor("participant") {
        @Override
        public void onActivityJoin() {
            doNFCBroadcast();
            System.out.println("joined!");
        }

        @Override
        public void onMessageReceived(MessageHeader header, JSONObject msg) {
            // System.out.println("Got msg!");
        }

        @Override
        public List<JunctionExtra> getInitialExtras() {
            ArrayList<JunctionExtra> extras = new ArrayList<JunctionExtra>();
            extras.add(prop);
            return extras;
        }

        @Override
        public void onActivityCreate() {

        }
    };
}