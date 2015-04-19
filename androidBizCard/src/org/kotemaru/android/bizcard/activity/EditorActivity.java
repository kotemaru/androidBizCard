package org.kotemaru.android.bizcard.activity;

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
			mModel.setCardModel(null);
			mCurrentCardId = intent.getExtras().getInt(Launcher.ExtraKey.ID.name());
			mController.mHandler.loadCard(mCurrentCardId);
			break;
		case AUTO_SETUP:
			mModel.getDialogModel().setInformationIfRequire(this, R.string.info_after_auto_setup);
			break;
		default:
			break;
		}
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
    	if (requestCode == Launcher.CAMERA_REQUEST_CODE) {
    		//byte[] data = CameraActivity.takePictureData();
    		//Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
    		Bitmap bitmap = CameraActivity.takePictureBitmap();
    		MyApplication.getInstance().getModel().getCaptureModel().setCardBitmap(bitmap);
    		Launcher.startCapture(this);
    	}
    }
	public void onClickCancel(View view) {
		finish();
	}
	public void onClickOk(View view) {
		CardDb db = MyApplication.getInstance().getCardDb();
		db.putCardModel(mModel.getCardModel());
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

}
