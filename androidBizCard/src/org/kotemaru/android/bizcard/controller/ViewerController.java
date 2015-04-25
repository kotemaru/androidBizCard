package org.kotemaru.android.bizcard.controller;

import java.util.Date;

import org.kotemaru.android.bizcard.Launcher;
import org.kotemaru.android.bizcard.MyApplication;
import org.kotemaru.android.bizcard.database.CardDb;
import org.kotemaru.android.bizcard.model.CardHolderActivtyModel;
import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.bizcard.model.Kind;
import org.kotemaru.android.delegatehandler.annotation.GenerateDelegateHandler;
import org.kotemaru.android.delegatehandler.annotation.Handle;
import org.kotemaru.android.fw.thread.ThreadManager;

import android.util.Log;
import android.widget.Toast;

@GenerateDelegateHandler
public class ViewerController extends BaseController {
	public static final String TAG = ViewerController.class.getSimpleName();

	public final ViewerControllerHandler mHandler;
	public final CardHolderActivtyModel mModel;
	public final CardDb mCardDb;

	public ViewerController(MyApplication app) {
		super(app);
		mHandler = new ViewerControllerHandler(this, app.getThreadManager());
		mModel = app.getModel().getCardHolderModel();
		mCardDb = app.getCardDb();
	}

	@Handle(thread = ThreadManager.WORKER)
	void loadCard(int id) {
		CardDb db = getFwApplication().getCardDb();
		CardModel model = db.getCardModel(id);
		mModel.setCardModelLocked(model);
	}

	@Handle(thread = ThreadManager.WORKER)
	void doAction(Kind kind, CardModel model) {
		Log.w(TAG, "doAction: " + kind);
		CharSequence val = model.get(kind);
		if (val == null || val.length() == 0) {
			mHandler.showToast(kind.getLabel(getFwApplication())+"が未定義です。");
			return;
		}
		switch (kind) {
		case TEL:
			Launcher.startDialer(getFwApplication(), val);
			setAccessData(model);
			break;
		case MOBILE:
			Launcher.startDialer(getFwApplication(), val);
			setAccessData(model);
			break;
		case EMAIL:
			Launcher.startMailer(getFwApplication(), val);
			setAccessData(model);
			break;
		case WEB:
			Launcher.startBrowser(getFwApplication(), val);
			break;
		default:
			Log.w(TAG, "doAction: Unknown kind " + kind);
			break;
		}
	}
	private void setAccessData(CardModel model) {
		mCardDb.setAccessDate(model.getId(), new Date());
		getFwApplication().getController().getCardListController().mHandler.loadCardList();
	}

	@Handle(thread = ThreadManager.UI)
	void showToast(String msg) {
		Toast.makeText(getFwApplication(), msg, Toast.LENGTH_SHORT).show();
	}

}
