package org.kotemaru.android.bizcard.activity;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;

public class CameraActivity extends FwActivityBase<MyApplication, CameraActivityModel> {
	private static final String TAG = "CameraActivity";

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
		setContentView(R.layout.fw_activity_camera);
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
			mCamera.setDisplayOrientation(getDisplayOrientation());
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

			int deg = getDisplayOrientation();
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

	private int getDisplayOrientation() {
		CameraInfo info = CameraUtil.getCameraInfo();
		int rotation = getWindowManager().getDefaultDisplay().getRotation();
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;  // compensate the mirror
		} else {  // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		Log.e("DEBUG", "getDisplayOrientation:" + result + ":" + rotation);
		return result;
	}

	private static final float CARD_ASPECT = 1.654545454545455F;

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

	private void setPreviewAspect(Camera camera) {
		Parameters params = camera.getParameters();
		Size rsize = params.getPictureSize();
		float raspect = (float) rsize.width / (float) rsize.height;
		float minDiff = 1000000000000F;
		Size psize = params.getPreviewSize();
		List<Size> sizes = params.getSupportedPreviewSizes();
		for (Size size : sizes) {
			float paspect = (float) size.width / (float) size.height;
			float diff = Math.abs(raspect - paspect);
			if (diff < minDiff) {
				psize = size;
				minDiff = diff;
			}
			if (diff == 0.0F) break;
		}
		params.setPreviewSize(psize.width, psize.height);
		camera.setParameters(params);

		FrameLayout.LayoutParams layoutParams = (LayoutParams) mPreview.getLayoutParams();
		if (mPreview.getWidth() < mPreview.getHeight()) {
			// portrait
			int cw = Math.min(psize.height, psize.width);
			int ch = Math.max(psize.height, psize.width);
			float aspect = (float) cw / ch;
			if (aspect < 0.0F) {
				layoutParams.width = Math.round(mPreview.getHeight() * aspect);
				layoutParams.height = mPreview.getHeight();
			} else {
				layoutParams.width = mPreview.getWidth();
				layoutParams.height = Math.round(mPreview.getWidth() / aspect);
			}
		} else {
			// landscape
			int cw = Math.max(psize.height, psize.width);
			int ch = Math.min(psize.height, psize.width);
			float aspect = (float) cw / ch;
			if (aspect < 0.0F) {
				layoutParams.width = mPreview.getWidth();
				layoutParams.height = Math.round(mPreview.getWidth() / aspect);
			} else {
				layoutParams.width = Math.round(mPreview.getHeight() * aspect);
				layoutParams.height = mPreview.getHeight();
			}
		}
		mPreview.setLayoutParams(layoutParams);
		mPreview.invalidate();
		mOverlay.setLayoutParams(layoutParams);
		mOverlay.invalidate();
	}

	private void setBestFps(Camera camera) {
		Parameters params = camera.getParameters();
		int min = 100000000;
		int max = 0;
		List<int[]> ranges = params.getSupportedPreviewFpsRange();
		for (int[] range : ranges) {
			if (min >= range[0] && range[1] >= max) {
				min = range[0];
				max = range[1];
			}
		}
		int[] r = new int[2];
		params.getPreviewFpsRange(r);
		Log.e("DEBUG", "--->" + r[0] + "," + r[1]);
		params.setPreviewFpsRange(min, max);
		mCamera.setParameters(params);
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
			mCamera = CameraUtil.open();
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

			// Camera.Parameters params = mCamera.getParameters();
			// List<Camera.Size> = params.getSupportedPictureSizes();
			// params.setAutoExposureLock(true);
			// params.setAutoWhiteBalanceLock(true);
			// params.setPreviewFpsRange(1, 20);
			// params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			// params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
			// Log.e("DEBUG","===>min="+params.getMinExposureCompensation());
			// int exposure = params.getMinExposureCompensation()
			// + (int)((params.getMaxExposureCompensation() - params.getMinExposureCompensation())*0.02);
			// params.setExposureCompensation(+3);
			// params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_DAYLIGHT);
			// mCamera.setParameters(params);

			// mCamera.setDisplayOrientation(90); // portrate 固定
			mCamera.setDisplayOrientation(getDisplayOrientation());
			setBestFps(mCamera);
			setPreviewAspect(mCamera);
			return true;
		} catch (Exception e) {
			Log.e(TAG, e.toString(), e);
			return false;
		}
	}

	private class CameraListener implements SurfaceHolder.Callback {
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			if (!initCamera()) {
				return;
			}
			try {
				mCamera.setPreviewDisplay(holder);
			} catch (IOException e) {
				e.printStackTrace();
			}
			mCamera.startPreview();
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format,
				int width, int height) {
			if (mCamera == null) return;
			mCamera.stopPreview();
			mCamera.startPreview();
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			if (mCamera != null) {
				mCamera.stopPreview();
			}
			// CameraUtil.close();
			// mCamera = null;
		}
	}

	private class OverlayListener implements SurfaceHolder.Callback, OverlaySurfaceListener {
		private SurfaceHolder surfaceHolder;

		private Paint paint = new Paint();
		private Rect mRect = new Rect();

		public OverlayListener() {
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			surfaceHolder = holder;
			surfaceHolder.setFormat(PixelFormat.TRANSPARENT);
			paint.setStyle(Style.STROKE);
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			surfaceHolder = holder;
			mRect = getClipRect(width, height);
			paint.setStrokeWidth(WindowUtil.dp2px(getApplication(), 2));
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			// nop.
		}

		@SuppressLint("WrongCall")
		public void doDraw() {
			try {
				Canvas canvas = surfaceHolder.lockCanvas();
				if (canvas != null) {
					try {
						onDraw(canvas);
					} finally {
						surfaceHolder.unlockCanvasAndPost(canvas);
					}
				}
			} catch (IllegalArgumentException e) {
				Log.w(TAG, e.toString());
			}
		}

		@Override
		public void onDraw(Canvas canvas) {
			canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
			paint.setColor(Color.CYAN);
			canvas.drawRect(mRect, paint);
		}

	}

}
