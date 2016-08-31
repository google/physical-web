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

import org.physical_web.collection.PhysicalWebCollection;
import org.physical_web.collection.PhysicalWebCollectionException;
import org.physical_web.collection.PwPair;
import org.physical_web.collection.PwsResult;
import org.physical_web.collection.PwsResultCallback;
import org.physical_web.collection.PwsResultIconCallback;
import org.physical_web.collection.UrlDevice;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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

public class UrlDeviceDiscoveryService extends Service
                                       implements UrlDeviceDiscoverer.UrlDeviceDiscoveryCallback {

  private static final String TAG = UrlDeviceDiscoveryService.class.getSimpleName();
  private static final String NOTIFICATION_GROUP_KEY = "URI_BEACON_NOTIFICATIONS";
  private static final String PREFS_VERSION_KEY = "prefs_version";
  private static final String SCAN_START_TIME_KEY = "scan_start_time";
  private static final String PW_COLLECTION_KEY = "pw_collection";
  private static final int PREFS_VERSION = 2;
  private static final int NEAREST_BEACON_NOTIFICATION_ID = 23;
  private static final int SECOND_NEAREST_BEACON_NOTIFICATION_ID = 24;
  private static final int SUMMARY_NOTIFICATION_ID = 25;
  private static final int NON_LOLLIPOP_NOTIFICATION_TITLE_COLOR = Color.parseColor("#ffffff");
  private static final int NON_LOLLIPOP_NOTIFICATION_URL_COLOR = Color.parseColor("#999999");
  private static final int NON_LOLLIPOP_NOTIFICATION_SNIPPET_COLOR = Color.parseColor("#999999");
  private static final int NOTIFICATION_VISIBILITY = NotificationCompat.VISIBILITY_PUBLIC;
  private static final long FIRST_SCAN_TIME_MILLIS = TimeUnit.SECONDS.toMillis(2);
  private static final long SECOND_SCAN_TIME_MILLIS = TimeUnit.SECONDS.toMillis(10);
  private static final long SCAN_STALE_TIME_MILLIS = TimeUnit.MINUTES.toMillis(2);
  private static final long LOCAL_SCAN_STALE_TIME_MILLIS = TimeUnit.SECONDS.toMillis(30);
  private boolean mCanUpdateNotifications = false;
  private boolean mSecondScanComplete = false;
  private boolean mIsBound = false;
  private long mScanStartTime;
  private Handler mHandler;
  private NotificationManagerCompat mNotificationManager;
  private List<UrlDeviceDiscoverer> mUrlDeviceDiscoverers;
  private List<UrlDeviceDiscoveryListener> mUrlDeviceDiscoveryListeners;
  private PhysicalWebCollection mPwCollection;

  // Notification of urls happens as follows:
  // 0. Begin scan
  // 1. Delete notification, show top two urls (mFirstScanTimeout)
  // 2. Show each new url as it comes in, if it's in the top two
  // 3. Stop scanning if no clients are subscribed (mSecondScanTimeout)

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
      mSecondScanComplete = true;
      if (!mIsBound) {
        stopSelf();
      }
    }
  };

  /**
   * Binder class for getting connections to the service.
   */
  public class LocalBinder extends Binder {
    public UrlDeviceDiscoveryService getServiceInstance() {
      return UrlDeviceDiscoveryService.this;
    }
  }
  private IBinder mBinder = new LocalBinder();

  /**
   * Callback for subscribers to this service.
   */
  public interface UrlDeviceDiscoveryListener {
    public void onUrlDeviceDiscoveryUpdate();
  }

  public UrlDeviceDiscoveryService() {
  }

  private void initialize() {
    mNotificationManager = NotificationManagerCompat.from(this);
    mUrlDeviceDiscoverers = new ArrayList<>();

    if (Utils.isMdnsEnabled(this)) {
      Log.d(TAG, "mdns started");
      mUrlDeviceDiscoverers.add(new MdnsUrlDeviceDiscoverer(this));
    }
    if (Utils.isWifiDirectEnabled(this)) {
      Log.d(TAG, "wifi direct started");
      mUrlDeviceDiscoverers.add(new WifiUrlDeviceDiscoverer(this));
    }
    mUrlDeviceDiscoverers.add(new SsdpUrlDeviceDiscoverer(this));
    mUrlDeviceDiscoverers.add(new BleUrlDeviceDiscoverer(this));
    for (UrlDeviceDiscoverer urlDeviceDiscoverer : mUrlDeviceDiscoverers) {
      urlDeviceDiscoverer.setCallback(this);
    }
    mUrlDeviceDiscoveryListeners = new ArrayList<>();
    mHandler = new Handler();
    mPwCollection = new PhysicalWebCollection();
    if (!Utils.setPwsEndpoint(this, mPwCollection)) {
      Utils.warnUserOnMissingApiKey(this);
    }
    mCanUpdateNotifications = false;
  }

  private void restoreCache() {
    // Make sure we are trying to load the right version of the cache
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    int prefsVersion = prefs.getInt(PREFS_VERSION_KEY, 0);
    long now = new Date().getTime();
    if (prefsVersion != PREFS_VERSION) {
      mScanStartTime = now;
      return;
    }

    // Don't load the cache if it's stale
    mScanStartTime = prefs.getLong(SCAN_START_TIME_KEY, 0);
    long scanDelta = now - mScanStartTime;
    if (scanDelta >= SCAN_STALE_TIME_MILLIS) {
      mScanStartTime = now;
      return;
    }

    // Restore the cached metadata
    try {
      JSONObject serializedCollection = new JSONObject(prefs.getString(PW_COLLECTION_KEY, null));
      mPwCollection = PhysicalWebCollection.jsonDeserialize(serializedCollection);
      Utils.setPwsEndpoint(this, mPwCollection);
    } catch (JSONException e) {
      Log.e(TAG, "Could not restore Physical Web collection cache", e);
    } catch (PhysicalWebCollectionException e) {
      Log.e(TAG, "Could not restore Physical Web collection cache", e);
    }
    // replace TxPower and RSSI data after restoring cache
    for (UrlDevice urlDevice : mPwCollection.getUrlDevices()) {
      if (Utils.isBleUrlDevice(urlDevice)) {
        Utils.updateRegion(urlDevice);
      }
    }
    // Unresolvable devices are typically not
    // relevant outside of scan range. Hence,
    // we specially clean them from the cache.
    if (scanDelta >= LOCAL_SCAN_STALE_TIME_MILLIS) {
      for (UrlDevice urlDevice : mPwCollection.getUrlDevices()) {
        if (!Utils.isResolvableDevice(urlDevice)) {
          mPwCollection.removeUrlDevice(urlDevice);
        }
      }
    }
  }

  @Override
  public void onCreate() {
    super.onCreate();
    initialize();
    restoreCache();
    cancelNotifications();
    mHandler.postDelayed(mFirstScanTimeout, FIRST_SCAN_TIME_MILLIS);
    mHandler.postDelayed(mSecondScanTimeout, SECOND_SCAN_TIME_MILLIS);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    startScan();
    return START_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    mIsBound = true;
    return mBinder;
  }

  @Override
  public boolean onUnbind(Intent intent) {
    mIsBound = false;
    if (mSecondScanComplete) {
      stopSelf();
    }
    // true ensures onRebind is called on succcessive binds
    return true;
  }

  @Override
  public void onRebind(Intent intent) {
    mIsBound = true;
  }
  private void cancelNotifications() {
    mNotificationManager.cancel(NEAREST_BEACON_NOTIFICATION_ID);
    mNotificationManager.cancel(SECOND_NEAREST_BEACON_NOTIFICATION_ID);
    mNotificationManager.cancel(SUMMARY_NOTIFICATION_ID);
  }

  private void saveCache() {
    // Write the PW Collection
    PreferenceManager.getDefaultSharedPreferences(this).edit()
        .putInt(PREFS_VERSION_KEY, PREFS_VERSION)
        .putLong(SCAN_START_TIME_KEY, mScanStartTime)
        .putString(PW_COLLECTION_KEY, mPwCollection.jsonSerialize().toString())
        .apply();
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "onDestroy:  service exiting");

    // Stop the scanners
    mHandler.removeCallbacks(mFirstScanTimeout);
    mHandler.removeCallbacks(mSecondScanTimeout);
    stopScan();
    saveCache();
    super.onDestroy();
  }

  @Override
  public void onUrlDeviceDiscovered(UrlDevice urlDevice) {
    // Add Device and fetch results
    // Don't short circuit because icons
    // and metadata may not be fetched
    mPwCollection.addUrlDevice(urlDevice);
    Log.d(TAG, urlDevice.getUrl());
    if (!Utils.isResolvableDevice(urlDevice)) {
      mPwCollection.addMetadata(new PwsResult.Builder(urlDevice.getUrl(), urlDevice.getUrl())
        .setTitle(Utils.getTitle(urlDevice))
        .setDescription(Utils.getDescription(urlDevice))
        .build());
    }
    mPwCollection.fetchPwsResults(new PwsResultCallback() {
      long mPwsTripTimeMillis = 0;

      @Override
      public void onPwsResult(PwsResult pwsResult) {
        PwsResult replacement = new Utils.PwsResultBuilder(pwsResult)
            .setPwsTripTimeMillis(pwsResult, mPwsTripTimeMillis)
            .build();
        mPwCollection.addMetadata(replacement);
        triggerCallback();
        updateNotifications();
      }

      @Override
      public void onPwsResultAbsent(String url) {
        triggerCallback();
      }

      @Override
      public void onPwsResultError(Collection<String> urls, int httpResponseCode, Exception e) {
        Log.d(TAG, "PwsResultError: " + httpResponseCode + " ", e);
        triggerCallback();
      }

      @Override
      public void onResponseReceived(long durationMillis) {
        mPwsTripTimeMillis = durationMillis;
      }
    }, new PwsResultIconCallback() {
      @Override
      public void onIcon(byte[] icon) {
        triggerCallback();
      }

      @Override
      public void onError(int httpResponseCode, Exception e) {
        Log.d(TAG, "PwsResultError: " + httpResponseCode + " ", e);
        triggerCallback();
      }
    });
    triggerCallback();
  }

  private void triggerCallback() {
    for (UrlDeviceDiscoveryListener urlDeviceDiscoveryListener : mUrlDeviceDiscoveryListeners) {
      urlDeviceDiscoveryListener.onUrlDeviceDiscoveryUpdate();
    }
  }

  /**
   * Create a new set of notifications or update those existing.
   */
  private void updateNotifications() {
    if (!mCanUpdateNotifications) {
      return;
    }

    List<PwPair> pwPairs = mPwCollection.getGroupedPwPairsSortedByRank(
        new Utils.PwPairRelevanceComparator());
    List<PwPair> notBlockedPwPairs = new ArrayList<>();
    for (PwPair i : pwPairs) {
      if (!Utils.isBlocked(i)) {
        notBlockedPwPairs.add(i);
      }
    }


    // If no beacons have been found
    if (notBlockedPwPairs.size() == 0) {
      // Remove all existing notifications
      cancelNotifications();
    } else if (notBlockedPwPairs.size() == 1) {
      updateNearbyBeaconNotification(true, notBlockedPwPairs.get(0),
          NEAREST_BEACON_NOTIFICATION_ID);
    } else {
      // Create a summary notification for both beacon notifications.
      // Do this first so that we don't first show the individual notifications
      updateSummaryNotification(notBlockedPwPairs);
      // Create or update a notification for second beacon
      updateNearbyBeaconNotification(false, notBlockedPwPairs.get(1),
                                     SECOND_NEAREST_BEACON_NOTIFICATION_ID);
      // Create or update a notification for first beacon. Needs to be added last to show up top
      updateNearbyBeaconNotification(false, notBlockedPwPairs.get(0),
                                     NEAREST_BEACON_NOTIFICATION_ID);

    }
  }

  /**
   * Create or update a notification with the given id for the beacon with the given address.
   */
  private void updateNearbyBeaconNotification(boolean single, PwPair pwPair, int notificationId) {
    PwsResult pwsResult = pwPair.getPwsResult();
    UrlDevice urlDevice = pwPair.getUrlDevice();
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
    builder.setSmallIcon(R.drawable.ic_notification)
        .setLargeIcon(Utils.getBitmapIcon(mPwCollection, pwsResult))
        .setContentTitle(pwsResult.getTitle())
        .setContentText(pwsResult.getDescription())
        .setPriority((Utils.isFavorite(pwPair.getPwsResult().getSiteUrl()))
            ? NotificationCompat.PRIORITY_DEFAULT : NotificationCompat.PRIORITY_MIN);
    if (Utils.isFatBeaconDevice(urlDevice)) {
      Intent intent = new Intent(this, OfflineTransportConnectionActivity.class);
      intent.putExtra(OfflineTransportConnectionActivity.EXTRA_CONNECTION_TYPE,
          OfflineTransportConnectionActivity.EXTRA_FAT_BEACON_CONNECTION);
      intent.putExtra(OfflineTransportConnectionActivity.EXTRA_DEVICE_ADDRESS,
          pwsResult.getSiteUrl());
      intent.putExtra(OfflineTransportConnectionActivity.EXTRA_PAGE_TITLE, pwsResult.getTitle());
      int requestID = (int) System.currentTimeMillis();
      builder.setContentIntent(PendingIntent.getActivity(this, requestID, intent, 0));
    } else if (Utils.isWifiDirectDevice(urlDevice)) {
      Intent intent = new Intent(this, OfflineTransportConnectionActivity.class);
      intent.putExtra(OfflineTransportConnectionActivity.EXTRA_CONNECTION_TYPE,
          OfflineTransportConnectionActivity.EXTRA_WIFI_DIRECT_CONNECTION);
      intent.putExtra(OfflineTransportConnectionActivity.EXTRA_DEVICE_ADDRESS,
          Utils.getWifiAddress(urlDevice));
      intent.putExtra(OfflineTransportConnectionActivity.EXTRA_PAGE_TITLE, pwsResult.getTitle());
      intent.putExtra(OfflineTransportConnectionActivity.EXTRA_DEVICE_PORT,
          Utils.getWifiPort(urlDevice));
      int requestID = (int) System.currentTimeMillis();
      builder.setContentIntent(PendingIntent.getActivity(this, requestID, intent, 0));
    } else {
      builder.setContentIntent(Utils.createNavigateToUrlPendingIntent(pwsResult, this));
    }
    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        && Utils.isPublic(pwPair.getUrlDevice())) {
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
  private void updateSummaryNotification(List<PwPair> pwPairs) {
    int numNearbyBeacons = pwPairs.size();
    String contentTitle = String.valueOf(numNearbyBeacons);
    Resources resources = getResources();
    contentTitle += " " + resources.getQuantityString(R.plurals.numFoundBeacons, numNearbyBeacons,
                                                      numNearbyBeacons);
    String contentText = getString(R.string.summary_notification_pull_down);
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
    builder.setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(contentTitle)
        .setContentText(contentText)
        .setSmallIcon(R.drawable.ic_notification)
        .setGroup(NOTIFICATION_GROUP_KEY)
        .setGroupSummary(true)
        .setPriority(Utils.containsFavorite(pwPairs)
            ? NotificationCompat.PRIORITY_DEFAULT : NotificationCompat.PRIORITY_MIN)
        .setContentIntent(createReturnToAppPendingIntent());
    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      builder.setVisibility(NOTIFICATION_VISIBILITY);
    }
    Notification notification = builder.build();

    // Create the big view for the notification (viewed by pulling down)
    RemoteViews remoteViews = updateSummaryNotificationRemoteViews(pwPairs);
    notification.bigContentView = remoteViews;

    mNotificationManager.notify(SUMMARY_NOTIFICATION_ID, notification);
  }

  /**
   * Create the big view for the summary notification.
   */
  private RemoteViews updateSummaryNotificationRemoteViews(List<PwPair> pwPairs) {
    RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_big_view);

    // Fill in the data for the top two beacon views
    updateSummaryNotificationRemoteViewsFirstBeacon(pwPairs.get(0), remoteViews);
    updateSummaryNotificationRemoteViewsSecondBeacon(pwPairs.get(1), remoteViews);

    // Create a pending intent that will open the physical web app
    // TODO(cco3): Use a clickListener on the VIEW MORE button to do this
    PendingIntent pendingIntent = createReturnToAppPendingIntent();
    remoteViews.setOnClickPendingIntent(R.id.otherBeaconsLayout, pendingIntent);

    return remoteViews;
  }

  private void updateSummaryNotificationRemoteViewsFirstBeacon(PwPair pwPair,
                                                               RemoteViews remoteViews) {
    PwsResult pwsResult = pwPair.getPwsResult();
    remoteViews.setImageViewBitmap(
        R.id.icon_firstBeacon, Utils.getBitmapIcon(mPwCollection, pwsResult));
    remoteViews.setTextViewText(R.id.title_firstBeacon, pwsResult.getTitle());
    remoteViews.setTextViewText(R.id.url_firstBeacon, pwsResult.getSiteUrl());
    remoteViews.setTextViewText(R.id.description_firstBeacon, pwsResult.getDescription());
    // Recolor notifications to have light text for non-Lollipop devices
    if (!(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)) {
      remoteViews.setTextColor(R.id.title_firstBeacon, NON_LOLLIPOP_NOTIFICATION_TITLE_COLOR);
      remoteViews.setTextColor(R.id.url_firstBeacon, NON_LOLLIPOP_NOTIFICATION_URL_COLOR);
      remoteViews.setTextColor(R.id.description_firstBeacon,
                               NON_LOLLIPOP_NOTIFICATION_SNIPPET_COLOR);
    }

    // Create an intent that will open the browser to the beacon's url
    // if the user taps the notification
    remoteViews.setOnClickPendingIntent(R.id.first_beacon_main_layout,
        Utils.createNavigateToUrlPendingIntent(pwsResult, this));
    remoteViews.setViewVisibility(R.id.firstBeaconLayout, View.VISIBLE);
  }

  private void updateSummaryNotificationRemoteViewsSecondBeacon(PwPair pwPair,
                                                                RemoteViews remoteViews) {
    PwsResult pwsResult = pwPair.getPwsResult();
    remoteViews.setImageViewBitmap(
        R.id.icon_secondBeacon, Utils.getBitmapIcon(mPwCollection, pwsResult));
    remoteViews.setTextViewText(R.id.title_secondBeacon, pwsResult.getTitle());
    remoteViews.setTextViewText(R.id.url_secondBeacon, pwsResult.getSiteUrl());
    remoteViews.setTextViewText(R.id.description_secondBeacon, pwsResult.getDescription());
    // Recolor notifications to have light text for non-Lollipop devices
    if (!(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)) {
      remoteViews.setTextColor(R.id.title_secondBeacon, NON_LOLLIPOP_NOTIFICATION_TITLE_COLOR);
      remoteViews.setTextColor(R.id.url_secondBeacon, NON_LOLLIPOP_NOTIFICATION_URL_COLOR);
      remoteViews.setTextColor(R.id.description_secondBeacon,
                               NON_LOLLIPOP_NOTIFICATION_SNIPPET_COLOR);
    }

    // Create an intent that will open the browser to the beacon's url
    // if the user taps the notification
    remoteViews.setOnClickPendingIntent(R.id.second_beacon_main_layout,
        Utils.createNavigateToUrlPendingIntent(pwsResult, this));
    remoteViews.setViewVisibility(R.id.secondBeaconLayout, View.VISIBLE);
  }

  private PendingIntent createReturnToAppPendingIntent() {
    Intent intent = new Intent(this, MainActivity.class);
    int requestID = (int) System.currentTimeMillis();
    PendingIntent pendingIntent = PendingIntent.getActivity(this, requestID, intent, 0);
    return pendingIntent;
  }

  public void addCallback(UrlDeviceDiscoveryListener urlDeviceDiscoveryListener) {
    mUrlDeviceDiscoveryListeners.add(urlDeviceDiscoveryListener);
  }

  public void removeCallback(UrlDeviceDiscoveryListener urlDeviceDiscoveryListener) {
    mUrlDeviceDiscoveryListeners.remove(urlDeviceDiscoveryListener);
  }

  public long getScanStartTime() {
    return mScanStartTime;
  }

  private void startScan() {
    for (UrlDeviceDiscoverer urlDeviceDiscoverer : mUrlDeviceDiscoverers) {
      urlDeviceDiscoverer.startScan();
    }
  }

  private void stopScan() {
    for (UrlDeviceDiscoverer urlDeviceDiscoverer : mUrlDeviceDiscoverers) {
      urlDeviceDiscoverer.stopScan();
    }
  }

  public void restartScan() {
    stopScan();
    mScanStartTime = new Date().getTime();
    startScan();
  }

  public boolean hasResults() {
    return !mPwCollection.getPwPairs().isEmpty();
  }

  public PhysicalWebCollection getPwCollection() {
    return mPwCollection;
  }

  public void clearCache() {
    stopScan();
    mScanStartTime = new Date().getTime();
    Utils.setPwsEndpoint(this, mPwCollection);
    mPwCollection.clear();
    saveCache();
  }

  public void newPwsStartScan() {
    Utils.setPwsEndpoint(this, mPwCollection);
    restartScan();
  }
}
