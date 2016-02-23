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
 * Metadata returned from the Physical Web Service for a single URL.
 */
public class PwsResult implements Comparable<PwsResult> {
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
   * @param requestUrl The request URL, as broadcasted by the device
   * @param siteUrl The site URL, as reported by PWS
   * @param groupId The URL group ID, as reported by PWS
   */
  public PwsResult(
      String requestUrl, String siteUrl, String title, String description, String iconUrl,
      String groupId) {
    mRequestUrl = requestUrl;
    mSiteUrl = siteUrl;
    mIconUrl = iconUrl;
    mTitle = title;
    mDescription = description;
    mGroupId = groupId;
    mExtraData = new JSONObject();
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
   * Fetches the title.
   * The title is parsed from the title tag of the web page.
   * @return The title
   */
  public String getTitle() {
    return mTitle;
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
    jsonObject.put(REQUESTURL_KEY, mRequestUrl);
    jsonObject.put(SITEURL_KEY, mSiteUrl);
    jsonObject.put(TITLE_KEY, mTitle);
    jsonObject.put(DESCRIPTION_KEY, mDescription);

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
    String requestUrl = jsonObject.getString(REQUESTURL_KEY);
    String siteUrl = jsonObject.getString(SITEURL_KEY);
    String title = jsonObject.getString(TITLE_KEY);
    String description = jsonObject.getString(DESCRIPTION_KEY);
    String iconUrl = null;
    if (jsonObject.has(ICONURL_KEY)) {
      iconUrl = jsonObject.getString(ICONURL_KEY);
    }
    String groupId = null;
    if (jsonObject.has(GROUPID_KEY)) {
      groupId = jsonObject.getString(GROUPID_KEY);
    }

    PwsResult pwsResult = new PwsResult(requestUrl, siteUrl, title, description, iconUrl, groupId);
    if (jsonObject.has(EXTRA_KEY)) {
      pwsResult.mExtraData = jsonObject.getJSONObject(EXTRA_KEY);
    }
    return pwsResult;
  }

  /**
   * Return a hash code for this PwsResult.
   * This calculation does not include the extra data.
   * @return hash code
   */
  @Override
  public int hashCode() {
    int hash = 1;
    hash = hash * 31 + mRequestUrl.hashCode();
    hash = hash * 31 + mSiteUrl.hashCode();
    hash = hash * 31 + mTitle.hashCode();
    hash = hash * 31 + mDescription.hashCode();
    hash = hash * 31 + mIconUrl.hashCode();
    hash = hash * 31 + ((mGroupId == null) ? 0 : mGroupId.hashCode());
    return hash;
  }

  /**
   * Check if two PwsResults are equal.
   * This does not compare extra data.
   * @param other the PwsResult to compare to.
   * @return true if the PwsResults are equal.
   */
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (other instanceof PwsResult) {
      PwsResult pwsResult = (PwsResult) other;
      return compareTo(pwsResult) == 0;
    }
    return false;
  }

  /**
   * Compare two PwsResults based on request URL alphabetical ordering, breaking ties by comparing
   * site URL and group ID.
   * @param other the PwsResult to compare to.
   * @return the comparison value.
   */
  @Override
  public int compareTo(PwsResult other) {
    if (this == other) {
      return 0;
    }

    // 1. mRequestUrl
    int compareValue = Utils.nullSafeCompare(mRequestUrl, other.mRequestUrl);
    if (compareValue != 0) {
      return compareValue;
    }

    // 2. mSiteUrl
    compareValue = Utils.nullSafeCompare(mSiteUrl, other.mSiteUrl);
    if (compareValue != 0) {
      return compareValue;
    }

    // 3. mTitle
    compareValue = Utils.nullSafeCompare(mTitle, other.mTitle);
    if (compareValue != 0) {
      return compareValue;
    }

    // 4. mDescription
    compareValue = Utils.nullSafeCompare(mDescription, other.mDescription);
    if (compareValue != 0) {
      return compareValue;
    }

    // 5. mIconUrl
    compareValue = Utils.nullSafeCompare(mIconUrl, other.mIconUrl);
    if (compareValue != 0) {
      return compareValue;
    }

    // 6. mGroupId
    return Utils.nullSafeCompare(mGroupId, other.mGroupId);
  }
}
