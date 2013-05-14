package com.promomark.cipclient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;

import com.promomark.cipclient.Downloader.FinishListener;

public class CIPClientApp extends Application implements FinishListener {

	private static CIPClientApp theOne;
	private Downloader downloader;
	private EventReporter eventReporter;
	LocationManager locationManager;
	private Activity current;
	private AppItem appItem;
	private CouponItem couponItem;
	private List<DrinkItem> drinkItems;
	private ARItem arItem;

	private static final String CIPCLIENTAPP = "CIPClient";

	class AppItem {
		int appId;
		String appTitle;
		int userId;
		String contestMobileUrl;
		String contestWebUrl;
		String contestText;
		String closestLocation;
		int distToLocationInMeters;
		boolean onPremise;
	}

	class ARItem {
		int id;
		String title;
		String targetImage;
		String displayTargetImage;
		String targetText;
		String triggeredContent;
		String triggeredContentTexture;
		String triggeredContentConfig;
	}

	class DrinkItem {
		String brandTitle;
		String brandLogo;
		String brandWebsite;
		int displayOrder;
		String drinkTitle;
		String drinkText;
		String drinkImage;
	}

	class CouponItem {
		int id;
		String title;
		String image;
		String text;
		Date startDate;
		Date endDate;
	}

	public CIPClientApp() {
		theOne = this;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		downloader = new Downloader(this, this);
		eventReporter = new EventReporter();
	}

	public static CIPClientApp instance() {
		return theOne;
	}

	public Downloader getDownloader() {
		return downloader;
	}

	public EventReporter getEventReporter() {
		return eventReporter;
	}

	public void setCurrentActivity(Activity activity) {
		this.current = activity;
	}

