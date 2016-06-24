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
 * Metadata returned from the Physical Web Service for a single URL.
 */
public class PwsResult {
  private static final String REQUESTURL_KEY = "requesturl";
  private static final String SITEURL_KEY = "siteurl";
  private static final String TITLE_KEY = "title";
  private static final String DESCRIPTION_KEY = "description";
  private static final String ICONURL_KEY = "iconurl";
  private static final String GROUPID_KEY = "groupid";
  private static final String EXTRA_KEY = "extra";
  private String mRequestUrl;
  private String mSiteUrl;
  private String mTitle;
  private String mDescription;
  private String mIconUrl;
  private String mGroupId;
  private JSONObject mExtraData;

  /**
   * Construct a PwsResult.
   * @param requestUrl The request URL, as broadcasted by the device.
   * @param siteUrl The site URL, as reported by the PWS.
   * @param title The site title, as reported by the PWS.
   * @param description The site description, as reported by the PWS.
   * @param iconUrl The site icon URL, as reported by the PWS.
   * @param groupId The URL group ID, as reported by the PWS.
   * @param extraData Extra data to associate with this UrlDevice.
   */
  public PwsResult(
      String requestUrl, String siteUrl, String title, String description, String iconUrl,
      String groupId, JSONObject extraData) {
    mRequestUrl = requestUrl;
    mSiteUrl = siteUrl;
    mIconUrl = (iconUrl == null || iconUrl.isEmpty()) ? null : iconUrl;
    mTitle = (title == null || title.isEmpty()) ? null : title;
    mDescription = (description == null || description.isEmpty()) ? null : description;
    mGroupId = (groupId == null || groupId.isEmpty()) ? null : groupId;
    mExtraData = extraData == null ? new JSONObject() : new JSONObject(extraData.toString());
  }

  /**
   * Construct a PwsResult.
   * @param requestUrl The request URL, as broadcasted by the device.
   * @param siteUrl The site URL, as reported by the PWS.
   */
  public PwsResult(String requestUrl, String siteUrl) {
    this(requestUrl, siteUrl, null, null, null, null, null);
  }

  /**
   * Builder class for constructing PwsResults.
   */
  public static class Builder {
    private String mNewRequestUrl;
    private String mNewSiteUrl;
    private String mNewTitle;
    private String mNewDescription;
    private String mNewIconUrl;
    private String mNewGroupId;
    private JSONObject mNewExtraData;

    /**
     * Construct a PwsResult Builder.
     * @param requestUrl The request URL, as broadcasted by the device.
     * @param siteUrl The site URL, as reported by the PWS.
     */
    public Builder(String requestUrl, String siteUrl) {
      mNewRequestUrl = requestUrl;
      mNewSiteUrl = siteUrl;
      mNewTitle = null;
      mNewDescription = null;
      mNewIconUrl = null;
      mNewGroupId = null;
      mNewExtraData = new JSONObject();
    }

    /**
     * Construct a PwsResult Builder.
     * @param pwsResult the PWSResult to clone.
     */
    public Builder(PwsResult pwsResult) {
      mNewRequestUrl = pwsResult.mRequestUrl;
      mNewSiteUrl = pwsResult.mSiteUrl;
      mNewIconUrl = pwsResult.mIconUrl;
      mNewTitle = pwsResult.mTitle;
      mNewDescription = pwsResult.mDescription;
      mNewGroupId = pwsResult.mGroupId;
      mNewExtraData = new JSONObject(pwsResult.mExtraData.toString());
    }

    /**
     * Sets a group ID.
     * @param iconUrl The site icon URL, as reported by the PWS.
     * @return the Builder object for chaining operations.
     */
    public Builder setIconUrl(String iconUrl) {
      mNewIconUrl = iconUrl;
      return this;
    }

    /**
     * Sets a group ID.
     * @param title The site title, as reported by the PWS.
     * @return the Builder object for chaining operations.
     */
    public Builder setTitle(String title) {
      mNewTitle = title;
      return this;
    }

