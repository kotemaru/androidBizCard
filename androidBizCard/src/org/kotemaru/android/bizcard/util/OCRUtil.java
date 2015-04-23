package org.kotemaru.android.bizcard.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import android.util.Pair;

import com.googlecode.leptonica.android.Box;
import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.googlecode.tesseract.android.TessBaseAPI.PageIteratorLevel;

public class OCRUtil {

	public static String initTrainedData(Context context, String lang) throws IOException {
		File dir = context.getFilesDir();
		String path = "tessdata/" + lang + ".traineddata";
		File file = new File(dir, path);
		if (file.exists()) return dir.getAbsolutePath();

		file.getParentFile().mkdirs();

		AssetManager assetManager = context.getAssets();
		InputStream in = assetManager.open(path);
		try {
			OutputStream out = new FileOutputStream(file);
			try {
				byte[] buf = new byte[4096];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
			} finally {
				out.close();
			}
			return dir.getAbsolutePath();
		} catch (IOException e) {
			file.delete();
			throw e;
		} finally {
			in.close();
		}
	}
	public static TessBaseAPI getTessBaseAPI(Context context, Bitmap bitmap, String lang) {
		String path;
		try {
			path = initTrainedData(context, lang);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		TessBaseAPI baseApi = new TessBaseAPI();
		// baseApi.setDebug(true);
		baseApi.init(path, lang);
		baseApi.setImage(bitmap);
		return baseApi;
	}
	public static String getText(Context context, Bitmap bitmap, String lang) {
		TessBaseAPI baseApi = getTessBaseAPI(context, bitmap, lang);
		String text = baseApi.getUTF8Text();
		baseApi.end();
		return text;
	}

	private static String sLastText;

	public static String getLastText() {
		return sLastText;
	}
	public static List<WordInfo> getWords(Context context, Bitmap bitmap, String lang, float minConfidence) {
		TessBaseAPI baseApi = getTessBaseAPI(context, bitmap, lang);
		sLastText = baseApi.getUTF8Text();
		ResultIterator ite = baseApi.getResultIterator();
		List<WordInfo> words = new ArrayList<WordInfo>(30);
		final int UNIT = PageIteratorLevel.RIL_WORD;
		ite.begin();
		do {
			float confidence = ite.confidence(UNIT);
			int[] rect = ite.getBoundingBox(UNIT);
			int minSize = Math.min(rect[Box.INDEX_W] - rect[Box.INDEX_X], rect[Box.INDEX_H] - rect[Box.INDEX_Y]);
			if (confidence > minConfidence && minSize > 10) {
				String word = ite.getUTF8Text(UNIT);
				words.add(new WordInfo(rect, word, confidence));
			}
		} while (ite.next(UNIT));

		baseApi.end();
		return words;
	}
	public static WordInfo _getBestString(Context context, Bitmap bitmap, Rect rect) throws IOException {
		Bitmap baseBitmap = crop(bitmap, rect);
		WordInfo engInfo = getBestString(context, baseBitmap, rect, "eng");
		WordInfo jpnInfo = getBestString(context, baseBitmap, rect, "jpn");
		if (engInfo.confidence > jpnInfo.confidence) {
			return engInfo;
		} else {
			return jpnInfo;
		}
	}
	public static WordInfo getBestStringXxx(Context context, Bitmap bitmap, Rect rect, String lang) throws IOException {
		bitmap = crop(bitmap, rect);
		return getBestString(context, bitmap, rect, lang);
	}
	public static WordInfo getBestString(Context context, Bitmap bitmap, Rect rect, String lang) throws IOException {
		TessBaseAPI baseApi = OCRUtil.getTessBaseAPI(context, bitmap, lang);
		String resText = baseApi.getUTF8Text();
		float maxConfidence = 0.0F;

		for (int i = 2; i <= 4; i++) {
			if (maxConfidence > 75) break;
			if (bitmap.getHeight() / i < 10) break;
			Bitmap target = Bitmap.createScaledBitmap(bitmap,
					bitmap.getWidth() / i, bitmap.getHeight() / i, false);
			baseApi.setImage(target);
			String text = baseApi.getUTF8Text();
			if (baseApi.meanConfidence() > maxConfidence) {
				resText = text;
				maxConfidence = baseApi.meanConfidence();
			}
		}
		baseApi.end();
		return new WordInfo(rect, resText, maxConfidence);
	}
	public static WordInfo getBestString(Context context, Bitmap bitmap, Rect rect) throws IOException {
		Bitmap baseBitmap = crop(bitmap, rect);
		TessBaseAPI engTessApi = getTessBaseAPI(context, baseBitmap, "eng");
		List<WordInfo> engChars = getChars(engTessApi);
		float engConfidence = confidence(engChars);
		if (engConfidence > 75) {
			return engMarge(engChars, context, baseBitmap);
		}

		TessBaseAPI baseApi = getTessBaseAPI(context, baseBitmap, "jpn");

		Log.e("DEBUG", "===>getBestString:=>" + baseApi.getUTF8Text() + ":" + baseApi.meanConfidence());
		List<WordInfo> jpnChars = getChars(baseApi);
		return jpnMarge(jpnChars, context, baseBitmap);
	}
	private static List<WordInfo> getChars(TessBaseAPI baseApi) {
		baseApi.getUTF8Text();
		ResultIterator ite = baseApi.getResultIterator();
		List<WordInfo> words = new ArrayList<WordInfo>(30);
		final int UNIT = PageIteratorLevel.RIL_SYMBOL;
		ite.begin();
		do {
			float confidence = ite.confidence(UNIT);
			int[] rect = ite.getBoundingBox(UNIT);
			String word = ite.getUTF8Text(UNIT);
			if (word != null) {
				words.add(new WordInfo(rect, word, confidence));
			}
		} while (ite.next(UNIT));

		baseApi.end();
		return words;
	}
	private static float confidence(List<WordInfo> words) {
		float confidence = 0;
		for (int i = 0; i < words.size(); i++) {
			WordInfo winfo = words.get(i);
			confidence += winfo.confidence;
		}
		confidence = confidence / words.size();
		return confidence;
	}

	private static WordInfo engMarge(List<WordInfo> words, Context context, Bitmap bitmap) throws IOException {
		StringBuilder sbuf = new StringBuilder();
		Rect rect = new Rect();
		float confidence = 0;
		for (WordInfo winfo : words) {
			char ch = winfo.word.charAt(0);
			if (ch == '¥') {
				Bitmap baseBitmap = crop(bitmap, winfo.rect);
				winfo = getBestString(context, baseBitmap, winfo.rect, "jpn");
				Log.e("DEBUG", "===>engMarge:" + ch + "=>" + winfo.word);
			}

			sbuf.append(winfo.word);
			rect.union(winfo.rect);
			confidence += winfo.confidence;
		}
		confidence = confidence / words.size();
		return new WordInfo(rect, sbuf.toString(), confidence);
	}

	private static WordInfo jpnMarge(List<WordInfo> words, Context context, Bitmap bitmap) throws IOException {
		StringBuilder sbuf = new StringBuilder();
		Rect rect = new Rect();
		float confidence = 0;
		for (WordInfo winfo : words) {
			char ch = winfo.word.charAt(0);
			// Log.e("DEBUG", "===>jpnMarge:" + ch + ":" + winfo.confidence);
			if (ch == '一' || ch == 'ー') {
				Bitmap baseBitmap = crop(bitmap, winfo.rect);
				WordInfo ewinfo = getBestString(context, baseBitmap, winfo.rect, "eng");
				// Log.e("DEBUG", "===>jpnMarge:" + ch + "=>" + ewinfo.word);
				if ("1".equals(ewinfo.word) || "l".equals(ewinfo.word)) {
					winfo = ewinfo;
				}
				// } else if (winfo.confidence < 70) {
				// Bitmap baseBitmap = crop(bitmap, winfo.rect);
				// winfo = getBestString(context, baseBitmap, winfo.rect, "jpn");
			}

			sbuf.append(winfo.word);
			rect.union(winfo.rect);
			confidence += winfo.confidence;
		}
		confidence = confidence / words.size();
		return new WordInfo(rect, sbuf.toString(), confidence);
	}

	private static WordInfo marge(List<WordInfo> jpnWords, List<WordInfo> engWords) {
		StringBuilder sbuf = new StringBuilder();
		Rect rect = new Rect();
		float confidence = 0;
		for (int i = 0; i < jpnWords.size(); i++) {
			WordInfo winfo = bestChar(jpnWords.get(i), engWords.get(i));

			sbuf.append(winfo.word);
			rect.union(winfo.rect);
			confidence += winfo.confidence;
		}
		confidence = confidence / jpnWords.size();
		return new WordInfo(rect, sbuf.toString(), confidence);
	}
	private static WordInfo bestChar(WordInfo jpnWinfo, WordInfo engWinfo) {
		char jch = jpnWinfo.word.charAt(0);
		char ech = engWinfo.word.charAt(0);
		if ((jch == '一' || jch == 'ー') && (ech == '1' || ech == 'l')) {
			return engWinfo;
		}
		return jpnWinfo.confidence > engWinfo.confidence ? jpnWinfo : engWinfo;
	}

	public static Bitmap crop(Bitmap bitmap, Rect rect) {
		int width = rect.width();
		int height = rect.height();
		int[] pixels = new int[width * height];
		bitmap.getPixels(pixels, 0, width, rect.left, rect.top, width, height);
		Bitmap cropBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		cropBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
		return cropBitmap;
	}

	public static class WordInfo {
		public final Rect rect;
		public final String word;
		public final float confidence;
		public final List<Pair<String, Double>> choices;

		public WordInfo(int[] rect, String word, float confidence) {
			this(rect, word, confidence, null);
		}
		public WordInfo(int[] rect, String word, float confidence, List<Pair<String, Double>> choices) {
			this.rect = new Rect(
					rect[Box.INDEX_X],
					rect[Box.INDEX_Y],
					rect[Box.INDEX_W],
					rect[Box.INDEX_H]);
			this.word = word;
			this.confidence = confidence;
			this.choices = choices;
		}
		public WordInfo(Rect rect, String word, float confidence) {
			this.rect = rect;
			this.word = word;
			this.confidence = confidence;
			this.choices = null;
		}
	}

	// ----------------------------------------------------------------------------------

	public static List<WordInfo> getBestChars(Context context, Bitmap bitmap, float limit) {
		TessBaseAPI baseApi = OCRUtil.getTessBaseAPI(context, bitmap, "jpn");
		sLastText = baseApi.getUTF8Text();
		ResultIterator ite = baseApi.getResultIterator();
		List<WordInfo> words = new ArrayList<WordInfo>(30);
		final int UNIT = PageIteratorLevel.RIL_SYMBOL;
		ite.begin();
		do {
			List<Pair<String, Double>> choices = ite.getChoicesAndConfidence(UNIT);
			Pair<String, Double> pair = choiceChar(choices);
			if (pair != null) {
				float confidence = pair.second.floatValue();
				if (confidence > limit) {
					int[] rect = ite.getBoundingBox(UNIT);
					String word = pair.first;
					words.add(new WordInfo(rect, word, confidence));
				}
			}
		} while (ite.next(UNIT));

		baseApi.end();
		return words;
	}
	private static Pair<String, Double> choiceChar(List<Pair<String, Double>> choices) {
		double max = 0;
		Pair<String, Double> choice = null;
		for (Pair<String, Double> pair : choices) {
			char ch = pair.first.charAt(0);
			double conf = pair.second;
			boolean isEngChar = (ch < 255);
			if (isEngChar) conf = conf * 2;
			if (conf > max) {
				max = conf;
				choice = pair;
			}
		}
		return choice;
	}

	public static class CharsInfo {
		public final Rect rect = new Rect();
		public float confidence;
		public final List<WordInfo> chars = new ArrayList<WordInfo>(20);
	}

	public static List<WordInfo> getBestWords(Context context, Bitmap bitmap, float limit) {
		List<WordInfo> chars = getBestChars(context, bitmap, limit);
		return toBestWords(chars);
	}

	private static Comparator<WordInfo> sColunComparator = new Comparator<WordInfo>() {
		@Override
		public int compare(WordInfo a, WordInfo b) {
			return a.rect.left - b.rect.left;
		}
	};

	public static List<WordInfo> toBestWords(List<WordInfo> chars) {
		Collections.sort(chars, sColunComparator);

		List<CharsInfo> csinfoList = new ArrayList<CharsInfo>(chars.size() / 10);
		for (WordInfo cinfo : chars) {
			CharsInfo csinfo = getNearWordInfo(csinfoList, cinfo);
			csinfo.rect.union(cinfo.rect);
			csinfo.chars.add(cinfo);
		}

		List<WordInfo> words = new ArrayList<WordInfo>(csinfoList.size());

		StringBuilder allText = new StringBuilder(1000);
		StringBuilder sbuf = new StringBuilder(100);
		for (CharsInfo csinfo : csinfoList) {
			// Collections.sort(csinfo.chars, comparator);
			sbuf.setLength(0);
			float conf = 0;
			for (WordInfo cinfo : csinfo.chars) {
				sbuf.append(cinfo.word.charAt(0));
				conf += cinfo.confidence;
			}
			conf = conf / csinfo.chars.size();
			Log.e("DEBUG", "word=" + conf + ":" + sbuf.toString());
			WordInfo winfo = new WordInfo(csinfo.rect, sbuf.toString(), conf);
			words.add(winfo);
			allText.append(sbuf);
		}
		sLastText = allText.toString();
		return words;
	}
	private static CharsInfo getNearWordInfo(List<CharsInfo> csinfoList, WordInfo cinfo) {
		for (CharsInfo csinfo : csinfoList) {
			if (isNear(csinfo.rect, cinfo.rect, csinfo.chars.get(0), cinfo)) return csinfo;
		}
		CharsInfo csinfo = new CharsInfo();
		csinfoList.add(csinfo);
		return csinfo;
	}
	private static boolean isNear(Rect base, Rect tgt, WordInfo b, WordInfo t) {
		if (b.word.charAt(0) == '経' && t.word.charAt(0) == '理') {
			// Log.e("DEBUG", "keiri=" + isIn1 + "," + isIn2 + "," + isIn3 + "," + isIn4);
			Log.e("DEBUG", "keiri=" + b.word + "=" + base + ":" + t.word + "=" + tgt);
		}
		int minSpace = (int) (base.height() * 1);
		if (base.right + minSpace < tgt.left) return false;

		boolean isIn1 = (base.top <= tgt.top && tgt.top <= base.bottom);
		boolean isIn2 = (base.top <= tgt.bottom && tgt.bottom <= base.bottom);
		boolean isIn3 = (tgt.top <= base.top && base.top <= tgt.bottom);
		boolean isIn4 = (tgt.top <= base.bottom && base.bottom <= tgt.bottom);

		if (b.word.charAt(0) == '経') {
			Log.e("DEBUG", "keiri=" + isIn1 + "," + isIn2 + "," + isIn3 + "," + isIn4);
		}
		return isIn1 || isIn2 || isIn3 || isIn4;
	}

}
