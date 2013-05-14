package com.promomark.cipclient;

import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.location.LocationManager;
import android.view.Gravity;
import android.widget.TextView;

import com.promomark.cipclient.Downloader.FinishListener;

public class CIPClientApp extends Application implements FinishListener {

	static CIPClientApp theOne;
	Downloader downloader;
	EventReporter eventReporter;
	DataObjects dataObjects;
	LocationManager locationManager;
	private Activity current;
	private boolean enabled[];

	public CIPClientApp() {
		theOne = this;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		downloader = new Downloader(this, this);
		eventReporter = new EventReporter();
		dataObjects = new DataObjects();
		enabled = new boolean[4];
		for (int i=0; i<enabled.length; i++)
			enabled[i] = false;
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

	public DataObjects getDataObjects() {
		return dataObjects;
	}

	public void setCurrentActivity(Activity activity) {
		this.current = activity;
		if (current instanceof MainActivity) {
			for (int i=0; i<enabled.length; i++)
				if (enabled[i])
					((MainActivity) current).setEnabled(i);
		}
	}

	public void displayFatalError(final String title, final String msg) {
		openAlert(title, msg, android.R.drawable.ic_dialog_alert, new OnClickListener() {
			public void onClick(DialogInterface dialog,
					int which) {
				current.finish();
			}
		});
	}

	public void displayInfo(final String title, final String msg) {
		openAlert(title, msg, android.R.drawable.ic_dialog_info, null);
	}
	
	private void openAlert(final String title, final String msg, int icon, final OnClickListener listener) {
		final TextView myMsg = new TextView(this);
		myMsg.setText(msg);
		myMsg.setGravity(Gravity.CENTER_HORIZONTAL);
		myMsg.setTextSize(17);
		myMsg.setTextColor(getResources().getColor(android.R.color.black));
		myMsg.setPadding(20, 20, 20, 20);

		current.runOnUiThread(new Runnable() {
			public void run() {
				new AlertDialog.Builder(current).setTitle(title)
						.setIcon(android.R.drawable.ic_dialog_info)
						.setNegativeButton("OK", listener)
						.setView(myMsg)
						.show();
			}
		});
	}
	
	public void getAppData() {
		try {
			String appDataS = eventReporter.getAppData();
			dataObjects.parseJSON(appDataS);
			downloader.startDownloads();
		} catch (Exception e) {
			String msg = e.getMessage() != null? e.getMessage(): e.getClass().getCanonicalName();
			displayFatalError("Problem",
					"Cannot contact server:\n'" + msg
							+ "'.\n\nThe application will now exit");
		}
	}

	@Override
	public void downloadsDone(int category) {
		enabled[category] = true;
		if (current instanceof MainActivity) {
			((MainActivity) current).setEnabled(category);
		}
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
        displayFatalError("Age verification", "You must be 21+ to use this app.\n\nThe application will now exit");
	}
}
