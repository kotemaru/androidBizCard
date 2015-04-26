package org.kotemaru.android.bizcard.model;

import java.util.List;

import org.kotemaru.android.bizcard.logic.ocr.ImageAnalyzer;
import org.kotemaru.android.bizcard.logic.ocr.WordInfo;
import org.kotemaru.android.fw.FwActivityModelBase;

import android.graphics.Bitmap;

public class CaptureActivityModel extends FwActivityModelBase {
	private Bitmap mCardBitmap = null;
	private List<WordInfo> mWordInfoList;
	private WordInfo mSelectWordInfo;
	private Kind mTargetKind;
	private ImageAnalyzer mImageAnalyzer;

	public Bitmap getCardBitmap() {
		return mCardBitmap;
	}

	public void setCardBitmap(Bitmap cardBitmap) {
		mCardBitmap = cardBitmap;
	}

	public List<WordInfo> getWordInfoList() {
		return mWordInfoList;
	}

	public void setWordInfoList(List<WordInfo> wordInfoList) {
		mWordInfoList = wordInfoList;
	}

	public WordInfo getSelectWordInfo() {
		return mSelectWordInfo;
	}

	public void setSelectWordInfo(WordInfo selectWordInfo) {
		mSelectWordInfo = selectWordInfo;
	}

	public Kind getTargetKind() {
		return mTargetKind;
	}

	public void setTargetKind(Kind targetKind) {
		mTargetKind = targetKind;
	}

	public ImageAnalyzer getImageAnalyzer() {
		return mImageAnalyzer;
	}

	public void setImageAnalyzer(ImageAnalyzer imageAnalyzer) {
		mImageAnalyzer = imageAnalyzer;
	}

}
