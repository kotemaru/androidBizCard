package org.kotemaru.android.bizcard.model;

import org.kotemaru.android.bizcard.MyApplication;
import org.kotemaru.android.fw.ModelLock;
import org.kotemaru.android.fw.dialog.DialogModel;
import org.kotemaru.android.fw.plugin.dialog.InformationDialogBuilder;

import android.content.Context;

public class CmDialogModel extends DialogModel {
	public CmDialogModel(ModelLock parentLock) {
		super(parentLock);
	}

	public boolean setInformationIfRequire(int resId) {
		Context context = MyApplication.getInstance();
		if (InformationDialogBuilder.isRequireShown(context, resId)) {
			super.setDialogBuilderLocked(new InformationDialogBuilder(context, resId, null));
			return true;
		}
		return false;
	}

}
