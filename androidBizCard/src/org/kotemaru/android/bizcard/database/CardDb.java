package org.kotemaru.android.bizcard.database;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.bizcard.model.CardModel.Kind;
import org.kotemaru.android.fw.util.sql.SqlUtil;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class CardDb {
	static final String DB_NAME = "card.db";
	static final int VERSION = 100;


	// テーブル定義
	private static final String CARD_TABLE = "CARD_TABLE";

	public enum Card implements SqlUtil.Column {
		// @formatter:off
		_ID				("integer primary key autoincrement"),
		ACCESS_DATE		("text"), // "yyyy/MM/dd hh:mm"
		CREATE_DATE		("text"), // "yyyy/MM/dd hh:mm"
		UPDATE_DATE		("text"), // "yyyy/MM/dd hh:mm"
		COMPANY			("text"),
		ORGANIZATION	("text"),
		POSITION		("text"),
		NAME			("text"),
		KANA			("text"),
		ADDRESS			("text"),
		TEL				("text"),
		MOBILE			("text"),
		FAX				("text"),
		EMAIL			("text"),
		WEB				("text"),
		NONTS			("text"),
		IMAGE_URL		("text"),
		TAG1			("text"),
		TAG2			("text"),
		TAG3			("text"),
		// @formatter:on
		;

		// --- 以下、定形 (enumは継承が出来ないので) ---
		private final int mDbVersion;
		private final String mType;
		private final String mWhere;

		Card(String type) {
			this(0, type);
		}
		Card(int dbVer, String type) {
			mDbVersion = dbVer;
			mType = type;
			mWhere = name() + "=?";
		}
		// @formatter:off
		public int getDbVersion() {return mDbVersion;}
		public String type() {return mType;}
		public String where() {return mWhere;}
		public long getLong(Cursor cursor) {return cursor.getLong(cursor.getColumnIndex(name()));}
		public int getInt(Cursor cursor) {return cursor.getInt(cursor.getColumnIndex(name()));}
		public String getString(Cursor cursor) {return cursor.getString(cursor.getColumnIndex(name()));}
		public void put(ContentValues values, long val) {values.put(name(), val);}
		public void put(ContentValues values, int val) {values.put(name(), val);}
		public void put(ContentValues values, String val) {values.put(name(), val);}
		// @formatter:on
	}


	private static class SqlHelper extends SQLiteOpenHelper {
		SqlHelper(Context context) {
			super(context, DB_NAME, null, VERSION);
		}
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(SqlUtil.getCreateTableDDL(CARD_TABLE, Card.values()));
			db.execSQL("CREATE INDEX COMPANY_INDEX ON " + CARD_TABLE + "(" + Card.COMPANY + ")");
			db.execSQL("CREATE INDEX ACCESS_DATE_INDEX ON " + CARD_TABLE + "(" + Card.ACCESS_DATE + ")");
			db.execSQL("CREATE INDEX TAG1_INDEX ON " + CARD_TABLE + "(" + Card.TAG1 + ")");
			db.execSQL("CREATE INDEX TAG2_INDEX ON " + CARD_TABLE + "(" + Card.TAG2 + ")");
			db.execSQL("CREATE INDEX TAG3_INDEX ON " + CARD_TABLE + "(" + Card.TAG3 + ")");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// This is first version.

			// Upgrade code sample.
			// switch (oldVersion) {
			// case 100:
			// db.execSQL(SqlUtil.getAlterTableDDL(101, TABLE, COLS.values()));
			// case 101:
			// db.execSQL(SqlUtil.getAlterTableDDL(102, TABLE, COLS.values()));
			// default:
			// break;
			// }
		}
	}

	private SqlHelper mSqlHelper;

	public CardDb(Context context) {
		mSqlHelper = new SqlHelper(context);
	}
	private CardModel toModel(Cursor cursor) {
		CardModel model = new CardModel();
		// @formatter:off
		model.setId(Card._ID.getInt(cursor));
		model.put(Kind.ACCESS_DATE  , Card.ACCESS_DATE .getString(cursor));
		model.put(Kind.CREATE_DATE  , Card.CREATE_DATE .getString(cursor));
		model.put(Kind.UPDATE_DATE  , Card.UPDATE_DATE .getString(cursor));

		model.put(Kind.COMPANY      , Card.COMPANY     .getString(cursor));
		model.put(Kind.ORGANIZATION , Card.ORGANIZATION.getString(cursor));
		model.put(Kind.POSITION     , Card.POSITION    .getString(cursor));
		model.put(Kind.NAME         , Card.NAME        .getString(cursor));
		model.put(Kind.KANA         , Card.KANA        .getString(cursor));
		model.put(Kind.ADDRESS      , Card.ADDRESS     .getString(cursor));
		model.put(Kind.TEL          , Card.TEL         .getString(cursor));
		model.put(Kind.MOBILE       , Card.MOBILE      .getString(cursor));
		model.put(Kind.FAX          , Card.FAX         .getString(cursor));
		model.put(Kind.EMAIL        , Card.EMAIL       .getString(cursor));
		model.put(Kind.WEB          , Card.WEB         .getString(cursor));
		model.put(Kind.NONTS        , Card.NONTS       .getString(cursor));
		model.put(Kind.IMAGE_URL    , Card.IMAGE_URL   .getString(cursor));
		// @formatter:on
		return model;
	}
	private ContentValues fromModel(CardModel model) {
		ContentValues values = new ContentValues();
		// @formatter:off
		Card._ID.put(values, model.getId());
		Card.ACCESS_DATE .put(values, model.get(Kind.ACCESS_DATE));
		Card.CREATE_DATE .put(values, model.get(Kind.CREATE_DATE));
		Card.UPDATE_DATE .put(values, model.get(Kind.UPDATE_DATE));

		Card.COMPANY     .put(values, model.get(Kind.COMPANY ));
		Card.ORGANIZATION.put(values, model.get(Kind.ORGANIZATION  ));
		Card.POSITION    .put(values, model.get(Kind.POSITION      ));
		Card.NAME        .put(values, model.get(Kind.NAME          ));
		Card.KANA        .put(values, model.get(Kind.KANA          ));
		Card.ADDRESS     .put(values, model.get(Kind.ADDRESS       ));
		Card.TEL         .put(values, model.get(Kind.TEL           ));
		Card.MOBILE      .put(values, model.get(Kind.MOBILE        ));
		Card.FAX         .put(values, model.get(Kind.FAX           ));
		Card.EMAIL       .put(values, model.get(Kind.EMAIL         ));
		Card.WEB         .put(values, model.get(Kind.WEB           ));
		Card.NONTS       .put(values, model.get(Kind.NONTS         ));
		Card.IMAGE_URL   .put(values, model.get(Kind.IMAGE_URL     ));
		// @formatter:on
		return values;
	}

	public boolean putCardModel(CardModel model) {
		SQLiteDatabase db = mSqlHelper.getWritableDatabase();
		ContentValues values = fromModel(model);
		if (model.getId() == -1) {
			values.remove(Card._ID.name());
			values.put(Card.CREATE_DATE.name(), getDateString(new Date()));
			long rowId = db.insert(CARD_TABLE, null, values);
			return rowId != -1;
		} else {
			values.put(Card.UPDATE_DATE.name(), getDateString(new Date()));
			int count = db.update(CARD_TABLE, values, Card._ID.where(), toArgs(model.getId()));
			return count == 1;
		}
	}
	public boolean setAccessDate(int id, Date date) {
		SQLiteDatabase db = mSqlHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(Card.ACCESS_DATE.name(), getDateString(date));
		int count = db.update(CARD_TABLE, values, Card._ID.where(), toArgs(id));
		return count == 1;
	}

	public CardModel getCardModel(int id) {
		SQLiteDatabase db = mSqlHelper.getReadableDatabase();
		Cursor cursor = db.query(CARD_TABLE, null, Card._ID.where(), toArgs(id), null, null, null);
		try {
			if (!cursor.moveToFirst()) return null;
			CardModel model = toModel(cursor);
			return model;
		} finally {
			cursor.close();
		}
	}

	public List<CardModel> getCardModelList() {
		SQLiteDatabase db = mSqlHelper.getReadableDatabase();
		Cursor cursor = db.query(CARD_TABLE, null, null, null, null, null, Card.COMPANY.name());
		try {
			List<CardModel> list = new ArrayList<CardModel>(cursor.getCount());
			while (cursor.moveToNext()) {
				CardModel model = toModel(cursor);
				list.add(model);
			}
			return list;
		} finally {
			cursor.close();
		}
	}


	private String[] toArgs(int id) {
		return new String[]{Integer.toString(id)};
	}

	private static SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US);
	public static String getDateString(Date date) {
		synchronized (sDateFormat) {
			return sDateFormat.format(date);
		}
	}


}
