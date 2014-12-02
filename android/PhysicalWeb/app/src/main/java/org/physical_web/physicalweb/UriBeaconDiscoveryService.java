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

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.RemoteViews;

import org.uribeacon.beacon.UriBeacon;
import org.uribeacon.scan.compat.BluetoothLeScannerCompat;
import org.uribeacon.scan.compat.BluetoothLeScannerCompatProvider;
import org.uribeacon.scan.compat.ScanCallback;
import org.uribeacon.scan.compat.ScanFilter;
import org.uribeacon.scan.compat.ScanResult;
import org.uribeacon.scan.compat.ScanSettings;
import org.uribeacon.scan.util.RangingUtils;
import org.uribeacon.scan.util.RegionResolver;

import java.util.ArrayList;
import java.util.HashMap;
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
 * As beacons are found and lost,
 * the notification is updated to reflect
 * the current number of nearby beacons.
 */

public class UriBeaconDiscoveryService extends Service implements MetadataResolver.MetadataResolverCallback {

  private static final String TAG = "UriBeaconDiscoveryService";
  private final ScanCallback mScanCallback = new ScanCallback() {
    @Override
    public void onScanResult(int callbackType, ScanResult scanResult) {
      switch (callbackType) {
        case ScanSettings.CALLBACK_TYPE_FIRST_MATCH:
          handleFoundDevice(scanResult);
          break;
        case ScanSettings.CALLBACK_TYPE_ALL_MATCHES:
          handleFoundDevice(scanResult);
          break;
        case ScanSettings.CALLBACK_TYPE_MATCH_LOST:
          handleLostDevice(scanResult);
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
  private static final String NOTIFICATION_GROUP_KEY = "URI_BEACON_NOTIFICATIONS";
  private static final int NEAREST_BEACON_NOTIFICATION_ID = 23;
  private static final int SECOND_NEAREST_BEACON_NOTIFICATION_ID = 24;
  private static final int SUMMARY_NOTIFICATION_ID = 25;
  private ScreenBroadcastReceiver mScreenStateBroadcastReceiver;
  private RegionResolver mRegionResolver;
  private NotificationManagerCompat mNotificationManager;
  private HashMap<String, MetadataResolver.UrlMetadata> mUrlToUrlMetadata;
  private HashMap<String, String> mDeviceAddressToUrl;

  public UriBeaconDiscoveryService() {
  }

  private void initialize() {
    mRegionResolver = new RegionResolver();
    mNotificationManager = NotificationManagerCompat.from(this);
    mUrlToUrlMetadata = new HashMap<>();
    mDeviceAddressToUrl = new HashMap<>();
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

  private BluetoothLeScannerCompat getLeScanner() {
    return BluetoothLeScannerCompatProvider.getBluetoothLeScannerCompat(getApplicationContext());
  }

  @Override
  public void onCreate() {
    super.onCreate();
    initialize();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
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
    mUrlToUrlMetadata = new HashMap<>();
    mDeviceAddressToUrl = new HashMap<>();
    cancelNotifications();
  }

  @Override
  public void onUrlMetadataReceived(String url, MetadataResolver.UrlMetadata urlMetadata) {
    mUrlToUrlMetadata.put(url, urlMetadata);
    updateNotifications();
  }

  @Override
  public void onDemoUrlMetadataReceived(String url, MetadataResolver.UrlMetadata urlMetadata) {

  }

  @Override
  public void onUrlMetadataIconReceived() {
    updateNotifications();
  }

  private void startSearchingForBeacons() {
    Log.v(TAG, "startSearchingForBeacons");

    ScanSettings settings = new ScanSettings.Builder()
        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
        .build();

    List<ScanFilter> filters = new ArrayList<>();

    ScanFilter filter = new ScanFilter.Builder()
        .setServiceData(UriBeacon.URI_SERVICE_UUID,
            new byte[]{},
            new byte[]{})
        .build();

    filters.add(filter);

    boolean started = getLeScanner().startScan(filters, settings, mScanCallback);
    Log.v(TAG, started ? "... scan started" : "... scan NOT started");
  }

  private void stopSearchingForBeacons() {
    Log.v(TAG, "stopSearchingForBeacons");
    getLeScanner().stopScan(mScanCallback);
  }

  private void handleFoundDevice(final ScanResult scanResult) {
    UriBeacon uriBeacon = UriBeacon.parseFromBytes(scanResult.getScanRecord().getBytes());
    if (uriBeacon != null) {
      final String address = scanResult.getDevice().getAddress();
      int rxPower = scanResult.getRssi();
      mRegionResolver.onUpdate(address, rxPower, RangingUtils.DEFAULT_TX_POWER_LEVEL);

      String url = uriBeacon.getUriString();
      // If we haven't yet stored this url in the metadata hash table
      if (!mUrlToUrlMetadata.containsKey(url)) {
        Log.d(TAG, "handleFoundDevice: first match");
        // Store this url and fetch the metadata
        mUrlToUrlMetadata.put(url, null);
        MetadataResolver.findUrlMetadata(this, UriBeaconDiscoveryService.this, url);
        // If we have already seen this url
      } else {
        Log.d(TAG, "handleFoundDevice: updating");
        updateNotifications();
      }

      // If we haven't yet stored this url in the device address hash table
      if (!mDeviceAddressToUrl.containsKey(address)) {
        // Store this address and associated url
        mDeviceAddressToUrl.put(address, url);
      }
    }
  }

  private void handleLostDevice(ScanResult scanResult) {
    UriBeacon uriBeacon = UriBeacon.parseFromBytes(scanResult.getScanRecord().getBytes());
    if (uriBeacon != null) {
      String address = scanResult.getDevice().getAddress();
      Log.i(TAG, String.format("handleLostDevice: %s", address));
      mRegionResolver.onLost(address);
      mDeviceAddressToUrl.remove(address);
      String url = uriBeacon.getUriString();
      mUrlToUrlMetadata.remove(url);
      updateNotifications();
    }
  }

  /**
   * Create a new set of notifications or update those existing
   */
  private void updateNotifications() {
    ArrayList<String> sortedBeacons = sortBeaconsByRegion();

    // If no beacons have been found
    if (sortedBeacons.size() == 0) {
      // Remove all existing notifications
      cancelNotifications();
      return;
    }
    // If at least one beacon has been found
    if (sortedBeacons.size() > 0) {
      // Create or update a notification for this beacon
      updateNearbyBeaconNotification(sortedBeacons.get(0), NEAREST_BEACON_NOTIFICATION_ID);
    }
    // If at least two beacons have been found
    if (sortedBeacons.size() > 1) {
      // Create or update a notification for this beacon
      updateNearbyBeaconNotification(sortedBeacons.get(1), SECOND_NEAREST_BEACON_NOTIFICATION_ID);
      // Create a summary notification for both beacon notifications
      updateSummaryNotification(sortedBeacons);
    }
  }

  /**
   * Sort the beacons into a single list, where the beacons are ordered by
   * near, mid, and far regions
   */
  private ArrayList<String> sortBeaconsByRegion() {
    HashMap<Integer, ArrayList<String>> addressesByRegion = new HashMap<>();
    addressesByRegion.put(RangingUtils.Region.NEAR, new ArrayList<String>());
    addressesByRegion.put(RangingUtils.Region.MID, new ArrayList<String>());
    addressesByRegion.put(RangingUtils.Region.FAR, new ArrayList<String>());
    for (String address : mDeviceAddressToUrl.keySet()) {
      int region = mRegionResolver.getRegion(address);
      addressesByRegion.get(region).add(address);
    }

    ArrayList<String> sortedBeacons = new ArrayList<>();
    sortedBeacons.addAll(addressesByRegion.get(RangingUtils.Region.NEAR));
    sortedBeacons.addAll(addressesByRegion.get(RangingUtils.Region.MID));
    sortedBeacons.addAll(addressesByRegion.get(RangingUtils.Region.FAR));

    return sortedBeacons;
  }

  /**
   * Create or update a notification with the given id
    for the beacon with the given address
   */
  private void updateNearbyBeaconNotification(String address, int notificationId) {
    String url = mDeviceAddressToUrl.get(address);
    MetadataResolver.UrlMetadata urlMetadata = mUrlToUrlMetadata.get(url);
    if (urlMetadata == null) {
      return;
    }

    // Create an intent that will open the browser to the beacon's url
    // if the user taps on the notification
    Intent navigateToBeaconUrlIntent = new Intent(Intent.ACTION_VIEW);
    if (!URLUtil.isNetworkUrl(url)) {
      url = "http://" + url;
    }
    navigateToBeaconUrlIntent.setData(Uri.parse(url));
    int requestID = (int) System.currentTimeMillis();
    PendingIntent pendingIntent = PendingIntent.getActivity(this, requestID, navigateToBeaconUrlIntent, 0);

    String title = urlMetadata.title;
    String description = urlMetadata.description;
    Bitmap icon = urlMetadata.icon;
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
    builder.setSmallIcon(R.drawable.ic_notification)
        .setLargeIcon(icon)
        .setContentTitle(title)
        .setContentText(description)
        .setPriority(NotificationCompat.PRIORITY_MIN)
        .setGroup(NOTIFICATION_GROUP_KEY)
        .setContentIntent(pendingIntent);
    Notification notification = builder.build();

    mNotificationManager.notify(notificationId, notification);
  }

  /**
   * Create or update the a single notification that is a collapsed version
   * of the top two beacon notifications
   */
  private void updateSummaryNotification(ArrayList<String> sortedBeacons) {
    int numNearbyBeacons = sortedBeacons.size();
    String contentTitle = String.valueOf(numNearbyBeacons) + " ";
    Resources resources = getResources();
    contentTitle += " " + resources.getQuantityString(R.plurals.numFoundBeacons, numNearbyBeacons, numNearbyBeacons);
    String contentText = "Pull down to see them.";
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
    Notification notification = builder.setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(contentTitle)
        .setContentText(contentText)
        .setSmallIcon(R.drawable.ic_notification)
        .setGroup(NOTIFICATION_GROUP_KEY)
        .setGroupSummary(true)
        .setPriority(NotificationCompat.PRIORITY_MIN)
        .build();

    // Create the big view for the notification (viewed by pulling down)
    RemoteViews remoteViews = updateSummaryNotificationRemoteViews(sortedBeacons);
    notification.bigContentView = remoteViews;

    mNotificationManager.notify(SUMMARY_NOTIFICATION_ID, notification);
  }

  /**
   * Create the big view for the summary notification
   */
  private RemoteViews updateSummaryNotificationRemoteViews(ArrayList<String> sortedBeacons) {
    RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_big_view);

    // Fill in the data for the top two beacon views
    updateSummaryNotificationRemoteViewsFirstBeacon(sortedBeacons.get(0), remoteViews);
    updateSummaryNotificationRemoteViewsSecondBeacon(sortedBeacons.get(1), remoteViews);

    // Create an intent that will open the browser to the beacon's url
    // if the user taps the notification
    // TODO: Use a clickListener on the VIEW MORE button to do this
    Intent intent_returnToApp = new Intent(this, MainActivity.class);
    intent_returnToApp.putExtra("isFromUriBeaconDiscoveryService", true);
    int requestID = (int) System.currentTimeMillis();
    PendingIntent pendingIntent = PendingIntent.getActivity(this, requestID, intent_returnToApp, 0);
    remoteViews.setOnClickPendingIntent(R.id.otherBeaconsLayout, pendingIntent);

    return remoteViews;
  }

  private void updateSummaryNotificationRemoteViewsFirstBeacon(String beaconAddress, RemoteViews remoteViews) {
    String url_firstBeacon = mDeviceAddressToUrl.get(beaconAddress);
    MetadataResolver.UrlMetadata urlMetadata_firstBeacon = mUrlToUrlMetadata.get(url_firstBeacon);
    if (urlMetadata_firstBeacon != null) {
      String title = mUrlToUrlMetadata.get(url_firstBeacon).title;
      String description = mUrlToUrlMetadata.get(url_firstBeacon).description;
      Bitmap icon = mUrlToUrlMetadata.get(url_firstBeacon).icon;
      remoteViews.setImageViewBitmap(R.id.icon_firstBeacon, icon);
      remoteViews.setTextViewText(R.id.title_firstBeacon, title);
      remoteViews.setTextViewText(R.id.url_firstBeacon, url_firstBeacon);
      remoteViews.setTextViewText(R.id.description_firstBeacon, description);

      // Create an intent that will open the browser to the beacon's url
      // if the user taps the notification
      if (!URLUtil.isNetworkUrl(url_firstBeacon)) {
        url_firstBeacon = "http://" + url_firstBeacon;
      }
      Intent intent_firstBeacon = new Intent(Intent.ACTION_VIEW);
      intent_firstBeacon.setData(Uri.parse(url_firstBeacon));
      int requestID = (int) System.currentTimeMillis();
      PendingIntent pendingIntent = PendingIntent.getActivity(this, requestID, intent_firstBeacon, 0);
      remoteViews.setOnClickPendingIntent(R.id.first_beacon_main_layout, pendingIntent);

      remoteViews.setViewVisibility(R.id.firstBeaconLayout, View.VISIBLE);
    } else {
      remoteViews.setViewVisibility(R.id.firstBeaconLayout, View.GONE);
    }
  }

  private void updateSummaryNotificationRemoteViewsSecondBeacon(String beaconAddress, RemoteViews remoteViews) {
    String url_secondBeacon = mDeviceAddressToUrl.get(beaconAddress);
    MetadataResolver.UrlMetadata urlMetadata_secondBeacon = mUrlToUrlMetadata.get(url_secondBeacon);
    if (urlMetadata_secondBeacon != null) {
      String title = mUrlToUrlMetadata.get(url_secondBeacon).title;
      String description = mUrlToUrlMetadata.get(url_secondBeacon).description;
      Bitmap icon = mUrlToUrlMetadata.get(url_secondBeacon).icon;
      remoteViews.setImageViewBitmap(R.id.icon_secondBeacon, icon);
      remoteViews.setTextViewText(R.id.title_secondBeacon, title);
      remoteViews.setTextViewText(R.id.url_secondBeacon, url_secondBeacon);
      remoteViews.setTextViewText(R.id.description_secondBeacon, description);

      // Create an intent that will open the browser to the beacon's url
      // if the user taps the notification
      if (!URLUtil.isNetworkUrl(url_secondBeacon)) {
        url_secondBeacon = "http://" + url_secondBeacon;
      }
      Intent intent_secondBeacon = new Intent(Intent.ACTION_VIEW);
      intent_secondBeacon.setData(Uri.parse(url_secondBeacon));
      int requestID = (int) System.currentTimeMillis();
      PendingIntent pendingIntent = PendingIntent.getActivity(this, requestID, intent_secondBeacon, 0);
      remoteViews.setOnClickPendingIntent(R.id.second_beacon_main_layout, pendingIntent);

      remoteViews.setViewVisibility(R.id.secondBeaconLayout, View.VISIBLE);
    } else {
      remoteViews.setViewVisibility(R.id.secondBeaconLayout, View.GONE);
    }
  }


  private void cancelNotifications() {
    Log.d(TAG, "cancelNotifications");
    mNotificationManager.cancel(SUMMARY_NOTIFICATION_ID);
    mNotificationManager.cancel(SECOND_NEAREST_BEACON_NOTIFICATION_ID);
    mNotificationManager.cancel(NEAREST_BEACON_NOTIFICATION_ID);
  }

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
}

