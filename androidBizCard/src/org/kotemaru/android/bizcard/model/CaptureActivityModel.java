package org.kotemaru.android.bizcard.model;

import java.util.List;

import org.kotemaru.android.bizcard.logic.ocr.ImageAnalyzer;
import org.kotemaru.android.bizcard.logic.ocr.WordInfo;
import org.kotemaru.android.fw.ModelLock;

import android.graphics.Bitmap;

public class CaptureActivityModel extends BaseActivityModel {
	private Bitmap mCardBitmap = null;
	private List<WordInfo> mWordInfoList;
	private Kind mTargetKind;
	private ImageAnalyzer mImageAnalyzer;
	private boolean mIsEditMode = false;

	public CaptureActivityModel(ModelLock parentLock) {
		super(parentLock);
	}

	public void reset(Bitmap cardBitmap) {
		writeLock();
		try {
			setCardBitmap(cardBitmap);
			setWordInfoList(null);
			setEditMode(false);
		} finally {
			writeUnlock();
		}
	}

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

	public boolean isEditMode() {
		return mIsEditMode;
	}

	public void setEditMode(boolean isEditMode) {
		mIsEditMode = isEditMode;
	}

}
