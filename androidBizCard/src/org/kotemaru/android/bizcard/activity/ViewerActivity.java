package org.kotemaru.android.bizcard.activity;

import java.util.Date;

import org.kotemaru.android.bizcard.Launcher;
import org.kotemaru.android.bizcard.MyApplication;
import org.kotemaru.android.bizcard.R;
import org.kotemaru.android.bizcard.controller.ViewerController;
import org.kotemaru.android.bizcard.database.CardDb;
import org.kotemaru.android.bizcard.model.CardHolderActivtyModel;
import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.bizcard.model.CardModel.Kind;
import org.kotemaru.android.bizcard.util.CardImageUtil;
import org.kotemaru.android.fw.FwActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class ViewerActivity extends BaseActivity<CardHolderActivtyModel> implements FwActivity {

	private CardHolderActivtyModel mModel;
	private ViewerController mController;
	private int mCurrentCardId;
	private CardDb mCardDb;

	@Override
	public CardHolderActivtyModel getActivityModel() {
		return mModel;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_viewer);
		setTitle(R.string.title_viewer);
		MyApplication app = MyApplication.getInstance();
		mModel = app.getModel().getCardHolderModel();
		mController = app.getController().getViewerController();
		mCardDb = app.getCardDb();

		View phoneBtn = findViewById(R.id.phone_button);
		phoneBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				CardModel model = mCardDb.getCardModel(mCurrentCardId);
				String tel = model.get(Kind.TEL);
				if (tel == null || tel.isEmpty()) return;
				Launcher.startDialer(ViewerActivity.this, tel);
				setAccessData();
			}
		});
		View mPhoneBtn = findViewById(R.id.mobile_phone_button);
		mPhoneBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				CardModel model = mCardDb.getCardModel(mCurrentCardId);
				String tel = model.get(Kind.MOBILE);
				if (tel == null || tel.isEmpty()) return;
				Launcher.startDialer(ViewerActivity.this, tel);
				setAccessData();
			}
		});
		View emailBtn = findViewById(R.id.email_button);
		emailBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				CardModel model = mCardDb.getCardModel(mCurrentCardId);
				String email = model.get(Kind.EMAIL);
				if (email == null || email.isEmpty()) return;
				Launcher.startMailer(ViewerActivity.this, email);
				setAccessData();
			}
		});
		View browserBtn = findViewById(R.id.browser_button);
		browserBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				CardModel model = mCardDb.getCardModel(mCurrentCardId);
				String url = model.get(Kind.WEB);
				if (url == null || url.isEmpty()) return;
				Launcher.startBrowser(ViewerActivity.this, url);
			}
		});

		View thumbnail = findViewById(R.id.thumbnail);
		thumbnail.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Launcher.startCapture(ViewerActivity.this);
			}
		});

	}
	private void setAccessData() {
		mCardDb.setAccessDate(mCurrentCardId, new Date());
		getFwApplication().getController().getCardListController().mHandler.loadCardList();
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

		ImageView thumnail = (ImageView) findViewById(R.id.thumbnail);
		Bitmap bitmap = CardImageUtil.loadThumbnail(this, mCurrentCardId); // TODO worker
		thumnail.setImageBitmap(bitmap);
		getFwApplication().getModel().getCaptureModel().setCardBitmap(bitmap);
	}

}
