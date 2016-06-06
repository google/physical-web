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
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * HTTP client that makes requests to the Physical Web Service.
 */
public class PwsClient {
  private static final String DEFAULT_PWS_ENDPOINT = "https://url-caster.appspot.com";
  private static final int DEFAULT_PWS_VERSION = 1;
  private static final String V1_RESOLVE_SCAN_PATH = "resolve-scan";
  private static final String V2_RESOLVE_SCAN_PATH = "v1alpha1/urls:resolve";
  private static final String UKNOWN_API_ERROR_MESSAGE = "Unknown API Version";
  private String mPwsEndpoint;
  private String apiKey;
  private int apiVersion;
  private List<Thread> mThreads;

  /**
   * Construct a PwsClient.
   */
  public PwsClient() {
    this(DEFAULT_PWS_ENDPOINT, DEFAULT_PWS_VERSION);
  }

  /**
   * Construct a PwsClient.
   * @param pwsEndpoint The URL to send requests to.
   * @param pwsApiVersion The API version the endpoint uses.
   */
  public PwsClient(String pwsEndpoint, int pwsApiVersion) {
    this(pwsEndpoint, pwsApiVersion, "");
  }

  /**
   * Construct a PwsClient.
   * @param pwsEndpoint The URL to send requests to.
   * @param pwsApiVersion The API version the endpoint uses.
   * @param pwsApiKey The api key to access the endpoint.
   */
  public PwsClient(String pwsEndpoint, int pwsApiVersion, String pwsApiKey) {
    setEndpoint(pwsEndpoint, pwsApiVersion, pwsApiKey);
    mThreads = new ArrayList<>();
  }

  /**
   * Set the URL, the API version, the API Key for making PWS requests.
   * @param pwsEndpoint The new PWS endpoint.
   * @param pwsApiVersion The new PWS API version.
   */
  public void setEndpoint(String pwsEndpoint, int pwsApiVersion) {
    setEndpoint(pwsEndpoint, pwsApiVersion, null);
  }

  /**
   * Set the URL, the API version, the API Key for making PWS requests.
   * @param pwsEndpoint The new PWS endpoint.
   * @param pwsApiVersion The new PWS API version.
   * @param pwsApiKey The new PWS API key.
   */
  public void setEndpoint(String pwsEndpoint, int pwsApiVersion, String pwsApiKey) {
    if ((pwsApiKey == null || pwsApiKey.isEmpty()) && pwsApiVersion >= 2){
      throw new RuntimeException("API Version 2 or higher requires an API key");
    }
    mPwsEndpoint = pwsEndpoint;
    apiVersion = pwsApiVersion;
    apiKey = pwsApiKey;
  }

  private String constructPwsResolveUrl() {
    switch(apiVersion){
      case 1:
        return mPwsEndpoint + "/" + V1_RESOLVE_SCAN_PATH;
      case 2:
       return mPwsEndpoint + "/" + V2_RESOLVE_SCAN_PATH + "?key=" + apiKey;
      default:
        throw new RuntimeException(UKNOWN_API_ERROR_MESSAGE);
    }
  }

  /**
   * Send an HTTP request to the PWS to resolve a set of URLs.
   * @param broadcastUrls The URLs to resolve.
   * @param pwsResultCallback The callback to be run when the response is received.
   */
  public void resolve(final Collection<String> broadcastUrls,
                      final PwsResultCallback pwsResultCallback) {
    // Create the response callback.
    final long startTime = new Date().getTime();
    JsonObjectRequest.RequestCallback requestCallback = new JsonObjectRequest.RequestCallback() {
      private void recordResponse() {
        pwsResultCallback.onResponseReceived(new Date().getTime() - startTime);
      }

      private PwsResult getPwsResult(JSONObject jsonUrlMetadata){
        switch(apiVersion){
          case 1:
            return getV1PwsResult(jsonUrlMetadata);
          case 2:
            return getV2PwsResult(jsonUrlMetadata);
          default:
            throw new RuntimeException(UKNOWN_API_ERROR_MESSAGE);
        }
      }

      private PwsResult getV1PwsResult(JSONObject jsonUrlMetadata){
        try {
          return new PwsResult.Builder(
              jsonUrlMetadata.getString("id"), jsonUrlMetadata.getString("url"))
              .setTitle(jsonUrlMetadata.optString("title"))
              .setDescription(jsonUrlMetadata.optString("description"))
              .setIconUrl(jsonUrlMetadata.optString("icon"))
              .setGroupId(jsonUrlMetadata.optString("groupId"))
              .build();
        } catch (JSONException e) {
          return null;
        }
      }

      private PwsResult getV2PwsResult(JSONObject jsonUrlMetadata){
        try {
          JSONObject jsonPageInfo = jsonUrlMetadata.getJSONObject("pageInfo");
          return new PwsResult.Builder(
              jsonUrlMetadata.getString("scannedUrl"), jsonUrlMetadata.getString("resolvedUrl"))
              .setTitle(jsonPageInfo.optString("title"))
              .setDescription(jsonPageInfo.optString("description"))
              .setIconUrl(jsonPageInfo.optString("icon"))
              .build();
        } catch (JSONException e) {
          return null;
        }
      }

      public void onResponse(JSONObject result) {
        recordResponse();

        // Build the metadata from the response.
        JSONArray foundMetadata;
        String jsonKey;
        switch(apiVersion){
          case 1:
            jsonKey = "metadata";
            break;
          case 2:
            jsonKey = "results";
            break;
          default:
            throw new RuntimeException(UKNOWN_API_ERROR_MESSAGE);
        }

        try {
          foundMetadata = result.getJSONArray(jsonKey);
        } catch (JSONException e) {
          pwsResultCallback.onPwsResultError(broadcastUrls, 200, e);
          return;
        }

        // Loop through the metadata for each url.
        Set<String> foundUrls = new HashSet<>();
        for (int i = 0; i < foundMetadata.length(); i++) {

          JSONObject jsonUrlMetadata = foundMetadata.getJSONObject(i);
          PwsResult pwsResult = getPwsResult(jsonUrlMetadata);

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
        recordResponse();
        pwsResultCallback.onPwsResultError(broadcastUrls, responseCode, e);
      }
    };

    // Create the request.
    String targetUrl = constructPwsResolveUrl();
    JSONObject payload = new JSONObject();
    try {
      JSONArray urls = new JSONArray();
      for (String url : broadcastUrls) {
        JSONObject obj = new JSONObject();
        obj.put("url", url);
        urls.put(obj);
      }
      String jsonKey;
      switch(apiVersion){
        case 1:
          jsonKey = "objects";
          break;
        case 2:
          jsonKey = "urls";
          break;
        default:
          throw new RuntimeException(UKNOWN_API_ERROR_MESSAGE);
      }
      payload.put(jsonKey, urls);

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
   * Given an icon url returned by the PWS, fetch that icon.
   * @param url The icon URL returned by the PWS.
   * @param pwsResultIconCallback The callback to run on an HTTP response.
   */
  public void downloadIcon(final String url, final PwsResultIconCallback pwsResultIconCallback) {
    BitmapRequest.RequestCallback requestCallback = new BitmapRequest.RequestCallback() {
      public void onResponse(byte[] result) {
        pwsResultIconCallback.onIcon(result);
      }

      public void onError(int responseCode, Exception e) {
        pwsResultIconCallback.onError(responseCode, e);
      }
    };

    Request request;
    try {
      request = new BitmapRequest(url, requestCallback);
    } catch (MalformedURLException e) {
      pwsResultIconCallback.onError(0, e);
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
