package org.kotemaru.android.bizcard.activity;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.kotemaru.android.bizcard.Launcher;
import org.kotemaru.android.bizcard.Launcher.ExtraKey;
import org.kotemaru.android.bizcard.Launcher.ExtraValue;
import org.kotemaru.android.bizcard.MyApplication;
import org.kotemaru.android.bizcard.R;
import org.kotemaru.android.bizcard.controller.CaptureController;
import org.kotemaru.android.bizcard.model.CaptureActivityModel;
import org.kotemaru.android.bizcard.model.CardModel.Kind;
import org.kotemaru.android.bizcard.util.OCRUtil.WordInfo;
import org.kotemaru.android.fw.util.camera.CameraActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnGenericMotionListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;

public class CaptureActivity extends BaseActivity<CaptureActivityModel> {
	private static final String TAG = "CaptureActivity";

	private CaptureActivityModel mModel;
	private CaptureController mController;
	private SurfaceView mMainview;
	private SelectorListener mSelectorListener = new SelectorListener();
	private ScaleGestureDetector mScaleGestureDetector;

	@Override
	public CaptureActivityModel getActivityModel() {
		return mModel;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_capture);
		MyApplication app = MyApplication.getInstance();
		mModel = app.getModel().getCaptureModel();
		mController = app.getController().getCaptureController();

		mMainview = (SurfaceView) findViewById(R.id.mainView);
		mMainview.getHolder().addCallback(mSelectorListener);

