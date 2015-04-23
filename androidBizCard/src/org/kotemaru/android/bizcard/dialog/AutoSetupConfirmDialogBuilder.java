package org.kotemaru.android.bizcard.dialog;

import org.kotemaru.android.fw.dialog.DialogBuilder;
import org.kotemaru.android.fw.dialog.DialogModel;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;

public class AutoSetupConfirmDialogBuilder implements DialogBuilder {

	private final AutoSetupConfirmListener mListener;

	public interface AutoSetupConfirmListener {
		public void onAutoSetup(Activity activity);
		public void onManualSetup(Activity activity);
	}

	public AutoSetupConfirmDialogBuilder(AutoSetupConfirmListener listener) {
		mListener = listener;
	}

	@Override
	public Dialog create(final Activity activity, final DialogModel model) {
		AlertDialog.Builder builer = new AlertDialog.Builder(activity)
				.setTitle(null)
				.setMessage("自動設定しますか？");
		builer.setCancelable(true);
		builer.setPositiveButton("自動設定", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (mListener != null) mListener.onAutoSetup(activity);
				dialog.dismiss();
				model.clear();
			}
		});
		builer.setNegativeButton("手動設定", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (mListener != null) mListener.onManualSetup(activity);
				dialog.dismiss();
				model.clear();
			}
		});
		return builer.create();
	}
	@Override
	public Dialog update(Activity activity, DialogModel model, Dialog dialog) {
		return dialog;
	}
}
