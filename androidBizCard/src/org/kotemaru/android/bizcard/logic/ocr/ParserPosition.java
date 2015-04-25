package org.kotemaru.android.bizcard.logic.ocr;

import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.bizcard.model.Kind;

import android.graphics.Rect;

public class ParserPosition implements Parser {
	public Kind getKind() {
		return Kind.POSITION;
	}
	public void parse(CardModel model, ImageAnalyzer analyzer) {
		WordInfo winfo = ParserUtil.searchKeyword(
				analyzer.mDataSetJpn.words,
				"代表", "CEO", "部長", "課長", "主任",
				"チーフ", "マネージャ", "プロデューサ", "ディレクタ");
		if (winfo == null) return;
		Rect rect = analyzer.getNearWordsRect(analyzer.mDataSetJpn, winfo);
		parse(model, analyzer, rect);
	}
	public void parse(CardModel model, ImageAnalyzer analyzer, Rect rect) {
		String word = analyzer.getBestString(analyzer.mDataSetJpn, rect);
		model.put(getKind(), word);
	}
}