		mScaleGestureDetector = new ScaleGestureDetector(this, mSelectorListener);
		mMainview.setOnTouchListener(new OnTouchListener() {
			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				mSelectorListener.onTouch(v, event);
				return mScaleGestureDetector.onTouchEvent(event);
			}
		});
		mMainview.setOnGenericMotionListener(mSelectorListener);

		// DEBUG
		if (mModel.getCardBitmap() == null) {
			try {
				AssetManager am = getAssets();
				InputStream in;
				in = am.open("test2.jpg");
				// Bitmap bitmap = ImageUtil.loadBitmap(in, new Point(1024,1024));
				Bitmap bitmap = BitmapFactory.decodeStream(in);
				mModel.setCardBitmap(bitmap);
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	protected void onLaunch(Intent intent) {
		ExtraValue mode = ExtraValue.toExtraValue(intent.getStringExtra(ExtraKey.CAPTURE_MODE.name()));
		Kind kind = Kind.toKind(intent.getStringExtra(ExtraKey.KIND.name()));
		switch (mode) {
		case WITH_TARGET:
			mModel.getDialogModel().setInformationIfRequire(this, R.string.info_select_chars);
			mModel.setTargetKind(kind);
			break;
		case AUTO_SETUP:
			mController.mHandler.doAutoSetup(this, mModel.getCardBitmap());
			break;
		default:
			break;
		}
	}

	@Override
	public void onUpdateInReadLocked(CaptureActivityModel model) {
		mSelectorListener.setBitmap(mModel.getCardBitmap());
		mSelectorListener.draw();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return createOptionsMenu(menu, MenuItemType.EDITOR, MenuItemType.CAMERA);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		MenuItemType type = MenuItemType.toMenuItemType(item.getTitle());
		if (type == MenuItemType.EDITOR && mSelectorListener.mSelection != null) {
			mController.mHandler.doScan(getBaseContext(),
					mSelectorListener.mBitmap,
					mSelectorListener.mSelection,
					mModel.getTargetKind());
			return true;
		}
		switch (type) {
		case CAMERA:
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			Launcher.startCapture(this, ExtraValue.AUTO_SETUP, Kind.NIL);
			return true;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == Launcher.CAMERA_REQUEST_CODE) {
			byte[] buff = CameraActivity.takePictureData();
			mController.mHandler.loadBitmap(this, buff);
		}
	}

	private class SelectorListener
			implements SurfaceHolder.Callback,
			ScaleGestureDetector.OnScaleGestureListener,
			OnGenericMotionListener, OnTouchListener
	{
		private SurfaceHolder mHolder;
		private int mWidth, mHeight;
		private float mFitScale;
		private float mScale;
		private float mCenterX = 0.5F, mCenterY = 0.5F;

		private Rect mSelection = null;
		private Bitmap mBitmap = null;
		private Matrix mMatrix = new Matrix();
		private Paint mPaint = new Paint();

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format,
				int width, int height) {
			//Log.e("DEBUG", "===>surfaceChanged");
			mHolder = holder;
			mWidth = width;
			mHeight = height;
			init();
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			mHolder = null;
		}
		public void setBitmap(Bitmap bitmap) {
			mBitmap = bitmap;
			init();
		}
		public void init() {
			//Log.e("DEBUG", "===>init:" + mBitmap + ":" + mHolder);
			if (mBitmap == null || mHolder == null) return;
			float scaleW = (float) mWidth / mBitmap.getWidth();
			float scaleH = (float) mHeight / mBitmap.getHeight();
			mFitScale = Math.min(scaleW, scaleH);
			mScale = mFitScale;
			draw();
		}

		public void draw() {
			try {
				if (mBitmap == null || mHolder == null) return;
				Canvas canvas = mHolder.lockCanvas();
				//Log.e("DEBUG", "===>draw:" + canvas + ":" + mScale);
				if (canvas == null) return;
				try {
					canvas.save();
					canvas.setMatrix(null);
					canvas.drawColor(Color.BLACK);

					mMatrix.reset();
					mMatrix.postTranslate(-mBitmap.getWidth() * mCenterX, -mBitmap.getHeight() * mCenterY);
					mMatrix.postScale(mScale, mScale);
					mMatrix.postTranslate(mWidth / 2, mHeight / 2);
					canvas.setMatrix(mMatrix);

					canvas.drawBitmap(mBitmap, 0.0F, 0.0F, mPaint);

					mPaint.setStyle(Paint.Style.STROKE);
					if (mSelection != null) {
						mPaint.setColor(Color.GREEN);
						canvas.drawRect(mSelection, mPaint);
					}
					if (mModel.getWordInfoList() != null) {
						mPaint.setColor(Color.CYAN);
						for (WordInfo winfo : mModel.getWordInfoList()) {
							canvas.drawRect(winfo.rect, mPaint);
						}
					}
				} finally {
					canvas.restore();
					mHolder.unlockCanvasAndPost(canvas);
				}
			} catch (IllegalArgumentException e) {
				Log.w(TAG, e.toString());
			}
		}

		private float mDragStartX, mDragStartY;

		private boolean onDraggable(MotionEvent event) {
			switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				mDragStartX = event.getX();
				mDragStartY = event.getY();
				break;
			case MotionEvent.ACTION_MOVE:
				mCenterX += (mDragStartX - event.getX()) * mScale / 100;
				mCenterY += (mDragStartY - event.getY()) * mScale / 100;
				mCenterX = Math.max(mCenterX, 0.0F);
				mCenterX = Math.min(mCenterX, 1.0F);
				mCenterY = Math.max(mCenterY, 0.0F);
				mCenterY = Math.min(mCenterY, 1.0F);
				draw();
				break;
			case MotionEvent.ACTION_UP:
				break;
			default:
				break;
			}
			return true;
		}

		@SuppressLint("ClickableViewAccessibility")
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getPointerCount() >= 2) return false;
			//Log.e("DEBUG", "===>onTouch:" + event);
			if (event.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
				return onDraggable(event);
			}
			List<WordInfo> words = mModel.getWordInfoList();
			if (words == null) return false;

			switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
				float offsetX = (mWidth / 2) - (mBitmap.getWidth() * mCenterX * mScale);
				float offsetY = (mHeight / 2) - (mBitmap.getHeight() * mCenterY * mScale);
				int rx = (int) ((event.getX() - offsetX) / mScale);
				int ry = (int) ((event.getY() - offsetY) / mScale);
				for (WordInfo winfo : words) {
					if (winfo.rect.contains(rx, ry)) {
						if (mSelection == null) {
							mSelection = new Rect(winfo.rect);
						} else {
							mSelection.union(winfo.rect);
						}
					}
				}
				draw();
				break;
			case MotionEvent.ACTION_UP:
				//Log.e("DEBUG", "===>select:" + mSelection);
				break;
			default:
				break;
			}
			return false;
		}
		public boolean _onTouch(View v, MotionEvent event) {
			if (event.getPointerCount() >= 2) return false;
			Log.e("DEBUG", "===>onTouch:" + event);
			if (event.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
				return onDraggable(event);
			}
			switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				float offsetX = (mWidth / 2) - (mBitmap.getWidth() * mCenterX * mScale);
				float offsetY = (mHeight / 2) - (mBitmap.getHeight() * mCenterY * mScale);
				mSelection.left = mSelection.right = (int) ((event.getX() - offsetX) / mScale);
				mSelection.top = mSelection.bottom = (int) ((event.getY() - offsetY) / mScale);
				draw();
				break;
			case MotionEvent.ACTION_MOVE:
				offsetX = (mWidth / 2) - (mBitmap.getWidth() * mCenterX * mScale);
				offsetY = (mHeight / 2) - (mBitmap.getHeight() * mCenterY * mScale);
				mSelection.right = (int) ((event.getX() - offsetX) / mScale);
				mSelection.bottom = (int) ((event.getY() - offsetY) / mScale);
				draw();
				break;
			case MotionEvent.ACTION_UP:
				//Log.e("DEBUG", "===>select:" + mSelection);
				break;
			default:
				break;
			}
			return false;
		}

		// for DEBUG
		@Override
		public boolean onGenericMotion(View v, MotionEvent event) {
			//Log.e("DEBUG", "===>onGenericMotion:" + event.getAxisValue(MotionEvent.AXIS_VSCROLL));
			if (event.getAction() == MotionEvent.ACTION_SCROLL) {
				float wheel = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
				mScale += (wheel * 0.1);
				mScale = Math.min(mFitScale * 3, mScale);
				mScale = Math.max(mFitScale / 3, mScale);
				draw();
				return true;
			}
			return false;
		}

		// for ScaleGestureDetector.OnScaleGestureListener
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			//Log.e("DEBUG", "===>onScale:" + mScale + ":" + detector.getScaleFactor() + ":" + detector.getFocusX());
			mScale *= detector.getScaleFactor();
			mScale = Math.min(mFitScale * 3, mScale);
			mScale = Math.max(mFitScale / 3, mScale);
			if (mScale > mFitScale) {
				mCenterX += mDragStartX - detector.getFocusX();
				mCenterY += mDragStartY - detector.getFocusY();
			} else {
				mCenterX = 0.5F;
				mCenterY = 0.5F;
			}
			draw();
			return true;
		}

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			mDragStartX = detector.getFocusX();
			mDragStartY = detector.getFocusY();
			return true;
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
		}

	}

}
