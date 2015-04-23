package org.kotemaru.android.bizcard.activity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.kotemaru.android.bizcard.Launcher;
import org.kotemaru.android.bizcard.Launcher.ExtraKey;
import org.kotemaru.android.bizcard.Launcher.ExtraValue;
import org.kotemaru.android.bizcard.MyApplication;
import org.kotemaru.android.bizcard.R;
import org.kotemaru.android.bizcard.controller.EditorController;
import org.kotemaru.android.bizcard.database.CardDb;
import org.kotemaru.android.bizcard.model.CardHolderActivtyModel;
import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.bizcard.model.CardModel.Kind;
import org.kotemaru.android.bizcard.util.CardImageUtil;
import org.kotemaru.android.fw.FwActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class EditorActivity extends BaseActivity<CardHolderActivtyModel> implements FwActivity {
	private static final String TAG = EditorActivity.class.getSimpleName();

	private CardHolderActivtyModel mModel;
	private EditorController mController;
	private int mCurrentCardId;
	private Map<Kind, EditText> mEditMap = new HashMap<Kind, EditText>();

	@Override
	public CardHolderActivtyModel getActivityModel() {
		return mModel;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_editor);
		setTitle(R.string.title_editor);
		MyApplication app = MyApplication.getInstance();
		mModel = app.getModel().getCardHolderModel();
		mController = app.getController().getEditorController();

		View parent = findViewById(R.id.items);
		for (Kind kind : Kind.values()) {
			if (kind.labelResId == 0) continue;
			View item = parent.findViewById(kind.textViewResId);
			if (item == null) continue;
			TextView title = (TextView) item.findViewById(R.id.title);
			EditText edit = (EditText) item.findViewById(R.id.edit);
			ImageView search = (ImageView) item.findViewById(R.id.search);
			title.setText(getString(kind.labelResId));
			search.setOnClickListener(getOnClickListener(kind));
			mEditMap.put(kind, edit);
		}
	}

	private OnClickListener getOnClickListener(final Kind kind) {
		return new OnClickListener() {
			@Override
			public void onClick(View v) {
				mEditMap.get(kind).requestFocus();
				Launcher.startCapture(EditorActivity.this, ExtraValue.WITH_TARGET, kind);
			}
		};
	}

	@Override
	protected void onLaunch(Intent intent) {
		String editorModeStr = intent.getStringExtra(ExtraKey.EDITOR_MODE.name());
		ExtraValue editorMode = ExtraValue.toExtraValue(editorModeStr);
		switch (editorMode) {
		case INIT:
			Log.i(TAG, "onResume:reset data");
			mCurrentCardId = intent.getExtras().getInt(Launcher.ExtraKey.ID.name());
			if (mCurrentCardId == -1) {
				mModel.getDialogModel().setInformationIfRequire(this, R.string.info_editor_camera);
				mModel.setCardModel(new CardModel());
			} else {
				mController.mHandler.loadCard(mCurrentCardId);
			}
			break;
		case AUTO_SETUP:
			mModel.getDialogModel().setInformationIfRequire(this, R.string.info_after_auto_setup);
			break;
		default:
			break;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		saveTextView(mModel.getCardModel());
	}

	@Override
	public void onUpdateInReadLocked(CardHolderActivtyModel model) {
		setupTextView(mModel.getCardModel());
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return createOptionsMenu(menu, MenuItemType.CAMERA);
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		Log.i(TAG, "onActivityResult:" + requestCode + "," + resultCode);
		if (requestCode == Launcher.CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
			Bitmap bitmap = CameraActivity.takePictureBitmap();
			getFwApplication().getModel().getCaptureModel().setCardBitmap(bitmap);
			Launcher.startCapture(this, ExtraValue.AUTO_SETUP, Kind.NIL);
		}
	}
	public void onClickCancel(View view) {
		finish();
	}
	public void onClickOk(View view) {
		CardModel cardModel = mModel.getCardModel();
		saveTextView(cardModel);
		CardDb db = getFwApplication().getCardDb();
		for (int i=0; i<100; i++) {
			cardModel.put(Kind.NAME, "NAME-"+i);
			mCurrentCardId = db.putCardModel(cardModel);
		}
		cardModel.setId(mCurrentCardId);
		try {
			Bitmap bitmap = getFwApplication().getModel().getCaptureModel().getCardBitmap();
			if (bitmap != null) {
				String url = CardImageUtil.saveThumbnail(this, mCurrentCardId, bitmap);
				cardModel.put(Kind.IMAGE_URL, url);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		getFwApplication().getController().getCardListController().mHandler.loadCardList();
		finish();
	}

	private void setupTextView(CardModel model) {
		for (Kind kind : Kind.values()) {
			if (kind.textViewResId == 0) continue;
			EditText textView = mEditMap.get(kind);
			if (textView == null) continue;
			if (model != null) {
				textView.setText(model.get(kind));
			} else {
				textView.setText(null);
			}
		}
	}
	private void saveTextView(CardModel model) {
		if (model == null) return;
		for (Kind kind : Kind.values()) {
			if (kind.textViewResId == 0) continue;
			EditText textView = mEditMap.get(kind);
			if (textView == null) continue;
			if (textView.getText() == null) {
				model.put(kind, null);
			} else {
				model.put(kind, textView.getText().toString());
			}
		}
	}

}
