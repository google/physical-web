/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package org.physical_web.physicalweb;

import org.physical_web.physicalweb.PwoMetadata.BleMetadata;
import org.physical_web.physicalweb.PwoMetadata.UrlMetadata;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;

/**
 * This class sends requests to the physical web service.
 *
 * For the largest use, metadata requests, the physical web service
 * scrapes the page at the given url for its metadata.
 */
class PwsClient {
  private static final String TAG = "PwsClient";
  private static final String PROD_URL = "https://url-caster.appspot.com";
  private static final String DEV_URL = "https://url-caster-dev.appspot.com";
  private static final String RESOLVE_SCAN_PATH = "resolve-scan";
  private static final String SHORTEN_URL_PATH = "shorten-url";
  private static final int UNDEFINED_SCORE = -1;
  private RequestQueue mRequestQueue;
  private Context mContext;
  private String mEndpointUrl;

  private static PwsClient mInstance;

  private PwsClient(Context context) {
    mContext = context.getApplicationContext();
    mRequestQueue = Volley.newRequestQueue(mContext);
    mEndpointUrl = PROD_URL;
  }

  public static PwsClient getInstance(Context context) {
    if (mInstance == null) {
        mInstance = new PwsClient(context);
    }
    return mInstance;
  }


  /////////////////////////////////
  // callbacks
  /////////////////////////////////

  public interface ResolveScanCallback {
    public void onUrlMetadataReceived(PwoMetadata pwoMetadata);
    public void onUrlMetadataIconReceived(PwoMetadata pwoMetadata);
  }

  public interface ShortenUrlCallback {
    public void onUrlShortened(String shortUrl);
    public void onError(String longUrl);
  }


  /////////////////////////////////
  // utilities
  /////////////////////////////////

  private String constructUrlStr(final String path) {
    return mEndpointUrl + "/" + path;
  }

  public void shortenUrl(final String longUrl,
                         final ShortenUrlCallback shortenUrlCallback,
                         final String tag) {
    // Create the json payload
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("longUrl", longUrl);
    } catch (JSONException e) {
      Log.e(TAG, "JSONException: " + e.toString());
      shortenUrlCallback.onError(longUrl);
      return;
    }

    // Create the http request
    JsonObjectRequest request = new JsonObjectRequest(
        constructUrlStr(SHORTEN_URL_PATH),
        jsonObject,
        new Response.Listener<JSONObject>() {
          @Override
          public void onResponse(JSONObject jsonResponse) {
            String shortUrl;
            try {
              shortUrl = jsonResponse.getString("id");
            } catch (JSONException e) {
              Log.e(TAG, "JSONException: " + e.toString());
              shortenUrlCallback.onError(longUrl);
              return;
            }
            shortenUrlCallback.onUrlShortened(shortUrl);
          }
        },
        new Response.ErrorListener() {
          @Override
          public void onErrorResponse(VolleyError volleyError) {
            Log.i(TAG, "VolleyError: " + volleyError.toString());
            shortenUrlCallback.onError(longUrl);
          }
        }
    );
    request.setTag(tag);

