package com.promomark.cipclient;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

public class SplashActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		new Thread() {
			public void run() {
				try {
					Thread.sleep(40000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				startActivity(new Intent(CIPClientApp.instance(), MainActivity.class));
			}
		}.start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return false;
	}

}
