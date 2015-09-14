/*
 * Copyright 2015 Google Inc. All rights reserved.
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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.webkit.URLUtil;

import org.uribeacon.scan.util.RangingUtils;
import org.uribeacon.scan.util.RegionResolver;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

/**
 * This class holds data about a Physical Web Object and its url.
 *
 * A physical web object is any source of a broadcasted url.
 */
class PwoMetadata implements Comparable<PwoMetadata> {
  private static final String URL_KEY = "deviceAddress";
  private static final String IS_PUBLIC_KEY = "isPublic";
  private static final String SCAN_MILLIS_KEY = "scanMillis";
  private static final String PWS_TRIP_MILLIS_KEY = "pwsTripMillis";
  private static final String BLE_METADATA_KEY = "bleMetadata";
  private static final String URL_METADATA_KEY = "urlMetadata";
  String url;
  boolean isPublic;
  long scanMillis;
  long pwsTripMillis;
  BleMetadata bleMetadata;
  UrlMetadata urlMetadata;

  /**
   * A container class for ble-specific metadata.
   */
  public static class BleMetadata {
    private static final String DEVICE_ADDRESS_KEY = "deviceAddress";
    private static final String RSSI_KEY = "rssi";
    private static final String TX_POWER_KEY = "txPower";
    String deviceAddress;
    int rssi;
    int txPower;

    private static RegionResolver sRegionResolver;

    public BleMetadata(String deviceAddress, int rssi, int txPower) {
      this.deviceAddress = deviceAddress;
      this.rssi = rssi;
      this.txPower = txPower;
    }

    public JSONObject toJsonObj() throws JSONException {
      JSONObject jsonObj = new JSONObject();
      jsonObj.put(DEVICE_ADDRESS_KEY, deviceAddress);
      jsonObj.put(RSSI_KEY, rssi);
      jsonObj.put(TX_POWER_KEY, txPower);
      return jsonObj;
    }

    public String toJsonStr() throws JSONException {
      return toJsonObj().toString();
    }

    public static BleMetadata fromJsonObj(JSONObject jsonObj) throws JSONException {
      BleMetadata bleMetadata = new BleMetadata(
        jsonObj.getString(DEVICE_ADDRESS_KEY),
        jsonObj.getInt(RSSI_KEY),
        jsonObj.getInt(TX_POWER_KEY));
      return bleMetadata;
    }

    public static BleMetadata fromJsonStr(String jsonStr) throws JSONException {
      return fromJsonObj(new JSONObject(jsonStr));
    }

    private static RegionResolver getRegionResolver() {
      if (sRegionResolver == null) {
        sRegionResolver = new RegionResolver();
      }
      return sRegionResolver;
    }

    public void updateRegionInfo() {
      getRegionResolver().onUpdate(this.deviceAddress, this.rssi, this.txPower);
    }

    public int getSmoothedRssi() {
      return getRegionResolver().getSmoothedRssi(deviceAddress);
    }

    public double getDistance() {
      return getRegionResolver().getDistance(deviceAddress);
    }

    public int getRegion() {
      return getRegionResolver().getRegion(deviceAddress);
    }

    public String getRegionString() {
      return RangingUtils.toString(getRegion());
    }
  }

  /**
   * A container class for a url's fetched metadata.
   * The metadata consists of the title, site url, description,
   * iconUrl and the icon (or favicon).
   * This data is scraped via a server that receives a url
   * and returns a json blob.
   */
  public static class UrlMetadata implements Comparable<UrlMetadata>{
    private static final String ID_KEY = "id";
    private static final String SITE_URL_KEY = "siteUrl";
    private static final String DISPLAY_URL_KEY = "displayUrl";
    private static final String TITLE_KEY = "title";
    private static final String DESCRIPTION_KEY = "description";
    private static final String ICON_URL_KEY = "iconUrl";
    private static final String ICON_KEY = "icon";
    private static final String RANK_KEY = "rank";
    private static final String GROUP_KEY = "group";
    public String id;
    public String siteUrl;
    public String displayUrl;
    public String title;
    public String description;
    public String iconUrl;
    public Bitmap icon;
    public double rank;
    public String group;

    public UrlMetadata() {
    }

    public int compareTo(UrlMetadata other) {
      int rankCompare = ((Double) rank).compareTo(other.rank);
      if (rankCompare != 0) {
        return rankCompare;
      }

      // If ranks are equal, compare based on title
      return title.compareTo(other.title);
    }

