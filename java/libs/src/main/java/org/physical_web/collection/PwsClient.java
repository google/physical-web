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

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * HTTP client that makes requests to the Physical Web Service.
 */
public class PwsClient {
  private static final String RESOLVE_SCAN_PATH = "resolve-scan";
  private String mPwsEndpoint;
  private List<Thread> mThreads;

  /**
   * Construct a PwsClient.
   * @param pwsEndpoint The URL to send requests to.
   */
  public PwsClient(String pwsEndpoint) {
    setPwsEndpoint(pwsEndpoint);
    mThreads = new ArrayList<>();
  }

  /**
   * Set the URL for making PWS requests.
   * @param pwsEndpoint The new PWS endpoint.
   */
  public void setPwsEndpoint(String pwsEndpoint) {
    mPwsEndpoint = pwsEndpoint;
  }

  private String constructPwsUrl(String path) {
    return mPwsEndpoint + "/" + path;
  }

  /**
   * Send an HTTP request to the PWS to resolve a set of URLs.
   * @param broadcastUrls The URLs to resolve.
   * @param pwsResultCallback The callback to be run when the response is received.
   */
  public void resolve(final Collection<String> broadcastUrls,
                      final PwsResultCallback pwsResultCallback) {
    // Create the response callback.
    JsonObjectRequest.RequestCallback requestCallback = new JsonObjectRequest.RequestCallback() {
      public void onResponse(JSONObject result) {
        // Build the metadata from the response.
        JSONArray foundMetadata;
        try {
          foundMetadata = result.getJSONArray("metadata");
        } catch (JSONException e) {
          pwsResultCallback.onPwsResultError(broadcastUrls, 200, e);
          return;
        }

        // Loop through the metadata for each url.
        Set<String> foundUrls = new HashSet<>();
        for (int i = 0; i < foundMetadata.length(); i++) {
          String requestUrl = null;
          String responseUrl = null;
          try {
            JSONObject jsonUrlMetadata = foundMetadata.getJSONObject(i);
            requestUrl = jsonUrlMetadata.getString("id");
            responseUrl = jsonUrlMetadata.getString("url");
          } catch (JSONException e) {
            continue;
          }
          PwsResult pwsResult = new PwsResult(requestUrl, responseUrl, null);
          pwsResultCallback.onPwsResult(pwsResult);
          foundUrls.add(pwsResult.getRequestUrl());
        }

        // See which urls the PWS didn't give us a response for.
        Set<String> missed = new HashSet<>(broadcastUrls);
        missed.removeAll(foundUrls);
        for (String url : missed) {
          pwsResultCallback.onPwsResultAbsent(url);
        }
      }

      public void onError(int responseCode, Exception e) {
        pwsResultCallback.onPwsResultError(broadcastUrls, responseCode, e);
      }
    };

    // Create the request.
    String targetUrl = constructPwsUrl(RESOLVE_SCAN_PATH);
    JSONObject payload = new JSONObject();
    try {
      JSONArray urls = new JSONArray();
      for (String url : broadcastUrls) {
        JSONObject obj = new JSONObject();
        obj.put("url", url);
        urls.put(obj);
      }
      payload.put("objects", urls);
    } catch (JSONException e) {
      pwsResultCallback.onPwsResultError(broadcastUrls, 0, e);
      return;
    }
    Request request;
    try {
      request = new JsonObjectRequest(targetUrl, payload, requestCallback);
    } catch (MalformedURLException e) {
      pwsResultCallback.onPwsResultError(broadcastUrls, 0, e);
      return;
    }
    makeRequest(request);
  }

  /**
   * Cancel all current HTTP requests.
   */
  public void cancelAllRequests() {
    for (Thread thread : mThreads) {
      thread.interrupt();
    }
    mThreads.clear();
  }

  private void makeRequest(Request request) {
    // Remove all threads that are no longer alive.
    for (Iterator<Thread> iterator = mThreads.iterator(); iterator.hasNext();) {
      if (!iterator.next().isAlive()) {
        iterator.remove();
      }
    }

    // Start the new thread and record it.
    request.start();
    mThreads.add(request);
  }
}
