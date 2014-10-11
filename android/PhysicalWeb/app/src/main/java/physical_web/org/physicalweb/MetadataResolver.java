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

package physical_web.org.physicalweb;

import android.app.Activity;
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

/**
 * Class for resolving url metadata.
 * Sends requests to the metadata server
 * which then scrapes the page at the given url
 * for its metadata
 */

public class MetadataResolver {
  private static String TAG = "MetadataResolver";
  private static Activity mActivity;
  private static String METADATA_URL = "http://url-caster.appspot.com/resolve-scan";
  private static RequestQueue mRequestQueue;
  private static boolean mIsInitialized = false;
  private static MetadataResolverCallback mMetadataResolverCallback;

  public MetadataResolver(Activity activity) {
    initialize(activity);
  }

  public static void initialize(Context context) {
    if (mRequestQueue == null) {
      mRequestQueue = Volley.newRequestQueue(context);
    }
    mIsInitialized = true;
  }


  /////////////////////////////////
  // callbacks
  /////////////////////////////////

  public interface MetadataResolverCallback {
    public void onUrlMetadataReceived(String url, UrlMetadata urlMetadata);
  }

  /**
   * Called when a url's metadata has been fetched and returned.
   *
   * @param url
   * @param urlMetadata
   */
  private static void onUrlMetadataReceived(String url, UrlMetadata urlMetadata) {
    // Callback to the context that made the request
    mMetadataResolverCallback.onUrlMetadataReceived(url, urlMetadata);
  }


  /////////////////////////////////
  // utilities
  /////////////////////////////////

  public static void findUrlMetadata(final Context context, final MetadataResolverCallback metadataResolverCallback, final String url) {
    // Store the context
    mActivity = (Activity) context;
    // Store the callback so we can call it back later
    mMetadataResolverCallback = metadataResolverCallback;
    mActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        initialize(context);
        requestUrlMetadata(url);
      }
    });
  }

  /**
   * Start the process that will ask
   * the metadata server for the given url's metadata
   *
   * @param url
   */
  public static void requestUrlMetadata(String url) {
    if (!mIsInitialized) {
      Log.e(TAG, "Not initialized.");
      return;
    }
    // Create the json request object
    JSONObject jsonObj = createUrlMetadataRequestObject(url);
    // Create the metadata request
    // for the given json request object
    JsonObjectRequest jsObjRequest = createUrlMetadataRequest(jsonObj);
    // Queue the request
    mRequestQueue.add(jsObjRequest);
  }

  /**
   * Create the json request object
   * that will be sent to the metadata server
   * asking for metadata for the given url.
   *
   * @param url
   * @return
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
    }
    return jsonObject;
  }

  /**
   * Create the url metadata request,
   * given the json request object
   *
   * @param jsonObj
   * @return
   */
  private static JsonObjectRequest createUrlMetadataRequest(JSONObject jsonObj) {
    return new JsonObjectRequest(
        METADATA_URL,
        jsonObj,
        new Response.Listener<JSONObject>() {
          // Called when the server returns a response
          @Override
          public void onResponse(JSONObject jsonResponse) {

            // Build the metadata from the response
            try {
              JSONArray foundMetaData = jsonResponse.getJSONArray("metadata");

              if (foundMetaData.length() > 0) {

                JSONObject jsonUrlMetadata = foundMetaData.getJSONObject(0);

                String title = "Unknown name";
                String url = "Unknown url";
                String description = "Unknown description";
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

                UrlMetadata urlMetadata = new UrlMetadata();
                urlMetadata.title = title;
                urlMetadata.description = description;
                urlMetadata.siteUrl = url;
                urlMetadata.iconUrl = iconUrl;
                downloadIcon(urlMetadata, url);

                onUrlMetadataReceived(id, urlMetadata);
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
   * @param urlMetadata
   * @param url
   */
  private static void downloadIcon(final UrlMetadata urlMetadata, final String url) {
    ImageRequest imageRequest = new ImageRequest(urlMetadata.iconUrl, new Response.Listener<Bitmap>() {
      @Override
      public void onResponse(Bitmap response) {
        urlMetadata.icon = response;
        onUrlMetadataReceived(url, urlMetadata);
      }
    }, 0, 0, null, null);
    mRequestQueue.add(imageRequest);
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
