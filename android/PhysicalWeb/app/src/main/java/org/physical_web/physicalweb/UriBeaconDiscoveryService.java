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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import org.uribeacon.beacon.UriBeacon;
import org.uribeacon.scan.compat.BluetoothLeScannerCompat;
import org.uribeacon.scan.compat.BluetoothLeScannerCompatProvider;
import org.uribeacon.scan.compat.ScanCallback;
import org.uribeacon.scan.compat.ScanFilter;
import org.uribeacon.scan.compat.ScanResult;
import org.uribeacon.scan.compat.ScanSettings;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * This is the services that scans for beacons.
 * When the application loads, it checks
 * if the service is running and if it is not,
 * the applications creates the service (from MainActivity).
 * The service finds nearby ble beacons,
 * and stores a count of them.
 * Also, the service listens for screen on/off events
 * and start/stops the scanning accordingly.
 * Also, this service issues a notification
 * informing the user of nearby beacons.
 * As beaoncs are found and lost,
 * the notification is updated to reflect
 * the current number of nearby beacons.
 */

public class UriBeaconDiscoveryService extends Service {

  private static final String TAG = "UriBeaconDiscoveryService";
  private static final int ID_NOTIFICATION = 23;
  private ScreenBroadcastReceiver mScreenStateBroadcastReceiver;
  private HashSet<String> mDeviceAddressesFound;

  public UriBeaconDiscoveryService() {
  }

  private void initialize() {
    mDeviceAddressesFound = new HashSet<>();
    initializeScreenStateBroadcastReceiver();
    startSearchingForBeacons();
  }

  /**
   * Create the broadcast receiver that will listen
   * for screen on/off events
   */
  private void initializeScreenStateBroadcastReceiver() {
    mScreenStateBroadcastReceiver = new ScreenBroadcastReceiver();
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(Intent.ACTION_SCREEN_ON);
    intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
    registerReceiver(mScreenStateBroadcastReceiver, intentFilter);
  }


  /////////////////////////////////
  // accessors
  /////////////////////////////////

  private BluetoothLeScannerCompat getLeScanner() {
    return BluetoothLeScannerCompatProvider.getBluetoothLeScannerCompat(getApplicationContext());
  }


  /////////////////////////////////
  // callbacks
  /////////////////////////////////

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    initialize();
    //make sure the service keeps running
    return START_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    // TODO: Return the communication channel to the service.
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "onDestroy:  service exiting");
    stopSearchingForBeacons();
    unregisterReceiver(mScreenStateBroadcastReceiver);
    cancelNotification();
  }

  private final ScanCallback mScanCallback = new ScanCallback() {
    @Override
    public void onScanResult(int callbackType, ScanResult scanResult) {
      UriBeacon uriBeacon = UriBeacon.parseFromBytes(scanResult.getScanRecord().getBytes());
      if (uriBeacon != null) {
        String url = uriBeacon.getUriString();
        switch (callbackType) {
          case ScanSettings.CALLBACK_TYPE_FIRST_MATCH:
            if (mDeviceAddressesFound.add(url)) {
              updateNearbyBeaconsNotification();
            }
            break;
          case ScanSettings.CALLBACK_TYPE_MATCH_LOST:
            if (mDeviceAddressesFound.remove(url)) {
              updateNearbyBeaconsNotification();
            }
            break;
          default:
            Log.e(TAG, "Unrecognized callback type constant received: " + callbackType);
        }
      }
    }

    @Override
    public void onScanFailed(int errorCode) {
      Log.d(TAG, "onScanFailed  " + "errorCode: " + errorCode);
    }
  };

  /**
   * This is the class that listens for screen on/off events
   */
  private class ScreenBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      boolean isScreenOn = Intent.ACTION_SCREEN_ON.equals(intent.getAction());
      if (isScreenOn) {
        startSearchingForBeacons();
      } else {
        stopSearchingForBeacons();
      }
    }
  }

  /////////////////////////////////
  // utilities
  /////////////////////////////////

  private void startSearchingForBeacons() {
    Log.v(TAG, "startSearchingForBeacons");

    ScanSettings settings = new ScanSettings.Builder()
        .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH | ScanSettings.CALLBACK_TYPE_MATCH_LOST)
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
        .build();

    List<ScanFilter> filters = new ArrayList<>();

    ScanFilter filter =  new ScanFilter.Builder()
        .setServiceData(UriBeacon.URI_SERVICE_UUID,
            new byte[] {},
            new byte[] {})
        .build();

    filters.add(filter);

    boolean started = getLeScanner().startScan(filters, settings, mScanCallback);
    Log.v(TAG, started ? "... scan started" : "... scan NOT started");
  }

  private void stopSearchingForBeacons() {
    Log.v(TAG, "stopSearchingForBeacons");
    getLeScanner().stopScan(mScanCallback);
  }

  /**
   * Update the notification that displays
   * the number of nearby beacons.
   * If there are no beacons the notification
   * is removed.
   */
  private void updateNearbyBeaconsNotification() {
    // Create the notification builder
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
    // Set the notification icon
    builder.setSmallIcon(R.drawable.ic_notification);

    // Set the title
    String contentTitle = "";
    // Get the number of current nearby beacons
    int numNearbyBeacons = mDeviceAddressesFound.size();

    // If there are no nearby beacons
    if (numNearbyBeacons == 0) {
      // Remove the notification
      cancelNotification();
      return;
    }

    // Add the ending part of the title
    // which is either singular or plural
    // based on the number of beacons
    contentTitle += String.valueOf(numNearbyBeacons) + " ";
    Resources resources = getResources();
    contentTitle += resources.getQuantityString(R.plurals.numFoundBeacons, numNearbyBeacons, numNearbyBeacons);
    builder.setContentTitle(contentTitle);

    // Have the app launch when the user taps the notification
    Intent resultIntent = new Intent(this, MainActivity.class);
    resultIntent.putExtra("isFromUriBeaconDiscoveryService", true);
    int requestID = (int) System.currentTimeMillis();
    PendingIntent resultPendingIntent = PendingIntent.getActivity(this, requestID, resultIntent, 0);

    // Build the notification
    builder.setContentIntent(resultPendingIntent);
    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    mNotificationManager.notify(ID_NOTIFICATION, builder.build());
  }

  private void cancelNotification() {
    NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager.cancel(ID_NOTIFICATION);

  }
}

