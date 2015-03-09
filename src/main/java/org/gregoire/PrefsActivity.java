package org.gregoire;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class PrefsActivity extends Activity {

	private static String TAG = "PrefsActivity";

	private static final String PREF_EMPTY_STRING = "";

	private ByteBuffer codeBuffer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_prefs);	
		
		final ListView listview = (ListView) findViewById(R.id.listView1);
		String[] values = new String[] { getString(R.string.opt_unlock), getString(R.string.opt_clear_call_log), getString(R.string.opt_clear_sms), getString(R.string.opt_clear_camera_roll), getString(R.string.opt_send_emergency_sms), getString(R.string.opt_wipe) };

		final ArrayList<String> list = new ArrayList<String>();
		for (int i = 0; i < values.length; ++i) {
			list.add(values[i]);
		}
		final PrefArrayAdapter adapter = new PrefArrayAdapter(this, android.R.layout.simple_list_item_1, list);
		listview.setAdapter(adapter);
		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
				final String item = (String) parent.getItemAtPosition(position);
				Log.v(TAG, "Selected: " + item);
				if (savePref("test", new byte[]{(byte) 0x01, (byte) 0x05})) {
					Log.v(TAG, "Test pref saved");
				}
			}

		});
	}

	private class PrefArrayAdapter extends ArrayAdapter<String> {

		HashMap<String, Integer> map = new HashMap<String, Integer>();

		public PrefArrayAdapter(Context context, int textViewResourceId, List<String> objects) {
			super(context, textViewResourceId, objects);
			for (int i = 0; i < objects.size(); ++i) {
				map.put(objects.get(i), i);
			}
		}

		@Override
		public long getItemId(int position) {
			String item = getItem(position);
			return map.get(item);
		}

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

}
