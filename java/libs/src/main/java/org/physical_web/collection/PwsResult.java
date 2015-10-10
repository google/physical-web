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

/**
 * Metadata returned from the Physical Web Service for a single URL.
 */
public class PwsResult {
  private String mRequestUrl;
  private String mSiteUrl;
  private String mGroupId;

  /**
   * Construct a PwsResult.
   * @param requestUrl The request URL, as broadcasted by the device
   * @param siteUrl The site URL, as reported by PWS
   * @param groupId The URL group ID, as reported by PWS
   */
  PwsResult(String requestUrl, String siteUrl, String groupId) {
    mRequestUrl = requestUrl;
    mSiteUrl = siteUrl;
    mGroupId = groupId;
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
   * Fetches the URL group ID.
   * The group ID is returned by the Physical Web Service and is used to group similar (but not
   * necessarily identical) URLs.
   * @return The URL group ID, may be null
   */
  public String getGroupId() {
    return mGroupId;
  }
}
