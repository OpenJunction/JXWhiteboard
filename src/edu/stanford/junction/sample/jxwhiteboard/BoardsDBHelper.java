package edu.stanford.junction.sample.jxwhiteboard;
import android.database.sqlite.*;
import android.content.Context;

public class BoardsDBHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 2;
	private static final String DATABASE_NAME = "JXWhiteboardDB";
	private static final String DICTIONARY_TABLE_NAME = "boards";
	private static final String DICTIONARY_TABLE_CREATE =
		"CREATE TABLE " + DICTIONARY_TABLE_NAME + " (name TEXT, data TEXT, seqNum INTEGER);";

	BoardsDBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DICTIONARY_TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db ,int from ,int to){}

}

