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
 * This is the services that scans for devices.
 * When the application loads, it checks
 * if the service is running and if it is not,
 * the applications creates the service (from MainActivity).
 * The service finds nearby ble devices,
 * and stores a count of them.
 * Also, the service listens for screen on/off events
 * and start/stops the scanning accordingly.
 * Also, this service issues a notification
 * informing the user of nearby devices.
 * As devices are found and lost,
 * the notification is updated to reflect
 * the current number of nearby devices.
 */

public class DeviceDiscoveryService extends Service {

  private static String TAG = "DeviceDiscoveryService";
  private static int ID_NOTIFICATION = 23;
  private ScreenBroadcastReceiver mScreenStateBroadcastReceiver;
  private HashSet<String> mDeviceAddressesFound;

  public DeviceDiscoveryService() {
  }

  private void initialize() {
    mDeviceAddressesFound = new HashSet<>();
    initializeScreenStateBroadcastReceiver();
    startSearchingForDevices();
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
    stopSearchingForDevices();
  }

  private final ScanCallback mScanCallback = new ScanCallback() {
    @Override
    public void onScanResult(int callbackType, ScanResult scanResult) {
      String address = scanResult.getDevice().getAddress();
      switch (callbackType) {
        case ScanSettings.CALLBACK_TYPE_FIRST_MATCH:
          if (mDeviceAddressesFound.add(address)) {
            updateNearbyDevicesNotification();
          }
          break;
        case ScanSettings.CALLBACK_TYPE_MATCH_LOST:
          if (mDeviceAddressesFound.remove(address)) {
            updateNearbyDevicesNotification();
          }
          break;
        default:
          Log.e(TAG, "Unrecognized callback type constant received: " + callbackType);
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
        startSearchingForDevices();
      } else {
        stopSearchingForDevices();
      }
    }
  }

  /////////////////////////////////
  // utilities
  /////////////////////////////////

  private void startSearchingForDevices() {
    Log.v(TAG, "startSearchingForDevices");

    ScanSettings settings = new ScanSettings.Builder()
        .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH | ScanSettings.CALLBACK_TYPE_MATCH_LOST)
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
        .build();

    List<ScanFilter> filters = new ArrayList<>();

    ScanFilter.Builder builder = new ScanFilter.Builder()
        .setServiceData(UriBeacon.URI_SERVICE_UUID,
            new byte[] {},
            new byte[] {});
    filters.add(builder.build());

    boolean started = getLeScanner().startScan(filters, settings, mScanCallback);
    Log.v(TAG, started ? "... scan started" : "... scan NOT started");
  }

  private void stopSearchingForDevices() {
    Log.v(TAG, "stopSearchingForDevices");
    getLeScanner().stopScan(mScanCallback);
  }

  /**
   * Update the notification that displays
   * the number of nearby devices.
   * If there are no devices the notification
   * is removed.
   */
  private void updateNearbyDevicesNotification() {
    // Create the notification builder
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
    // Set the notification icon
    builder.setSmallIcon(R.drawable.ic_notification);

    // Set the title
    String contentTitle = "";
    // Get the number of current nearby devices
    int numNearbyDevices = mDeviceAddressesFound.size();

    // If there are no nearby devices
    if (numNearbyDevices == 0) {
      // Remove the notification
      NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      mNotificationManager.cancel(ID_NOTIFICATION);
      return;
    }

    // Add the ending part of the title
    // which is either singular or plural
    // based on the number of devices
    contentTitle += String.valueOf(numNearbyDevices) + " ";
    Resources resources = getResources();
    contentTitle += resources.getQuantityString(R.plurals.numFoundBeacons, numNearbyDevices, numNearbyDevices);
    builder.setContentTitle(contentTitle);

    // Have the app launch when the user taps the notification
    Intent resultIntent = new Intent(this, MainActivity.class);
    int requestID = (int) System.currentTimeMillis();
    PendingIntent resultPendingIntent = PendingIntent.getActivity(this, requestID, resultIntent, 0);

    // Build the notification
    builder.setContentIntent(resultPendingIntent);
    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    mNotificationManager.notify(ID_NOTIFICATION, builder.build());
  }
}

