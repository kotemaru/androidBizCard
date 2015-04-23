package org.kotemaru.android.bizcard.util;

import java.lang.Thread.UncaughtExceptionHandler;

import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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

}
