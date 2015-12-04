/**************************************************************************************************
 Filename:       SensorTagAccelerometerProfile.java
 Revised:        $Date: Wed Apr 22 13:01:34 2015 +0200$
 Revision:       $Revision: 599e5650a33a4a142d060c959561f9e9b0d88146$

 Copyright (c) 2013 - 2015 Texas Instruments Incorporated

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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;
import android.view.View;

import com.example.ti.ble.common.BluetoothLeService;
import com.example.ti.ble.common.GattInfo;
import com.example.ti.ble.common.GenericBluetoothProfile;
import com.example.ti.util.GenericCharacteristicTableRow;
import com.example.ti.util.Point3D;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SensorTagIoProfile extends GenericBluetoothProfile {
    public SensorTagIoProfile(Context con, BluetoothDevice device, BluetoothGattService service, BluetoothLeService controller) {
		super(con,device,service,controller);
		this.tRow =  new GenericCharacteristicTableRow(con);
		
		List<BluetoothGattCharacteristic> characteristics = this.mBTService.getCharacteristics();
		for (BluetoothGattCharacteristic c : characteristics) {

            if (c.getUuid().toString().equals(SensorTagGatt.UUID_IO_DATA.toString())) {
				this.dataC = c;
			}
			if (c.getUuid().toString().equals(SensorTagGatt.UUID_IO_CONF.toString())) {
				this.configC = c;
			}
//			if (c.getUuid().toString().equals(SensorTagGatt.UUID_IO_PERI.toString())) {
//				this.periodC = c;
//			}
		}
		
		this.tRow.sl1.autoScale = true;
		this.tRow.sl1.autoScaleBounceBack = true;
		
		this.tRow.sl2.autoScale = true;
		this.tRow.sl2.autoScaleBounceBack = true;
		this.tRow.sl2.setColor(255, 0, 150, 125);
		this.tRow.sl2.setVisibility(View.VISIBLE);
        this.tRow.sl2.setEnabled(true);
		
		this.tRow.sl3.autoScale = true;
		this.tRow.sl3.autoScaleBounceBack = true;
		this.tRow.sl3.setColor(255, 0, 0, 0);
		this.tRow.sl3.setVisibility(View.VISIBLE);
        this.tRow.sl3.setEnabled(true);
		
		this.tRow.setIcon(this.getIconPrefix(), this.dataC.getUuid().toString());
		
		this.tRow.title.setText(GattInfo.uuidToName(UUID.fromString(this.dataC.getUuid().toString())));
		this.tRow.uuidLabel.setText(this.dataC.getUuid().toString());
		this.tRow.value.setText("X:0.00G, Y:0.00G, Z:0.00G");
		
	}
	
	public static boolean isCorrectService(BluetoothGattService service) {
        if ((service.getUuid().toString().compareTo(SensorTagGatt.UUID_IO_SERV.toString())) == 0) {
            Log.d("DeviceActivity", "Found IO !");
            return true;
		} else {
            return false;
        }
	}
    @Override
	public void didUpdateValueForCharacteristic(BluetoothGattCharacteristic c) {
			if (c.equals(this.dataC)){
				Point3D v = Sensor.IO.convert(this.dataC.getValue());
				if (this.tRow.config == false) this.tRow.value.setText(String.format("X:%.2fG, Y:%.2fG, Z:%.2fG", v.x,v.y,v.z));
				this.tRow.sl1.addValue((float)v.x);
				this.tRow.sl2.addValue((float)v.y);
				this.tRow.sl3.addValue((float)v.z);
			}
	}
    @Override
    public Map<String,String> getMQTTMap() {
        Point3D v = Sensor.IO.convert(this.dataC.getValue());
        Map<String,String> map = new HashMap<String, String>();
        map.put("io_x", String.format("%.2f", v.x));
        map.put("io_y",String.format("%.2f",v.y));
        map.put("io_z", String.format("%.2f", v.z));
        return map;
    }

    private static byte RED_LED = 0x01;
    private static byte GREEN_LED = 0x02;
    private static byte BUZZER = 0x04;

    public void green(boolean on) {
        Log.d("GenericBluetoothProfile", "Green :" + on);
        control(GREEN_LED, on);
    }

    public void red(boolean on) {
        Log.d("GenericBluetoothProfile", "Red :" + on);
        control(RED_LED, on);
    }

    public void buzzer(boolean on) {
        Log.d("GenericBluetoothProfile", "Buzzer :" + on);
        control(BUZZER, on);
    }

    public void control(byte element, boolean on) {
        mBTLeService.readCharacteristic(this.dataC);
        byte[] value = this.dataC.getValue();
        if(on) {
            value[0] = (byte)(value[0] | element);
        } else {
            value[0] = (byte)(value[0] & ~element);
        }
        writeDataValue(value);
    }

    public void writeDataValue(byte[] value) {
        int error = mBTLeService.writeCharacteristic(this.dataC, value);
        if (error != 0) {
            if (this.dataC != null) {
                printError("Data write failed: " + value, this.dataC, error);
            }
        }
    }

    public void configureService() {
        byte REMOTE_MODE = 1;
        writeControlValue(new byte[]{REMOTE_MODE});
        writeDataValue(new byte[]{0x00}); //Clear all controls
        this.isConfigured = true;
    }

    public void writeControlValue(byte[] value) {
        int error = mBTLeService.writeCharacteristic(this.configC, value);
        if (error != 0) {
            if (this.configC != null) {
                printError("Data write failed: " + value, this.configC, error);
            }
        }
    }

    public void LED(byte b) {
        writeDataValue(new byte[] {b});
    }
}
