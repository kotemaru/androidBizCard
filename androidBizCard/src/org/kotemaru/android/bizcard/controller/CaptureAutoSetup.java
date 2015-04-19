package org.kotemaru.android.bizcard.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kotemaru.android.bizcard.MyApplication;
import org.kotemaru.android.bizcard.R;
import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.bizcard.model.CardModel.Kind;
import org.kotemaru.android.bizcard.util.OCRUtil;
import org.kotemaru.android.bizcard.util.OCRUtil.WordInfo;
import org.kotemaru.android.delegatehandler.annotation.GenerateDelegateHandler;
import org.kotemaru.android.fw.dialog.DialogModel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

@GenerateDelegateHandler
public class CaptureAutoSetup {
	private final Context mContext;
	private Bitmap mBitmap;
	private final CardModel mCardModel;
	private final DialogModel mDialogModel;
	private List<WordInfo> mBaseWords;
	private List<WordInfo> mBaseEngWords;
	private String mEText;

	public CaptureAutoSetup(Context context, CardModel cardModel, DialogModel dialogModel) {
		mContext = context;
		mCardModel = cardModel;
		mDialogModel = dialogModel;
	}

	private void updateProgress(Kind kind) {
		if (kind.labelResId == 0) return;
		Log.i("DEBUG","updateProgress:"+kind);
		String msg = mContext.getString(R.string.prog_ocr_3, mContext.getString(kind.labelResId));
		mDialogModel.setProgress(msg, false, null);
		MyApplication.getInstance().updateCurrentActivity();
	}

	public CardModel getAutoSetupCardModel(Context context, Bitmap bitmap, List<WordInfo> words, List<WordInfo> ewords, String etext) throws IOException {
		mBitmap = bitmap;
		mBaseWords = words;
		mBaseEngWords = ewords;
		mEText = etext;
		setupCampany();
        setupOrganization();
        setupPosition();
        setupAddress();
        setupEMail();
        setupTel();
        setupWeb();
        setupName();
		return mCardModel;
	}

