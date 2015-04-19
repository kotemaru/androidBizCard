package org.kotemaru.android.bizcard.activity;

import org.kotemaru.android.bizcard.Launcher;
import org.kotemaru.android.bizcard.Launcher.ExtraValue;
import org.kotemaru.android.fw.FwActivityBase;
import org.kotemaru.android.fw.FwActivityModelBase;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;

public abstract class BaseActivity<M extends FwActivityModelBase> extends FwActivityBase<M> {
	private static final String TAG = BaseActivity.class.getSimpleName();
	private boolean mIsFirstLaunch;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mIsFirstLaunch = true;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		onLaunch(intent);
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (mIsFirstLaunch) {
			onLaunch(getIntent());
			mIsFirstLaunch = false;
		}
	}

	abstract protected void onLaunch(Intent intent);

	public enum MenuItemType {
		CAMERA(android.R.drawable.ic_menu_camera),
		EDITOR(android.R.drawable.ic_menu_edit),
		REGISTER(android.R.drawable.ic_menu_add),
		SEARCH(android.R.drawable.ic_menu_search), ;

		public final int iconResId;

		MenuItemType(int iconResId) {
			this.iconResId = iconResId;
		}
		public static MenuItemType toMenuItemType(CharSequence name) {
			if (name == null) return null;
			try {
				return valueOf(name.toString());
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
	}

	public boolean createOptionsMenu(Menu menu, MenuItemType... types) {
		for (MenuItemType type : types) {
			MenuItem item = menu.add(type.name());
			item.setIcon(type.iconResId);
			item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
			if (type == MenuItemType.SEARCH) {
				SearchView mSearchView = new SearchView(this);
				item.setActionView(mSearchView);
			}
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		MenuItemType type = MenuItemType.toMenuItemType(item.getTitle());
		switch (type) {
		case CAMERA:
			Launcher.startCamera(this);
			return true;
		case EDITOR:
			Launcher.startEditor(this);
			return true;
		case REGISTER:
			Launcher.startEditor(this, ExtraValue.INIT, -1);
			return true;
		case SEARCH:
			// TODO:
			return true;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

}
