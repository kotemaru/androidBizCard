package org.kotemaru.android.bizcard.controller;

import org.kotemaru.android.bizcard.MyApplication;
import org.kotemaru.android.bizcard.database.CardDb;
import org.kotemaru.android.bizcard.model.CardHolderActivtyModel;
import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.delegatehandler.annotation.GenerateDelegateHandler;
import org.kotemaru.android.delegatehandler.annotation.Handle;
import org.kotemaru.android.fw.thread.ThreadManager;

@GenerateDelegateHandler
public class ViewerController extends BaseController {
	public final ViewerControllerHandler mHandler;
	public final CardHolderActivtyModel mModel;

	public ViewerController(MyApplication app) {
		super(app);
		mHandler = new ViewerControllerHandler(this, app.getThreadManager());
		mModel = app.getModel().getCardHolderModel();
	}

	@Handle(thread = ThreadManager.WORKER)
	void loadCard(int id) {
		CardDb db = getFwApplication().getCardDb();
		CardModel model = db.getCardModel(id);
		mModel.setCardModelLocked(model);
	}

}
