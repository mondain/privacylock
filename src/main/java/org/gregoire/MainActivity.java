package org.gregoire;

import org.gregoire.compat.Config;
import org.gregoire.compat.DefaultConfig;
import org.gregoire.compat.ICSConfig;
import org.gregoire.compat.JellyBeanConfig;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static String TAG = "privacylock";

	private static final int ADMIN_INTENT = 15;

	private DevicePolicyManager devicePolicyManager;

	private ComponentName adminReceiverName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// set up our lockscreen - A simple method that sets the screen to fullscreen. It removes the Notifications bar, the Actionbar and the virtual keys (if they are on the phone)
		this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		Config config = null;
		if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) { // < 14
			config = new ICSConfig();
		} else if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) { // < 19
			config = new JellyBeanConfig();
		} else {
			config = new DefaultConfig();
		}
		config.doConfig(this);
		startService(new Intent(this, LockScreenService.class));

		setContentView(R.layout.activity_main);
		devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		adminReceiverName = new ComponentName(this, AdminReceiver.class);
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
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.v(TAG, "onKeyDown " + event);
		if (keyCode == KeyEvent.KEYCODE_HOME) {
			Log.v(TAG, "HOME");
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		Log.v(TAG, "onBackPressed ");
		return; //Do nothing!
	}

	public void unlockScreen(View view) {
		Log.v(TAG, "unlockScreen ");
		//Instead of using finish(), this totally destroys the process
		android.os.Process.killProcess(android.os.Process.myPid());
		this.finish();
	}

}
