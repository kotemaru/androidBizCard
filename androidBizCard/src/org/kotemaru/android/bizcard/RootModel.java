package org.kotemaru.android.bizcard;

import org.kotemaru.android.bizcard.model.CameraActivityModel;
import org.kotemaru.android.bizcard.model.CaptureActivityModel;
import org.kotemaru.android.bizcard.model.CardHolderActivtyModel;
import org.kotemaru.android.bizcard.model.CardListActivityModel;
import org.kotemaru.android.fw.ModelLock;

public class RootModel extends ModelLock {
	private CameraActivityModel mCameraModel = new CameraActivityModel(this);
	private CaptureActivityModel mCaptureModel = new CaptureActivityModel(this);
	private CardListActivityModel mCardListModel = new CardListActivityModel(this);
	private CardHolderActivtyModel mCardHolderModel = new CardHolderActivtyModel(this);

	public RootModel() {
		super(null);
	}

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
