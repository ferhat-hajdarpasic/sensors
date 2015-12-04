/**************************************************************************************************
  Filename:       DeviceActivity.java
  Revised:        $Date: Wed Apr 22 13:01:34 2015 +0200$
  Revision:       $Revision: 599e5650a33a4a142d060c959561f9e9b0d88146$

  Copyright (c) 2013 - 2014 Texas Instruments Incorporated

  All rights reserved not granted herein.
  Limited License. 

  Texas Instruments Incorporated grants a world-wide, royalty-free,
  non-exclusive license under copyrights and patents it now or hereafter
  owns or controls to make, have made, use, import, offer to sell and sell ("Utilize")
  this software subject to the terms herein.  With respect to the foregoing patent
  license, such license is granted  solely to the extent that any such patent is necessary
  to Utilize the software alone.  The patent license shall not apply to any combinations which
  include this software, other than combinations with devices manufactured by or for TI ('TI Devices').
  No hardware patent is licensed hereunder.

  Redistributions must preserve existing copyright notices and reproduce this license (including the
  above copyright notice and the disclaimer and (if applicable) source code license limitations below)
  in the documentation and/or other materials provided with the distribution

  Redistribution and use in binary form, without modification, are permitted provided that the following
  conditions are met:

 * No reverse engineering, decompilation, or disassembly of this software is permitted with respect to any
      software provided in binary form.
 * any redistribution and use are licensed by TI for use only with TI Devices.
 * Nothing shall obligate TI to provide you with source code for the software licensed and provided to you in object code.

  If software source code is provided to you, modification and redistribution of the source code are permitted
  provided that the following conditions are met:

 * any redistribution and use of the source code, including any resulting derivative works, are licensed by
      TI for use only with TI Devices.
 * any redistribution and use of any object code compiled from the source code and any resulting derivative
      works, are licensed by TI for use only with TI Devices.

  Neither the name of Texas Instruments Incorporated nor the names of its suppliers may be used to endorse or
  promote products derived from this software without specific prior written permission.

  DISCLAIMER.

  THIS SOFTWARE IS PROVIDED BY TI AND TI'S LICENSORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
  BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL TI AND TI'S LICENSORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
  POSSIBILITY OF SUCH DAMAGE.


 **************************************************************************************************/
package com.example.ti.ble.sensortag;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;


import com.example.ti.ble.btsig.profiles.DeviceInformationServiceProfile;
import com.example.ti.ble.common.AzureIoTCloudProfile;
import com.example.ti.ble.common.BluetoothLeService;
import com.example.ti.ble.common.GattInfo;
import com.example.ti.ble.common.GenericBluetoothProfile;
import com.example.ti.ble.ti.profiles.TIOADProfile;



@SuppressLint("InflateParams")
public class DeviceActivity extends ViewPagerActivity {
	// Log
	// private static String TAG = "DeviceActivity";

	// Activity
	public static final String EXTRA_DEVICE = "EXTRA_DEVICE";
	private static final int PREF_ACT_REQ = 0;
	static final int FWUPDATE_ACT_REQ = 1;

	DeviceView mDeviceView = null;

	// BLE
	BluetoothLeService mBtLeService = null;
	BluetoothDevice mBluetoothDevice = null;
	BluetoothGatt mBtGatt = null;
	private List<BluetoothGattService> mServiceList = null;
	boolean mServicesRdy = false;
	private boolean mIsReceiving = false;
    AzureIoTCloudProfile mqttProfile;

	// SensorTagGatt
	BluetoothGattService mOadService = null;
	BluetoothGattService mConnControlService = null;
	private boolean mIsSensorTag2;
	public ProgressDialog progressDialog;

	//GUI
	List<GenericBluetoothProfile> mProfiles;
	private DeviceActivityBroadcastReceiver mGattUpdateReceiver;
	public SensorTagIoProfile mSensorTagIoProfile;
	private SensorTagIoProfile sensorTagIoProfile;

	public DeviceActivity() {
		mResourceFragmentPager = R.layout.fragment_pager;
		mResourceIdPager = R.id.pager;
	}

	public static DeviceActivity getInstance() {
		return (DeviceActivity) mThis;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		int samplingPeriod = getSamplingPeriod(this);
		mGattUpdateReceiver = new DeviceActivityBroadcastReceiver(this, mServiceList, samplingPeriod);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();

		// BLE
		mBtLeService = BluetoothLeService.getInstance();
		mBluetoothDevice = intent.getParcelableExtra(EXTRA_DEVICE);
		mServiceList = new ArrayList<BluetoothGattService>();

		mIsSensorTag2 = false;
		// Determine type of SensorTagGatt
		String deviceName = mBluetoothDevice.getName();
		if ((deviceName.equals("SensorTag2")) ||(deviceName.equals("CC2650 SensorTag"))) {
			mIsSensorTag2 = true;
		} else {
			mIsSensorTag2 = false;
		}

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		// GUI
		mDeviceView = new DeviceView();
		mSectionsPagerAdapter.addSection(mDeviceView, "ID= " + mBluetoothDevice.getAddress());
		//HelpView hw = new HelpView();
		//hw.setParameters("help_device.html", R.layout.fragment_help, R.id.webpage);
		//mSectionsPagerAdapter.addSection(hw, "Help");
		mProfiles = new ArrayList<GenericBluetoothProfile>();
		progressDialog = new ProgressDialog(DeviceActivity.this);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setIndeterminate(true);
		progressDialog.setTitle("Discovering Services");
        progressDialog.setMessage("");
		progressDialog.setMax(100);
        progressDialog.setProgress(0);
        progressDialog.show();

        // GATT database
		Resources res = getResources();
		XmlResourceParser xpp = res.getXml(R.xml.gatt_uuid);
		new GattInfo(xpp);

	}

