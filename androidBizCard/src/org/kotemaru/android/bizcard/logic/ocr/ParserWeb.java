package org.kotemaru.android.bizcard.logic.ocr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.bizcard.model.Kind;

import android.graphics.Rect;

public class ParserWeb implements Parser {
	private static final Pattern URL_PATTERN =
			Pattern.compile("(http://|https://){1}[\\w\\.\\-/:\\#\\?\\=\\&\\;\\%\\~\\+]+",
					Pattern.CASE_INSENSITIVE);
	public Kind getKind() {
		return Kind.WEB;
	}
	public void parse(CardModel model, ImageAnalyzer analyzer) {
		String text = analyzer.mDataSetEng.text;
		Matcher m = URL_PATTERN.matcher(text);
		if (m.find()) {
			String word = text.substring(m.start(), m.end());
			model.put(getKind(), word);
		}
	}
	public void parse(CardModel model, ImageAnalyzer analyzer, Rect rect) {
		String text = analyzer.getBestString(analyzer.mDataSetEng, rect);
		model.put(getKind(), text);
	}
}
