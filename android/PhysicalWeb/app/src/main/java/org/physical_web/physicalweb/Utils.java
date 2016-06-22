/*
 * Copyright 2016 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 j*
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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.uribeacon.scan.util.RangingUtils;
import org.uribeacon.scan.util.RegionResolver;

import org.json.JSONException;

import java.net.URI;
import java.net.URISyntaxException;

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
  private static final String SCANTIME_KEY = "scantime";
  private static final String PUBLIC_KEY = "public";
  private static final String RSSI_KEY = "rssi";
  private static final String TXPOWER_KEY = "tx";
  private static final String PWSTRIPTIME_KEY = "pwstriptime";
  private static final RegionResolver REGION_RESOLVER = new RegionResolver();
  private static final String SEPARATOR = "\0";

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
    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    return sharedPref.getString(context.getString(R.string.pws_endpoint_setting_key),
                                defaultEndpoint);
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
    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    return sharedPref.getString(context.getString(R.string.custom_pws_url_key), "");
  }

  private static int readCustomPwsEndpointVersion(Context context) {
    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    return Integer.parseInt(sharedPref.getString(
      context.getString(R.string.custom_pws_version_key),
      context.getString(R.string.custom_pws_version_default)));
  }

  private static String readCustomPwsEndpointApiKey(Context context) {
    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    return sharedPref.getString(context.getString(R.string.custom_pws_api_key_key), "");
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
   * Saves the endpoint to SharedPreferences.
   * @param context The context for the SharedPreferences.
   * @param endpoint The endpoint formatted for SharedPreferences.
   */
  public static void setPwsEndpointPreference(Context context, String endpoint) {
    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = sharedPref.edit();
    editor.putString(context.getString(R.string.pws_endpoint_setting_key),
                     endpoint).commit();
  }

  /**
   * Saves the default settings to the SharedPreferences.
   * @param context The context for the SharedPreferences.
   */
  public static void setSharedPreferencesDefaultValues(Context context) {
    PreferenceManager.setDefaultValues(context, R.xml.settings, false);
    setPwsEndpointPreference(context, getCurrentPwsEndpointString(context));
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
   * Checks if device is Bluetooth Low Energy.
   * @param urlDevice The device that is getting checked.
   * @return If the device is BLE or not.
   */
  public static boolean isBleUrlDevice(UrlDevice urlDevice) {
    try {
      urlDevice.getExtraInt(RSSI_KEY);
    } catch (JSONException e) {
      return false;
    }
    return true;
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
   * Gets the top ranked result for a given groupId.
   * @param pwCollection The collection of results.
   * @param groupId The groupId for the results.
   * @return The pair with the highest ranking for the groupId.
   */
  public static PwPair getTopRankedPwPairByGroupId(
      PhysicalWebCollection pwCollection, String groupId) {
    // This does the same thing as the PhysicalWebCollection method, only it uses our custom
    // getGroupId method.
    for (PwPair pwPair : pwCollection.getGroupedPwPairsSortedByRank()) {
      if (getGroupId(pwPair.getPwsResult()).equals(groupId)) {
        return pwPair;
      }
    }
    return null;
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
}
