package org.kotemaru.android.bizcard.controller;

import java.util.List;

import org.kotemaru.android.bizcard.MyApplication;
import org.kotemaru.android.bizcard.R;
import org.kotemaru.android.bizcard.database.CardDb;
import org.kotemaru.android.bizcard.model.CardListActivityModel;
import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.delegatehandler.annotation.GenerateDelegateHandler;
import org.kotemaru.android.delegatehandler.annotation.Handle;
import org.kotemaru.android.fw.FwControllerBase;
import org.kotemaru.android.fw.thread.ThreadManager;

@GenerateDelegateHandler
public class CardListController extends FwControllerBase<MyApplication> {
	public final CardListControllerHandler mHandler;
	public final CardListActivityModel mModel;

	public CardListController(MyApplication app) {
		super(app);
		mHandler = new CardListControllerHandler(this, app.getThreadManager());
		mModel = app.getModel().getCardListModel();
	}

	@Handle(thread = ThreadManager.WORKER)
	void loadCardList() {
		CardDb db = getApplication().getCardDb();
		List<CardModel> list = db.getCardModelList();
		mModel.writeLock();
		try {
			mModel.setCardModelList(list);
		} finally {
			mModel.writeUnlock();
		}
		if (list.size() == 0) {
			mModel.getDialogModel().setInformationIfRequire(getApplication(), R.string.info_register);
		}
		getApplication().updateCurrentActivity();
	}

}
