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
 * A basic implementation of the PWO interface.
 */
public class SimplePwo implements Pwo {
  private static final String PWO_TYPE = "simple";
  private static final String ID_KEY = "id";
  private static final String URL_KEY = "url";
  private String mId;
  private String mUrl;

  /**
   * Construct a SimplePwo.
   * @param id The id of the PWO.
   * @param url The URL broadcasted by the PWO.
   */
  SimplePwo(String id, String url) {
    mId = id;
    mUrl = url;
  }

  /**
   * Fetches the ID of the PWO.
   * The ID should be unique across PWOs.  This should even be the case when
   * one real world device is broadcasting multiple URLs.
   * @return The ID of the PWO.
   */
  public String getId() {
    return mId;
  }

  /**
   * Fetches the URL broadcasted by the PWO.
   * @return The broadcasted URL.
   */
  public String getUrl() {
    return mUrl;
  }

  /**
   * Creates a JSON object that represents the PWO.
   * @return The JSON object representing the PWO.
   */
  public JSONObject toJsonObject() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(ID_KEY, mId);
    jsonObject.put(URL_KEY, mUrl);
    return jsonObject;
  }
}