	private String getBestString(WordInfo winfo) {
		Log.e("DEBUG","===>getBestString:"+winfo.rect);
		try {
			winfo = OCRUtil.getBestString(mContext, mBitmap, winfo.rect);
			return winfo.word.replaceAll("[ \t\n]", "");
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (IllegalArgumentException e) {
			//e.printStackTrace();
			Log.e("DEBUG","===>getBestString:"+e+":"+winfo.rect);
			return winfo.word;
		}
	}

	private void setupCampany() {
		Kind kind = Kind.COMPANY;
		updateProgress(kind);
		WordInfo winfo = searchKeyword("株式","有限","会社","会ネ士","会ネ土", "㈱", "㈲");
		if (winfo == null) return;
		winfo = getLineWord(winfo);
		mCardModel.put(kind, getBestString(winfo));
	}

	private static final Pattern ORG_PATTERN_1 = Pattern.compile("[部課]$");
	private static final Pattern ORG_PATTERN_2 = Pattern.compile("[ユニット]$");

	private void setupOrganization() {
		Kind kind = Kind.ORGANIZATION;
		updateProgress(kind);

		StringBuilder sbuf = new StringBuilder();
		for (WordInfo winfo : mBaseWords) {
			String word = winfo.word.trim();
			Matcher m1 = ORG_PATTERN_1.matcher(word);
			Matcher m2 = ORG_PATTERN_2.matcher(word);
			if (m1.find()) {
				sbuf.append(' ').append(getBestString(winfo));
			} else if (m2.find()) {
				sbuf.append(' ').append(getBestString(winfo));
			}
		}
		mCardModel.put(kind, sbuf.toString().trim());
	}

	private void setupPosition() {
		Kind kind = Kind.POSITION;
		updateProgress(kind);
		WordInfo winfo = searchKeyword(
				"代表", "CEO", "部長", "課長", "主任",
				"チーフ", "マネージャ", "プロデューサ", "ディレクタ");
		if (winfo == null) return;
		winfo = getLineWord(winfo);
		mCardModel.put(kind, getBestString(winfo));
	}
	private void setupName() throws IOException {
		Kind kind = Kind.NAME;
		updateProgress(kind);

		WordInfo maxSizeInfo = mBaseWords.get(0);
		for (WordInfo winfo : mBaseWords) {
			if (winfo.confidence > 50 && winfo.rect.height() > maxSizeInfo.rect.height()) {
				maxSizeInfo = winfo;
			}
		}
		WordInfo winfo = getLineWord(maxSizeInfo);
		winfo = OCRUtil.getBestStringXxx(mContext, mBitmap, winfo.rect, "jpn");
		String name = winfo.word.replaceAll("[ \t\n]", "");
		mCardModel.put(kind, name);
	}
	private void setupAddress() {
		Kind kind = Kind.ADDRESS;
		updateProgress(kind);
		WordInfo winfo = searchKeyword(
				"〒", "東京都", "北海道", "大阪府", "京都府", "県", "丁目");
		if (winfo == null) return;
		winfo = getLineWord(winfo);
		mCardModel.put(kind, getBestString(winfo));
	}

	private static final Pattern EMAIL_PATTERN = Pattern.compile("[0-9a-zA-Z_-]+@([0-9a-zA-Z_-]+[.])+[a-zA-Z]{2,3}");

	private void setupEMail() {
		Kind kind = Kind.EMAIL;
		updateProgress(kind);
		Preprocessor prepro = new Preprocessor() {
			@Override
			public String preprocess(String org) {
				return org.replace('®', '@').replace('©', '@');
			}
		};
		//String word = searchPattern(prepro, EMAIL_PATTERN);
		//if (word == null) return;
		String text = prepro.preprocess(mEText);
		Matcher m = EMAIL_PATTERN.matcher(text);
		if (m.find()) {
			String word = text.substring(m.start(), m.end());
			mCardModel.put(kind, word);
		}
	}

	private static final Pattern TEL_PATTERN =
			Pattern.compile("(FAX:?-?)?[0-9]{2,5}-[0-9]{2,4}-[0-9]{4}", Pattern.CASE_INSENSITIVE);
	private static final Pattern MOBILE_HEADER_PATTERN =
			Pattern.compile("^0[789]0-");
	private static final Pattern FAX_HEADER_PATTERN =
			Pattern.compile("^FAX:?-?", Pattern.CASE_INSENSITIVE);
	private static final Preprocessor sTelPreprocessor = new Preprocessor() {
		@Override
		public String preprocess(String org) {
			return org.replace('(', '-').replace(')', '-').replace('~', '-')
					.replaceAll("^-", "").replaceAll("[ \t\n]", "");
		}
	};

	private void setupTel() {
		Kind kind = Kind.TEL;
		updateProgress(kind);
		//List<String> words = searchPatterns(sTelPreprocessor, TEL_PATTERN);
		//if (words.isEmpty()) return;
		//for (String word : words) {
		String text = sTelPreprocessor.preprocess(mEText);
		Matcher m = TEL_PATTERN.matcher(text);
		while (m.find()) {
			String word = text.substring(m.start(), m.end());
			Matcher macherFax = FAX_HEADER_PATTERN.matcher(word);
			Matcher macherMobile = MOBILE_HEADER_PATTERN.matcher(word);
			if (macherFax.find()) {
				mCardModel.put(Kind.FAX, word.substring(macherFax.end()));
			} else if (macherMobile.find()) {
				mCardModel.put(Kind.MOBILE, word);
			} else if (mCardModel.get(Kind.TEL) == null) {
				mCardModel.put(Kind.TEL, word);
			}
		}
	}

	private static final Pattern URL_PATTERN =
			Pattern.compile("(http://|https://){1}[\\w\\.\\-/:\\#\\?\\=\\&\\;\\%\\~\\+]+",
					Pattern.CASE_INSENSITIVE);
	private void setupWeb() {
		Kind kind = Kind.WEB;
		updateProgress(kind);
		String word = searchPattern(null, URL_PATTERN);
		if (word == null) return;
		mCardModel.put(kind, word);
	}




	// -----------------------------------
	private WordInfo searchKeyword(String... keywords) {
		for (String kw : keywords) {
			for (WordInfo winfo : mBaseWords) {
				//Log.e("DEBUG","searchKeyword:"+kw+":"+winfo.word);
				if (winfo.word.indexOf(kw) >= 0) return winfo;
			}
		}
		return null;
	}
	private List<WordInfo> searchKeywords(String... keywords) {
		List<WordInfo> list = new ArrayList<WordInfo>();
		for (String kw : keywords) {
			for (WordInfo winfo : mBaseWords) {
				if (winfo.word.indexOf(kw) >= 0) list.add(winfo);
			}
		}
		return list;
	}
	private WordInfo getLineWord(WordInfo baseWinfo) {
		Rect rect = new Rect(baseWinfo.rect);
		int charCount = baseWinfo.word.length();
		List<WordInfo> list = new ArrayList<WordInfo>();
		for (WordInfo winfo : mBaseWords) {
			if (isNear(rect, charCount, winfo.rect)) {
				list.add(winfo);
				rect.union(winfo.rect);
				charCount += winfo.word.length();
			}
		}

		Collections.sort(list, sLineWordComparator);
		float confidence = 0;
		StringBuilder sbuf = new StringBuilder();
		for (WordInfo winfo : list) {
			sbuf.append(winfo.word);
			rect.union(winfo.rect);
			confidence += winfo.confidence;
			Log.e("DEBUG","getLineWord:"+winfo.rect+"=>"+rect);
		}
		confidence = confidence / list.size();
		return new WordInfo(rect, sbuf.toString(), confidence);
	}
	private boolean isNear(Rect base, int charLen, Rect tgt) {
		int baseY = base.top + base.height()/2;
		int tgtY = tgt.top + tgt.height()/2;
		if (!nearlyEqueal(base.height(), baseY, tgt.height(), tgtY)) return false;
		int space = base.width()/charLen * 2;
		if (tgt.right < base.left-space) return false;
		if (base.right+space < tgt.left) return false;
		return true;
	}

	private boolean nearlyEqueal(int currentH, int currentY, int h, int y) {
		float delta = currentH * 0.5F;
		float minH = currentH - delta;
		float maxH = currentH + delta;
		float minY = currentY - delta;
		float maxY = currentY + delta;

		return minH < h && h < maxH && minY < y && y < maxY;
	}

	private final static Comparator<WordInfo> sLineWordComparator = new Comparator<WordInfo>() {
		@Override
		public int compare(WordInfo a, WordInfo b) {
			return a.rect.left - b.rect.left;
		}
	};

	private interface Preprocessor {
		String preprocess(String org);
	}

	private String mPatternMatchWord;
	private WordInfo mPatternMatchWinfo;

	private List<String> searchPatterns(Preprocessor proprocessor, Pattern... patterns) {
		mPatternMatchWord = null;
		mPatternMatchWinfo = null;
		List<String> list = new ArrayList<String>();
		for (WordInfo winfo : mBaseEngWords) {
			String word = winfo.word;
			if (proprocessor != null) word = proprocessor.preprocess(word);
			for (Pattern patt : patterns) {
				Matcher m = patt.matcher(word);
				if (m.find()) {
					list.add(word.substring(m.start(), m.end()));
				}
			}
		}
		return list;
	}
	private String searchPattern(Preprocessor proprocessor, Pattern... patterns) {
		for (WordInfo winfo : mBaseEngWords) {
			String word = winfo.word;
			if (proprocessor != null) word = proprocessor.preprocess(word);
			for (Pattern patt : patterns) {
				Log.e("DEBUG","searchPattern:"+patt+":"+word);
				Matcher m = patt.matcher(word);
				if (m.find()) {
					return word.substring(m.start(), m.end());
				}
			}
		}
		return null;
	}


}
