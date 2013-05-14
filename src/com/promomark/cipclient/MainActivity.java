package com.promomark.cipclient;

import java.io.File;

import android.util.Log;
import android.view.View;

import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.Rotation;
import com.metaio.sdk.jni.Vector3d;
import com.promomark.cipclient.CIPClientApp.ARItem;

public class MainActivity extends MetaioSDKViewActivity {

	private boolean running;
	private IGeometry mModel;

	@Override
	protected int getGUILayout() {
		// Attaching layout to the activity
		return R.layout.activity_main;
	}

	public void onButtonClick(View v) {
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();
		CIPClientApp.instance().setCurrentActivity(this);
	}

	@Override
	protected void loadContent() {
		try {
			ARItem item = CIPClientApp.instance().getARItem();
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
				MetaioDebug.log(Log.ERROR, "Error loading geometry: "
						+ base + item.triggeredContent);

		} catch (Exception e) {

		}
	}

	@Override
	protected void onGeometryTouched(IGeometry geometry) {
		if (running) {
			geometry.stopAnimation();
		} else {
			geometry.startAnimation(geometry.getAnimationNames().get(0), true);
		}
		running = !running;
	}

	@Override
	protected IMetaioSDKCallback getMetaioSDKCallbackHandler() {
		// TODO Auto-generated method stub
		return null;
	}

}
