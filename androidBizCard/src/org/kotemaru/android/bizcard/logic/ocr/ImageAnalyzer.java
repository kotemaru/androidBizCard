package org.kotemaru.android.bizcard.logic.ocr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.kotemaru.android.fw.dialog.ProgressDialogBuilder;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.Log;
import android.util.Pair;

import com.googlecode.leptonica.android.Box;
import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.googlecode.tesseract.android.TessBaseAPI.PageIteratorLevel;

public class ImageAnalyzer {
	private static final String TAG = ImageAnalyzer.class.getSimpleName();
	public static final String JPN = "jpn";
	public static final String ENG = "eng";

	private Context mContext;
	private Bitmap mBitmap;
	public final DataSet mDataSetJpn = new DataSet(JPN, 50);
	public final DataSet mDataSetEng = new DataSet(ENG, 60);

	public ImageAnalyzer(Context context, Bitmap bitmap) {
		mContext = context;
		mBitmap = bitmap;
	}

	public class DataSet {
		public final String lang;
		public final float minConfidence;
		TessBaseAPI tessApi;
		String path;
		public  String text;
		public List<WordInfo> words;
		public DataSet(String lang, float minConfidence) {
			this.lang = lang;
			this.minConfidence = minConfidence;
		}
	}

	public static String initTrainedData(Context context, String lang) throws IOException {
		File dir = context.getCacheDir();
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


	public void init(ProgressDialogBuilder progress, boolean isFull) throws IOException {
		init(mDataSetJpn, isFull);
		if (progress.isCancelled()) return;
		init(mDataSetEng, isFull);
	}
	public void close() {
		mDataSetJpn.tessApi.end();
		if (mDataSetEng.tessApi != null) mDataSetEng.tessApi.end();
	}


	private void init(DataSet dataSet, boolean isFull) throws IOException {
		String path = initTrainedData(mContext, dataSet.lang);
		dataSet.path = path;
		dataSet.tessApi = new TessBaseAPI();
		dataSet.tessApi.init(path, dataSet.lang);
		if (!isFull) return;
		dataSet.tessApi.setImage(mBitmap);
		dataSet.text = dataSet.tessApi.getUTF8Text();
		dataSet.words = getWords(dataSet);
		//dataSet.tessApi.end();
	}

	private List<WordInfo> getWords(DataSet dataSet) {
		ResultIterator ite = dataSet.tessApi.getResultIterator();
		List<WordInfo> words = new ArrayList<WordInfo>(30);
		final int UNIT = PageIteratorLevel.RIL_WORD;
		ite.begin();
		do {
			float confidence = ite.confidence(UNIT);
			int[] rect = ite.getBoundingBox(UNIT);
			int minSize = Math.min(rect[Box.INDEX_W] - rect[Box.INDEX_X], rect[Box.INDEX_H] - rect[Box.INDEX_Y]);
			if (confidence > dataSet.minConfidence && minSize > 10) {
				String word = ite.getUTF8Text(UNIT);
				words.add(new WordInfo(rect, word, confidence));
			}
		} while (ite.next(UNIT));
		return words;
	}

	public Rect getNearWordsRect(DataSet dataSet, WordInfo tgtWinfo) {
		Rect rect = new Rect(tgtWinfo.rect);
		//List<WordInfo> list = new ArrayList<WordInfo>();
		for (WordInfo winfo : dataSet.words) {
			if (isNear(rect, winfo.rect)) {
				rect.union(winfo.rect);
				//list.add(winfo);
			}
		}
		return rect;
	}

	private static boolean isNear(Rect base, Rect tgt) {
		int minSpace = (int) (base.height() * 1.2);
		if (base.right + minSpace < tgt.left) return false;

		boolean isIn1 = (base.top <= tgt.top && tgt.top <= base.bottom);
		boolean isIn2 = (base.top <= tgt.bottom && tgt.bottom <= base.bottom);
		boolean isIn3 = (tgt.top <= base.top && base.top <= tgt.bottom);
		boolean isIn4 = (tgt.top <= base.bottom && base.bottom <= tgt.bottom);
		return isIn1 || isIn2 || isIn3 || isIn4;
	}

	public String getBestString(DataSet dataSet, Rect rect) {
		Bitmap bitmap = crop(mBitmap, rect, 80);

		//dataSet.tessApi.init(dataSet.path, dataSet.lang);
		dataSet.tessApi.setImage(bitmap);
		dataSet.tessApi.getUTF8Text();
		List<WordInfo> list = new ArrayList<WordInfo>(30);

		ResultIterator ite = dataSet.tessApi.getResultIterator();
		final int UNIT = PageIteratorLevel.RIL_SYMBOL;
		ite.begin();
		do {
			String word = ite.getUTF8Text(UNIT);
			float confidence = ite.confidence(UNIT);
			if (word == null || word.isEmpty()|| confidence < dataSet.minConfidence) continue;
			list.add(new WordInfo(ite.getBoundingBox(UNIT), word, confidence));
		} while (ite.next(UNIT));
		//dataSet.tessApi.end();

		//mDataSetEng.tessApi.init(dataSet.path, dataSet.lang);
		mDataSetEng.tessApi.setImage(bitmap);
		StringBuilder sbuf = new StringBuilder();
		for (WordInfo winfo : list) {
			char ch = winfo.word.charAt(0);
			if (ch == '一' || ch == 'ー' || ch == '－' || ch == '。' || ch == '○') {
				mDataSetEng.tessApi.setRectangle(winfo.rect);
				String word = mDataSetEng.tessApi.getUTF8Text();
				if (word != null && !word.isEmpty()) {
					ch = word.charAt(0);
				}
			}
			sbuf.append(ch);
		}

		//return new WordInfo(rect, sbuf.toString(), confidence);
		return sbuf.toString();
	}

	public String getBestString_ARM_not_work(DataSet dataSet, Rect rect) {
		Bitmap bitmap = crop(mBitmap, rect, 80);

		dataSet.tessApi.init(dataSet.path, dataSet.lang);
		dataSet.tessApi.setImage(bitmap);
		dataSet.tessApi.getUTF8Text();
		//float confidence = dataSet.tessApi.meanConfidence();
		StringBuilder sbuf = new StringBuilder();

		ResultIterator ite = dataSet.tessApi.getResultIterator();
		final int UNIT = PageIteratorLevel.RIL_SYMBOL;
		ite.begin();
		do {
			String word = ite.getUTF8Text(UNIT);
			float confidence = ite.confidence(UNIT);
			if (word == null || word.isEmpty()|| confidence < dataSet.minConfidence) continue;
			char ch = word.charAt(0);
			if (ch == '一' || ch == 'ー' || ch == '－' || ch == '。' || ch == '○') {
				List<Pair<String, Double>> choices =  ite.getChoicesAndConfidence(UNIT);
				for (Pair<String, Double> pair : choices) {
					char tgtCh = pair.first.charAt(0);
					if (tgtCh < 128) ch = tgtCh;
				}
			}
			sbuf.append(ch);
		} while (ite.next(UNIT));
		dataSet.tessApi.end();
		//return new WordInfo(rect, sbuf.toString(), confidence);
		return sbuf.toString();
	}

	public static Bitmap crop(Bitmap bitmap, Rect rect, float height) {
		int x = rect.left-1;
		int y = rect.top-1;
		int w = rect.width()+2;
		int h = rect.height()+2;
		float scale = (h<height) ? 1.0F : (height/h);
		Matrix matrix = new Matrix();
		matrix.setScale(scale, scale);
		Log.d(TAG,"crop:scale="+scale);
		Bitmap cropBitmap = Bitmap.createBitmap(bitmap, x, y, w, h, matrix, false);
		return cropBitmap;
	}


}
