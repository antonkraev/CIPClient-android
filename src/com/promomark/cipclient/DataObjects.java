package com.promomark.cipclient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

public class DataObjects {

	AppItem appItem;
	CouponItem couponItem;
	List<DrinkItem> drinkItems;
	ARItem arItem;

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

	private Date parseDate(String date) throws Exception {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
		return df.parse(date);
	}

	public void parseJSON(String json) throws Exception {
		JSONObject obj = new JSONObject(json);
		if (obj.has("status") && obj.getString("status").equals("fail")) {
			throw new Exception(obj.getString("msg"));
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
		appItem.onPremise = "true".equalsIgnoreCase(obj.getString("onPremise"));

		CIPClientApp.instance().getEventReporter().reportEvent(
				appItem.onPremise ? EventReporter.APPLAUNCH_ON_PREMISE
						: EventReporter.APPLAUNCH_OFF_PREMISE, (String) null);

		JSONObject ar = obj.getJSONArray("ar").getJSONObject(0);
		arItem = new ARItem();
		arItem.id = Integer.parseInt(ar.getString("id"));
		arItem.title = ar.getString("title");
		arItem.targetText = ar.getString("targetText");

		String targetImage = ar.getString("targetImage");
		arItem.targetImage = Downloader.convertToLocalName(targetImage);
		CIPClientApp.instance().getDownloader().addItem(Downloader.CATEGORY_AR, targetImage);

		String displayTargetImage = ar.getString("displayTargetImage");
		arItem.displayTargetImage = Downloader
				.convertToLocalName(displayTargetImage);
		CIPClientApp.instance().getDownloader().addItem(Downloader.CATEGORY_AR, displayTargetImage);

		String triggeredContent = ar.getString("triggeredContent");
		arItem.triggeredContent = Downloader
				.convertToLocalName(triggeredContent);
		CIPClientApp.instance().getDownloader().addItem(Downloader.CATEGORY_AR, triggeredContent);

		String triggeredContentTexture = ar
				.getString("triggeredContentTexture");
		arItem.triggeredContentTexture = Downloader
				.convertToLocalName(triggeredContentTexture);
		CIPClientApp.instance().getDownloader().addItem(Downloader.CATEGORY_AR, triggeredContentTexture);

		String triggeredContentConfig = ar.getString("triggeredContentConfig");
		arItem.triggeredContentConfig = Downloader
				.convertToLocalName(triggeredContentConfig);
		CIPClientApp.instance().getDownloader().addItem(Downloader.CATEGORY_AR, triggeredContentConfig);

		JSONObject coupon = obj.getJSONArray("coupons").getJSONObject(0);
		couponItem = new CouponItem();
		couponItem.id = Integer.parseInt(coupon.getString("id"));
		couponItem.title = coupon.getString("name");

		String image = coupon.getString("image");
		couponItem.image = Downloader.convertToLocalName(image);
		CIPClientApp.instance().getDownloader().addItem(Downloader.CATEGORY_COUPONS, image);

		couponItem.text = coupon.getString("text");
		couponItem.startDate = parseDate(coupon.getString("startDate"));
		couponItem.endDate = parseDate(coupon.getString("endDate"));

		drinkItems = new ArrayList<DrinkItem>();
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
				CIPClientApp.instance().getDownloader().addItem(Downloader.CATEGORY_DRINKS, image);
				drinkItems.add(drinkItem);
			}
		}

		Collections.sort(drinkItems, new Comparator<DrinkItem>() {
			public int compare(DrinkItem lhs, DrinkItem rhs) {
				return lhs.displayOrder - rhs.displayOrder;
			}
		});
	}
}
