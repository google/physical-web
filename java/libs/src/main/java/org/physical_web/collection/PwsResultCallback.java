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
public interface PwsResultCallback {
  /**
   * Handle a valid PwsResult.
   * @param pwsResult The result returned from the Physical Web Service.
   */
  void onPwsResult(PwsResult pwsResult);

  /**
   * Handle a URL that did not receive a result from the PWS.
   * @param url The URL sent to the Physical Web Service.
   */
  void onPwsResultAbsent(String url);

  /**
   * Handle an error that occurred while attempting to use the Physical Web
   * Service.
   * @param urls The urls sent in batch to the Physical Web Service.
   * @param httpResponseCode The HTTP response code returned by the Physical
   *        Web Service.  This will be 0 if a response was never received.
   * @param e The encountered exception.
   */
  void onPwsResultError(Collection<String> urls, int httpResponseCode, Exception e);
}