	public static int getSamplingPeriod(Activity activity) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		final int sampling_frequency = R.string.sampling_frequency;
		return (int)getNumber(activity, prefs, sampling_frequency, 1000);
	}

	public static float getNumber(Activity activity, SharedPreferences prefs,
								int sampling_frequency, float defaultValue) {
		String key = activity.getResources().getString(sampling_frequency);
		return Float.parseFloat(prefs.getString(key, "" + defaultValue));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
        if (mqttProfile != null) {
            mqttProfile.disconnect();

        }
        if (mIsReceiving) {
            unregisterReceiver(mGattUpdateReceiver);
            mIsReceiving = false;
        }
        for (GenericBluetoothProfile p : mProfiles) {
            p.onPause();
        }
        if (!this.isEnabledByPrefs("keepAlive")) {
            this.mBtLeService.timedDisconnect();
        }
        //View should be started again from scratch
        this.mDeviceView.first = true;
        this.mProfiles = null;
        this.mDeviceView.removeRowsFromTable();
        this.mDeviceView = null;
		finishActivity(PREF_ACT_REQ);
		finishActivity(FWUPDATE_ACT_REQ);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		this.optionsMenu = menu;
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.device_activity_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
		case R.id.opt_prefs:
			startPreferenceActivity(mBluetoothDevice, this);
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}
	public boolean isEnabledByPrefs(String prefName) {
		String preferenceKeyString = "pref_"
				+ prefName;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mBtLeService);

		Boolean defaultValue = true;
		return prefs.getBoolean(preferenceKeyString, defaultValue);
	}
	@Override
	protected void onResume() {
		// Log.d(TAG, "onResume");
		super.onResume();
		if (!mIsReceiving) {
			registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
			mIsReceiving = true;
		}
		for (GenericBluetoothProfile p : mProfiles) {
            if (p.isConfigured != true) p.configureService();
            if (p.isEnabled != true) p.enableService();
			p.onResume();
		}
		this.mBtLeService.abortTimedDisconnect();
	}

	@Override
	protected void onPause() {
		// Log.d(TAG, "onPause");
		super.onPause();
	}
	public static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter fi = new IntentFilter();
		fi.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		fi.addAction(BluetoothLeService.ACTION_DATA_NOTIFY);
		fi.addAction(BluetoothLeService.ACTION_DATA_WRITE);
		fi.addAction(BluetoothLeService.ACTION_DATA_READ);
		fi.addAction(DeviceInformationServiceProfile.ACTION_FW_REV_UPDATED);
        fi.addAction(TIOADProfile.ACTION_PREPARE_FOR_OAD);
		return fi;
	}

	void onViewInflated(View view) {
		// Log.d(TAG, "Gatt view ready");
		//setBusy(true);

		// Set title bar to device name
		//setTitle(mBluetoothDevice.getName());
		setTitle("Impact Monitor");

		// Create GATT object
		mBtGatt = BluetoothLeService.getBtGatt();
		// Start service discovery
		if (!mServicesRdy && mBtGatt != null) {
			if (mBtLeService.getNumServices() == 0)
				discoverServices();
			else {
			}
		}
	}

	boolean isSensorTag2() {
		return mIsSensorTag2;
	}

	String firmwareRevision() {
		return mGattUpdateReceiver.mFwRev;
	}
	BluetoothGattService getOadService() {
		return mOadService;
	}

	BluetoothGattService getConnControlService() {
		return mConnControlService;
	}

	public static void startPreferenceActivity(BluetoothDevice bluetoothDevice, Activity activity) {
		// Launch preferences
		final Intent i = new Intent(activity, PreferencesActivity.class);
		i.putExtra(PreferencesActivity.EXTRA_SHOW_FRAGMENT,
				PreferencesFragment.class.getName());
		i.putExtra(PreferencesActivity.EXTRA_NO_HEADERS, true);
		i.putExtra(EXTRA_DEVICE, bluetoothDevice);
		activity.startActivityForResult(i, PREF_ACT_REQ);
	}

	void discoverServices() {
		if (mBtGatt.discoverServices()) {
			mServiceList.clear();
			setBusy(true);

		} else {

		}
	}

	protected void setBusy(boolean b) {
		mDeviceView.setBusy(b);
	}
	// Activity result handling
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		default:
			break;
		}
	}

	void setError(String txt) {
		setBusy(false);
		Toast.makeText(this, txt, Toast.LENGTH_LONG).show();
	}

	private void setStatus(String txt) {
		Toast.makeText(this, txt, Toast.LENGTH_SHORT).show();
	}
	protected void observeAcceleration(MotionSensor p) {
		final Motion reading = p.getReading();
		if (this.mqttProfile != null) {
			this.mqttProfile.addSensorReading(reading);
		}

		final double totalAcceleration = ConcussionDetector.getTotalAcceleration(reading);
		Log.d("#", "Total acceleration=" + totalAcceleration);
		if (totalAcceleration >= 2.0) {
			this.runOnUiThread(new Runnable() {
				@Override
				public void run() {

					try {
						Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
						Ringtone r = RingtoneManager.getRingtone(DeviceActivity.this, notification);
						r.play();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	public void enableService(final GenericBluetoothProfile p) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mDeviceView.addRowToTable(p.getTableRow());
				p.enableService();
				progressDialog.setProgress(progressDialog.getProgress() + 1);
			}
		});
	}


	public void setSensorTagIoProfile(SensorTagIoProfile sensorTagIoProfile) {
		this.sensorTagIoProfile = sensorTagIoProfile;
	}
}
