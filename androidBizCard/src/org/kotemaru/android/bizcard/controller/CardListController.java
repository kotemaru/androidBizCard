package org.kotemaru.android.bizcard.controller;

import java.util.List;

import org.kotemaru.android.bizcard.MyApplication;
import org.kotemaru.android.bizcard.R;
import org.kotemaru.android.bizcard.database.CardDb;
import org.kotemaru.android.bizcard.model.CardListActivityModel;
import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.fw.annotation.GenerateDelegateHandler;
import org.kotemaru.android.fw.annotation.Handle;
import org.kotemaru.android.fw.thread.ThreadManager;

@GenerateDelegateHandler
public class CardListController extends BaseController {
	public final CardListControllerHandler mHandler;
	public final CardListActivityModel mModel;
	public final CardDb mCardDb;

	public CardListController(MyApplication app) {
		super(app);
		mHandler = new CardListControllerHandler(this, app.getThreadManager());
		mModel = app.getModel().getCardListModel();
		mCardDb = app.getCardDb();
	}

	@Handle(thread = ThreadManager.WORKER)
	void loadCardList() {
		List<CardModel> list = mCardDb.getCardModelList(mModel.getQueryText());
		mModel.writeLock();
		try {
			mModel.setCardModelList(list);
		} finally {
			mModel.writeUnlock();
		}
		if (list.size() == 0) {
			mModel.getDialogModel().setInformationIfRequire(R.string.info_register);
		}
		getFwApplication().updateCurrentActivity();
	}

	@Handle(thread = ThreadManager.WORKER)
	void removeCardModel(CardModel model) {
		mCardDb.removeCardModel(model.getId());
		loadCardList();
	}

}
