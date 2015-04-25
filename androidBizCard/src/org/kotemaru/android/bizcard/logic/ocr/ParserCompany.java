package org.kotemaru.android.bizcard.logic.ocr;

import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.bizcard.model.Kind;

import android.graphics.Rect;

public class ParserCompany implements Parser {
	public Kind getKind() {
		return Kind.COMPANY;
	}
	public void parse(CardModel model, ImageAnalyzer analyzer) {
		WordInfo winfo = ParserUtil.searchKeyword(
				analyzer.mDataSetJpn.words,
				"株式", "有限", "会社", "会ネ士", "会ネ土", "㈱", "㈲");
		if (winfo == null) return;
		Rect rect = analyzer.getNearWordsRect(analyzer.mDataSetJpn, winfo);
		parse(model, analyzer, rect);
	}
	public void parse(CardModel model, ImageAnalyzer analyzer, Rect rect) {
		String word = analyzer.getBestString(analyzer.mDataSetJpn, rect);
		model.put(getKind(), word);
	}
}
