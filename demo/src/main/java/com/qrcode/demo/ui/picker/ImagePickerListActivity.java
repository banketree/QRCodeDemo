/*
 * Copyright (c) 2015 [1076559197@qq.com | tchen0707@gmail.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qrcode.demo.ui.picker;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.qrcode.demo.R;
import com.qrcode.demo.picker.data.ImageBucket;
import com.qrcode.demo.picker.data.ImagePickerHelper;
import com.qrcode.demo.ui.picker.adapter.ListViewDataAdapter;
import com.qrcode.demo.ui.picker.adapter.ViewHolderBase;
import com.qrcode.demo.ui.picker.adapter.ViewHolderCreator;
import com.qrcode.demo.utils.ImageLoaderHelper;
import com.thinkcore.activity.TAppActivity;
import com.thinkcore.utils.TActivityUtils;
import com.thinkcore.utils.TStringUtils;

import java.util.List;

public class ImagePickerListActivity extends TAppActivity implements
		OnClickListener {

	private static final int IMAGE_PICKER_DETAIL_REQUEST_CODE = 200;

	public static final String KEY_BUNDLE_ALBUM_PATH = "KEY_BUNDLE_ALBUM_PATH";
	public static final String KEY_BUNDLE_ALBUM_NAME = "KEY_BUNDLE_ALBUM_NAME";

	ListView mImagePickerListView;

	private ListViewDataAdapter<ImageBucket> mListViewAdapter = null;
	private AsyncTask<Void, Void, List<ImageBucket>> mAlbumLoadTask = null;
//	private TitleView mTitleBarView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_image_picker_list);
		initView();

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (null != mAlbumLoadTask && !mAlbumLoadTask.isCancelled()) {
			mAlbumLoadTask.cancel(true);
			mAlbumLoadTask = null;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode != RESULT_OK) {
			return;
		}

		if (requestCode == IMAGE_PICKER_DETAIL_REQUEST_CODE) {
			setResult(RESULT_OK, data);
			finish();
		}
	}

	@Override
	public void onClick(View v) {
//		if (v == mTitleBarView.getLeftImageView()) {
//			finish();
//		}
	}

	protected void initView() {
//		mTitleBarView = new TitleView(this);
//		mTitleBarView.getCenterView().setText("图片选择");
//		mTitleBarView.getLeftImageView().setBackgroundResource(
//				R.drawable.eotuui_btn_titlebar_back);
//		mTitleBarView.getLeftImageView().setOnClickListener(this);

		mImagePickerListView = (ListView) findViewById(R.id.ListView_image_picker_list_view);

		mListViewAdapter = new ListViewDataAdapter<ImageBucket>(
				new ViewHolderCreator<ImageBucket>() {

					@Override
					public ViewHolderBase<ImageBucket> createViewHolder(
							int position) {

						return new ViewHolderBase<ImageBucket>() {
							ImageView mItemImage;
							TextView mItemTitle;

							@Override
							public View createView(LayoutInflater layoutInflater) {
								View convertView = layoutInflater.inflate(
										R.layout.list_item_common_image_picker,
										null);
								mItemImage = (ImageView) convertView
										.findViewById(R.id.list_item_common_image_picker_thumbnail);

								mItemTitle = (TextView) convertView
										.findViewById(R.id.list_item_common_image_picker_title);

								return convertView;
							}

							@Override
							public void showData(int position,
									ImageBucket itemData) {
								if (null != itemData) {
									String imagePath = itemData.bucketList.get(
											0).getImagePath();
									if (!TStringUtils.isEmpty(imagePath)) {
										ImageLoader.getInstance().displayImage(
												"file://" + imagePath,
												mItemImage,
												ImageLoaderHelper.getInstance(
														mContext)
														.getDisplayOptions());
									}

									int count = itemData.count;
									String title = itemData.bucketName;

									if (!TStringUtils.isEmpty(title)) {
										mItemTitle.setText(title + "(" + count
												+ ")");
									}
								}
							}
						};
					}
				});
		mImagePickerListView.setAdapter(mListViewAdapter);

		mImagePickerListView
				.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						if (null != mListViewAdapter
								&& null != mListViewAdapter.getDataList()
								&& !mListViewAdapter.getDataList().isEmpty()
								&& position < mListViewAdapter.getDataList()
										.size()) {

							Bundle extras = new Bundle();
							extras.putParcelableArrayList(
									KEY_BUNDLE_ALBUM_PATH,
									mListViewAdapter.getDataList()
											.get(position).bucketList);
							extras.putString(KEY_BUNDLE_ALBUM_NAME,
									mListViewAdapter.getDataList()
											.get(position).bucketName);

							Intent datatIntent = new Intent(mContext,
									ImagePickerDetailActivity.class);
							datatIntent.putExtra("extras", extras);
							startActivityForResult(datatIntent,
									IMAGE_PICKER_DETAIL_REQUEST_CODE);
						}
					}
				});

		mAlbumLoadTask = new AsyncTask<Void, Void, List<ImageBucket>>() {

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				ImagePickerHelper.getHelper().init(mContext);
			}

			@Override
			protected List<ImageBucket> doInBackground(Void... params) {
				return ImagePickerHelper.getHelper().getImagesBucketList();
			}

			@Override
			protected void onPostExecute(List<ImageBucket> list) {
				mListViewAdapter.getDataList().addAll(list);
				mListViewAdapter.notifyDataSetChanged();
			}
		};

		mAlbumLoadTask.execute();
	}
}
