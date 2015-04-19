package org.kotemaru.android.bizcard.activity;

import java.util.List;

import org.kotemaru.android.bizcard.Launcher;
import org.kotemaru.android.bizcard.MyApplication;
import org.kotemaru.android.bizcard.R;
import org.kotemaru.android.bizcard.controller.CardListController;
import org.kotemaru.android.bizcard.model.CardListActivityModel;
import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.bizcard.model.CardModel.Kind;
import org.kotemaru.android.fw.FwActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class CardListActivity extends BaseActivity<CardListActivityModel> implements FwActivity {

	private CardListActivityModel mModel;
	private CardListController mController;
	private ListView mListView;
	private CardListAdapter mCardListAdapter;

	@Override
	public CardListActivityModel getActivityModel() {
		return mModel;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		MyApplication app = MyApplication.getInstance();
		mModel = app.getModel().getCardListModel();
		mController = app.getController().getCardListController();

		mCardListAdapter = new CardListAdapter();
		mListView = (ListView) findViewById(R.id.main_list);
		mListView.setAdapter(mCardListAdapter);
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Launcher.startViewer(CardListActivity.this, (int) id);
			}
		});

		mController.mHandler.loadCardList();
	}
	@Override
	protected void onLaunch(Intent intent) {
		// nop.
	}

	@Override
	public void onUpdateInReadLocked(CardListActivityModel model) {
		if (mModel.getCardModelList() != mCardListAdapter.getCardModelList()) {
			mCardListAdapter.setCardModelList(mModel.getCardModelList());
		}
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return createOptionsMenu(menu, MenuItemType.SEARCH, MenuItemType.REGISTER);
	}

	public static class CardListAdapter extends BaseAdapter {
		private LayoutInflater mInflater = LayoutInflater.from(MyApplication.getInstance());
		private List<CardModel> mCardModelList = null;

		public void setCardModelList(List<CardModel> list) {
			mCardModelList = list;
			notifyDataSetChanged();
		}

		public List<CardModel> getCardModelList() {
			return mCardModelList;
		}

		@Override
		public int getCount() {
			if (mCardModelList == null) return 0;
			return mCardModelList.size();
		}

		@Override
		public Object getItem(int position) {
			return mCardModelList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return mCardModelList.get(position).getId();
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			if (view == null) {
				view = mInflater.inflate(R.layout.listitem_card, null);
				view.setTag(new ViewHolder(view));
			}

			CardModel model = mCardModelList.get(position);
			ViewHolder holder = (ViewHolder) view.getTag();
			holder.mCompany.setText(model.get(Kind.COMPANY));
			holder.mPosition.setText(model.get(Kind.POSITION));
			holder.mName.setText(model.get(Kind.NAME));
			String accessDate = model.get(Kind.ACCESS_DATE);
			if (accessDate != null) {
				holder.mOption.setText(accessDate.substring(0, 10) + "に連絡");
			} else {
				holder.mOption.setText(null);
			}
			return view;
		}

		private static class ViewHolder {
			ImageView mThumbnail;
			TextView mCompany;
			TextView mPosition;
			TextView mName;
			TextView mOption;

			ViewHolder(View parent) {
				mThumbnail = (ImageView) parent.findViewById(R.id.thumbnail);
				mCompany = (TextView) parent.findViewById(R.id.company);
				mPosition = (TextView) parent.findViewById(R.id.position);
				mName = (TextView) parent.findViewById(R.id.name);
				mOption = (TextView) parent.findViewById(R.id.option);
			}
		}

	}

}
