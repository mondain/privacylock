package org.gregoire;

import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
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
				unlockScreen(view);
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
		// set the view
		wm.addView(topView, params);
		// focus us
		topView.requestFocus();

		devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		adminReceiverName = new ComponentName(this, AdminReceiver.class);
	}

	@TargetApi(19) // Build.VERSION_CODES.KITKAT
	private void setUiVisibility(View topView) {
		Log.v(TAG, "Default >= KitKat");
		topView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE);
	}

	@TargetApi(18) // Build.VERSION_CODES.JELLY_BEAN_MR2
 	private void setUiVisibilityJB(View topView) {
		Log.v(TAG, "JellyBean");
		topView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LOW_PROFILE);
	}

	@TargetApi(14) // Build.VERSION_CODES.ICE_CREAM_SANDWICH
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

	public void unlockScreen(View view) {
		Log.v(TAG, "unlockScreen ");
		startAndroidLauncher();
		//Instead of using finish(), this totally destroys the process
		android.os.Process.killProcess(android.os.Process.myPid());
		this.finish();
	}

}
