package com.promomark.cipclient;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.Rotation;
import com.metaio.sdk.jni.TrackingValues;
import com.metaio.sdk.jni.TrackingValuesVector;
import com.metaio.sdk.jni.Vector3d;
import com.promomark.cipclient.DataObjects.ARItem;
import com.promomark.cipclient.DataObjects.AppItem;
import com.promomark.cipclient.DataObjects.DrinkItem;

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
	private ImageButton arImage;
	private boolean arActive = false;
	private ImageButton couponImage;
	private static final int AR_VIEW = -1;

	@Override
	protected int getGUILayout() {
		return R.layout.activity_main;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();

		initControls(0, false);
		
		CIPClientApp.instance().setCurrentActivity(this);
		
		selected(Downloader.CATEGORY_AR);
	}

	private void initControls(int selected, boolean enabled) {
		buttonBar = new ButtonBar((ViewGroup) findViewById(R.id.buttonbar),
				this, selected, enabled);
		info = (ImageButton) findViewById(R.id.info);
		main = (FrameLayout) findViewById(R.id.main);
		info.setOnClickListener(this);

		inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		initArInfoView(inflater.inflate(R.layout.view_arinfo, null));
		initDrinksView(inflater.inflate(R.layout.view_drinks, null));
		initCouponView(inflater.inflate(R.layout.view_coupon, null));
		initContestView(inflater.inflate(R.layout.view_contest, null));
	}

	private void initArInfoView(View view) {
		this.arInfoView = view;

		Drawable backgroundimage = view.getBackground();
		backgroundimage.setAlpha(127);

		DataObjects data = CIPClientApp.instance().getDataObjects();

		TextView info = (TextView) arInfoView.findViewById(R.id.info);
		info.setText(data.arItem.targetText);

		arImage = (ImageButton) arInfoView.findViewById(R.id.image);
		arImage.setImageBitmap(getCachedBitmap(data.arItem.displayTargetImage, true));
		arImage.setOnClickListener(this);
	}

	private Bitmap getCachedBitmap(String filename, boolean scale) {
		String imgPath = getExternalFilesDir(null) + File.separator + filename;
		BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap res = BitmapFactory.decodeFile(imgPath, options);	    
	    
    	float density = getResources().getDisplayMetrics().density;
    	boolean isCoupon = filename.contains("coupon");
    	float sourceDensityX = isCoupon? 1.7f: 2.0f;
    	float sourceDensityY = isCoupon? 1.9f: 2.0f;
	    if (scale && density != sourceDensityX) {
	    	//2.0 is the density of our stock images
	    	int imageWidth = (int) (res.getWidth() * density / sourceDensityX);
	    	int imageHeight = (int) (res.getHeight() * density / sourceDensityY);
	    	res = Bitmap.createScaledBitmap(res, imageWidth, imageHeight, false);
	    }
	    
	    return res;
	}

	private void initDrinksView(View view) {
		this.drinksView = view;
		ListView list = (ListView) view.findViewById(R.id.list);
		list.setAdapter(new DrinkAdapter(this, CIPClientApp.instance()
				.getDataObjects().drinkItems));

		Drawable backgroundimage = view.getBackground();
		backgroundimage.setAlpha(127);
	}

	private void initCouponView(View view) {
		this.couponView = view;
		DataObjects data = CIPClientApp.instance().getDataObjects();

		couponImage = (ImageButton) couponView.findViewById(R.id.image);
		couponImage.setImageBitmap(getCachedBitmap(data.couponItem.image, true));
		couponImage.setOnClickListener(this);
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
							CIPClientApp
									.instance()
									.getEventReporter()
									.reportEvent(
											EventReporter.AR_ANIMATION_STARTED,
											(String) null);
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
		if (arActive && index != AR_VIEW) {
			doPause();
			doStop();
			createViews();
			initControls(index, true);
			arActive = false;
		}
		main.removeAllViews();

		switch (index) {
		case Downloader.CATEGORY_AR:
			main.addView(arInfoView);
			break;

		case Downloader.CATEGORY_DRINKS:
			main.addView(drinksView);
			break;

		case Downloader.CATEGORY_COUPONS:
			main.addView(couponView);
			CIPClientApp
					.instance()
					.getEventReporter()
					.reportEvent(
							EventReporter.COUPON_DOWNLOADED,
							CIPClientApp.instance().getDataObjects().couponItem.image);
			break;

		case Downloader.CATEGORY_CONTEST:
			main.addView(contestView);
			break;

		case AR_VIEW:
			arActive = true;
			CIPClientApp
					.instance()
					.getEventReporter()
					.reportEvent(EventReporter.AR_TARGET_SELECTED,
							(String) null);
			doStart();
			doResume();
			break;
		}
	}

	public void setEnabled(int category) {
		buttonBar.setEnabled(category, true);
	}

	@Override
	public void onClick(View view) {
		if (view == info) {
			String version = "1.0";
			try {
				version = getPackageManager().getPackageInfo(getPackageName(), 0 ).versionName;
			} catch (NameNotFoundException e) {
			}
			CIPClientApp
					.instance()
					.displayInfo(
							"About",
							"This Paradise road-trip powered by Promomark. Version " + version + "\n\n"
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
		} else if (view == arImage) {
			selected(AR_VIEW);
		} else if (view == couponImage) {
			AppItem appItem = CIPClientApp.instance().getDataObjects().appItem;
			CIPClientApp
					.instance()
					.displayInfo(
							"Closest location",
							appItem.closestLocation
									+ "("
									+ (int) (appItem.distToLocationInMeters / 1609.34)
									+ " mi)");
		}
	}

	private class DrinkAdapter extends BaseAdapter {
		private List<DrinkItem> drinks;
		private Context context;

		DrinkAdapter(Context context, List<DrinkItem> drinks) {
			this.context = context;
			this.drinks = drinks;
		}

		public int getCount() {
			return this.drinks.size();
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View itemView;
			if (convertView == null) {
				itemView = LayoutInflater.from(context).inflate(
						R.layout.my_drinks_list_item, parent, false);
			} else {
				itemView = convertView;
			}

			ImageView image = (ImageView) itemView.findViewById(R.id.image);
			TextView title = (TextView) itemView.findViewById(R.id.title);
			TextView text = (TextView) itemView.findViewById(R.id.text);

			DrinkItem drink = drinks.get(position);
			title.setText(drink.drinkTitle);
			text.setText(drink.drinkText);
			image.setImageBitmap(getCachedBitmap(drink.drinkImage, false));
			return itemView;
		}
	}
}
