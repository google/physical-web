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
import android.net.Uri;
import android.webkit.URLUtil;

/**
 * This class holds data about a Physical Web Object and its url.
 *
 * A physical web object is any source of a broadcasted url.
 */
class PwoMetadata implements Comparable<PwoMetadata>{
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
    String deviceAddress;
    int rssi;
    int txPower;

    public BleMetadata(String deviceAddress, int rssi, int txPower) {
      this.deviceAddress = deviceAddress;
      this.rssi = rssi;
      this.txPower = txPower;
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
    public String id;
    public String siteUrl;
    public String displayUrl;
    public String title;
    public String description;
    public String iconUrl;
    public Bitmap icon;
    public float rank;

    public UrlMetadata() {
    }

    public int compareTo(UrlMetadata other) {
      int rankCompare = ((Float) rank).compareTo(other.rank);
      if (rankCompare != 0) {
        return rankCompare;
      }
  
      // If ranks are equal, compare based on title
      return title.compareTo(other.title);
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

  public String getNavigableUrl(Context context) {
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
    urlToNavigateTo = PwsClient.getInstance(context).createUrlProxyGoLink(urlToNavigateTo);
    return urlToNavigateTo;
  }

  public Intent createNavigateToUrlIntent(Context context) {
    String urlToNavigateTo = getNavigableUrl(context);
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse(urlToNavigateTo));
    return intent;
  }

  public PendingIntent createNavigateToUrlPendingIntent(Context context) {
    Intent intent = createNavigateToUrlIntent(context);
    int requestID = (int) System.currentTimeMillis();
    PendingIntent pendingIntent = PendingIntent.getActivity(context, requestID, intent, 0);
    return pendingIntent;
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
}
