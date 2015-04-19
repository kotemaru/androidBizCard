package org.kotemaru.android.bizcard.model;

import org.kotemaru.android.fw.FwActivityModelBase;


public class CardHolderActivtyModel extends FwActivityModelBase {
	private CardModel mCardModel;

	public CardModel getCardModel() {
		return mCardModel;
	}

	public void setCardModel(CardModel cardModel) {
		mCardModel = cardModel;
	}

	public void setCardModelLocked(CardModel cardModel) {
		writeLock();
		try {
			setCardModel(cardModel);
		} finally {
			writeUnlock();
		}
	}

}
