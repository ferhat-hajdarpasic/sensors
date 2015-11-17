package com.example.ti.ble.common;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;

public class BluetoothGattCallback {
    private final BluetoothLeService bluetoothLeService;
    /**
     * GATT client callbacks
     */
    android.bluetooth.BluetoothGattCallback mGattCallbacks;

    public BluetoothGattCallback(BluetoothLeService bluetoothLeService) {
        this.bluetoothLeService = bluetoothLeService;
        this.mGattCallbacks = new android.bluetooth.BluetoothGattCallback() {

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                int newState) {
                if (BluetoothGattCallback.this.bluetoothLeService.getmBluetoothGatt() == null) {
                    // Log.e(TAG, "mBluetoothGatt not created!");
                    return;
                }

                BluetoothDevice device = gatt.getDevice();
                String address = device.getAddress();
                // Log.d(TAG, "onConnectionStateChange (" + address + ") " + newState +
                // " status: " + status);

                try {
                    switch (newState) {
                        case BluetoothProfile.STATE_CONNECTED:
                            //refreshDeviceCache(mBluetoothGatt);
                            BluetoothGattCallback.this.bluetoothLeService.broadcastUpdate(BluetoothLeService.ACTION_GATT_CONNECTED, address, status);
                            break;
                        case BluetoothProfile.STATE_DISCONNECTED:
                            BluetoothGattCallback.this.bluetoothLeService.broadcastUpdate(BluetoothLeService.ACTION_GATT_DISCONNECTED, address, status);
                            break;
                        default:
                            // Log.e(TAG, "New state not processed: " + newState);
                            break;
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                BluetoothDevice device = gatt.getDevice();
                BluetoothGattCallback.this.bluetoothLeService.broadcastUpdate(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED, device.getAddress(),
                        status);
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic) {
                BluetoothGattCallback.this.bluetoothLeService.broadcastUpdate(BluetoothLeService.ACTION_DATA_NOTIFY, characteristic,
                        BluetoothGatt.GATT_SUCCESS);
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt,
                                             BluetoothGattCharacteristic characteristic, int status) {
                if (BluetoothGattCallback.this.bluetoothLeService.isBlocking()) BluetoothGattCallback.this.bluetoothLeService.unlockBlockingThread(status);
                if (BluetoothGattCallback.this.bluetoothLeService.getNonBlockQueue().size() > 0) {
                    BluetoothGattCallback.this.bluetoothLeService.getLock().lock();
                    for (int ii = 0; ii < BluetoothGattCallback.this.bluetoothLeService.getNonBlockQueue().size(); ii++) {
                        BluetoothLeService.bleRequest req = BluetoothGattCallback.this.bluetoothLeService.getNonBlockQueue().get(ii);
                        if (req.characteristic == characteristic) {
                            req.status = BluetoothLeService.bleRequestStatus.done;
                            BluetoothGattCallback.this.bluetoothLeService.getNonBlockQueue().remove(ii);
                            break;
                        }
                    }
                    BluetoothGattCallback.this.bluetoothLeService.getLock().unlock();
                }
                BluetoothGattCallback.this.bluetoothLeService.broadcastUpdate(BluetoothLeService.ACTION_DATA_READ, characteristic, status);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt,
                                              BluetoothGattCharacteristic characteristic, int status) {
                if (BluetoothGattCallback.this.bluetoothLeService.isBlocking()) BluetoothGattCallback.this.bluetoothLeService.unlockBlockingThread(status);
                if (BluetoothGattCallback.this.bluetoothLeService.getNonBlockQueue().size() > 0) {
                    BluetoothGattCallback.this.bluetoothLeService.getLock().lock();
                    for (int ii = 0; ii < BluetoothGattCallback.this.bluetoothLeService.getNonBlockQueue().size(); ii++) {
                        BluetoothLeService.bleRequest req = BluetoothGattCallback.this.bluetoothLeService.getNonBlockQueue().get(ii);
                        if (req.characteristic == characteristic) {
                            req.status = BluetoothLeService.bleRequestStatus.done;
                            BluetoothGattCallback.this.bluetoothLeService.getNonBlockQueue().remove(ii);
                            break;
                        }
                    }
                    BluetoothGattCallback.this.bluetoothLeService.getLock().unlock();
                }
                BluetoothGattCallback.this.bluetoothLeService.broadcastUpdate(BluetoothLeService.ACTION_DATA_WRITE, characteristic, status);
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt,
                                         BluetoothGattDescriptor descriptor, int status) {
                if (BluetoothGattCallback.this.bluetoothLeService.isBlocking()) BluetoothGattCallback.this.bluetoothLeService.unlockBlockingThread(status);
                BluetoothGattCallback.this.bluetoothLeService.unlockBlockingThread(status);
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt,
                                          BluetoothGattDescriptor descriptor, int status) {
                if (BluetoothGattCallback.this.bluetoothLeService.isBlocking()) BluetoothGattCallback.this.bluetoothLeService.unlockBlockingThread(status);
                // Log.i(TAG, "onDescriptorWrite: " + descriptor.getUuid().toString());
            }
        };
    }
}