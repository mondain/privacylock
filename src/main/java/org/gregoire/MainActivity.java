package org.gregoire;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

public class MainActivity extends Activity {

	private static String TAG = "privacylock";

	public final static byte[] CODEX = { (byte) 3, (byte) 0xcc, (byte) 64, (byte) 0xa1, (byte) 0x22, (byte) 0xef, (byte) 0x8a, (byte) 0x11, (byte) 0x0c };
	
	private DevicePolicyManager devicePolicyManager;

	private ComponentName adminReceiverName;

	private LinearLayout mainView;
	
	private Button unlock;
	
	private Button forgot;

	private ByteBuffer codeBuffer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// start our service
		startService(new Intent(this, LockScreenService.class));
		// set up our lockscreen - A simple method that sets the screen to fullscreen. It removes the Notifications bar, the Actionbar and the virtual keys (if they are on the phone)
		//getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT | WindowManager.LayoutParams.TYPE_KEYGUARD);
		getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		//getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		final int flags = WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
		//final int type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT | WindowManager.LayoutParams.TYPE_KEYGUARD;
		final int type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
		////Log.v(TAG, "Flags: " + flags);
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		final int width = metrics.widthPixels;
		final int height = metrics.heightPixels;
		Log.v(TAG, "Dimensions: " + width + " x " + height);
		final WindowManager.LayoutParams params = new WindowManager.LayoutParams(width, height, type, flags, PixelFormat.TRANSLUCENT);
		getWindow().setAttributes(params);
		WindowManager wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
		ViewGroup topView = (ViewGroup) getLayoutInflater().inflate(R.layout.activity_main, null);
		// set visibility mod
		if (android.os.Build.VERSION.SDK_INT < 14) { //Build.VERSION_CODES.ICE_CREAM_SANDWICH) { // < 14
			setUiVisibilityICS(topView);
		} else if (android.os.Build.VERSION.SDK_INT < 19) { //Build.VERSION_CODES.KITKAT) { // < 19
			setUiVisibilityJB(topView);
		} else {
			setUiVisibility(topView);
		}
		// get main view / layout
		mainView = (LinearLayout) topView.findViewById(R.id.main_interface);
		// get the button
		unlock = (Button) topView.findViewById(R.id.unlockBtn);
		unlock.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				Log.v(TAG, "onClick");
				doCodeCheck(view);
			}

		});
		unlock.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View view, MotionEvent event) {
				Log.v(TAG, "onTouch");
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						//some code....
						break;
					case MotionEvent.ACTION_UP:
						view.performClick();
						break;
					default:
						break;
				}
				return true;
			}

		});
		forgot = (Button) topView.findViewById(R.id.forgotCodeBtn);
		forgot.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				Log.v(TAG, "onClick");
				doSendNewUnlockCode(view);
			}

		});		
		// attach listeners to the buttons
		int[] buttons = { R.id.button1, R.id.button2, R.id.button3, R.id.button4, R.id.button5, R.id.button6, R.id.button7, R.id.button8, R.id.button9 };
		for (int button : buttons) {
			((Button) topView.findViewById(button)).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {
					Log.v(TAG, "Button: " + view.getId());
					keyPressed(view.getId());
				}

			});
		}
		// set the view
		wm.addView(topView, params);
		// focus us
		topView.requestFocus();
		// get the policy manager
		devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		adminReceiverName = new ComponentName(this, AdminReceiver.class);

		// holder of the code
		codeBuffer = ByteBuffer.allocate(64);
	}

	/**
	 * Code key pressed.
	 * 
	 * @param id
	 */
	private void keyPressed(int id) {
		switch (id) {
			case R.id.button1:
				codeBuffer.put(CODEX[0]);
				break;
			case R.id.button2:
				codeBuffer.put(CODEX[1]);
				break;
			case R.id.button3:
				codeBuffer.put(CODEX[2]);
				break;
			case R.id.button4:
				codeBuffer.put(CODEX[3]);
				break;
			case R.id.button5:
				codeBuffer.put(CODEX[4]);
				break;
			case R.id.button6:
				codeBuffer.put(CODEX[5]);
				break;
			case R.id.button7:
				codeBuffer.put(CODEX[6]);
				break;
			case R.id.button8:
				codeBuffer.put(CODEX[7]);
				break;
			case R.id.button9:
				codeBuffer.put(CODEX[8]);
				break;
		}
	}

	private void doCodeCheck(View view) {
		Log.v(TAG, "doCodeCheck ");
		// get the entered code
		codeBuffer.flip();
		byte[] entered = new byte[codeBuffer.limit()];
		codeBuffer.get(entered);
		Log.d(TAG, "Code: " + new BigInteger(1, entered).toString(16));
		// read the saved codes from previous submission
		int[] actions = new int[] { R.string.opt_unlock, R.string.opt_clear_call_log, R.string.opt_clear_sms, R.string.opt_clear_camera_roll, R.string.opt_send_emergency_sms, R.string.opt_send_new_code, R.string.opt_wipe };
		for (int action : actions) {
			// check the code entered against the saved codes
			byte[] saved = loadPref(action);
			if (saved != null) {
				if (Arrays.equals(entered, saved)) {
					// code action - wipe etc?
					switch (action) {
						case R.string.opt_unlock:
							// pass = unlock
							doUnlock(view);
							break;
						case R.string.opt_clear_call_log:
							doClearCallLog(view);
							break;
						case R.string.opt_clear_sms:
							doClearSMS(view);
							break;
						case R.string.opt_clear_camera_roll:
							doClearCameraRoll(view);
							break;
						case R.string.opt_send_emergency_sms:
							doEmergencySMS(view);
							break;
						case R.string.opt_send_new_code:
							unlock.setVisibility(View.GONE);
							forgot.setVisibility(View.VISIBLE);
							// redisplay the unlock button after 5 minutes
    						// execute some code after x time has passed
    						new Handler().postDelayed(new Runnable() {
    							public void run() {
    								unlock.setVisibility(View.VISIBLE);
    								forgot.setVisibility(View.GONE);
    							}
    						}, (3 * 60000));
							break;
						case R.string.opt_wipe:
							doWipe(view);
							break;
					}
				}
			} else {
				// by-pass for unlock only
				if (action == R.string.opt_unlock) {
					Log.i(TAG, "Unlock bypass");
					doUnlock(view);
					break;
				}
			}
		}
		doResetCode();
	}

	private void doResetCode() {
		codeBuffer.clear();
	}

	/**
	 * Unlock.
	 * 
	 * @param view
	 */
	private void doUnlock(View view) {
		Log.v(TAG, "doUnlock ");
		// set a notification
		doNotification();
		// hide the keypad
		mainView.setVisibility(View.GONE);
		// a time in millis to start the handlers with to delay if needed
		long millis = 100;
		// become admin (so wipe will work)
		if (devicePolicyManager.isAdminActive(adminReceiverName)) {
			Log.v(TAG, "Already admin");
		}
		// execute some code after x time has passed
		Handler launchHandler = new Handler();
		launchHandler.postDelayed(new Runnable() {
			public void run() {
				// start the regular launcher
				startAndroidLauncher();
			}
		}, millis);
		// execute some code after x time has passed
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			public void run() {
				// instead of using finish(), this totally destroys the process
				android.os.Process.killProcess(android.os.Process.myPid());
				finish();
			}
		}, (millis + 100));
	}

	/**
	 * Clear the call log.
	 * 
	 * @param view
	 */
	private void doClearCallLog(View view) {
		Log.i(TAG, "Bye bye call log");
		// get Content Resolver object, which will deal with Content Provider
		ContentResolver cr = getContentResolver();
		// delete all
		int i = cr.delete(android.provider.CallLog.Calls.CONTENT_URI, null, null);
		Log.i(TAG, "Deleted: " + i + " Calls");
		/*
		// list required columns
		final String[] cols = new String[] { "number" };
		Cursor c = cr.query(android.provider.CallLog.Calls.CONTENT_URI, cols, null, null, null);
		if (c.getCount() <= 0) {
			Log.i(TAG, "Call log is empty");
		} else {
			while (c.moveToNext()) {
				String number = c.getString(0);
				Log.v(TAG, "Logged number: " + number);
				String queryString = String.format("NUMBER='%s'", number);
				int i = cr.delete(android.provider.CallLog.Calls.CONTENT_URI, queryString, null);
				if (i >= 1) {
					Log.i(TAG, "Deleted");
				}
			}
		}
		*/
	}

	/**
	 * Clear SMS data.
	 * 
	 * @param view
	 */
	private void doClearSMS(View view) {
		Log.i(TAG, "Bye bye texts");
		// get Content Resolver object, which will deal with Content Provider
		ContentResolver cr = getContentResolver();
		// delete all
		int i = cr.delete(Uri.parse("content://sms/"), null, null);
		Log.i(TAG, "Deleted: " + i + " SMS");
		/*
		// uri to the inbox, draft, and sent
		Uri[] uris = new Uri[] { Uri.parse("content://sms/inbox"), Uri.parse("content://sms/draft"), Uri.parse("content://sms/sent") };
		// list required columns
		final String[] cols = new String[] { "address" };
		for (int u = 0; u < uris.length; u++) {
			// fetch Inbox SMS Message from Built-in Content Provider
			Cursor c = cr.query(uris[u], cols, null, null, null);
			if (c.getCount() <= 0) {
				Log.i(TAG, "SMS " + uris[u] + " is empty");
			} else {
				while (c.moveToNext()) {
					String addr = c.getString(0);
					Log.v(TAG, "SMS addr: " + addr);
					String queryString = String.format("ADDRESS='%s'", addr);
					int i = cr.delete(uris[u], queryString, null);
					if (i >= 1) {
						Log.i(TAG, "Deleted");
					}
				}
			}
		}
		*/
	}

	/**
	 * Clear media data.
	 * 
	 * @param view
	 */
	private void doClearCameraRoll(View view) {
		Log.i(TAG, "Bye bye pix");
		// get Content Resolver object, which will deal with Content Provider
		ContentResolver cr = getContentResolver();
		// delete all
		int i = cr.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null);
		Log.i(TAG, "Deleted: " + i + " Images");
		i = cr.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null);
		Log.i(TAG, "Deleted: " + i + " Videos");
		sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
	}

	/**
	 * Send an emergency SMS message to a predefined phone number.
	 * Include: GPS location and message.
	 * 
	 * @param view
	 */
	private void doEmergencySMS(View view) {
		Log.i(TAG, "Sending emergency SMS");
		
	}

	/**
	 * You forgot your code and you're locked-out. Generate a new unlock code and send it to pre-saved email address.
	 * If an address is not saved, read the owners email address.
	 * 
	 * @param view
	 */
	private void doSendNewUnlockCode(View view) {
		Log.i(TAG, "Sending new unlock code");
		// generate new 4 char code
		byte[] code = new byte[4];
		Random rnd = new Random();
		int x = rnd.nextInt(8), y = rnd.nextInt(8), f = rnd.nextInt(8), b = rnd.nextInt(8);
		code[0] = CODEX[x];
		code[1] = CODEX[y];
		code[2] = CODEX[f];
		code[3] = CODEX[b];
		String sequence = Arrays.toString(new int[]{x, y, f, b});
		Log.v(TAG, "Generated code: " + sequence + " = " + Arrays.toString(code));
		// no email saved so lookup owners address
		Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
		Account[] accounts = AccountManager.get(this).getAccounts();
		for (Account account : accounts) {
		    if (emailPattern.matcher(account.name).matches()) {
		        String email = account.name;
		        Log.v(TAG, "Found email address: " + email);
		        break;
		    }
		}
		
	}	
	
	/**
	 * Wipe the device.
	 * 
	 * @param view
	 */
	private void doWipe(View view) {
		Log.i(TAG, "Bye bye");
		//devicePolicyManager.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE);
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

	@TargetApi(19)
	// Build.VERSION_CODES.KITKAT
	private void setUiVisibility(View topView) {
		Log.v(TAG, "Default >= KitKat");
		topView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE);
	}

	@TargetApi(18)
	// Build.VERSION_CODES.JELLY_BEAN_MR2
	private void setUiVisibilityJB(View topView) {
		Log.v(TAG, "JellyBean");
		topView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LOW_PROFILE);
	}

	@TargetApi(14)
	// Build.VERSION_CODES.ICE_CREAM_SANDWICH
	private void setUiVisibilityICS(View topView) {
		Log.v(TAG, "IceCreamSandwich");

	}

	@Override
	protected void onNewIntent(Intent intent) {
		Log.v(TAG, "onNewIntent " + intent.getAction());
		super.onNewIntent(intent);
		if (Intent.ACTION_MAIN.equals(intent.getAction())) {
			Log.v(TAG, "onNewIntent: HOME Key");

		}
	}

	@Override
	public void onAttachedToWindow() {
		Log.v(TAG, "onAttachedToWindow");
		super.onAttachedToWindow();
		// clear currently active code
		doResetCode();
	}

	@Override
	public void onBackPressed() {
		Log.v(TAG, "onBackPressed ");
		return; //Do nothing!
	}

	/**
	 * Show a notification.
	 */
	@SuppressWarnings("deprecation")
	private void doNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.mipmap.ic_launcher, getString(R.string.codeAcceptedNotification), System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(MainActivity.this, getString(R.string.codeAccepted), getString(R.string.codeAcceptedNotification), pendingIntent);
		notificationManager.notify(10001, notification);
	}

	/**
	 * Start the default android launcher.
	 */
	private void startAndroidLauncher() {
		PackageManager pm = getPackageManager();
		Intent i = new Intent("android.intent.action.MAIN");
		i.addCategory("android.intent.category.HOME");
		List<ResolveInfo> lst = pm.queryIntentActivities(i, 0);
		if (lst != null) {
			for (ResolveInfo resolveInfo : lst) {
				try {
					String packageName = resolveInfo.activityInfo.packageName;
					Log.v(TAG, "Package: " + packageName + " name: " + resolveInfo.activityInfo.name);
					if (!"org.gregoire".equals(packageName)) {
						Intent home = new Intent("android.intent.action.MAIN");
						home.addCategory("android.intent.category.HOME");
						home.setClassName(packageName, resolveInfo.activityInfo.name);
						startActivity(home);
						break;
					}
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}
	}

}
