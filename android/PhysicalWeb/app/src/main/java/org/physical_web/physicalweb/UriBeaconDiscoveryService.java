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
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
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
import org.uribeacon.scan.util.RegionResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This is a service that scans for nearby uriBeacons.
 * It is created by MainActivity.
 * It finds nearby ble beacons,
 * and stores a count of them.
 * It also listens for screen on/off events
 * and start/stops the scanning accordingly.
 * It also silently issues a notification
 * informing the user of nearby beacons.
 * As beacons are found and lost,
 * the notification is updated to reflect
 * the current number of nearby beacons.
 */

public class UriBeaconDiscoveryService extends Service implements MetadataResolver.MetadataResolverCallback, MdnsUrlDiscoverer.MdnsUrlDiscovererCallback {

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
  private static final int TIMEOUT_FOR_OLD_BEACONS = 2;
  private static final int NON_LOLLIPOP_NOTIFICATION_TITLE_COLOR = Color.parseColor("#ffffff");
  private static final int NON_LOLLIPOP_NOTIFICATION_URL_COLOR = Color.parseColor("#999999");
  private static final int NON_LOLLIPOP_NOTIFICATION_SNIPPET_COLOR = Color.parseColor("#999999");
  private static final int NOTIFICATION_PRIORITY = NotificationCompat.PRIORITY_LOW;
  private ScreenBroadcastReceiver mScreenStateBroadcastReceiver;
  private RegionResolver mRegionResolver;
  private NotificationManagerCompat mNotificationManager;
  private HashMap<String, MetadataResolver.UrlMetadata> mUrlToUrlMetadata;
  private List<String> mSortedDevices;
  private HashMap<String, String> mDeviceAddressToUrl;
  private MdnsUrlDiscoverer mMdnsUrlDiscoverer;
  private Comparator<String> mComparator = new Comparator<String>() {
    @Override
    public int compare(String address, String otherAddress) {
      // Sort by the stabilized region of the device, unless
      // they are the same, in which case sort by distance.
      final String nearest = mRegionResolver.getNearestAddress();
      if (address.equals(nearest)) {
        return -1;
      }
      if (otherAddress.equals(nearest)) {
        return 1;
      }
      int r1 = mRegionResolver.getRegion(address);
      int r2 = mRegionResolver.getRegion(otherAddress);
      if (r1 != r2) {
        return ((Integer) r1).compareTo(r2);
      }
      // The two devices are in the same region, sort by device address.
      return address.compareTo(otherAddress);
    }
  };

  public UriBeaconDiscoveryService() {
  }

  private static String generateMockBluetoothAddress(int hashCode) {
    String mockAddress = "00:11";
    for (int i = 0; i < 4; i++) {
      mockAddress += String.format(":%02X", hashCode & 0xFF);
      hashCode = hashCode >> 8;
    }
    return mockAddress;
  }

  private void initialize() {
    mNotificationManager = NotificationManagerCompat.from(this);
    mMdnsUrlDiscoverer = new MdnsUrlDiscoverer(this, UriBeaconDiscoveryService.this);
    initializeScreenStateBroadcastReceiver();
  }

