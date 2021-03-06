package com.whitespider.impact.ble.sensortag;

import android.app.AlertDialog;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.whitespider.impact.ble.btsig.profiles.DeviceInformationServiceProfile;
import com.whitespider.impact.ble.common.AzureIoTCloudProfile;
import com.whitespider.impact.ble.common.BluetoothLeService;
import com.whitespider.impact.ble.common.GenericBluetoothProfile;
import com.whitespider.impact.ble.ti.profiles.TIOADProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by ferhat on 30/10/2015.
 */
public class DeviceActivityBroadcastReceiver extends BroadcastReceiver {
    public String mFwRev;
    List<BluetoothGattService> serviceList;
    List<BluetoothGattCharacteristic> charList = new ArrayList<BluetoothGattCharacteristic>();
    final DeviceActivity deviceActivity;
    private int samplingPeriod;

    public DeviceActivityBroadcastReceiver(DeviceActivity deviceActivity,
                                           List<BluetoothGattService> serviceList,
                                           int samplingPeriod) {
        this.samplingPeriod = samplingPeriod;
        this.serviceList = serviceList;
        this.deviceActivity = deviceActivity;
        mFwRev = new String("1.5");
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        final String action = intent.getAction();
        final int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS,
                BluetoothGatt.GATT_SUCCESS);
        if (DeviceInformationServiceProfile.ACTION_FW_REV_UPDATED.equals(action)) {
            mFwRev = intent.getStringExtra(DeviceInformationServiceProfile.EXTRA_FW_REV_STRING);
            Log.d("DeviceActivity", "Got FW revision : " + mFwRev + " from DeviceInformationServiceProfile");
            for (GenericBluetoothProfile p : deviceActivity.mProfiles) {
                p.didUpdateFirmwareRevision(mFwRev);
            }
        }
        if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                onServiceDiscovered(context);
            } else {
                Toast.makeText(deviceActivity.getApplication(), "Service discovery failed",
                        Toast.LENGTH_LONG).show();
                return;
            }
        } else if (BluetoothLeService.ACTION_DATA_NOTIFY.equals(action)) {
            onDataNotify(intent);
        } else if (BluetoothLeService.ACTION_DATA_WRITE.equals(action)) {
            onDataWrite(intent);

        } else if (BluetoothLeService.ACTION_DATA_READ.equals(action)) {
            onDataRead(intent);
        } else {
            if (TIOADProfile.ACTION_PREPARE_FOR_OAD.equals(action)) {
                new FirmwareUpdateStartAsyncTask(deviceActivity, context).execute();
            }
        }
        if (status != BluetoothGatt.GATT_SUCCESS) {
            deviceActivity.setError("GATT error code: " + status);
        }
    }

    protected void onDataRead(Intent intent) {
        byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
        String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
        for (int ii = 0; ii < charList.size(); ii++) {
            BluetoothGattCharacteristic tempC = charList.get(ii);
            if ((tempC.getUuid().toString().equals(uuidStr))) {
                for (int jj = 0; jj < deviceActivity.mProfiles.size(); jj++) {
                    GenericBluetoothProfile p = deviceActivity.mProfiles.get(jj);
                    p.didReadValueForCharacteristic(tempC);
                }
                break;
            }
        }
    }

    private void onDataWrite(Intent intent) {
        byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
        String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
        for (int ii = 0; ii < charList.size(); ii++) {
            BluetoothGattCharacteristic tempC = charList.get(ii);
            if ((tempC.getUuid().toString().equals(uuidStr))) {
                for (int jj = 0; jj < deviceActivity.mProfiles.size(); jj++) {
                    GenericBluetoothProfile p = deviceActivity.mProfiles.get(jj);
                    p.didWriteValueForCharacteristic(tempC);
                }
                break;
            }
        }
    }

    private void onDataNotify(Intent intent) {
        byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
        String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
        for (int ii = 0; ii < charList.size(); ii++) {
            BluetoothGattCharacteristic tempC = charList.get(ii);
            if ((tempC.getUuid().toString().equals(uuidStr))) {
                for (int jj = 0; jj < deviceActivity.mProfiles.size(); jj++) {
                    GenericBluetoothProfile p = deviceActivity.mProfiles.get(jj);
                    if (p.isDataC(tempC)) {
                        p.didUpdateValueForCharacteristic(tempC);
                        Map<String, String> map = p.getMQTTMap();
                        if (map != null) {
                            if(p instanceof MotionSensor) {
                                deviceActivity.observeAcceleration((MotionSensor) p);
                            }
                            for (Map.Entry<String, String> e : map.entrySet()) {
                                if (deviceActivity.mqttProfile != null) {
                                    deviceActivity.mqttProfile.addSensorValueToPendingMessage(e);
                                }
                            }
                        }
                    }
                }
                break;
            }
        }
    }

    private void onServiceDiscovered(final Context context) {
        serviceList = deviceActivity.mBtLeService.getSupportedGattServices();
        if (serviceList.size() > 0) {
            for (int ii = 0; ii < serviceList.size(); ii++) {
                BluetoothGattService s = serviceList.get(ii);
                List<BluetoothGattCharacteristic> c = s.getCharacteristics();
                if (c.size() > 0) {
                    for (int jj = 0; jj < c.size(); jj++) {
                        charList.add(c.get(jj));
                    }
                }
            }
        }
        Log.d("DeviceActivity", "Total characteristics " + charList.size());
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {

                int nrNotificationsOn = 0;
                int maxNotifications;
                int servicesDiscovered = 0;
                int totalCharacteristics = 0;
                for (BluetoothGattService s : serviceList) {
                    List<BluetoothGattCharacteristic> chars = s.getCharacteristics();
                    totalCharacteristics += chars.size();
                }
                deviceActivity.mqttProfile = new AzureIoTCloudProfile(context, deviceActivity.mBluetoothDevice, null, deviceActivity.mBtLeService);
                //deviceActivity.mProfiles.add(deviceActivity.mqttProfile);
                if (totalCharacteristics == 0) {
                    deviceActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            deviceActivity.progressDialog.hide();
                            deviceActivity. progressDialog.dismiss();
                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                                    context);
                            alertDialogBuilder.setTitle("Error !");
                            alertDialogBuilder.setMessage(serviceList.size() + " Services found, but no characteristics found, device will be disconnected !");
                            alertDialogBuilder.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    deviceActivity.mBtLeService.refreshDeviceCache(deviceActivity.mBtGatt);
                                    deviceActivity.discoverServices();
                                }
                            });
                            alertDialogBuilder.setNegativeButton("Disconnect", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    deviceActivity.mBtLeService.disconnect(deviceActivity.mBluetoothDevice.getAddress());
                                }
                            });
                            AlertDialog a = alertDialogBuilder.create();
                            a.show();
                        }
                    });
                    return;
                }
                final int final_totalCharacteristics = totalCharacteristics;
                deviceActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        deviceActivity.progressDialog.setIndeterminate(false);
                        deviceActivity.progressDialog.setTitle("Generating GUI");
                        deviceActivity.progressDialog.setMessage("Found a total of " + serviceList.size() + " services with a total of " + final_totalCharacteristics + " characteristics on this device");

                    }
                });
                if (Build.VERSION.SDK_INT > 18) maxNotifications = 7;
                else {
                    maxNotifications = 4;
                    deviceActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Android version 4.3 detected, max 4 notifications enabled", Toast.LENGTH_LONG).show();
                        }
                    });
                }
                for (int ii = 0; ii < serviceList.size(); ii++) {
                    BluetoothGattService s = serviceList.get(ii);
                    List<BluetoothGattCharacteristic> chars = s.getCharacteristics();
                    if (chars.size() == 0) {

                        Log.d("DeviceActivity", "No characteristics found for this service !!!");
                        return;
                    }
                    servicesDiscovered++;
                    nrNotificationsOn = discoverService(nrNotificationsOn, maxNotifications, servicesDiscovered, s, context);
                }
                deviceActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        deviceActivity.progressDialog.setTitle("Enabling Services");
                        deviceActivity.progressDialog.setMax(deviceActivity.mProfiles.size());
                        deviceActivity.progressDialog.setProgress(0);
                    }
                });
                for (final GenericBluetoothProfile p : deviceActivity.mProfiles) {
                    deviceActivity.enableService(p);
                    p.onResume();
                }
                deviceActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        deviceActivity.progressDialog.hide();
                        deviceActivity.progressDialog.dismiss();
                    }
                });
            }
        });
        worker.start();
    }

    public int discoverService(int nrNotificationsOn, int maxNotifications, float servicesDiscovered, BluetoothGattService s, Context context) {
        final float serviceDiscoveredcalc = servicesDiscovered;
        final float serviceTotalcalc = (float) serviceList.size();
        deviceActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceActivity.progressDialog.setProgress((int) ((serviceDiscoveredcalc / (serviceTotalcalc - 1)) * 100));
            }
        });
        Log.d("DeviceActivity", "Configuring service with uuid : " + s.getUuid().toString());
        if (SensorTagGyroscopeProfile.isCorrectService(s)) {
            GenericBluetoothProfile profile = new SensorTagGyroscopeProfile(context, deviceActivity.mBluetoothDevice, s, deviceActivity.mBtLeService, samplingPeriod);
            nrNotificationsOn = addProfile(nrNotificationsOn, maxNotifications, profile);
            Log.d("DeviceActivity", "Found Gyroscope !");

        }
        if (SensorTagAccelerometerProfile.isCorrectService(s)) {
            GenericBluetoothProfile profile = new SensorTagAccelerometerProfile(context, deviceActivity.mBluetoothDevice, s, deviceActivity.mBtLeService, samplingPeriod);
            nrNotificationsOn = addProfile(nrNotificationsOn, maxNotifications, profile);
            Log.d("DeviceActivity", "Found Accelerometer !");

        }
        if (SensorTagMovementProfile.isCorrectService(s)) {
            GenericBluetoothProfile profile = new SensorTagMovementProfile(context, deviceActivity.mBluetoothDevice, s, deviceActivity.mBtLeService, samplingPeriod);
            nrNotificationsOn = addProfile(nrNotificationsOn, maxNotifications, profile);
            Log.d("DeviceActivity", "Found Accelerometer !");

        }
        if (SensorTagIoProfile.isCorrectService(s)) {

            final SensorTagIoProfile sensorTagIoProfile = new SensorTagIoProfile(context, deviceActivity.mBluetoothDevice, s, deviceActivity.mBtLeService);
            this.deviceActivity.setSensorTagIoProfile(sensorTagIoProfile);
            nrNotificationsOn = addIoProfile(nrNotificationsOn, maxNotifications, sensorTagIoProfile);
            Log.d("DeviceActivity", "Found IO Service !");

        }
        if ((s.getUuid().toString().compareTo("f000ccc0-0451-4000-b000-000000000000")) == 0) {
            deviceActivity.mConnControlService = s;
        }
        return nrNotificationsOn;
    }

    private int addIoProfile(int nrNotificationsOn, int maxNotifications, SensorTagIoProfile ioProfile) {
        if (nrNotificationsOn < maxNotifications) {
            ioProfile.configureService();
            nrNotificationsOn++;
        }
        return nrNotificationsOn;
    }
    private int addProfile(int nrNotificationsOn, int maxNotifications, GenericBluetoothProfile profile) {
        deviceActivity.mProfiles.add(profile);
        if (nrNotificationsOn < maxNotifications) {
            profile.configureService();
            nrNotificationsOn++;
        } else {
            profile.grayOutCell(true);
        }
        return nrNotificationsOn;
    }
}
