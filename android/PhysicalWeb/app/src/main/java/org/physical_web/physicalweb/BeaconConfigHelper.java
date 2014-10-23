/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.physical_web.physicalweb;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.Arrays;
import java.util.UUID;

public class BeaconConfigHelper extends BluetoothGattCallback {
  public static final ParcelUuid CONFIG_SERVICE_UUID = ParcelUuid.fromString("b35d7da6-eed4-4d59-8f89-f6573edea967");
  private static final UUID DATA_ONE = UUID.fromString("b35d7da7-eed4-4d59-8f89-f6573edea967");
  private static final UUID DATA_TWO = UUID.fromString("b35d7da8-eed4-4d59-8f89-f6573edea967");
  private static final UUID DATA_LENGTH = UUID.fromString("b35d7da9-eed4-4d59-8f89-f6573edea967");
  private static final int DATA_LENGTH_MAX = 20;
  private static String TAG = "BeaconConfigHelper";
  private final Context mContext;

  private BluetoothGatt mBluetoothGatt;
  private Integer mDataLength;
  private byte[] mData;
  private BeaconConfigCallback mCallback;
  private BluetoothGattService mGattService;
  private Handler mBackgroundHandler;

  public BeaconConfigHelper(Context context, BeaconConfigCallback callback) {
    mContext = context;
    mCallback = callback;
    mBackgroundHandler = new Handler();
  }

  /**
   * Concatenates two byte arrays.
   *
   * @param a the first array.
   * @param b the second array.
   * @return the concatenated array.
   */
  private static byte[] concatenate(byte[] a, byte[] b) {
    byte[] result = new byte[a.length + b.length];
    System.arraycopy(a, 0, result, 0, a.length);
    System.arraycopy(b, 0, result, a.length, b.length);
    return result;
  }

  /**
   * Connect and read the UriBeacon.
   *
   * @param device
   */
  public void connectUriBeacon(BluetoothDevice device) {
    mData = null;
    mDataLength = null;
    mBluetoothGatt = device.connectGatt(mContext, false, this);
  }

  /**
   * Initiate a write and then close the connection.
   *
   * @param scanRecord
   */
  public void writeUriBeacon(byte[] scanRecord) {
    mData = scanRecord;
    BluetoothGattService service = mBluetoothGatt.getService(CONFIG_SERVICE_UUID.getUuid());
    writeCharacteristic(DATA_ONE, mData, 0);
  }

  @Override
  public void onConnectionStateChange(android.bluetooth.BluetoothGatt gatt, int status, int newState) {
    super.onConnectionStateChange(gatt, status, newState);
    if (newState == BluetoothProfile.STATE_CONNECTED) {
      mBluetoothGatt.discoverServices();
    }
  }

  @Override
  public void onServicesDiscovered(android.bluetooth.BluetoothGatt gatt, int status) {
    if (status == BluetoothGatt.GATT_SUCCESS) {
      mGattService = mBluetoothGatt.getService(CONFIG_SERVICE_UUID.getUuid());
      if (mGattService != null) {
        // Start the operation that will read the beacon's advertising packet
        readCharacteristic(DATA_LENGTH);
        return;
      }
    }
    Log.e(TAG, "onServicesDiscovered failed");
  }

  /**
   * Called when a characteristic read operation has occurred, chain to the next read operation
   * in order: DATA_LENGTH DATA_ONE [DATA_TWO].
   */
  @Override
  public void onCharacteristicRead(android.bluetooth.BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
    Log.d(TAG, "Characteristic " + characteristic.getUuid().toString() + " "
      + " len: " + characteristic.getValue().length);
    // If the operation was successful
    if (status == BluetoothGatt.GATT_SUCCESS) {
      UUID uuid = characteristic.getUuid();
      if (DATA_LENGTH.equals(uuid)) {
        mDataLength = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 0);
        readCharacteristic(DATA_ONE);
      } else if (DATA_ONE.equals(uuid)) {
        mData = characteristic.getValue();
        if (mDataLength > DATA_LENGTH_MAX) {
          readCharacteristic(DATA_TWO);
        } else {
          mCallback.onBeaconConfigReadUrlComplete(mData, status);
        }
      } else if (DATA_TWO.equals(uuid)) {
        mData = concatenate(mData, characteristic.getValue());
        mCallback.onBeaconConfigReadUrlComplete(mData, status);
      }
    } else {
      mCallback.onBeaconConfigReadUrlComplete(null, status);
    }
  }

  /**
   * Called when a Characteristic Write operation has completed. Chain to the DATA_TWO
   * operation there is more than DATA_LENGTH_MAX data.
   */
  @Override
  public void onCharacteristicWrite(android.bluetooth.BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
    if (status == BluetoothGatt.GATT_SUCCESS) {
      UUID uuid = characteristic.getUuid();
      if (DATA_ONE.equals(uuid) && mData.length > DATA_LENGTH_MAX) {
        writeCharacteristic(DATA_TWO, mData, DATA_LENGTH_MAX);
        return;
      }
    }
    try {
      // If we don't sleep here the write works only 20% of the time.
      Thread.sleep(20);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    mCallback.onBeaconConfigWriteUrlComplete(status);
  }

  // Start a read operation.
  private void readCharacteristic(UUID uuid) {
    BluetoothGattCharacteristic characteristic = mGattService.getCharacteristic(uuid);
    if (!mBluetoothGatt.readCharacteristic(characteristic)) {
      Log.e(TAG, "readCharacteristic failed for " + uuid);
    }
  }

  // Start a write operation.
  private void writeCharacteristic(UUID uuid, byte[] data, int offset) {
    int len = Math.min(DATA_LENGTH_MAX, data.length - offset);
    byte[] dataRange = Arrays.copyOfRange(data, offset, offset + len);
    BluetoothGattCharacteristic characteristic = mGattService.getCharacteristic(uuid);
    characteristic.setValue(dataRange);
    if (!mBluetoothGatt.writeCharacteristic(characteristic)) {
      Log.e(TAG, "writeCharacteristic failed for " + uuid);
    }
  }

  /**
   * Close this UriBeacon Bluetooth GATT client. Application should call this method as early as
   * possible after it is done with this GATT client.
   */
  public void closeUriBeacon() {
    if (mBluetoothGatt == null) {
      return;
    }
    mBluetoothGatt.close();
    mBluetoothGatt = null;
  }

  public interface BeaconConfigCallback {
    public void onBeaconConfigReadUrlComplete(byte[] scanRecord, int status);

    public void onBeaconConfigWriteUrlComplete(int status);
  }
}
