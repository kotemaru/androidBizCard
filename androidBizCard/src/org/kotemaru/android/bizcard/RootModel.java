package org.kotemaru.android.bizcard;

import org.kotemaru.android.bizcard.model.CameraActivityModel;
import org.kotemaru.android.bizcard.model.CaptureActivityModel;
import org.kotemaru.android.bizcard.model.CardHolderActivtyModel;
import org.kotemaru.android.bizcard.model.CardListActivityModel;

public class RootModel {
	private CameraActivityModel mCameraModel = new CameraActivityModel();
	private CaptureActivityModel mCaptureModel = new CaptureActivityModel();
	private CardListActivityModel mCardListModel = new CardListActivityModel();
	private CardHolderActivtyModel mCardHolderModel = new CardHolderActivtyModel();

	public CameraActivityModel getCameraModel() {
		return mCameraModel;
	}

	public CardListActivityModel getCardListModel() {
		return mCardListModel;
	}

	public CardHolderActivtyModel getCardHolderModel() {
		return mCardHolderModel;
	}

	public CaptureActivityModel getCaptureModel() {
		return mCaptureModel;
	}
}
