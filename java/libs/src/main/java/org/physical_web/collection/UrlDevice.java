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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The class defining a Physical Web URL device.
 */
public class UrlDevice {
  private static final String ID_KEY = "id";
  private static final String URL_KEY = "url";
  private static final String EXTRA_KEY = "extra";
  private final String mId;
  private final String mUrl;
  private final JSONObject mExtraData;

  /**
   * Construct a UrlDevice.
   * @param id The id of the device.
   * @param url The URL broadcasted by the device.
   * @param extraData Extra data to associate with this UrlDevice.
   */
  private UrlDevice(String id, String url, JSONObject extraData) {
    mId = id;
    mUrl = url;
    mExtraData = extraData == null ? new JSONObject() : new JSONObject(extraData.toString());
  }

  /**
   * Construct a UrlDevice.
   * @param id The id of the device.
   * @param url The URL broadcasted by the device.
   */
  public UrlDevice(String id, String url) {
    this(id, url, null);
  }

  /**
   * Builder class for constructing UrlDevices.
   */
  public static class Builder {
    private String mNewId;
    private String mNewUrl;
    private JSONObject mNewExtraData;

    /**
     * Construct a UrlDevice Builder.
     * @param id The id of the device.
     * @param url The URL broadcasted by the device.
     */
    public Builder(String id, String url) {
      mNewId = id;
      mNewUrl = url;
      mNewExtraData = new JSONObject();
    }

    /**
     * Construct a UrlDevice Builder.
     * @param urlDevice the UrlDevice to clone.
     */
    public Builder(UrlDevice urlDevice) {
      mNewId = urlDevice.mId;
      mNewUrl = urlDevice.mUrl;
      mNewExtraData = new JSONObject(urlDevice.mExtraData.toString());
    }

    /**
     * Sets a JSONObject to be the base extra data.
     * @param extraData the base extra data.
     * @return the Builder object for chaining operations.
     */
    public Builder setExtra(JSONObject extraData) {
      mNewExtraData = extraData == null ? new JSONObject() : new JSONObject(extraData.toString());
      return this;
    }

    /**
     * Stores a boolean as extra data.
     * @param value The value to store.
     * @return the Builder object for chaining operations.
     */
    public Builder addExtra(String key, boolean value) {
      mNewExtraData.put(key, value);
      return this;
    }

    /**
     * Stores an int as extra data.
     * @param value The value to store.
     * @return the Builder object for chaining operations.
     */
    public Builder addExtra(String key, int value) {
      mNewExtraData.put(key, value);
      return this;
    }

    /**
     * Stores an long as extra data.
     * @param value The value to store.
     * @return the Builder object for chaining operations.
     */
    public Builder addExtra(String key, long value) {
      mNewExtraData.put(key, value);
      return this;
    }

    /**
     * Stores an object as extra data.
     * @param value The value to store.  Any object of type JSONObject, JSONArray, String, Boolean,
     *     Integer, Long, Double, NULL, or null. May not be NaNs or infinities.
     * @return the Builder object for chaining operations.
     */
    public Builder addExtra(String key, Object value) {
      mNewExtraData.put(key, value);
      return this;
    }

    /**
     * Creates a UrlDevice from data provided to the builder.
     * @return The constructed UrlDevice.
     */
    public UrlDevice build() {
      return new UrlDevice(mNewId, mNewUrl, mNewExtraData);
    }
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
   * Get extra boolean value.
   * @param key The key of the stored value.
   * @return The stored value.
   * @throws JSONException If the mapping doesn't exist or is not the required type.
   */
  public boolean getExtraBoolean(String key) throws JSONException {
    return mExtraData.getBoolean(key);
  }

  /**
   * Get extra boolean value.
   * @param key The key of the stored value.
   * @return The stored value or false if it doesn't exist in specified form.
   */
  public boolean optExtraBoolean(String key) {
    return mExtraData.optBoolean(key);
  }

  /**
   * Get extra boolean value.
   * @param key The key of the stored value.
   * @param defaultValue The value to return if the key does not exist or the value cannot be
   *     coerced into the necessary type.
   * @return The stored value or the provided default if it doesn't exist in specified form.
   */
  public boolean optExtraBoolean(String key, boolean defaultValue) {
    return mExtraData.optBoolean(key, defaultValue);
  }

  /**
   * Get extra int value.
   * @param key The key of the stored value.
   * @return The stored value.
   * @throws JSONException If the mapping doesn't exist or is not the required type.
   */
  public int getExtraInt(String key) throws JSONException {
    return mExtraData.getInt(key);
  }

