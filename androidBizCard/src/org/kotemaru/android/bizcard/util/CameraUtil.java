package org.kotemaru.android.bizcard.util;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.List;

import android.app.Activity;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

public class CameraUtil {
	private static final String TAG = "CameraUtil";

	private static Camera sCamera = null;
	private static Camera.CameraInfo sCameraInfo = null;

	private static UncaughtExceptionHandler sOriginExceptionHandler;
	private static UncaughtExceptionHandler sExceptionHandler;

	private static void initExcepHandler() {
		if (sExceptionHandler != null) return;
		sOriginExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
		sExceptionHandler = new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread thread, final Throwable ex) {
				CameraUtil.close();
				if (sOriginExceptionHandler != null) {
					sOriginExceptionHandler.uncaughtException(thread, ex);
				}
				Handler handler = new Handler(Looper.getMainLooper());
				handler.post(new Runnable() {
					@Override
					public void run() {
						if (ex instanceof Error) {
							throw (Error) ex;
						} else if (ex instanceof RuntimeException) {
							throw (RuntimeException) ex;
						} else {
							throw new Error(ex);
						}
					}
				});
			}
		};
		Thread.setDefaultUncaughtExceptionHandler(sExceptionHandler);
	}

	public static Camera.CameraInfo getCameraInfo() {
		return sCameraInfo;
	}

	public static Camera open() {
		initExcepHandler();
		try {
			int cameraId = -1;
			// リアカメラを探す。
			Camera.CameraInfo info = new Camera.CameraInfo();
			for (int id = 0; id < Camera.getNumberOfCameras(); id++) {
				Camera.getCameraInfo(id, info);
				if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
					cameraId = id;
					break;
				}
			}
			sCamera = Camera.open(cameraId);
			sCameraInfo = info;
			Log.i(TAG, "open:" + sCamera);
			return sCamera;
		} catch (Exception e) {
			Log.e(TAG, e.toString(), e);
			return null;
		}
	}

	public static void close() {
		if (sCamera == null) return;
		try {
			sCamera.stopPreview();
		} finally {
			sCamera.release();
			sCamera = null;
		}
	}

	public static void setupBestParameters(Camera camera, Activity activity, SurfaceView preview) {
		camera.setDisplayOrientation(getDisplayOrientation(activity));

		Camera.Parameters params = camera.getParameters();
		int[] range = getBestFpsRange(camera);
		params.setPreviewFpsRange(range[0], range[1]);
		Camera.Size size = getBsetPreviewSize(camera);
		params.setPreviewSize(size.width, size.height);
		camera.setParameters(params);

		Point viewSize = getBsetSurfaceViewSize(preview, size);
		FrameLayout.LayoutParams layoutParams = (LayoutParams) preview.getLayoutParams();
		layoutParams.width = viewSize.x;
		layoutParams.height = viewSize.y;
		preview.setLayoutParams(layoutParams);
		preview.invalidate();
	}


	public static int getDisplayOrientation(Activity activity) {
		CameraInfo info = CameraUtil.getCameraInfo();
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
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
		} else {  // back-camra
			result = (info.orientation - degrees + 360) % 360;
		}
		return result;
	}

	public static int[] getBestFpsRange(Camera camera) {
		Parameters params = camera.getParameters();
		int min = Integer.MAX_VALUE;
		int max = 0;

		List<int[]> ranges = params.getSupportedPreviewFpsRange();
		for (int[] range : ranges) {
			if (min >= range[0] && range[1] >= max) {
				min = range[0];
				max = range[1];
			}
		}
		return new int[]{min, max};
	}

	public static Size getBsetPreviewSize(Camera camera) {
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
		return psize;
	}

	public static Point getBsetSurfaceViewSize(SurfaceView preview, Size psize) {
		Point bestSize = new Point(0,0);
		if (preview.getWidth() < preview.getHeight()) {
			// portrait
			int cw = Math.min(psize.height, psize.width);
			int ch = Math.max(psize.height, psize.width);
			float aspect = (float) cw / ch;
			if (aspect < 0.0F) {
				bestSize.x = Math.round(preview.getHeight() * aspect);
				bestSize.y = preview.getHeight();
			} else {
				bestSize.x = preview.getWidth();
				bestSize.y = Math.round(preview.getWidth() / aspect);
			}
		} else {
			// landscape
			int cw = Math.max(psize.height, psize.width);
			int ch = Math.min(psize.height, psize.width);
			float aspect = (float) cw / ch;
			if (aspect < 0.0F) {
				bestSize.x = preview.getWidth();
				bestSize.y = Math.round(preview.getWidth() / aspect);
			} else {
				bestSize.x = Math.round(preview.getHeight() * aspect);
				bestSize.y = preview.getHeight();
			}
		}
		return bestSize;
	}

}
