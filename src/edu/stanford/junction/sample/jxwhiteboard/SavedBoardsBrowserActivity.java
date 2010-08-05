package edu.stanford.junction.sample.jxwhiteboard;

import edu.stanford.junction.sample.jxwhiteboard.intents.WhiteboardIntents;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.os.IBinder;
import android.os.Handler;
import android.os.Message;
import android.content.ServiceConnection;
import android.app.Service;
import android.app.ListActivity;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.content.Intent;
import android.content.ComponentName;
import android.util.Log;
import android.database.sqlite.*;
import android.database.Cursor;

import edu.stanford.junction.extra.JSONObjWrapper;

import org.json.JSONObject;
import java.net.URI;
import java.util.*;
import java.text.DateFormat;


public class SavedBoardsBrowserActivity extends ListActivity implements OnItemClickListener{

    private Handler mMainHandler;
    private ArrayAdapter<SavedBoard> mBoards;
	private final DateFormat dateFormat = DateFormat.getDateTimeInstance();


    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SQLiteOpenHelper helper = new BoardsDBHelper(this);
		SQLiteDatabase db = helper.getReadableDatabase();
		List<SavedBoard> boards = selectAllBoards(db);
		mBoards = new ArrayAdapter<SavedBoard>(this, 
											   android.R.layout.simple_list_item_1,
											   boards);
		setListAdapter(mBoards);
		getListView().setTextFilterEnabled(true);
		getListView().setOnItemClickListener(this); 
	}


	private List<SavedBoard> selectAllBoards(SQLiteDatabase db){
		ArrayList<SavedBoard> boards = new ArrayList<SavedBoard>();
		Cursor cursor = db.query("boards", new String[] {"name", "data", "seqNum"}, 
								 null, null, null, null, null);
		if(cursor.moveToFirst()){
			do {
				SavedBoard b = new SavedBoard(cursor.getString(0), cursor.getString(1), cursor.getLong(2));
				Log.d("SavedBoardsBrowserActivity", "got board: " + b);
				boards.add(b);
			} while(cursor.moveToNext());
		}
		if(cursor != null && !cursor.isClosed()){
			cursor.close();
		}
		Log.d("SavedBoardsBrowserActivity", "total boards loaded: " + boards.size());
		return boards;
	}


    public void onItemClick(AdapterView parent, View v, int position, long id){
		Intent intent = new Intent();
		SavedBoard b = (SavedBoard)mBoards.getItem(position);
		intent.putExtra(WhiteboardIntents.EXTRA_SAVED_BOARD_NAME, b.name);
		intent.putExtra(WhiteboardIntents.EXTRA_SAVED_BOARD_DATA, b.data);
		intent.putExtra(WhiteboardIntents.EXTRA_SAVED_BOARD_SEQNUM, b.seqNum);
		setResult(RESULT_OK, intent);
		finish();
    }


	public void onDestroy(){
		super.onDestroy();
	}

}




