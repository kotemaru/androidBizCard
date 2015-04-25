package org.kotemaru.android.bizcard.model;

import java.util.List;

import org.kotemaru.android.bizcard.logic.ocr.WordInfo;
import org.kotemaru.android.fw.FwActivityModelBase;

import android.graphics.Bitmap;
import android.hardware.Camera.Parameters;

public class CameraActivityModel extends FwActivityModelBase {
	private Bitmap mCardBitmap = null;
	private int mExposure = 0;
	private String mFocusMode = Parameters.FOCUS_MODE_AUTO;
	private String mFlashMode = Parameters.FLASH_MODE_AUTO;
	private List<WordInfo> mWordInfoList;
	private WordInfo mSelectWordInfo;
	private boolean mIsPreviewMode;

	public Bitmap getCardBitmap() {
		return mCardBitmap;
	}

	public void setCardBitmap(Bitmap cardBitmap) {
		mCardBitmap = cardBitmap;
	}

	public String getFocusMode() {
		return mFocusMode;
	}

	public void setFocusMode(String focusMode) {
		mFocusMode = focusMode;
	}

	public String getFlashMode() {
		return mFlashMode;
	}

	public void setFlashMode(String flashMode) {
		mFlashMode = flashMode;
	}

	public int getExposure() {
		return mExposure;
	}

	public void setExposure(int exposure) {
		mExposure = exposure;
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

	public boolean isPreviewMode() {
		return mIsPreviewMode;
	}

	public void setPreviewMode(boolean isPreviewMode) {
		mIsPreviewMode = isPreviewMode;
	}

}
