package org.kotemaru.android.bizcard.model;


public class CardModel {
	private static final Kind[] sKindValues = Kind.values();

	private int mId = -1;
	private final CharSequence[] mValues;

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

	public void put(Kind kind, CharSequence value) {
		int index = getKindIndex(kind);
		mValues[index] = value;
	}
	public CharSequence get(Kind kind) {
		int index = getKindIndex(kind);
		return mValues[index];
	}

}
