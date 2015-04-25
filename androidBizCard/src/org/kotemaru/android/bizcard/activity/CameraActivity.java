package org.kotemaru.android.bizcard.activity;

import java.io.IOException;
import java.io.InputStream;

import org.kotemaru.android.bizcard.BuildConfig;
import org.kotemaru.android.bizcard.MyApplication;
import org.kotemaru.android.bizcard.R;
import org.kotemaru.android.bizcard.model.CameraActivityModel;
import org.kotemaru.android.bizcard.util.CameraUtil;
import org.kotemaru.android.bizcard.widget.OverlaySurfaceView;
import org.kotemaru.android.bizcard.widget.OverlaySurfaceView.OverlaySurfaceListener;
import org.kotemaru.android.fw.FwActivityBase;
import org.kotemaru.android.fw.dialog.AlertDialogListener;
import org.kotemaru.android.fw.util.WindowUtil;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

public class CameraActivity extends FwActivityBase<MyApplication, CameraActivityModel> {
	public static final String TAG = CameraActivity.class.getSimpleName();
	private static final float CARD_ASPECT = 1.654545454545455F;

	private static byte[] sPictureData = null;
	private static Bitmap sPictureBitmap = null;;

	private CameraActivityModel mModel;
	private Camera mCamera;
	private SurfaceView mPreview;
	private OverlaySurfaceView mOverlay;
	private ImageView mShutterButton;
	private CameraListener mCameraListener = new CameraListener();
	private OverlayListener mOverlayListener = new OverlayListener();

	public static byte[] __takePictureData() {
		try {
			return sPictureData;
		} finally {
			sPictureData = null;
		}
	}

	public static Bitmap takePictureBitmap() {
		try {
			return sPictureBitmap;
		} finally {
			sPictureBitmap = null;
		}
	}

	protected SurfaceView getOverlaySurfaceView() {
		return mOverlay;
	}

	@Override
	public MyApplication getFwApplication() {
		return MyApplication.getInstance();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);
		mModel = new CameraActivityModel();
		sPictureBitmap = null;

		mPreview = (SurfaceView) findViewById(R.id.preview);
		mPreview.getHolder().addCallback(mCameraListener);

		mOverlay = (OverlaySurfaceView) findViewById(R.id.overlay);
		mPreview.getHolder().addCallback(mOverlayListener);
		mOverlay.setOverlaySurfaceListener(mOverlayListener);

		mShutterButton = (ImageView) findViewById(R.id.shutter_button);
		mShutterButton.setOnClickListener(mDefaultShutterAction);
	}

	protected OnClickListener mDefaultShutterAction = new OnClickListener() {
		@Override
		public void onClick(View v) {
			mCamera.setDisplayOrientation(CameraUtil.getDisplayOrientation(CameraActivity.this));
			mCamera.autoFocus(new AutoFocusCallback() {
				@Override
				public void onAutoFocus(boolean success, Camera camera) {
					if (success) {
						mCamera.takePicture(null, null, new PictureCallback() {
							@Override
							public void onPictureTaken(byte[] data, Camera camera) {
								// sPictureData = data;
								sPictureBitmap = getRotateBitmap(data);
								setResult(RESULT_OK, null);
								finish();
							}
						});
					} else {
						Toast.makeText(CameraActivity.this, "Failed focus.", Toast.LENGTH_SHORT).show();
					}
				}
			});
		}
	};

	// DEBUG
	private void setDummyBitmap() {
		try {
			AssetManager am = getAssets();
			InputStream in;
			in = am.open("test2.jpg");
			// Bitmap bitmap = ImageUtil.loadBitmap(in, new Point(1024,1024));
			Bitmap bitmap = BitmapFactory.decodeStream(in);
			sPictureBitmap = bitmap;
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Bitmap getRotateBitmap(byte[] data) {
		BitmapRegionDecoder decoder;
		try {
			decoder = BitmapRegionDecoder.newInstance(data, 0, data.length, false);
			Rect clipRect = getClipRect(decoder.getWidth(), decoder.getHeight());
			Bitmap bitmap = decoder.decodeRegion(clipRect, null);

			int deg = CameraUtil.getDisplayOrientation(this);
			if (deg == 0) return bitmap;
			Matrix mat = new Matrix();
			mat.postRotate(deg);
			Bitmap bitmap2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mat, true);
			bitmap.recycle();
			return bitmap2;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public CameraActivityModel getActivityModel() {
		return mModel;
	}

	@SuppressLint("WrongCall")
	@Override
	public void onUpdateInReadLocked(CameraActivityModel model) {
		// mOverlayListener.onDraw();
	}


	private Rect getClipRect(int width, int height) {
		int w, h;
		if (width < height) {
			h = (int) ((float) height * 0.8F);
			w = (int) (h / CARD_ASPECT);
		} else {
			w = (int) ((float) width * 0.8F);
			h = (int) (w / CARD_ASPECT);
		}
		int top = (height - h) / 2;
		int left = (width - w) / 2;
		int bottom = top + h;
		int right = left + w;
		return new Rect(left, top, right, bottom);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mOverlay.invalidate();
		// initCamera();
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		CameraUtil.close();
		mCamera = null;
		super.onPause();
	}

	private boolean initCamera() {
		if (mCamera != null) return true;
		try {
			//mCamera = CameraUtil.open();
			if (mCamera == null) {
				mModel.getDialogModel().setAlert("Error!!", "Can not open camera.", new AlertDialogListener() {
					@Override
					public void onDialogOkay(Activity activity) {
						if (BuildConfig.DEBUG) {
							setDummyBitmap();
							setResult(RESULT_OK, null);
						}
						activity.finish();
					}
				});
				update();
				return false;
			}
			CameraUtil.setupBestParameters(mCamera, this, mPreview);
			mOverlay.setLayoutParams(mPreview.getLayoutParams());
			mOverlay.invalidate();
			return true;
		} catch (Exception e) {
			Log.e(TAG, e.toString(), e);
			return false;
		}
	}

	private class CameraListener implements SurfaceHolder.Callback {
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			if (!initCamera()) return;
			try {
				mCamera.setPreviewDisplay(holder);
			} catch (IOException e) {
				Log.e(TAG, "Camera error:" + e, e);
			}
			mCamera.startPreview();
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			if (mCamera == null) return;
			mCamera.stopPreview();
			mCamera.startPreview();
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			if (mCamera != null) {
				mCamera.stopPreview();
			}
		}
	}

	private class OverlayListener implements SurfaceHolder.Callback, OverlaySurfaceListener {
		private SurfaceHolder mSurfaceHolder;
		private Paint mPaint = new Paint();
		private Rect mRect = new Rect();

		public OverlayListener() {
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			mSurfaceHolder = holder;
			mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
			mPaint.setStyle(Style.STROKE);
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			mSurfaceHolder = holder;
			mRect = getClipRect(width, height);
			mPaint.setStrokeWidth(WindowUtil.dp2px(getApplication(), 2));
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			// nop.
		}

		@SuppressLint("WrongCall")
		public void doDraw() {
			try {
				Canvas canvas = mSurfaceHolder.lockCanvas();
				if (canvas != null) {
					try {
						onDraw(canvas);
					} finally {
						mSurfaceHolder.unlockCanvasAndPost(canvas);
					}
				}
			} catch (IllegalArgumentException e) {
				Log.w(TAG, e.toString());
			}
		}

		@Override
		public void onDraw(Canvas canvas) {
			canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
			mPaint.setColor(Color.CYAN);
			canvas.drawRect(mRect, mPaint);
		}
	}

}
