package org.kotemaru.android.bizcard.logic.ocr;

import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.bizcard.model.Kind;

import android.graphics.Rect;

public class ParserName implements Parser {
	public Kind getKind() {
		return Kind.NAME;
	}
	public void parse(CardModel model, ImageAnalyzer analyzer) {
		WordInfo maxSizeInfo = analyzer.mDataSetJpn.words.get(0);
		for (WordInfo winfo : analyzer.mDataSetJpn.words) {
			if (winfo.confidence > 50 && winfo.rect.height() > maxSizeInfo.rect.height()) {
				maxSizeInfo = winfo;
			}
		}
		Rect rect = analyzer.getNearWordsRect(analyzer.mDataSetJpn, maxSizeInfo);
		parse(model, analyzer, rect);
	}
	public void parse(CardModel model, ImageAnalyzer analyzer, Rect rect) {
		String word = analyzer.getBestString(analyzer.mDataSetJpn, rect);
		model.put(getKind(), word);
	}
}
