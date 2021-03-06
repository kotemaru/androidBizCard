package org.kotemaru.android.bizcard;

import org.kotemaru.android.bizcard.activity.CameraActivity;
import org.kotemaru.android.bizcard.activity.CaptureActivity;
import org.kotemaru.android.bizcard.activity.CardListActivity;
import org.kotemaru.android.bizcard.activity.EditorActivity;
import org.kotemaru.android.bizcard.activity.ViewerActivity;
import org.kotemaru.android.bizcard.model.Kind;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class Launcher {
	private static final String TAG = Launcher.class.getSimpleName();
	public static final int CAMERA_REQUEST_CODE = 10001;

	public enum ExtraKey {
		ID, EDITOR_MODE, CAPTURE_MODE, KIND;
	}

	public enum ExtraValue {
		NIL, INIT, VIEW, WITH_TARGET, AUTO_SETUP;

		public static ExtraValue toExtraValue(String name) {
			if (name == null) return ExtraValue.NIL;
			return ExtraValue.valueOf(name);
		}
	}

	public static void startMain(Context context) {
		Log.d(TAG, "startMain");
		Intent intent = new Intent(context, CardListActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(intent);
	}

	public static void startCamera(Activity context) {
		Log.d(TAG, "startCamera");
		Intent intent = new Intent(context, CameraActivity.class);
		// intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
		context.startActivityForResult(intent, CAMERA_REQUEST_CODE);
	}

	public static void startCapture(Context context, ExtraValue mode, Kind kind) {
		Log.d(TAG, "startCapture:" + kind);
		Intent intent = new Intent(context, CaptureActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
		intent.putExtra(ExtraKey.CAPTURE_MODE.name(), mode.name());
		intent.putExtra(ExtraKey.KIND.name(), kind.name());
		context.startActivity(intent);
	}
	public static void startCapture(Context context) {
		startCapture(context, ExtraValue.NIL, Kind.NIL);
	}

	public static void startViewer(Context context, int cardId) {
		Log.d(TAG, "startViewer:" + cardId);
		Intent intent = new Intent(context, ViewerActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
		intent.putExtra(ExtraKey.ID.name(), cardId);
		context.startActivity(intent);
	}

	public static void startEditor(Context context, ExtraValue mode, int cardId) {
		Log.d(TAG, "startEditor:" + cardId);
		Intent intent = new Intent(context, EditorActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
		intent.putExtra(ExtraKey.EDITOR_MODE.name(), mode.name());
		intent.putExtra(ExtraKey.ID.name(), cardId);
		context.startActivity(intent);
	}

	public static void startEditor(Context context) {
		startEditor(context, ExtraValue.NIL, -1);
	}

	public static void startDialer(Context context, CharSequence tel) {
		Log.d(TAG, "startDialer:" + tel);
		Uri uri = Uri.parse("tel:" + tel);
		Intent intent = new Intent(Intent.ACTION_DIAL, uri);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

	public static void startMailer(Context context, CharSequence email) {
		Log.d(TAG, "startMailer:" + email);
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setAction(Intent.ACTION_SEND);
		intent.setType("message/rfc822");
		intent.putExtra(Intent.EXTRA_EMAIL, new String[] { email.toString() });
		context.startActivity(intent);
	}
	public static void startBrowser(Context context, CharSequence url) {
		Log.d(TAG, "startBrowser:" + url);
		Uri uri = Uri.parse(url.toString());
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}
}
