package org.kotemaru.android.bizcard.logic.ocr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.bizcard.model.Kind;

import android.graphics.Rect;

public class ParserOrganization implements Parser {
	private static final Pattern ORG_PATTERN_1 = Pattern.compile("[部課]$");
	private static final Pattern ORG_PATTERN_2 = Pattern.compile("ユニット$");

	public Kind getKind() {
		return Kind.ORGANIZATION;
	}
	public void parse(CardModel model, ImageAnalyzer analyzer) {

		StringBuilder sbuf = new StringBuilder();
		for (WordInfo winfo : analyzer.mDataSetJpn.words) {
			String word = winfo.word.trim();
			Matcher m1 = ORG_PATTERN_1.matcher(word);
			Matcher m2 = ORG_PATTERN_2.matcher(word);
			if (m1.find()) {
				sbuf.append(' ').append(analyzer.getBestString(analyzer.mDataSetJpn, winfo.rect));
			} else if (m2.find()) {
				sbuf.append(' ').append(analyzer.getBestString(analyzer.mDataSetJpn, winfo.rect));
			}
		}
		model.put(getKind(), sbuf.toString().trim());
	}
	public void parse(CardModel model, ImageAnalyzer analyzer, Rect rect) {
		String word = analyzer.getBestString(analyzer.mDataSetJpn, rect);
		model.put(getKind(), word);
	}
}