  private void initializeLists() {
    mRegionResolver = new RegionResolver();
    mUrlToUrlMetadata = new HashMap<>();
    mSortedDevices = null;
    mDeviceAddressToUrl = new HashMap<>();
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
    // Since sometimes the lists have values when onStartCommand gets called
    initializeLists();
    // Start scanning only if the screen is on
    PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
    if (powerManager.isScreenOn()) {
      startSearchingForUriBeacons();
      mMdnsUrlDiscoverer.startScanning();
    }

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
    stopSearchingForUriBeacons();
    mMdnsUrlDiscoverer.stopScanning();
    unregisterReceiver(mScreenStateBroadcastReceiver);
    mNotificationManager.cancelAll();
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

  @Override
  public void onMdnsUrlFound(String url) {
    if (!mUrlToUrlMetadata.containsKey(url)) {
      // Fabricate the device values so that we can show these ersatz beacons
      String mockAddress = generateMockBluetoothAddress(url.hashCode());
      int mockRssi = 0;
      int mockTxPower = 0;
      mUrlToUrlMetadata.put(url, null);
      mDeviceAddressToUrl.put(mockAddress, url);
      mRegionResolver.onUpdate(mockAddress, mockRssi, mockTxPower);
      MetadataResolver.findUrlMetadata(this, UriBeaconDiscoveryService.this, url);
    }
  }

  private void startSearchingForUriBeacons() {
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

  private void stopSearchingForUriBeacons() {
    getLeScanner().stopScan(mScanCallback);
  }

  private void handleFoundDevice(ScanResult scanResult) {
    long timeStamp = scanResult.getTimestampNanos();
    long now = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
    if (now - timeStamp < TimeUnit.SECONDS.toNanos(TIMEOUT_FOR_OLD_BEACONS)) {
      UriBeacon uriBeacon = UriBeacon.parseFromBytes(scanResult.getScanRecord().getBytes());
      if (uriBeacon != null) {
        String address = scanResult.getDevice().getAddress();
        int rssi = scanResult.getRssi();
        int txPowerLevel = uriBeacon.getTxPowerLevel();
        String url = uriBeacon.getUriString();
        // If we haven't yet seen this url
        if (!mUrlToUrlMetadata.containsKey(url)) {
          mUrlToUrlMetadata.put(url, null);
          mDeviceAddressToUrl.put(address, url);
          // Fetch the metadata for this url
          MetadataResolver.findUrlMetadata(this, UriBeaconDiscoveryService.this, url);
        }
        // Update the ranging data
        mRegionResolver.onUpdate(address, rssi, txPowerLevel);
      }
    }
  }

  /**
   * Create a new set of notifications or update those existing
   */
  private void updateNotifications() {
    mSortedDevices = getSortedBeaconsWithMetadata();

    // If no beacons have been found
    if (mSortedDevices.size() == 0) {
      // Remove all existing notifications
      mNotificationManager.cancelAll();
      return;
    }

    // If at least two beacons have been found
    if (mSortedDevices.size() > 1) {
      // Create a summary notification for both beacon notifications.
      // Do this first so that we don't first show the individual notifications
      updateSummaryNotification();
      // Create or update a notification for this beacon
      updateNearbyBeaconNotification(mDeviceAddressToUrl.get(mSortedDevices.get(1)),
          SECOND_NEAREST_BEACON_NOTIFICATION_ID);
    }

    // If at least one beacon has been found
    if (mSortedDevices.size() > 0) {
      // Create or update a notification for this beacon
      updateNearbyBeaconNotification(mDeviceAddressToUrl.get(mSortedDevices.get(0)),
          NEAREST_BEACON_NOTIFICATION_ID);
    }
  }

  private ArrayList<String> getSortedBeaconsWithMetadata() {
    ArrayList<String> unSorted = new ArrayList<>(mDeviceAddressToUrl.size());
    for (String key : mDeviceAddressToUrl.keySet()) {
      if (mUrlToUrlMetadata.get(mDeviceAddressToUrl.get(key)) != null) {
        unSorted.add(key);
      }
    }
    Collections.sort(unSorted, mComparator);
    return unSorted;
  }

  /**
   * Create or update a notification with the given id
   * for the beacon with the given address
   */
  private void updateNearbyBeaconNotification(String url, int notificationId) {
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
    // Route through the proxy server go link
    url = MetadataResolver.createUrlProxyGoLink(url);
    navigateToBeaconUrlIntent.setData(Uri.parse(url));
    int requestID = (int) System.currentTimeMillis();
    PendingIntent pendingIntent = PendingIntent.getActivity(this, requestID,
        navigateToBeaconUrlIntent, 0);

    String title = urlMetadata.title;
    String description = urlMetadata.description;
    Bitmap icon = urlMetadata.icon;
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
    builder.setSmallIcon(R.drawable.ic_notification)
        .setLargeIcon(icon)
        .setContentTitle(title)
        .setContentText(description)
        .setPriority(NOTIFICATION_PRIORITY)
        .setGroup(NOTIFICATION_GROUP_KEY)
        .setContentIntent(pendingIntent);
    Notification notification = builder.build();

    mNotificationManager.notify(notificationId, notification);
  }

  /**
   * Create or update the a single notification that is a collapsed version
   * of the top two beacon notifications
   */
  private void updateSummaryNotification() {
    int numNearbyBeacons = mSortedDevices.size();
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
        .setPriority(NOTIFICATION_PRIORITY)
        .build();

    // Create the big view for the notification (viewed by pulling down)
    RemoteViews remoteViews = updateSummaryNotificationRemoteViews();
    notification.bigContentView = remoteViews;

    mNotificationManager.notify(SUMMARY_NOTIFICATION_ID, notification);
  }

  /**
   * Create the big view for the summary notification
   */
  private RemoteViews updateSummaryNotificationRemoteViews() {
    RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_big_view);

    // Fill in the data for the top two beacon views
    updateSummaryNotificationRemoteViewsFirstBeacon(mDeviceAddressToUrl.get(mSortedDevices.get(0)), remoteViews);
    updateSummaryNotificationRemoteViewsSecondBeacon(mDeviceAddressToUrl.get(mSortedDevices.get(1)), remoteViews);

    // Create an intent that will open the browser to the beacon's url
    // if the user taps the notification
    // TODO: Use a clickListener on the VIEW MORE button to do this
    Intent intent_returnToApp = new Intent(this, MainActivity.class);
    int requestID = (int) System.currentTimeMillis();
    PendingIntent pendingIntent = PendingIntent.getActivity(this, requestID, intent_returnToApp, 0);
    remoteViews.setOnClickPendingIntent(R.id.otherBeaconsLayout, pendingIntent);

    return remoteViews;
  }

  private void updateSummaryNotificationRemoteViewsFirstBeacon(String url, RemoteViews remoteViews) {
    MetadataResolver.UrlMetadata urlMetadata_firstBeacon = mUrlToUrlMetadata.get(url);
    if (urlMetadata_firstBeacon != null) {
      String title = mUrlToUrlMetadata.get(url).title;
      String description = mUrlToUrlMetadata.get(url).description;
      Bitmap icon = mUrlToUrlMetadata.get(url).icon;
      remoteViews.setImageViewBitmap(R.id.icon_firstBeacon, icon);
      remoteViews.setTextViewText(R.id.title_firstBeacon, title);
      remoteViews.setTextViewText(R.id.url_firstBeacon, url);
      remoteViews.setTextViewText(R.id.description_firstBeacon, description);
      // Recolor notifications to have light text for non-Lollipop devices
      if (!(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)) {
        remoteViews.setTextColor(R.id.title_firstBeacon, NON_LOLLIPOP_NOTIFICATION_TITLE_COLOR);
        remoteViews.setTextColor(R.id.url_firstBeacon, NON_LOLLIPOP_NOTIFICATION_URL_COLOR);
        remoteViews.setTextColor(R.id.description_firstBeacon, NON_LOLLIPOP_NOTIFICATION_SNIPPET_COLOR);
      }

      // Create an intent that will open the browser to the beacon's url
      // if the user taps the notification
      if (!URLUtil.isNetworkUrl(url)) {
        url = "http://" + url;
      }
      // Route through the proxy server go link
      url = MetadataResolver.createUrlProxyGoLink(url);

      Intent intent_firstBeacon = new Intent(Intent.ACTION_VIEW);
      intent_firstBeacon.setData(Uri.parse(url));
      int requestID = (int) System.currentTimeMillis();
      PendingIntent pendingIntent = PendingIntent.getActivity(this, requestID, intent_firstBeacon, 0);
      remoteViews.setOnClickPendingIntent(R.id.first_beacon_main_layout, pendingIntent);
      remoteViews.setViewVisibility(R.id.firstBeaconLayout, View.VISIBLE);
    } else {
      remoteViews.setViewVisibility(R.id.firstBeaconLayout, View.GONE);
    }
  }

  private void updateSummaryNotificationRemoteViewsSecondBeacon(String url, RemoteViews remoteViews) {
    MetadataResolver.UrlMetadata urlMetadata_secondBeacon = mUrlToUrlMetadata.get(url);
    if (urlMetadata_secondBeacon != null) {
      String title = mUrlToUrlMetadata.get(url).title;
      String description = mUrlToUrlMetadata.get(url).description;
      Bitmap icon = mUrlToUrlMetadata.get(url).icon;
      remoteViews.setImageViewBitmap(R.id.icon_secondBeacon, icon);
      remoteViews.setTextViewText(R.id.title_secondBeacon, title);
      remoteViews.setTextViewText(R.id.url_secondBeacon, url);
      remoteViews.setTextViewText(R.id.description_secondBeacon, description);
      // Recolor notifications to have light text for non-Lollipop devices
      if (!(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)) {
        remoteViews.setTextColor(R.id.title_secondBeacon, NON_LOLLIPOP_NOTIFICATION_TITLE_COLOR);
        remoteViews.setTextColor(R.id.url_secondBeacon, NON_LOLLIPOP_NOTIFICATION_URL_COLOR);
        remoteViews.setTextColor(R.id.description_secondBeacon, NON_LOLLIPOP_NOTIFICATION_SNIPPET_COLOR);
      }

      // Create an intent that will open the browser to the beacon's url
      // if the user taps the notification
      if (!URLUtil.isNetworkUrl(url)) {
        url = "http://" + url;
      }
      // Route through the proxy server go link
      url = MetadataResolver.createUrlProxyGoLink(url);

      Intent intent_secondBeacon = new Intent(Intent.ACTION_VIEW);
      intent_secondBeacon.setData(Uri.parse(url));
      int requestID = (int) System.currentTimeMillis();
      PendingIntent pendingIntent = PendingIntent.getActivity(this, requestID, intent_secondBeacon, 0);
      remoteViews.setOnClickPendingIntent(R.id.second_beacon_main_layout, pendingIntent);
      remoteViews.setViewVisibility(R.id.secondBeaconLayout, View.VISIBLE);
    } else {
      remoteViews.setViewVisibility(R.id.secondBeaconLayout, View.GONE);
    }
  }

  /**
   * This is the class that listens for screen on/off events
   */
  private class ScreenBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      boolean isScreenOn = Intent.ACTION_SCREEN_ON.equals(intent.getAction());
      initializeLists();
      mNotificationManager.cancelAll();
      if (isScreenOn) {
        startSearchingForUriBeacons();
        mMdnsUrlDiscoverer.startScanning();
      } else {
        stopSearchingForUriBeacons();
        mMdnsUrlDiscoverer.stopScanning();
      }
    }
  }
}

