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

package physical_web.org.physicalweb;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import org.uribeacon.beacon.UriBeacon;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

/**
 * This is the class that manages the low-level
 * reading from and writing to beacon
 * advertising packets via GATT services.
 */

public class BeaconConfigHelper {

  private static final String TAG = "BeaconConfigHelper";
  private static BluetoothGatt mBluetoothGatt;
  private static byte[] mAdvertisingPacketData_read = null;
  private static byte[] mAdvertisingPacketData_write = null;
  private static BluetoothGattService mBeaconBluetoothGattService = null;
  private static BeaconConfigCallback mBeaconConfigCallback = null;
  private static final UUID UUID_BEACON_READ_WRITE_SERVICE = UUID.fromString("b35d7da6-eed4-4d59-8f89-f6573edea967");
  private static final UUID UUID_BEACON_DATA_PART_1 = UUID.fromString("b35d7da7-eed4-4d59-8f89-f6573edea967");
  private static final UUID UUID_BEACON_DATA_PART_2 = UUID.fromString("b35d7da8-eed4-4d59-8f89-f6573edea967");
  private static final UUID UUID_BEACON_DATA_LENGTH = UUID.fromString("b35d7da9-eed4-4d59-8f89-f6573edea967");
  private static final int MAX_NUM_BYTES_DATA_PART_1 = 20;

  public BeaconConfigHelper() {

  }


  /////////////////////////////////
  // accessors
  /////////////////////////////////


  /////////////////////////////////
  // callbacks
  /////////////////////////////////

  public interface BeaconConfigCallback {

    public void onBeaconConfigReadUrlComplete(String url);

    public void onBeaconConfigWriteUrlComplete();
  }