    public JSONObject toJsonObj() throws JSONException {
      JSONObject jsonObj = new JSONObject();
      jsonObj.put(ID_KEY, id);
      jsonObj.put(SITE_URL_KEY, siteUrl);
      jsonObj.put(DISPLAY_URL_KEY, displayUrl);
      jsonObj.put(TITLE_KEY, title);
      jsonObj.put(DESCRIPTION_KEY, description);
      jsonObj.put(ICON_URL_KEY, iconUrl);
      if (icon != null) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        icon.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] bitmapData = stream.toByteArray();
        jsonObj.put(ICON_KEY, Base64.encodeToString(bitmapData, Base64.DEFAULT));
      }
      jsonObj.put(RANK_KEY, rank);
      jsonObj.put(GROUP_KEY, group);
      return jsonObj;
    }

    public String toJsonStr() throws JSONException {
      return toJsonObj().toString();
    }

    public static UrlMetadata fromJsonObj(JSONObject jsonObj) throws JSONException {
      UrlMetadata urlMetadata = new UrlMetadata();
      urlMetadata.id = jsonObj.getString(ID_KEY);
      urlMetadata.siteUrl = jsonObj.getString(SITE_URL_KEY);
      urlMetadata.displayUrl = jsonObj.getString(DISPLAY_URL_KEY);
      urlMetadata.title = jsonObj.getString(TITLE_KEY);
      urlMetadata.description = jsonObj.getString(DESCRIPTION_KEY);
      urlMetadata.iconUrl = jsonObj.getString(ICON_URL_KEY);
      if (jsonObj.has(ICON_KEY)) {
        byte[] bitmapData = Base64.decode(jsonObj.getString(ICON_KEY), Base64.DEFAULT);
        urlMetadata.icon = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
      }
      urlMetadata.rank = jsonObj.getDouble(RANK_KEY);
      urlMetadata.group = jsonObj.getString(GROUP_KEY);
      return urlMetadata;
    }

    public static UrlMetadata fromJsonStr(String jsonStr) throws JSONException {
      return fromJsonObj(new JSONObject(jsonStr));
    }
  }

  public PwoMetadata(String url, long scanMillis) {
    this.url = url;
    this.scanMillis = scanMillis;
    // Default isPublic to true
    isPublic = true;
  }

  public void setUrlMetadata(UrlMetadata urlMetadata, long pwsTripMillis) {
    this.urlMetadata = urlMetadata;
    this.pwsTripMillis = pwsTripMillis;
  }

  public void setBleMetadata(String deviceAddress, int rssi, int txPower) {
    this.bleMetadata = new BleMetadata(deviceAddress, rssi, txPower);
  }

  public boolean hasBleMetadata() {
    return bleMetadata != null;
  }

  public boolean hasUrlMetadata() {
    return urlMetadata != null;
  }

  public String getNavigableUrl() {
    String urlToNavigateTo = url;
    if (hasUrlMetadata()) {
      String siteUrl = urlMetadata.siteUrl;
      if (siteUrl != null) {
        urlToNavigateTo = siteUrl;
      }
    }
    if (!URLUtil.isNetworkUrl(urlToNavigateTo)) {
      urlToNavigateTo = "http://" + urlToNavigateTo;
    }
    return urlToNavigateTo;
  }

  public Intent createNavigateToUrlIntent() {
    String urlToNavigateTo = getNavigableUrl();
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse(urlToNavigateTo));
    return intent;
  }

  public PendingIntent createNavigateToUrlPendingIntent(Context context) {
    Intent intent = createNavigateToUrlIntent();
    int requestID = (int) System.currentTimeMillis();
    return PendingIntent.getActivity(context, requestID, intent, 0);
  }

  public int compareTo(PwoMetadata other) {
    // Give preference to the PWO that has url metatada
    if (!hasUrlMetadata()) {
      return 1;
    }
    if (!other.hasUrlMetadata()) {
      return -1;
    }
    return urlMetadata.compareTo(other.urlMetadata);
  }

  public JSONObject toJsonObj() throws JSONException {
    JSONObject jsonObj = new JSONObject();
    jsonObj.put(URL_KEY, url);
    jsonObj.put(SCAN_MILLIS_KEY, scanMillis);
    jsonObj.put(PWS_TRIP_MILLIS_KEY, pwsTripMillis);
    if (hasBleMetadata()) {
      jsonObj.put(BLE_METADATA_KEY, bleMetadata.toJsonObj());
    }
    if (hasUrlMetadata()) {
      jsonObj.put(URL_METADATA_KEY, urlMetadata.toJsonObj());
    }
    return jsonObj;
  }

  public String toJsonStr() throws JSONException {
    return toJsonObj().toString();
  }

  public static PwoMetadata fromJsonObj(JSONObject jsonObj) throws JSONException {
    PwoMetadata pwoMetadata = new PwoMetadata(jsonObj.getString(URL_KEY),
                                              jsonObj.getLong(SCAN_MILLIS_KEY));
    pwoMetadata.pwsTripMillis = jsonObj.getLong(PWS_TRIP_MILLIS_KEY);
    if (jsonObj.has(BLE_METADATA_KEY)) {
      pwoMetadata.bleMetadata = BleMetadata.fromJsonObj(jsonObj.getJSONObject(BLE_METADATA_KEY));
    }
    if (jsonObj.has(URL_METADATA_KEY)) {
      pwoMetadata.urlMetadata = UrlMetadata.fromJsonObj(jsonObj.getJSONObject(URL_METADATA_KEY));
    }
    return pwoMetadata;
  }

  public static PwoMetadata fromJsonStr(String jsonStr) throws JSONException {
    return fromJsonObj(new JSONObject(jsonStr));
  }
}
