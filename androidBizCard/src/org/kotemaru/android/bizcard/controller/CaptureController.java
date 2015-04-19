package org.kotemaru.android.bizcard.controller;

import java.io.IOException;
import java.util.List;

import org.kotemaru.android.bizcard.Launcher;
import org.kotemaru.android.bizcard.Launcher.ExtraValue;
import org.kotemaru.android.bizcard.MyApplication;
import org.kotemaru.android.bizcard.R;
import org.kotemaru.android.bizcard.activity.EditorActivity;
import org.kotemaru.android.bizcard.model.CaptureActivityModel;
import org.kotemaru.android.bizcard.model.CardHolderActivtyModel;
import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.bizcard.model.CardModel.Kind;
import org.kotemaru.android.bizcard.util.OCRUtil;
import org.kotemaru.android.bizcard.util.OCRUtil.WordInfo;
import org.kotemaru.android.delegatehandler.annotation.GenerateDelegateHandler;
import org.kotemaru.android.delegatehandler.annotation.Handle;
import org.kotemaru.android.fw.FwControllerBase;
import org.kotemaru.android.fw.thread.ThreadManager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

@GenerateDelegateHandler
public class CaptureController extends FwControllerBase<MyApplication> {
	public final CaptureControllerHandler mHandler;
	public final CaptureActivityModel mModel;

	public static final String JPN = "jpn";
	public static final String ENG = "eng";

	public CaptureController(MyApplication app) {
		super(app);
		mHandler = new CaptureControllerHandler(this, app.getThreadManager());
		mModel = app.getModel().getCaptureModel();
		mHandler.setDebugMode(true);
	}

	@Handle(thread = ThreadManager.WORKER)
	public void loadBitmap(Context context, byte[] data) throws IOException {
		Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
		mModel.writeLock();
		try {
			mModel.setCardBitmap(bitmap);
		} finally {
			mModel.writeUnlock();
		}
		getApplication().updateCurrentActivity();
	}

	@Handle(thread = ThreadManager.WORKER)
	public void doAutoSetup(Context context, Bitmap bitmap) throws IOException {
		if (bitmap == null) return;

		mModel.getDialogModel().setProgress(context.getString(R.string.prog_ocr_1), false, null);
		getApplication().updateCurrentActivity();

		List<WordInfo> words = OCRUtil.getWords(context, bitmap, JPN, 40);
		List<WordInfo> ewords = OCRUtil.getWords(context, bitmap, ENG, 60);
		String etext = OCRUtil.getText(context, bitmap, ENG);
		mModel.writeLock();
		try {
			mModel.setWordInfoList(words);
		} finally {
			mModel.writeUnlock();
		}
		mModel.getDialogModel().setProgress(context.getString(R.string.prog_ocr_2), false, null);
		getApplication().updateCurrentActivity();

		CardModel cardModel = new CardModel();
		CaptureAutoSetup tool = new CaptureAutoSetup(context, cardModel, mModel.getDialogModel());
		tool.getAutoSetupCardModel(context,bitmap, mModel.getWordInfoList(), ewords, etext);
		getApplication().getModel().getCardHolderModel().setCardModelLocked(cardModel);
		Launcher.startEditor(context, ExtraValue.AUTO_SETUP, -1);
	}
	@Handle(thread = ThreadManager.WORKER)
	public void doScan(Context context, Bitmap bitmap, Rect rect, Kind kind) throws IOException {
		WordInfo winfo = OCRUtil.getBestString(context, bitmap, rect);
		CardHolderActivtyModel holderModel = getApplication().getModel().getCardHolderModel();
		holderModel.writeLock();
		try {
			holderModel.getCardModel().put(kind, winfo.word);
		} finally {
			holderModel.writeUnlock();
		}
		getApplication().goBackActivity(EditorActivity.class);
	}

}
