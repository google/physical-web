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
public class SimpleUrlDevice implements UrlDevice {
  private String mId;
  private String mUrl;

  /**
   * Construct a SimpleUrlDevice.
   * @param id The id of the device.
   * @param url The URL broadcasted by the device.
   */
  SimpleUrlDevice(String id, String url) {
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
}
