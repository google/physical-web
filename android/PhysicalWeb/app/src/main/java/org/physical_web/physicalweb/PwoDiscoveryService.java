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

import org.physical_web.physicalweb.PwoMetadata.BleMetadata;
import org.physical_web.physicalweb.PwoMetadata.UrlMetadata;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import org.uribeacon.scan.util.RegionResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This is a service that scans for nearby Physical Web Objects.
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

public class PwoDiscoveryService extends Service
                                 implements PwsClient.ResolveScanCallback,
                                            PwoDiscoverer.PwoDiscoveryCallback {

  private static final String TAG = "PwoDiscoveryService";
  private static final String NOTIFICATION_GROUP_KEY = "URI_BEACON_NOTIFICATIONS";
  private static final int NEAREST_BEACON_NOTIFICATION_ID = 23;
  private static final int SECOND_NEAREST_BEACON_NOTIFICATION_ID = 24;
  private static final int SUMMARY_NOTIFICATION_ID = 25;
  private static final int NON_LOLLIPOP_NOTIFICATION_TITLE_COLOR = Color.parseColor("#ffffff");
  private static final int NON_LOLLIPOP_NOTIFICATION_URL_COLOR = Color.parseColor("#999999");
  private static final int NON_LOLLIPOP_NOTIFICATION_SNIPPET_COLOR = Color.parseColor("#999999");
  private static final int NOTIFICATION_PRIORITY = NotificationCompat.PRIORITY_MIN;
  private static final int NOTIFICATION_VISIBILITY = NotificationCompat.VISIBILITY_PUBLIC;
  private static final long FIRST_SCAN_TIME_MILLIS = TimeUnit.SECONDS.toMillis(2);
  private static final long SECOND_SCAN_TIME_MILLIS = TimeUnit.SECONDS.toMillis(10);
  private boolean mCanUpdateNotifications = false;
  private long mScanStartTime;
  private Handler mHandler;
  private RegionResolver mRegionResolver;
  private NotificationManagerCompat mNotificationManager;
  private HashMap<String, PwoMetadata> mUrlToPwoMetadata;
  private List<PwoDiscoverer> mPwoDiscoverers;

  // Notification of urls happens as follows:
  // 0. Begin scan
  // 1. Delete notification, show top two urls (mFirstScanTimeout)
  // 2. Show each new url as it comes in, if it's in the top two
  // 3. Stop scanning (mSecondScanTimeout)

  private Runnable mFirstScanTimeout = new Runnable() {
    @Override
    public void run() {
      mCanUpdateNotifications = true;
      updateNotifications();
    }
  };

  private Runnable mSecondScanTimeout = new Runnable() {
    @Override
    public void run() {
      stopSelf();
    }
  };

  public PwoDiscoveryService() {
  }

  private void initialize() {
    mNotificationManager = NotificationManagerCompat.from(this);
    mPwoDiscoverers = new ArrayList<>();
    mPwoDiscoverers.add(new MdnsPwoDiscoverer(this));
    mPwoDiscoverers.add(new SsdpPwoDiscoverer(this));
    mPwoDiscoverers.add(new BlePwoDiscoverer(this));
    for (PwoDiscoverer pwoDiscoverer : mPwoDiscoverers) {
      pwoDiscoverer.setCallback(this);
    }
    mHandler = new Handler();
  }

  private void initializeLists() {
    mRegionResolver = new RegionResolver();
    mUrlToPwoMetadata = new HashMap<>();
  }

  @Override
  public void onCreate() {
    super.onCreate();
    initialize();
  }

  @Override
  @SuppressWarnings("deprecation")
  public int onStartCommand(Intent intent, int flags, int startId) {
    // Since sometimes the lists have values when onStartCommand gets called
    initializeLists();
    // Start scanning only if the screen is on
    PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
    // NOTE: use powerManager.isInteractive() when minsdk >= 20
    if (powerManager.isScreenOn()) {
      startSearchingForPwos();
    }

    //make sure the service keeps running
    return START_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "onDestroy:  service exiting");
    stopSearchingForPwos();
    super.onDestroy();
  }

  @Override
  public void onUrlMetadataReceived(PwoMetadata pwoMetadata) {
    updateNotifications();
  }

  @Override
  public void onUrlMetadataAbsent(PwoMetadata pwoMetadata) {
  }

  @Override
  public void onUrlMetadataIconReceived(PwoMetadata pwoMetadata) {
    updateNotifications();
  }

  @Override
  public void onPwoDiscovered(PwoMetadata pwoMetadata) {
    if (pwoMetadata.hasBleMetadata()) {
      BleMetadata bleMetadata = pwoMetadata.bleMetadata;
      mRegionResolver.onUpdate(bleMetadata.deviceAddress, bleMetadata.rssi, bleMetadata.txPower);
    }

    if (!mUrlToPwoMetadata.containsKey(pwoMetadata.url)) {
      mUrlToPwoMetadata.put(pwoMetadata.url, pwoMetadata);
      PwsClient.getInstance(this).findUrlMetadata(pwoMetadata, this, TAG);
    }
  }

  private void startSearchingForPwos() {
    mScanStartTime = new Date().getTime();
    mCanUpdateNotifications = false;
    mHandler.postDelayed(mFirstScanTimeout, FIRST_SCAN_TIME_MILLIS);
    mHandler.postDelayed(mSecondScanTimeout, SECOND_SCAN_TIME_MILLIS);
    for (PwoDiscoverer pwoDiscoverer : mPwoDiscoverers) {
      pwoDiscoverer.startScan();
    }
  }

  private void stopSearchingForPwos() {
    mHandler.removeCallbacks(mFirstScanTimeout);
    mHandler.removeCallbacks(mSecondScanTimeout);
    for (PwoDiscoverer pwoDiscoverer : mPwoDiscoverers) {
      pwoDiscoverer.stopScan();
    }
  }

  /**
   * Create a new set of notifications or update those existing.
   */
  private void updateNotifications() {
    if (!mCanUpdateNotifications) {
      return;
    }

    List<PwoMetadata> pwoMetadataList = getSortedPwoMetadataWithUrlMetadata();

    // If no beacons have been found
    if (pwoMetadataList.size() == 0) {
      // Remove all existing notifications
      mNotificationManager.cancelAll();
    } else if (pwoMetadataList.size() == 1) {
      updateNearbyBeaconNotification(true, pwoMetadataList.get(0), NEAREST_BEACON_NOTIFICATION_ID);
    } else {
      // Create a summary notification for both beacon notifications.
      // Do this first so that we don't first show the individual notifications
      updateSummaryNotification(pwoMetadataList);
      // Create or update a notification for second beacon
      updateNearbyBeaconNotification(false, pwoMetadataList.get(1),
                                     SECOND_NEAREST_BEACON_NOTIFICATION_ID);
      // Create or update a notification for first beacon. Needs to be added last to show up top
      updateNearbyBeaconNotification(false, pwoMetadataList.get(0),
                                     NEAREST_BEACON_NOTIFICATION_ID);

    }
  }

  private List<PwoMetadata> getSortedPwoMetadataWithUrlMetadata() {
    List<PwoMetadata> pwoMetadataList = new ArrayList<>(mUrlToPwoMetadata.size());
    for (PwoMetadata pwoMetadata : mUrlToPwoMetadata.values()) {
      if (pwoMetadata.hasUrlMetadata()) {
        pwoMetadataList.add(pwoMetadata);
      }
    }
    Collections.sort(pwoMetadataList);
    return pwoMetadataList;
  }

  /**
   * Create or update a notification with the given id for the beacon with the given address.
   */
  private void updateNearbyBeaconNotification(boolean single, PwoMetadata pwoMetadata,
                                              int notificationId) {
    UrlMetadata urlMetadata = pwoMetadata.urlMetadata;
    // Create an intent that will open the browser to the beacon's url
    // if the user taps on the notification
    PendingIntent pendingIntent = pwoMetadata.createNavigateToUrlPendingIntent(this);

    String title = urlMetadata.title;
    String description = urlMetadata.description;
    Bitmap icon = urlMetadata.icon;
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
    builder.setSmallIcon(R.drawable.ic_notification)
        .setLargeIcon(icon)
        .setContentTitle(title)
        .setContentText(description)
        .setPriority(NOTIFICATION_PRIORITY)
        .setContentIntent(pendingIntent);
    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && pwoMetadata.isPublic) {
      builder.setVisibility(NOTIFICATION_VISIBILITY);
    }
    // For some reason if there is only one notification and you call setGroup
    // the notification doesn't show up on the N7 running kit kat
    if (!single) {
      builder = builder.setGroup(NOTIFICATION_GROUP_KEY);
    }
    Notification notification = builder.build();

    mNotificationManager.notify(notificationId, notification);
  }

  /**
   * Create or update the a single notification that is a collapsed version
   * of the top two beacon notifications.
   */
  private void updateSummaryNotification(List<PwoMetadata> pwoMetadataList) {
    int numNearbyBeacons = pwoMetadataList.size();
    String contentTitle = String.valueOf(numNearbyBeacons);
    Resources resources = getResources();
    contentTitle += " " + resources.getQuantityString(R.plurals.numFoundBeacons, numNearbyBeacons,
                                                      numNearbyBeacons);
    String contentText = getString(R.string.summary_notification_pull_down);
    PendingIntent pendingIntent = createReturnToAppPendingIntent();
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
    builder.setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(contentTitle)
        .setContentText(contentText)
        .setSmallIcon(R.drawable.ic_notification)
        .setGroup(NOTIFICATION_GROUP_KEY)
        .setGroupSummary(true)
        .setPriority(NOTIFICATION_PRIORITY)
        .setContentIntent(pendingIntent);
    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      builder.setVisibility(NOTIFICATION_VISIBILITY);
    }
    Notification notification = builder.build();

    // Create the big view for the notification (viewed by pulling down)
    RemoteViews remoteViews = updateSummaryNotificationRemoteViews(pwoMetadataList);
    notification.bigContentView = remoteViews;

    mNotificationManager.notify(SUMMARY_NOTIFICATION_ID, notification);
  }

  /**
   * Create the big view for the summary notification.
   */
  private RemoteViews updateSummaryNotificationRemoteViews(List<PwoMetadata> pwoMetadataList) {
    RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_big_view);

    // Fill in the data for the top two beacon views
    updateSummaryNotificationRemoteViewsFirstBeacon(pwoMetadataList.get(0), remoteViews);
    updateSummaryNotificationRemoteViewsSecondBeacon(pwoMetadataList.get(1), remoteViews);

    // Create a pending intent that will open the physical web app
    // TODO(cco3): Use a clickListener on the VIEW MORE button to do this
    PendingIntent pendingIntent = createReturnToAppPendingIntent();
    remoteViews.setOnClickPendingIntent(R.id.otherBeaconsLayout, pendingIntent);

    return remoteViews;
  }

  private void updateSummaryNotificationRemoteViewsFirstBeacon(PwoMetadata pwoMetadata,
                                                               RemoteViews remoteViews) {
    UrlMetadata urlMetadata = pwoMetadata.urlMetadata;
    remoteViews.setImageViewBitmap(R.id.icon_firstBeacon, urlMetadata.icon);
    remoteViews.setTextViewText(R.id.title_firstBeacon, urlMetadata.title);
    remoteViews.setTextViewText(R.id.url_firstBeacon, urlMetadata.displayUrl);
    remoteViews.setTextViewText(R.id.description_firstBeacon, urlMetadata.description);
    // Recolor notifications to have light text for non-Lollipop devices
    if (!(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)) {
      remoteViews.setTextColor(R.id.title_firstBeacon, NON_LOLLIPOP_NOTIFICATION_TITLE_COLOR);
      remoteViews.setTextColor(R.id.url_firstBeacon, NON_LOLLIPOP_NOTIFICATION_URL_COLOR);
      remoteViews.setTextColor(R.id.description_firstBeacon,
                               NON_LOLLIPOP_NOTIFICATION_SNIPPET_COLOR);
    }

    // Create an intent that will open the browser to the beacon's url
    // if the user taps the notification
    PendingIntent pendingIntent = pwoMetadata.createNavigateToUrlPendingIntent(this);
    remoteViews.setOnClickPendingIntent(R.id.first_beacon_main_layout, pendingIntent);
    remoteViews.setViewVisibility(R.id.firstBeaconLayout, View.VISIBLE);
  }

  private void updateSummaryNotificationRemoteViewsSecondBeacon(PwoMetadata pwoMetadata,
                                                                RemoteViews remoteViews) {
    UrlMetadata urlMetadata = pwoMetadata.urlMetadata;
    remoteViews.setImageViewBitmap(R.id.icon_secondBeacon, urlMetadata.icon);
    remoteViews.setTextViewText(R.id.title_secondBeacon, urlMetadata.title);
    remoteViews.setTextViewText(R.id.url_secondBeacon, urlMetadata.displayUrl);
    remoteViews.setTextViewText(R.id.description_secondBeacon, urlMetadata.description);
    // Recolor notifications to have light text for non-Lollipop devices
    if (!(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)) {
      remoteViews.setTextColor(R.id.title_secondBeacon, NON_LOLLIPOP_NOTIFICATION_TITLE_COLOR);
      remoteViews.setTextColor(R.id.url_secondBeacon, NON_LOLLIPOP_NOTIFICATION_URL_COLOR);
      remoteViews.setTextColor(R.id.description_secondBeacon,
                               NON_LOLLIPOP_NOTIFICATION_SNIPPET_COLOR);
    }

    // Create an intent that will open the browser to the beacon's url
    // if the user taps the notification
    PendingIntent pendingIntent = pwoMetadata.createNavigateToUrlPendingIntent(this);
    remoteViews.setOnClickPendingIntent(R.id.second_beacon_main_layout, pendingIntent);
    remoteViews.setViewVisibility(R.id.secondBeaconLayout, View.VISIBLE);
  }

  private PendingIntent createReturnToAppPendingIntent() {
    Intent intent = new Intent(this, MainActivity.class);
    int requestID = (int) System.currentTimeMillis();
    PendingIntent pendingIntent = PendingIntent.getActivity(this, requestID, intent, 0);
    return pendingIntent;
  }
}
