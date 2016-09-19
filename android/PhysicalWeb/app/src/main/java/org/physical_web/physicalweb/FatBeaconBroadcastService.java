/*
 * Copyright 2016 Google Inc. All rights reserved.
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

import org.physical_web.physicalweb.ble.AdvertiseDataUtils;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.UUID;

/**
 * Shares a Web page via bluetooth.
 * Lastly, it surfaces a persistent notification whenever a FatBeacon is currently being broadcast.
 **/
@TargetApi(21)
public class FatBeaconBroadcastService extends Service {

  private static final String TAG = FatBeaconBroadcastService.class.getSimpleName();
  private static final String SERVICE_UUID = "ae5946d4-e587-4ba8-b6a5-a97cca6affd3";
  private static final UUID CHARACTERISTIC_WEBPAGE_UUID = UUID.fromString(
      "d1a517f0-2499-46ca-9ccc-809bc1c966fa");
  private static final String PREVIOUS_BROADCAST_INFO_KEY = "previousInfo";
  private static final int BROADCASTING_NOTIFICATION_ID = 8;
  private boolean mStartedByRestart;
  private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
  private NotificationManagerCompat mNotificationManager;
  private String mDisplayInfo;
  private BluetoothManager mBluetoothManager;
  private BluetoothGattServer mGattServer;
  private byte[] data;
  public static final String TITLE_KEY = "title";
  public static final String URI_KEY = "uri";

  /*
    * Callback handles all incoming requests from GATT clients.
    * From connections to read/write requests.
    */
  private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
    private int transferSpeed = 20;
    private int queueOffset;
    @Override
    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
      super.onConnectionStateChange(device, status, newState);

      if (newState == BluetoothProfile.STATE_CONNECTED) {
        Log.i(TAG, "Connected to device " + device.getAddress());
        queueOffset = 0;
      } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
        Log.i(TAG, "Disconnected from device " + device.getAddress());
      }
    }

    @Override
    public void onCharacteristicReadRequest(BluetoothDevice device,
        int requestId,
        int offset,
        BluetoothGattCharacteristic characteristic) {
      super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
      Log.i(TAG, "onCharacteristicReadRequest " + characteristic.getUuid().toString());

      if (CHARACTERISTIC_WEBPAGE_UUID.equals(characteristic.getUuid())) {
        Log.d(TAG, "Data length:" + data.length + ", offset:" + queueOffset);
        if (queueOffset < data.length) {
          int end = queueOffset + transferSpeed >= data.length ?
              data.length : queueOffset + transferSpeed;
          Log.d(TAG, "Data length:" + data.length + ", offset:" + queueOffset + ", end:" + end);
          mGattServer.sendResponse(device,
              requestId,
              BluetoothGatt.GATT_SUCCESS,
              0,
              Arrays.copyOfRange(data, queueOffset, end));
          queueOffset = end;
        } else if (queueOffset == data.length) {
          mGattServer.sendResponse(device,
              requestId,
              BluetoothGatt.GATT_SUCCESS,
              0,
              new byte[]{});
          queueOffset++;
        }
      }

            /*
             * Unless the characteristic supports WRITE_NO_RESPONSE,
             * always send a response back for any request.
             */
      mGattServer.sendResponse(device,
          requestId,
          BluetoothGatt.GATT_FAILURE,
          0,
          null);
    }

    @Override
    public void onMtuChanged(BluetoothDevice device, int mtu) {
      super.onMtuChanged(device, mtu);
      transferSpeed = mtu - 5;
    }
  };


  private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();

      if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
        switch (state) {
          case BluetoothAdapter.STATE_OFF:
            stopSelf();
            break;
          default:

        }
      }
    }
  };

  /////////////////////////////////
  // callbacks
  /////////////////////////////////

  @Override
  public void onCreate() {
    super.onCreate();
    mBluetoothLeAdvertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
    mNotificationManager = NotificationManagerCompat.from(this);
    mBluetoothManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    fetchBroadcastData(intent);
    if (mDisplayInfo == null || data == null) {
      stopSelf();
      return START_STICKY;
    }
    IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    registerReceiver(mReceiver, filter);
    broadcastUrl();
    initGattServer();
    return START_STICKY;
  }

  private void fetchBroadcastData(Intent intent) {
    if ((mStartedByRestart = intent == null)) {
      mDisplayInfo = PreferenceManager.getDefaultSharedPreferences(this)
          .getString(PREVIOUS_BROADCAST_INFO_KEY, null);
      return;
    }
    mDisplayInfo = intent.getStringExtra(TITLE_KEY);
    String intentUri = intent.getStringExtra(URI_KEY);
    if (intentUri == null) {
      return;
    }
    try {
      data = Utils.getBytes(getContentResolver().openInputStream(Uri.parse(intentUri)));
    } catch (IOException e) {
      data = null;
      Log.e(TAG, "Error reading file");
    }
    PreferenceManager.getDefaultSharedPreferences(this).edit()
        .putString(PREVIOUS_BROADCAST_INFO_KEY, mDisplayInfo)
        .apply();
  }

  @Override
  public void onDestroy() {
    unregisterReceiver(stopServiceReceiver);
    unregisterReceiver(mReceiver);
    disableUrlBroadcasting();
    super.onDestroy();
  }

  // Fires when user swipes away app from the recent apps list
  @Override
  public void onTaskRemoved (Intent rootIntent) {
    stopSelf();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  // The callbacks for the ble advertisement events
  private final AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {

    // Fires when the URL is successfully being advertised
    @Override
    public void onStartSuccess(AdvertiseSettings advertiseSettings) {
      Utils.createBroadcastNotification(FatBeaconBroadcastService.this, stopServiceReceiver,
          BROADCASTING_NOTIFICATION_ID, getString(R.string.fatbeacon_notification_title),
          mDisplayInfo, "fatBeaconFilter");
      if (!mStartedByRestart) {
        Toast.makeText(getApplicationContext(), R.string.fatbeacon_broadcasting_confirmation,
            Toast.LENGTH_LONG).show();
      }
    }

    // Fires when the URL could not be advertised
    @Override
    public void onStartFailure(int result) {
      Log.d(TAG, "onStartFailure" + result);
    }
  };



  /////////////////////////////////
  // utilities
  /////////////////////////////////

  // Broadcast via bluetooth the stored URL
  private void broadcastUrl() {
    byte[] bytes = null;
    try {
      bytes = mDisplayInfo.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      Log.e(TAG, "Could not encode URL", e);
      return;
    }
    AdvertiseData advertiseData = AdvertiseDataUtils.getFatBeaconAdvertisementData(bytes);
    AdvertiseSettings advertiseSettings = AdvertiseDataUtils.getAdvertiseSettings(true);
    mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
    mBluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, mAdvertiseCallback);
  }

  // Turn off URL broadcasting
  private void disableUrlBroadcasting() {
    mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
    mNotificationManager.cancel(BROADCASTING_NOTIFICATION_ID);
  }

  private BroadcastReceiver stopServiceReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      stopSelf();
    }
  };

  private void initGattServer() {
    mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
    BluetoothGattService service = new BluetoothGattService(UUID.fromString(SERVICE_UUID),
        BluetoothGattService.SERVICE_TYPE_PRIMARY);
    BluetoothGattCharacteristic webpage = new BluetoothGattCharacteristic(
        CHARACTERISTIC_WEBPAGE_UUID, BluetoothGattCharacteristic.PROPERTY_READ,
        BluetoothGattCharacteristic.PERMISSION_READ);
    service.addCharacteristic(webpage);
    mGattServer.addService(service);
  }
}
