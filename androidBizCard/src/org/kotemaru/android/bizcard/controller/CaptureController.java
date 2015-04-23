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
import org.kotemaru.android.fw.dialog.OnDialogCancelListener;
import org.kotemaru.android.fw.dialog.ProgressDialogBuilder;
import org.kotemaru.android.fw.thread.ThreadManager;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.Log;

@GenerateDelegateHandler
public class CaptureController extends BaseController {
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
		getFwApplication().updateCurrentActivity();
	}

	boolean mIsCancelled = false;

	@Handle(thread = ThreadManager.WORKER)
	public void doAutoSetup(final Context context, Bitmap bitmap) throws IOException {
		if (bitmap == null) return;
		ProgressDialogBuilder progress = mModel.getDialogModel().setProgress(context.getString(R.string.prog_ocr_1), true,
				new OnDialogCancelListener() {
					@Override
					public void onDialogCancel(Activity activity) {
						Log.e("DEBUG", "onDialogCancel");
						mModel.getDialogModel().setProgress(context.getString(R.string.prog_ocr_cancel), false, null);
					}
				});
		try {
			List<WordInfo> words = OCRUtil.getWords(context, bitmap, JPN, 40);
			if (progress.isCancelled()) return;
			List<WordInfo> ewords = OCRUtil.getWords(context, bitmap, ENG, 60);
			if (progress.isCancelled()) return;
			// List<WordInfo> words = OCRUtil.getBestWords(context, bitmap, 50);
			// List<WordInfo> ewords = words;
			String etext = OCRUtil.getLastText();
			mModel.writeLock();
			try {
				mModel.setWordInfoList(words);
			} finally {
				mModel.writeUnlock();
			}
			progress.setMessage(context.getString(R.string.prog_ocr_2));

			if (progress.isCancelled()) return;
			CardModel cardModel = new CardModel();
			CaptureAutoSetup tool = new CaptureAutoSetup(context, cardModel, mModel.getDialogModel());
			tool.getAutoSetupCardModel(context, bitmap, mModel.getWordInfoList(), ewords, etext);
			getFwApplication().getModel().getCardHolderModel().setCardModelLocked(cardModel);
			Launcher.startEditor(context, ExtraValue.AUTO_SETUP, -1);
		} finally {
			mModel.getDialogModel().clear();
		}
	}
	@Handle(thread = ThreadManager.WORKER)
	public void doScan(Context context, Bitmap bitmap, Rect rect, Kind kind) throws IOException {
		mModel.getDialogModel().setProgress(context.getString(R.string.prog_ocr_1), false, null);
		try {
			WordInfo winfo = OCRUtil.getBestString(context, bitmap, rect);
			CardHolderActivtyModel holderModel = getFwApplication().getModel().getCardHolderModel();
			holderModel.writeLock();
			try {
				holderModel.getCardModel().put(kind, winfo.word);
			} finally {
				holderModel.writeUnlock();
			}
			getFwApplication().goBackActivity(EditorActivity.class);
		} finally {
			mModel.getDialogModel().clear();
		}
	}

}
