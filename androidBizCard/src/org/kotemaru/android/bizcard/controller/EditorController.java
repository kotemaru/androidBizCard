package org.kotemaru.android.bizcard.controller;

import java.io.IOException;

import org.kotemaru.android.bizcard.MyApplication;
import org.kotemaru.android.bizcard.database.CardDb;
import org.kotemaru.android.bizcard.model.CardHolderActivtyModel;
import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.bizcard.model.Kind;
import org.kotemaru.android.bizcard.util.CardImageUtil;
import org.kotemaru.android.fw.annotation.GenerateDelegateHandler;
import org.kotemaru.android.fw.annotation.Handle;
import org.kotemaru.android.fw.thread.ThreadManager;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;

@GenerateDelegateHandler
public class EditorController extends BaseController {
	public static final String TAG = EditorController.class.getSimpleName();

	public final EditorControllerHandler mHandler;
	public final CardHolderActivtyModel mModel;
	public final CardDb mCardDb;

	public EditorController(MyApplication app) {
		super(app);
		mHandler = new EditorControllerHandler(this, app.getThreadManager());
		mModel = app.getModel().getCardHolderModel();
		mCardDb = app.getCardDb();
	}

	@Handle(thread = ThreadManager.WORKER)
	void loadCard(int id) {
		CardModel model = mCardDb.getCardModel(id);
		mModel.setCardModelLocked(model);
	}

	@Handle(thread = ThreadManager.WORKER)
	void register(Activity activity, CardModel model) {
		int id = mCardDb.putCardModel(model);
		if (id == -1) {
			Log.e(TAG, "register DB failed:"+model.getId()+":"+model.get(Kind.NAME));
			mModel.getDialogModel().setAlert("登録失敗", "データベースに書込ができませんでした。", null);
			return;
		}
		//for (int i=0; i<100; i++) {
		//	model.put(Kind.NAME, "NAME-"+i);
		//	mCurrentCardId = mCardDb.putCardModel(model);
		//}
		//model.setId(mCurrentCardId);

		try {
			Bitmap bitmap = getFwApplication().getModel().getCaptureModel().getCardBitmap();
			if (bitmap != null) {
				String url = CardImageUtil.saveThumbnail(getFwApplication(), id, bitmap);
				model.put(Kind.IMAGE_URL, url); // 意味なし
			}
		} catch (IOException e) {
			Log.e(TAG, "register image failed:"+id, e);
		}
		getFwApplication().getController().getCardListController().mHandler.loadCardList();

		mHandler.finishActivity(activity);
	}

	@Handle(thread = ThreadManager.UI)
	void finishActivity(Activity activity) {
		if (!activity.isFinishing()) {
			activity.finish();
		}
	}

}
