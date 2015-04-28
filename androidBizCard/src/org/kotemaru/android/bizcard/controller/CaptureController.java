package org.kotemaru.android.bizcard.controller;

import java.io.IOException;

import org.kotemaru.android.bizcard.Launcher;
import org.kotemaru.android.bizcard.Launcher.ExtraValue;
import org.kotemaru.android.bizcard.MyApplication;
import org.kotemaru.android.bizcard.R;
import org.kotemaru.android.bizcard.activity.EditorActivity;
import org.kotemaru.android.bizcard.logic.ocr.CardImageAnalyzer;
import org.kotemaru.android.bizcard.logic.ocr.ImageAnalyzer;
import org.kotemaru.android.bizcard.model.CaptureActivityModel;
import org.kotemaru.android.bizcard.model.CardHolderActivtyModel;
import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.bizcard.model.Kind;
import org.kotemaru.android.fw.annotation.GenerateDelegateHandler;
import org.kotemaru.android.fw.annotation.Handle;
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
	void loadBitmap(Context context, byte[] data) throws IOException {
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
	void doAnalyzeAll(final Context context, Bitmap bitmap, boolean isInitOnly) throws IOException {
		if (bitmap == null) return;
		ProgressDialogBuilder progress = mModel.getDialogModel().setProgress(context.getString(R.string.prog_ocr_1), true,
				new OnDialogCancelListener() {
					@Override
					public void onDialogCancel(Activity activity) {
						Log.e("DEBUG", "onDialogCancel");
						mModel.getDialogModel().setProgress(context.getString(R.string.prog_ocr_cancel), false, null);
					}
				});
		ImageAnalyzer analyzer = mModel.getImageAnalyzer();
		if (analyzer == null) {
			analyzer = new ImageAnalyzer(context);
			mModel.setImageAnalyzer(analyzer);
		}
		try {
			analyzer.init(bitmap, progress);
			mModel.writeLock();
			try {
				mModel.setWordInfoList(analyzer.mDataSetJpn.words);
			} finally {
				mModel.writeUnlock();
			}

			if (isInitOnly) return;
			if (progress.isCancelled()) return;
			progress.setMessage(context.getString(R.string.prog_ocr_2));
			CardImageAnalyzer cardAnalyzer = new CardImageAnalyzer(context, analyzer);
			CardModel cardModel = new CardModel();
			cardAnalyzer.parseAll(cardModel, progress);

			getFwApplication().getModel().getCardHolderModel().setCardModelLocked(cardModel);
			Launcher.startEditor(context, ExtraValue.AUTO_SETUP, -1);
		} finally {
			mModel.getDialogModel().clear();
			analyzer.release();
		}
	}

	@Handle(thread = ThreadManager.WORKER)
	void doAnalyzeOne(Context context, Bitmap bitmap, Rect rect, Kind kind) throws IOException {
		mModel.getDialogModel().setProgress(context.getString(R.string.prog_ocr_1), false, null);
		ImageAnalyzer analyzer = mModel.getImageAnalyzer();
		analyzer.setBitmap(bitmap);
		try {
			CardImageAnalyzer cardAnalyzer = new CardImageAnalyzer(context, analyzer);

			CardModel cardModel = new CardModel();
			cardAnalyzer.parse(cardModel, kind, rect);

			CardHolderActivtyModel holderModel = getFwApplication().getModel().getCardHolderModel();
			holderModel.writeLock();
			try {
				holderModel.getCardModel().put(kind, cardModel.get(kind));
			} finally {
				holderModel.writeUnlock();
			}
			getFwApplication().goBackActivity(EditorActivity.class);
		} finally {
			mModel.getDialogModel().clear();
			analyzer.release();
		}
	}

}
