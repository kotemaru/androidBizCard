package org.kotemaru.android.bizcard.logic.ocr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.bizcard.model.Kind;

import android.graphics.Rect;

public class ParserFax implements Parser {
	protected static final Pattern TEL_PATTERN = Pattern.compile("[0-9]{2,5}-[0-9]{2,4}-[0-9]{4}");
	public Kind getKind() {
		return Kind.FAX;
	}
	public void parse(CardModel model, ImageAnalyzer analyzer) {
	}
	public void parse(CardModel model, ImageAnalyzer analyzer, Rect rect) {
		String text = analyzer.getBestString(analyzer.mDataSetEng, rect);
		text = ParserTel.trim(text);
		Matcher m = TEL_PATTERN.matcher(text);
		if (m.find()) {
			String word = text.substring(m.start(), m.end());
			model.put(getKind(), word);
		} else {
			model.put(getKind(), text);
		}
	}
}
