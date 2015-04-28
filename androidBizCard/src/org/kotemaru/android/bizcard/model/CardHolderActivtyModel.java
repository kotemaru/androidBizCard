package org.kotemaru.android.bizcard.model;

import org.kotemaru.android.fw.ModelLock;

public class CardHolderActivtyModel extends BaseActivityModel {
	private CardModel mCardModel;

	public CardHolderActivtyModel(ModelLock parentLock) {
		super(parentLock);
	}

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
