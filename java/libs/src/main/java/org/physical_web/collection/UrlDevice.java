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
package org.physical_web.collection;

import org.json.JSONObject;

/**
 * A basic implementation of the UrlDevice interface.
 */
public class UrlDevice implements Comparable<UrlDevice> {
  private static final String ID_KEY = "id";
  private static final String URL_KEY = "url";
  private String mId;
  private String mUrl;

  /**
   * Construct a UrlDevice.
   * @param id The id of the device.
   * @param url The URL broadcasted by the device.
   */
  UrlDevice(String id, String url) {
    mId = id;
    mUrl = url;
  }

  /**
   * Fetches the ID of the device.
   * The ID should be unique across UrlDevices.  This should even be the case when
   * one real world device is broadcasting multiple URLs.
   * @return The ID of the device.
   */
  public String getId() {
    return mId;
  }

  /**
   * Fetches the URL broadcasted by the device.
   * @return The broadcasted URL.
   */
  public String getUrl() {
    return mUrl;
  }

  /**
   * Returns the rank of this device given its associated PwsResult.
   * @param pwsResult is the response received from the Physical Web Service
   *        for the url broadcasted by this UrlDevice.
   * @return .5 (at the moment we don't have anything by which to judge rank)
   */
  public double getRank(PwsResult pwsResult) {
    return .5;
  }

  /**
   * Creates a String that represents the UrlDevice.
   * @return The serialized UrlDevice.
   */
  public JSONObject jsonSerialize() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(ID_KEY, mId);
    jsonObject.put(URL_KEY, mUrl);
    return jsonObject;
  }

  /**
   * Creates a UrlDevice represented by the supplied JSON object.
   * @param jsonObject The serialized UrlDevice.
   * @return The deserialized UrlDevice.
   */
  public static UrlDevice jsonDeserialize(JSONObject jsonObject) {
    String id = jsonObject.getString("id");
    String url = jsonObject.getString("url");
    return new UrlDevice(id, url);
  }

  /**
   * Return a hash code for this UrlDevice.
   * @return hash code
   */
  @Override
  public int hashCode() {
    int hash = 1;
    hash = hash * 31 + mId.hashCode();
    hash = hash * 31 + mUrl.hashCode();
    return hash;
  }

  /**
   * Check if two UrlDevices are equal.
   * @param other the UrlDevice to compare to.
   * @return true if the UrlDevices are equal
   */
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (other instanceof UrlDevice) {
      UrlDevice otherUrlDevice = (UrlDevice) other;
      return mId.equals(otherUrlDevice.getId()) &&
          mUrl.equals(otherUrlDevice.getUrl());
    }
    return false;
  }

  /**
   * Compare two UrlDevices based on device ID, breaking ties by comparing broadcast URL.
   * @param other the UrlDevice to compare to.
   * @return the comparison value.
   */
  @Override
  public int compareTo(UrlDevice other) {
    if (this == other) {
      return 0;
    }

    int compareValue = mId.compareTo(other.getId());
    if (compareValue != 0) {
      return compareValue;
    }

    return mUrl.compareTo(other.getUrl());
  }
}
