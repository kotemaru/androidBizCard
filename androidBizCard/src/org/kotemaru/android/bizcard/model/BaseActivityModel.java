package org.kotemaru.android.bizcard.model;

import org.kotemaru.android.fw.ModelLock;
import org.kotemaru.android.fw.base.FwActivityModelBase;

public class BaseActivityModel extends FwActivityModelBase<CmDialogModel> {
	public BaseActivityModel(ModelLock parentLock) {
		super(parentLock);
	}
	@Override
	public CmDialogModel createDialogModel() {
		return new CmDialogModel(this);
	}
}
