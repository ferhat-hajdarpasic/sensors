package com.example.ti.ble.sensortag;

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

import com.example.ti.ble.btsig.profiles.DeviceInformationServiceProfile;
import com.example.ti.ble.common.AzureIoTCloudProfile;
import com.example.ti.ble.common.BluetoothLeService;
import com.example.ti.ble.common.GenericBluetoothProfile;
import com.example.ti.ble.common.IBMIoTCloudProfile;
import com.example.ti.ble.ti.profiles.TIOADProfile;

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

    public DeviceActivityBroadcastReceiver(DeviceActivity deviceActivity, List<BluetoothGattService> serviceList) {
        this.serviceList = serviceList;
        this.deviceActivity = deviceActivity;
        mFwRev = new String("1.5"); // Assuming all SensorTags are up to date until actual FW revision is read
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
//onCharacteristicChanged(uuidStr, value);
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

    private void onDataRead(Intent intent) {
// Data read
        byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
        String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
        for (int ii = 0; ii < charList.size(); ii++) {
            BluetoothGattCharacteristic tempC = charList.get(ii);
            if ((tempC.getUuid().toString().equals(uuidStr))) {
                for (int jj = 0; jj < deviceActivity.mProfiles.size(); jj++) {
                    GenericBluetoothProfile p = deviceActivity.mProfiles.get(jj);
                    p.didReadValueForCharacteristic(tempC);
                }
//Log.d("DeviceActivity","Got Characteristic : " + tempC.getUuid().toString());
                break;
            }
        }
    }

    private void onDataWrite(Intent intent) {
// Data written
        byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
        String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
        for (int ii = 0; ii < charList.size(); ii++) {
            BluetoothGattCharacteristic tempC = charList.get(ii);
            if ((tempC.getUuid().toString().equals(uuidStr))) {
                for (int jj = 0; jj < deviceActivity.mProfiles.size(); jj++) {
                    GenericBluetoothProfile p = deviceActivity.mProfiles.get(jj);
                    p.didWriteValueForCharacteristic(tempC);
                }
//Log.d("DeviceActivity","Got Characteristic : " + tempC.getUuid().toString());
                break;
            }
        }
    }

    private void onDataNotify(Intent intent) {
// Notification
        byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
        String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
//Log.d("DeviceActivity","Got Characteristic : " + uuidStr);
        for (int ii = 0; ii < charList.size(); ii++) {
            BluetoothGattCharacteristic tempC = charList.get(ii);
            if ((tempC.getUuid().toString().equals(uuidStr))) {
                for (int jj = 0; jj < deviceActivity.mProfiles.size(); jj++) {
                    GenericBluetoothProfile p = deviceActivity.mProfiles.get(jj);
                    if (p.isDataC(tempC)) {
                        p.didUpdateValueForCharacteristic(tempC);
//Do MQTT
                        Map<String, String> map = p.getMQTTMap();
                        if (map != null) {
                            for (Map.Entry<String, String> e : map.entrySet()) {
                                if (deviceActivity.mqttProfile != null)
                                    deviceActivity.mqttProfile.addSensorValueToPendingMessage(e);
                            }
                        }
                    }
                }
//Log.d("DeviceActivity","Got Characteristic : " + tempC.getUuid().toString());
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

//Iterate through the services and add GenericBluetoothServices for each service
                int nrNotificationsOn = 0;
                int maxNotifications;
                int servicesDiscovered = 0;
                int totalCharacteristics = 0;
//serviceList = mBtLeService.getSupportedGattServices();
                for (BluetoothGattService s : serviceList) {
                    List<BluetoothGattCharacteristic> chars = s.getCharacteristics();
                    totalCharacteristics += chars.size();
                }
//Special profile for Cloud service
                deviceActivity.mqttProfile = new AzureIoTCloudProfile(context, deviceActivity.mBluetoothDevice, null, deviceActivity.mBtLeService);
                deviceActivity.mProfiles.add(deviceActivity.mqttProfile);
                if (totalCharacteristics == 0) {
//Something bad happened, we have a problem
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
//Try again
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
                    final float serviceDiscoveredcalc = (float) servicesDiscovered;
                    final float serviceTotalcalc = (float) serviceList.size();
                    deviceActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            deviceActivity.progressDialog.setProgress((int) ((serviceDiscoveredcalc / (serviceTotalcalc - 1)) * 100));
                        }
                    });
                    Log.d("DeviceActivity", "Configuring service with uuid : " + s.getUuid().toString());
                    if (SensorTagHumidityProfile.isCorrectService(s)) {
                        SensorTagHumidityProfile hum = new SensorTagHumidityProfile(context, deviceActivity.mBluetoothDevice, s, deviceActivity.mBtLeService);
                        deviceActivity.mProfiles.add(hum);
                        if (nrNotificationsOn < maxNotifications) {
                            hum.configureService();
                            nrNotificationsOn++;
                        } else {
                            hum.grayOutCell(true);
                        }
                        Log.d("DeviceActivity", "Found Humidity !");
                    }
                    if (SensorTagLuxometerProfile.isCorrectService(s)) {
                        SensorTagLuxometerProfile lux = new SensorTagLuxometerProfile(context, deviceActivity.mBluetoothDevice, s,deviceActivity. mBtLeService);
                        deviceActivity.mProfiles.add(lux);
                        if (nrNotificationsOn < maxNotifications) {
                            lux.configureService();
                            nrNotificationsOn++;
                        } else {
                            lux.grayOutCell(true);
                        }
                    }
                    if (SensorTagSimpleKeysProfile.isCorrectService(s)) {
                        SensorTagSimpleKeysProfile key = new SensorTagSimpleKeysProfile(context, deviceActivity.mBluetoothDevice, s, deviceActivity.mBtLeService);
                        deviceActivity.mProfiles.add(key);
                        if (nrNotificationsOn < maxNotifications) {
                            key.configureService();
                            nrNotificationsOn++;
                        } else {
                            key.grayOutCell(true);
                        }
                        Log.d("DeviceActivity", "Found Simple Keys !");
                    }
                    if (SensorTagBarometerProfile.isCorrectService(s)) {
                        SensorTagBarometerProfile baro = new SensorTagBarometerProfile(context, deviceActivity.mBluetoothDevice, s, deviceActivity.mBtLeService);
                        deviceActivity.mProfiles.add(baro);
                        if (nrNotificationsOn < maxNotifications) {
                            baro.configureService();
                            nrNotificationsOn++;
                        } else {
                            baro.grayOutCell(true);
                        }
                        Log.d("DeviceActivity", "Found Barometer !");
                    }
                    if (SensorTagAmbientTemperatureProfile.isCorrectService(s)) {
                        SensorTagAmbientTemperatureProfile irTemp = new SensorTagAmbientTemperatureProfile(context, deviceActivity.mBluetoothDevice, s, deviceActivity.mBtLeService);
                        deviceActivity.mProfiles.add(irTemp);
                        if (nrNotificationsOn < maxNotifications) {
                            irTemp.configureService();
                            nrNotificationsOn++;
                        } else {
                            irTemp.grayOutCell(true);
                        }
                        Log.d("DeviceActivity", "Found Ambient Temperature !");
                    }
                    if (SensorTagIRTemperatureProfile.isCorrectService(s)) {
                        SensorTagIRTemperatureProfile irTemp = new SensorTagIRTemperatureProfile(context, deviceActivity.mBluetoothDevice, s, deviceActivity.mBtLeService);
                        deviceActivity.mProfiles.add(irTemp);
                        if (nrNotificationsOn < maxNotifications) {
                            irTemp.configureService();
                        } else {
                            irTemp.grayOutCell(true);
                        }
//No notifications add here because it is already enabled above ..
                        Log.d("DeviceActivity", "Found IR Temperature !");
                    }
                    if (SensorTagMovementProfile.isCorrectService(s)) {
                        SensorTagMovementProfile mov = new SensorTagMovementProfile(context, deviceActivity.mBluetoothDevice, s, deviceActivity.mBtLeService);
                        deviceActivity.mProfiles.add(mov);
                        if (nrNotificationsOn < maxNotifications) {
                            mov.configureService();
                            nrNotificationsOn++;
                        } else {
                            mov.grayOutCell(true);
                        }
                        Log.d("DeviceActivity", "Found Motion !");
                    }


                    if (SensorTagGyroscopeProfile.isCorrectService(s)) {
                        SensorTagGyroscopeProfile gyro = new SensorTagGyroscopeProfile(context, deviceActivity.mBluetoothDevice, s, deviceActivity.mBtLeService);
                        deviceActivity.mProfiles.add(gyro);
                        if (nrNotificationsOn < maxNotifications) {
                            gyro.configureService();
                            nrNotificationsOn++;
                        } else {
                            gyro.grayOutCell(true);
                        }
                        Log.d("DeviceActivity", "Found Motion !");

                    }
                    if (SensorTagAccelerometerProfile.isCorrectService(s)) {
                        SensorTagAccelerometerProfile acc = new SensorTagAccelerometerProfile(context, deviceActivity.mBluetoothDevice, s, deviceActivity.mBtLeService);
                        deviceActivity.mProfiles.add(acc);
                        if (nrNotificationsOn < maxNotifications) {
                            acc.configureService();
                            nrNotificationsOn++;
                        } else {
                            acc.grayOutCell(true);
                        }
                        Log.d("DeviceActivity", "Found Motion !");

                    }
                    if (DeviceInformationServiceProfile.isCorrectService(s)) {
                        DeviceInformationServiceProfile devInfo = new DeviceInformationServiceProfile(context, deviceActivity.mBluetoothDevice, s, deviceActivity.mBtLeService);
                        deviceActivity.mProfiles.add(devInfo);
                        devInfo.configureService();
                        Log.d("DeviceActivity", "Found Device Information Service");
                    }
                    if (TIOADProfile.isCorrectService(s)) {
                        TIOADProfile oad = new TIOADProfile(context, deviceActivity.mBluetoothDevice, s, deviceActivity.mBtLeService);
                        deviceActivity.mProfiles.add(oad);
                        oad.configureService();
                        deviceActivity.mOadService = s;
                        Log.d("DeviceActivity", "Found TI OAD Service");
                    }
                    if ((s.getUuid().toString().compareTo("f000ccc0-0451-4000-b000-000000000000")) == 0) {
                        deviceActivity.mConnControlService = s;
                    }
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

                    deviceActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            deviceActivity.mDeviceView.addRowToTable(p.getTableRow());
                            p.enableService();
                            deviceActivity.progressDialog.setProgress(deviceActivity.progressDialog.getProgress() + 1);
                        }
                    });
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
}
