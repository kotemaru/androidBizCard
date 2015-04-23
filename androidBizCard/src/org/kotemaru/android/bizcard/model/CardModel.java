package org.kotemaru.android.bizcard.model;

import org.kotemaru.android.bizcard.R;

public class CardModel {
	public enum Kind {
		// @formatter:off
		NIL(0),
		ACCESS_DATE		(R.string.kind_access_date	),
		CREATE_DATE		(R.string.kind_create_date	),
		UPDATE_DATE		(R.string.kind_update_date	),
		COMPANY			(R.string.kind_company		,R.id.company),
		ORGANIZATION	(R.string.kind_organization	,R.id.organization	),
		POSITION		(R.string.kind_position		,R.id.position		),
		NAME			(R.string.kind_name			,R.id.name			),
		KANA			(R.string.kind_kana			,R.id.kana			),
		ADDRESS			(R.string.kind_address		,R.id.address		),
		TEL				(R.string.kind_tel			,R.id.tel			),
		MOBILE			(R.string.kind_mobile		,R.id.mobile		),
		FAX				(R.string.kind_fax			,R.id.fax			),
		EMAIL			(R.string.kind_email		,R.id.email		),
		WEB				(R.string.kind_web			,R.id.web			),
		NONTS			(R.string.kind_notes		,R.id.notes		),
		IMAGE_URL		(R.string.kind_image_url	),
		// @formatter:oｎ
		;

		public final int labelResId;
		public final int textViewResId;
		Kind(int labelResId) {
			this(labelResId, 0);
		}
		Kind(int labelResId, int textViewResId) {
			this.labelResId = labelResId;
			this.textViewResId = textViewResId;
		}

		public static Kind toKind(String name) {
			if (name == null) return Kind.NIL;
			return Kind.valueOf(name);
		}
	}
	private static final Kind[] sKindValues = Kind.values();

	private int mId = -1;
	private final String[] mValues;

	public CardModel() {
		mValues = new String[sKindValues.length];
	}
	private int getKindIndex(Kind kind) {
		for (int i=0;i<sKindValues.length;i++) {
			if (sKindValues[i] == kind) return i;
		}
		return -1;
	}

	public int getId() {
		return mId;
	}
	public void setId(int id) {
		mId = id;
	}

	public void put(Kind kind, String value) {
		int index = getKindIndex(kind);
		mValues[index] = value;
	}
	public String get(Kind kind) {
		int index = getKindIndex(kind);
		return mValues[index];
	}

}
