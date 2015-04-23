package org.kotemaru.android.bizcard;

import org.kotemaru.android.bizcard.controller.CaptureController;
import org.kotemaru.android.bizcard.controller.CardListController;
import org.kotemaru.android.bizcard.controller.EditorController;
import org.kotemaru.android.bizcard.controller.ViewerController;
import org.kotemaru.android.fw.FwControllerBase;

public class RootController extends FwControllerBase<MyApplication> {
	private final CaptureController mCaptureController;
	private final CardListController mCardListController;
	private final ViewerController mViewerController;
	private final EditorController mEditorController;

	public RootController(MyApplication app) {
		super(app);
		mCaptureController = new CaptureController(app);
		mCardListController = new CardListController(app);
		mViewerController = new ViewerController(app);
		mEditorController = new EditorController(app);
	}

	public CardListController getCardListController() {
		return mCardListController;
	}

	public ViewerController getViewerController() {
		return mViewerController;
	}

	public EditorController getEditorController() {
		return mEditorController;
	}

	public CaptureController getCaptureController() {
		return mCaptureController;
	}
}
