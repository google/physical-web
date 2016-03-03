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
 * The class defining a Physical Web URL device.
 */
public class UrlDevice {
  private static final String ID_KEY = "id";
  private static final String URL_KEY = "url";
  private static final String EXTRA_KEY = "extra";
  private String mId;
  private String mUrl;
  private JSONObject mExtraData;

  /**
   * Construct a SimpleUrlDevice.
   * @param id The id of the device.
   * @param url The URL broadcasted by the device.
   */
  public UrlDevice(String id, String url) {
    mId = id;
    mUrl = url;
    mExtraData = new JSONObject();
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
   * TODO(cco3): Move ranking outside of this class
   */
  public double getRank(PwsResult pwsResult) {
    return .5;
  }

  /**
   * Get extra data JSONObject.
   * This is where client code should store custom data.
   * @return Extra data.
   */
  public JSONObject getExtraData() {
    return mExtraData;
  }

  /**
   * Create a JSON object that represents this data structure.
   * @return a JSON serialization of this data structure.
   */
  public JSONObject jsonSerialize() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(ID_KEY, mId);
    jsonObject.put(URL_KEY, mUrl);

    if (mExtraData.length() > 0) {
      jsonObject.put(EXTRA_KEY, mExtraData);
    }

    return jsonObject;
  }

  /**
   * Populate a UrlDevice with data from a given JSON object.
   * @param jsonObject a serialized UrlDevice.
   * @return The UrlDevice represented by the serialized object.
   */
  public static UrlDevice jsonDeserialize(JSONObject jsonObject) {
    String id = jsonObject.getString(ID_KEY);
    String url = jsonObject.getString(URL_KEY);

    UrlDevice urlDevice = new UrlDevice(id, url);
    if (jsonObject.has(EXTRA_KEY)) {
      urlDevice.mExtraData = jsonObject.getJSONObject(EXTRA_KEY);
    }
    return urlDevice;
  }

  /**
   * Return a hash code for this SimpleUrlDevice.
   * This calculation does not include the extra data.
   * @return hash code
   */
  public int hashCode() {
    int hash = 1;
    hash = hash * 31 + mId.hashCode();
    hash = hash * 31 + mUrl.hashCode();
    return hash;
  }

  /**
   * Check if two UrlDevices are equal.
   * This does not compare extra data.
   * @param other the UrlDevice to compare to.
   * @return true if the UrlDevices are equal
   */
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (other instanceof UrlDevice) {
      UrlDevice urlDevice = (UrlDevice) other;
      return compareTo(urlDevice) == 0;
    }
    return false;
  }

  /**
   * Compare two UrlDevices based on device ID, breaking ties by comparing broadcast URL.
   * @param other the UrlDevice to compare to.
   * @return the comparison value.
   */
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
