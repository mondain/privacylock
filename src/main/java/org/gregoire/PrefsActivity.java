package org.gregoire;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

public class PrefsActivity extends Activity {

	private static String TAG = "PrefsActivity";

	private static boolean DEBUG_MODE;

	private static final String PREF_EMPTY_STRING = "";

	private static final int ADMIN_INTENT = 15;

	private DevicePolicyManager devicePolicyManager;

	private ComponentName adminReceiverName;

	private LinearLayout mainView;

	private LinearLayout entryView;

	private TableLayout smsSection;

	private EditText smsNumberEntryBox;

	private TableLayout emailSection;

	private EditText emailEntryBox;

	// holder of list items / options
	private ArrayList<ListItem> list = new ArrayList<ListItem>();

	private AtomicInteger currentSelectedId = new AtomicInteger();

	private ByteBuffer codeBuffer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// get debug mode
		/*
		PackageManager pacMan = getPackageManager();
		String pacName = getPackageName();
		ApplicationInfo appInfo = null;
		try {
		    appInfo = pacMan.getApplicationInfo(pacName, 0);
			DEBUG_MODE = (0 != (appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE));
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		*/
		DEBUG_MODE = BuildConfig.DEBUG;
		// set the layout
		setContentView(R.layout.activity_prefs);
		// get the policy manager
		devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		adminReceiverName = new ComponentName(this, AdminReceiver.class);
		// load the list
		list.add(new ListItem(R.string.opt_unlock, getString(R.string.opt_unlock), getString(R.string.opt_desc_unlock)));
		list.add(new ListItem(R.string.opt_clear_call_log, getString(R.string.opt_clear_call_log), getString(R.string.opt_desc_clear_call_log)));
		list.add(new ListItem(R.string.opt_clear_sms, getString(R.string.opt_clear_sms), getString(R.string.opt_desc_clear_sms)));
		list.add(new ListItem(R.string.opt_clear_camera_roll, getString(R.string.opt_clear_camera_roll), getString(R.string.opt_desc_clear_camera_roll)));
		list.add(new ListItem(R.string.opt_send_emergency_sms, getString(R.string.opt_send_emergency_sms), getString(R.string.opt_desc_send_emergency_sms)));
		list.add(new ListItem(R.string.opt_send_new_code, getString(R.string.opt_send_new_code), getString(R.string.opt_desc_send_new_code)));
		list.add(new ListItem(R.string.opt_wipe, getString(R.string.opt_wipe), getString(R.string.opt_desc_wipe)));
		// get the views
		mainView = (LinearLayout) findViewById(R.id.main_interface);
		entryView = (LinearLayout) findViewById(R.id.sub_interface);
		smsSection = (TableLayout) findViewById(R.id.smsSection);
		smsNumberEntryBox = (EditText) findViewById(R.id.smsEntry);
		emailSection = (TableLayout) findViewById(R.id.emailSection);
		emailEntryBox = (EditText) findViewById(R.id.emailEntry);
		// get the listing
		final ListView listview = (ListView) findViewById(R.id.listView1);
		listview.setAdapter(new ListItemAdapter<ListItem>(this, R.layout.list_detail, list));
		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
				final ListItem item = (ListItem) parent.getItemAtPosition(position);
				if (DEBUG_MODE)
					Log.v(TAG, "Selected: " + item);
				// get the integer id
				currentSelectedId.set(item.id);
				// show the entry view (keypad or sms value)
				mainView.setVisibility(View.GONE);
				entryView.setVisibility(View.VISIBLE);
				// if emergency sms show the txt box
				if (currentSelectedId.get() == R.string.opt_send_emergency_sms) {
					smsSection.setVisibility(View.VISIBLE);
					// look for current value
					String sms = null;
                    try {
                    	sms = new String(PrefsActivity.loadPref(PrefsActivity.this, 999), "UTF8");
                    } catch (Exception e) {
                    }
					if (sms != null) {
						smsNumberEntryBox.setText(sms);
					}
				} else {
					smsSection.setVisibility(View.GONE);
				}
				if (currentSelectedId.get() == R.string.opt_send_new_code) {
					emailSection.setVisibility(View.VISIBLE);
					// look for current value
					String email = null;
                    try {
	                    email = new String(PrefsActivity.loadPref(PrefsActivity.this, 666), "UTF8");
                    } catch (Exception e) {
                    }
					if (email != null) {
						emailEntryBox.setText(email);
					}
				} else {
					emailSection.setVisibility(View.GONE);
				}
			}

		});
		// attach listeners to the buttons
		int[] buttons = { R.id.button1, R.id.button2, R.id.button3, R.id.button4, R.id.button5, R.id.button6, R.id.button7, R.id.button8, R.id.button9 };
		for (int button : buttons) {
			((Button) findViewById(button)).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {
					if (DEBUG_MODE)
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
				final int selectedItem = currentSelectedId.get();
				if (DEBUG_MODE)
					Log.v(TAG, "Save: " + selectedItem);
				// get the app context
				final Context context = getApplicationContext();
				// check for a code sequence
				if (codeBuffer.position() > 0) {
					// get the bytes
					codeBuffer.flip();
					byte[] buf = new byte[codeBuffer.limit()];
					codeBuffer.get(buf);
					// save the current selection
					if (savePref(context, String.format("%d", selectedItem), buf)) {
						if (DEBUG_MODE)
							Log.v(TAG, "Pref for " + selectedItem + " saved");
						Toast.makeText(getApplicationContext(), getString(R.string.codeSavedNotification), Toast.LENGTH_SHORT).show();
					}
				} else {
					Log.v(TAG, "No code sequence to save");
				}
				codeBuffer.clear();
				switch (selectedItem) {
					case R.string.opt_send_emergency_sms:
						// if its sms emergency save the number
						String number = smsNumberEntryBox.getText().toString();
						try {
							if (savePref(context, "999", number.getBytes("UTF8"))) {
								if (DEBUG_MODE)
									Log.v(TAG, "SMS number: " + number + " saved");
							}
						} catch (UnsupportedEncodingException e) {
							if (DEBUG_MODE)
								Log.w(TAG, "SMS number: " + number + " save failed " + e.getLocalizedMessage());
						}
						break;
					case R.string.opt_send_new_code:
						// if its failsafe save the email address
						String email = emailEntryBox.getText().toString();
						try {
							if (savePref(context, "666", email.getBytes("UTF8"))) {
								if (DEBUG_MODE)
									Log.v(TAG, "Email: " + email + " saved");
							}
						} catch (UnsupportedEncodingException e) {
							if (DEBUG_MODE)
								Log.w(TAG, "Email: " + email + " save failed " + e.getLocalizedMessage());
						}
						break;
					case R.string.opt_wipe:
						// become admin (so wipe will work)
						if (!devicePolicyManager.isAdminActive(adminReceiverName)) {
							if (DEBUG_MODE)
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
							if (DEBUG_MODE)
								Log.v(TAG, "Already admin");
						}
						break;
				}
				// check for special "send new code" sequence and add it if its missing
				if (PrefsActivity.loadPref(context, R.string.opt_send_new_code) == null) {
					byte[] failsafe = new byte[13];
					Arrays.fill(failsafe, MainActivity.CODEX[8]);
					if (savePref(context, String.format("%d", R.string.opt_send_new_code), failsafe)) {
						if (DEBUG_MODE)
							Log.v(TAG, "Failsafe sequence saved");
					}
				}
				// hide the keypad
				entryView.setVisibility(View.GONE);
				smsSection.setVisibility(View.GONE);
				emailSection.setVisibility(View.GONE);
				mainView.setVisibility(View.VISIBLE);
			}

		});
		((Button) findViewById(R.id.emailBtn)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				if (DEBUG_MODE)
					Log.v(TAG, "Test email");
				new MailerTask().execute(emailEntryBox.getText().toString(), getString(R.string.emailTestText));
			}

		});
		((Button) findViewById(R.id.cancelBtn)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				if (DEBUG_MODE)
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
				if (DEBUG_MODE)
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

	public final static boolean savePref(Context context, String key, byte[] val) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String str = Base64.encodeToString(val, Base64.NO_WRAP | Base64.NO_PADDING);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(key, str);
		return editor.commit();
	}

	public final static byte[] loadPref(Context context, String key) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String str = prefs.getString(key, PREF_EMPTY_STRING);
		if (DEBUG_MODE)
			Log.v(TAG, str);
		byte[] value = Base64.decode(str, Base64.NO_WRAP | Base64.NO_PADDING);
		return value;
	}

	public final static byte[] loadPref(Context context, int id) {
		String key = String.format("%d", id);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String str = prefs.getString(key, "");
		if (DEBUG_MODE)
			Log.v(TAG, "Preference - key: " + key + " value: " + str);
		byte[] value = null;
		if (!("").equals(str)) {
			value = Base64.decode(str, Base64.NO_WRAP | Base64.NO_PADDING);
			if (DEBUG_MODE)
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

	@SuppressWarnings("deprecation")
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (DEBUG_MODE)
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

	private class ListItemAdapter<T> extends ArrayAdapter<ListItem> {

		public ListItemAdapter(Context context, int textViewResourceId, ArrayList<ListItem> list) {
			super(context, textViewResourceId, list);
			if (DEBUG_MODE)
				Log.v(TAG, "Adapter - list: " + list);
		}

		private class ViewHolder {

			TextView title;

			TextView desc;
		}

		@SuppressWarnings("unchecked")
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if (convertView == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = vi.inflate(R.layout.list_detail, null);
				holder = new ViewHolder();
				holder.title = (TextView) convertView.findViewById(R.id.textTitle);
				holder.desc = (TextView) convertView.findViewById(R.id.textDesc);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			if (!isEmpty()) {
				ListItem item = getItem(position);
				holder.title.setText(item.title);
				holder.desc.setText(item.desc);
			}
			return convertView;
		}

	}

	private class ListItem {

		int id;

		String title;

		String desc;

		ListItem(int id, String title, String desc) {
			this.id = id;
			this.title = title;
			this.desc = desc;
		}

		@Override
		public String toString() {
			return "ListItem [id=" + id + ", title=" + title + ", desc=" + desc + "]";
		}

	}

	private class MailerTask extends AsyncTask<String, Void, Boolean> {

		protected Boolean doInBackground(String... args) {
			// send the code
			try {
				SMTPMailer.send(args[0], args[1]);
				return Boolean.TRUE;
			} catch (Exception e) {
				if (DEBUG_MODE)
					Log.w(TAG, "Exception sending test email", e);
			}
			return Boolean.FALSE;
		}

		protected void onPostExecute(Boolean status) {
		}

	}

}
