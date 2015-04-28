package org.kotemaru.android.bizcard.activity;

import org.kotemaru.android.bizcard.Launcher;
import org.kotemaru.android.bizcard.Launcher.ExtraValue;
import org.kotemaru.android.bizcard.MyApplication;
import org.kotemaru.android.bizcard.R;
import org.kotemaru.android.bizcard.controller.ViewerController;
import org.kotemaru.android.bizcard.model.CardHolderActivtyModel;
import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.bizcard.model.Kind;
import org.kotemaru.android.bizcard.util.CardImageUtil;
import org.kotemaru.android.fw.FwActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

public class ViewerActivity extends BaseActivity<CardHolderActivtyModel> implements FwActivity {

	private View mContentRoot;
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
		mContentRoot = findViewById(R.id.content_root);
		setTitle(R.string.title_viewer);
		MyApplication app = MyApplication.getInstance();
		mModel = app.getModel().getCardHolderModel();
		mController = app.getController().getViewerController();

		View phoneBtn = findViewById(R.id.phone_button);
		phoneBtn.setOnClickListener(new ButtonListener(Kind.TEL));
		View mobileBtn = findViewById(R.id.mobile_phone_button);
		mobileBtn.setOnClickListener(new ButtonListener(Kind.MOBILE));
		View emailBtn = findViewById(R.id.email_button);
		emailBtn.setOnClickListener(new ButtonListener(Kind.EMAIL));
		View browserBtn = findViewById(R.id.browser_button);
		browserBtn.setOnClickListener(new ButtonListener(Kind.WEB));

		View thumbnail = findViewById(R.id.thumbnail);
		thumbnail.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Launcher.startCapture(ViewerActivity.this, ExtraValue.VIEW, Kind.NIL);
			}
		});
	}
	private class ButtonListener implements OnClickListener {
		private Kind mKind;
		public ButtonListener(Kind kind) {
			mKind = kind;
		}
		@Override
		public void onClick(View v) {
			mController.mHandler.doAction(mKind, mModel.getCardModel());
		}
	}

	@Override
	protected void onLaunch(Intent intent) {
		mCurrentCardId = intent.getExtras().getInt(Launcher.ExtraKey.ID.name());
		mController.mHandler.loadCard(mCurrentCardId);
	}

	@Override
	public void onUpdate(CardHolderActivtyModel model) {
		modelToView(mModel.getCardModel());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return createOptionsMenu(menu, MenuItemType.EDITOR);
	}

	private void modelToView(CardModel model) {
		for (Kind kind : Kind.values()) {
			kind.modelToView(model, mContentRoot);
		}

		ImageView thumnail = (ImageView) findViewById(R.id.thumbnail);
		Bitmap bitmap = CardImageUtil.loadThumbnail(this, mCurrentCardId); // TODO worker
		thumnail.setImageBitmap(bitmap);
		getFwApplication().getModel().getCaptureModel().reset(bitmap);
	}

}
