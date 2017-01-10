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

import org.physical_web.collection.PhysicalWebCollection;
import org.physical_web.collection.PwPair;
import org.physical_web.collection.PwsClient;
import org.physical_web.collection.PwsResult;
import org.physical_web.collection.UrlDevice;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.view.Menu;
import android.widget.Toast;

import org.uribeacon.scan.util.RangingUtils;
import org.uribeacon.scan.util.RegionResolver;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;



/**
 * This class is for various static utilities, largely for manipulation of data structures provided
 * by the collections library.
 */
class Utils {
  public static final String PROD_ENDPOINT = "https://url-caster.appspot.com";
  public static final int PROD_ENDPOINT_VERSION = 1;
  public static final String DEV_ENDPOINT = "https://url-caster-dev.appspot.com";
  public static final int DEV_ENDPOINT_VERSION = 1;
  public static final String GOOGLE_ENDPOINT = "https://physicalweb.googleapis.com";
  public static final int GOOGLE_ENDPOINT_VERSION = 2;
  public static final String FAVORITES_KEY = "favorites";
  public static final String BLE_DEVICE_TYPE = "ble";
  public static final String FAT_BEACON_DEVICE_TYPE = "fat-beacon";
  public static final String MDNS_LOCAL_DEVICE_TYPE = "mdns-local";
  public static final String MDNS_PUBLIC_DEVICE_TYPE = "mdns-public";
  public static final String WIFI_DIRECT_DEVICE_TYPE = "wifidirect";
  public static final String SSDP_DEVICE_TYPE = "ssdp";
  private static final String USER_OPTED_IN_KEY = "userOptedIn";
  private static final String MAIN_PREFS_KEY = "physical_web_preferences";
  private static final String DISCOVERY_SERVICE_PREFS_KEY =
      "org.physical_web.physicalweb.DISCOVERY_SERVICE_PREFS";
  private static final String SCANTIME_KEY = "scantime";
  private static final String TYPE_KEY = "type";
  private static final String PUBLIC_KEY = "public";
  private static final String TITLE_KEY = "title";
  private static final String DESCRIPTION_KEY = "description";
  private static final String RSSI_KEY = "rssi";
  private static final String TXPOWER_KEY = "tx";
  private static final String PWSTRIPTIME_KEY = "pwstriptime";
  private static final String WIFIDIRECT_KEY = "wifidirect";
  private static final String WIFIDIRECT_PORT_KEY = "wifiport";
  private static final RegionResolver REGION_RESOLVER = new RegionResolver();
  private static final String SEPARATOR = "\0";
  private static Set<String> mFavoriteUrls = new HashSet<>();
  private static Set<String> mBlockedUrls = new HashSet<>();
  private static final int GZIP_SIGNATURE_LENGTH = 2;

  // Compares PwPairs by first considering if it has been favorited
  // and then considering distance
  public static class PwPairRelevanceComparator implements Comparator<PwPair> {
    private Set<String> mFavorites = mFavoriteUrls;
    public Map<String, Double> mCachedDistances;

    PwPairRelevanceComparator() {
      mCachedDistances = new HashMap<>();
    }

    public double getDistance(UrlDevice urlDevice) {
      if (mCachedDistances.containsKey(urlDevice.getId())) {
        return mCachedDistances.get(urlDevice.getId());
      }
      double distance = Utils.getDistance(urlDevice);
      mCachedDistances.put(urlDevice.getId(), distance);
      return distance;
    }

    @Override
    public int compare(PwPair lhs, PwPair rhs) {
      String lSite = lhs.getPwsResult().getSiteUrl();
      String rSite = rhs.getPwsResult().getSiteUrl();
      if (mFavorites.contains(lSite) == mFavorites.contains(rSite)) {
        return Double.compare(getDistance(lhs.getUrlDevice()),
            getDistance(rhs.getUrlDevice()));
      } else {
        if (mFavorites.contains(lSite)) {
          return -1;
        }
        return 1;
      }
    }
  }

