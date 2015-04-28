package org.kotemaru.android.bizcard.activity;

import java.util.ArrayList;
import java.util.List;

import org.kotemaru.android.bizcard.Launcher;
import org.kotemaru.android.bizcard.Launcher.ExtraKey;
import org.kotemaru.android.bizcard.Launcher.ExtraValue;
import org.kotemaru.android.bizcard.MyApplication;
import org.kotemaru.android.bizcard.R;
import org.kotemaru.android.bizcard.controller.CaptureController;
import org.kotemaru.android.bizcard.logic.ocr.WordInfo;
import org.kotemaru.android.bizcard.model.CaptureActivityModel;
import org.kotemaru.android.bizcard.model.Kind;
import org.kotemaru.android.fw.util.WindowUtil;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
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
import android.view.View.OnTouchListener;

public class CaptureActivity extends BaseActivity<CaptureActivityModel> {
	private static final String TAG = "CaptureActivity";

	private Context mContext;
	private CaptureActivityModel mModel;
	private CaptureController mController;
	private SurfaceView mMainview;
	private SelectorListener mSelectorListener;
	private ScaleGestureDetector mScaleGestureDetector;

	@Override
	public CaptureActivityModel getActivityModel() {
		return mModel;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_capture);
		setTitle(R.string.title_capture);
		MyApplication app = MyApplication.getInstance();
		mModel = app.getModel().getCaptureModel();
		mController = app.getController().getCaptureController();

