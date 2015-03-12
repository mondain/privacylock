package org.gregoire;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

public class PrefsActivity extends Activity {

	private static String TAG = "PrefsActivity";

	private static final String PREF_EMPTY_STRING = "";

	private static final int ADMIN_INTENT = 15;

	private DevicePolicyManager devicePolicyManager;

	private ComponentName adminReceiverName;
	
	private LinearLayout mainView;

	private LinearLayout entryView;
	
	private EditText smsNumberEntryBox;

	private HashMap<String, Integer> map = new HashMap<String, Integer>();

	private AtomicInteger currentSelectedId = new AtomicInteger();

	private ByteBuffer codeBuffer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_prefs);
		// get the policy manager
		devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		adminReceiverName = new ComponentName(this, AdminReceiver.class);
		// load the map
		map.put(getString(R.string.opt_unlock), R.string.opt_unlock);
		map.put(getString(R.string.opt_clear_call_log), R.string.opt_clear_call_log);
		map.put(getString(R.string.opt_clear_sms), R.string.opt_clear_sms);
		map.put(getString(R.string.opt_clear_camera_roll), R.string.opt_clear_camera_roll);
		map.put(getString(R.string.opt_send_emergency_sms), R.string.opt_send_emergency_sms);
		map.put(getString(R.string.opt_send_new_code), R.string.opt_send_new_code);
		map.put(getString(R.string.opt_wipe), R.string.opt_wipe);
		// get the views
		mainView = (LinearLayout) findViewById(R.id.main_interface);
		entryView = (LinearLayout) findViewById(R.id.sub_interface);
		smsNumberEntryBox = (EditText) findViewById(R.id.smsEntry);
		// get the listing
		final ListView listview = (ListView) findViewById(R.id.listView1);
		final ArrayList<String> list = new ArrayList<String>();
		list.addAll(map.keySet());
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list);
		listview.setAdapter(adapter);
		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
				final String item = (String) parent.getItemAtPosition(position);
				Log.v(TAG, "Selected: " + item + " id: " + map.get(item));
				// get the integer id
				currentSelectedId.set(map.get(item));
				// show the entry view (keypad or sms value)
				mainView.setVisibility(View.GONE);
				entryView.setVisibility(View.VISIBLE);
				// if emergency sms show the txt box
				if (currentSelectedId.get() == R.string.opt_send_emergency_sms) {
					smsNumberEntryBox.setVisibility(View.VISIBLE);
				} else {
					smsNumberEntryBox.setVisibility(View.GONE);
				}
			}

		});
		// attach listeners to the buttons
		int[] buttons = { R.id.button1, R.id.button2, R.id.button3, R.id.button4, R.id.button5, R.id.button6, R.id.button7, R.id.button8, R.id.button9 };
		for (int button : buttons) {
			((Button) findViewById(button)).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {
					Log.v(TAG, "Button: " + view.getId());
					switch (view.getId()) {
						case R.id.button1:
							codeBuffer.put(MainActivity.CODEX[0]);
							break;
						case R.id.button2:
							codeBuffer.put(MainActivity.CODEX[1]);
							break;
						case R.id.button3:
							codeBuffer.put(MainActivity.CODEX[2]);
							break;
						case R.id.button4:
							codeBuffer.put(MainActivity.CODEX[3]);
							break;
						case R.id.button5:
							codeBuffer.put(MainActivity.CODEX[4]);
							break;
						case R.id.button6:
							codeBuffer.put(MainActivity.CODEX[5]);
							break;
						case R.id.button7:
							codeBuffer.put(MainActivity.CODEX[6]);
							break;
						case R.id.button8:
							codeBuffer.put(MainActivity.CODEX[7]);
							break;
						case R.id.button9:
							codeBuffer.put(MainActivity.CODEX[8]);
							break;
					}
				}
			});
		}
		// buttons
		((Button) findViewById(R.id.saveBtn)).setOnClickListener(new OnClickListener() {

			final Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
			
			{
				intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminReceiverName);
				intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable admin functionality");
			}
			
			@Override
			public void onClick(View view) {
				Log.v(TAG, "Save: " + currentSelectedId.get());
				if (codeBuffer.position() == 0) {
					Log.v(TAG, "No code entered");
				} else {
					// get the bytes
					codeBuffer.flip();
					byte[] buf = new byte[codeBuffer.limit()];
					codeBuffer.get(buf);
					// save the current selection
					if (savePref(String.format("%d", currentSelectedId.get()), buf)) {
						Log.v(TAG, "Pref for " + currentSelectedId.get() + " saved");
						Toast.makeText(getApplicationContext(), getString(R.string.codeSavedNotification), Toast.LENGTH_SHORT).show();
					}
					codeBuffer.clear();
					// check for special "send new code" sequence and add it if its missing
					if (loadPref(String.format("%d", R.string.opt_send_new_code)) == null) {
						byte[] failsafe = new byte[13];
						Arrays.fill(failsafe, MainActivity.CODEX[8]);
						if (savePref(String.format("%d", R.string.opt_send_new_code), failsafe)) {
							Log.v(TAG, "Failsafe sequence saved");
						}
					}	
					// if its sms emergency save the number
					if (currentSelectedId.get() == R.string.opt_send_emergency_sms) {
						String number = smsNumberEntryBox.getText().toString();
						if (savePref("sms-number", number.getBytes())) {
							Log.v(TAG, "SMS number: " + number + " saved");
						}
					}					
					// request admin for actions requiring it
					if (currentSelectedId.get() == R.string.opt_wipe) {
    					// become admin (so wipe will work)
    					if (!devicePolicyManager.isAdminActive(adminReceiverName)) {
    						Log.v(TAG, "Admin is not active");
    						// execute some code after x time has passed
    						Handler handler = new Handler();
    						handler.postDelayed(new Runnable() {
    							public void run() {
    	    						// try to become active – must happen here in this activity, to get result
    	    						startActivityForResult(intent, 1); // 1 is enabled, 0 is disabled
    							}
    						}, 250);
    					} else {
    						// already is a device administrator, can do security operations now
    						Log.v(TAG, "Already admin");
    					}
					}
				}
				// hide the keypad
				entryView.setVisibility(View.GONE);
				mainView.setVisibility(View.VISIBLE);
			}

		});
		((Button) findViewById(R.id.cancelBtn)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				Log.v(TAG, "Cancel");
				// clear the current selection
				currentSelectedId.set(0);
				codeBuffer.clear();
				// hide the keypad
				entryView.setVisibility(View.GONE);
				mainView.setVisibility(View.VISIBLE);
			}

		});
		((Button) findViewById(R.id.quitBtn)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				Log.v(TAG, "Quit");
				// clear the current selection
				currentSelectedId.set(0);
				codeBuffer.clear();
				// exit
				finish();
			}

		});
		// holder of the code
		codeBuffer = ByteBuffer.allocate(64);
	}

	private boolean savePref(String key, byte[] val) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String str = Base64.encodeToString(val, Base64.NO_WRAP | Base64.NO_PADDING);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(key, str);
		return editor.commit();
	}

	private byte[] loadPref(String key) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor editor = prefs.edit();
		String str = prefs.getString(key, PREF_EMPTY_STRING);
		Log.v(TAG, str);
		byte[] value = Base64.decode(str, Base64.NO_WRAP | Base64.NO_PADDING);
		Log.v(TAG, "");
		return value;
	}
	
	private byte[] loadPref(int id) {
		String key = String.format("%d", id);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor editor = prefs.edit();
		String str = prefs.getString(key, "");
		Log.v(TAG, "Preference - key: " + key + " value: " + str);
		byte[] value = null;
		if (!("").equals(str)) {
			value = Base64.decode(str, Base64.NO_WRAP | Base64.NO_PADDING);
			Log.v(TAG, "" + Arrays.toString(value));
		}
		return value;
	}

	private boolean doRemovePref(String key) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor editor = prefs.edit();
		editor.remove(key);
		return editor.commit();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		Log.v(TAG, "onActivityResult - intent: " + intent + " req: " + requestCode + " res: " + resultCode);
		if (requestCode == ADMIN_INTENT) {
			final String str;
			if (resultCode == RESULT_OK) {
				str = getString(R.string.adminRegisterd);
			} else {
				str = getString(R.string.adminRegisterFailed);
			}
			NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			Notification notification = new Notification(R.mipmap.ic_launcher, str, System.currentTimeMillis());
			Intent notificationIntent = new Intent(this, MainActivity.class);
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
			notification.setLatestEventInfo(PrefsActivity.this, getString(R.string.admin), str, pendingIntent);
			notificationManager.notify(10001, notification);			
		}
	}

}
