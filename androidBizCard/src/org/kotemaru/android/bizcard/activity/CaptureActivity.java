package org.kotemaru.android.bizcard.activity;

import java.util.ArrayList;
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
import android.view.View.OnGenericMotionListener;
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
		mMainview.setOnGenericMotionListener(mSelectorListener);

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
			mModel.getDialogModel().setInformationIfRequire(this, R.string.info_capture_camera);
			mModel.setTargetKind(Kind.NIL);
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
		if (type == MenuItemType.EDITOR) {
			if (mModel.getTargetKind() == Kind.NIL) { // auto setup mode
				mController.mHandler.doAutoSetup(this, mModel.getCardBitmap());
			} else if (mSelectorListener.mSelection != null) {
				mController.mHandler.doScan(getBaseContext(),
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
			mModel.setCardBitmap(bitmap);
			Launcher.startCapture(this, ExtraValue.AUTO_SETUP, Kind.NIL);
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

		private Rect mSelection = new Rect();
		private List<WordInfo> mSelectList = new ArrayList<WordInfo>();
		private Bitmap mBitmap = null;
		private Matrix mMatrix = new Matrix();
		private Paint mPaint = new Paint();

		// private Canvas mCanvas;
		// private Bitmap mCancasBitmap;
		private Handle mHandle1 = new Handle(100, 100, Color.argb(100, 0, 255, 255));
		private Handle mHandle2 = new Handle(200, 200, Color.argb(100, 0, 255, 255));

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format,
				int width, int height) {
			// Log.e("DEBUG", "===>surfaceChanged");
			mHolder = holder;
			mWidth = width;
			mHeight = height;
			// mCancasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			// mCanvas = new Canvas(mCancasBitmap);
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
					Log.e("DEBUG", "scale=" + mScale);
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
					// if (mHandle1 != null && mHandle2 != null) {
					// mPaint.setColor(Color.BLUE);
					// canvas.drawRect(mHandle1.x, mHandle1.y, mHandle2.x, mHandle2.y, mPaint);
					// }
					// if (mHandle1 != null) mHandle1.draw(canvas);
					// if (mHandle2 != null) mHandle2.draw(canvas);
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
				if (mScale > mFitScale) {
					mCenterX += (mDragStartX - event.getX()) / mWidth;
					mCenterY += (mDragStartY - event.getY()) / mHeight;

					// Log.e("DEBUG","==>center2:"+mCenterX+","+mCenterY+":"+detector.getFocusX()+","+detector.getFocusY());
					mCenterX = Math.max(mCenterX, 0.0F);
					mCenterX = Math.min(mCenterX, 1.0F);
					mCenterY = Math.max(mCenterY, 0.0F);
					mCenterY = Math.min(mCenterY, 1.0F);
				} else {
					mCenterX = 0.5F;
					mCenterY = 0.5F;
				}
				mDragStartX = event.getX();
				mDragStartY = event.getY();
				draw();
				break;
			case MotionEvent.ACTION_UP:
				break;
			default:
				break;
			}
			return true;
		}

		private Handle mDragHandle = null;

		@SuppressLint("ClickableViewAccessibility")
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getPointerCount() >= 2) return false;
			Log.e("DEBUG", "===>onTouch:" + event);
			if (event.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
				return onDraggable(event);
			}
			int evx = (int) event.getX();
			int evy = (int) event.getY();

			int action = event.getAction() & MotionEvent.ACTION_MASK;
			if (mDragHandle != null) {
				mDragHandle.move(evx, evy);
				draw();
				if (action == MotionEvent.ACTION_UP) mDragHandle = null;
				return true;
			}
			if (action == MotionEvent.ACTION_DOWN && mHandle1.contains(evx, evy)) {
				mDragHandle = mHandle1;
				return true;
			}
			if (action == MotionEvent.ACTION_DOWN && mHandle2.contains(evx, evy)) {
				mDragHandle = mHandle2;
				return true;
			}
			if (onSelection(v, event)) return true;
			return onDraggable(event);
			/*
			 * switch (action) {
			 * case MotionEvent.ACTION_DOWN:
			 * float offsetX = (mWidth / 2) - (mBitmap.getWidth() * mCenterX * mScale);
			 * float offsetY = (mHeight / 2) - (mBitmap.getHeight() * mCenterY * mScale);
			 * mHandle1.x = mHandle2.x = (int) ((event.getX() - offsetX) / mScale);
			 * mHandle1.y = mHandle2.y = (int) ((event.getY() - offsetY) / mScale);
			 * draw();
			 * break;
			 * case MotionEvent.ACTION_MOVE:
			 * offsetX = (mWidth / 2) - (mBitmap.getWidth() * mCenterX * mScale);
			 * offsetY = (mHeight / 2) - (mBitmap.getHeight() * mCenterY * mScale);
			 * mHandle2.x = (int) ((event.getX() - offsetX) / mScale);
			 * mHandle2.y = (int) ((event.getY() - offsetY) / mScale);
			 * draw();
			 * break;
			 * case MotionEvent.ACTION_UP:
			 * // Log.e("DEBUG", "===>select:" + mSelection);
			 * break;
			 * default:
			 * break;
			 * }
			 * return false;
			 */
		}

		// for DEBUG
		@Override
		public boolean onGenericMotion(View v, MotionEvent event) {
			// Log.e("DEBUG", "===>onGenericMotion:" + event.getAxisValue(MotionEvent.AXIS_VSCROLL));
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
			// Log.e("DEBUG", "===>onScale:" + mScale + ":" + detector.getScaleFactor() + ":" + detector.getFocusX());
			mScale *= detector.getScaleFactor();
			mScale = Math.min(mFitScale * 3, mScale);
			mScale = Math.max(mFitScale / 3, mScale);
			if (mScale > mFitScale) {
				mCenterX += (mDragStartX - detector.getFocusX()) / mWidth;
				mCenterY += (mDragStartY - detector.getFocusY()) / mHeight;
				// Log.e("DEBUG","==>center2:"+mCenterX+","+mCenterY+":"+detector.getFocusX()+","+detector.getFocusY());
				mCenterX = Math.max(mCenterX, 0.0F);
				mCenterX = Math.min(mCenterX, 1.0F);
				mCenterY = Math.max(mCenterY, 0.0F);
				mCenterY = Math.min(mCenterY, 1.0F);
			} else {
				mCenterX = 0.5F;
				mCenterY = 0.5F;
			}
			mDragStartX = detector.getFocusX();
			mDragStartY = detector.getFocusY();
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

		private class Handle {
			int x, y; // mBitmapの座標系
			int diffX = 40, diffY = 40; // dp
			int size = 12; // dp
			int color;

			public Handle(int x, int y, int color) {
				this.x = x;
				this.y = y;
				this.color = color;
			}

			public void draw(Canvas canvas) {
				mPaint.setStrokeWidth(dp2px(2));
				mPaint.setColor(color);
				int radius = dp2px(size);
				int cx = x + dp2px(diffX);
				int cy = y + dp2px(diffY);

				canvas.drawCircle(cx, cy, radius, mPaint);
				radius = (int) (WindowUtil.dp2px(mContext, 4) / mScale);
				canvas.drawCircle(cx, cy, radius, mPaint);
				canvas.drawLine(x, y, cx, cy, mPaint);
				mPaint.setAlpha(255);
			}
			private int dp2px(int dp) {
				return (int) (WindowUtil.dp2px(mContext, dp) / mScale);
			}

			public boolean contains(int evx, int evy) {
				evx = convertX(evx);
				evy = convertY(evy);
				int radius = dp2px(size * 3 / 2);
				int cx = x + dp2px(diffX);
				int cy = y + dp2px(diffY);
				boolean res = (cx - radius < evx && evx < cx + radius) && (cy - radius < evy && evy < cy + radius);
				Log.e("DEBUG", "===>contains:" + res + "=" + x + "," + y + ":" + evx + "," + evy + ":" + radius);
				return res;
			}
			public void move(int evx, int evy) {
				x = convertX(evx) - dp2px(diffX);
				y = convertY(evy) - dp2px(diffX);
			}
		}

		public int convertX(int evx) {
			float offsetX = (mWidth / 2) - (mBitmap.getWidth() * mCenterX * mScale);
			return (int) ((evx - offsetX) / mScale);
		}
		public int convertY(int evy) {
			float offsetY = (mHeight / 2) - (mBitmap.getHeight() * mCenterY * mScale);
			return (int) ((evy - offsetY) / mScale);
		}

		boolean mIsDragging = false;

		public boolean onSelection(View v, MotionEvent event) {
			List<WordInfo> words = mModel.getWordInfoList();
			if (words == null) return false;
			float offsetX = (mWidth / 2) - (mBitmap.getWidth() * mCenterX * mScale);
			float offsetY = (mHeight / 2) - (mBitmap.getHeight() * mCenterY * mScale);
			int rx = (int) ((event.getX() - offsetX) / mScale);
			int ry = (int) ((event.getY() - offsetY) / mScale);
			WordInfo selectWInfo = null;
			boolean isSelected = false;
			for (WordInfo winfo : words) {
				if (winfo.rect.contains(rx, ry)) {
					isSelected = mSelectList.contains(winfo);
					selectWInfo = winfo;
				}
			}

			switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				mIsDragging = false;
				break;
			case MotionEvent.ACTION_MOVE:
				mIsDragging = true;
				if (selectWInfo != null && !isSelected) {
					mSelectList.add(selectWInfo);
					draw();
					return true;
				}
				break;
			case MotionEvent.ACTION_UP:
				if (!mIsDragging) {
					if (selectWInfo != null) {
						if (isSelected) {
							mSelectList.remove(selectWInfo);
						} else {
							mSelectList.add(selectWInfo);
						}
					} else {
						mSelectList.clear();
					}
					draw();
					return true;
				}
				mIsDragging = false;
				break;
			default:
				break;
			}
			return false;
		}

	}

}
