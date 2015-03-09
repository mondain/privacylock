package org.gregoire;

import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static String TAG = "privacylock";

	private static final int ADMIN_INTENT = 15;

	private DevicePolicyManager devicePolicyManager;

	private ComponentName adminReceiverName;

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
		// get the button
		Button unlock = (Button) topView.findViewById(R.id.unlockBtn);
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
		// attach listeners to the buttons
		int[] buttons = { R.id.button1, R.id.button2, R.id.button3, R.id.button4, R.id.button5, R.id.button6, R.id.button7, R.id.button8, R.id.button9 };
		for (int button : buttons) {
			((Button) topView.findViewById(button)).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {
					Log.v(TAG, "Button: " + view.getId());
					//((Button) view).setBackgroundColor(getResources().getColor(R.color.ltblue));
					keyPressed(view.getId());
					
				}

			});
		}
		// set the view
		wm.addView(topView, params);
		// focus us
		topView.requestFocus();

		devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		adminReceiverName = new ComponentName(this, AdminReceiver.class);
	}

	/**
	 * Code key pressed.
	 * 
	 * @param id
	 */
	private void keyPressed(int id) {
		switch (id) {
			case R.id.button1:

				break;
			case R.id.button2:

				break;
			case R.id.button3:

				break;
			case R.id.button4:

				break;
			case R.id.button5:

				break;
			case R.id.button6:

				break;
			case R.id.button7:

				break;
			case R.id.button8:

				break;
			case R.id.button9:

				break;
		}
	}

	private void doCodeCheck(View view) {
		Log.v(TAG, "doCodeCheck ");

		doUnlock(view);
	}

	private void doUnlock(View view) {
		Log.v(TAG, "doUnlock ");
		doNotification();
		startAndroidLauncher();
		//Instead of using finish(), this totally destroys the process
		android.os.Process.killProcess(android.os.Process.myPid());
		this.finish();
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

	//	@Override
	//	public void onClick(View v) {
	//		switch (v.getId()) {
	//			case R.id.btnEnable:
	//				Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
	//				intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mComponentName);
	//				intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, description);
	//				startActivityForResult(intent, ADMIN_INTENT);
	//				break;
	//
	//			case R.id.btnDisable:
	//				mDevicePolicyManager.removeActiveAdmin(mComponentName);
	//				Toast.makeText(getApplicationContext(), "Admin registration removed", Toast.LENGTH_SHORT).show();
	//				break;
	//
	//			case R.id.btnLock:
	//				boolean isAdmin = mDevicePolicyManager.isAdminActive(mComponentName);
	//				if (isAdmin) {
	//					mDevicePolicyManager.lockNow();
	//				} else {
	//					Toast.makeText(getApplicationContext(), "Not Registered as admin", Toast.LENGTH_SHORT).show();
	//				}
	//				break;
	//		}
	//	}	

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

	private void startAndroidLauncher() {
		PackageManager pm = getPackageManager();
		Intent i = new Intent("android.intent.action.MAIN");
		i.addCategory("android.intent.category.HOME");
		List<ResolveInfo> lst = pm.queryIntentActivities(i, 0);
		if (lst != null) {
			for (ResolveInfo resolveInfo : lst) {
				try {
					Log.v(TAG, "Package: " + resolveInfo.activityInfo.packageName + " name: " + resolveInfo.activityInfo.name);
					Intent home = new Intent("android.intent.action.MAIN");
					home.addCategory("android.intent.category.HOME");
					home.setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
					startActivity(home);
					break;
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}
	}

}
