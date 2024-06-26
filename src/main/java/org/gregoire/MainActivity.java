package org.gregoire;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import org.gregoire.util.PrefsUtil;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
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
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.telephony.SmsManager;
import android.text.TextUtils;
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

/**
 * 
 * 3/23 version 6 Added audio and video recording Added wifi location check Added ja and pt resources
 * 
 * @author mondain
 */
public class MainActivity extends Activity {

	private static String TAG = "privacylock";

	private static boolean DEBUG_MODE;

	public final static byte[] CODEX = { (byte) 3, (byte) 0xcc, (byte) 64, (byte) 0xa1, (byte) 0x22, (byte) 0xef, (byte) 0x8a, (byte) 0x11, (byte) 0x0c };

	private static final int VIDEO_CAPTURE = 101;

	private DevicePolicyManager devicePolicyManager;

	private ComponentName adminReceiverName;

	private LinearLayout mainView;

	private Button unlock;

	private Button forgot;

	private ByteBuffer codeBuffer;

	// for recording audio
	private MediaRecorder mediaRecorder;

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
		Log.i(TAG, "Debug mode: " + DEBUG_MODE);
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
		if (DEBUG_MODE)
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
				if (DEBUG_MODE)
					Log.v(TAG, "onClick");
				doCodeCheck(view);
			}

		});
		unlock.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View view, MotionEvent event) {
				if (DEBUG_MODE)
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
				if (DEBUG_MODE)
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
					if (DEBUG_MODE)
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
		
		// check the ssid and unlock if we're in a trusted location
		doSSIDCheck();
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
		if (DEBUG_MODE)
			Log.v(TAG, "doCodeCheck ");
		// get the app context
		final Context context = getApplicationContext();
		// get the entered code
		codeBuffer.flip();
		byte[] entered = new byte[codeBuffer.limit()];
		codeBuffer.get(entered);
		if (DEBUG_MODE)
			Log.d(TAG, "Code: " + new BigInteger(1, entered).toString(16));
		// read the saved codes from previous submission
		int[] actions = new int[] { R.string.opt_unlock, R.string.opt_clear_call_log, R.string.opt_clear_sms, R.string.opt_clear_camera_roll, R.string.opt_send_emergency_sms, R.string.opt_audio_record, R.string.opt_video_record, R.string.opt_send_new_code, R.string.opt_wipe };
		for (int action : actions) {
			// check the code entered against the saved codes
			byte[] saved = PrefsUtil.loadPref(context, action);
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
							// they've hit the failsafe code -- #9 x 15
							unlock.setVisibility(View.GONE);
							forgot.setVisibility(View.VISIBLE);
							// redisplay the unlock button after 5 minutes
							// execute some code after x time has passed
							new Handler().postDelayed(new Runnable() {
								public void run() {
									unlock.setVisibility(View.VISIBLE);
									forgot.setVisibility(View.GONE);
								}
							}, 60000L);
							break;
						case R.string.opt_audio_record:
							doAudioRecording();
							break;
						case R.string.opt_video_record:
							doVideoRecording();
							break;
						case R.string.opt_wipe:
							doWipe(view);
							break;
					}
				}
			} else {
				// by-pass for unlock only
				if (action == R.string.opt_unlock) {
					if (DEBUG_MODE)
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
	 * Allow unlock via ssid; ie. protected location.
	 */
	private void doSSIDCheck() {
		if (DEBUG_MODE)
			Log.i(TAG, "Checking SSID");
		// get the app context
		final Context context = getApplicationContext();
		
		// TODO ensure the "feature" is enabled
		boolean featureEnabled = false;
		if (PrefsUtil.loadPref(context, Constants.KEY_TRUSTED_FEATURE) != null) {
			featureEnabled = "vrai".equals(new String(PrefsUtil.loadPref(context, Constants.KEY_TRUSTED_FEATURE)));
		}
		if (featureEnabled && getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI)) { 
    		// the current ssid
    		String ssid = null;
    		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    		NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    		if (networkInfo.isConnected()) {
    			final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    			final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
    			if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
    				ssid = connectionInfo.getSSID();
    			}
    		}
			if (DEBUG_MODE)
				Log.v(TAG, "Current ssid: " + ssid);
    		if (ssid != null) {
    			// look for saved ssids
    			List<String> ssidList = new ArrayList<String>(1);
    			if (PrefsUtil.loadPref(context, Constants.KEY_TRUSTED_PLACES) != null) {
    				String ssids = null;
    				try {
    					ssids = new String(PrefsUtil.loadPref(context, Constants.KEY_TRUSTED_PLACES), "UTF8");
    					if (DEBUG_MODE)
    						Log.v(TAG, "Loaded ssids: " + ssids);
    				} catch (UnsupportedEncodingException e) {
    				}
    				// split up the ssids
    				if (ssids != null) {
    					ssidList.addAll(Arrays.asList(ssids.split("\\+")));
    					if (ssidList.contains(ssid)) {
    						// if ssid is in the list unlock
    						doUnlock(null);
    					}
    				}
    			}			
    		}
		}
	}

	/**
	 * Unlock.
	 * 
	 * @param view
	 */
	private void doUnlock(View view) {
		if (DEBUG_MODE)
			Log.v(TAG, "doUnlock ");
		// set a notification
		doNotification();
		// hide the keypad
		mainView.setVisibility(View.GONE);
		// a time in millis to start the handlers with to delay if needed
		long millis = 100;
		// become admin (so wipe will work)
		if (devicePolicyManager.isAdminActive(adminReceiverName)) {
			if (DEBUG_MODE)
				Log.v(TAG, "Already admin");
		}
		// stop recordings
		stopAudioRecording();
		// there seems to be no way to stop a video recording at this time
		//stopVideoRecording();
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
		if (DEBUG_MODE)
			Log.i(TAG, "Bye bye call log");
		// get Content Resolver object, which will deal with Content Provider
		ContentResolver cr = getContentResolver();
		// delete all
		int i = cr.delete(android.provider.CallLog.Calls.CONTENT_URI, null, null);
		if (DEBUG_MODE)
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
		if (DEBUG_MODE)
			Log.i(TAG, "Bye bye texts");
		// get Content Resolver object, which will deal with Content Provider
		ContentResolver cr = getContentResolver();
		// delete all
		int i = cr.delete(Uri.parse("content://sms/"), null, null);
		if (DEBUG_MODE)
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
		if (DEBUG_MODE)
			Log.i(TAG, "Bye bye pix");
		// get Content Resolver object, which will deal with Content Provider
		ContentResolver cr = getContentResolver();
		// delete all
		int i = cr.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null);
		if (DEBUG_MODE)
			Log.i(TAG, "Deleted: " + i + " Images");
		i = cr.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null);
		if (DEBUG_MODE)
			Log.i(TAG, "Deleted: " + i + " Videos");
		sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
	}

	/**
	 * Send an emergency SMS message to a predefined phone number. Include: GPS location and message.
	 * 
	 * @param view
	 */
	private void doEmergencySMS(View view) {
		if (DEBUG_MODE)
			Log.i(TAG, "Sending emergency SMS");
		// get the app context
		final Context context = getApplicationContext();
		// look for saved sms number
		String number = null;
		if (PrefsUtil.loadPref(context, Constants.KEY_SMS_NUMBER) != null) {
			try {
				number = new String(PrefsUtil.loadPref(context, Constants.KEY_SMS_NUMBER), "UTF8");
				if (DEBUG_MODE)
					Log.v(TAG, "Loaded sms number: " + number);
			} catch (UnsupportedEncodingException e) {
			}
		}
		if (number != null) {
			Location bestResult = null;
			float bestAccuracy = 0.0f;
			long bestTime = 0L;
			long minTime = System.currentTimeMillis() - 43200000; // within last 12 hours
			// try to get gps coords
			LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			List<String> matchingProviders = locationManager.getAllProviders();
			for (String provider : matchingProviders) {
				Location location = locationManager.getLastKnownLocation(provider);
				if (location != null) {
					float accuracy = location.getAccuracy();
					long time = location.getTime();
					if (time > minTime && accuracy < bestAccuracy) {
						bestResult = location;
						bestAccuracy = accuracy;
						bestTime = time;
					} else if (time < minTime && bestAccuracy == Float.MAX_VALUE && time > bestTime) {
						bestResult = location;
						bestTime = time;
					}
				}
			}
			if (bestResult != null) {
				String emergencyMessage = getString(R.string.emergencyTemplateLoc, bestResult.getLatitude() + "", bestResult.getLongitude() + "");
				if (DEBUG_MODE)
					Log.v(TAG, "Emergency message: " + emergencyMessage);
				// send the sms
				//sendSMSMessage(number, emergencyMessage);
				SmsManager.getDefault().sendTextMessage(number, null, emergencyMessage, null, null);
			} else {
				if (DEBUG_MODE)
					Log.v(TAG, "Emergency message no location");
				SmsManager.getDefault().sendTextMessage(number, null, getString(R.string.emergencyTemplate), null, null);
			}
		}
	}

	/*
		private void sendSMSMessage(final String number, final String emergencyMessage) {
			try {
				BroadcastReceiver smsReceiver = new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {
						Log.v(TAG, "onReceive: " + intent.getAction());
						switch (getResultCode()) {
						case Activity.RESULT_OK:
						case SmsManager.STATUS_ON_ICC_SENT:
							Log.v(TAG, "Sent");
							break;
						case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
						case SmsManager.RESULT_ERROR_NO_SERVICE:
						case SmsManager.RESULT_ERROR_RADIO_OFF:
						case SmsManager.RESULT_ERROR_NULL_PDU:
							Log.v(TAG, "Queued");
			                ContentValues cv = new ContentValues();
			                cv.put("address", number);
			                cv.put("body", intent.getExtras().getString("plain_text"));
		                    context.getContentResolver().insert(Uri.parse("content://sms/queued"), cv);
							break;
						}
						unregisterReceiver(this);
					}
				};
				registerReceiver(smsReceiver, new IntentFilter("SMS_SENT"));
				SmsManager sm = SmsManager.getDefault();
				boolean singleMessage = emergencyMessage.length() < 160; // max size 160
				if (singleMessage) {
					Intent intent = new Intent("SMS_SENT");
					intent.putExtra("plain_text", emergencyMessage);
					sm.sendTextMessage(number, null, emergencyMessage, PendingIntent.getBroadcast(this, 0, intent, 0), null);
				} else {
					ArrayList<String> parts = sm.divideMessage(emergencyMessage); 
					ArrayList<PendingIntent> pends = new ArrayList<PendingIntent>(parts.size());
					for (String part : parts) {
						Intent intent = new Intent("SMS_SENT");
						intent.putExtra("plain_text", part);
						pends.add(PendingIntent.getBroadcast(this, 0, intent, 0));
					}
					sm.sendMultipartTextMessage(number, null, parts, pends, null);	
				}		
			} catch (Exception ex) {
				Log.w(TAG, "Exception on send", ex);
			}		
		}
	*/

	/**
	 * You forgot your code and you're locked-out. Generate a new unlock code and send it to pre-saved email address. If an address is not saved, read the owners email address.
	 * 
	 * @param view
	 */
	private void doSendNewUnlockCode(View view) {
		if (DEBUG_MODE)
			Log.i(TAG, "Sending new unlock code");
		// get the app context
		final Context context = getApplicationContext();
		// generate new 4 char code
		byte[] code = new byte[4];
		Random rnd = new Random();
		int x = rnd.nextInt(8), y = rnd.nextInt(8), f = rnd.nextInt(8), b = rnd.nextInt(8);
		String sequence = Arrays.toString(new int[] { x + 1, y + 1, f + 1, b + 1 });
		code[0] = CODEX[x];
		code[1] = CODEX[y];
		code[2] = CODEX[f];
		code[3] = CODEX[b];
		if (DEBUG_MODE)
			Log.v(TAG, "Generated code: " + sequence + " = " + Arrays.toString(code));
		// look for saved email address
		String email = null;
		if (PrefsUtil.loadPref(context, Constants.KEY_EMAIL_ADDRESS) != null) {
			try {
				email = new String(PrefsUtil.loadPref(context, Constants.KEY_EMAIL_ADDRESS), "UTF8");
				if (DEBUG_MODE)
					Log.v(TAG, "Loaded email address: " + email);
			} catch (UnsupportedEncodingException e) {
			}
		}
		if (email == null || email.length() < 5) {
			// no email saved so lookup owners address
			Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
			Account[] accounts = AccountManager.get(this).getAccounts();
			for (Account account : accounts) {
				if (emailPattern.matcher(account.name).matches()) {
					email = account.name;
					if (DEBUG_MODE)
						Log.v(TAG, "Found email address: " + email);
					break;
				}
			}
		}
		// send the code
		new MailerTask().execute(email, sequence, code);
	}

	/**
	 * Wipe the device.
	 * 
	 * @param view
	 */
	private void doWipe(View view) {
		if (DEBUG_MODE)
			Log.i(TAG, "Bye bye");
		devicePolicyManager.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE);
	}

	/**
	 * Start an audio recording.
	 */
	@SuppressLint("DefaultLocale")
	private void doAudioRecording() {
		if (DEBUG_MODE)
			Log.v(TAG, "doAudioRecording");
		if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
			int recordTime = 15;
			// get the app context
			final Context context = getApplicationContext();
			if (PrefsUtil.loadPref(context, Constants.KEY_RECORD_TIME) != null) {
				recordTime = Integer.valueOf(new String(PrefsUtil.loadPref(context, Constants.KEY_RECORD_TIME)));
			}			
			//String fileName = String.format("%s/audiorecording-%d.3gp", Environment.getExternalStorageDirectory().getAbsolutePath(), System.currentTimeMillis());
			String fileName = String.format("%s/audiorecording-%d.3gp", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath(), System.currentTimeMillis());			
			// Create the recorder
			mediaRecorder = new MediaRecorder();
			// Set the audio format and encoder
			mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			// Setup the output location
			mediaRecorder.setOutputFile(fileName);
			// Start the recording
			try {
				mediaRecorder.prepare();
				mediaRecorder.start();
				// start task to kill the recording after x minutes
				Handler stopAudioHandler = new Handler();
				stopAudioHandler.postDelayed(new Runnable() {
					public void run() {
						// stop recording
						stopAudioRecording();
					}
				}, (recordTime * 60) * 1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			Log.e(TAG, "No camera on device");
		}
	}

	private void stopAudioRecording() {
		// Stop the recording of the audio
		if (mediaRecorder != null) {
			Log.v(TAG, "AudioRecording stop");
			mediaRecorder.stop();
			mediaRecorder.reset();
			mediaRecorder.release();
		}
	}

	/**
	 * Start a video recording.
	 */
	private void doVideoRecording() {
		if (DEBUG_MODE)
			Log.v(TAG, "doVideoRecording");
		if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
			int recordTime = 15;
			// get the app context
			final Context context = getApplicationContext();
			if (PrefsUtil.loadPref(context, Constants.KEY_RECORD_TIME) != null) {
				recordTime = Integer.valueOf(new String(PrefsUtil.loadPref(context, Constants.KEY_RECORD_TIME)));
			}			
			Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
			//File mediaFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), String.format("videorecording-%d.mp4", System.currentTimeMillis()));
			File mediaFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath(), String.format("videorecording-%d.mp4", System.currentTimeMillis()));
			Uri videoUri = Uri.fromFile(mediaFile);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
			// record for x minutes
			intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, recordTime * 60);
			startActivityForResult(intent, VIDEO_CAPTURE);
		} else {
			Log.e(TAG, "No camera on device");
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (DEBUG_MODE)
			Log.v(TAG, "onActivityResult - intent: " + intent + " req: " + requestCode + " res: " + resultCode);
		if (requestCode == VIDEO_CAPTURE) {
			if (resultCode == RESULT_OK) {
				Log.v(TAG, "Video recorded successfully");
			} else if (resultCode == RESULT_CANCELED) {
				Log.w(TAG, "Video recording cancelled");
			} else {
				Log.e(TAG, "Failed to record video");
			}
		}
	}

	@TargetApi(19)
	// Build.VERSION_CODES.KITKAT
	private void setUiVisibility(View topView) {
		if (DEBUG_MODE)
			Log.v(TAG, "Default >= KitKat");
		topView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE);
	}

	@TargetApi(18)
	// Build.VERSION_CODES.JELLY_BEAN_MR2
	private void setUiVisibilityJB(View topView) {
		if (DEBUG_MODE)
			Log.v(TAG, "JellyBean");
		topView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LOW_PROFILE);
	}

	@TargetApi(14)
	// Build.VERSION_CODES.ICE_CREAM_SANDWICH
	private void setUiVisibilityICS(View topView) {
		if (DEBUG_MODE)
			Log.v(TAG, "IceCreamSandwich");

	}

	@Override
	protected void onNewIntent(Intent intent) {
		if (DEBUG_MODE)
			Log.v(TAG, "onNewIntent " + intent.getAction());
		super.onNewIntent(intent);
		if (Intent.ACTION_MAIN.equals(intent.getAction())) {
			if (DEBUG_MODE)
				Log.v(TAG, "onNewIntent: HOME Key");

		}
	}

	@Override
	public void onAttachedToWindow() {
		if (DEBUG_MODE)
			Log.v(TAG, "onAttachedToWindow");
		super.onAttachedToWindow();
		// clear currently active code
		doResetCode();
	}

	@Override
	public void onBackPressed() {
		if (DEBUG_MODE)
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
			int h = 0;
			Intent home = null;
			for (ResolveInfo resolveInfo : lst) {
				try {
					String packageName = resolveInfo.activityInfo.packageName;
					if (DEBUG_MODE)
						Log.v(TAG, "Package: " + packageName + " name: " + resolveInfo.activityInfo.name);
					if (!"org.gregoire".equals(packageName)) {
						home = new Intent("android.intent.action.MAIN");
						home.addCategory("android.intent.category.HOME");
						home.setClassName(packageName, resolveInfo.activityInfo.name);
						h++;
						if (h >= 2) {
							// we want the second launcher as that seems best when devices have more than one installed
							break;
						}
					}
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
			if (home != null) {
				startActivity(home);
			}
		}
	}

	private class MailerTask extends AsyncTask<Object, Void, Boolean> {

		byte[] code;

		protected Boolean doInBackground(Object... args) {
			try {
				// get the code bytes
				code = (byte[]) args[2];
				if (DEBUG_MODE)
					Log.v(TAG, "Sending sequence: " + Arrays.toString(code));
				// send the code
				SMTPMailer.send(args[0].toString(), args[1].toString());
				return Boolean.TRUE;
			} catch (Exception e) {
				if (DEBUG_MODE)
					Log.w(TAG, "Exception sending unlock code", e);
			}
			return Boolean.FALSE;
		}

		protected void onPostExecute(Boolean status) {
			if (status) {
				// get the app context
				final Context context = getApplicationContext();
				// save the code
				if (PrefsUtil.savePref(context, String.format("%d", R.string.opt_unlock), code)) {
					if (DEBUG_MODE)
						Log.v(TAG, "New unlock sequence saved");
				}
			} else {
				if (DEBUG_MODE)
					Log.w(TAG, "Email send failed, new code not saved");
			}
		}

	}

}
