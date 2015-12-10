package com.whitespider.impact.ble.common;

import android.content.Context;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

/**
 * Created by ferhat on 14/11/2015.
 */
public class AzureEventBusClient {
    public AzureEventBusClient(Context context, String url, String deviceId) {
    }

    public void unregisterResources() {
        
    }

    public String getClientId() {
        return null;
    }

    public String getServerURI() {
        return null;
    }

    public void publish(String publishTopic, byte[] myNames, int i, boolean b) {
        AsyncHttpClient client = new AsyncHttpClient();
        String url = "https://mqqt-test.servicebus.windows.net/ferhat-event_hub/publishers/android/messages";
        //Generated at http://eventhubsas.azurewebsites.net/
//        client.addHeader("Authorization","SharedAccessSignature sr=https%3a%2f%2fmqqt-test.servicebus.windows.net%2fferhat-event_hub&sig=7FCXi98wORixZzGVHl1gdCOQQBgDhXkWirqwsukbDXE%3d&se=1447508012&skn=FERHAT-MQTT-SHARED-POLICy");
        client.addHeader("Authorization","SharedAccessSignature sr=https%3a%2f%2fmqqt-test.servicebus.windows.net%2fferhat-event_hub&sig=GmP9R%2frUi6KL%2fFbw16YwQwMZJvSuNPdm8OBmXhHpu%2bo%3d&se=1447767848&skn=FERHAT-MQTT-SHARED-POLICy");
        JSONObject params = new JSONObject();
        try {
            params.put("DeviceId", 12);
            params.put("Temperature", 98);
            StringEntity entity = new StringEntity(params.toString());
            client.post(null,url,entity,"application/json", new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int i, Header[] headers, byte[] bytes) {
                    Log.d("HABA", "Success");
                }
                @Override
                public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                    Log.d("HABA", "Failure:" + throwable.getMessage() );
                }
            });
        }
        catch (Exception ex){
            ex.printStackTrace();
        }

    }

    public void connect(MqttConnectOptions options, IMqttActionListener iMqttActionListener) {


    }
}