		mContext = this;
		mSelectorListener = new SelectorListener();
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
		// mMainview.setOnGenericMotionListener(mSelectorListener);
	}

	@Override
	protected void onLaunch(Intent intent) {
		ExtraValue mode = ExtraValue.toExtraValue(intent.getStringExtra(ExtraKey.CAPTURE_MODE.name()));
		Kind kind = Kind.toKind(intent.getStringExtra(ExtraKey.KIND.name()));
		mModel.writeLock();
		try {
			switch (mode) {
			case WITH_TARGET:
				if (mModel.getWordInfoList() == null) {
					mController.mHandler.doAnalyzeAll(this, mModel.getCardBitmap(), true);
				} else {
					mModel.getDialogModel().setInformationIfRequire(R.string.info_select_chars);
				}
				mModel.setTargetKind(kind);
				mModel.setEditMode(true);
				break;
			case AUTO_SETUP:
				mModel.setWordInfoList(null);
				// DialogUtil.setInformationIfRequire(mModel.getDialogModel(), R.string.info_capture_camera);
				mModel.setTargetKind(Kind.NIL);
				mModel.setEditMode(true);
				mController.mHandler.doAnalyzeAll(this, mModel.getCardBitmap(), false);
				break;
			case VIEW:
				mModel.setWordInfoList(null);
				mModel.setEditMode(false);
			default:
				break;
			}
		} finally {
			mModel.writeUnlock();
		}
		intent.removeExtra(ExtraKey.CAPTURE_MODE.name());
	}

	@Override
	public void onUpdate(CaptureActivityModel model) {
		mSelectorListener.setBitmap(mModel.getCardBitmap());
		mSelectorListener.draw();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return createOptionsMenu(menu, MenuItemType.EDITOR, MenuItemType.CAMERA);
	}
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		for (int i = 0; i < menu.size(); i++) {
			MenuItem item = menu.getItem(i);
			item.setVisible(mModel.isEditMode());
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		MenuItemType type = MenuItemType.toMenuItemType(item.getItemId());
		if (type == MenuItemType.EDITOR) {
			if (mModel.getTargetKind() == Kind.NIL) { // auto setup mode
				// mController.mHandler.doAnalyzeAll(this, mModel.getCardBitmap());
			} else if (mSelectorListener.mSelection != null) {
				mController.mHandler.doAnalyzeOne(getBaseContext(),
						mSelectorListener.mBitmap,
						mSelectorListener.mSelection,
						mModel.getTargetKind());
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == Launcher.CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
			Bitmap bitmap = CameraActivity.takePictureBitmap();
			mModel.reset(bitmap);
			Launcher.startCapture(this, ExtraValue.AUTO_SETUP, Kind.NIL);
		}
	}

	public enum DragMode {
		NONE, SELECT_CLICK, SELECT_DRAG, CLICK, SCROLL, SCALE
	}

	private class SelectorListener
			implements SurfaceHolder.Callback,
			ScaleGestureDetector.OnScaleGestureListener,
			OnTouchListener
	{
		private SurfaceHolder mHolder;
		private int mWidth, mHeight;
		private float mFitScale;
		private float mScale;
		private float mCenterX = 0.5F, mCenterY = 0.5F;
		private float mDragStartX, mDragStartY;

		private Rect mSelection = new Rect();
		private List<WordInfo> mSelectList = new ArrayList<WordInfo>();
		private Bitmap mBitmap = null;
		private Matrix mMatrix = new Matrix();
		private Paint mPaint = new Paint();
		private DragMode mDragMode = DragMode.NONE;

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
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
			// Log.e("DEBUG", "===>init:" + mBitmap + ":" + mHolder);
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
				// Log.e("DEBUG", "===>draw:" + canvas + ":" + mScale);
				if (canvas == null) return;
				try {
					canvas.save();
					// Log.e("DEBUG", "scale=" + mScale);
					canvas.setMatrix(null);
					canvas.drawColor(Color.BLACK);

					mMatrix.reset();
					float cx = -mBitmap.getWidth() * mCenterX;
					float cy = -mBitmap.getHeight() * mCenterY;
					mMatrix.postTranslate(cx, cy);
					mMatrix.postScale(mScale, mScale);
					mMatrix.postTranslate(mWidth / 2, mHeight / 2);
					canvas.concat(mMatrix);
					canvas.drawBitmap(mBitmap, 0F, 0F, mPaint);

					mPaint.setStyle(Paint.Style.STROKE);
					mPaint.setStrokeWidth((WindowUtil.dp2px(mContext, 1) / mScale));
					if (mModel.getWordInfoList() != null) {
						mPaint.setColor(Color.CYAN);
						for (WordInfo winfo : mModel.getWordInfoList()) {
							canvas.drawRect(winfo.rect, mPaint);
						}
					}

					if (!mSelectList.isEmpty()) {
						mSelection.setEmpty();
						mPaint.setStyle(Paint.Style.FILL);
						mPaint.setAlpha(100);
						for (WordInfo wi : mSelectList) {
							canvas.drawRect(wi.rect, mPaint);
							mSelection.union(wi.rect);
						}
						mPaint.setAlpha(255);
						mPaint.setStyle(Paint.Style.STROKE);
						mPaint.setColor(Color.BLUE);
						canvas.drawRect(mSelection, mPaint);
					}
				} finally {
					canvas.restore();
					mHolder.unlockCanvasAndPost(canvas);
				}
			} catch (IllegalArgumentException e) {
				Log.w(TAG, e.toString());
			}
		}

		@SuppressLint("ClickableViewAccessibility")
		public boolean onTouch(View v, MotionEvent event) {
			// Log.e("DEBUG", "===>onTouch:" + event);
			if (event.getPointerCount() >= 2) return false;
			if (onSelection(v, event)) {
				draw();
				return true;
			}
			return onDraggable(event);
		}

		public boolean onSelection(View v, MotionEvent event) {
			WordInfo winfo = getWordInfo(event.getX(), event.getY());
			boolean isSelected = mSelectList.contains(winfo);
			// Log.e("DEBUG", "===>onSelection:"+event.getAction()+":" + winfo);

			switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				if (winfo != null) {
					mDragMode = DragMode.SELECT_CLICK;
					return true;
				}
				break;
			case MotionEvent.ACTION_MOVE:
				if (mDragMode != DragMode.SELECT_CLICK && mDragMode != DragMode.SELECT_DRAG) break;
				if (winfo != null && !isSelected) {
					mSelectList.add(winfo);
				}
				mDragMode = DragMode.SELECT_DRAG;
				return true;
			case MotionEvent.ACTION_UP:
				if (mDragMode != DragMode.SELECT_CLICK && mDragMode != DragMode.SELECT_DRAG) break;
				if (mDragMode == DragMode.SELECT_CLICK) {
					if (winfo != null && isSelected) {
						mSelectList.remove(winfo);
					} else if (winfo != null && !isSelected) {
						mSelectList.add(winfo);
					}
				}
				mDragMode = DragMode.NONE;
				return true;
			default:
				break;
			}
			return false;
		}

		private boolean onDraggable(MotionEvent event) {
			switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				mDragMode = DragMode.CLICK;
				mDragStartX = event.getX();
				mDragStartY = event.getY();
				break;
			case MotionEvent.ACTION_MOVE:
				if (mDragMode != DragMode.CLICK && mDragMode != DragMode.SCROLL) break;
				scroll(event.getX(), event.getY());
				mDragStartX = event.getX();
				mDragStartY = event.getY();
				draw();
				mDragMode = DragMode.SCROLL;
				break;
			case MotionEvent.ACTION_UP:
				if (mDragMode == DragMode.CLICK) {
					mSelectList.clear();
					draw();
				}
				mDragMode = DragMode.NONE;
				break;
			default:
				break;
			}
			return true;
		}

		// for ScaleGestureDetector.OnScaleGestureListener
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			mScale *= detector.getScaleFactor();
			mScale = Math.min(mFitScale * 3, mScale);
			mScale = Math.max(mFitScale / 3, mScale);
			scroll(detector.getFocusX(), detector.getFocusY());
			onScaleBegin(detector);
			draw();
			return true;
		}

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			mDragMode = DragMode.SCALE;
			mDragStartX = detector.getFocusX();
			mDragStartY = detector.getFocusY();
			return true;
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
			mDragMode = DragMode.NONE;
		}

		// ------------------------------------------------------------------------------
		// privates
		private int toBitmapX(float evx) {
			float offsetX = (mWidth / 2) - (mBitmap.getWidth() * mCenterX * mScale);
			return (int) ((evx - offsetX) / mScale);
		}
		private int toBitmapY(float evy) {
			float offsetY = (mHeight / 2) - (mBitmap.getHeight() * mCenterY * mScale);
			return (int) ((evy - offsetY) / mScale);
		}

		private WordInfo getWordInfo(float evx, float evy) {
			List<WordInfo> words = mModel.getWordInfoList();
			if (words == null) return null;
			int rx = toBitmapX(evx);
			int ry = toBitmapY(evy);
			for (WordInfo winfo : words) {
				if (winfo.rect.contains(rx, ry)) return winfo;
			}
			return null;
		}

		private void scroll(float evx, float evy) {
			if (mScale > mFitScale) {
				mCenterX += (mDragStartX - evx) / mWidth;
				mCenterY += (mDragStartY - evy) / mHeight;

				mCenterX = Math.max(mCenterX, 0.0F);
				mCenterX = Math.min(mCenterX, 1.0F);
				mCenterY = Math.max(mCenterY, 0.0F);
				mCenterY = Math.min(mCenterY, 1.0F);
			} else {
				mCenterX = 0.5F;
				mCenterY = 0.5F;
			}
		}

	}

}
