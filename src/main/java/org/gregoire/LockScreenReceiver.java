package org.gregoire;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class LockScreenReceiver extends BroadcastReceiver {

	private static String TAG = "LockScreenReceiver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v(TAG, "onReceive " + intent.getAction());
		String action = intent.getAction();
		// if the screen was just turned on or it just booted up, start your Lock Activity
		if (action.equals(Intent.ACTION_SCREEN_OFF) || action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			Intent i = new Intent(context, MainActivity.class);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(i);
		}
	}
	
}
