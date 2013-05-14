package com.promomark.cipclient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

public class EventReporter {

	private static final String EVENTREPORTER = "EventReporter";

	public static final String API_PREFIX = "https://cip.c4platform.com/";

	public static final String AGE_VERIFICATION_PASSED = "AGE_VERIFICATION_PASSED";
	public static final String AGE_VERIFICATION_FAILED = "AGE_VERIFICATION_FAILED";
	public static final String APPLAUNCH_OFF_PREMISE = "APPLAUNCH_OFF_PREMISE";
	public static final String APPLAUNCH_ON_PREMISE = "APPLAUNCH_ON_PREMISE";
	public static final String AR_TARGET_SELECTED = "AR_TARGET_SELECTED";
	public static final String AR_ANIMATION_STARTED = "AR_ANIMATION_STARTED";
	public static final String AR_ANIMATION_ENDED = "AR_ANIMATION_ENDED";
	public static final String COUPON_DOWNLOADED = "COUPON_DOWNLOADED";
	public static final String DRINK_CLICKED = "DRINK_CLICKED";
	public static final String CONTEST_REDIRECT = "CONTEST_REDIRECT";

	public static final String ID = "id";
	public static final String AGE_VERIFIED = "ageVerified";

	String deviceID;
	String deviceIP;
	String userAgent;
	int width;
	int height;
	double latitude;
	double longitude;
	boolean couponReported = false;
	SharedPreferences sharedPreferences;
	long ageVerified = 0;

	public long getAgeVerified() {
		return ageVerified;
	}

	public void setAgeVerified(long age) {
		this.ageVerified = age;
		sharedPreferences.edit().putLong(AGE_VERIFIED, ageVerified).commit();
	}

	public EventReporter() {
		sharedPreferences = CIPClientApp.instance().getSharedPreferences("preferences", Context.MODE_PRIVATE);
		
		deviceID = sharedPreferences.getString(ID, null);
		if (deviceID == null) {
			deviceID = UUID.randomUUID().toString();
			sharedPreferences.edit().putString(ID, deviceID).commit();
		}
		ageVerified = sharedPreferences.getLong(AGE_VERIFIED, 0);
		
		deviceIP = "unknown";
		userAgent = Build.MANUFACTURER + "/" + Build.MODEL + "/"
				+ Build.PRODUCT + " " + Build.VERSION.SDK_INT;
		WindowManager windowManager = (WindowManager) CIPClientApp.instance()
				.getSystemService(Context.WINDOW_SERVICE);
		final DisplayMetrics dm = new DisplayMetrics();
		windowManager.getDefaultDisplay().getMetrics(dm);
		width = dm.widthPixels > dm.heightPixels ? dm.widthPixels
				: dm.heightPixels;
		height = dm.widthPixels > dm.heightPixels ? dm.heightPixels
				: dm.widthPixels;
		if (width < height) {
			int t = width;
			width = height;
			height = t;
		}
	}

	void setLocation(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	private Map<String, String> getParams() {
		Map<String, String> params = new HashMap<String, String>();

		params.put("token", "6e2a40c7-b67b-4227-80ff-36896188be23");
		params.put("lat", latitude + "");
		params.put("long", longitude + "");
		params.put("deviceWidth", width + "");
		params.put("deviceHeight", height + "");
		params.put("imei", deviceID);
		params.put("srcip", deviceIP);
		params.put("useragent", userAgent);
		return params;
	}

	private String doGet(String url, Map<String, String> params) {
		try {
			String[] keys = params.keySet().toArray(new String[] {});
			for (int i = 0; i < keys.length; i++) {
				char c = (i == 0 ? '?' : '&');
				String key = keys[i];
				url = url + c + key + "="
						+ URLEncoder.encode(params.get(key), "UTF-8");
			}
			Log.i(EVENTREPORTER, "url: " + url);

			final HttpClient client = new DefaultHttpClient();
			final HttpGet request = new HttpGet();
			request.setURI(new URI(url));
			final HttpResponse response = client.execute(request);
			final BufferedReader in = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));

			StringBuffer res = new StringBuffer();
			String str;
			while ((str = in.readLine()) != null) {
				res.append(str);
			}

			Log.i(EVENTREPORTER, "res: " + res);
			return res.toString();
		} catch (Exception e) {
			Log.e(EVENTREPORTER, "error " + e.getClass().getCanonicalName() + ": " + e.getMessage());
			return null;
		}
	}

	boolean reportEvent(String event, String data) {
		if (event.equals(COUPON_DOWNLOADED)) {
			if (couponReported == true) {
				return false;
			} else {
				couponReported = true;
			}
		}

		final Map<String, String> params = getParams();
		params.put("event", event);
		if (data != null) {
			params.put("data", data);
		}

		new Thread() {
			public void run() {
				doGet(API_PREFIX + "reportEvent", params);
			}
		}.start();
		
		return true;
	}

	boolean reportEvent(String event, Date data) {
		return reportEvent(event, DateFormat.format("yyyy/mm/dd", data)
				.toString());
	}

	String getAppData() {
		return doGet(API_PREFIX + "getappdata", getParams());
	}
}
