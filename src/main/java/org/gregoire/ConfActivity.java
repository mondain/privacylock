package org.gregoire;

import java.io.UnsupportedEncodingException;

import org.gregoire.util.PrefsUtil;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

public class ConfActivity extends Activity {

	private static String TAG = "ConfActivity";

	private static boolean DEBUG_MODE;

	private static final String PREF_EMPTY_STRING = "";

	private LinearLayout mainView;

	private EditText recordingNumberEntryBox;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// get debug mode
		DEBUG_MODE = BuildConfig.DEBUG;
		// set the layout
		setContentView(R.layout.activity_conf);
		// get the views
		mainView = (LinearLayout) findViewById(R.id.main_interface);
		recordingNumberEntryBox = (EditText) findViewById(R.id.recordPicker);
		// look for current value
		String recordTime = null;
        try {
        	recordTime = new String(PrefsUtil.loadPref(this, Constants.KEY_RECORD_TIME), "UTF8");
        } catch (Exception e) {
        }
		if (!TextUtils.isEmpty(recordTime)) {
			recordingNumberEntryBox.setText(recordTime);
		}
		ToggleButton toggleBtn = (ToggleButton) findViewById(R.id.trustedToggleBtn);
		// look for current value
		String enabled = null;
        try {
        	enabled = new String(PrefsUtil.loadPref(this, Constants.KEY_TRUSTED_FEATURE), "UTF8");
        } catch (Exception e) {
        }
		if (!TextUtils.isEmpty(enabled)) {
			toggleBtn.setChecked("vrai".equals(enabled));
		}
		toggleBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				boolean checked = ((ToggleButton) view).isChecked();
				if (DEBUG_MODE)
					Log.v(TAG, "Toggle trusted place" + checked);
				// get the app context
				final Context context = getApplicationContext();
				try {
					if (PrefsUtil.savePref(context, Constants.KEY_TRUSTED_FEATURE, (checked ? "vrai" : "faux").getBytes("UTF8"))) {
						if (DEBUG_MODE)
							Log.v(TAG, "Trusted place feature: " + checked + " saved");
					}
				} catch (UnsupportedEncodingException e) {
					if (DEBUG_MODE)
						Log.w(TAG, "Trusted place feature: " + checked + " save failed " + e.getLocalizedMessage());
				}
			}

		});
		
		// buttons
		((Button) findViewById(R.id.saveBtn)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				if (DEBUG_MODE)
					Log.v(TAG, "Save");
				// get the app context
				final Context context = getApplicationContext();

				String recordTime = recordingNumberEntryBox.getText().toString();
				try {
					if (PrefsUtil.savePref(context, Constants.KEY_RECORD_TIME, recordTime.getBytes("UTF8"))) {
						if (DEBUG_MODE)
							Log.v(TAG, "Record time: " + recordTime + " saved");
					}
				} catch (UnsupportedEncodingException e) {
					if (DEBUG_MODE)
						Log.w(TAG, "Record time: " + recordTime + " save failed " + e.getLocalizedMessage());
				}

				Toast.makeText(getApplicationContext(), getString(R.string.configSavedNotification), Toast.LENGTH_SHORT).show();
			}

		});
		((Button) findViewById(R.id.trustedAddBtn)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				if (DEBUG_MODE)
					Log.v(TAG, "Add trusted place");
				// get the app context
				final Context context = getApplicationContext();

				// save current ssid
				String ssid = getCurrentWifiSSID();
				try {
					if (PrefsUtil.savePref(context, Constants.KEY_TRUSTED_PLACES, ssid.getBytes("UTF8"))) {
						if (DEBUG_MODE)
							Log.v(TAG, "Trusted place: " + ssid + " saved");
					}
				} catch (UnsupportedEncodingException e) {
					if (DEBUG_MODE)
						Log.w(TAG, "Trusted place: " + ssid + " save failed " + e.getLocalizedMessage());
				}
			}

		});
		((Button) findViewById(R.id.trustedRemoveBtn)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				if (DEBUG_MODE)
					Log.v(TAG, "Remove trusted place");
				// get the app context
				final Context context = getApplicationContext();
				
				PrefsUtil.removePref(context, Constants.KEY_TRUSTED_PLACES);

			}

		});
	}

	public String getCurrentWifiSSID() {
		// the current ssid
		String ssid = null;
		// check for wifi support
		if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI)) {
			// get the app context
			final Context context = getApplicationContext();
			ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			if (networkInfo.isConnected()) {
				final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
				final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
				if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
					ssid = connectionInfo.getSSID();
				}
			}
		}
		return ssid;
	}

}
