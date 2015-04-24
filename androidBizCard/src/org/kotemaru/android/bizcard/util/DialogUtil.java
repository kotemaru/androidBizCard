package org.kotemaru.android.bizcard.util;

import org.kotemaru.android.bizcard.MyApplication;
import org.kotemaru.android.fw.dialog.DialogModel;
import org.kotemaru.android.fw.plugin.dialog.InformationDialogBuilder;

import android.content.Context;

public class DialogUtil {
	public static boolean setInformationIfRequire(DialogModel dialogModel, int resId) {
		Context context = MyApplication.getInstance();
		if (InformationDialogBuilder.isRequireShown(context, resId)) {
			dialogModel.setDialogBuilderLocked(new InformationDialogBuilder(context, resId, null));
			return true;
		}
		return false;
	}
}
