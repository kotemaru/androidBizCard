package org.kotemaru.android.bizcard.activity;

import java.io.File;
import java.util.List;

import org.kotemaru.android.bizcard.Launcher;
import org.kotemaru.android.bizcard.MyApplication;
import org.kotemaru.android.bizcard.R;
import org.kotemaru.android.bizcard.controller.CardListController;
import org.kotemaru.android.bizcard.model.CardListActivityModel;
import org.kotemaru.android.bizcard.model.CardModel;
import org.kotemaru.android.bizcard.model.CardModel.Kind;
import org.kotemaru.android.bizcard.util.CardImageUtil;
import org.kotemaru.android.fw.FwActivity;
import org.kotemaru.android.fw.dialog.ConfirmDialogListener;
import org.kotemaru.android.fw.util.image.DefaultImageLoaderProducer;
import org.kotemaru.android.fw.util.image.ImageLoader;
import org.kotemaru.android.fw.util.image.ImageLoaderProducer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
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
	private ImageLoaderProducer mImageLoaderProducer;
	private ImageLoader mImageLoader;
	private GestureDetector mCardGestureDetector;
	private CardGestureListener mCardGestureListener;

	@Override
	public CardListActivityModel getActivityModel() {
		return mModel;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setTitle(R.string.title_card_list);
		MyApplication app = MyApplication.getInstance();
		mModel = app.getModel().getCardListModel();
		mController = app.getController().getCardListController();

		mCardListAdapter = new CardListAdapter();
		mCardGestureListener = new CardGestureListener();
		mCardGestureDetector = new GestureDetector(this, mCardGestureListener);
		mListView = (ListView) findViewById(R.id.main_list);
		mListView.setAdapter(mCardListAdapter);
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Launcher.startViewer(CardListActivity.this, (int) id);
			}
		});
		mListView.setOnTouchListener(new OnTouchListener() {
			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return mCardGestureDetector.onTouchEvent(event);
			}
		});

		mImageLoaderProducer = new CardImageLoaderProducer(this);
		mImageLoader = new ImageLoader(mImageLoaderProducer);

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
	@Override
	protected boolean onQueryTextSubmit(String query) {
		mModel.setQueryText(query);
		mController.mHandler.loadCardList();
		return true;
	}


	@Override
	public void onPause() {
		mImageLoader.clear();
		super.onPause();
	}

	public class CardImageLoaderProducer extends DefaultImageLoaderProducer {
		public CardImageLoaderProducer(Context context) {
			super(context);
			setCacheDir(new File(getFilesDir() + "/thumbnail"));
			// setImageSize(new Point(100,100));
		}

		@Override
		public File getCacheFile(CharSequence imageId) {
			String str = "0000000000" + imageId;
			str = str.substring(str.length() - 10);
			File file = new File(mCacheDir, str + ".png");
			Log.e("DEBUG", "===>getCacheFile:" + file + ":" + CardImageUtil.getThumbnailFile(mContext, 1));
			return file;
		}
	}

	public class CardListAdapter extends BaseAdapter {
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

		@SuppressLint("InflateParams")
		@Override
		public View getView(int position, View view, ViewGroup parent) {
			if (view == null) {
				view = mInflater.inflate(R.layout.listitem_card, null);
				view.setTag(new ViewHolder(view));
				view.setOnTouchListener(new OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						Log.e("DEBUG", "###onTOuce:listitem");
						ViewHolder holder = (ViewHolder) v.getTag();
						mCardGestureListener.setCardModel(holder.mCardModel);
						return false;
					}
				});
			}

			CardModel model = mCardModelList.get(position);
			ViewHolder holder = (ViewHolder) view.getTag();
			holder.mCardModel = model;
			holder.mCompany.setText(model.get(Kind.COMPANY));
			holder.mPosition.setText(model.get(Kind.POSITION));
			holder.mName.setText(model.get(Kind.NAME));
			String accessDate = model.get(Kind.ACCESS_DATE);
			if (accessDate != null) {
				holder.mOption.setText("最終連絡日:" + accessDate.substring(0, 10));
			} else {
				holder.mOption.setText(null);
			}

			mImageLoader.setImage(holder.mThumbnail, Integer.toString(model.getId()));
			return view;
		}

		private class ViewHolder {
			CardModel mCardModel;
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

	private class CardGestureListener extends SimpleOnGestureListener {
		private CardModel mCardModel;

		public void setCardModel(CardModel cardModel) {
			mCardModel = cardModel;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			Log.e("DEBUG", "###onFling:listitem:" + mCardModel.getId()+":"+velocityX+velocityY);
			if (Math.abs(velocityX) < 1000 || Math.abs(velocityX)<=Math.abs(velocityY)) return false;
			mModel.getDialogModel().setConfirm("削除", mCardModel.get(Kind.NAME) + "さんの名刺を削除します。",
					new ConfirmDialogListener() {
						@Override
						public void onDialogOkay(Activity activity) {
							getFwApplication().getCardDb().removeCardModel(mCardModel.getId());
							mController.mHandler.loadCardList();
						}
						@Override
						public void onDialogCancel(Activity activity) {
							// nop.
						}
					});
			return true;
		}

	}

}
