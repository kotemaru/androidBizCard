package org.kotemaru.android.bizcard.model;

import java.util.List;

import org.kotemaru.android.fw.FwActivityModelBase;

public class CardListActivityModel extends FwActivityModelBase {
	private List<CardModel> mCardModelList;

	public List<CardModel> getCardModelList() {
		return mCardModelList;
	}

	public void setCardModelList(List<CardModel> cardModelList) {
		mCardModelList = cardModelList;
	}

}