    /**
     * Sets a description.
     * @param description The site description, as reported by the PWS.
     * @return the Builder object for chaining operations.
     */
    public Builder setDescription(String description) {
      mNewDescription = description;
      return this;
    }

    /**
     * Sets a group ID.
     * @param groupId The URL group ID, as reported by the PWS.
     * @return the Builder object for chaining operations.
     */
    public Builder setGroupId(String groupId) {
      mNewGroupId = groupId;
      return this;
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
     * Creates a PWSResult from data provided to the builder.
     * @return The constructed PwsResult.
     */
    public PwsResult build() {
      return new PwsResult(
          mNewRequestUrl, mNewSiteUrl, mNewTitle, mNewDescription, mNewIconUrl, mNewGroupId,
          mNewExtraData);
    }
  }

  /**
   * Fetches the request URL.
   * The request URL is the query sent to the Physical Web Service and should be identical to the
   * URL broadcasted by the device.
   * @return The request URL
   */
  public String getRequestUrl() {
    return mRequestUrl;
  }

  /**
   * Fetches the site URL.
   * The site URL is returned by the Physical Web Service. It may differ from the request URL if
   * a redirector or URL shortener was used.
   * @return The site URL
   */
  public String getSiteUrl() {
    return mSiteUrl;
  }

  /**
   * Check if we have a title.
   * @return whether this object has a title.
   */
  public boolean hasTitle() {
    return mTitle != null;
  }

  /**
   * Fetches the title.
   * The title is parsed from the title tag of the web page.
   * @return The title
   */
  public String getTitle() {
    return mTitle;
  }

  /**
   * Check if we have a description.
   * @return whether this object has a description.
   */
  public boolean hasDescription() {
    return mDescription != null;
  }

  /**
   * Fetches the description.
   * The description is a snippet of text describing the contents of the web page.
   * @return The description
   */
  public String getDescription() {
    return mDescription;
  }

  /**
   * Check if we have an icon URL.
   * @return whether this object has an icon URL.
   */
  public boolean hasIconUrl() {
    return mIconUrl != null;
  }

  /**
   * Fetches the icon URL.
   * The icon URL is returned by the Physical Web Service.
   * @return The icon URL
   */
  public String getIconUrl() {
    return mIconUrl;
  }

  /**
   * Check if we have a group id.
   * @return whether this object has a group id.
   */
  public boolean hasGroupId() {
    return mGroupId != null;
  }

  /**
   * Fetches the URL group ID.
   * The group ID is returned by the Physical Web Service and is used to group similar (but not
   * necessarily identical) URLs.
   * @return The URL group ID, may be null
   */
  public String getGroupId() {
    return mGroupId;
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
    jsonObject.put(REQUESTURL_KEY, mRequestUrl);
    jsonObject.put(SITEURL_KEY, mSiteUrl);
    if (mTitle != null) {
      jsonObject.put(TITLE_KEY, mTitle);
    }
    if (mDescription != null) {
      jsonObject.put(DESCRIPTION_KEY, mDescription);
    }
    if (mIconUrl != null) {
      jsonObject.put(ICONURL_KEY, mIconUrl);
    }
    if (mGroupId != null) {
      jsonObject.put(GROUPID_KEY, mGroupId);
    }
    if (mExtraData.length() > 0) {
      jsonObject.put(EXTRA_KEY, mExtraData);
    }
    return jsonObject;
  }

  /**
   * Populate a PwsResult with data from a given JSON object.
   * @param jsonObject a serialized PwsResult.
   * @return The PwsResult represented by the serialized object.
   */
  public static PwsResult jsonDeserialize(JSONObject jsonObject) {
    return new Builder(jsonObject.getString(REQUESTURL_KEY), jsonObject.getString(SITEURL_KEY))
        .setExtra(jsonObject.optJSONObject(EXTRA_KEY))
        .setTitle(jsonObject.optString(TITLE_KEY))
        .setDescription(jsonObject.optString(DESCRIPTION_KEY))
        .setIconUrl(jsonObject.optString(ICONURL_KEY))
        .setGroupId(jsonObject.optString(GROUPID_KEY))
        .build();
  }
}
