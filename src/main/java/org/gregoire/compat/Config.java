package org.gregoire.compat;

import android.app.Activity;

public abstract class Config {

	protected final String TAG = "Config";
	
	public abstract void doConfig(Activity activity);
	
}