	public void displayFatalError(final String title, final String msg) {
		current.runOnUiThread(new Runnable() {
			public void run() {
				new AlertDialog.Builder(current).setTitle(title)
						.setMessage(msg)
						.setNegativeButton("OK", new OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								current.finish();
							}
						}).show();
			}
		});
	}

	public void getAppData() {
		String appDataS = eventReporter.getAppData();
		if (appDataS == null) {
			displayFatalError("Cannot contact server",
					"Cannot contact server. The application will now exit");
		}

		parseJSON(appDataS);
	}

	private Date parseDate(String date) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
		try {
			return df.parse(date);
		} catch (ParseException e) {
			Log.e(CIPCLIENTAPP, "Cannot parse: " + date);
			return new Date();
		}
	}

	public void parseJSON(String json) {
		try {
			JSONObject obj = new JSONObject(json);
			if (obj.has("status") && obj.getString("status").equals("fail")) {
				String msg = obj.getString("msg");
				displayFatalError("Cannot contact server",
						"Cannot contact server, " + msg
								+ ". The application will now exit");
			}

			appItem = new AppItem();
			appItem.appId = Integer.parseInt(obj.getString("appId"));
			appItem.appTitle = obj.getString("appTitle");
			appItem.userId = Integer.parseInt(obj.getString("userid"));
			appItem.contestMobileUrl = obj.getString("contestMobileUrl");
			appItem.contestWebUrl = obj.getString("contestWebUrl");
			appItem.contestText = obj.getString("contestText");
			appItem.closestLocation = obj.getString("closestLocation");
			appItem.distToLocationInMeters = Integer.parseInt(obj
					.getString("distToLocationInMeters"));
			appItem.onPremise = "true".equalsIgnoreCase(obj
					.getString("onPremise"));

			eventReporter.reportEvent(
					appItem.onPremise ? EventReporter.APPLAUNCH_ON_PREMISE
							: EventReporter.APPLAUNCH_OFF_PREMISE,
					(String) null);

			JSONObject ar = obj.getJSONArray("ar").getJSONObject(0);
			arItem = new ARItem();
			arItem.id = Integer.parseInt(ar.getString("id"));
			arItem.title = ar.getString("title");
			arItem.targetText = ar.getString("targetText");

			String targetImage = ar.getString("targetImage");
			arItem.targetImage = Downloader.convertToLocalName(targetImage);
			downloader.addItem(Downloader.CATEGORY_AR, targetImage);

			String displayTargetImage = ar.getString("displayTargetImage");
			arItem.displayTargetImage = Downloader
					.convertToLocalName(displayTargetImage);
			downloader.addItem(Downloader.CATEGORY_AR, displayTargetImage);

			String triggeredContent = ar.getString("triggeredContent");
			arItem.triggeredContent = Downloader
					.convertToLocalName(triggeredContent);
			downloader.addItem(Downloader.CATEGORY_AR, triggeredContent);

			String triggeredContentTexture = ar
					.getString("triggeredContentTexture");
			arItem.triggeredContentTexture = Downloader
					.convertToLocalName(triggeredContentTexture);
			downloader.addItem(Downloader.CATEGORY_AR, triggeredContentTexture);

			String triggeredContentConfig = ar
					.getString("triggeredContentConfig");
			arItem.triggeredContentConfig = Downloader
					.convertToLocalName(triggeredContentConfig);
			downloader.addItem(Downloader.CATEGORY_AR, triggeredContentConfig);

			JSONObject coupon = obj.getJSONArray("coupons").getJSONObject(0);
			couponItem = new CouponItem();
			couponItem.id = Integer.parseInt(coupon.getString("id"));
			couponItem.title = coupon.getString("name");

			String image = coupon.getString("image");
			arItem.displayTargetImage = Downloader.convertToLocalName(image);
			downloader.addItem(Downloader.CATEGORY_COUPONS, image);

			couponItem.text = coupon.getString("text");
			couponItem.startDate = parseDate(coupon.getString("startDate"));
			couponItem.endDate = parseDate(coupon.getString("endDate"));

			drinkItems = new ArrayList<CIPClientApp.DrinkItem>();
			JSONArray brands = obj.getJSONArray("brands");
			for (int i = 0; i < brands.length(); i++) {
				JSONObject brand = (JSONObject) brands.get(i);
				String brandTitle = brand.getString("name");
				String brandLogo = brand.getString("logo");
				String brandWebsite = brand.getString("website");
				JSONArray features = brand.getJSONArray("features");
				for (int j = 0; j < features.length(); j++) {
					JSONObject feature = (JSONObject) features.get(j);
					DrinkItem drinkItem = new DrinkItem();
					drinkItem.brandTitle = brandTitle;
					drinkItem.brandLogo = brandLogo;
					drinkItem.brandWebsite = brandWebsite;
					drinkItem.displayOrder = Integer.parseInt(feature
							.getString("displayOrder"));
					drinkItem.drinkTitle = feature.getString("name");
					drinkItem.drinkText = feature.getString("text");

					image = feature.getString("image");
					drinkItem.drinkImage = Downloader.convertToLocalName(image);
					downloader.addItem(Downloader.CATEGORY_DRINKS, image);
					drinkItems.add(drinkItem);
				}
			}

			Collections.sort(drinkItems, new Comparator<DrinkItem>() {
				public int compare(DrinkItem lhs, DrinkItem rhs) {
					return lhs.displayOrder - rhs.displayOrder;
				}
			});

			downloader.startDownloads();

			// AR test
		} catch (Exception e) {
			Log.e(CIPCLIENTAPP, "Cannot parse json: " + e.getMessage());
		}
	}

	@Override
	public void downloadsDone(int category) {
		// TODO Auto-generated method stub

	}

	public ARItem getARItem() {
		return arItem;
	}

	public void setLocation(double latitude, double longitude) {
		eventReporter.setLocation(latitude, longitude);
		new Thread() {
			public void run() {
				getAppData();
			}
		}.start();
	}

	public void ageOk(Date birthday) {
        eventReporter.setAgeVerified(birthday.getTime());
        eventReporter.reportEvent(EventReporter.AGE_VERIFICATION_PASSED, birthday);
    }

	public void ageFailed(Date birthday) {
        eventReporter.reportEvent(EventReporter.AGE_VERIFICATION_FAILED, birthday);
        displayFatalError("Age verification", "You must be 21+ to use this app. The application will now exit");
	}
}
