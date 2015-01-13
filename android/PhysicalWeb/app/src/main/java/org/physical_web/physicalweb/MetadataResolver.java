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

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
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

/**
 * Class for resolving url metadata.
 * Sends requests to the metadata server
 * which then scrapes the page at the given url
 * for its metadata
 */

class MetadataResolver {
  private static final String TAG = "MetadataResolver";
  private static final String METADATA_URL = "http://url-caster.appspot.com/resolve-scan";
  private static final String DEMO_METADATA_URL = "http://url-caster.appspot.com/demo";
  private static RequestQueue mRequestQueue;
  private static boolean mIsInitialized = false;
  private static MetadataResolverCallback mMetadataResolverCallback;
  private static Context mContext;

  private static void initialize(Context context) {
    if (mRequestQueue == null) {
      mRequestQueue = Volley.newRequestQueue(context);
    }
    mIsInitialized = true;
    mContext = context;
  }


  /////////////////////////////////
  // callbacks
  /////////////////////////////////

  public interface MetadataResolverCallback {
    public void onUrlMetadataReceived(String url, UrlMetadata urlMetadata);

    public void onDemoUrlMetadataReceived(String url, UrlMetadata urlMetadata);

    public void onUrlMetadataIconReceived();
  }


  /////////////////////////////////
  // utilities
  /////////////////////////////////

  public static void findUrlMetadata(final Context context, final MetadataResolverCallback metadataResolverCallback, final String url) {
    // Store the callback so we can call it back later
    mMetadataResolverCallback = metadataResolverCallback;
    initialize(context);
    requestUrlMetadata(url);
  }

  /**
   * Start the process that will ask
   * the metadata server for the given url's metadata
   *
   * @param url The url for which to request data
   */
  private static void requestUrlMetadata(String url) {
    if (!mIsInitialized) {
      Log.e(TAG, "Not initialized.");
      return;
    }
    // Create the json request object
    JSONObject jsonObj = createUrlMetadataRequestObject(url);
    // Create the metadata request
    // for the given json request object
    JsonObjectRequest jsObjRequest = createUrlMetadataRequest(jsonObj, false);
    // Queue the request
    mRequestQueue.add(jsObjRequest);
  }

  public static void findDemoUrlMetadata(final Context context, final MetadataResolverCallback metadataResolverCallback) {
    // Store the callback so we can call it back later
    mMetadataResolverCallback = metadataResolverCallback;
    initialize(context);
    requestDemoUrlMetadata();
  }

  private static void requestDemoUrlMetadata() {
    if (!mIsInitialized) {
      Log.e(TAG, "Not initialized.");
      return;
    }
    // Create the metadata request
    // for the given json request object
    JsonObjectRequest jsObjRequest = createUrlMetadataRequest(null, true);
    // Queue the request
    mRequestQueue.add(jsObjRequest);
  }

  /**
   * Create the json request object
   * that will be sent to the metadata server
   * asking for metadata for the given url.
   *
   * @param url The url for which the request data will be created
   * @return The constructed json object
   */
  private static JSONObject createUrlMetadataRequestObject(String url) {
    JSONObject jsonObject = new JSONObject();
    try {
      JSONArray urlJsonArray = new JSONArray();
      JSONObject urlJsonObject = new JSONObject();
      urlJsonObject.put("url", url);
      urlJsonArray.put(urlJsonObject);
      jsonObject.put("objects", urlJsonArray);
    } catch (JSONException ex) {
      Log.d(TAG, "error: " + ex);
    }
    return jsonObject;
  }

  /**
   * Create the url metadata request,
   * given the json request object
   *
   * @param jsonObj The given json object to use in the request
   * @return The created json request object
   */
  private static JsonObjectRequest createUrlMetadataRequest(JSONObject jsonObj, final boolean isDemoRequest) {
    return new JsonObjectRequest(
        isDemoRequest ? DEMO_METADATA_URL : METADATA_URL,
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

                  String title = "";
                  String url = "";
                  String description = "";
                  String iconUrl = "/favicon.ico";
                  String id = jsonUrlMetadata.getString("id");

                  if (jsonUrlMetadata.has("title")) {
                    title = jsonUrlMetadata.getString("title");
                  }
                  if (jsonUrlMetadata.has("url")) {
                    url = jsonUrlMetadata.getString("url");
                  }
                  if (jsonUrlMetadata.has("description")) {
                    description = jsonUrlMetadata.getString("description");
                  }
                  if (jsonUrlMetadata.has("icon")) {
                    // We might need to do some magic here.
                    iconUrl = jsonUrlMetadata.getString("icon");
                  }

                  // TODO: Eliminate this fallback since we expect the server to always return an icon.
                  // Provisions for a favicon specified as a relative URL.
                  if (!iconUrl.startsWith("http")) {
                    // Lets just assume we are dealing with a relative path.
                    Uri fullUri = Uri.parse(url);
                    Uri.Builder builder = fullUri.buildUpon();
                    // Append the default favicon path to the URL.
                    builder.path(iconUrl);
                    iconUrl = builder.toString();
                  }

                  // Create the metadata object
                  UrlMetadata urlMetadata = new UrlMetadata();
                  urlMetadata.title = title;
                  urlMetadata.description = description;
                  urlMetadata.siteUrl = url;
                  urlMetadata.iconUrl = iconUrl;

                  // Kick off the icon download
                  downloadIcon(urlMetadata);

                  if (isDemoRequest) {
                    mMetadataResolverCallback.onDemoUrlMetadataReceived(id, urlMetadata);
                  } else {
                    mMetadataResolverCallback.onUrlMetadataReceived(id, urlMetadata);
                  }
                }

              }
            } catch (JSONException e) {
              e.printStackTrace();
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
  private static void downloadIcon(final UrlMetadata urlMetadata) {
    ImageRequest imageRequest = new ImageRequest(urlMetadata.iconUrl, new Response.Listener<Bitmap>() {
      @Override
      public void onResponse(Bitmap response) {
        urlMetadata.icon = response;
        mMetadataResolverCallback.onUrlMetadataIconReceived();
      }
    }, 0, 0, null, null);
    mRequestQueue.add(imageRequest);
  }

  public static String createUrlProxyGoLink(String url) {
    try {
      url = mContext.getString(R.string.proxy_go_link_base_url) + URLEncoder.encode(url, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return url;
  }

  /**
   * A container class for a url's fetched metadata.
   * The metadata consists of the title, site url, description,
   * iconUrl and the icon (or favicon).
   * This data is scraped via a server that receives a url
   * and returns a json blob.
   */
  public static class UrlMetadata {
    public String title;
    public String siteUrl;
    public String description;
    public String iconUrl;
    public Bitmap icon;

    public UrlMetadata() {
    }

  }
}