  /**
   * This is the class that listens for GATT changes.
   */
  private static final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
    // Called when GATT connection state changes
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
      Log.d(TAG, "onConnectionStateChange:  " + "status: " + status + "  newState: " + newState);
      // If a connection has been established to the GATT server
      if (newState == BluetoothProfile.STATE_CONNECTED) {
        Log.d(TAG, "onConnectionStateChange:  connected!");
        onConnectedToNearbyBeaconGattServer();
        // if the connection to the GATT server has stopped
      } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
        Log.d(TAG, "onConnectionStateChange:  disconnected!");
        onDisconnectedFromNearbyBeaconGattServer();
      }
    }

    /**
     * Called when the available services have been discovered
     */
    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
      // if the service discovery was successful
      if (status == BluetoothGatt.GATT_SUCCESS) {
        try {
          onNearbyBeaconGattServicesDiscovered();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    /**
     * Called when a characteristic read operation has occurred
     */
    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
      // If the operation was successful
      if (status == BluetoothGatt.GATT_SUCCESS) {
        onNearbyBeaconsGattCharacteristicRead(characteristic);
      }
    }

    /**
     * Called when a characteristic write operation has occurred
     *
     * @param gatt GATT client that called writeCharacteristic
     * @param characteristic Characteristic that was written to the associated remote device.
     * @param status The result of the write operation GATT_SUCCESS if the operation succeeds.
     */
    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
      // If the operation was successful
      if (status == BluetoothGatt.GATT_SUCCESS) {
        onNearbyBeaconsGattCharacteristicWrite(characteristic);
      }
    }
  };

  /**
   * Called when a connection to the beacon GATT server has been established.
   */
  private static void onConnectedToNearbyBeaconGattServer() {
    Log.i(TAG, "Connected to GATT server.");
    mBluetoothGatt.discoverServices();
  }

  /**
   * Called when a connection to the beacon GATT server has been severed.
   */
  private static void onDisconnectedFromNearbyBeaconGattServer() {
    Log.i(TAG, "Disconnected from GATT server.");
  }

  /**
   * Called when the beacon's GATT services have been discovered.
   *
   * @throws IOException
   */
  private static void onNearbyBeaconGattServicesDiscovered() throws IOException {
    // Store a reference to the GATT service
    mBeaconBluetoothGattService = mBluetoothGatt.getService(UUID_BEACON_READ_WRITE_SERVICE);
    // Start the operation that will read the beacon's advertising packet
    beginReadingBeaconAdvertisingPacket();
  }

  /**
   * Called when the operation to read a beacon's GATT service has completed.
   *
   * @param bluetoothGattCharacteristic Characteristic that was read from the associated remote device.
   */
  private static void onNearbyBeaconsGattCharacteristicRead(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
    // If the read was for retrieving part 1 of the beacon advertising packet.
    if (bluetoothGattCharacteristic.getUuid().equals(UUID_BEACON_DATA_PART_1)) {
      handleGattCharacteristicRead_beaconDataPart1(bluetoothGattCharacteristic);
      // If the read was for retrieving the length of the beacon advertising packet.
    } else if (bluetoothGattCharacteristic.getUuid().equals(UUID_BEACON_DATA_LENGTH)) {
      handleGattCharacteristicRead_beaconDataLength(bluetoothGattCharacteristic);
      // If the read was for retrieving part 2 of the beacon advertising packet.
    } else if (bluetoothGattCharacteristic.getUuid().equals(UUID_BEACON_DATA_PART_2)) {
      handleGattCharacteristicRead_beaconDataPart2(bluetoothGattCharacteristic);
    }
  }

  /**
   * Called when the operation to write to a beacon's GATT service has completed.
   *
   * @param bluetoothGattCharacteristic Characteristic that was written to the associated remote device.
   */
  private static void onNearbyBeaconsGattCharacteristicWrite(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
    // If the write was for writing part 1 of the beacon advertising packet.
    if (bluetoothGattCharacteristic.getUuid().equals(UUID_BEACON_DATA_PART_1)) {
      handleGattCharacteristicWrite_beaconDataPart1(bluetoothGattCharacteristic);
      // If the write was for writing part 2 of the beacon advertising packet.
    } else if (bluetoothGattCharacteristic.getUuid().equals(UUID_BEACON_DATA_PART_2)) {
      handleGattCharacteristicWrite_beaconDataPart2(bluetoothGattCharacteristic);
    }
  }

  /**
   * Called when the read advertising packet operation
   * from the beacon has completed
   */
  private static void onReadComplete_beaconData() {
    String url = createUrlFromScanRecord(mAdvertisingPacketData_read);
    if (url == null) {
      url = "No url found";
    }
    mBeaconConfigCallback.onBeaconConfigReadUrlComplete(url);
  }

  /**
   * Find the url that is encoded into the scan record,
   * but also expand a short url
   * and ensure an http prefix exists.
   *
   * @param scanRecord encoded url
   * @return The url that was encoded the scan record.
   */
  public static String createUrlFromScanRecord(byte[] scanRecord) {
    String url = null;

    // Get the raw url that is in the advertising packet
    UriBeacon uriBeacon = UriBeacon.parseFromBytes(scanRecord);
    if (uriBeacon != null) {
      url = uriBeacon.getUriString();
    }

    // If a url was found
    if (url != null) {

      // If this is a shortened url
      if (UrlShortener.isShortUrl(url)) {
        // Expand the url to it's original url
        url = UrlShortener.lengthenShortUrl(url);
      }
    }
    return url;
  }

  /**
   * Called when the write advertising packet operation
   * to the beacon has completed
   */
  private static void onWriteComplete_beaconData() {
    mBeaconConfigCallback.onBeaconConfigWriteUrlComplete();
  }


  /////////////////////////////////
  // utilities
  /////////////////////////////////

  public static void readBeaconUrl(Context context, BeaconConfigCallback beaconConfigCallback, BluetoothDevice beaconBluetoothDevice) {
    Log.d(TAG, "readBeaconUrl");
    // Clear out the read data
    mAdvertisingPacketData_read = null;
    // Start with a fresh gatt service
    mBeaconBluetoothGattService = null;
    // Store the context so we can call back later
    mBeaconConfigCallback = beaconConfigCallback;
    // Connect to the nearby beacon's GATT service.
    connectToNearbyBeacon(context, beaconBluetoothDevice);
  }

  /**
   * Connect to the nearby beacon's GATT service.
   *
   * @param context
   * @param beaconBluetoothDevice Device hosting the GATT Server
   */
  private static void connectToNearbyBeacon(Context context, BluetoothDevice beaconBluetoothDevice) {
    mBluetoothGatt = beaconBluetoothDevice.connectGatt(context, true, mBluetoothGattCallback);
  }

  /**
   * Start reading the beacon's advertising packet.
   */
  private static void beginReadingBeaconAdvertisingPacket() {
    readCharacteristic_beaconDataPart1();
  }

  /**
   * Read part 1 of the beacon's advertising packet
   */
  private static void readCharacteristic_beaconDataPart1() {
    BluetoothGattCharacteristic characteristic_beaconDataPart1 = mBeaconBluetoothGattService.getCharacteristic(UUID_BEACON_DATA_PART_1);
    mBluetoothGatt.readCharacteristic(characteristic_beaconDataPart1);
  }

  /**
   * Read the length the of the beacon's advertising packet
   */
  private static void readCharacteristic_beaconDataLength() {
    BluetoothGattCharacteristic characteristic_beaconDataLength = mBeaconBluetoothGattService.getCharacteristic(UUID_BEACON_DATA_LENGTH);
    mBluetoothGatt.readCharacteristic(characteristic_beaconDataLength);
  }

  /**
   * Read part 2 of the beacon's advertising packet.
   */
  private static void readCharacteristic_beaconDataPart2() {
    BluetoothGattCharacteristic characteristic_beaconDataPart2 = mBeaconBluetoothGattService.getCharacteristic(UUID_BEACON_DATA_PART_2);
    mBluetoothGatt.readCharacteristic(characteristic_beaconDataPart2);
  }

  /**
   * Run actions given that the read operation
   * of part 1 of the beacon's advertising packet
   * has completed.
   *
   * @param bluetoothGattCharacteristic Data Part 1 characteristic that was read from the associated remote device.
   */
  private static void handleGattCharacteristicRead_beaconDataPart1(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
    // Store the read value byte array
    mAdvertisingPacketData_read = bluetoothGattCharacteristic.getValue();
    // Read the beacon's advertising packet length
    readCharacteristic_beaconDataLength();
  }

  /**
   * Run actions given that the read operation
   * of the length of the beacon's advertising packet
   * has completed.
   *
   * @param bluetoothGattCharacteristic Length characteristic that was read from the associated remote device.
   */
  private static void handleGattCharacteristicRead_beaconDataLength(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
    // Get the read value data length
    int dataLength = (int) bluetoothGattCharacteristic.getValue()[0];
    // If the length is greater than the threshold length for part 1
    if (dataLength > MAX_NUM_BYTES_DATA_PART_1) {
      // Read part 2 of the beacon data's advertising packet.
      readCharacteristic_beaconDataPart2();
      // If the length is not greater than the threshold length for part 1
    } else {
      onReadComplete_beaconData();
    }
  }

  /**
   * Run actions given that the read operation
   * of part 1 of the beacon's advertising packet
   * has completed.
   *
   * @param bluetoothGattCharacteristic Data part 2 characteristic that was read from the associated remote device.
   */
  private static void handleGattCharacteristicRead_beaconDataPart2(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
    // Get the read value byte array
    byte[] data_part2 = bluetoothGattCharacteristic.getValue();
    // Combine this byte array with that received from the part 1 read
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      outputStream.write(mAdvertisingPacketData_read);
      outputStream.write(data_part2);
      // Store the combined byte array
      mAdvertisingPacketData_read = outputStream.toByteArray();
      onReadComplete_beaconData();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Write the given url to the
   * currently-being-configured beacon.
   * This involves constructing the advertising packet
   * that contains the url and then pushing that packet
   * to the beacon via GATT.
   *
   * @param url URL to write to the beacon
   */
  public static void writeBeaconUrl(Context context, BeaconConfigCallback beaconConfigCallback, BluetoothDevice beaconBluetoothDevice, String url) {
    Log.d(TAG, "writeBeaconUrl" + "  url:  " + url);
    Log.d(TAG, "gatt service object: " + mBeaconBluetoothGattService);
    //TODO: need to check for existence of gatt connection and create one if needed
    beginWritingBeaconAdvertisingPacket(url);
  }

  /**
   * Start the process of writing an advertising packet
   * that contains the given url to the
   * currently-being-configured beacon.
   *
   * @param url URL to write to the beacon
   */
  private static void beginWritingBeaconAdvertisingPacket(String url) {
    try {
      // Create the advertising packet that contains
      // the given url.
      mAdvertisingPacketData_write = BeaconHelper.createAdvertisingPacket(url);
    } catch (IOException e) {
      e.printStackTrace();
    }
    byte[] data_toWrite;
    // If the packet data byte array is less than or equal to the
    // part 1 threshold length
    if (mAdvertisingPacketData_write.length <= MAX_NUM_BYTES_DATA_PART_1) {
      // Store the data to write as the entire byte array
      data_toWrite = mAdvertisingPacketData_write;
      // If the packet data byte array is greater than the
      // part 1 threshold length
    } else {
      // Store the data to write as the first part of the byte array
      // up to the threshold
      data_toWrite = Arrays.copyOfRange(mAdvertisingPacketData_write, 0, MAX_NUM_BYTES_DATA_PART_1);
    }
    // Write part 1 of the advertising packet to the beacon
    writeCharacteristic_beaconDataPart1(data_toWrite);
  }

  /**
   * Write the given data to the beacon's advertising packet.
   * This only writes part 1 which is up to 20 bytes
   * of the packet data.
   *
   * @param data First part of the data to write to the beacon; up to 20 bytes
   */
  private static void writeCharacteristic_beaconDataPart1(byte[] data) {
    // Get the characteristic for part 1 of the beacon data
    BluetoothGattCharacteristic characteristic_beaconDataPart1 = mBeaconBluetoothGattService.getCharacteristic(UUID_BEACON_DATA_PART_1);
    // Write the given byte array data to that characteristic
    characteristic_beaconDataPart1.setValue(data);
    // Write the updated characteristic via GATT
    mBluetoothGatt.writeCharacteristic(characteristic_beaconDataPart1);
  }

  /**
   * Write the given data to the beacon's advertising packet.
   * This only writes part 2 which is up to 8 bytes
   * of the packet data and is appended to the data from part 1.
   *
   * @param data Second part of the data to write to the beacon; up to 8 bytes.
   */
  private static void writeCharacteristic_beaconDataPart2(byte[] data) {
    // Get the characteristic for part 2 of the beacon data
    BluetoothGattCharacteristic characteristic_beaconDataPart2 = mBeaconBluetoothGattService.getCharacteristic(UUID_BEACON_DATA_PART_2);
    // Write the given byte array data to that characteristic
    characteristic_beaconDataPart2.setValue(data);
    // Write the updated characteristic via GATT
    mBluetoothGatt.writeCharacteristic(characteristic_beaconDataPart2);
  }

  /**
   * Run actions given that the data was successfully written
   * to part 1 of the beacon advertising packet.
   *
   * @param bluetoothGattCharacteristic Data part 1 characteristic that was written to the associated remote device.
   */
  private static void handleGattCharacteristicWrite_beaconDataPart1(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
    // If the length of the advertising packet is less than or equal to the part 1 threshold length
    if (mAdvertisingPacketData_write.length <= MAX_NUM_BYTES_DATA_PART_1) {
      onWriteComplete_beaconData();
      // If the length of the advertising packet is greater than the part 1 threshold length
    } else {
      // Get the second part of the data to write from the threshold length index
      // to the total length index of the advertising packet
      byte[] data_toWrite = Arrays.copyOfRange(mAdvertisingPacketData_write, MAX_NUM_BYTES_DATA_PART_1, mAdvertisingPacketData_write.length);
      // Write the given data to part 2 of the beacon advertising packet
      writeCharacteristic_beaconDataPart2(data_toWrite);
    }
  }

  /**
   * Run actions given that the data was successfully written
   * to part 2 of the beacon advertising packet.
   *
   * @param bluetoothGattCharacteristic Data part 2 characteristic that was written to the associated remote device.
   */
  private static void handleGattCharacteristicWrite_beaconDataPart2(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
    onWriteComplete_beaconData();
  }

  /**
   * Close out any running bluetooth services
   * being used by this fragment (notably the GATT service).
   */
  public static void shutDownConfigGatt() {
    if (mBluetoothGatt == null) {
      return;
    }
    mBluetoothGatt.close();
    mBluetoothGatt = null;
  }
}
