package org.kotemaru.android.bizcard.logic.ocr;

import java.io.IOException;

import org.kotemaru.android.bizcard.R;
import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.bizcard.model.Kind;
import org.kotemaru.android.delegatehandler.annotation.GenerateDelegateHandler;
import org.kotemaru.android.fw.dialog.ProgressDialogBuilder;

import android.content.Context;
import android.graphics.Rect;

@GenerateDelegateHandler
public class CardImageAnalyzer {
	private final Context mContext;
	private final ImageAnalyzer mImageAnalyzer;

	private static Parser[] PARSERS = {
			new ParserAddress(),
			new ParserCompany(),
			new ParserEmail(),
			new ParserFax(),
			new ParserMobile(),
			new ParserName(),
			new ParserOrganization(),
			new ParserPosition(),
			new ParserTel(),
			new ParserWeb(),
	};


	public CardImageAnalyzer(Context context, ImageAnalyzer imageAnalyzer) throws IOException {
		mContext = context;
		mImageAnalyzer = imageAnalyzer;
	}

	public void parseAll(CardModel model, ProgressDialogBuilder progress) throws IOException {
		for (Parser parser : PARSERS) {
			if (progress.isCancelled()) return;
			String msg = mContext.getString(R.string.prog_ocr_3, parser.getKind().getLabel(mContext));
			progress.setMessage(msg);
			parser.parse(model, mImageAnalyzer);
		}
	}

	public void parse(CardModel model, Kind kind, Rect rect) {
		for (Parser parser : PARSERS) {
			if (parser.getKind() == kind) {
				parser.parse(model, mImageAnalyzer, rect);
				return;
			}
		}
	}


}
