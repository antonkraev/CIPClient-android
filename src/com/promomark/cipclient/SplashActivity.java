package com.promomark.cipclient;

import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;

public class SplashActivity extends Activity implements LocationListener,
		OnClickListener {

	private LocationManager locationManager;
	private DatePicker date;
	private View ageView;
	private static final float ACURACY = 100f;
	private boolean locationObtained;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		locationObtained = false;
	}

	@Override
	public void onLocationChanged(Location location) {
		if (location.getAccuracy() <= ACURACY) {
			locationObtained = true;
			locationManager.removeUpdates(this);

			CIPClientApp.instance().setLocation(location.getLatitude(),
					location.getLongitude());
			TextView status = (TextView) findViewById(R.id.status);
			status.setVisibility(View.GONE);

			long age = CIPClientApp.instance().getEventReporter()
					.getAgeVerified();
			if (age == 0) {
				date = (DatePicker) findViewById(R.id.date);
				Button enter = (Button) findViewById(R.id.enter);
				enter.setOnClickListener(this);
				ageView = findViewById(R.id.ageConfirmation);
				ageView.setVisibility(View.VISIBLE);
			} else {
				CIPClientApp.instance().ageOk(new Date(age));
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		CIPClientApp.instance().setCurrentActivity(this);

		if (!locationObtained) {
			locationManager = (LocationManager) getBaseContext()
					.getSystemService(Context.LOCATION_SERVICE);
			final Location location = locationManager
					.getLastKnownLocation(LocationManager.GPS_PROVIDER);

			if (location != null && location.getAccuracy() < ACURACY) {
				onLocationChanged(location);
			} else {
				locationManager.requestLocationUpdates(
						LocationManager.GPS_PROVIDER, 0, 0, this);
				new Thread() {
					public void run() {
						try {
							Thread.sleep(60000);
						} catch (InterruptedException e) {
							// ignore
						}

						if (!locationObtained) {
							CIPClientApp
									.instance()
									.displayFatalError(
											"Location",
											"Cannot obtain your location. Please enable location services.\n\nThe application will now exit");
						}
					}
				}.start();
			}
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public void onClick(View v) {
		ageView.setVisibility(View.GONE);

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, date.getYear());
		cal.set(Calendar.MONTH, date.getMonth());
		cal.set(Calendar.DAY_OF_MONTH, date.getDayOfMonth());
		cal.add(Calendar.YEAR, 21);

		Calendar current = Calendar.getInstance();
		current.setTime(new Date());

		if (current.after(cal)) {
			cal.add(Calendar.YEAR, -21);
			CIPClientApp.instance().ageOk(cal.getTime());
		} else {
			cal.add(Calendar.YEAR, -21);
			CIPClientApp.instance().ageFailed(cal.getTime());
		}
	}

}
