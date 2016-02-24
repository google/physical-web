/*
 * Copyright 2016 Google Inc. All rights reserved.
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
 * Callback to implement for requests made to fetch icons.
 */
public abstract class PwsResultIconCallback {
  /**
   * Handle a valid PwsResult.
   * @param icon The icon returned from the HTTP request.
   */
  public abstract void onIcon(byte[] icon);

  /**
   * Handle an error that occurred while attempting to use the Physical Web
   * Service.
   * @param httpResponseCode The HTTP response code returned by the Physical
   *        Web Service.  This will be 0 if a response was never received.
   * @param e The encountered exception.
   */
  public void onError(int httpResponseCode, Exception e) {}
}