  /**
   * Surface a notification to the user that the Physical Web is broadcasting. The notification
   * specifies the transport or URL that is being broadcast and cannot be swiped away.
   * @param context
   * @param stopServiceReceiver
   * @param broadcastNotificationId
   * @param title
   * @param text
   * @param filter
   */
  public static void createBroadcastNotification(Context context,
      BroadcastReceiver stopServiceReceiver, int broadcastNotificationId, CharSequence title,
      CharSequence text, String filter) {
    Intent resultIntent = new Intent();
    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
    stackBuilder.addParentStack(MainActivity.class);
    stackBuilder.addNextIntent(resultIntent);
    PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
        PendingIntent.FLAG_UPDATE_CURRENT);
    context.registerReceiver(stopServiceReceiver, new IntentFilter(filter));
    PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, new Intent(filter),
        PendingIntent.FLAG_UPDATE_CURRENT);
    NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
        .setSmallIcon(R.drawable.ic_leak_add_white_24dp)
        .setContentTitle(title)
        .setContentText(text)
        .setOngoing(true)
        .addAction(android.R.drawable.ic_menu_close_clear_cancel,
            context.getString(R.string.stop), pIntent);
    builder.setContentIntent(resultPendingIntent);

    NotificationManager notificationManager = (NotificationManager) context.getSystemService(
        Context.NOTIFICATION_SERVICE);
    notificationManager.notify(broadcastNotificationId, builder.build());
  }

  /**
   * Reads the data from the inputStream.
   * @param inputStream The input to read from.
   * @return The entire input stream as a byte array
   * @throws IOException
   */
  public static byte[] getBytes(InputStream inputStream) throws IOException {
    ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
    int bufferSize = 1024;
    byte[] buffer = new byte[bufferSize];

    int len;
    while ((len = inputStream.read(buffer)) != -1) {
      byteBuffer.write(buffer, 0, len);
    }
    return byteBuffer.toByteArray();
  }

  /**
   * Get set of Blocked hosts.
   */
  public static Set<String> getBlockedHosts() {
    return mBlockedUrls;
  }

  /**
   * Hides all items in the given menu.
   * @param menu The menu to hide items for.
   */
  public static void hideAllMenuItems(Menu menu) {
    menu.findItem(R.id.action_about).setVisible(false);
    menu.findItem(R.id.action_settings).setVisible(false);
    menu.findItem(R.id.block_settings).setVisible(false);
    menu.findItem(R.id.action_demos).setVisible(false);
  }

  /**
   * Check if URL has been favorited.
   * @param siteUrl
   */
  public static boolean isFavorite(String siteUrl) {
    return mFavoriteUrls.contains(siteUrl);
  }

  /**
   * Toggles favorite status.
   * @param siteUrl
   */
  public static void toggleFavorite(String siteUrl) {
    if (isFavorite(siteUrl)) {
      mFavoriteUrls.remove(siteUrl);
      return;
    }
    mFavoriteUrls.add(siteUrl);
  }

  /**
   * Save favorites to shared preferences.
   * @param context To get shared preferences.
   */
  public static void saveFavorites(Context context) {
    // Write the PW Collection
    PreferenceManager.getDefaultSharedPreferences(context).edit()
      .putStringSet(FAVORITES_KEY, mFavoriteUrls)
      .apply();
  }

  /**
   * Get favorites from shared preferences.
   * @param context To get shared preferences.
   */
  public static void restoreFavorites(Context context) {
    mFavoriteUrls = new HashSet<>(PreferenceManager.getDefaultSharedPreferences(
        context).getStringSet(FAVORITES_KEY, new HashSet<String>()));
  }

  /**
   * Check if any PwPair in the list is favorited.
   * @param pairs List of pwPairs
   */
  public static boolean containsFavorite(List<PwPair> pairs) {
    for (PwPair pair : pairs) {
      if (isFavorite(pair.getPwsResult().getSiteUrl())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check if domain of URL has been blocked.
   * @param siteUrl
   * @return is siteURL blocked
   */
  public static boolean isBlocked(PwPair pwPair) {
    if (Utils.isWifiDirectDevice(pwPair.getUrlDevice())) {
      return mBlockedUrls.contains(Utils.getWifiAddress(pwPair.getUrlDevice()));
    }
    try {
      return mBlockedUrls.contains(new URI(pwPair.getPwsResult().getSiteUrl()).getHost());
    } catch (URISyntaxException e) {
      return false;
    }
  }

  /**
   * Block the host of siteUrl.
   * @param siteUrl
   */
  public static void addBlocked(PwPair pwPair) {
    if (Utils.isWifiDirectDevice(pwPair.getUrlDevice())) {
      mBlockedUrls.add(Utils.getWifiAddress(pwPair.getUrlDevice()));
      return;
    }
    try {
      mBlockedUrls.add(new URI(pwPair.getPwsResult().getSiteUrl()).getHost());
    } catch (URISyntaxException e) {
      return;
    }
  }

  /**
   * Unblock the host.
   * @param host
   */
  public static void removeBlocked(String host) {
    if (mBlockedUrls.contains(host)) {
      mBlockedUrls.remove(host);
    }
  }

  /**
   * Save blocked set to shared preferences.
   * @param context to access shared preferences
   */
  public static void saveBlocked(Context context) {
    // Write the PW Collection
    PreferenceManager.getDefaultSharedPreferences(context).edit()
      .putStringSet("blocked", mBlockedUrls)
      .apply();
  }

  /**
   * Restore blocked set from shared preferences.
   * @param context to access shared preferences
   */
  public static void restoreBlocked(Context context) {
    mBlockedUrls = new HashSet<>(PreferenceManager.getDefaultSharedPreferences(
        context).getStringSet("blocked", new HashSet<String>()));
  }

  public static class WifiDirectInfo {
    public String title;
    public int port;

    public WifiDirectInfo(String title, int port) {
      this.title = title;
      this.port = port;
    }
  }

  /**
   * Checks to see if name matches defined wifi direct structure.
   * @param name WifiDirect device name
   */
  public static WifiDirectInfo parseWifiDirectName(String name) {
    String split[] = name.split("-");
    if (split.length < 3 || !split[0].equals("PW")
        || !split[split.length - 1].matches("\\d+")) {
      return null;
    }
    return new WifiDirectInfo(name.substring(3, name.lastIndexOf('-')),
        Integer.parseInt(split[split.length - 1]));

  }

  private static class PwsEndpoint {
    public String url;
    public int apiVersion;
    public String apiKey;
    public boolean neededApiKey;

    public PwsEndpoint(String url, int apiVersion, String apiKey) {
      this.url = url;
      this.apiVersion = apiVersion;
      this.apiKey = apiKey;
      this.neededApiKey = needApiKey();
      if (this.neededApiKey) {
        this.url = PROD_ENDPOINT;
        this.apiVersion = PROD_ENDPOINT_VERSION;
      }
    }

    private boolean needApiKey() {
      return apiVersion >= 2 && apiKey.isEmpty();
    }

  }



  private static void throwEncodeException(JSONException e) {
    throw new RuntimeException("Could not encode JSON", e);
  }

  private static void deletePreference(Context context, String preferenceName) {
    context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE).edit().clear().apply();
  }

  private static int getGoogleApiKeyResourceId(Context context) {
    return context.getResources().getIdentifier("google_api_key", "string",
                                                context.getPackageName());
  }

  private static PwsEndpoint getCurrentPwsEndpoint(Context context) {
    return new PwsEndpoint(getCurrentPwsEndpointUrl(context),
                           getCurrentPwsEndpointVersion(context),
                           getCurrentPwsEndpointApiKey(context));
  }

  private static String getCurrentPwsEndpointString(Context context) {
    String defaultEndpoint = getDefaultPwsEndpointPreferenceString(context);
    return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(
        R.string.pws_endpoint_setting_key), defaultEndpoint);
  }

  private static String getCurrentPwsEndpointUrl(Context context) {
    String endpoint = getCurrentPwsEndpointString(context);
    return endpoint.split(SEPARATOR)[0];
  }

  private static int getCurrentPwsEndpointVersion(Context context) {
    String endpoint = getCurrentPwsEndpointString(context);
    return Integer.parseInt(endpoint.split(SEPARATOR)[1]);
  }

  private static String getCurrentPwsEndpointApiKey(Context context) {
    String endpoint = getCurrentPwsEndpointString(context);
    if (endpoint.endsWith(SEPARATOR) || endpoint.endsWith("null")) {
      return "";
    }
    return endpoint.split(SEPARATOR)[2];
  }

  private static String readCustomPwsEndpointUrl(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context)
        .getString(context.getString(R.string.custom_pws_url_key), "");
  }

  private static int readCustomPwsEndpointVersion(Context context) {
    return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context)
        .getString(context.getString(R.string.custom_pws_version_key),
                   context.getString(R.string.custom_pws_version_default)));
  }

  private static String readCustomPwsEndpointApiKey(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context)
        .getString(context.getString(R.string.custom_pws_api_key_key), "");
  }

  /**
   * Gets whether the user has optedIn from SharedPreferences.
   * @param context The context for the SharedPreferences.
   * @return True if the user has optedIn otherwise false.
   */
  public static boolean checkIfUserHasOptedIn(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context)
        .getBoolean(USER_OPTED_IN_KEY, false);
  }

  public abstract static class UrlDeviceDiscoveryServiceConnection implements ServiceConnection {
    private Context mContext;

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      // Forward the service to the implementing class
      serviceHandler((UrlDeviceDiscoveryService.LocalBinder) service);
      mContext.unbindService(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
    }

    public void connect(Context context) {
      mContext = context;
      Intent intent = new Intent(mContext, UrlDeviceDiscoveryService.class);
      mContext.startService(intent);
      mContext.bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    public abstract void serviceHandler(UrlDeviceDiscoveryService.LocalBinder localBinder);
  }

  /**
   * Delete the cached results from the UrlDeviceDisoveryService.
   * @param context The context for the service.
   */
  public static void deleteCache(Context context) {
    new UrlDeviceDiscoveryServiceConnection() {
      @Override
      public void serviceHandler(UrlDeviceDiscoveryService.LocalBinder localBinder) {
        localBinder.getServiceInstance().clearCache();
      }
    }.connect(context);
  }

  /**
   * Starts scanning with UrlDeviceDisoveryService.
   * @param context The context for the service.
   */
  public static void startScan(Context context) {
    new UrlDeviceDiscoveryServiceConnection() {
      @Override
      public void serviceHandler(UrlDeviceDiscoveryService.LocalBinder localBinder) {
        localBinder.getServiceInstance().restartScan();
      }
    }.connect(context);
  }

  /**
   * Format the endpoint URL, version, and API key.
   * @param pwsUrl The URL of the Physical Web Service.
   * @param pwsVersion The API version the PWS is running.
   * @param apiKey The API key for the PWS.
   * @return The PWS endpoint formatted for saving to SharedPreferences.
   */
  public static String formatEndpointForSharedPrefernces(String pwsUrl, int pwsVersion,
                                                       String apiKey) {
    pwsUrl = pwsUrl == null ? "" : pwsUrl;
    apiKey = apiKey == null ? "" : apiKey;
    return pwsUrl + SEPARATOR + pwsVersion + SEPARATOR + apiKey;
  }

  /**
   * Get the default PWS Endpoint formatted for SharedPreferences.
   * @param context The context for the SharedPreferences.
   * @return The default PWS endpoint formatted for saving to SharedPreferences.
   */
  public static String getDefaultPwsEndpointPreferenceString(Context context) {
    if (isGoogleApiKeyAvailable(context)) {
      return formatEndpointForSharedPrefernces(GOOGLE_ENDPOINT, GOOGLE_ENDPOINT_VERSION,
                                                        getGoogleApiKey(context));
    }
    return formatEndpointForSharedPrefernces(PROD_ENDPOINT, PROD_ENDPOINT_VERSION, "");
  }

  /**
   * Saves the optIn preference in the default shared preferences file.
   * @param context The context for the SharedPreferences.
   */
  public static void setOptInPreference(Context context) {
    PreferenceManager.getDefaultSharedPreferences(context).edit()
        .putBoolean(USER_OPTED_IN_KEY, true).apply();
  }

  /**
   * Saves the endpoint to SharedPreferences.
   * @param context The context for the SharedPreferences.
   * @param endpoint The endpoint formatted for SharedPreferences.
   */
  public static void setPwsEndpointPreference(Context context, String endpoint) {
    PreferenceManager.getDefaultSharedPreferences(context).edit()
        .putString(context.getString(R.string.pws_endpoint_setting_key), endpoint)
        .apply();
  }

  /**
   * Saves the default settings to the SharedPreferences.
   * @param context The context for the SharedPreferences.
   */
  public static void setSharedPreferencesDefaultValues(Context context) {
    PreferenceManager.setDefaultValues(context, R.xml.settings, false);
    setPwsEndpointPreference(context, getCurrentPwsEndpointString(context));
    if (context.getSharedPreferences(MAIN_PREFS_KEY, Context.MODE_PRIVATE)
          .getBoolean(USER_OPTED_IN_KEY, false)) {
      setOptInPreference(context);
      deletePreference(context, MAIN_PREFS_KEY);
      deletePreference(context, DISCOVERY_SERVICE_PREFS_KEY);
    }
  }

  /**
   * Sets the client's endpoint to the currently selected endpoint.
   * @param context The context for the SharedPreferences.
   * @param pwsClient The client used for requests to the Physical Web Service.
   * @return If the endpoint was successfully set to current endpoint or if it had to be reverted.
   *         to the default endpoint.
   */
  public static boolean setPwsEndpoint(Context context, PwsClient pwsClient) {
    PwsEndpoint endpoint = getCurrentPwsEndpoint(context);
    pwsClient.setEndpoint(endpoint.url, endpoint.apiVersion, endpoint.apiKey);
    return !endpoint.neededApiKey;
  }

  /**
   * Sets the client's endpoint to the currently selected endpoint.
   * @param context The context for the SharedPreferences.
   * @param physicalWebCollection The client used for requests to the Physical Web Service.
   * @return If the endpoint was successfully set to current endpoint or if it had to be reverted.
   *         to the default endpoint.
   */
  public static boolean setPwsEndpoint(Context context,
                                       PhysicalWebCollection physicalWebCollection) {
    PwsEndpoint endpoint = getCurrentPwsEndpoint(context);
    physicalWebCollection.setPwsEndpoint(endpoint.url, endpoint.apiVersion, endpoint.apiKey);
    return !endpoint.neededApiKey;
  }

  /**
   * Sets the client's endpoint to the Google endpoint.
   * @param context The context for the SharedPreferences.
   * @param pwsClient The client used for requests to the Physical Web Service.
   */
  public static void setPwsEndPointToGoogle(Context context, PwsClient pwsClient) {
    pwsClient.setEndpoint(GOOGLE_ENDPOINT, GOOGLE_ENDPOINT_VERSION, getGoogleApiKey(context));
  }

  /**
   * Checks if the currently selected PWS has a valid format, not that it exists.
   * @param context The context for the SharedPreferences.
   * @return If the current PWS configured properly.
   */
  public static boolean isCurrentPwsSelectionValid(Context context) {
    String endpoint = getCurrentPwsEndpointUrl(context);
    int apiVersion = getCurrentPwsEndpointVersion(context);
    String apiKey = getCurrentPwsEndpointApiKey(context);
    return !endpoint.isEmpty() && !(apiVersion >= 2 && apiKey.isEmpty());
  }

  /**
   * Checks if the Google API key resource exists.
   * @param context The context for the resources.
   * @return If the Google API key is available.
   */
  public static boolean isGoogleApiKeyAvailable(Context context) {
    return getGoogleApiKeyResourceId(context) != 0;
  }

  /**
   * Get the Google API key if available.
   * @param context The context for the resources.
   * @return The API key for the Google PWS if available or an empty string.
   */
  public static String getGoogleApiKey(Context context) {
    int resourceId = getGoogleApiKeyResourceId(context);
    return resourceId != 0 ? context.getString(resourceId) : "";
  }

  /**
   * Gets the saved settings for the custom PWS.
   * @param context The context for the SharedPreferences.
   * @return The custom PWS endpoint formatted for SharedPrefences.
   */
  public static String getCustomPwsEndpoint(Context context) {
    return formatEndpointForSharedPrefernces(readCustomPwsEndpointUrl(context),
                                             readCustomPwsEndpointVersion(context),
                                             readCustomPwsEndpointApiKey(context));
  }

  /**
   * Get the saved setting for enabling Fatbeacon.
   * @param context The context for the SharedPreferences.
   * @return The enable Fatbeacon setting.
   */
  public static boolean isFatBeaconEnabled(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context)
        .getBoolean(context.getString(R.string.fatbeacon_key), false);
  }

  /**
   * Get the saved setting for enabling mDNS folder.
   * @param context The context for the SharedPreferences.
   * @return The enable mDNS folder setting.
   */
  public static boolean isMdnsEnabled(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context)
        .getBoolean(context.getString(R.string.mDNS_key), false);
  }

  /**
   * Get the saved setting for enabling Wifi direct.
   * @param context The context for the SharedPreferences.
   * @return The enable wifi direct setting.
   */
  public static boolean isWifiDirectEnabled(Context context) {
   return PreferenceManager.getDefaultSharedPreferences(context)
       .getBoolean(context.getString(R.string.wifi_direct_key), false);
  }

  /**
   * Get the saved setting for debug View.
   * @param context The context for the SharedPreferences.
   * @return The debug view setting.
   */
  public static boolean isDebugViewEnabled(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context)
        .getBoolean(context.getString(R.string.debug_key), false);
  }

  /**
   * Get the saved setting for enabling mDNS folder.
   * @param context The context for the SharedPreferences.
   * @return The enable mDNS folder setting.
   */
  public static int getWifiDirectPort(Context context) {
    String port = PreferenceManager.getDefaultSharedPreferences(context)
        .getString(context.getString(R.string.wifi_port_key), "1234");
    if (port.matches("\\d+")) {
      return Integer.parseInt(port);
    }
    return 1234;
  }

  /**
   * Toast the user to indicate the API key is missing.
   * @param context The context for the resources.
   */
  public static void warnUserOnMissingApiKey(Context context) {
    Toast.makeText(context, R.string.error_api_key_no_longer_available, Toast.LENGTH_SHORT).show();
  }

  /**
   * Create an intent for opening a URL.
   * @param pwsResult The result that has the URL the user clicked on.
   * @return The intent that opens the URL.
   */
  public static Intent createNavigateToUrlIntent(PwsResult pwsResult) {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse(pwsResult.getSiteUrl()));
    return intent;
  }

  /**
   * Setup an intent to open the URL.
   * @param pwsResult The result that has the URL the user clicked on.
   * @param context The context for the activity.
   * @return The intent that opens the URL.
   */
  public static PendingIntent createNavigateToUrlPendingIntent(
      PwsResult pwsResult, Context context) {
    Intent intent = createNavigateToUrlIntent(pwsResult);
    int requestID = (int) System.currentTimeMillis();
    return PendingIntent.getActivity(context, requestID, intent, 0);
  }

  /**
   * Gets the top ranked PwPair with the given groupId.
   * @param pwCollection The collection of PwPairs.
   * @param groupId The group id of the requested PwPair.
   * @return The top ranked PwPair with a given group id if it exist.
   */
  public static PwPair getTopRankedPwPairByGroupId(
      PhysicalWebCollection pwCollection, String groupId) {
    // This does the same thing as the PhysicalWebCollection method, only it uses our custom
    // getGroupId method.
    for (PwPair pwPair : pwCollection.getGroupedPwPairsSortedByRank(
        new PwPairRelevanceComparator())) {
      if (getGroupId(pwPair.getPwsResult()).equals(groupId)) {
        return pwPair;
      }
    }
    return null;
  }

  /**
   * Decode the downloaded icon to a Bitmap.
   * @param pwCollection The collection where the icon is stored.
   * @param pwsResult The result the icon is for.
   * @return The icon as a Bitmap.
   */
  public static Bitmap getBitmapIcon(PhysicalWebCollection pwCollection, PwsResult pwsResult) {
    byte[] iconBytes = pwCollection.getIcon(pwsResult.getIconUrl());
    if (iconBytes == null) {
      return null;
    }
    return BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
  }

  /**
   * Get the scan time for a device if available.
   * @param urlDevice The device to get the scan time for.
   * @return The scan time in millisecond for the device.
   * @throws RuntimeException If the device doesn't have a scan time.
   */
  public static long getScanTimeMillis(UrlDevice urlDevice) {
    try {
      return urlDevice.getExtraLong(SCANTIME_KEY);
    } catch (JSONException e) {
      throw new RuntimeException("Scan time not available in device " + urlDevice.getId(), e);
    }
  }

  /**
   * Checks if device is public.
   * @param urlDevice The device that is getting checked.
   * @return If the device is public or not.
   */
  public static boolean isPublic(UrlDevice urlDevice) {
    return urlDevice.optExtraBoolean(PUBLIC_KEY, true);
  }

  /**
   * Checks if device is PWS resolvable.
   * @param urlDevice The device that is getting checked.
   * @return If the device is resolvable or not.
   */
  public static boolean isResolvableDevice(UrlDevice urlDevice) {
    String type = urlDevice.optExtraString(TYPE_KEY, "");
    return type.equals(BLE_DEVICE_TYPE)
        || type.equals(SSDP_DEVICE_TYPE)
        || type.equals(MDNS_PUBLIC_DEVICE_TYPE);
  }

  /**
   * Checks if device is Bluetooth Low Energy.
   * @param urlDevice The device that is getting checked.
   * @return If the device is BLE or not.
   */
  public static boolean isBleUrlDevice(UrlDevice urlDevice) {
    return urlDevice.optExtraString(TYPE_KEY, "").equals(BLE_DEVICE_TYPE);
  }

  /**
   * Gets if the device is a FatBeacon.
   * @param urlDevice The device that is getting checked.
   * @return Is a FatBeacon device
   */
  public static boolean isFatBeaconDevice(UrlDevice urlDevice) {
    return urlDevice.optExtraString(TYPE_KEY, "").equals(FAT_BEACON_DEVICE_TYPE);
  }

  /**
   * Checks if device is mdns device broadcasting public url.
   * @param urlDevice The device that is getting checked.
   * @return If the device is mdns public or not.
   */
  public static boolean isMDNSPublicDevice(UrlDevice urlDevice) {
    return urlDevice.optExtraString(TYPE_KEY, "").equals(MDNS_PUBLIC_DEVICE_TYPE);
  }

  /**
   * Checks if device is Local.
   * @param urlDevice The device that is getting checked.
   * @return If the device is local or not.
   */
  public static boolean isMDNSLocalDevice(UrlDevice urlDevice) {
    return urlDevice.optExtraString(TYPE_KEY, "").equals(MDNS_LOCAL_DEVICE_TYPE);
  }

  /**
   * Gets if the device is Wifi Direct.
   * @param urlDevice The device that is getting checked.
   * @return Is a WifiDirect device
   */
  public static boolean isWifiDirectDevice(UrlDevice urlDevice) {
    return urlDevice.optExtraString(TYPE_KEY, "").equals(WIFI_DIRECT_DEVICE_TYPE);
  }

  /**
   * Gets if the device is SSDP.
   * @param urlDevice The device that is getting checked.
   * @return Is a SSDP device
   */
  public static boolean isSSDPDevice(UrlDevice urlDevice) {
    return urlDevice.optExtraString(TYPE_KEY, "").equals(SSDP_DEVICE_TYPE);
  }

  /**
   * Gets the device RSSI if it is Bluetooth Low Energy.
   * @param urlDevice The device that is getting checked.
   * @return The RSSI for the device.
   * @throws RuntimeException If the device is not BLE.
   */
  public static int getRssi(UrlDevice urlDevice) {
    try {
      return urlDevice.getExtraInt(RSSI_KEY);
    } catch (JSONException e) {
      throw new RuntimeException("Tried to get RSSI from non-ble device " + urlDevice.getId(), e);
    }
  }

  /**
   * Gets the device TX power if it is Bluetooth Low Energy.
   * @param urlDevice The device that is getting checked.
   * @return The TX power for the device.
   * @throws RuntimeException If the device is not BLE.
   */
  public static int getTxPower(UrlDevice urlDevice) {
    try {
      return urlDevice.getExtraInt(TXPOWER_KEY);
    } catch (JSONException e) {
      throw new RuntimeException(
          "Tried to get TX power from non-ble device " + urlDevice.getId(), e);
    }
  }

  /**
   * Gets the UrlDevice Title.
   * @param urlDevice The device that is getting checked.
   * @return The Title for the device.
   * @throws RuntimeException if no title present.
   */
  public static String getTitle(UrlDevice urlDevice) {
    try {
      return urlDevice.getExtraString(TITLE_KEY);
    } catch (JSONException e) {
      throw new RuntimeException(
          "Tried to get Title when no title set " + urlDevice.getId(), e);
    }
  }

  /**
   * Gets the UrlDevice Description.
   * @param urlDevice The device that is getting checked.
   * @return The Description for the device.
   * @throws RuntimeException if no description present.
   */
  public static String getDescription(UrlDevice urlDevice) {
    try {
      return urlDevice.getExtraString(DESCRIPTION_KEY);
    } catch (JSONException e) {
      throw new RuntimeException(
          "Tried to get Description when no description set " + urlDevice.getId(), e);
    }
  }

  /**
   * Gets the UrlDevice MAC address.
   * @param urlDevice The device that is getting checked.
   * @return The MAC address for the device.
   * @throws RuntimeException if no address present.
   */
  public static String getWifiAddress(UrlDevice urlDevice) {
    try {
      return urlDevice.getExtraString(WIFIDIRECT_KEY);
    } catch (JSONException e) {
      throw new RuntimeException(
          "Tried to get address when no description set " + urlDevice.getId(), e);
    }
  }

  /**
   * Gets the UrlDevice port.
   * @param urlDevice The device that is getting checked.
   * @return The port for the device.
   * @throws RuntimeException if no port present.
   */
  public static int getWifiPort(UrlDevice urlDevice) {
    try {
      return urlDevice.getExtraInt(WIFIDIRECT_PORT_KEY);
    } catch (JSONException e) {
      throw new RuntimeException(
          "Tried to get port when no port set " + urlDevice.getId(), e);
    }
  }

  /**
   * Gets the amount of time in milliseconds to get the result from the PWS if available.
   * @param pwsResult The result that is being queried.
   * @return The trip time for the result.
   * @throws RuntimeException If the trip time is not recorded.
   */
  public static long getPwsTripTimeMillis(PwsResult pwsResult) {
    try {
      return pwsResult.getExtraLong(PWSTRIPTIME_KEY);
    } catch (JSONException e) {
      throw new RuntimeException("PWS trip time not recorded in PwsResult");
    }
  }

  /**
   * Gets the groupId for the result.
   * @param pwsResult The result that is being queried.
   * @return The groupId for the result.
   */
  public static String getGroupId(PwsResult pwsResult) {
    // The PWS does not always give us a group id yet.
    if (pwsResult.getGroupId() == null || pwsResult.getGroupId().equals("")) {
      try {
        return new URI(pwsResult.getSiteUrl()).getHost() + pwsResult.getTitle();
      } catch (URISyntaxException e) {
        return pwsResult.getSiteUrl();
      }
    }
    return pwsResult.getGroupId();
  }

  /**
   * Updates the region resolver with the device.
   * @param urlDevice The device to update region with.
   */
  public static void updateRegion(UrlDevice urlDevice) {
    REGION_RESOLVER.onUpdate(urlDevice.getId(), getRssi(urlDevice), getTxPower(urlDevice));
  }

  /**
   * Gets the smoothed RSSI for device from the region resolver.
   * @param urlDevice The device being queried.
   * @return The smoothed RSSI for the device.
   */
  public static double getSmoothedRssi(UrlDevice urlDevice) {
    return REGION_RESOLVER.getSmoothedRssi(urlDevice.getId());
  }

  /**
   * Gets the distance for device from the region resolver.
   * @param urlDevice The device being queried.
   * @return The distance for the device.
   */
  public static double getDistance(UrlDevice urlDevice) {
    return REGION_RESOLVER.getDistance(urlDevice.getId());
  }

  /**
   * Gets the region string for device from the region resolver.
   * @param urlDevice The device being queried.
   * @return The region string for the device.
   */
  public static String getRegionString(UrlDevice urlDevice) {
    return RangingUtils.toString(REGION_RESOLVER.getRegion(urlDevice.getId()));
  }

  static class UrlDeviceBuilder extends UrlDevice.Builder {

    /**
     * Constructor for the UrlDeviceBuilder.
     * @param id The id of the UrlDevice.
     * @param url The url of the UrlDevice.
     */
    public UrlDeviceBuilder(String id, String url) {
      super(id, url);
    }

    /**
    * Set the device type.
    * @return The builder with type set.
    */
    public UrlDeviceBuilder setDeviceType(String type) {
      addExtra(TYPE_KEY, type);
      return this;
    }

    /**
     * Setter for the ScanTimeMillis.
     * @param timeMillis The scan time of the UrlDevice.
     * @return The builder with ScanTimeMillis set.
     */
    public UrlDeviceBuilder setScanTimeMillis(long timeMillis) {
      addExtra(SCANTIME_KEY, timeMillis);
      return this;
    }

    /**
     * Set the public key to false.
     * @return The builder with public set to false.
     */
    public UrlDeviceBuilder setPrivate() {
      addExtra(PUBLIC_KEY, false);
      return this;
    }

    /**
     * Set the public key to true.
     * @return The builder with public set to true.
     */
    public UrlDeviceBuilder setPublic() {
      addExtra(PUBLIC_KEY, true);
      return this;
    }

    /**
     * Set the title.
     * @param title corresonding to UrlDevice.
     * @return The builder with title
     */
    public UrlDeviceBuilder setTitle(String title) {
      addExtra(TITLE_KEY, title);
      return this;
    }

    /**
     * Set the description.
     * @param description corresonding to UrlDevice.
     * @return The builder with description
     */
    public UrlDeviceBuilder setDescription(String description) {
      addExtra(DESCRIPTION_KEY, description);
      return this;
    }

    /**
     * Set wifi-direct MAC address.
     * @param MAC address corresonding to UrlDevice.
     * @return The builder with address
     */
    public UrlDeviceBuilder setWifiAddress(String address) {
      addExtra(WIFIDIRECT_KEY, address);
      return this;
    }

        /**
     * Set wifi-direct port.
     * @param port corresonding to UrlDevice.
     * @return The builder with port
     */
    public UrlDeviceBuilder setWifiPort(int port) {
      addExtra(WIFIDIRECT_PORT_KEY, port);
      return this;
    }

    /**
     * Setter for the RSSI.
     * @param rssi The RSSI of the UrlDevice.
     * @return The builder with RSSI set.
     */
    public UrlDeviceBuilder setRssi(int rssi) {
      addExtra(RSSI_KEY, rssi);
      return this;
    }

    /**
     * Setter for the TX power.
     * @param txPower The TX power of the UrlDevice.
     * @return The builder with TX power set.
     */
    public UrlDeviceBuilder setTxPower(int txPower) {
      addExtra(TXPOWER_KEY, txPower);
      return this;
    }
  }

  static class PwsResultBuilder extends PwsResult.Builder {
    /**
     * Constructor for the PwsResultBuilder.
     * @param pwsResult The base result of the PwsResultBuilder.
     */
    public PwsResultBuilder(PwsResult pwsResult) {
      super(pwsResult);
    }

    /**
     * Setter for the PWS Trip Time.
     * @param pwsResult The pwsResult.
     * @param timeMillis The PWS Trip Time for the result.
     * @return The builder PWS Trip Time set.
     */
    public PwsResultBuilder setPwsTripTimeMillis(PwsResult pwsResult, long timeMillis) {
      addExtra(PWSTRIPTIME_KEY, timeMillis);
      return this;
    }
  }

  /**
   * Determines if a file is gzipped by examining the signature of
   * the file, which is the first two bytes.
   * @param file to be determined if is gzipped.
   * @return true If the contents of the file are gzipped otherwise false.
   */
  public static boolean isGzippedFile(File file) {
    InputStream input;

    try {
      input = new FileInputStream(file);
    } catch(FileNotFoundException e) {
      return false;
    }

    byte[] signature = new byte[GZIP_SIGNATURE_LENGTH];

    try {
      input.read(signature);
    } catch(IOException e) {
      return false;
    }

    return ((signature[0] == (byte) (GZIPInputStream.GZIP_MAGIC))
            && (signature[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8)));
  }


  /**
   * Out-of-place Gunzips from src to dest.
   * @param src file containing gzipped information.
   * @param dest file to place decompressed information.
   * @return File that has decompressed information.
   */
  public static File gunzip(File src, File dest) {

    byte[] buffer = new byte[1024];

    try{

      GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(src));

      FileOutputStream out = new FileOutputStream(dest);

      int len;
      while ((len = gzis.read(buffer)) > 0) {
        out.write(buffer, 0, len);
      }

      gzis.close();
      out.close();

    } catch(IOException ex){
       ex.printStackTrace();
    }
    return dest;
   }
}
