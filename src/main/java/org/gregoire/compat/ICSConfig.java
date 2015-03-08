package org.gregoire.compat;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ICSConfig extends Config {

	public void doConfig(Activity activity) {
		Log.v(TAG, "ICS");
	}
	
}
