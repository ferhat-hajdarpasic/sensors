package com.example.ti.ble.common;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;

import com.example.ti.ble.sensortag.Accelerometer;
import com.example.ti.ble.sensortag.Motion;
import com.example.ti.ble.sensortag.R;
import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.SyncHttpClient;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class AzureIoTCloudProfile extends GenericBluetoothProfile {
    AzureEventBusClient client;
    final String addrShort;
    static AzureIoTCloudProfile mThis;
    Map<String, String> valueMap = new HashMap<String, String>();
    List<Motion> valueList = Collections.synchronizedList(new ArrayList<Motion>());
    public boolean ready;
    private Timer publishTimer;
    BroadcastReceiver cloudConfigUpdateReceiver;
    cloudConfig config;

    public AzureIoTCloudProfile(final Context con, BluetoothDevice device, BluetoothGattService service, BluetoothLeService controller) {
        super(con,device,service,controller);
        this.tRow =  new IBMIoTCloudTableRow(con);
        this.tRow.setOnClickListener(null);

        config = readCloudConfigFromPrefs();

        if (config != null) {
            Log.d("IBMIoTCloudProfile", "Stored cloud configuration" + "\r\n" + config.toString());
        }
        else {
            config = initPrefsWithIBMQuickStart();
            Log.d("IBMIoTCloudProfile", "Stored cloud configuration was corrupt, starting new based on IBM IoT Quickstart variables" + config.toString());
        }

        String addr = mBTDevice.getAddress();
        String[] addrSplit = addr.split(":");
        int[] addrBytes = new int[6];
        for (int ii = 0; ii < 6; ii++) {
            addrBytes[ii] = Integer.parseInt(addrSplit[ii], 16);
        }
        ready = false;
        this.addrShort = String.format("%02x%02x%02x%02x%02x%02x",addrBytes[0],addrBytes[1],addrBytes[2],addrBytes[3],addrBytes[4],addrBytes[5]);
        Log.d("IBMIoTCloudProfile", "Device ID : " + addrShort);
        this.tRow.sl1.setVisibility(View.INVISIBLE);
        this.tRow.sl2.setVisibility(View.INVISIBLE);
        this.tRow.sl3.setVisibility(View.INVISIBLE);
        this.tRow.title.setText("Cloud View");
        this.tRow.setIcon("sensortag2cloudservice","","");
        this.tRow.value.setText("Device ID : " + addr);

        IBMIoTCloudTableRow tmpRow = (IBMIoTCloudTableRow) this.tRow;
        tmpRow.pushToCloud.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    connect();
                }
                else {
                    disconnect();
                }
            }
        });

        tmpRow.configureCloud.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CloudProfileConfigurationDialogFragment dF = CloudProfileConfigurationDialogFragment.newInstance(addrShort);

                final Activity act = (Activity)context;
                dF.show(act.getFragmentManager(),"CloudConfig");


              }
        });

        if (config.service == CloudProfileConfigurationDialogFragment.DEF_CLOUD_IBMQUICKSTART_CLOUD_SERVICE) {
            ((IBMIoTCloudTableRow) this.tRow).cloudURL.setText("Open in browser");
            ((IBMIoTCloudTableRow) this.tRow).cloudURL.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://quickstart.internetofthings.ibmcloud.com/#/device/" + addrShort + "/sensor/")));
                }
            });
        }
        else {
            ((IBMIoTCloudTableRow) this.tRow).cloudURL.setText("");
            ((IBMIoTCloudTableRow) this.tRow).cloudURL.setAlpha(0.1f);
        }
        mThis = this;
        cloudConfigUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(CloudProfileConfigurationDialogFragment.ACTION_CLOUD_CONFIG_WAS_UPDATED)) {
                    Log.d("IBMIoTCloudProfile","Cloud configuration was updated !");
                    if (client != null) {
                        config = readCloudConfigFromPrefs();
                    }
                }
            }
        };
        this.context.registerReceiver(cloudConfigUpdateReceiver,makeCloudConfigUpdateFilter());
    }
    public boolean disconnect() {
        try {
            ((IBMIoTCloudTableRow) tRow).setCloudConnectionStatusImage(context.getResources().getDrawable(R.drawable.cloud_disconnected));
            ready = false;
            if (client != null) {
                Log.d("IBMIoTCloudProfile", "Disconnecting from Azure");
                client.unregisterResources();
                client = null;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean connect() {
        client = new AzureEventBusClient(this.context,null,config.deviceId);
        publishTimer = new Timer();
        MQTTTimerTask task = new MQTTTimerTask();
        publishTimer.schedule(task,1000,1000);
        ready = true;
        ((IBMIoTCloudTableRow) tRow).setCloudConnectionStatusImage(context.getResources().getDrawable(R.drawable.cloud_connected));
        return true;
    }


    public void addSensorValueToPendingMessage(Map.Entry<String,String> e) {
        this.valueMap.put(e.getKey(),e.getValue());
    }

    public void addSensorReading(Motion reading) {
        this.valueList.add(reading);
    }


    @Override
    public void onPause() {
        super.onPause();
        this.context.unregisterReceiver(cloudConfigUpdateReceiver);
    }
    @Override
    public void onResume() {
        super.onResume();
        this.context.registerReceiver(cloudConfigUpdateReceiver,makeCloudConfigUpdateFilter());
    }
    @Override
    public void enableService () {

    }
    @Override
    public void disableService () {

    }
    @Override
    public void configureService() {

    }
    @Override
    public void deConfigureService() {

    }
    @Override
    public void didUpdateValueForCharacteristic(BluetoothGattCharacteristic c) {
    }
    @Override
    public void didReadValueForCharacteristic(BluetoothGattCharacteristic c) {
    }
    public static AzureIoTCloudProfile getInstance() {
        return mThis;
    }

    private static IntentFilter makeCloudConfigUpdateFilter() {
        final IntentFilter fi = new IntentFilter();
        fi.addAction(CloudProfileConfigurationDialogFragment.ACTION_CLOUD_CONFIG_WAS_UPDATED);
        return fi;
    }

    class cloudConfig extends Object {
        public Integer service;
        public String username;
        public String password;
        public String deviceId;
        public String brokerAddress;
        public int brokerPort;
        public String publishTopic;
        public boolean cleanSession;
        public boolean useSSL;
        cloudConfig () {

        }
        @Override
        public String toString() {
            String s = new String();
            s = "Cloud configuration :\r\n";
            s += "Service : " + service + "\r\n";
            s += "Username : " + username + "\r\n";
            s += "Password : " + password + "\r\n";
            s += "Device ID : " + deviceId + "\r\n";
            s += "Broker Address : " + brokerAddress + "\r\n";
            s += "Proker Port : " + brokerPort + "\r\n";
            s += "Publish Topic : " + publishTopic + "\r\n";
            s += "Clean Session : " + cleanSession + "\r\n";
            s += "Use SSL : " + useSSL + "\r\n";
            return s;
        }
    }
    public cloudConfig readCloudConfigFromPrefs() {
        cloudConfig c = new cloudConfig();
        try {
            c.service = Integer.parseInt(CloudProfileConfigurationDialogFragment.retrieveCloudPref(CloudProfileConfigurationDialogFragment.PREF_CLOUD_SERVICE,this.context),10);
            c.username = CloudProfileConfigurationDialogFragment.retrieveCloudPref(CloudProfileConfigurationDialogFragment.PREF_CLOUD_USERNAME,this.context);
            c.password = CloudProfileConfigurationDialogFragment.retrieveCloudPref(CloudProfileConfigurationDialogFragment.PREF_CLOUD_PASSWORD,this.context);
            c.deviceId = CloudProfileConfigurationDialogFragment.retrieveCloudPref(CloudProfileConfigurationDialogFragment.PREF_CLOUD_DEVICE_ID,this.context);
            c.brokerAddress = CloudProfileConfigurationDialogFragment.retrieveCloudPref(CloudProfileConfigurationDialogFragment.PREF_CLOUD_BROKER_ADDR,this.context);
            c.brokerPort = Integer.parseInt(CloudProfileConfigurationDialogFragment.retrieveCloudPref(CloudProfileConfigurationDialogFragment.PREF_CLOUD_BROKER_PORT,this.context),10);
            c.publishTopic = CloudProfileConfigurationDialogFragment.retrieveCloudPref(CloudProfileConfigurationDialogFragment.PREF_CLOUD_PUBLISH_TOPIC,this.context);
            c.cleanSession = Boolean.parseBoolean(CloudProfileConfigurationDialogFragment.retrieveCloudPref(CloudProfileConfigurationDialogFragment.PREF_CLOUD_CLEAN_SESSION,this.context));
            c.useSSL = Boolean.parseBoolean(CloudProfileConfigurationDialogFragment.retrieveCloudPref(CloudProfileConfigurationDialogFragment.PREF_CLOUD_USE_SSL,this.context));
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return c;
    }
    public cloudConfig initPrefsWithIBMQuickStart() {
        cloudConfig c = new cloudConfig();
        c.service = CloudProfileConfigurationDialogFragment.DEF_CLOUD_IBMQUICKSTART_CLOUD_SERVICE;
        c.username = CloudProfileConfigurationDialogFragment.DEF_CLOUD_IBMQUICKSTART_USERNAME;
        c.password = CloudProfileConfigurationDialogFragment.DEF_CLOUD_IBMQUICKSTART_PASSWORD;
        c.deviceId = CloudProfileConfigurationDialogFragment.DEF_CLOUD_IBMQUICKSTART_DEVICEID_PREFIX + addrShort;
        c.brokerAddress = CloudProfileConfigurationDialogFragment.DEF_CLOUD_IBMQUICKSTART_BROKER_ADDR;
        try {
            c.brokerPort = Integer.parseInt(CloudProfileConfigurationDialogFragment.DEF_CLOUD_IBMQUICKSTART_BROKER_PORT);
        }
        catch (Exception e) {
            c.brokerPort = 1883;
        }
        c.publishTopic = CloudProfileConfigurationDialogFragment.DEF_CLOUD_IBMQUICKSTART_PUBLISH_TOPIC;
        c.cleanSession = CloudProfileConfigurationDialogFragment.DEF_CLOUD_IBMQUICKSTART_CLEAN_SESSION;
        c.useSSL = CloudProfileConfigurationDialogFragment.DEF_CLOUD_IBMQUICKSTART_USE_SSL;
        return c;
    }

    class MQTTTimerTask extends TimerTask {
        @Override
        public void run() {
            Log.d("MQTTTimerTask", "MQTTTimerTask ran.");
            if (ready) {
                final Activity activity = (Activity) context;
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((IBMIoTCloudTableRow)tRow).setCloudConnectionStatusImage(activity.getResources().getDrawable(R.drawable.cloud_connected_tx));
                    }
                });

                List<Motion> copy = new ArrayList<Motion>(valueList);
                if(!copy.isEmpty()) {
                    SyncHttpClient client = new SyncHttpClient();
                    try {
                        String url = "https://mqqt-test.servicebus.windows.net/ferhat-event_hub/publishers/android/messages";
                        //Generated at http://eventhubsas.azurewebsites.net/
                        client.addHeader("Authorization", "SharedAccessSignature sr=https%3a%2f%2fmqqt-test.servicebus.windows.net%2fferhat-event_hub&sig=xWa9Y%2btbPeJCzNMp08dAT3RZmdVA9tRB%2bp%2bAq%2fFKuQY%3d&se=1447849892&skn=FERHAT-MQTT-SHARED-POLICy");
                        Gson gson = new Gson();
                        final String json = gson.toJson(copy);
                        StringEntity entity = new StringEntity(json);
                        client.post(null, url, entity, "application/json", new AsyncHttpResponseHandler() {
                            @Override
                            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                                Log.d("Successfully sent:", json);
                            }

                            @Override
                            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                                Log.d("Failed to send:", "" + json, throwable);
                            }
                        });
                    } catch (UnsupportedEncodingException e) {
                        Log.d("Failed to send:", "" + copy.size(), e);
                    } finally {
                    }
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((IBMIoTCloudTableRow)tRow).setCloudConnectionStatusImage(activity.getResources().getDrawable(R.drawable.cloud_connected));
                    }
                });
            }
            else {
                Log.d("IBMIoTCloudProfile", "MQTTTimerTask ran, but MQTT not ready");
            }
        }
    }
}
