package org.kotemaru.android.bizcard.activity;

import org.kotemaru.android.bizcard.Launcher;
import org.kotemaru.android.bizcard.Launcher.ExtraValue;
import org.kotemaru.android.bizcard.MyApplication;
import org.kotemaru.android.bizcard.R;
import org.kotemaru.android.fw.base.FwActivityBase;
import org.kotemaru.android.fw.base.FwActivityModelBase;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;

public abstract class BaseActivity<M extends FwActivityModelBase<?>>
		extends FwActivityBase<MyApplication, M>
{
	public static final String TAG = BaseActivity.class.getSimpleName();

	@Override
	public MyApplication getFwApplication() {
		return MyApplication.getInstance();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	@Override
	protected void onAfterCreate() {
		super.onAfterCreate();
		onLaunch(getIntent());
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		onLaunch(intent);
	}

	abstract protected void onLaunch(Intent intent);

	public enum MenuItemType {
		// @formatter:off
		CAMERA  (android.R.drawable.ic_menu_camera,	R.string.menu_camra),
		EDITOR  (android.R.drawable.ic_menu_edit,	R.string.menu_edit),
		REGISTER(android.R.drawable.ic_menu_add	,	R.string.menu_register),
		SEARCH  (android.R.drawable.ic_menu_search,	R.string.menu_search),
		// @formatter:on
		;
		static final MenuItemType[] VALUES = values();

		public final int iconResId;
		public final int labelResId;

		MenuItemType(int iconResId, int labelResId) {
			this.iconResId = iconResId;
			this.labelResId = labelResId;
		}
		public static MenuItemType toMenuItemType(int menuId) {
			for (MenuItemType type : VALUES) {
				if (type.labelResId == menuId) return type;
			}
			return null;
		}
	}

	public boolean createOptionsMenu(Menu menu, MenuItemType... types) {
		for (MenuItemType type : types) {
			MenuItem item = menu.add(0, type.labelResId, 0, type.labelResId);
			item.setIcon(type.iconResId);
			item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
			if (type == MenuItemType.SEARCH) {
				SearchView searchView = createSearchView();
				item.setActionView(searchView);
			}
		}
		return true;
	}
	private SearchView createSearchView() {
		final SearchView searchView = new SearchView(this);
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener(){
			@Override
			public boolean onQueryTextChange(String newText) {
				return BaseActivity.this.onQueryTextChange(newText);
			}
			@Override
			public boolean onQueryTextSubmit(String query) {
				return BaseActivity.this.onQueryTextSubmit(query);
			}
		});
		searchView.setOnCloseListener(new SearchView.OnCloseListener(){
			@Override
			public boolean onClose() {
				BaseActivity.this.onQueryTextSubmit(null);
				return false;
			}
		});
		return searchView;
	}

	protected boolean onQueryTextChange(String query) {
		return false;
	}
	protected boolean onQueryTextSubmit(String query) {
		return false;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		MenuItemType type = MenuItemType.toMenuItemType(item.getItemId());
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
