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

package com.qrcode.demo;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;

import com.nineoldandroids.view.ViewHelper;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.qrcode.demo.camera.CameraManager;
import com.qrcode.demo.decode.DecodeThread;
import com.qrcode.demo.decode.DecodeUtils;
import com.qrcode.demo.ui.picker.ImagePickerDetailActivity;
import com.qrcode.demo.ui.picker.ImagePickerListActivity;
import com.qrcode.demo.utils.BeepManager;
import com.qrcode.demo.utils.InactivityTimer;
import com.thinkcore.activity.TAppActivity;
import com.thinkcore.utils.TActivityUtils;
import com.thinkcore.utils.TStringUtils;

import java.io.IOException;

public class CaptureActivity extends TAppActivity implements
		SurfaceHolder.Callback {

	public static final String TAG_LOG = CaptureActivity.class.getSimpleName();
	public static final int IMAGE_PICKER_REQUEST_CODE = 100;

	SurfaceView mPreviewSurfaceView;
	ImageView mErrorMaskImageView;
	ImageView mScanMaskImageView;
	FrameLayout mCropViewFrameLayout;
	Button mPictureBtn;
	Button mLightBtn;
	RadioGroup mModeGroup;
	RelativeLayout mContainer;

	private CameraManager mCameraManager;
	private CaptureActivityHandler mHandler;

	private boolean hasSurface;
	private boolean isLightOn;

	private InactivityTimer mInactivityTimer;
	private BeepManager mBeepManager;

	private int mQrcodeCropWidth = 0;
	private int mQrcodeCropHeight = 0;
	private int mBarcodeCropWidth = 0;
	private int mBarcodeCropHeight = 0;

	private ObjectAnimator mScanMaskObjectAnimator = null;

	private Rect mCropRect;
	private int mDataMode = DecodeUtils.DECODE_DATA_MODE_QRCODE;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_capture);
		initViews();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// CameraManager must be initialized here, not in onCreate(). This is
		// necessary because we don't
		// want to open the camera driver and measure the screen size if we're
		// going to show the help on
		// first launch. That led to bugs where the scanning rectangle was the
		// wrong size and partially
		// off screen.
		mCameraManager = new CameraManager(getApplication());

		mHandler = null;

		if (hasSurface) {
			// The activity was paused but not stopped, so the surface still
			// exists. Therefore
			// surfaceCreated() won't be called, so init the camera here.
			initCamera(mPreviewSurfaceView.getHolder());
		} else {
			// Install the callback and wait for surfaceCreated() to init the
			// camera.
			mPreviewSurfaceView.getHolder().addCallback(this);
		}

		mInactivityTimer.onResume();
	}

	@Override
	protected void onPause() {
		if (mHandler != null) {
			mHandler.quitSynchronously();
			mHandler = null;
		}

		mBeepManager.close();
		mInactivityTimer.onPause();
		mCameraManager.closeDriver();

		if (!hasSurface) {
			mPreviewSurfaceView.getHolder().removeCallback(this);
		}

		if (null != mScanMaskObjectAnimator
				&& mScanMaskObjectAnimator.isStarted()) {
			mScanMaskObjectAnimator.cancel();
		}
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		mInactivityTimer.shutdown();
		super.onDestroy();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (holder == null) {
			Log.e(TAG_LOG,
					"*** WARNING *** surfaceCreated() gave us a null surface!");
		}
		if (!hasSurface) {
			hasSurface = true;
			// initCamera(holder);
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		initCamera(holder);
	}

	private void initViews() {
		mPreviewSurfaceView = (SurfaceView) findViewById(R.id.SurfaceView_preview);
		mErrorMaskImageView = (ImageView) findViewById(R.id.ImageView_error_mask);
		mScanMaskImageView = (ImageView) findViewById(R.id.ImageView_scan_mask);
		mCropViewFrameLayout = (FrameLayout) findViewById(R.id.FrameLayout_crop_view);
		mPictureBtn = (Button) findViewById(R.id.Button_picture_btn);
		mLightBtn = (Button) findViewById(R.id.Button_light_btn);
		mModeGroup = (RadioGroup) findViewById(R.id.RadioGroup_mode_group);
		mContainer = (RelativeLayout) findViewById(R.id.capture_container);

		hasSurface = false;
		mInactivityTimer = new InactivityTimer(this);
		mBeepManager = new BeepManager(this);

		initCropViewAnimator();

		mPictureBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TActivityUtils.jumpToActivityForResult(mContext,
						ImagePickerListActivity.class,
						IMAGE_PICKER_REQUEST_CODE);
			}
		});

		mLightBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isLightOn) {
					mCameraManager.setTorch(false);
					mLightBtn.setSelected(false);
				} else {
					mCameraManager.setTorch(true);
					mLightBtn.setSelected(true);
				}
				isLightOn = !isLightOn;
			}
		});

		mModeGroup
				.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						if (checkedId == R.id.RadioButton_mode_barcode) {
							PropertyValuesHolder qr2barWidthVH = PropertyValuesHolder
									.ofFloat("width", 1.0f,
											(float) mBarcodeCropWidth
													/ mQrcodeCropWidth);
							PropertyValuesHolder qr2barHeightVH = PropertyValuesHolder
									.ofFloat("height", 1.0f,
											(float) mBarcodeCropHeight
													/ mQrcodeCropHeight);
							ValueAnimator valueAnimator = ValueAnimator
									.ofPropertyValuesHolder(qr2barWidthVH,
											qr2barHeightVH);
							valueAnimator
									.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
										@Override
										public void onAnimationUpdate(
												ValueAnimator animation) {
											Float fractionW = (Float) animation
													.getAnimatedValue("width");
											Float fractionH = (Float) animation
													.getAnimatedValue("height");

											RelativeLayout.LayoutParams parentLayoutParams = (RelativeLayout.LayoutParams) mCropViewFrameLayout
													.getLayoutParams();
											parentLayoutParams.width = (int) (mQrcodeCropWidth * fractionW);
											parentLayoutParams.height = (int) (mQrcodeCropHeight * fractionH);
											mCropViewFrameLayout
													.setLayoutParams(parentLayoutParams);
										}
									});
							valueAnimator
									.addListener(new Animator.AnimatorListener() {
										@Override
										public void onAnimationStart(
												Animator animation) {

										}

										@Override
										public void onAnimationEnd(
												Animator animation) {
											initCrop();
											setDataMode(DecodeUtils.DECODE_DATA_MODE_BARCODE);
										}

										@Override
										public void onAnimationCancel(
												Animator animation) {

										}

										@Override
										public void onAnimationRepeat(
												Animator animation) {

										}
									});
							valueAnimator.start();

						} else if (checkedId == R.id.RadioButton_mode_qrcode) {
							PropertyValuesHolder bar2qrWidthVH = PropertyValuesHolder
									.ofFloat("width", 1.0f,
											(float) mQrcodeCropWidth
													/ mBarcodeCropWidth);
							PropertyValuesHolder bar2qrHeightVH = PropertyValuesHolder
									.ofFloat("height", 1.0f,
											(float) mQrcodeCropHeight
													/ mBarcodeCropHeight);
							ValueAnimator valueAnimator = ValueAnimator
									.ofPropertyValuesHolder(bar2qrWidthVH,
											bar2qrHeightVH);
							valueAnimator
									.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
										@Override
										public void onAnimationUpdate(
												ValueAnimator animation) {
											Float fractionW = (Float) animation
													.getAnimatedValue("width");
											Float fractionH = (Float) animation
													.getAnimatedValue("height");

											RelativeLayout.LayoutParams parentLayoutParams = (RelativeLayout.LayoutParams) mCropViewFrameLayout
													.getLayoutParams();
											parentLayoutParams.width = (int) (mBarcodeCropWidth * fractionW);
											parentLayoutParams.height = (int) (mBarcodeCropHeight * fractionH);
											mCropViewFrameLayout
													.setLayoutParams(parentLayoutParams);
										}
									});
							valueAnimator
									.addListener(new Animator.AnimatorListener() {
										@Override
										public void onAnimationStart(
												Animator animation) {

										}

										@Override
										public void onAnimationEnd(
												Animator animation) {
											initCrop();
											setDataMode(DecodeUtils.DECODE_DATA_MODE_QRCODE);
										}

										@Override
										public void onAnimationCancel(
												Animator animation) {

										}

										@Override
										public void onAnimationRepeat(
												Animator animation) {

										}
									});
							valueAnimator.start();
						}
					}
				});
	}

	private void initCropViewAnimator() {
		mQrcodeCropWidth = getResources().getDimensionPixelSize(
				R.dimen.qrcode_crop_width);
		mQrcodeCropHeight = getResources().getDimensionPixelSize(
				R.dimen.qrcode_crop_height);

		mBarcodeCropWidth = getResources().getDimensionPixelSize(
				R.dimen.barcode_crop_width);
		mBarcodeCropHeight = getResources().getDimensionPixelSize(
				R.dimen.barcode_crop_height);
	}

	public CameraManager getCameraManager() {
		return mCameraManager;
	}

	public void initCrop() {
		int cameraWidth = mCameraManager.getCameraResolution().y;
		int cameraHeight = mCameraManager.getCameraResolution().x;

		int[] location = new int[2];
		mCropViewFrameLayout.getLocationInWindow(location);

		int cropLeft = location[0];
		int cropTop = location[1];

		int cropWidth = mCropViewFrameLayout.getWidth();
		int cropHeight = mCropViewFrameLayout.getHeight();

		int containerWidth = mContainer.getWidth();
		int containerHeight = mContainer.getHeight();

		int x = cropLeft * cameraWidth / containerWidth;
		int y = cropTop * cameraHeight / containerHeight;

		int width = cropWidth * cameraWidth / containerWidth;
		int height = cropHeight * cameraHeight / containerHeight;

		setCropRect(new Rect(x, y, width + x, height + y));
	}

	public Handler getHandler() {
		return mHandler;
	}

	/**
	 * A valid barcode has been found, so give an indication of success and show
	 * the results.
	 */
	public void handleDecode(String result, Bundle bundle) {
		mInactivityTimer.onActivity();
		mBeepManager.playBeepSoundAndVibrate();

		// if (!CommonUtils.isEmpty(result) && CommonUtils.isUrl(result)) {
		// Intent intent = new Intent(Intent.ACTION_VIEW);
		// intent.setData(Uri.parse(result));
		// startActivity(intent);
		// } else {
		// bundle.putString(ResultActivity.BUNDLE_KEY_SCAN_RESULT, result);
		// readyGo(ResultActivity.class, bundle);
		// }
	}

	private void onCameraPreviewSuccess() {
		initCrop();
		mErrorMaskImageView.setVisibility(View.GONE);

		ViewHelper.setPivotX(mScanMaskImageView, 0.0f);
		ViewHelper.setPivotY(mScanMaskImageView, 0.0f);

		mScanMaskObjectAnimator = ObjectAnimator.ofFloat(mScanMaskImageView,
				"scaleY", 0.0f, 1.0f);
		mScanMaskObjectAnimator.setDuration(2000);
		mScanMaskObjectAnimator.setInterpolator(new DecelerateInterpolator());
		mScanMaskObjectAnimator.setRepeatCount(-1);
		mScanMaskObjectAnimator.setRepeatMode(ObjectAnimator.RESTART);
		mScanMaskObjectAnimator.start();
	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		if (surfaceHolder == null) {
			throw new IllegalStateException("No SurfaceHolder provided");
		}
		if (mCameraManager.isOpen()) {
			Log.w(TAG_LOG,
					"initCamera() while already open -- late SurfaceView callback?");
			return;
		}
		try {
			mCameraManager.openDriver(surfaceHolder);
			// Creating the handler starts the preview, which can also throw a
			// RuntimeException.
			if (mHandler == null) {
				mHandler = new CaptureActivityHandler(this, mCameraManager);
			}

			onCameraPreviewSuccess();
		} catch (IOException ioe) {
			Log.w(TAG_LOG, ioe);
			makeText(getResString(R.string.open_camera_error));
			finish();
		} catch (RuntimeException e) {
			// Barcode Scanner has seen crashes in the wild of this variety:
			// java.?lang.?RuntimeException: Fail to connect to camera service
			Log.w(TAG_LOG, "Unexpected error initializing camera", e);
			makeText(getResString(R.string.open_camera_error));
			finish();
		}

	}

	public Rect getCropRect() {
		return mCropRect;
	}

	public void setCropRect(Rect cropRect) {
		this.mCropRect = cropRect;
	}

	public int getDataMode() {
		return mDataMode;
	}

	public void setDataMode(int dataMode) {
		this.mDataMode = dataMode;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode != RESULT_OK) {
			return;
		}

		if (requestCode == IMAGE_PICKER_REQUEST_CODE) {
			String imagePath = data
					.getStringExtra(ImagePickerDetailActivity.KEY_BUNDLE_RESULT_IMAGE_PATH);

			if (!TStringUtils.isEmpty(imagePath)) {
				ImageLoader.getInstance().loadImage("file://" + imagePath,
						new ImageLoadingListener() {
							@Override
							public void onLoadingStarted(String imageUri,
									View view) {
							}

							@Override
							public void onLoadingFailed(String imageUri,
									View view, FailReason failReason) {

							}

							@Override
							public void onLoadingComplete(String imageUri,
									View view, Bitmap loadedImage) {
								String resultZxing = new DecodeUtils(
										DecodeUtils.DECODE_DATA_MODE_ALL)
										.decodeWithZxing(loadedImage);
								String resultZbar = new DecodeUtils(
										DecodeUtils.DECODE_DATA_MODE_ALL)
										.decodeWithZbar(loadedImage);

								if (!TStringUtils.isEmpty(resultZbar)) {
									Bundle extras = new Bundle();
									extras.putInt(DecodeThread.DECODE_MODE,
											DecodeUtils.DECODE_MODE_ZBAR);

									handleDecode(resultZbar, extras);
								} else if (!TStringUtils.isEmpty(resultZxing)) {
									Bundle extras = new Bundle();
									extras.putInt(DecodeThread.DECODE_MODE,
											DecodeUtils.DECODE_MODE_ZXING);

									handleDecode(resultZxing, extras);
								} else {
									handleDecode("", null);
								}
							}

							@Override
							public void onLoadingCancelled(String imageUri,
									View view) {

							}
						});
			}
		}
	}
}
