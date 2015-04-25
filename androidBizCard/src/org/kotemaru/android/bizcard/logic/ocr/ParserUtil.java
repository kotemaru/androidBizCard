package org.kotemaru.android.bizcard.logic.ocr;

import java.util.List;



public class ParserUtil {
	public static WordInfo searchKeyword(List<WordInfo> words, String... keywords) {
		for (String kw : keywords) {
			for (WordInfo winfo : words) {
				if (winfo.word.indexOf(kw) >= 0) return winfo;
			}
		}
		return null;
	}
}

