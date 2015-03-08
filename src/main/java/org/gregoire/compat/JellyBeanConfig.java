package org.gregoire.compat;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.view.View;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class JellyBeanConfig extends Config {

	public void doConfig(Activity activity) {
		Log.v(TAG, "JellyBean");
		activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
	}
	
}