    // Send off the request
    mRequestQueue.add(request);
  }

  public void findUrlMetadata(PwoMetadata pwoMetadata,
                              ResolveScanCallback resolveScanCallback,
                              final String tag) {
    // Create the metadata request
    // for the given json request object
    JsonObjectRequest request = createUrlMetadataRequest(pwoMetadata, resolveScanCallback);
    request.setTag(tag);
    // Queue the request
    mRequestQueue.add(request);
  }

  /**
   * Create the json request object
   * that will be sent to the metadata server
   * asking for metadata for the given url.
   *
   * @param url The url for which the request data will be created
   * @return The constructed json object
   */
  private static JSONObject createUrlMetadataRequestObject(PwoMetadata pwoMetadata) {
    JSONObject jsonObject = new JSONObject();
    try {
      JSONArray urlJsonArray = new JSONArray();
      JSONObject urlJsonObject = new JSONObject();
      urlJsonObject.put("url", pwoMetadata.url);
      if (pwoMetadata.hasBleMetadata()) {
        BleMetadata bleMetadata = pwoMetadata.bleMetadata;
        urlJsonObject.put("txpower", bleMetadata.txPower);
        urlJsonObject.put("rssi", bleMetadata.rssi);
      }
      urlJsonArray.put(urlJsonObject);
      jsonObject.put("objects", urlJsonArray);
    } catch (JSONException ex) {
      Log.d(TAG, "error: " + ex);
    }
    return jsonObject;
  }

  /**
   * Create the url metadata request, given the json request object.
   *
   * @param jsonObj The given json object to use in the request
   * @return The created json request object
   */
  private JsonObjectRequest createUrlMetadataRequest(
      final PwoMetadata pwoMetadata,
      final ResolveScanCallback resolveScanCallback) {
    String resolveScanPath = RESOLVE_SCAN_PATH;
    final long creationTimestamp = new Date().getTime();

    // Create the json request object
    JSONObject jsonObj = createUrlMetadataRequestObject(pwoMetadata);
    return new JsonObjectRequest(
        constructUrlStr(resolveScanPath),
        jsonObj,
        new Response.Listener<JSONObject>() {
          // Called when the server returns a response
          @Override
          public void onResponse(JSONObject jsonResponse) {

            // Build the metadata from the response
            try {
              JSONArray foundMetaData = jsonResponse.getJSONArray("metadata");

              // Loop through the metadata for each url
              if (foundMetaData.length() > 0) {

                for (int i = 0; i < foundMetaData.length(); i++) {
                  JSONObject jsonUrlMetadata = foundMetaData.getJSONObject(i);

                  UrlMetadata urlMetadata = new UrlMetadata();
                  urlMetadata.id = jsonUrlMetadata.getString("id");
                  urlMetadata.siteUrl = jsonUrlMetadata.getString("url");
                  urlMetadata.displayUrl = jsonUrlMetadata.getString("displayUrl");
                  urlMetadata.title = jsonUrlMetadata.optString("title");
                  urlMetadata.description = jsonUrlMetadata.optString("description");
                  urlMetadata.iconUrl = jsonUrlMetadata.optString("icon");
                  urlMetadata.rank = (float) jsonUrlMetadata.getDouble("rank");

                  pwoMetadata.setUrlMetadata(urlMetadata,
                                             new Date().getTime() - creationTimestamp);

                  if (!urlMetadata.iconUrl.isEmpty()) {
                    downloadIcon(pwoMetadata, resolveScanCallback);
                  }
                  resolveScanCallback.onUrlMetadataReceived(pwoMetadata);
                }

              }
            } catch (JSONException e) {
              Log.i(TAG, "Pws gave bad JSON: " + e);
            }
          }
        },
        new Response.ErrorListener() {

          @Override
          public void onErrorResponse(VolleyError volleyError) {
            Log.i(TAG, "VolleyError: " + volleyError.toString());
          }
        }
    );
  }

  /**
   * Asynchronously download the image for the url favicon.
   *
   * @param urlMetadata The metadata for the given url
   */
  private void downloadIcon(final PwoMetadata pwoMetadata,
                            final ResolveScanCallback resolveScanCallback) {
    final UrlMetadata urlMetadata = pwoMetadata.urlMetadata;
    ImageRequest imageRequest = new ImageRequest(urlMetadata.iconUrl,
                                                 new Response.Listener<Bitmap>() {
      @Override
      public void onResponse(Bitmap response) {
        urlMetadata.icon = response;
        resolveScanCallback.onUrlMetadataIconReceived(pwoMetadata);
      }
    }, 0, 0, null, null);
    mRequestQueue.add(imageRequest);
  }

  public String createUrlProxyGoLink(String url) {
    try {
      url = mContext.getString(R.string.proxy_go_link_base_url) + URLEncoder.encode(url, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      Log.e(TAG, "createUrlProxyGoLink error: " + e);
    }
    return url;
  }

  public void cancelAllRequests(final String tag) {
    mRequestQueue.cancelAll(tag);
  }

  public void useProdEndpoint() {
      mEndpointUrl = PROD_URL;
  }

  public void useDevEndpoint() {
      mEndpointUrl = DEV_URL;
  }
}
