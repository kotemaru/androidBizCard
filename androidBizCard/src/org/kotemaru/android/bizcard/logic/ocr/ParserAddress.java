package org.kotemaru.android.bizcard.logic.ocr;

import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.bizcard.model.Kind;

import android.graphics.Rect;

public class ParserAddress implements Parser {
	public Kind getKind() {
		return Kind.ADDRESS;
	}
	public void parse(CardModel model, ImageAnalyzer analyzer) {
		WordInfo winfo = ParserUtil.searchKeyword(
				analyzer.mDataSetJpn.words,
				"〒", "東京都", "北海道", "大阪府", "京都府", "県", "丁目");
		if (winfo == null) return;
		Rect rect = analyzer.getNearWordsRect(analyzer.mDataSetJpn, winfo);
		parse(model, analyzer, rect);
	}
	public void parse(CardModel model, ImageAnalyzer analyzer, Rect rect) {
		String word = analyzer.getBestString(analyzer.mDataSetJpn, rect);
		model.put(getKind(), word);
	}
}
