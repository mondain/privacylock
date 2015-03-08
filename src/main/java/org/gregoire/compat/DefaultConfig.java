package org.gregoire.compat;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.view.View;

/**
 * Requires at least api 19
 * 
 * @author mondain
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class DefaultConfig extends Config {

	public void doConfig(Activity activity) {
		Log.v(TAG, "Default >= KitKat");
		activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE);
	}
	
}
