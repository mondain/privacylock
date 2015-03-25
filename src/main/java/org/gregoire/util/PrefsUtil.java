package org.gregoire.util;

import java.util.Arrays;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

public class PrefsUtil {

	private static String TAG = "PrefsUtil";

	private static final String PREF_EMPTY_STRING = "";

	public final static byte[] loadPref(Context context, int id) {
		return loadPref(context, String.format("%d", id));
	}
	
	public final static byte[] loadPref(Context context, String key) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String str = prefs.getString(key, PREF_EMPTY_STRING);
		Log.v(TAG, "Preference - key: " + key + " value: " + str);
		byte[] value = null;
		if (!TextUtils.isEmpty(str)) {
			value = Base64.decode(str, Base64.NO_WRAP | Base64.NO_PADDING);
			Log.v(TAG, "" + Arrays.toString(value));
		}
		return value;
	}

	public final static boolean savePref(Context context, int key, byte[] val) {
		return savePref(context, String.format("%d", key), val);
	}

	public final static boolean savePref(Context context, String key, byte[] val) {
		if (val.length > 0) {
    		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    		String str = Base64.encodeToString(val, Base64.NO_WRAP | Base64.NO_PADDING);
    		SharedPreferences.Editor editor = prefs.edit();
    		editor.putString(key, str);
    		return editor.commit();
		}
		return false;
	}

	public final static boolean removePref(Context context, int id) {
		return removePref(context, String.format("%d", id));
	}

	public final static boolean removePref(Context context, String key) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.remove(key);
		return editor.commit();
	}

}
