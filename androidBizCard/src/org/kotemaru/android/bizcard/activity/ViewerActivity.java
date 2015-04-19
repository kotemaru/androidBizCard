package org.kotemaru.android.bizcard.activity;

import org.kotemaru.android.bizcard.Launcher;
import org.kotemaru.android.bizcard.MyApplication;
import org.kotemaru.android.bizcard.R;
import org.kotemaru.android.bizcard.controller.ViewerController;
import org.kotemaru.android.bizcard.model.CardHolderActivtyModel;
import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.bizcard.model.CardModel.Kind;
import org.kotemaru.android.fw.FwActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;

public class ViewerActivity extends BaseActivity<CardHolderActivtyModel> implements FwActivity {

	private CardHolderActivtyModel mModel;
	private ViewerController mController;
	private int mCurrentCardId;

	@Override
	public CardHolderActivtyModel getActivityModel() {
		return mModel;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_viewer);
		MyApplication app = MyApplication.getInstance();
		mModel = app.getModel().getCardHolderModel();
		mController = app.getController().getViewerController();
	}


	@Override
	protected void onLaunch(Intent intent) {
		mCurrentCardId = intent.getExtras().getInt(Launcher.ExtraKey.ID.name());
		mController.mHandler.loadCard(mCurrentCardId);
	}
	@Override
	public void onUpdateInReadLocked(CardHolderActivtyModel model) {
		setupTextView(mModel.getCardModel());
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return createOptionsMenu(menu, MenuItemType.EDITOR);
	}

	private void setupTextView(CardModel model) {
		for (Kind kind : Kind.values()) {
			if (kind.textViewResId == 0) continue;
			TextView textView = (TextView) findViewById(kind.textViewResId);
			if (textView == null) continue;
			if (model != null) {
				textView.setText(model.get(kind));
			} else {
				textView.setText(null);
			}
		}
	}

}
