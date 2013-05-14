package com.promomark.cipclient;

import java.io.File;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.Rotation;
import com.metaio.sdk.jni.TrackingValues;
import com.metaio.sdk.jni.TrackingValuesVector;
import com.metaio.sdk.jni.Vector3d;
import com.promomark.cipclient.DataObjects.ARItem;

public class MainActivity extends MetaioSDKViewActivity implements
		ButtonBar.OnSelectionChanged, OnClickListener {

	private IGeometry mModel;
	private ButtonBar buttonBar;
	private ImageButton info;
	private View arInfoView;
	private View drinksView;
	private View couponView;
	private View contestView;
	private LayoutInflater inflater;
	private FrameLayout main;
	private ImageButton enter1;
	private ImageButton enter2;

	@Override
	protected int getGUILayout() {
		return R.layout.activity_main;
	}

	@Override
	protected void onResume() {
		super.onResume();
		buttonBar = new ButtonBar((ViewGroup) findViewById(R.id.buttonbar),
				this);
		CIPClientApp.instance().setCurrentActivity(this);
		info = (ImageButton) findViewById(R.id.info);
		main = (FrameLayout) findViewById(R.id.main);
		info.setOnClickListener(this);

		inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		initArInfoView(inflater.inflate(R.layout.view_arinfo, null));

		drinksView = inflater.inflate(R.layout.view_drinks, null);

		initCouponView(inflater.inflate(R.layout.view_coupon, null));

		initContestView(inflater.inflate(R.layout.view_contest, null));

		selected(Downloader.CATEGORY_AR);
	}

	private void initArInfoView(View view) {
		this.arInfoView = view;
		DataObjects data = CIPClientApp.instance().getDataObjects();

		TextView info = (TextView) arInfoView.findViewById(R.id.info);
		info.setText(data.arItem.targetText);

		String imgPath = getExternalFilesDir(null) + File.separator
				+ data.arItem.displayTargetImage;
		ImageView image = (ImageView) arInfoView.findViewById(R.id.image);
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap bitmap = BitmapFactory.decodeFile(imgPath, options);
		image.setImageBitmap(bitmap);
	}

	private void initCouponView(View view) {
		this.couponView = view;
		DataObjects data = CIPClientApp.instance().getDataObjects();

		TextView info = (TextView) couponView.findViewById(R.id.info);
		String msg = "Closest location: " + data.appItem.closestLocation + "("
				+ (int) (data.appItem.distToLocationInMeters / 1609.34)
				+ " mi)";
		info.setText(msg);

		String imgPath = getExternalFilesDir(null) + File.separator
				+ data.couponItem.image;
		ImageView image = (ImageView) couponView.findViewById(R.id.image);
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap bitmap = BitmapFactory.decodeFile(imgPath, options);
		image.setImageBitmap(bitmap);
	}

	private void initContestView(View view) {
		this.contestView = view;
		DataObjects data = CIPClientApp.instance().getDataObjects();

		TextView info = (TextView) contestView.findViewById(R.id.text);
		info.setText(data.appItem.contestText);

		enter1 = (ImageButton) contestView.findViewById(R.id.enter1);
		enter2 = (ImageButton) contestView.findViewById(R.id.enter2);
		enter1.setOnClickListener(this);
		enter2.setOnClickListener(this);
	}

	@Override
	protected void loadContent() {
		try {
			ARItem item = CIPClientApp.instance().getDataObjects().arItem;
			String base = getExternalFilesDir(null) + File.separator;

			// Assigning tracking configuration
			boolean result = metaioSDK.setTrackingConfiguration(base
					+ item.triggeredContentConfig);
			MetaioDebug.log("Tracking data loaded: " + result);

			// Loading 3D geometry
			mModel = metaioSDK.createGeometry(base + item.triggeredContent);
			if (mModel != null) {
				// Set geometry properties
				// mModel.setScale(new Vector3d(64.0f, 64.0f, 64.0f));
				mModel.setRotation(new Rotation(new Vector3d(0.3f, 0, 0)));
			} else
				MetaioDebug.log(Log.ERROR, "Error loading geometry: " + base
						+ item.triggeredContent);

		} catch (Exception e) {

		}
	}

	@Override
	protected void onGeometryTouched(IGeometry geometry) {
		CIPClientApp.instance().getEventReporter()
				.reportEvent(EventReporter.AR_ANIMATION_ENDED, (String) null);
		selected(Downloader.CATEGORY_CONTEST);
	}

	@Override
	protected IMetaioSDKCallback getMetaioSDKCallbackHandler() {
		return new IMetaioSDKCallback() {
			@Override
			public void onTrackingEvent(TrackingValuesVector trackingValues) {
				try {
					for (int i = 0; i < trackingValues.size(); i++) {
						final TrackingValues v = trackingValues.get(i);

						if (v.getCoordinateSystemID() == 1) {
							mModel.startAnimation(mModel.getAnimationNames()
									.get(0), true);
						}
					}
				} catch (Exception e) {
					// ignore
				}
			}
		};
	}

	@Override
	public void selected(int index) {
		switch (index) {
		case Downloader.CATEGORY_AR:
			main.removeAllViews();
			main.addView(arInfoView);
			break;

		case Downloader.CATEGORY_DRINKS:
			main.removeAllViews();
			main.addView(drinksView);
			break;

		case Downloader.CATEGORY_COUPONS:
			main.removeAllViews();
			main.addView(couponView);
			CIPClientApp
					.instance()
					.getEventReporter()
					.reportEvent(
							EventReporter.COUPON_DOWNLOADED,
							CIPClientApp.instance().getDataObjects().couponItem.image);
			break;

		case Downloader.CATEGORY_CONTEST:
			main.removeAllViews();
			main.addView(contestView);
			break;
		}
	}

	public void setEnabled(int category) {
		buttonBar.setEnabled(category, true);
	}

	@Override
	public void onClick(View view) {
		if (view == info) {
			CIPClientApp
					.instance()
					.displayInfo(
							"About",
							"This Paradise road-trip powered by Promomark. Version 1.0.1\n\n"
									+ "Drink Responsibly.\nDrive Responsibly.\n©2013 Cheeseburger in Paradise.");
		} else if (view == enter1 || view == enter2) {
			CIPClientApp
					.instance()
					.displayOkCancel(
							"Redirect",
							"You will now be redirected to Cheeseburger in Paradise contest page",
							new android.content.DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									CIPClientApp
											.instance()
											.getEventReporter()
											.reportEvent(
													EventReporter.CONTEST_REDIRECT,
													(String) null);
									String url = CIPClientApp.instance()
											.getDataObjects().appItem.contestMobileUrl;

									startActivity(new Intent(
											Intent.ACTION_VIEW, Uri.parse(url)));
								}
							});
		}
	}
}
