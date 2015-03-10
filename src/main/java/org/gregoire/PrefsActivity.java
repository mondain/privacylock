package org.gregoire;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
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

	private HashMap<String, Integer> map = new HashMap<String, Integer>();

	private AtomicInteger currentSelectedId = new AtomicInteger();

	private ByteBuffer codeBuffer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_prefs);
		// load the map
		map.put(getString(R.string.opt_unlock), R.string.opt_unlock);
		map.put(getString(R.string.opt_clear_call_log), R.string.opt_clear_call_log);
		map.put(getString(R.string.opt_clear_sms), R.string.opt_clear_sms);
		map.put(getString(R.string.opt_clear_camera_roll), R.string.opt_clear_camera_roll);
		map.put(getString(R.string.opt_send_emergency_sms), R.string.opt_send_emergency_sms);
		map.put(getString(R.string.opt_wipe), R.string.opt_wipe);
		// get the views
		mainView = (LinearLayout) findViewById(R.id.main_interface);
		entryView = (LinearLayout) findViewById(R.id.sub_interface);
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
							codeBuffer.put((byte) 3);
							break;
						case R.id.button2:
							codeBuffer.put((byte) 0xcc);
							break;
						case R.id.button3:
							codeBuffer.put((byte) 64);
							break;
						case R.id.button4:
							codeBuffer.put((byte) 0xa1);
							break;
						case R.id.button5:
							codeBuffer.put((byte) 22);
							break;
						case R.id.button6:
							codeBuffer.put((byte) 0xef);
							break;
						case R.id.button7:
							codeBuffer.put((byte) 8);
							break;
						case R.id.button8:
							codeBuffer.put((byte) 1);
							break;
						case R.id.button9:
							codeBuffer.put((byte) 0x0c);
							break;
					}
				}
			});
		}
		// buttons
		((Button) findViewById(R.id.saveBtn)).setOnClickListener(new OnClickListener() {

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
					}
					codeBuffer.clear();
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
    	    						Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
    	    						intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminReceiverName);
    	    						intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable admin functionality");
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
		// get the policy manager
		devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		adminReceiverName = new ComponentName(this, AdminReceiver.class);
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

	private boolean doRemovePref(String key) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor editor = prefs.edit();
		editor.remove(key);
		return editor.commit();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		Log.v(TAG, "onActivityResult " + intent.getAction());
		if (requestCode == ADMIN_INTENT) {
			if (resultCode == RESULT_OK) {
				Toast.makeText(getApplicationContext(), "Registered As Admin", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getApplicationContext(), "Failed to register as Admin", Toast.LENGTH_SHORT).show();
			}
		}
	}

}
