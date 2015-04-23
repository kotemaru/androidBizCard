package org.kotemaru.android.bizcard.controller;

import org.kotemaru.android.bizcard.MyApplication;
import org.kotemaru.android.delegatehandler.rt.DelegateHandlerError;
import org.kotemaru.android.delegatehandler.rt.OnDelegateHandlerErrorListener;
import org.kotemaru.android.fw.FwControllerBase;
import org.kotemaru.android.fw.thread.ThreadManager;

public class BaseController extends FwControllerBase<MyApplication>
		implements OnDelegateHandlerErrorListener {

	protected BaseController(MyApplication app) {
		super(app);
	}

	@Override
	public void onDelegateHandlerError(final Throwable t, String methodName, Object... args) {
		getFwApplication().getThreadManager().post(ThreadManager.UI, new Runnable() {
			@Override
			public void run() {
				throw new DelegateHandlerError(t);
			}
		}, 0);
	}
}
