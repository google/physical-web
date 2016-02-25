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

import java.util.Collection;

/**
 * Callback to implement for requests made to the Physical Web Service.
 */
public abstract class PwsResultCallback {
  /**
   * Handles a valid PwsResult.
   * @param pwsResult The result returned from the Physical Web Service.
   */
  public abstract void onPwsResult(PwsResult pwsResult);

  /**
   * Handles a URL that did not receive a result from the PWS.
   * @param url The URL sent to the Physical Web Service.
   */
  public void onPwsResultAbsent(String url) {}

  /**
   * Handles an error that occurred while attempting to use the Physical Web Service.
   * @param urls The urls sent in batch to the Physical Web Service.
   * @param httpResponseCode The HTTP response code returned by the Physical
   *        Web Service.  This will be 0 if a response was never received.
   * @param e The encountered exception.
   */
  public void onPwsResultError(Collection<String> urls, int httpResponseCode, Exception e) {}

  /**
   * Handles error that occurred while attempting to use the Physical Web Service.
   * @param durationMillis The number of milliseconds it took to receive a response.
   */
  public void onResponseReceived(long durationMillis) {}
}
