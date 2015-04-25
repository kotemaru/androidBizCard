package org.kotemaru.android.bizcard.logic.ocr;

import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.bizcard.model.Kind;

import android.graphics.Rect;

public interface Parser {
	public Kind getKind();
	public void parse(CardModel model, ImageAnalyzer analyzer);
	public void parse(CardModel model, ImageAnalyzer analyzer, Rect rect);
}
