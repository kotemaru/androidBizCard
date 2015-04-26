package org.kotemaru.android.bizcard.controller;

import org.kotemaru.android.bizcard.MyApplication;
import org.kotemaru.android.fw.FwControllerBase;
import org.kotemaru.android.fw.thread.DelegateHandlerError;
import org.kotemaru.android.fw.thread.OnDelegateHandlerErrorListener;
import org.kotemaru.android.fw.thread.ThreadManager;

import android.util.Log;

public class BaseController
		extends FwControllerBase<MyApplication>
		implements OnDelegateHandlerErrorListener {
	public static final String TAG = BaseController.class.getSimpleName();

	protected BaseController(MyApplication app) {
		super(app);
	}

	@Override
	public void onDelegateHandlerError(final Throwable t, String methodName, Object... args) {
		Log.e(TAG, "onDelegateHandlerError:" + methodName + "(" + args + "):" + t);
		getFwApplication().getThreadManager().post(ThreadManager.UI, new Runnable() {
			@Override
			public void run() {
				throw new DelegateHandlerError(t);
			}
		}, 0);
	}
}
