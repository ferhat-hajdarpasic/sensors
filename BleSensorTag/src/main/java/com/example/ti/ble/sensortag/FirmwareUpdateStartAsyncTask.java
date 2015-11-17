package com.example.ti.ble.sensortag;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.example.ti.ble.common.GenericBluetoothProfile;

/**
 * Created by ferhat on 30/10/2015.
 */
public class FirmwareUpdateStartAsyncTask extends AsyncTask<String, Integer, Void> {
    private final DeviceActivity deviceActivity;
    ProgressDialog pd;Context con;

    public FirmwareUpdateStartAsyncTask(DeviceActivity deviceActivity, Context c) {
        this.con = c;
        this.pd = deviceActivity.progressDialog;
        this.deviceActivity = deviceActivity;
    }

    @Override
    protected void onPreExecute() {
        this.pd = new ProgressDialog(deviceActivity);
        this.pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        this.pd.setIndeterminate(false);
        this.pd.setTitle("Starting firmware update");
        this.pd.setMessage("");
        this.pd.setMax(deviceActivity.mProfiles.size());
        this.pd.show();
        super.onPreExecute();
    }

    @Override
    protected Void doInBackground(String... params) {
        Integer ii = 1;
        for (GenericBluetoothProfile p : deviceActivity.mProfiles) {

            p.disableService();
            p.deConfigureService();
            publishProgress(ii);
            ii = ii + 1;
        }

        if (deviceActivity.isSensorTag2()) {
            final Intent i = new Intent(this.con, FwUpdateActivity_CC26xx.class);
            deviceActivity.startActivityForResult(i, DeviceActivity.FWUPDATE_ACT_REQ);
        }
        else {
            final Intent i = new Intent(this.con, FwUpdateActivity.class);
            deviceActivity.startActivityForResult(i, DeviceActivity.FWUPDATE_ACT_REQ);
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        this.pd.setProgress(values[0]);
    }

    @Override
    protected void onPostExecute(Void result) {
        this.pd.dismiss();
        onPostExecute(result);
    }
}
