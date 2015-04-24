package org.kotemaru.android.bizcard.activity;

import java.util.HashMap;
import java.util.Map;

import org.kotemaru.android.bizcard.Launcher;
import org.kotemaru.android.bizcard.Launcher.ExtraKey;
import org.kotemaru.android.bizcard.Launcher.ExtraValue;
import org.kotemaru.android.bizcard.MyApplication;
import org.kotemaru.android.bizcard.R;
import org.kotemaru.android.bizcard.controller.EditorController;
import org.kotemaru.android.bizcard.model.CardHolderActivtyModel;
import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.bizcard.model.Kind;
import org.kotemaru.android.bizcard.util.DialogUtil;
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

	private View mContentRoot;
	private CardHolderActivtyModel mModel;
	private EditorController mController;
	private int mCurrentCardId;
	private Map<Kind, EditText> mEditTextMap = new HashMap<Kind, EditText>();

	@Override
	public CardHolderActivtyModel getActivityModel() {
		return mModel;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_editor);
		mContentRoot = findViewById(R.id.content_root);
		setTitle(R.string.title_editor);
		MyApplication app = MyApplication.getInstance();
		mModel = app.getModel().getCardHolderModel();
		mController = app.getController().getEditorController();
		initEditText();
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
				DialogUtil.setInformationIfRequire(mModel.getDialogModel(), R.string.info_editor_camera);
				mModel.setCardModel(new CardModel());
			} else {
				mController.mHandler.loadCard(mCurrentCardId);
			}
			break;
		case AUTO_SETUP:
			DialogUtil.setInformationIfRequire(mModel.getDialogModel(), R.string.info_after_auto_setup);
			break;
		default:
			break;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		viewToModel(mModel.getCardModel());
	}

	@Override
	public void onUpdateInReadLocked(CardHolderActivtyModel model) {
		modelToView(mModel.getCardModel());
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
		viewToModel(cardModel);
		mController.mHandler.register(this, cardModel);
	}

	private void initEditText() {
		for (Kind kind : Kind.values()) {
			String label = kind.getLabel(this);
			View item = kind.getTextView(mContentRoot);
			if (label == null || item == null) continue;

			TextView title = (TextView) item.findViewById(R.id.title);
			EditText editText = (EditText) item.findViewById(R.id.edit);
			ImageView search = (ImageView) item.findViewById(R.id.search);
			title.setText(label);
			search.setOnClickListener(getOnClickListener(kind));
			mEditTextMap.put(kind, editText);
		}
	}
	private OnClickListener getOnClickListener(final Kind kind) {
		return new OnClickListener() {
			@Override
			public void onClick(View v) {
				mEditTextMap.get(kind).requestFocus();
				Launcher.startCapture(EditorActivity.this, ExtraValue.WITH_TARGET, kind);
			}
		};
	}

	private void modelToView(CardModel model) {
		for (Kind kind : Kind.values()) {
			kind.modelToView(model, mContentRoot);
		}
	}
	private void viewToModel(CardModel model) {
		if (model == null) return;
		for (Kind kind : Kind.values()) {
			kind.viewToModel(mContentRoot, model);
		}
	}

}
