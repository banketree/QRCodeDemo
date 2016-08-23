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

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.qrcode.demo.R;
import com.qrcode.demo.picker.data.ImageItem;
import com.qrcode.demo.ui.picker.adapter.ListViewDataAdapter;
import com.qrcode.demo.ui.picker.adapter.ViewHolderBase;
import com.qrcode.demo.ui.picker.adapter.ViewHolderCreator;
import com.qrcode.demo.utils.ImageLoaderHelper;
import com.thinkcore.activity.TAppActivity;
import com.thinkcore.utils.TStringUtils;

import java.util.List;

public class ImagePickerDetailActivity extends TAppActivity implements
		OnClickListener {

	public static final String KEY_BUNDLE_RESULT_IMAGE_PATH = "KEY_BUNDLE_RESULT_IMAGE_PATH";

	GridView commonImagePickerDetailGridView;

	private ListViewDataAdapter<ImageItem> mGridViewAdapter = null;
	private List<ImageItem> mGridListData = null;
//	private TitleView mTitleBarView;
	private String mName = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_image_picker_detail);

		Bundle extras = getIntent().getBundleExtra("extras");
		if (extras != null) {
			mGridListData = extras
					.getParcelableArrayList(ImagePickerListActivity.KEY_BUNDLE_ALBUM_PATH);

			mName = extras
					.getString(ImagePickerListActivity.KEY_BUNDLE_ALBUM_NAME);
		}

		if (mGridListData == null) {
			finish();
			makeText("数据异常");
			return;
		}

		initView();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
//		if (v == mTitleBarView.getLeftImageView()) {
//			finish();
//		}
	}

	private void initView() {
//		mTitleBarView = new TitleView(this);
//
//		if (!TStringUtils.isEmpty(mName)) {
//			mTitleBarView.getCenterView().setText(mName);
//		}
//
//		mTitleBarView.getLeftImageView().setBackgroundResource(
//				R.drawable.eotuui_btn_titlebar_back);
//		mTitleBarView.getLeftImageView().setOnClickListener(this);

		commonImagePickerDetailGridView = (GridView) findViewById(R.id.GridView_image_picker_detail);

		mGridViewAdapter = new ListViewDataAdapter<>(
				new ViewHolderCreator<ImageItem>() {
					@Override
					public ViewHolderBase<ImageItem> createViewHolder(
							int position) {
						return new ViewHolderBase<ImageItem>() {

							ImageView mItemImage;

							@Override
							public View createView(LayoutInflater layoutInflater) {
								View convertView = layoutInflater.inflate(
										R.layout.grid_item_common_image_picker,
										null);
								mItemImage = (ImageView) convertView
										.findViewById(R.id.grid_item_common_image_picker_image);
								return convertView;
							}

							@Override
							public void showData(int position,
									ImageItem itemData) {
								if (null != itemData) {
									String imagePath = itemData.getImagePath();
									if (!TStringUtils.isEmpty(imagePath)) {
										ImageLoader.getInstance().displayImage(
												"file://" + imagePath,
												mItemImage,
												ImageLoaderHelper.getInstance(
														mContext)
														.getDisplayOptions());
									}
								}
							}
						};
					}
				});
		mGridViewAdapter.getDataList().addAll(mGridListData);
		commonImagePickerDetailGridView.setAdapter(mGridViewAdapter);

		commonImagePickerDetailGridView
				.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						if (null != mGridViewAdapter
								&& null != mGridViewAdapter.getDataList()
								&& !mGridViewAdapter.getDataList().isEmpty()
								&& position < mGridViewAdapter.getDataList()
										.size()) {

							Intent intent = new Intent();
							intent.putExtra(KEY_BUNDLE_RESULT_IMAGE_PATH,
									mGridViewAdapter.getDataList()
											.get(position).getImagePath());

							setResult(RESULT_OK, intent);
							finish();
						}
					}
				});
	}

}
