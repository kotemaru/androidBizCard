package org.kotemaru.android.bizcard.activity;

import java.io.IOException;
import java.util.List;

import org.kotemaru.android.bizcard.R;
import org.kotemaru.android.bizcard.model.CameraActivityModel;
import org.kotemaru.android.fw.FwActivityBase;
import org.kotemaru.android.fw.dialog.AlertDialogListener;
import org.kotemaru.android.fw.util.camera.CameraUtil;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

public class CameraActivity extends FwActivityBase<CameraActivityModel> {
	private static final String TAG = "CameraActivity";

	private static byte[] sPictureData = null;
	private static Bitmap sPictureBitmap = null;;

	private CameraActivityModel mModel;
	private Camera mCamera;
	private SurfaceView mPreview;
	private SurfaceView mOverlay;
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
			sPictureData = null;
		}
	}

	protected SurfaceView getOverlaySurfaceView() {
		return mOverlay;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fw_activity_camera);
		mModel = new CameraActivityModel();
		sPictureData = null;

		mPreview = (SurfaceView) findViewById(R.id.preview);
		mPreview.getHolder().addCallback(mCameraListener);

		mOverlay = (SurfaceView) findViewById(R.id.overlay);
		mPreview.getHolder().addCallback(mOverlayListener);

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
								//sPictureData = data;
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
	private Bitmap getRotateBitmap(byte[] data) {
		Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
		int deg = getDisplayOrientation();
		Matrix mat = new Matrix();
		mat.postRotate(deg);
		Bitmap bitmap2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mat, true);
		bitmap.recycle();
		return bitmap2;
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
		Log.e("DEBUG","getDisplayOrientation:"+result+":"+rotation);
		return result;
	}

	private void setPreviewAspect(Camera camera) {
		int rotation = getWindowManager().getDefaultDisplay().getRotation();

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
	}

	@Override
	protected void onResume() {
		super.onResume();
		// initCamera();
	}
	@Override
	protected void onPause() {
		CameraUtil.close();
		super.onPause();
	}
	private void initCamera() {
		CameraUtil.close();
		try {
			mCamera = CameraUtil.open();
			if (mCamera == null) {
				mModel.getDialogModel().setAlert("Error!!", "Can not access camera.", new AlertDialogListener() {
					@Override
					public void onDialogOkay(Activity activity) {
						activity.finish();
					}
				});
				update();
			}

			Camera.Parameters params = mCamera.getParameters();
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
			mCamera.setParameters(params);

			// mCamera.setDisplayOrientation(90); // portrate 固定
			mCamera.setDisplayOrientation(getDisplayOrientation());
			setPreviewAspect(mCamera);
		} catch (Exception e) {
			Log.e(TAG, e.toString(), e);
		}
	}

	private class CameraListener implements SurfaceHolder.Callback {
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			initCamera();
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
			// initCamera(holder);
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			CameraUtil.close();
			mCamera = null;
		}
	}

	private class OverlayListener implements SurfaceHolder.Callback {
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

		private static final float ASPECT = 1.654545454545455F;

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			surfaceHolder = holder;
			int w, h;
			if (width > height) {
				h = (int) (height * 0.8);
				w = (int) (h * ASPECT);
			} else {
				w = (int) (width * 0.8);
				h = (int) (w * ASPECT);
			}
			int top = (height - h) / 2;
			int left = (width - w) / 2;
			int bottom = top + h;
			int right = left + w;
			mRect.set(left, top, right, bottom);
			paint.setStrokeWidth(width / 100);
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			// nop.
		}
		public void onDraw() {
			drawRect(mRect, Color.CYAN);
		}

		public void drawRect(Rect rect1, int color) {
			try {
				Canvas canvas = surfaceHolder.lockCanvas();
				if (canvas != null) {
					try {
						canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
						paint.setColor(color);
						canvas.drawRect(rect1, paint);
					} finally {
						surfaceHolder.unlockCanvasAndPost(canvas);
					}
				}
			} catch (IllegalArgumentException e) {
				Log.w(TAG, e.toString());
			}
		}

	}
}
