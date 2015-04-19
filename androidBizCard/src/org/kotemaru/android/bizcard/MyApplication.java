package org.kotemaru.android.bizcard;

import org.kotemaru.android.bizcard.database.CardDb;
import org.kotemaru.android.fw.FwActivity;
import org.kotemaru.android.fw.FwApplicationBase;
import org.kotemaru.android.fw.thread.DefaultThreadManager;
import org.kotemaru.android.fw.thread.ThreadManager;

public class MyApplication extends FwApplicationBase<RootModel, FwActivity, RootController> {
	private static MyApplication sInstance;

	private CardDb mCardDb;

	@Override
	public void onCreate() {
		super.onCreate();
		sInstance = this;
		mCardDb = new CardDb(this);
	}

	public static MyApplication getInstance() {
		return sInstance;
	}

	@Override
	public ThreadManager createThreadManager() {
		ThreadManager tm = DefaultThreadManager.getInstance();
		tm.registerThread(ThreadManager.WORKER);
		tm.registerThread(ThreadManager.NETWORK, 2, Thread.MIN_PRIORITY);
		return tm;
	}

	@Override
	public RootModel createModel() {
		return new RootModel();
	}

	@Override
	public RootController createController() {
		return new RootController(this);
	}

	public CardDb getCardDb() {
		return mCardDb;
	}
}
