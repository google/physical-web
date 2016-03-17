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
import org.physical_web.collection.PwsResult;
import org.physical_web.collection.UrlDevice;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

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
  private static final String SCANTIME_KEY = "scantime";
  private static final String PUBLIC_KEY = "public";
  private static final String RSSI_KEY = "rssi";
  private static final String TXPOWER_KEY = "tx";
  private static final String PWSTRIPTIME_KEY = "pwstriptime";
  private static final RegionResolver REGION_RESOLVER = new RegionResolver();

  private static void throwEncodeException(JSONException e) {
    throw new RuntimeException("Could not encode JSON", e);
  }

  public static Intent createNavigateToUrlIntent(PwsResult pwsResult) {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse(pwsResult.getSiteUrl()));
    return intent;
  }

  public static PendingIntent createNavigateToUrlPendingIntent(
      PwsResult pwsResult, Context context) {
    Intent intent = createNavigateToUrlIntent(pwsResult);
    int requestID = (int) System.currentTimeMillis();
    return PendingIntent.getActivity(context, requestID, intent, 0);
  }

  public static Bitmap getBitmapIcon(PhysicalWebCollection pwCollection, PwsResult pwsResult) {
    byte[] iconBytes = pwCollection.getIcon(pwsResult.getIconUrl());
    if (iconBytes == null) {
      return null;
    }
    return BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
  }

  public static void setScanTimeMillis(UrlDevice urlDevice, long timeMillis) {
    try {
      urlDevice.getExtraData().put(SCANTIME_KEY, timeMillis);
    } catch (JSONException e) {
      throwEncodeException(e);
    }
  }

  public static long getScanTimeMillis(UrlDevice urlDevice) {
    try {
      return urlDevice.getExtraData().getLong(SCANTIME_KEY);
    } catch (JSONException e) {
      throw new RuntimeException("Scan time not recorded in UrlDevice");
    }
  }

  public static void setPrivate(UrlDevice urlDevice) {
    try {
      urlDevice.getExtraData().put(PUBLIC_KEY, false);
    } catch (JSONException e) {
      throwEncodeException(e);
    }
  }

  public static void setPublic(UrlDevice urlDevice) {
    urlDevice.getExtraData().remove(PUBLIC_KEY);
  }

  public static boolean isPublic(UrlDevice urlDevice) {
    return urlDevice.getExtraData().optBoolean(PUBLIC_KEY, true);
  }

  public static boolean isBleUrlDevice(UrlDevice urlDevice) {
    return urlDevice.getExtraData().has(RSSI_KEY);
  }

  public static void setRssi(UrlDevice urlDevice, int rssi) {
    try {
      urlDevice.getExtraData().put(RSSI_KEY, rssi);
    } catch (JSONException e) {
      throwEncodeException(e);
    }
  }

  public static int getRssi(UrlDevice urlDevice) {
    try {
      return urlDevice.getExtraData().getInt(RSSI_KEY);
    } catch (JSONException e) {
      throw new RuntimeException("Tried to get RSSI from non-ble device " + urlDevice.getId(), e);
    }
  }

  public static void setTxPower(UrlDevice urlDevice, int txPower) {
    try {
      urlDevice.getExtraData().put(TXPOWER_KEY, txPower);
    } catch (JSONException e) {
      throwEncodeException(e);
    }
  }

  public static int getTxPower(UrlDevice urlDevice) {
    try {
      return urlDevice.getExtraData().getInt(TXPOWER_KEY);
    } catch (JSONException e) {
      throw new RuntimeException(
          "Tried to get TX power from non-ble device " + urlDevice.getId(), e);
    }
  }

  public static void setPwsTripTimeMillis(PwsResult pwsResult, long timeMillis) {
    try {
      pwsResult.getExtraData().put(PWSTRIPTIME_KEY, timeMillis);
    } catch (JSONException e) {
      throwEncodeException(e);
    }
  }

  public static long getPwsTripTimeMillis(PwsResult pwsResult) {
    try {
      return pwsResult.getExtraData().getLong(PWSTRIPTIME_KEY);
    } catch (JSONException e) {
      throw new RuntimeException("PWS trip time not recorded in PwsResult");
    }
  }

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

  public static void updateRegion(UrlDevice urlDevice) {
    REGION_RESOLVER.onUpdate(urlDevice.getId(), getRssi(urlDevice), getTxPower(urlDevice));
  }

  public static double getSmoothedRssi(UrlDevice urlDevice) {
    return REGION_RESOLVER.getSmoothedRssi(urlDevice.getId());
  }

  public static double getDistance(UrlDevice urlDevice) {
    return REGION_RESOLVER.getDistance(urlDevice.getId());
  }

  public static String getRegionString(UrlDevice urlDevice) {
    return RangingUtils.toString(REGION_RESOLVER.getRegion(urlDevice.getId()));
  }
}
