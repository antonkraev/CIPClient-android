package com.promomark.cipclient;

import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;

public class SplashActivity extends Activity implements LocationListener, OnClickListener {

	private LocationManager locationManager;
	private DatePicker date;
	private View ageView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		locationManager = (LocationManager) getBaseContext().getSystemService(
				Context.LOCATION_SERVICE);
		Criteria fine = new Criteria();
		fine.setAccuracy(Criteria.ACCURACY_FINE);
		String provider = this.locationManager.getBestProvider(fine, true);
		locationManager.requestLocationUpdates(provider, 10 * 60 * 1000, 100, this);

		setContentView(R.layout.activity_splash);
	}

	@Override
	public void onLocationChanged(Location location) {
		if (location.getAccuracy() < 50) {
			locationManager.removeUpdates(this);
			
			CIPClientApp.instance().setLocation(location.getLatitude(), location.getLongitude());
			TextView status = (TextView) findViewById(R.id.status);
			status.setVisibility(View.GONE);
			
			long age = CIPClientApp.instance().getEventReporter().getAgeVerified();
			if (age == 0) {
				date = (DatePicker) findViewById(R.id.date);
				Button enter = (Button) findViewById(R.id.enter);
				enter.setOnClickListener(this);
				ageView = findViewById(R.id.ageConfirmation);
				ageView.setVisibility(View.VISIBLE);
			} else {
				CIPClientApp.instance().ageOk(new Date(age));
				startActivity(new Intent(CIPClientApp.instance(), MainActivity.class));
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		CIPClientApp.instance().setCurrentActivity(this);
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
			startActivity(new Intent(CIPClientApp.instance(), MainActivity.class));
		} else {
			cal.add(Calendar.YEAR, -21);
			CIPClientApp.instance().ageFailed(cal.getTime());
		}
	}
	
}
