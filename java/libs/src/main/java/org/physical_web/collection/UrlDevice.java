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
 * The interface defining a Physical Web URL device.
 */
public interface UrlDevice extends Comparable<UrlDevice> {
  /**
   * Fetches the ID of the UrlDevice.
   * The ID should be unique across UrlDevices.  This should even be the case when
   * one real world device is broadcasting multiple URLs.
   * @return The ID of the UrlDevice.
   */
  String getId();

  /**
   * Fetches the URL broadcasted by the UrlDevice.
   * @return The broadcasted URL.
   */
  String getUrl();

  /**
   * Returns the rank for a given UrlDevice and its associated PwsResult.
   * Guidelines for values:
   * - Rank should typically be based on quality signals from the UrlDevice
   *   and pwsResult.  E.g. if we know the UrlDevice knows its distance from
   *   the user, we could combine this information with the quality signal from
   *   pwsResult.
   * - Rank should typically be between 0 and 1, but out of bounds values will
   *   not be rounded into range.
   * - 0 should represent extremely low quality
   * - .5 should represent median quality
   * - 1 should represent extremely high quality
   * @param pwsResult is the response received from the Physical Web Service
   *        for the url broadcasted by this UrlDevice.
   * @return The rank of the UrlDevice, given its associated PwsResult.
   */
  double getRank(PwsResult pwsResult);
}
