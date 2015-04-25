package org.kotemaru.android.bizcard.logic.ocr;

import android.graphics.Rect;

import com.googlecode.leptonica.android.Box;

public class WordInfo {
	public final Rect rect;
	public final String word;
	public final float confidence;

	public WordInfo(WordInfo org) {
		this(org.rect, org.word, org.confidence);
	}
	public WordInfo(int[] rect, String word, float confidence) {
		this.rect = new Rect(
				rect[Box.INDEX_X],
				rect[Box.INDEX_Y],
				rect[Box.INDEX_W],
				rect[Box.INDEX_H]);
		this.word = word;
		this.confidence = confidence;
	}
	public WordInfo(Rect rect, String word, float confidence) {
		this.rect = rect;
		this.word = word;
		this.confidence = confidence;
	}

}
