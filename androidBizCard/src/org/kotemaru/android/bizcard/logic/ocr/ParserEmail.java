package org.kotemaru.android.bizcard.logic.ocr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.bizcard.model.Kind;

import android.graphics.Rect;

public class ParserEmail implements Parser {
	private static final Pattern EMAIL_PATTERN = Pattern.compile("[0-9a-zA-Z_-]+@([0-9a-zA-Z_-]+[.])+[a-zA-Z]{2,3}");

	public Kind getKind() {
		return Kind.EMAIL;
	}
	public void parse(CardModel model, ImageAnalyzer analyzer) {
		String text = analyzer.mDataSetEng.text.replace('®', '@').replace('©', '@');
		Matcher m = EMAIL_PATTERN.matcher(text);
		if (m.find()) {
			String word = text.substring(m.start(), m.end());
			model.put(getKind(), word);
		}
	}
	public void parse(CardModel model, ImageAnalyzer analyzer, Rect rect) {
		String text = analyzer.getBestString(analyzer.mDataSetEng, rect);
		text = text.replace('®', '@').replace('©', '@');
		Matcher m = EMAIL_PATTERN.matcher(text);
		if (m.find()) {
			String word = text.substring(m.start(), m.end());
			model.put(getKind(), word);
		} else {
			model.put(getKind(), text);
		}
	}
}
