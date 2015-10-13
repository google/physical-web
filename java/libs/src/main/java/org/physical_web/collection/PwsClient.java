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
 * HTTP client that makes requests to the Physical Web Service.
 */
public class PwsClient {
  private String mPwsEndpoint;

  /**
   * Construct a PwsClient.
   * @param pwsEndpoint The URL to send requests to.
   */
  public PwsClient(String pwsEndpoint) {
    setPwsEndpoint(pwsEndpoint);
  }

  /**
   * Set the URL for making PWS requests.
   * @param pwsEndpoint The new PWS endpoint.
   */
  public void setPwsEndpoint(String pwsEndpoint) {
    mPwsEndpoint = pwsEndpoint;
  }

  /**
   * Send an HTTP request to the PWS to resolve a set of URLs.
   * @param broadcastUrls The URLs to resolve.
   */
  public void resolve(Collection broadcastUrls, PwsResultCallback pwsResultCallback) {
    // TODO(cco3): Implement this
  }
}