  /**
   * Get extra int value.
   * @param key The key of the stored value.
   * @return The stored value or 0 if it doesn't exist in specified form.
   */
  public int optExtraInt(String key) {
    return mExtraData.optInt(key);
  }

  /**
   * Get extra int value.
   * @param key The key of the stored value.
   * @param defaultValue The value to return if the key does not exist or the value cannot be
   *     coerced into the necessary type.
   * @return The stored value or the provided default if it doesn't exist in specified form.
   */
  public int optExtraInt(String key, int defaultValue) {
    return mExtraData.optInt(key, defaultValue);
  }

  /**
   * Get extra long value.
   * @param key The key of the stored value.
   * @return The stored value.
   * @throws JSONException If the mapping doesn't exist or is not the required type.
   */
  public long getExtraLong(String key) throws JSONException {
    return mExtraData.getLong(key);
  }

  /**
   * Get extra long value.
   * @param key The key of the stored value.
   * @return The stored value or 0 if it doesn't exist in specified form.
   */
  public long optExtraLong(String key) {
    return mExtraData.optLong(key);
  }

  /**
   * Get extra long value.
   * @param key The key of the stored value.
   * @param defaultValue The value to return if the key does not exist or the value cannot be
   *     coerced into the necessary type.
   * @return The stored value or the provided default if it doesn't exist in specified form.
   */
  public long optExtraLong(String key, long defaultValue) {
    return mExtraData.optLong(key, defaultValue);
  }

  /**
   * Get extra double value.
   * @param key The key of the stored value.
   * @return The stored value.
   * @throws JSONException If the mapping doesn't exist or is not the required type.
   */
  public double getExtraDouble(String key) throws JSONException {
    return mExtraData.getDouble(key);
  }

  /**
   * Get extra double value.
   * @param key The key of the stored value.
   * @return The stored value or 0 if it doesn't exist in specified form.
   */
  public double optExtraDouble(String key) {
    return mExtraData.optDouble(key);
  }

  /**
   * Get extra double value.
   * @param key The key of the stored value.
   * @param defaultValue The value to return if the key does not exist or the value cannot be
   *     coerced into the necessary type.
   * @return The stored value or the provided default if it doesn't exist in specified form.
   */
  public double optExtraDouble(String key, double defaultValue) {
    return mExtraData.optDouble(key, defaultValue);
  }

  /**
   * Get extra String value.
   * @param key The key of the stored value.
   * @return The stored value.
   * @throws JSONException If the mapping doesn't exist or is not the required type.
   */
  public String getExtraString(String key) throws JSONException {
    return mExtraData.getString(key);
  }

  /**
   * Get extra String value.
   * @param key The key of the stored value.
   * @return The stored value or null if it doesn't exist in specified form.
   */
  public String optExtraString(String key) {
    return mExtraData.optString(key);
  }

  /**
   * Get extra String value.
   * @param key The key of the stored value.
   * @param defaultValue The value to return if the key does not exist or the value cannot be
   *     coerced into the necessary type.
   * @return The stored value or the provided default if it doesn't exist in specified form.
   */
  public String optExtraString(String key, String defaultValue) {
    return mExtraData.optString(key, defaultValue);
  }

  /**
   * Get extra JSONArray value.
   * @param key The key of the stored value.
   * @return The stored value.
   * @throws JSONException If the mapping doesn't exist or is not the required type.
   */
  public JSONArray getExtraJSONArray(String key) throws JSONException {
    return mExtraData.getJSONArray(key);
  }

  /**
   * Get extra JSONArray value.
   * @param key The key of the stored value.
   * @return The stored value or null if it doesn't exist in specified form.
   */
  public JSONArray optExtraJSONArray(String key) {
    return mExtraData.optJSONArray(key);
  }

  /**
   * Get extra JSONObject value.
   * @param key The key of the stored value.
   * @return The stored value.
   * @throws JSONException If the mapping doesn't exist or is not the required type.
   */
  public JSONObject getExtraJSONObject(String key) throws JSONException {
    return mExtraData.getJSONObject(key);
  }

  /**
   * Get extra JSONObject value.
   * @param key The key of the stored value.
   * @return The stored value or null if it doesn't exist in specified form.
   */
  public JSONObject optExtraJSONObject(String key) {
    return mExtraData.optJSONObject(key);
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
    return new Builder(jsonObject.getString(ID_KEY), jsonObject.getString(URL_KEY))
       .setExtra(jsonObject.optJSONObject(EXTRA_KEY))
       .build();
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
