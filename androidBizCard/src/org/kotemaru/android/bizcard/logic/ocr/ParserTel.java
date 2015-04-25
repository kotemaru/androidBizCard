package org.kotemaru.android.bizcard.logic.ocr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.bizcard.model.Kind;

import android.graphics.Rect;

public class ParserTel implements Parser {
	protected static final Pattern TEL_PATTERN =
			Pattern.compile("(FAX:?-?)?[0-9]{2,5}-[0-9]{2,4}-[0-9]{4}", Pattern.CASE_INSENSITIVE);
	protected static final Pattern MOBILE_HEADER_PATTERN =
			Pattern.compile("^0[789]0-");
	protected static final Pattern FAX_HEADER_PATTERN =
			Pattern.compile("^FAX:?-?", Pattern.CASE_INSENSITIVE);

	public static String trim(String text) {
		return text.replace('(', '-').replace(')', '-').replace('~', '-')
				.replaceAll("^-", "").replaceAll("[ \t\n]", "");
	};

	public Kind getKind() {
		return Kind.TEL;
	}
	public void parse(CardModel model, ImageAnalyzer analyzer) {
		String text = trim(analyzer.mDataSetEng.text);
		Matcher m = TEL_PATTERN.matcher(text);
		while (m.find()) {
			String word = text.substring(m.start(), m.end());
			Matcher macherFax = FAX_HEADER_PATTERN.matcher(word);
			Matcher macherMobile = MOBILE_HEADER_PATTERN.matcher(word);
			if (macherFax.find()) {
				model.put(Kind.FAX, word.substring(macherFax.end()));
			} else if (macherMobile.find()) {
				model.put(Kind.MOBILE, word);
			} else if (model.get(Kind.TEL) == null) {
				model.put(Kind.TEL, word);
			}
		}
	}
	public void parse(CardModel model, ImageAnalyzer analyzer, Rect rect) {
		String text = analyzer.getBestString(analyzer.mDataSetEng, rect);
		text = trim(text);
		Matcher m = TEL_PATTERN.matcher(text);
		if (m.find()) {
			String word = text.substring(m.start(), m.end());
			model.put(getKind(), word);
		}
	}
}
